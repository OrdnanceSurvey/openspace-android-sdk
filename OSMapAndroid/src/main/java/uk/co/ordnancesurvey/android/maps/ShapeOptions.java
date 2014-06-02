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
 * Options for a {@link Shape}
 */
abstract class ShapeOptions {
	private int mFillColor = 0xff000000;
	private int mStrokeColor = 0xff000000;
	private float mStrokeWidth = 10;
	private boolean mVisible = true;
	private float mZIndex;

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
	 * Returns the stroke color.
	 * @return The color in ARGB format.
	 */
	int getStrokeColor() {
		return mStrokeColor;
	}

	/**
	 * Returns the stroke width.
	 * @return The width in screen pixels.
	 */
	float getStrokeWidth() {
		return mStrokeWidth;
	}


	/**
	 * Returns the fill color
	 * @return The  color in ARGB format.
	 */
	int getFillColor() {
		return mFillColor;
	}


	void setFillColor(int fillColor) {
		mFillColor = fillColor;
	}

	void setStrokeColor(int strokeColor) {
		mStrokeColor = strokeColor;
	}


	
	void setStrokeWidth(float strokeWidth) {
		mStrokeWidth = strokeWidth;
	}

	
	void setZIndex(float zIndex) {
		mZIndex = zIndex;
	}

	
	void setVisible(boolean visible) {
		mVisible = visible;
	}
}
