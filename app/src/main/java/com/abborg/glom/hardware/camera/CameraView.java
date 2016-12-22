package com.abborg.glom.hardware.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.abborg.glom.di.ComponentInjector;

import java.io.IOException;

import javax.inject.Inject;

/**
 * View that the camera projects it capturing onto
 */
public class CameraView extends TextureView implements
        TextureView.SurfaceTextureListener {

    @Inject
    CameraCompat camera;

    private MediaPlayer mediaPlayer;
    private SurfaceTexture surfaceTexture;
    private String lastSavedVideoPath;
    private boolean isPlaybackMode;

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

    public void closeCamera() {
        camera.close();
    }

    public void takePicture() {
        camera.takePicture();
    }

    public void startPreview() { camera.startPreview(); }

    public void savePicture(String path) {
        camera.savePicture(path);
    }

    public void startRecording(String path, int maxDuration) {
        lastSavedVideoPath = path;
        camera.startRecording(path, maxDuration);
    }

    public void stopRecording() {
        camera.stopRecording();
    }

    public void stopPreview() {
        camera.stopPreview();
    }

    public void startPlayback(final Context context) {
        if (surfaceTexture == null || TextUtils.isEmpty(lastSavedVideoPath)) return;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    //FIXME
//                    isPlaybackMode = true;
//
//                    mediaPlayer = new MediaPlayer();
//                    mediaPlayer.setDataSource(lastSavedVideoPath);
//                    mediaPlayer.setSurface(new Surface(surfaceTexture));
//                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//                    mediaPlayer.setLooping(true);
//                    mediaPlayer.prepareAsync();
//                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                        @Override
//                        public void onPrepared(MediaPlayer mp) {
//                            mediaPlayer.start();
//                            Log.d(TAG, "Start media player");
//                        }
//                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    isPlaybackMode = false;
                }
            }
        }, 5000);
    }

    public void stopPlayback() {
        if (isPlaybackMode && mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;

            isPlaybackMode = false;
            Log.d(TAG, "Released media player");
        }
    }

    public String getLastSavedVideoPath() {
        return lastSavedVideoPath;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "SurfaceTexture is available");

        if (camera == null) return;

        // fix inverted image from front camera ?
//        if (camera.getCameraId() == camera.getFrontCameraId()) {
//            Matrix matrix = new Matrix();
//            matrix.setScale(-1, 1);
//            matrix.postTranslate(width, 0);
//            setTransform(matrix);
//        }

        camera.setPreviewTexture(surface);
        surfaceTexture = surface;
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
