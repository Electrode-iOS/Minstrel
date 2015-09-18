package io.theholygrail.jsbridge;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JSWebView extends WebView {
    private static final String TAG = JSWebView.class.getSimpleName();
    private int mFunctionCacheLimit = 200;

    private String mJsInterfacePrefix = "NativeBridge";

    public BridgeSupport bridgeSupport = null;

    public JSWebView(Context context) {
        super(context);
        setupDefaults();
    }

    public JSWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDefaults();
    }

    public JSWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupDefaults();
    }

    public void setJsInterfacePrefix(String interfacePrefix) {
        mJsInterfacePrefix = interfacePrefix;
    }

    public void executeJavascript(String javascript) {
        // Useful for seeing the injected javascript.
        //Log.d(TAG, "javascript: " + javascript);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript(javascript, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    JSLog.d(TAG, "evaluateJavascript.onReceiveValue(): " + value);
                }
            });
        } else {
            loadUrl("javascript:" + javascript);
        }
    }

    public void setFunctionCacheLimit(int count) {
        mFunctionCacheLimit = count;
        executeJavascript("__functionIDLimit = " + mFunctionCacheLimit + ";");
    }

    // Private stuff -------------------------------------------------------------------------------

    public class BridgeSupport {
        private ValueCallback<String> mResultCallback = null;
        private final Lock resultLock = new ReentrantLock();

        protected void expectResult(ValueCallback<String> resultCallback) {
            resultLock.lock();
            mResultCallback = resultCallback;
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void passResult(String result) {
            if (mResultCallback != null) {
                mResultCallback.onReceiveValue(result);
                resultLock.unlock();
            }
        }
    }

    private void exportNamespaceToJS(Object obj, String namespace, String interfaceName) {
        Method methods[] = obj.getClass().getMethods();
        String ns = TextUtils.isEmpty(namespace) ? "" : namespace + ".";
        String dot = TextUtils.isEmpty(namespace) ? "" : ".";

        executeJavascript(mJsInterfacePrefix + dot + namespace + " = { };");

        for (Method method : methods) {
            if (Utils.canExportToJavascript(method)) {
                String methodName = method.getName();
                String parameterString = generateParameterString(method);
                String callString = generateCallString(method);

                String jsString = mJsInterfacePrefix + "." + ns + methodName + " = function(" + parameterString + ") { " +
                        interfaceName + "." + methodName + "("+callString+"); }";
                executeJavascript(jsString);
            }
        }
    }

    private String generateParameterString(Method method) {
        Class paramClasses[] = method.getParameterTypes();
        String result = "";
        for (int i = 0; i < paramClasses.length; i++) {
            if (i == 0)
                result += "arg0";
            else
                result += ", arg"+String.valueOf(i);
        }

        return result;
    }

    private String generateCallString(Method method) {
        Class paramClasses[] = method.getParameterTypes();
        String result = "";
        for (int i = 0; i < paramClasses.length; i++) {
            if (i == 0)
                result += "valueToBridgeString(arg0)";
            else
                result += ", valueToBridgeString(arg"+String.valueOf(i)+")";
        }

        return result;
    }

    private void setupDefaults() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setLoadsImagesAutomatically(true);
        // TODO: Caching policy?
        //getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        getSettings().setDomStorageEnabled(true);
        setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                JSLog.d("JSWebViewError", "Code: " + errorCode + "Error: " + description + " Url: " + failingUrl);
            }
        });

        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {
                //return super.onConsoleMessage(consoleMessage);
                JSLog.i(TAG, "console: " + consoleMessage.message() + " source: " + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber());
                return false;
            }
        });

        bridgeSupport = new BridgeSupport();
    }

    private String getValueToBridgeStringFunction() {
        StringBuilder buf = new StringBuilder();

        try {
            InputStream is = getContext().getAssets().open("valuetobridgestring.js");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            String str;

            while ((str=reader.readLine()) != null) {
                buf.append(str);
            }
            reader.close();
        } catch (IOException ioe) {
            JSLog.e(TAG, "Could not read asset from lib");
        }

        return buf.toString();
    }

    private void loadJavascriptBaseSupport() {
        executeJavascript("__functionIDCounter = 0, __functionCache = { };");

        // set the limit of our function cache to the default.
        // this is used in the javascript injection below.
        setFunctionCacheLimit(mFunctionCacheLimit);

        executeJavascript(getValueToBridgeStringFunction());
    }

    private void setupJsInterfaces(JSInterface jsInterface) {
        addJavascriptInterface(jsInterface.getJsObject(), jsInterface.getInterfaceName());

        List<JSInterface> subInterfaces = jsInterface.getSubInterfaces();
        if (subInterfaces != null) {
            for (JSInterface subInterface: subInterfaces) {
                setupJsInterface(subInterface);
            }
        }
    }

    public void setupJsInterface(JSInterface jsInterface) {
        addJavascriptInterface(bridgeSupport, "__bridgeSupport");
        setupJsInterfaces(jsInterface);
    }

    private void setupSubNamespace(JSInterface jsInterface, String nameSpace) {
            List<JSInterface> subInterfaces = jsInterface.getSubInterfaces();

            String dot = TextUtils.isEmpty(nameSpace) ? "": ".";

            for (JSInterface subInterface:subInterfaces) {
                exportNamespaceToJS(subInterface.getJsObject(), nameSpace + dot + subInterface.getInterfaceName(), subInterface.getInterfaceName());
                setupSubNamespace(subInterface, nameSpace + subInterface.getInterfaceName());
            }
    }

    public void setupNamespace(JSInterface mainInterface) {
        loadJavascriptBaseSupport();

        exportNamespaceToJS(mainInterface.getJsObject(), "", mainInterface.getInterfaceName());
        setupSubNamespace(mainInterface, "");
    }
}
