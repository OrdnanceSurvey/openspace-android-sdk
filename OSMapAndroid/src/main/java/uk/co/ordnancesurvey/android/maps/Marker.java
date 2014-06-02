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
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.Matrix;
import android.view.View;
import android.graphics.Canvas;

/**
 * An icon placed at a particular point on the map's surface. A marker icon is drawn oriented against 
 * the device's screen rather than the map's surface; i.e., it will not necessarily change orientation 
 * due to map rotations, tilting, or zooming.
 * <p>
 * A marker has the following properties:
 * <p>
 * <b>Anchor</b>
 * <br>The point on the image that will be placed at the LatLng position of the marker. 
 * This defaults to 50% from the left of the image and at the bottom of the image.
 * <p><b>Position</b>
 * <br>The {@link GridPoint} value for the marker's position on the map. You can change this 
 * value at any time if you want to move the marker.
 * <p><b>Title</b>
 * <br>A text string that's displayed in an info window when the user taps the marker. 
 * You can change this value at any time.
 * <p><b>Snippet</b>
 * <br>Additional text that's displayed below the title. You can change this value at any time.
 * <p><b>Icon</b>
 * <br>A bitmap that's displayed for the marker. If the icon is left unset, a default icon is displayed. 
 * You can specify an alternative coloring of the default icon using defaultMarker(float). 
 * You can't change the icon once you've created the marker.
 * <p><b>Drag Status</b>
 * <br>If you want to allow the user to drag the marker, set this property to true. 
 * You can change this value at any time. The default is false.
 * <p><b>Visibility</b>
 * <br>By default, the marker is visible. To make the marker invisible, set this property to false. 
 * You can change this value at any time.
 * <p>
 * <b>Example</b>
 * <pre>
 * <code>
 OSMap map = ... // get a map.
 // Add a marker at Scafell Pike
 Marker marker = map.addMarker(new MarkerOptions()
     .position(GridPoint.parse("NY2154807223"))
     .title("Scafell Pike")
     .snippet("Highest Mountain in England"));
  * </code></pre>
  * <p>
  * <b>Developer Guide</b>
  * <p>For more information, read the Markers developer guide.
  * 
 */
public final class Marker {
	private GridPoint mGridPoint;
	private final Bitmap mIconBitmap;
	private final float mIconTintR;
	private final float mIconTintG;
	private final float mIconTintB;
	private boolean mVisible;
	private boolean mDraggable;
	private String mTitle;
	private String mSnippet;
	private float mAnchorU;
	private float mAnchorV;
	private GLMapRenderer mMap;
	private float mBearing;

	// Volatile so that reading/writing to it is also an appropriate barrier.
	private volatile Bitmap mVolatileInfoBitmap;
	private boolean mInfoWindowHighlighted;
	
	Marker(MarkerOptions options, Bitmap icon, GLMapRenderer map)
	{
		mGridPoint = options.getGridPoint();
		mIconBitmap = icon;
		mIconTintR = options.getIcon().mTintR;
		mIconTintG = options.getIcon().mTintG;
		mIconTintB = options.getIcon().mTintB;
		mVisible = options.isVisible();
		mDraggable = options.isDraggable();
		mTitle = options.getTitle();
		mSnippet = options.getSnippet();
		mAnchorU = options.getAnchorU();
		mAnchorV = options.getAnchorV();
		mMap = map;
	}
	
	/**
	 * Returns the position of the marker.
	 * @return A {@link GridPoint} object specifying the marker's current position
	 */
	public GridPoint getGridPoint()
	{
		return mGridPoint;
	}
	
	
	public void setGridPoint(GridPoint gp)
	{
		mGridPoint = gp;
		requestRender();
	}
	
