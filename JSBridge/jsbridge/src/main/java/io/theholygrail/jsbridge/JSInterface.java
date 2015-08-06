package io.theholygrail.jsbridge;

import android.util.Log;
import android.webkit.JavascriptInterface;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class JSInterface {
    private static final String TAG = JSInterface.class.getSimpleName();

    private Object mJsObject;
    private String mInterface;

    private List<JSInterface> mSubInterfaces;

    public JSInterface(Object object, String interfaceName) {
        mJsObject = object;
        mInterface = interfaceName;
        mSubInterfaces = new ArrayList<>();
    }

    public void addSubInterface(JSInterface subInterface) {
        mSubInterfaces.add(subInterface);
    }

    public String getInterfaceName() {
        return mInterface;
    }

    public Object getJsObject() {
        return mJsObject;
    }

    public List<JSInterface> getSubInterfaces() {
        return mSubInterfaces;
    }

    public List<String> getJavascriptMethods() {
        List<String> methodNames = new ArrayList<>();
        Method[] methods = getJsObject().getClass().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(JavascriptInterface.class)) {
                methodNames.add(method.getName());

                Log.d(TAG, "Added method: " + method.getName());
            }
        }

        return methodNames;
    }
}
