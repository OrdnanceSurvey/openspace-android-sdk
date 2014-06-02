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

import android.graphics.Bitmap;

/**
 * Used to create a definition of an image, used for marker icons.
 */
public final class BitmapDescriptorFactory {
	// Hue as in HSV .
	/** Constant value:  210.0 */
	public final static float HUE_AZURE = 210.0f;	
	/** Constant value:  240.0 */
	public final static float HUE_BLUE = 240.0f;
	/** Constant value:  180.0 */
	public final static float HUE_CYAN = 180.0f;
	/** Constant value:  120.0 */
	public final static float HUE_GREEN = 120.0f;
	/** Constant value:  300.0 */
	public final static float HUE_MAGENTA = 300.0f;
	/** Constant value:  30.0 */
	public final static float HUE_ORANGE = 30.0f;
	/** Constant value:  0.0 */
	public final static float HUE_RED = 0.0f;
	/** Constant value:  330.0 */
	public final static float HUE_ROSE = 330.0f;
	/** Constant value:  270.0 */
	public final static float HUE_VIOLET = 270.0f;
	/** Constant value:  60.0 */
	public final static float HUE_YELLOW = 60.0f;

	private BitmapDescriptorFactory() { }

	private static final BitmapDescriptor DEFAULT_MARKER = new BitmapDescriptor(null);
	/** Creates a bitmap descriptor that refers to the default marker image. */
	public static BitmapDescriptor defaultMarker()
	{
		return DEFAULT_MARKER;
	}

	/**
	 * Creates a bitmap descriptor that refers to a colorization of the default marker image. 
	 * For convenience, there is a predefined set of hue values. See example {@link #HUE_YELLOW}.
	 * @param hue The hue of the marker which must be in the range 0 <= hue < 360
	 * @return a bitmap descriptor
	 */
	public static BitmapDescriptor defaultMarker(float hue)
	{
		float[] rgb = new float[3];
		hsv2rgb(rgb,hue,1,1);
		BitmapDescriptor bmd = new BitmapDescriptor(null, rgb[0], rgb[1], rgb[2]);
		return bmd;
	}
	
	/**
	 * Creates a {@link BitmapDescriptor} using the name of an image in the assets directory.
	 * @param assetName	The name of an image in the assets directory.
	 * @return the {@link BitmapDescriptor} that was loaded from the asset or null if failed to load.
	 */
	public static BitmapDescriptor fromAsset(String assetName)
	{
		return new BitmapDescriptor(assetName, BitmapDescriptor.Type.PATH_ASSET);
	}
	
	/**
	 * Creates a bitmap descriptor from a given image.
	 */
	public static BitmapDescriptor fromBitmap(Bitmap image)
	{
		BitmapDescriptor bmd = new BitmapDescriptor(image);
		return bmd;
	}

	/**
	 * Creates a {@link BitmapDescriptor} using the name of an image file located in the internal storage. 
	 * In particular, this calls {@link android.content.Context#openFileInput(String)}.
	 * @param fileName	The name of the image file.
	 * @return the {@link BitmapDescriptor} that was loaded from the asset or null if failed to load.
	 */
	public static BitmapDescriptor fromFile(String fileName)
	{
		return new BitmapDescriptor(fileName, BitmapDescriptor.Type.PATH_FILEINPUT);
	}
	

	/**
	 * Creates a {@link BitmapDescriptor} from an absolute file path.
	 * @param path	The absolute path of the image.
	 * @return the {@link BitmapDescriptor} that was loaded from the absolute path or null if failed to load.
	 */
	public static BitmapDescriptor fromPath(String path)
	{
		return new BitmapDescriptor(path, BitmapDescriptor.Type.PATH_ABSOLUTE);
	}

	/**
	 * Creates a {@link BitmapDescriptor} using the resource id of an image.
	 * @param resourceId	The resource id of an image.
	 * @return the {@link BitmapDescriptor} that was loaded from the asset or null if failed to load.
	 */
	public static BitmapDescriptor fromResource(int resourceId)
	{
		return new BitmapDescriptor(resourceId);
	}
	
	private static void hsv2rgb(float[] rgb, float h, float s, float v)
	{
		h /= 60;
		h += 1;
		h %= 6;
		if (h < 0)
		{
			h += 6;
		}
		int primary = (int)Math.floor(h/2)%3;
		h -= primary*2+1;
		float c = (v*s);
		float x = c*Math.max(-h, 0.0f);
		float y = c*Math.max(h, 0.0f);
		float r = 0, g = 0, b = 0;
		switch (primary)
		{
		case 0:
			b = x;
			r = c;
			g = y;
			break;
		case 1:
			r = x;
			g = c;
			b = y;
			break;
		case 2:
			g = x;
			b = c;
			r = y;
			break;
		default:
			assert false;
		}
		rgb[0] = r;
		rgb[1] = g;
		rgb[2] = b;
	}}
