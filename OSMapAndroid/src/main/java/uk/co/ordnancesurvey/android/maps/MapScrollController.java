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

import java.util.Arrays;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Scroller;

final class MapScrollController extends CombinedGestureDetector {
	interface ScrollListener {
		public void onScrollScaleFling(MapScrollController detector);
		public boolean onSingleTapConfirmed(MotionEvent e);
	}

	private static final String TAG = "MapScrollController";

	// Set this to true to enable constant-speed scrolling useful for profiling tile-fetching/drawing.
	private static final boolean TEST_CONSTANT_SCROLL = BuildConfig.DEBUG && false;
	private static final boolean TEST_ZOOMING = BuildConfig.DEBUG && false;

	private final ScrollListener listener;

	private final Scroller mScroller;
	private int mFlingPrevX, mFlingPrevY;

	private final Zoomer mZoomer;
	private float mZoomFocusOffsetX, mZoomFocusOffsetY;
	private GridPoint mZoomStartCenter, mZoomFinalCenter;

	// Array of zoom scales. Should only be used by zoomInStep() and zoomOutStep().
	private volatile float[] mZoomScales = new float[0];
	private float mMaximumMPP = Float.POSITIVE_INFINITY;

	private int mWidthPx, mHeightPx;
	private double mX = 437500;
	private double mY = 115500;
	private float mScale = 1;

	private final Context statContext;
	private long statLastEventTime;
	private int statEventCount;
	private int statFrameCount;
	private float statLagSumSq;
	private long statLastReportFrameTime;

	public MapScrollController(Context context, DragListener dragListener, ScrollListener aListener) {
		super(context, dragListener);
		listener = aListener;
		mScroller = new Scroller(context);
		mZoomer = new Zoomer(context);

		statContext = context;
	}

	public void setZoomScales(float[] zoomScales) {
		zoomScales = zoomScales.clone();
		Arrays.sort(zoomScales);
		float prevScale = Float.NEGATIVE_INFINITY;
		for (float scale : zoomScales) {
			if (!(0 < scale && scale < Float.POSITIVE_INFINITY)) {
				throw new IllegalArgumentException("Zoom scales must be finite positive numbers, got " + scale);
			}
			if (prevScale == scale) {
				throw new IllegalArgumentException("Got duplicate zoom scale: " + scale);
			}
			assert scale >= 0 && !Float.isNaN(scale) && !Float.isInfinite(scale) && prevScale < scale;
			prevScale = scale;
		}
		mZoomScales = zoomScales;
		
		// Check that mScale is still valid.
		if(mScale < zoomScales[0])
		{
			mScale = zoomScales[0];
		}
		if(mScale > zoomScales[zoomScales.length - 1])
		{
			mScale = zoomScales[zoomScales.length-1];
		}
	}

	public void setWidthHeight(int width, int height) {
		synchronized (this) {
			mWidthPx = width;
			mHeightPx = height;
			mMaximumMPP = Math.min(GridPoint.GRID_WIDTH/(float)width, GridPoint.GRID_HEIGHT/(float)height);
		}
		// This should not be necessary since the map needs to re-render on a size change.
		//listener.onScrollScaleFling(this);
	}

	public void zoomToScale(MotionEvent e, float scale, float offsetX, float offsetY) {
		synchronized (this) {
			if (BuildConfig.DEBUG && e != null) {
				statLastEventTime = e.getEventTime();
			}
			mScroller.forceFinished(true);
			mZoomer.forceFinished(true);

			if (scale != mScale) {
				mZoomFocusOffsetX = offsetX;
				mZoomFocusOffsetY = offsetY;
				mZoomStartCenter = null;
				mZoomFinalCenter = null;
				mZoomer.startZoom(mScale, scale, durationForZoom(mScale, scale, null, null));
			}
		}
		listener.onScrollScaleFling(this);
	}

	public void zoomToScale(MotionEvent e, float scale) {
		zoomToScale(e, scale, 0, 0);
	}

	public void zoomToCenterScale(MotionEvent e, GridPoint p, float scale, boolean animated) {
		synchronized (this) {
			if (BuildConfig.DEBUG && e != null) {
				statLastEventTime = e.getEventTime();
			}
			mScroller.forceFinished(true);
			mZoomer.forceFinished(true);

			mZoomStartCenter = new GridPoint(mX, mY);
			mZoomFinalCenter = p;
			mZoomer.startZoom(mScale, scale, animated ? durationForZoom(mScale, scale, mZoomStartCenter, mZoomFinalCenter) : 0);
		}
		listener.onScrollScaleFling(this);
	}

