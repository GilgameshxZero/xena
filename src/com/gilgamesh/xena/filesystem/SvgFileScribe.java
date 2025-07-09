package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.multithreading.DebouncedTask;
import com.gilgamesh.xena.scribble.CompoundPath;
import com.gilgamesh.xena.scribble.PathManager;
import com.gilgamesh.xena.scribble.ScribbleActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.nio.file.Paths;
import java.nio.file.Files;

public class SvgFileScribe {
	static public abstract class Callback {
		public abstract void onDebounceSaveUpdate(boolean isSaved);
	}

	static public final float COORDINATE_SCALE_FACTOR
		= 12f / XenaApplication.DPI * 160f;
	static public final int DEBOUNCE_SAVE_MS = 64000;

	private boolean isSaved = true;
	private Callback callback;

	private ScribbleActivity scribbleActivity;
	private Uri uri;
	private PathManager pathManager;

	public final DebouncedTask saveTask
		= new DebouncedTask(new DebouncedTask.Callback() {
			@Override
			public void onRun() {
				save();
				callback.onDebounceSaveUpdate(isSaved);
			}

			@Override
			public void onDebounce() {
				isSaved = false;
				callback.onDebounceSaveUpdate(isSaved);
			}
		});

	public SvgFileScribe(Callback callback, ScribbleActivity scribbleActivity,
		Uri uri, PathManager pathManager) {
		this.callback = callback;
		this.scribbleActivity = scribbleActivity;
		this.uri = uri;
		this.pathManager = pathManager;
	}

