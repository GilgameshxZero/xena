// // Source code is decompiled from a .class file using FernFlower decompiler.
// // package com.onyx.android.sdk.pen;
// package com.gilgamesh.xena;

// import com.onyx.android.sdk.pen.*;

// import android.graphics.Rect;
// import android.view.MotionEvent;
// import android.view.View;
// import com.onyx.android.sdk.device.Device;
// import com.onyx.android.sdk.pen.touch.AppTouchRender;
// // import com.onyx.android.sdk.pen.touch.SFTouchRender;
// import com.onyx.android.sdk.pen.touch.TouchRender;
// import com.onyx.android.sdk.utils.DeviceFeatureUtil;
// import com.onyx.android.sdk.utils.EventBusHolder;
// import java.util.ArrayList;
// import java.util.Iterator;
// import java.util.List;

// public class TouchHelper {
// 	public static final int STROKE_STYLE_PENCIL = 0;
// 	public static final int STROKE_STYLE_FOUNTAIN = 1;
// 	public static final int STROKE_STYLE_MARKER = 2;
// 	public static final int STROKE_STYLE_NEO_BRUSH = 3;
// 	public static final int STROKE_STYLE_CHARCOAL = 4;
// 	public static final int STROKE_STYLE_DASH = 5;
// 	public static final int STROKE_STYLE_CHARCOAL_V2 = 6;
// 	public static final int FEATURE_APP_TOUCH_RENDER = 1;
// 	public static final int FEATURE_SF_TOUCH_RENDER = 2;
// 	public static final int FEATURE_ALL_TOUCH_RENDER = 3;
// 	private volatile boolean a;
// 	private volatile boolean b;
// 	private volatile boolean c;
// 	private List<TouchRender> d;

// 	private TouchHelper(View view, RawInputCallback callback) {
// 		this(view, DeviceFeatureUtil.hasStylus(view.getContext()) ? 2 : 1, callback);
// 	}

// 	private TouchHelper(View view, int feature, RawInputCallback callback) {
// 		this.a = false;
// 		this.b = false;
// 		this.c = false;
// 		ArrayList var4;
// 		ArrayList var10002 = var4 = new ArrayList<TouchRender>();
// 		this.d = var10002;
// 		if ((feature & 2) == 2) {
// 			ArrayList var10000 = var4;
// 			SFTouchRender var5;
// 			var5 = new SFTouchRender(view, callback);
// 			var10000.add(var5);
// 		}

// 		if ((feature & 1) == 1) {
// 			this.d.add(new AppTouchRender(view, callback));
// 		}

// 		this.bindHostView(view, callback);
// 	}

// 	public static TouchHelper create(View hostView, RawInputCallback callback) {
// 		if (hostView != null) {
// 			return new TouchHelper(hostView, callback);
// 		} else {
// 			throw new IllegalArgumentException("hostView should not be null!");
// 		}
// 	}

// 	public static TouchHelper create(View hostView, boolean stylus, RawInputCallback callback) {
// 		byte stylus1;
// 		if (stylus) {
// 			stylus1 = 2;
// 		} else {
// 			stylus1 = 1;
// 		}

// 		return create(hostView, stylus1, callback);
// 	}

// 	public static TouchHelper create(View hostView, int feature, RawInputCallback callback) {
// 		if (hostView != null) {
// 			return new TouchHelper(hostView, feature, callback);
// 		} else {
// 			throw new IllegalArgumentException("hostView should not be null!");
// 		}
// 	}

// 	private void a() {
// 		this.a = false;
// 		this.b = false;
// 		this.c = false;
// 	}

// 	public static void register(Object subscriber) {
// 		getEventBusHolder().register(subscriber);
// 	}

// 	public static void unregister(Object subscriber) {
// 		getEventBusHolder().unregister(subscriber);
// 	}

// 	public static EventBusHolder getEventBusHolder() {
// 		return TouchEventBus.getInstance().getEventBusHolder();
// 	}

// 	public TouchHelper bindHostView(View hostView, RawInputCallback callback) {
// 		if (hostView == null) {
// 			throw new IllegalArgumentException("hostView should not be null!");
// 		} else {
// 			Iterator var3 = this.d.iterator();

// 			while (var3.hasNext()) {
// 				((TouchRender) var3.next()).bindHostView(hostView, callback);
// 			}

// 			return this;
// 		}
// 	}

// 	public boolean onTouchEvent(MotionEvent event) {
// 		TouchHelper var10000 = this;
// 		boolean var3 = false;

// 		for (Iterator var2 = var10000.d.iterator(); var2
// 				.hasNext(); var3 = ((TouchRender) var2.next()).onTouchEvent(event)) {
// 		}

// 		return var3;
// 	}

// 	public TouchHelper setStrokeStyle(int style) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setStrokeStyle(style);
// 		}

// 		return this;
// 	}

// 	public TouchHelper setStrokeColor(int color) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setStrokeColor(color);
// 		}

// 		return this;
// 	}

// 	public TouchHelper setStrokeWidth(float w) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setStrokeWidth(w);
// 		}

// 		return this;
// 	}

// 	public TouchHelper debugLog(boolean enable) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).debugLog(enable);
// 		}

// 		return this;
// 	}

// 	public TouchHelper setLimitRect(Rect limitRect, List<Rect> excludeRectList) {
// 		Iterator var3 = this.d.iterator();

// 		while (var3.hasNext()) {
// 			((TouchRender) var3.next()).setLimitRect(limitRect, excludeRectList);
// 		}

