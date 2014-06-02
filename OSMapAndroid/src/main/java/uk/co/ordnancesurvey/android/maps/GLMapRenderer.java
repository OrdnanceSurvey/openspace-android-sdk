/**
 * OpenSpace Android SDK Licence Terms
 *
 * The OpenSpace Android SDK is protected by © Crown copyright – Ordnance Survey 2013.[https://github.com/OrdnanceSurvey]
 *
 * All rights reserved (subject to the BSD licence terms as follows):.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * Neither the name of Ordnance Survey nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 */
package uk.co.ordnancesurvey.android.maps;
import java.io.File;
import java.nio.*;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


import static android.opengl.GLES20.*;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * There are three coordinate systems (at least) in use here.
 * 
 * DisplayCoordinates are internal to this SDK, and have the origin at the centre of the screen, with x/y increasing towards the top-right. Units are pixels. Markers are rendered 
 * in DisplayCoordinates.
 * 
 * ScreenCoordinates are standard android, i.e. origin top-left, x/y increases towards the bottom-right. We use these as little as possible.
 * 
 * Tiles are rendered in tile coordinates, which have the origin at the bottom left of the grid (not the screen). The units are tiles (i.e. a tile always has dimensions 1x1) and the 
 * actual size of a tile is set up by modifying the projection transform.
 */
final class GLMapRenderer extends GLSurfaceView implements GLSurfaceView.Renderer, TileFetcherDelegate, OSMapPrivate, LocationSource.OnLocationChangedListener {

	private static final String TAG = "GLMapRenderer";
	private final Context mContext;
	
