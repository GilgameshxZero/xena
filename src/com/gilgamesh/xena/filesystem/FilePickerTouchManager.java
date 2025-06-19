package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.XenaApplication;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class FilePickerTouchManager implements View.OnTouchListener {
	static private final float PAGE_THRESHOLD_DP = 48;
	static private final float PAGE_THRESHOLD_PX = FilePickerTouchManager.PAGE_THRESHOLD_DP
			* XenaApplication.DPI / 160;

	private FilePickerActivity filePickerActivity;

	private float touchDownY;

	FilePickerTouchManager(FilePickerActivity filePickerActivity) {
		this.filePickerActivity = filePickerActivity;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				this.touchDownY = event.getY();
				return true;
			case MotionEvent.ACTION_UP:
				Log.v(XenaApplication.TAG, "FilePickerActivity::onTouch:ACTION_UP.");
				if (event.getY() > this.touchDownY
						+ FilePickerTouchManager.PAGE_THRESHOLD_PX) {
					this.filePickerActivity.page--;
					this.filePickerActivity.updateListing();
				} else if (event.getY() < this.touchDownY
						- FilePickerTouchManager.PAGE_THRESHOLD_PX) {
					this.filePickerActivity.page++;
					this.filePickerActivity.updateListing();
				} else {
					v.callOnClick();
				}
				return true;
			default:
				return false;
		}
	}
}