	public void load() {
		try {
			InputStream in
				= this.scribbleActivity.getContentResolver().openInputStream(this.uri);
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
						String[] data
							= parser.getAttributeValue(null, "data-xena").split(" ");

						if (data.length > 2) {
							this.pathManager.setZoomStepId(Integer.parseInt(data[2]));
						}

						this.pathManager.setViewportOffset(
							new PointF(Integer.parseInt(data[0]) * XenaApplication.DPI,
								Integer.parseInt(data[1]) * XenaApplication.DPI));
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
					lastPoint.x
						= Integer.parseInt(buffer.toString())
							/ SvgFileScribe.COORDINATE_SCALE_FACTOR;
					buffer.setLength(0);
					for (i++; i < d.length() && d.charAt(i) != 'l'; i++) {
						buffer.append(d.charAt(i));
					}
					lastPoint.y
						= Integer.parseInt(buffer.toString())
							/ SvgFileScribe.COORDINATE_SCALE_FACTOR;
					buffer.setLength(0);
					CompoundPath path = this.pathManager.addPath(lastPoint).getValue();

					for (i++; i < d.length();) {
						if (d.charAt(i) == ' ') {
							i++;
						}
						buffer.append(d.charAt(i));
						for (i++; i < d.length() && d.charAt(i) != ' '
							&& d.charAt(i) != '-'; i++) {
							buffer.append(d.charAt(i));
						}
						pointDelta.x
							= Integer.parseInt(buffer.toString())
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
						pointDelta.y
							= Integer.parseInt(buffer.toString())
								/ SvgFileScribe.COORDINATE_SCALE_FACTOR;
						buffer.setLength(0);
						lastPoint.x += pointDelta.x;
						lastPoint.y += pointDelta.y;
						path.addPoint(lastPoint);
					}

					this.pathManager.finalizePath(path);
				}
				XenaApplication.log("SvgFileScribe::loadPathsFromSvg: Parsed "
					+ this.pathManager.getPathsCount() + " paths.");
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
				"SvgFileScribe::loadPathsFromSvg: Failed to open file: " + e.toString()
					+ ".");
		}
	}

	private void save() {
		try {
			Files.createDirectories(Paths.get(this.uri.getPath()).getParent());
			OutputStreamWriter outputStreamWriter
				= new OutputStreamWriter(this.scribbleActivity.getContentResolver()
					.openOutputStream(this.uri, "wt"));
			try {
				RectF containerBox
					= new RectF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
						Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
				StringBuilder stringBuilder = new StringBuilder();
				Iterator<Map.Entry<Integer, CompoundPath>> iterator
					= this.pathManager.getPathsIterator();
				while (iterator.hasNext()) {
					stringBuilder.append("<path d=\"");
					Iterator<PointF> pointsIterator
						= iterator.next().getValue().points.iterator();
					PointF point = pointsIterator.next();
					stringBuilder.append("M"
						+ Math.round(point.x * SvgFileScribe.COORDINATE_SCALE_FACTOR) + " "
						+ Math.round(point.y * SvgFileScribe.COORDINATE_SCALE_FACTOR)
						+ "l");
					containerBox.union(point.x, point.y);

					PointF nextPoint, pointDelta = new PointF(0, 0),
						roundError = new PointF(0, 0);
					String[] pointDeltaS = new String[2];
					while (pointsIterator.hasNext()) {
						nextPoint = pointsIterator.next();

						// Minimize ` -`.
						pointDelta.x
							= nextPoint.x * SvgFileScribe.COORDINATE_SCALE_FACTOR
								- point.x * SvgFileScribe.COORDINATE_SCALE_FACTOR
								+ roundError.x;
						pointDelta.y
							= nextPoint.y * SvgFileScribe.COORDINATE_SCALE_FACTOR
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

				containerBox.left
					-= ScribbleActivity.STROKE_WIDTH_DP
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.top
					-= ScribbleActivity.STROKE_WIDTH_DP
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.right
					+= ScribbleActivity.STROKE_WIDTH_DP
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				containerBox.bottom
					+= ScribbleActivity.STROKE_WIDTH_DP
						* SvgFileScribe.COORDINATE_SCALE_FACTOR;
				outputStreamWriter
					.write("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\""
						+ Math.round(Math
							.floor(containerBox.left * SvgFileScribe.COORDINATE_SCALE_FACTOR))
						+ " "
						+ Math.round(Math
							.floor(containerBox.top * SvgFileScribe.COORDINATE_SCALE_FACTOR))
						+ " "
						+ Math.round(Math.ceil((containerBox.right - containerBox.left))
							* SvgFileScribe.COORDINATE_SCALE_FACTOR)
						+ " "
						+ Math.round(Math.ceil((containerBox.bottom - containerBox.top))
							* SvgFileScribe.COORDINATE_SCALE_FACTOR)
						+ "\" stroke=\"black\" stroke-width=\""
						+ Math.round(ScribbleActivity.STROKE_WIDTH_DP
							* SvgFileScribe.COORDINATE_SCALE_FACTOR)
						+ "\" stroke-linecap=\"round\" stroke-linejoin=\"round\" fill=\"none\" data-xena=\""
						+ Math.round(
							this.pathManager.getViewportOffset().x / XenaApplication.DPI)
						+ ' '
						+ Math.round(
							this.pathManager.getViewportOffset().y / XenaApplication.DPI)
						+ ' ' + this.pathManager.getZoomStepId() + "\">"
						+ "<style>@media(prefers-color-scheme:dark){svg{background-color:black;stroke:white;}}</style>\n");
				outputStreamWriter.write(stringBuilder.toString());
				outputStreamWriter.write("</svg>\n");

				XenaApplication
					.log("SvgFileScribe::save: Saved to " + uri.toString() + ".");
				this.isSaved = true;
			} catch (IOException e) {
				Log.e(XenaApplication.TAG,
					"SvgFileScribe::save: Failed to write to file: " + e.toString()
						+ ".");
			} finally {
				outputStreamWriter.close();
			}
		} catch (IOException e) {
			Log.e(XenaApplication.TAG,
				"SvgFileScribe::save: Failed to write to file: " + e.toString() + ".");
		}
	}

	// Returns true iff no save task is not pending or failed.
	public boolean isSaved() {
		return this.isSaved;
	}
}
