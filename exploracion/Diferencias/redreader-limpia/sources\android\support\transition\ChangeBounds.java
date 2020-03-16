package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;
import java.util.Map;

public class ChangeBounds extends Transition {
    private static final Property<View, PointF> BOTTOM_RIGHT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "bottomRight") {
        public void set(View view, PointF bottomRight) {
            ViewUtils.setLeftTopRightBottom(view, view.getLeft(), view.getTop(), Math.round(bottomRight.x), Math.round(bottomRight.y));
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> BOTTOM_RIGHT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "bottomRight") {
        public void set(ViewBounds viewBounds, PointF bottomRight) {
            viewBounds.setBottomRight(bottomRight);
        }

        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static final Property<Drawable, PointF> DRAWABLE_ORIGIN_PROPERTY = new Property<Drawable, PointF>(PointF.class, "boundsOrigin") {
        private Rect mBounds = new Rect();

        public void set(Drawable object, PointF value) {
            object.copyBounds(this.mBounds);
            this.mBounds.offsetTo(Math.round(value.x), Math.round(value.y));
            object.setBounds(this.mBounds);
        }

        public PointF get(Drawable object) {
            object.copyBounds(this.mBounds);
            return new PointF((float) this.mBounds.left, (float) this.mBounds.top);
        }
    };
    private static final Property<View, PointF> POSITION_PROPERTY = new Property<View, PointF>(PointF.class, "position") {
        public void set(View view, PointF topLeft) {
            int left = Math.round(topLeft.x);
            int top = Math.round(topLeft.y);
            ViewUtils.setLeftTopRightBottom(view, left, top, view.getWidth() + left, view.getHeight() + top);
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final String PROPNAME_BOUNDS = "android:changeBounds:bounds";
    private static final String PROPNAME_CLIP = "android:changeBounds:clip";
    private static final String PROPNAME_PARENT = "android:changeBounds:parent";
    private static final String PROPNAME_WINDOW_X = "android:changeBounds:windowX";
    private static final String PROPNAME_WINDOW_Y = "android:changeBounds:windowY";
    private static final Property<View, PointF> TOP_LEFT_ONLY_PROPERTY = new Property<View, PointF>(PointF.class, "topLeft") {
        public void set(View view, PointF topLeft) {
            ViewUtils.setLeftTopRightBottom(view, Math.round(topLeft.x), Math.round(topLeft.y), view.getRight(), view.getBottom());
        }

        public PointF get(View view) {
            return null;
        }
    };
    private static final Property<ViewBounds, PointF> TOP_LEFT_PROPERTY = new Property<ViewBounds, PointF>(PointF.class, "topLeft") {
        public void set(ViewBounds viewBounds, PointF topLeft) {
            viewBounds.setTopLeft(topLeft);
        }

        public PointF get(ViewBounds viewBounds) {
            return null;
        }
    };
    private static RectEvaluator sRectEvaluator = new RectEvaluator();
    private static final String[] sTransitionProperties = {PROPNAME_BOUNDS, PROPNAME_CLIP, PROPNAME_PARENT, PROPNAME_WINDOW_X, PROPNAME_WINDOW_Y};
    private boolean mReparent = false;
    private boolean mResizeClip = false;
    private int[] mTempLocation = new int[2];

    private static class ViewBounds {
        private int mBottom;
        private int mBottomRightCalls;
        private int mLeft;
        private int mRight;
        private int mTop;
        private int mTopLeftCalls;
        private View mView;

        ViewBounds(View view) {
            this.mView = view;
        }

        /* access modifiers changed from: 0000 */
        public void setTopLeft(PointF topLeft) {
            this.mLeft = Math.round(topLeft.x);
            this.mTop = Math.round(topLeft.y);
            this.mTopLeftCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        /* access modifiers changed from: 0000 */
        public void setBottomRight(PointF bottomRight) {
            this.mRight = Math.round(bottomRight.x);
            this.mBottom = Math.round(bottomRight.y);
            this.mBottomRightCalls++;
            if (this.mTopLeftCalls == this.mBottomRightCalls) {
                setLeftTopRightBottom();
            }
        }

        private void setLeftTopRightBottom() {
            ViewUtils.setLeftTopRightBottom(this.mView, this.mLeft, this.mTop, this.mRight, this.mBottom);
            this.mTopLeftCalls = 0;
            this.mBottomRightCalls = 0;
        }
    }

    public ChangeBounds() {
    }

    public ChangeBounds(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, Styleable.CHANGE_BOUNDS);
        boolean resizeClip = TypedArrayUtils.getNamedBoolean(a, (XmlResourceParser) attrs, "resizeClip", 0, false);
        a.recycle();
        setResizeClip(resizeClip);
    }

    @Nullable
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    public void setResizeClip(boolean resizeClip) {
        this.mResizeClip = resizeClip;
    }

    public boolean getResizeClip() {
        return this.mResizeClip;
    }

    private void captureValues(TransitionValues values) {
        View view = values.view;
        if (ViewCompat.isLaidOut(view) || view.getWidth() != 0 || view.getHeight() != 0) {
            values.values.put(PROPNAME_BOUNDS, new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
            values.values.put(PROPNAME_PARENT, values.view.getParent());
            if (this.mReparent) {
                values.view.getLocationInWindow(this.mTempLocation);
                values.values.put(PROPNAME_WINDOW_X, Integer.valueOf(this.mTempLocation[0]));
                values.values.put(PROPNAME_WINDOW_Y, Integer.valueOf(this.mTempLocation[1]));
            }
            if (this.mResizeClip) {
                values.values.put(PROPNAME_CLIP, ViewCompat.getClipBounds(view));
            }
        }
    }

    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private boolean parentMatches(View startParent, View endParent) {
        if (!this.mReparent) {
            return true;
        }
        boolean z = true;
        TransitionValues endValues = getMatchedTransitionValues(startParent, true);
        if (endValues == null) {
            if (startParent != endParent) {
                z = false;
            }
            return z;
        }
        if (endParent != endValues.view) {
            z = false;
        }
        return z;
    }

    /* JADX WARNING: type inference failed for: r0v27, types: [android.animation.Animator] */
    /* JADX WARNING: type inference failed for: r0v28, types: [android.animation.Animator] */
    /* JADX WARNING: type inference failed for: r0v33, types: [android.animation.ObjectAnimator] */
    /* JADX WARNING: type inference failed for: r0v36, types: [android.animation.ObjectAnimator] */
    /* JADX WARNING: type inference failed for: r14v13 */
    /* JADX WARNING: type inference failed for: r0v38 */
    /* JADX WARNING: type inference failed for: r0v41, types: [android.animation.ObjectAnimator] */
    /* JADX WARNING: type inference failed for: r0v43 */
    /* JADX WARNING: type inference failed for: r0v44 */
    /* JADX WARNING: type inference failed for: r0v45 */
    /* JADX WARNING: type inference failed for: r0v46 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Unknown variable types count: 6 */
    @Nullable
    public Animator createAnimator(@NonNull ViewGroup sceneRoot, @Nullable TransitionValues startValues, @Nullable TransitionValues endValues) {
        Animator animator;
        boolean z;
        View view;
        ? r0;
        int endLeft;
        int startTop;
        Animator animator2;
        Rect startClip;
        int i;
        Rect endClip;
        Rect startClip2;
        ObjectAnimator clipAnimator;
        TransitionValues transitionValues = startValues;
        TransitionValues transitionValues2 = endValues;
        if (transitionValues == null) {
            ViewGroup viewGroup = sceneRoot;
            TransitionValues transitionValues3 = transitionValues2;
            animator = null;
        } else if (transitionValues2 == null) {
            ViewGroup viewGroup2 = sceneRoot;
            TransitionValues transitionValues4 = transitionValues2;
            animator = null;
        } else {
            Map<String, Object> startParentVals = transitionValues.values;
            Map<String, Object> endParentVals = transitionValues2.values;
            ViewGroup startParent = (ViewGroup) startParentVals.get(PROPNAME_PARENT);
            ViewGroup endParent = (ViewGroup) endParentVals.get(PROPNAME_PARENT);
            if (startParent == null) {
                ViewGroup viewGroup3 = sceneRoot;
                Map<String, Object> map = startParentVals;
                Map<String, Object> map2 = endParentVals;
                ViewGroup viewGroup4 = startParent;
                ViewGroup viewGroup5 = endParent;
                TransitionValues transitionValues5 = transitionValues2;
            } else if (endParent == null) {
                ViewGroup viewGroup6 = sceneRoot;
                Map<String, Object> map3 = startParentVals;
                Map<String, Object> map4 = endParentVals;
                ViewGroup viewGroup7 = startParent;
                ViewGroup viewGroup8 = endParent;
                TransitionValues transitionValues6 = transitionValues2;
            } else {
                View view2 = transitionValues2.view;
                if (parentMatches(startParent, endParent)) {
                    Rect startBounds = (Rect) transitionValues.values.get(PROPNAME_BOUNDS);
                    Rect endBounds = (Rect) transitionValues2.values.get(PROPNAME_BOUNDS);
                    int startLeft = startBounds.left;
                    int endLeft2 = endBounds.left;
                    int startTop2 = startBounds.top;
                    int endTop = endBounds.top;
                    int startRight = startBounds.right;
                    Map<String, Object> map5 = startParentVals;
                    int endRight = endBounds.right;
                    Map<String, Object> map6 = endParentVals;
                    int startBottom = startBounds.bottom;
                    ViewGroup viewGroup9 = startParent;
                    int endBottom = endBounds.bottom;
                    ViewGroup viewGroup10 = endParent;
                    int startWidth = startRight - startLeft;
                    Rect rect = startBounds;
                    int startHeight = startBottom - startTop2;
                    Rect rect2 = endBounds;
                    int endWidth = endRight - endLeft2;
                    int endHeight = endBottom - endTop;
                    View view3 = view2;
                    Rect startClip3 = (Rect) transitionValues.values.get(PROPNAME_CLIP);
                    Rect endClip2 = (Rect) transitionValues2.values.get(PROPNAME_CLIP);
                    int numChanges = 0;
                    if (!((startWidth == 0 || startHeight == 0) && (endWidth == 0 || endHeight == 0))) {
                        if (!(startLeft == endLeft2 && startTop2 == endTop)) {
                            numChanges = 0 + 1;
                        }
                        if (!(startRight == endRight && startBottom == endBottom)) {
                            numChanges++;
                        }
                    }
                    if ((startClip3 != null && !startClip3.equals(endClip2)) || (startClip3 == null && endClip2 != null)) {
                        numChanges++;
                    }
                    if (numChanges > 0) {
                        Rect startClip4 = startClip3;
                        Rect endClip3 = endClip2;
                        if (!this.mResizeClip) {
                            view = view3;
                            ViewUtils.setLeftTopRightBottom(view, startLeft, startTop2, startRight, startBottom);
                            if (numChanges != 2) {
                                int endHeight2 = endHeight;
                                int endWidth2 = endWidth;
                                int startHeight2 = startHeight;
                                View view4 = view;
                                int startWidth2 = startWidth;
                                int i2 = numChanges;
                                if (startLeft != endLeft2) {
                                    view = view4;
                                } else if (startTop2 != endTop) {
                                    view = view4;
                                } else {
                                    Path bottomRight = getPathMotion().getPath((float) startRight, (float) startBottom, (float) endRight, (float) endBottom);
                                    view = view4;
                                    int i3 = endLeft2;
                                    int i4 = endTop;
                                    int i5 = endRight;
                                    int i6 = endHeight2;
                                    int i7 = startHeight2;
                                    int i8 = endWidth2;
                                    int i9 = startWidth2;
                                    z = true;
                                    int endHeight3 = startRight;
                                    int endWidth3 = startTop2;
                                    int startWidth3 = startLeft;
                                    int startHeight3 = startBottom;
                                    r0 = ObjectAnimatorUtils.ofPointF(view, BOTTOM_RIGHT_ONLY_PROPERTY, bottomRight);
                                }
                                int i10 = endLeft2;
                                int i11 = endTop;
                                int i12 = endRight;
                                int i13 = endHeight2;
                                int i14 = startHeight2;
                                int i15 = endWidth2;
                                int i16 = startWidth2;
                                z = true;
                                int endHeight4 = startRight;
                                int endWidth4 = startTop2;
                                int startWidth4 = startLeft;
                                int startHeight4 = startBottom;
                                r0 = ObjectAnimatorUtils.ofPointF(view, TOP_LEFT_ONLY_PROPERTY, getPathMotion().getPath((float) startLeft, (float) startTop2, (float) endLeft2, (float) endTop));
                            } else if (startWidth == endWidth && startHeight == endHeight) {
                                int i17 = numChanges;
                                int endHeight5 = endHeight;
                                int startHeight5 = startHeight;
                                int endWidth5 = endWidth;
                                int i18 = endLeft2;
                                int i19 = startLeft;
                                int i20 = endTop;
                                int i21 = endRight;
                                int i22 = startWidth;
                                int i23 = endHeight5;
                                int i24 = startHeight5;
                                int i25 = endWidth5;
                                z = true;
                                int endHeight6 = startRight;
                                int endWidth6 = startTop2;
                                int startHeight6 = startBottom;
                                r0 = ObjectAnimatorUtils.ofPointF(view, POSITION_PROPERTY, getPathMotion().getPath((float) startLeft, (float) startTop2, (float) endLeft2, (float) endTop));
                            } else {
                                int endHeight7 = endHeight;
                                int endWidth7 = endWidth;
                                int startHeight7 = startHeight;
                                int i26 = numChanges;
                                final ViewBounds viewBounds = new ViewBounds(view);
                                int startWidth5 = startWidth;
                                Path topLeftPath = getPathMotion().getPath((float) startLeft, (float) startTop2, (float) endLeft2, (float) endTop);
                                ObjectAnimator topLeftAnimator = ObjectAnimatorUtils.ofPointF(viewBounds, TOP_LEFT_PROPERTY, topLeftPath);
                                Path path = topLeftPath;
                                View view5 = view;
                                ObjectAnimator bottomRightAnimator = ObjectAnimatorUtils.ofPointF(viewBounds, BOTTOM_RIGHT_PROPERTY, getPathMotion().getPath((float) startRight, (float) startBottom, (float) endRight, (float) endBottom));
                                AnimatorSet set = new AnimatorSet();
                                set.playTogether(new Animator[]{topLeftAnimator, bottomRightAnimator});
                                ? r14 = set;
                                set.addListener(new AnimatorListenerAdapter() {
                                    private ViewBounds mViewBounds = viewBounds;
                                });
                                int i27 = endLeft2;
                                int i28 = endTop;
                                int i29 = endRight;
                                r0 = r14;
                                int i30 = endHeight7;
                                int i31 = startHeight7;
                                int i32 = endWidth7;
                                view = view5;
                                z = true;
                                int endHeight8 = startRight;
                                int endWidth8 = startTop2;
                                int startHeight8 = startBottom;
                                int i33 = startWidth5;
                                int startWidth6 = startLeft;
                            }
                        } else {
                            int i34 = endWidth;
                            int i35 = startHeight;
                            view = view3;
                            int i36 = numChanges;
                            int startWidth7 = startWidth;
                            int maxWidth = Math.max(startWidth7, endWidth);
                            int i37 = startRight;
                            int i38 = startBottom;
                            ViewUtils.setLeftTopRightBottom(view, startLeft, startTop2, startLeft + maxWidth, startTop2 + Math.max(startHeight, endHeight));
                            if (startLeft == endLeft2 && startTop2 == endTop) {
                                endLeft = endLeft2;
                                animator2 = null;
                                startTop = startTop2;
                                int i39 = startLeft;
                            } else {
                                int i40 = startLeft;
                                startTop = startTop2;
                                endLeft = endLeft2;
                                animator2 = ObjectAnimatorUtils.ofPointF(view, POSITION_PROPERTY, getPathMotion().getPath((float) startLeft, (float) startTop2, (float) endLeft2, (float) endTop));
                            }
                            int i41 = startTop;
                            final Rect finalClip = endClip3;
                            if (startClip4 == null) {
                                i = 0;
                                startClip = new Rect(0, 0, startWidth7, startHeight);
                            } else {
                                i = 0;
                                startClip = startClip4;
                            }
                            if (endClip3 == null) {
                                endClip = new Rect(i, i, endWidth, endHeight);
                            } else {
                                endClip = endClip3;
                            }
                            if (!startClip.equals(endClip)) {
                                ViewCompat.setClipBounds(view, startClip);
                                int endHeight9 = endHeight;
                                int i42 = endWidth;
                                int i43 = endHeight9;
                                int endLeft3 = endLeft;
                                int endLeft4 = startWidth7;
                                AnonymousClass8 r10 = r0;
                                Rect rect3 = endClip;
                                final View view6 = view;
                                startClip2 = startClip;
                                final int i44 = endLeft3;
                                int i45 = maxWidth;
                                clipAnimator = ObjectAnimator.ofObject(view, "clipBounds", sRectEvaluator, new Object[]{startClip, endClip});
                                final int i46 = endTop;
                                int i47 = startHeight;
                                final int startHeight9 = endRight;
                                int i48 = endTop;
                                int i49 = endRight;
                                z = true;
                                final int endTop2 = endBottom;
                                AnonymousClass8 r02 = new AnimatorListenerAdapter() {
                                    private boolean mIsCanceled;

                                    public void onAnimationCancel(Animator animation) {
                                        this.mIsCanceled = true;
                                    }

                                    public void onAnimationEnd(Animator animation) {
                                        if (!this.mIsCanceled) {
                                            ViewCompat.setClipBounds(view6, finalClip);
                                            ViewUtils.setLeftTopRightBottom(view6, i44, i46, startHeight9, endTop2);
                                        }
                                    }
                                };
                                clipAnimator.addListener(r10);
                            } else {
                                Rect rect4 = endClip;
                                startClip2 = startClip;
                                int i50 = endWidth;
                                int i51 = startHeight;
                                int i52 = endTop;
                                int i53 = endRight;
                                int i54 = maxWidth;
                                int i55 = endLeft;
                                z = true;
                                int endLeft5 = startWidth7;
                                clipAnimator = null;
                            }
                            Rect rect5 = startClip2;
                            r0 = TransitionUtils.mergeAnimators(animator2, clipAnimator);
                        }
                        if (view.getParent() instanceof ViewGroup) {
                            final ViewGroup parent = (ViewGroup) view.getParent();
                            ViewGroupUtils.suppressLayout(parent, z);
                            addListener(new TransitionListenerAdapter() {
                                boolean mCanceled = false;

                                public void onTransitionCancel(@NonNull Transition transition) {
                                    ViewGroupUtils.suppressLayout(parent, false);
                                    this.mCanceled = true;
                                }

                                public void onTransitionEnd(@NonNull Transition transition) {
                                    if (!this.mCanceled) {
                                        ViewGroupUtils.suppressLayout(parent, false);
                                    }
                                    transition.removeListener(this);
                                }

                                public void onTransitionPause(@NonNull Transition transition) {
                                    ViewGroupUtils.suppressLayout(parent, false);
                                }

                                public void onTransitionResume(@NonNull Transition transition) {
                                    ViewGroupUtils.suppressLayout(parent, true);
                                }
                            });
                        }
                        return r0;
                    }
                    int i56 = endLeft2;
                    int i57 = startRight;
                    int i58 = startTop2;
                    int i59 = startLeft;
                    int i60 = endWidth;
                    int i61 = startHeight;
                    int i62 = endTop;
                    Rect rect6 = startClip3;
                    Rect rect7 = endClip2;
                    int i63 = endRight;
                    int i64 = startBottom;
                    int i65 = startWidth;
                    View view7 = view3;
                    int i66 = numChanges;
                    TransitionValues transitionValues7 = startValues;
                    TransitionValues transitionValues8 = endValues;
                } else {
                    Map<String, Object> map7 = endParentVals;
                    ViewGroup viewGroup11 = startParent;
                    ViewGroup viewGroup12 = endParent;
                    View view8 = view2;
                    TransitionValues transitionValues9 = startValues;
                    int startX = ((Integer) transitionValues9.values.get(PROPNAME_WINDOW_X)).intValue();
                    int startY = ((Integer) transitionValues9.values.get(PROPNAME_WINDOW_Y)).intValue();
                    TransitionValues transitionValues10 = endValues;
                    int endX = ((Integer) transitionValues10.values.get(PROPNAME_WINDOW_X)).intValue();
                    int endY = ((Integer) transitionValues10.values.get(PROPNAME_WINDOW_Y)).intValue();
                    if (!(startX == endX && startY == endY)) {
                        sceneRoot.getLocationInWindow(this.mTempLocation);
                        Bitmap bitmap = Bitmap.createBitmap(view8.getWidth(), view8.getHeight(), Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        view8.draw(canvas);
                        final BitmapDrawable drawable = new BitmapDrawable(bitmap);
                        float transitionAlpha = ViewUtils.getTransitionAlpha(view8);
                        ViewUtils.setTransitionAlpha(view8, 0.0f);
                        ViewUtils.getOverlay(sceneRoot).add(drawable);
                        PathMotion pathMotion = getPathMotion();
                        int[] iArr = this.mTempLocation;
                        Canvas canvas2 = canvas;
                        Bitmap bitmap2 = bitmap;
                        final ViewGroup viewGroup13 = sceneRoot;
                        BitmapDrawable bitmapDrawable = drawable;
                        AnonymousClass10 r6 = r0;
                        final View view9 = view8;
                        int i67 = startX;
                        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(drawable, new PropertyValuesHolder[]{PropertyValuesHolderUtils.ofPointF(DRAWABLE_ORIGIN_PROPERTY, pathMotion.getPath((float) (startX - iArr[0]), (float) (startY - iArr[1]), (float) (endX - iArr[0]), (float) (endY - iArr[1])))});
                        final float f = transitionAlpha;
                        AnonymousClass10 r03 = new AnimatorListenerAdapter() {
                            public void onAnimationEnd(Animator animation) {
                                ViewUtils.getOverlay(viewGroup13).remove(drawable);
                                ViewUtils.setTransitionAlpha(view9, f);
                            }
                        };
                        anim.addListener(r6);
                        return anim;
                    }
                }
                return null;
            }
            return null;
        }
        return animator;
    }
}
