package com.abborg.glom.utils;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * Created by jitrapon on 18/5/16.
 */
public class JavaScriptInterface {

    Context context;

    public JavaScriptInterface(Context c) {
        context = c;
    }

    @JavascriptInterface
    public void message(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void log(String tag, String msg) {
        Log.d(tag, msg);
    }

    @JavascriptInterface
    public void error(String tag, String msg) {
        Log.e(tag, msg);
    }
}