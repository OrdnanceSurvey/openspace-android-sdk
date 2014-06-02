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

abstract class ShapeOverlay {
	// Applicable to all shapes.
	private boolean mVisible;
	private float mZIndex;

	// Applicable to all shapes, but sometimes renamed to "color" and "width"
	private int mStrokeColor;
	private float mStrokeWidth;

	// Applicable to filled shapes: Circle and Polygon
	private int mFillColor;

	private GLMapRenderer mMap;

	ShapeOverlay(ShapeOptions options, GLMapRenderer map)
	{
		mVisible = options.isVisible();
		mZIndex = options.getZIndex();

		mStrokeColor = options.getStrokeColor();
		mStrokeWidth = options.getStrokeWidth();

		mFillColor = options.getFillColor();

		mMap = map;
	}

	/**
	 * Sets the zIndex.
	 * Overlays (such as shapes) with higher zIndices are drawn above those with lower indices.
	 * @param zIndex
	 */
	public void setZIndex(float zIndex) {
		mZIndex = zIndex;
		requestRender();
	}

	/**
	 * Sets the visibility of the shape.
	 * If this shape is not visible then it will not be drawn. All other state is preserved. Defaults to True.
	 * @param visible	false to make this shape invisible.
	 */
	public void setVisible(boolean visible) {
		mVisible = visible;
		requestRender();
	}

	/**
	 * Returns the zIndex.
	 * @return The zIndex of this shape.
	 */
	public float getZIndex() {
		return mZIndex;
	}

	/**
	 * Checks whether the shape is visible.
	 * @return True if the shape is visible; false if it is invisible.
	 */
	public boolean isVisible() {
		return mVisible;
	}

	

	/**
	 * Sets the stroke color.
	 * <p>
	 * The stroke color is the color of this shape's outline, in the integer format specified by 
	 * android.graphics.Color. If TRANSPARENT is used then no outline is drawn.
	 * @param color	The stroke color in the android.graphics.Color format.
	 */
	public void setStrokeColor(int color) {		
		mStrokeColor = color;
		requestRender();
	}


	/**
	 * Sets the stroke width.
	 * <p>
	 * The stroke width is the width (in screen pixels) of the shape's outline. It must be zero or greater. 
	 * If it is zero then no outline is drawn. The default value is 10.
	 * <p>
	 * Behaviour with a negative width is undefined.
	 * @param width	The stroke width, in screen pixels.
	 */
	public void setStrokeWidth(float width) {
		mStrokeWidth = width;
		requestRender();
	}

	


	/**
	 * Returns the stroke color.
	 * @return The color of the circle in ARGB format.
	 */
	public int getStrokeColor() {
		return mStrokeColor;
	}

	/**
	 * Returns the stroke width.
	 * @return The width in screen pixels.
	 */
	public float getStrokeWidth() {
		return mStrokeWidth;
	}

	/**
	 * Returns the fill color of this Shape.
	 * @return The fill color of the shape in ARGB format.
	 */
	public int getFillColor() {
		return mFillColor;
	}



	/**
	 * Sets the fill color.
	 * <p>
	 * The fill color is the color inside the shape, in the integer format specified by android.graphics.Color. 
	 * If TRANSPARENT is used then no fill is drawn.
	 * @param fillColor	The color in the android.graphics.Color format.
	 */
	public void setFillColor(int fillColor) {
		if(mFillColor != fillColor)
		{
			requestRender();
		}
		mFillColor = fillColor;
	}


	final GLMapRenderer getMap() {
		return mMap;
	}

	final void requestRender() {
		GLMapRenderer map = getMap();
		if (map != null) {
			map.requestRender();
		}
	}

	/**
	 * Removes this shape from the map.
	 */
	public final void remove() {
		GLMapRenderer map = getMap();
		if (map != null) {
			map.removeOverlay(this);
		}
	}
}
