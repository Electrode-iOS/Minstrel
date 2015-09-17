package io.theholygrail.jsbridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public abstract class JSLog {
    public static final String DEFAULT_TAG = "jsbridge"; // Default tag used if not provided

    public interface LogInterface {
        void verbose(@NonNull String tag, @NonNull String message, @Nullable Throwable t);
        void debug(@NonNull String tag, @NonNull String message, @Nullable Throwable t);
        void info(@NonNull String tag, @NonNull String message, @Nullable Throwable t);
        void warn(@NonNull String tag, @NonNull String message, @Nullable Throwable t);
        void error(@NonNull String tag, @NonNull String message, @Nullable Throwable t);
        void failure(@NonNull String tag, @NonNull String message, @Nullable Throwable t);
    }

    @NonNull
    private static volatile LogInterface sInstance = new DefaultLog();

    public static void setInstance(@NonNull LogInterface instance) {
        sInstance = instance;
    }

    public static void v(@NonNull String message) {
        sInstance.verbose(DEFAULT_TAG, message, null);
    }

    public static void v(@NonNull String tag, @NonNull String message) {
        sInstance.verbose(tag, message, null);
    }

    public static void v(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        sInstance.verbose(tag, message, t);
    }

    public static void d(@NonNull String message) {
        sInstance.debug(DEFAULT_TAG, message, null);
    }

    public static void d(@NonNull String tag, @NonNull String message) {
        sInstance.debug(tag, message, null);
    }

    public static void d(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        sInstance.debug(tag, message, t);
    }

    public static void i(@NonNull String message) {
        sInstance.info(DEFAULT_TAG, message, null);
    }

    public static void i(@NonNull String tag, @NonNull String message) {
        sInstance.info(tag, message, null);
    }

    public static void i(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        sInstance.info(tag, message, t);
    }

    public static void w(@NonNull String message) {
        sInstance.warn(DEFAULT_TAG, message, null);
    }

    public static void w(@NonNull String tag, @NonNull String message) {
        sInstance.warn(tag, message, null);
    }

    public static void w(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        sInstance.warn(tag, message, t);
    }

    public static void e(@NonNull String message) {
        sInstance.warn(DEFAULT_TAG, message, null);
    }

    public static void e(@NonNull String tag, @NonNull String message) {
        sInstance.warn(tag, message, null);
    }

    public static void e(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        sInstance.warn(tag, message, t);
    }

    public static void wtf(@NonNull String message) {
        sInstance.failure(DEFAULT_TAG, message, null);
    }

    public static void wtf(@NonNull String tag, @NonNull String message) {
        sInstance.failure(tag, message, null);
    }

    public static void wtf(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        sInstance.failure(tag, message, t);
    }

    public static class DefaultLog implements LogInterface {

        @Override
        public void verbose(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
            if (t != null) {
                Log.v(tag, message, t);
            } else {
                Log.v(tag, message);
            }
        }

        @Override
        public void debug(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
            if (t != null) {
                Log.d(tag, message, t);
            } else {
                Log.d(tag, message);
            }
        }

        @Override
        public void info(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
            if (t != null) {
                Log.i(tag, message, t);
            } else {
                Log.i(tag, message);
            }
        }

        @Override
        public void warn(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
            if (t != null) {
                Log.w(tag, message, t);
            } else {
                Log.w(tag, message);
            }
        }

        @Override
        public void error(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
            if (t != null) {
                Log.e(tag, message, t);
            } else {
                Log.e(tag, message);
            }
        }

        @Override
        public void failure(@NonNull String tag, @NonNull String message, @Nullable Throwable t) {
            if (t != null) {
                Log.wtf(tag, message, t);
            } else {
                Log.wtf(tag, message);
            }
        }
    }
}
