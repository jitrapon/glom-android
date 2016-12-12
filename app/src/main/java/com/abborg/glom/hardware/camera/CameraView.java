package com.abborg.glom.hardware.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;

import com.abborg.glom.di.ComponentInjector;

import javax.inject.Inject;

/**
 * View that the camera projects it capturing onto
 */
public class CameraView extends TextureView implements
        TextureView.SurfaceTextureListener {

    @Inject
    CameraCompat camera;

    private static final String TAG = "CameraView";

    /**
     * Camera object must be opened before
     */
    public CameraView(Context context, Handler handler) {
        super(context);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        camera.setHandler(handler);
        setSurfaceTextureListener(this);
    }

    public int getBackCameraId() { return camera.getBackCameraId(); }

    public int getFrontCameraId() { return camera.getFrontCameraId(); }

    public void openCamera(int cameraId) {
        camera.open(cameraId);
    }

    public void openCamera() {
        camera.open();
    }

    public void releaseCamera() {
        camera.close();
    }

    public void takePicture() {
        camera.takePicture();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "SurfaceTexture is available");

        if (camera == null) return;

        camera.setPreviewTexture(surface);
        camera.startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "SurfaceTexture size changed to width: " + width + ", height:" + height);

        camera.stopPreview();

        // set preview size and make any resize, rotate or
        // reformatting changes here

        camera.setPreviewTexture(surface);
        camera.startPreview();
    }

    /**
     * Called every frame the texture view is updated
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "SurfaceTexture destroyed");
        return true;
    }
}
