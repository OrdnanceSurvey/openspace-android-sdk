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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import android.content.Context;
import android.util.Log;

/**
 Geocoder provides an interface to lookup positions based on place names, postcodes, grid references, roads, or any 
 combination thereof.
 
 It operates on either/both an offline database which is provided with the SDK or Ordnance Survey online services

 OSGeocoder is not thread-safe, and is not reentrant. It must only be called from a single thread, and only one geocoding 
 operation can be run at any one time. Clients should use cancelGeocode to cancel online searches, and not issue requests 
 while isGeocoding returns YES.
 
 
 The search always returns a (possibly empty) array of objects derived from OSPlacemark objects. Any matching roads are
 returned as OSRoad objects.
 
 A search of type OSGeocodeTypeOnlinePostcode or OSGeocodeTypeOnlineGazetteer will use the appropriate Ordnance Survey 
 online service to fetch results. 

 A search of type OSGeocodeTypeGazetteer will return matching place names. These will be ordered by the type of place; 
 Cities will be returned before Towns, and Towns before other features.
 
 A search of type OSGeocodeTypeGridReference will treat the search term as an OS Grid Reference. It will return up to one 
 result. A valid search term consists of two letters as per the [OS National Grid Square convention][https://www.ordnancesurvey.co.uk/oswebsite/gps/information/coordinatesystemsinfo/guidetonationalgrid/page9.html] 
 followed by 0,2,4,6,8 or 10 digits. Invalid search terms (not matching this format, or containing invalid letter 
 sequences) will return no results.
 
 A search of type OSGeocodeTypeRoad will return roads matching the search term. If none are found, then the search term 
 will be split into a road and a location.  If a comma is present in the search term, the split will be made there, 
 otherwise the split is carried out automatically. The search will then return roads near matching locations.
 
 OSGeocoder allows searches to be carried out with a range. The behaviour of this range varies.
 
 A search over multiple types (for example Gazetteer and Road) will search for entries of the specified types that
 match the search string. These results will be ordered by result type.
 
 The range field is honoured for searches of type OSGeocodeTypeGazetteer and OSGeocodeTypeRoad, but ignored for all 
 other types of search. A search combining multiple types will honour the range field where it can, but will apply the 
 range individually to each result set, so a combined search with a range of
        (NSRange){0,100} 
 may return 100 gazetteer entries AND 100 roads.
 */
public class Geocoder {

	private final static String TAG = Geocoder.class.getSimpleName();

	public static enum GeocodeType {
		Gazetteer,
		Postcode,
		GridReference,
		Road,
		OnlineGazetteer,
		OnlinePostcode,
		;

		// These are private because EnumSet is mutable! Always return a copy.
		private static final EnumSet<GeocodeType> ALL_OFFLINE = EnumSet.of(Gazetteer, Postcode, GridReference, Road);
		private static final EnumSet<GeocodeType> ALL_ONLINE = EnumSet.of(OnlineGazetteer, OnlinePostcode);
		public static EnumSet<GeocodeType> allOffline() {
			return EnumSet.copyOf(ALL_OFFLINE);
		}
		public static EnumSet<GeocodeType> allOnline() {
			return EnumSet.copyOf(ALL_ONLINE);
		}
	}

	@SuppressWarnings("serial")
	public abstract static class GeocodeException extends Exception {
		GeocodeException() {
			super();
		}

		GeocodeException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

		GeocodeException(String detailMessage) {
			super(detailMessage);
		}

		GeocodeException(Throwable throwable) {
			super(throwable);
		}
	}

	@SuppressWarnings("serial")
	public static class GeocodeNetworkException extends GeocodeException {
		GeocodeNetworkException() {
			super();
		}

		GeocodeNetworkException(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}

		GeocodeNetworkException(String detailMessage) {
			super(detailMessage);
		}

		GeocodeNetworkException(Throwable throwable) {
			super(throwable);
		}
	}

