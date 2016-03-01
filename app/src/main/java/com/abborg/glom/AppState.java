package com.abborg.glom;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.data.DataUpdater;
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
 * As long as the OS does not kill the application context, this class and fields will
 * not get initialized
 *
 * Created by Boat on 22/10/58.
 */
public class AppState
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    // static variables persist as long as the class is in memory
    private static AppState instance = null;

    private Context context;

    /* The current user */
    private User user;

    /* The currently active circle */
    private Circle currentCircle;

    /* List of circle info */
    private List<CircleInfo> circles;

    /* App's shared preferences file */
    private SharedPreferences sharedPref;

    private DataUpdater dataUpdater;

    /* Helper class that verifies Google's Api client */
    private GoogleApiAvailability apiAvailability;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* Whether or not to keep Google Api connection alive */
    private boolean keepGoogleApiAlive;

    /* App-wide date formatter */
    private DateTimeFormatter dateTimeFormatter;

    /* Determines the type of app start */
    public enum AppStart {
        FIRST_TIME, FIRST_TIME_VERSION, NORMAL;
    }

    /* The app version code (not the name) used on the last start of the app */
    private static final String LAST_APP_VERSION = "last_app_version";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Finds out started for the first time (ever or in the current version).<br/>
     * <br/>
     * Note: This method is <b>not idempotent</b> only the first call will
     * determine the proper result. Any subsequent calls will only return
     * {@link AppStart#NORMAL} until the app is started again. So you might want
     * to consider caching the result!
     *
     * @return the type of app start
     */
    public AppStart checkAppStart() {
        PackageInfo pInfo;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        AppStart appStart = AppStart.NORMAL;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int lastVersionCode = sharedPreferences.getInt(LAST_APP_VERSION, -1);
            int currentVersionCode = pInfo.versionCode;
            appStart = checkAppStart(currentVersionCode, lastVersionCode);

            // Update version in preferences
            sharedPreferences.edit()
                    .putInt(LAST_APP_VERSION, currentVersionCode).commit();
        } catch (NameNotFoundException e) {
            Log.w("ERROR",
                    "Unable to determine current app version from package manager. Defensisvely assuming normal app start.");
        }
        return appStart;
    }

    public AppStart checkAppStart(int currentVersionCode, int lastVersionCode) {
        if (lastVersionCode == -1) {
            return AppStart.FIRST_TIME;
        }
        else if (lastVersionCode < currentVersionCode) {
            return AppStart.FIRST_TIME_VERSION;
        }
        else if (lastVersionCode > currentVersionCode) {
            Log.w("ERROR", "Current version code (" + currentVersionCode
                    + ") is less then the one recognized on last startup ("
                    + lastVersionCode
                    + "). Defenisvely assuming normal app start.");
            return AppStart.NORMAL;
        }
        else {
            return AppStart.NORMAL;
        }
    }

    private AppState(Context ctx) {
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

        // initialize the user info
        // if user is not there, TODO sign in again
        dataUpdater = new DataUpdater(context);
        dataUpdater.open();
        user = dataUpdater.getCurrentUser(Const.TEST_USER_ID);
        if (user == null) {
            //TODO SIGN IN
            user = createUser(Const.TEST_USER_NAME, Const.TEST_USER_ID, Const.TEST_USER_AVATAR, Const.TEST_USER_LAT, Const.TEST_USER_LONG);
//            user = createUser(Const.TEST_USER_NAME_2, Const.TEST_USER_ID_2, Const.TEST_USER_AVATAR_2, Const.TEST_USER_LAT_2, Const.TEST_USER_LONG_2);
        }
        dataUpdater.setCurrentUser(user);

        // determine if the user has launched the app before and what version
        init(checkAppStart());
    }

    private void init(AppStart appStart) {
        switch (appStart) {
            case NORMAL:
                Log.d("INIT", "App has launched normally, version is the same");
                break;
            case FIRST_TIME_VERSION:
                Log.d("INIT", "App has been upgraded! Version is different");
                break;
            case FIRST_TIME:
                Log.d("INIT", "App has not been launched before, resetting the state to default");
                dataUpdater.resetCircles();
                dataUpdater.createCircle(
                        context.getResources().getString(R.string.friends_circle_title), null,
                        Const.TEST_CIRCLE_ID
                );
                break;
            default:

        }

        circles = dataUpdater.getCirclesInfo();
        currentCircle = dataUpdater.getCircleById(Const.TEST_CIRCLE_ID);
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    private User createUser(String name, String id, String avatar, double latitude, double longitude) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        User user = new User(name, id, location);
        user.setAvatar(avatar);
        return user;
    }

    public static AppState getInstance(Context context) {
        if (instance == null) {
            instance = new AppState(context.getApplicationContext());
        }
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

    public User getUser() { return user; }

    public void setCurrentCircle(Circle circle) { this.currentCircle = circle; }

    public Circle getCurrentCircle() { return currentCircle; }

    public List<CircleInfo> getCircleInfo() { return circles; }

    public SharedPreferences getSharedPreferences() { return sharedPref; }

    public DataUpdater getDataUpdater() { return dataUpdater; }

    public GoogleApiClient getGoogleApiClient() { return apiClient; }
}