	private int durationForZoom(float startScale, float finalScale, GridPoint startCenter, GridPoint finalCenter) {
		final int ZOOM_MIN_DURATION = 250;
		final int ZOOM_MAX_DURATION = 2500;
		double absLogDiff = Math.abs(Math.log(finalScale/(double)startScale));
		double distPx = 0;
		if (startCenter != null && finalCenter != null)
		{
			double distM = startCenter.distanceTo(finalCenter);
			distPx = distM/Math.max(startScale,finalScale);
		}
		// TODO: Take the window size into account too.
		return Math.min(ZOOM_MAX_DURATION, ZOOM_MIN_DURATION + (int)(absLogDiff*400) + (int)distPx/2);
	}

	private float getFinalMPP() {
		synchronized (this) {
			if (!mZoomer.isFinished()) {
				return mZoomer.getFinalZoom();
			}
			return mScale;
		}
	}

	public boolean zoomInStep(MotionEvent e, float offsetX, float offsetY) {
		float currentFinalMPP = getFinalMPP();
		float[] zoomScales = mZoomScales;

		int index = Arrays.binarySearch(zoomScales, currentFinalMPP);
		// index is "the non-negative index of the element, or a negative index which is -index - 1 where the element would be inserted."
		int prevIndex = (index >= 0 ? index-1 : -(index+1)-1);
		assert prevIndex < zoomScales.length;
		if (prevIndex < 0)
		{
			return false;
		}

		float bestMPP = zoomScales[prevIndex];
		assert !(Float.isInfinite(bestMPP) || Float.isNaN(bestMPP));

		assert bestMPP < currentFinalMPP;

		zoomToScale(e, bestMPP, offsetX, offsetY);
		return true;
	}

	public boolean zoomOutStep(MotionEvent e) {
		float currentFinalMPP = getFinalMPP();
		float[] zoomScales = mZoomScales;
		float maxMPP = mMaximumMPP;

		int index = Arrays.binarySearch(zoomScales, currentFinalMPP);
		// index is "the non-negative index of the element, or a negative index which is -index - 1 where the element would be inserted."
		int nextIndex = (index >= 0 ? index+1 : -(index+1));
		assert nextIndex >= 0;
		boolean tooFar = (nextIndex >= zoomScales.length);

		float bestMPP = tooFar ? Float.POSITIVE_INFINITY : zoomScales[nextIndex];

		// If we're zooming out too far, limit it to the maxMPP (provided maxMPP is sensible).
		if (bestMPP > maxMPP && maxMPP >= 0) {
			assert !(Float.isInfinite(maxMPP) || Float.isNaN(maxMPP)) : "We checked (a > b && b >= 0) which should never be true if b is negative/infinite/NaN.";
			if (maxMPP > currentFinalMPP) {
				zoomToScale(e, maxMPP);
				return true;
			}
			return false;
		}
		if (Float.isInfinite(bestMPP))
		{
			// Can't zoom out any more and maxMPP is not set, so don't do anything.
			return false;
		}
		assert !(Float.isInfinite(bestMPP) || Float.isNaN(bestMPP));

		assert bestMPP > currentFinalMPP;

		zoomToScale(e, bestMPP);
		return true;
	}

	@Override
	protected boolean onSingleTapConfirmed(MotionEvent e) {
		return listener.onSingleTapConfirmed(e);
	}

	@Override
	protected boolean onDoubleTap(MotionEvent e, float offsetX, float offsetY) {
		// TODO: Do we want this to be overridable?
		//return listener.onDoubleTap(e, offsetX, offsetY);
		if (TEST_ZOOMING) {
			zoomToCenterScale(e, new GridPoint(437500, 115500), 0.875f, true);
			return true;
		}
		return zoomInStep(e, offsetX, offsetY);
	}

	@Override
	protected void onTwoFingerTap(MotionEvent e) {
		// TODO: Do we want this to be overridable?
		//listener.onTwoFingerTap(e);
		if (TEST_ZOOMING) {
			zoomToCenterScale(e, new GridPoint(223222, 727687), 448, true);
			return;
		}
		zoomOutStep(e);
	}

