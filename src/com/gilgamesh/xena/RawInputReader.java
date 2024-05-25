// // Source code is decompiled from a .class file using FernFlower decompiler.
// // package com.onyx.android.sdk.pen;
// package com.gilgamesh.xena;

// import com.onyx.android.sdk.pen.*;

// import android.graphics.Matrix;
// import android.graphics.Rect;
// import android.graphics.RectF;
// import android.util.Log;
// import android.view.View;
// // import androidx.annotation.Nullable;
// import com.onyx.android.sdk.api.device.epd.EpdController;
// import com.onyx.android.sdk.data.note.TouchPoint;
// import com.onyx.android.sdk.pen.data.TouchPointList;
// import com.onyx.android.sdk.pen.event.PenActiveEvent;
// import com.onyx.android.sdk.utils.CollectionUtils;
// import com.onyx.android.sdk.utils.Debug;
// import com.onyx.android.sdk.utils.EventBusHolder;
// import com.onyx.android.sdk.utils.RectUtils;
// import com.onyx.android.sdk.utils.RxTimerUtil;
// import com.onyx.android.sdk.utils.RxTimerUtil.TimerObserver;
// import com.onyx.android.sdk.pen.event.PenDeactivateEvent;

// import java.lang.ref.WeakReference;
// import java.util.List;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;

// public class RawInputReader {
// 	private static final String x;
// 	private static final int y = 0;
// 	private static final int z = 0;
// 	private static final int A = 1;
// 	private static final int B = 2;
// 	private static final int C = 3;
// 	private static final int D = 4;
// 	private static final int E = 5;
// 	private static final int F = 6;
// 	private static boolean G;
// 	private static boolean H;
// 	private volatile boolean a = false;
// 	private volatile boolean b = false;
// 	private volatile boolean c = false;
// 	private volatile boolean d = false;
// 	private volatile float[] e = new float[2];
// 	private volatile float[] f = new float[2];
// 	private volatile int[] g = new int[2];
// 	private volatile Matrix h;
// 	private volatile TouchPointList i;
// 	private volatile float j = 2.0F;
// 	private volatile int k = -16777216;
// 	private RawInputCallback l;
// 	private ExecutorService m = null;
// 	private volatile WeakReference<View> n;
// 	private int o = 500;
// 	private volatile boolean p = true;
// 	private volatile boolean q;
// 	private volatile boolean r = true;
// 	private volatile RectF s;
// 	private volatile RectF t;
// 	private RxTimerUtil.TimerObserver u;
// 	private RxTimerUtil.TimerObserver v;
// 	private boolean w;

// 	public RawInputReader() {
// 	}

// 	private native void nativeRawReader();

// 	private native void nativeRawClose();

// 	private native boolean nativeIsValid();

// 	private static native void nativeDebug(boolean var0);

// 	private native void nativeSetStrokeWidth(float var1);

// 	private native void nativeSetLimitRegion(float[] var1);

// 	private native void nativeSetExcludeRegion(float[] var1);

// 	private native void nativeSetPenState(int var1);

// 	private native void nativePausePen();

// 	private native void nativeSetRegionMode(int var1);

// 	private native void nativeEnableSideBtnErase(boolean var1);

// 	private void c() {
// 		this.nativeRawClose();
// 	}

// 	public static void debugLog(boolean enable) {
// 		G = enable;
// 		nativeDebug(enable);
// 	}

// 	private void f() {
// 		if (this.getHostView() != null) {
// 			this.getHostView().post(() -> {
// 				this.getHostView().getLocationOnScreen(this.g);
// 			});
// 		}
// 	}

// 	private void m() {
// 		this.s = null;
// 		this.t = null;
// 	}

// 	private boolean i() {
// 		return this.w;
// 	}

// 	private void b(boolean reported) {
// 		this.q = reported;
// 	}

// 	private boolean h() {
// 		return this.q;
// 	}

// 	private void a(String message) {
// 		if (G) {
// 			Log.d(x, message);
// 		}
// 	}

// 	private float[] a(Rect rect) {
// 		// RectF var2 = EpdController.mapToRawTouchPoint(this.getHostView(),
// 		// RectUtils.toRectF(rect));
// 		RectF var2 = RectUtils.toRectF(rect);
// 		if (rect.width() > 0 && rect.height() > 0 && (var2 == null || var2.width() <= 0.0F || var2.height() <= 0.0F)) {
// 			Log.e(x, "[GILGAMESH] Empty region detected when mapping!!!!!");
// 		}

