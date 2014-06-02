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

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.View.OnTouchListener;

/***
* Scrolling and zooming.
*
* Unused. Obsoleted by CombinedGestureDetector.
* We might want to combine GestureDetector and ScaleGestureDetector and also add support for two-finger taps.
*/
abstract class AbstractScrollController implements OnTouchListener {
	private int numberOfTouches = 0;
	private float prevX, prevY, prevRadius;

	private VelocityTracker mVelocityTracker;
	private final float mMinFlingVelocitySq;
	private final float mMaxFlingVelocity;

	public AbstractScrollController(Context context)
	{
		ViewConfiguration configuration = ViewConfiguration.get(context);
		float minFlingVelocity = configuration.getScaledMinimumFlingVelocity();
		mMinFlingVelocitySq = minFlingVelocity*minFlingVelocity;
		mMaxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
	}

	protected abstract void onScroll(float dx, float dy, float dScale, float flingVX, float flingVY, long eventTime);

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final int action = event.getActionMasked();
		final int pointerCount = event.getPointerCount();

		// The touch which went down or up.
		int changedIndex = -1;
		// Whether the change is down or up (if there is a change)
		boolean changeIsUp = false;

		switch (action)
		{
		case MotionEvent.ACTION_DOWN:
			changedIndex = 0;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			changedIndex = event.getActionIndex();
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEvent.ACTION_UP:
			assert pointerCount == 1;
			changedIndex = 0;
			changeIsUp = true;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			changedIndex = event.getActionIndex();
			changeIsUp = true;
			break;
		case MotionEvent.ACTION_CANCEL:
			onScroll(0, 0, 1, 0, 0, 0);
			numberOfTouches = 0;
			if (mVelocityTracker != null)
			{
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			return true;
		default:
			return false;
		}

		if (mVelocityTracker == null)
		{
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(event);

		int flingVX = 0, flingVY = 0;
		if (numberOfTouches == 1 && changedIndex == 0 && changeIsUp)
		{
			mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
			float vX = mVelocityTracker.getXVelocity();
			float vY = mVelocityTracker.getYVelocity();
			mVelocityTracker.recycle();
			mVelocityTracker = null;

			float vSq = vX * vX + vY * vY;
			if (vSq > mMinFlingVelocitySq)
			{
				flingVX = Math.round(-vX);
				flingVY = Math.round(-vY);
			}
		}

		float sX = 0, sY = 0, sXX = 0, sYY = 0;
		float changedX = 0, changedY = 0;
		int count = pointerCount;
		for (int i = 0; i < count; i++) {
			float x = event.getX(i);
			float y = event.getY(i);
			sX += x;
			sY += y;
			sXX += x*x;
			sYY += y*y;
			if (i == changedIndex)
			{
				changedX = x;
				changedY = y;
			}
		}

		// "Old" means the touch state before accounting for the current touch down/up, if any.
		// "New" means the touch state after accounting for the current touch down/up.
		float oldAvgX;
		float oldAvgY;
		float oldRadius;
		float newAvgX;
		float newAvgY;
		float newRadius;
		int newCount = count;

		// For ACTION_MOVE (i.e. most of the time), old and new states are the same.
		// For other actions, at least one of the states includes all touches.
		// Set both old and new, and fixup in the next step.
		{
			float avgX = sX/count;
			float avgY = sY/count;
			float varX = sXX/count - avgX*avgX;
			float varY = sYY/count - avgY*avgY;
			float radius = (float)Math.sqrt(varX+varY);
			oldAvgX = newAvgX = avgX;
			oldAvgY = newAvgY = avgY;
			oldRadius = newRadius = radius;
		}

		if (changedIndex >= 0)
		{
			// If a touch went down or up, then subtract its contribution to the stats.
			// TODO: This is numerically horrible but seems to work okay.
			sX -= changedX;
			sY -= changedY;
			sXX -= changedX*changedX;
			sYY -= changedY*changedY;
			count -= 1;

			// Now calculate the stats again.
			float avgX = sX/count;
			float avgY = sY/count;
			float varX = sXX/count - avgX*avgX;
			float varY = sYY/count - avgY*avgY;
			float radius = (float)Math.sqrt(varX+varY);

			// Fixup "old" or "new" as needed. "new" also has a count.
			if (changeIsUp)
			{
				newAvgX = avgX;
				newAvgY = avgY;
				newRadius = radius;
				newCount = count;
			}
			else
			{
				oldAvgX = avgX;
				oldAvgY = avgY;
				oldRadius = radius;
			}
		}

		// Apparently some touchscreens can interpret a single touch as two touches near each other. Interpret this as a single touch.
		int adjCount = (newCount > 1 && newRadius < 5) ? 1 : newCount;

		// If we are currently dragging/pinching, report changes based on the "old" state (i.e. before processing a second finger or the touch-up).
		float scrollDX = 0;
		float scrollDY = 0;
		float scrollDScale = 1;
		long eventTime = 0;
		
		if (numberOfTouches > 0)
		{
			eventTime = event.getEventTime();
			float dx = oldAvgX-prevX;
			float dy = oldAvgY-prevY;
			switch (numberOfTouches)
			{
			case 0:
				assert false;
			case 1:
				scrollDX = -dx;
				scrollDY = -dy;
				break;
			default:
				float dscale = oldRadius/prevRadius;
				float viewCenterX = v.getWidth()/2;
				float viewCenterY = v.getHeight()/2;
				dx += (oldAvgX-viewCenterX)*(1-dscale);
				dy += (oldAvgY-viewCenterY)*(1-dscale);
				scrollDX = -dx;
				scrollDY = -dy;
				scrollDScale = dscale;
				break;
			}
		}

		numberOfTouches = adjCount;
		prevX = newAvgX;
		prevY = newAvgY;
		prevRadius = newRadius;

		onScroll(scrollDX, scrollDY, scrollDScale, flingVX, flingVY, eventTime);

		return true;
	}

}
