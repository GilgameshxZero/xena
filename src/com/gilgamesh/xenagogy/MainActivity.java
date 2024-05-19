package com.gilgamesh.xenagogy;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.webkit.WebView;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.onyx.android.sdk.api.device.EpdDeviceManager;
import com.onyx.android.sdk.device.Device;
import com.onyx.android.sdk.api.device.epd.UpdateOption;

public class MainActivity extends Activity implements View.OnClickListener {
	private static final String TAG = MainActivity.class.getSimpleName();

	TextView textView;
	SurfaceView surfaceView;
	RelativeLayout activityMain;
	WebView webView;

	Button button_partial_update;
	Button button_regal_partial;
	Button button_enter_fast_mode;
	Button button_quit_fast_mode;
	Button button_screen_refresh;
	Button button_enter_x_mode;
	Button button_enter_normal_mode;
	Button button_enter_A2_mode;
	Button button_enter_du_mode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_epd_demo);
		Log.e(TAG, "pogchamp");

		textView = findViewById(R.id.textview);
		surfaceView = findViewById(R.id.surfaceview);
		activityMain = findViewById(R.id.activity_main);
		this.webView = findViewById(R.id.webview);
		this.webView.loadUrl("http://usaco.org");

		button_partial_update = findViewById(R.id.button_partial_update);
		button_regal_partial = findViewById(R.id.button_regal_partial);
		button_enter_fast_mode = findViewById(R.id.button_enter_fast_mode);
		button_quit_fast_mode = findViewById(R.id.button_quit_fast_mode);
		button_screen_refresh = findViewById(R.id.button_screen_refresh);
		button_enter_x_mode = findViewById(R.id.button_enter_x_mode);
		button_enter_normal_mode = findViewById(R.id.button_enter_normal_mode);
		button_enter_A2_mode = findViewById(R.id.button_enter_A2_mode);
		button_enter_du_mode = findViewById(R.id.button_enter_du_mode);

		EpdDeviceManager.setGcInterval(5);
	}

	// Request code for selecting a PDF document.
	private static final int PICK_PDF_FILE = 299999;

	private void openFile(Uri pickerInitialUri) {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("image/svg+xml");

		// Optionally, specify a URI for the file that should appear in the
		// system file picker when it loads.
		// intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
		startActivityForResult(intent, PICK_PDF_FILE);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "herehere");
		Log.d(TAG, String.valueOf(requestCode));
		Log.d(TAG, String.valueOf(resultCode));
		String uri = data.getData().toString();
		Log.d(TAG, uri);

		this.updateTextView(uri);
		this.webView.loadUrl(uri);
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "pogchamp2");
		if (v.equals(button_partial_update)) {
			Log.d(TAG, "pogchamp3");
			updateTextView("partial");
			EpdDeviceManager.refreshScreenWithGCIntervalWithoutRegal(textView);
			// EpdDeviceManager.applyWithGCInterval(textView, true);
			// EpdController.setViewDefaultUpdateMode(textView, UpdateMode.GU);
			// EpdDeviceManager.applyWithGCIntervalWithoutRegal(textView);

			openFile(Uri.EMPTY);
		} else if (v.equals(button_regal_partial)) {
			updateTextView("partial regal");
			EpdDeviceManager.refreshScreenWithGCIntervalWithRegal(textView);
			// EpdDeviceManager.applyWithGCInterval(textView, true);
			// EpdController.setViewDefaultUpdateMode(textView, UpdateMode.REGAL);
			// EpdDeviceManager.applyWithGCIntervalWitRegal(textView, true);
		} else if (v.equals(button_screen_refresh)) {
			updateTextView("refresh");
			EpdDeviceManager.refreshScreenWithGCIntervalWithoutRegal(activityMain);
			// EpdController.refreshScreen(textView, UpdateMode.GC);
			// EpdDeviceManager.applyGCUpdate(textView);
			// EpdController.invalidate(textView, UpdateMode.GC);
			// EpdController.repaintEveryThing(UpdateMode.GC);
		} else if (v.equals(button_enter_fast_mode)) {
			EpdDeviceManager.enterAnimationUpdate(true);
		} else if (v.equals(button_quit_fast_mode)) {
			EpdDeviceManager.exitAnimationUpdate(true);
		} else if (v.equals(button_enter_x_mode)) {
			Device.currentDevice().setAppScopeRefreshMode(UpdateOption.FAST_X);
		} else if (v.equals(button_enter_A2_mode)) {
			Device.currentDevice().setAppScopeRefreshMode(UpdateOption.FAST);
		} else if (v.equals(button_enter_normal_mode)) {
			Device.currentDevice().setAppScopeRefreshMode(UpdateOption.NORMAL);
		} else if (v.equals(button_enter_du_mode)) {
			Device.currentDevice().setAppScopeRefreshMode(UpdateOption.FAST_QUALITY);
		}
	}

	private void updateTextView(String text) {
		textView.setText(text);
	}
}
