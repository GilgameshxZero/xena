package com.gilgamesh.xena.scribble;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.ImageView;
import android.util.AttributeSet;

import android.content.Context;

public class ScribbleView extends ImageView {
	static private final Paint PAINT_TENTATIVE_LINE;
	static {
		PAINT_TENTATIVE_LINE = new Paint();
		PAINT_TENTATIVE_LINE.setAntiAlias(true);
		PAINT_TENTATIVE_LINE.setColor(Color.BLACK);
		PAINT_TENTATIVE_LINE.setStyle(Paint.Style.STROKE);
		PAINT_TENTATIVE_LINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TENTATIVE_LINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_TENTATIVE_LINE.setStrokeWidth(Chunk.STROKE_WIDTH);
	}

	private boolean isDrawing = false;
	private Path tentativePoints = new Path();

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
		if (!this.tentativePoints.isEmpty()) {
			canvas.drawPath(this.tentativePoints, PAINT_TENTATIVE_LINE);
		}
		this.isDrawing = false;
	}

	public void addTentativePoint(float x, float y) {
		if (this.tentativePoints.isEmpty()) {
			this.tentativePoints.moveTo(x, y);
		} else {
			this.tentativePoints.lineTo(x, y);
		}
	}

	public void clearTentativePoints() {
		this.tentativePoints.rewind();
	}
}
