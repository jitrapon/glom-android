package com.abborg.glom.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.NoteItem;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.JavaScriptInterface;
import com.abborg.glom.views.DrawCanvasView;

import org.joda.time.DateTime;

/**
 * Created by jitrapon on 17/5/16.
 */
public class DrawActivity extends AppCompatActivity implements DrawCanvasView.DrawCanvasChangeListener, Handler.Callback {

    private static final String TAG = "DrawActivity";

    /** Item state information **/
    AppState appState;
    Circle circle;
    User user;
    DataUpdater dataUpdater;
    NoteItem note;

    DrawCanvasView canvas;
    WebView webView;
    Handler handler;
    CoordinatorLayout rootView;

    /* JS Script path */
    private static final String JS_PATH = Const.ASSETS_FOLDER + "draw.html";
    private static final String APP_ID = "1";

    /* App-specific action opcode for protocol in networking */
    private static final String DELIMITER = ",";
    private static final int ACTION_CREATE = 0;
    private static final int ACTION_LEAVE = 1;
    private static final int ACTION_JOIN = 2;
    private static final int ACTION_SHOW_MESSAGE = 3;
    private static final int ACTION_DRAW = 4;
    private static final int ACTION_NEW_POINT = 5;
    private static final int ACTION_ERASE = 6;

    private boolean shouldCreateRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appState = AppState.getInstance();
        if (appState == null || appState.getDataUpdater() == null || getIntent() == null) {
            finish();
        }
        circle = appState.getActiveCircle();
        user = appState.getActiveUser();
        dataUpdater = appState.getDataUpdater();
        handler = new Handler(this);
        shouldCreateRoom = getIntent().getAction().equals(getResources().getString(R.string.ACTION_CREATE_NOTE));
        if (!shouldCreateRoom) {
            String id = getIntent().getStringExtra(getString(R.string.EXTRA_NOTE_ID));
            for (BoardItem item : circle.getItems()) {
                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_NOTE) {
                    note = (NoteItem) item;
                    break;
                }
            }
        }

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
                webView.addJavascriptInterface(new JavaScriptInterface(handler), "App");
                webView.setWebViewClient(new WebViewClient() {

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        Log.d(TAG, "Running javascript function");
                        call("run", Const.SERVER_APP_URL + "/" + APP_ID);
                    }
                });
                webView.loadUrl(JS_PATH);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        canvas = (DrawCanvasView) findViewById(R.id.canvas_view);
        if (canvas != null) canvas.setEventListener(this);
        rootView = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    }

    private void call(String functionName, Object... params) {
        StringBuilder paramBuilder = new StringBuilder();
        if (params == null || params.length == 0) {
            webView.loadUrl("javascript:" + functionName + "()");
            return;
        }

        for (Object param : params) {
            paramBuilder.append(param).append(DELIMITER);
        }
        String action = paramBuilder.toString();
        action = action.substring(0, action.length() - 1);
        webView.loadUrl("javascript:" + functionName + "(\"" + action + "\")");
    }

    /********************************************
     * CANVAS VIEW CALLBACKS
     *******************************************/

    /**
     * Sending info to server about drawing location
     */
    @Override
    public void onDraw(boolean moved, float x, float y) {
        if (note != null)
            call("send", moved ? ACTION_NEW_POINT : ACTION_DRAW, user.getId(), note.getId(), x, y);
    }

    /**
     * Close connection to server
     */
    @Override
    public void onExit() {
        if (note != null) {
            call("send", ACTION_LEAVE, user.getId(), note.getId());
            call("stop");
        }
    }

    /********************************************
     * GAME STATE EVENT HANDLER
     *******************************************/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            /* Called when a socket connection has been established successfully */
            /* Action create room payload: <create action code>,<user id>,<item id>,<circle id> */
            /* Action join payload: <join action code>,<user id>,<item id> */
            case Const.MSG_SOCKET_CONNECTED:
//                Toast.makeText(getApplicationContext(),
//                        getResources().getString(R.string.notification_app_connected), Toast.LENGTH_LONG).show();
                if (shouldCreateRoom) {
                    note = NoteItem.createNote(circle, DateTime.now(), DateTime.now());
                    call("send", ACTION_CREATE, user.getId(), note.getId(), circle.getId());
                }
                else {
                    call("send", ACTION_JOIN, user.getId(), note.getId());
                }
                break;

             /* Called when a socket connection has been closed successfully */
            case Const.MSG_SOCKET_DISCONNECTED:
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.notification_app_disconnected), Toast.LENGTH_LONG).show();
                break;

            /* Called when a data payload has been received */
            case Const.MSG_SOCKET_DATA_RECEIVED:
                try {
                    String data = (String) msg.obj;
                    String[] rawData = data.split(DELIMITER);
                    if (rawData.length > 0) {
                        int actionCode = Integer.parseInt(rawData[0]);
                        switch (actionCode) {
                            case ACTION_SHOW_MESSAGE:
                                String message = rawData.length > 1 ? rawData[1] : null;
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                break;
                            case ACTION_JOIN: {
                                String userId = rawData[1];
                                Snackbar.make(
                                        rootView,
                                        String.format(getResources().getString(R.string.notification_user_joined), userId),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                                break;
                            }
                            case ACTION_LEAVE: {
                                String userId = rawData[1];
                                Snackbar.make(
                                        rootView,
                                        String.format(getResources().getString(R.string.notification_user_left), userId),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                                break;
                            }
                            case ACTION_NEW_POINT: {
                                float x = Float.parseFloat(rawData[3]);
                                float y = Float.parseFloat(rawData[4]);
                                canvas.draw(true, x, y);
                                break;
                            }
                            case ACTION_DRAW: {
                                float x = Float.parseFloat(rawData[3]);
                                float y = Float.parseFloat(rawData[4]);
                                canvas.draw(false, x, y);
                                break;
                            }
                        }
                    }
                }
                catch (Exception ex) {
                    Log.d(TAG, ex.getMessage());
                }
                break;
            default: return false;
        }

        return true;
    }
}
