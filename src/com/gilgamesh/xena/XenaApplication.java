package com.gilgamesh.xena;

import android.app.Application;
import android.content.res.Resources;

public class XenaApplication extends Application {
	static public final String TAG = "Xena";
	static public final int DPI = Resources.getSystem()
			.getDisplayMetrics().densityDpi;
}
