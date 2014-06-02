package uk.co.ordnancesurvey.android.maps;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/*
final class TileData
{
	public final byte[] bytes;
}
*/

interface TileFetcherDelegate
{
	public abstract void tileReadyAsyncCallback(final MapTile tile, final Bitmap bmp);
}

/**
 * This class is NOT threadsafe. It is designed to be used from a single thread.
 * Do not call clear() or requestBitmapForTile() without first calling lock() and subsequently calling unlock()
 * 
 */
final class TileFetcher {
	private final static String TAG = "TileFetcher";

	private final Context mContext;
	private final TileFetcherDelegate mDelegate;
	private final ReentrantLock mLock = new ReentrantLock();
	private final Condition mFull = mLock.newCondition();
	
	private final HashSet<MapTile> mRequests = new HashSet<MapTile>();
	
	private final LinkedList<MapTile> mFetches = new LinkedList<MapTile>();
	private volatile OSTileSource[] mVolatileSynchronousSources = new OSTileSource[0];
	private volatile OSTileSource[] mVolatileAsynchronousSources = new OSTileSource[0];
	
	// Threads are initially stopped.
	private boolean mStopThread = true;
	private final TileFetchThread mAsynchronousFetchThreads[] = new TileFetchThread[]{
		new TileFetchThread(),
		new TileFetchThread(),
		//new TileFetchThread(),
		//new TileFetchThread(),
		//new TileFetchThread(),
	};

	private final TileCache mTileCache;

	private boolean mNetworkReachable;

	private final BroadcastReceiver mNetworkReceiver;
	
	public TileFetcher(Context context, TileFetcherDelegate delegate)
	{
		mContext = context;
		mDelegate = delegate;

		ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		int memoryClass = activityManager.getMemoryClass();
		int memoryMB = memoryClass/2;
		int diskMB = 128;
		File cacheDir = new File(context.getCacheDir(), "uk.co.ordnancesurvey.android.maps.TILE_CACHE");
		int appVersion;
		try {
			appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Failed to get package version for " + context.getPackageName(), e);
			assert !BuildConfig.DEBUG : "This shouldn't happen!";
			appVersion = 1;
		}
		mTileCache = TileCache.newInstance(memoryMB, diskMB, cacheDir, appVersion);

		mNetworkReceiver = new BroadcastReceiver() {
			@Override 
			public void onReceive(Context context, Intent intent) {
				onNetworkChange();
			}
		};
		
		start();
	}

	public void setTileSources(Collection<OSTileSource> sources)
	{
		ArrayList<OSTileSource> synchronousSources = new ArrayList<OSTileSource>(sources.size());
		ArrayList<OSTileSource> asynchronousSources = new ArrayList<OSTileSource>(sources.size());
		for (OSTileSource source : sources)
		{
			boolean synchronous = source.isSynchronous();
			ArrayList<OSTileSource> sourceList = (synchronous ? synchronousSources : asynchronousSources);
			sourceList.add(source);
		}
		mVolatileSynchronousSources = synchronousSources.toArray(new OSTileSource[0]);
		mVolatileAsynchronousSources = asynchronousSources.toArray(new OSTileSource[0]);
	}

	// This can be called without holding a lock.
	public void finishRequest(MapTile tile)
	{
		mRequests.remove(tile);
	}

	public void lock()
	{
		assert !mLock.isHeldByCurrentThread() : "This lock shouldn't need to be recursive";
		mLock.lock();
	}
	
	public void unlock()
	{
		assert mLock.isHeldByCurrentThread();
		mLock.unlock();
	}

	public void start()
	{
		if (!mStopThread)
		{
			assert false : "Threads already started!";
			return;
		}

		joinAll();

		mStopThread = false;
		// TODO: Can threads be restarted like this?
		for (TileFetchThread t : mAsynchronousFetchThreads) {
			t.start();
		}

		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(mNetworkReceiver, filter);
	}

