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
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
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
	private final float DRAW_MOVE_EPSILON = 3;
	private final int STROKE_WIDTH_TENTATIVE = Chunk.STROKE_WIDTH / 4;
	static public final Paint PAINT;
	static {
		PAINT = new Paint();
		PAINT.setColor(Color.BLACK);
		PAINT.setStyle(Paint.Style.FILL);
	}

	private SvgFileScribe svgFileScribe = new SvgFileScribe();
	private PathManager pathManager;

	private SurfaceView surfaceView;
	private boolean surfaceAvailable = false;
	private ReentrantLock surfaceLock = new ReentrantLock();
	private TouchHelper touchHelper;
	private Uri uri;
	private boolean isRawInputting = false;

	private RawInputCallback rawInputCallback = new RawInputCallback() {
		static Rect refreshRect = new Rect();

		private final int DEBOUNCE_REDRAW_DELAY_MS = 1000;

		private PointF previousErasePoint = new PointF();
		private CompoundPath currentPath;

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
			this.currentPath = pathManager
					.addPath(new PointF(touchPoint.x - pathManager.getViewportOffset().x,
							touchPoint.y - pathManager.getViewportOffset().y))
					.getValue();
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			// The new path has already been loaded by the PathManager. Conclude it by
			// drawing it onto the chunk bitmaps here.
			pathManager.finalizePath(this.currentPath);
			this.debounceRedraw(DEBOUNCE_REDRAW_DELAY_MS);
			svgFileScribe.debounceSave(ScribbleActivity.this, uri,
					pathManager);
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
			PointF lastPoint = this.currentPath.points
					.get(this.currentPath.points.size() - 1);
			PointF newPoint = new PointF(
					touchPoint.x - pathManager.getViewportOffset().x,
					touchPoint.y - pathManager.getViewportOffset().y);
			if (this.currentPath.points.size() > 1
					&& Geometry.distance(lastPoint, newPoint) < DRAW_MOVE_EPSILON) {
				return;
			}
			// // Temporary line to be traced over later.
			// refreshRect.set(
			// 		(int) Math.min(Math.floor(touchPoint.x),
			// 				Math.floor(lastPoint.x + pathManager.getViewportOffset().x))
			// 				- STROKE_WIDTH_TENTATIVE,
			// 		(int) Math.min(Math.floor(touchPoint.y),
			// 				Math.ceil(lastPoint.y + pathManager.getViewportOffset().y))
			// 				- STROKE_WIDTH_TENTATIVE,
			// 		(int) Math.max(Math.ceil(touchPoint.x),
			// 				Math.ceil(lastPoint.x + pathManager.getViewportOffset().x))
			// 				+ STROKE_WIDTH_TENTATIVE,
			// 		(int) Math.max(Math.ceil(touchPoint.y),
			// 				Math.ceil(lastPoint.y + pathManager.getViewportOffset().y))
			// 				+ STROKE_WIDTH_TENTATIVE);
			// Log.d(XenaApplication.TAG,
			// 		"ScribbleActivity::onRawDrawingTouchPointMoveReceived: "
			// 				+ refreshRect.toString());
			// Canvas lockCanvas = surfaceView.getHolder().lockCanvas(refreshRect);
			// lockCanvas.drawRect(refreshRect, ScribbleActivity.PAINT);
			// surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);

			this.currentPath.addPoint(newPoint);
		}

		@Override
		public void onRawDrawingTouchPointListReceived(
				TouchPointList touchPointList) {
		}

		@Override
		public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
			isRawInputting = true;
			this.debounceRedrawTask.cancel();
			this.previousErasePoint.set(
					touchPoint.x - pathManager.getViewportOffset().x,
					touchPoint.y - pathManager.getViewportOffset().y);
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
			int initialSize = pathManager.getPathsCount();
			PointF currentErasePoint = new PointF(
					touchPoint.x - pathManager.getViewportOffset().x,
					touchPoint.y - pathManager.getViewportOffset().y);

			for (Chunk chunk : pathManager.getVisibleChunks()) {
				// Copy so that concurrent read/writes don't happen.
				HashSet<Integer> pathIds = new HashSet<Integer>(chunk.getPathIds());
				for (Integer pathId : pathIds) {
					if (pathManager.getPath(pathId).isIntersectingSegment(
							this.previousErasePoint,
							currentErasePoint)) {
						pathManager.removePathId(pathId);
					}
				}
			}

			if (initialSize != pathManager.getPathsCount()) {
				drawBitmapToSurface();
				svgFileScribe.debounceSave(ScribbleActivity.this, uri,
						pathManager);
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
					pathManager.setViewportOffset(new PointF(
							pathManager.getViewportOffset().x + touchPoint.x
									- previousPoint.x,
							pathManager.getViewportOffset().y + touchPoint.y
									- previousPoint.y));
					previousPoint.x = touchPoint.x;
					previousPoint.y = touchPoint.y;
					drawBitmapToSurface();
					// No need to reset raw input capture here, for some reason.
					break;
				case MotionEvent.ACTION_UP:
					break;
			}
			return true;
		}
	};

	// openRawDrawing must not be called twice in succession.
	private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.d(XenaApplication.TAG, "ScribbleActivity::surfaceHolderCallback: ("
					+ width + ", " + height + ").");

			surfaceLock.lock();
			touchHelper.closeRawDrawing();

			surfaceAvailable = true;
			pathManager = new PathManager(new Point(width, height));
			SvgFileScribe.loadPathsFromSvg(ScribbleActivity.this, uri, pathManager);
			drawBitmapToSurface();

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

		this.surfaceView = findViewById(R.id.activity_draw_surface_view);
		this.surfaceView.getHolder().addCallback(surfaceHolderCallback);
		this.surfaceView.setOnTouchListener(surfaceViewOnTouchListener);
		this.surfaceAvailable = false;

		this.touchHelper = TouchHelper.create(surfaceView, rawInputCallback)
				.setStrokeWidth(Chunk.STROKE_WIDTH)
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
				this.pathManager, 0);
		this.touchHelper.setRawDrawingEnabled(false);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		this.touchHelper.closeRawDrawing();
		super.onDestroy();
	}

	private void drawBitmapToSurface() {
		this.surfaceLock.lock();
		if (this.surfaceAvailable) {
			// Do not use hardware canvas here so that we can lock regions later on.
			Canvas lockCanvas = this.surfaceView.getHolder().lockCanvas();
			for (Chunk chunk : pathManager.getVisibleChunks()) {
				lockCanvas.drawBitmap(chunk.getBitmap(),
						chunk.OFFSET_X + pathManager.getViewportOffset().x,
						chunk.OFFSET_Y + pathManager.getViewportOffset().y, null);
			}
			this.surfaceView.getHolder().unlockCanvasAndPost(lockCanvas);
		}
		this.surfaceLock.unlock();
	}
}
