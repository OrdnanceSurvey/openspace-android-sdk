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

import android.graphics.PointF;

final class ScreenProjection {
	private final int mScreenWidth;
	private final int mScreenHeight;

	private final GridPoint mCentre;
	private final float mMetresPerPixel;
	private final GridRect mVisibleMapRect;

	ScreenProjection(int width, int height, MapScrollController.ScrollPosition scrollpos) {
		mScreenWidth = width;
		mScreenHeight = height;

		mCentre = new GridPoint(scrollpos.x, scrollpos.y);
		mMetresPerPixel = scrollpos.metresPerPixel;

		float mapWidth = mScreenWidth * mMetresPerPixel;
		float mapHeight = mScreenHeight * mMetresPerPixel;

		// Clip the rect in case the user has somehow scrolled off the map.
		mVisibleMapRect = GridRect.fromCentreXYWH(mCentre.x, mCentre.y, mapWidth, mapHeight).clippedToGridBounds();
	}

	public PointF toScreenLocation(GridPoint gp)
	{
		return toScreenLocation(gp, new PointF());
	}

	public PointF toScreenLocation(GridPoint gp, PointF pointOut)
	{
		float metresPerPixel = mMetresPerPixel;

		pointOut.x = mScreenWidth/2.0f + (float)(gp.x-mCentre.x)/metresPerPixel;
		pointOut.y = mScreenHeight/2.0f - (float)(gp.y-mCentre.y)/metresPerPixel;
		return pointOut;
	}

	public GridPoint fromScreenLocation(PointF point)
	{
		return fromScreenLocation(point.x, point.y);
	}

	public GridPoint fromScreenLocation(float x, float y)
	{
		float metresPerPixel = mMetresPerPixel;

		double mapx = mCentre.x + (x - mScreenWidth/2.0f) * metresPerPixel;
		double mapy = mCentre.y + (mScreenHeight/2.0f - y) * metresPerPixel;
		return new GridPoint(mapx, mapy);
	}

	GridRect getVisibleMapRect() {
		return mVisibleMapRect;
	}

	GridRect getVisibleMapRectWithScreenInsets(float insetx, float insety) {
		float mpp = mMetresPerPixel;
		return mVisibleMapRect.inset(insetx*mpp, insety*mpp);
	}

	GridRect getExpandedVisibleMapRect() {
		return getVisibleMapRectWithScreenInsets(-mScreenWidth/2.0f, -mScreenHeight/2.0f);
	}

	GridPoint getCenter() {
		return mCentre;
	}

	PointF displayPointFromGridPoint(GridPoint gp, PointF displayPointOut)
	{
		double mapCenterX = mCentre.x;
		double mapCenterY = mCentre.y;
		float metresPerPixel = mMetresPerPixel;

		float xPixels = (float)(gp.x-mapCenterX)/metresPerPixel;
		float yPixels = (float)(gp.y-mapCenterY)/metresPerPixel;

		displayPointOut.x = xPixels;
		displayPointOut.y = yPixels;
		return displayPointOut;
	}

	float getMetresPerPixel()
	{
		return mMetresPerPixel;
	}
}