	private void joinAll()
	{
		assert mStopThread : "Threads should be stopped when we join.";

		boolean interrupted = false;
		for (TileFetchThread t : mAsynchronousFetchThreads) {
			// Loosely modeled after android.os.SystemClock.sleep():
			//   https://github.com/android/platform_frameworks_base/blob/android-4.2.2_r1/core/java/android/os/SystemClock.java#L108
			for (;;) {
				try {
					t.join();
					break;
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		}
		if (interrupted)
		{
			Thread.currentThread().interrupt();
		}
	}

	public void stop(boolean waitForThreadsToStop)
	{
		if (mStopThread)
		{
			assert false : "Threads already stopped";
			if (waitForThreadsToStop)
			{
				joinAll();
			}
			return;
		}
		mStopThread = true;
		for (TileFetchThread t : mAsynchronousFetchThreads)
		{
			t.interrupt();
		}

		mContext.unregisterReceiver(mNetworkReceiver);
	}
	
	// This must be called with a lock held.
	public void clear()
	{
		assert mLock.isHeldByCurrentThread();
		if(mFetches.size() == mRequests.size())
		{
			mRequests.clear();
		}
		else
		{
			mRequests.removeAll(mFetches);
		}
		mFetches.clear();
	}

	private static int[] getNetworkTypes() {
		if (Build.VERSION.SDK_INT >= 13) {
			return getNetworkTypesAPI13();
		}
		final int[] NETWORK_TYPES = {
				ConnectivityManager.TYPE_WIMAX, 
				ConnectivityManager.TYPE_WIFI, 
				ConnectivityManager.TYPE_MOBILE,
			};
		return NETWORK_TYPES;
	}

	@TargetApi(13)
	private static int[] getNetworkTypesAPI13() {
		final int[] NETWORK_TYPES = {
				ConnectivityManager.TYPE_ETHERNET,
				ConnectivityManager.TYPE_BLUETOOTH,
				ConnectivityManager.TYPE_WIMAX, 
				ConnectivityManager.TYPE_WIFI, 
				ConnectivityManager.TYPE_MOBILE,
			};
		return NETWORK_TYPES;
	}

	void onNetworkChange() {
		ConnectivityManager connectivityManager = (ConnectivityManager)mContext.getSystemService( Context.CONNECTIVITY_SERVICE );

		// We don't care about network changes, but we do care about our general connected state - i.e do we have any kind of 
		// network connection
		
		// getActiveNetworkInfo is the obvious call to use, but is apparently pretty buggy
		// http://code.google.com/p/android/issues/detail?id=11891
		// http://code.google.com/p/android/issues/detail?id=11866
		
		// Would use TYPE_DUMMY, but it requires API Level 13
		final int NETWORK_TYPES[] = getNetworkTypes();

		boolean reachable = false;
		for(int testType : NETWORK_TYPES) {
			NetworkInfo nwInfo = connectivityManager.getNetworkInfo(testType);
			if(nwInfo != null && nwInfo.isConnectedOrConnecting()) {
				reachable = true;
				break;
			}
		}

		boolean wasReachable = mNetworkReachable;
		mNetworkReachable = reachable;
		if(reachable && !wasReachable)
		{
			// We have a network. If this is newly available, we should pump any outstanding requests.
		}
	}
	
	// This must be called with a lock held
	public Bitmap requestBitmapForTile(MapTile tile, boolean asyncFetchOK)
	{
		assert mLock.isHeldByCurrentThread();
		// Attempt a synchronous response.
		Bitmap bmp = bitmapForTile(tile, true);
		if(!asyncFetchOK || bmp != null || mDelegate == null)
		{
			return bmp;
		}

		// Copy the tile!
		tile = new MapTile(tile);

		if(mRequests.add(tile))
		{
			mFetches.add(tile);
			if(mFetches.size() == 1)
			{
				mFull.signal();
			}
		}
		return null;
	}
		
	private Bitmap bitmapForTile(MapTile tile, boolean synchronous)
	{
		if (synchronous)
		{
			byte[] data = mTileCache.get(tile);
			if (data != null)
			{
				return BitmapFactory.decodeByteArray(data, 0, data.length);
			}
		}
		OSTileSource[] sources = synchronous ? mVolatileSynchronousSources : mVolatileAsynchronousSources;
		for (OSTileSource source : sources)
		{
			// Don't try to fetch if the network is down.
			if(source.isNetwork() && !mNetworkReachable)
			{
				continue;
			}
			byte[] data = source.dataForTile(tile);
			if (data == null)
			{
				continue;
			}
			mTileCache.putAsync(new MapTile(tile), data);
			return BitmapFactory.decodeByteArray(data, 0, data.length);
		}
		// TODO how are we handling errors?
		return null;
	}

	// A non-private function so we don't get TileFetcher.access$2 in Traceview.
	void threadFunc()
	{
		while(!mStopThread)
		{
			// Pull an object off the stack, or wait till an object is added.
			// Wait until the queue is not empty.
			MapTile tile = null;

			mLock.lock();
			try
			{
				while(tile == null)
				{
					tile = mFetches.pollFirst();
					if(tile == null)
					{
						try
						{
							mFull.await();
						} catch(InterruptedException e) {
							if (mStopThread) {
								return;
							}
						}
					}
				}
			} finally {
				mLock.unlock();
			}

			Bitmap bmp = bitmapForTile(tile, false);
			mDelegate.tileReadyAsyncCallback(tile, bmp);
		}
	}

	static final AtomicLong sThreadNum = new AtomicLong();
	private class TileFetchThread extends Thread
	{
		public TileFetchThread() {
			// tchan: Do not lower the priority, or scrolling can make tiles load very slowly.
			//this.setPriority(Thread.MIN_PRIORITY);

			this.setName("TileFetchThread-" + sThreadNum.incrementAndGet());
		}
		@Override
		public void run()
		{
			threadFunc();
		}
	};
}
