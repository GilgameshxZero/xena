package com.gilgamesh.xena.scribble;

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
	static private final float TOUCH_BORDER_INVALID_RATIO = 0.1f;
	static private final float ZOOM_STEP = 1.2f;

	private final int FLICK_LOWER_BOUND_MS = 80;
	private final int FLICK_UPPER_BOUND_MS = 220;
	private final float ZOOM_DISTANCE_BOUND = 128;

	private PointF previousPoint = new PointF();
	private long actionDownTimeMs = 0;
	private float zoomBeginDistance;

	private ScribbleActivity scribbleActivity;

	public TouchManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean onTouch(View v, MotionEvent event) {
		if (!this.scribbleActivity.isTouchDrawMode
				&& (this.scribbleActivity.isDrawing || this.scribbleActivity.isErasing
						|| this.scribbleActivity.isInputCooldown)) {
			return false;
		}

		PointF touchPoint = new PointF(event.getX(), event.getY());
		long eventDurationMs = (System.currentTimeMillis()
				- this.actionDownTimeMs);

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (this.scribbleActivity.isTouchDrawMode) {
					this.scribbleActivity.penManager.onBeginRawDrawing(false,
							new TouchPoint(touchPoint.x, touchPoint.y));
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
						Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:DOWN "
								+ this.scribbleActivity.pathManager.getViewportOffset());

						this.scribbleActivity.isPanning = true;

						this.actionDownTimeMs = System.currentTimeMillis();
					}
				}

				this.scribbleActivity.panBeginOffset = this.scribbleActivity.pathManager
						.getViewportOffset();
				this.previousPoint.x = touchPoint.x;
				this.previousPoint.y = touchPoint.y;

				break;
			case MotionEvent.ACTION_MOVE:
				if (this.scribbleActivity.isTouchDrawMode) {
					this.scribbleActivity.penManager.onRawDrawingTouchPointMoveReceived(
							new TouchPoint(touchPoint.x, touchPoint.y));
					break;
				}

				if (!this.scribbleActivity.isPanning) {
					break;
				}

				// Don't process until we exit flick range.
				if (eventDurationMs <= FLICK_UPPER_BOUND_MS) {
					break;
				}

			{
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
			}
				this.previousPoint.x = touchPoint.x;
				this.previousPoint.y = touchPoint.y;

				// Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:MOVE "
				// + pathManager.getViewportOffset());

				// Do not redraw while dragging.
				// if (this.scribbleActivity.isRedrawing) {
				// this.scribbleActivity.redraw();
				// } else {
				// this.scribbleActivity.drawBitmapToView(false, true);
				// }

				// No need to reset raw input capture here, for some reason.
				break;
			case MotionEvent.ACTION_UP:
				if (this.scribbleActivity.isTouchDrawMode) {
					this.scribbleActivity.penManager.onEndRawDrawing(false,
							new TouchPoint(touchPoint.x, touchPoint.y));
					break;
				}

				if (!this.scribbleActivity.isPanning) {
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE");
					break;
				}

				// Note: ACTION_UP is not guaranteed to fire after ACTION_DOWN.
				this.scribbleActivity.isPanning = false;

				// Detect flicks.
				if (eventDurationMs >= FLICK_LOWER_BOUND_MS
						&& eventDurationMs <= FLICK_UPPER_BOUND_MS) {
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:FLICK");

					// Determine flick direction.
					int direction = this.scribbleActivity.panBeginOffset.y < this.scribbleActivity.pathManager
							.getViewportOffset().y
							+ touchPoint.y - this.previousPoint.y ? 1 : -1;
					{
						PointF newOffset = new PointF(
								this.scribbleActivity.panBeginOffset.x,
								this.scribbleActivity.panBeginOffset.y
										+ direction * this.scribbleActivity.scribbleView.getHeight()
												* TouchManager.FLICK_MOVE_RATIO
												/ this.scribbleActivity.pathManager.getZoomScale());
						this.scribbleActivity.pathManager.setViewportOffset(newOffset);
						this.scribbleActivity.updateTextViewStatus();
					}
				} else {
					Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:UP");
				}

				this.scribbleActivity.svgFileScribe.debounceSave(this.scribbleActivity,
						this.scribbleActivity.svgUri,
						this.scribbleActivity.pathManager);
				this.scribbleActivity.drawBitmapToView(true, true);
				break;
			// Deprecated events may still be used by Boox API.
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_2_DOWN:
				this.zoomBeginDistance = Geometry.distance(touchPoint,
						new PointF(event.getX(1), event.getY(1)));
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onTouch:ACTION_POINTER_DOWN "
								+ this.zoomBeginDistance);
				break;
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_2_UP:
				float zoomEndDistance = Geometry.distance(touchPoint,
						new PointF(event.getX(1), event.getY(1)));
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onTouch:ACTION_POINTER_UP "
								+ zoomEndDistance);

				if (this.zoomBeginDistance - zoomEndDistance >= ZOOM_DISTANCE_BOUND) {
					// Zoom out.
					this.scribbleActivity.pathManager.setZoomScale(
							this.scribbleActivity.pathManager.getZoomScale()
									/ TouchManager.ZOOM_STEP);
				} else if (zoomEndDistance
						- this.zoomBeginDistance >= ZOOM_DISTANCE_BOUND) {
					// Zoom in.
					this.scribbleActivity.pathManager.setZoomScale(
							this.scribbleActivity.pathManager.getZoomScale()
									* TouchManager.ZOOM_STEP);
				}
				this.scribbleActivity.touchHelper.setStrokeWidth(
						Chunk.STROKE_WIDTH
								* this.scribbleActivity.pathManager.getZoomScale());
				ScribbleActivity.PAINT_TENTATIVE_LINE.setStrokeWidth(
						Chunk.STROKE_WIDTH
								* this.scribbleActivity.pathManager.getZoomScale());
				this.scribbleActivity.updateTextViewStatus();
				this.scribbleActivity.drawBitmapToView(true, true);

				// Cancel panning by remaining finger.
				this.scribbleActivity.isPanning = false;
				break;
		}
		return true;
	}
}
