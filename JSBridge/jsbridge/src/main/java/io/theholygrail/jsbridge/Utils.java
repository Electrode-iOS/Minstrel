package io.theholygrail.jsbridge;

import android.os.Build;
import android.webkit.JavascriptInterface;

import java.lang.reflect.Method;

/**
 * Helper methods used in the bridge
 */
public class Utils {
    /**
     * Checks if the given method can be exported to JavaScript. If the runtime API level is at least 17 this method
     * checks if the JavascriptInterface annotation is present; if the API level is below 17 (and the given method is
     * not null) <code>true</code> is always returned.
     *
     * @param method
     *         The method to check
     *
     * @return <code>true</code> if the method can be exported, else <code>false</code>
     */
    public static boolean canExportToJavascript(Method method) {
        return method != null && (!javascriptAnnotationsRequired() ||
                method.isAnnotationPresent(JavascriptInterface.class));
    }

    /**
     * Checks whether the current platform requires a JavascriptInterface annotation for a method to be exported to
     * JavaScript, i.e. whether the runtime is at least API 17.
     *
     * @return <code>true</code> if annotations are required, else <code>false</code>
     */
    public static boolean javascriptAnnotationsRequired() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }
}
