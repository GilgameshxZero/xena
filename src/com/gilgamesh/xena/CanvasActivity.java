package com.gilgamesh.xena;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

// import com.onyx.android.sdk.data.note.TouchPoint;
// import com.onyx.android.sdk.pen.RawInputCallback;
// import com.onyx.android.sdk.pen.TouchHelper;
// import com.onyx.android.sdk.pen.data.TouchPointList;

public class CanvasActivity extends Activity {
	private ImageView imageView;
	private Bitmap bitmap;
	private Paint paint;
	private Canvas canvas;
	private ArrayList<Path> paths;

	// private TouchHelper touchHelper;

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

				// touchHelper.openRawDrawing();
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

		// this.touchHelper = TouchHelper.create(this.imageView, new RawInputCallback() {
		// 	@Override
		// 	public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
		// 	}

		// 	@Override
		// 	public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
		// 	}

		// 	@Override
		// 	public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
		// 	}

		// 	@Override
		// 	public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
		// 	}

		// 	@Override
		// 	public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
		// 	}

		// 	@Override
		// 	public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
		// 	}

		// 	@Override
		// 	public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
		// 	}

		// 	@Override
		// 	public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
		// 	}
		// }).setStrokeWidth(3.0f).openRawDrawing();
	}

	// @Override
	// protected void onResume() {
	// 	this.touchHelper.setRawDrawingEnabled(true);
	// 	super.onResume();
	// }

	// @Override
	// protected void onPause() {
	// 	this.touchHelper.setRawDrawingEnabled(false);
	// 	super.onPause();
	// }

	// @Override
	// protected void onDestroy() {
	// 	this.touchHelper.closeRawDrawing();
	// 	super.onDestroy();
	// }
}
