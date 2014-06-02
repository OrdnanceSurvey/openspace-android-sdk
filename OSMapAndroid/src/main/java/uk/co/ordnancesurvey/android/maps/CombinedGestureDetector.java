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
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

abstract class CombinedGestureDetector implements View.OnTouchListener {
	//private static final String TAG = "CombinedGestureDetector";

	public interface DragListener {
		public Object onDragBegin(MotionEvent e);
		public void onDrag(MotionEvent e, Object dragObject);
		public void onDragEnd(MotionEvent e, Object dragObject);
		public void onDragCancel(MotionEvent e, Object dragObject);
	}
	private final GestureDetector mGestureDetector;
	private final ScaleGestureDetector mScaleGestureDetector;
	private final float mTouchSlopSq;

	private boolean consumingScaleEvents;

	private View mCurrentView;
	private MotionEvent mCurrentEvent;
	private boolean mCurrentEventCalledOnScaleBegin;

	private boolean mIgnoreFurtherGestures;
	private boolean mTwoFingerTapPossible;
	private boolean mScaleAlreadyHasTouchSlop;

	private final DragListener mDragListener;
	private boolean mDragStarted;
	private float mDragInitialX, mDragInitialY;
	private Object mDragObject;

	private float mScaleInitialSpan;
	private float mPrevScaleFocusX;
	private float mPrevScaleFocusY;

	public CombinedGestureDetector(Context context, DragListener dragListener) {
		GestureListener listener = new GestureListener();
		mGestureDetector = new GestureDetector(context, listener);
		mScaleGestureDetector = new ScaleGestureDetector(context, listener);

		mGestureDetector.setIsLongpressEnabled(true);
		mGestureDetector.setOnDoubleTapListener(listener);

		float touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mTouchSlopSq = touchSlop*touchSlop;

		mDragListener = dragListener;
	}

	/**
	* Combined scroll/zoom/fling method.
	*/
	protected abstract void onScroll(float dx, float dy, float dScale, float scaleOffsetX, float scaleOffsetY, float flingVX, float flingVY, long eventTime);

	protected abstract boolean onSingleTapConfirmed(MotionEvent e);
	protected abstract void onTwoFingerTap(MotionEvent e);
	protected abstract boolean onDoubleTap(MotionEvent e, float offsetX, float offsetY);

	@Override
	public boolean onTouch(View v, MotionEvent e) {
		mCurrentView = v;
		if (BuildConfig.DEBUG)
		{
			// Only save this on debug builds. It's potentially dangerous, since the event can get recycled!
			mCurrentEvent = e;
		}
		boolean consumedGestureEvent = mGestureDetector.onTouchEvent(e);
		mCurrentEventCalledOnScaleBegin = false;
		if (consumingScaleEvents)
		{
			consumingScaleEvents = mScaleGestureDetector.onTouchEvent(e);
		}
		mCurrentView = null;
		mCurrentEvent = null;

		int action = e.getActionMasked();
		boolean actionIsSecondaryDown = (action == MotionEvent.ACTION_POINTER_DOWN);
		boolean actionIsFinalUp = (action == MotionEvent.ACTION_UP);
		boolean actionIsCancel = action == MotionEvent.ACTION_CANCEL;
		boolean actionIsLast = actionIsFinalUp || actionIsCancel;
		assert !actionIsFinalUp || e.getPointerCount()==1 : "actionIsFinalUp should imply only one pointer";

		if (actionIsSecondaryDown && e.getPointerCount() == 2)
		{
			// OS-44: On some Android versions, ScaleGestureDetector already applies touch slop.
			// We detect this by the lack of an onScaleBegin() callback on a secondary touch down.
			// On such a device, set mTwoFingerTapPossible here and clear it on onScaleBegin().
			boolean gotScaleBegin = mCurrentEventCalledOnScaleBegin;
			mScaleAlreadyHasTouchSlop = !gotScaleBegin;
			if (!gotScaleBegin)
			{
				mTwoFingerTapPossible = true;
			}
		}

		boolean twoFingerTap = !mIgnoreFurtherGestures && mTwoFingerTapPossible && actionIsFinalUp && e.getEventTime()-e.getDownTime() < ViewConfiguration.getDoubleTapTimeout();
		//Log.v(TAG, String.format(Locale.ENGLISH, "%d isd=%b ifg=%b ttp=%b ifu=%b tt=%b", action, actionIsSecondaryDown, mIgnoreFurtherGestures, mTwoFingerTapPossible, actionIsFinalUp, twoFingerTap));
		if (twoFingerTap) {
			// TODO: This should cancel other gestures (e.g. double tap).
			mTwoFingerTapPossible = false;
			onTwoFingerTap(e);
		}

		boolean dragging = (mDragObject != null);
		if (dragging) {
			if (actionIsFinalUp) {
				mDragListener.onDragEnd(e, mDragObject);
				mDragObject = null;
			} else if (actionIsCancel) {
				mDragListener.onDragCancel(e, mDragObject);
				mDragObject = null;
			} else {
				if (!mDragStarted) {
					float dx = e.getX() - mDragInitialX;
					float dy = e.getY() - mDragInitialY;
					if (dx*dx+dy*dy > mTouchSlopSq) {
						mDragStarted = true;
					}
				}
				if (mDragStarted) {
					mDragListener.onDrag(e, mDragObject);
				}
			}
		}

		if (actionIsLast)
		{
			// On the final "up" or a cancel, stop ignoring further events.
			mIgnoreFurtherGestures = false;
		}

		return consumingScaleEvents || consumedGestureEvent || twoFingerTap;
	}

