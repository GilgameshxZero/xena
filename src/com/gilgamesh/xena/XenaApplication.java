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
	static private final String SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD
		= "SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD";
	static private final int PALM_TOUCH_THRESHOLD_DEFAULT = 9999;
	static private int PALM_TOUCH_THRESHOLD;
	static private final String SHARED_PREFERENCES_PAN_UPDATE_ENABLED
		= "SHARED_PREFERENCES_PAN_UPDATE_ENABLED";
	static private final boolean PAN_UPDATE_ENABLED_DEFAULT = true;
	static private boolean PAN_UPDATE_ENABLED;
	static private final String SHARED_PREFERENCES_DRAW_END_REFRESH
		= "SHARED_PREFERENCES_DRAW_END_REFRESH";
	static private final int DRAW_END_REFRESH_DEFAULT = 60000;
	static private int DRAW_END_REFRESH;
	static private final String SHARED_PREFERENCES_FLICK_DISTANCE
		= "SHARED_PREFERENCES_FLICK_DISTANCE";
	static private final float FLICK_DISTANCE_DEFAULT = 0.8f;
	static private float FLICK_DISTANCE;

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
			= XenaApplication.preferences.getInt(
				XenaApplication.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD,
				XenaApplication.PALM_TOUCH_THRESHOLD_DEFAULT);
		XenaApplication.PAN_UPDATE_ENABLED
			= XenaApplication.preferences.getBoolean(
				XenaApplication.SHARED_PREFERENCES_PAN_UPDATE_ENABLED,
				XenaApplication.PAN_UPDATE_ENABLED_DEFAULT);
		XenaApplication.DRAW_END_REFRESH
			= XenaApplication.preferences.getInt(
				XenaApplication.SHARED_PREFERENCES_DRAW_END_REFRESH,
				XenaApplication.DRAW_END_REFRESH_DEFAULT);
		XenaApplication.FLICK_DISTANCE
			= XenaApplication.preferences.getFloat(
				XenaApplication.SHARED_PREFERENCES_FLICK_DISTANCE,
				XenaApplication.FLICK_DISTANCE_DEFAULT);
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

	static public void setPalmTouchThreshold(int value) {
		XenaApplication.PALM_TOUCH_THRESHOLD = value;

		SharedPreferences.Editor editor = XenaApplication.preferences.edit();
		editor.putInt(XenaApplication.SHARED_PREFERENCES_PALM_TOUCH_THRESHOLD,
			value);
		editor.commit();
	}

	static public int getPalmTouchThreshold() {
		return XenaApplication.PALM_TOUCH_THRESHOLD;
	}

	static public void setPanUpdateEnabled(boolean value) {
		XenaApplication.PAN_UPDATE_ENABLED = value;

		SharedPreferences.Editor editor = XenaApplication.preferences.edit();
		editor.putBoolean(XenaApplication.SHARED_PREFERENCES_PAN_UPDATE_ENABLED,
			value);
		editor.commit();
	}

	static public boolean getPanUpdateEnabled() {
		return XenaApplication.PAN_UPDATE_ENABLED;
	}

	static public void setDrawEndRefresh(int value) {
		XenaApplication.DRAW_END_REFRESH = value;

		SharedPreferences.Editor editor = XenaApplication.preferences.edit();
		editor.putInt(XenaApplication.SHARED_PREFERENCES_DRAW_END_REFRESH, value);
		editor.commit();
	}

	static public int getDrawEndRefresh() {
		return XenaApplication.DRAW_END_REFRESH;
	}

	static public void setFlickDistance(float value) {
		XenaApplication.FLICK_DISTANCE = value;

		SharedPreferences.Editor editor = XenaApplication.preferences.edit();
		editor.putFloat(XenaApplication.SHARED_PREFERENCES_FLICK_DISTANCE, value);
		editor.commit();
	}

	static public float getFlickDistance() {
		return XenaApplication.FLICK_DISTANCE;
	}
}
