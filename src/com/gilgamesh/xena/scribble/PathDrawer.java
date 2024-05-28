package com.gilgamesh.xena.scribble;

import java.util.ArrayList;
import java.util.LinkedList;

import android.graphics.Path;
import android.graphics.PointF;

// Path state and utility functions.
public class PathDrawer {
	// Ground truth for paths for loading/saving SVGs.
	public LinkedList<ArrayList<PointF>> paths;
	// Used to draw paths.
	public LinkedList<Path> pathsNative = new LinkedList<Path>();
	// Used to intersect eraser paths.
	public LinkedList<Path> pathsNativeFilled = new LinkedList<Path>();
}
