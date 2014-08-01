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

public final class PolygonOptions extends PolyOptions {
	public PolygonOptions add(GridPoint gp)
	{
		mPoints.add(gp);
		return this;
	}

	public PolygonOptions add(GridPoint... points)
	{
		for(GridPoint gp : points)
		{
			mPoints.add(gp);
		}
		return this;
	}

	public PolygonOptions addAll(Iterable<GridPoint> points)
	{
		for (GridPoint gp : points)
		{
			mPoints.add(gp);
		}
		return this;
	}

	public PolygonOptions strokeColor(int color)
	{
		setStrokeColor(color);
		return this;
	}

	public PolygonOptions strokeWidth(float width)
	{
		setStrokeWidth(width);
		return this;
	}

	public PolygonOptions fillColor(int color)
	{
		setFillColor(color);
		return this;
	}

	public PolygonOptions zIndex(float zIndex)
	{
		setZIndex(zIndex);
		return this;
	}

	public PolygonOptions visible(boolean visible)
	{
		setVisible(visible);
		return this;
	}

	@Override
	public int getStrokeColor()
	{
		return super.getStrokeColor();
	}

	@Override
	public float getStrokeWidth() {
		return super.getStrokeWidth();
	}

	@Override
	public int getFillColor()
	{
		return super.getFillColor();
	}
}