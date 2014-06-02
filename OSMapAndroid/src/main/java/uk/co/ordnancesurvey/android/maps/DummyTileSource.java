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

import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.annotation.TargetApi;
import android.content.Context;

final class DummyTileSource extends OSTileSource {
	Context mContext;
	boolean mSynchronous;
	public DummyTileSource(Context context, boolean synchronous)
	{
		super(null);
		mContext = context;
		mSynchronous = synchronous;
	}

	@Override
	boolean isNetwork()
	{
		return mSynchronous;
	}

	@Override
	boolean isSynchronous()
	{
		return mSynchronous;
	}

	@Override
	byte[] dataForTile(MapTile tile) {
		// TODO: Do we want to support this?
		assert false : "Not supported yet!";
		return null;
	};

	//@Override
	Bitmap bitmapForTile(final MapTile tile)
	{
		if(mSynchronous)
		{
			long timeNow = SystemClock.uptimeMillis();
			while(SystemClock.uptimeMillis() - timeNow < 10);
		}
		else
		{
			long timeNow = SystemClock.uptimeMillis();
			while(SystemClock.uptimeMillis() - timeNow < 50)
			{
				Thread.yield();
			}
		}

		
		Bitmap bmp;
		if(tile.y == 0 && tile.x == 0)
		{
			bmp = ((BitmapDrawable)mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon)).getBitmap();
		}
		else
		{
			bmp =  generateFilledBitmap(tile);
		}
		return bmp;
	}

	private Bitmap generateFilledBitmap(MapTile tile)
	{
		int seed = tile.y * 10 + tile.x;
		int tileSizePixels = tile.layer.tileSizePixels;

		Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		Bitmap bmp = Bitmap.createBitmap(tileSizePixels, tileSizePixels, conf);
		Canvas canvas = new Canvas(bmp);
		Random rand = new Random(seed);

		// Fill the canvas with a randomish colour.
		canvas.drawColor(0xFF000000|rand.nextInt());

		boolean pattern = true;
		if (pattern) {
			int c0 = 0xFF000000;
			int c1 = 0xFFFFFFFF;
			Bitmap patternBmp = Bitmap.createBitmap(new int[]{c0,c1,c1,c0}, 2, 2, conf);
			BitmapDrawable drawable = new BitmapDrawable(null, patternBmp);
			drawable.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
			drawable.setBounds(1, 1, tileSizePixels-1, tileSizePixels-1);
			drawable.draw(canvas);
		} else {
			// Fill it again with a lighter colour, leaving a 1px border.
			canvas.clipRect(1, 1, tileSizePixels-1, tileSizePixels-1);
			canvas.drawColor(0xFF404040|rand.nextInt());
		}


		// A hint to users of the bitmap.
		if (Build.VERSION.SDK_INT >= 12)
		{
			setHasAlphaAPI12(bmp, false);
		}
		return bmp;
	}

	@TargetApi(12)
	private static void setHasAlphaAPI12(Bitmap bmp, boolean hasAlpha) {
		bmp.setHasAlpha(hasAlpha);
	}
}
