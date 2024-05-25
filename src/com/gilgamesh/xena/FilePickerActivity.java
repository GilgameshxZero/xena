package com.gilgamesh.xena;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
// import android.widget.Button;
import android.util.Log;

public class FilePickerActivity extends Activity
		implements View.OnClickListener {
	private static enum INTENT_REQUEST {
		CREATE_NEW,
		LOAD_EXISTING
	}

	// private Button buttonCreateNew;
	// private Button buttonLoadExisting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.startActivity(new Intent(this, CanvasActivity.class)
				.setAction(Intent.ACTION_RUN)
				.addCategory(Intent.CATEGORY_DEFAULT));
		this.setContentView(R.layout.activity_file_picker);

		// this.buttonCreateNew =
		// findViewById(R.id.activity_file_picker_button_create_new);
		// this.buttonLoadExisting =
		// findViewById(R.id.activity_file_picker_button_load_existing);
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
		}
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		Uri uri = data.getData();
		Log.d(Xena.TAG, "FilePickerActivity.onActivityResult: " + uri.toString());
		this.startActivity(
				new Intent(this, CanvasActivity.class).setAction(Intent.ACTION_RUN)
						.addCategory(Intent.CATEGORY_DEFAULT).setData(uri));
	}
}
