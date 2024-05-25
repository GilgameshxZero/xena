package com.gilgamesh.xena;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.view.View;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import com.onyx.android.sdk.device.Device;

import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.utils.RectUtils;
import com.onyx.android.sdk.pen.RawInputManager;
import com.onyx.android.sdk.pen.RawInputReader;

import java.util.Timer;
import java.util.TimerTask;

public class CanvasActivity extends Activity {
	private ImageView imageView;
	private Bitmap bitmap;
	private Paint paint;
	private Canvas canvas;
	private ArrayList<Path> paths;

	private TouchHelper touchHelper;
	private RawInputCallback rawInputCallback;
	private RawInputManager man;
	private RawInputReader reader;

	private TimerTask refreshTimerTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_canvas);

		this.imageView = findViewById(R.id.activity_canvas_image_view);
		this.imageView.post(new Runnable() {
			@Override
			public void run() {
				bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(),
						Bitmap.Config.ARGB_8888);
				imageView.setImageBitmap(bitmap);

				canvas = new Canvas(bitmap);
				canvas.drawPath(paths.get(0), paint);

				View view = imageView; // getWindow().getDecorView().getRootView();

				Log.d(Xena.TAG, "gilgamesh");
				touchHelper = TouchHelper.create(view, rawInputCallback);
				Log.d(Xena.TAG, "gilgamesh");
				touchHelper.setStrokeWidth(10.0f);

				Log.d(Xena.TAG, "gilgamesh");
				Rect limit = new Rect(); // 0, 0, 100, 100);
				view.getLocalVisibleRect(limit);
				ArrayList<Rect> exclude = new ArrayList<Rect>();
				exclude.add(new Rect(0, 0, 1, 1));
				touchHelper.setLimitRect(limit, exclude);
				Log.d(Xena.TAG, "gilgamesh");
				Log.d(Xena.TAG, RectUtils.toRectF(limit).toString());
				float[] p1 = new float[] { 0, 0 };
				float[] p2 = new float[] { 0, 0 };
				// Log.d(Xena.TAG, Device.currentDevice().mapToRawTouchPoint(view, p1,
				// p2).toString());
				Log.d(Xena.TAG, Device.currentDevice().mapToRawTouchPoint(view, RectUtils.toRectF(limit)).toString());

				// touchHelper.debugLog(true);
				// touchHelper.enableFingerTouch(true);
				touchHelper.openRawDrawing();
				touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL);
				touchHelper.setSingleRegionMode();
				touchHelper.setRawDrawingEnabled(true);
				// touchHelper.setRawInputReaderEnable(true);
				// touchHelper.setRawDrawingRenderEnabled(false);

				Log.d(Xena.TAG, "CanvasActivity.onCreate: " + limit +
						touchHelper.isRawDrawingCreated() + " "
						+ touchHelper.isRawDrawingInputEnabled() + " " +
						touchHelper.isRawDrawingRenderEnabled());

				// man = new RawInputManager();
				// man.setRawInputCallback(rawInputCallback);
				// Rect limit = new Rect(); // 0, 0, 100, 100);
				// imageView.getLocalVisibleRect(limit);
				// man.setLimitRect(limit, new ArrayList<Rect>());
				// man.setHostView(imageView);
				// man.setSingleRegionMode();
				// man.startRawInputReader();
				// Log.d(Xena.TAG, "CanvasActivity.onCreate: " + man.isUseRawInput());

				// reader = new RawInputReader();
				// reader.setRawInputCallback(rawInputCallback);
				// Rect limit = new Rect(); // 0, 0, 100, 100);
				// imageView.getLocalVisibleRect(limit);
				// reader.setLimitRect(limit);
				// reader.setHostView(imageView);
				// reader.setSingleRegionMode();
				// reader.start();
			}
		});

		this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.paint.setColor(Color.BLACK);
		this.paint.setStrokeWidth(10);
		this.paint.setStyle(Paint.Style.STROKE);

		this.paths = new ArrayList<Path>();
		this.paths.add(new Path());
		this.paths.get(0).moveTo(0, 0);
		this.paths.get(0).lineTo(500, 500);
		this.paths.get(0).lineTo(1000, 500);

		this.rawInputCallback = new RawInputCallback() {
			private String mode;

			@Override
			public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
				Log.d(Xena.TAG, "CanvasActivity.onBeginRawDrawing");
				mode = "draw";
			}

			@Override
			public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
				Log.d(Xena.TAG, "CanvasActivity.onEndRawDrawing");
				mode = "none";
				// refreshTimerTask = new TimerTask() {
				// @Override
				// public void run() {
				// runOnUiThread(new Runnable() {
				// @Override
				// public void run() {
				// touchHelper.setRawDrawingEnabled(false);
				// touchHelper.setRawDrawingEnabled(true);
				// }
				// });
				// }
				// };
				// new Timer().schedule(refreshTimerTask, 1000);
			}

			@Override
			public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
				// Log.d(Xena.TAG, "CanvasActivity.onRawDrawingTouchPointMoveReceived: " +
				// touchPoint.x + ", " + touchPoint.y);
			}

			@Override
			public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
				// Log.d(Xena.TAG, "CanvasActivity.onRawDrawingTouchPointListReceived");
			}

			@Override
			public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
				Log.d(Xena.TAG, "CanvasActivity.onBeginRawErasing");
				mode = "erase";
			}

			@Override
			public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
				Log.d(Xena.TAG, "CanvasActivity.onEndRawErasing");
				mode = "none";
				// refreshTimerTask = new TimerTask() {
				// @Override
				// public void run() {
				// runOnUiThread(new Runnable() {
				// @Override
				// public void run() {
				// touchHelper.setRawDrawingEnabled(false);
				// touchHelper.setRawDrawingEnabled(true);
				// }
				// });
				// }
				// };
				// new Timer().schedule(refreshTimerTask, 1000);
			}

			@Override
			public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
				// Log.d(Xena.TAG, "CanvasActivity.onRawErasingTouchPointMoveReceived: " +
				// touchPoint.x + ", " + touchPoint.y);
			}

			@Override
			public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
				// Log.d(Xena.TAG, "CanvasActivity.onRawErasingTouchPointListReceived");
			}

			@Override
			public void onPenActive(TouchPoint point) {
				Log.d(Xena.TAG, "CanvasActivity.onPenActive [" + mode + "]: " + point.x + ", " + point.y);
			}
		};
	}

	@Override
	protected void onResume() {
		if (this.touchHelper != null) {
			this.touchHelper.setRawDrawingEnabled(true);
			Log.d(Xena.TAG, "CanvasActivity.onResume: " + this.touchHelper.isRawDrawingCreated() + " "
					+ this.touchHelper.isRawDrawingInputEnabled() + " " + this.touchHelper.isRawDrawingRenderEnabled());
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		this.touchHelper.setRawDrawingEnabled(false);
		Log.d(Xena.TAG, "CanvasActivity.onPause: " + this.touchHelper.isRawDrawingCreated() + " "
				+ this.touchHelper.isRawDrawingInputEnabled() + " " + this.touchHelper.isRawDrawingRenderEnabled());
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		this.touchHelper.closeRawDrawing();
		Log.d(Xena.TAG, "CanvasActivity.onDestroy: " + this.touchHelper.isRawDrawingCreated() + " "
				+ this.touchHelper.isRawDrawingInputEnabled() + " " + this.touchHelper.isRawDrawingRenderEnabled());
		super.onDestroy();
	}
}
