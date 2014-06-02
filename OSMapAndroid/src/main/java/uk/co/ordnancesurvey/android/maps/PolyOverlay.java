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

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import android.util.Log;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.graphics.PointF;
import android.opengl.Matrix;

abstract class PolyOverlay extends ShapeOverlay {
	private volatile PolyPoints mPoints;
	private final boolean mClosed;
	// mRotation and mPixelCoordinates were originally used for the location marker. They are left in for future convenience and possible future developer
	// use, but should be properly tested before such use.
	private float mRotation = 0;
	private final boolean mPixelCoordinates;

	
	PolyOverlay(PolyOptions options, GLMapRenderer map, boolean closed)
	{
		super(options, map);
		mClosed = closed;
		mPoints = new PolyPoints(options.getPoints());
		mPixelCoordinates = options.getPixelCoordinates();
		calculatePolygonForLine(options.getPoints());	
	}
	
	
	void setRotation(float rotation)
	{
		mRotation = rotation;
		requestRender();
	}

	public void setPoints(List<GridPoint> points)
	{
		mPoints = new PolyPoints(points);
		calculatePolygonForLine(points);	
		requestRender();
	}

	public List<GridPoint> getPoints() {
		return mPoints.getPoints();
	}

	PolyPoints getPolyPoints() {
		return mPoints;
	}

	final void glDraw(float[] orthoMatrix, float[] tempMatrix, PointF tempPoint, float metresPerPixel) {
		GLMapRenderer map = getMap();
		if (map == null)
		{
			return;
		}

		ScreenProjection projection = map.getProjection();

		// Read mPoints once; it could change in another thread!
		PolyPoints points = getPolyPoints();

		ShaderOverlayProgram program = map.shaderOverlayProgram;
		
		glSetMatrix(program.uniformMVP, orthoMatrix, tempMatrix, projection, points, metresPerPixel);


		// Set up the line coordinates.
		glVertexAttribPointer(program.attribVCoord, 2, GL_FLOAT, false, 0, points.mVertexBuffer);
		glDrawPoints(program.uniformColor, points, metresPerPixel, program.attribVCoord);
	}


