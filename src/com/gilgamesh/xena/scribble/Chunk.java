package com.gilgamesh.xena.scribble;

import java.util.HashSet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

// Low-level store for a bitmap and operations onto the bitmap.
// Chunk stores some path IDs which are in the chunk, as well as a bitmap rendering of all paths in the chunk. The chunk also stores the offset of its top-left corner in the viewport to enable rendering.
// Each bitmap/chunk requires around 11MB of memory for 1872x1404 space.
public class Chunk {
	static public final Paint PAINT;
	static {
		PAINT = new Paint();
		PAINT.setAntiAlias(true);
		PAINT.setColor(Color.BLACK);
		PAINT.setStyle(Paint.Style.STROKE);
		PAINT.setStrokeJoin(Paint.Join.ROUND);
		PAINT.setStrokeCap(Paint.Cap.ROUND);
		PAINT.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX);
	}
	static private final Paint PAINT_ERASE;
	static {
		PAINT_ERASE = new Paint();
		PAINT_ERASE.setAntiAlias(true);
		PAINT_ERASE.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
		PAINT_ERASE.setColor(Color.TRANSPARENT);
		PAINT_ERASE.setStyle(Paint.Style.STROKE);
		PAINT_ERASE.setStrokeJoin(Paint.Join.ROUND);
		PAINT_ERASE.setStrokeCap(Paint.Cap.ROUND);
		PAINT_ERASE.setStrokeWidth(ScribbleActivity.STROKE_WIDTH_PX * 1.5f);
	}

	public final int OFFSET_X;
	public final int OFFSET_Y;

	// Unloadable/reloadable resources of this chunk for rendering. TODO: unload
	// at some point.
	private Bitmap bitmap;
	private Canvas canvas;

	// Path IDs in the chunk.
	private HashSet<Integer> pathIds = new HashSet<Integer>();

	// Used to query paths by ID for rendering.
	private PathManager pathManager;

	public Chunk(PathManager pathManager, int width, int height, int offsetX,
			int offsetY) {
		this.pathManager = pathManager;
		this.OFFSET_X = offsetX;
		this.OFFSET_Y = offsetY;
		this.bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		this.canvas = new Canvas(this.bitmap);
		this.canvas.drawRect(0, 0, width, height, Chunk.PAINT_ERASE);
	}

	// Adding a path will render it onto the bitmap. A path may be added again to
	// re-render it.
	public void addPath(CompoundPath path) {
		this.pathIds.add(path.ID);
		Path offsetPath = new Path(path.path);
		offsetPath.offset(-this.OFFSET_X, -this.OFFSET_Y);
		canvas.drawPath(offsetPath, Chunk.PAINT);
	}

	// Adds path but does not draw it.
	public void addPath(int pathId) {
		this.pathIds.add(pathId);
	}

	// Removing a path will de-render it from the bitmap.
	public void removePath(CompoundPath path) {
		this.pathIds.remove(path.ID);
		Path offsetPath = new Path(path.path);
		offsetPath.offset(-this.OFFSET_X, -this.OFFSET_Y);
		canvas.drawPath(offsetPath, Chunk.PAINT_ERASE);

		// Paths whose bounding box intersects this one will be redrawn.
		for (int pathId : this.pathIds) {
			CompoundPath pathToRedraw = this.pathManager.getPath(pathId);
			if (RectF.intersects(pathToRedraw.bounds, path.bounds)) {
				this.addPath(pathToRedraw);
			}
		}
	}

	public Bitmap getBitmap() {
		return this.bitmap;
	}

	public HashSet<Integer> getPathIds() {
		return this.pathIds;
	}
}
