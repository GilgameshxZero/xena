package com.gilgamesh.xena.pdf;

import java.io.IOException;

import com.gilgamesh.xena.XenaApplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRenderer.Page;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

public class PdfReader {
	public Bitmap[] bitmaps;

	private Context context;
	private Uri uri;
	private int pageCount;
	private PointF pointScale = new PointF();

	public PdfReader(Context context, Uri uri) {
		this.context = context;
		this.uri = uri;

		DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
		this.pointScale.x = displayMetrics.xdpi / 72;
		this.pointScale.y = displayMetrics.ydpi / 72;

		try {
			PdfRenderer renderer = new PdfRenderer(context.getContentResolver()
					.openFileDescriptor(uri, "r"));
			this.pageCount = renderer.getPageCount();
			this.bitmaps = new Bitmap[this.pageCount];
			renderer.close();

			Log.v(XenaApplication.TAG,
					"PdfReader::PdfReader: Found " + pageCount + " pages.");
		} catch (IOException e) {
			Log.e(XenaApplication.TAG,
					"PdfReader::PdfReader: Failed to parse file: "
							+ e.toString() + ".");
		}
	}

	// Caches bitmaps.
	public Bitmap getBitmapForPage(int pageIdx) {
		if (this.bitmaps[pageIdx] == null) {
			try {
				PdfRenderer renderer = new PdfRenderer(context.getContentResolver()
						.openFileDescriptor(uri, "r"));
				Page page = renderer.openPage(pageIdx);
				Log.v(XenaApplication.TAG,
						"PdfReader::PdfReader: Page " + pageIdx + " is " + page.getWidth()
								+ "x"
								+ page.getHeight() + ".");
				this.bitmaps[pageIdx] = Bitmap.createBitmap(
						Math.round(page.getWidth() * this.pointScale.x),
						Math.round(page.getHeight() * this.pointScale.y),
						Bitmap.Config.ARGB_8888);
				page.render(this.bitmaps[pageIdx], null, null,
						Page.RENDER_MODE_FOR_DISPLAY);
				page.close();
				renderer.close();
			} catch (IOException e) {
				Log.e(XenaApplication.TAG,
						"PdfReader::PdfReader: Failed to read page " + pageIdx + ": "
								+ e.toString() + ".");
			}
		}
		return this.bitmaps[pageIdx];
	}
}
