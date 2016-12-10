package com.abborg.glom.hardware.camera;

import android.os.Handler;
import android.view.SurfaceHolder;

/**
 * Abstracts Camera API differences from Camera1 and Camera2 classes
 */
public interface CameraCompat {

    int CAMERA_READY = 0;
    int CAMERA_RELEASED = 1;
    int CAMERA_ERROR = 2;

    void setHandler(Handler handler);

    void open(int cameraId);

    void close();

    int getOrientation(int cameraId);

    void setPreviewDisplay(SurfaceHolder holder);

    void startPreview();

    void stopPreview();
}