	private void calculatePolygonForLine(List<GridPoint> points)
	{
		PolyPoints polyPoints = getPolyPoints();
		int numPoints = points.size();
		int numVertices = numPoints * 11;
		if(mClosed)
		{
			numVertices += 11;
		}
				
		
		// Allow space for each normal (1 per point) and each vertex we need to generate later
		FloatBuffer polyData  = Utils.directFloatBuffer((numVertices + numPoints) * 2);

		// Pre-compute the normals for each line segment
		for(int i = 0; i < numPoints; i++)
		{
			GridPoint pt1 = points.get(i);
			// These initializers are just to avoid compiler warnings about possible failure to initialize
			double ax = 0;
			double bx = 0;
			double ay = 0;
			double by = 0;
			boolean setup_a = false;
			boolean setup_b = false;
			// Incoming line
			if(mClosed || i > 0)
			{
				int j = (i+numPoints-1) % numPoints;
				GridPoint pt0 = points.get(j);

				ax = pt1.x - pt0.x;
				ay = pt1.y - pt0.y;

				double maga = Math.sqrt(ax*ax + ay*ay);
				// Normalize a
				ax = ax / maga;
				ay = ay / maga;
				setup_a = true;
			}
			
			// Outgoing line
			if(mClosed || i < (numPoints - 1))
			{
				int k = (i+1) % numPoints;
				GridPoint pt2 = points.get(k);
				bx = pt2.x - pt1.x;
				by = pt2.y - pt1.y;
				// Normalize b.
				double magb = Math.sqrt(bx*bx + by*by);
				bx = bx / magb;
				by = by / magb;	
				setup_b = true;
			}
			double cx;
			double cy;

			if (setup_a && ! setup_b)
			{
				// Last point. Use the incoming normal
				cx = ay;
				cy = -ax;
			}
			else
			{
				// Use the normal for the outgoing line
				cx = by;
				cy = -bx;
				
			}
			
			Log.v("Corner", "Set " + cx + "," + cy);
	
			polyData.put((float)cx);
			polyData.put((float)cy);
		}
		polyPoints.mAdditionalData = polyData;
	}

	
	/* Default implementation renders points as a line, possibly a wide line */
	void glDrawPoints(int shaderOverlayUniformColor, PolyPoints points, float metresPerPixel, int shaderOverlayAttribVCoord) {
		// Calculate triangles on the fly. We have a normal vector, and a corner vector. Both are normalized. 
		// First thing to do is work out what lineWidth is in metres.
		
		float width = getStrokeWidth();
		if(width == 1)
		{
			glDrawArrays(GL_LINE_STRIP, 0, points.mVertexCount);
			return;
		}
		
		FloatBuffer polyData = points.mAdditionalData;
		
		
		int numPoints = points.mVertexCount;
		
		// Offset to triangles.
		int toff = numPoints * 2;
		float lineWidthM = getStrokeWidth() / 2;
		if(!mPixelCoordinates)
		{
			lineWidthM *= metresPerPixel;
		}

		polyData.rewind();
		// Write an additional pair of points for a closed line.
		int writePoints = numPoints;
		if(mClosed)
		{
			writePoints++;
		}
		int vertexStep = 22;
		for(int i = 0; (i+1) < writePoints; i++)
		{		
			// Get point A (index i)
			// Not good - we need to synchronize the read of points with the read of polydata.
			float ax = points.mVertexBuffer.get((i % numPoints)*2);
			float ay = points.mVertexBuffer.get((i % numPoints)*2+1);

			// Get Point B (index i + 1)
			float bx = points.mVertexBuffer.get(((i+1) % numPoints)*2);
			float by = points.mVertexBuffer.get(((i+1) % numPoints)*2+1);
			
			// Normal to vector A->B
			float nabx = polyData.get((i % numPoints) * 2);
			float naby = polyData.get((i % numPoints) * 2 + 1);
			
			// Normal to vector B->C (where C is index i + 2)
			float nbcx = polyData.get(((i+1) % numPoints) * 2);
			float nbcy = polyData.get(((i+1) % numPoints) * 2 + 1);
			
			// Compute the normalised B->C vector by going CCW PI/2 from its normal
			// Used in dot product to determine which way we are turning at this corner
			float bcx = -nbcy;
			float bcy = nbcx;
			
			// Compute the vertices for the end cap "fill" triangle for this corner
			// This depends on whether we are turning CCW or CW
			float c1x, c1y, c2x, c2y, c3x, c3y;
			if ( bcx * nabx + bcy * naby < 0)
			{
				// ab -> bc is turning CCW
				c1x = bx;
				c1y = by;
				c2x = (float)(bx + nabx * lineWidthM);
				c2y = (float)(by + naby * lineWidthM);
				c3x = (float)(bx + nbcx * lineWidthM);
				c3y = (float)(by + nbcy * lineWidthM);
			}
			else
			{
				// ab -> bc is turning CW
				c1x = bx;
				c1y = by;
				c2x = (float)(bx - nabx * lineWidthM);
				c2y = (float)(by - naby * lineWidthM);
				c3x = (float)(bx - nbcx * lineWidthM);
				c3y = (float)(by - nbcy * lineWidthM);
			}
			
			
//			Log.v("Render", "Original " + ax + "," + ay);
//			Log.v("Render", "Normal " + nx + "," + ny);

			// The line segment itself
			float p1x =  (float)(ax + nabx * lineWidthM);
			float p1y =  (float)(ay + naby * lineWidthM);
			float p2x =  (float)(ax - nabx * lineWidthM);
			float p2y =  (float)(ay - naby * lineWidthM);
			float p3x =  (float)(bx + nabx * lineWidthM);
			float p3y =  (float)(by + naby * lineWidthM);
			float p4x =  (float)(bx - nabx * lineWidthM);
			float p4y =  (float)(by - naby * lineWidthM);
			
			// The corner filling triangle two null triangle groups
			float p5x =  p4x; // Repeat p4 to terminate that triangle
			float p5y =  p4y;
			float p6x =  c1x; // Repeat c1 twice to prevent an incipient triangle
			float p6y =  c1y;
			float p7x =  c1x;
			float p7y =  c1y; 
			float p8x =  c2x;
			float p8y =  c2y;
			float p9x =  c3x;
			float p9y =  c3y;
			float p10x =  c3x; // Repeat c3 to terminate triangle 
			float p10y =  c3y;
			float p11x =  (float)(bx + nbcx * lineWidthM); // Next triangle will start with this, so pre-fill it
			float p11y =  (float)(by + nbcy * lineWidthM);
			
//			Log.v("Render", "Point " + p1x + "," + p1y);
//			Log.v("Render", "Point " + p2x + "," + p2y);

			int offset = toff + i * vertexStep;
			polyData.put(offset, p1x);
			polyData.put(offset + 1, p1y);
			polyData.put(offset + 2, p2x);
			polyData.put(offset + 3, p2y);
			polyData.put(offset + 4, p3x);
			polyData.put(offset + 5, p3y);
			polyData.put(offset + 6, p4x);
			polyData.put(offset + 7, p4y);
			polyData.put(offset + 8, p5x);
			polyData.put(offset + 9, p5y);
			polyData.put(offset + 10, p6x);
			polyData.put(offset + 11, p6y);
			polyData.put(offset + 12, p7x);
			polyData.put(offset + 13, p7y);
			polyData.put(offset + 14, p8x);
			polyData.put(offset + 15, p8y);
			polyData.put(offset + 16, p9x);
			polyData.put(offset + 17, p9y);
			polyData.put(offset + 18, p10x);
			polyData.put(offset + 19, p10y);
			polyData.put(offset + 20, p11x);
			polyData.put(offset + 21, p11y);

		}
		
		float strokeWidth = getStrokeWidth();
		glLineWidth(strokeWidth);
		
		int strokeColor = getStrokeColor();
		Utils.setUniformPremultipliedColorARGB(shaderOverlayUniformColor, strokeColor);

		polyData.position(toff);
		glVertexAttribPointer(shaderOverlayAttribVCoord, 2, GL_FLOAT, false, 0, polyData.slice());

		glDrawArrays(GL_TRIANGLE_STRIP, 0, (writePoints-1) * vertexStep/2);
	}

