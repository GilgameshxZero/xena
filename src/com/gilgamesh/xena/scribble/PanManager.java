package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;
import com.gilgamesh.xena.filesystem.SvgFileScribe;

import android.content.SharedPreferences;
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

	static private final String SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE
		= "SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE";
	static private final float PALM_TOUCH_THRESHOLD_DEFAULT = 9999;
	static private float PALM_TOUCH_THRESHOLD;

	PointF panBeginOffset;
	private PointF previousPoint;
	private long actionDownTimeMs;
	private float actionSizeMax;
	private long zoomDownTimeMs;
	private float zoomBeginDistance;
	// POINTER events may fire multiple times; only take normal sequences.
	private boolean zoomDownMatched = true;
	private long previousTapTimeMs = 0;
	private long previousIgnoreChainTimeMs = 0;

	private ScribbleActivity scribbleActivity;

	public PanManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;

		PanManager.PALM_TOUCH_THRESHOLD
			= Float.parseFloat(XenaApplication.preferences.getString(
				PanManager.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE,
				String.valueOf(PanManager.PALM_TOUCH_THRESHOLD_DEFAULT)));
	}

	public void setPalmTouchThreshold(float newThreshold) {
		PanManager.PALM_TOUCH_THRESHOLD = newThreshold;

		SharedPreferences.Editor editor = XenaApplication.preferences.edit();
		editor.putString(PanManager.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE,
			String.valueOf(newThreshold));
		editor.commit();
	}

	public float getPalmTouchThreshold() {
		return PanManager.PALM_TOUCH_THRESHOLD;
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

	public void onActionDown(int idx, PointF position, float sizeMax) {
		long currentTimeMs = System.currentTimeMillis();

		XenaApplication.log("PanManager::onActionDown: DOWN, idx = ", idx,
			", position = ", position, ".");
		if (this.maybeIgnore(false, currentTimeMs)) {
			return;
		}

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
			this.zoomDownMatched = false;
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
		if (this.actionSizeMax >= PanManager.PALM_TOUCH_THRESHOLD) {
			this.maybeIgnore(true, currentTimeMs);
			return;
		}

		PointF viewportOffset
			= this.scribbleActivity.pathManager.getViewportOffset();
		float zoomScale = this.scribbleActivity.pathManager.getZoomScale();
		if (idx == 0) {
			if (!this.scribbleActivity.isPanning || this.zoomDownMatched == false) {
				this.zoomDownMatched = true;
				this.maybeIgnore(true, currentTimeMs);
				return;
			}
			this.zoomDownMatched = true;

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
				this.scribbleActivity.redraw(true, true);
			} else {
				// If not panned, treat as a tap.
				if (eventDurationMs < PanManager.TAP_UPPER_BOUND_MS) {
					if (currentTimeMs
						- this.previousTapTimeMs <= PanManager.DOUBLE_TAP_UPPER_BOUND_MS) {
						XenaApplication.log("PanManager::onActionUp: DOUBLE_TAP.");
						this.scribbleActivity.isPenEraseMode
							= !this.scribbleActivity.isPenEraseMode;
						this.scribbleActivity.drawEraseToggle
							.setBackgroundResource(this.scribbleActivity.isPenEraseMode
								? R.drawable.solid_empty
								: R.drawable.dotted_empty);
						this.scribbleActivity.redraw(true, true);
						this.previousTapTimeMs = 0;
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
					this.scribbleActivity.redraw(true, true);
				} else {
					this.maybeIgnore(true, currentTimeMs);
				}
			}
		} else {
			float zoomEndDistance = Geometry.distance(this.previousPoint, position);
			boolean wasMatched = this.zoomDownMatched, zoomChanged = false;
			this.zoomDownMatched = true;

			if (currentTimeMs - this.zoomDownTimeMs < PanManager.ZOOM_LOWER_BOUND_MS
				|| wasMatched
				|| this.actionSizeMax >= PanManager.PALM_TOUCH_THRESHOLD) {
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
				this.scribbleActivity.setStrokeWidthScale(zoomScale);
				this.scribbleActivity.refreshTextViewStatus();
				this.scribbleActivity.redraw(true, true);
				this.scribbleActivity.svgFileScribe.saveTask
					.debounce(SvgFileScribe.DEBOUNCE_SAVE_MS);
			} else {
				this.maybeIgnore(true, currentTimeMs);
			}

			// Cancel panning by remaining finger.
			this.scribbleActivity.isPanning = false;
		}
	}

	public void onActionMove(int idx, PointF position, float sizeMax) {
		long currentTimeMs = System.currentTimeMillis(),
			eventDurationMs = currentTimeMs - this.actionDownTimeMs;

		// Don't process until we exit flick range.
		if (this.maybeIgnore(false, currentTimeMs)
			|| eventDurationMs <= PanManager.FLICK_UPPER_BOUND_MS) {
			return;
		}

		this.actionSizeMax = Math.max(this.actionSizeMax, sizeMax);
		if (!this.scribbleActivity.isPanning
			|| this.actionSizeMax >= PanManager.PALM_TOUCH_THRESHOLD
			|| !this.zoomDownMatched) {
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
		this.scribbleActivity.redraw(false,
			this.scribbleActivity.redrawTask.isAwaiting());
	}
}
