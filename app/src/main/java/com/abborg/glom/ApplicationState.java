package com.abborg.glom;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.abborg.glom.data.DataProvider;
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

import java.io.File;
import java.util.List;

/**
 * Class that handles objects that are to be used for the entire application
 * so long as the OS does not kill the application context, this class and fields will
 * not get initialized
 *
 * Created by Boat on 22/10/58.
 */
public class ApplicationState
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    // static variables persist as long as the class is in memory
    private static ApplicationState instance = null;

    private Context context;

    /* The current activeUser */
    private User activeUser;

    /* The currently active circle */
    private Circle activeCircle;

    /* List of circle info */
    private List<CircleInfo> circles;

    private DataProvider dataProvider;

    /* Helper class that verifies Google's Api client */
    private GoogleApiAvailability apiAvailability;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* Whether or not to keep Google Api connection alive */
    private boolean keepGoogleApiAlive;

    /* App-wide date formatter */
    private DateTimeFormatter dateTimeFormatter;

    /* API KEY to access Google APIs */
    private String GOOGLE_API_KEY;

    /* Cached and downloaded file paths */
    private File cacheDir;

    /* Internal storage memory */
    private File internalFilesDir;

    /* External storage memory */
    private File externalFilesDir;

    public static ApplicationState init(Context ctx, Handler handler) {
        instance = new ApplicationState(ctx, handler);
        return instance;
    }

    private ApplicationState(Context ctx, Handler handler) {
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

        // initialize the date format
        dateTimeFormatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.action_create_event_datetime_format));

        // initialize the google API key
        GOOGLE_API_KEY = context.getResources().getString(R.string.google_maps_key);

        // initialize the paths to store cached and internal files
        cacheDir = context.getCacheDir();
        internalFilesDir = context.getFilesDir();
        externalFilesDir = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/");

        // initialize model and data provider
        DataProvider.init(this, context, handler);
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public static ApplicationState getInstance() {
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

    public void setDataProvider(DataProvider updater) { dataProvider = updater; }

    public void setCircleInfos(List<CircleInfo> info) { circles = info; }

    public void setActiveUser(User user) { activeUser = user; }

    public User getActiveUser() { return activeUser; }

    public void setActiveCircle(Circle circle) { activeCircle = circle; }

    public Circle getActiveCircle() { return activeCircle; }

    public List<CircleInfo> getAllCircleInfo() { return circles; }

    public DataProvider getDataProvider() { return dataProvider; }

    public GoogleApiClient getGoogleApiClient() { return apiClient; }

    public String getGoogleApiKey() { return GOOGLE_API_KEY; }

    public File getCacheDir() { return cacheDir; }

    public File getInternalFilesDir() { return internalFilesDir; }

    public File getExternalFilesDir() { return externalFilesDir; }
}
