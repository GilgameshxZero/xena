package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.algorithm.Geometry;
import com.gilgamesh.xena.filesystem.SvgFileScribe;
import com.gilgamesh.xena.pdf.PageBitmap;
import com.gilgamesh.xena.pdf.PdfReader;
import com.gilgamesh.xena.R;
import com.gilgamesh.xena.XenaApplication;

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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ScribbleActivity extends Activity
		implements View.OnClickListener {
	static private final float FLICK_MOVE_RATIO = 0.8f;
	static private final float TOUCH_BORDER_INVALID_RATIO = 0.15f;
	static private final float DRAW_MOVE_EPSILON = 3f;
	static private final Paint PAINT_TENTATIVE_LINE;
	static {
		PAINT_TENTATIVE_LINE = new Paint();
		PAINT_TENTATIVE_LINE.setAntiAlias(true);
		PAINT_TENTATIVE_LINE.setColor(Color.BLACK);
		PAINT_TENTATIVE_LINE.setStyle(Paint.Style.STROKE);
		PAINT_TENTATIVE_LINE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_TENTATIVE_LINE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_TENTATIVE_LINE.setStrokeWidth(Chunk.STROKE_WIDTH);
	}
	static private final Paint PAINT_TRANSPARENT;
	static {
		PAINT_TRANSPARENT = new Paint();
		PAINT_TRANSPARENT.setAntiAlias(true);
		PAINT_TRANSPARENT
				.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
		PAINT_TRANSPARENT.setColor(Color.TRANSPARENT);
	}

	static public final String EXTRA_PDF_URI = "EXTRA_PDF_URI";

	private SvgFileScribe svgFileScribe = new SvgFileScribe();
	private PathManager pathManager;
	private PdfReader pdfReader;

	private ScribbleView scribbleView;
	private Bitmap scribbleViewBitmap;
	private Canvas scribbleViewCanvas;
	private TouchHelper touchHelper;
	private Uri svgUri;
	// pdfUri is null if no PDF is loaded.
	private Uri pdfUri;

	private boolean isDrawing = false;
	private boolean isErasing = false;
	private boolean isRedrawing = false;
	private boolean isInputCooldown = false;
	private boolean isPanning = false;
	private PointF panBeginOffset = new PointF();

	public void redraw() {
		this.isRedrawing = false;
		this.drawBitmapToView(true, true);
		this.touchHelper.setRawDrawingEnabled(false).setRawDrawingEnabled(true);
	}

	private RawInputCallback rawInputCallback = new RawInputCallback() {
		private final int DEBOUNCE_REDRAW_DELAY_MS = 1000;
		private final int DEBOUNCE_INPUT_COOLDOWN_DELAY_MS = 200;

		// Drawing end/begin pairs may fire within milliseconds. In this case,
		// ignore both events. We accomplish this with a debounce on the end events.
		private final int DEBOUNCE_END_DRAW_DELAY_MS = 10;

		// The erasing events are a little broken. The end erase event is not
		// guaranteed, and sometimes without lifting the eraser there are multiple
		// begin/end events created. Thus, we only interpret an erase event ending
		// if no erase-related events have been received within a certain time.
		// Before then, we erase all points received.
		private final int DEBOUNCE_END_ERASE_DELAY_MS = 150;

		private PointF previousErasePoint = new PointF();
		private PointF previousTentativeDrawPoint = new PointF();
		private CompoundPath currentPath;

		// Cancellable dummy.
		private TimerTask debounceRedrawTask = new TimerTask() {
			@Override
			public void run() {
			}
		};

		private void cancelRedraw() {
			isRedrawing = false;
			this.debounceRedrawTask.cancel();
		}

		private void debounceRedraw(int delayMs) {
			this.cancelRedraw();
			isRedrawing = true;
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					Log.v(XenaApplication.TAG, "ScribbleActivity::debounceRedrawTask");

					redraw();
				}
			};
			new Timer().schedule(task, delayMs);
			this.debounceRedrawTask = task;
		}

		// Debounce cooldown for input end operations to prevent panning; implements
		// palm rejection.
		private TimerTask debounceInputCooldownTask = new TimerTask() {
			@Override
			public void run() {
			}
		};

		private void debounceInputCooldown(int delayMs) {
			this.debounceInputCooldownTask.cancel();
			isInputCooldown = true;
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					Log.v(XenaApplication.TAG,
							"ScribbleActivity::debounceInputCooldownTask");

					isInputCooldown = false;
				}
			};
			new Timer().schedule(task, delayMs);
			this.debounceInputCooldownTask = task;
		}

		// Debounce for erroneous draw end/begin pairs.
		private TimerTask debounceEndDrawTask = new TimerTask() {
			@Override
			public void run() {
			}
		};

		private void debounceEndDraw(int delayMs) {
			this.debounceEndDrawTask.cancel();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					Log.v(XenaApplication.TAG, "ScribbleActivity::debounceEndDrawTask");

					// The new path has already been loaded by the PathManager. Conclude
					// it by drawing it onto the chunk bitmaps here.
					isDrawing = false;
					scribbleView.clearTentativePoints();
					debounceRedraw(DEBOUNCE_REDRAW_DELAY_MS);
					debounceInputCooldown(DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);

					pathManager.finalizePath(currentPath);
					svgFileScribe.debounceSave(ScribbleActivity.this, svgUri,
							pathManager);
				}
			};
			new Timer().schedule(task, delayMs);
			this.debounceEndDrawTask = task;
		}

		// Sometimes, the end erase event is never received, so debounce it.
		private TimerTask debounceEndEraseTask = new TimerTask() {
			@Override
			public void run() {
			}
		};

		private void debounceEndErase(int delayMs) {
			this.debounceEndEraseTask.cancel();
			// Split into a few steps in case of multi-threading errors.
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					Log.v(XenaApplication.TAG, "ScribbleActivity::debounceEndEraseTask");

					isErasing = false;
					debounceInputCooldown(DEBOUNCE_INPUT_COOLDOWN_DELAY_MS);
				}
			};
			new Timer().schedule(task, delayMs);
			this.debounceEndEraseTask = task;
		}

		@Override
		public void onBeginRawDrawing(boolean b, TouchPoint touchPoint) {
			// If currently panning, that means there were erroneous panning events
			// fired. Undo them, and unset panning.
			if (isPanning) {
				if (panBeginOffset != pathManager.getViewportOffset()) {
					pathManager.setViewportOffset(panBeginOffset);
					drawBitmapToView(true, true);
				}

				Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:UNDO "
						+ pathManager.getViewportOffset());

				isPanning = false;
			}

			this.debounceEndDrawTask.cancel();

			// If currently drawing, treat this event the same as a move event.
			if (isDrawing) {
				this.onRawDrawingTouchPointMoveReceived(touchPoint);
				return;
			}

			Log.v(XenaApplication.TAG, "ScribbleActivity::onBeginRawDrawing");
			this.cancelRedraw();
			isDrawing = true;
			this.previousTentativeDrawPoint.set(touchPoint.x, touchPoint.y);
			scribbleView.addTentativePoint(touchPoint.x, touchPoint.y);
			this.currentPath = pathManager
					.addPath(new PointF(touchPoint.x - pathManager.getViewportOffset().x,
							touchPoint.y - pathManager.getViewportOffset().y))
					.getValue();
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			this.debounceEndDraw(DEBOUNCE_END_DRAW_DELAY_MS);
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
			if (!isDrawing) {
				return;
			}

			PointF lastPoint = currentPath.points.get(currentPath.points.size() - 1);
			PointF newPoint = new PointF(
					touchPoint.x - pathManager.getViewportOffset().x,
					touchPoint.y - pathManager.getViewportOffset().y);

			if (currentPath.points.size() > 1
					&& Geometry.distance(lastPoint, newPoint) < DRAW_MOVE_EPSILON) {
				return;
			}
			currentPath.addPoint(newPoint);

			// Log.v(XenaApplication.TAG,
			// "ScribbleActivity::onRawDrawingTouchPointMoveReceived "
			// + touchPoint);

			if (scribbleView.isDrawing()) {
				// Log.v(XenaApplication.TAG, "Dirty ScribbleView.");
			} else {
				// Draw line for the purposes of screenshare, which does not capture any
				// raw drawing activities.
				scribbleViewCanvas.drawLine(previousTentativeDrawPoint.x,
						previousTentativeDrawPoint.y, touchPoint.x, touchPoint.y,
						PAINT_TENTATIVE_LINE);
				// scribbleView.addTentativePoint(touchPoint.x, touchPoint.y);
				scribbleView.postInvalidate();
				previousTentativeDrawPoint.set(touchPoint.x, touchPoint.y);
			}
		}

		@Override
		public void onRawDrawingTouchPointListReceived(
				TouchPointList touchPointList) {
		}

		@Override
		public void onBeginRawErasing(boolean b, TouchPoint touchPoint) {
			if (isErasing) {
				// This should be interpreted as an erase move event.
				this.onRawErasingTouchPointMoveReceived(touchPoint);
				return;
			}

			Log.v(XenaApplication.TAG, "ScribbleActivity::onBeginRawErasing");

			// This is the beginning of an erase move sequence.
			this.debounceEndErase(DEBOUNCE_END_ERASE_DELAY_MS);
			isPanning = false;
			isErasing = true;

			this.cancelRedraw();
			redraw();

			this.previousErasePoint.set(
					touchPoint.x - pathManager.getViewportOffset().x,
					touchPoint.y - pathManager.getViewportOffset().y);
		}

		@Override
		public void onEndRawErasing(boolean b, TouchPoint touchPoint) {
			if (!isErasing) {
				return;
			}

			// Process this event as a move event.
			this.onRawErasingTouchPointMoveReceived(touchPoint);

			Log.v(XenaApplication.TAG, "ScribbleActivity::onEndRawErasing");
		}

		@Override
		public void onRawErasingTouchPointMoveReceived(TouchPoint touchPoint) {
			// Some events come after onEndRawErasing; instead of ignoring those
			// events, we restart erasing at this time.
			if (!isErasing) {
				this.onBeginRawErasing(false, touchPoint);
			}

			// Log.v(XenaApplication.TAG,
			// "ScribbleActivity::onRawErasingTouchPointMoveReceived");
			this.debounceEndErase(DEBOUNCE_END_ERASE_DELAY_MS);

			// Actual logic to handle erasing.
			int initialSize = pathManager.getPathsCount();
			PointF currentErasePoint = new PointF(
					touchPoint.x - pathManager.getViewportOffset().x,
					touchPoint.y - pathManager.getViewportOffset().y);

			for (Chunk chunk : pathManager.getVisibleChunks()) {
				// Copy so that concurrent read/writes don't happen.
				HashSet<Integer> pathIds = new HashSet<Integer>(chunk.getPathIds());
				for (Integer pathId : pathIds) {
					if (pathManager.getPath(pathId).isIntersectingSegment(
							this.previousErasePoint, currentErasePoint)) {
						pathManager.removePathId(pathId);
					}
				}
			}

			if (initialSize != pathManager.getPathsCount()) {
				drawBitmapToView(true, true);
				svgFileScribe.debounceSave(ScribbleActivity.this, svgUri, pathManager);
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

	private View.OnTouchListener scribbleViewOnTouchListener = new View.OnTouchListener() {
		private final int FLICK_LOWER_BOUND_MS = 80;
		private final int FLICK_UPPER_BOUND_MS = 220;

		private PointF previousPoint = new PointF();
		private long actionDownTimeMs = 0;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (isDrawing || isErasing || isInputCooldown) {
				return false;
			}

			PointF touchPoint = new PointF(event.getX(), event.getY());
			long eventDurationMs = (System.currentTimeMillis()
					- this.actionDownTimeMs);

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// Sometimes, this fires slightly before a draw/erase event. The
					// draw/erase event will cancel panning in that case.

					if (isPanning) {
						Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:RESET "
								+ pathManager.getViewportOffset());
					} else {
						RectF bounds = new RectF(
								scribbleView.getWidth()
										* ScribbleActivity.TOUCH_BORDER_INVALID_RATIO,
								scribbleView.getHeight()
										* ScribbleActivity.TOUCH_BORDER_INVALID_RATIO,
								scribbleView.getWidth()
										* (1 - ScribbleActivity.TOUCH_BORDER_INVALID_RATIO),
								scribbleView.getHeight()
										* (1 - ScribbleActivity.TOUCH_BORDER_INVALID_RATIO));
						if (bounds.contains(touchPoint.x, touchPoint.y)) {
							// Only count actions that don't start near the border.
							Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:DOWN "
									+ pathManager.getViewportOffset());

							isPanning = true;

							this.actionDownTimeMs = System.currentTimeMillis();
						}
					}

					panBeginOffset = pathManager.getViewportOffset();
					this.previousPoint.x = touchPoint.x;
					this.previousPoint.y = touchPoint.y;

					break;
				case MotionEvent.ACTION_MOVE:
					if (!isPanning) {
						break;
					}

					// Don't process until we exit flick range.
					if (eventDurationMs <= FLICK_UPPER_BOUND_MS) {
						break;
					}

					pathManager.setViewportOffset(new PointF(
							pathManager.getViewportOffset().x + touchPoint.x
									- this.previousPoint.x,
							pathManager.getViewportOffset().y + touchPoint.y
									- this.previousPoint.y));
					this.previousPoint.x = touchPoint.x;
					this.previousPoint.y = touchPoint.y;

					// Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:MOVE "
					// + pathManager.getViewportOffset());

					if (isRedrawing) {
						redraw();
					}
					drawBitmapToView(false, true);

					// No need to reset raw input capture here, for some reason.
					break;
				case MotionEvent.ACTION_UP:
					if (!isPanning) {
						Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:IGNORE");
						break;
					}

					// Note: ACTION_UP is not guaranteed to fire after ACTION_DOWN.
					isPanning = false;

					// Detect flicks.
					if (eventDurationMs >= FLICK_LOWER_BOUND_MS
							&& eventDurationMs <= FLICK_UPPER_BOUND_MS) {
						Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:FLICK");

						// Determine flick direction.
						int direction = panBeginOffset.y < pathManager.getViewportOffset().y
								+ touchPoint.y - this.previousPoint.y ? 1 : -1;

						pathManager
								.setViewportOffset(new PointF(panBeginOffset.x, panBeginOffset.y
										+ direction * scribbleView.getHeight()
												* ScribbleActivity.FLICK_MOVE_RATIO));
					} else {
						Log.v(XenaApplication.TAG, "ScribbleActivity::onTouch:UP");
					}

					svgFileScribe.debounceSave(ScribbleActivity.this, svgUri,
							pathManager);
					drawBitmapToView(true, true);
					break;
			}
			return true;
		}
	};

	// Switching orientation may rebuild the activity.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_scribble);
		this.parseUri();

		this.scribbleView = findViewById(R.id.activity_scribble_scribble_view);
		this.scribbleView.post(new Runnable() {
			@Override
			public void run() {
				initDrawing();
			}
		});
		this.scribbleView.setOnTouchListener(scribbleViewOnTouchListener);

		this.touchHelper = TouchHelper.create(scribbleView, rawInputCallback)
				.setStrokeWidth(Chunk.STROKE_WIDTH)
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL);
	}

	@Override
	protected void onResume() {
		this.touchHelper.setRawDrawingEnabled(false).setLimitRect(
				new Rect(0, 0, this.scribbleView.getWidth(),
						this.scribbleView.getHeight()),
				new ArrayList<>()).setStrokeWidth(Chunk.STROKE_WIDTH)
				.setStrokeStyle(TouchHelper.STROKE_STYLE_PENCIL)
				.setRawDrawingEnabled(true);
		super.onResume();
	}

	@Override
	protected void onPause() {
		// onPause will be called when "back" is pressed as well.
		this.svgFileScribe.debounceSave(ScribbleActivity.this, svgUri,
				this.pathManager, 0);
		this.touchHelper.setRawDrawingEnabled(false);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		this.touchHelper.closeRawDrawing();
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
	}

	private void parseUri() {
		Uri pickedUri = this.getIntent().getData();
		this.svgUri = pickedUri;
		String pdfUriString = this.getIntent().getStringExtra(EXTRA_PDF_URI);
		if (pdfUriString != null) {
			this.pdfUri = Uri.parse(pdfUriString);
			this.pdfReader = new PdfReader(this, this.pdfUri);
			Log.v(XenaApplication.TAG,
					"ScribbleActivity::parseUri: Got 2 URIs: " + this.svgUri.toString()
							+ " and "
							+ this.pdfUri.toString() + ".");
		} else {
			Log.v(XenaApplication.TAG,
					"ScribbleActivity::parseUri: Got 1 URI: " + this.svgUri.toString()
							+ ".");
		}
	}

	private void initDrawing() {
		this.pathManager = new PathManager(
				new Point(this.scribbleView.getWidth(), this.scribbleView.getHeight()));
		SvgFileScribe.loadPathsFromSvg(ScribbleActivity.this, this.svgUri,
				this.pathManager);

		this.scribbleViewBitmap = Bitmap.createBitmap(this.scribbleView.getWidth(),
				this.scribbleView.getHeight(),
				Bitmap.Config.ARGB_8888);
		this.scribbleViewCanvas = new Canvas(this.scribbleViewBitmap);
		this.scribbleView.setImageBitmap(this.scribbleViewBitmap);
		drawBitmapToView(true, true);

		this.touchHelper
				.setLimitRect(
						new Rect(0, 0, this.scribbleView.getWidth(),
								this.scribbleView.getHeight()),
						new ArrayList<>())
				.openRawDrawing().setRawDrawingEnabled(true);
	}

	private void drawBitmapToView(boolean force, boolean invalidate) {
		if (!force && scribbleView.isDrawing()) {
			// Log.v(XenaApplication.TAG, "Dirty ScribbleView.");
			return;
		}

		this.scribbleViewCanvas.drawRect(0, 0, scribbleView.getWidth(),
				scribbleView.getHeight(),
				PAINT_TRANSPARENT);

		if (this.pdfReader != null) {
			ArrayList<PageBitmap> pages = this.pdfReader.getBitmapsForViewport(
					new RectF(-this.pathManager.getViewportOffset().x,
							-this.pathManager.getViewportOffset().y,
							-this.pathManager.getViewportOffset().x
									+ this.scribbleView.getWidth(),
							-this.pathManager.getViewportOffset().y
									+ this.scribbleView.getHeight()));
			for (PageBitmap page : pages) {
				this.scribbleViewCanvas.drawBitmap(page.bitmap,
						page.location.left + this.pathManager.getViewportOffset().x,
						page.location.top + this.pathManager.getViewportOffset().y, null);
			}
		}

		for (Chunk chunk : pathManager.getVisibleChunks()) {
			this.scribbleViewCanvas.drawBitmap(chunk.getBitmap(),
					chunk.OFFSET_X + pathManager.getViewportOffset().x,
					chunk.OFFSET_Y + pathManager.getViewportOffset().y, null);
		}

		if (invalidate) {
			this.scribbleView.postInvalidate();
		}
	}
}
