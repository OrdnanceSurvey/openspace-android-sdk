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

import android.net.Uri;
import android.util.Log;

/**
 * Do not use this class; this only returns URIs for requests using the ZoomMap TileMatrixSet.
 *
 */
final class WMTSTileSource extends WebTileSource {

	private final static String TAG = WMTSTileSource.class.getSimpleName();

	private final String mApiKey;
	private final String mApiKeyPackageName;

	
	public WMTSTileSource(String apiKey, String apiKeyPackageName, String[] productsOrNull) {
		super(productsOrNull);
		mApiKey = apiKey;
		mApiKeyPackageName = apiKeyPackageName;
	}

	@Override
	String uriStringForTile(MapTile tile) {
		MapLayer layer = tile.layer;
		String productCode = layer.productCode;

		if(!isProductSupported(productCode))
		{
			return null;
		}

        /**
         * NB. this only works for ZoomMap TileMatrixSet
         */
		if (productCode.length() == 4 && productCode.startsWith("CS"))
		{
			// TODO: Magic number
			int mapHeight = Math.round(1344000/layer.tileSizeMetres);
			String wmtsCode = productCode.substring(2);
			
			int tileRow = mapHeight-1-tile.y;
			int tileCol = tile.x;

			// Use Uri.encode() instead of URLEncoder.encode():
			//   - It works for path elements (not just query keys/values).
			//   - It doesn't make us catch UnsupportedEncodingException.
			// TODO: handle non-pro API keys.
			String uriString = "https://osopenspacepro.ordnancesurvey.co.uk/osmapapi/wmts/" +
					Uri.encode(mApiKey) +
					"/ts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&LAYER=osgb&STYLE=default&FORMAT=image/png&TILEMATRIXSET=ZoomMap" +
					"&TILEMATRIX=" + Uri.encode(wmtsCode) +
					"&TILEROW=" +tileRow +
					"&TILECOL=" +tileCol +
					"&appId=" + Uri.encode(mApiKeyPackageName);

            //Log.v(TAG, uriString);

			return uriString;
		}

		return null;
	}
}
