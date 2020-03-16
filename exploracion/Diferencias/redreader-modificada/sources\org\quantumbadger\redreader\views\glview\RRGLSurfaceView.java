package org.quantumbadger.redreader.views.glview;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayListRenderer;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayListRenderer.DisplayListManager;
import org.quantumbadger.redreader.views.imageview.FingerTracker;

public class RRGLSurfaceView extends GLSurfaceView {
    private final DisplayListManager mDisplayListManager;
    private final FingerTracker mFingerTracker;

    public RRGLSurfaceView(Context context, DisplayListManager displayListManager) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setRenderer(new RRGLDisplayListRenderer(displayListManager, this));
        setRenderMode(0);
        this.mFingerTracker = new FingerTracker(displayListManager);
        this.mDisplayListManager = displayListManager;
    }

    public boolean onTouchEvent(MotionEvent event) {
        this.mFingerTracker.onTouchEvent(event);
        requestRender();
        return true;
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mDisplayListManager.onUIAttach();
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mDisplayListManager.onUIDetach();
    }
}
