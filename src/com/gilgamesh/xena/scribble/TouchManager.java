package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;
import com.onyx.android.sdk.data.note.TouchPoint;

import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import androidx.preference.PreferenceManager;

public class TouchManager implements View.OnTouchListener {
	static private final float FLICK_MOVE_RATIO = 0.85f;
	static private final float TOUCH_BORDER_INVALID_RATIO = 0f;
	static private final int FLICK_LOWER_BOUND_MS = 10;
	static private final int FLICK_UPPER_BOUND_MS = 220;
	static private final float FLICK_OFFSET_THRESHOLD_DP = 10;
	static private final float FLICK_OFFSET_THRESHOLD_PX
		= TouchManager.FLICK_OFFSET_THRESHOLD_DP * XenaApplication.DPI / 160;
	static private final float PAN_DISTANCE_THRESHOLD_DP = 10;
	static private final float PAN_DISTANCE_THRESHOLD_PX
		= TouchManager.PAN_DISTANCE_THRESHOLD_DP * XenaApplication.DPI / 160;
	static private final float ZOOM_DISTANCE_BOUND_DP = 25;
	static private final float ZOOM_DISTANCE_BOUND_PX
		= TouchManager.ZOOM_DISTANCE_BOUND_DP * XenaApplication.DPI / 160;
	static private final int ZOOM_LOWER_BOUND_MS = 80;
	static private final int TAP_UPPER_BOUND_MS = 220;
	static private final int DOUBLE_TAP_UPPER_BOUND_MS = 360;
	static final int IGNORE_CHAIN_BOUND_MS = 250;
	static private final String SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE
		= "SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE";
	static float PALM_TOUCH_MAJOR_MINOR_THRESHOLD;

	private PointF previousPoint = new PointF();
	private long actionDownTimeMs = 0;
	private float actionDownMaxTouchMajorMinor = 0f;
	private long zoomDownTimeMs = 0;
	private float zoomBeginDistance;
	private boolean zoomDownConsumed = true;
	private int cMoveEvents;
	private long previousTapTimeMs = 0;
	long previousIgnoreChainTimeMs = 0;

	private ScribbleActivity scribbleActivity;
	private SharedPreferences sharedPreferences;

	public TouchManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;

