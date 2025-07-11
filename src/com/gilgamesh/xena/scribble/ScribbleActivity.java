package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.filesystem.SvgFileScribe;
import com.gilgamesh.xena.multithreading.DebouncedTask;
import com.gilgamesh.xena.pdf.PdfReader;
import com.gilgamesh.xena.BaseActivity;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

import com.onyx.android.sdk.pen.TouchHelper;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.io.File;

public class ScribbleActivity extends BaseActivity
	implements View.OnClickListener {
	static public final float STROKE_WIDTH_DP = 2.5f;
	static public final float STROKE_WIDTH_PX
		= ScribbleActivity.STROKE_WIDTH_DP * XenaApplication.DPI / 160;

	static public final String EXTRA_SVG_PATH = "EXTRA_SVG_PATH";
	static public final String EXTRA_PDF_PATH = "EXTRA_PDF_PATH";

	static private final int TEXT_VIEW_PATH_SUFFIX_LENGTH = 24;
	static private final PointF PIXELS_PER_PAGE
		= new PointF((int) XenaApplication.DPI * 8.5f, XenaApplication.DPI * 11f);

	final DebouncedTask redrawTask
		= new DebouncedTask(new DebouncedTask.Callback() {
			@Override
			public void onRun() {
				XenaApplication.log("ScribbleActivity::redrawTask.");
				redraw(true);
			}
		});

	private final PdfReader.Callback PDF_READER_CALLBACK
		= new PdfReader.Callback() {
			public void onPageSizedIntoViewport() {
				redraw(true);
			}
		};

	// Managers. Managers use SvgFileScribe, but only when they have been attached
	// in onScribbleViewReady.
	PathManager pathManager;
	RasterManager rasterManager;
	SvgFileScribe svgFileScribe;
	DrawManager drawManager;
	PanManager panManager;
	PenManager penManager;
	TouchManager touchManager;
	PdfReader pdfReader;

	// General aliases.
	ScribbleView scribbleView;
	TouchHelper touchHelper;
	Uri svgUri;
	// pdfUri is null if no PDF is loaded.
	private Uri pdfUri;
	private LinearLayout controls;
	private View drawPanToggle;
	private TextView textViewStatus;
	private ImageView artToggle;
	private TextView textViewPath;
	View drawEraseToggle;
	private LinearLayout modal;
	private EditText modalEditX;
	private EditText modalEditY;
	private EditText modalEditZoom;

	// State is package-private.
	boolean isPanning = false;
	boolean isPenEraseMode = false;

	static public enum PenTouchMode {
		DEFAULT, FORCE_DRAW, FORCE_PAN
	};

	PenTouchMode penTouchMode = PenTouchMode.DEFAULT;

	static public enum BrushMode {
		DEFAULT, CHARCOAL
	};

	BrushMode brushMode = BrushMode.DEFAULT;

	// Switching orientation may rebuild the activity.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.scribble_activity);

		this.controls = findViewById(R.id.scribble_activity_controls);
		this.drawPanToggle = findViewById(R.id.scribble_activity_draw_pan_toggle);
		this.textViewStatus = findViewById(R.id.scribble_activity_text_status);
		this.artToggle = findViewById(R.id.scribble_activity_art_toggle);
		this.textViewPath = findViewById(R.id.scribble_activity_text_path);
		this.drawEraseToggle
			= findViewById(R.id.scribble_activity_draw_erase_toggle);
		this.modal = findViewById(R.id.scribble_activity_modal);
		this.modalEditX = findViewById(R.id.scribble_activity_modal_edit_x);
		this.modalEditY = findViewById(R.id.scribble_activity_modal_edit_y);
		this.modalEditZoom = findViewById(R.id.scribble_activity_modal_edit_zoom);

		this.scribbleView = findViewById(R.id.scribble_activity_scribble_view);
		this.scribbleView.scribbleActivity = this;
		this.scribbleView.post(new Runnable() {
			@Override
			public void run() {
				onScribbleViewReady();
			}
		});

		this.drawManager = new DrawManager(this);
		this.panManager = new PanManager(this);
		this.penManager = new PenManager(this);
		this.touchManager = new TouchManager(this);
	}

	@Override
	protected void onResume() {
		if (this.touchHelper != null && !this.touchHelper.isRawDrawingCreated()) {
			this.openTouchHelperRawDrawing();
		}
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
		if (!this.svgFileScribe.isSaved()) {
			this.svgFileScribe.saveTask.debounce(0);
		}
		if (this.touchHelper != null && this.touchHelper.isRawDrawingCreated()) {
			this.touchHelper.closeRawDrawing();
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (this.touchHelper != null && this.touchHelper.isRawDrawingCreated()) {
			this.touchHelper.closeRawDrawing();
		}
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		if (this.panManager.maybeIgnore(false, System.currentTimeMillis())) {
			return;
		}

		switch (v.getId()) {
			case R.id.scribble_activity_draw_pan_toggle:
				XenaApplication
					.log("ScribbleActivity::onClick: scribble_activity_draw_pan_toggle.");
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
				this.redraw(this.redrawTask.isAwaiting());
				break;
			case R.id.scribble_activity_text_status:
				XenaApplication
					.log("ScribbleActivity::onClick: scribble_activity_text_status.");
				PointF viewportOffset = this.pathManager.getViewportOffset();
				this.modalEditX.setText(String.valueOf((int) Math
					.floor(-viewportOffset.x / ScribbleActivity.PIXELS_PER_PAGE.x)));
				this.modalEditY.setText(String.valueOf((int) Math
					.floor(-viewportOffset.y / ScribbleActivity.PIXELS_PER_PAGE.y)));
				this.modalEditZoom
					.setText(String.valueOf(this.pathManager.getZoomStepId()));
				this.modal.setVisibility(View.VISIBLE);
				this.redraw(this.redrawTask.isAwaiting());
				this.touchHelper.closeRawDrawing();
				break;
			case R.id.scribble_activity_art_toggle:
				XenaApplication
					.log("ScribbleActivity::onClick: scribble_activity_art_toggle.");
				switch (this.brushMode) {
					case DEFAULT:
						this.touchHelper.closeRawDrawing();
						this.brushMode = BrushMode.CHARCOAL;
						this.artToggle.setBackgroundResource(R.drawable.solid_empty);
						this.openTouchHelperRawDrawing();
						break;
					case CHARCOAL:
						this.touchHelper.closeRawDrawing();
						this.brushMode = BrushMode.DEFAULT;
						this.artToggle.setBackgroundResource(R.drawable.dotted_empty);
						this.openTouchHelperRawDrawing();
						break;
				}
				break;
			case R.id.scribble_activity_text_path:
				XenaApplication
					.log("ScribbleActivity::onClick: scribble_activity_text_path.");
				this.redraw(this.redrawTask.isAwaiting());
				this.svgFileScribe.saveTask.debounce(0);
				break;
			case R.id.scribble_activity_draw_erase_toggle:
				XenaApplication.log(
					"ScribbleActivity::onClick: scribble_activity_draw_erase_toggle.");
				this.toggleDrawErase();
				break;
			case R.id.scribble_activity_modal_button_cancel:
				XenaApplication.log(
					"ScribbleActivity::onClick: scribble_activity_modal_button_cancel.");
				this.modal.setVisibility(View.GONE);
				this.openTouchHelperRawDrawing();
				break;
			case R.id.scribble_activity_modal_button_set:
				XenaApplication.log(
					"ScribbleActivity::onClick: scribble_activity_modal_button_set, coordinates = (",
					this.modalEditX.getText().toString(), ", ",
					this.modalEditY.getText().toString(), ").");
				this.pathManager.setViewportOffset(new PointF(
					-Integer.parseInt(this.modalEditX.getText().toString())
						* ScribbleActivity.PIXELS_PER_PAGE.x,
					-Integer.parseInt(this.modalEditY.getText().toString())
						* ScribbleActivity.PIXELS_PER_PAGE.y));
				this.pathManager.setZoomStepId(
					Integer.parseInt(this.modalEditZoom.getText().toString()));
				this.refreshTextViewStatus();
				this.modal.setVisibility(View.GONE);
				this.openTouchHelperRawDrawing();
				break;
		}
	}

	private void onScribbleViewReady() {
		this.pathManager
			= new PathManager(
				new Point(this.scribbleView.getWidth(), this.scribbleView.getHeight()));
		this.rasterManager = new RasterManager();

		String svgPath = this.getIntent().getStringExtra(EXTRA_SVG_PATH);
		String pdfPath = this.getIntent().getStringExtra(EXTRA_PDF_PATH);
		this.svgUri = Uri.fromFile(new File(svgPath));

		this.svgFileScribe = new SvgFileScribe(new SvgFileScribe.Callback() {
			@Override
			public void onDebounceSaveUpdate(boolean isSaved) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						refreshTextViewPath(isSaved);
					}
				});
			}
		}, this, this.svgUri, this.pathManager);
		this.svgFileScribe.load();

		// After these lines, SvgFileScribe may be called from the managers.
		this.scribbleView.setOnTouchListener(this.touchManager);

		// At this point, ScribbleActivity is ready to draw.

		// PdfReader may attempt to redraw.
		if (pdfPath != null) {
			this.pdfUri = Uri.fromFile(new File(pdfPath));
			this.pdfReader
				= new PdfReader(this, this.pdfUri, this.PDF_READER_CALLBACK);
			XenaApplication
				.log("ScribbleActivity::onScribbleViewReady: Parsed 2 URIs: "
					+ this.svgUri.toString() + " and " + this.pdfUri.toString() + ".");
		} else {
			XenaApplication
				.log("ScribbleActivity::onScribbleViewReady: Parsed 1 URI: "
					+ this.svgUri.toString() + ".");
		}

		this.refreshTextViewPath(true);
		this.refreshTextViewStatus();

		// Set raw drawing exclusions only after text views have been set.
		this.touchHelper = TouchHelper.create(scribbleView, this.penManager);
		this.openTouchHelperRawDrawing();
		this.redraw(false);
	}

	// Always "invalidates", i.e. sets scribbleView dirty, and guarantees a redraw
	// in the future, which may accumulate several redraw calls. It is guaranteed
	// that all changes before this function is called will be drawn, eventually.
	public void redraw(boolean refreshRawDrawing) {
		this.redrawTask.cancel();
		this.scribbleView.invalidate();
		if (refreshRawDrawing) {
			this.touchHelper.setRawDrawingEnabled(false).setRawDrawingEnabled(true);

			// Maybe reset to eraser mode.
			this.touchHelper.setEraserRawDrawingEnabled(this.isPenEraseMode);
			this.touchHelper.setBrushRawDrawingEnabled(!this.isPenEraseMode);
		}
	}

	void refreshTextViewStatus() {
		PointF viewportOffset = this.pathManager.getViewportOffset();
		this.textViewStatus.setText(
			(int) Math.floor(-viewportOffset.x / ScribbleActivity.PIXELS_PER_PAGE.x)
				+ ", "
				+ (int) Math
					.floor(-viewportOffset.y / ScribbleActivity.PIXELS_PER_PAGE.y)
				+ " @ " + Math.round(this.pathManager.getZoomScale() * 100) + "%");
	}

	private void refreshTextViewPath(boolean isSaved) {
		String uriString
			= this.pdfUri != null ? this.pdfUri.toString() : this.svgUri.toString();
		this.textViewPath.setText("..." + uriString.substring(
			uriString.length() - ScribbleActivity.TEXT_VIEW_PATH_SUFFIX_LENGTH));
		this.textViewPath.setBackgroundResource(
			isSaved ? R.drawable.solid_empty : R.drawable.dotted_empty);
	}

	ArrayList<Rect> getRawDrawingExclusions() {
		// Empty list is required to void any previous exclusions.
		// Failing to exclude controls may result in non-responsive touch after
		// clicking with the stylus.
		ArrayList<Rect> exclusions = new ArrayList<Rect>();
		exclusions.add(new Rect(this.controls.getLeft(), this.controls.getTop(),
			this.controls.getRight(), this.controls.getBottom()));
		return exclusions;
	}

	void toggleDrawErase() {
		this.isPenEraseMode = !this.isPenEraseMode;
		this.drawEraseToggle.setBackgroundResource(
			this.isPenEraseMode ? R.drawable.solid_empty : R.drawable.dotted_empty);
		this.redraw(this.redrawTask.isAwaiting());
		this.touchHelper.setEraserRawDrawingEnabled(this.isPenEraseMode);
		this.touchHelper.setBrushRawDrawingEnabled(!this.isPenEraseMode);
	}

	void openTouchHelperRawDrawing() {
		float zoomScale = 1f;
		if (this.pathManager != null) {
			zoomScale = this.pathManager.getZoomScale();
		}
		this.touchHelper.setLimitRect(new Rect(0, 0, this.scribbleView.getWidth(),
			this.scribbleView.getHeight()), this.getRawDrawingExclusions());
		this.touchHelper.openRawDrawing().setRawDrawingEnabled(true);
		this.refreshStrokeStyle();
		this.setStrokeWidthScale(zoomScale);
	}

	private void refreshStrokeStyle() {
		switch (this.brushMode) {
			case DEFAULT:
				this.touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
					.setStrokeColor(0xff000000);
				break;
			case CHARCOAL:
				// CHARCOAL_V2 also works as a variant of CHARCOAL.
				this.touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_CHARCOAL)
					.setStrokeColor(0xff000000);
				break;
		}
	}

	void setStrokeWidthScale(float scale) {
		float scaledWidth = ScribbleActivity.STROKE_WIDTH_PX * scale;
		switch (this.brushMode) {
			case DEFAULT:
				scaledWidth *= 1.15f;
				break;
			case CHARCOAL:
				scaledWidth *= 3f;
				break;
		}
		this.touchHelper.setStrokeWidth(scaledWidth);
		ScribbleView.PAINT_TENTATIVE.setStrokeWidth(scaledWidth);
	}
}
