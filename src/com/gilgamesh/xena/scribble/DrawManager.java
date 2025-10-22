package com.gilgamesh.xena.scribble;

import com.gilgamesh.algorithm.Geometry;
import com.gilgamesh.multithreading.DebouncedTask;
import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.filesystem.SvgFileScribe;

import android.graphics.Point;
import android.graphics.PointF;

import java.util.HashSet;

// Used by DrawManager and alt-mode TouchManager.
public class DrawManager {
	static private final float DRAW_MOVE_EPSILON_DP = 2f;
	static private final float DRAW_MOVE_EPSILON_PX
		= DrawManager.DRAW_MOVE_EPSILON_DP * XenaApplication.DPI / 160;
	static private final float ERASE_MOVE_EPSILON_DP = 4f;
	static private final float ERASE_MOVE_EPSILON_PX
		= DrawManager.ERASE_MOVE_EPSILON_DP * XenaApplication.DPI / 160;

	// For a short bit after draw or erase, touch events are disabled.
	static private final int DEBOUNCE_INPUT_COOLDOWN_DELAY_MS = 0;

	// Drawing end/begin pairs may fire within milliseconds. In this case,
	// ignore both events. We accomplish this with a debounce on the end events.
	// This will not be used for draw begin and draw move events, because draw end
	// is guaranteed to fire.
	static final int DEBOUNCE_END_DRAW_DELAY_MS = 10;

	// The erasing events are a little broken. The end erase event is not
	// guaranteed, and sometimes without lifting the eraser there are multiple
	// begin/end events created. Thus, we only interpret an erase event ending
	// if no erase-related events have been received within a certain time.
	// Before then, we erase all points received.
	static private final int DEBOUNCE_END_ERASE_DELAY_MS = 300;

	private PointF previousErasePoint = new PointF();
	private CompoundPath currentPath;
	PointF endDrawTaskTouchPoint;

	private ScribbleActivity scribbleActivity;

