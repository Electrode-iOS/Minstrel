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

/**
 * Created by brandon on 4/28/15.
 */

public class JSValue {
    protected Object mValue = null;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public JSValue(Object value) {
        if (value instanceof String) {
            // lets try to convert it to a json object..
            try {
                String stringValue = (String)value;
                JSONObject jsonObject = new JSONObject(stringValue);
                // hmm.. we didn't throw an exception.. get our value and decompose it.
                Object rawValue = jsonObject.get("__rawValue");
                // decompose turns it into a JSValue.. this is only relevant at
                // the top level to just bring that value over.
                mValue = JSValue.decompose(rawValue).mValue;
            } catch (JSONException e) {
                e.printStackTrace();
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
            if (stringValue.indexOf("function:") == 0) {
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

        return (Integer)result;
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

    public String javascriptStringValue() {
        String result = null;

        if (isFunction()) {
            // rip the "function:" part off
            String base64String = ((String)mValue).replace("function:", "");
            // decode it back to it's javascript origins.
            byte[] data = Base64.decode(base64String, Base64.DEFAULT);

            try {
                result = new String(data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else if (isObject()) {
            result = "{";

            Map map = mapValue();
            Set keys = map.keySet();

            for (Object s: keys) {
                Object obj = map.get(s);
                String value = "";
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
        JSValue result = null;

        if (!isFunction()) {
            return;
        }

        JSONArray jsonArgs = null;
        try {
            jsonArgs = new JSONArray(args);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // convert our args into a JSValue that we can string-ify.
        JSValue jsArgs = decompose(jsonArgs);
        String argsString = jsArgs.javascriptStringValue();

        // setup our result expectation if we need to.
        if (resultCallback != null) {
            webView.bridgeSupport.expectResult(new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    resultCallback.onReceiveValue(new JSValue(value));
                }
            });
        }

        final String arguments = argsString;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // This is strictly to retrieve the result value.

                String jsFunction = javascriptStringValue();

                // setup our call
                webView.executeJavascript("var __lastCallback = " + jsFunction);
                // make sure __lastResult is cleared so we don't get a previous value.
                webView.executeJavascript("var __lastResult = null;");
                // make said call...
                webView.executeJavascript("var __lastResult = __lastCallback(" + arguments + ");");
                // get result...
                webView.executeJavascript("__bridgeSupport.passResult(__lastResult);");
            }
        });
    }


    // Protected stuff -----------------------------------------------------------------------------

    private JSValue() {
        mValue = null;
    }

    protected static JSValue decompose(Object object) {
        JSValue result = null;

        if (object instanceof JSONArray) {
            // turn it into a normal array of JSValue's.
            JSONArray jsonArray = (JSONArray)object;
            List<Object> array = new ArrayList<Object>();

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
            Map<String, Object> map = new HashMap<String, Object>();

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
            if (object.equals(JSONObject.NULL))
                jsValue.mValue = null;
            else
                jsValue.mValue = object;
            result = jsValue;
        }

        return result;
    }

}
