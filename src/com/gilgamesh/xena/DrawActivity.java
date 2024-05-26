package com.gilgamesh.xena;

import java.util.LinkedList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ListIterator;
import java.util.Iterator;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
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
	private static LinkedList<Path> drawPaths = new LinkedList<Path>();
	// Duplicate list of paths which can be iterated through.
	private static LinkedList<ArrayList<PointF>> drawPathsLocal = new LinkedList<ArrayList<PointF>>();
	private static PointF drawOffset = new PointF(0, 0);
	private static Uri uri;

	private static void coverWithRectF(RectF rectangle, PointF point) {
		rectangle.left = Math.min(rectangle.left, point.x);
		rectangle.top = Math.min(rectangle.top, point.y);
		rectangle.right = Math.max(rectangle.right, point.x);
		rectangle.bottom = Math.max(rectangle.bottom, point.y);
	}

	private final int STROKE_WIDTH = 4;
	private final float DRAW_SVG_SCALE_FACTOR = 8;
	private final float DRAW_MOVE_EPSILON = 3;

	private final int DEBOUNCE_SAVE_DELAY_MS = 15000;
	private TimerTask debounceSaveTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	private void debounceSave(int delayMs) {
		this.debounceSaveTask.cancel();
		this.debounceSaveTask = new TimerTask() {
			@Override
			public void run() {
				OutputStreamWriter outputStreamWriter;
				try {
					outputStreamWriter = new OutputStreamWriter(
							getContentResolver().openOutputStream(uri, "wt"));
					try {
						RectF containerBox = new RectF(Float.POSITIVE_INFINITY,
								Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
								Float.NEGATIVE_INFINITY);
						StringBuilder stringBuilder = new StringBuilder();
						for (ArrayList<PointF> path : drawPathsLocal) {
							stringBuilder.append(
									"<path d=\"");
							Iterator<PointF> pathIterator = path.iterator();
							PointF point = pathIterator.next();
							stringBuilder
									.append(
											"M" + Math.round(point.x * DRAW_SVG_SCALE_FACTOR) + " "
													+ Math.round(point.y * DRAW_SVG_SCALE_FACTOR) + "l");
							DrawActivity.coverWithRectF(containerBox, point);

							PointF nextPoint;
							PointF pointDelta = new PointF(0, 0);
							PointF roundError = new PointF(0, 0);
							String[] pointDeltaS = new String[2];
							while (pathIterator.hasNext()) {
								nextPoint = pathIterator.next();

								// Minimize ` -`.
								pointDelta.x = nextPoint.x * DRAW_SVG_SCALE_FACTOR
										- point.x * DRAW_SVG_SCALE_FACTOR + roundError.x;
								pointDelta.y = nextPoint.y * DRAW_SVG_SCALE_FACTOR
										- point.y * DRAW_SVG_SCALE_FACTOR + roundError.y;
								roundError.x = pointDelta.x - Math.round(pointDelta.x);
								roundError.y = pointDelta.y - Math.round(pointDelta.y);
								pointDeltaS[0] = String.valueOf(Math.round(pointDelta.x));
								pointDeltaS[1] = String.valueOf(Math.round(pointDelta.y));
								if (pointDeltaS[0].charAt(0) != '-') {
									stringBuilder.append(" ");
								}
								stringBuilder.append(pointDeltaS[0]);
								if (pointDeltaS[1].charAt(0) != '-') {
									stringBuilder.append(" ");
								}
								stringBuilder.append(pointDeltaS[1]);

								point = nextPoint;
								DrawActivity.coverWithRectF(containerBox, point);
							}
							stringBuilder.append("\"/>\n");
						}

						containerBox.left -= STROKE_WIDTH * DRAW_SVG_SCALE_FACTOR;
						containerBox.top -= STROKE_WIDTH * DRAW_SVG_SCALE_FACTOR;
						containerBox.right += STROKE_WIDTH * DRAW_SVG_SCALE_FACTOR;
						containerBox.bottom += STROKE_WIDTH * DRAW_SVG_SCALE_FACTOR;
						outputStreamWriter.write(
								"<svg viewBox=\""
										+ Math.round(containerBox.left * DRAW_SVG_SCALE_FACTOR)
										+ " "
										+ Math.round(containerBox.top * DRAW_SVG_SCALE_FACTOR) + " "
										+ Math.round(containerBox.right - containerBox.left)
												* DRAW_SVG_SCALE_FACTOR
										+ " "
										+ Math.round(containerBox.bottom - containerBox.top)
												* DRAW_SVG_SCALE_FACTOR
										+ "\" stroke=\"black\" stroke-width=\""
										+ Math.round(STROKE_WIDTH * DRAW_SVG_SCALE_FACTOR)
										+ "\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">");
						outputStreamWriter.write(
								"<style>@media(prefers-color-scheme:dark){svg{background-color:black;stroke:white;}}</style>\n");
						outputStreamWriter.write(stringBuilder.toString());
						outputStreamWriter.write(
								"</svg>\n");
					} catch (IOException e) {
						Log.e(Xena.TAG,
								"DrawActivity::debounceSaveTask: Failed to write to file: "
										+ e.toString() + ".");
					} finally {
						outputStreamWriter.close();
					}
				} catch (IOException e) {
					Log.e(Xena.TAG,
							"DrawActivity::debounceSaveTask: Failed to write to file: "
									+ e.toString() + ".");
				}
				Log.d(Xena.TAG, "DrawActivity::debounceSaveTask: Finished to "
						+ uri.toString() + ".");
			}
		};
		new Timer().schedule(this.debounceSaveTask, delayMs);
	}

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
			Log.d(Xena.TAG, "DrawActivity::debounceRedraw");
			this.debounceRedrawTask.cancel();
			this.debounceRedrawTask = new TimerTask() {
				@Override
				public void run() {
					Log.d(Xena.TAG, "DrawActivity::debounceRedrawTask");
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
			drawPaths.add(new Path());
			drawPathsLocal.add(new ArrayList<PointF>());
			drawPaths.getLast()
					.moveTo(touchPoint.x - drawOffset.x,
							touchPoint.y - drawOffset.y);
			drawPathsLocal.getLast().add(
					new PointF(touchPoint.x - drawOffset.x, touchPoint.y - drawOffset.y));
		}

		@Override
		public void onEndRawDrawing(boolean b, TouchPoint touchPoint) {
			drawPathToCanvas(drawPaths.getLast());
			this.debounceRedraw(DEBOUNCE_REDRAW_DELAY_MS);
			debounceSave(DEBOUNCE_SAVE_DELAY_MS);
		}

		@Override
		public void onRawDrawingTouchPointMoveReceived(TouchPoint touchPoint) {
			PointF lastPoint = drawPathsLocal.getLast()
					.get(drawPathsLocal.getLast().size() - 1);
			PointF newPoint = new PointF(touchPoint.x - drawOffset.x,
					touchPoint.y - drawOffset.y);
			PointF pointDelta = new PointF(newPoint.x - lastPoint.x,
					newPoint.y - lastPoint.y);
			if (drawPathsLocal.getLast().size() > 1) {
				if (Math.sqrt(pointDelta.x * pointDelta.x
						+ pointDelta.y * pointDelta.y) < DRAW_MOVE_EPSILON) {
					return;
				}
			}
			drawPaths.getLast()
					.lineTo(newPoint.x, newPoint.y);
			drawPathsLocal.getLast().add(newPoint);
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
			int initialSize = drawPaths.size();
			ListIterator<Path> iterator = drawPaths.listIterator();
			ListIterator<ArrayList<PointF>> iteratorLocal = drawPathsLocal
					.listIterator();
			paint.getFillPath(erasePath, erasePath);
			while (iterator.hasNext()) {
				Path fillPath = new Path();
				paint.getFillPath(iterator.next(), fillPath);
				fillPath.op(erasePath, Path.Op.INTERSECT);
				iteratorLocal.next();
				if (!fillPath.isEmpty()) {
					iterator.remove();
					iteratorLocal.remove();
				}
			}
			Log.d(Xena.TAG, "DrawActivity::onEndRawErasing: removed "
					+ (initialSize - drawPaths.size()) + " paths.");
			drawAllPathsToCanvas();
			this.debounceRedraw(DEBOUNCE_REDRAW_ERASE_DELAY_MS);
			debounceSave(DEBOUNCE_SAVE_DELAY_MS);
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
					Log.d(Xena.TAG,
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

			Log.d(Xena.TAG, "DrawActivity::surfaceHolderCallback: width = " + width
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
		this.loadSvgFromFileData();

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
		this.debounceSave(0);
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
		for (Path path : drawPaths) {
			drawPathToCanvas(path);
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

	private void loadSvgFromFileData() {
		try {
			InputStream in = getContentResolver().openInputStream(DrawActivity.uri);
			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(in, null);
				parser.nextTag();
				parser.require(XmlPullParser.START_TAG, null, "svg");
				parser.nextTag();
				parser.require(XmlPullParser.START_TAG, null, "style");
				parser.next();
				parser.getText();
				parser.nextTag();
				parser.require(XmlPullParser.END_TAG, null, "style");
				while (parser.nextTag() != XmlPullParser.END_TAG) {
					parser.require(XmlPullParser.START_TAG, null, "path");

					DrawActivity.drawPaths.add(new Path());
					Path path = DrawActivity.drawPaths.getLast();
					DrawActivity.drawPathsLocal.add(new ArrayList<PointF>());
					ArrayList<PointF> pathLocal = DrawActivity.drawPathsLocal.getLast();
					String d = parser.getAttributeValue(null, "d");
					StringBuilder buffer = new StringBuilder();
					PointF pointDelta = new PointF();
					PointF lastPoint = new PointF();

					int i = 1;
					for (; i < d.length() && d.charAt(i) != ' '; i++) {
						buffer.append(d.charAt(i));
					}
					lastPoint.x = Integer.parseInt(buffer.toString())
							/ DRAW_SVG_SCALE_FACTOR;
					buffer.setLength(0);
					for (i++; i < d.length() && d.charAt(i) != 'l'; i++) {
						buffer.append(d.charAt(i));
					}
					lastPoint.y = Integer.parseInt(buffer.toString())
							/ DRAW_SVG_SCALE_FACTOR;
					buffer.setLength(0);
					path.moveTo(lastPoint.x, lastPoint.y);
					pathLocal.add(new PointF(lastPoint));

					for (i++; i < d.length();) {
						if (d.charAt(i) == ' ') {
							i++;
						}
						buffer.append(d.charAt(i));
						for (i++; i < d.length() && d.charAt(i) != ' '
								&& d.charAt(i) != '-'; i++) {
							buffer.append(d.charAt(i));
						}
						pointDelta.x = Integer.parseInt(buffer.toString())
								/ DRAW_SVG_SCALE_FACTOR;
						buffer.setLength(0);
						if (d.charAt(i) == ' ') {
							i++;
						}
						buffer.append(d.charAt(i));
						for (i++; i < d.length() && d.charAt(i) != ' '
								&& d.charAt(i) != '-'; i++) {
							buffer.append(d.charAt(i));
						}
						pointDelta.y = Integer.parseInt(buffer.toString())
								/ DRAW_SVG_SCALE_FACTOR;
						buffer.setLength(0);
						lastPoint.x += pointDelta.x;
						lastPoint.y += pointDelta.y;
						path.lineTo(lastPoint.x, lastPoint.y);
						pathLocal.add(new PointF(lastPoint));
					}

					parser.nextTag();
					parser.require(XmlPullParser.END_TAG, null, "path");
				}
				parser.require(XmlPullParser.END_TAG, null, "svg");
				Log.d(Xena.TAG, "DrawActivity::loadSvgFromFileData: Parsed "
						+ DrawActivity.drawPaths.size() + " paths.");
			} catch (IOException e) {
				Log.e(Xena.TAG,
						"DrawActivity::loadSvgFromFileData: Failed to read from file: "
								+ e.toString() + ".");
			} catch (XmlPullParserException e) {
				Log.e(Xena.TAG,
						"DrawActivity::loadSvgFromFileData: Failed to parse file: "
								+ e.toString() + ".");
			} finally {
				in.close();
			}
		} catch (IOException e) {
			Log.e(Xena.TAG,
					"DrawActivity::loadSvgFromFileData: Failed to read from file: "
							+ e.toString() + ".");
		}
	}
}
