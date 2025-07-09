package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.pdf.PageBitmap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ScribbleView extends ImageView {
	static private final Paint PAINT_BITMAP;
	static {
		PAINT_BITMAP = new Paint();
		PAINT_BITMAP.setAntiAlias(true);
		PAINT_BITMAP.setFilterBitmap(true);
	}
	static final Paint PAINT_WHITE;
	static {
		PAINT_WHITE = new Paint();
		PAINT_WHITE.setAntiAlias(true);
		PAINT_WHITE.setColor(Color.WHITE);
	}

	private boolean isDrawing = false;
	private PointF viewportOffset, viewSize = new PointF();
	private float zoomScale;

	ScribbleActivity scribbleActivity = null;

	public ScribbleView(Context context) {
		super(context);
	}

	public ScribbleView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ScribbleView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public ScribbleView(Context context, AttributeSet attrs, int defStyleAttr,
		int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	@Override
	public void invalidate() {
		this.isDrawing = true;
		super.invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (this.scribbleActivity == null
			|| this.scribbleActivity.pathManager == null) {
			return;
		}

		this.viewSize.x = this.getWidth();
		this.viewSize.y = this.getHeight();
		this.viewportOffset = this.scribbleActivity.pathManager.getViewportOffset();
		this.zoomScale = this.scribbleActivity.pathManager.getZoomScale();

		canvas.drawRect(0, 0, this.viewSize.x, this.viewSize.y,
			ScribbleView.PAINT_WHITE);

		if (this.scribbleActivity.pdfReader != null) {
			for (PageBitmap page : this.scribbleActivity.pdfReader
				.getBitmapsForViewport(
					new RectF(-this.viewportOffset.x, -this.viewportOffset.y,
						-this.viewportOffset.x + this.viewSize.x / this.zoomScale,
						-this.viewportOffset.y + this.viewSize.y / this.zoomScale))) {
				canvas.drawBitmap(page.bitmap,
					new Rect(0, 0, page.bitmap.getWidth(), page.bitmap.getHeight()),
					new RectF(
						(page.location.left + this.viewportOffset.x) * this.zoomScale,
						(page.location.top + this.viewportOffset.y) * this.zoomScale,
						(page.location.right + this.viewportOffset.x) * this.zoomScale,
						(page.location.bottom + this.viewportOffset.y) * this.zoomScale),
					ScribbleView.PAINT_BITMAP);
			}
		}

		for (Chunk chunk : this.scribbleActivity.pathManager.getVisibleChunks()) {
			canvas.drawBitmap(chunk.getBitmap(),
				new Rect(0, 0, this.scribbleActivity.pathManager.CHUNK_SIZE.x,
					this.scribbleActivity.pathManager.CHUNK_SIZE.y),
				new RectF((this.viewportOffset.x + chunk.OFFSET_X) * this.zoomScale,
					(this.viewportOffset.y + chunk.OFFSET_Y) * this.zoomScale,
					(this.viewportOffset.x + chunk.OFFSET_X
						+ this.scribbleActivity.pathManager.CHUNK_SIZE.x) * this.zoomScale,
					(this.viewportOffset.y + chunk.OFFSET_Y
						+ this.scribbleActivity.pathManager.CHUNK_SIZE.y) * this.zoomScale),
				ScribbleView.PAINT_BITMAP);
		}

		this.isDrawing = false;
	}

	public boolean isDrawing() {
		return this.isDrawing;
	}
}
