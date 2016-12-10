package com.abborg.glom.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.FrameLayout;

import com.abborg.glom.R;
import com.abborg.glom.hardware.camera.CameraCompat;
import com.abborg.glom.hardware.camera.CameraView;

public class CameraActivity extends AppCompatActivity implements
    Handler.Callback {

    private FrameLayout preview;
    private CameraView cameraView;

    private Handler handler;

    private static final String TAG = "CameraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        handler = new Handler(this);

        setContentView(R.layout.activity_camera);

        preview = (FrameLayout) findViewById(R.id.camera_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Initializing camera view");
        cameraView = new CameraView(this, handler);
        cameraView.openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        cameraView.releaseCamera();
        preview.removeAllViews();
        cameraView = null;
    }

    /**********************************************************
     * Handler Callbacks
     **********************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case CameraCompat.CAMERA_READY: {
                Log.d(TAG, "Camera is ready to start previewing");

                preview.removeAllViews();
                preview.addView(cameraView);
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
