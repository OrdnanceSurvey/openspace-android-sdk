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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.WeakHashMap;

import android.graphics.Bitmap;
import android.util.Log;

final class GLImageCache {
	private final static String TAG = "GLTileCache";

	private WeakHashMap<Bitmap, ImageTexture> mImages = new WeakHashMap<Bitmap, ImageTexture>();
	private HashSet<ImageTexture> mLoadedTextures = new HashSet<ImageTexture>();
	private ReferenceQueue<Bitmap> mReapableQueue = new ReferenceQueue<Bitmap>();

	public void resetForSurfaceCreated() {
		mLoadedTextures.clear();
		mImages.clear();
	}

	public ImageTexture bindTextureForBitmap(Bitmap bmp) {
		ImageTexture tex = mImages.get(bmp);
		if (tex != null)
		{
			glBindTexture(GL_TEXTURE_2D, tex.textureId);
			return tex;
		}

		if (BuildConfig.DEBUG) {
			Log.v(TAG, "Uploading texture for uncached bitmap");
		}

		// We're about to do something vaguely expensive, so we might as well clear unused textures.
		for (ImageTexture reapable; null != (reapable = (ImageTexture)mReapableQueue.poll()); ) {
			// Only remove textures which we think are "loaded".
			// This stops us from deleting textures that were created before the most recent resetForSurfaceCreated().
			if (mLoadedTextures.remove(reapable)) {
				Utils.deleteTexture(reapable.textureId);
			}
		}

		// TODO: This can leak a texture in the face of exceptions.
		tex = new ImageTexture(bmp, mReapableQueue);
		mLoadedTextures.add(tex);
		mImages.put(bmp, tex);

		return tex;
	}

	/**
	* A loaded texture. The textureId is only valid in the GL context in which it was originally loaded.
	* It becomes invalid when the GL context is lost.
	* The texture may also be deleted on any future call to bindTextureForBitmap() if the bitmap becomes unreachable.
	*/
	final static class ImageTexture extends WeakReference<Bitmap> {
		final int textureId;
		final int width;
		final int height;
		final FloatBuffer vertexCoords;
		public ImageTexture(Bitmap bmp, ReferenceQueue<Bitmap> queue) {
			super(bmp, queue);
			// TODO: Is this really the right place?
			textureId = Utils.generateTexture(bmp);
			int w = bmp.getWidth();
			int h = bmp.getHeight();
			width = w;
			height = h;
			vertexCoords = Utils.directFloatBuffer(new float[]{
				0,h,
				w,h,
				0,0,
				w,0,
			});
		}
	};
}
