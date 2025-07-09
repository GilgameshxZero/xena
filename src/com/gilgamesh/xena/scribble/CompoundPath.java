package com.gilgamesh.xena.scribble;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.algorithm.Geometry;

import java.util.ArrayList;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

// Path which has accessible coordinates, and other utilities for drawing and detection.
public class CompoundPath {
	static public final float SHORT_DISTANCE_EPS_DP = 12;
	static public final float SHORT_DISTANCE_EPS_PX
		= CompoundPath.SHORT_DISTANCE_EPS_DP * XenaApplication.DPI / 160;

	static public abstract class Callback {
		public abstract void onPointAdded(CompoundPath that, PointF previousPoint,
			PointF currentPointF);
	}

	// CompoundPaths are given unique IDs sequentially.
	static public int nextId = 0;

	public final int ID = CompoundPath.nextId++;

	private Callback callback;

	// Ground truth for paths for loading/saving SVGs.
	public ArrayList<PointF> points = new ArrayList<PointF>();
	// Used to draw paths.
	public Path path = new Path();
	// Bounding box of path.
	public RectF bounds = new RectF();
	// Stores the chunks that this path is in.
	public ArrayList<Point> containingChunks = new ArrayList<Point>();

	public CompoundPath(PointF point, Callback callback) {
		// PointF constructor only available in API version 30.
		this.points.add(new PointF(point.x, point.y));
		this.path.moveTo(point.x, point.y);
		this.bounds.set(point.x, point.y, point.x, point.y);
		this.callback = callback;
		this.callback.onPointAdded(this, null, point);
	}

	public boolean isIntersectingSegment(PointF start, PointF end) {
		// Quick check to see if we even need to iterate through the whole path.
		RectF expandedBounds
			= new RectF(this.bounds.left - CompoundPath.SHORT_DISTANCE_EPS_PX,
				this.bounds.top - CompoundPath.SHORT_DISTANCE_EPS_PX,
				this.bounds.right + CompoundPath.SHORT_DISTANCE_EPS_PX,
				this.bounds.bottom + CompoundPath.SHORT_DISTANCE_EPS_PX);
		if (!(expandedBounds.contains(start.x, start.y)
			|| expandedBounds.contains(end.x, end.y)
			|| Geometry.isSegmentsIntersecting(
				new PointF(this.bounds.left - CompoundPath.SHORT_DISTANCE_EPS_PX,
					this.bounds.top - CompoundPath.SHORT_DISTANCE_EPS_PX),
				new PointF(this.bounds.right + CompoundPath.SHORT_DISTANCE_EPS_PX,
					this.bounds.bottom + CompoundPath.SHORT_DISTANCE_EPS_PX),
				start, end)
			|| Geometry.isSegmentsIntersecting(
				new PointF(this.bounds.right + CompoundPath.SHORT_DISTANCE_EPS_PX,
					this.bounds.top - CompoundPath.SHORT_DISTANCE_EPS_PX),
				new PointF(this.bounds.left - CompoundPath.SHORT_DISTANCE_EPS_PX,
					this.bounds.bottom + CompoundPath.SHORT_DISTANCE_EPS_PX),
				start, end))) {
			return false;
		}

		for (int i = 0; i < points.size() - 1; i++) {
			if (Geometry.isSegmentsIntersecting(points.get(i), points.get(i + 1),
				start, end)
				|| Geometry.distance(points.get(i),
					start) < CompoundPath.SHORT_DISTANCE_EPS_PX
				|| Geometry.distance(points.get(i),
					end) < CompoundPath.SHORT_DISTANCE_EPS_PX) {
				return true;
			}
		}
		return false;
	}

	public void addPoint(PointF point) {
		this.callback.onPointAdded(this, this.points.get(this.points.size() - 1),
			point);
		this.points.add(new PointF(point.x, point.y));
		this.path.lineTo(point.x, point.y);
		this.bounds.union(point.x, point.y);
	}
}
