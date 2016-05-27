package com.abborg.glom.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * Main drawable custom view that provides users with canvas to draw on.
 *
 * Created by jitrapon on 17/5/16.
 */
public class CanvasView extends View {

    private static final String USER_PATH = "User";

    /** Stores a list of user painted paths **/
    private Map<String, DrawPath> paths;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;

    private int backgroundColor = Color.WHITE;
    private float eraserSize = 70f;
    private float drawSize = 7f;
    private int drawColor = Color.BLUE;

    private CanvasEventListener listener;

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

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (DrawPath drawPath : paths.values()) {
            canvas.drawBitmap(bitmap, 0, 0, null);
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
                drawPath.x = x;
                drawPath.y = y;
            }
        }
    }

    private void touchUp(String userId) {
        DrawPath drawPath = paths.get(userId);
        if (drawPath != null) {
            drawPath.path.lineTo(drawPath.x, drawPath.y);
            bitmapCanvas.drawPath(drawPath.path, drawPath.paint);
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
}