	public GLMapRenderer(Context context, MapScrollController scrollController) {
		
		super(context);
		mContext = context;
	
		mHandler = new Handler(context.getMainLooper());
		if (BuildConfig.DEBUG) {
			if (GLMapRenderer.class.desiredAssertionStatus()) {
				Log.v(TAG, "Assertions are enabled!");
			} else {
				String s = "Assertions are disabled! To enable them, run \"adb shell setprop debug.assert 1\" and reinstall app";
				Log.w(TAG, s);
				Toast.makeText(context, s, Toast.LENGTH_LONG).show();
				// Sanity check that we have the test the right way around.
				assert false;
			}
		}

		if (BuildConfig.DEBUG)
		{
			setDebugFlags(DEBUG_CHECK_GL_ERROR);
		}

		setEGLContextClientVersion(2);

		if (Utils.EMULATOR_GLES_WORKAROUNDS_ENABLED && Build.FINGERPRINT.matches("generic\\/(?:google_)?sdk\\/generic\\:.*") && Build.CPU_ABI.matches("armeabi(?:\\-.*)?"))
		{
			// A bug in the ARM emulator causes it to fail to choose a config, possibly if the backing GL context does not support no alpha channel.
			//
			// 4.2 with Google APIs, ARM:
			//  BOARD == BOOTLOADER == MANUFACTURER == SERIAL == unknown
			//  BRAND == DEVICE == generic
			//  CPU_ABI == armeabi-v7a
			//  CPU_ABI2 == armeabi
			//  DISPLAY == google_sdk-eng 4.2 JB_MR1 526865 test-keys
			//  FINGERPRINT == generic/google_sdk/generic:4.2/JB_MR1/526865:eng/test-keys
			//  HARDWARE == goldfish
			//  HOST == android-mac-sl-10.mtv.corp.google.com
			//  ID == JB_MR1
			//  MODEL == PRODUCT == google_sdk
			//  TAGS == test-keys
			//  TIME == 1352427062000
			//  TYPE == eng
			//  USER == android-build

			Log.w(TAG, "Setting an emulator-compatible GL config; this should not happen on devices!");
			setEGLConfigChooser(8, 8, 8, 8, 8, 0);
		}
		
		//setEGLConfigChooser(new MultiSampleConfigChooser());

		setRenderer(this);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mMinFramePeriodMillis = (int)(1000/((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRefreshRate());

		ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		int memoryClass = activityManager.getMemoryClass();
		// The "memory class" is the recommended maximum per-app memory usage in MB. Let the GL tile cache use around half of this.
		mGLTileCache = new GLTileCache(memoryClass * (1048576/2));
		mGLImageCache = new GLImageCache();

		mScrollController = scrollController;

		mTileFetcher = new TileFetcher(context, this);
		mLocationSource = new OSLocation(context);
	}
	
	private final GLTileCache mGLTileCache;
	private final GLImageCache mGLImageCache;
	private final TileFetcher mTileFetcher;
	private final MapScrollController mScrollController;
	private final MapScrollController.ScrollPosition mScrollState = new MapScrollController.ScrollPosition();
	// TODO: This is an icky default, but ensures that it's not null.
	// This does not actually need to be volatile, but it encourages users to read it once.
	private volatile ScreenProjection mVolatileProjection = new ScreenProjection(320, 320, mScrollState);
	final float[] mMVPOrthoMatrix = new float[16];
	private int mGLViewportWidth, mGLViewportHeight;
	ShaderProgram shaderProgram;
	ShaderOverlayProgram shaderOverlayProgram;
	ShaderCircleProgram shaderCircleProgram;
	GLProgram mLastProgram = null;


	static final FloatBuffer vertexBuffer = Utils.directFloatBuffer(new float[]{
			0,0,
			1,0,
			0,-1,
			1,-1,
	});
	


	// Render thread temporaries. Do not use these outside of the GL thread.
	private final float[] rTempMatrix = new float[32];
	private final PointF rTempPoint = new PointF();
	private final FloatBuffer rTempFloatBuffer = Utils.directFloatBuffer(8);
	private final Rect rTempTileRect = new Rect();
	private final MapTile rTempTile = new MapTile();
	private final FetchQuota rFetchQuota = new FetchQuota();

	private MapLayer[] mLayers;

	private MapLayer mPreviousLayer;
	private MapLayer mFadingOutLayer;
	private long mFadingInStartUptimeMillis;
	private static final int ZOOM_FADE_DURATION= 400; // It's 0.4s in the iOS code.
	private final Handler mHandler;
	private final Runnable mCameraChangeRunnable = new Runnable() {
		public void run() {
			// This listener is set on the main thread, so no problem using it like this.
			if(mOnCameraChangeListener != null)
			{
				ScreenProjection projection = getProjection();
				CameraPosition position = new CameraPosition(projection.getCenter(), projection.getMetresPerPixel());
				mOnCameraChangeListener.onCameraChange(position);
			}
		}
	};
	
	// Markers
	private final LinkedList<Marker> mMarkers = new LinkedList<Marker>();
	private final ReentrantReadWriteLock mMarkersLock = new ReentrantReadWriteLock();
	private InfoWindowAdapter mInfoWindowAdapter;
	private OnMapClickListener mOnMapClickListener; 
	private OnMapLongClickListener mOnMapLongClickListener; 
	private OnMarkerClickListener mOnMarkerClickListener; 
	private OnMarkerDragListener mOnMarkerDragListener; 
	private OnInfoWindowClickListener mOnInfoWindowClickListener;
	private OnCameraChangeListener mOnCameraChangeListener;
	private Marker mExpandedMarker = null;
	// Overlays
	private final LinkedList<PolyOverlay> mPolyOverlays = new LinkedList<PolyOverlay>();
	private final LinkedList<Circle> mCircleOverlays = new LinkedList<Circle>();

	// FPS limiter
	private final int mMinFramePeriodMillis;
	private long mPreviousFrameUptimeMillis;

	// Debug variables.
	private final static boolean DEBUG_FRAME_TIMING = BuildConfig.DEBUG && false;
	private long debugPreviousFrameUptimeMillis;
	private long debugPreviousFrameNanoTime;
	private boolean mMyLocationEnabled;
	private LocationSource mLocationSource;
	private GridPoint mCurrentGridPoint = null;
	private Location mCurrentLocation = null;
	private LocationOverlay mLocationOverlay = null;
	private boolean mForeground;
	private OnMyLocationChangeListener mMyLocationChangeListener;
	
	// Avoid issuing too many location callbacks
	private double lastx;
	private double lasty;
	private float lastMPP;
	
	// Maintain a dirty area for drawing fallbacks. We'll implement this as a simple rect for the moment
	private class DirtyArea 
	{

		private double minX, minY, maxX, maxY;
		
		private boolean mDidDraw;
		boolean isEmpty()
		{
			return Double.isInfinite(minX);
		}
				
		void zero() {
			minX = Double.POSITIVE_INFINITY;
			minY = Double.POSITIVE_INFINITY;
			maxX = Double.NEGATIVE_INFINITY;
			maxY = Double.NEGATIVE_INFINITY;
		}
		
		void drewRect() 
		{
			mDidDraw = true;
		}
		
		boolean didDraw()
		{
			return mDidDraw;
		}
		
		void addDirtyRect(float tilesize, MapTile tile)
		{
			minX = Math.min(minX,  tile.x * tilesize);
			minY = Math.min(minY,  tile.y * tilesize);
			maxX = Math.max(maxX,  tile.x * tilesize + tilesize);
			maxY = Math.max(maxY,  tile.y * tilesize + tilesize);
		}
		
		
		void reset() {
			ScreenProjection projection = mVolatileProjection;
			GridRect visible = projection.getVisibleMapRect();
			// Set to full visible area.
			maxX = visible.maxX;
			minX = visible.minX;
			maxY = visible.maxY;
			minY = visible.minY;
			mDidDraw = false;
		}
	};
	DirtyArea mDirtyArea = new DirtyArea();

	void setMapLayers(MapLayer[] layers) {
		layers = layers.clone();
		Arrays.sort(layers, Collections.reverseOrder(MapLayer.COMPARE_METRES_PER_PIXEL));

		float[] mpps = new float[layers.length];
		int i = 0;
		for (MapLayer layer : layers) {
			mpps[i++] = layer.metresPerPixel;
		}
		mScrollController.setZoomScales(mpps);

		mLayers = layers;
	}

	public void setInfoWindowAdapter(InfoWindowAdapter adapter)
	{
		mInfoWindowAdapter = adapter;
	}
	
	public void setOnMapClickListener(OnMapClickListener listener)
	{
		mOnMapClickListener = listener;
	}

	public void setOnMapLongClickListener(OnMapLongClickListener listener)
	{
		mOnMapLongClickListener = listener;
	}

	public void setOnMarkerClickListener(OnMarkerClickListener listener)
	{
		mOnMarkerClickListener = listener;
	}
	
	public void setOnMarkerDragListener(OnMarkerDragListener listener)
	{
		mOnMarkerDragListener = listener;
	}
	
	public void setOnInfoWindowClickListener(OnInfoWindowClickListener listener)
	{
		mOnInfoWindowClickListener = listener;
	}
	
	public void setMyLocationEnabled(boolean enabled)
	{
		if(mForeground)
		{
			if(enabled)
			{
				mLocationSource.activate(this);
			}
			else
			{
				mLocationOverlay = null;
				mLocationSource.deactivate();

				requestRender();
			}
			mMyLocationEnabled = mLocationSource.isCheckingLocation();
		} 
		else
		{
			mMyLocationEnabled = enabled;
		}
	}

	public final boolean isMyLocationEnabled() 
	{
		return mMyLocationEnabled;
	}
	
	@Override
	public void setOnMyLocationChangeListener(OnMyLocationChangeListener listener)
	{
		mMyLocationChangeListener = listener;
	}
	
	// Called on main thread, and used on main thread
	@Override
	public void setOnCameraChangeListener(OnCameraChangeListener listener)
	{
		mOnCameraChangeListener = listener;
	}

	
	@Override
	public Location getMyLocation() 
	{
		return mCurrentLocation;
	}
	

	@Override
	public GridPoint getMyGridPoint() 
	{
		return mCurrentGridPoint;
	}

	@Override
	public void moveCamera(CameraPosition camera, boolean animated)
	{
		mScrollController.zoomToCenterScale(null, camera.target, camera.zoom, animated);
	}

	@Override
	public void setLocationSource(LocationSource locationSource)
	{
		if(mMyLocationEnabled)
		{
			mLocationOverlay = null;
			mLocationSource.deactivate();
		}
		mLocationSource = locationSource;
		if(mLocationSource == null)
		{
			mMyLocationEnabled = false;
		}
		if(mMyLocationEnabled)
		{
			mLocationSource.activate(this);
			mMyLocationEnabled = mLocationSource.isCheckingLocation();
		}
	}
	
	@Override
	public void onLocationChanged(Location location) 
	{
		LocationOverlay overlay = mLocationOverlay;
		if(overlay == null)
		{
			overlay = new LocationOverlay(this);
		}
		
		// Need to convert the WGS84 Lat/Long to an actual grid ref.
		MapProjection proj = MapProjection.getDefault();
		mCurrentLocation = new Location(location);
		mCurrentGridPoint = proj.toGridPoint(location.getLatitude(), location.getLongitude());
		
		// Correct for display orientation
		Display display = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = 0;
		switch(display.getRotation())
		{
			case 1:
				rotation = -90;
				break;
			case 2:
				rotation = 180;
				break;
			case 3:
				rotation = 90;
				break;
			default:
				break;
				
		}
		location.setBearing(location.getBearing() - rotation);
		
		overlay.setLocation(location);
		
		// tchan: Not currently necessary, since the above methods call requestRender().
		//requestRender();
		mLocationOverlay = overlay;

		if (mMyLocationChangeListener != null)
		{
			mMyLocationChangeListener.onMyLocationChange(mCurrentGridPoint);
		}
	}
	

	void onDestroy() {		
		mTileFetcher.stop(false);
		mLocationSource.deactivate();
	}
	
	public void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");	
		mForeground = true;
		
		if(mMyLocationEnabled)
		{
			mLocationSource.activate(this);
			mMyLocationEnabled = mLocationSource.isCheckingLocation();
		}
	}
	
	public void onPause() {
		super.onPause();
		Log.v(TAG, "onPause");
		
		// TODO - should we stop the tile provider on pause? I think not, but not sure. 
		mLocationSource.deactivate();
		mForeground = false;
	}
	
	GLImageCache getGLImageCache() {
		return mGLImageCache;
	}
	
	// Round the metres per pixel down to 1,2,5, 
	private MapLayer mapLayerForMPP(float metresPerPixel)
	{
		MapLayer bestLayer = null;
		float bestScore = Float.POSITIVE_INFINITY;
		// Precalculate log(mpp). This costs an extra log() but means we don't have to do float division (which might overflow/underflow).
		float logMPP = (float)Math.log(metresPerPixel);
		for (MapLayer layer : mLayers)
		{
			float score = Math.abs((float)Math.log(layer.metresPerPixel)-logMPP);
			if (score < bestScore)
			{
				bestScore = score;
				bestLayer = layer;
			}
		}
		return bestLayer;
	}

	private int indexForMapLayerOrNegative(MapLayer layer) {
		assert layer != null;
		int index = Arrays.binarySearch(mLayers, layer, MapLayer.COMPARE_METRES_PER_PIXEL_REVERSED);
		if (index < 0) {
			assert false : "This might happen if mLayers is changed in another thread. If this happens frequently enough when debugging, onDrawFrame() should be changed to only read mLayers once.";
			return Integer.MIN_VALUE/2;
		}
		return index;
	}

	private MapLayer mapLayerForIndexOrNull(int index) {
		MapLayer[] layers = mLayers;
		if (0 <= index && index < layers.length) {
			return layers[index];
		}
		return null;
	}

	private int bindTextureForTile(MapTile tile, FetchQuota quota)
	{
		int textureId = mGLTileCache.bindTextureForTile(tile);
		if (textureId != 0)
		{
			return textureId;
		}

		// Don't fetch if there's no quota!
		if (quota == null)
		{
			return 0;
		}

		// Don't fetch if we've exceeded limits.
		if(quota.isExceeded())
		{
			return 0;
		}

		Bitmap bmp = mTileFetcher.requestBitmapForTile(tile, quota.canAsyncFetch());
		if (bmp == null)
		{
			quota.fetchFailure();
		}
		else
		{
			quota.fetchSuccess();
			textureId = mGLTileCache.putTextureForTile(tile, bmp);
		}
		return textureId;
	}

	public final void clear() {
		mMarkersLock.writeLock().lock();
		try {
			mMarkers.clear();
			mExpandedMarker = null;
		} finally {
			mMarkersLock.writeLock().unlock();
		}

		synchronized (mPolyOverlays) {
			mPolyOverlays.clear();
		}

		synchronized (mCircleOverlays) {
			mCircleOverlays.clear();
		}

		requestRender();
	}
	
	public final Marker addMarker(MarkerOptions markerOptions)
	{
		Bitmap icon = markerOptions.getIcon().loadBitmap(getContext());
		Marker marker = new Marker(markerOptions, icon, this);
		mMarkersLock.writeLock().lock();
		try {
			mMarkers.add(marker);
		} finally {
			mMarkersLock.writeLock().unlock();
		}
		requestRender();
		return marker;
	}
	
	public void removeMarker(Marker marker)
	{
		mMarkersLock.writeLock().lock();
		try {
			mMarkers.remove(marker);
			if (mExpandedMarker == marker)
			{
				mExpandedMarker = null;
			}
		} finally {
			mMarkersLock.writeLock().unlock();
		}
		requestRender();
	}

	@Override
	public final Polyline addPolyline(PolylineOptions polylineOptions)
	{
		Polyline polyline = new Polyline(polylineOptions, this);
		synchronized (mPolyOverlays) {
			mPolyOverlays.add(polyline);
		}
		requestRender();
		return polyline;
	}

	@Override
	public final Polygon addPolygon(PolygonOptions polygonOptions)
	{
		Polygon polygon = new Polygon(polygonOptions, this);
		synchronized (mPolyOverlays) {
			mPolyOverlays.add(polygon);
		}
		requestRender();
		return polygon;
	}

	void removePolyOverlay(PolyOverlay polygon)
	{
		synchronized (mPolyOverlays) {
			mPolyOverlays.remove(polygon);
		}
		requestRender();
	}

	@Override
	public final Circle addCircle(CircleOptions circleOptions)
	{
		Circle circle = new Circle(circleOptions, this);
		synchronized (mCircleOverlays) {
			mCircleOverlays.add(circle);
		}
		requestRender();
		return circle;
	}

	void removeCircle(Circle circle)
	{
		synchronized (mCircleOverlays) {
			mCircleOverlays.remove(circle);
		}
		requestRender();
	}

	void removeOverlay(ShapeOverlay shapeOverlay) {
		if (shapeOverlay instanceof Circle) {
			removeCircle((Circle)shapeOverlay);
		} else {
			removePolyOverlay((PolyOverlay)shapeOverlay);
		}
	}

	public void tileReadyAsyncCallback(final MapTile tile, final Bitmap bmp)
	{
		queueEvent(new Runnable() {
			public void run() {
				tileReadyCallback(tile, bmp);
			}			
		});
	}

	
	public void tileReadyCallback(final MapTile tile, final Bitmap bmp)
	{
		if (bmp != null)
		{
			mGLTileCache.putTextureForTile(tile, bmp);
		}
		mTileFetcher.finishRequest(tile);
		if(bmp != null)
		{
			requestRender();
		}
	}

	private void roundToPixelBoundary() {
		// OS-56: A better pixel-aligned-drawing algorithm.
		float originalMPP = mScrollState.metresPerPixel;
		MapLayer layer = mapLayerForMPP(originalMPP);
		float originalSizeScreenPx = layer.tileSizeMetres/originalMPP;
		float roundedSizeScreenPx = (float)Math.ceil(originalSizeScreenPx);
		float roundedMPP = layer.tileSizeMetres/roundedSizeScreenPx;
		if (mapLayerForMPP(roundedMPP) != layer) {
			// If rounding up would switch layer boundaries, try rounding down.
			roundedSizeScreenPx = (float)Math.floor(originalSizeScreenPx);
			roundedMPP = layer.tileSizeMetres/roundedSizeScreenPx;
			// If that breaks too, we're in trouble.
			if (roundedSizeScreenPx < 1 || mapLayerForMPP(roundedMPP) != layer) {
				assert false : "This shouldn't happen!";
				return;
			}
		}

		double tileOriginX = Math.floor(mScrollState.x/layer.tileSizeMetres)*layer.tileSizeMetres;
		double tileOriginY = Math.floor(mScrollState.y/layer.tileSizeMetres)*layer.tileSizeMetres;

		// OS-57: Fudge the rounding by half a pixel if the screen width is odd.
		double halfPixelX = (mGLViewportWidth%2 == 0 ? 0 : 0.5);
		double halfPixelY = (mGLViewportHeight%2 == 0 ? 0 : 0.5);
		double roundedOffsetPxX = Math.rint((mScrollState.x - tileOriginX)/roundedMPP - halfPixelX) + halfPixelX;
		double roundedOffsetPxY = Math.rint((mScrollState.y - tileOriginY)/roundedMPP - halfPixelY) + halfPixelY;

		mScrollState.metresPerPixel = roundedMPP;
		mScrollState.x = tileOriginX+roundedOffsetPxX*roundedMPP;
		mScrollState.y = tileOriginY+roundedOffsetPxY*roundedMPP;

		// TODO: tchan: Check that it's rounded correctly, to within about 1e-4 of a pixel boundary. Something like this.
		//assert Math.abs(Math.IEEEremainder((float)(tileRect.left-mapCenterX/mapTileSize)*screenTileSize, 1)) < 1.0e-4f;
		//assert Math.abs(Math.IEEEremainder((float)(tileRect.top-mapCenterY/mapTileSize)*screenTileSize, 1)) < 1.0e-4f;
	}

	void setProgram(GLProgram program)
	{
		if(program == mLastProgram)
		{
			return;
		}
		
		if(mLastProgram != null)
		{
			mLastProgram.stopUsing();
		}
		
		program.use();
		mLastProgram = program;
	}
	@Override
	public void onDrawFrame(GL10 unused) {
		// Get the timestamp ASAP. Future code might use this to time animations; we want them to be as smooth as possible.
		final long nowUptimeMillis;

		final boolean LIMIT_FRAMERATE = true;
		if (LIMIT_FRAMERATE) {
			// OS-62: This seems to make scrolling smoother.
			long now = SystemClock.uptimeMillis();
			long timeToSleep = mMinFramePeriodMillis - (now-mPreviousFrameUptimeMillis);
			if (0 < timeToSleep && timeToSleep <= mMinFramePeriodMillis) {
				SystemClock.sleep(timeToSleep);
				now += timeToSleep;
			}
			nowUptimeMillis = now;
			mPreviousFrameUptimeMillis = nowUptimeMillis;
		} else {
			nowUptimeMillis = SystemClock.uptimeMillis();
		}

		final long debugDiffUptimeMillis, debugDiffNanoTime;
		if (DEBUG_FRAME_TIMING) {
			// OS-60: Support code. Do this "ASAP" too, so the times are as accurate as possible.
			final long nowNanoTime = System.nanoTime();
			debugDiffUptimeMillis = nowUptimeMillis-debugPreviousFrameUptimeMillis;
			debugDiffNanoTime = nowNanoTime-debugPreviousFrameNanoTime;
			debugPreviousFrameUptimeMillis = nowUptimeMillis;
			debugPreviousFrameNanoTime = nowNanoTime;
		} else {
            debugDiffUptimeMillis = 0;
            debugDiffNanoTime = 0;
        }

		// Update the scroll position too, because it uses SystemClock.uptimeMillis() internally (ideally we'd pass it the timestamp we got above).
		mScrollController.getScrollPosition(mScrollState, true);
		roundToPixelBoundary();
		// And create a new projection.
		ScreenProjection projection = new ScreenProjection(mGLViewportWidth, mGLViewportHeight, mScrollState);
		if (DEBUG_FRAME_TIMING) {
			// OS-60 OS-62: Print inter-frame time (based on time at entry to onDrawFrame(), whether we're "animating" (e.g. flinging), and the distance scrolled in metres.
			ScreenProjection oldProjection = mVolatileProjection;
			Log.v(TAG, debugDiffUptimeMillis + " " + debugDiffNanoTime + " AS=" + mScrollState.animatingScroll + " dx=" + (projection.getCenter().x - oldProjection.getCenter().x) + " dy=" + (projection.getCenter().y - oldProjection.getCenter().y));
		}
		mVolatileProjection = projection;

		//leakGPUMemory();
		// At the start of each frame, mark each tile as off-screen.
		// They are marked on-screen as part of tile drawing.
		mGLTileCache.resetTileVisibility();
		mTileFetcher.lock();
		mTileFetcher.clear();
		
		Utils.throwIfErrors();
		

		glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);			

		setProgram(shaderProgram);

		
		Utils.throwIfErrors();
	
		glActiveTexture(GL_TEXTURE0);
		glUniform1i(shaderProgram.uniformTexture, 0);

		float metresPerPixel = projection.getMetresPerPixel();

		boolean animating = (mScrollState.animatingScroll || mScrollState.animatingZoom);
		boolean needRedraw = animating;
		// OS-50: Ideally we'd set this to fadingToLayer during an animated fade+zoom, but this would require resetting it if we decide not to fade.
		// (Not resetting it could mean looping over several million tiles when animating from fully zoomed-out to fully zoomed-in.)
		// This only makes a difference if we fail to render a frame in the second half of the zoom animation.

		MapLayer currentLayer = mapLayerForMPP(metresPerPixel);
		MapLayer fadingFromLayer = null;
		MapLayer fadingToLayer = null;
		float fadeToAlpha = 0;
		if (mScrollState.animatingZoom)
		{
			float startMPP = mScrollState.animationStartMetresPerPixel;
			float finalMPP = mScrollState.animationFinalMetresPerPixel;
			fadingFromLayer = mapLayerForMPP(startMPP);
			fadingToLayer = mapLayerForMPP(finalMPP);
			if (fadingFromLayer == fadingToLayer) {
				// If we're not actually changing layers, do this to avoid the assert crash.
				fadingFromLayer = null;
				fadingToLayer = null;
				mFadingOutLayer = null;
			} else {
				fadeToAlpha = (float)((Math.log(metresPerPixel)-Math.log(startMPP))/(Math.log(finalMPP)-Math.log(startMPP)));

				// OS-50 If the zoom gets interrupted, this will continue the fade roughly as if it was non-animated.
				if (currentLayer != fadingToLayer)
				{
					mFadingOutLayer = currentLayer;
				}
				// OS-50: Also set the alpha such that if we subsequently calculate fadeToAlpha we get roughly the same result.
				// TODO: This also affects the end of an animated zoom! Sigh.
				mFadingInStartUptimeMillis = nowUptimeMillis - (long)(fadeToAlpha*ZOOM_FADE_DURATION);
				assert Math.abs(fadeToAlpha - (nowUptimeMillis-mFadingInStartUptimeMillis)/(float)ZOOM_FADE_DURATION) < 1/256.0f;
			}
		}
		else
		{
			if (mPreviousLayer != currentLayer && mFadingOutLayer != mPreviousLayer)
			{
				mFadingOutLayer = mPreviousLayer;
				mFadingInStartUptimeMillis = nowUptimeMillis;
			}
			else if (mFadingOutLayer != null && nowUptimeMillis >= mFadingInStartUptimeMillis+ZOOM_FADE_DURATION)
			{
				mFadingOutLayer = null;
			}

			if (mFadingOutLayer != null)
			{
				fadingToLayer = currentLayer;
				fadingFromLayer = mFadingOutLayer;
				fadeToAlpha = (nowUptimeMillis-mFadingInStartUptimeMillis)/(float)ZOOM_FADE_DURATION;
			}
		}
		mPreviousLayer = currentLayer;

		if (fadingToLayer != null && Math.abs(indexForMapLayerOrNegative(fadingFromLayer)-indexForMapLayerOrNegative(fadingToLayer)) != 1)
		{
			fadingFromLayer = null;
			fadingToLayer = null;
		}

		final boolean fading = (fadingToLayer != null);
		assert fading == (fadingFromLayer != null);
		// If we are fading, use the layer we're fading *from* as the "base layer" for deciding which fallbacks to render.
		// This matches what the (old) iOS maps app does.
		final MapLayer baseLayer = (fading ? fadingFromLayer : currentLayer);


		// Reset the fetch quota. It doesn't really matter where we do this, as long as we do it before drawLayer.
		rFetchQuota.reset(nowUptimeMillis);

		// We need to enable depth-testing to write to the depth buffer.
		glEnable(GL_DEPTH_TEST);
		// The default depth function is GL_LESS, so we don't actually need to pass in any depths (for now).
		
		if(fading)
		{
			rFetchQuota.setNoAsyncFetches();
		}

	
		float depth = 0.5f;
		float alpha = 1.0f;
		
		mDirtyArea.reset();
		glVertexAttribPointer(shaderProgram.attribVCoord, 2, GL_FLOAT, false, 0, vertexBuffer);

		// Don't execute any fetches on a layer that is fading out.
		if(fadingToLayer != null)
		{
			rFetchQuota.setNoAsyncFetches();
		}
		needRedraw |= drawLayerWithFallbacks(baseLayer, rFetchQuota, alpha, depth);
		depth = 0.0f;
		alpha = fadeToAlpha;
		if(!mDirtyArea.didDraw())
		{
			Log.v(TAG, "Failed to draw any tiles!");
		}
		mDirtyArea.reset();
		drawLayerWithFallbacks(fadingToLayer, rFetchQuota, fadeToAlpha, depth);
		
		glDisable(GL_DEPTH_TEST);

		// Always redraw if we're fading.
		needRedraw |= fading;

		mTileFetcher.unlock();

		Utils.throwIfErrors();

	
		// Enable alpha-blending
		glEnable(GL_BLEND);

		// Draw overlays
		setProgram(shaderOverlayProgram);

		synchronized (mPolyOverlays) {
			for(PolyOverlay poly: mPolyOverlays)
			{
				poly.glDraw(mMVPOrthoMatrix, rTempMatrix, rTempPoint, metresPerPixel);
			}
		}
		Utils.throwIfErrors();

		setProgram(shaderCircleProgram);

		{
			// TODO: Render circles in screen coordinates!
			float[] tempMatrix = rTempMatrix;
			Matrix.translateM(tempMatrix, 0, mMVPOrthoMatrix, 0, mGLViewportWidth/2.0f, mGLViewportHeight/2.0f, 0);
			Matrix.scaleM(tempMatrix, 0, 1, -1, 1);
			glUniformMatrix4fv(shaderCircleProgram.uniformMVP, 1, false, tempMatrix, 0);
		}

		Utils.throwIfErrors();
		synchronized (mCircleOverlays) {
			for(Circle circle: mCircleOverlays)
			{
				circle.glDraw(rTempPoint, rTempFloatBuffer);
			}
		}
		Utils.throwIfErrors();

		// Draw location overlays. Read the ivars once to give some semblance of thread safety.
		LocationOverlay overlay = mLocationOverlay;
		if(overlay != null)
		{
			overlay.glDraw(mMVPOrthoMatrix, rTempMatrix, rTempPoint, rTempFloatBuffer, metresPerPixel);
		}
		Utils.throwIfErrors();

		setProgram(shaderProgram);

		drawMarkers(projection);

		
		if(needRedraw)
		{
			requestRender();
		}		
		
		// Only make a callback if the state of the map has changed.
		if(!animating)
		{
			if(lastx != mScrollState.x || lasty != mScrollState.y || lastMPP != mScrollState.metresPerPixel)
			{
				if(mOnCameraChangeListener != null)
				{
					// Notify on the main thread
					mHandler.removeCallbacks(mCameraChangeRunnable);
					mHandler.post(mCameraChangeRunnable);
				}
				lastx = mScrollState.x;
				lasty = mScrollState.y;
				lastMPP = mScrollState.metresPerPixel;
			}			
		}
	}

