package com.abborg.glom.hardware.camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayOutputStream;
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
    private int cameraId;

    private Handler handler;
    private ExecutorService threadPool;
    private byte[] capturedImageData;
    private MediaRecorder recorder;

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
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(capturedImageData, 0, capturedImageData.length, options);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream);
                    fos.write(outputStream.toByteArray());
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

                    // open the camera
                    cameraId = id;
                    if (cameraId == -1) {
                        camera = Camera.open();
                        cameraId = getBackCameraId();
                    }
                    else {
                        camera = Camera.open(cameraId);
                    }

                    // set camera orientation
                    Camera.Parameters params = camera.getParameters();
                    params.set("orientation", "landscape");
                    if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        params.set("rotation", 90);
                        params.setRotation(90);
                    }
                    else {
                        params.set("rotation", 270);
                        params.setRotation(270);
                    }
                    camera.setDisplayOrientation(90);
                    camera.setParameters(params);

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

    @Override
    public void startRecording(final String path, final int maxDuration) {
        executeAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    if (prepareForRecording(path, maxDuration)) {
                        recorder.start();

                        sendMessage(VIDEO_START_RECORDING);
                    }
                    else {
                        releaseMediaRecorder();

                        sendMessage(VIDEO_ERROR);
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                    ex.printStackTrace();

                    sendMessage(VIDEO_ERROR);
                }
            }
        });
    }

    @Override
    public void stopRecording() {
        try {
            if (recorder != null) {
                recorder.stop();
                releaseMediaRecorder();
                camera.lock();

                sendMessage(VIDEO_STOP_RECORDING);
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();

            sendMessage(VIDEO_ERROR);
        }
    }

    private boolean prepareForRecording(String path, int maxDuration) {
        try {
            if (recorder == null) {
                recorder = new MediaRecorder();
            }
            camera.unlock();
            recorder.setCamera(camera);
            recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            if (cameraId == getFrontCameraId()) {
                recorder.setOrientationHint(270);
            }
            else {
                recorder.setOrientationHint(90);
            }
            recorder.setMaxDuration(maxDuration);
            recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
            recorder.setOutputFile(path);
            recorder.prepare();
        }
        catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
            ex.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder(){
        if (recorder != null) {
            recorder.reset();
            recorder.release();
            recorder = null;
        }
    }

    @Override
    public int getCameraId() {
        return cameraId;
    }
}
