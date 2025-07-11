package com.gilgamesh.xena.scribble;

import java.util.concurrent.atomic.AtomicBoolean;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.pdf.PageBitmap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
	static final Paint PAINT_TENTATIVE;
	static {
		PAINT_TENTATIVE = new Paint();
		PAINT_TENTATIVE.setAntiAlias(true);
		PAINT_TENTATIVE.setColor(Color.BLACK);
		PAINT_TENTATIVE.setStyle(Paint.Style.STROKE);
		PAINT_TENTATIVE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TENTATIVE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_TENTATIVE.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX);
	}

	private AtomicBoolean isDirty = new AtomicBoolean();
	private PointF viewportOffset, viewSize = new PointF();
	private float zoomScale;

	// Drawn atop all other bitmaps.
	Path tentativePath = null;

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
		this.isDirty.set(true);
		XenaApplication.log("ScribbleView::invalidate: Queued draw.");
		super.invalidate();
	}

	@Override
	synchronized protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (this.scribbleActivity == null
			|| this.scribbleActivity.pathManager == null) {
			return;
		}

		// Together with `synchronized`, allows non-forcing queued invalidates.
		if (!this.isDirty.getAndSet(false)) {
			XenaApplication.log("ScribbleView::onDraw: Refreshing.");
		}
		XenaApplication.log("ScribbleView::onDraw: Drawing.");

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

		if (this.tentativePath != null) {
			canvas.drawPath(this.tentativePath, ScribbleView.PAINT_TENTATIVE);
		}
	}
}