	// Callback from marker when show/hideInfoWindow is called
	void onInfoWindowShown(Marker marker)
	{
		// if we are setting a new expanded marker, hide the old one.
		assert !mMarkersLock.isWriteLockedByCurrentThread();
		mMarkersLock.writeLock().lock();
		try {
			if(mExpandedMarker != null && mExpandedMarker != marker)
			{
				// Need to hide the old one. This will cause a callback into this function
				// with a null parameter
				mExpandedMarker.hideInfoWindow();
			}
			mExpandedMarker = marker;
		} finally {
			mMarkersLock.writeLock().unlock();
		}
		requestRender();
	}
	
	public boolean singleClick(float screenx, float screeny)
	{
		boolean handled = false;
		
		ScreenProjection projection = mVolatileProjection;
		PointF screenLocation = new PointF(screenx, screeny);

		// Check for a click on an info window first.
		if(mExpandedMarker != null)
		{
			if(mExpandedMarker.isClickOnInfoWindow(screenLocation))
			{
				if(mOnInfoWindowClickListener != null)
				{
					mOnInfoWindowClickListener.onInfoWindowClick(mExpandedMarker);
				}
			}
		}
		
		
		// TODO do we need to handle stacked markers where one marker declines the touch?
		Marker marker = findMarker(projection, screenLocation);
		if(marker != null)
		{
			handled = false;
			if(mOnMarkerClickListener != null)
			{
				handled = mOnMarkerClickListener.onMarkerClick(marker);					
			}
			if(!handled)
			{
				if(marker == mExpandedMarker)
				{
					marker.hideInfoWindow();
				}
				else
				{
					marker.showInfoWindow();
				}
				// TODO move map to ensure visible
				handled = true;
			}
		}
		
		if(!handled)
		{
			if(mOnMapClickListener != null)
			{
				GridPoint gp = projection.fromScreenLocation(screenx, screeny);
				handled = mOnMapClickListener.onMapClick(gp);
			}
			if(!handled)
			{
				// TODO move camera here
			}
		}
		return handled;
	}
	
