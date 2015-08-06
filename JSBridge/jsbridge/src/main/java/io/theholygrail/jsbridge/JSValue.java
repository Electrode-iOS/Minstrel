package io.theholygrail.jsbridge;

import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.ValueCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class JSValue {
    private static final String TAG = JSValue.class.getSimpleName();
    protected Object mValue = null;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public JSValue(Object value) {
        if (value instanceof String) {
            // lets try to convert it to a json object..
            try {
                JSONObject jsonObject = new JSONObject((String)value);
                mValue = JSValue.decompose(jsonObject).mValue;
            } catch (JSONException e) {
                // not a json object, decompose the value itself
                mValue = JSValue.decompose(value).mValue;
            }
        } else {
            mValue = JSValue.decompose(value);
        }
    }

    // Validity checking ---------------------------------------------------------------------------

    public Boolean isValid() {
        return (mValue != null);
    }

    // Type checking -------------------------------------------------------------------------------

    public Boolean isFunction() {
        Boolean result = false;

        if (mValue instanceof String) {
            String stringValue = (String)mValue;
            if (JsValueUtil.parseQuotes(stringValue).startsWith("function:")) {
                result = true;
            }
        }

        return result;
    }

    public Boolean isNull() {
        return (mValue == null);
    }

    public Boolean isString() {
        return (mValue instanceof String && !isFunction());
    }

    public Boolean isBoolean() {
        return (mValue instanceof Boolean);
    }

    public Boolean isNumber() {
        return (mValue instanceof Integer || mValue instanceof Double);
    }

    public Boolean isInteger() {
        return (mValue instanceof Integer);
    }

    public Boolean isDouble() {
        return (mValue instanceof Double);
    }

    public Boolean isObject() {
        return (mValue instanceof Map);
    }

    public Boolean isMap() {
        // convenience in case the naming is confusing.
        return isObject();
    }

    public Boolean isArray() {
        return (mValue instanceof List);
    }

    public Boolean isList() {
        return isArray();
    }

    // Value retrieval -----------------------------------------------------------------------------

    public String stringValue() {
        String result = null;

        if (mValue != null) {
            if (mValue instanceof String && !isFunction()) {
                result = (String)mValue;
            } else if (mValue instanceof Integer || mValue instanceof Double) {
                Number numberValue = (Number)mValue;
                result = numberValue.toString();
            } else {
                // TODO: handle booleans and unhandled values explicitly
                result = String.valueOf(mValue);
            }
        }

        return result;
    }

    public Integer integerValue() {
        Integer result = null;

        if (mValue != null) {
            if (mValue instanceof Integer) {
                result = (Integer)mValue;
            } else if (mValue instanceof Double) {
                Number numberValue = (Number)mValue;
                result = numberValue.intValue();
            } else if (mValue instanceof String) {
                String stringValue = (String)mValue;
                result = Integer.valueOf(stringValue);
            }
        }

        return result;
    }

    public Double doubleValue() {
        Double result = null;

        if (mValue != null) {
            if (mValue instanceof Double) {
                result = (Double)mValue;
            } else if (mValue instanceof Integer) {
                Number numberValue = (Number)mValue;
                result = numberValue.doubleValue();
            } else if (mValue instanceof String) {
                String stringValue = (String)mValue;
                result = Double.valueOf(stringValue);
            }
        }

        return result;
    }

    public Boolean booleanValue() {
        Boolean result = null;

        if (mValue != null) {
            if (mValue instanceof Integer || mValue instanceof Double) {
                Number numberValue = (Number)mValue;
                result = Boolean.valueOf(numberValue.toString());
            } else if (mValue instanceof String) {
                String stringValue = (String)mValue;
                result = Boolean.valueOf(stringValue);
            }
        }

        return result;
    }

    public Map mapValue() {
        Map result = null;

        if (mValue != null && mValue instanceof Map) {
            result = (Map)mValue;
        }

        return result;
    }

    public List listValue() {
        List result = null;

        if (mValue != null && mValue instanceof List) {
            result = (List)mValue;
        }

        return result;
    }

    public String functionSourceValue() {
        String result = null;

        if (isFunction()) {
            String[] parts = splitFunctionValues();

            if (parts.length == 3) {
                // source code is base64 encoded in parts[2].
                String base64String = parts[2];
                // decode it back to it's javascript origins.
                byte[] data = Base64.decode(base64String, Base64.DEFAULT);

                try {
                    result = new String(data, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public String functionIDValue() {
        String result = null;

        if (isFunction()) {
            String[] parts = splitFunctionValues();

            if (parts.length == 3) {
                // function ID is in parts[1].
                result = parts[1];
            }
        }

        return result;
    }

    public String javascriptStringValue() {
        String result;

        if (isFunction()) {
            result = "__functionCache[" + functionIDValue() + "]";
        } else if (isObject()) {
            result = "{";

            Map map = mapValue();
            Set keys = map.keySet();

            for (Object s: keys) {
                if (result.length() != 1) {
                    result+= ",";
                }
                Object obj = map.get(s);
                String value;
                if (obj instanceof JSValue) {
                    value = ((JSValue) obj).javascriptStringValue();
                } else {
                    value = obj.toString();
                }
                result += s + ": " + value;
            }

            result += " }";
        } else if (isArray()) {
            result = "[";

            List list = listValue();
            int index = 0;

            for (Object obj : list) {
                String value;
                if (obj instanceof JSValue) {
                    value = ((JSValue)obj).javascriptStringValue();
                } else {
                    value = obj.toString();
                }
                if (index == 0) {
                    result += value;
                } else {
                    result += ", " + value;
                }
                index++;
            }

            result += "]";
        } else if (isString()) {
            result = "'" + stringValue().replace("'", "\'") + "'";
        } else {
            result = stringValue();
        }

        return result;
    }

    public void callFunction(final JSWebView webView, Object args[], final ValueCallback<JSValue> resultCallback) {
        if (!isFunction()) {
            return;
        }

        // Convert arguments into JSValues we can stringify
        JSValue jsArg;
        StringBuilder argumentBuilder = new StringBuilder();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                jsArg = decompose(args[i]);
                argumentBuilder.append(jsArg.javascriptStringValue());

                if (i != args.length -1) {
                    argumentBuilder.append(',');
                }
            }
        }

        // setup our result expectation if we need to.
        if (resultCallback != null) {
            webView.bridgeSupport.expectResult(new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    resultCallback.onReceiveValue(new JSValue(value));
                }
            });
        }


        final String arguments = argumentBuilder.toString();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // This is strictly to retrieve the result value.
                String jsFunction = javascriptStringValue();
                // 1. setup our call
                // 2. make sure __lastResult is cleared so we don't get a previous value.
                // 3. make said call...
                // 4. get result and pass it back to native.
                webView.executeJavascript("var __lastCallback = " + jsFunction + ";\n" +
                        "var __lastResult = null;\n" +
                        "var __lastResult = __lastCallback(" + arguments + ");\n" +
                        "__bridgeSupport.passResult(__lastResult);");
            }
        });
    }

    // Protected stuff -----------------------------------------------------------------------------

    private JSValue() {
        mValue = null;
    }

    protected static JSValue decompose(Object object) {
        JSValue result;

        if (object instanceof JSONArray) {
            // turn it into a normal array of JSValue's.
            JSONArray jsonArray = (JSONArray)object;
            List<Object> array = new ArrayList<>();

            // old skool iteration, we want to preserve the order.
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    Object item = jsonArray.get(i);
                    array.add(JSValue.decompose(item));
                } catch (JSONException e) {
                    // .. do nothing.
                }
            }
            JSValue jsValue = new JSValue();
            jsValue.mValue = array;
            result = jsValue;
        } else if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject)object;
            Map<String, Object> map = new HashMap<>();

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    Object value = jsonObject.get(key);
                    map.put(key, JSValue.decompose(value));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            JSValue jsValue = new JSValue();
            jsValue.mValue = map;
            result = jsValue;
        } else if (object instanceof JSValue) {
            result = (JSValue)object;
        } else {
            JSValue jsValue = new JSValue();
            jsValue.mValue = object; // can be null

            result = jsValue;
        }

        return result;
    }

    private String[] splitFunctionValues() {
        String result[] = null;

        if (isFunction()) {
            // "function:<id>:<base64 data>"
            String value = (String) mValue;
            result = value.split(":");
        }

        return result;
    }
}
