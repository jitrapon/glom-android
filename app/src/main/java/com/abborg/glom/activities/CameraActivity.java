package com.abborg.glom.activities;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.abborg.glom.R;
import com.abborg.glom.hardware.camera.CameraCompat;
import com.abborg.glom.hardware.camera.CameraView;

public class CameraActivity extends AppCompatActivity implements
        Handler.Callback,
        View.OnClickListener,
        View.OnTouchListener {

    /* View that contains child camera view */
    private FrameLayout preview;
    private CameraView cameraView;
    private ImageView closeButton;
    private ImageView changeCameraButton;
    private ProgressBar captureButton;

    private int currentCameraId;
    private static int BACK_CAMERA_ID;
    private static int FRONT_CAMERA_ID;

    private boolean isVideoMode;

    private Handler handler;

    private static final String TAG = "CameraActivity";

    /**********************************************************
     * Activity Callbacks
     **********************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(this);

        setContentView(R.layout.activity_camera);

        preview = (FrameLayout) findViewById(R.id.camera_view);
        closeButton = (ImageView) findViewById(R.id.close_button);
        changeCameraButton = (ImageView) findViewById(R.id.change_camera_button);
        captureButton = (ProgressBar) findViewById(R.id.capture_button);
        closeButton.setOnClickListener(this);
        changeCameraButton.setOnClickListener(this);
        captureButton.setOnTouchListener(this);
        preview.removeAllViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeCamera();
    }

    /**********************************************************
     * View Callbacks
     **********************************************************/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_button:
                finish();
                break;

            case R.id.change_camera_button:
                closeCamera();
                if (currentCameraId == BACK_CAMERA_ID) {
                    Log.d(TAG, "Switching from back camera to front camera");
                    openCamera(FRONT_CAMERA_ID);
                }
                else if (currentCameraId == FRONT_CAMERA_ID) {
                    Log.d(TAG, "Switching from front camera to back camera");
                    openCamera(BACK_CAMERA_ID);
                }
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.capture_button) {
            Drawable background = captureButton.getBackground();

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    background.setAlpha(80);
                    break;
                case MotionEvent.ACTION_UP:
                    background.setAlpha(255);
                    break;
            }

            return true;
        }

        return false;
    }

    private void takePicture() {

    }

    private void openCamera() {
        cameraView = new CameraView(this, handler);
        FRONT_CAMERA_ID = cameraView.getFrontCameraId();
        BACK_CAMERA_ID = cameraView.getBackCameraId();
        cameraView.openCamera();
    }

    private void openCamera(int cameraId) {
        cameraView = new CameraView(this, handler);
        FRONT_CAMERA_ID = cameraView.getFrontCameraId();
        BACK_CAMERA_ID = cameraView.getBackCameraId();
        cameraView.openCamera(cameraId);
    }

    private void closeCamera() {
        if (cameraView != null) {
            cameraView.releaseCamera();
            preview.removeAllViews();
            cameraView = null;
        }
    }

    /**********************************************************
     * Handler Callbacks
     **********************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case CameraCompat.CAMERA_READY: {
                preview.removeAllViews();
                preview.addView(cameraView);
                preview.addView(closeButton);
                preview.addView(changeCameraButton);
                preview.addView(captureButton);

                currentCameraId = (int) msg.obj;

                Log.d(TAG, "Camera is ready to start previewing on id " + currentCameraId);

                break;
            }

            case CameraCompat.CAMERA_ERROR: {
                Log.d(TAG, "Camera failed to launch");
                break;
            }

            case CameraCompat.CAMERA_RELEASED: {
                Log.d(TAG, "Camera is released");
                break;
            }
        }

        return false;
    }
}
