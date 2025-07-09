package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.filesystem.SvgFileScribe;
import com.gilgamesh.xena.multithreading.DebouncedTask;
import com.gilgamesh.xena.pdf.PdfReader;
import com.gilgamesh.xena.BaseActivity;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

import com.onyx.android.sdk.pen.TouchHelper;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
	private TextView textViewPath;
	private TextView textViewStatus;
	View drawEraseToggle;
	private View drawPanToggle;
	private LinearLayout coordinateDialog;
	private EditText coordinateEditPalm;
	private EditText coordinateEditX;
	private EditText coordinateEditY;

	// State is package-private.
	boolean isPanning = false;
	boolean isPenEraseMode = false;

	static public enum PenTouchMode {
		DEFAULT, FORCE_DRAW, FORCE_PAN
	};

	PenTouchMode penTouchMode = PenTouchMode.DEFAULT;

	// Switching orientation may rebuild the activity.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_scribble);

		this.textViewPath = findViewById(R.id.activity_scribble_text_view_path);
		this.textViewStatus = findViewById(R.id.activity_scribble_text_view_status);
		this.drawEraseToggle
			= findViewById(R.id.activity_scribble_draw_erase_toggle);
		this.drawPanToggle = findViewById(R.id.activity_scribble_draw_pan_toggle);
		this.coordinateDialog
			= findViewById(R.id.activity_scribble_coordinate_dialog);
		this.coordinateEditPalm
			= findViewById(R.id.activity_scribble_coordinate_dialog_edit_palm);
		this.coordinateEditPalm.setTransformationMethod(null);
		this.coordinateEditX
			= findViewById(R.id.activity_scribble_coordinate_dialog_edit_x);
		this.coordinateEditX.setTransformationMethod(null);
		this.coordinateEditY
			= findViewById(R.id.activity_scribble_coordinate_dialog_edit_y);
		this.coordinateEditY.setTransformationMethod(null);

		this.scribbleView = findViewById(R.id.activity_scribble_scribble_view);
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
		if (this.touchHelper != null) {
			this.touchHelper.setRawDrawingEnabled(false)
				.setLimitRect(new Rect(0, 0, this.scribbleView.getWidth(),
					this.scribbleView.getHeight()), this.getRawDrawingExclusions())
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
				.setRawDrawingEnabled(true);
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
		if (this.panManager.maybeIgnore(false, System.currentTimeMillis())) {
			return;
		}

		switch (v.getId()) {
			case R.id.activity_scribble_text_view_path:
				XenaApplication
					.log("ScribbleActivity::onClick: activity_scribble_text_view_path.");
				this.redraw(this.redrawTask.isAwaiting());
				this.svgFileScribe.saveTask.debounce(0);
				break;
			case R.id.activity_scribble_text_view_status:
				XenaApplication.log(
					"ScribbleActivity::onClick: activity_scribble_text_view_status.");
				PointF viewportOffset = this.pathManager.getViewportOffset();
				this.coordinateEditPalm.setText(String
					.valueOf((int) Math.round(this.panManager.getPalmTouchThreshold())));
				this.coordinateEditX.setText(String.valueOf((int) Math
					.floor(-viewportOffset.x / ScribbleActivity.PIXELS_PER_PAGE.x)));
				this.coordinateEditY.setText(String.valueOf((int) Math
					.floor(-viewportOffset.y / ScribbleActivity.PIXELS_PER_PAGE.y)));
				this.coordinateDialog.setVisibility(View.VISIBLE);
				this.redraw(this.redrawTask.isAwaiting());
				this.touchHelper.closeRawDrawing();
				break;
			case R.id.activity_scribble_draw_erase_toggle:
				XenaApplication.log(
					"ScribbleActivity::onClick: activity_scribble_draw_erase_toggle.");
				this.isPenEraseMode = !this.isPenEraseMode;
				this.drawEraseToggle.setBackgroundResource(this.isPenEraseMode
					? R.drawable.solid_empty
					: R.drawable.dotted_empty);
				this.redraw(this.redrawTask.isAwaiting());
				break;
			case R.id.activity_scribble_draw_pan_toggle:
				XenaApplication
					.log("ScribbleActivity::onClick: activity_scribble_draw_pan_toggle.");
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
			case R.id.activity_scribble_coordinate_dialog_set:
				XenaApplication.log(
					"ScribbleActivity::onClick: activity_scribble_coordinate_dialog_set, coordinates = (",
					this.coordinateEditX.getText().toString(), ", ",
					this.coordinateEditY.getText().toString(), ").");
				this.pathManager.setViewportOffset(new PointF(
					-Integer.parseInt(this.coordinateEditX.getText().toString())
						* ScribbleActivity.PIXELS_PER_PAGE.x,
					-Integer.parseInt(this.coordinateEditY.getText().toString())
						* ScribbleActivity.PIXELS_PER_PAGE.y));
				this.panManager.setPalmTouchThreshold(
					Float.parseFloat(this.coordinateEditPalm.getText().toString()));
				this.refreshTextViewStatus();
				this.coordinateDialog.setVisibility(View.GONE);

				View view = this.getCurrentFocus();
				if (view != null) {
					InputMethodManager imm
						= (InputMethodManager) getSystemService(
							Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
				}

				// this.redraw(true);
				this.openTouchHelperRawDrawing();
				break;
		}
	}

	private void onScribbleViewReady() {
		this.pathManager
			= new PathManager(
				new Point(this.scribbleView.getWidth(), this.scribbleView.getHeight()));

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
		this.touchHelper
			= TouchHelper.create(scribbleView, this.penManager)
				.setRawDrawingEnabled(false)
				.setLimitRect(new Rect(0, 0, this.scribbleView.getWidth(),
					this.scribbleView.getHeight()), this.getRawDrawingExclusions())
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
				.setRawDrawingEnabled(true);

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

		this.redraw(true);
		this.openTouchHelperRawDrawing();
	}

	// Always "invalidates", i.e. sets scribbleView dirty, and guarantees a redraw
	// in the future, which may accumulate several redraw calls. It is guaranteed
	// that all changes before this function is called will be drawn, eventually.
	public void redraw(boolean refreshRawDrawing) {
		this.redrawTask.cancel();
		this.scribbleView.invalidate();
		if (refreshRawDrawing) {
			this.touchHelper.setRawDrawingEnabled(false).setRawDrawingEnabled(true);
		}
	}

	void refreshTextViewStatus() {
		PointF viewportOffset = this.pathManager.getViewportOffset();
		this.textViewStatus.setText(
			(int) Math.floor(-viewportOffset.x / ScribbleActivity.PIXELS_PER_PAGE.x)
				+ ", "
				+ (int) Math
					.floor(-viewportOffset.y / ScribbleActivity.PIXELS_PER_PAGE.y)
				+ " | " + Math.round(this.pathManager.getZoomScale() * 100) + "%");
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
		ArrayList<Rect> exclusions = new ArrayList<Rect>();
		exclusions.add(
			new Rect(this.drawEraseToggle.getLeft(), this.drawEraseToggle.getTop(),
				this.drawEraseToggle.getRight(), this.drawEraseToggle.getBottom()));
		exclusions
			.add(new Rect(this.drawPanToggle.getLeft(), this.drawPanToggle.getTop(),
				this.drawPanToggle.getRight(), this.drawPanToggle.getBottom()));
		return exclusions;
	}

	void openTouchHelperRawDrawing() {
		float zoomScale = 1f;
		if (this.pathManager != null) {
			zoomScale = this.pathManager.getZoomScale();
		}
		this.touchHelper
			.setLimitRect(new Rect(0, 0, this.scribbleView.getWidth(),
				this.scribbleView.getHeight()), this.getRawDrawingExclusions())
			.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL).openRawDrawing()
			.setRawDrawingEnabled(true);
		this.setStrokeWidthScale(zoomScale);
	}

	// Slightly wider than anticipated due to the lack of anti-aliasing on
	// temporary lines.
	void setStrokeWidthScale(float scale) {
		this.touchHelper
			.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX * scale * 1.2f);
	}
}
