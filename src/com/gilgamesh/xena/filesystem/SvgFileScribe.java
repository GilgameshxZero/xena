package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.XenaApplication;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

public class SvgFileScribe {
	static final public float COORDINATE_SCALE_FACTOR = 8;

	static private void coverWithRectF(RectF rectangle, PointF point) {
		rectangle.left = Math.min(rectangle.left, point.x);
		rectangle.top = Math.min(rectangle.top, point.y);
		rectangle.right = Math.max(rectangle.right, point.x);
		rectangle.bottom = Math.max(rectangle.bottom, point.y);
	}

	// Parameter viewportOffset is updated.
	static public LinkedList<ArrayList<PointF>> loadPathsFromSvg(Context context,
			Uri uri, PointF viewportOffset) {
		LinkedList<ArrayList<PointF>> paths = new LinkedList<ArrayList<PointF>>();
		try {
			InputStream in = context.getContentResolver().openInputStream(uri);
			try {
				XmlPullParser parser = Xml.newPullParser();
				parser.setInput(in, null);
				while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
					parser.next();

					if (parser.getEventType() != XmlPullParser.START_TAG) {
						continue;
					}

					if (parser.getName().equals("svg")) {
						// Viewport is not scaled.
						String[] viewport = parser.getAttributeValue(null, "data-xena")
								.split(" ");
						viewportOffset.x = Integer.parseInt(viewport[0]);
						viewportOffset.y = Integer.parseInt(viewport[1]);
					}

					if (!parser.getName().equals("path")) {
						continue;
					}

					// Only parse paths from the SVG file. Other elements are thrown away.
					paths.add(new ArrayList<PointF>());
					ArrayList<PointF> path = paths.getLast();
					String d = parser.getAttributeValue(null, "d");
					StringBuilder buffer = new StringBuilder();
					PointF pointDelta = new PointF();
					PointF lastPoint = new PointF();

					int i = 1;
					for (; i < d.length() && d.charAt(i) != ' '; i++) {
						buffer.append(d.charAt(i));
					}
					lastPoint.x = Integer.parseInt(buffer.toString())
							/ SvgFileScribe.COORDINATE_SCALE_FACTOR;
					buffer.setLength(0);
					for (i++; i < d.length() && d.charAt(i) != 'l'; i++) {
						buffer.append(d.charAt(i));
					}
					lastPoint.y = Integer.parseInt(buffer.toString())
							/ SvgFileScribe.COORDINATE_SCALE_FACTOR;
					buffer.setLength(0);
					path.add(new PointF(lastPoint));

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
								/ SvgFileScribe.COORDINATE_SCALE_FACTOR;
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
								/ SvgFileScribe.COORDINATE_SCALE_FACTOR;
						buffer.setLength(0);
						lastPoint.x += pointDelta.x;
						lastPoint.y += pointDelta.y;
						path.add(new PointF(lastPoint));
					}
				}
				Log.v(XenaApplication.TAG, "SvgFileScribe::loadPathsFromSvg: Parsed "
						+ paths.size() + " paths.");
			} catch (IOException e) {
				Log.e(XenaApplication.TAG,
						"SvgFileScribe::loadPathsFromSvg: Failed to read file: "
								+ e.toString() + ".");
			} catch (XmlPullParserException e) {
				Log.e(XenaApplication.TAG,
						"SvgFileScribe::loadPathsFromSvg: Failed to parse file: "
								+ e.toString() + ".");
			} finally {
				in.close();
			}
		} catch (IOException e) {
			Log.e(XenaApplication.TAG,
					"SvgFileScribe::loadPathsFromSvg: Failed to open file: "
							+ e.toString() + ".");
		}
		return paths;
	}

	private TimerTask debounceSaveTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	private void debounceSaveTaskRun(Context context, Uri uri,
			LinkedList<ArrayList<PointF>> paths, PointF viewportOffset,
			final int STROKE_WIDTH) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
					context.getContentResolver().openOutputStream(uri, "wt"));
			try {
				RectF containerBox = new RectF(Float.POSITIVE_INFINITY,
						Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
						Float.NEGATIVE_INFINITY);
				StringBuilder stringBuilder = new StringBuilder();
				for (ArrayList<PointF> path : paths) {
					stringBuilder.append(
							"<path d=\"");
					Iterator<PointF> pathIterator = path.iterator();
					PointF point = pathIterator.next();
					stringBuilder
							.append(
									"M" + Math
											.round(point.x * SvgFileScribe.COORDINATE_SCALE_FACTOR)
											+ " "
											+ Math.round(
													point.y * SvgFileScribe.COORDINATE_SCALE_FACTOR)
											+ "l");
					SvgFileScribe.coverWithRectF(containerBox, point);

					PointF nextPoint, pointDelta = new PointF(0, 0),
							roundError = new PointF(0, 0);
					String[] pointDeltaS = new String[2];
					while (pathIterator.hasNext()) {
						nextPoint = pathIterator.next();

						// Minimize ` -`.
						pointDelta.x = nextPoint.x * SvgFileScribe.COORDINATE_SCALE_FACTOR
								- point.x * SvgFileScribe.COORDINATE_SCALE_FACTOR
								+ roundError.x;
						pointDelta.y = nextPoint.y * SvgFileScribe.COORDINATE_SCALE_FACTOR
								- point.y * SvgFileScribe.COORDINATE_SCALE_FACTOR
								+ roundError.y;
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
						SvgFileScribe.coverWithRectF(containerBox, point);
					}
					stringBuilder.append("\"/>\n");
				}

				containerBox.left -= STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.top -= STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.right += STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.bottom += STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				outputStreamWriter.write(
						"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\""
								+ Math.round(
										containerBox.left * SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ " "
								+ Math.round(
										containerBox.top * SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ " "
								+ Math.round((containerBox.right - containerBox.left)
										* SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ " "
								+ Math.round((containerBox.bottom - containerBox.top)
										* SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ "\" stroke=\"black\" stroke-width=\""
								+ Math
										.round(STROKE_WIDTH * SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ "\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\" data-xena=\""
								+ Math.round(viewportOffset.x)
								+ ' '
								+ Math.round(viewportOffset.y)
								+ "\">"
								+ "<style>@media(prefers-color-scheme:dark){svg{background-color:black;stroke:white;}}</style>\n");
				outputStreamWriter.write(stringBuilder.toString());
				outputStreamWriter.write(
						"</svg>\n");
				Log.v(XenaApplication.TAG,
						"SvgFileScribe::debounceSaveTaskRun: Saved to "
								+ uri.toString() + ".");
			} catch (IOException e) {
				Log.e(XenaApplication.TAG,
						"SvgFileScribe::debounceSaveTaskRun: Failed to write to file: "
								+ e.toString() + ".");
			} finally {
				outputStreamWriter.close();
			}
		} catch (IOException e) {
			Log.e(XenaApplication.TAG,
					"SvgFileScribe::debounceSaveTaskRun: Failed to write to file: "
							+ e.toString() + ".");
		}
	}

	public void debounceSave(Context context, Uri uri,
			LinkedList<ArrayList<PointF>> paths, PointF drawOffset,
			final int STROKE_WIDTH,
			int delayMs) {
		this.debounceSaveTask.cancel();
		this.debounceSaveTask = new TimerTask() {
			@Override
			public void run() {
				debounceSaveTaskRun(context, uri, paths, drawOffset, STROKE_WIDTH);
			}
		};
		new Timer().schedule(this.debounceSaveTask, delayMs);
	}

	// Default delayMs.
	public void debounceSave(Context context, Uri uri,
			LinkedList<ArrayList<PointF>> paths, PointF drawOffset,
			final int STROKE_WIDTH) {
		this.debounceSave(context, uri, paths, drawOffset, STROKE_WIDTH, 15000);
	}
}
