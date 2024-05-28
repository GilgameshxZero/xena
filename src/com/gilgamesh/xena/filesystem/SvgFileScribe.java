package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.scribble.Chunk;
import com.gilgamesh.xena.scribble.CompoundPath;
import com.gilgamesh.xena.scribble.PathManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
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

	static public void loadPathsFromSvg(Context context,
			Uri uri, PathManager pathManager) {
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
						pathManager.setViewportOffset(new PointF(
								Integer.parseInt(viewport[0]), Integer.parseInt(viewport[1])));
					}

					if (!parser.getName().equals("path")) {
						continue;
					}

					// Only parse paths from the SVG file. Other elements are thrown away.
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
					CompoundPath path = pathManager.addPath(lastPoint).getValue();

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
						path.addPoint(lastPoint);
					}

					pathManager.finalizePath(path);
				}
				Log.v(XenaApplication.TAG, "SvgFileScribe::loadPathsFromSvg: Parsed "
						+ pathManager.getPathsCount() + " paths.");
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
	}

	private TimerTask debounceSaveTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	private void debounceSaveTaskRun(Context context, Uri uri,
			PathManager pathManager) {
		try {
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
					context.getContentResolver().openOutputStream(uri, "wt"));
			try {
				RectF containerBox = new RectF(Float.POSITIVE_INFINITY,
						Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
						Float.NEGATIVE_INFINITY);
				StringBuilder stringBuilder = new StringBuilder();
				Iterator<Map.Entry<Integer, CompoundPath>> iterator = pathManager
						.getPathsIterator();
				while (iterator.hasNext()) {
					stringBuilder.append(
							"<path d=\"");
					Iterator<PointF> pointsIterator = iterator.next().getValue().points
							.iterator();
					PointF point = pointsIterator.next();
					stringBuilder
							.append(
									"M" + Math
											.round(point.x * SvgFileScribe.COORDINATE_SCALE_FACTOR)
											+ " "
											+ Math.round(
													point.y * SvgFileScribe.COORDINATE_SCALE_FACTOR)
											+ "l");
					containerBox.union(point.x, point.y);

					PointF nextPoint, pointDelta = new PointF(0, 0),
							roundError = new PointF(0, 0);
					String[] pointDeltaS = new String[2];
					while (pointsIterator.hasNext()) {
						nextPoint = pointsIterator.next();

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
						containerBox.union(point.x, point.y);
					}
					stringBuilder.append("\"/>\n");
				}

				containerBox.left -= Chunk.STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.top -= Chunk.STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.right += Chunk.STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.bottom += Chunk.STROKE_WIDTH
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				outputStreamWriter.write(
						"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\""
								+ Math.round(Math.floor(
										containerBox.left * SvgFileScribe.COORDINATE_SCALE_FACTOR))
								+ " "
								+ Math.round(Math.floor(
										containerBox.top * SvgFileScribe.COORDINATE_SCALE_FACTOR))
								+ " "
								+ Math.round(Math.ceil((containerBox.right - containerBox.left))
										* SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ " "
								+ Math.round(Math.ceil((containerBox.bottom - containerBox.top))
										* SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ "\" stroke=\"black\" stroke-width=\""
								+ Math
										.round(Chunk.STROKE_WIDTH
												* SvgFileScribe.COORDINATE_SCALE_FACTOR)
								+ "\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\" data-xena=\""
								+ Math.round(pathManager.getViewportOffset().x)
								+ ' '
								+ Math.round(pathManager.getViewportOffset().y)
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
			PathManager pathManager,
			int delayMs) {
		this.debounceSaveTask.cancel();
		this.debounceSaveTask = new TimerTask() {
			@Override
			public void run() {
				debounceSaveTaskRun(context, uri, pathManager);
			}
		};
		new Timer().schedule(this.debounceSaveTask, delayMs);
	}

	// Default delayMs.
	public void debounceSave(Context context, Uri uri,
			PathManager pathManager) {
		this.debounceSave(context, uri, pathManager, 60000);
	}
}
