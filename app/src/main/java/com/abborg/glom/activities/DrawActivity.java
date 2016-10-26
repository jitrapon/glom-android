package com.abborg.glom.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.Toast;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.FileUtils;
import com.abborg.glom.utils.JavaScriptInterface;
import com.abborg.glom.views.CanvasView;
import com.abborg.glom.views.ColorPickerDialog;

import org.joda.time.DateTime;
import org.joda.time.Instant;

import java.io.InputStream;

import javax.inject.Inject;

/**
 * This activity handles live drawItem and drawing board item. It contains a custom view
 * CanvasView
 *
 * Created by jitrapon on 17/5/16.
 */
@SuppressLint("SetJavaScriptEnabled")
public class DrawActivity extends AppCompatActivity implements
        CanvasView.CanvasEventListener,
        Handler.Callback,
        ColorPickerDialog.OnColorSelectedListener {

    private static final String TAG = "DrawActivity";

    @Inject
    ApplicationState appState;

    @Inject
    DataProvider dataProvider;

    Circle circle;
    User user;
    DrawItem drawItem;

    CanvasView canvas;
    WebView webView;
    Handler handler;
    CoordinatorLayout rootView;
    View sizeAdjustView;
    SeekBar drawSizeBar;
    SeekBar eraserSizeBar;
    String savedFilePath;

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

    /********************************************
     * ACTIVITY LIFECYCLES
     *******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        circle = appState.getActiveCircle();
        user = appState.getActiveUser();
        handler = new Handler(this);
        shouldCreateRoom = getIntent().getAction().equals(getResources().getString(R.string.ACTION_CREATE_DRAWING));
        savedFilePath = getIntent().getStringExtra(getResources().getString(R.string.EXTRA_DRAWING_PATH));
        if (!shouldCreateRoom) {
            String id = getIntent().getStringExtra(getString(R.string.EXTRA_DRAWING_ID));
            for (BoardItem item : circle.getItems()) {
                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_DRAWING) {
                    drawItem = (DrawItem) item;
                    break;
                }
            }
        }
        else {
            drawItem = DrawItem.createDrawing(circle, DateTime.now(), DateTime.now());
        }

        setupView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dataProvider != null) dataProvider.setHandler(handler);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void finishWithResult(Intent intent) {
        if (intent != null) {
            setResult(RESULT_OK, intent);
        }
        else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawItem == null) super.onBackPressed();
        else {
            String name = TextUtils.isEmpty(drawItem.getName()) ? drawItem.getId() : drawItem.getName();
            final String savedFile = appState.getExternalFilesDir().getPath() + "/" + name + ".png";

            final Snackbar snackbar = Snackbar.make(
                    rootView, getString(R.string.notification_saving_bitmap), Snackbar.LENGTH_INDEFINITE);
            snackbar.show();

            // save the bitmap on a worker thread, then finishes the activity
            dataProvider.run(new Runnable() {
                @Override
                public void run() {
                    FileUtils.saveBitmapAsFile(canvas.getBitmap(), savedFile);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            snackbar.dismiss();

                            Intent intent = new Intent();
                            intent.putExtra(getString(R.string.EXTRA_DRAWING_ID), drawItem.getId());
                            intent.putExtra(getString(R.string.EXTRA_DRAWING_NAME), drawItem.getName());
                            intent.putExtra(getString(R.string.EXTRA_DRAWING_FILE), savedFile);
                            intent.putExtra(getString(R.string.EXTRA_DRAWING_TIME), Instant.now().getMillis());
                            intent.putExtra(getString(R.string.EXTRA_DRAWING_MODE), shouldCreateRoom);
                            finishWithResult(intent);
                        }
                    });
                }
            });
        }
    }

    private void setupView() {
        setContentView(R.layout.activity_draw);

        canvas = (CanvasView) findViewById(R.id.canvas_view);
        rootView = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
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

            if (!shouldCreateRoom) {
                canvas.setSavedPath(savedFilePath);
            }
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

    /********************************************
     * MENU BUTTON CALLBACKS
     *******************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_draw, menu);
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
        else if (id == R.id.action_change_size) {
            showSizeChangeDialog();
            return true;
        }
        else if (id == R.id.action_set_background) {
            openFileBrowser("image/*");
            return true;
        }
        else if (id == R.id.action_clear) {
            canvas.clear();
            canvas.setBackground(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openFileBrowser(String fileType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType(fileType);
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.intent_select_images)), Const.IMAGE_SELECTED_RESULT_CODE);
    }

    private void showColorPickerDialog(int initialColor) {
        ColorPickerDialog colorPickerDialog = new ColorPickerDialog(this, R.style.AlertDialogTheme, initialColor, this);
        colorPickerDialog.show();
    }

    private void showSizeChangeDialog() {
        sizeAdjustView = LayoutInflater.from(this).inflate(R.layout.dialog_change_draw_size, rootView, false);
        drawSizeBar = (SeekBar) sizeAdjustView.findViewById(R.id.draw_size_seekbar);
        eraserSizeBar = (SeekBar) sizeAdjustView.findViewById(R.id.eraser_size_seekbar);
        drawSizeBar.setMax(200);
        eraserSizeBar.setMax(200);
        drawSizeBar.setProgress((int)drawSize);
        eraserSizeBar.setProgress((int)eraserSize);
        drawSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                drawSize = (float) progress;
                canvas.setDrawSize(drawSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        eraserSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                eraserSize = (float) progress;
                canvas.setEraserSize(eraserSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Dialog dialog = new Dialog(this);
        Window window = dialog.getWindow();
        WindowManager.LayoutParams param = window.getAttributes();
        param.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        param.y = 200;
        window.setAttributes(param);
        dialog.setContentView(sizeAdjustView);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                drawSizeBar.setProgress((int)drawSize);
                eraserSizeBar.setProgress((int)eraserSize);
            }
        });
        dialog.show();
    }

    @Override
    public void onColorSelected(int color) {
        drawColor = color;
        canvas.setDrawColor(color);
        invalidateOptionsMenu();
    }

    /********************************************
     * CANVAS VIEW CALLBACKS
     *******************************************/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Const.IMAGE_SELECTED_RESULT_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    Uri uri = data.getData();
                    if (uri != null) {
                        Log.d(TAG, "Found URI of selected image " + uri.toString());
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Drawable background = Drawable.createFromStream(inputStream, uri.toString());
                        canvas.setBackground(background);
                    }
                    else Log.d(TAG, "URI is null)");
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        }
    }

    /* <start action-code>,<user-id>,<item-id>,<color>,<size>,<x>,<y> */
    @Override
    public void onDrawStart(int color, float size, float x, float y) {
        if (drawItem != null && isConnected) call("send", ACTION_DRAW_START, user.getId(), drawItem.getId(), color, size, x, y);
    }

    /* <erase action-code>,<user-id>,<item-id>,<size>,<x>,<y> */
    @Override
    public void onEraseStart(float size, float x, float y) {
        if (drawItem != null && isConnected) call("send", ACTION_ERASE_START, user.getId(), drawItem.getId(), size, x, y);
    }

    /* <draw action-code>,<user-id>,<item-id>,<x>,<y> */
    @Override
    public void onMove(float x, float y) {
        if (drawItem != null && isConnected) call("send", ACTION_MOVE, user.getId(), drawItem.getId(), x, y);
    }

    /* <end action-code>,<user-id>,<item-id> */
    @Override
    public void onUp() {
        if (drawItem != null && isConnected) call("send", ACTION_UP, user.getId(), drawItem.getId());
    }

    /**
     * Close connection to server
     */
    /* <leave action code>,<user id>,<item id> */
    @Override
    public void onExit() {
        if (drawItem != null && isConnected) {
            call("send", ACTION_LEAVE, user.getId(), drawItem.getId());
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
                    call("send", ACTION_CREATE, user.getId(), drawItem.getId(), circle.getId());
                }

                /* <join action code>,<user id>,<item id> */
                else {
                    call("send", ACTION_JOIN, user.getId(), drawItem.getId());
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
