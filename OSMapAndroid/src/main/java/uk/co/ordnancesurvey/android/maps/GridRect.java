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
 * Represents a rectangular region on the OS Map.
 */
public final class GridRect {
	/** Left-most coordinate, in metres */
	public final double minX;
	/** Right-most coordinate in metres */
	public final double maxX;
	/* Bottom-most coordinate in metres */
	public final double minY;
	/* Top-most coordinate in metres */
	public final double maxY;

	/**
	 * A pre-defined GridRect that will be interpreted as a null rect.
	 */
	static final GridRect GridRectNull = new GridRect(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
	/**
	 * The bounds of OS National Grid
	 */
	static final GridRect GridRectBounds = new GridRect(0, 0, GridPoint.GRID_WIDTH, GridPoint.GRID_HEIGHT);

	/**
	 * Construct a GridRect
	 * @param x0 South-West Eastings
	 * @param y0 South-West Northings
	 * @param x1 North-East Eastings
	 * @param y1 North-East Northings
	 */
	public GridRect(double x0, double y0, double x1, double y1) {
		minX = x0;
		minY = y0;
		maxX = x1;
		maxY = y1;
	}

	/**
	 * Construct a GridRect with a centre coordinate and a width/height in metres.
	 * @param cX Centre Eastings
	 * @param cY Centre Northings
	 * @param w  Width in metres
	 * @param h  Height in metres
	 * @return new GridRect
	 */
	public static GridRect fromCentreXYWH(double cX, double cY, double w, double h) {
		double x0 = cX-w/2;
		double y0 = cY-h/2;
		double x1 = cX+w/2;
		double y1 = cY+h/2;
		return new GridRect(x0, y0, x1, y1);
	}

	/**
	 * Construct a new GridRect, clipped to the OS National Grid bounds.
	 * @param x0 South-West Eastings
	 * @param y0 South-West Northings
	 * @param x1 North-East Eastings
	 * @param y1 North-East Northings
	 * @return new GridRect
	 */
	static GridRect clippedToGridBounds(double x0, double y0, double x1, double y1) {
		x0 = Math.max(0, x0);
		y0 = Math.max(0, y0);
		x1 = Math.min(GridPoint.GRID_WIDTH, x1);
		y1 = Math.min(GridPoint.GRID_HEIGHT, y1);
		return new GridRect(x0, y0, x1, y1);
	}

	GridRect clippedToGridBounds() {
		// TODO: Is this actually worth it? The extra alloc could be optimized away by a sufficiently clever JIT.
		if (0 <= minX && maxX <= GridPoint.GRID_WIDTH && 0 <= minY && maxY <= GridPoint.GRID_HEIGHT) {
			return this;
		}
		return clippedToGridBounds(minX, minY, maxX, maxY);
	}

	boolean contains(double x, double y)
	{
		return (minX <= x && x < maxX) && (minY <= y && y < maxY);
	}
	
	boolean contains(GridPoint gp)
	{
		if(gp == null)
		{
			return false;
		}
		return contains(gp.x, gp.y);
	}
	
	// Assumes normalized rects.
	GridRect inset(double dx, double dy)
	{
		double x0 = minX + dx;
		double x1 = maxX - dx;
		double y0 = minY + dy;
		double y1 = maxY - dy;
		if(x1 > x0 && y1 > y0)
		{
			return new GridRect(x0,y0,x1,y1);
		}

		return GridRectNull;
	}

	/**
	 * Returns a {@link GridPoint} corresponding to the SW corner (may allocate an object).
	 * @return {@link GridPoint} for the SW corner
	 */
	public GridPoint sw() {
		return new GridPoint(minX, minY);
	}

	/**
	 * Returns a {@link GridPoint} corresponding to the NE corner (may allocate an object).
	 * @return {@link GridPoint} for the NE corner
	 */
	public GridPoint ne() {
		return new GridPoint(maxX, maxY);
	}

	/**
	 * Returns a {@link GridPoint} corresponding to the NW corner (may allocate an object).
	 * @return {@link GridPoint} for the NW corner
	 */
	public GridPoint nw() {
		return new GridPoint(minX, maxY);
	}

	/**
	 * Returns a {@link GridPoint} corresponding to the SE corner (may allocate an object).
	 * @return {@link GridPoint} for the SE corner
	 */
	public GridPoint se() {
		return new GridPoint(maxX, minY);
	}

	/**
	 * Returns a {@link GridPoint} corresponding to the approximate center (may allocate an object).
	 * @return {@link GridPoint} for the center
	 */
	public GridPoint center() {
		return new GridPoint((minX+maxX)/2,(minY+maxY)/2);
	}

	boolean isNull() {
		return Double.isInfinite(minX) || Double.isInfinite(minY);
	}

	GridRect normalized() {
		double x0, x1, y0, y1;
		x0 = Math.min(minX,  maxX);
		x1 = Math.max(minX, maxX);
		y0 = Math.min(minY, maxY); 
		y1 = Math.max(minY, maxY); 
		GridRect rect = this;
		if(x0 < minX || y0 < minY)
		{
			if(x0 < x1 && y0 < y1)
			{
				rect = new GridRect(x0,y0,x1,y1);
			}
			else
			{
				rect = GridRectNull;
			}
		}
		return rect;
		
	}

	// Assumes normalized rects
	GridRect intersect(GridRect rect) {	
		double x0 = Math.max(minX, rect.minX);
		double x1 = Math.min(maxX, rect.maxX);
		double y0 = Math.max(minY, rect.minY);
		double y1 = Math.min(maxY, rect.maxY);
		if(x1 > x0 && y1 > y0)
		{
			return new GridRect(x0,y0,x1,y1);
		}
		return GridRectNull;
	}
	
}
