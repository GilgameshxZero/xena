package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.XenaApplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class HoverView extends ImageView {
	static final Paint PAINT_BLACK;
	static {
		PAINT_BLACK = new Paint();
		PAINT_BLACK.setAntiAlias(true);
		PAINT_BLACK.setColor(Color.BLACK);
		PAINT_BLACK.setStyle(Paint.Style.STROKE);
		PAINT_BLACK.setStrokeJoin(Paint.Join.ROUND);
		PAINT_BLACK.setStrokeCap(Paint.Cap.ROUND);
		PAINT_BLACK.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX);
	}

	PointF location
		= new PointF(-ScribbleActivity.STROKE_WIDTH_PX,
			-ScribbleActivity.STROKE_WIDTH_PX);

	public HoverView(Context context) {
		super(context);
	}

	public HoverView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HoverView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public HoverView(Context context, AttributeSet attrs, int defStyleAttr,
		int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	synchronized protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		XenaApplication.log("HoverView::onDraw: ", this.location, ".");

		canvas.drawCircle(this.location.x, this.location.y,
			ScribbleActivity.STROKE_WIDTH_PX / 2, PAINT_BLACK);
	}
}
