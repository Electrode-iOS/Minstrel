package io.theholygrail.jsbridgetest;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import io.theholygrail.jsbridge.JSWebView;


public class MainActivity extends ActionBarActivity {

    public JSWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (JSWebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCod,String description, String failingUrl) {
                Log.d("webviewerror", description);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("webviewerror", "error: " + consoleMessage.message() + " line: " + consoleMessage.lineNumber());
                return false;
            }
        });

        webView.evaluateJavascript("var _lastCallback = nil", null);
        //webView.evaluateJavascript("android = {  };", null);
        //webView.evaluateJavascript("android.doSomething2 = function(arg0) { alert(String(arg0)); }", null);
        //webView.evaluateJavascript("android.doSomething = function(arg0) { alert(String(arg0)); }", null);
        //webView.evaluateJavascript("function doSomethingWithFunction(callback) { _lastCallback = callback; alert(\"Hello! I am an alert box!!\"); }", null);

        webView.addJavascriptInterface(new Bridge(this, webView), "android");
        webView.loadUrl("http://theholygrail.io/androidjs2.html");


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
