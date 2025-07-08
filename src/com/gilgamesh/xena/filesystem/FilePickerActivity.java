package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.scribble.ScribbleActivity;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.InputDevice;
import android.widget.EditText;
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

public class FilePickerActivity extends Activity
		implements View.OnClickListener {
	static private final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1;
	static private final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2;
	static private final int REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 3;
	static private final String SHARED_PREFERENCES_EDIT_TEXT_CACHE = "SHARED_PREFERENCES_EDIT_TEXT_CACHE";
	static private final Point MIN_PANE_SIZE_DP = new Point(384, 72);
	static private final Point MIN_PANE_SIZE_PX = new Point(
			FilePickerActivity.MIN_PANE_SIZE_DP.x * XenaApplication.DPI / 160,
			FilePickerActivity.MIN_PANE_SIZE_DP.y * XenaApplication.DPI / 160);
	static private final int MARGIN_SIZE_DP = 6;
	static private final int MARGIN_SIZE_PX = FilePickerActivity.MARGIN_SIZE_DP
			* XenaApplication.DPI / 160;
	static private final LayoutParams LISTING_LAYOUT_ROW = new LayoutParams(
			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

	private Point GRID_DIMENSIONS;
	private int PANES_PER_PAGE;
	private Point PANE_SIZE;
	private LayoutParams LISTING_LAYOUT_PANE = null;

	private EditText editText;
	private LinearLayout layoutListing;
	private SharedPreferences sharedPreferences;

	private FilePickerTouchManager touchManager;

	int page = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_file_picker);

		this.editText = findViewById(R.id.activity_file_picker_edit_text);
		this.layoutListing = findViewById(R.id.activity_file_picker_layout_listing);

		this.layoutListing.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						layoutListing.getViewTreeObserver()
								.removeOnGlobalLayoutListener(this);
						onLayoutListingViewReady();
					}
				});

		this.touchManager = new FilePickerTouchManager(this);
		this.layoutListing.setOnTouchListener(this.touchManager);

		// TODO: Deprecated.
		this.sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.editText
				.setText(this.sharedPreferences
						.getString(FilePickerActivity.SHARED_PREFERENCES_EDIT_TEXT_CACHE,
								Environment.getExternalStorageDirectory().toString() + "/"));
		this.editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				page = 0;
				updateListing();
			}
		});
	}

	private void onLayoutListingViewReady() {
		// Calculate PANE_SIZE.
		this.GRID_DIMENSIONS = new Point(
				this.layoutListing.getWidth()
						/ (FilePickerActivity.MIN_PANE_SIZE_PX.x
								+ FilePickerActivity.MARGIN_SIZE_PX * 2),
				this.layoutListing.getHeight()
						/ (FilePickerActivity.MIN_PANE_SIZE_PX.y
								+ FilePickerActivity.MARGIN_SIZE_PX * 2));
		this.PANES_PER_PAGE = this.GRID_DIMENSIONS.x * this.GRID_DIMENSIONS.y;
		this.PANE_SIZE = new Point(
				this.layoutListing.getWidth() / this.GRID_DIMENSIONS.x
						- FilePickerActivity.MARGIN_SIZE_PX * 2,
				this.layoutListing.getHeight() / this.GRID_DIMENSIONS.y
						- FilePickerActivity.MARGIN_SIZE_PX * 2);
		Log.v(XenaApplication.TAG, "FilePickerActivity::onLayoutListingViewReady: "
				+ this.layoutListing.getWidth() + ", "
				+ this.layoutListing.getHeight() + " | "
				+ MIN_PANE_SIZE_PX + " | "
				+ this.GRID_DIMENSIONS + " | " + this.PANE_SIZE);
		this.LISTING_LAYOUT_PANE = new LayoutParams(this.PANE_SIZE.x,
				this.PANE_SIZE.y);
		this.LISTING_LAYOUT_PANE.setMargins(
				FilePickerActivity.MARGIN_SIZE_PX, FilePickerActivity.MARGIN_SIZE_PX,
				FilePickerActivity.MARGIN_SIZE_PX, FilePickerActivity.MARGIN_SIZE_PX);

		updateListing();
	}

	@Override
	public void onClick(View v) {
		String path = this.editText.getText().toString();

		switch (v.getId()) {
			case R.id.activity_file_picker_button_date:
				path = path.substring(0, path.lastIndexOf('/'));
				this.editText.setText(path + "/"
						+ new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
				this.maybeStartScribbleActivity(true);
				break;
			case R.id.activity_file_picker_button_go:
				this.maybeStartScribbleActivity(true);
				break;
			default:
				TextView textView = (TextView) v;
				String text = textView.getText().toString();
				Log.v(XenaApplication.TAG,
						"FilePickerActivity::onClick: Text: " + text + ".");

				path = path.substring(0, path.lastIndexOf('/'));
				if (text.equals("../")) {
					this.editText.setText(
							path.substring(0, path.lastIndexOf('/')) + "/");
				} else {
					this.editText.setText(path + "/" + text);
				}
				this.editText.setSelection(editText.getText().length());
				this.maybeStartScribbleActivity(false);
				break;
		}
	}

	@Override
	protected void onResume() {
		try {
			if (!Environment.isExternalStorageManager()) {
				Log.v(XenaApplication.TAG,
						"FilePickerActivity::onResume:MANAGE_EXTERNAL_STORAGE.");
				startActivityForResult(
						new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
								Uri.parse("package:" + this.getPackageName())),
						FilePickerActivity.REQUEST_CODE_MANAGE_EXTERNAL_STORAGE);
			}
		} catch (NoSuchMethodError e) {
			Log.v(XenaApplication.TAG,
					"FilePickerActivity::onResume:MANAGE_EXTERNAL_STORAGE: "
							+ e.toString());
		}
		if (checkSelfPermission(
				Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			Log.v(XenaApplication.TAG,
					"FilePickerActivity::onResume:READ_EXTERNAL_STORAGE.");
			requestPermissions(
					new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
					FilePickerActivity.REQUEST_CODE_READ_EXTERNAL_STORAGE);
		}
		if (checkSelfPermission(
				Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			Log.v(XenaApplication.TAG,
					"FilePickerActivity::onResume:WRITE_EXTERNAL_STORAGE.");
			requestPermissions(
					new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
					FilePickerActivity.REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
		}

		if (this.layoutListing.getWidth() != 0) {
			this.onLayoutListingViewReady();
		}
		super.onResume();
	}

	private void maybeStartScribbleActivity(boolean forceStart) {
		String path = this.editText.getText().toString();

		SharedPreferences.Editor editor = this.sharedPreferences.edit();
		editor.putString(FilePickerActivity.SHARED_PREFERENCES_EDIT_TEXT_CACHE,
				path);
		editor.commit();

		int extensionSeparator = path.lastIndexOf('.');
		String extension = extensionSeparator == -1 ? ""
				: path.substring(extensionSeparator + 1);
		Log.v(XenaApplication.TAG,
				"FilePickerActivity::onClick: Extension: " + extension);

		if (extension.equals("svg")) {
			this.startActivity(
					new Intent(this, ScribbleActivity.class)
							.setAction(Intent.ACTION_RUN)
							.addCategory(Intent.CATEGORY_DEFAULT)
							.putExtra(ScribbleActivity.EXTRA_SVG_PATH,
									path));
		} else if (extension.equals("pdf")) {
			this.startActivity(
					new Intent(this, ScribbleActivity.class)
							.setAction(Intent.ACTION_RUN)
							.addCategory(Intent.CATEGORY_DEFAULT)
							.putExtra(ScribbleActivity.EXTRA_SVG_PATH,
									path + ".svg")
							.putExtra(ScribbleActivity.EXTRA_PDF_PATH,
									path));
		} else if (forceStart) {
			this.startActivity(
					new Intent(this, ScribbleActivity.class)
							.setAction(Intent.ACTION_RUN)
							.addCategory(Intent.CATEGORY_DEFAULT)
							.putExtra(ScribbleActivity.EXTRA_SVG_PATH,
									path + ".svg"));
		}
	}

	void updateListing() {
		if (this.LISTING_LAYOUT_PANE == null) {
			return;
		}

		this.layoutListing.removeAllViews();

		String path = this.editText.getText().toString();
		path = path.substring(0, path.lastIndexOf('/'));
		Log.v(XenaApplication.TAG, "FilePickerActivity::updateListing: " + path);

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

		// Clamp page.
		if (this.page < 0) {
			this.page = 0;
		}
		if (this.page * this.PANES_PER_PAGE >= filesList.size()) {
			this.page = filesList.size() / this.PANES_PER_PAGE;
		}

		for (int i = 0; i < this.GRID_DIMENSIONS.y; i++) {
			LinearLayout row = new LinearLayout(this);
			for (int j = 0; j < this.GRID_DIMENSIONS.x; j++) {
				int idx = i * this.GRID_DIMENSIONS.x + j
						+ this.page * this.PANES_PER_PAGE;
				if (idx >= filesList.size()) {
					break;
				}

				File file = filesList.get(idx);
				TextView textView;
				if (file.isDirectory()) {
					textView = new TextView(
							new ContextThemeWrapper(this,
									R.style.file_picker_pane_directory));
					textView.setPaintFlags(
							textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				} else {
					textView = new TextView(
							new ContextThemeWrapper(this, R.style.file_picker_pane_file));
				}
				textView.setText(file.getName() + (file.isDirectory() ? "/" : ""));
				textView.setOnTouchListener(this.touchManager);
				row.addView(textView, this.LISTING_LAYOUT_PANE);
			}
			this.layoutListing.addView(row, FilePickerActivity.LISTING_LAYOUT_ROW);
		}
	}
}
