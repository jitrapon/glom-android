package com.abborg.glom.activities;

import android.content.Intent;
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

    private int currentCameraId;
    private static int BACK_CAMERA_ID;
    private static int FRONT_CAMERA_ID;

    private boolean isSaving;
    private boolean isVideoMode;
    private boolean isEditMode;

    private Handler handler;

    private static final String TAG = "CameraActivity";

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
        closeButton.setOnClickListener(this);
        changeCameraButton.setOnClickListener(this);
        captureButton.setOnTouchListener(this);
        doneButton.setOnTouchListener(this);
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
    public void onBackPressed() {
        if (isEditMode) {
            isEditMode = false;
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
                    cameraView.takePicture();
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
                        savePicture();
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
            cameraView.releaseCamera();
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

    private void updateView() {
        if (isEditMode) {
            changeCameraButton.setVisibility(View.GONE);
            captureButton.setVisibility(View.GONE);
            doneButton.setVisibility(View.VISIBLE);
            ViewUtils.animateScale(doneButton, 0, 1, 200);
        }
        else {
            changeCameraButton.setVisibility(View.VISIBLE);
            captureButton.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.GONE);
            ViewUtils.animateScale(captureButton, 0, 1, 200);
            ViewUtils.animateScale(changeCameraButton, 0, 1, 200);
        }
    }

    /**********************************************************
     * Handler Callbacks
     **********************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case CameraCompat.CAMERA_READY: {
                preview.addView(cameraView);
                preview.addView(closeButton);
                preview.addView(changeCameraButton);
                preview.addView(captureButton);
                preview.addView(doneButton);

                updateView();

                currentCameraId = (int) msg.obj;

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
        }

        return false;
    }
}
