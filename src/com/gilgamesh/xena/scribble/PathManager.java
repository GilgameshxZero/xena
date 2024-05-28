package com.gilgamesh.xena.scribble;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.gilgamesh.xena.XenaApplication;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

// Path state and utility functions.
public class PathManager {
	// Size of one chunk is one viewport, always.
	private final Point CHUNK_SIZE;

	// Compound paths are given sequential unique IDs.
	private int nextPathId = 0;

	// Maps ID -> Compound path.
	private HashMap<Integer, CompoundPath> paths = new HashMap<Integer, CompoundPath>();

	// Maps chunk coordinate -> path IDs intersecting this chunk. Paths may span
	// multiple chunks in a 2x2 chunk area.
	private HashMap<Point, HashSet<Integer>> chunkPathIds = new HashMap<Point, HashSet<Integer>>();

	// Offset of the upper-left point of the viewport.
	private PointF viewportOffset = new PointF(0, 0);

	private Point currentChunk = new Point(0, 0);
	private HashSet<Integer> loadedPathIds = new HashSet<Integer>();

	// Returns the joint set of path IDs for a 3x3 chunk area around the viewport
	// offset.
	private void computeLoadedPathIds() {
		Point chunk = new Point((int) -Math.floor(viewportOffset.x / CHUNK_SIZE.x),
				(int) -Math.floor(viewportOffset.y / CHUNK_SIZE.y));
		this.loadedPathIds = new HashSet<Integer>();

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				HashSet<Integer> chunkPathIds = this.chunkPathIds
						.get(new Point(chunk.x + i, chunk.y + j));
				if (chunkPathIds != null) {
					this.loadedPathIds.addAll(chunkPathIds);
				}
			}
		}

		Log.v(XenaApplication.TAG,
				"PathManager::computeLoadedPathIds: Found " + this.loadedPathIds.size()
						+ " paths in chunks surrounding " + chunk.x + ", " + chunk.y + ".");
	}

	public PathManager(Point chunkSize) {
		this.CHUNK_SIZE = chunkSize;
	}

	public AbstractMap.SimpleEntry<Integer, CompoundPath> addPath(PointF point) {
		CompoundPath path = new CompoundPath(point,
				new CompoundPath.Callback() {
					private final int pathId = nextPathId;

					@Override
					public void onPointAdded(PointF point) {
						// Update the chunkPathIds map.
						Point chunk = new Point((int) Math.floor(point.x / CHUNK_SIZE.x),
								(int) Math.floor(point.y / CHUNK_SIZE.y));
						HashSet<Integer> pathIds = chunkPathIds.get(chunk);
						if (pathIds != null) {
							if (!pathIds.contains(this.pathId)) {
								pathIds.add(this.pathId);
								Log.v(XenaApplication.TAG,
										"Added path " + this.pathId + " to chunk " + chunk.x + ", "
												+ chunk.y + ".");
							}
						} else {
							pathIds = new HashSet<Integer>();
							pathIds.add(this.pathId);
							chunkPathIds.put(chunk, pathIds);
							Log.v(XenaApplication.TAG,
									"Added path " + this.pathId + " to new chunk " + chunk.x
											+ ", " + chunk.y + ".");
						}

						// Load this path if it is in loaded chunks.
						if (Math.abs(chunk.x - currentChunk.x) <= 1
								&& Math.abs(chunk.y - currentChunk.y) <= 1) {
							loadedPathIds.add(this.pathId);
						}
					}
				});
		this.paths.put(nextPathId, path);
		return new AbstractMap.SimpleEntry<Integer, CompoundPath>(nextPathId++,
				path);
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
	public void prepareEntryForRemove(Map.Entry<Integer, CompoundPath> entry) {
		this.loadedPathIds.remove(entry.getKey());
		CompoundPath path = entry.getValue();
		Point[] chunks = new Point[] {
				new Point((int) Math.floor(path.bounds.left / CHUNK_SIZE.x),
						(int) Math.floor(path.bounds.top / CHUNK_SIZE.y)),
				new Point((int) Math.floor(path.bounds.right / CHUNK_SIZE.x),
						(int) Math.floor(path.bounds.top / CHUNK_SIZE.y)),
				new Point((int) Math.floor(path.bounds.right / CHUNK_SIZE.x),
						(int) Math.floor(path.bounds.bottom / CHUNK_SIZE.y)),
				new Point((int) Math.floor(path.bounds.left / CHUNK_SIZE.x),
						(int) Math.floor(path.bounds.bottom / CHUNK_SIZE.y)) };
		for (Point chunk : chunks) {
			HashSet<Integer> pathIds = chunkPathIds.get(chunk);
			if (pathIds != null && pathIds.contains(entry.getKey())) {
				pathIds.remove(entry.getKey());
				Log.v(XenaApplication.TAG,
						"PathManager::prepareEntryForRemove: Removed path " + entry.getKey()
								+ " from chunk " + chunk.x + ", " + chunk.y + ".");
			}
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
			this.computeLoadedPathIds();
			this.currentChunk = newChunk;
		}
	}

	public HashSet<Integer> getLoadedPathIds() {
		return this.loadedPathIds;
	}
}
