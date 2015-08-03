package io.theholygrail.jsbridgetest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;

import io.theholygrail.jsbridge.JSValue;
import io.theholygrail.jsbridge.JSWebView;

/**
 * Created by brandon on 4/27/15.
 */

public class Bridge {
    final ActionBarActivity mContext;
    JSWebView mWebView;

    Bridge(ActionBarActivity context, JSWebView webView) {
        mContext = context;
        mWebView = webView;
    }

    @JavascriptInterface
    public void doSomething2(String param) {
        JSValue valueParam = new JSValue(param);

        if (valueParam.isFunction()) {
            Object args[] = {5};

            String value = valueParam.javascriptStringValue();
            //Log.d("javascriptvalue", value);

            //Log.d("javascriptparamsource", valueParam.functionSourceValue());

            valueParam.callFunction(mWebView, args, new ValueCallback<JSValue>() {
                @Override
                public void onReceiveValue(final JSValue value) {
                    if (value.isValid()) {
                        // make sure we do this UI stuff on the UI thread y0.
                        mContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                                alertDialog.setTitle("The value is ...");
                                alertDialog.setMessage(value.stringValue());
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                alertDialog.show();
                            }
                        });
                    }
                }
            });

        }
    }
}
