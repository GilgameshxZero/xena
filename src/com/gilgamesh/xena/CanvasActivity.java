package com.gilgamesh.xena;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ListIterator;

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
import android.graphics.PointF;

import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;

public class CanvasActivity extends Activity {
	private final float STROKE_WIDTH = 5;

	private static LinkedList<Path> drawPaths = new LinkedList<Path>();
	private static PointF drawOffset = new PointF(0, 0);

	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private SurfaceView surfaceView;
	private Bitmap bitmap;
	private Canvas canvas;

	private TouchHelper touchHelper;
	private boolean isRawInputting = false;

	private RawInputCallback rawInputCallback = new RawInputCallback() {
		private Path erasePath = new Path();
		private final int DEBOUNCE_REDRAW_DELAY_MS = 1000;
		private final int DEBOUNCE_REDRAW_ERASE_DELAY_MS = 50;
		// Cancellable dummy.
		private TimerTask debounceRedrawTask = new TimerTask() {
			@Override
			public void run() {
			}
		};

		private void resetDebounce() {
			this.debounceRedrawTask.cancel();
		}

		private void debounceRedraw(int delayMs) {
			Log.d(Xena.TAG, "CanvasActivity.debounceRedraw");
			this.resetDebounce();
			this.debounceRedrawTask = new TimerTask() {
				@Override
				public void run() {
					Log.d(Xena.TAG, "CanvasActivity.debounceRedrawTask");
					isRawInputting = false;
					drawBitmapToSurface();
					touchHelper.setRawDrawingEnabled(false);
					touchHelper.setRawDrawingEnabled(true);
				}
			};
			new Timer().schedule(this.debounceRedrawTask, delayMs);
		}

		@Override
		public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
			isRawInputting = true;
			this.resetDebounce();
			drawPaths.add(new Path());
			drawPaths.getLast()
					.moveTo(touchPoint.x - drawOffset.x,
							touchPoint.y - drawOffset.y);
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			drawPathToCanvas(drawPaths.getLast());
			this.debounceRedraw(DEBOUNCE_REDRAW_DELAY_MS);
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
			drawPaths.getLast()
					.lineTo(touchPoint.x - drawOffset.x,
							touchPoint.y - drawOffset.y);
		}

		@Override
		public void onRawDrawingTouchPointListReceived(
				TouchPointList touchPointList) {
		}

		@Override
		public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
			isRawInputting = true;
			this.resetDebounce();
			erasePath.reset();
			erasePath
					.moveTo(touchPoint.x - drawOffset.x,
							touchPoint.y - drawOffset.y);
		}

		@Override
		public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
			int initialSize = drawPaths.size();
			ListIterator<Path> iterator = drawPaths.listIterator();
			while (iterator.hasNext()) {
				Path resultPath = new Path();
				resultPath.op(iterator.next(), erasePath, Path.Op.INTERSECT);
				if (!resultPath.isEmpty()) {
					iterator.remove();
				}
			}
			Log.d(Xena.TAG, "CanvasActivity.onEndRawErasing: removed "
					+ (initialSize - drawPaths.size()) + " paths.");
			drawAllPathsToCanvas();
			this.debounceRedraw(DEBOUNCE_REDRAW_ERASE_DELAY_MS);
		}

		@Override
		public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
			erasePath
					.lineTo(touchPoint.x - drawOffset.x,
							touchPoint.y - drawOffset.y);
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
		private PointF previousPoint = new PointF(0, 0);

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			PointF touchPoint = new PointF(event.getX(), event.getY());
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					previousPoint.x = touchPoint.x;
					previousPoint.y = touchPoint.y;
					break;
				case MotionEvent.ACTION_MOVE:
					// Track the touches but do not update offset if using pen.
					if (isRawInputting) {
						break;
					}
					drawOffset.x += touchPoint.x - previousPoint.x;
					drawOffset.y += touchPoint.y - previousPoint.y;
					previousPoint.x = touchPoint.x;
					previousPoint.y = touchPoint.y;
					drawAllPathsToCanvas();
					drawBitmapToSurface();
					break;
				case MotionEvent.ACTION_UP:
					if (isRawInputting) {
						break;
					}
					Log.d(Xena.TAG,
							"CanvasActivity.surfaceViewOnTouchListener: drawOffset = ("
									+ drawOffset.x + ", " + drawOffset.y + ".");
					break;
			}
			return true;
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
			drawAllPathsToCanvas();
			drawBitmapToSurface();

			touchHelper.setLimitRect(new Rect(0, 0, width, height), new ArrayList<>())
					.openRawDrawing().setRawDrawingEnabled(true);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			touchHelper.closeRawDrawing();
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

	private void drawPathToCanvas(Path path) {
		Path offsetPath = new Path(path);
		offsetPath.offset(drawOffset.x, drawOffset.y);
		canvas.drawPath(offsetPath, paint);
	}

	private void drawAllPathsToCanvas() {
		canvas.drawColor(Color.WHITE);
		for (Path path : drawPaths) {
			drawPathToCanvas(path);
		}
	}

	private void drawBitmapToSurface() {
		Canvas lockCanvas = this.surfaceView.getHolder().lockCanvas();
		lockCanvas.drawColor(Color.WHITE);
		lockCanvas.drawBitmap(bitmap, 0, 0, paint);
		this.surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
	}
}
