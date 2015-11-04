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

    private AppState(Context context) {
        this.context = context;

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

        user = createUser(Const.TEST_USER_NAME, Const.TEST_USER_ID, Const.TEST_USER_AVATAR, Const.TEST_USER_LAT, Const.TEST_USER_LONG);

        dataUpdater = new DataUpdater(context, user);
//        dataUpdater.open();
//        dataUpdater.resetCircles();
//
//        // populate the default circle
//        // default circle contains all unique users that the current user has
//        Circle friendCircle = dataUpdater.createCircle(
//                context.getResources().getString(R.string.friends_circle_title),
//                new ArrayList<>(Arrays.asList(
//                        createUser("Cat", "pusheen", "http://data.whicdn.com/images/139778481/superthumb.jpg", 1.003, 103.0),
//                        createUser("Sunadda", "fatcat18", "http://images8.cpcache.com/image/17244178_155x155_pad.png", 1.0, 102.1441)
//                )), context.getResources().getString(R.string.friends_circle_id)
//        );
//
//        // create event 1 for friends
//        DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
//        DateTime eventTime = formatter.parseDateTime("25/03/2016 15:30:52");
//        String place = "ChIJB5FY5M2e4jARo48nbVRhgAo";
//        Location location = null;
//        dataUpdater.createEvent("Meetup", friendCircle,
//                new ArrayList<>(Arrays.asList(user)), eventTime, place, location, Event.IN_CIRCLE,
//                new ArrayList<User>(), true, true, true, null
//        );
//
//        // create event 2 for friends
//        eventTime = null;
//        place = "ChIJq-I3V62f4jAR8QRewT7N3to";
//        location = null;
//        dataUpdater.createEvent("This is a party", friendCircle,
//                new ArrayList<>(Arrays.asList(user)), eventTime, place, location, Event.IN_CIRCLE,
//                new ArrayList<User>(), true, true, true, "Test new event party message to everyone here..."
//        );
//
//        // another circle
//        Circle circle1 = dataUpdater.createCircle("My Love",
//                new ArrayList<User>(Arrays.asList(
//                        createUser("Sunadda", "fatcat18", "http://images8.cpcache.com/image/17244178_155x155_pad.png", 1.0, 102.1441)
//                )), "my-love"
//        );
//
//        // create event 1 for my-love
//        eventTime = formatter.parseDateTime("18/10/2015 09:00:00");
//        place = null;
//        location = null;
//        dataUpdater.createEvent("Nad's birthday", circle1,
//                new ArrayList<>(Arrays.asList(user)), eventTime, place, location, Event.IN_CIRCLE,
//                new ArrayList<User>(), true, true, true, null
//        );
//
//        eventTime = null;
//        place = null;
//        location = new Location("");
//        location.setLatitude(13.732756);
//        location.setLongitude(100.643101);
//        dataUpdater.createEvent("Event with exact coordinate", circle1,
//                new ArrayList<>(Arrays.asList(user)), eventTime, place, location, Event.IN_CIRCLE,
//                new ArrayList<User>(), true, true, true, "Thus it was not rare to find, on the Sunday, the tallboy on its feet by the fire, and the dressing table on its head by the bed, and the night-stool on its face by the door, and the washand-stand on its back by the window; and, on the Monday, the tallboy on its back by the bed, and the dressing table on its face by the door, and the night-stool on its back by the window and the washand-stand on its feet by the fire; and on the Tuesday…");

        init();
    }

    public void init() {
        try {
            dataUpdater.open();
        }
        catch (SQLException ex) {
            Log.e("Database", ex.getMessage());
        }

        circles = dataUpdater.getCirclesInfo();
        currentCircle = dataUpdater.getCircleByName("My Love");
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

    public void cleanup() {
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
