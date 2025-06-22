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
	static private final float[] ZOOM_STEPS = new float[] { 0.17f, 0.41f, 0.64f,
			0.8f, 1f, 1.25f, 1.56f, 2.43f, 5.90f };
	static private final int ZOOM_STEP_ID_DEFAULT = 4;
	static private final float CHUNK_SIZE_SCALE = 0.25f;

	private final PathManager that = this;

	// Size of one chunk is one viewport, always.
	public final Point CHUNK_SIZE;

	// Maps ID -> Compound path.
	private HashMap<Integer, CompoundPath> paths = new HashMap<Integer, CompoundPath>();

	// Maps chunk coordinate -> path IDs intersecting this chunk. Paths may span
	// multiple chunks in a (larger than, if created on another device) 2x2 chunk
	// area.
	private HashMap<Point, Chunk> chunks = new HashMap<Point, Chunk>();

	// Offset of the upper-left point of the viewport.
	private PointF viewportOffset = new PointF(0, 0);

	private Point currentChunk = new Point(0, 0);

	private int zoomStepId = 0;

	public PathManager(Point chunkSizePrescale) {
		this.CHUNK_SIZE = new Point(
				(int) Math.ceil(chunkSizePrescale.x * PathManager.CHUNK_SIZE_SCALE),
				(int) Math.ceil(chunkSizePrescale.y * PathManager.CHUNK_SIZE_SCALE));
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
				.floor(path.bounds.left
						/ this.CHUNK_SIZE.x); chunkCoordinateX <= (int) Math
								.floor(path.bounds.right
										/ this.CHUNK_SIZE.x); chunkCoordinateX++) {
			for (int chunkCoordinateY = (int) Math
					.floor(
							path.bounds.top
									/ this.CHUNK_SIZE.y); chunkCoordinateY <= (int) Math
											.floor(
													path.bounds.bottom
															/ this.CHUNK_SIZE.y); chunkCoordinateY++) {
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
		this.updateCurrentChunk();
	}

	private void updateCurrentChunk() {
		Point newChunk = new Point(
				(int) -Math
						.floor(this.viewportOffset.x / this.CHUNK_SIZE.x),
				(int) -Math
						.floor(this.viewportOffset.y / this.CHUNK_SIZE.y));
		if (!newChunk.equals(this.currentChunk)) {
			Log.v(XenaApplication.TAG,
					"PathManager::setViewportOffset: Moved into new chunk "
							+ newChunk.x + ", " + newChunk.y + ".");
			this.currentChunk = newChunk;
		}
	}

	public float getZoomScale() {
		return PathManager.ZOOM_STEPS[this.zoomStepId
				+ PathManager.ZOOM_STEP_ID_DEFAULT];
	}

	public int getZoomStepId() {
		return this.zoomStepId;
	}

	public void zoomIn() {
		this.setZoomStepId(this.zoomStepId + 1);
	}

	public void zoomOut() {
		this.setZoomStepId(this.zoomStepId - 1);
	}

	public void resetZoom() {
		this.setZoomStepId(0);
	}

	public void setZoomStepId(int newZoomStepId) {
		// Clamp.
		newZoomStepId = Math.min(
				PathManager.ZOOM_STEPS.length - PathManager.ZOOM_STEP_ID_DEFAULT - 1,
				Math.max(-PathManager.ZOOM_STEP_ID_DEFAULT, newZoomStepId));

		// Also update the viewport offset so that scale functions according to the
		// center of the screen.
		PointF newViewportOffset = new PointF(
				this.viewportOffset.x
						- this.CHUNK_SIZE.x / PathManager.ZOOM_STEPS[this.zoomStepId
								+ PathManager.ZOOM_STEP_ID_DEFAULT] / 2
						+ this.CHUNK_SIZE.x / PathManager.ZOOM_STEPS[newZoomStepId
								+ PathManager.ZOOM_STEP_ID_DEFAULT] / 2,
				this.viewportOffset.y
						- this.CHUNK_SIZE.y / PathManager.ZOOM_STEPS[this.zoomStepId
								+ PathManager.ZOOM_STEP_ID_DEFAULT] / 2
						+ this.CHUNK_SIZE.y / PathManager.ZOOM_STEPS[newZoomStepId
								+ PathManager.ZOOM_STEP_ID_DEFAULT] / 2);

		this.zoomStepId = newZoomStepId;
		this.setViewportOffset(newViewportOffset);
	}

	public ArrayList<Chunk> getVisibleChunks() {
		ArrayList<Chunk> visibleChunks = new ArrayList<Chunk>();
		for (int i = -1; i < Math
				.ceil(1 / PathManager.CHUNK_SIZE_SCALE
						/ PathManager.ZOOM_STEPS[this.zoomStepId
								+ PathManager.ZOOM_STEP_ID_DEFAULT]); i++) {
			for (int j = -1; j < Math
					.ceil(1 / PathManager.CHUNK_SIZE_SCALE
							/ PathManager.ZOOM_STEPS[this.zoomStepId
									+ PathManager.ZOOM_STEP_ID_DEFAULT]); j++) {
				Point chunkCoordinate = new Point(this.currentChunk.x + i,
						this.currentChunk.y + j);
				Chunk chunk = chunks.get(chunkCoordinate);
				if (chunk == null) {
					chunk = new Chunk(this, this.CHUNK_SIZE.x,
							this.CHUNK_SIZE.y, chunkCoordinate.x * this.CHUNK_SIZE.x,
							chunkCoordinate.y * this.CHUNK_SIZE.y);
					chunks.put(chunkCoordinate, chunk);
				}
				visibleChunks.add(chunk);
			}
		}
		return visibleChunks;
	}

	public Point getChunkCoordinateForPoint(PointF point) {
		return new Point((int) Math.floor(point.x / CHUNK_SIZE.x),
				(int) Math.floor(point.y / CHUNK_SIZE.y));
	}

	public Chunk getChunkForCoordinate(Point chunkCoordinate) {
		return this.chunks.get(chunkCoordinate);
	}
}