		this.sharedPreferences
			= PreferenceManager.getDefaultSharedPreferences(scribbleActivity);
		TouchManager.PALM_TOUCH_MAJOR_MINOR_THRESHOLD
			= Float.parseFloat(this.sharedPreferences.getString(
				TouchManager.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE, "9999"));
	}

	public void updatePalmTouchThreshold(float newThreshold) {
		TouchManager.PALM_TOUCH_MAJOR_MINOR_THRESHOLD = newThreshold;

		SharedPreferences.Editor editor = this.sharedPreferences.edit();
		editor.putString(TouchManager.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE,
			String.valueOf(newThreshold));
		editor.commit();
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_2_DOWN:
			case MotionEvent.ACTION_POINTER_2_UP:
				return this.onTouchInner(event.getAction(), event.getX(), event.getY(),
					event.getX(1), event.getY(1), event.getTouchMajor(1),
					event.getTouchMinor(1));
			default:
				return this.onTouchInner(event.getAction(), event.getX(), event.getY(),
					0, 0, event.getTouchMajor(), event.getTouchMinor());
		}
	}

	@SuppressWarnings("deprecation")
	public boolean onTouchInner(int eventAction, float eventX0, float eventY0,
		float eventX1, float eventY1, float touchMajor, float touchMinor) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.DEFAULT
			&& (this.scribbleActivity.isDrawing || this.scribbleActivity.isErasing
				|| this.scribbleActivity.isInputCooldown)) {
			return false;
		}

		PointF touchPoint = new PointF(eventX0, eventY0);
		long currentTimeMs = System.currentTimeMillis();
		long eventDurationMs = (currentTimeMs - this.actionDownTimeMs);

		switch (eventAction) {
			case MotionEvent.ACTION_DOWN:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.penManager.onBeginRawDrawing(false,
						new TouchPoint(touchPoint.x, touchPoint.y));
					break;
				}

				XenaApplication.log("ScribbleActivity::onTouch:DOWN " + touchPoint);

				if (currentTimeMs
					- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				// Sometimes, this fires slightly before a draw/erase event. The
				// draw/erase event will cancel panning in that case.

				if (this.scribbleActivity.isPanning) {
					XenaApplication.log("ScribbleActivity::onTouch:RESET " + touchPoint);
				} else {
					RectF bounds
						= new RectF(
							this.scribbleActivity.scribbleView.getWidth()
								* TouchManager.TOUCH_BORDER_INVALID_RATIO,
							this.scribbleActivity.scribbleView.getHeight()
								* TouchManager.TOUCH_BORDER_INVALID_RATIO,
							this.scribbleActivity.scribbleView.getWidth()
								* (1 - TouchManager.TOUCH_BORDER_INVALID_RATIO),
							this.scribbleActivity.scribbleView.getHeight()
								* (1 - TouchManager.TOUCH_BORDER_INVALID_RATIO));
					if (bounds.contains(touchPoint.x, touchPoint.y)) {
						// Only count actions that don't start near the border.
						this.scribbleActivity.isPanning = true;
					} else {
						XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
						this.previousIgnoreChainTimeMs = currentTimeMs;
					}
				}

				if (this.scribbleActivity.isPanning) {
					this.scribbleActivity.panBeginOffset
						= this.scribbleActivity.pathManager.getViewportOffset();
					this.previousPoint.x = touchPoint.x;
					this.previousPoint.y = touchPoint.y;
					this.actionDownTimeMs = currentTimeMs;

					this.cMoveEvents = 0;
					this.actionDownMaxTouchMajorMinor = Math.max(touchMajor, touchMinor);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.penManager.onRawDrawingTouchPointMoveReceived(
						new TouchPoint(touchPoint.x, touchPoint.y));
					break;
				}

				if (currentTimeMs
					- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					// XenaApplication.log(
					// "ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				if (!this.scribbleActivity.isPanning) {
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
					this.previousIgnoreChainTimeMs = currentTimeMs;
					break;
				}

				// Don't process until we exit flick range.
				if (eventDurationMs <= TouchManager.FLICK_UPPER_BOUND_MS) {
					break;
				}

				this.cMoveEvents++;
				this.actionDownMaxTouchMajorMinor
					= Math.max(this.actionDownMaxTouchMajorMinor,
						Math.max(touchMajor, touchMinor));

				if (this.actionDownMaxTouchMajorMinor >= TouchManager.PALM_TOUCH_MAJOR_MINOR_THRESHOLD
					|| !this.zoomDownConsumed) {
					break;
				}

			{
				PointF newOffset
					= new PointF(
						this.scribbleActivity.pathManager.getViewportOffset().x
							+ (touchPoint.x - this.previousPoint.x)
								/ this.scribbleActivity.pathManager.getZoomScale(),
						this.scribbleActivity.pathManager.getViewportOffset().y
							+ (touchPoint.y - this.previousPoint.y)
								/ this.scribbleActivity.pathManager.getZoomScale());
				this.scribbleActivity.pathManager.setViewportOffset(newOffset);

				// Do not update text view while dragging.
				// this.scribbleActivity.updateTextViewStatus();
			}
				this.previousPoint.x = touchPoint.x;
				this.previousPoint.y = touchPoint.y;

				// XenaApplication.log( "ScribbleActivity::onTouch:MOVE "
				// + pathManager.getViewportOffset());

				if (this.scribbleActivity.isRedrawing) {
					this.scribbleActivity.redraw();
				} else {
					this.scribbleActivity.drawBitmapToView(false, true);
				}

				// No need to reset raw input capture here, because it is assumed that
				// the Boox canvas is clean.
				break;
			case MotionEvent.ACTION_UP:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.penManager.onEndRawDrawing(false,
						new TouchPoint(touchPoint.x, touchPoint.y));
					break;
				}

				this.actionDownMaxTouchMajorMinor
					= Math.max(this.actionDownMaxTouchMajorMinor,
						Math.max(touchMajor, touchMinor));
				XenaApplication.log("ScribbleActivity::onTouch:UP " + touchPoint + " "
					+ this.cMoveEvents + " " + this.actionDownMaxTouchMajorMinor);

				if (currentTimeMs
					- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				if (this.actionDownMaxTouchMajorMinor >= TouchManager.PALM_TOUCH_MAJOR_MINOR_THRESHOLD) {
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
					this.previousIgnoreChainTimeMs = currentTimeMs;
					break;
				}

				if (!this.scribbleActivity.isPanning
					|| this.zoomDownConsumed == false) {
					this.zoomDownConsumed = true;
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
					this.previousIgnoreChainTimeMs = currentTimeMs;
					break;
				}

				this.zoomDownConsumed = true;

				// Note: ACTION_UP is not guaranteed to fire after ACTION_DOWN.
				this.scribbleActivity.isPanning = false;

				// Detect flicks.
				float flickOffsetX
					= this.scribbleActivity.pathManager.getViewportOffset().x
						+ touchPoint.x - this.previousPoint.x
						- this.scribbleActivity.panBeginOffset.x;
				float flickOffsetY
					= this.scribbleActivity.pathManager.getViewportOffset().y
						+ touchPoint.y - this.previousPoint.y
						- this.scribbleActivity.panBeginOffset.y;
				if (eventDurationMs >= TouchManager.FLICK_LOWER_BOUND_MS
					&& eventDurationMs <= TouchManager.FLICK_UPPER_BOUND_MS
					&& Math.abs(flickOffsetY) >= TouchManager.FLICK_OFFSET_THRESHOLD_PX) {
					XenaApplication.log("ScribbleActivity::onTouch:FLICK");

					// Determine flick direction.
					int direction = flickOffsetY > 0 ? 1 : -1;

					this.scribbleActivity.pathManager.setViewportOffset(
						new PointF(this.scribbleActivity.panBeginOffset.x,
							this.scribbleActivity.panBeginOffset.y
								+ direction * this.scribbleActivity.scribbleView.getHeight()
									* TouchManager.FLICK_MOVE_RATIO
									/ this.scribbleActivity.pathManager.getZoomScale()));
					this.scribbleActivity.updateTextViewStatus();
					this.scribbleActivity.svgFileScribe.debounceSave(
						this.scribbleActivity, this.scribbleActivity.svgUri,
						this.scribbleActivity.pathManager);
					this.scribbleActivity.penManager.cancelRedraw();
					this.scribbleActivity.redraw();
				} else {
					// If not panned, treat as a tap.
					if (eventDurationMs < TouchManager.TAP_UPPER_BOUND_MS
						&& this.cMoveEvents == 0) {
						if (currentTimeMs
							- this.previousTapTimeMs <= TouchManager.DOUBLE_TAP_UPPER_BOUND_MS) {
							XenaApplication.log("TouchManager::onTouch:DOUBLE_TAP.");
							this.scribbleActivity.isPenEraseMode
								= !this.scribbleActivity.isPenEraseMode;
							this.scribbleActivity.drawEraseToggle
								.setBackgroundResource(this.scribbleActivity.isPenEraseMode
									? R.drawable.solid_empty
									: R.drawable.dotted_empty);
							this.scribbleActivity.penManager.cancelRedraw();
							this.scribbleActivity.redraw();
							this.previousTapTimeMs = 0;
						} else {
							XenaApplication.log("TouchManager::onTouch:TAP.");
							this.previousTapTimeMs = currentTimeMs;
						}
					} else if (eventDurationMs > TouchManager.FLICK_UPPER_BOUND_MS
						&& Math.sqrt(flickOffsetX * flickOffsetX
							+ flickOffsetY * flickOffsetY) >= PAN_DISTANCE_THRESHOLD_PX) {
						XenaApplication.log("ScribbleActivity::onTouch:PAN");

						PointF newOffset
							= new PointF(
								this.scribbleActivity.pathManager.getViewportOffset().x
									+ (touchPoint.x - this.previousPoint.x)
										/ this.scribbleActivity.pathManager.getZoomScale(),
								this.scribbleActivity.pathManager.getViewportOffset().y
									+ (touchPoint.y - this.previousPoint.y)
										/ this.scribbleActivity.pathManager.getZoomScale());
						this.scribbleActivity.pathManager.setViewportOffset(newOffset);

						this.scribbleActivity.updateTextViewStatus();
						this.scribbleActivity.svgFileScribe.debounceSave(
							this.scribbleActivity, this.scribbleActivity.svgUri,
							this.scribbleActivity.pathManager);
						this.scribbleActivity.penManager.cancelRedraw();
						this.scribbleActivity.redraw();
					} else {
						XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
						this.previousIgnoreChainTimeMs = currentTimeMs;
					}
				}
				break;
			// Deprecated events may still be used by Boox API.
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_2_DOWN:
				XenaApplication.log("ScribbleActivity::onTouch:ACTION_POINTER_DOWN "
					+ this.zoomBeginDistance);
				this.actionDownMaxTouchMajorMinor
					= Math.max(this.actionDownMaxTouchMajorMinor,
						Math.max(touchMajor, touchMinor));

				if (currentTimeMs
					- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				this.zoomBeginDistance
					= Geometry.distance(touchPoint, new PointF(eventX1, eventY1));
				this.zoomDownTimeMs = currentTimeMs;
				this.zoomDownConsumed = false;
				break;
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_2_UP:
				float zoomEndDistance
					= Geometry.distance(touchPoint, new PointF(eventX1, eventY1));
				this.actionDownMaxTouchMajorMinor
					= Math.max(this.actionDownMaxTouchMajorMinor,
						Math.max(touchMajor, touchMinor));
				XenaApplication.log("ScribbleActivity::onTouch:ACTION_POINTER_UP "
					+ zoomEndDistance + " " + this.actionDownMaxTouchMajorMinor);

				boolean wasConsumed = this.zoomDownConsumed;
				this.zoomDownConsumed = true;

				if (currentTimeMs
					- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				if (currentTimeMs
					- this.zoomDownTimeMs < TouchManager.ZOOM_LOWER_BOUND_MS
					|| wasConsumed
					|| this.actionDownMaxTouchMajorMinor >= TouchManager.PALM_TOUCH_MAJOR_MINOR_THRESHOLD) {
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
					this.previousIgnoreChainTimeMs = currentTimeMs;
					break;
				}

				boolean zoomChanged = false;
				if (this.zoomBeginDistance
					- zoomEndDistance >= TouchManager.ZOOM_DISTANCE_BOUND_PX) {
					this.scribbleActivity.pathManager.zoomOut();
					zoomChanged = true;
				} else if (zoomEndDistance
					- this.zoomBeginDistance >= TouchManager.ZOOM_DISTANCE_BOUND_PX) {
					this.scribbleActivity.pathManager.zoomIn();
					zoomChanged = true;
				}

				if (zoomChanged) {
					XenaApplication.log("ScribbleActivity::onTouch:ZOOM");
					this.scribbleActivity.setStrokeWidthScale(
						this.scribbleActivity.pathManager.getZoomScale());
					this.scribbleActivity.updateTextViewStatus();
					this.scribbleActivity.penManager.cancelRedraw();
					this.scribbleActivity.redraw();
					this.scribbleActivity.svgFileScribe.debounceSave(
						this.scribbleActivity, this.scribbleActivity.svgUri,
						this.scribbleActivity.pathManager);
				} else {
					XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
					this.previousIgnoreChainTimeMs = currentTimeMs;
				}

				// Cancel panning by remaining finger.
				this.scribbleActivity.isPanning = false;
				break;
			default:
				XenaApplication.log("ScribbleActivity::onTouch:OTHER " + eventAction);
				XenaApplication.log("ScribbleActivity::onTouch:IGNORE");
				this.previousIgnoreChainTimeMs = currentTimeMs;
				break;
		}
		return true;
	}
}
