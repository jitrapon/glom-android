package com.abborg.glom.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.hardware.camera.CameraCompat;
import com.abborg.glom.hardware.camera.CameraView;
import com.abborg.glom.utils.FileUtils;
import com.abborg.glom.utils.ViewUtils;

import java.io.File;

import javax.inject.Inject;

public class CameraActivity extends AppCompatActivity implements
        Handler.Callback,
        View.OnClickListener,
        View.OnTouchListener {

    @Inject
    ApplicationState appState;

    /* View that contains child camera view */
    private FrameLayout preview;
    private CameraView cameraView;
    private ImageView closeButton;
    private ImageView changeCameraButton;
    private ProgressBar captureButton;
    private ImageView doneButton;
    private ImageView switchModeButton;
    private ObjectAnimator recordingAnimation;

    private int currentCameraId;
    private static int BACK_CAMERA_ID;
    private static int FRONT_CAMERA_ID;

    private boolean isSaving;
    private boolean isVideoMode;
    private boolean isRecording;
    private boolean isEditMode;
    private boolean isEditVideoMode;

    private Handler handler;

    private static final String TAG = "CameraActivity";
    private static final int MAX_VIDEO_DURATION_SEC = 20 * 1000;

    /**********************************************************
     * Activity Callbacks
     **********************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        handler = new Handler(this);

        setContentView(R.layout.activity_camera);

        preview = (FrameLayout) findViewById(R.id.camera_view);
        closeButton = (ImageView) findViewById(R.id.close_button);
        changeCameraButton = (ImageView) findViewById(R.id.change_camera_button);
        captureButton = (ProgressBar) findViewById(R.id.capture_button);
        doneButton = (ImageView) findViewById(R.id.done_button);
        switchModeButton = (ImageView) findViewById(R.id.switch_mode_button);
        switchModeButton.setOnClickListener(this);
        closeButton.setOnClickListener(this);
        changeCameraButton.setOnClickListener(this);
        captureButton.setOnTouchListener(this);
        doneButton.setOnTouchListener(this);
        preview.removeAllViews();

        isVideoMode = false;
        captureButton.setMax(MAX_VIDEO_DURATION_SEC);
        captureButton.setProgress(0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isRecording) {
            stopRecording();
        }
        cameraView.stopPlayback();
        closeCamera();
    }

    /**********************************************************
     * View Callbacks
     **********************************************************/

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            isEditMode = false;

            cameraView.stopPlayback();
            if (!TextUtils.isEmpty(cameraView.getLastSavedVideoPath())) {
                File temp = new File(cameraView.getLastSavedVideoPath());
                if (temp.exists()){
                    temp.delete();
                }
            }

            cameraView.startPreview();
            updateView();
        }
        else {
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.close_button:
                if (isEditMode) {
                    isEditMode = false;

                    cameraView.stopPlayback();
                    if (!TextUtils.isEmpty(cameraView.getLastSavedVideoPath())) {
                        File temp = new File(cameraView.getLastSavedVideoPath());
                        if (temp.exists()){
                            temp.delete();
                        }
                    }

                    cameraView.startPreview();
                    updateView();
                }
                else {
                    finish();
                }
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

            case R.id.switch_mode_button:
                toggleCaptureMode();
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
                    if (!isVideoMode) {
                        cameraView.takePicture();
                    }
                    else {
                        if (!isRecording) {
                            startRecording();
                        }
                        else {
                            stopRecording();
                        }
                    }
                    break;
            }

            return true;
        }
        else if (v.getId() == R.id.done_button) {
            Drawable background = doneButton.getBackground();

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    background.setAlpha(80);
                    break;
                case MotionEvent.ACTION_UP:
                    background.setAlpha(255);
                    if (!isSaving) {
                        if (isEditVideoMode) {
                            finish();
                        }
                        else {
                            savePicture();
                        }
                    }
                    break;
            }

            return true;
        }

        return false;
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
            cameraView.closeCamera();
            preview.removeAllViews();
            cameraView = null;
        }
    }

    private void savePicture() {
        File downloadDir = appState.getExternalMediaDir();
        if (downloadDir != null) {
            isSaving = true;
            String name = downloadDir + File.separator + "photo_" + FileUtils.getTimestampFilename() + ".jpg";
            Log.d(TAG, "Saving file as " + name);
            cameraView.savePicture(name);
        }
    }

    private void startRecording() {
        File downloadDir = appState.getExternalMediaDir();
        if (downloadDir != null) {
            cameraView.startRecording(downloadDir + File.separator + "video_" + FileUtils.getTimestampFilename() + ".mp4",
                    MAX_VIDEO_DURATION_SEC);
        }
    }

    private void stopRecording() {
        cameraView.stopRecording();
    }

    private void toggleCaptureMode() {
        if (isVideoMode) {
            isVideoMode = false;
            switchModeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_video));
            Toast.makeText(this, "Camera mode", Toast.LENGTH_SHORT).show();
        }
        else {
            isVideoMode = true;
            switchModeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.ic_camera));
            Toast.makeText(this, "Video mode", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateView() {
        if (isEditMode || isEditVideoMode) {
            cameraView.stopPreview();
            if (isVideoMode) {
                cameraView.startPlayback(this);
            }

            changeCameraButton.setVisibility(View.GONE);
            captureButton.setVisibility(View.GONE);
            switchModeButton.setVisibility(View.GONE);
            if (closeButton.getVisibility() == View.GONE) {
                closeButton.setVisibility(View.VISIBLE);
                ViewUtils.animateScale(closeButton, 0, 1, 200);
            }
            doneButton.setVisibility(View.VISIBLE);
            ViewUtils.animateScale(doneButton, 0, 1, 200);
        }
        else if (isRecording) {
            switchModeButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.GONE);
            changeCameraButton.setVisibility(View.GONE);
        }
        else {
            changeCameraButton.setVisibility(View.VISIBLE);
            captureButton.setVisibility(View.VISIBLE);
            switchModeButton.setBackground(ContextCompat.getDrawable(this, isVideoMode ? R.drawable.ic_camera: R.drawable.ic_video));
            switchModeButton.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.GONE);
            if (closeButton.getVisibility() == View.GONE) {
                closeButton.setVisibility(View.VISIBLE);
                ViewUtils.animateScale(closeButton, 0, 1, 200);
            }
            ViewUtils.animateScale(captureButton, 0, 1, 200);
            ViewUtils.animateScale(changeCameraButton, 0, 1, 200);
            ViewUtils.animateScale(switchModeButton, 0, 1, 200);
        }
    }

    /**********************************************************
     * Handler Callbacks
     **********************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case CameraCompat.CAMERA_READY: {
                currentCameraId = (int) msg.obj;

                preview.addView(cameraView);
                preview.addView(closeButton);
                preview.addView(changeCameraButton);
                preview.addView(captureButton);
                preview.addView(doneButton);
                preview.addView(switchModeButton);
                updateView();

                Log.d(TAG, "Camera is ready to start previewing on id " + currentCameraId);

                break;
            }

            case CameraCompat.CAMERA_ERROR: {
                Log.d(TAG, "Camera failed to launch");

                updateView();
                break;
            }

            case CameraCompat.CAMERA_RELEASED: {
                Log.d(TAG, "Camera is released");
                break;
            }

            case CameraCompat.PICTURE_READY: {
                Log.d(TAG, "Picture is ready");

                isEditMode = true;
                updateView();

                break;
            }

            case CameraCompat.PICTURE_SAVED: {
                String path = (String) msg.obj;
                Log.d(TAG, "Picture is saved at " + path);

                Intent intent = new Intent();
                intent.putExtra(getResources().getString(R.string.EXTRA_CAMERA_IMAGE), path);
                setResult(RESULT_OK, intent);
                finish();

                break;
            }

            case CameraCompat.PICTURE_ERROR: {
                isSaving = false;
                Toast.makeText(this, "Could not save this image, please try again", Toast.LENGTH_SHORT).show();

                break;
            }

            case CameraCompat.VIDEO_START_RECORDING: {
                Log.d(TAG, "Video started recording");
                isRecording = true;
                recordingAnimation = ViewUtils.animateProgress(captureButton, 0, MAX_VIDEO_DURATION_SEC, MAX_VIDEO_DURATION_SEC);
                updateView();

                break;
            }

            case CameraCompat.VIDEO_STOP_RECORDING: {
                Log.d(TAG, "Video stopped recording");
                isRecording = false;
                isEditVideoMode = true;
                captureButton.setProgress(0);
                if (recordingAnimation != null) {
                    recordingAnimation.cancel();
                    recordingAnimation = null;
                }
                captureButton.clearAnimation();
                updateView();

                break;
            }
        }

        return false;
    }
}
