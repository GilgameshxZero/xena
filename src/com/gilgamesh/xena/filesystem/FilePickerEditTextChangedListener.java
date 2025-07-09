package com.gilgamesh.xena.filesystem;

import android.text.Editable;
import android.text.TextWatcher;

public class FilePickerEditTextChangedListener implements TextWatcher {
	private FilePickerActivity filePickerActivity;

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
	}

	@Override
	public void afterTextChanged(Editable s) {
		this.filePickerActivity.listingPage = 0;
		this.filePickerActivity.refreshListing();
	}
}