	boolean containsPoint(ScreenProjection projection, PointF testPoint, PointF tempPoint, RectF tempRect)
	{
		// tempPoint is used to save memory allocation - the alternative is allocating a new object
		// for every call.
		getScreenLocation(projection, tempPoint);

		tempRect.left = tempPoint.x;
		tempRect.top = tempPoint.y;
		tempRect.right = tempPoint.x + mIconBitmap.getWidth();
		tempRect.bottom = tempPoint.y + mIconBitmap.getHeight();

		return tempRect.contains(testPoint.x, testPoint.y);
	}
	
	
	/**
	 * Sets the visibility of this marker. If set to false and an info window is currently showing 
	 * for this marker, this will hide the info window.
	 */
	public void setVisible(boolean visible)
	{
		mVisible = visible;
		requestRender();
	}
	
	public boolean isVisible()
	{
		return mVisible;
	}
	
	
	/**
	 * Sets the bearing of the ground overlay (the direction that the vertical axis of the marker points) in degrees 
	 * clockwise from north. The rotation is performed about the anchor point.
	 * 
	 * @param bearing	bearing in degrees clockwise from north
	 */
	void setBearing(float bearing)
	{
		mBearing = bearing;
		requestRender();
	}
	
	/**
	 * Shows the info window of this marker on the map, if this marker {@link #isVisible()}.
	 */
	public void showInfoWindow()
	{
		if(!isVisible()) 
		{
			return;
		}
		// Get a view
		View view = mMap.getInfoWindow(this);

		if(mInfoWindowHighlighted)
		{
			// This is holo light blue
			view.setBackgroundColor(0xff33b5e5);
		}
		if (view == null)
		{
			return;
		}

		if (view.getWidth() == 0 || view.getHeight() == 0)
		{
			assert false: "View width/height should be nonzero";
			return;
		}
		// Convert to a bitmap.
		Bitmap bmp = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);
		view.draw(canvas);
	
		// Save the bitmap, but only after we're done drawing!
		mVolatileInfoBitmap = bmp;

