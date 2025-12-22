package com.gilgamesh.xena.filesystem;

import com.gilgamesh.xena.XenaApplication;

import android.text.Editable;
import android.text.TextWatcher;

public class FilePickerEditTextChangedListener implements TextWatcher {
	private FilePickerActivity filePickerActivity;
	private String beforeText;

	public FilePickerEditTextChangedListener(
		FilePickerActivity filePickerActivity) {
		this.filePickerActivity = filePickerActivity;
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
		int after) {
		beforeText = s.toString();
	}

	@Override
	public void afterTextChanged(Editable s) {
		String afterText = s.toString();
		if (!beforeText.substring(0, beforeText.lastIndexOf('/'))
			.equals(afterText.substring(0, afterText.lastIndexOf('/')))) {
			this.filePickerActivity.listingPage = 0;
		}
		this.filePickerActivity.refreshListing();
	}
}
