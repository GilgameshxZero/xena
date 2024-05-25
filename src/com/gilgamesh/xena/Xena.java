package com.gilgamesh.xena;

import android.graphics.RectF;
import android.graphics.PointF;

public class Xena {
	public static final String TAG = "XENA";

	public static void coverWithRectF(RectF rectangle, PointF point) {
		rectangle.left = Math.min(rectangle.left, point.x);
		rectangle.top = Math.min(rectangle.top, point.y);
		rectangle.right = Math.max(rectangle.right, point.x);
		rectangle.bottom = Math.max(rectangle.bottom, point.y);
	}
}
