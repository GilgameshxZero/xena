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
		CREATE_NEW,
		LOAD_EXISTING
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_file_picker);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.activity_file_picker_button_create_new:
				this.startActivityForResult(
						new Intent(Intent.ACTION_CREATE_DOCUMENT)
								.addCategory(Intent.CATEGORY_OPENABLE).setType("image/svg+xml"),
						INTENT_REQUEST.CREATE_NEW.ordinal());
				break;
			case R.id.activity_file_picker_button_load_existing:
				this.startActivityForResult(
						new Intent(Intent.ACTION_OPEN_DOCUMENT)
								.addCategory(Intent.CATEGORY_OPENABLE).setType("image/svg+xml"),
						INTENT_REQUEST.LOAD_EXISTING.ordinal());
				break;
			case R.id.activity_file_picker_button_load_pdf:
				Log.e(XenaApplication.TAG,
						"FilePickerActivity::onClick: PDF not implemented.");
				break;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		Uri uri = data.getData();
		Log.d(XenaApplication.TAG,
				"FilePickerActivity::onActivityResult: " + uri.toString());
		this.startActivity(
				new Intent(this, ScribbleActivity.class).setAction(Intent.ACTION_RUN)
						.addCategory(Intent.CATEGORY_DEFAULT).setData(uri));
	}
}
