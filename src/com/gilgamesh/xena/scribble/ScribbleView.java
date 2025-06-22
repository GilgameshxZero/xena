package com.gilgamesh.xena.scribble;

import android.graphics.Canvas;
import android.widget.ImageView;
import android.util.AttributeSet;

import android.content.Context;

public class ScribbleView extends ImageView {
	private boolean isDrawing = false;

	public ScribbleView(Context context) {
		super(context);
	}

	public ScribbleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ScribbleView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ScribbleView(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void postInvalidate() {
		this.isDrawing = true;
		super.postInvalidate();
	}

	public boolean isDrawing() {
		return this.isDrawing;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		this.isDrawing = false;
	}
}
