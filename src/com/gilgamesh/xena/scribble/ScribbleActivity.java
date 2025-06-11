package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.filesystem.SvgFileScribe;
import com.gilgamesh.xena.pdf.PageBitmap;
import com.gilgamesh.xena.pdf.PdfReader;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

import com.onyx.android.sdk.pen.TouchHelper;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class ScribbleActivity extends Activity
		implements View.OnClickListener {
	// `final` is deceptive for mutable objects.
	static final Paint PAINT_TENTATIVE_LINE;
	static {
		PAINT_TENTATIVE_LINE = new Paint();
		PAINT_TENTATIVE_LINE.setAntiAlias(true);
		PAINT_TENTATIVE_LINE.setColor(Color.BLACK);
		PAINT_TENTATIVE_LINE.setStyle(Paint.Style.STROKE);
		PAINT_TENTATIVE_LINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TENTATIVE_LINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_TENTATIVE_LINE.setStrokeWidth(Chunk.STROKE_WIDTH);
	}
	static private final Paint PAINT_TRANSPARENT;
	static {
		PAINT_TRANSPARENT = new Paint();
		PAINT_TRANSPARENT.setAntiAlias(true);
		PAINT_TRANSPARENT
				.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
		PAINT_TRANSPARENT.setColor(Color.TRANSPARENT);
	}

	static public final String EXTRA_PDF_URI = "EXTRA_PDF_URI";

	// Managers.
	SvgFileScribe svgFileScribe = new SvgFileScribe();
	PathManager pathManager;
	private PdfReader pdfReader;
	PenManager scribblePenManager;
	TouchManager scribbleTouchManager;

	// General aliases.
	ScribbleView scribbleView;
	private Bitmap scribbleViewBitmap;
	Canvas scribbleViewCanvas;
	TouchHelper touchHelper;
	Uri svgUri;
	// pdfUri is null if no PDF is loaded.
	private Uri pdfUri;
	private TextView textView;

	// State is package-private.
	boolean isDrawing = false;
	boolean isErasing = false;
	boolean isRedrawing = false;
	boolean isInputCooldown = false;
	boolean isPanning = false;
	PointF panBeginOffset = new PointF();

	public void redraw() {
		this.isRedrawing = false;
		this.drawBitmapToView(true, true);
		this.touchHelper.setRawDrawingEnabled(false).setRawDrawingEnabled(true);
	}

	// Switching orientation may rebuild the activity.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_scribble);
		this.parseUri();

		this.scribbleTouchManager = new TouchManager(this);
		this.scribblePenManager = new PenManager(this);

		this.scribbleView = findViewById(R.id.activity_scribble_scribble_view);
		this.scribbleView.post(new Runnable() {
			@Override
			public void run() {
				initDrawing();
			}
		});
		this.scribbleView.setOnTouchListener(this.scribbleTouchManager);

		this.textView = findViewById(R.id.activity_scribble_text_view);

		// TouchHelper must be initialized onCreate, since it is used in `onResume`.
		this.touchHelper = TouchHelper.create(scribbleView, this.scribblePenManager)
				.setStrokeWidth(Chunk.STROKE_WIDTH)
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL);
	}

	@Override
	protected void onResume() {
		this.touchHelper.setRawDrawingEnabled(false).setLimitRect(
				new Rect(0, 0, this.scribbleView.getWidth(),
						this.scribbleView.getHeight()),
				new ArrayList<>())
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
				.setRawDrawingEnabled(true);
		if (this.pathManager != null) {
			this.touchHelper
					.setStrokeWidth(Chunk.STROKE_WIDTH * this.pathManager.getZoomScale());
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		// onPause will be called when "back" is pressed as well.
		this.svgFileScribe.debounceSave(ScribbleActivity.this, svgUri,
				this.pathManager, 0);
		this.touchHelper.setRawDrawingEnabled(false);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		this.touchHelper.closeRawDrawing();
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
	}

	private void parseUri() {
		Uri pickedUri = this.getIntent().getData();
		this.svgUri = pickedUri;
		String pdfUriString = this.getIntent().getStringExtra(EXTRA_PDF_URI);
		if (pdfUriString != null) {
			this.pdfUri = Uri.parse(pdfUriString);
			this.pdfReader = new PdfReader(this, this.pdfUri);
			Log.v(XenaApplication.TAG,
					"ScribbleActivity::parseUri: Got 2 URIs: " + this.svgUri.toString()
							+ " and "
							+ this.pdfUri.toString() + ".");
		} else {
			Log.v(XenaApplication.TAG,
					"ScribbleActivity::parseUri: Got 1 URI: " + this.svgUri.toString()
							+ ".");
		}
	}

	private void initDrawing() {
		this.pathManager = new PathManager(
				new Point(this.scribbleView.getWidth(), this.scribbleView.getHeight()));
		SvgFileScribe.loadPathsFromSvg(ScribbleActivity.this, this.svgUri,
				this.pathManager);

		this.scribbleViewBitmap = Bitmap.createBitmap(this.scribbleView.getWidth(),
				this.scribbleView.getHeight(),
				Bitmap.Config.ARGB_8888);
		this.scribbleViewCanvas = new Canvas(this.scribbleViewBitmap);
		this.scribbleView.setImageBitmap(this.scribbleViewBitmap);
		drawBitmapToView(true, true);

		this.updateTextView(this.pathManager.getViewportOffset());

		this.touchHelper
				.setLimitRect(
						new Rect(0, 0, this.scribbleView.getWidth(),
								this.scribbleView.getHeight()),
						new ArrayList<>())
				.openRawDrawing().setRawDrawingEnabled(true);
	}

	void drawBitmapToView(boolean force, boolean invalidate) {
		if (!force && scribbleView.isDrawing()) {
			// Log.v(XenaApplication.TAG, "Dirty ScribbleView.");
			return;
		}

		this.scribbleViewCanvas.drawRect(0, 0, scribbleView.getWidth(),
				scribbleView.getHeight(),
				PAINT_TRANSPARENT);

		if (this.pdfReader != null) {
			for (PageBitmap page : this.pdfReader.getBitmapsForViewport(
					new RectF(-this.pathManager.getViewportOffset().x,
							-this.pathManager.getViewportOffset().y,
							-this.pathManager.getViewportOffset().x
									+ this.scribbleView.getWidth()
											/ this.pathManager.getZoomScale(),
							-this.pathManager.getViewportOffset().y
									+ this.scribbleView.getHeight()
											/ this.pathManager.getZoomScale()))) {
				Log.v("XENA",
						"" + new Rect(0, 0,
								Math.round(page.location.right - page.location.left),
								Math.round(page.location.bottom - page.location.top)) + " "
								+ new RectF(
										page.location.left * this.pathManager.getZoomScale()
												+ this.pathManager.getViewportOffset().x,
										page.location.top * this.pathManager.getZoomScale()
												+ this.pathManager.getViewportOffset().y,
										page.location.right * this.pathManager.getZoomScale()
												+ this.pathManager.getViewportOffset().x,
										page.location.bottom * this.pathManager.getZoomScale()
												+ this.pathManager.getViewportOffset().y));
				this.scribbleViewCanvas.drawBitmap(page.bitmap,
						new Rect(0, 0, Math.round(page.location.right - page.location.left),
								Math.round(page.location.bottom - page.location.top)),
						new RectF(
								(page.location.left
										+ this.pathManager.getViewportOffset().x)
										* this.pathManager.getZoomScale(),
								(page.location.top
										+ this.pathManager.getViewportOffset().y)
										* this.pathManager.getZoomScale(),
								(page.location.right
										+ this.pathManager.getViewportOffset().x)
										* this.pathManager.getZoomScale(),
								(page.location.bottom
										+ this.pathManager.getViewportOffset().y)
										* this.pathManager.getZoomScale()),
						null);
			}
		}

		for (Chunk chunk : this.pathManager.getVisibleChunks()) {
			this.scribbleViewCanvas.drawBitmap(chunk.getBitmap(),
					new Rect(0, 0, this.pathManager.CHUNK_SIZE.x,
							this.pathManager.CHUNK_SIZE.y),
					new RectF((this.pathManager.getViewportOffset().x
							+ chunk.OFFSET_X) * this.pathManager.getZoomScale(),
							(this.pathManager.getViewportOffset().y
									+ chunk.OFFSET_Y) * this.pathManager.getZoomScale(),
							(this.pathManager.getViewportOffset().x
									+ chunk.OFFSET_X + this.pathManager.CHUNK_SIZE.x)
									* this.pathManager.getZoomScale(),
							(this.pathManager.getViewportOffset().y
									+ chunk.OFFSET_Y + this.pathManager.CHUNK_SIZE.y)
									* this.pathManager.getZoomScale()),
					null);
		}

		if (invalidate) {
			this.scribbleView.postInvalidate();
		}
	}

	void updateTextView(PointF coordinate) {
		this.textView.setText(Math.round(coordinate.x) + ", "
				+ Math.round(coordinate.y) + " | "
				+ Math.round(this.pathManager.getZoomScale() * 100) + "%");
	}
}
