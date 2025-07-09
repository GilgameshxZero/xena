package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;
import com.gilgamesh.xena.filesystem.SvgFileScribe;
import com.gilgamesh.xena.multithreading.DebouncedTask;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.pen.RawInputCallback;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.HashSet;

public class PenManager extends RawInputCallback {
	static private final float DRAW_MOVE_EPSILON_DP = 2f;
	static private final float DRAW_MOVE_EPSILON_PX
		= PenManager.DRAW_MOVE_EPSILON_DP * XenaApplication.DPI / 160;
	static private final float ERASE_MOVE_EPSILON_DP = 4f;
	static private final float ERASE_MOVE_EPSILON_PX
		= PenManager.ERASE_MOVE_EPSILON_DP * XenaApplication.DPI / 160;
	static private final int DEBOUNCE_REDRAW_DELAY_MS = 64000;

	// For a short bit after draw or erase, touch events are disabled.
	static private final int DEBOUNCE_INPUT_COOLDOWN_DELAY_MS = 50;

	// Drawing end/begin pairs may fire within milliseconds. In this case,
	// ignore both events. We accomplish this with a debounce on the end events.
	// This will not be used for draw begin and draw move events, because draw end
	// is guaranteed to fire.
	static private final int DEBOUNCE_END_DRAW_DELAY_MS = 10;

	// The erasing events are a little broken. The end erase event is not
	// guaranteed, and sometimes without lifting the eraser there are multiple
	// begin/end events created. Thus, we only interpret an erase event ending
	// if no erase-related events have been received within a certain time.
	// Before then, we erase all points received.
	static private final int DEBOUNCE_END_ERASE_DELAY_MS = 300;

	private PointF previousErasePoint = new PointF();
	private PointF previousTentativeDrawPoint = new PointF();
	private CompoundPath currentPath;

	private PointF endDrawTaskTouchPoint;

	// Fields from parent.
	private ScribbleActivity scribbleActivity;

