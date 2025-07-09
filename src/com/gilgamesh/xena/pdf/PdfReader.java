package com.gilgamesh.xena.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.scribble.ScribbleActivity;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRenderer.Page;
import android.net.Uri;
import android.util.Log;

// Pages are laid out vertically, left-aligned at 0.
public class PdfReader {
	static private final float RENDER_SCALE = 1f;

	private PdfRenderer renderer;
	private PageBitmap[] pages;
	private int pageCount;
	private PointF pointScale = new PointF();
	private boolean sizingDone = false;

	public PdfReader(ScribbleActivity scribbleActivity, Uri uri) {

		// 72 because PDFs render at 72dpi.
		this.pointScale.x = XenaApplication.DPI / 72f;
		this.pointScale.y = XenaApplication.DPI / 72f;

		try {
			this.renderer
				= new PdfRenderer(
					scribbleActivity.getContentResolver().openFileDescriptor(uri, "r"));
		} catch (IOException e) {
			Log.e(XenaApplication.TAG,
				"PdfReader::PdfReader: Failed to parse file: " + e.toString() + ".");
		}

		this.pageCount = renderer.getPageCount();
		this.pages = new PageBitmap[this.pageCount];

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(new Runnable() {
			public void run() {
				float nextTop = 0;
				for (int i = 0; i < pageCount; i++) {
					XenaApplication.log("PdfReader::PdfReader: Sized page " + i + ".");
					Page page = renderer.openPage(i);
					Point pageSize = new Point(page.getWidth(), page.getHeight());
					page.close();
					pages[i] = new PageBitmap();
					pages[i].location
						= new RectF(0, nextTop, pageSize.x * pointScale.x,
							nextTop + pageSize.y * pointScale.y);
					nextTop += pageSize.y * pointScale.y;
				}

				// Call redraw when done.
				sizingDone = true;
				scribbleActivity.redraw();
			}
		});
		executorService.shutdown();

		XenaApplication.log("PdfReader::PdfReader: Found " + pageCount + " pages.");
	}

	// Caches bitmaps.
	private synchronized PageBitmap getBitmapForPage(int pageIdx) {
		if (pageIdx < 0 || pageIdx >= this.pages.length) {
			return null;
		}

		if (this.pages[pageIdx].bitmap == null) {
			Page page = renderer.openPage(pageIdx);
			this.pages[pageIdx].bitmap
				= Bitmap.createBitmap(
					Math.round(page.getWidth() * this.pointScale.x * RENDER_SCALE),
					Math.round(page.getHeight() * this.pointScale.y * RENDER_SCALE),
					Bitmap.Config.ARGB_8888);
			page.render(this.pages[pageIdx].bitmap, null, null,
				Page.RENDER_MODE_FOR_DISPLAY);
			XenaApplication.log("PdfReader::PdfReader: Rendered page " + pageIdx
				+ ": " + page.getWidth() + "x" + page.getHeight() + ".");
			page.close();
		}

		return this.pages[pageIdx];
	}

	// Returns PageBitmaps which intersect a rectangle viewport.
	public ArrayList<PageBitmap> getBitmapsForViewport(RectF viewport) {
		ArrayList<PageBitmap> validPages = new ArrayList<PageBitmap>();
		if (this.sizingDone) {
			int firstPage = this.pageCount, lastPage = 0;
			for (int i = 0; i < this.pageCount; i++) {
				if (RectF.intersects(viewport, this.pages[i].location)) {
					validPages.add(this.getBitmapForPage(i));
					firstPage = Math.min(firstPage, i);
					lastPage = Math.max(lastPage, i);
				} else if (validPages.size() > 0) {
					break;
				}
			}

			final int finalFirstPage = firstPage;
			final int finalLastPage = lastPage;
			ExecutorService executorService = Executors.newSingleThreadExecutor();
			executorService.execute(new Runnable() {
				public void run() {
					getBitmapForPage(finalFirstPage - 2);
					getBitmapForPage(finalFirstPage - 1);
					getBitmapForPage(finalLastPage + 1);
					getBitmapForPage(finalLastPage + 2);
				}
			});
			executorService.shutdown();
		}
		return validPages;
	}
}
