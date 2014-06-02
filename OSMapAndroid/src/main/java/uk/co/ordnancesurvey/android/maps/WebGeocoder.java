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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

final class WebGeocoder {

    private final static String TAG = WebGeocoder.class.getSimpleName();

	private final String mAPIKey;
    private final String mAppPackageName;
	private final boolean mOpenSpacePro;

	public WebGeocoder(String apiKey, String appPackageName, boolean openSpacePro)
	{
		if (apiKey == null) {
			throw new IllegalArgumentException("WebGeocoder(): Must provide apiKey");
		}

		mAPIKey = apiKey;
        mAppPackageName = appPackageName;
		mOpenSpacePro = openSpacePro;
	}

	private URL URLForQuery(String query, Geocoder.GeocodeType type)
	{
		if(query == null || query.length() == 0)
		{
			return null;
		}
		
		String typeStr;
		switch(type)
		{
			case OnlineGazetteer:
				typeStr = "gazetteer";
				break;
				
			case OnlinePostcode:
				typeStr = "postcode";
				break;
				
			default:
				throw new AssertionError();
		}

		try {
			String proString = mOpenSpacePro ? "osopenspacepro" : "openspace";
			String urlString = "https://" + proString + ".ordnancesurvey.co.uk/osmapapi/"
					+ Uri.encode(typeStr)
					+ "?key=" + Uri.encode(mAPIKey)
					+ "&appId=" + Uri.encode(mAppPackageName)
					+"&f=json&q=" + Uri.encode(query);

            //Log.v(TAG, urlString);

			return new URL(urlString);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	

	private static GridPoint GridPointForJSONObject(JSONObject location)
	{
		try {
			JSONObject gmlPoint = location.getJSONObject("gml:Point");
			String gmlPos = gmlPoint.getString("gml:pos");
			if(gmlPos.length() > 0
					&& !gmlPos.equals("null null")
					&& gmlPoint.getString("srsName").equals("EPSG:27700")
					&& gmlPoint.getString("srsDimension").equals("2"))
			{
				int r = gmlPos.indexOf(" ");
				if(r > 0)
				{
					String eastingStr = gmlPos.substring(0, r);
					String northingStr = gmlPos.substring(r+1);
					if(eastingStr.length() > 0 && northingStr.length() > 0)
					{
						return new GridPoint(Double.valueOf(eastingStr), Double.valueOf(northingStr));
					}
				}
			}
		} catch(JSONException e) {
		}
		return null;
	}

	private List<Placemark> processData(String searchString, String data) throws JSONException
	{
		ArrayList<Placemark> placemarks = new ArrayList<Placemark>(10);
		if(data.length() >= 3 && data.charAt(0) == '(')
		{
			// The postcode results are wrapped in "callbackname(...);" even when CALLBACK is the empty string or unset.
			data = data.substring(1, data.length() - 3);
		}

		JSONObject result = new JSONObject(data);

		//({"PostcodeResult":{"location":{"gml:Point":{"srsName":"EPSG:27700","gml:pos":"544652 258405","srsDimension":"2"}}}});
		JSONObject postcodeResult = result.optJSONObject("PostcodeResult");
		if (postcodeResult != null)
		{
			JSONObject postcodeLocation = postcodeResult.getJSONObject("location");
			GridPoint postcodeGridPoint = GridPointForJSONObject(postcodeLocation);
			if(postcodeGridPoint != null)
			{
				placemarks.add(new Placemark(searchString.toUpperCase(Locale.ENGLISH), null, null, postcodeGridPoint));
			}
		}

		//{"GazetteerResult": {"items": {"Item": [{"county": "Lancashire", "type": "FARM", "location": {"gml:Point": { "srsName": "EPSG:27700", "srsDimension": "2",  "gml:pos": "354500 451500" }}, "name": "Isle of Skye Fm"},...]}, ... }}
		JSONObject gazetteerResult  = result.optJSONObject("GazetteerResult");
		if (gazetteerResult != null)
		{
			JSONObject gazetteerItems = gazetteerResult.optJSONObject("items");
			JSONArray gazetteerItemItems = gazetteerItems.optJSONArray("Item");
			if(gazetteerItemItems == null)
			{
				gazetteerItemItems = new JSONArray();
				gazetteerItemItems.put(gazetteerItems.getJSONObject("Item"));
			}
			for(int j = 0; j < gazetteerItemItems.length(); j++)
			{
				JSONObject itemDict = gazetteerItemItems.getJSONObject(j);
				JSONObject itemLocation = itemDict.optJSONObject("location");
				GridPoint gp = GridPointForJSONObject(itemLocation);
				if(gp == null)
				{
					continue;
				}
				String county = itemDict.optString("county");
				String name = itemDict.optString("name");
				String type = itemDict.optString("type");

				// type is e.g. "FARM", "CITY", "TOWN", "OTHER", "UNKNOWN", but where's the complete list?
				placemarks.add(new Placemark(name, type, county, gp));

			}
		}

		// Replace the empty array with nil.
		if(placemarks.size() != 0)
		{
			return placemarks;
		}
		return null;
	}

	public List<? extends Placemark> geocodeString(String s, Geocoder.GeocodeType geocodeType, GridRect boundingRect, int start, int numResults) throws Geocoder.GeocodeException
	{
		switch (geocodeType)
		{
		case OnlineGazetteer:
		case OnlinePostcode:
			break;
		default:
			throw new IllegalArgumentException();
		}

		URL url = URLForQuery(s, geocodeType);
		HttpURLConnection connection = null;
		try {
			// This is supposed to do no network I/O - but can still throw IOException
			connection = (HttpURLConnection)url.openConnection();
			InputStream is = connection.getInputStream();
			int statusCode = connection.getResponseCode();
			if (statusCode != 200)
			{
				throw new Geocoder.GeocodeNetworkException("Expected HTTP status 200, got " + statusCode);
			}
			// The example in the docs do not appear to close the stream:
			//   http://developer.android.com/reference/java/net/HttpURLConnection.html
			String data = new String(Helpers.readAllNoClose(is), "UTF-8");
			return processData(s, data);
		} catch(IOException e) {
			// Possible failures:
			//   openConnection()
			//   getInputStream()
			//   getResponseCode()
			//   readAllNoClose()
			throw new Geocoder.GeocodeNetworkException(e);
		} catch (JSONException e) {
			throw new Geocoder.GeocodeNetworkException(e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

	}
}
