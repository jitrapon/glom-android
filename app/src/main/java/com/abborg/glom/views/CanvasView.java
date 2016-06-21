package com.abborg.glom.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.ThumbnailUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.abborg.glom.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Main drawable custom view that provides users with canvas to draw on.
 *
 * Created by jitrapon on 17/5/16.
 */
public class CanvasView extends View {

    private static final String USER_PATH = "User";
    private static final String TAG = "CanvasView";

    /** Stores a list of user painted paths **/
    private Map<String, DrawPath> paths;
    private Bitmap bitmap;
    private Bitmap loadedBitmap;
    private Canvas bitmapCanvas;

    private int backgroundColor = Color.WHITE;
    private float eraserSize = 70f;
    private float drawSize = 7f;
    private int drawColor = Color.BLUE;

    private CanvasEventListener listener;

    private String savedPath;

    public interface CanvasEventListener {

        void onDrawStart(int color, float size, float x, float y);

        void onEraseStart(float size, float x, float y);

        void onMove(float x, float y);

        void onUp();

        void onExit();
    }

    /************************************
     * CUSTOM PATH CLASS
     ************************************/
    private class DrawPath {

        /** Path information **/
        public Path path;

        /** Paint information of the path **/
        public Paint paint;

        private int mode;

        public String tag;

        /** Whether or not the path is in erase mode **/
        private static final int DRAW_FLAG = 0x01;
        private static final int ERASE_FLAG = 0x02;

        private float x, y;
        private static final float TOUCH_TOLERANCE = 4;

        public DrawPath(Path p1, Paint p2) {
            path = p1;
            paint = p2;
            mode = DRAW_FLAG;
        }

        public void setEraseActive(float size) {
            paint.setStrokeWidth(size);
            updatePaintStyle(ERASE_FLAG);
        }

        public void setDrawActive(int color, float size) {
            paint.setColor(color);
            paint.setStrokeWidth(size);
            updatePaintStyle(DRAW_FLAG);
        }

        public boolean isEraseActive() { return mode == ERASE_FLAG; }

        public boolean isDrawActive() { return mode == DRAW_FLAG; }

        private void updatePaintStyle(int newMode) {
            mode = newMode;
            switch (mode) {
                case ERASE_FLAG:
                    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//                    paint.setColor(backgroundColor);
                    break;
                case DRAW_FLAG:
                    paint.setMaskFilter(null);
                    paint.setXfermode(null);
                    paint.setAlpha(0xff);
                    break;
            }
        }
    }

    /************************************
     * CANVAS INITIALIZATIONS
     ************************************/

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setEventListener(CanvasEventListener changeListener) {
        listener = changeListener;
    }

    private void init() {
        setDrawingCacheEnabled(true);

        backgroundColor = Color.WHITE;
        setBackgroundColor(backgroundColor);

        paths = new HashMap<>();
        DrawPath drawPath = new DrawPath(new Path(), createPaint(drawColor, drawSize));
        drawPath.tag = USER_PATH;
        paths.put(USER_PATH, drawPath);
    }