// 		float[] rect1;
// 		float[] var10000 = rect1 = new float[4];
// 		rect1[0] = var2.left;
// 		rect1[1] = var2.top;
// 		rect1[2] = var2.right;
// 		var10000[3] = var2.bottom;
// 		return var10000;
// 	}

// 	private float[] a(List<Rect> rectList) {
// 		float[] var2 = new float[rectList.size() * 4];

// 		for (int var3 = 0; var3 < rectList.size(); ++var3) {
// 			Rect var4;
// 			Rect var10000 = var4 = (Rect) rectList.get(var3);
// 			// RectF var5 = EpdController.mapToRawTouchPoint(this.getHostView(),
// 			// RectUtils.toRectF(var4));
// 			RectF var5 = RectUtils.toRectF(var4);
// 			if (var10000.width() > 0 && var4.height() > 0
// 					&& (var5 == null || var5.width() <= 0.0F || var5.height() <= 0.0F)) {
// 				Log.e(x, "[GILGAMESH] Empty region detected!!!!!");
// 			}

// 			int var6 = var3 * 4;
// 			var2[var6] = var5.left;
// 			int var7 = var6 + 1;
// 			var2[var7] = var5.top;
// 			var7 = var6 + 2;
// 			var2[var7] = var5.right;
// 			var6 += 3;
// 			var2[var6] = var5.bottom;
// 		}

// 		return var2;
// 	}

// 	private void o() {
// 		this.e().shutdown();
// 		this.m = null;
// 	}

// 	private ExecutorService e() {
// 		if (this.m == null) {
// 			this.m = Executors.newSingleThreadExecutor();
// 		}

// 		return this.m;
// 	}

// 	private void q() {
// 		// this.e().submit(new a(this));
// 		this.e().submit(new Runnable() {
// 			public void run() {
// 				label42: {
// 					try {
// 						try {
// 							Debug.i(this.getClass(), "submitJob nativeRawReader " + Thread.currentThread().getName() + "-"
// 									+ Thread.currentThread().getId(), new Object[0]);
// 							RawInputReader.this.nativeRawReader();
// 							break label42;
// 						} catch (Exception var4) {
// 						}
// 					} catch (Throwable var5) {
// 						Debug.i(this.getClass(), "submitJob finally closeRawInput " + Thread.currentThread().getName() + "-"
// 								+ Thread.currentThread().getId(), new Object[0]);
// 						RawInputReader.this.c();
// 						throw var5;
// 					}

// 					Debug.i(this.getClass(), "submitJob finally closeRawInput " + Thread.currentThread().getName() + "-"
// 							+ Thread.currentThread().getId(), new Object[0]);
// 					RawInputReader.this.c();
// 					return;
// 				}

// 				Debug.i(this.getClass(), "submitJob finally closeRawInput " + Thread.currentThread().getName() + "-"
// 						+ Thread.currentThread().getId(), new Object[0]);
// 				RawInputReader.this.c();
// 			}
// 		});
// 	}

// 	private void a(float x, float y, int pressure, int tx, int ty, boolean shortcutDrawing, boolean shortcutErasing,
// 			long ts) {
// 		if (H) {
// 			if (G) {
// 				Debug.d(this.getClass(), "down point missing, use first move point as down point.", new Object[0]);
// 			}

// 			RawInputReader var10000 = this;
// 			boolean var10 = this.a;
// 			var10000.a(x, y, pressure, 0, tx, ty, ts, var10, shortcutDrawing, shortcutErasing);
// 		}
// 	}

// 	private void a(float x, float y, int pressure, int size, int tx, int ty, long ts) {
// 		RawInputReader var10000 = this;
// 		RawInputReader var10001 = this;
// 		RawInputReader var10002 = this;
// 		TouchPoint var9;
// 		float pressure1 = (float) pressure;
// 		float size1 = (float) size;
// 		TouchPoint var10003 = var9 = new TouchPoint(x, y, pressure1, size1, tx, ty, ts);
// 		var10002.d(var9);
// 		var10001.e(var9);
// 		var10000.c(var9);
// 	}

