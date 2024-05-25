// // Source code is decompiled from a .class file using FernFlower decompiler.
// // package com.onyx.android.sdk.pen.touch;
// package com.gilgamesh.xena;

// import com.onyx.android.sdk.pen.touch.*;

// import android.graphics.Rect;
// import android.view.View;
// import com.onyx.android.sdk.api.device.epd.EpdController;
// import com.onyx.android.sdk.device.Device;
// import com.onyx.android.sdk.pen.EpdPenManager;
// import com.onyx.android.sdk.pen.RawInputCallback;
// // import com.onyx.android.sdk.pen.RawInputManager;
// // import com.onyx.android.sdk.pen.RawInputReader;
// import java.lang.ref.WeakReference;
// import java.util.List;

// public class SFTouchRender implements TouchRender {
//    private WeakReference<View> a;
//    private RawInputCallback b;
//    private EpdPenManager c;
//    private RawInputManager d;

//    public SFTouchRender(View view, RawInputCallback callback) {
//    }

//    public static TouchRender create(View hostView, RawInputCallback callback) {
//       if (hostView != null) {
//          return new SFTouchRender(hostView, callback);
//       } else {
//          throw new IllegalArgumentException("hostView should not be null!");
//       }
//    }

//    private View e() {
//       WeakReference var1;
//       return (var1 = this.a) == null ? null : (View)var1.get();
//    }

//    private boolean a() {
//       return this.b == null || this.e() == null;
//    }

//    private EpdPenManager d() {
//       if (this.c == null) {
//          EpdPenManager var1;
//          var1 = new EpdPenManager();
//          this.c = var1;
//       }

//       return this.c;
//    }

//    private void a(View view) {
//       this.d().setHostView(view);
//    }

//    private void a(View view, RawInputCallback callback) {
//       this.f().setHostView(view);
//       this.f().setRawInputCallback(callback);
//    }

//    private RawInputManager f() {
//       if (this.d == null) {
//          RawInputManager var1;
//          var1 = new RawInputManager();
//          this.d = var1;
//       }

//       return this.d;
//    }

//    private void j() {
//       this.setStrokeStyle(0);
//    }

//    private void b() {
//       this.f().startRawInputReader();
//       this.d().startDrawing();
//    }

//    private void c() {
//       this.f().quitRawInputReader();
//       this.d().quitDrawing();
//    }

//    private void g() {
//       EpdController.leaveScribbleMode(this.f().getHostView());
//       this.f().pauseRawInputReader();
//       this.d().pauseDrawing();
//    }

//    private void h() {
//       EpdController.leaveScribbleMode(this.f().getHostView());
//       this.d().pauseDrawing();
//    }

//    private void k() {
//       this.d().resumeDrawing();
//    }

//    private void l() {
//       this.f().resumeRawInputReader();
//    }

//    private void i() {
//       this.f().pauseRawInputReader();
//    }

//    public void bindHostView(View view, RawInputCallback callback) {
//       WeakReference var3;
//       var3 = new WeakReference(view);
//       this.a = var3;
//       this.b = callback;
//       this.a(view, callback);
//       this.a(view);
//    }

//    public void setStrokeStyle(int style) {
//       this.d().setStrokeStyle(style);
//    }

//    public void setStrokeColor(int color) {
//       EpdPenManager.setStrokeColor(color);
//       this.f().setStrokeColor(color);
//    }

//    public void setStrokeWidth(float w) {
//       this.f().setStrokeWidth(w);
//    }

//    public void debugLog(boolean enable) {
//       RawInputReader.debugLog(enable);
//    }

//    public void setLimitRect(Rect limitRect, List<Rect> excludeRectList) {
//       this.f().setLimitRect(limitRect, excludeRectList);
//    }

//    public void setLimitRect(List<Rect> limitRectList, List<Rect> excludeRectList) {
//       this.f().setLimitRect(limitRectList, excludeRectList);
//    }

//    public void setLimitRect(List<Rect> limitRectList) {
//       this.f().setLimitRect(limitRectList);
//    }

//    public void setExcludeRect(List<Rect> excludeRectList) {
//       this.f().setExcludeRect(excludeRectList);
//    }

//    public void openDrawing() {
//       this.j();
//       this.b();
//    }

//    public void closeDrawing() {
//       this.j();
//       this.g();
//       this.c();
//    }

//    public void setDrawingRenderEnabled(boolean enabled) {
//       if (enabled) {
//          this.k();
//       } else {
//          this.h();
//       }

//    }

//    public void setBrushRawDrawingEnabled(boolean enable) {
//       Device.currentDevice().setBrushRawDrawingEnabled(enable);
//    }

//    public void setEraserRawDrawingEnabled(boolean enable) {
//       Device.currentDevice().setEraserRawDrawingEnabled(enable);
//    }

//    public void setInputReaderEnable(boolean enabled) {
//       if (enabled) {
//          this.l();
//       } else {
//          this.i();
//       }

//    }

//    public void setSingleRegionMode() {
//       this.f().setSingleRegionMode();
//    }

//    public void setMultiRegionMode() {
//       this.f().setMultiRegionMode();
//    }

//    public void setPenUpRefreshTimeMs(int penUpRefreshTimeMs) {
//       this.f().setPenUpRefreshTimeMs(penUpRefreshTimeMs);
//    }

//    public void setPenUpRefreshEnabled(boolean enable) {
//       this.f().setPenUpRefreshEnabled(enable);
//    }

//    public void setFilterRepeatMovePoint(boolean filter) {
//       this.f().setFilterRepeatMovePoint(filter);
//    }

//    public void setPostInputEvent(boolean post) {
//       this.f().setPostInputEvent(post);
//    }

//    public void enableSideBtnErase(boolean enable) {
//       this.f().enableSideBtnErase(enable);
//       Device.currentDevice().setEnablePenSideButton(enable);
//    }

//    public void enableFingerTouch(boolean enable) {
//    }

//    public void onlyEnableFingerTouch(boolean only) {
//    }
// }
