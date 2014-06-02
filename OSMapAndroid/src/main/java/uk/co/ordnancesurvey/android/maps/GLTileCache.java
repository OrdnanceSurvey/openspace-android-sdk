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

import static android.opengl.GLES20.*;

import java.util.Locale;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

final class GLTileCache {
	private final static String TAG = "GLTileCache";

	private final LRUHashMap<MapTile, TileTexture> mTiles;
	private final int mMemorySoftLimit;
	private int mCurrentMemoryUsage;

	// Counter-based visibility check. Much faster than moving things between HashMaps!
	private int mCurrentVisibilityCount;

	// Some stats.
	private int statHitCount;
	private int statMissCount;
	private int statUploadCount;
	private int statAllocCount;
	private int statReuseCount;
	private int statFailedReuseCount;
	private long statLastPrinted;

	public GLTileCache(int memorySoftLimitBytes) {
		mMemorySoftLimit = memorySoftLimitBytes;

		float loadFactor = 0.75f;
		int estimatedBytesPerTile = 200*200*4;
		int initialCapacity = (int)((memorySoftLimitBytes/estimatedBytesPerTile +10)/loadFactor);
		mTiles = new LRUHashMap<MapTile, TileTexture>(initialCapacity, loadFactor);
	}

	/**
	* Call this from GLSurfaceView.Renderer.onSurfaceCreated().
	* This assumes that previous texture IDs have been invalidated.
	*/
	public void resetForSurfaceCreated() {
		mTiles.clear();
		mCurrentMemoryUsage = 0;
	}

	/**
	* Call this at the start of GLSurfaceView.Renderer.onDrawFrame() and then get textures to mark them "on-screen".
	*/
	public void resetTileVisibility() {
		mCurrentVisibilityCount++;

		// Log stats here, since it's a convenient method that's called once per frame.
		logStats();
	}

	/**
	* Get a cached texture, optionally allocating a new texture.
	* This results in a few extra gets when putting a new texture, but reduces the amount of code that needs to avoid leaking textures.
	*
	* This method does not update hit/miss stats, since the hit/miss rate here is an implementation detail.
	*
	* @param tile The tile to get.
	* @param allocate Whether to allocate or recycle a texture if the tile is not in the cache.
	* @return
	*/
	TileTexture getTextureForTile(MapTile tile, boolean allocate) {
		if (tile == null) {
			throw new NullPointerException("GLTileCache.getTextureForTile(MapTile,boolean) requires non-null tile");
		}

		// Try to grab a visible tile.
		TileTexture tex = mTiles.get(tile);
		if (tex != null)
		{
			// Mark it "visible".
			tex.lastVisibilityCount = mCurrentVisibilityCount;
			return tex;
		}

		// At this point, we decide whether to allocate/recycle a texture ID using this flag.
		// If true, the caller is expected to unconditionally fill it with the bitmap.
		if (!allocate) {
			return null;
		}

		// If we're using more than the "soft limit", try to pick a texture to recycle.
		if (mCurrentMemoryUsage > mMemorySoftLimit)
		{
			MapTile keyToRemove = mTiles.getProbableEldestKey();
			tex = mTiles.remove(keyToRemove);
			if (tex != null)
			{
				int lastVisibilityCount = tex.lastVisibilityCount;
				int currentVisibilityCount = mCurrentVisibilityCount;
				if (lastVisibilityCount == currentVisibilityCount || lastVisibilityCount == currentVisibilityCount-1) {
					// If it was marked visible this frame or last frame, put it back!
					mTiles.put(keyToRemove, tex);
					tex = null;
					statFailedReuseCount++;
				}
			}
		}

		// If there's nothing to recycle, try this instead.
		if (tex == null) {
			tex = new TileTexture();
			statAllocCount++;
		} else {
			statReuseCount++;
		}
		mTiles.put(new MapTile(tile), tex);

		return tex;
	}

	
	/**
	 * Fetches a tile from the cache.
	 * @param tile
	 * @return The texture ID of the cached tile, or 0.
	 */
	public int bindTextureForTile(MapTile tile) {
		TileTexture tex = getTextureForTile(tile, false);
		if (tex == null)
		{
			statMissCount++;
			return 0;
		}
		statHitCount++;

		int texId = tex.textureId;
		glBindTexture(GL_TEXTURE_2D, texId);
		return texId;
	}

	/**
	* Uploads a bitmap to a texture and adds it to the cache.
	* @param tile
	* @param bitmap
	* @return  The texture ID of the newly-uploaded texture.
	*/
	public int putTextureForTile(MapTile tile, Bitmap bitmap)
	{
		statUploadCount++;

		if (bitmap.isRecycled())
		{
			assert false;
			throw new IllegalArgumentException("Tried to texture-upload a recycled bitmap");
		}

		TileTexture tex = getTextureForTile(tile, true);
		int texId = tex.textureId;

		glBindTexture(GL_TEXTURE_2D, texId);

		boolean loaded = false;
		// Bitmap.getByteCount() requires API level 12. Use this instead.
		int newMemoryUsage = bitmap.getRowBytes() * bitmap.getHeight();

		/*
		if (false)
		{
			// Texture compression test. Very slow!
			ETC1Util.ETC1Texture etc1 = etc1 = Utils.compressBitmapETC1(bitmap);
			if (etc1 != null)
			{
				ETC1Util.loadTexture(GL_TEXTURE_2D, 0, 0, GL_RGB, GL_UNSIGNED_BYTE, etc1);
				loaded = true;
				newMemoryUsage = etc1.getData().capacity();
			}
		}
		*/

		if (!loaded)
		{
			Utils.texImage2DPremultiplied(bitmap);
		}

		int oldMemoryUsage = tex.memoryUsage;
		tex.memoryUsage = newMemoryUsage;
		mCurrentMemoryUsage += newMemoryUsage-oldMemoryUsage;
		return texId;
	}

	private void logStats() {
		if (BuildConfig.DEBUG)
		{
			long t = SystemClock.uptimeMillis();
			if (t - statLastPrinted < 10000)
			{
				return;
			}
			statLastPrinted = t;
			Log.v(TAG, String.format(Locale.ENGLISH, "%d hits, %d misses, %d uploads, %d textures, %d reused, %d reuse failures, %.3g/%.3g MB used", statHitCount, statMissCount, statUploadCount, statAllocCount, statReuseCount, statFailedReuseCount, mCurrentMemoryUsage/1048576.0f, mMemorySoftLimit/1048576.0f));
		}
	}

	final static class TileTexture {
		int textureId;
		int memoryUsage;
		int lastVisibilityCount;
		public TileTexture() {
			// TODO: Is this really the right place?
			textureId = Utils.generateTexture();
		}
	};
}