// 	private boolean g() {
// 		return this.c;
// 	}

// 	private void a(boolean active) {
// 		this.c = active;
// 	}

// 	private void e(TouchPoint touchPoint) {
// 		if (this.i()) {
// 			if (!this.g()) {
// 				this.a(true);
// 				PenActiveEvent var2;
// 				var2 = new PenActiveEvent(touchPoint);
// 				this.a((Object) var2);
// 			}

// 			RawInputReader var10000 = this;
// 			this.b();
// 			TimeUnit var3 = TimeUnit.MILLISECONDS;
// 			// RxTimerUtil.TimerObserver touchPoint1 = var10000.b(touchPoint);
// 			// RxTimerUtil.timerOnCurScheduler(100L, var3, touchPoint1);
// 		}
// 	}

// 	private void b() {
// 		RxTimerUtil.cancel(this.v);
// 		this.v = null;
// 	}

// 	// private RxTimerUtil.TimerObserver b(TouchPoint touchPoint) {
// 	// if (this.v == null) {

// 	// TimerObserver var2;
// 	// var2 = new TimerObserver() {
// 	// // $FF: synthetic field
// 	// final TouchPoint a;

// 	// {
// 	// this.a = touchPoint;
// 	// }

// 	// public void a(Long aLong) {
// 	// if (RawInputReader.this.g()) {
// 	// RawInputReader.this.a(false);
// 	// RawInputReader.this.a((Object) (new PenDeactivateEvent(this.a)));
// 	// }

// 	// }
// 	// };
// 	// this.v = var2;
// 	// }

// 	// return this.v;
// 	// }

// 	private void r() {
// 		this.h = EpdController.getRawTouchPointToScreenMatrix();
// 	}

// 	private TouchPoint d(TouchPoint touchPoint) {
// 		this.e[0] = touchPoint.x;
// 		this.e[1] = touchPoint.y;
// 		if (this.h == null) {
// 			this.r();
// 		}

// 		float[] var2;
// 		if (this.h != null) {
// 			var2 = this.f;
// 			this.h.mapPoints(var2, this.e);
// 		} else {
// 			var2 = this.e;
// 			float[] var3 = this.f;
// 			EpdController.mapToView((View) null, var2, var3);
// 		}

// 		float[] var10006 = this.f;
// 		var10006[0] -= (float) this.g[0];
// 		float[] var10005 = this.f;
// 		var10005[1] -= (float) this.g[1];
// 		touchPoint.x = this.f[0];
// 		touchPoint.y = this.f[1];
// 		return touchPoint;
// 	}

// 	private boolean a(TouchPoint touchPoint, boolean create) {
// 		if (this.i == null) {
// 			if (!create) {
// 				return false;
// 			}

// 			TouchPointList create1;
// 			create1 = new TouchPointList(600);
// 			this.i = create1;
// 		}

// 		if (touchPoint != null && this.i != null) {
// 			this.i.add(touchPoint);
// 		}

// 		return true;
// 	}

// 	private boolean j() {
// 		return this.d;
// 	}

// 	private void a(Object event) {
// 		if (this.i()) {
// 			this.getEventBusHolder().post(event);
// 		}
// 	}

// 	private void a(float x, float y, int pressure, int size, int tx, int ty, long ts, boolean erasing,
// 			boolean shortcutDrawing, boolean shortcutErasing) {
// 		this.n();
// 		TouchPoint var12;
// 		float pressure1 = (float) pressure;
// 		float size1 = (float) size;
// 		TouchPoint var10002 = var12 = new TouchPoint(x, y, pressure1, size1, tx, ty, ts);
// 		this.d(var12);
// 		if (!erasing) {
// 			this.p();
// 			this.f(var12);
// 		}

// 		if (this.a(var12, true)) {
// 			this.a(var12, erasing, shortcutDrawing, shortcutErasing);
// 		}

// 	}

// 	private void a(float x, float y, int pressure, int size, int tx, int ty, long ts, boolean erasing) {
// 		TouchPoint var10;
// 		float var10004 = x;
// 		x = (float) pressure;
// 		float pressure1 = (float) size;
// 		TouchPoint var10002 = var10 = new TouchPoint(var10004, y, x, pressure1, tx, ty, ts);
// 		this.d(var10002);
// 		if (!this.a(var10)) {
// 			this.a(var10, true);
// 			if (!erasing) {
// 				this.p();
// 				this.f(var10);
// 			}