	private static final int MARKER_DRAG_OFFSET = 70;
	
	/**
	 * Return the object handling the long click, if we want to initiate a drag
	 */
	public Object longClick(float screenx, float screeny)
	{
		ScreenProjection projection = mVolatileProjection;
		GridPoint gp = projection.fromScreenLocation(screenx, screeny);
		GridPoint gp2 = projection.fromScreenLocation(screenx, screeny-MARKER_DRAG_OFFSET);

		// Check gp2 as well, because we don't want to lift a marker out of bounds.
		if(!gp.isInBounds() || !gp2.isInBounds())
		{
			return null;
		}

		// TODO do we need to handle stacked markers where one marker declines the touch?
		Marker marker = findDraggableMarker(projection, new PointF(screenx, screeny));
		if(marker != null)
		{
			if(mOnMarkerDragListener != null)
			{
				mOnMarkerDragListener.onMarkerDragStart(marker);
			}

			// Set position up a bit, so we can see the marker.
			updateMarkerPosition(marker, screenx, screeny);
			// TODO scroll map up if necessary
			return marker;
		}
		
		if(mOnMapLongClickListener != null)
		{
			mOnMapLongClickListener.onMapLongClick(gp);
		}
		return null;
	}
	
	public void drag(float screenx, float screeny, Object draggable)
	{
		if(draggable instanceof Marker)
		{

			Marker marker = (Marker)draggable;
			if(mOnMarkerDragListener != null)
			{
				mOnMarkerDragListener.onMarkerDrag(marker);
			}
			
			updateMarkerPosition(marker, screenx, screeny);
		}
	}
	
