package android.support.v4.content.res;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.FontRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.graphics.TypefaceCompat;
import android.support.v4.util.Preconditions;
import android.util.Log;
import android.util.TypedValue;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public final class ResourcesCompat {
    private static final String TAG = "ResourcesCompat";

    public static abstract class FontCallback {
        public abstract void onFontRetrievalFailed(int i);

        public abstract void onFontRetrieved(@NonNull Typeface typeface);

        @RestrictTo({Scope.LIBRARY_GROUP})
        public final void callbackSuccessAsync(final Typeface typeface, @Nullable Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrieved(typeface);
                }
            });
        }

        @RestrictTo({Scope.LIBRARY_GROUP})
        public final void callbackFailAsync(final int reason, @Nullable Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrievalFailed(reason);
                }
            });
        }
    }

    @Nullable
    public static Drawable getDrawable(@NonNull Resources res, @DrawableRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawable(id, theme);
        }
        return res.getDrawable(id);
    }

    @Nullable
    public static Drawable getDrawableForDensity(@NonNull Resources res, @DrawableRes int id, int density, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawableForDensity(id, density, theme);
        }
        if (VERSION.SDK_INT >= 15) {
            return res.getDrawableForDensity(id, density);
        }
        return res.getDrawable(id);
    }

    @ColorInt
    public static int getColor(@NonNull Resources res, @ColorRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColor(id, theme);
        }
        return res.getColor(id);
    }

    @Nullable
    public static ColorStateList getColorStateList(@NonNull Resources res, @ColorRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColorStateList(id, theme);
        }
        return res.getColorStateList(id);
    }

    @Nullable
    public static Typeface getFont(@NonNull Context context, @FontRes int id) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, new TypedValue(), 0, null, null, false);
    }

    public static void getFont(@NonNull Context context, @FontRes int id, @NonNull FontCallback fontCallback, @Nullable Handler handler) throws NotFoundException {
        Preconditions.checkNotNull(fontCallback);
        if (context.isRestricted()) {
            fontCallback.callbackFailAsync(-4, handler);
            return;
        }
        loadFont(context, id, new TypedValue(), 0, fontCallback, handler, false);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static Typeface getFont(@NonNull Context context, @FontRes int id, TypedValue value, int style, @Nullable FontCallback fontCallback) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, value, style, fontCallback, null, true);
    }

    private static Typeface loadFont(@NonNull Context context, int id, TypedValue value, int style, @Nullable FontCallback fontCallback, @Nullable Handler handler, boolean isRequestFromLayoutInflator) {
        Resources resources = context.getResources();
        resources.getValue(id, value, true);
        Typeface typeface = loadFont(context, resources, value, id, style, fontCallback, handler, isRequestFromLayoutInflator);
        if (typeface != null || fontCallback != null) {
            return typeface;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Font resource ID #0x");
        sb.append(Integer.toHexString(id));
        sb.append(" could not be retrieved.");
        throw new NotFoundException(sb.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:64:0x00f0  */
    private static Typeface loadFont(@NonNull Context context, Resources wrapper, TypedValue value, int id, int style, @Nullable FontCallback fontCallback, @Nullable Handler handler, boolean isRequestFromLayoutInflator) {
        int i;
        Resources resources = wrapper;
        TypedValue typedValue = value;
        int i2 = id;
        int i3 = style;
        FontCallback fontCallback2 = fontCallback;
        Handler handler2 = handler;
        if (typedValue.string != null) {
            String file = typedValue.string.toString();
            if (!file.startsWith("res/")) {
                if (fontCallback2 != null) {
                    fontCallback2.callbackFailAsync(-3, handler2);
                }
                return null;
            }
            Typeface typeface = TypefaceCompat.findFromCache(resources, i2, i3);
            if (typeface != null) {
                if (fontCallback2 != null) {
                    fontCallback2.callbackSuccessAsync(typeface, handler2);
                }
                return typeface;
            }
            try {
                if (file.toLowerCase().endsWith(".xml")) {
                    try {
                        FamilyResourceEntry familyEntry = FontResourcesParserCompat.parse(resources.getXml(i2), resources);
                        if (familyEntry == null) {
                            try {
                                Log.e(TAG, "Failed to find font-family tag");
                                if (fontCallback2 != null) {
                                    fontCallback2.callbackFailAsync(-3, handler2);
                                }
                                return null;
                            } catch (XmlPullParserException e) {
                                e = e;
                                i = -3;
                                Context context2 = context;
                                Typeface typeface2 = typeface;
                                String str = TAG;
                                StringBuilder sb = new StringBuilder();
                                sb.append("Failed to parse xml resource ");
                                sb.append(file);
                                Log.e(str, sb.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            } catch (IOException e2) {
                                e = e2;
                                i = -3;
                                Context context3 = context;
                                Typeface typeface3 = typeface;
                                String str2 = TAG;
                                StringBuilder sb2 = new StringBuilder();
                                sb2.append("Failed to read xml resource ");
                                sb2.append(file);
                                Log.e(str2, sb2.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            }
                        } else {
                            Typeface typeface4 = typeface;
                            i = -3;
                            try {
                                return TypefaceCompat.createFromResourcesFamilyXml(context, familyEntry, wrapper, id, style, fontCallback, handler, isRequestFromLayoutInflator);
                            } catch (XmlPullParserException e3) {
                                e = e3;
                                Context context4 = context;
                                String str3 = TAG;
                                StringBuilder sb3 = new StringBuilder();
                                sb3.append("Failed to parse xml resource ");
                                sb3.append(file);
                                Log.e(str3, sb3.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            } catch (IOException e4) {
                                e = e4;
                                Context context5 = context;
                                String str22 = TAG;
                                StringBuilder sb22 = new StringBuilder();
                                sb22.append("Failed to read xml resource ");
                                sb22.append(file);
                                Log.e(str22, sb22.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            }
                        }
                    } catch (XmlPullParserException e5) {
                        e = e5;
                        Typeface typeface5 = typeface;
                        i = -3;
                        Context context6 = context;
                        String str32 = TAG;
                        StringBuilder sb32 = new StringBuilder();
                        sb32.append("Failed to parse xml resource ");
                        sb32.append(file);
                        Log.e(str32, sb32.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    } catch (IOException e6) {
                        e = e6;
                        Typeface typeface6 = typeface;
                        i = -3;
                        Context context7 = context;
                        String str222 = TAG;
                        StringBuilder sb222 = new StringBuilder();
                        sb222.append("Failed to read xml resource ");
                        sb222.append(file);
                        Log.e(str222, sb222.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    }
                } else {
                    Typeface typeface7 = typeface;
                    i = -3;
                    try {
                        typeface = TypefaceCompat.createFromResourcesFontFile(context, resources, i2, file, i3);
                        if (fontCallback2 != null) {
                            if (typeface != null) {
                                try {
                                    fontCallback2.callbackSuccessAsync(typeface, handler2);
                                } catch (XmlPullParserException e7) {
                                    e = e7;
                                    Typeface typeface22 = typeface;
                                    String str322 = TAG;
                                    StringBuilder sb322 = new StringBuilder();
                                    sb322.append("Failed to parse xml resource ");
                                    sb322.append(file);
                                    Log.e(str322, sb322.toString(), e);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                } catch (IOException e8) {
                                    e = e8;
                                    Typeface typeface32 = typeface;
                                    String str2222 = TAG;
                                    StringBuilder sb2222 = new StringBuilder();
                                    sb2222.append("Failed to read xml resource ");
                                    sb2222.append(file);
                                    Log.e(str2222, sb2222.toString(), e);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                }
                            } else {
                                fontCallback2.callbackFailAsync(-3, handler2);
                            }
                        }
                        return typeface;
                    } catch (XmlPullParserException e9) {
                        e = e9;
                        String str3222 = TAG;
                        StringBuilder sb3222 = new StringBuilder();
                        sb3222.append("Failed to parse xml resource ");
                        sb3222.append(file);
                        Log.e(str3222, sb3222.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    } catch (IOException e10) {
                        e = e10;
                        String str22222 = TAG;
                        StringBuilder sb22222 = new StringBuilder();
                        sb22222.append("Failed to read xml resource ");
                        sb22222.append(file);
                        Log.e(str22222, sb22222.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    }
                }
            } catch (XmlPullParserException e11) {
                e = e11;
                i = -3;
                Context context8 = context;
                Typeface typeface8 = typeface;
                String str32222 = TAG;
                StringBuilder sb32222 = new StringBuilder();
                sb32222.append("Failed to parse xml resource ");
                sb32222.append(file);
                Log.e(str32222, sb32222.toString(), e);
                if (fontCallback2 != null) {
                    fontCallback2.callbackFailAsync(i, handler2);
                }
                return null;
            } catch (IOException e12) {
                e = e12;
                i = -3;
                Context context9 = context;
                Typeface typeface9 = typeface;
                String str222222 = TAG;
                StringBuilder sb222222 = new StringBuilder();
                sb222222.append("Failed to read xml resource ");
                sb222222.append(file);
                Log.e(str222222, sb222222.toString(), e);
                if (fontCallback2 != null) {
                }
                return null;
            }
        } else {
            Context context10 = context;
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Resource \"");
            sb4.append(resources.getResourceName(i2));
            sb4.append("\" (");
            sb4.append(Integer.toHexString(id));
            sb4.append(") is not a Font: ");
            sb4.append(value);
            throw new NotFoundException(sb4.toString());
        }
    }

    private ResourcesCompat() {
    }
}
