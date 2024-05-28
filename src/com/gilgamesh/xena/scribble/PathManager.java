package com.gilgamesh.xena.scribble;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import com.gilgamesh.xena.XenaApplication;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

// Path state and utility functions.
public class PathManager {
	private final PathManager that = this;

	// Size of one chunk is one viewport, always.
	private final Point CHUNK_SIZE;

	// Maps ID -> Compound path.
	private HashMap<Integer, CompoundPath> paths = new HashMap<Integer, CompoundPath>();

	// Maps chunk coordinate -> path IDs intersecting this chunk. Paths may span
	// multiple chunks in a (larger than, if created on another device) 2x2 chunk
	// area.
	private HashMap<Point, Chunk> chunks = new HashMap<Point, Chunk>();

	// Offset of the upper-left point of the viewport.
	private PointF viewportOffset = new PointF(0, 0);

	private Point currentChunk = new Point(0, 0);

	public PathManager(Point chunkSize) {
		this.CHUNK_SIZE = chunkSize;
	}

	public AbstractMap.SimpleEntry<Integer, CompoundPath> addPath(PointF point) {
		CompoundPath path = new CompoundPath(point,
				new CompoundPath.Callback() {
					@Override
					public void onPointAdded(CompoundPath path, PointF previousPoint,
							PointF currentPoint) {
						// Update/create new chunks and render the new path segment onto the
						// chunk. Use containingChunks to remember that this path spans this
						// chunk.
						Point chunkCoordinate = new Point(
								(int) Math.floor(currentPoint.x / CHUNK_SIZE.x),
								(int) Math.floor(currentPoint.y / CHUNK_SIZE.y));
						Chunk chunk = chunks.get(chunkCoordinate);
						if (chunk != null) {
							if (!chunk.getPathIds().contains(path.ID)) {
								chunk.addPath(path.ID);
								path.containingChunks.add(chunkCoordinate);
								Log.v(XenaApplication.TAG,
										"Added path " + path.ID + " to chunk "
												+ chunkCoordinate.x + ", "
												+ chunkCoordinate.y + ".");
							}
						} else {
							chunk = new Chunk(that, CHUNK_SIZE.x,
									CHUNK_SIZE.y, chunkCoordinate.x * CHUNK_SIZE.x,
									chunkCoordinate.y * CHUNK_SIZE.y);
							chunk.addPath(path.ID);
							chunks.put(chunkCoordinate, chunk);
							path.containingChunks.add(chunkCoordinate);
							Log.v(XenaApplication.TAG,
									"Added path " + path.ID + " to new chunk "
											+ chunkCoordinate.x
											+ ", " + chunkCoordinate.y + ".");
						}
					}
				});
		this.paths.put(path.ID, path);
		return new AbstractMap.SimpleEntry<Integer, CompoundPath>(path.ID, path);
	}

	public CompoundPath getPath(int id) {
		return this.paths.get(id);
	}

	public int getPathsCount() {
		return this.paths.size();
	}

	public Iterator<Map.Entry<Integer, CompoundPath>> getPathsIterator() {
		return this.paths.entrySet().iterator();
	}

	// MUST call this function to remove a path iterator. This will remove the
	// path from chunk loading as well.
	public void removePathId(int pathId) {
		CompoundPath path = this.paths.get(pathId);
		for (int chunkCoordinateX = (int) Math
				.floor(path.bounds.left / CHUNK_SIZE.x); chunkCoordinateX <= (int) Math
						.floor(path.bounds.right / CHUNK_SIZE.y); chunkCoordinateX++) {
			for (int chunkCoordinateY = (int) Math
					.floor(
							path.bounds.top / CHUNK_SIZE.x); chunkCoordinateY <= (int) Math
									.floor(
											path.bounds.bottom / CHUNK_SIZE.y); chunkCoordinateY++) {
				Chunk chunk = chunks
						.get(new Point(chunkCoordinateX, chunkCoordinateY));
				if (chunk != null && chunk.getPathIds().contains(pathId)) {
					chunk.removePath(path);
					Log.v(XenaApplication.TAG,
							"PathManager::prepareEntryForRemove: Removed path "
									+ pathId
									+ " from chunk " + chunkCoordinateX + ", " + chunkCoordinateY
									+ ".");
				}
			}
		}

		this.paths.remove(pathId);
	}

	// Renders the complete path onto all relevant chunk bitmaps.
	public void finalizePath(CompoundPath path) {
		for (Point chunkCoordinate : path.containingChunks) {
			Chunk chunk = chunks.get(chunkCoordinate);
			chunk.addPath(path);
		}
	}

	public PointF getViewportOffset() {
		return this.viewportOffset;
	}

	public void setViewportOffset(PointF offset) {
		this.viewportOffset = offset;

		// Load chunks.
		Point newChunk = new Point((int) -Math.floor(offset.x / CHUNK_SIZE.x),
				(int) -Math.floor(offset.y / CHUNK_SIZE.y));
		if (!newChunk.equals(this.currentChunk)) {
			Log.v(XenaApplication.TAG,
					"PathManager::setViewportOffset: Moved into new chunk "
							+ newChunk.x + ", " + newChunk.y + ".");
			this.currentChunk = newChunk;
		}
	}

	public Chunk[] getVisibleChunks() {
		Chunk[] visibleChunks = new Chunk[4];
		Point[] chunkCoordinates = new Point[] {
				new Point(this.currentChunk),
				new Point(this.currentChunk.x - 1, this.currentChunk.y),
				new Point(this.currentChunk.x, this.currentChunk.y - 1),
				new Point(this.currentChunk.x - 1, this.currentChunk.y - 1)
		};
		for (int i = 0; i < 4; i++) {
			visibleChunks[i] = chunks.get(chunkCoordinates[i]);
			if (visibleChunks[i] == null) {
				visibleChunks[i] = new Chunk(this, CHUNK_SIZE.x,
						CHUNK_SIZE.y, chunkCoordinates[i].x * CHUNK_SIZE.x,
						chunkCoordinates[i].y * CHUNK_SIZE.y);
				chunks.put(chunkCoordinates[i], visibleChunks[i]);
			}
		}
		return visibleChunks;
	}
}
