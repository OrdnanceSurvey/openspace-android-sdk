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
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.support.v4.util.LruCache;
import android.util.Log;

abstract class CombinedLruCache<K> {
	private static final String TAG = "TileCache";
	private static final int BYTES_PER_MB = 1024*1024;

	private final TileMemoryCache<K> mMemoryCache;
	private final DiskLruCache mDiskCache;
	private final ThreadPoolExecutor mAsyncExecutor;

	public CombinedLruCache(int memoryMB, int diskMB, File dir, int appVersion) {
		if (memoryMB > 0)
		{
			if (memoryMB > Integer.MAX_VALUE/BYTES_PER_MB)
			{
				Log.w(TAG, "Reducing requested memory cache of " + memoryMB + " MB to max of " + Integer.MAX_VALUE/BYTES_PER_MB);
				assert !BuildConfig.DEBUG || false : "Too big!";
				memoryMB = Integer.MAX_VALUE/BYTES_PER_MB;
			}
			mMemoryCache = new TileMemoryCache<K>(memoryMB*BYTES_PER_MB);
		} else {
			mMemoryCache = null;
		}

		mDiskCache = (diskMB > 0) ? openDiskCacheOrNull(dir, appVersion, 1, diskMB*(long)BYTES_PER_MB) : null;
		mAsyncExecutor = (mDiskCache != null) ? new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()) : null;
	}

	public byte[] get(K key) {
		if (mMemoryCache != null) {
			byte[] ret = mMemoryCache.get(key);
			if (ret != null) {
				return ret;
			}
		}

		if (mDiskCache != null) {
			try {
				DiskLruCache.Snapshot snapshot = mDiskCache.get(stringForKey(key));
				if (snapshot != null) {
					byte[] ret = snapshot.getBytes(0);
					if (ret != null) {
						return ret;
					}
				}
			} catch (IOException e) {
				Log.w(TAG, "Failed to read cache", e);
			}
		}

		return null;
	}

	public void putAsync(K key, final byte[] value) {
		if (mMemoryCache != null) {
			mMemoryCache.put(key, value);
		}

		if (mDiskCache != null) {
			final String stringKey = stringForKey(key);
			final DiskLruCache cache = mDiskCache;

			mAsyncExecutor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						DiskLruCache.Editor editor = cache.edit(stringKey);
						if (editor != null) {
							editor.set(0, value);
							editor.commit();
						}
					} catch (IOException e) {
						Log.e(TAG, "Failed to write cache entry", e);
					}
				}
			});
		}
	}

	abstract String stringForKey(K key);

	static DiskLruCache openDiskCacheOrNull(File directory, int appVersion, int valueCount, long maxSize) {
		try {
			return DiskLruCache.open(directory, appVersion, valueCount, maxSize);
		} catch (IOException e) {
			Log.w(TAG, "Failed to open cache directory " + directory.getPath(), e);
			return null;
		}

	}
	static final class TileMemoryCache<K> extends LruCache<K, byte[]>{
		public TileMemoryCache(int maxSize) {
			super(maxSize);
		}

		@Override
		protected int sizeOf(K key, byte[] value) {
			return value.length;
		}
	}
}