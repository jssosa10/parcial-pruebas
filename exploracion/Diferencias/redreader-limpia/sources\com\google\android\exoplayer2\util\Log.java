package com.google.android.exoplayer2.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;

public final class Log {
    public static final int LOG_LEVEL_ALL = 0;
    public static final int LOG_LEVEL_ERROR = 3;
    public static final int LOG_LEVEL_INFO = 1;
    public static final int LOG_LEVEL_OFF = Integer.MAX_VALUE;
    public static final int LOG_LEVEL_WARNING = 2;
    private static int logLevel = 0;
    private static boolean logStackTraces = true;

    private Log() {
    }

    public static int getLogLevel() {
        return logLevel;
    }

    public boolean getLogStackTraces() {
        return logStackTraces;
    }

    public static void setLogLevel(int logLevel2) {
        logLevel = logLevel2;
    }

    public static void setLogStackTraces(boolean logStackTraces2) {
        logStackTraces = logStackTraces2;
    }

    public static void d(String tag, String message) {
        if (logLevel == 0) {
            android.util.Log.d(tag, message);
        }
    }

    public static void d(String tag, String message, @Nullable Throwable throwable) {
        if (!logStackTraces) {
            d(tag, appendThrowableMessage(message, throwable));
        }
        if (logLevel == 0) {
            android.util.Log.d(tag, message, throwable);
        }
    }

    public static void i(String tag, String message) {
        if (logLevel <= 1) {
            android.util.Log.i(tag, message);
        }
    }

    public static void i(String tag, String message, @Nullable Throwable throwable) {
        if (!logStackTraces) {
            i(tag, appendThrowableMessage(message, throwable));
        }
        if (logLevel <= 1) {
            android.util.Log.i(tag, message, throwable);
        }
    }

    public static void w(String tag, String message) {
        if (logLevel <= 2) {
            android.util.Log.w(tag, message);
        }
    }

    public static void w(String tag, String message, @Nullable Throwable throwable) {
        if (!logStackTraces) {
            w(tag, appendThrowableMessage(message, throwable));
        }
        if (logLevel <= 2) {
            android.util.Log.w(tag, message, throwable);
        }
    }

    public static void e(String tag, String message) {
        if (logLevel <= 3) {
            android.util.Log.e(tag, message);
        }
    }

    public static void e(String tag, String message, @Nullable Throwable throwable) {
        if (!logStackTraces) {
            e(tag, appendThrowableMessage(message, throwable));
        }
        if (logLevel <= 3) {
            android.util.Log.e(tag, message, throwable);
        }
    }

    private static String appendThrowableMessage(String message, @Nullable Throwable throwable) {
        String str;
        if (throwable == null) {
            return message;
        }
        String throwableMessage = throwable.getMessage();
        if (TextUtils.isEmpty(throwableMessage)) {
            str = message;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(message);
            sb.append(" - ");
            sb.append(throwableMessage);
            str = sb.toString();
        }
        return str;
    }
}
