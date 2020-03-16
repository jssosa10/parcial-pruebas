package com.google.android.exoplayer2;

import java.util.HashSet;

public final class ExoPlayerLibraryInfo {
    public static final boolean ASSERTIONS_ENABLED = true;
    public static final boolean GL_ASSERTIONS_ENABLED = false;
    public static final String TAG = "ExoPlayer";
    public static final boolean TRACE_ENABLED = true;
    public static final String VERSION = "2.9.1";
    public static final int VERSION_INT = 2009001;
    public static final String VERSION_SLASHY = "ExoPlayerLib/2.9.1";
    private static final HashSet<String> registeredModules = new HashSet<>();
    private static String registeredModulesString = "goog.exo.core";

    private ExoPlayerLibraryInfo() {
    }

    public static synchronized String registeredModules() {
        String str;
        synchronized (ExoPlayerLibraryInfo.class) {
            str = registeredModulesString;
        }
        return str;
    }

    public static synchronized void registerModule(String name) {
        synchronized (ExoPlayerLibraryInfo.class) {
            if (registeredModules.add(name)) {
                StringBuilder sb = new StringBuilder();
                sb.append(registeredModulesString);
                sb.append(", ");
                sb.append(name);
                registeredModulesString = sb.toString();
            }
        }
    }
}
