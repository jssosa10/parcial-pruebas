package org.quantumbadger.redreader.views.glview.displaylist;

import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.quantumbadger.redreader.views.glview.RRGLSurfaceView;
import org.quantumbadger.redreader.views.glview.Refreshable;
import org.quantumbadger.redreader.views.glview.program.RRGLContext;
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;
import org.quantumbadger.redreader.views.imageview.FingerTracker.FingerListener;

public class RRGLDisplayListRenderer implements Renderer, Refreshable {
    private int frames = 0;
    private final DisplayListManager mDisplayListManager;
    private RRGLContext mGLContext;
    private RRGLMatrixStack mMatrixStack;
    private final float[] mPixelMatrix = new float[16];
    private RRGLDisplayList mScene;
    private final RRGLSurfaceView mSurfaceView;
    private long startTime = -1;

    public interface DisplayListManager extends FingerListener {
        void onGLSceneCreate(RRGLDisplayList rRGLDisplayList, RRGLContext rRGLContext, Refreshable refreshable);

        void onGLSceneResolutionChange(RRGLDisplayList rRGLDisplayList, RRGLContext rRGLContext, int i, int i2);

        boolean onGLSceneUpdate(RRGLDisplayList rRGLDisplayList, RRGLContext rRGLContext);

        void onUIAttach();

        void onUIDetach();
    }

    public RRGLDisplayListRenderer(DisplayListManager displayListManager, RRGLSurfaceView surfaceView) {
        this.mDisplayListManager = displayListManager;
        this.mSurfaceView = surfaceView;
    }

    public void onSurfaceCreated(GL10 ignore, EGLConfig config) {
        this.mGLContext = new RRGLContext(this.mSurfaceView.getContext());
        this.mMatrixStack = new RRGLMatrixStack(this.mGLContext);
        this.mScene = new RRGLDisplayList();
        this.mGLContext.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        this.mDisplayListManager.onGLSceneCreate(this.mScene, this.mGLContext, this);
    }

    public void onSurfaceChanged(GL10 ignore, int width, int height) {
        this.mGLContext.setViewport(width, height);
        float hScale = 2.0f / ((float) width);
        float vScale = -2.0f / ((float) height);
        Matrix.setIdentityM(this.mPixelMatrix, 0);
        Matrix.translateM(this.mPixelMatrix, 0, -1.0f, 1.0f, 0.0f);
        Matrix.scaleM(this.mPixelMatrix, 0, hScale, vScale, 1.0f);
        this.mDisplayListManager.onGLSceneResolutionChange(this.mScene, this.mGLContext, width, height);
    }

    public void onDrawFrame(GL10 ignore) {
        long time = System.currentTimeMillis();
        if (this.startTime == -1) {
            this.startTime = time;
        }
        this.frames++;
        if (time - this.startTime >= 1000) {
            this.startTime = time;
            StringBuilder sb = new StringBuilder();
            sb.append("Frames: ");
            sb.append(this.frames);
            Log.i("FPS", sb.toString());
            this.frames = 0;
        }
        boolean animating = this.mDisplayListManager.onGLSceneUpdate(this.mScene, this.mGLContext);
        this.mGLContext.clear();
        this.mGLContext.activatePixelMatrix(this.mPixelMatrix, 0);
        this.mMatrixStack.assertAtRoot();
        this.mScene.startRender(this.mMatrixStack, time);
        this.mMatrixStack.assertAtRoot();
        if (animating || this.mScene.isAnimating()) {
            this.mSurfaceView.requestRender();
        }
    }

    public void refresh() {
        this.mSurfaceView.requestRender();
    }
}