// 			this.b(var10, erasing);
// 		}
// 	}

// 	private boolean a(TouchPoint curMovePoint) {
// 		if (this.isFilterRepeatMovePoint() && this.i != null) {
// 			TouchPoint var4;
// 			if ((var4 = (TouchPoint) CollectionUtils.getLast(this.i.getPoints())) != null) {
// 				long var2;
// 				if ((var2 = curMovePoint.timestamp - var4.timestamp) <= 0L) {
// 					return false;
// 				} else {
// 					float var10000 = Math.abs(curMovePoint.x - var4.x) + Math.abs(curMovePoint.y - var4.y);
// 					float var5 = Math.abs(curMovePoint.pressure - var4.pressure);
// 					return var10000 / (float) var2 < 0.005F && var5 <= 2.0F;
// 				}
// 			} else {
// 				return false;
// 			}
// 		} else {
// 			return false;
// 		}
// 	}

// 	private void b(float x, float y, int pressure, int size, int tx, int ty, long ts, boolean erasing,
// 			boolean releaseOutLimitRegion, boolean forceReport) {
// 		TouchPoint var12;
// 		float pressure1 = (float) pressure;
// 		float size1 = (float) size;
// 		TouchPoint var10002 = var12 = new TouchPoint(x, y, pressure1, size1, tx, ty, ts);
// 		this.d(var12);
// 		if (!erasing) {
// 			this.f(var12);
// 		}

// 		if (this.i != null && this.i.size() > 0) {
// 			this.a(this.i, erasing, forceReport);
// 		}

// 		this.n();
// 		this.b(var12, erasing, releaseOutLimitRegion, forceReport);
// 		this.b(false);
// 		if (!erasing) {
// 			this.l();
// 		}

// 	}

// 	private void f(TouchPoint touchPoint) {
// 		if (this.s == null) {
// 			RawInputReader var10000 = this;
// 			RectF var4;
// 			TouchPoint var10002 = touchPoint;
// 			float touchPoint1 = touchPoint.x;
// 			float var2 = var10002.y;
// 			RectF var10001 = var4 = new RectF(touchPoint1, var2, touchPoint1, var2);
// 			var10000.s = var4;
// 		} else {
// 			RectF var3 = this.s;
// 			float var5 = touchPoint.x;
// 			var3.union(var5, touchPoint.y);
// 		}

// 	}

// 	private void l() {
// 		if (this.isPenUpRefreshEnabled()) {
// 			this.p();
// 			long var10000 = (long) this.o;
// 			RawInputReader var10001 = this;
// 			TimeUnit var1 = TimeUnit.MILLISECONDS;
// 			// RxTimerUtil.timer(var10000, var1, var10001.d());
// 		}
// 	}

// 	// @Nullable
// 	private RectF a() {
// 		if (this.s == null) {
// 			return null;
// 		} else {
// 			RectF var1;
// 			RectF var10001 = var1 = new RectF(this.s);
// 			RectUtils.expand(var10001, this.j);
// 			if (this.t != null) {
// 				var1.union(this.t);
// 			}

// 			this.t = new RectF(this.s);
// 			this.s = null;
// 			return var1;
// 		}
// 	}

// 	private void p() {
// 		RxTimerUtil.cancel(this.u);
// 		this.u = null;
// 	}

// 	// private RxTimerUtil.TimerObserver d() {
// 	// if (this.u == null) {
// 	// TimerObserver var1;
// 	// var1 = new TimerObserver() {
// 	// public void a(Long aLong) {
// 	// RawInputReader var10000 = RawInputReader.this;
// 	// var10000.a(var10000.a());
// 	// }
// 	// };
// 	// this.u = var1;
// 	// }

// 	// return this.u;
// 	// }

// 	private void n() {
// 		this.i = null;
// 	}

// 	private void a(TouchPoint point, boolean erasing, boolean shortcutDrawing, boolean shortcutErasing) {
// 		if (this.l != null && (this.j() || erasing)) {
// 			this.b(true);
// 			if (erasing) {
// 				this.l.onBeginRawErasing(shortcutErasing, point);
// 			} else {
// 				this.l.onBeginRawDrawing(shortcutDrawing, point);
// 			}

