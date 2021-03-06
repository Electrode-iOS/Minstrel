package io.theholygrail.jsbridge;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by brandon on 4/28/15.
 */
public class JSWebView extends WebView {
    private Context mContext = null;
    private int mFunctionCacheLimit = 200;

    public BridgeSupport bridgeSupport = null;

    public JSWebView(Context context) {
        super(context);
        mContext = context;
        setupDefaults();
    }

    public JSWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setupDefaults();
    }

    public JSWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setupDefaults();
    }

    @Override
    public void addJavascriptInterface(Object obj, String interfaceName) {
        super.addJavascriptInterface(obj, "__"+interfaceName);

        loadJavascriptBaseSupport();
        loadJavascriptSupportBits(interfaceName);
        exportObjectMethodsToJS(obj, interfaceName);
    }

    public void executeJavascript(String javascript) {
        // Useful for seeing the injected javascript.
        //Log.d("javascript", javascript);

        /*
        The WebView.evaluateJavascript() method would be a much better choice, but it's not
        available until a later SDK than we support.
         */
        loadUrl("javascript:" + Uri.encode(javascript));
    }

    public void setFunctionCacheLimit(int count) {
        mFunctionCacheLimit = count;
        executeJavascript("__functionIDLimit = " + mFunctionCacheLimit + ";");
    }

    // Private stuff -------------------------------------------------------------------------------

    public class BridgeSupport {
        private Context mContext = null;
        private ValueCallback<String> mResultCallback = null;
        private final Lock resultLock = new ReentrantLock();

        BridgeSupport(Context context) {
            mContext = context;
        }

        protected void expectResult(ValueCallback<String> resultCallback) {
            resultLock.lock();
            mResultCallback = resultCallback;
        }

        @JavascriptInterface
        public void passResult(String result) {
            mResultCallback.onReceiveValue(result);
            resultLock.unlock();
        }
    }

    private void exportObjectMethodsToJS(Object obj, String interfaceName) {
        Method methods[] = obj.getClass().getMethods();

        for (Method method : methods) {
            if (hasOnlyStringParameters(method)) {
                // export it.
                String methodName = method.getName();
                String parameterString = generateParameterString(method);
                String callString = generateCallString(method);

                String jsString = interfaceName + "." + methodName + " = function(" + parameterString + ") { "
                        + "__"+ interfaceName + "." + methodName + "("+callString+"); }";
                executeJavascript(jsString);
            }
        }
    }

    private Boolean hasOnlyStringParameters(Method method) {
        Boolean result = true;
        Class paramClasses[] = method.getParameterTypes();
        for (int i = 0; i < paramClasses.length; i++) {
            String typeName = paramClasses[i].getSimpleName();
            if (!typeName.equals("String")) {
                result = false;
                break;
            }
        }
        return result;
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
        getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d("JSWebViewError", "Code: " + errorCode + "Error: " + description + " Url: " + failingUrl);
            }
        });
        setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {
                Log.d("JSWebViewError", "Error: " + consoleMessage.message() + " line: " + consoleMessage.lineNumber());
                return false;
            }
        });
    }

    private void loadJavascriptSupportBits(String interfaceName) {
        executeJavascript(interfaceName + " = { };");
    }

    private void loadJavascriptBaseSupport() {
        if (bridgeSupport == null) {
            bridgeSupport = new BridgeSupport(mContext);

            // add our return handling stuff.
            addJavascriptInterface(bridgeSupport, "__bridgeSupport");

            // add our function passing stuff.
            executeJavascript("__functionIDCounter = 0, __functionCache = { };");

            // set the limit of our function cache to the default.
            // this is used in the javascript injection below.
            setFunctionCacheLimit(mFunctionCacheLimit);

            // would be nice to load this from a file contained in the .jar instead?
            executeJavascript("function valueToBridgeString(obj, embedded, cache) {\n" +
                                    "    // recursion sanity check\n" +
                                    "    if (!cache) cache = [];\n" +
                                    "    if (cache.indexOf(obj) >= 0) {\n" +
                                    "        throw new Error('Can\\'t do circular references');\n" +
                                    "    } else {\n" +
                                    "        cache.push(obj);\n" +
                                    "    }\n" +
                                    "\n" +
                                    "    var rtn;\n" +
                                    "    switch (typeof obj) {\n" +
                                    "        case 'object':\n" +
                                    "            if (!obj) {\n" +
                                    "                rtn = JSON.stringify(obj);\n" +
                                    "            } else if (Array.isArray(obj)) {\n" +
                                    "                rtn = '[' + obj.map(function(item) {\n" +
                                    "                    return valueToBridgeString(item, true, cache);\n" +
                                    "                }).join(',') + ']';\n" +
                                    "            } else {\n" +
                                    "                var rtn = '{';\n" +
                                    "                for (var name in obj) {\n" +
                                    "                    if (obj.hasOwnProperty(name)) {\n" +
                                    "                        if (rtn.length > 1) {\n" +
                                    "                            rtn += ',';\n" +
                                    "                        }\n" +
                                    "                        rtn += JSON.stringify(name);\n" +
                                    "                        rtn += ': ';\n" +
                                    "                        rtn += valueToBridgeString(obj[name], true, cache);\n" +
                                    "                    }\n" +
                                    "                }\n" +
                                    "                rtn += '}';\n" +
                                    "            }\n" +
                                    "            break;\n" +
                                    "        case 'function':\n" +
                                    "            rtn = '\\\"function:' + __functionIDCounter.toString() + ':' + btoa(obj.toString()) + '\\\"';\n" +
                                    "            __functionCache[__functionIDCounter] = obj;\n" +
                                    "            __functionIDCounter++;\n" +
                                    "            if (__functionIDCounter > __functionIDLimit) { __functionIDCounter = 0; }\n" +
                                    "            break;\n" +
                                    "        default:\n" +
                                    "            if (obj === undefined) {\n" +
                                    "                rtn = 'null';\n" +
                                    "            } else {" +
                                    "                rtn = JSON.stringify(obj);\n" +
                                    "            }\n" +
                                    "    }\n" +
                                    "    if (!embedded) {\n" +
                                    "        rtn = '{\"__rawValue\": ' + rtn + '}';\n" +
                                    "    }\n" +
                                    "    return rtn;\n" +
                                    "}\n");
        }
    }
}

