package com.abborg.glom.hardware.camera;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.abborg.glom.di.ComponentInjector;

import javax.inject.Inject;

/**
 * Surface view that the camera projects it capturing onto
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    @Inject
    CameraCompat camera;

    private SurfaceHolder surfaceHolder;

    private static final String TAG = "CameraView";

    /**
     * Camera object must be opened before
     */
    public CameraView(Context context, Handler handler) {
        super(context);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        camera.setHandler(handler);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void openCamera() {
        camera.open(0);
    }

    public void releaseCamera() {
        camera.close();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "SurfaceView created");

        if (camera == null) return;

        camera.setPreviewDisplay(surfaceHolder);
        camera.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "SurfaceView changed to format: " + format + ", width: " + width + ", height:" + height);

        if (surfaceHolder.getSurface() == null) return;

        camera.stopPreview();

        // set preview size and make any resize, rotate or
        // reformatting changes here

        camera.setPreviewDisplay(surfaceHolder);
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "SurfaceView destroyed");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }
}
