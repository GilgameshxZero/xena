package com.gilgamesh.xena.scribble;

import java.util.LinkedList;

import android.graphics.PointF;

// Path state and utility functions.
public class PathDrawer {
	public LinkedList<CompoundPath> paths;

	// Offset of the upper-left point of the viewport.
	public PointF viewportOffset = new PointF();
}
