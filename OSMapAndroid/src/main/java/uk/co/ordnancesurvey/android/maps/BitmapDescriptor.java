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

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

/**
 * Defines an image. For a marker, it can be used to set the image of the marker icon. To obtain a BitmapDescriptor use the factory 
 * class {@link BitmapDescriptorFactory}.
 */
public final class BitmapDescriptor {
	private static final String TAG = "BitmapDescriptor";
	private final Type mType;

	private final Bitmap mBitmap;
	final float mTintR;
	final float mTintG;
	final float mTintB;

	private final String mPathString;
	private final int mResourceId;

	BitmapDescriptor(Bitmap bitmap)
	{
		this(bitmap, -1, -1, -1);
	}

	BitmapDescriptor(Bitmap bitmap, float tintR, float tintG, float tintB)
	{
		mType = Type.BITMAP;
		mTintR = tintR;
		mTintG = tintG;
		mTintB = tintB;
		mBitmap = bitmap;
		mPathString = null;
		mResourceId = 0;
	}

	BitmapDescriptor(String pathString, Type type) {
		mType = type;

		mTintR = mTintG = mTintB = -1;
		mBitmap = null;
		mPathString = pathString;
		mResourceId = 0;
	}

	BitmapDescriptor(int resourceId) {
		mType = Type.RESOURCE_ID;

		mTintR = mTintG = mTintB = -1;
		mBitmap = null;
		mPathString = null;
		mResourceId = resourceId;
	}

	Bitmap loadBitmap(Context context)
	{
		InputStream is = null;
		try {
			switch (mType) {
			case BITMAP:
				if (mBitmap == null)
				{
					return Images.getOpenspaceMarker(context.getResources().getDisplayMetrics());
				}
				return mBitmap;
			case PATH_ABSOLUTE:
				return BitmapFactory.decodeFile(mPathString);
			case PATH_ASSET:
				is = context.getAssets().open(mPathString);
				return BitmapFactory.decodeStream(is);
			case PATH_FILEINPUT:
				is = context.openFileInput(mPathString);
				return BitmapFactory.decodeStream(is);
			case RESOURCE_ID:
				return ((BitmapDrawable)context.getResources().getDrawable(mResourceId)).getBitmap();
			}
			assert false;
			return null;
		} catch(IOException ex) {
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.i(TAG, "Failed to load bitmap", e);
				}
			}
		}
	}

	enum Type {
		BITMAP,
		PATH_ABSOLUTE,
		PATH_ASSET,
		PATH_FILEINPUT,
		RESOURCE_ID,
	}
}