	void glSetMatrix(int shaderOverlayUniformMVP, float[] orthoMatrix, float[] mvpTempMatrix, ScreenProjection projection, PolyPoints points, float metresPerPixel) {
		GridRect gridRect = projection.getVisibleMapRect();
		double topLeftX = gridRect.minX;
		double topLeftY = gridRect.maxY;

		float tx = (float)(points.mVertexCentre.x-topLeftX);
		float ty = (float)(points.mVertexCentre.y-topLeftY);

		// Convert from metres to pixels.
		// Translate by the appropriate number of metres
		Matrix.scaleM(mvpTempMatrix, 0, orthoMatrix, 0, 1/metresPerPixel, -1/metresPerPixel, 1);
		Matrix.translateM(mvpTempMatrix, 0, tx, ty, 0);
		Utils.throwIfErrors();
		if(mPixelCoordinates)
		{
			Matrix.scaleM(mvpTempMatrix, 0, orthoMatrix, 0, metresPerPixel, metresPerPixel, 1);
		}
		if(mRotation != 0)
		{	
			// mRotation is clockwise, so change the sign because rotateM is counter clockwise.
			Matrix.rotateM(mvpTempMatrix, 0, -mRotation, 0, 0, 1);
		}


		glUniformMatrix4fv(shaderOverlayUniformMVP, 1, false, mvpTempMatrix, 0);
		Utils.throwIfErrors();
	}

	final static class PolyPoints {
		private final GridPoint[] mArray;
		public final int mVertexCount;
		public final GridPoint mVertexCentre;
		public final FloatBuffer mVertexBuffer;
		public FloatBuffer mAdditionalData;
		public PolyPoints(List<GridPoint> points) {
			mArray = points.toArray(new GridPoint[0]);
			mVertexCount = mArray.length;
			mVertexCentre = getMidpoint(mArray);
			mVertexBuffer = getVertexBuffer(mArray, mVertexCentre, 1);
		}

		public List<GridPoint> getPoints() {
			return Collections.unmodifiableList(Arrays.asList(mArray));
		}

		private static GridPoint getMidpoint(GridPoint[] points) {
			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;

			for (GridPoint p : points) {
				minX = Math.min(minX, p.x);
				minY = Math.min(minY, p.y);
				maxX = Math.max(maxX, p.x);
				maxY = Math.max(maxY, p.y);
			}
			return new GridPoint((minX+maxX)/2, (minY+maxY)/2);
		}

		private static FloatBuffer getVertexBuffer(GridPoint[] points, GridPoint center, float scale) {
			// Set up line coordinates.
			FloatBuffer vertexBuffer = Utils.directFloatBuffer(points.length * 2);
			double centerX = center.x;
			double centerY = center.y;
			for(GridPoint gp : points)
			{
				float scaledX = (float)(gp.x-centerX)/scale;
				float scaledY = (float)(gp.y-centerY)/scale;
				vertexBuffer.put(scaledX);
				vertexBuffer.put(scaledY);
			}
			// Reset the position; this is where glVertexAttribPointer() starts from!
			vertexBuffer.position(0);

			return vertexBuffer;
		}
	}
}
