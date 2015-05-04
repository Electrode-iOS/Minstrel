package io.theholygrail.jsbridge;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by brandon on 4/28/15.
 */
public class JSWebView extends WebView {
    private Context mContext = null;

    public BridgeSupport bridgeSupport = null;

    public JSWebView(Context context) {
        super(context);
    }

    public JSWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JSWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // the two constructors above end up calling this guy, so just set it up once.
        mContext = context;
    }

    @Override
    public void addJavascriptInterface(Object obj, String interfaceName) {
        super.addJavascriptInterface(obj, "__"+interfaceName);

        loadJavascriptBaseSupport();
        loadJavascriptSupportBits(interfaceName);
        exportObjectMethodsToJS(obj, interfaceName);
    }

    public void executeJavascript(String javascript) {
        loadUrl("javascript:" + Uri.encode(javascript));
    }

    // Private stuff -------------------------------------------------------------------------------

    /*
    This is strictly to retrieve the result value.  The WebView.evaluateJavascript() method would be
    a much better choice, but it's not available until a later SDK than we support.
     */
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

                String jsString = interfaceName+"."+methodName+" = function("+parameterString+") { __"+interfaceName+"."+methodName+"("+callString+"); }";
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

    private void loadJavascriptSupportBits(String interfaceName) {
        executeJavascript(interfaceName + " = { };");
    }

    private void loadJavascriptBaseSupport() {
        if (bridgeSupport == null) {
            bridgeSupport = new BridgeSupport(mContext);

            addJavascriptInterface(bridgeSupport, "__bridgeSupport");

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
                                    "            rtn = '\\\"function::' + btoa(obj.toString()) + '\\\"';\n" +
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

