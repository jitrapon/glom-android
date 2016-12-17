package com.abborg.glom.hardware.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Old wrapper around deprecated Android's camera1 API
 */
@SuppressWarnings("deprecation")
public class CameraOld implements
        CameraCompat,
        Camera.PictureCallback,
        Camera.ShutterCallback {

    private Camera camera;

    private Handler handler;
    private ExecutorService threadPool;
    private byte[] capturedImageData;

    private static final String TAG = "CameraOld";

    public CameraOld() {
        threadPool = Executors.newSingleThreadExecutor();
    }

    private void executeAsync(Runnable runnable) {
        threadPool.submit(runnable);
    }

    private void sendMessage(int msgId, Object... args) {
        if (handler != null) {
            if (args != null && args.length == 1) {
                handler.sendMessage(handler.obtainMessage(msgId, args[0]));
            }
            else {
                handler.sendMessage(handler.obtainMessage(msgId));
            }
        }
    }

    @Override
    public void savePicture(final String path) {
        executeAsync(new Runnable() {
            @Override
            public void run() {
                File file = new File(path);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(capturedImageData);
                    fos.close();
                    capturedImageData = null;
                    sendMessage(PICTURE_SAVED, path);
                }
                catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    e.printStackTrace();
                    sendMessage(PICTURE_ERROR, e);
                }
            }
        });
    }

    @Override
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void open() {
        open(-1);
    }

    @Override
    public void open(final int id) {
        executeAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    int cameraId = id;
                    if (cameraId == -1) {
                        camera = Camera.open();
                        cameraId = getBackCameraId();
                    }
                    else {
                        camera = Camera.open(cameraId);
                    }
                    Camera.Parameters params = camera.getParameters();
                    params.set("orientation", "landscape");
                    params.set("rotation", 90);
                    camera.setParameters(params);
                    camera.setDisplayOrientation(90);
                    if (camera != null) {
                        sendMessage(CAMERA_READY, cameraId);
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
    public void setPreviewTexture(SurfaceTexture surface) {
        try {
            camera.setPreviewTexture(surface);
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

    @Override
    public int getBackCameraId() {
        int cameraID = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraID = i;
                break;
            }
        }
        return cameraID;
    }

    @Override
    public int getFrontCameraId() {
        int cameraID = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraID = i;
                break;
            }
        }
        return cameraID;
    }

    @Override
    public void takePicture() {
        try {
            camera.takePicture(this, null, this);
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        capturedImageData = data;
        sendMessage(CameraCompat.PICTURE_READY);
    }

    @Override
    public void onShutter() {
        sendMessage(CameraCompat.SHUTTER);
    }
}
