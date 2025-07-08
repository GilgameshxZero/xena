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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.io.File;

public class ScribbleActivity extends Activity
		implements View.OnClickListener {
	static public final float STROKE_WIDTH_DP = 2.5f;
	static public final float STROKE_WIDTH_PX = ScribbleActivity.STROKE_WIDTH_DP
			* XenaApplication.DPI / 160;

	// `final` is deceptive for mutable objects.
	static final Paint PAINT_TENTATIVE_LINE;
	static {
		PAINT_TENTATIVE_LINE = new Paint();
		PAINT_TENTATIVE_LINE.setAntiAlias(true);
		PAINT_TENTATIVE_LINE.setColor(Color.BLACK);
		PAINT_TENTATIVE_LINE.setStyle(Paint.Style.STROKE);
		PAINT_TENTATIVE_LINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TENTATIVE_LINE.setStrokeCap(Paint.Cap.ROUND);
	}
	static private final Paint PAINT_TRANSPARENT;
	static {
		PAINT_TRANSPARENT = new Paint();
		PAINT_TRANSPARENT.setAntiAlias(true);
		PAINT_TRANSPARENT
				.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
		PAINT_TRANSPARENT.setColor(Color.TRANSPARENT);
	}
	static private final Paint PAINT_BITMAP;
	static {
		PAINT_BITMAP = new Paint();
		PAINT_BITMAP.setAntiAlias(true);
		PAINT_BITMAP.setFilterBitmap(true);
	}
	static public final String EXTRA_SVG_PATH = "EXTRA_SVG_PATH";
	static public final String EXTRA_PDF_PATH = "EXTRA_PDF_PATH";
	static private final int TEXT_VIEW_PATH_SUFFIX_LENGTH = 24;
	static private PointF PIXELS_PER_PAGE = new PointF(
			(int) XenaApplication.DPI * 8.5f, XenaApplication.DPI * 11f);

	// Managers.
	SvgFileScribe svgFileScribe;
	PathManager pathManager;
	private PdfReader pdfReader;
	PenManager penManager;
	TouchManager touchManager;

	// General aliases.
	ScribbleView scribbleView;
	private Bitmap scribbleViewBitmap;
	Canvas scribbleViewCanvas;
	TouchHelper touchHelper;
	Uri svgUri;
	// pdfUri is null if no PDF is loaded.
	private Uri pdfUri;
	private TextView textViewPath;
	private TextView textViewStatus;
	View drawEraseToggle;
	private View drawPanToggle;
	private LinearLayout coordinateDialog;
	private EditText coordinateEditX;
	private EditText coordinateEditY;
	private Button coordinateButtonSet;

	// State is package-private.
	boolean isDrawing = false;
	boolean isErasing = false;
	boolean isRedrawing = false;
	boolean isInputCooldown = false;
	boolean isPanning = false;
	boolean isPenEraseMode = false;

	static public enum PenTouchMode {
		DEFAULT,
		FORCE_DRAW,
		FORCE_PAN
	};

	PenTouchMode penTouchMode = PenTouchMode.DEFAULT;

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

		this.textViewPath = findViewById(R.id.activity_scribble_text_view_path);
		this.textViewStatus = findViewById(R.id.activity_scribble_text_view_status);
		this.drawEraseToggle = findViewById(
				R.id.activity_scribble_draw_erase_toggle);
		this.drawPanToggle = findViewById(
				R.id.activity_scribble_draw_pan_toggle);
		this.coordinateDialog = findViewById(
				R.id.activity_scribble_coordinate_dialog);
		this.coordinateEditX = findViewById(
				R.id.activity_scribble_coordinate_dialog_edit_x);
		this.coordinateEditX.setTransformationMethod(null);
		this.coordinateEditY = findViewById(
				R.id.activity_scribble_coordinate_dialog_edit_y);
		this.coordinateEditY.setTransformationMethod(null);
		this.coordinateButtonSet = findViewById(
				R.id.activity_scribble_coordinate_dialog_set);

		this.svgFileScribe = new SvgFileScribe(new SvgFileScribe.Callback() {
			@Override
			public void onDebounceSaveUpdate(boolean isSaved) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateTextViewPath(isSaved);
					}
				});
			}
		});

		this.parseUri();

		this.touchManager = new TouchManager(this);
		this.penManager = new PenManager(this);

		this.scribbleView = findViewById(R.id.activity_scribble_scribble_view);
		this.scribbleView.post(new Runnable() {
			@Override
			public void run() {
				initDrawing();
			}
		});
		this.scribbleView.setOnTouchListener(this.touchManager);

		// TouchHelper must be initialized onCreate, since it is used in `onResume`.
		this.touchHelper = TouchHelper.create(scribbleView, this.penManager);
	}

	@Override
	protected void onResume() {
		this.touchHelper
				.setRawDrawingEnabled(false)
				.setLimitRect(
						new Rect(0, 0, this.scribbleView.getWidth(),
								this.scribbleView.getHeight()),
						this.getRawDrawingExclusions())
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
				.setRawDrawingEnabled(true);
		if (this.pathManager != null) {
			this.setStrokeWidthScale(this.pathManager.getZoomScale());
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		// onPause will be called when "back" is pressed as well.
		// Do not resave if already saved—avoids friction where file has been
		// processed/moved elsewhere already.
		if (!this.svgFileScribe.getIsSaved()) {
			this.svgFileScribe.debounceSave(ScribbleActivity.this, svgUri,
					this.pathManager, 0);
		}
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
		long currentTimeMs = System.currentTimeMillis();
		if (currentTimeMs
				- this.touchManager.previousIgnoreChainTimeMs <= TouchManager.IGNORE_CHAIN_BOUND_MS) {
			this.touchManager.previousIgnoreChainTimeMs = currentTimeMs;
			Log.v(XenaApplication.TAG, "ScribbleActivity::onClick:IGNORE_CHAIN");
			return;
		}

		switch (v.getId()) {
			case R.id.activity_scribble_text_view_path:
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onClick:activity_scribble_text_view_path.");
				this.penManager.cancelRedraw();
				this.redraw();
				this.svgFileScribe.debounceSave(ScribbleActivity.this, svgUri,
						this.pathManager, 0);
				break;
			case R.id.activity_scribble_text_view_status:
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onClick:activity_scribble_text_view_status.");
				PointF viewportOffset = this.pathManager.getViewportOffset();
				this.coordinateEditX.setText(String.valueOf((int) Math.floor(
						-viewportOffset.x / ScribbleActivity.PIXELS_PER_PAGE.x)));
				this.coordinateEditY.setText(String.valueOf((int) Math.floor(
						-viewportOffset.y / ScribbleActivity.PIXELS_PER_PAGE.y)));
				this.coordinateDialog.setVisibility(View.VISIBLE);
				this.penManager.cancelRedraw();
				this.redraw();
				this.touchHelper.closeRawDrawing();
				break;
			case R.id.activity_scribble_draw_erase_toggle:
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onClick:activity_scribble_draw_erase_toggle.");
				this.isPenEraseMode = !this.isPenEraseMode;
				this.drawEraseToggle
						.setBackgroundResource(this.isPenEraseMode ? R.drawable.solid_empty
								: R.drawable.dotted_empty);
				this.penManager.cancelRedraw();
				this.redraw();
				break;
			case R.id.activity_scribble_draw_pan_toggle:
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onClick:activity_scribble_draw_pan_toggle.");
				switch (this.penTouchMode) {
					case DEFAULT:
						this.penTouchMode = PenTouchMode.FORCE_DRAW;
						this.drawPanToggle.setBackgroundResource(R.drawable.solid_filled);
						break;
					case FORCE_DRAW:
						this.penTouchMode = PenTouchMode.FORCE_PAN;
						this.drawPanToggle.setBackgroundResource(R.drawable.solid_empty);
						this.touchHelper.closeRawDrawing();
						break;
					case FORCE_PAN:
						this.penTouchMode = PenTouchMode.DEFAULT;
						this.drawPanToggle.setBackgroundResource(R.drawable.dotted_empty);
						this.openTouchHelperRawDrawing();
						break;
				}
				this.penManager.cancelRedraw();
				this.redraw();
				break;
			case R.id.activity_scribble_coordinate_dialog_set:
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onClick:activity_scribble_coordinate_dialog_set. "
								+ this.coordinateEditX.getText().toString() + " "
								+ this.coordinateEditY.getText().toString());
				this.pathManager.setViewportOffset(new PointF(
						-Integer.parseInt(this.coordinateEditX.getText().toString())
								* ScribbleActivity.PIXELS_PER_PAGE.x,
						-Integer.parseInt(this.coordinateEditY.getText().toString())
								* ScribbleActivity.PIXELS_PER_PAGE.y));
				this.updateTextViewStatus();
				this.coordinateDialog.setVisibility(View.GONE);
				this.coordinateEditX.clearFocus();
				this.coordinateEditY.clearFocus();
				this.drawBitmapToView(true, true);
				this.openTouchHelperRawDrawing();
				break;
		}
	}

	private void parseUri() {
		String svgPath = this.getIntent().getStringExtra(EXTRA_SVG_PATH);
		String pdfPath = this.getIntent().getStringExtra(EXTRA_PDF_PATH);
		this.svgUri = Uri.fromFile(new File(svgPath));

		this.updateTextViewPath(true);

		if (pdfPath != null) {
			this.pdfUri = Uri.fromFile(new File(pdfPath));
			this.pdfReader = new PdfReader(this, this.pdfUri);
			Log.v(XenaApplication.TAG,
					"ScribbleActivity::parseUri: Received 2 URIs: "
							+ this.svgUri.toString()
							+ " and "
							+ this.pdfUri.toString() + ".");
		} else {
			Log.v(XenaApplication.TAG,
					"ScribbleActivity::parseUri: Received 1 URI: "
							+ this.svgUri.toString()
							+ ".");
		}
	}

	private void initDrawing() {
		this.pathManager = new PathManager(
				new Point(this.scribbleView.getWidth(), this.scribbleView.getHeight()));
		SvgFileScribe.loadPathsFromSvg(this, this.svgUri, this.pathManager);

		this.scribbleViewBitmap = Bitmap.createBitmap(this.scribbleView.getWidth(),
				this.scribbleView.getHeight(),
				Bitmap.Config.ARGB_8888);
		this.scribbleViewCanvas = new Canvas(this.scribbleViewBitmap);
		this.scribbleView.setImageBitmap(this.scribbleViewBitmap);
		drawBitmapToView(true, true);

		this.updateTextViewStatus();
		this.openTouchHelperRawDrawing();
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
				this.scribbleViewCanvas.drawBitmap(page.bitmap,
						new Rect(0, 0, page.bitmap.getWidth(), page.bitmap.getHeight()),
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
						PAINT_BITMAP);
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
					PAINT_BITMAP);
		}

		if (invalidate) {
			this.scribbleView.postInvalidate();
		}
	}

	void updateTextViewStatus() {
		PointF viewportOffset = this.pathManager.getViewportOffset();
		this.textViewStatus.setText((int) Math.floor(
				-viewportOffset.x / ScribbleActivity.PIXELS_PER_PAGE.x)
				+ ", "
				+ (int) Math.floor(
						-viewportOffset.y / ScribbleActivity.PIXELS_PER_PAGE.y)
				+ " | "
				+ Math.round(this.pathManager.getZoomScale() * 100) + "%");
	}

	void updateTextViewPath(boolean isSaved) {
		String uriString = this.pdfUri != null ? this.pdfUri.toString()
				: this.svgUri.toString();
		this.textViewPath.setText("..." + uriString.substring(uriString.length()
				- ScribbleActivity.TEXT_VIEW_PATH_SUFFIX_LENGTH));
		this.textViewPath.setBackgroundResource(
				isSaved ? R.drawable.solid_empty : R.drawable.dotted_empty);
	}

	ArrayList<Rect> getRawDrawingExclusions() {
		ArrayList<Rect> exclusions = new ArrayList<Rect>();
		exclusions.add(
				new Rect(this.drawEraseToggle.getLeft(), this.drawEraseToggle.getTop(),
						this.drawEraseToggle.getRight(), this.drawEraseToggle.getBottom()));
		exclusions.add(
				new Rect(this.drawPanToggle.getLeft(), this.drawPanToggle.getTop(),
						this.drawPanToggle.getRight(), this.drawPanToggle.getBottom()));
		return exclusions;
	}

	void openTouchHelperRawDrawing() {
		float zoomScale = 1f;
		if (this.pathManager != null) {
			zoomScale = this.pathManager.getZoomScale();
		}
		this.touchHelper
				.setLimitRect(
						new Rect(0, 0, this.scribbleView.getWidth(),
								this.scribbleView.getHeight()),
						this.getRawDrawingExclusions())
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
				.openRawDrawing().setRawDrawingEnabled(true);
		this.setStrokeWidthScale(zoomScale);
	}

	void setStrokeWidthScale(float scale) {
		this.touchHelper
				.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX * scale * 1.25f);
		ScribbleActivity.PAINT_TENTATIVE_LINE
				.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX * scale);
		Chunk.PAINT.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX * scale);
		Chunk.PAINT_ERASE
				.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX * scale * 1.5f);
	}
}