	@Override
	protected void onScroll(float dX, float dY, float dScale, float scaleOffsetX, float scaleOffsetY, float flingVX, float flingVY, long eventTime) {
		synchronized (this) {
			if (eventTime != 0)
			{
				statLastEventTime = eventTime;
			}

			dX += scaleOffsetX*(dScale-1);
			dY += scaleOffsetY*(dScale-1);

			mX += dX*mScale;
			mY += -dY*mScale;
			mScale /= dScale;

			// Stop any animated scroll (TODO: only if it is a fling or dX/dY are nonzero?)
			// This allows the user to "catch" a fling by pressing down on the screen.
			mScroller.forceFinished(true);

			if (dScale != 1)
			{
				// Only stop an animated zoom if the user is pinching.
				// Repeated double/two-finger taps should not cancel a pending gesture.
				// If the user starts scrolling, the zoom will continue, but that actually looks okay.
				mZoomer.forceFinished(true);
			}

			if (flingVX != 0 || flingVY != 0)
			{
				mFlingPrevX = 0;
				mFlingPrevY = 0;
				mScroller.fling(0, 0, Math.round(flingVX), -Math.round(flingVY), Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
			}
		}

		listener.onScrollScaleFling(this);
	}

	public void getScrollPosition(ScrollPosition ret, boolean updateFling)
	{
		double x, y;
		float scale;
		boolean animatingScroll = false;
		boolean animatingZoom = false;
		float animationStartScale = 0;
		float animationFinalScale = 0;
		synchronized (this) {
			scale = mScale;
			x = mX;
			y = mY;
			if (updateFling)
			{
				if (BuildConfig.DEBUG)
				{
					printRenderStats();
				}
				if (mScroller.computeScrollOffset())
				{
					int flingX = mScroller.getCurrX();
					int flingY = mScroller.getCurrY();
					x += (flingX-mFlingPrevX)*scale;
					y += (flingY-mFlingPrevY)*scale;
					mFlingPrevX = flingX;
					mFlingPrevY = flingY;

					animatingScroll = !mScroller.isFinished();
				}
				if (mZoomer.computeScrollOffset())
				{
					float prevScale = scale;
					scale = mZoomer.getCurrZoom();
					float dScale = prevScale/scale;

					if (mZoomStartCenter != null && mZoomFinalCenter != null) {
						double p = mZoomer.getProgress();
						x = (1-p)*mZoomStartCenter.x + p*mZoomFinalCenter.x;
						y = (1-p)*mZoomStartCenter.y + p*mZoomFinalCenter.y;
					} else {
						x += mZoomFocusOffsetX*(dScale-1)*scale;
						y -= mZoomFocusOffsetY*(dScale-1)*scale;
					}

					animatingZoom = !mZoomer.isFinished();
					if (animatingZoom)
					{
						animationStartScale = mZoomer.getStartZoom();
						animationFinalScale = mZoomer.getFinalZoom();
					}
				}

				if (scale > mMaximumMPP)
				{
					scale = mMaximumMPP;
				}
				x = clipScaled(x, GridPoint.GRID_WIDTH,  mWidthPx,  scale);
				y = clipScaled(y, GridPoint.GRID_HEIGHT, mHeightPx, scale);
				mScale = scale;
				mX = x;
				mY = y;

				if (TEST_CONSTANT_SCROLL)
				{
					// Debug code to do a constant-speed scroll, which makes profiling e.g. tile loading performance easier and more consistent across runs.
					// For simplicity, it does not write to x or ret.x; the change will be picked up on the next frame.
					mY += scale*30;
					animatingScroll = true;
				}
			}
		}
		ret.x = x;
		ret.y = y;
		ret.metresPerPixel = scale;
		ret.animatingScroll = animatingScroll;
		ret.animatingZoom = animatingZoom;
		ret.animationStartMetresPerPixel = animationStartScale;
		ret.animationFinalMetresPerPixel= animationFinalScale;
	}

	private double clipScaled(double posM, double sizeM, int sizePx, float mpp)
	{
		double min = sizePx/2*mpp;
		double max = sizeM - min;
		return Math.max(min, Math.min(max, posM));
	}

	private void printRenderStats()
	{
		long uptimeMillis = SystemClock.uptimeMillis();
		statFrameCount++;
		if (statLastEventTime != 0)
		{
			float tt = (float)(uptimeMillis - statLastEventTime);
			statLastEventTime = 0;
			statLagSumSq += tt*tt;
			statEventCount++;
		}
		long dt = uptimeMillis - statLastReportFrameTime;
		if (dt > 1000)
		{
			final float fps = (float)(statFrameCount*1000.0/dt);
			final float rmslatency = (float)Math.sqrt(statLagSumSq/statEventCount);
			statLastReportFrameTime = uptimeMillis;
			statLagSumSq = 0;
			statFrameCount = 0;
			statEventCount = 0;

			final Activity activity = (Activity)statContext;
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					String stats = String.format(Locale.ENGLISH, "%.3g fps, %.3g ms (rms)", fps, rmslatency);
					activity.setTitle(stats);
					Log.v(TAG, stats);
				}
			});
		}
	}


	static final class ScrollPosition {
		double x,y;
		public float metresPerPixel = 1;
		public boolean animatingScroll;
		public boolean animatingZoom;
		public float animationStartMetresPerPixel;
		public float animationFinalMetresPerPixel;
	}

	static class Zoomer extends Scroller {
		public Zoomer(Context context) {
			super(context);
		}
		// Maximum and minimum integer steps.
		private static final int STEP_INITIAL_INT = Integer.MAX_VALUE;
		private static final int STEP_FINAL_INT = 0;

		private static final double STEP_INITIAL = STEP_INITIAL_INT;
		private static final double STEP_FINAL = STEP_FINAL_INT;

		private double mZoomLogInitial;
		private double mZoomLogFinal;

		public final float getCurrZoom() {
			// When finished, revProgress == 0, so logZoom will be be mZoomLogFinal.
			double logZoom = mZoomLogFinal + getRevLogProgress()*(mZoomLogInitial-mZoomLogFinal);
			// finished -> logZoom == mZoomLogFinal
			assert !isFinished() || logZoom == mZoomLogFinal;
			return (float)Math.exp(logZoom);
		}

		/**
		* Returns the "reverse logarithmic progress", i.e. 0 when the animation is finished.
		* The animation happens in this scale.
		*/
		private final double getRevLogProgress() {
			double step = getCurrX();
			double logProgress = (step-STEP_FINAL)/(STEP_INITIAL-STEP_FINAL);
			return logProgress;
		}

		/**
		* Returns the (non-logarithmic) progress, i.e. such that 
		*   getCurrZoom() ~= (1-p)*getStartZoom() + p*getFinalZoom()
		* This is suitable for interpolating scroll position.
		*/
		public final double getProgress() {
			double logProgress = 1-getRevLogProgress();
			double progress;
			double logDiff = mZoomLogFinal-mZoomLogInitial;
			// Avoid numerical instability when logDiff is very small:
			//   If logDiff is small, expm1() returns its argument and the formula is approximately (logProgress*logDiff)/logDiff ~= logProgress
			//   This does not lose significant precision until logDiff is zero or a denorm, so check abs(logDiff) >= MIN_NORMAL
			// Alternatively, we could check abs(logDiff) >= macheps which is approximately the threshold below which expm1() returns its argument.
			// However, Java doesn't give us a helpful symbolic constant and the above check is sufficient.
			if (Math.abs(logDiff) >= Double.MIN_NORMAL) {
				progress = Math.expm1(logProgress*logDiff)/Math.expm1(logDiff);
			} else {
				progress = logProgress;
			}
			return progress;
		}

		public final float getStartZoom() {
			return (float)Math.exp(mZoomLogInitial);
		}

		public final float getFinalZoom() {
			return (float)Math.exp(mZoomLogFinal);
		}

		public void startZoom(float initialZoom, float finalZoom, int duration) {
			mZoomLogInitial = Math.log(initialZoom);
			mZoomLogFinal = Math.log(finalZoom);
			assert initialZoom == getStartZoom() && finalZoom == getFinalZoom() : "Doubles should be accurate enough for f == (float)exp(log((double)f)) for positive f";
			startScroll(STEP_INITIAL_INT, 0, STEP_FINAL_INT-STEP_INITIAL_INT, 0, duration);
		}
	}
}
