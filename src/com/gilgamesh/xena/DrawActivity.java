package com.gilgamesh.xena;

import com.gilgamesh.xena.filesystem.SvgFileIo;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

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
import android.net.Uri;

import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;

public class DrawActivity extends Activity {
	private static LinkedList<Path> pathsNative = new LinkedList<Path>();
	private static LinkedList<Path> pathsNativeFilled = new LinkedList<Path>();
	// Duplicate list of paths which can be iterated through.
	private static LinkedList<ArrayList<PointF>> paths = new LinkedList<ArrayList<PointF>>();
	private static PointF drawOffset = new PointF(0, 0);
	private static Uri uri;

	private final int STROKE_WIDTH = 4;
	private final float DRAW_MOVE_EPSILON = 3;

	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private SurfaceView surfaceView;
	private Bitmap bitmap = null;
	private Canvas canvas;
	private boolean surfaceAvailable = false;
	private ReentrantLock surfaceLock = new ReentrantLock();

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

		private void debounceRedraw(int delayMs) {
			Log.d(XenaApplication.TAG, "DrawActivity::debounceRedraw");
			this.debounceRedrawTask.cancel();
			this.debounceRedrawTask = new TimerTask() {
				@Override
				public void run() {
					Log.d(XenaApplication.TAG, "DrawActivity::debounceRedrawTask");
					isRawInputting = false;
					drawBitmapToSurface();
					touchHelper.setRawDrawingEnabled(false).setRawDrawingEnabled(true);
				}
			};
			new Timer().schedule(this.debounceRedrawTask, delayMs);
		}

