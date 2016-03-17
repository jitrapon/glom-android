package com.abborg.glom;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.model.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

/**
 * Class that handles objects that are to be used for the entire application
 * so long as the OS does not kill the application context, this class and fields will
 * not get initialized
 *
 * Created by Boat on 22/10/58.
 */
public class AppState
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    // static variables persist as long as the class is in memory
    private static AppState instance = null;

    private Context context;

    /* The current activeUser */
    private User activeUser;

    /* The currently active circle */
    private Circle activeCircle;

    /* List of circle info */
    private List<CircleInfo> circles;

    private DataUpdater dataUpdater;

    /* Helper class that verifies Google's Api client */
    private GoogleApiAvailability apiAvailability;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* Whether or not to keep Google Api connection alive */
    private boolean keepGoogleApiAlive;

    /* App-wide date formatter */
    private DateTimeFormatter dateTimeFormatter;

    public static AppState init(Context ctx, Handler handler) {
        instance = new AppState(ctx, handler);
        return instance;
    }

    private AppState(Context ctx, Handler handler) {
        context = ctx.getApplicationContext();

        // It's important to initialize the ResourceZoneInfoProvider; otherwise
        // joda-time-android will not work.
        JodaTimeAndroid.init(context);

        // make sure the phone has installed required Google Play Services version
        // if it's available, connect to Google API services
        apiAvailability = GoogleApiAvailability.getInstance();
        if (verifyGooglePlayServices(context)) {
            apiClient = new GoogleApiClient
                    .Builder(context)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        if (!apiClient.isConnected()) apiClient.connect();
        keepGoogleApiAlive = false;

        dateTimeFormatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.action_create_event_datetime_format));

        DataUpdater.init(this, context, handler);
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public static AppState getInstance() {
        return instance;
    }

    public void setKeepGoogleApiClientAlive(boolean alive) {
        keepGoogleApiAlive = alive;
    }

    public boolean shouldKeepGoogleApiAlive() { return keepGoogleApiAlive; }

    public boolean verifyGooglePlayServices(Context context) {
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
//                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
//                        .show();
            } else {
//                Log.i(TAG, "This device is not supported.");
//                finish();
            }
            return false;
        }
        return true;
    }

    public void connectGoogleApiClient() {
        if (apiClient != null && !apiClient.isConnected()) {
            apiClient.connect();
        }
    }

    public void disconnectGoogleApiClient() {
        if (apiClient != null && apiClient.isConnected()) {
            apiClient.disconnect();
            Log.d("Google API Client", "Google Places API disconnected");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Google API Client", "Google Places API connected.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e("Google API Client", "Google Places API connection failed with error code: "
                + result.getErrorCode());

        Toast.makeText(context,
                "Google Places API connection failed with error code:" +
                        result.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("Google API Client", "Google Places API connection suspended.");
    }

    public void setDataUpdater(DataUpdater updater) { dataUpdater = updater; }

    public void setCircleInfos(List<CircleInfo> info) { circles = info; }

    public void setActiveUser(User user) { activeUser = user; }

    public User getActiveUser() { return activeUser; }

    public void setActiveCircle(Circle circle) { activeCircle = circle; }

    public Circle getActiveCircle() { return activeCircle; }

    public List<CircleInfo> getAllCircleInfo() { return circles; }

    public DataUpdater getDataUpdater() { return dataUpdater; }

    public GoogleApiClient getGoogleApiClient() { return apiClient; }
}