// 		}
// 	}

// 	private void b(TouchPoint touchPoint, boolean erasing, boolean releaseOutLimitRegion, boolean forceReport) {
// 		if (this.l != null && (forceReport || this.j() || erasing) && this.h()) {
// 			if (erasing) {
// 				this.l.onEndRawErasing(releaseOutLimitRegion, touchPoint);
// 			} else {
// 				this.l.onEndRawDrawing(releaseOutLimitRegion, touchPoint);
// 			}

// 		}
// 	}

// 	private void b(TouchPoint touchPoint, boolean erasing) {
// 		if (this.l != null && (this.j() || erasing) && this.h()) {
// 			if (erasing) {
// 				this.l.onRawErasingTouchPointMoveReceived(touchPoint);
// 			} else {
// 				this.l.onRawDrawingTouchPointMoveReceived(touchPoint);
// 			}

// 		}
// 	}

// 	private void a(TouchPointList touchPointList, boolean erasing, boolean forceReport) {
// 		if (this.l != null && touchPointList != null && (forceReport || this.j() || erasing) && this.h()) {
// 			if (erasing) {
// 				this.l.onRawErasingTouchPointListReceived(touchPointList);
// 			} else {
// 				this.l.onRawDrawingTouchPointListReceived(touchPointList);
// 			}

// 		}
// 	}

// 	private void c(TouchPoint touchPoint) {
// 		if (this.l != null && this.j()) {
// 			this.l.onPenActive(touchPoint);
// 		}
// 	}

// 	private void a(RectF refreshRect) {
// 		if (this.l != null && this.j() && refreshRect != null) {
// 			Debug.d(this.getClass(), "invokePenUpRefresh: " + refreshRect.toString(), new Object[0]);
// 			this.l.onPenUpRefresh(refreshRect);
// 		}
// 	}

// 	static {
// 		System.loadLibrary("onyx_pen_touch_reader");
// 		x = RawInputReader.class.getSimpleName();
// 		H = true;
// 	}

// 	public void setRawInputCallback(RawInputCallback callback) {
// 		this.l = callback;
// 	}

// 	public void setHostView(View view) {
// 		RawInputReader var10000 = this;
// 		RawInputReader var10001 = this;
// 		WeakReference var2;
// 		var2 = new WeakReference(view);
// 		var10001.n = var2;
// 		var10000.f();
// 	}

// 	public View getHostView() {
// 		return this.n == null ? null : (View) this.n.get();
// 	}

// 	public void start() {
// 		this.f();
// 		this.c();
// 		this.b = false;
// 		this.d = false;
// 		this.q();
// 		this.a("start");
// 	}

// 	public void resume() {
// 		this.f();
// 		this.nativeSetPenState(4);
// 		this.d = true;
// 		this.a("resume");
// 	}

// 	public void pause() {
// 		this.nativePausePen();
// 		this.d = false;
// 		this.a("pause");
// 	}

// 	public void quit() {
// 		this.p();
// 		this.nativeSetPenState(4);
// 		this.l = null;
// 		this.m();
// 		this.c();
// 		this.d = false;
// 		this.j = 2.0F;
// 		this.k = -16777216;
// 		this.b = true;
// 		this.c = false;
// 		this.o();
// 		this.a("quit");
// 	}

// 	public boolean isFdValid() {
// 		return this.nativeIsValid();
// 	}

// 	public void setStrokeWidth(float w) {
// 		this.j = w;
// 		this.nativeSetStrokeWidth(w);
// 		EpdController.setStrokeWidth(w);
// 		this.a("setStrokeWidth:" + w);
// 	}

// 	public void setStrokeColor(int color) {
// 		this.k = color;
// 	}

// 	public void setPenUpRefreshTimeMs(int penUpRefreshTimeMs) {
// 		this.o = penUpRefreshTimeMs;
// 	}

// 	public void setPostInputEvent(boolean post) {
// 		this.w = post;
// 	}

// 	public void setPenUpRefreshEnabled(boolean enable) {
// 		this.p = enable;
// 	}

// 	public boolean isPenUpRefreshEnabled() {
// 		return this.p;
// 	}

