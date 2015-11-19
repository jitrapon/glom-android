package com.abborg.glom;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;

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

    private SharedPreferences sharedPref;

    private DataUpdater dataUpdater;

    /* Helper class that verifies Google's Api client */
    private GoogleApiAvailability apiAvailability;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* App-wide date formatter */
    private DateTimeFormatter dateTimeFormatter;

    private AppState(Context context) {
        this.context = context;

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

        dateTimeFormatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.action_create_event_datetime_format));

        // initialize the user info
        // if user is not there, TODO sign in again
        dataUpdater = new DataUpdater(context);
        dataUpdater.open();
        user = dataUpdater.initCurrentUser(Const.TEST_USER_ID);
        if (user == null) {
            //TODO SIGN IN
            user = createUser(Const.TEST_USER_NAME, Const.TEST_USER_ID, Const.TEST_USER_AVATAR, Const.TEST_USER_LAT, Const.TEST_USER_LONG);
        }
        dataUpdater.setCurrentUser(user);

//        reset();

        init();
    }

    /**
     * Call this to reset the state of everything
     */
    public void reset() {
        dataUpdater.resetCircles();
        dataUpdater.createCircle(
                context.getResources().getString(R.string.friends_circle_title), null, context.getResources().getString(R.string.friends_circle_id)
        );

        circles = dataUpdater.getCirclesInfo();
        currentCircle = dataUpdater.getCircleByName(context.getResources().getString(R.string.friends_circle_title));
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public void init() {
        try {
            dataUpdater.open();
        }
        catch (SQLException ex) {
            Log.e("Database", ex.getMessage());
        }

//        // create event 1 for friends
//        if (currentCircle != null) {
//            DateTime eventTime = dateTimeFormatter.parseDateTime("25/03/2016 15:30:52");
//            String place = "ChIJB5FY5M2e4jARo48nbVRhgAo";
//            Location location = null;
//            dataUpdater.createEvent("Meetup", currentCircle,
//                    new ArrayList<>(Arrays.asList(user)), eventTime, null, place, location, Event.IN_CIRCLE,
//                    new ArrayList<User>(), true, true, true, null
//            );
//        }
//        else
//            Log.d("UNABLE TO CREATE EVENT", "Current circle null");

        circles = dataUpdater.getCirclesInfo();
        currentCircle = dataUpdater.getCircleByName(context.getResources().getString(R.string.friends_circle_title));
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

    public boolean verifyGooglePlayServices(Context context) {
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
//            if (apiAvailability.isUserResolvableError(resultCode)) {
//                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
//                        .show();
//            } else {
//                Log.i(TAG, "This device is not supported.");
//                activity.finish();
//            }
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