	public PenManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
	}

	// Cooldown for input end operations to prevent panning.
	DebouncedTask inputCooldownTask
		= new DebouncedTask(new DebouncedTask.Callback() {
			@Override
			public void onRun() {
				XenaApplication.log("PenManager::inputCooldownTask");
			}
		});

	// Erroneous draw end/begin pairs.
	DebouncedTask endDrawTask = new DebouncedTask(new DebouncedTask.Callback() {
		@Override
		public void onRun() {
			if (scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
				scribbleActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						scribbleActivity.touchManager.onTouchInner(MotionEvent.ACTION_UP,
							endDrawTaskTouchPoint.x, endDrawTaskTouchPoint.y, 0, 0, 0, 0);
					}
				});
				return;
			}

			XenaApplication.log("PenManager::endDrawTask");

			// The new path has already been loaded by the PathManager. Conclude
			// it by drawing it onto the chunk bitmaps here.
			scribbleActivity.redrawTask.debounce(PenManager.DEBOUNCE_REDRAW_DELAY_MS);
			inputCooldownTask.debounce(PenManager.DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);

			scribbleActivity.pathManager.finalizePath(currentPath);
			scribbleActivity.svgFileScribe.saveTask
				.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
		}
	});

	// Sometimes, the end erase event is never received, or comes in early.
	DebouncedTask endEraseTask = new DebouncedTask(new DebouncedTask.Callback() {
		@Override
		public void onRun() {
			XenaApplication.log("PenManager::endEraseTask");
			inputCooldownTask.debounce(PenManager.DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);
		}
	});

	@Override
	public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			this.scribbleActivity.touchManager.onTouchInner(MotionEvent.ACTION_DOWN,
				touchPoint.x, touchPoint.y, 0, 0, 0, 0);
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
				this.scribbleActivity.updateTextViewStatus();
				this.scribbleActivity.redraw(true, false);
			}

			XenaApplication.log("PenManager::onTouch:UNDO "
				+ this.scribbleActivity.pathManager.getViewportOffset());

			this.scribbleActivity.isPanning = false;
		}

		// If currently drawing, treat this event the same as a move event.
		if (this.endDrawTask.isAwaiting()) {
			this.onRawDrawingTouchPointMoveReceived(touchPoint);
			return;
		}

		XenaApplication.log("PenManager::onBeginRawDrawing");
		this.scribbleActivity.redrawTask.cancel();
		this.endDrawTask.debounce(-1);
		this.previousTentativeDrawPoint.set(touchPoint.x, touchPoint.y);
		this.currentPath
			= this.scribbleActivity.pathManager
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
			this.endDrawTask.debounce(PenManager.DEBOUNCE_END_DRAW_DELAY_MS);
			return;
		}

		if (this.scribbleActivity.isPenEraseMode) {
			this.onEndRawErasing(b, touchPoint);
			return;
		}

		this.endDrawTaskTouchPoint = new PointF(touchPoint.x, touchPoint.y);
		this.endDrawTask.debounce(DEBOUNCE_END_DRAW_DELAY_MS);
	}

	@Override
	public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			this.scribbleActivity.touchManager.onTouchInner(MotionEvent.ACTION_MOVE,
				touchPoint.x, touchPoint.y, 0, 0, 0, 0);
			return;
		}

		if (this.scribbleActivity.isPenEraseMode) {
			this.onRawErasingTouchPointMoveReceived(touchPoint);
			return;
		}

		if (!this.endDrawTask.isAwaiting()) {
			return;
		}

		this.endDrawTask.debounce(-1);

		PointF lastPoint = currentPath.points.get(currentPath.points.size() - 1);
		PointF newPoint
			= new PointF(
				touchPoint.x / this.scribbleActivity.pathManager.getZoomScale()
					- this.scribbleActivity.pathManager.getViewportOffset().x,
				touchPoint.y / this.scribbleActivity.pathManager.getZoomScale()
					- this.scribbleActivity.pathManager.getViewportOffset().y);

		if (currentPath.points.size() > 1 && Geometry.distance(lastPoint,
			newPoint) < PenManager.DRAW_MOVE_EPSILON_PX) {
			return;
		}

		currentPath.addPoint(newPoint);
	}

	@Override
	public void onRawDrawingTouchPointListReceived(
		TouchPointList touchPointList) {
	}

	@Override
	public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
		if (!this.scribbleActivity.isPenEraseMode
			&& this.scribbleActivity.penTouchMode != ScribbleActivity.PenTouchMode.DEFAULT) {
			return;
		}

		if (this.endEraseTask.isAwaiting()) {
			// This should be interpreted as an erase move event.
			this.onRawErasingTouchPointMoveReceived(touchPoint);
			return;
		}

		XenaApplication.log("PenManager::onBeginRawErasing");

		// This is the beginning of an erase move sequence.
		this.endEraseTask.debounce(PenManager.DEBOUNCE_END_ERASE_DELAY_MS);
		this.scribbleActivity.isPanning = false;

		this.scribbleActivity.redraw(true, true);

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

		if (!this.endEraseTask.isAwaiting()) {
			return;
		}

		XenaApplication.log("PenManager::onEndRawErasing");

		// Process this event as a move event.
		this.onRawErasingTouchPointMoveReceived(touchPoint);
	}

	@Override
	public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
		if (!this.scribbleActivity.isPenEraseMode
			&& this.scribbleActivity.penTouchMode != ScribbleActivity.PenTouchMode.DEFAULT) {
			return;
		}

		// Some events come after onEndRawErasing; instead of ignoring those
		// events, we restart erasing at this time.
		if (!this.endEraseTask.isAwaiting()) {
			this.onBeginRawErasing(false, touchPoint);
		} else {
			this.endEraseTask.debounce(PenManager.DEBOUNCE_END_ERASE_DELAY_MS);
		}

		// Actual logic to handle erasing.
		int initialSize = this.scribbleActivity.pathManager.getPathsCount();
		PointF currentErasePoint
			= new PointF(
				touchPoint.x / this.scribbleActivity.pathManager.getZoomScale()
					- this.scribbleActivity.pathManager.getViewportOffset().x,
				touchPoint.y / this.scribbleActivity.pathManager.getZoomScale()
					- this.scribbleActivity.pathManager.getViewportOffset().y);

		// Only process this event if significantly different from previous
		// point.
		if (Geometry.distance(this.previousErasePoint,
			currentErasePoint) <= PenManager.ERASE_MOVE_EPSILON_PX) {
			return;
		}

		HashSet<Point> chunkIds = new HashSet<Point>();
		chunkIds.add(this.scribbleActivity.pathManager
			.getChunkCoordinateForPoint(this.previousErasePoint));
		chunkIds.add(this.scribbleActivity.pathManager
			.getChunkCoordinateForPoint(currentErasePoint));
		for (Point chunkId : chunkIds) {
			// Copy so that concurrent read/writes don't happen.
			HashSet<Integer> pathIds
				= new HashSet<Integer>(this.scribbleActivity.pathManager
					.getChunkForCoordinate(chunkId).getPathIds());
			for (Integer pathId : pathIds) {
				if (this.scribbleActivity.pathManager.getPath(pathId)
					.isIntersectingSegment(this.previousErasePoint, currentErasePoint)) {
					this.scribbleActivity.pathManager.removePathId(pathId);
				}
			}
		}

		if (initialSize != this.scribbleActivity.pathManager.getPathsCount()) {
			this.scribbleActivity.redraw(true, false);
			this.scribbleActivity.svgFileScribe.saveTask
				.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
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
