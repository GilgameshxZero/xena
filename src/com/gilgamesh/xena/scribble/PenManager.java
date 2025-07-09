package com.gilgamesh.xena.scribble;

import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.pen.RawInputCallback;

import android.graphics.PointF;
import android.graphics.RectF;

public class PenManager extends RawInputCallback {
	private ScribbleActivity scribbleActivity;

	public PenManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
	}

	@Override
	public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			this.scribbleActivity.panManager.onActionDown(0,
				new PointF(touchPoint.x, touchPoint.y), 0);
			return;
		}

		this.scribbleActivity.drawManager
			.onDrawBegin(new PointF(touchPoint.x, touchPoint.y));
	}

	@Override
	public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			// End pan is only fired after the draw delay elapses, in case the pen
			// multi-fires.
			this.scribbleActivity.drawManager.endDrawTaskTouchPoint
				= new PointF(touchPoint.x, touchPoint.y);
			this.scribbleActivity.drawManager.endDrawTask
				.debounce(DrawManager.DEBOUNCE_END_DRAW_DELAY_MS);
		} else {
			this.scribbleActivity.drawManager
				.onDrawEnd(new PointF(touchPoint.x, touchPoint.y));
		}
	}

	@Override
	public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_PAN) {
			this.scribbleActivity.panManager.onActionMove(0,
				new PointF(touchPoint.x, touchPoint.y), 0);
			return;
		}

		if (this.scribbleActivity.isPenEraseMode) {
			this.scribbleActivity.drawManager
				.onEraseMove(new PointF(touchPoint.x, touchPoint.y));
		} else {
			this.scribbleActivity.drawManager
				.onDrawMove(new PointF(touchPoint.x, touchPoint.y));
		}
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

		this.scribbleActivity.drawManager
			.onEraseBegin(new PointF(touchPoint.x, touchPoint.y));
	}

	@Override
	public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
		if (!this.scribbleActivity.isPenEraseMode
			&& this.scribbleActivity.penTouchMode != ScribbleActivity.PenTouchMode.DEFAULT) {
			return;
		}

		this.scribbleActivity.drawManager
			.onEraseEnd(new PointF(touchPoint.x, touchPoint.y));
	}

	@Override
	public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
		if (!this.scribbleActivity.isPenEraseMode
			&& this.scribbleActivity.penTouchMode != ScribbleActivity.PenTouchMode.DEFAULT) {
			return;
		}

		this.scribbleActivity.drawManager
			.onEraseMove(new PointF(touchPoint.x, touchPoint.y));
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
