package com.gilgamesh.xena;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.SurfaceView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;

public class CanvasActivity extends Activity {
	private final int STROKE_WIDTH = 6;
	private final int DEBOUNCE_REDRAW_DELAY_MS = 1000;

	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private SurfaceView surfaceView;
	private Bitmap bitmap;
	private Canvas canvas;

	private TouchHelper touchHelper;

	private RawInputCallback rawInputCallback = new RawInputCallback() {
		// Cancellable dummy.
		private TimerTask debounceRedrawTask = new TimerTask() {
			@Override
			public void run() {
			}
		};

		private void resetDebounce() {
			this.debounceRedrawTask.cancel();
		}

		private void debounceRedraw() {
			this.resetDebounce();
			this.debounceRedrawTask = new TimerTask() {
				@Override
				public void run() {
					drawBitmapToSurface();
					touchHelper.setRawDrawingEnabled(false);
					touchHelper.setRawDrawingEnabled(true);
					Log.d(Xena.TAG, "CanvasActivity.debounceRedrawTask");
				}
			};
			new Timer().schedule(this.debounceRedrawTask, DEBOUNCE_REDRAW_DELAY_MS);
			Log.d(Xena.TAG, "CanvasActivity.debounceRedraw");
		}

		@Override
		public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
			this.resetDebounce();
			Xena.drawPaths.add(new Path());
			Xena.drawPaths.get(Xena.drawPaths.size() - 1).moveTo(touchPoint.x,
					touchPoint.y);
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			canvas.drawPath(Xena.drawPaths.get(Xena.drawPaths.size() - 1), paint);
			this.debounceRedraw();
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
			Xena.drawPaths.get(Xena.drawPaths.size() - 1).lineTo(touchPoint.x,
					touchPoint.y);
		}

		@Override
		public void onRawDrawingTouchPointListReceived(
				TouchPointList touchPointList) {
		}

		@Override
		public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
			this.resetDebounce();
		}

		@Override
		public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
			this.debounceRedraw();
		}

		@Override
		public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
		}

		@Override
		public void onRawErasingTouchPointListReceived(
				TouchPointList touchPointList) {
		}

		@Override
		public void onPenActive(TouchPoint touchPoint) {
		}

		@Override
		public void onPenUpRefresh(RectF refreshRect) {
		}
	};

	private View.OnTouchListener surfaceViewOnTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			Log.d(Xena.TAG, "CanvasActivity.onTouchListener: " + event.getAction());
			return false;
		}
	};

	private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			touchHelper = TouchHelper.create(surfaceView, rawInputCallback)
					.setStrokeWidth(STROKE_WIDTH)
					.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL).openRawDrawing()
					.setRawDrawingEnabled(true);
			surfaceView.setOnTouchListener(surfaceViewOnTouchListener);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			touchHelper.closeRawDrawing();

			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);
			for (Path path : Xena.drawPaths) {
				canvas.drawPath(path, paint);
			}
			drawBitmapToSurface();

			touchHelper.setLimitRect(new Rect(0, 0, width, height), new ArrayList<>())
					.openRawDrawing().setRawDrawingEnabled(true);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			holder.removeCallback(this);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_canvas);

		this.paint.setColor(Color.BLACK);
		this.paint.setStrokeWidth(STROKE_WIDTH);
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeJoin(Paint.Join.ROUND);
		this.paint.setStrokeCap(Paint.Cap.ROUND);
		this.paint.setPathEffect(new CornerPathEffect(STROKE_WIDTH));

		this.surfaceView = findViewById(R.id.activity_canvas_surface_view);
		this.surfaceView.getHolder().addCallback(surfaceHolderCallback);
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
		if (this.touchHelper != null) {
			this.touchHelper.setRawDrawingEnabled(false);
		}
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (this.touchHelper != null) {
			this.touchHelper.closeRawDrawing();
		}
		super.onDestroy();
	}

	private void drawBitmapToSurface() {
		Canvas lockCanvas = this.surfaceView.getHolder().lockCanvas();
		lockCanvas.drawColor(Color.WHITE);
		lockCanvas.drawBitmap(bitmap, 0, 0, paint);
		this.surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
	}
}