		@Override
		public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
			isRawInputting = true;
			this.debounceRedrawTask.cancel();
			pathsNative.add(new Path());
			paths.add(new ArrayList<PointF>());
			pathsNative.getLast()
					.moveTo(touchPoint.x - drawOffset.x,
							touchPoint.y - drawOffset.y);
			paths.getLast().add(
					new PointF(touchPoint.x - drawOffset.x, touchPoint.y - drawOffset.y));
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			pathsNativeFilled.add(new Path());
			paint.getFillPath(pathsNative.getLast(), pathsNativeFilled.getLast());
			drawPathToCanvas(pathsNative.getLast());
			this.debounceRedraw(DEBOUNCE_REDRAW_DELAY_MS);
			SvgFileIo.debounceSave(DrawActivity.this, uri, paths, STROKE_WIDTH);
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
			PointF lastPoint = paths.getLast()
					.get(paths.getLast().size() - 1);
			PointF newPoint = new PointF(touchPoint.x - drawOffset.x,
					touchPoint.y - drawOffset.y);
			PointF pointDelta = new PointF(newPoint.x - lastPoint.x,
					newPoint.y - lastPoint.y);
			if (paths.getLast().size() > 1) {
				if (Math.sqrt(pointDelta.x * pointDelta.x
						+ pointDelta.y * pointDelta.y) < DRAW_MOVE_EPSILON) {
					return;
				}
			}
			pathsNative.getLast()
					.lineTo(newPoint.x, newPoint.y);
			paths.getLast().add(newPoint);
		}

		@Override
		public void onRawDrawingTouchPointListReceived(
				TouchPointList touchPointList) {
		}

		@Override
		public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
			isRawInputting = true;
			this.debounceRedrawTask.cancel();
			erasePath.reset();
			erasePath
					.moveTo(touchPoint.x - drawOffset.x,
							touchPoint.y - drawOffset.y);
		}

		@Override
		public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
			int initialSize = paths.size();
			ListIterator<ArrayList<PointF>> iterator = paths
					.listIterator();
			ListIterator<Path> iteratorNative = pathsNative.listIterator();
			ListIterator<Path> iteratorNativeFilled = pathsNativeFilled
					.listIterator();
			paint.getFillPath(erasePath, erasePath);
			Path intersectPath = new Path();
			while (iterator.hasNext()) {
				iterator.next();
				iteratorNative.next();
				intersectPath.op(iteratorNativeFilled.next(), erasePath,
						Path.Op.INTERSECT);
				if (!intersectPath.isEmpty()) {
					iterator.remove();
					iteratorNative.remove();
					iteratorNativeFilled.remove();
				}
			}
			Log.d(XenaApplication.TAG, "DrawActivity::onEndRawErasing: removed "
					+ (initialSize - paths.size()) + " paths.");
			drawAllPathsToCanvas();
			this.debounceRedraw(DEBOUNCE_REDRAW_ERASE_DELAY_MS);
			SvgFileIo.debounceSave(DrawActivity.this, uri, paths, STROKE_WIDTH);
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
					// No need to reset raw input capture here, for some reason.
					break;
				case MotionEvent.ACTION_UP:
					if (isRawInputting) {
						break;
					}
					Log.d(XenaApplication.TAG,
							"DrawActivity::surfaceViewOnTouchListener: drawOffset = ("
									+ drawOffset.x + ", " + drawOffset.y + ").");
					break;
			}
			return true;
		}
	};

	// openRawDrawing must not be called twice in succession.
	private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			surfaceLock.lock();
			surfaceAvailable = true;
			surfaceLock.unlock();
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			surfaceLock.lock();
			touchHelper.closeRawDrawing();

			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			canvas = new Canvas(bitmap);
			drawAllPathsToCanvas();
			drawBitmapToSurface();

			Log.d(XenaApplication.TAG,
					"DrawActivity::surfaceHolderCallback: width = " + width
							+ ", height = " + height + ".");
			touchHelper.setLimitRect(new Rect(0, 0, width, height), new ArrayList<>())
					.openRawDrawing().setRawDrawingEnabled(true);
			surfaceLock.unlock();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			surfaceLock.lock();
			surfaceAvailable = false;
			surfaceLock.unlock();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_draw);
		DrawActivity.uri = getIntent().getData();
		DrawActivity.paths = SvgFileIo.loadPathsFromSvg(DrawActivity.this, uri);
		for (ArrayList<PointF> path : paths) {
			Path pathNative = new Path();
			pathNative.moveTo(path.get(0).x, path.get(0).y);
			for (int i = 1; i < path.size(); i++) {
				pathNative.lineTo(path.get(i).x, path.get(i).y);
			}
			DrawActivity.pathsNative.add(pathNative);

			Path pathNativeFilled = new Path();
			paint.getFillPath(pathNative, pathNativeFilled);
			DrawActivity.pathsNativeFilled.add(pathNativeFilled);
		}

		this.paint.setColor(Color.BLACK);
		this.paint.setStrokeWidth(STROKE_WIDTH);
		this.paint.setStyle(Paint.Style.STROKE);
		this.paint.setStrokeJoin(Paint.Join.ROUND);
		this.paint.setStrokeCap(Paint.Cap.ROUND);
		this.paint.setPathEffect(new CornerPathEffect(STROKE_WIDTH));

		this.surfaceView = findViewById(R.id.activity_draw_surface_view);
		this.surfaceView.getHolder().addCallback(surfaceHolderCallback);
		this.surfaceView.setOnTouchListener(surfaceViewOnTouchListener);

		this.touchHelper = TouchHelper.create(surfaceView, rawInputCallback)
				.setStrokeWidth(this.STROKE_WIDTH)
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL).openRawDrawing()
				.setRawDrawingEnabled(true);
	}

	@Override
	protected void onResume() {
		this.touchHelper.setRawDrawingEnabled(true);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// onPause will be called when "back" is pressed as well.
		SvgFileIo.debounceSave(DrawActivity.this, uri, paths, STROKE_WIDTH, 0);
		this.touchHelper.setRawDrawingEnabled(false);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		this.touchHelper.closeRawDrawing();
		super.onDestroy();
	}

	private void drawPathToCanvas(Path path) {
		Path offsetPath = new Path(path);
		offsetPath.offset(drawOffset.x, drawOffset.y);
		canvas.drawPath(offsetPath, paint);
	}

	private void drawAllPathsToCanvas() {
		canvas.drawColor(Color.WHITE);
		for (Path pathNative : pathsNative) {
			drawPathToCanvas(pathNative);
		}
	}

	private void drawBitmapToSurface() {
		this.surfaceLock.lock();
		if (surfaceAvailable) {
			Canvas lockCanvas = this.surfaceView.getHolder().lockCanvas();
			lockCanvas.drawColor(Color.WHITE);
			lockCanvas.drawBitmap(bitmap, 0, 0, paint);
			this.surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
		}
		this.surfaceLock.unlock();
	}
}
