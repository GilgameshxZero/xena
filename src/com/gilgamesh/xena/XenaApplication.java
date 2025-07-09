package com.gilgamesh.xena;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.util.Log;

public class XenaApplication extends Application {
	static public final String TAG = "Xena";
	static public final int DPI
		= Resources.getSystem().getDisplayMetrics().densityDpi;
	static public boolean IS_DEBUG = false;

	@Override
	public void onCreate() {
		XenaApplication.IS_DEBUG
			= (0 != (this.getApplicationInfo().flags
				& ApplicationInfo.FLAG_DEBUGGABLE));
	}

	static public void log(String... strings) {
		if (!XenaApplication.IS_DEBUG) {
			return;
		}

		String concatenated = "";
		for (String i : strings) {
			concatenated += i;
		}
		Log.v(XenaApplication.TAG, concatenated);
	}
}
