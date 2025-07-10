package com.gilgamesh.xena;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.util.Log;
import androidx.preference.PreferenceManager;

public class XenaApplication extends Application {
	static public final String TAG = "Xena";
	static public final int DPI
		= Resources.getSystem().getDisplayMetrics().densityDpi;

	// Global settings are stored on XenaApplication.
	static private final String SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE
		= "SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE";
	static private final float PALM_TOUCH_THRESHOLD_DEFAULT = 9999;
	static public float PALM_TOUCH_THRESHOLD;

	static public boolean IS_DEBUG = false;
	static public SharedPreferences preferences;

	@Override
	public void onCreate() {
		XenaApplication.IS_DEBUG
			= (0 != (this.getApplicationInfo().flags
				& ApplicationInfo.FLAG_DEBUGGABLE));
		XenaApplication.preferences
			= PreferenceManager.getDefaultSharedPreferences(this);

		XenaApplication.PALM_TOUCH_THRESHOLD
			= Float.parseFloat(XenaApplication.preferences.getString(
				XenaApplication.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE,
				String.valueOf(XenaApplication.PALM_TOUCH_THRESHOLD_DEFAULT)));
	}

	static public void log(Object... objects) {
		if (!XenaApplication.IS_DEBUG) {
			return;
		}

		Log.v(XenaApplication.TAG,
			XenaApplication.concatenateObjectString(objects));
	}

	static public void error(Object... objects) {
		Log.e(XenaApplication.TAG,
			XenaApplication.concatenateObjectString(objects));
	}

	static private String concatenateObjectString(Object... objects) {
		String concatenated = "";
		for (Object i : objects) {
			if (i != null) {
				concatenated += i.toString();
			}
		}
		return concatenated;
	}

	static public void setPalmTouchThreshold(float newThreshold) {
		XenaApplication.PALM_TOUCH_THRESHOLD = newThreshold;

		SharedPreferences.Editor editor = XenaApplication.preferences.edit();
		editor.putString(
			XenaApplication.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD_CACHE,
			String.valueOf(newThreshold));
		editor.commit();
	}

	static public float getPalmTouchThreshold() {
		return XenaApplication.PALM_TOUCH_THRESHOLD;
	}
}