// 	public void setFilterRepeatMovePoint(boolean filter) {
// 		this.r = filter;
// 	}

// 	public boolean isFilterRepeatMovePoint() {
// 		return this.r;
// 	}

// 	public void setSingleRegionMode() {
// 		this.nativeSetRegionMode(1);
// 		EpdController.setScreenHandWritingRegionMode(this.getHostView(), 1);
// 		this.a("setSingleRegionMode");
// 	}

// 	public void setMultiRegionMode() {
// 		this.nativeSetRegionMode(0);
// 		EpdController.setScreenHandWritingRegionMode(this.getHostView(), 0);
// 		this.a("setMultiRegionMode");
// 	}

// 	public void enableSideBtnErase(boolean enable) {
// 		this.nativeEnableSideBtnErase(enable);
// 	}

// 	public void setLimitRect(Rect rect) {
// 		if (rect != null) {
// 			// this.nativeSetLimitRegion(this.a(rect));
// 			EpdController.setScreenHandWritingRegionLimit(this.getHostView(), new Rect[] { rect });
// 			this.a("setLimitRect");
// 		}

// 	}

// 	public void setLimitRect(List<Rect> rectList) {
// 		if (rectList != null && rectList.size() > 0) {
// 			// this.nativeSetLimitRegion(this.a(rectList));
// 			EpdController.setScreenHandWritingRegionLimit(this.getHostView(), (Rect[]) rectList.toArray(new Rect[0]));
// 			this.a("setLimitRect");
// 		}

// 	}

// 	public void setExcludeRect(List<Rect> rectList) {
// 		if (rectList != null && rectList.size() > 0) {
// 			this.nativeSetExcludeRegion(this.a(rectList));
// 			EpdController.setScreenHandWritingRegionExclude(this.getHostView(), (Rect[]) rectList.toArray(new Rect[0]));
// 			this.a("setExcludeRect");
// 		}

// 	}

// 	public void onTouchPointReceived(float x, float y, int pressure, int tx, int ty, boolean isErasing,
// 			boolean shortcutDrawing, boolean shortcutErasing, int state, long ts) {
// 		if (state == 5) {
// 			this.a(x, y, pressure, 0, tx, ty, ts);
// 		}

// 		if (state == 6 || this.j()) {
// 			boolean var12;
// 			RawInputReader var10000;
// 			if (state == 0) {
// 				var10000 = this;
// 				this.a = isErasing;
// 				this.r();
// 				var12 = this.a;
// 				var10000.a(x, y, pressure, 0, tx, ty, ts, var12, shortcutDrawing, shortcutErasing);
// 			} else if (state == 1) {
// 				this.a = isErasing;
// 				if (this.h()) {
// 					var10000 = this;
// 					var12 = this.a;
// 					var10000.a(x, y, pressure, 0, tx, ty, ts, var12);
// 				} else {
// 					this.r();
// 					this.a(x, y, pressure, tx, ty, shortcutDrawing, shortcutErasing, ts);
// 				}
// 			} else if (state != 2 && state != 3) {
// 				if (state == 6) {
// 					var10000 = this;
// 					RawInputReader var10001 = this;
// 					Log.d(RawInputReader.x, "EpdShapeHandler pen force release");
// 					this.a = isErasing;
// 					var12 = this.a;
// 					var10001.b(x, y, pressure, 0, tx, ty, ts, var12, false, true);
// 					var10000.a = false;
// 				}
// 			} else {
// 				this.a = isErasing;
// 				byte isErasing1 = 0;
// 				shortcutDrawing = this.a;
// 				if (state == 3) {
// 					shortcutErasing = true;
// 				} else {
// 					shortcutErasing = false;
// 				}

// 				this.b(x, y, pressure, isErasing1, tx, ty, ts, shortcutDrawing, shortcutErasing, false);
// 				this.a = false;
// 			}

// 		}
// 	}

// 	public EventBusHolder getEventBusHolder() {
// 		return TouchEventBus.getInstance().getEventBusHolder();
// 	}

// 	public TouchPointList detachTouchPointList() {
// 		TouchPointList var10000 = this.i;
// 		this.n();
// 		return var10000;
// 	}

// 	public boolean isErasing() {
// 		return this.a;
// 	}
// }
