package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;
import com.gilgamesh.xena.filesystem.SvgFileScribe;

import android.graphics.Point;
import android.graphics.PointF;

// Used by PanManager and alt-mode PenManager.
public class PanManager {
	static private final float FLICK_MOVE_RATIO = 0.85f;
	static private final int FLICK_LOWER_BOUND_MS = 10;
	static private final int FLICK_UPPER_BOUND_MS = 210;
	static private final float FLICK_OFFSET_THRESHOLD_DP = 10;
	static private final float FLICK_OFFSET_THRESHOLD_PX
		= PanManager.FLICK_OFFSET_THRESHOLD_DP * XenaApplication.DPI / 160;

	static private final float PAN_DISTANCE_THRESHOLD_DP = 10;
	static private final float PAN_DISTANCE_THRESHOLD_PX
		= PanManager.PAN_DISTANCE_THRESHOLD_DP * XenaApplication.DPI / 160;

	static private final float ZOOM_DISTANCE_BOUND_DP = 25;
	static private final float ZOOM_DISTANCE_BOUND_PX
		= PanManager.ZOOM_DISTANCE_BOUND_DP * XenaApplication.DPI / 160;
	static private final int ZOOM_LOWER_BOUND_MS = 80;

	static private final int TAP_UPPER_BOUND_MS = 210;
	static private final int DOUBLE_TAP_UPPER_BOUND_MS = 360;

	static private final int IGNORE_CHAIN_BOUND_MS = 210;

	static private final float ACTION_BORDER_IGNORE_DP = 8;
	static private final float ACTION_BORDER_IGNORE_PX
		= PanManager.ACTION_BORDER_IGNORE_DP * XenaApplication.DPI / 160;

	PointF panBeginOffset;
	private PointF previousPoint;
	private PointF actionDownPoint;
	private long actionDownTimeMs;
	private float actionSizeMax;
	private long zoomDownTimeMs;
	private float zoomBeginDistance;
	// POINTER events may fire multiple times; only take normal sequences.
	private long previousTapTimeMs = 0;
	private long previousIgnoreChainTimeMs = 0;

	private ScribbleActivity scribbleActivity;
	private Point scribbleActivitySize;

