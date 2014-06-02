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

public final class CircleOptions extends ShapeOptions {
	private GridPoint mCenter;
	private double mRadius;

	/**
	 * Sets the zIndex.
	 * Overlays (such as circles) with higher zIndices are drawn above those with lower indices.
	 * @param zIndex
	 * @return this CircleOptions object
	 */
	public CircleOptions zIndex(float zIndex)
	{
		setZIndex(zIndex);
		return this;
	}

	/**
	 * Sets the visibility of the circle.
	 * If this circle is not visible then it will not be drawn. All other state is preserved. Defaults to True.
	 * @param visible	false to make this shape invisible.
	 * @return this CircleOptions object
	 */
	public CircleOptions visible(boolean visible)
	{
		setVisible(visible);
		return this;
	}


	/**
	 * Sets the stroke color.
	 * <p>
	 * The stroke color is the color of this circle's outline, in the integer format specified by 
	 * android.graphics.Color. If TRANSPARENT is used then no outline is drawn.
	 * @param color	The stroke color in the android.graphics.Color format.
	 * @return this CircleOptions object
	 */
	public CircleOptions strokeColor(int color)
	{
		setStrokeColor(color);
		return this;
	}

	/**
	 * Sets the stroke width.
	 * <p>
	 * The stroke width is the width (in screen pixels) of the circle's outline. It must be zero or greater. 
	 * If it is zero then no outline is drawn. The default value is 10.
	 * <p>
	 * Behaviour with a negative width is undefined.
	 * @param width	The stroke width, in screen pixels.
 	 * @return this CircleOptions object
	 */
	public CircleOptions strokeWidth(float width)
	{
		setStrokeWidth(width);
		return this;
	}

	/**
	 * Sets the fill color.
	 * <p>The fill color is the color inside the circle, in the integer format specified by 
	 * android.graphics.Color. If TRANSPARENT is used then no fill is drawn.
	 * <p>
	 * By default the fill color is transparent (0x00000000).
	 * @param color	color in the android.graphics.Color format
  	 * @return this CircleOptions object
  	 */
	public CircleOptions fillColor(int color)
	{
		setFillColor(color);
		return this;
	}

	/**
	 * Sets the center using a {@link GridPoint}.
	 * <p>
	 * The center must not be null.
	 * <p>
	 * This method is mandatory because there is no default center.
	 * @param center The center of the circle.
	 * @return this CircleOptions object
	 */
	public CircleOptions center(GridPoint center)
	{
		mCenter = center;
		return this;
	}

	/**
	 * Sets the radius in meters.
	 * <p>
	 * The radius must be zero or greater. The default radius is zero.
	 * @param radius	radius in meters
	 * @return this CircleOptions object
	 */
	public CircleOptions radius(double radius)
	{
		mRadius = radius;
		return this;
	}




	/**
	 * Returns the center as a {@link GridPoint}
	 * @return The geographic center as a {@link GridPoint}.
	 */
	public GridPoint getCenter() {
		return mCenter;
	}

	/**
	 * Returns the circle's radius, in meters.
	 * @return The radius in meters.
	 */
	public double getRadius() {
		return mRadius;
	}
}
