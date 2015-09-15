package com.abborg.glom.utils;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;

/**
 * Keeps a singleton of class that encapsulates logic for connecting with the server
 * and Google Play Services
 *
 * Created by Boat on 13/9/58.
 */
public class Connection implements ConnectionCallbacks, OnConnectionFailedListener {

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* The singleton instance to be used throughout the app cycle */
    private static Connection instance;

    /* Helper class that verifies Google's Api client */
    private GoogleApiAvailability apiAvailability;

    private WeakReference activityWeakReference;

    private Connection() {};

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final String TAG = "CONNECTION";

    /**
     * Call this method to get reference to a singleton instance of this apiClient
     *
     * @param activity
     * @return
     */
    public static Connection getInstance(Activity activity) {
        if (instance == null) {
            instance = new Connection(activity);
        }

        return instance;
    }

    //TODO verify in onCreate and onResume in the main activity
    public boolean verifyGooglePlayServices(Activity activity) {
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                activity.finish();
            }
            return false;
        }

        return true;
    }

    public GoogleApiClient getApiClient() {
        return apiClient;
    }

    private Connection(Activity activity) {
        apiClient = new GoogleApiClient
                .Builder(activity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        apiAvailability = GoogleApiAvailability.getInstance();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Location services failed.");
    }

}