		mMap.onInfoWindowShown(this);
	}
	
	/**
	 * Hides the info window if it is shown from this marker.
	 * <p>
	 * This method has no effect if this marker is not visible.
	 */
	public void hideInfoWindow()
	{
		// Check to see if our info window is shown - if not do nothing
		if(mVolatileInfoBitmap == null || !isVisible())
		{
			return;
		}
		mInfoWindowHighlighted = false;
		mVolatileInfoBitmap = null;
		mMap.onInfoWindowShown(null);
	}
	
	
	/**
	 * Sets the draggability of the marker. When a marker is draggable, it can be moved by the user by long pressing on the marker.
	 */
	public void setDraggable(boolean draggable)
	{
		mDraggable = draggable;
	}
	
	/**
	 * Gets the draggability of the marker. When a marker is draggable, it can be moved by the user by long pressing on the marker.
	 * @return true if the marker is draggable; otherwise, returns false.
	 */
	public boolean isDraggable()
	{
		return mDraggable;
	}

	/**
	 * Removes this marker from the map. After a marker has been removed, the behavior of all its methods is undefined.
	 */
	public void remove()
	{
		GLMapRenderer map = mMap;
		if (map != null) {
			map.removeMarker(this);
		}
		mMap = null;
	}
	
	/**
	 * Sets the title of the marker.
	 */
	public void setTitle(String title)
	{
		mTitle = title;
		// TODO: Update info bitmap atomically?
	}
	
	/**
	 * Sets the snippet of the marker.
	 */
	public void setSnippet(String snippet)
	{
		mSnippet = snippet;
		// TODO: Update info bitmap atomically?
	}

	/**
	 * Gets the title of the marker.
	 * @return A string containing the marker's title.
	 */
	public String getTitle()
	{
		return mTitle;
	}

	/**
	 * Gets the snippet of the marker.
	 * @return A string containing the marker's snippet.
	 */
	public String getSnippet()
	{
		return mSnippet;
	}
	
	float getAnchorU()
	{
		return mAnchorU;
	}

	float getAnchorV()
	{
		return mAnchorV;
	}

	final void requestRender() {
		GLMapRenderer map = mMap;
		if (map != null) {
			map.requestRender();
		}
	}

	PointF getScreenLocation(ScreenProjection projection, PointF screenLocationOut)
	{
		projection.toScreenLocation(mGridPoint, screenLocationOut);

		// U and V are in the range 0..1 where 0,0 is the top left. Since we draw the marker from the bottom left,
		// convert V' = 1 - V.
		int height = mIconBitmap.getHeight();
		int width = mIconBitmap.getWidth();

		screenLocationOut.x -= width * mAnchorU;
		screenLocationOut.y -= height * mAnchorV;

		return screenLocationOut;
	}
	
	
	boolean isClickOnInfoWindow(PointF clickLocation) 
	{
		PointF temp = new PointF();
		// TODO Thread safety - what happens if projection changes? Does it matter?
		ScreenProjection projection = mMap.getProjection();
		PointF markerLocation = getScreenLocation(projection, temp);

		
		// Edge effects don't matter; no one will notice a 1 px difference on a click.
		RectF checkRect = new RectF(markerLocation.x + mIconBitmap.getWidth()/ 2 - mVolatileInfoBitmap.getWidth()/2, markerLocation.y - mIconBitmap.getHeight() - 30,0, 0);
		checkRect.right = checkRect.left + mVolatileInfoBitmap.getWidth();
		checkRect.bottom = checkRect.top + mVolatileInfoBitmap.getHeight();
		boolean ret = checkRect.contains(clickLocation.x, clickLocation.y);
		if(ret)
		{
			setInfoWindowHighlighted(true);
		}
		return ret;
	}
	
	void setInfoWindowHighlighted(boolean highlighted)
	{
		mInfoWindowHighlighted = highlighted;
		showInfoWindow();
	}
	
	void glDraw(float[] ortho, float[] mvpTempMatrix, GLImageCache imageCache, PointF temp)
	{
		ShaderProgram program = mMap.shaderProgram;
		glUniform4f(program.uniformTintColor, mIconTintR, mIconTintG, mIconTintB, 1);

		// Render the marker. For the moment, use the standard marker - and load it every time too!!
		GLImageCache.ImageTexture tex = imageCache.bindTextureForBitmap(mIconBitmap);
		if(tex == null)
		{
			return;
		}
		


		// Draw this texture in the correct place 
		glVertexAttribPointer(program.attribVCoord, 2, GL_FLOAT, false, 0, tex.vertexCoords);

		ScreenProjection projection = mMap.getProjection();
		projection.toScreenLocation(mGridPoint, temp);
		PointF screenLocation = temp;
		final float OFFSET = 1/3.0f;
		float xPixels = (float)Math.rint(screenLocation.x+OFFSET);
		float yPixels = (float)Math.rint(screenLocation.y+OFFSET);

		Matrix.translateM(mvpTempMatrix, 0, ortho, 0, xPixels, yPixels, 0);
		if(mBearing != 0)
		{
			Matrix.rotateM(mvpTempMatrix, 0, mBearing, 0, 0, 1);
		}

		glUniformMatrix4fv(program.uniformMVP, 1, false, mvpTempMatrix, 0);
		
		int height = mIconBitmap.getHeight();
		int width = mIconBitmap.getWidth();
		
		// Render the marker, anchored at the correct position.
		xPixels = -width * mAnchorU;
		yPixels = -height * mAnchorV;
		glVertexAttrib4f(program.attribVOffset, xPixels, yPixels, 0, 1);
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		Utils.throwIfErrors();

		// Draw the info window if necessary
		Bitmap infoBitmap = mVolatileInfoBitmap;
		if(infoBitmap != null)
		{
			// Draw centered above the marker
			yPixels -= tex.height;
			xPixels -= tex.width / 2;
			tex = imageCache.bindTextureForBitmap(infoBitmap);
			if(tex == null)
			{
				return;
			}

			glUniform4f(program.uniformTintColor, -1, -1, -1, 1);

			glVertexAttribPointer(program.attribVCoord, 2, GL_FLOAT, false, 0, tex.vertexCoords);
			glVertexAttrib4f(program.attribVOffset, xPixels, yPixels, 0, 1);
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
			Utils.throwIfErrors();
        }
	}
}
