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

	static public boolean IS_DEBUG = false;
	static public SharedPreferences preferences;

	@Override
	public void onCreate() {
		XenaApplication.IS_DEBUG
			= (0 != (this.getApplicationInfo().flags
				& ApplicationInfo.FLAG_DEBUGGABLE));
		XenaApplication.preferences
			= PreferenceManager.getDefaultSharedPreferences(this);
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
}
