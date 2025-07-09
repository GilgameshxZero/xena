package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;

import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.pen.RawInputCallback;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;

import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class PenManager extends RawInputCallback {
	static private final float DRAW_MOVE_EPSILON_DP = 2f;
	static private final float DRAW_MOVE_EPSILON_PX = PenManager.DRAW_MOVE_EPSILON_DP
			* XenaApplication.DPI / 160;
	static private final float ERASE_MOVE_EPSILON_DP = 4f;
	static private final float ERASE_MOVE_EPSILON_PX = PenManager.ERASE_MOVE_EPSILON_DP
			* XenaApplication.DPI / 160;
	private final int DEBOUNCE_REDRAW_DELAY_MS = 64000;
	private final int DEBOUNCE_INPUT_COOLDOWN_DELAY_MS = 200;

	// Drawing end/begin pairs may fire within milliseconds. In this case,
	// ignore both events. We accomplish this with a debounce on the end events.
	private final int DEBOUNCE_END_DRAW_DELAY_MS = 10;

	// The erasing events are a little broken. The end erase event is not
	// guaranteed, and sometimes without lifting the eraser there are multiple
	// begin/end events created. Thus, we only interpret an erase event ending
	// if no erase-related events have been received within a certain time.
	// Before then, we erase all points received.
	private final int DEBOUNCE_END_ERASE_DELAY_MS = 300;

	private PointF previousErasePoint = new PointF();
	private PointF previousTentativeDrawPoint = new PointF();
	private CompoundPath currentPath;

	private PointF endDrawTaskTouchPoint;

	// Fields from parent.
	private ScribbleActivity scribbleActivity;

	public PenManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
	}

	// Cancellable dummy.
	private TimerTask debounceRedrawTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	public void cancelRedraw() {
		this.scribbleActivity.isRedrawing = false;
		this.debounceRedrawTask.cancel();
	}

	private void debounceRedraw(int delayMs) {
		this.cancelRedraw();
		this.scribbleActivity.isRedrawing = true;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Log.v(XenaApplication.TAG, "ScribbleActivity::debounceRedrawTask");

				scribbleActivity.redraw();
			}
		};
		new Timer().schedule(task, delayMs);
		this.debounceRedrawTask = task;
	}

	// Debounce cooldown for input end operations to prevent panning; implements
	// palm rejection.
	private TimerTask debounceInputCooldownTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	private void debounceInputCooldown(int delayMs) {
		this.debounceInputCooldownTask.cancel();
		this.scribbleActivity.isInputCooldown = true;
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::debounceInputCooldownTask");

				scribbleActivity.isInputCooldown = false;
			}
		};
		new Timer().schedule(task, delayMs);
		this.debounceInputCooldownTask = task;
	}

	// Debounce for erroneous draw end/begin pairs.
	private TimerTask debounceEndDrawTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	private void debounceEndDraw(int delayMs) {
		this.debounceEndDrawTask.cancel();
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				if (scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
					scribbleActivity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							scribbleActivity.touchManager.onTouchInner(
									MotionEvent.ACTION_UP, endDrawTaskTouchPoint.x,
									endDrawTaskTouchPoint.y, 0, 0, 0, 0);
						}
					});
					return;
				}

				Log.v(XenaApplication.TAG, "ScribbleActivity::debounceEndDrawTask");

				// The new path has already been loaded by the PathManager. Conclude
				// it by drawing it onto the chunk bitmaps here.
				scribbleActivity.isDrawing = false;
				debounceRedraw(DEBOUNCE_REDRAW_DELAY_MS);
				debounceInputCooldown(DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);

				scribbleActivity.pathManager.finalizePath(currentPath);
				scribbleActivity.svgFileScribe.debounceSave(scribbleActivity,
						scribbleActivity.svgUri,
						scribbleActivity.pathManager);
			}
		};
		new Timer().schedule(task, delayMs);
		this.debounceEndDrawTask = task;
	}

	// Sometimes, the end erase event is never received, so debounce it.
	private TimerTask debounceEndEraseTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	private void debounceEndErase(int delayMs) {
		this.debounceEndEraseTask.cancel();
		// Split into a few steps in case of multi-threading errors.
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				Log.v(XenaApplication.TAG, "ScribbleActivity::debounceEndEraseTask");

				scribbleActivity.isErasing = false;
				scribbleActivity.touchHelper.setRawDrawingEnabled(true);
				debounceInputCooldown(DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);
			}
		};
		new Timer().schedule(task, delayMs);
		this.debounceEndEraseTask = task;
	}

	@Override
	public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			this.scribbleActivity.touchManager.onTouchInner(
					MotionEvent.ACTION_DOWN, touchPoint.x, touchPoint.y, 0, 0, 0, 0);
			return;
		}

		if (this.scribbleActivity.isPenEraseMode) {
			this.onBeginRawErasing(b, touchPoint);
			return;
		}

		// If currently panning, that means there were erroneous panning events
		// fired. Undo them, and unset panning.
		if (this.scribbleActivity.isPanning) {
			if (this.scribbleActivity.panBeginOffset != this.scribbleActivity.pathManager
					.getViewportOffset()) {
				this.scribbleActivity.pathManager
						.setViewportOffset(this.scribbleActivity.panBeginOffset);
				this.scribbleActivity
						.updateTextViewStatus();
				this.scribbleActivity.drawBitmapToView(true, true);
			}

			Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:UNDO "
					+ this.scribbleActivity.pathManager.getViewportOffset());

			this.scribbleActivity.isPanning = false;
		}

		this.debounceEndDrawTask.cancel();

		// If currently drawing, treat this event the same as a move event.
		if (this.scribbleActivity.isDrawing) {
			this.onRawDrawingTouchPointMoveReceived(touchPoint);
			return;
		}

		Log.v(XenaApplication.TAG, "ScribbleActivity::onBeginRawDrawing");
		this.cancelRedraw();
		this.scribbleActivity.isDrawing = true;
		this.previousTentativeDrawPoint.set(touchPoint.x, touchPoint.y);
		this.currentPath = this.scribbleActivity.pathManager
				.addPath(new PointF(
						touchPoint.x / this.scribbleActivity.pathManager.getZoomScale()
								- this.scribbleActivity.pathManager.getViewportOffset().x,
						touchPoint.y / this.scribbleActivity.pathManager.getZoomScale()
								- this.scribbleActivity.pathManager.getViewportOffset().y))
				.getValue();
	}

	@Override
	public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			this.endDrawTaskTouchPoint = new PointF(touchPoint.x, touchPoint.y);
			this.debounceEndDraw(DEBOUNCE_END_DRAW_DELAY_MS);
			return;
		}

		if (this.scribbleActivity.isPenEraseMode) {
			this.onEndRawErasing(b, touchPoint);
			return;
		}

		this.endDrawTaskTouchPoint = new PointF(touchPoint.x, touchPoint.y);
		this.debounceEndDraw(DEBOUNCE_END_DRAW_DELAY_MS);
	}

	@Override
	public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			this.scribbleActivity.touchManager.onTouchInner(
					MotionEvent.ACTION_MOVE, touchPoint.x, touchPoint.y, 0, 0, 0, 0);
			return;
		}

		if (this.scribbleActivity.isPenEraseMode) {
			this.onRawErasingTouchPointMoveReceived(touchPoint);
			return;
		}

		if (!this.scribbleActivity.isDrawing) {
			return;
		}

		PointF lastPoint = currentPath.points.get(currentPath.points.size() - 1);
		PointF newPoint = new PointF(
				touchPoint.x / this.scribbleActivity.pathManager.getZoomScale()
						- this.scribbleActivity.pathManager.getViewportOffset().x,
				touchPoint.y / this.scribbleActivity.pathManager.getZoomScale()
						- this.scribbleActivity.pathManager.getViewportOffset().y);

		if (currentPath.points.size() > 1
				&& Geometry.distance(lastPoint,
						newPoint) < PenManager.DRAW_MOVE_EPSILON_PX) {
			return;
		}

		// Log.v(XenaApplication.TAG,
		// "ScribbleActivity::onRawDrawingTouchPointMoveReceived "
		// + touchPoint);

		currentPath.addPoint(newPoint);

		// TODO: re-enable.
		// this.scribbleActivity.scribbleViewCanvas.drawLine(
		// previousTentativeDrawPoint.x,
		// previousTentativeDrawPoint.y, touchPoint.x, touchPoint.y,
		// ScribbleActivity.PAINT_TENTATIVE_LINE);
		if (this.scribbleActivity.scribbleView.isDrawing()) {
			// Log.v(XenaApplication.TAG, "Dirty ScribbleView.");
		} else {
			// Draw line for the purposes of screenshare, which does not capture any
			// raw drawing activities.
			// this.scribbleActivity.scribbleView.postInvalidate();
		}
		previousTentativeDrawPoint.set(touchPoint.x, touchPoint.y);
	}

	@Override
	public void onRawDrawingTouchPointListReceived(
			TouchPointList touchPointList) {
		if (this.scribbleActivity.isPenEraseMode) {
			this.onRawErasingTouchPointListReceived(touchPointList);
			return;
		}
	}

	@Override
	public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
		if (!this.scribbleActivity.isPenEraseMode
				&& this.scribbleActivity.penTouchMode != ScribbleActivity.PenTouchMode.DEFAULT) {
			return;
		}

		if (this.scribbleActivity.isErasing) {
			// This should be interpreted as an erase move event.
			this.onRawErasingTouchPointMoveReceived(touchPoint);
			return;
		}

		Log.v(XenaApplication.TAG, "ScribbleActivity::onBeginRawErasing");

		this.scribbleActivity.touchHelper.setRawDrawingEnabled(false);

		// This is the beginning of an erase move sequence.
		this.debounceEndErase(DEBOUNCE_END_ERASE_DELAY_MS);
		this.scribbleActivity.isPanning = false;
		this.scribbleActivity.isErasing = true;

		this.cancelRedraw();
		this.scribbleActivity.redraw();

		this.previousErasePoint.set(
				touchPoint.x / this.scribbleActivity.pathManager.getZoomScale()
						- this.scribbleActivity.pathManager.getViewportOffset().x,
				touchPoint.y / this.scribbleActivity.pathManager.getZoomScale()
						- this.scribbleActivity.pathManager.getViewportOffset().y);
	}

	@Override
	public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
		if (!this.scribbleActivity.isPenEraseMode
				&& this.scribbleActivity.penTouchMode != ScribbleActivity.PenTouchMode.DEFAULT) {
			return;
		}

		if (!this.scribbleActivity.isErasing) {
			return;
		}

		// Process this event as a move event.
		this.onRawErasingTouchPointMoveReceived(touchPoint);

		Log.v(XenaApplication.TAG, "ScribbleActivity::onEndRawErasing");
	}

	@Override
	public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
		if (!this.scribbleActivity.isPenEraseMode
				&& this.scribbleActivity.penTouchMode != ScribbleActivity.PenTouchMode.DEFAULT) {
			return;
		}

		// Some events come after onEndRawErasing; instead of ignoring those
		// events, we restart erasing at this time.
		if (!this.scribbleActivity.isErasing) {
			this.onBeginRawErasing(false, touchPoint);
		}

		// Actual logic to handle erasing.
		int initialSize = this.scribbleActivity.pathManager.getPathsCount();
		PointF currentErasePoint = new PointF(
				touchPoint.x / this.scribbleActivity.pathManager.getZoomScale()
						- this.scribbleActivity.pathManager.getViewportOffset().x,
				touchPoint.y / this.scribbleActivity.pathManager.getZoomScale()
						- this.scribbleActivity.pathManager.getViewportOffset().y);

		// Only process this event if significantly different from previous point.
		if (Geometry.distance(this.previousErasePoint,
				currentErasePoint) <= PenManager.ERASE_MOVE_EPSILON_PX) {
			return;
		}

		// Log.v(XenaApplication.TAG,
		// "ScribbleActivity::onRawErasingTouchPointMoveReceived "
		// + currentErasePoint);

		this.debounceEndErase(DEBOUNCE_END_ERASE_DELAY_MS);

		HashSet<Point> chunkIds = new HashSet<Point>();
		chunkIds.add(
				this.scribbleActivity.pathManager
						.getChunkCoordinateForPoint(this.previousErasePoint));
		chunkIds.add(
				this.scribbleActivity.pathManager
						.getChunkCoordinateForPoint(currentErasePoint));
		for (Point chunkId : chunkIds) {
			// Copy so that concurrent read/writes don't happen.
			HashSet<Integer> pathIds = new HashSet<Integer>(
					this.scribbleActivity.pathManager.getChunkForCoordinate(chunkId)
							.getPathIds());
			for (Integer pathId : pathIds) {
				if (this.scribbleActivity.pathManager.getPath(pathId)
						.isIntersectingSegment(
								this.previousErasePoint, currentErasePoint)) {
					this.scribbleActivity.pathManager.removePathId(pathId);
				}
			}
		}

		if (initialSize != this.scribbleActivity.pathManager.getPathsCount()) {
			this.scribbleActivity.drawBitmapToView(true, true);
			this.scribbleActivity.svgFileScribe.debounceSave(this.scribbleActivity,
					this.scribbleActivity.svgUri, this.scribbleActivity.pathManager);
		}

		this.previousErasePoint.set(currentErasePoint);
	}

	@Override
	public void onRawErasingTouchPointListReceived(
			TouchPointList touchPointList) {
	}

	@Override
	public void onPenActive(TouchPoint touchPoint) {
	}

	@Override
	public void onPenUpRefresh(RectF refreshRect) {
	}
}
