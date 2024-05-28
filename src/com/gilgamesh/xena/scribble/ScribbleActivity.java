package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.filesystem.SvgFileScribe;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;

import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
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
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class ScribbleActivity extends Activity {
	private final int STROKE_WIDTH = 4;
	private final float DRAW_MOVE_EPSILON = 3;

	private SvgFileScribe svgFileScribe = new SvgFileScribe();
	private PathDrawer pathDrawer = new PathDrawer();

	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private SurfaceView surfaceView;
	private Bitmap bitmap = null;
	private Canvas canvas;

	private boolean surfaceAvailable = false;
	private ReentrantLock surfaceLock = new ReentrantLock();
	private TouchHelper touchHelper;
	private Uri uri;
	private boolean isRawInputting = false;

	private RawInputCallback rawInputCallback = new RawInputCallback() {
		private final int DEBOUNCE_REDRAW_DELAY_MS = 1000;

		private PointF previousErasePoint = new PointF();

		// Cancellable dummy.
		private TimerTask debounceRedrawTask = new TimerTask() {
			@Override
			public void run() {
			}
		};

		private void debounceRedraw(int delayMs) {
			this.debounceRedrawTask.cancel();
			this.debounceRedrawTask = new TimerTask() {
				@Override
				public void run() {
					Log.d(XenaApplication.TAG, "ScribbleActivity::debounceRedrawTask");
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
			pathDrawer.paths
					.add(new CompoundPath(touchPoint.x - pathDrawer.viewportOffset.x,
							touchPoint.y - pathDrawer.viewportOffset.y));
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			drawPathToCanvas(pathDrawer.paths.getLast().path);
			this.debounceRedraw(DEBOUNCE_REDRAW_DELAY_MS);
			svgFileScribe.debounceSave(ScribbleActivity.this, uri,
					pathDrawer.paths, pathDrawer.viewportOffset,
					STROKE_WIDTH);
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
			PointF lastPoint = pathDrawer.paths.getLast().points
					.get(pathDrawer.paths.getLast().points.size() - 1);
			PointF newPoint = new PointF(touchPoint.x - pathDrawer.viewportOffset.x,
					touchPoint.y - pathDrawer.viewportOffset.y);
			if (pathDrawer.paths.getLast().points.size() > 1
					&& Geometry.distance(lastPoint, newPoint) < DRAW_MOVE_EPSILON) {
				return;
			}
			pathDrawer.paths.getLast().addPoint(newPoint);
		}

		@Override
		public void onRawDrawingTouchPointListReceived(
				TouchPointList touchPointList) {
		}

		@Override
		public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
			Log.d(XenaApplication.TAG,
					"ScribbleActivity::onBeginRawErasing.");
			isRawInputting = true;
			this.debounceRedrawTask.cancel();
			this.previousErasePoint.set(touchPoint.x - pathDrawer.viewportOffset.x,
					touchPoint.y - pathDrawer.viewportOffset.y);
		}

		@Override
		public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
			// Delay the raw input release since the touch events come a little later
			// after onEndRawErasing.
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					isRawInputting = false;
				}
			}, 50);
		}

		@Override
		public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
			int initialSize = pathDrawer.paths.size();
			PointF currentErasePoint = new PointF(
					touchPoint.x - pathDrawer.viewportOffset.x,
					touchPoint.y - pathDrawer.viewportOffset.y);
			ListIterator<CompoundPath> iterator = pathDrawer.paths
					.listIterator();
			while (iterator.hasNext()) {
				if (iterator.next().isIntersectingSegment(this.previousErasePoint,
						currentErasePoint)) {
					iterator.remove();
				}
			}
			if (initialSize != pathDrawer.paths.size()) {
				drawAllPathsToCanvas();
				drawBitmapToSurface();
				svgFileScribe.debounceSave(ScribbleActivity.this, uri,
						pathDrawer.paths, pathDrawer.viewportOffset,
						STROKE_WIDTH);
			}
			this.previousErasePoint.set(currentErasePoint);
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
			if (isRawInputting) {
				return false;
			}
			PointF touchPoint = new PointF(event.getX(), event.getY());
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					previousPoint.x = touchPoint.x;
					previousPoint.y = touchPoint.y;
					break;
				case MotionEvent.ACTION_MOVE:
					pathDrawer.viewportOffset.x += touchPoint.x - previousPoint.x;
					pathDrawer.viewportOffset.y += touchPoint.y - previousPoint.y;
					previousPoint.x = touchPoint.x;
					previousPoint.y = touchPoint.y;
					drawAllPathsToCanvas();
					drawBitmapToSurface();
					// No need to reset raw input capture here, for some reason.
					break;
				case MotionEvent.ACTION_UP:
					Log.d(XenaApplication.TAG,
							"ScribbleActivity::surfaceViewOnTouchListener: pathDrawer.viewportOffset = ("
									+ pathDrawer.viewportOffset.x + ", "
									+ pathDrawer.viewportOffset.y + ").");
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
					"ScribbleActivity::surfaceHolderCallback: width = "
							+ width + ", height = " + height + ".");
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
		this.uri = getIntent().getData();
		this.pathDrawer.paths = SvgFileScribe.loadPathsFromSvg(
				ScribbleActivity.this,
				uri, this.pathDrawer.viewportOffset);

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
		this.svgFileScribe.debounceSave(ScribbleActivity.this, uri,
				this.pathDrawer.paths, this.pathDrawer.viewportOffset,
				STROKE_WIDTH, 0);
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
		offsetPath.offset(this.pathDrawer.viewportOffset.x,
				this.pathDrawer.viewportOffset.y);
		canvas.drawPath(offsetPath, paint);
	}

	private void drawAllPathsToCanvas() {
		canvas.drawColor(Color.WHITE);
		for (CompoundPath path : this.pathDrawer.paths) {
			drawPathToCanvas(path.path);
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
