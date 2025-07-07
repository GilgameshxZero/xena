package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;
import com.onyx.android.sdk.data.note.TouchPoint;

import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class TouchManager implements View.OnTouchListener {
	static private final float FLICK_MOVE_RATIO = 0.8f;
	static private final float TOUCH_BORDER_INVALID_RATIO = 0f;
	static private final int FLICK_LOWER_BOUND_MS = 80;
	static private final int FLICK_UPPER_BOUND_MS = 260;
	static private final int MOVE_LOWER_BOUND_MS = 260;
	static private final float FLICK_OFFSET_THRESHOLD_DP = 30;
	static private final float FLICK_OFFSET_THRESHOLD_PX = TouchManager.FLICK_OFFSET_THRESHOLD_DP
			* XenaApplication.DPI / 160;
	static private final float ZOOM_DISTANCE_BOUND_DP = 128;
	static private final float ZOOM_DISTANCE_BOUND_PX = TouchManager.ZOOM_DISTANCE_BOUND_DP
			* XenaApplication.DPI / 160;
	static private final int ZOOM_LOWER_BOUND_MS = 160;
	static private final int DOUBLE_TAP_UPPER_BOUND_MS = 360;
	static final int IGNORE_CHAIN_BOUND_MS = 360;

	private PointF previousPoint = new PointF();
	private long actionDownTimeMs = 0;
	private long zoomDownTimeMs = 0;
	private float zoomBeginDistance;
	private boolean zoomDownConsumed = true;
	private boolean hasPanned;
	private long previousTapTimeMs = 0;
	long previousIgnoreChainTimeMs = 0;

	private ScribbleActivity scribbleActivity;

	public TouchManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_2_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_2_UP:
				return this.onTouchInner(event.getAction(), event.getX(), event.getY(),
						event.getX(1), event.getY(1));
			default:
				return this.onTouchInner(event.getAction(), event.getX(), event.getY(),
						0, 0);
		}
	}

	@SuppressWarnings("deprecation")
	public boolean onTouchInner(int eventAction, float eventX0, float eventY0,
			float eventX1, float eventY1) {
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

				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onTouch:DOWN " + touchPoint);

				if (currentTimeMs
						- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				// Sometimes, this fires slightly before a draw/erase event. The
				// draw/erase event will cancel panning in that case.

				if (this.scribbleActivity.isPanning) {
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:RESET "
							+ this.scribbleActivity.pathManager.getViewportOffset());
				} else {
					RectF bounds = new RectF(
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

						this.actionDownTimeMs = currentTimeMs;
					} else {
						Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE");
						this.previousIgnoreChainTimeMs = currentTimeMs;
					}
				}

				this.scribbleActivity.panBeginOffset = this.scribbleActivity.pathManager
						.getViewportOffset();
				this.previousPoint.x = touchPoint.x;
				this.previousPoint.y = touchPoint.y;

				this.hasPanned = false;

				break;
			case MotionEvent.ACTION_MOVE:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.penManager.onRawDrawingTouchPointMoveReceived(
							new TouchPoint(touchPoint.x, touchPoint.y));
					break;
				}

				if (!this.scribbleActivity.isPanning) {
					break;
				}

				// Don't process until we exit flick range.
				if (eventDurationMs <= TouchManager.MOVE_LOWER_BOUND_MS) {
					break;
				}

			{
				// PointF newOffset = new PointF(
				// 		this.scribbleActivity.pathManager.getViewportOffset().x
				// 				+ (touchPoint.x
				// 						- this.previousPoint.x)
				// 						/ this.scribbleActivity.pathManager.getZoomScale(),
				// 		this.scribbleActivity.pathManager.getViewportOffset().y
				// 				+ (touchPoint.y
				// 						- this.previousPoint.y)
				// 						/ this.scribbleActivity.pathManager.getZoomScale());
				// this.scribbleActivity.pathManager.setViewportOffset(newOffset);

				// Do not update text view while dragging.
				// this.scribbleActivity.updateTextViewStatus();
			}
				// this.previousPoint.x = touchPoint.x;
				// this.previousPoint.y = touchPoint.y;

				this.hasPanned = true;

				// Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:MOVE "
				// + pathManager.getViewportOffset());

				// Do not redraw while dragging.

				// if (this.scribbleActivity.isRedrawing) {
				// this.scribbleActivity.redraw();
				// } else {
				// this.scribbleActivity.drawBitmapToView(false, true);
				// }

				// No need to reset raw input capture here, because it is assumed that
				// the Boox canvas is clean.
				break;
			case MotionEvent.ACTION_UP:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.penManager.onEndRawDrawing(false,
							new TouchPoint(touchPoint.x, touchPoint.y));
					break;
				}

				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onTouch:UP " + touchPoint);

				if (currentTimeMs
						- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				if (!this.scribbleActivity.isPanning
						|| this.zoomDownConsumed == false) {
					this.zoomDownConsumed = true;
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE");
					this.previousIgnoreChainTimeMs = currentTimeMs;
					break;
				}

				this.zoomDownConsumed = true;

				// Note: ACTION_UP is not guaranteed to fire after ACTION_DOWN.
				this.scribbleActivity.isPanning = false;

				// Detect flicks.
				float flickOffset = this.scribbleActivity.pathManager
						.getViewportOffset().y
						+ touchPoint.y - this.previousPoint.y
						- this.scribbleActivity.panBeginOffset.y;
				if (eventDurationMs >= TouchManager.FLICK_LOWER_BOUND_MS
						&& eventDurationMs <= TouchManager.FLICK_UPPER_BOUND_MS
						&& Math
								.abs(flickOffset) >= TouchManager.FLICK_OFFSET_THRESHOLD_PX) {
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:FLICK");

					// Determine flick direction.
					int direction = flickOffset > 0
							? 1
							: -1;
					{
						this.scribbleActivity.pathManager.setViewportOffset(new PointF(
								this.scribbleActivity.panBeginOffset.x,
								this.scribbleActivity.panBeginOffset.y
										+ direction * this.scribbleActivity.scribbleView.getHeight()
												* TouchManager.FLICK_MOVE_RATIO
												/ this.scribbleActivity.pathManager.getZoomScale()));
					}

					this.hasPanned = true;
				}

				// If not panned, treat as a tap.
				if (!this.hasPanned) {
					if (currentTimeMs
							- this.previousTapTimeMs <= TouchManager.DOUBLE_TAP_UPPER_BOUND_MS) {
						Log.v(XenaApplication.TAG, "TouchManager::onTouch:double_tap.");
						this.scribbleActivity.isPenEraseMode = !this.scribbleActivity.isPenEraseMode;
						this.scribbleActivity.drawEraseToggle.setBackgroundResource(
								this.scribbleActivity.isPenEraseMode
										? R.drawable.solid_empty
										: R.drawable.dotted_empty);
						this.scribbleActivity.penManager.cancelRedraw();
						this.scribbleActivity.redraw();
						this.previousTapTimeMs = 0;
					} else {
						this.previousTapTimeMs = currentTimeMs;
					}
				} else {
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:PAN");

				PointF newOffset = new PointF(
						this.scribbleActivity.pathManager.getViewportOffset().x
								+ (touchPoint.x
										- this.previousPoint.x)
										/ this.scribbleActivity.pathManager.getZoomScale(),
						this.scribbleActivity.pathManager.getViewportOffset().y
								+ (touchPoint.y
										- this.previousPoint.y)
										/ this.scribbleActivity.pathManager.getZoomScale());
				this.scribbleActivity.pathManager.setViewportOffset(newOffset);

					this.scribbleActivity.updateTextViewStatus();
					this.scribbleActivity.svgFileScribe.debounceSave(
							this.scribbleActivity,
							this.scribbleActivity.svgUri,
							this.scribbleActivity.pathManager);
					this.scribbleActivity.penManager.cancelRedraw();
					this.scribbleActivity.redraw();
				}
				break;
			// Deprecated events may still be used by Boox API.
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_2_DOWN:
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onTouch:ACTION_POINTER_DOWN "
								+ this.zoomBeginDistance);

				if (currentTimeMs
						- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				this.zoomBeginDistance = Geometry.distance(touchPoint,
						new PointF(eventX1, eventY1));
				this.zoomDownTimeMs = currentTimeMs;
				this.zoomDownConsumed = false;
				break;
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_2_UP:
				float zoomEndDistance = Geometry.distance(touchPoint,
						new PointF(eventX1, eventY1));
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onTouch:ACTION_POINTER_UP " + zoomEndDistance);

				if (currentTimeMs
						- this.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
					this.previousIgnoreChainTimeMs = currentTimeMs;
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE_CHAIN");
					break;
				}

				if (currentTimeMs
						- this.zoomDownTimeMs < TouchManager.ZOOM_LOWER_BOUND_MS
						|| this.zoomDownConsumed) {
					this.zoomDownConsumed = true;
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE");
					this.previousIgnoreChainTimeMs = currentTimeMs;
					break;
				}
				this.zoomDownConsumed = true;

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
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:ZOOM");
					this.scribbleActivity.setStrokeWidthScale(
							this.scribbleActivity.pathManager.getZoomScale());
					this.scribbleActivity.updateTextViewStatus();
					this.scribbleActivity.penManager.cancelRedraw();
					this.scribbleActivity.redraw();
					this.scribbleActivity.svgFileScribe.debounceSave(
							this.scribbleActivity,
							this.scribbleActivity.svgUri,
							this.scribbleActivity.pathManager);
				}

				// Cancel panning by remaining finger.
				this.scribbleActivity.isPanning = false;
				break;
		}
		return true;
	}
}
