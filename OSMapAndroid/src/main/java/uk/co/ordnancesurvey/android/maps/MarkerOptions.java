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
 * Defines MarkerOptions for a marker.
 */
public final class MarkerOptions {
	private GridPoint mGridPoint;
	private BitmapDescriptor mBitmapDescriptor = BitmapDescriptorFactory.defaultMarker();
	// Default anchor is middle of bottom
	private float mU = 0.5f;
	private float mV = 1.0f;
	private boolean mDraggable;
	private boolean mVisible = true;
	private String mTitle;
	private String mSnippet;

	/**
	 * Creates a new set of marker options.
	 */
	public MarkerOptions() {
	}
	
	/**
	 * Specifies the anchor to be at a particular point in the marker image.
	 * <p>
The anchor specifies the point in the icon image that is anchored to the marker's position on the Earth's surface.
The anchor point is specified in the continuous space [0.0, 1.0] x [0.0, 1.0], where (0, 0) is the top-left corner of the image, and (1, 1) is the bottom-right corner. The anchoring point in a W x H image is the nearest discrete grid point in a (W + 1) x (H + 1) grid, obtained by scaling the then rounding. For example, in a 4 x 2 image, the anchor point (0.7, 0.6) resolves to the grid point at (3, 1).
<pre><code>
* *-----+-----+-----+-----*
* |     |     |     |     |
* |     |     |     |     |
* +-----+-----+-----+-----+
* |     |     |   X |     |   (U, V) = (0.7, 0.6)
* |     |     |     |     |
* *-----+-----+-----+-----*
*
* *-----+-----+-----+-----*
* |     |     |     |     |
* |     |     |     |     |
* +-----+-----+-----X-----+   (X, Y) = (3, 1)
* |     |     |     |     |
* |     |     |     |     |
* *-----+-----+-----+-----*
* </code></pre>
 	 * @param u u-coordinate of the anchor, as a ratio of the image width (in the range [0, 1])
	 * @param v v-coordinate of the anchor, as a ratio of the image height (in the range [0, 1])
	 * @return the object for which the method was called, with the new anchor set.
	 */
	public MarkerOptions anchor(float u, float v)
	{
		mU = u;
		mV = v;
		return this;
	}
	
	/**
	 * Horizontal distance, normalized to [0, 1], of the anchor from the left edge.
	 * @return the u value of the anchor.
	 */
	public float getAnchorU()
	{
		return mU;
	}
	
	/**
	 * Vertical distance, normalized to [0, 1], of the anchor from the left edge.
	 * @return the v value of the anchor.
	 */
	public float getAnchorV()
	{
		return mV;
	}
	
	/**
	 * Sets the location for the marker.
	 * @return the object for which the method was called, with the new position set.
	 */
	public MarkerOptions gridPoint(GridPoint gp)
	{
		mGridPoint = gp;
		return this;
	}
	
	public GridPoint getGridPoint()
	{
		return mGridPoint;
	}

	/**
	 * Sets the icon for the marker.
	 * @param icon	if null, the default marker is used.
	 * @return the object for which the method was called, with the new icon set.
	 */
	public MarkerOptions icon(BitmapDescriptor icon) {
		mBitmapDescriptor = icon;
		return this;
	}

	/**
	 * Sets the title for the marker.
	 * @param title
	 * @return the object for which the method was called, with the new title set.	
	 */
	public MarkerOptions title(String title)
	{
		mTitle = title;
		return this;
	}
	
	
	/**
	 * Sets the snippet for the marker.
	 * @return the object for which the method was called, with the new snippet set.
	 */	
	public MarkerOptions snippet(String snippet)
	{
		mSnippet = snippet;
		return this;		
	}

	/**
	 * Sets the visibility for the marker.
	 * @return the object for which the method was called, with the new visibility state set.
	 */
	public MarkerOptions visible(boolean visible)
	{
		mVisible = visible;
		return this;
	}
	
	/**
	 * Sets the draggability for the marker.
	* @return the object for which the method was called, with the new draggable state set.
	 */
	public MarkerOptions draggable(boolean draggable)
	{
		mDraggable = draggable;
		return this;
	}

	/**
	 * Gets the custom icon set for this MarkerOptions object.
	 * @return A BitmapDescriptor representing the custom icon, or null if no custom icon is set.
	 */
	public BitmapDescriptor getIcon()
	{
		return mBitmapDescriptor;
	}
	
	/**
	 * Gets the title set for this MarkerOptions object.
	 * @return A string containing the marker's title.
	 */
	public String getTitle()
	{
		return mTitle;
	}
	
	
	/**
	 * Gets the snippet set for this MarkerOptions object.
	 * @return A string containing the marker's snippet.
	 */
	public String getSnippet()
	{
		return mSnippet;
	}
	
	/**
	 * Gets the visibility setting for this MarkerOptions object.
	 * @return true if the marker is visible; otherwise, returns false.
	 */
	public boolean isVisible()
	{
		return mVisible;
	}
	
	/**
	 * Gets the draggability setting for this MarkerOptions object.
	 * @return true if the marker is draggable; otherwise, returns false.
	 */
	public boolean isDraggable()
	{
		return mDraggable;
	}
}
