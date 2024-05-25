// // Source code is decompiled from a .class file using FernFlower decompiler.
// // package com.onyx.android.sdk.pen;
// package com.gilgamesh.xena;

// // import com.onyx.android.sdk.pen.*;
// import com.onyx.android.sdk.pen.RawInputCallback;

// import android.graphics.Rect;
// import android.view.View;
// import java.util.List;

// public class RawInputManager {
//    private RawInputCallback a;
//    private RawInputReader b = null;
//    private boolean c = true;

//    public RawInputManager() {
//    }

//    private RawInputReader a() {
//       if (this.b == null) {
//          RawInputReader var1;
//          var1 = new RawInputReader();
//          this.b = var1;
//       }

//       return this.b;
//    }

//    public void setRawInputCallback(RawInputCallback callback) {
//       this.a = callback;
//    }

//    public void startRawInputReader() {
//       if (this.isUseRawInput()) {
//          this.a().setRawInputCallback(this.a);
//          this.a().start();
//       }
//    }

//    public void resumeRawInputReader() {
//       if (this.isUseRawInput()) {
//          this.a().resume();
//       }
//    }

//    public void pauseRawInputReader() {
//       if (this.isUseRawInput()) {
//          this.a().pause();
//       }
//    }

//    public void quitRawInputReader() {
//       if (this.isUseRawInput()) {
//          this.a().quit();
//       }
//    }

//    public boolean isUseRawInput() {
//       return this.c;
//    }

//    public RawInputManager setUseRawInput(boolean use) {
//       this.c = use;
//       return this;
//    }

//    public View getHostView() {
//       return this.a().getHostView();
//    }

//    public RawInputManager setHostView(View view) {
//       RawInputManager var10000 = this;
//       RawInputManager var10001 = this;
//       this.a().setHostView(view);
//       Rect var2;
//       Rect var10003 = var2 = new Rect();
//       view.getLocalVisibleRect(var10003);
//       var10001.a().setLimitRect(var2);
//       return var10000;
//    }

//    public RawInputManager setLimitRect(Rect limitRect, List<Rect> excludeRectList) {
//       this.a().setLimitRect(limitRect);
//       this.a().setExcludeRect(excludeRectList);
//       return this;
//    }

//    public RawInputManager setLimitRect(List<Rect> limitRect, List<Rect> excludeRectList) {
//       this.a().setLimitRect(limitRect);
//       this.a().setExcludeRect(excludeRectList);
//       return this;
//    }

//    public RawInputManager setLimitRect(List<Rect> limitRectList) {
//       this.a().setLimitRect(limitRectList);
//       return this;
//    }

//    public RawInputManager setExcludeRect(List<Rect> excludeRectList) {
//       this.a().setExcludeRect(excludeRectList);
//       return this;
//    }

//    public void setStrokeWidth(float w) {
//       this.a().setStrokeWidth(w);
//    }

//    public void setStrokeColor(int color) {
//       this.a().setStrokeColor(color);
//    }

//    public void setSingleRegionMode() {
//       this.a().setSingleRegionMode();
//    }

//    public void setMultiRegionMode() {
//       this.a().setMultiRegionMode();
//    }

//    public void setPenUpRefreshTimeMs(int penUpRefreshTimeMs) {
//       this.a().setPenUpRefreshTimeMs(penUpRefreshTimeMs);
//    }

//    public void setPenUpRefreshEnabled(boolean enable) {
//       this.a().setPenUpRefreshEnabled(enable);
//    }

//    public void setFilterRepeatMovePoint(boolean filter) {
//       this.a().setFilterRepeatMovePoint(filter);
//    }

//    public void setPostInputEvent(boolean post) {
//       this.a().setPostInputEvent(post);
//    }

//    public void enableSideBtnErase(boolean enable) {
//       this.a().enableSideBtnErase(enable);
//    }
// }