// 		return this;
// 	}

// 	public TouchHelper setLimitRect(List<Rect> limitRectList, List<Rect> excludeRectList) {
// 		Iterator var3 = this.d.iterator();

// 		while (var3.hasNext()) {
// 			((TouchRender) var3.next()).setLimitRect(limitRectList, excludeRectList);
// 		}

// 		return this;
// 	}

// 	public TouchHelper setLimitRect(List<Rect> limitRectList) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setLimitRect(limitRectList);
// 		}

// 		return this;
// 	}

// 	public TouchHelper setExcludeRect(List<Rect> excludeRectList) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setExcludeRect(excludeRectList);
// 		}

// 		return this;
// 	}

// 	public TouchHelper openRawDrawing() {
// 		this.a();
// 		Iterator var1 = this.d.iterator();

// 		while (var1.hasNext()) {
// 			((TouchRender) var1.next()).openDrawing();
// 		}

// 		this.a = true;
// 		return this;
// 	}

// 	public void closeRawDrawing() {
// 		this.a = false;
// 		Iterator var1 = this.d.iterator();

// 		while (var1.hasNext()) {
// 			((TouchRender) var1.next()).closeDrawing();
// 		}

// 	}

// 	public TouchHelper setRawDrawingEnabled(boolean enabled) {
// 		if (!this.a) {
// 			return this;
// 		} else {
// 			this.setRawDrawingRenderEnabled(enabled);
// 			this.setRawInputReaderEnable(enabled);
// 			this.resetPenDefaultRawDrawing();
// 			return this;
// 		}
// 	}

// 	public boolean isRawDrawingInputEnabled() {
// 		return this.c;
// 	}

// 	public boolean isRawDrawingRenderEnabled() {
// 		return this.b;
// 	}

// 	public TouchHelper setRawDrawingRenderEnabled(boolean enabled) {
// 		if (!this.a) {
// 			return this;
// 		} else if (this.b == enabled) {
// 			return this;
// 		} else {
// 			Iterator var2 = this.d.iterator();

// 			while (var2.hasNext()) {
// 				((TouchRender) var2.next()).setDrawingRenderEnabled(enabled);
// 			}

// 			this.b = enabled;
// 			return this;
// 		}
// 	}

// 	public TouchHelper forceSetRawDrawingEnabled(boolean enabled) {
// 		if (!this.a) {
// 			return this;
// 		} else {
// 			Iterator var2 = this.d.iterator();

// 			while (var2.hasNext()) {
// 				TouchRender var10000 = (TouchRender) var2.next();
// 				var10000.setDrawingRenderEnabled(enabled);
// 				var10000.setInputReaderEnable(enabled);
// 			}

// 			this.b = enabled;
// 			this.c = enabled;
// 			return this;
// 		}
// 	}

// 	public TouchHelper setRawInputReaderEnable(boolean enabled) {
// 		if (!this.a) {
// 			return this;
// 		} else if (this.c == enabled) {
// 			return this;
// 		} else {
// 			Iterator var2 = this.d.iterator();

// 			while (var2.hasNext()) {
// 				((TouchRender) var2.next()).setInputReaderEnable(enabled);
// 			}

// 			this.c = enabled;
// 			return this;
// 		}
// 	}

// 	public boolean isRawDrawingCreated() {
// 		return this.a;
// 	}

// 	public TouchHelper setSingleRegionMode() {
// 		Iterator var1 = this.d.iterator();

// 		while (var1.hasNext()) {
// 			((TouchRender) var1.next()).setSingleRegionMode();
// 		}

// 		return this;
// 	}

// 	public TouchHelper setMultiRegionMode() {
// 		Iterator var1 = this.d.iterator();

// 		while (var1.hasNext()) {
// 			((TouchRender) var1.next()).setMultiRegionMode();
// 		}

// 		return this;
// 	}

// 	public void setPenUpRefreshTimeMs(int penUpRefreshTimeMs) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setPenUpRefreshTimeMs(penUpRefreshTimeMs);
// 		}

// 	}

// 	@Deprecated
// 	public void setPenUpRefreshEnabled(boolean enable) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setPenUpRefreshEnabled(enable);
// 		}

// 	}

// 	public void setFilterRepeatMovePoint(boolean filter) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setPenUpRefreshEnabled(filter);
// 		}

// 	}

// 	public void setPostInputEvent(boolean post) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setPostInputEvent(post);
// 		}

// 	}

// 	public void enableSideBtnErase(boolean enable) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).enableSideBtnErase(enable);
// 		}

// 	}

// 	public void enableFingerTouch(boolean enable) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).enableFingerTouch(enable);
// 		}

// 	}

// 	public void onlyEnableFingerTouch(boolean only) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).onlyEnableFingerTouch(only);
// 		}

// 	}

// 	public TouchHelper setBrushRawDrawingEnabled(boolean enable) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setBrushRawDrawingEnabled(enable);
// 		}

// 		return this;
// 	}

// 	public TouchHelper setEraserRawDrawingEnabled(boolean drawing) {
// 		Iterator var2 = this.d.iterator();

// 		while (var2.hasNext()) {
// 			((TouchRender) var2.next()).setEraserRawDrawingEnabled(drawing);
// 		}

// 		return this;
// 	}

// 	public void resetPenDefaultRawDrawing() {
// 		Device.currentDevice().setBrushRawDrawingEnabled(true);
// 		Device.currentDevice().setEraserRawDrawingEnabled(false);
// 	}
// }
