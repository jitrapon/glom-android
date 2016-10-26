package com.abborg.glom;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

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
 * Created by Jitrapon
 */
public class ApplicationState implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    /* The current activeUser */
    private User activeUser;

    /* The currently active circle */
    private Circle activeCircle;

    /* List of circle info */
    private List<CircleInfo> circles;

    /* Google Play API client */
    private GoogleApiClient googleApiClient;

    /* Whthere or not Google API client is connected */
    private boolean googleApiClientConnected;

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

    /* Server connectivity state */
    private ConnectivityStatus connectivityStatus;

    private ConnectivityManager connectivityManager;

    public enum ConnectivityStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public ApplicationState(Context ctx) {
        Context context = ctx.getApplicationContext();

        // check device connectivity
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityStatus = isNetworkAvailable() ? ConnectivityStatus.CONNECTED : ConnectivityStatus.DISCONNECTED;

        // It's important to initialize the ResourceZoneInfoProvider; otherwise
        // joda-time-android will not work.
        JodaTimeAndroid.init(context);

        // initialize the date format
        dateTimeFormatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.action_create_event_datetime_format));

        // initialize the google API key
        GOOGLE_API_KEY = context.getResources().getString(R.string.google_maps_key);

        // initialize the paths to store cached and internal files
        cacheDir = context.getCacheDir();
        internalFilesDir = context.getFilesDir();
        externalFilesDir = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/");

        // make sure the phone has installed required Google Play Services version
        // if it's available, connect to Google API services
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int googlePlayServicesResultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (googlePlayServicesResultCode == ConnectionResult.SUCCESS) {
            googleApiClient = new GoogleApiClient
                    .Builder(context)
                    .addApi(Places.GEO_DATA_API)
                    .addApi(Places.PLACE_DETECTION_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            connectGoogleApiClient();
            keepGoogleApiAlive = false;
        }
        else {
            googleApiClientConnected = false;
        }
    }

    public boolean isNetworkAvailable() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public void setKeepGoogleApiClientAlive(boolean alive) {
        keepGoogleApiAlive = alive;
    }

    public boolean shouldKeepGoogleApiAlive() { return keepGoogleApiAlive; }

    public void connectGoogleApiClient() {
        if (googleApiClient != null && !googleApiClient.isConnected()) {
            googleApiClient.connect();
        }
    }

    public void disconnectGoogleApiClient() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
            Log.d("Google API Client", "Google Places API disconnected");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Google API Client", "Google Places API connected.");
        googleApiClientConnected = true;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e("Google API Client", "Google Places API connection failed with error code: "
                + result.getErrorCode());
        googleApiClientConnected = false;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("Google API Client", "Google Places API connection suspended.");
        googleApiClientConnected = false;
    }

    public void setCircleInfos(List<CircleInfo> info) { circles = info; }

    public void setActiveUser(User user) { activeUser = user; }

    public User getActiveUser() { return activeUser; }

    public void setActiveCircle(Circle circle) { activeCircle = circle; }

    public Circle getActiveCircle() { return activeCircle; }

    public List<CircleInfo> getAllCircleInfo() { return circles; }

    public GoogleApiClient getGoogleApiClient() { return googleApiClient; }

    public boolean isGoogleApiClientConnected() { return googleApiClientConnected; }

    public String getGoogleApiKey() { return GOOGLE_API_KEY; }

    public File getCacheDir() { return cacheDir; }

    public File getInternalFilesDir() { return internalFilesDir; }

    public File getExternalFilesDir() { return externalFilesDir; }

    public ConnectivityStatus getConnectionStatus() { return connectivityStatus; }

    public void setConnectivityStatus(ConnectivityStatus status) { connectivityStatus = status; }
}