	class GestureListener implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {
		private boolean mScaleStarted;

		@Override
		public boolean onDown(MotionEvent e) {
			if (mIgnoreFurtherGestures) {
				return false;
			}
			// On the initial touch down,
			//  - Start consuming scale events.
			//  - Cancel any pending two-finger tap.
			//  - Cancel any fling.
			consumingScaleEvents = true;
			mTwoFingerTapPossible = false;
			CombinedGestureDetector.this.onScroll(0, 0, 1, 0, 0, 0, 0, e.getEventTime());
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			if (mIgnoreFurtherGestures) {
				return false;
			}
			assert !mScaleStarted;
			CombinedGestureDetector.this.onScroll(0, 0, 1, 0, 0, -velocityX, -velocityY, e2.getEventTime());
			return true;
		}
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (mIgnoreFurtherGestures) {
				return false;
			}
			if (mScaleStarted) {
				// This appears to happen since Android 4.1. Observed on
				//   Galaxy S III (GT-I9300, Android 4.1.2).
				//   Nexus 7 (Android 4.2.x)
				// Not observed on
				//   HTC One (Android 4.0.x)
				//   Galaxy S II (Android 4.0.x)
				if (BuildConfig.DEBUG)
				{
					assert Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
					//DebugHelpers.logAndroidBuild();
				}
				// Don't scroll; it's already handled by onScale().
				return false;
			}
			CombinedGestureDetector.this.onScroll(distanceX, distanceY, 1, 0, 0, 0, 0, e2.getEventTime());
			return true;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mCurrentEventCalledOnScaleBegin = true;
			if (mIgnoreFurtherGestures) {
				return false;
			}
			mScaleStarted = true;

			// OS-44: On some Android versions, ScaleGestureDetector already applies touch slop.
			if (BuildConfig.DEBUG) {
				// On such devices, onScaleBegin() should happen on ACTION_MOVE, not ACTION_POINTER_DOWN
				assert mScaleAlreadyHasTouchSlop == (mCurrentEvent.getActionMasked() == MotionEvent.ACTION_MOVE);
			}
			// If ScaleGestureDetector already applies touch slop, then we must have exceeded the threshold. Two-finger tap is no longer possible.
			mTwoFingerTapPossible = (mScaleAlreadyHasTouchSlop ? false : true);

			float x = detector.getFocusX();
			float y = detector.getFocusY();
			mPrevScaleFocusX = x;
			mPrevScaleFocusY = y;
			mScaleInitialSpan = detector.getCurrentSpan();
			return true;
		}
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if (mIgnoreFurtherGestures) {
				return false;
			}
			float x = detector.getFocusX();
			float y = detector.getFocusY();
			float dX = x-mPrevScaleFocusX;
			float dY = y-mPrevScaleFocusY;
			float dScale;
			if (mTwoFingerTapPossible)
			{
				float currentSpan = detector.getCurrentSpan();
				float dSpan = (currentSpan-mScaleInitialSpan);
				float dSq = dSpan*dSpan + dX*dX + dY*dY;
				if (dSq < mTouchSlopSq)
				{
					return false;
				}

				mTwoFingerTapPossible = false;
				dScale = currentSpan/mScaleInitialSpan;
			}
			else
			{
				dScale = detector.getScaleFactor();
			}
			mPrevScaleFocusX = x;
			mPrevScaleFocusY = y;

			View v = mCurrentView;
			float scaleOffsetX = x-v.getWidth()/2;
			float scaleOffsetY = y-v.getHeight()/2;

			CombinedGestureDetector.this.onScroll(-dX, -dY, dScale, scaleOffsetX, scaleOffsetY, 0, 0, detector.getEventTime());
			return true;
		}
		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			mScaleStarted = false;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (mIgnoreFurtherGestures) {
				return false;
			}
			return CombinedGestureDetector.this.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (mIgnoreFurtherGestures) {
				return false;
			}
			float x = e.getX();
			float y = e.getY();
			View v = mCurrentView;
			float scaleOffsetX = x-v.getWidth()/2;
			float scaleOffsetY = y-v.getHeight()/2;
			boolean consumed = CombinedGestureDetector.this.onDoubleTap(e, scaleOffsetX, scaleOffsetY);
			mIgnoreFurtherGestures = consumed;
			return consumed;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (mIgnoreFurtherGestures) {
				return;
			}
			Object dragObject = mDragListener.onDragBegin(e);
			mDragObject = dragObject;
			mDragStarted = false;
			mDragInitialX = e.getX();
			mDragInitialY = e.getY();
			mIgnoreFurtherGestures = (dragObject != null);
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			return false;
		}
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}
		@Override
		public void onShowPress(MotionEvent e) {
		}
	}

}
