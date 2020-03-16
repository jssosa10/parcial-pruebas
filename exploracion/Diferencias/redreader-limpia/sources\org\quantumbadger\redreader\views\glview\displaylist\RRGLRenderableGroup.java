package org.quantumbadger.redreader.views.glview.displaylist;

import java.util.ArrayList;
import java.util.Iterator;
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public class RRGLRenderableGroup extends RRGLRenderable {
    private final ArrayList<RRGLRenderable> mChildren = new ArrayList<>(16);

    public final void add(RRGLRenderable child) {
        this.mChildren.add(child);
        if (isAdded()) {
            child.onAdded();
        }
    }

    public final void remove(RRGLRenderable child) {
        if (isAdded()) {
            child.onRemoved();
        }
        this.mChildren.remove(child);
    }

    public void onAdded() {
        if (!isAdded()) {
            Iterator it = this.mChildren.iterator();
            while (it.hasNext()) {
                ((RRGLRenderable) it.next()).onAdded();
            }
        }
        super.onAdded();
    }

    /* access modifiers changed from: protected */
    public void renderInternal(RRGLMatrixStack matrixStack, long time) {
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((RRGLRenderable) this.mChildren.get(i)).startRender(matrixStack, time);
        }
    }

    public void onRemoved() {
        super.onRemoved();
        if (!isAdded()) {
            Iterator it = this.mChildren.iterator();
            while (it.hasNext()) {
                ((RRGLRenderable) it.next()).onRemoved();
            }
        }
    }

    public boolean isAnimating() {
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (((RRGLRenderable) this.mChildren.get(i)).isAnimating()) {
                return true;
            }
        }
        return false;
    }

    public void setOverallAlpha(float alpha) {
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((RRGLRenderable) this.mChildren.get(i)).setOverallAlpha(alpha);
        }
    }
}