	private final WebGeocoder mWebGeocoder;
	private final DBGeocoder mDBGeocoder;

	/* Initialise the OSGeocoder
	 If both path and apiKey are nil, then results will only be returned for OSGeocodeTypeGridReference
	 If path is nil, searches for offline types OSGeocodeTypeGazetteer, OSGeocodeTypePostCode, and OSGeocodeTypeRoad will return no results.
	 If apiKey is nil, searches for online types OSGeocodeTypeOnlineGazetteer and OSGeocodeTypeOnlinePostCode will return no results.
	 
	 @param path location of a database file to use.
	 @param apiKey key for online searches.
	 @param context application context
	 @param openSpacePro indicates if online searches should be made against the pro service
	 */
	public Geocoder(File db, String apiKey, Context context, boolean openSpacePro) throws FailedToLoadException
	{
		mWebGeocoder = (apiKey == null ? null : new WebGeocoder(apiKey, context.getPackageName(), openSpacePro));

		mDBGeocoder = (db == null ? null : new DBGeocoder(db));
	}

	public void close()
	{
		if (mDBGeocoder != null)
		{
			mDBGeocoder.close();
		}
	}

	private List<? extends Placemark> geocodeString(String s, GeocodeType geocodeType, GridRect boundingRect, int start, int numResults) throws GeocodeException
	{
		switch (geocodeType)
		{
		case OnlineGazetteer:
		case OnlinePostcode:
			if (mWebGeocoder != null)
			{
				return mWebGeocoder.geocodeString(s, geocodeType, boundingRect, start, numResults);
			}
			return null;
		case Gazetteer:
		case Postcode:
		case Road:
			if (mDBGeocoder != null)
			{
				return mDBGeocoder.geocodeString(s, geocodeType, boundingRect, start, numResults);
			}
			return null;
		case GridReference:
			try {
				int[] temp = new int[1];
				GridPoint gp = GridPoint.parse(s.trim(), temp);
				int numDigits = temp[0];
				String gpStr = gp.toString(numDigits >= 0 ? numDigits : 6);
				Placemark result = new Placemark(gpStr, null, null, gp);

				return Arrays.asList(result);
			} catch (ParseException e) {
				// Parse failed. Oh well!
				return null;
			}
		}
		throw new AssertionError();
	}

/**
    @param geocodeTypes   types of search to execute
    @param boundingRect  limiting rectangle for search. To search the entire area, specify  null. This parameter is ignored for online searches
    @param start, numResults  specifies number (and offset) of results to return. This will be applied individually to each 
        type of search, so a range of {0,100} may return more than 100 results on combined gazetteer/road searches. 
        The range is ignored for postcode and grid reference searches. 
 		To return ALL results, set numResults to 0.
 */
	public Result geocodeString(String s, EnumSet<GeocodeType> geocodeTypes, GridRect boundingRect, int start, int numResults)
	{
		//Log.v(TAG, "Starting search");
		ArrayList<Placemark> placemarks = new ArrayList<Placemark>();
		ArrayList<GeocodeException> exceptions = new ArrayList<GeocodeException>();

		for (GeocodeType t : geocodeTypes)
		{
			try
			{
				Collection<? extends Placemark> results = geocodeString(s, t, boundingRect, start, numResults);
				if (results != null)
				{
					placemarks.addAll(results);
				}
			}
			catch (GeocodeException e)
			{
				exceptions.add(e);
			}
		}

		return new Result(placemarks, exceptions);
	}

	public final static class Result
	{
		private final List<? extends Placemark> mPlacemarks;
		private final List<? extends GeocodeException> mExceptions;
		Result(List<? extends Placemark> placemarks, List<? extends GeocodeException> exceptions) {
			mPlacemarks = placemarks;
			mExceptions = exceptions;
		}
		public List<? extends Placemark> getPlacemarks() {
			return mPlacemarks;
		}
		public List<? extends GeocodeException> getExceptions() {
			return mExceptions;
		}
	}
}

