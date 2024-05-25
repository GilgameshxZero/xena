package com.gilgamesh.xena;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;

public class CanvasActivity extends Activity {
	private final int PAINT_WIDTH = 6;

	private ImageView imageView;
	private Bitmap bitmap;
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Canvas canvas;
	private ArrayList<Path> paths = new ArrayList<Path>();

	private TouchHelper touchHelper;

	private RawInputCallback rawInputCallback = new RawInputCallback() {
		private String mode = "none";

		@Override
		public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
			Log.d(Xena.TAG, "CanvasActivity.onBeginRawDrawing");
			paths.add(new Path());
			paths.get(paths.size() - 1).moveTo(touchPoint.x, touchPoint.y);
			this.mode = "draw";
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			Log.d(Xena.TAG, "CanvasActivity.onEndRawDrawing");
			touchHelper.setRawDrawingEnabled(false);
			this.mode = "none";
			canvas.drawPath(paths.get(paths.size() - 1), paint);
			imageView.setImageBitmap(bitmap);
			touchHelper.setRawDrawingEnabled(true);
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
		}

		@Override
		public void onRawDrawingTouchPointListReceived(TouchPointList touchPointList) {
		}

		@Override
		public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
			Log.d(Xena.TAG, "CanvasActivity.onBeginRawErasing");
			paths.add(new Path());
			paths.get(paths.size() - 1).moveTo(touchPoint.x, touchPoint.y);
			this.mode = "erase";
		}

		@Override
		public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
			Log.d(Xena.TAG, "CanvasActivity.onEndRawErasing");
			touchHelper.setRawDrawingEnabled(false);
			this.mode = "none";
			canvas.drawPath(paths.get(paths.size() - 1), paint);
			imageView.setImageBitmap(bitmap);
			touchHelper.setRawDrawingEnabled(true);
		}

		@Override
		public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
		}

		@Override
		public void onRawErasingTouchPointListReceived(TouchPointList touchPointList) {
		}

		@Override
		public void onPenActive(TouchPoint touchPoint) {
			if (this.mode != "none") {
				paths.get(paths.size() - 1).lineTo(touchPoint.x, touchPoint.y);
			}
		}
	};

	private Runnable imageViewPost = new Runnable() {
		@Override
		public void run() {
			bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
			imageView.setImageBitmap(bitmap);

			canvas = new Canvas(bitmap);

			touchHelper = TouchHelper.create(imageView, rawInputCallback);
			touchHelper.setStrokeWidth(PAINT_WIDTH);

			Rect limit = new Rect();
			imageView.getLocalVisibleRect(limit);
			touchHelper.setLimitRect(limit, new ArrayList<Rect>());

			touchHelper.openRawDrawing();
			touchHelper.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL);
			touchHelper.setSingleRegionMode();
			touchHelper.setRawDrawingEnabled(true);

			Log.d(Xena.TAG, "CanvasActivity.onCreate: " + limit + touchHelper.isRawDrawingCreated() + " "
					+ touchHelper.isRawDrawingInputEnabled() + " " + touchHelper.isRawDrawingRenderEnabled());
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_canvas);

		this.imageView = findViewById(R.id.activity_canvas_image_view);
		this.imageView.post(this.imageViewPost);

		this.paint.setColor(Color.BLACK);
		this.paint.setStrokeWidth(PAINT_WIDTH);
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeJoin(Paint.Join.ROUND);
		this.paint.setStrokeCap(Paint.Cap.ROUND);
		this.paint.setPathEffect(new CornerPathEffect(PAINT_WIDTH));
	}

	@Override
	protected void onResume() {
		if (this.touchHelper != null) {
			this.touchHelper.setRawDrawingEnabled(true);
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		this.touchHelper.setRawDrawingEnabled(false);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		this.touchHelper.closeRawDrawing();
		super.onDestroy();
	}
}
