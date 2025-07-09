package com.gilgamesh.algorithm;

import android.graphics.PointF;

public class Geometry {
	static public int sign(float a) {
		if (a == 0) {
			return 0;
		}
		return a > 0 ? 1 : -1;
	}

	// Cross product of vectors (b - a) and (c - a) (in that order).
	static public float crossProduct(PointF a, PointF b, PointF c) {
		return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
	}

	static public boolean isSegmentsIntersecting(PointF a, PointF b, PointF c,
			PointF d) {
		return Geometry.sign(Geometry.crossProduct(a, b, c)) != Geometry
				.sign(Geometry.crossProduct(a, b, d))
				&& Geometry.sign(Geometry.crossProduct(c, d, a)) != Geometry
						.sign(Geometry.crossProduct(c, d, b));
	}

	static public float distance(PointF a, PointF b) {
		return (float) Math
				.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
	}

	static public float distanceToLine(PointF a, PointF b, PointF c) {
		return Math
				.abs((c.y - b.y) * a.x - (c.x - b.x) * a.y + c.x * b.y - c.y * b.x)
				/ (float) Math
						.sqrt((c.y - b.y) * (c.y - b.y) + (c.x - b.x) * (c.x - b.x));
	}
}
