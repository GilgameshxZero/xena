package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.XenaApplication;

import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

public class TouchManager implements View.OnTouchListener {
	private ScribbleActivity scribbleActivity;

	public TouchManager(ScribbleActivity scribbleActivity) {
		this.scribbleActivity = scribbleActivity;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean onTouch(View v, MotionEvent event) {
		if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.DEFAULT
			&& (this.scribbleActivity.drawManager.endDrawTask.isAwaiting()
				|| this.scribbleActivity.drawManager.endEraseTask.isAwaiting()
				|| this.scribbleActivity.drawManager.inputCooldownTask.isAwaiting())) {
			return false;
		}

		PointF position;
		float sizeMax;

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_MOVE:
				position = new PointF(event.getX(), event.getY());
				sizeMax = Math.max(event.getTouchMajor(), event.getTouchMinor());
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_2_DOWN:
			case MotionEvent.ACTION_POINTER_2_UP:
				position = new PointF(event.getX(1), event.getY(1));
				sizeMax = Math.max(event.getTouchMajor(1), event.getTouchMinor(1));
				break;
			default:
				XenaApplication.log(
					"TouchManager::onTouch: OTHER, event.getAction() = ",
					event.getAction(), ".");
				this.scribbleActivity.panManager.maybeIgnore(true,
					System.currentTimeMillis());
				return false;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.drawManager.onDrawBegin(position);
				} else {
					this.scribbleActivity.panManager.onActionDown(0, position, sizeMax);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.drawManager.onDrawEnd(position);
				} else {
					this.scribbleActivity.panManager.onActionUp(0, position, sizeMax);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (this.scribbleActivity.penTouchMode == ScribbleActivity.PenTouchMode.FORCE_DRAW) {
					this.scribbleActivity.drawManager.onDrawMove(position);
				} else {
					this.scribbleActivity.panManager.onActionMove(0, position, sizeMax);
				}
				break;
			// Deprecated events may still be sent by Boox API.
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_2_DOWN:
				this.scribbleActivity.panManager.onActionDown(1, position, sizeMax);
				break;
			case MotionEvent.ACTION_POINTER_UP:
			case MotionEvent.ACTION_POINTER_2_UP:
				this.scribbleActivity.panManager.onActionUp(1, position, sizeMax);
				break;
		}

		return true;
	}
}