    private Paint createPaint(int color, float strokeWidth) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setPathEffect(new CornerPathEffect(30));
        return paint;
    }

    /************************************
     * CANVAS CALLBACKS
     ************************************/

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        Log.d(TAG, "Creating new bitmap on canvas of size " + w + " x " + h + ", " + oldW + " x " + oldH);

        if (!TextUtils.isEmpty(savedPath)) {
            try {
                Log.d(TAG, "Attempting to load drawing from path " + savedPath);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                options.inMutable = true;
                loadedBitmap = BitmapFactory.decodeFile(savedPath, options);
                Log.d(TAG, "Set drawing canvas to loaded drawing");
            }
            catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
                Toast.makeText(getContext(), getResources().getString(R.string.notification_load_drawing_failed), Toast.LENGTH_LONG).show();
            }
        }

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        if (loadedBitmap != null) {
            bitmapCanvas.drawBitmap(loadedBitmap, 0, 0, null);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, null);
        for (DrawPath drawPath : paths.values()) {
            canvas.drawPath(drawPath.path, drawPath.paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(USER_PATH, x, y);
                if (listener != null) {
                    if (isDrawActive()) listener.onDrawStart(drawColor, drawSize, x, y);
                    else if (isEraserActive()) listener.onEraseStart(eraserSize, x, y);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                touchMove(USER_PATH, x, y);
                if (listener != null) listener.onMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(USER_PATH);
                if (listener != null) listener.onUp();
                break;
            default: return false;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (listener != null) listener.onExit();
    }

    /************************************
     * PRIVATE CANVAS METHODS
     ************************************/

    private void touchStart(String userId, float x, float y) {
        DrawPath drawPath = paths.get(userId);
        if (drawPath != null) {
            drawPath.path.reset();
            drawPath.path.moveTo(x, y);
            drawPath.x = x;
            drawPath.y = y;
        }
    }

    private void touchMove(String userId, float x, float y) {
        DrawPath drawPath = paths.get(userId);
        if (drawPath != null) {
            float dx = Math.abs(x - drawPath.x);
            float dy = Math.abs(y - drawPath.y);
            if (dx >= DrawPath.TOUCH_TOLERANCE || dy >= DrawPath.TOUCH_TOLERANCE) {
                drawPath.path.quadTo(drawPath.x, drawPath.y, (x + drawPath.x)/2, (y + drawPath.y)/2);
                if (drawPath.isEraseActive()) {
                    bitmapCanvas.drawPath(drawPath.path, drawPath.paint);
                    drawPath.path.reset();
                    drawPath.path.moveTo(drawPath.x, drawPath.y);
                }

                drawPath.x = x;
                drawPath.y = y;
            }
        }
    }

    private void touchUp(String userId) {
        DrawPath drawPath = paths.get(userId);
        if (drawPath != null) {
            if (!drawPath.isEraseActive()) {
                drawPath.path.lineTo(drawPath.x, drawPath.y);
                bitmapCanvas.drawPath(drawPath.path, drawPath.paint);
            }
            drawPath.path.reset();
        }
    }

    /************************************
     * PUBLIC CANVAS METHODS
     ************************************/

    public void start(String userId, float x, float y) {
        touchStart(userId, x, y);
        invalidate();
    }

    public void move(String userId, float x, float y) {
        touchMove(userId, x, y);
        invalidate();
    }

    public void up(String userId) {
        touchUp(userId);
        invalidate();
    }

    public void removePath(String userId) {
        paths.remove(userId);
    }

    public void setEraserActive(String userId, float size) {
        DrawPath drawPath;
        if (TextUtils.isEmpty(userId)) {
            drawPath = paths.get(USER_PATH);
            drawPath.setEraseActive(eraserSize);
        }
        else {
            drawPath = paths.get(userId);
            if (drawPath == null) {
                drawPath = new DrawPath(new Path(), createPaint(Color.BLACK, 7f));
                paths.put(userId, drawPath);
            }
            drawPath.setEraseActive(size);
        }
    }

    public boolean isEraserActive() {
        return paths.get(USER_PATH).isEraseActive();
    }

    public void setDrawActive(String userId, int color, float size) {
        DrawPath drawPath;
        if (TextUtils.isEmpty(userId)) {
            drawPath = paths.get(USER_PATH);
            drawPath.setDrawActive(drawColor, drawSize);
        }
        else {
            drawPath = paths.get(userId);
            if (drawPath == null) {
                drawPath = new DrawPath(new Path(), createPaint(color, size));
                paths.put(userId, drawPath);
            }
            drawPath.setDrawActive(color, size);
        }
    }

    public boolean isDrawActive() {
        return paths.get(USER_PATH).isDrawActive();
    }

    public void setDrawColor(int color) {
        drawColor = color;
        DrawPath drawPath = paths.get(USER_PATH);
        if (drawPath != null && isDrawActive()) {
            drawPath.paint.setColor(color);
        }
    }

    public void setDrawSize(float size) {
        drawSize = size;
        DrawPath drawPath = paths.get(USER_PATH);
        if (drawPath != null && isDrawActive()) {
            drawPath.paint.setStrokeWidth(size);
        }
    }

    public void setEraserSize(float size) {
        eraserSize = size;
        DrawPath drawPath = paths.get(USER_PATH);
        if (drawPath != null && isEraserActive()) {
            drawPath.paint.setStrokeWidth(size);
        }
    }

    public void clear() {
        setDrawingCacheEnabled(false);
        savedPath = null;
        loadedBitmap = null;
        onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
        invalidate();
        setDrawingCacheEnabled(true);
    }

    /**
     * This method runs asynchronously in a separate thread
     */
    public void save(String path) {
        Log.d(TAG, "Saving bitmap to " + path);
        Bitmap currentBitmap = ThumbnailUtils.extractThumbnail(getDrawingCache(), getWidth(), getHeight());

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.d(TAG, "Bitmap saved successfully");
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, ex.getMessage());
            }
        }
        // you can now save the bitmap to a file, or display it in an ImageView:
//        ImageView testArea = ...
//        testArea.setImageBitmap(currentBitmap);
//
//        // these days you often need a "byte array". for example,
//        // to save to parse.com or other cloud services
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        currentBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
//        byte[] yourByteArray;
//        yourByteArray = baos.toByteArray();
    }

    public void setSavedPath(String path) {
        savedPath = path;
    }
}