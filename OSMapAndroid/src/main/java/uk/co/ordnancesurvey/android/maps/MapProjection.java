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

/**
 * Encapsulates the conversion of Latitude/Longitude in WGS-84 to a {@link GridPoint}.
 * @author bblaukopf
 *
 */
public abstract class MapProjection {
	MapProjection(){
		//"package-private" constructor
	}

	private static final MapProjection DEFAULT_PROJECTION = new BasicMapProjection();
	
	/**
	 * Get the default Map Projection
	 * @return a {@link BasicMapProjection}
	 */
	public static MapProjection getDefault() {
		return DEFAULT_PROJECTION;
	}

	/**
	 * Converts a WGS84 latitude/longitude to the corresponding GridPoint.
	 * Accuracy depends on the projection used.
	 * @param latitude
	 * @param longitude
	 * @return newly created GridPoint
	 */
	public abstract GridPoint toGridPoint(double latitude, double longitude);

	/**
	* Converts a GridPoint to the corresponding WGS84 latitude/longitude.
	* Results are returned in a caller-provided array, where the first element
	* contains the latitude and the second (double[1]) contains the longitude.
	* Accuracy depends on the projection used.
	* @param gp The GridPoint to convert.
	* @param latLngOut Must be an array of length >= 2.
	*/
	public abstract void fromGridPoint(GridPoint gp, double[] latLngOut);
}
