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

import java.nio.FloatBuffer;

import android.graphics.PointF;
import android.location.Location;

public class LocationOverlay {
	Circle mAccuracyCircle;
	GLMapRenderer mMap;
	Marker mChevronMarker;
	
	LocationOverlay(GLMapRenderer map)
	{
		mMap = map;
		CircleOptions options = new CircleOptions().fillColor(0x408080FF).strokeColor(0xFF8080FF).strokeWidth(5).visible(false);
		mAccuracyCircle = new Circle(options, map);
		
		mChevronMarker =  new Marker(new MarkerOptions().anchor(0.5f, 0.5f), Images.getLocationArrow(null), mMap);
	}
	

	
	void setLocation(Location location)
	{
		GridPoint center = mMap.getMyGridPoint();
//		int basex =  437500;
//		int basey = 115500;
//		center = new GridPoint(basex,basey);

		mAccuracyCircle.setCenter(center);
		mAccuracyCircle.setRadius(location.getAccuracy());
		mAccuracyCircle.setVisible(true);
		mChevronMarker.setBearing(location.getBearing());
		mChevronMarker.setGridPoint(center);
	}
	
	
	void glDraw(float[] orthoMatrix, float[] tempMatrix, PointF rTempPoint, FloatBuffer rTempFloatBuffer, float metresPerPixel) 
	{
		mAccuracyCircle.glDraw(rTempPoint, rTempFloatBuffer);
		
		mMap.setProgram(mMap.shaderProgram);
		mChevronMarker.glDraw(orthoMatrix, tempMatrix, mMap.getGLImageCache(),  rTempPoint);
	}

}
