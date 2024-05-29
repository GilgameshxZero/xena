package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.scribble.ScribbleActivity;

import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class FilePickerActivity extends Activity
		implements View.OnClickListener {
	static private enum INTENT_REQUEST {
		LOAD_SVG,
		LOAD_PDF,
		LOAD_PDF_SVG,
	}

	private Uri uri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_file_picker);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.activity_file_picker_button_load_svg:
				this.startActivityForResult(
						new Intent(Intent.ACTION_CREATE_DOCUMENT)
								.addCategory(Intent.CATEGORY_OPENABLE).setType("image/svg+xml"),
						INTENT_REQUEST.LOAD_SVG.ordinal());
				break;
			case R.id.activity_file_picker_button_load_pdf:
				this.startActivityForResult(
						new Intent(Intent.ACTION_OPEN_DOCUMENT)
								.addCategory(Intent.CATEGORY_OPENABLE)
								.setType("application/pdf"),
						INTENT_REQUEST.LOAD_PDF.ordinal());
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			return;
		}
		Log.v(XenaApplication.TAG,
				"FilePickerActivity::onActivityResult: Picked "
						+ data.getData().toString());

		switch (INTENT_REQUEST.values()[requestCode]) {
			case LOAD_SVG:
				this.uri = data.getData();
				this.startActivity(
						new Intent(this, ScribbleActivity.class)
								.setAction(Intent.ACTION_RUN)
								.addCategory(Intent.CATEGORY_DEFAULT).setData(uri));
				break;
			case LOAD_PDF:
				this.uri = data.getData();
				String[] segments = this.uri.getPath().split("/");
				this.startActivityForResult(
						new Intent(Intent.ACTION_CREATE_DOCUMENT)
								.addCategory(Intent.CATEGORY_OPENABLE)
								.setType("image/svg+xml")
								.putExtra(Intent.EXTRA_TITLE,
										segments[segments.length - 1] + ".svg"),
						INTENT_REQUEST.LOAD_PDF_SVG.ordinal());
				break;
			case LOAD_PDF_SVG:
				this.startActivity(
						new Intent(this, ScribbleActivity.class)
								.setAction(Intent.ACTION_RUN)
								.addCategory(Intent.CATEGORY_DEFAULT).setData(data.getData())
								.putExtra(ScribbleActivity.EXTRA_PDF_URI, this.uri.toString()));
				break;
		}
	}
}
