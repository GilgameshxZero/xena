package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.filesystem.SvgFileScribe;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;
import com.onyx.android.sdk.data.note.TouchPoint;
import com.onyx.android.sdk.pen.data.TouchPointList;
import com.onyx.android.sdk.pen.RawInputCallback;
import com.onyx.android.sdk.pen.TouchHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Bitmap;
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
import android.view.View;
import android.widget.ImageView;

public class ScribbleActivity extends Activity {
	private final float DRAW_MOVE_EPSILON = 3;
	static public final Paint PAINT;
	static {
		PAINT = new Paint();
		PAINT.setAntiAlias(true);
		PAINT.setColor(Color.BLACK);
		PAINT.setStyle(Paint.Style.STROKE);
		PAINT.setStrokeJoin(Paint.Join.ROUND);
		PAINT.setStrokeCap(Paint.Cap.ROUND);
		PAINT.setStrokeWidth(Chunk.STROKE_WIDTH);
	}

	private SvgFileScribe svgFileScribe = new SvgFileScribe();
	private PathManager pathManager;

	private ImageView imageView;
	private Bitmap imageViewBitmap;
	private Canvas imageViewCanvas;
	private TouchHelper touchHelper;
	private Uri uri;
	private boolean isRawInputting = false;

	private RawInputCallback rawInputCallback = new RawInputCallback() {
		private final int DEBOUNCE_REDRAW_DELAY_MS = 300;

		private PointF previousErasePoint = new PointF();
		private PointF previousTentativeDrawPoint = new PointF();
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
					Log.v(XenaApplication.TAG, "ScribbleActivity::debounceRedrawTask");
					isRawInputting = false;
					drawBitmapToView(true);
					touchHelper.setRawDrawingEnabled(false).setRawDrawingEnabled(true);
				}
			};
			new Timer().schedule(this.debounceRedrawTask, delayMs);
		}

		@Override
		public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
			isRawInputting = true;
			this.debounceRedrawTask.cancel();
			this.previousTentativeDrawPoint.set(touchPoint.x, touchPoint.y);
			this.currentPath = pathManager.addPath(new PointF(
					touchPoint.x - pathManager.getViewportOffset().x,
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
			if (imageView.isDirty()) {
				Log.v(XenaApplication.TAG,
						"ScribbleActivity::onRawDrawingTouchPointMoveReceived: ImageView is dirty, skipping draw.");
			} else {
				imageViewCanvas.drawLine(
						previousTentativeDrawPoint.x,
						previousTentativeDrawPoint.y,
						touchPoint.x,
						touchPoint.y,
						PAINT);
				imageView.postInvalidate();
				previousTentativeDrawPoint.set(touchPoint.x, touchPoint.y);
			}

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
				drawBitmapToView(true);
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

	private View.OnTouchListener imageViewOnTouchListener = new View.OnTouchListener() {
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
					drawBitmapToView(false);
					// No need to reset raw input capture here, for some reason.
					break;
				case MotionEvent.ACTION_UP:
					drawBitmapToView(true);
					break;
			}
			return true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_scribble);
		this.uri = getIntent().getData();

		this.imageView = findViewById(R.id.activity_scribble_image_view);
		this.imageView.post(new Runnable() {
			@Override
			public void run() {
				pathManager = new PathManager(
						new Point(imageView.getWidth(), imageView.getHeight()));
				SvgFileScribe.loadPathsFromSvg(ScribbleActivity.this, uri, pathManager);
				imageViewBitmap = Bitmap.createBitmap(imageView.getWidth(),
						imageView.getHeight(),
						Bitmap.Config.ARGB_8888);
				imageViewCanvas = new Canvas(imageViewBitmap);
				imageView.setImageBitmap(imageViewBitmap);
				drawBitmapToView(true);

				touchHelper
						.setLimitRect(
								new Rect(0, 0, imageView.getWidth(), imageView.getHeight()),
								new ArrayList<>())
						.openRawDrawing().setRawDrawingEnabled(true);
			}
		});
		this.imageView.setOnTouchListener(imageViewOnTouchListener);

		this.touchHelper = TouchHelper.create(imageView, rawInputCallback)
				.setStrokeWidth(Chunk.STROKE_WIDTH)
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL);
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

	private void drawBitmapToView(boolean force) {
		if (!force && imageView.isDirty()) {
			Log.v(XenaApplication.TAG,
					"ScribbleActivity::drawBitmapToView: ImageView is dirty, skipping draw.");
			return;
		}
		for (Chunk chunk : pathManager.getVisibleChunks()) {
			this.imageViewCanvas.drawBitmap(chunk.getBitmap(),
					chunk.OFFSET_X + pathManager.getViewportOffset().x,
					chunk.OFFSET_Y + pathManager.getViewportOffset().y, null);
		}
		this.imageView.postInvalidate();
	}
}
