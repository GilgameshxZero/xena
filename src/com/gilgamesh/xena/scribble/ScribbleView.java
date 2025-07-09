package com.gilgamesh.xena.scribble;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.ImageView;
import android.util.AttributeSet;

import com.gilgamesh.xena.pdf.PageBitmap;

import android.content.Context;

public class ScribbleView extends ImageView {
	private boolean isDrawing = false;
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

	public boolean isDrawing() {
		return this.isDrawing;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (this.scribbleActivity == null
			|| this.scribbleActivity.pathManager == null) {
			return;
		}

		// Point viewSize = new Point(this.getWidth(), this.getHeight());
		// PointF viewportOffset = this.scribbleActivity.pathManager
		// .getViewportOffset();

		canvas.drawRect(0, 0, this.getWidth(), this.getHeight(),
			ScribbleActivity.PAINT_WHITE);

		if (this.scribbleActivity.pdfReader != null) {
			for (PageBitmap page : this.scribbleActivity.pdfReader
				.getBitmapsForViewport(
					new RectF(-this.scribbleActivity.pathManager.getViewportOffset().x,
						-this.scribbleActivity.pathManager.getViewportOffset().y,
						-this.scribbleActivity.pathManager.getViewportOffset().x
							+ this.scribbleActivity.scribbleView.getWidth()
								/ this.scribbleActivity.pathManager.getZoomScale(),
						-this.scribbleActivity.pathManager.getViewportOffset().y
							+ this.scribbleActivity.scribbleView.getHeight()
								/ this.scribbleActivity.pathManager.getZoomScale()))) {
				canvas.drawBitmap(page.bitmap,
					new Rect(0, 0, page.bitmap.getWidth(), page.bitmap.getHeight()),
					new RectF(
						(page.location.left
							+ this.scribbleActivity.pathManager.getViewportOffset().x)
							* this.scribbleActivity.pathManager.getZoomScale(),
						(page.location.top
							+ this.scribbleActivity.pathManager.getViewportOffset().y)
							* this.scribbleActivity.pathManager.getZoomScale(),
						(page.location.right
							+ this.scribbleActivity.pathManager.getViewportOffset().x)
							* this.scribbleActivity.pathManager.getZoomScale(),
						(page.location.bottom
							+ this.scribbleActivity.pathManager.getViewportOffset().y)
							* this.scribbleActivity.pathManager.getZoomScale()),
					ScribbleActivity.PAINT_BITMAP);
			}
		}

		for (Chunk chunk : this.scribbleActivity.pathManager.getVisibleChunks()) {
			canvas.drawBitmap(chunk.getBitmap(),
				new Rect(0, 0, this.scribbleActivity.pathManager.CHUNK_SIZE.x,
					this.scribbleActivity.pathManager.CHUNK_SIZE.y),
				new RectF(
					(this.scribbleActivity.pathManager.getViewportOffset().x
						+ chunk.OFFSET_X)
						* this.scribbleActivity.pathManager.getZoomScale(),
					(this.scribbleActivity.pathManager.getViewportOffset().y
						+ chunk.OFFSET_Y)
						* this.scribbleActivity.pathManager.getZoomScale(),
					(this.scribbleActivity.pathManager.getViewportOffset().x
						+ chunk.OFFSET_X + this.scribbleActivity.pathManager.CHUNK_SIZE.x)
						* this.scribbleActivity.pathManager.getZoomScale(),
					(this.scribbleActivity.pathManager.getViewportOffset().y
						+ chunk.OFFSET_Y + this.scribbleActivity.pathManager.CHUNK_SIZE.y)
						* this.scribbleActivity.pathManager.getZoomScale()),
				ScribbleActivity.PAINT_BITMAP);
		}

		this.isDrawing = false;
	}
}
