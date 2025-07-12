package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.scribble.ScribbleActivity;
import com.gilgamesh.xena.BaseActivity;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class FilePickerActivity extends BaseActivity
	implements View.OnClickListener {
	static private final int REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE = 0;
	static private final String SHARED_PREFERENCES_EDIT_TEXT
		= "SHARED_PREFERENCES_EDIT_TEXT";
	static private final Point MIN_PANE_SIZE_DP = new Point(384, 72);
	static private final Point MIN_PANE_SIZE_PX
		= new Point(
			FilePickerActivity.MIN_PANE_SIZE_DP.x * XenaApplication.DPI / 160,
			FilePickerActivity.MIN_PANE_SIZE_DP.y * XenaApplication.DPI / 160);
	static private final int MARGIN_SIZE_DP = 6;
	static private final int MARGIN_SIZE_PX
		= FilePickerActivity.MARGIN_SIZE_DP * XenaApplication.DPI / 160;
	static private final LayoutParams LISTING_ROW_PARAMS
		= new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

	private Point LISTING_SIZE;
	private Point GRID_DIMENSIONS;
	private int PANES_PER_PAGE;
	private Point PANE_SIZE;
	private LayoutParams LISTING_PARAMS;

	private EditText editText;
	private LinearLayout listingLayout;
	private LinearLayout modal;
	private EditText modalEditPalm;
	private ImageView modalEditPanUpdate;
	private EditText modalEditDrawRefresh;

	private FilePickerTouchManager touchManager;

	private boolean tentativeModalEditPanUpdateState;
	private boolean ready = false;

	int listingPage = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.file_picker_activity);
		this.editText = findViewById(R.id.file_picker_activity_edit_text);
		this.listingLayout = findViewById(R.id.file_picker_activity_layout_listing);
		this.modal = findViewById(R.id.file_picker_activity_modal);
		this.modalEditPalm
			= findViewById(R.id.file_picker_activity_modal_edit_palm);
		this.modalEditPanUpdate
			= findViewById(R.id.file_picker_activity_modal_edit_pan_update);
		this.modalEditDrawRefresh
			= findViewById(R.id.file_picker_activity_modal_edit_draw_refresh);

		this.touchManager = new FilePickerTouchManager(this);

		this.listingLayout.getViewTreeObserver()
			.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					listingLayout.getViewTreeObserver()
						.removeOnGlobalLayoutListener(this);
					onListingReady();
				}
			});
		this.listingLayout.setOnTouchListener(this.touchManager);

		// Default to external storage directory.
		this.setEditText(XenaApplication.preferences.getString(
			FilePickerActivity.SHARED_PREFERENCES_EDIT_TEXT,
			Environment.getExternalStorageDirectory().toString() + "/"));
		this.editText
			.addTextChangedListener(new FilePickerEditTextChangedListener(this));
	}

	private void onListingReady() {
		// Compute constants which depend on LayoutListing sizing.
		this.LISTING_SIZE
			= new Point(this.listingLayout.getWidth(),
				this.listingLayout.getHeight());
		this.GRID_DIMENSIONS
			= new Point(
				Math.max(1,
					this.LISTING_SIZE.x / (FilePickerActivity.MIN_PANE_SIZE_PX.x
						+ FilePickerActivity.MARGIN_SIZE_PX * 2)),
				Math.max(1, this.LISTING_SIZE.y / (FilePickerActivity.MIN_PANE_SIZE_PX.y
					+ FilePickerActivity.MARGIN_SIZE_PX * 2)));
		this.PANES_PER_PAGE = this.GRID_DIMENSIONS.x * this.GRID_DIMENSIONS.y;
		this.PANE_SIZE
			= new Point(
				this.LISTING_SIZE.x / this.GRID_DIMENSIONS.x
					- FilePickerActivity.MARGIN_SIZE_PX * 2,
				this.LISTING_SIZE.y / this.GRID_DIMENSIONS.y
					- FilePickerActivity.MARGIN_SIZE_PX * 2);
		this.LISTING_PARAMS = new LayoutParams(this.PANE_SIZE.x, this.PANE_SIZE.y);
		this.LISTING_PARAMS.setMargins(FilePickerActivity.MARGIN_SIZE_PX,
			FilePickerActivity.MARGIN_SIZE_PX, FilePickerActivity.MARGIN_SIZE_PX,
			FilePickerActivity.MARGIN_SIZE_PX);

		// Update listingLayout only after layout is ready.
		XenaApplication.log("FilePickerActivity::onListingReady: LISTING_SIZE = (",
			this.LISTING_SIZE.x, ", ", this.LISTING_SIZE.y, "), GRID_DIMENSIONS = (",
			this.GRID_DIMENSIONS, "), PANE_SIZE = (", this.PANE_SIZE, ").");
		this.ready = true;
		this.refreshListing();
	}

	@Override
	public void onClick(View view) {
		String originalPath = this.editText.getText().toString(),
			path = originalPath.substring(0, originalPath.lastIndexOf('/'));

		switch (view.getId()) {
			case R.id.file_picker_activity_button_settings:
				this.modalEditPalm
					.setText(String.valueOf(XenaApplication.getPalmTouchThreshold()));
				this.setEditText(XenaApplication.preferences.getString(
					FilePickerActivity.SHARED_PREFERENCES_EDIT_TEXT,
					Environment.getExternalStorageDirectory().toString() + "/"));
				this.tentativeModalEditPanUpdateState
					= XenaApplication.getPanUpdateEnabled();
				this.modalEditPanUpdate
					.setBackgroundResource(this.tentativeModalEditPanUpdateState
						? R.drawable.solid_filled
						: R.drawable.solid_empty);
				this.modalEditDrawRefresh
					.setText(String.valueOf(XenaApplication.getDrawEndRefresh()));
				this.modal.setVisibility(View.VISIBLE);
				break;
			case R.id.file_picker_activity_button_date:
				this.setEditText(
					path + "/" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
				this.maybeStartScribbleActivity(true);
				break;
			case R.id.file_picker_activity_button_svg:
				this.maybeStartScribbleActivity(true);
				break;
			case R.id.file_picker_activity_modal_edit_pan_update:
				this.tentativeModalEditPanUpdateState
					= !this.tentativeModalEditPanUpdateState;
				this.modalEditPanUpdate
					.setBackgroundResource(this.tentativeModalEditPanUpdateState
						? R.drawable.solid_filled
						: R.drawable.solid_empty);
				break;
			case R.id.file_picker_activity_modal_button_cancel:
				this.modal.setVisibility(View.GONE);
				break;
			case R.id.file_picker_activity_modal_button_set:
				XenaApplication.setPalmTouchThreshold(
					Integer.parseInt(this.modalEditPalm.getText().toString()));
				XenaApplication
					.setPanUpdateEnabled(this.tentativeModalEditPanUpdateState);
				XenaApplication.setDrawEndRefresh(
					Integer.parseInt(this.modalEditDrawRefresh.getText().toString()));
				this.modal.setVisibility(View.GONE);
				break;
			default:
				String text = ((TextView) view).getText().toString();
				XenaApplication.log("FilePickerActivity::onClick: Pane, text = \"",
					text, "\".");
				this.setEditText(text.equals("../")
					? path.substring(0, path.lastIndexOf('/')) + "/"
					: path + "/" + text);
				this.maybeStartScribbleActivity(false);
				break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Force granting of permissions.
		if (checkSelfPermission(
			Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
			|| checkSelfPermission(
				Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			XenaApplication.log("FilePickerActivity::onResume: Request permissions.");
			requestPermissions(
				new String[] { Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE },
				FilePickerActivity.REQUEST_CODE_READ_WRITE_EXTERNAL_STORAGE);
		}

		// Reset listingLayout if resuming.
		if (this.ready) {
			this.onListingReady();
		}
	}

	// Attempts starting scribble activity using the current path file. Only
	// starts it if it is of type .svg or .pdf, or forceStart is set.
	private void maybeStartScribbleActivity(boolean forceStart) {
		String path = this.editText.getText().toString();

		// Update shared preferences to current path.
		SharedPreferences.Editor editor = XenaApplication.preferences.edit();
		editor.putString(FilePickerActivity.SHARED_PREFERENCES_EDIT_TEXT, path);
		editor.commit();

		// Case based on extension.
		int extensionSeparator = path.lastIndexOf('.');
		String extension
			= extensionSeparator == -1 ? "" : path.substring(extensionSeparator + 1);
		XenaApplication.log(
			"FilePickerActivity::maybeStartScribbleActivity: extension = \"",
			extension, "\".");
		if (extension.equals("svg")) {
			this.startActivity(new Intent(this, ScribbleActivity.class)
				.setAction(Intent.ACTION_RUN).addCategory(Intent.CATEGORY_DEFAULT)
				.putExtra(ScribbleActivity.EXTRA_SVG_PATH, path));
		} else if (extension.equals("pdf")) {
			this.startActivity(new Intent(this, ScribbleActivity.class)
				.setAction(Intent.ACTION_RUN).addCategory(Intent.CATEGORY_DEFAULT)
				.putExtra(ScribbleActivity.EXTRA_SVG_PATH, path + ".svg")
				.putExtra(ScribbleActivity.EXTRA_PDF_PATH, path));
		} else if (forceStart) {
			this.startActivity(new Intent(this, ScribbleActivity.class)
				.setAction(Intent.ACTION_RUN).addCategory(Intent.CATEGORY_DEFAULT)
				.putExtra(ScribbleActivity.EXTRA_SVG_PATH, path + ".svg"));
		}
	}

	private void setEditText(String text) {
		this.editText.setText(text);
		this.editText.setSelection(text.length());
	}

	// Update listing panes with current page and directory.
	void refreshListing() {
		// Block calls before listingLayout is ready.
		if (!this.ready) {
			return;
		}

		this.listingLayout.removeAllViews();

		// Get files in current directory.
		String path = this.editText.getText().toString();
		path = path.substring(0, path.lastIndexOf('/'));
		XenaApplication.log("FilePickerActivity::refreshListing: path = \"", path,
			"\".");

		File[] files = new File(path).listFiles();
		if (files == null) {
			files = new File[0];
		}
		ArrayList<File> filesList = new ArrayList<File>(Arrays.asList(files));
		filesList.add(new File(".."));
		Collections.sort(filesList, new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				if (a.isDirectory() && !b.isDirectory()) {
					return -1;
				}
				if (!a.isDirectory() && b.isDirectory()) {
					return 1;
				}
				return a.getName().toLowerCase().compareTo(b.getName().toLowerCase());
			}
		});

		// Wrap listingPage.
		final int C_PAGES
			= (filesList.size() + this.PANES_PER_PAGE) / this.PANES_PER_PAGE;
		this.listingPage = (this.listingPage % C_PAGES + C_PAGES) % C_PAGES;

		// Add all panes to listingLayout.
		for (int i = 0; i < this.GRID_DIMENSIONS.y; i++) {
			LinearLayout row = new LinearLayout(this);
			for (int j = 0; j < this.GRID_DIMENSIONS.x; j++) {
				int idx
					= i * this.GRID_DIMENSIONS.x + j
						+ this.listingPage * this.PANES_PER_PAGE;
				if (idx >= filesList.size()) {
					break;
				}

				// Theme pane based on file vs. directory.
				File file = filesList.get(idx);
				TextView textView;
				if (file.isDirectory()) {
					textView
						= new TextView(new ContextThemeWrapper(this,
							R.style.file_picker_pane_directory));
					textView.setPaintFlags(
						textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				} else {
					textView
						= new TextView(
							new ContextThemeWrapper(this, R.style.file_picker_pane_file));
				}
				textView.setText(file.getName() + (file.isDirectory() ? "/" : ""));
				textView.setOnTouchListener(this.touchManager);
				row.addView(textView, this.LISTING_PARAMS);
			}
			this.listingLayout.addView(row, FilePickerActivity.LISTING_ROW_PARAMS);
		}
	}
}
