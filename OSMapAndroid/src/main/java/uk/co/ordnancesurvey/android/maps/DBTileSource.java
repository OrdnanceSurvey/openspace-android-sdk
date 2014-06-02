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
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

final class DBTileSource extends OSTileSource {
	//private final static String TAG = "DBTileSource";

	static final class ZoomLevel
	{
		final int internalZoomLevel;
		final String product_code;
		final int bbox_x0;
		final int bbox_x1;
		final int bbox_y0;
		final int bbox_y1;

		ZoomLevel(int internalZoomLevel, String product_code, int bbox_x0, int bbox_x1, int bbox_y0, int bbox_y1) {
			this.internalZoomLevel = internalZoomLevel;
			this.product_code = product_code;
			this.bbox_x0 = bbox_x0;
			this.bbox_x1 = bbox_x1;
			this.bbox_y0 = bbox_y0;
			this.bbox_y1 = bbox_y1;
		}

		boolean containsTile(int x, int y) {
			return (bbox_x0 <= x && x < bbox_x1 && bbox_y0 <= y && y < bbox_y1);
		}
	};

	
	private final SQLiteDatabase mDB;
	private final ZoomLevel[] mZoomLevels;
	
	private DBTileSource(Context context, String path) throws SQLiteException {
		super(null);
		mDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		
		Cursor cursor = mDB.rawQuery("select * from zoom_levels", null);
		try {
			int bbox_x0 = cursor.getColumnIndexOrThrow("bbox_x0");
			int bbox_x1= cursor.getColumnIndexOrThrow("bbox_x1");
			int bbox_y0 = cursor.getColumnIndexOrThrow("bbox_y0");
			int bbox_y1 = cursor.getColumnIndexOrThrow("bbox_y1");
			int product_code = cursor.getColumnIndexOrThrow("product_code");
			int zoom_level = cursor.getColumnIndexOrThrow("zoom_level");
			ArrayList<ZoomLevel> levels = new ArrayList<ZoomLevel>();
			while(cursor.moveToNext())
			{
				ZoomLevel zl = new ZoomLevel(
						cursor.getInt(zoom_level),
						cursor.getString(product_code),
						cursor.getInt(bbox_x0),
						cursor.getInt(bbox_x1),
						cursor.getInt(bbox_y0),
						cursor.getInt(bbox_y1));

				levels.add(zl);
			}
			mZoomLevels = levels.toArray(new ZoomLevel[0]);
		} finally {
			cursor.close();
		}
	}

	public static DBTileSource openFile(Context context, File db) throws FailedToLoadException {
		if (!db.exists()) {
			// Reduce logspam: Don't try to open a database doesn't exist.
			// Sqlite unconditionally logs about 30 lines at ERROR severity.
			throw new FailedToLoadException("File not found: " + db.getPath());
		}
		try {
			return new DBTileSource(context, db.getPath());
		} catch (SQLiteException e) {
			throw new FailedToLoadException(e);
		}
	}

	public static DBTileSource openFromExternalStorage(Context context, String name) throws FailedToLoadException {
		// TODO: use Context.getExternalFilesDir() instead?
		// TODO: Do we actually want to support loading from the SD card? What about DRM?
		File sdcard = Environment.getExternalStorageDirectory();
		File db = new File(sdcard, name);
		return openFile(context, db);
	}

	private ZoomLevel zoomLevelForLayer(MapLayer layer) {
		String productCode = layer.productCode;
		for(ZoomLevel zl : mZoomLevels)
		{
			if(zl.product_code.equals(productCode))
			{
				return zl;
			}
		}
		return null;
	}

	@Override
	byte[] dataForTile(MapTile tile) {
		MapLayer layer = tile.layer;

		ZoomLevel zl = zoomLevelForLayer(layer);
		if(zl == null || !zl.containsTile(tile.x, tile.y))
		{
			return null;
		}

		Cursor cursor = null;

        try {
            cursor = mDB.rawQuery(
                "SELECT tile_data FROM tiles WHERE tile_row = ? and tile_column = ? and zoom_level = ?",
                new String[]{String.valueOf(tile.y),  String.valueOf(tile.x), String.valueOf(zl.internalZoomLevel)});

            if(cursor.moveToFirst())
            {
                return cursor.getBlob(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }


		return null;
	}

	@Override
	boolean isNetwork() {
		return false;
	}

	@Override
	boolean isSynchronous() {
		return true;
	}

    @Override
    public void close() throws IOException {
        if (mDB == null) {
            return;
        }

        mDB.close();
    }
}
