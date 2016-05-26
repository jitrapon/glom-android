package com.abborg.glom.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
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
public class DrawCanvasView extends View {

    private static final String TAG = "DrawCanvasView";
    private static final String USER_PATH = "User";

    /** Stores a list of user painted paths **/
    private Map<String, DrawPath> paths;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;

    private DrawCanvasChangeListener listener;
    private int invalidateRectSize;

    public interface DrawCanvasChangeListener {

        void onDrawStart(float x, float y);

        void onDraw(float x, float y);

        void onDrawEnd();

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

        private float x, y;
        private static final float TOUCH_TOLERANCE = 4;

        public DrawPath(Path p1, Paint p2) {
            path = p1;
            paint = p2;
        }
    }

    /************************************
     * CANVAS INITIALIZATIONS
     ************************************/

    public DrawCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setEventListener(DrawCanvasChangeListener changeListener) {
        listener = changeListener;
    }

    private void init() {
        setBackgroundColor(Color.WHITE);

        invalidateRectSize = 100;

        paths = new HashMap<>();
        paths.put(USER_PATH, new DrawPath(new Path(), createPaint(Color.BLACK, 7f)));
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
            canvas.drawBitmap(bitmap, 0, 0, drawPath.paint);
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
                if (listener != null) listener.onDrawStart(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                touchMove(USER_PATH, x, y);
                if (listener != null) listener.onDraw(x, y);
                break;
            case MotionEvent.ACTION_UP:
                touchUp(USER_PATH);
                if (listener != null) listener.onDrawEnd();
                break;
            default: return false;
        }

        return invalidate(x, y);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (listener != null) listener.onExit();
    }

    /**
     * Forces a redraw on the canvas at a rectangular area around (x,y)
     * after a specified period in milliseconds has passed
     */
    private boolean invalidate(float x, float y) {
        int l = (int) (x - invalidateRectSize);
        int t = (int) (y - invalidateRectSize);
        int r = (int) (x + invalidateRectSize);
        int b = (int) (y + invalidateRectSize);

        invalidate(l, t, r, b);
        return true;
    }

    /************************************
     * PRIVATE CANVAS METHODS
     ************************************/

    private void touchStart(String userId, float x, float y) {
        DrawPath drawPath = paths.get(userId);
        if (drawPath == null) {
            drawPath = new DrawPath(new Path(), createPaint(Color.RED, 7f));
            paths.put(userId, drawPath);
        }

        drawPath.path.reset();
        drawPath.path.moveTo(x, y);
        drawPath.x = x;
        drawPath.y = y;
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

    public void startDraw(String userId, float x, float y) {
        touchStart(userId, x, y);
        invalidate(x, y);
    }

    public void draw(String userId, float x, float y) {
        touchMove(userId, x, y);
        invalidate(x, y);
    }

    public void endDraw(String userId) {
        touchUp(userId);
        invalidate();
    }

    public void removePath(String userId) {
        paths.remove(userId);
    }
}