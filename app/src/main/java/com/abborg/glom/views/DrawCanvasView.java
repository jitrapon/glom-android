package com.abborg.glom.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Main drawable custom view that provides users with canvas to draw on.
 *
 * Created by jitrapon on 17/5/16.
 */
public class DrawCanvasView extends View {

    private static final String TAG = "DrawCanvasView";

    private Paint paint;
    private Path path;
    private DrawCanvasChangeListener listener;

    public interface DrawCanvasChangeListener {

        void onDraw(float x, float y);

        void onFinished();
    }

    /************************************
     * CANVAS METHODS
     ************************************/

    public DrawCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setEventListener(DrawCanvasChangeListener changeListener) {
        listener = changeListener;
    }

    private void init() {
        Log.d(TAG, "Initializing canvas");
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);

        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                if (listener != null) listener.onDraw(x, y);
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                if (listener != null) listener.onDraw(x, y);
                break;
            default: return false;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        Log.d(TAG, "Canvas is about to be detached and disposed");
        if (listener != null) listener.onFinished();
    }
}
