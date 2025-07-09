package com.gilgamesh.xena.pdf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import com.gilgamesh.xena.XenaApplication;
import com.gilgamesh.xena.scribble.ScribbleActivity;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRenderer.Page;
import android.net.Uri;

// Pages are laid out vertically, left-aligned at 0.
public class PdfReader {
	static public abstract class Callback {
		public abstract void onPageSizedIntoViewport();
	}

	// 72 because PDFs render at 72dpi.
	static private final float PDF_DPI = 72f;
	static private final PointF POINT_SCALE
		= new PointF(XenaApplication.DPI / PdfReader.PDF_DPI,
			XenaApplication.DPI / PdfReader.PDF_DPI);
	static private final float RENDER_SCALE = 1f;

	private PdfRenderer renderer;
	private PageBitmap[] pages;
	private ReentrantLock pageLock = new ReentrantLock();
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	private RectF callerViewport = null;

	public PdfReader(ScribbleActivity scribbleActivity, Uri uri,
		Callback callback) {
		// Create page array and start sizing all pages in background.
		try {
			this.renderer
				= new PdfRenderer(
					scribbleActivity.getContentResolver().openFileDescriptor(uri, "r"));
			this.pages = new PageBitmap[renderer.getPageCount()];

			this.executor.execute(new Runnable() {
				public void run() {
					float nextTop = 0;
					for (int i = 0; i < pages.length; i++) {
						pageLock.lock();
						Page page = renderer.openPage(i);
						Point pageSize = new Point(page.getWidth(), page.getHeight());
						page.close();
						pageLock.unlock();

						pages[i] = new PageBitmap();
						pages[i].location
							= new RectF(0, nextTop, pageSize.x * PdfReader.POINT_SCALE.x,
								nextTop + pageSize.y * PdfReader.POINT_SCALE.y);
						nextTop += pageSize.y * PdfReader.POINT_SCALE.y;
						XenaApplication.log("PdfReader::PdfReader: Sized page ", i, ".");

						// If a page is sized into the viewport, call the callback to redraw
						// it immediately.
						if (callerViewport != null
							&& RectF.intersects(callerViewport, pages[i].location)) {
							callback.onPageSizedIntoViewport();
							XenaApplication
								.log("PdfReader::PdfReader: Callback redraw for page ", i, ".");
						}
					}
				}
			});

			XenaApplication
				.log("PdfReader::PdfReader: Found " + pages.length + " pages.");
		} catch (IOException e) {
			XenaApplication.error("PdfReader::PdfReader: Failed to parse file \"",
				uri, "\": \"", e.toString(), "\".");
			this.pages = new PageBitmap[0];
		}
	}

	private PageBitmap getBitmapForPage(int pageIdx, boolean preload) {
		// Do nothing if not yet ready, since we cannot open multiple pages at once.
		if (pageIdx < 0 || pageIdx >= this.pages.length) {
			return null;
		}

		// Cache bitmap if not available, otherwise return it directly.
		if (this.pages[pageIdx].bitmap == null) {
			this.pages[pageIdx].bitmap
				= Bitmap.createBitmap(
					Math.round((this.pages[pageIdx].location.right
						- this.pages[pageIdx].location.left) * PdfReader.RENDER_SCALE),
					Math.round((this.pages[pageIdx].location.bottom
						- this.pages[pageIdx].location.top) * PdfReader.RENDER_SCALE),
					Bitmap.Config.ARGB_8888);

			this.pageLock.lock();
			Page page = renderer.openPage(pageIdx);
			page.render(this.pages[pageIdx].bitmap, null, null,
				Page.RENDER_MODE_FOR_DISPLAY);
			page.close();
			this.pageLock.unlock();

			XenaApplication.log("PdfReader::getBitmapForPage: Rendered page "
				+ pageIdx + ": " + page.getWidth() + "x" + page.getHeight() + ".");
		}

		if (preload) {
			this.executor.execute(new Runnable() {
				public void run() {
					getBitmapForPage(pageIdx - 2, false);
					getBitmapForPage(pageIdx - 1, false);
					getBitmapForPage(pageIdx + 1, false);
					getBitmapForPage(pageIdx + 2, false);
				}
			});
		}

		return this.pages[pageIdx];
	}

	// Returns PageBitmaps which intersect a rectangle viewport. Also caches
	// viewport, so that if a page is sized within the viewport, the callback here
	// will be called.
	public ArrayList<PageBitmap> getBitmapsForViewport(RectF viewport) {
		this.callerViewport = viewport;

		ArrayList<PageBitmap> validPages = new ArrayList<PageBitmap>();
		// Requested bitmaps may not be a contiguous subarray if width differs, and
		// viewport is on the right. We cannot binary search while guaranteeing
		// correctness.
		for (int i = 0; i < pages.length; i++) {
			if (this.pages[i] != null
				&& RectF.intersects(viewport, this.pages[i].location)) {
				validPages.add(this.getBitmapForPage(i, true));
			} else if (validPages.size() > 0) {
				break;
			}
		}

		return validPages;
	}
}
