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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

final class MapLayer {
	final String productCode;
	final String layerCode;
	final int tileSizePixels;
	final float tileSizeMetres;
	final float metresPerPixel;

	private MapLayer(String productCode, String layerCode, int tileSizePixels, float tileSizeMetres, String retinaProductCode, String forceFallbackCode) {
		this.productCode = productCode;
		this.layerCode = layerCode;
		this.tileSizePixels = tileSizePixels;
		this.tileSizeMetres = tileSizeMetres;
		this.metresPerPixel = tileSizeMetres/tileSizePixels;
	}

	private MapLayer(String productCode, String layerCode, int tileSizePixels, float tileSizeMetres, String retinaProductCode) {
		this(productCode, layerCode, tileSizePixels, tileSizeMetres, retinaProductCode, null);
	}

	private MapLayer(String productCode, String layerCode, int tileSizePixels, float tileSizeMetres) {
		this(productCode, layerCode, tileSizePixels, tileSizeMetres, null, null);
	}

	static MapLayer[] layersForProductCodes(String[] productCodes) {
		Arrays.sort(productCodes);
		ArrayList<MapLayer> list = new ArrayList<MapLayer>();
		for (MapLayer layer : ALL_LAYERS) {
			boolean isContained = (Arrays.binarySearch(productCodes, layer.productCode) >= 0);
			if (isContained)
			{
				list.add(layer);
			}
		}
		MapLayer[] ret = list.toArray(new MapLayer[0]);
		Arrays.sort(ret, COMPARE_METRES_PER_PIXEL);
		return ret;
	}

	static MapLayer[] getDefaultLayers() {
		return layersForProductCodes(new String[] {"SV","SVR","50K","50KR","250K","250KR","MS","MSR","OV2","OV1","OV0"});
	}

	private static MapLayer[] ALL_LAYERS = new MapLayer[] {
		new MapLayer("SV",       "1", 250,    250), // Street view
		new MapLayer("SVR",      "2", 250,    500, "SV"), // Street view
		new MapLayer("VMD",    "2.5", 200,    500), // Vector Map District
		new MapLayer("VMDR",     "4", 250,   1000), // Vector Map District
		new MapLayer("50K",      "5", 200,   1000), // 1:50k
		new MapLayer("50KR",    "10", 200,   2000, "50K"), // 1:50k
		new MapLayer("250K",    "25", 200,   5000), // 1:250k
		new MapLayer("250KR",   "50", 200,  10000, "250K"), // 1:250k
		new MapLayer("MS",     "100", 200,  20000), // 1:1M
		new MapLayer("MSR",    "200", 200,  40000, "MS"), // 1:1M
		new MapLayer("OV2",    "500", 200, 100000), // Overview 2
		new MapLayer("OV1",   "1000", 200, 200000, "OV2"), // Overview 1
		new MapLayer("OV0",   "2500", 200, 500000), // Overview 0

		// Pro products
		new MapLayer("VML",      "1", 250,    250), // Vector Map Local
		new MapLayer("VMLR",     "2", 250,    500, "VML"), // Vector Map Local
		new MapLayer("25K",    "2.5", 200,    500), // 1:25k
		new MapLayer("25KR",     "4", 250,   1000), // 1:25k

		// Zoom products
		new MapLayer("CS00",   "896", 250, 224000),
		new MapLayer("CS01",   "448", 250, 112000),
		new MapLayer("CS02",   "224", 250,  56000),
		new MapLayer("CS03",   "112", 250,  28000),
		new MapLayer("CS04",    "56", 250,  14000),
		new MapLayer("CS05",    "28", 250,   7000),
		new MapLayer("CS06",    "14", 250,   3500),
		new MapLayer("CS07",     "7", 250,   1750),
		new MapLayer("CS08",   "3.5", 250,    875),
		new MapLayer("CS09",  "1.75", 250,  437.5f),
		new MapLayer("CS10", "0.875", 250, 218.75f),

		// Undocumented projects extracted from JavaScript:
		//   resolutionLookup = '''{"SV": [1.0, 250],"SVR": [2.0, 250],"50K": [5.0, 200],"50KR": [10.0, 200],"250K": [25.0, 200],"250KR": [50.0, 200],"MS": [100.0, 200],"MSR": [200.0, 200],"OV2": [500.0, 200],"OV1": [1000.0, 200],"OV0": [2500.0, 200],"VMD": [2.5, 200],"VMDR": [4.0, 250],"25K": [2.5, 200],"25KR": [4.0, 250],"VML": [1.0, 250],"VMLR": [2.0, 250],"10KBW": [1.0, 250],"10KBWR": [2.0, 250],"10KR": [2.0, 250],"10K": [1.0, 250],"CS00": [896.0, 250],"CS01": [448.0, 250],"CS02": [224.0, 250],"CS03": [112.0, 250],"CS04": [56.0, 250],"CS05": [28.0, 250],"CS06": [14.0, 250],"CS07": [7.0, 250],"CS08": [3.5, 250],"CS09": [1.75, 250],"CS10": [0.875, 250],"CSG06": [14.0, 250],"CSG07": [7.0, 250],"CSG08": [3.5, 250],"CSG09": [1.75, 250]}'''
		//   for k,v in sorted(json.loads(resolutionLookup).iteritems()): print '{%-10s%5g, %3r},'%('"%s",'%k,v[0],v[1])
		new MapLayer("10K",       "1", 250,    250),
		new MapLayer("10KBW",     "1", 250,    250),
		new MapLayer("10KBWR",    "2", 250,    500),
		new MapLayer("10KR",      "2", 250,    500),
		new MapLayer("CSG06",    "14", 250,   3500),
		new MapLayer("CSG07",     "7", 250,   1750),
		new MapLayer("CSG08",   "3.5", 250,    875),
		new MapLayer("CSG09",  "1.75", 250,  437.5f),

		new MapLayer("25K-660DPI", null, 260,    250), // 1:25k
		new MapLayer("25K-330DPI", null, 260,    500, "25K-660DPI", "25K-660DPI"), // 1:25k
		new MapLayer("25K-165DPI", null, 260,   1000, "25K-330DPI", "25K-660DPI"), // 1:25k
		new MapLayer("50K-660DPI", null, 260,    500), // 1:50k
		new MapLayer("50K-330DPI", null, 260,   1000, "50K-660DPI", "50K-660DPI"), // 1:50k
		new MapLayer("50K-165DPI", null, 260,   2000, "50K-330DPI", "50K-660DPI"), // 1:50k
	};

	public final static Comparator<MapLayer> COMPARE_METRES_PER_PIXEL = new MetresPerPixelComparator();
	public final static Comparator<MapLayer> COMPARE_METRES_PER_PIXEL_REVERSED = Collections.reverseOrder(COMPARE_METRES_PER_PIXEL);

	final static class MetresPerPixelComparator implements Comparator<MapLayer> {
		@Override
		public int compare(MapLayer lhs, MapLayer rhs) {
			return Float.compare(lhs.metresPerPixel, rhs.metresPerPixel);
		}
	}
}
