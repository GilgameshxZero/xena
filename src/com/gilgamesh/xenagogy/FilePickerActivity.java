package com.gilgamesh.xenagogy;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import com.gilgamesh.xenagogy.CanvasActivity;

public class FilePickerActivity extends Activity implements View.OnClickListener {
	private static final String TAG = FilePickerActivity.class.getSimpleName();
	private static final int INTENT_REQUEST_CREATE_NEW = 0;
	private static final int INTENT_REQUEST_LOAD_EXISTING = 1;

	Button buttonCreateNew;
	Button buttonLoadExisting;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_file_picker);

		this.buttonCreateNew = findViewById(R.id.activity_file_picker_button_create_new);
		this.buttonLoadExisting = findViewById(R.id.activity_file_picker_button_load_existing);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.activity_file_picker_button_create_new:
				this.startActivityForResult(
						new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/svg+xml"),
						INTENT_REQUEST_CREATE_NEW);
				break;
			case R.id.activity_file_picker_button_load_existing:
				this.startActivityForResult(
						new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/svg+xml"),
						INTENT_REQUEST_LOAD_EXISTING);
				break;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		Uri uri = data.getData();
		Log.i(TAG, "onActivityResult: " + uri.toString());
		this.startActivity(new Intent(this, CanvasActivity.class).setAction(Intent.ACTION_RUN)
				.addCategory(Intent.CATEGORY_DEFAULT).setData(uri));
	}
}
