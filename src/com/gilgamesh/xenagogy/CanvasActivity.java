package com.gilgamesh.xenagogy;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CanvasActivity extends Activity {
	WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_canvas);

		this.webView = findViewById(R.id.activity_canvas_webview);

		this.webView.getSettings().setJavaScriptEnabled(true);
		this.webView.setWebChromeClient(new WebChromeClient());
		this.webView.setWebViewClient(new WebViewClient());
		this.webView.loadUrl("http://gilgamesh.cc");
	}
}
