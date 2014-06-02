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

final class WMSTileSource extends WebTileSource {


	private final static String TAG = WMSTileSource.class.getSimpleName();

	private final String mApiKey;
	private final String mApiKeyPackageName;
	private final boolean mIsPro;

    public WMSTileSource(String apiKey, String apiKeyPackageName, boolean isPro, String[] productsOrNull) {
        super(productsOrNull);
        mApiKey = apiKey;
        mApiKeyPackageName = apiKeyPackageName;
        mIsPro = isPro;
    }

	@Override
	String uriStringForTile(MapTile tile) {

		MapLayer layer = tile.layer;

		if(!isProductSupported(layer.productCode))
		{
			return null;
		}

		// This calculates the actual bbox.
		float bboxX0 = layer.tileSizeMetres *tile.x;
		float bboxY0 = layer.tileSizeMetres *tile.y;
		float bboxX1 = bboxX0 + layer.tileSizeMetres;
		float bboxY1 = bboxY0 + layer.tileSizeMetres;

		String uriString = "https://" + (mIsPro ? "osopenspacepro" : "openspace") + ".ordnancesurvey.co.uk/osmapapi/ts" +
				"?FORMAT=image/png" +
				"&SERVICE=WMS" +
				"&VERSION=1.1.1" +
				"&EXCEPTIONS=application/vnd.ogc.se_inimage" +
				"&SRS=EPSG:27700" +
				"&STYLES=" +
				"&REQUEST=GetMap" +
				"&KEY=" + Uri.encode(mApiKey) +
				"&appId=" +  Uri.encode(mApiKeyPackageName) +
				"&WIDTH=" + layer.tileSizePixels + 
				"&HEIGHT="+ layer.tileSizePixels +
				"&BBOX=" + bboxX0 + "," + bboxY0 + "," + bboxX1 + "," + bboxY1 +
				"&LAYERS=" + Uri.encode(layer.layerCode) +
				"&PRODUCT=" + Uri.encode(layer.productCode);

        //Log.v(TAG, uriString);

		return uriString;
	}
}
