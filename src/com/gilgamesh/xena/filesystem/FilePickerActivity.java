package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.scribble.ScribbleActivity;

import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FilePickerActivity extends Activity
		implements View.OnClickListener {
	private EditText editText;
	private LinearLayout layoutListing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_file_picker);

		this.editText = findViewById(R.id.activity_file_picker_edit_text);
		this.editText
				.setText(Environment.getExternalStorageDirectory().toString() + "/");
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
				updateListing();
			}
		});

		this.layoutListing = findViewById(R.id.activity_file_picker_layout_listing);
		this.updateListing();
	}

	@Override
	public void onClick(View v) {
		String path = this.editText.getText().toString();

		switch (v.getId()) {
			case R.id.activity_file_picker_button:
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
				} else {
					this.startActivity(
							new Intent(this, ScribbleActivity.class)
									.setAction(Intent.ACTION_RUN)
									.addCategory(Intent.CATEGORY_DEFAULT)
									.putExtra(ScribbleActivity.EXTRA_SVG_PATH,
											path + ".svg"));
				}

				break;
			default:
				TextView textView = (TextView) v;
				String text = textView.getText().toString();
				Log.v(XenaApplication.TAG,
						"FilePickerActivity::onClick: Text: " + text);

				path = path.substring(0, path.lastIndexOf('/'));
				if (text == "../") {
					this.editText.setText(
							path.substring(0, path.lastIndexOf('/')) + "/");
				} else {
					this.editText.setText(path + "/" + text);
				}
				this.editText.setSelection(editText.getText().length());

				break;
		}
	}

	private void updateListing() {
		this.layoutListing.removeAllViews();

		String path = this.editText.getText().toString();
		path = path.substring(0, path.lastIndexOf('/'));
		Log.v(XenaApplication.TAG, "FilePickerActivity::updateListing: " + path);

		this.addToListing("../", true);

		File[] files = new File(path).listFiles();
		ArrayList<File> filesList = new ArrayList<File>();
		for (File file : files) {
			filesList.add(file);
		}
		Collections.sort(filesList, new Comparator<File>() {
			@Override
			public int compare(File a, File b) {
				return a.getName().compareTo(b.getName());
			}
		});

		for (File file : filesList) {
			this.addToListing(file.getName() + (file.isDirectory() ? "/" : ""),
					file.isDirectory());
		}
	}

	private void addToListing(String element, boolean underline) {
		TextView textView = new TextView(
				new ContextThemeWrapper(this, R.style.directory_listing));
		if (underline) {
			textView
					.setPaintFlags(
							textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		}
		textView.setText(element);
		this.layoutListing.addView(textView);
	}
}
