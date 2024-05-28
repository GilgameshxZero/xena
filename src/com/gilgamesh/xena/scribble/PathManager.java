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
	public PointF viewportOffset = new PointF();

	public PathManager(Point chunkSize) {
		this.CHUNK_SIZE = chunkSize;
	}

	public AbstractMap.SimpleEntry<Integer, CompoundPath> addPath(PointF point) {
		CompoundPath path = new CompoundPath(point,
				new CompoundPath.PointAddedCallback() {
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

	// Returns the joint set of path IDs for a 3x3 chunk area around the viewport
	// offset.
	public HashSet<Integer> getChunkPathIds() {
		Point chunk = new Point((int) Math.floor(viewportOffset.x / CHUNK_SIZE.x),
				(int) Math.floor(viewportOffset.y / CHUNK_SIZE.y));
		HashSet<Integer> pathIds = this.chunkPathIds.get(chunk);
		if (pathIds == null) {
			pathIds = new HashSet<Integer>();
		}

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				HashSet<Integer> chunkPathIds = this.chunkPathIds
						.get(new Point(chunk.x + i, chunk.y + j));
				if (chunkPathIds != null) {
					pathIds.addAll(chunkPathIds);
				}
			}
		}

		Log.v(XenaApplication.TAG,
				"PathManager::getChunkPathIds: Found " + pathIds.size()
						+ " paths in chunks surrounding " + chunk.x + ", " + chunk.y + ".");
		return pathIds;
	}
}
