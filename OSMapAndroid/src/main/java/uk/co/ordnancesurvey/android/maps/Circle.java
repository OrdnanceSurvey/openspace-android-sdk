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

import static android.opengl.GLES20.*;

import java.nio.FloatBuffer;

import android.graphics.PointF;

/**
 * A circle in the OS National Grid projection.
 * <p>
 * A circle has the following properties.
 * <p><b>Center</b>
 * <br>The center of the Circle is specified as a {@link GridPoint}.
 * <p><b>Radius</b>
 * <br>The radius of the circle, specified in meters. It should be zero or greater.
 * <p><b>Stroke Width</b>
 * <br>The width of the circle's outline in screen pixels. The width is constant and independent of the camera's zoom level. The default value is 10.
 * <p><b>Stroke Color</b>
 * <br>The color of the circle outline in ARGB format, the same format used by android.graphics.Color. The default value is black (0xff000000).
 * <p><b>Fill Color</b>
 * <br>The color of the circle fill in ARGB format, the same format used by android.graphics.Color. The default value is transparent (0x00000000).
 * <p><b>Visibility</b>
 * <br>Indicates if the circle is visible or invisible, i.e., whether it is drawn on the map. An invisible polygon is not drawn, but retains all of its 
 * other properties. The default is true, i.e., visible.
 * <p>Methods that modify a Polygon must be called on the main thread. If not, an IllegalStateException may be thrown at runtime.
 */
public final class Circle extends ShapeOverlay {
	private GridPoint mCenter;
	private double mRadius;

	Circle(CircleOptions options, GLMapRenderer map) {
		super(options, map);

		mCenter = options.getCenter();
		mRadius = options.getRadius();
	}


	/**
	 * Returns the center as a {@link GridPoint}
	 * @return The geographic center as a {@link GridPoint}.
	 */
	public GridPoint getCenter() {
		return mCenter;
	}

	/**
	 * Sets the center using a {@link GridPoint}.
	 * <p>
	 * The center must not be null, as there is no default value.
	 * @param center	The geographic center of the circle, specified as a {@link GridPoint}.
	 */
	public void setCenter(GridPoint center) {
		if(mCenter == null || mCenter.x != center.x || mCenter.y != center.y)
		{
			requestRender();
		}
		mCenter = center;
	}

	/**
	 * Returns the circle's radius, in meters.
	 * @return The radius in meters.
	 */
	public double getRadius() {
		return mRadius;
	}

	/**
	 * Sets the radius in meters.
	 * <p>
	 * The radius must be zero or greater.
	 * @param radius	The radius, in meters.
	 */
	public void setRadius(double radius) {
		if(radius != mRadius)
		{
			requestRender();
		}
		mRadius = radius;
	}


	void glDraw(PointF tempPoint, FloatBuffer tempFloatBuffer) {
		GLMapRenderer map = getMap();
		if (map == null)
		{
			return;
		}

		GridPoint center = getCenter();
		if(center == null)
		{
			return;
		}
		double radius = getRadius();
		int fillColor = getFillColor();
		int strokeColor = getStrokeColor();
		float strokeWidth = getStrokeWidth();
		
		ShaderCircleProgram program = map.shaderCircleProgram;

		Utils.setUniformPremultipliedColorARGB(program.uniformFillColor, fillColor);
		Utils.setUniformPremultipliedColorARGB(program.uniformStrokeColor, strokeColor);

		ScreenProjection projection = map.getProjection();
		PointF displayCenter = projection.displayPointFromGridPoint(center, tempPoint);
		float mpp = projection.getMetresPerPixel();
		float displayRadius = (float)radius/mpp;
		float innerRadius = displayRadius-strokeWidth/2;
		float outerRadius = displayRadius+strokeWidth/2;

		// Expand by an extra half-pixel to account for any AA.
		float expandby = outerRadius+0.5f;

		tempFloatBuffer.position(0);
		tempFloatBuffer.put(displayCenter.x-expandby).put(displayCenter.y-expandby);
		tempFloatBuffer.put(displayCenter.x+expandby).put(displayCenter.y-expandby);
		tempFloatBuffer.put(displayCenter.x-expandby).put(displayCenter.y+expandby);
		tempFloatBuffer.put(displayCenter.x+expandby).put(displayCenter.y+expandby);
		tempFloatBuffer.position(0);

		glVertexAttribPointer(program.attribVCoord, 2, GL_FLOAT, false, 0, tempFloatBuffer);

		// TODO: We need to use a different shader for large radii.
		glUniform4f(program.uniformCenterRadius, displayCenter.x, displayCenter.y, innerRadius, outerRadius);

		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		Utils.throwIfErrors();
	}

}