	private void updateMarkerPosition(Marker marker, float x, float y) {

		ScreenProjection projection = mVolatileProjection;

		GridPoint gp = projection.fromScreenLocation(x, y).clippedToGridBounds();
		marker.setGridPoint(gp);
	}

	public void dragEnded(float screenx, float screeny, Object draggable)
	{
		if(draggable instanceof Marker)
		{
			Marker marker = (Marker)draggable;
			if(mOnMarkerDragListener != null)
			{
				mOnMarkerDragListener.onMarkerDragEnd(marker);
			}

			updateMarkerPosition(marker, screenx, screeny);
		}
	}

	private interface MarkerCallable<T>
	{
		// Return true if iteration should stop.
		abstract boolean run(Marker marker, T params);
	}
	
	public View getInfoWindow(Marker marker)
	{
		View view = null;
		View contentView = null;
		if(mInfoWindowAdapter != null)
		{
			view = mInfoWindowAdapter.getInfoWindow(marker);
			if (view == null)
			{
				contentView = mInfoWindowAdapter.getInfoContents(marker);
			}
		}
		if(view == null)
		{
			view = defaultInfoWindow(marker, contentView);
		}

		// OS-80 The view might be null here (if there's nothing to display in the info window)
		if (view != null && (view.getWidth() == 0 || view.getHeight() == 0))
		{
			// Force a layout if the width or height is 0. Should we do this all the time?
			layoutInfoWindow(view);
		}
		return view;
	}

