package com.gilgamesh.xenagogy;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;

import com.gilgamesh.xenagogy.R;

//

import android.os.Bundle;
// import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.onyx.android.sdk.api.device.EpdDeviceManager;
import com.onyx.android.sdk.api.device.epd.EpdController;
import com.onyx.android.sdk.api.device.epd.UpdateMode;

import com.onyx.android.sdk.device.Device;
// import com.onyx.android.sdk.api.device.EpdDevice;
import com.onyx.android.sdk.api.device.epd.UpdateOption;

// import butterknife.Bind;
// import butterknife.ButterKnife;

public class MainActivity extends Activity implements View.OnClickListener {
  private static final String TAG = MainActivity.class.getSimpleName();

  TextView textView;
  SurfaceView surfaceView;
  RelativeLayout activityMain;

  Button button_partial_update;
  Button button_regal_partial;
  Button button_enter_fast_mode;
  Button button_quit_fast_mode;
  Button button_screen_refresh;
  Button button_enter_x_mode;
  Button button_enter_normal_mode;
  Button button_enter_A2_mode;
  Button button_enter_du_mode;

  private boolean isFastMode = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_epd_demo);
		Log.e(TAG, "pogchamp");

		textView = findViewById(R.id.textview);
		surfaceView = findViewById(R.id.surfaceview);
		activityMain = findViewById(R.id.activity_main);
		button_partial_update = findViewById(R.id.button_partial_update);
		button_regal_partial = findViewById(R.id.button_regal_partial);
		button_enter_fast_mode = findViewById(R.id.button_enter_fast_mode);
		button_quit_fast_mode = findViewById(R.id.button_quit_fast_mode);
		button_screen_refresh = findViewById(R.id.button_screen_refresh);
		button_enter_x_mode = findViewById(R.id.button_enter_x_mode);
		button_enter_normal_mode = findViewById(R.id.button_enter_normal_mode);
		button_enter_A2_mode = findViewById(R.id.button_enter_A2_mode);
		button_enter_du_mode = findViewById(R.id.button_enter_du_mode);

    // ButterKnife.bind(this);
    // button_partial_update.setOnClickListener(this);
    // button_regal_partial.setOnClickListener(this);
    // button_enter_fast_mode.setOnClickListener(this);
    // button_quit_fast_mode.setOnClickListener(this);
    // button_screen_refresh.setOnClickListener(this);
    // button_enter_x_mode.setOnClickListener(this);
    // button_enter_normal_mode.setOnClickListener(this);
    // button_enter_A2_mode.setOnClickListener(this);
    // button_enter_du_mode.setOnClickListener(this);

    // // set full update after how many partial update
    EpdDeviceManager.setGcInterval(5);

		//

    // setContentView(R.layout.activity_main);
    // TextView textView = (TextView)findViewById(R.id.my_text);
    // textView.setText("Hello, world!");

		//

		// TextView textView = new TextView(this);
		// textView.setText(getString(R.string.hello_world));
		// setContentView(textView);
  }

  @Override
  public void onClick(View v) {
		Log.e(TAG, "pogchamp2");
    if (v.equals(button_partial_update)) {
			Log.e(TAG, "pogchamp3");
      updateTextView();
			EpdDeviceManager.refreshScreenWithGCIntervalWithoutRegal(textView);
			// EpdDeviceManager.applyWithGCInterval(textView, true);
			// EpdController.setViewDefaultUpdateMode(textView, UpdateMode.GU);
      // EpdDeviceManager.applyWithGCIntervalWithoutRegal(textView);
    } else if (v.equals(button_regal_partial)) {
      updateTextView();
			EpdDeviceManager.refreshScreenWithGCIntervalWithRegal(textView);
			// EpdDeviceManager.applyWithGCInterval(textView, true);
			// EpdController.setViewDefaultUpdateMode(textView, UpdateMode.REGAL);
      // EpdDeviceManager.applyWithGCIntervalWitRegal(textView, true);
    } else if (v.equals(button_screen_refresh)) {
      updateTextView();
			EpdDeviceManager.refreshScreenWithGCIntervalWithoutRegal(activityMain);
			// EpdController.refreshScreen(textView, UpdateMode.GC);
			// EpdDeviceManager.applyGCUpdate(textView);
			// EpdController.invalidate(textView, UpdateMode.GC);
      // EpdController.repaintEveryThing(UpdateMode.GC);
    } else if (v.equals(button_enter_fast_mode)) {
      isFastMode = true;
      EpdDeviceManager.enterAnimationUpdate(true);
    } else if (v.equals(button_quit_fast_mode)) {
      EpdDeviceManager.exitAnimationUpdate(true);
      isFastMode = false;
    } else if (v.equals(button_enter_x_mode)) {
			Device.currentDevice().setAppScopeRefreshMode(UpdateOption.FAST_X);
      // EpdController.clearAppScopeUpdate();
      // EpdController.applyAppScopeUpdate(TAG, true, true, UpdateMode.ANIMATION_X, Integer.MAX_VALUE);
    } else if (v.equals(button_enter_A2_mode)) {
			Device.currentDevice().setSystemRefreshMode(UpdateOption.FAST);
      // EpdController.clearAppScopeUpdate();
      // EpdController.applyAppScopeUpdate(TAG, true, true, UpdateMode.ANIMATION_QUALITY, Integer.MAX_VALUE);
    } else if (v.equals(button_enter_normal_mode)) {
			Device.currentDevice().setAppScopeRefreshMode(UpdateOption.NORMAL);
      // EpdController.clearAppScopeUpdate();
      // EpdController.applyAppScopeUpdate(TAG, false, true, UpdateMode.None, Integer.MAX_VALUE);
    } else if (v.equals(button_enter_du_mode)) {
			Device.currentDevice().setSystemRefreshMode(UpdateOption.FAST_QUALITY);
      // EpdController.clearAppScopeUpdate();
      // EpdController.applyAppScopeUpdate(TAG, true, true, UpdateMode.DU_QUALITY, Integer.MAX_VALUE);
    }
  }

  private void updateTextView() {
    StringBuilder sb = new StringBuilder();
    sb.append(textView.getText());
    sb.append("hello world!");
    textView.setText(sb.toString());
  }
}
