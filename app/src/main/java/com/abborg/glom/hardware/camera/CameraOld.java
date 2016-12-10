package com.abborg.glom.hardware.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Old wrapper around deprecated Android's camera1 API
 */
@SuppressWarnings("deprecation")
public class CameraOld implements CameraCompat {

    private Camera camera;

    private Handler handler;
    private ExecutorService threadPool;

    private static final String TAG = "CameraOld";

    public CameraOld() {
        threadPool = Executors.newSingleThreadExecutor();
    }

    private void executeAsync(Runnable runnable) {
        threadPool.submit(runnable);
    }

    private void sendMessage(int msgId) {
        if (handler != null) {
            handler.sendMessage(handler.obtainMessage(msgId));
        }
    }

    @Override
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void open(final int cameraId) {
        executeAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    camera = Camera.open(cameraId);
                    Camera.Parameters params = camera.getParameters();
                    params.set("orientation", "landscape");
                    params.set("rotation", 90);
                    camera.setParameters(params);
                    camera.setDisplayOrientation(90);
                    if (camera != null) {
                        sendMessage(CAMERA_READY);
                        return;
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                    ex.printStackTrace();
                }
                sendMessage(CAMERA_ERROR);
            }
        });
    }

    @Override
    public void close() {
        if (camera != null) {
            stopPreview();
            camera.release();
            camera = null;

            sendMessage(CAMERA_RELEASED);
        }
    }

    @Override
    public int getOrientation(int cameraId) {
        return 0;
    }

    @Override
    public void setPreviewDisplay(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
        }
        catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void startPreview() {
        try {
            camera.startPreview();
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {
        try {
            camera.stopPreview();
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }
}