	public PanManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
		this.scribbleActivitySize
			= new Point(this.scribbleActivity.getWindow().getDecorView().getWidth(),
				this.scribbleActivity.getWindow().getDecorView().getHeight());
	}

	public void onActionDown(int idx, PointF position, float sizeMax) {
		long currentTimeMs = System.currentTimeMillis();

		XenaApplication.log("PanManager::onActionDown: DOWN, idx = ", idx,
			", position = ", position, ".");
		if (this.maybeIgnore(false, currentTimeMs)) {
			return;
		}

		this.actionDownPoint = position;

		if (idx == 0) {
			// Sometimes, this fires slightly before a draw/erase event. The
			// draw/erase event will cancel panning in that case.
			if (this.scribbleActivity.isPanning) {
				XenaApplication.log("PanManager::onActionDown: RESET, position = ",
					position, ".");
			} else {
				this.scribbleActivity.isPanning = true;
			}

			if (this.scribbleActivity.isPanning) {
				this.panBeginOffset
					= this.scribbleActivity.pathManager.getViewportOffset();
				this.previousPoint = position;
				this.actionDownTimeMs = currentTimeMs;
				this.actionSizeMax = sizeMax;
			}
		} else {
			this.zoomBeginDistance = Geometry.distance(this.previousPoint, position);
			this.zoomDownTimeMs = currentTimeMs;

			// Disable panning while computing zoom.
			this.scribbleActivity.isPanning = false;
		}
	}

	public void onActionUp(int idx, PointF position, float sizeMax) {
		long currentTimeMs = System.currentTimeMillis(),
			eventDurationMs = currentTimeMs - this.actionDownTimeMs;

		this.actionSizeMax = Math.max(this.actionSizeMax, sizeMax);
		XenaApplication.log("PanManager::onActionUp: UP, idx = ", idx,
			", position = ", position, ",  actionSizeMax = ", this.actionSizeMax,
			".");
		if (this.maybeIgnore(false, currentTimeMs)) {
			return;
		}
		if (this.actionSizeMax >= XenaApplication.getPalmTouchThreshold()
			|| this.isActionDownPointOnBorder()) {
			this.maybeIgnore(true, currentTimeMs);
			return;
		}

		PointF viewportOffset
			= this.scribbleActivity.pathManager.getViewportOffset();
		float zoomScale = this.scribbleActivity.pathManager.getZoomScale();
		if (idx == 0) {
			if (!this.scribbleActivity.isPanning) {
				this.maybeIgnore(true, currentTimeMs);
				return;
			}

			// Note: ACTION_UP is not guaranteed to fire after ACTION_DOWN.
			this.scribbleActivity.isPanning = false;

			// Detect flicks.
			float flickOffsetX
				= viewportOffset.x + position.x - this.previousPoint.x
					- this.panBeginOffset.x,
				flickOffsetY
					= viewportOffset.y + position.y - this.previousPoint.y
						- this.panBeginOffset.y;
			if (eventDurationMs >= PanManager.FLICK_LOWER_BOUND_MS
				&& eventDurationMs <= PanManager.FLICK_UPPER_BOUND_MS
				&& Math.abs(flickOffsetY) >= PanManager.FLICK_OFFSET_THRESHOLD_PX) {
				XenaApplication.log("PanManager::onActionUp: FLICK.");

				// Determine flick direction.
				int direction = flickOffsetY > 0 ? 1 : -1;

				this.scribbleActivity.pathManager
					.setViewportOffset(new PointF(this.panBeginOffset.x,
						this.panBeginOffset.y
							+ direction * this.scribbleActivity.scribbleView.getHeight()
								* PanManager.FLICK_MOVE_RATIO / zoomScale));
				this.scribbleActivity.refreshTextViewStatus();
				this.scribbleActivity.svgFileScribe.saveTask
					.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
				this.scribbleActivity.redraw(true);
			} else {
				// If not panned, treat as a tap.
				if (eventDurationMs < PanManager.TAP_UPPER_BOUND_MS) {
					if (currentTimeMs
						- this.previousTapTimeMs <= PanManager.DOUBLE_TAP_UPPER_BOUND_MS) {
						XenaApplication.log("PanManager::onActionUp: DOUBLE_TAP.");
						this.previousTapTimeMs = 0;
						this.scribbleActivity.toggleDrawErase();
					} else {
						XenaApplication.log("PanManager::onActionUp: TAP.");
						this.previousTapTimeMs = currentTimeMs;
					}
				} else if (eventDurationMs > PanManager.FLICK_UPPER_BOUND_MS
					&& Math.sqrt(flickOffsetX * flickOffsetX
						+ flickOffsetY * flickOffsetY) >= PAN_DISTANCE_THRESHOLD_PX) {
					XenaApplication.log("PanManager::onActionUp: PAN.");

					PointF newOffset
						= new PointF(
							viewportOffset.x
								+ (position.x - this.previousPoint.x) / zoomScale,
							viewportOffset.y
								+ (position.y - this.previousPoint.y) / zoomScale);
					this.scribbleActivity.pathManager.setViewportOffset(newOffset);

					this.scribbleActivity.refreshTextViewStatus();
					this.scribbleActivity.svgFileScribe.saveTask
						.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
					this.scribbleActivity.redraw(true);
				} else {
					this.maybeIgnore(true, currentTimeMs);
				}
			}
		} else {
			float zoomEndDistance = Geometry.distance(this.previousPoint, position);
			boolean zoomChanged = false;

			if (currentTimeMs - this.zoomDownTimeMs < PanManager.ZOOM_LOWER_BOUND_MS
				|| this.actionSizeMax >= XenaApplication.getPalmTouchThreshold()) {
				this.maybeIgnore(true, currentTimeMs);
				return;
			}

			if (this.zoomBeginDistance
				- zoomEndDistance >= PanManager.ZOOM_DISTANCE_BOUND_PX) {
				this.scribbleActivity.pathManager.zoomOut();
				zoomChanged = true;
			} else if (zoomEndDistance
				- this.zoomBeginDistance >= PanManager.ZOOM_DISTANCE_BOUND_PX) {
				this.scribbleActivity.pathManager.zoomIn();
				zoomChanged = true;
			}

			if (zoomChanged) {
				XenaApplication.log("PanManager::onActionUp: ZOOM.");
				// Boox API is buggy, so we need to close & open here.
				this.scribbleActivity.touchHelper.closeRawDrawing();
				this.scribbleActivity.openTouchHelperRawDrawing();
				this.scribbleActivity.refreshTextViewStatus();
				this.scribbleActivity.redraw(true);
				this.scribbleActivity.svgFileScribe.saveTask
					.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
			} else {
				this.maybeIgnore(true, currentTimeMs);
			}
		}
	}

	public void onActionMove(int idx, PointF position, float sizeMax) {
		long currentTimeMs = System.currentTimeMillis(),
			eventDurationMs = currentTimeMs - this.actionDownTimeMs;

		this.actionSizeMax = Math.max(this.actionSizeMax, sizeMax);

		// Don't process until we exit flick range. Not panning may be caused by
		// zoom.
		if (this.maybeIgnore(false, currentTimeMs)
			|| eventDurationMs <= PanManager.FLICK_UPPER_BOUND_MS
			|| !this.scribbleActivity.isPanning
			|| !XenaApplication.getPanUpdateEnabled()) {
			return;
		}

		if (this.actionSizeMax >= XenaApplication.getPalmTouchThreshold()
			|| this.isActionDownPointOnBorder()) {
			this.maybeIgnore(true, currentTimeMs);
			return;
		}

		PointF viewportOffset
			= this.scribbleActivity.pathManager.getViewportOffset();
		float zoomScale = this.scribbleActivity.pathManager.getZoomScale();
		this.scribbleActivity.pathManager.setViewportOffset(new PointF(
			viewportOffset.x + (position.x - this.previousPoint.x) / zoomScale,
			viewportOffset.y + (position.y - this.previousPoint.y) / zoomScale));
		this.previousPoint = position;

		// If screen is dirty, clear it.
		this.scribbleActivity.redraw(this.scribbleActivity.redrawTask.isAwaiting());
	}

	// Chain ignores if force is false, otherwise always ignore.
	public boolean maybeIgnore(boolean force, long currentTimeMs) {
		if (!force) {
			if (currentTimeMs
				- this.previousIgnoreChainTimeMs <= PanManager.IGNORE_CHAIN_BOUND_MS) {
				XenaApplication.log("PanManager::maybeIgnore: IGNORE_CHAIN.");
				this.previousIgnoreChainTimeMs = currentTimeMs;
				return true;
			}
			return false;
		} else {
			XenaApplication.log("PanManager::maybeIgnore: IGNORE.");
			this.previousIgnoreChainTimeMs = currentTimeMs;
			return true;
		}
	}

	private boolean isActionDownPointOnBorder() {
		return this.actionDownPoint.x < PanManager.ACTION_BORDER_IGNORE_PX
			|| this.actionDownPoint.y < PanManager.ACTION_BORDER_IGNORE_PX
			|| this.actionDownPoint.x > this.scribbleActivitySize.x
				- PanManager.ACTION_BORDER_IGNORE_PX
			|| this.actionDownPoint.y > this.scribbleActivitySize.y
				- PanManager.ACTION_BORDER_IGNORE_PX;
	}
}
