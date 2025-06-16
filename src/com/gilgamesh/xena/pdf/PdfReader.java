package com.gilgamesh.xena.pdf;

import java.io.IOException;
import java.util.ArrayList;

import com.gilgamesh.xena.XenaApplication;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRenderer.Page;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

// Pages are laid out vertically, left-aligned at 0.
public class PdfReader {
	static final private float RENDER_SCALE = 2f;

	private PageBitmap[] pages;
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
			this.pages = new PageBitmap[this.pageCount];
			float nextTop = 0;
			for (int i = 0; i < this.pageCount; i++) {
				Page page = renderer.openPage(i);
				this.pages[i] = new PageBitmap();
				this.pages[i].location = new RectF(0, nextTop, page.getWidth()
						* this.pointScale.x,
						nextTop + page.getHeight() * this.pointScale.y);
				nextTop += page.getHeight() * this.pointScale.y;
				page.close();
			}
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
	private PageBitmap getBitmapForPage(int pageIdx) {
		if (this.pages[pageIdx].bitmap == null) {
			try {
				PdfRenderer renderer = new PdfRenderer(context.getContentResolver()
						.openFileDescriptor(uri, "r"));
				Page page = renderer.openPage(pageIdx);
				Log.v(XenaApplication.TAG,
						"PdfReader::PdfReader: Rendered page " + pageIdx + ": "
								+ page.getWidth()
								+ "x"
								+ page.getHeight() + ".");
				this.pages[pageIdx].bitmap = Bitmap.createBitmap(
						Math.round(page.getWidth() * this.pointScale.x * RENDER_SCALE),
						Math.round(page.getHeight() * this.pointScale.y * RENDER_SCALE),
						Bitmap.Config.ARGB_8888);
				page.render(this.pages[pageIdx].bitmap, null, null,
						Page.RENDER_MODE_FOR_DISPLAY);
				// Bitmap bitmap = Bitmap.createBitmap(
				// Math.round(page.getWidth() * this.pointScale.x),
				// Math.round(page.getHeight() * this.pointScale.y),
				// Bitmap.Config.ARGB_8888);
				// page.render(bitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY);
				// int cPixels = bitmap.getWidth() * bitmap.getHeight();
				// int pixels[] = new int[cPixels];
				// bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0,
				// bitmap.getWidth(),
				// bitmap.getHeight());
				// this.pages[pageIdx].bitmap = Bitmap.createBitmap(
				// Math.round(page.getWidth() * this.pointScale.x),
				// Math.round(page.getHeight() * this.pointScale.y),
				// Bitmap.Config.ALPHA_8);
				// for (int i = 0; i < cPixels; i++) {
				// Color color = Color.valueOf(pixels[i]);
				// pixels[i] = (int) ((1 - (color.red() + color.green() + color.blue())
				// / 3)
				// * color.alpha() * 255) << 24;
				// }
				// this.pages[pageIdx].bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0,
				// 0,
				// bitmap.getWidth(), bitmap.getHeight());
				page.close();
				renderer.close();
			} catch (IOException e) {
				Log.e(XenaApplication.TAG,
						"PdfReader::PdfReader: Failed to read page " + pageIdx + ": "
								+ e.toString() + ".");
			}
		}
		return this.pages[pageIdx];
	}

	// Returns PageBitmaps which intersect a rectangle viewport.
	public ArrayList<PageBitmap> getBitmapsForViewport(RectF viewport) {
		ArrayList<PageBitmap> validPages = new ArrayList<PageBitmap>();
		for (int i = 0; i < this.pageCount; i++) {
			if (RectF.intersects(viewport, this.pages[i].location)) {
				validPages.add(this.getBitmapForPage(i));
			} else if (validPages.size() > 0) {
				break;
			}
		}
		return validPages;
	}
}