	// Cooldown for input end operations to prevent panning.
	DebouncedTask inputCooldownTask
		= new DebouncedTask(new DebouncedTask.Callback() {
			@Override
			public void onRun() {
				XenaApplication.log("DrawManager::inputCooldownTask.");
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
						scribbleActivity.panManager.onActionUp(0, endDrawTaskTouchPoint, 0);
					}
				});
				return;
			}

			XenaApplication.log("DrawManager::endDrawTask.");

			// The new path has already been loaded by the PathManager. Conclude
			// it by drawing it onto the chunk bitmaps here.
			scribbleActivity.scribbleView.tentativePath.reset();
			scribbleActivity.redrawTask.debounce(XenaApplication.getDrawEndRefresh());
			// scribbleActivity.refreshRawDrawing();
			inputCooldownTask.debounce(DrawManager.DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);

			scribbleActivity.pathManager.finalizePath(currentPath);
			scribbleActivity.svgFileScribe.saveTask
				.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
		}
	});

	// Sometimes, the end erase event is never received, or comes in early.
	DebouncedTask endEraseTask = new DebouncedTask(new DebouncedTask.Callback() {
		@Override
		public void onRun() {
			XenaApplication.log("DrawManager::endEraseTask.");
			inputCooldownTask.debounce(DrawManager.DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);
		}
	});

	public DrawManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
	}

	public void onDrawBegin(PointF position) {
		if (this.scribbleActivity.isPenEraseMode) {
			this.onEraseBegin(position);
			return;
		}

		// If currently panning, that means there were erroneous panning events
		// fired. Undo them, and unset panning.
		if (this.scribbleActivity.isPanning) {
			if (this.scribbleActivity.panManager.panBeginOffset != this.scribbleActivity.pathManager
				.getViewportOffset()) {
				this.scribbleActivity.pathManager
					.setViewportOffset(this.scribbleActivity.panManager.panBeginOffset);
				this.scribbleActivity.refreshTextViewStatus();
				this.scribbleActivity.redraw(false);
			}

			XenaApplication.log("DrawManager::onDrawBegin: UNDO, viewportOffset = ",
				this.scribbleActivity.pathManager.getViewportOffset(), ".");

			this.scribbleActivity.isPanning = false;
		}

		// If currently drawing, treat this event the same as a move event.
		if (this.endDrawTask.isAwaiting()) {
			this.onDrawMove(position);
			return;
		}

		XenaApplication.log("DrawManager::onDrawBegin.");
		this.scribbleActivity.redrawTask.cancel();
		this.endDrawTask.debounce(-1);

		PointF viewportOffset
			= this.scribbleActivity.pathManager.getViewportOffset();
		float zoomScale = this.scribbleActivity.pathManager.getZoomScale();
		this.currentPath
			= this.scribbleActivity.pathManager
				.addPath(new PointF(position.x / zoomScale - viewportOffset.x,
					position.y / zoomScale - viewportOffset.y))
				.getValue();
		this.scribbleActivity.scribbleView.tentativePath.moveTo(position.x,
			position.y);
	}

	public void onDrawEnd(PointF position) {
		if (this.scribbleActivity.isPenEraseMode) {
			this.onEraseEnd(position);
			return;
		}

		this.endDrawTaskTouchPoint = new PointF(position.x, position.y);
		this.endDrawTask.debounce(DEBOUNCE_END_DRAW_DELAY_MS);
	}

	public void onDrawMove(PointF position) {
		if (this.scribbleActivity.isPenEraseMode) {
			this.onEraseMove(position);
			return;
		}

		if (!this.endDrawTask.isAwaiting()) {
			return;
		}

		this.endDrawTask.debounce(-1);

		PointF viewportOffset
			= this.scribbleActivity.pathManager.getViewportOffset();
		float zoomScale = this.scribbleActivity.pathManager.getZoomScale();
		PointF lastPoint
			= this.currentPath.points.get(this.currentPath.points.size() - 1),
			newPoint
				= new PointF(position.x / zoomScale - viewportOffset.x,
					position.y / zoomScale - viewportOffset.y);

		if (this.currentPath.points.size() >= 1 && Geometry.distance(lastPoint,
			newPoint) < DrawManager.DRAW_MOVE_EPSILON_PX) {
			return;
		}

		this.currentPath.addPoint(newPoint);
		this.scribbleActivity.scribbleView.tentativePath.lineTo(position.x,
			position.y);
		this.scribbleActivity.redraw(false);
	}

	public void onEraseBegin(PointF position) {
		if (this.endEraseTask.isAwaiting()) {
			// This should be interpreted as an erase move event.
			this.onEraseMove(position);
			return;
		}

		XenaApplication.log("DrawManager::onEraseBegin.");

		// This is the beginning of an erase move sequence.
		this.endEraseTask.debounce(DrawManager.DEBOUNCE_END_ERASE_DELAY_MS);
		this.scribbleActivity.isPanning = false;

		// Only refresh if needed.
		this.scribbleActivity.redraw(this.scribbleActivity.redrawTask.isAwaiting());

		this.previousErasePoint.set(
			position.x / this.scribbleActivity.pathManager.getZoomScale()
				- this.scribbleActivity.pathManager.getViewportOffset().x,
			position.y / this.scribbleActivity.pathManager.getZoomScale()
				- this.scribbleActivity.pathManager.getViewportOffset().y);
	}

	public void onEraseEnd(PointF position) {
		if (!this.endEraseTask.isAwaiting()) {
			return;
		}

		XenaApplication.log("DrawManager::onEraseEnd.");

		// Process this event as a move event.
		this.onEraseMove(position);
	}

	public void onEraseMove(PointF position) {
		// Some events come after onEndRawErasing; instead of ignoring those
		// events, we restart erasing at this time.
		if (!this.endEraseTask.isAwaiting()) {
			this.onEraseBegin(position);
		} else {
			this.endEraseTask.debounce(DrawManager.DEBOUNCE_END_ERASE_DELAY_MS);
		}

		// Actual logic to handle erasing.
		int initialSize = this.scribbleActivity.pathManager.getPathsCount();
		PointF currentErasePoint
			= new PointF(
				position.x / this.scribbleActivity.pathManager.getZoomScale()
					- this.scribbleActivity.pathManager.getViewportOffset().x,
				position.y / this.scribbleActivity.pathManager.getZoomScale()
					- this.scribbleActivity.pathManager.getViewportOffset().y);

		// Only process this event if significantly different from previous point.
		if (Geometry.distance(this.previousErasePoint,
			currentErasePoint) <= DrawManager.ERASE_MOVE_EPSILON_PX) {
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
			this.scribbleActivity.redraw(false);
			this.scribbleActivity.svgFileScribe.saveTask
				.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
		}

		this.previousErasePoint.set(currentErasePoint);
	}
}