	private View defaultInfoWindow(Marker marker, View contentView)
	{
		String title = marker.getTitle();
		String snippet = marker.getSnippet();
		if (contentView == null && title == null && snippet == null)
		{
			// Can't show anything
			return null;
		}

		Context context = getContext();

		// use the default info window, with title and snippet
		LinearLayout layout = new LinearLayout(context);
		layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);
		NinePatchDrawable drawable = Images.getInfoBgDrawable(context.getResources());
		viewSetBackgroundCompat(layout, drawable);

		if (contentView != null)
		{
			layout.addView(contentView);
		}
		else
		{
			// Need a background image for the marker.
			if (title != null)
			{
				TextView text = new TextView(context);
				text.setText(title);
				text.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				text.setGravity(Gravity.CENTER);
				layout.addView(text);
			}
	
			// Add snippet if present
			if(snippet != null)
			{
				TextView text = new TextView(context);
				text.setText(snippet);
				text.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				text.setTextColor(0xffbdbdbd);
				text.setGravity(Gravity.CENTER);
				layout.addView(text);
			}
		}

		layoutInfoWindow(layout);
		return layout;
	}

	private void layoutInfoWindow(View v) {
		measureAndLayout(v, 500, 500);
	}

	private void measureAndLayout(View v, int width, int height) {
		// Do an unconstrained layout first, because...
		int unconstrained = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		v.measure(unconstrained, unconstrained);

		int measuredW = v.getMeasuredWidth();
		int measuredH = v.getMeasuredHeight();
		if (measuredW > width || measuredH >= height)
		{
			// ... If the LinearLayout has children with unspecified LayoutParams,
			// the LinearLayout seems to fill the space available.
			v.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
			measuredW = v.getMeasuredWidth();
			measuredH = v.getMeasuredHeight();
		}

		v.layout(0,0,measuredW,measuredH);
	}

	@SuppressWarnings("deprecation")
	private void viewSetBackgroundCompat(View view, Drawable bg) {
		if (Build.VERSION.SDK_INT >= 16) {
			viewSetBackgroundAPI16(view, bg);
		} else {
			// Deprecated in API level 16, but we need to support 10.
			view.setBackgroundDrawable(bg);
		}
	}

	@TargetApi(16)
	private void viewSetBackgroundAPI16(View view, Drawable bg) {
		view.setBackground(bg);
	}

	private final MarkerCallable<PointF> mDrawMarkerCallable = new MarkerCallable<PointF>() {
		@Override
		public boolean run(Marker marker, PointF tempPoint)
		{
			marker.glDraw(mMVPOrthoMatrix, rTempMatrix, mGLImageCache, tempPoint);
			return false;
		}
	};

	private void drawMarkers(final ScreenProjection projection) {
		// Draw from the bottom up, so that top most marker is fully visible even if overlapped
		iterateVisibleMarkers(true, projection, mDrawMarkerCallable, rTempPoint);
	}

	private Marker findMarker(final ScreenProjection projection, PointF screenLocation, final boolean draggableOnly) {
		final PointF tempPoint = new PointF();
		final RectF tempRect = new RectF();
		MarkerCallable<PointF> callable = new MarkerCallable<PointF>() {
			@Override
			public boolean run(Marker marker, PointF screenLocation)
			{
				return (!draggableOnly || marker.isDraggable()) && marker.containsPoint(projection, screenLocation, tempPoint, tempRect);
			}	
		};		
		// Iterate from the top-down, since we're looking to capture a click.
		return iterateVisibleMarkers(false, projection, callable, screenLocation);
	}

	private Marker findMarker(ScreenProjection projection, PointF screenLocation) {
		return findMarker(projection, screenLocation, false);
	}

	private Marker findDraggableMarker(ScreenProjection projection, PointF screenLocation) {
		return findMarker(projection, screenLocation, true);
	}

	private <T> Marker processMarker(Marker marker, GridRect checkRect, MarkerCallable<T> callable, T params)
	{
		// Check bounds
		if(marker == null)
		{
			return null;
		}
		
		GridPoint gp = marker.getGridPoint();

		// Skip invisible or out of visible area markers
		if(!marker.isVisible() || !checkRect.contains(gp))
		{
			return null;
		}
		
		if(callable.run(marker, params))
		{
			return marker;
		}
		
		return null;
	}
	
	private <T> Marker iterateVisibleMarkers(boolean bottomUp, ScreenProjection projection, MarkerCallable<T> callable, T params)
	{
		Marker ret = null;
		
		// Look at more markers than are nominally visible, in case their bitmap covers the relevant area.
		
		// We extend to four times the actual screen area.
		
		// TODO can we do something more intelligent than this... like remember the maximum bitmap size for markers, plus take
		// account of anchors?
		GridRect checkRect = projection.getExpandedVisibleMapRect();
		
		mMarkersLock.readLock().lock();
		{
			Iterator<Marker> iter;
			if(bottomUp)
			{
				iter= mMarkers.iterator();
			}
			else
			{
				iter = mMarkers.descendingIterator();
				ret = processMarker(mExpandedMarker, checkRect, callable, params);
			}
			Marker marker = null;
			
			while(ret == null && iter.hasNext()) {
				marker = iter.next();
				// processMarker returns non-null if iteration should stop.
				ret = processMarker(marker, checkRect, callable, params);
			}
			
			if(ret == null && bottomUp)
			{
				ret = processMarker(mExpandedMarker, checkRect, callable, params);
			}
			
		} 
		mMarkersLock.readLock().unlock();
		return ret;
	}
	

	private boolean drawLayerWithFallbacks(MapLayer layer, FetchQuota quota, float alpha, float depth) 
	{
		if(layer == null)
		{
			return false;
		}
	
		int baseLayerIndex = indexForMapLayerOrNegative(layer);


		boolean needsRedraw = drawLayer(layer, quota, alpha, depth);
		
		
		MapLayer fallbackLayer = null;
		// Fallback in preference to +1, -1, -2, -3. 
		for(int i = 1; !mDirtyArea.isEmpty() && i >= -3 ; i--)
		{
			// If we are rendering with alpha != 1.0, then only draw one layer at most, to avoid overlapping transparency.		
			if(alpha < 1.0 && mDirtyArea.didDraw())
			{
				break;
			}
			
			// Skip over 0 difference, as that's the same as the desired layer. 
			if(i == 0)
			{
				i = i - 1;
			}
			depth += 0.1f;


			fallbackLayer = mapLayerForIndexOrNull(baseLayerIndex + i);
			needsRedraw |= drawLayer(fallbackLayer, quota, alpha, depth);
		}
//		if(fallbackLayer != null)
//		{
//			Log.v(TAG, "Rendered " + layer.productCode + " fell back to " + fallbackLayer.productCode + " at " + alpha + " " + mDirtyArea.isEmpty() + " " + needsRedraw);
//		}
//		else
//		{
//			Log.v(TAG, "Rendered " + layer.productCode + " at " + alpha + " " + mDirtyArea.isEmpty() + " " + needsRedraw);			
//		}
		return needsRedraw;
	}
	
	private boolean drawLayer(MapLayer layer, FetchQuota quota, float alpha, float depth) {
		if (layer == null)
		{
			return false;
		}

		ScreenProjection projection = mVolatileProjection;
		float metresPerPixel = projection.getMetresPerPixel();
		
		GridRect visibleMapRect = projection.getVisibleMapRect();

		float mapTileSize = layer.tileSizeMetres;
		float screenTileSize = mapTileSize / metresPerPixel;

		double mapTopLeftX = visibleMapRect.minX;
		double mapTopLeftY = visibleMapRect.maxY;

		// Draw only the dirty area.
		Rect tileRect = rTempTileRect;
		tileRect.left = (int)Math.floor(mDirtyArea.minX/mapTileSize);
		tileRect.top = (int)Math.floor(mDirtyArea.minY/mapTileSize);
		tileRect.right = (int)Math.ceil(mDirtyArea.maxX/mapTileSize);
		tileRect.bottom = (int)Math.ceil(mDirtyArea.maxY/mapTileSize);

		mDirtyArea.zero();
		

		// Set alpha.
		glUniform4f(shaderProgram.uniformTintColor, -1, -1, -1, alpha);

		// Blend if alpha is not 1.
		if (alpha == 1.0f) {
			glDisable(GL_BLEND);
		} else {
			glEnable(GL_BLEND);
		}

		// Set up projection matrix	so we can refer to things in tile coordinates.
		{
			float[] mvpTempMatrix = rTempMatrix;
			Matrix.scaleM(mvpTempMatrix, 0, mMVPOrthoMatrix, 0, screenTileSize, screenTileSize, 1);
			glUniformMatrix4fv(shaderProgram.uniformMVP, 1, false, mvpTempMatrix, 0);
		}

		// Render from the centre, spiralling out. 
		MapTile tile = rTempTile;
		tile.set(tileRect.centerX(), tileRect.centerY(), layer);
		
		int tilesWide = tileRect.width();
		int tilesHigh = tileRect.height();
		int numTiles = Math.max(tilesWide, tilesHigh);
		if((numTiles & 1) == 0)
		{
			numTiles++;
		}
		numTiles = numTiles * numTiles;
		int offy = 1;
		int offx = 0;
		if(tilesWide > tilesHigh)
		{
			offx = 1;
			offy = 0;
		}
		
		int tilesNeeded = 1;
		int tilesDrawn = 0;
		int line = 0;

		boolean needRedraw = false;
		for(int i = 0; i < numTiles; i++)
		{
			// Is the tile actually visible?
			if(tileRect.contains(tile.x, tile.y))
			{
				if(bindTextureForTile(tile, quota) != 0)
				{
					// Draw this texture in the correct place. The offset is expressed in tiles (and can be a fraction of a tile).
					// tchan: We cast to float at the end to avoid losing too much precision.
					glVertexAttrib4f(shaderProgram.attribVOffset, (float)(tile.x - mapTopLeftX/mapTileSize), -(float)(tile.y - mapTopLeftY/mapTileSize), depth, 1);
					glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
					// Note that we drew something
					mDirtyArea.drewRect();
				}
				else
				{
					// Failed to draw this bit.
					mDirtyArea.addDirtyRect(mapTileSize, tile);
					
					if(quota == null || quota.isExceeded())
					{
						needRedraw = true;
						// Still continue because we can draw tiles from the GL cache
					}
				}
			}
			// Draw in a spiral manner.
			tile.y += offy;
			tile.x += offx;
			tilesDrawn++;
			if(tilesDrawn == tilesNeeded)
			{
				// Gone sufficient tiles in this direction.
				tilesDrawn = 0;
				int tmp = offx;
				offx = offy;
				offy = -tmp;				
				line++;
				// Every 2nd line, increase the line length
				if(line == 2)
				{
					line = 0;
					tilesNeeded++;
				}
			}
		}
		return needRedraw;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		if (BuildConfig.DEBUG)
		{
			Utils.logGLInfo();
		}

		mGLTileCache.resetForSurfaceCreated();
		mGLImageCache.resetForSurfaceCreated();

		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);

		// Blending is enabled for markers and disabled for tiles, but the blend function is always the same.
		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		// Grey "background colour", like the iOS version of this SDK.
		//glClearColor(0.5f, 0.5f, 0.5f, 1);
		// Requested change to white...
		glClearColor(1, 1, 1, 1);


		shaderProgram = new ShaderProgram();
		shaderOverlayProgram = new ShaderOverlayProgram();
		shaderCircleProgram = new ShaderCircleProgram();

		glReleaseShaderCompiler();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		if (BuildConfig.DEBUG) {
			Log.v(TAG, "Viewport " + width + "*" + height);
		}

		mGLViewportWidth = width;
		mGLViewportHeight = height;
		glViewport(0, 0, width, height);

		// The nominal order is "near,far", but somehow we need to list them backwards.
		Matrix.orthoM(mMVPOrthoMatrix, 0, 0, width, height, 0, 1, -1);

		mScrollController.setWidthHeight(width, height);
	}
	
	private static class FetchQuota {
		// Always perform at least 4 async fetches or 1 sync fetch, even after the render time limit is exceeded.
		public int remainingAsyncFetches;
		public int remainingSyncFetches;
		private long limitUptimeMillis;
		private long hardLimitUptimeMillis;
		private boolean noAsyncFetches;

		public void reset(long now) {
			// Allow 10mS per frame for loading tiles to prevent scroll judder. We could similarly throttle annotations....
			remainingAsyncFetches = 4;
			remainingSyncFetches = 1;
			limitUptimeMillis = now + 10;
			hardLimitUptimeMillis = now + 200;
			noAsyncFetches = false;
		}
		
		public void setNoAsyncFetches() {
			noAsyncFetches = true;
		}

		public boolean canAsyncFetch() {
			return !noAsyncFetches;
		}
		
		public void fetchSuccess()
		{
			remainingSyncFetches--;
		}
		
		public void fetchFailure()
		{
			if(!noAsyncFetches)
			{
				remainingAsyncFetches--;
			}
		}
		
		public boolean isExceeded()
		{
			long t = SystemClock.uptimeMillis();
			if(t > limitUptimeMillis)
			{
				if(remainingSyncFetches <= 0)
				{
					return true;
				}
				if(t > hardLimitUptimeMillis)
				{
					return true;
				}
			}
			return false;			
		}
	}

	public ScreenProjection getProjection() {
		// TODO: Is this allowed to return null in the Google Maps v2 API?
		return mVolatileProjection;
	}

	@Override
	public void setTileSources(Collection<OSTileSource> tileSources) {
		mTileFetcher.setTileSources(tileSources);
	}

	
	@Override
	public OSTileSource webTileSource(String apiKey, boolean openSpacePro, String[] productsOrNull)
	{
        String apiKeyPackageName = getContext().getPackageName();

		// The WMTS implementation is incorrect; always use the old WMS source for now
        return new WMSTileSource(apiKey, apiKeyPackageName, openSpacePro, productsOrNull);

	}

	@Override
	public OSTileSource localTileSource(Context context, File file) throws FailedToLoadException {
		return DBTileSource.openFile(context, file);
	}

	@Override
	public Collection<OSTileSource> localTileSourcesInDirectory(Context context, File dir) {
		ArrayList<OSTileSource> ret = new ArrayList<OSTileSource>();
		File[] files = dir.listFiles();
		if (files == null)
		{
			return ret;
		}
		for (File f : files) {
			if (!f.getName().endsWith(".ostiles")) {
				continue;
			}
			try {
				ret.add(DBTileSource.openFile(context, f));
			} catch (FailedToLoadException e) {
				Log.v(TAG, "Failed to load " + f.getPath(), e);
			}
		}
		return ret;
	}
	/*
	private int leaks = 0;
	private void leakGPUMemory() {
		long tileSizeBytes = TILE_SIZE_PIXELS * TILE_SIZE_PIXELS * 4;
		long estGPUUsage = tileSizeBytes*NUM_TEXTURES;
		float MB = 1048576.0f;

		Utils.generateTexture();
		GLUtils.texImage2D(GL_TEXTURE_2D, 0, mBitmaps[1], 0);
		leaks++;

		Log.v(TAG, String.format(Locale.ENGLISH, "Estimated RAM: %g + %g GPU + %g GPU leaked", estGPUUsage/MB, estGPUUsage/MB, (leaks*tileSizeBytes)/MB));
	}
*/
}
