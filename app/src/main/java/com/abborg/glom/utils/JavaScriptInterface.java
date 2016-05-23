package com.abborg.glom.utils;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.abborg.glom.Const;

/**
 * Bridge between Android and Javscript. All function calls are done in a thread 'JavaBridge'.
 *
 * Created by jitrapon on 18/5/16.
 */
public class JavaScriptInterface {

    Handler handler;

    public JavaScriptInterface(Handler h) {
        handler = h;
    }

    @JavascriptInterface
    public void log(String tag, String msg) {
        Log.d(tag, msg);
    }

    @JavascriptInterface
    public void error(String tag, String msg) {
        Log.e(tag, msg);
    }

    @JavascriptInterface
    public void onConnected() {
        if (handler != null)
            handler.sendMessage(handler.obtainMessage(Const.MSG_SOCKET_CONNECTED));
    }

    @JavascriptInterface
    public void onDisconnected() {
        if (handler != null)
            handler.sendMessage(handler.obtainMessage(Const.MSG_SOCKET_DISCONNECTED));
    }

    /** Data will be in the format of '[action-code],[data payload],[data payload]...' **/
    @JavascriptInterface
    public void onReceived(String data) {
        if (handler != null && !TextUtils.isEmpty(data)) {
            handler.sendMessage(handler.obtainMessage(Const.MSG_SOCKET_DATA_RECEIVED, data));
        }
    }
}