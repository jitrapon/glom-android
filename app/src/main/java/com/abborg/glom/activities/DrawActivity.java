package com.abborg.glom.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.JavaScriptInterface;
import com.abborg.glom.views.DrawCanvasView;

/**
 * Created by jitrapon on 17/5/16.
 */
public class DrawActivity extends AppCompatActivity implements DrawCanvasView.DrawCanvasChangeListener {

    private static final String TAG = "DrawActivity";

    /** Circle state information **/
    AppState appState;
    Circle circle;
    User user;
    DataUpdater dataUpdater;

    WebView webView;

    /* JS Script path */
    private static final String JS_PATH = Const.ASSETS_FOLDER + "draw.html";
    private static final String APP_ID = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appState = AppState.getInstance();
        if (appState == null || appState.getDataUpdater() == null) {
            finish();
        }
        circle = appState.getActiveCircle();
        user = appState.getActiveUser();
        dataUpdater = appState.getDataUpdater();

        setContentView(R.layout.activity_draw);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.title_activity_note_unsaved));
        }

        try {
            Log.d(TAG, "Initializing web view to run javascript using " + JS_PATH);
            webView = (WebView) findViewById(R.id.web_view);
            if (webView != null) {
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setAllowFileAccessFromFileURLs(true); //Maybe you don't need this rule
                webSettings.setAllowUniversalAccessFromFileURLs(true);
                webView.addJavascriptInterface(new JavaScriptInterface(this), "App");
                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        Log.d(TAG, "Running javascript function");
                        view.loadUrl("javascript:run(\"" + Const.SERVER_APP_URL + "/" + APP_ID + "\")");
                    }
                });
                webView.loadUrl(JS_PATH);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        DrawCanvasView canvas = (DrawCanvasView) findViewById(R.id.canvas_view);
        if (canvas != null) canvas.setEventListener(this);
    }

    @Override
    public void onDraw(float x, float y) {
        webView.loadUrl("javascript:send(" + x + ", " + y + ")");
    }

    @Override
    public void onFinished() {
        webView.loadUrl("javascript:stop()");
    }
}
