package com.abborg.glom.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
import com.abborg.glom.views.CanvasView;
import com.abborg.glom.views.ColorPickerDialog;

import org.joda.time.DateTime;

/**
 * This activity handles live note and drawing board item. It contains a custom view
 * CanvasView
 *
 * Created by jitrapon on 17/5/16.
 */
@SuppressLint("SetJavaScriptEnabled")
public class DrawActivity extends AppCompatActivity implements
        CanvasView.CanvasEventListener, Handler.Callback, ColorPickerDialog.OnColorSelectedListener {

    private static final String TAG = "DrawActivity";

    /** Item state information **/
    AppState appState;
    Circle circle;
    User user;
    DataUpdater dataUpdater;
    NoteItem note;

    CanvasView canvas;
    WebView webView;
    Handler handler;
    CoordinatorLayout rootView;

    private float eraserSize = 70f;
    private float drawSize = 7f;
    private int drawColor = Color.BLUE;

    private boolean isConnected = false;

    MenuItem eraserMenuItem;
    MenuItem pencilMenuItem;
    MenuItem colorPickerItem;

    /* JS Script path */
    private static final String JS_PATH = Const.ASSETS_FOLDER + "draw.html";
    private static final String APP_ID = "1";

    /* App-specific action opcode for protocol in networking */
    private static final String DELIMITER = ",";
    private static final int ACTION_CREATE = 0;
    private static final int ACTION_LEAVE = 1;
    private static final int ACTION_JOIN = 2;
    private static final int ACTION_SHOW_MESSAGE = 3;
    private static final int ACTION_MOVE = 4;
    private static final int ACTION_DRAW_START = 5;
    private static final int ACTION_UP = 6;
    private static final int ACTION_ERASE_START = 7;

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

        setupView();
    }

    private void setupView() {
        setContentView(R.layout.activity_draw);

        canvas = (CanvasView) findViewById(R.id.canvas_view);
        rootView = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            if (note == null || TextUtils.isEmpty(note.getName()))
                getSupportActionBar().setTitle(getString(R.string.title_activity_note_unsaved));
            else getSupportActionBar().setTitle(note.getName());
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

        if (canvas != null) {
            canvas.setEventListener(this);
            canvas.setDrawColor(drawColor);
            canvas.setDrawSize(drawSize);
            canvas.setEraserSize(eraserSize);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note, menu);
        eraserMenuItem = menu.findItem(R.id.action_erase);
        pencilMenuItem = menu.findItem(R.id.action_grease_pencil);
        colorPickerItem = menu.findItem(R.id.action_pick_color);
        colorPickerItem.getIcon().setColorFilter(drawColor, PorterDuff.Mode.MULTIPLY);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        eraserMenuItem.setIcon(canvas.isEraserActive() ? R.drawable.ic_action_erase_active : R.drawable.ic_action_erase);
        pencilMenuItem.setIcon(canvas.isDrawActive() ? R.drawable.ic_grease_pencil_active : R.drawable.ic_action_grease_pencil);
        colorPickerItem.getIcon().setColorFilter(drawColor, PorterDuff.Mode.MULTIPLY);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_erase) {
            canvas.setEraserActive(null, eraserSize);
            invalidateOptionsMenu();
            return true;
        }
        else if (id == R.id.action_grease_pencil) {
            canvas.setDrawActive(null, drawColor, drawSize);
            invalidateOptionsMenu();
            return true;
        }
        else if (id == R.id.action_pick_color) {
            showColorPickerDialog(drawColor);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showColorPickerDialog(int initialColor) {
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, initialColor, this);
        colorPickerDialog.show();
    }

    /********************************************
     * DRAW TOOLS CHANGE LISTENERS
     *******************************************/

    @Override
    public void onColorSelected(int color) {
        drawColor = color;
        canvas.setDrawColor(color);
        invalidateOptionsMenu();
    }

    /********************************************
     * CANVAS VIEW CALLBACKS
     *******************************************/

    /* <start action-code>,<user-id>,<item-id>,<color>,<size>,<x>,<y> */
    @Override
    public void onDrawStart(int color, float size, float x, float y) {
        if (note != null && isConnected) call("send", ACTION_DRAW_START, user.getId(), note.getId(), color, size, x, y);
    }

    /* <erase action-code>,<user-id>,<item-id>,<size>,<x>,<y> */
    @Override
    public void onEraseStart(float size, float x, float y) {
        if (note != null && isConnected) call("send", ACTION_ERASE_START, user.getId(), note.getId(), size, x, y);
    }

    /* <draw action-code>,<user-id>,<item-id>,<x>,<y> */
    @Override
    public void onMove(float x, float y) {
        if (note != null && isConnected) call("send", ACTION_MOVE, user.getId(), note.getId(), x, y);
    }

    /* <end action-code>,<user-id>,<item-id> */
    @Override
    public void onUp() {
        if (note != null && isConnected) call("send", ACTION_UP, user.getId(), note.getId());
    }

    /**
     * Close connection to server
     */
    /* <leave action code>,<user id>,<item id> */
    @Override
    public void onExit() {
        if (note != null && isConnected) {
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
            case Const.MSG_SOCKET_CONNECTED:
                isConnected = true;

                /* Action create room payload: <create action code>,<user id>,<item id>,<circle id> */
                if (shouldCreateRoom) {
                    note = NoteItem.createNote(circle, DateTime.now(), DateTime.now());
                    call("send", ACTION_CREATE, user.getId(), note.getId(), circle.getId());
                }

                /* <join action code>,<user id>,<item id> */
                else {
                    call("send", ACTION_JOIN, user.getId(), note.getId());
                }
                break;

             /* Called when a socket connection has been closed successfully */
            case Const.MSG_SOCKET_DISCONNECTED:
                isConnected = false;
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

                            /* <message action code>,<message payload> */
                            case ACTION_SHOW_MESSAGE:
                                String message = rawData.length > 1 ? rawData[1] : null;
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                break;

                            /* <join action code>,<user id>,<item id> */
                            case ACTION_JOIN: {
                                String userId = rawData[1];
                                Snackbar.make(
                                        rootView,
                                        String.format(getResources().getString(R.string.notification_user_joined), userId),
                                        Snackbar.LENGTH_LONG)
                                        .show();
                                break;
                            }

                            /* <leave action code>,<user id>,<item id> */
                            case ACTION_LEAVE: {
                                String userId = rawData[1];
                                Snackbar.make(
                                        rootView,
                                        String.format(getResources().getString(R.string.notification_user_left), userId),
                                        Snackbar.LENGTH_LONG)
                                        .show();

                                canvas.removePath(userId);
                                break;
                            }

                            /* <draw action-code>,<user-id>,<item-id>,<color>,<size>,<x>,<y> */
                            case ACTION_DRAW_START: {
                                String userId = rawData[1];
                                int color = Integer.parseInt(rawData[3]);
                                float size = Float.parseFloat(rawData[4]);
                                float x = Float.parseFloat(rawData[5]);
                                float y = Float.parseFloat(rawData[6]);
                                canvas.setDrawActive(userId, color, size);
                                canvas.start(userId, x, y);
                                break;
                            }

                            /* <erase action-code>,<user-id>,<item-id>,<size>,<x>,<y> */
                            case ACTION_ERASE_START: {
                                String userId = rawData[1];
                                float size = Float.parseFloat(rawData[3]);
                                float x = Float.parseFloat(rawData[4]);
                                float y = Float.parseFloat(rawData[5]);
                                canvas.setEraserActive(userId, size);
                                canvas.start(userId, x, y);
                                break;
                            }

                            /* <move action-code>,<user-id>,<item-id>,<x>,<y> */
                            case ACTION_MOVE: {
                                String userId = rawData[1];
                                float x = Float.parseFloat(rawData[3]);
                                float y = Float.parseFloat(rawData[4]);
                                canvas.move(userId, x, y);
                                break;
                            }

                            /* <up action-code>,<user-id>,<item-id> */
                            case ACTION_UP: {
                                String userId = rawData[1];
                                canvas.up(userId);
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
