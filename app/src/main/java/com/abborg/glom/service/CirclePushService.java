package com.abborg.glom.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.activities.MainActivity;
import com.abborg.glom.utils.RequestHandler;
import com.abborg.glom.interfaces.ResponseListener;
import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.joda.time.Duration;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This service handles sending background push message to the server
 * This can include location updates and other updates within circles
 * This service does not spawn new thread to do the network operations because
 * Volley automatically handles that.
 *
 * This class expects data to be passed in primitive format (i.e. String, integers)
 * and not serialized or parcelled objects.
 *
 * TODO Need to use PowerManager.PARTIAL_WAKE_LOCK to keep this Service running while device is sleeping
 * TODO or let user have an option
 *
 * Created by Jitrapon Tiachunpun on 8/10/58.
 */
public class CirclePushService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "CirclePushService";

    /* The location request that will be sent to retrieve location */
    private LocationRequest locationRequest;

    /* List of circle id to broadcast location to */
    private ArrayList<String> circles;

    /* List of broadcast location duration for each circle */
    private ArrayList<Float> durations;

    /* Wakelock to keep CPU running */
    private PowerManager.WakeLock wakeLock;

    /* Current user id */
    private String userId;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* Helper class that verifies Google's Api client */
    private GoogleApiAvailability apiAvailability;

    /* Current user's location */
    private Location userLocation;

    /* Polling interval of the location request to update the location */
    public static final long LOCATION_REQUEST_INTERVAL = 3000;

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Service receiving start command");

        // handle each intent separately
        handleCommand(intent);

        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location has changed.");
        if (location != null) {

            // broadcast to MainActivity
            userLocation = location;
            Intent intent = new Intent(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE));
            intent.putExtra(getResources().getString(R.string.EXTRA_USER_LOCATION_UPDATE), location);
            intent.putStringArrayListExtra(getResources().getString(R.string.EXTRA_CIRCLES_LOCATION_UPDATE), circles);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            // loop through list of circles and send updates to them
            if (!circles.isEmpty()) {
                AppState appState = AppState.getInstance(this);
                appState.getDataUpdater().open();
                for (String circleId : circles) {
                    sendLocationUpdateRequest(circleId, userLocation);
                    appState.getDataUpdater().updateUserLocation(appState.getUser().getId(), circleId, location.getLatitude(), location.getLongitude());
                    Log.d(TAG, "Sending location info of " + location.getLatitude() + ", " + location.getLongitude());
                }
            }
        }
    }

    /**
     * Retrieves user's current location based on interval, intent, or last known location
     * @param lastLocation if true, use the last known location
     * @param interval The interval in milliseconds to poll for location updates, specify 0 to make a single request
     */
    private void getUserLocation(boolean lastLocation, long interval) {
        if (lastLocation) {
            userLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            if (userLocation == null) {
                Log.i(TAG, "Requesting new user location with interval of " + interval);
                if (interval > 0) locationRequest.setInterval(interval);
                LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
            }
            else {
                Log.i(TAG, "User last location is intact");

                // broadcast to MainActivity
                Intent intent = new Intent(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE));
                intent.putExtra(getResources().getString(R.string.EXTRA_USER_LOCATION_UPDATE), userLocation);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                // loop through list of circles and send updates to them
                for (String circleId : circles) {
                    sendLocationUpdateRequest(circleId, userLocation);
                }
            }
        }
        else {
            Log.i(TAG, "Requesting new user location with interval of " + interval);
            if (interval > 0) locationRequest.setInterval(interval);
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
        }
    }

    /**
     * POST /location
     * Sends location update to the server
     */
    private void sendLocationUpdateRequest(String circleId, Location location) {
        JSONObject body =  new JSONObject();
        final Context ctx = this;

        try {
            body.put(Const.JSON_SERVER_USERID, userId);
            body.put(Const.JSON_SERVER_CIRCLEID, circleId);

            JSONObject loc = new JSONObject();
            loc.put(Const.JSON_SERVER_LOCATION_LAT, location.getLatitude());
            loc.put(Const.JSON_SERVER_LOCATION_LONG, location.getLongitude());

            body.put(Const.JSON_SERVER_LOCATION, loc);
        }
        catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
        }

        RequestHandler.getInstance(ctx).post("Update Location", Const.API_LOCATION, body, new ResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                if (response != null) {
                    try {
                        String message = response.getString(Const.JSON_SERVER_MESSAGE);
                        if (message != null && getApplicationContext() != null)
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                    }
                    catch (JSONException ex) {
                        Log.e(TAG, ex.getMessage());
                        if (getApplicationContext() != null)
                            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onError(VolleyError error) {
                RequestHandler.getInstance(ctx).handleError(error);
            }
        });
    }

    private void showNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, Const.NOTIFY_BROADCAST_LOCATION, intent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (circles.size() < 1) {
            notificationManager.cancel(Const.NOTIFY_BROADCAST_LOCATION);
        }

        StringBuilder messageBuilder = new StringBuilder();
        String messageTitle = getResources().getString(R.string.notification_title_broadcast_location);
        for (String name : circles) {
            messageBuilder.append(name + ", ");
        }
        messageBuilder.setLength(Math.max(messageBuilder.length() - 2, 0));

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_action_place)
                .setContentTitle(messageTitle)
                .setContentText(messageBuilder.toString())
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_action_cancel,
                        getResources().getString(R.string.notification_action_cancel_broadcast_location), pendingIntent);

        notificationManager.notify(Const.NOTIFY_BROADCAST_LOCATION, notificationBuilder.build());
    }

    private void hideNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Const.NOTIFY_BROADCAST_LOCATION);
    }

    private void handleCommand(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            userId = intent.getStringExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_USER_ID));
            String circleId = intent.getStringExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_CIRCLE_ID));
            long duration = intent.getLongExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_DURATION), -1);

            // action to add the user's circle to list of broadcast
            if (action.equals(getResources().getString(R.string.ACTION_CIRCLE_ENABLE_LOCATION_BROADCAST))) {
                if (!circles.contains(circleId)) {
                    circles.add(circleId);

                    // acquire the wakelock to prevent CPU from sleeping
                    if (wakeLock != null && circles.size() == 1) {
                        wakeLock.acquire();
                        Log.d(TAG, "Acquiring CPU Wakelock");
                    }

                    // if we receive a duration that's not -1, we set an alarm to stop broadcasting
                    //TODO
                    if (duration != -1) {
                        Duration broadcastDuration = new Duration(duration);
                        Handler handler = new Handler();
                        duration = 7000;
                        handler.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "TIME TO CUT OFF BROADCAST!", Toast.LENGTH_SHORT).show();
                            }
                        }, duration);
                    }
                }

                // display notification to user
                showNotification();
            }

            // action to remove the user's circle to list of broadcast
            else if (action.equals(getResources().getString(R.string.ACTION_CIRCLE_DISABLE_LOCATION_BROADCAST))) {
                circles.remove(circleId);

                // display notification to user
                showNotification();
            }

            // disconnect Google API client
            if (circles.isEmpty()) {
                if (apiClient.isConnected()) {
                    apiClient.disconnect();
                    Log.d(TAG, "No more circle to broadcast. Location services disconnected");
                }

                // remove any wakelock
                if (wakeLock != null) {
                   if (wakeLock.isHeld()) {
                       wakeLock.release();
                       Log.d(TAG, "Released CPU wakelock");
                   }
                }

                // end the service
                stopSelf();
            }
            else {
                if (!apiClient.isConnected()) apiClient.connect();
                for (String broadcastCircleId : circles) {
                    Log.d(TAG, "Broadcasting locations to " + broadcastCircleId);
                }
            }
        }
    }

    //TODO verify in onCreate and onResume in the main activity
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

    /**
     * One-time initialization of service. If service is already started, this is not called.
     */
    @Override
    public void onCreate() {
        circles = new ArrayList<>();
        durations = new ArrayList<>();

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        apiAvailability = GoogleApiAvailability.getInstance();

        if (verifyGooglePlayServices(this)) {
            apiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    }

    /**
     * Clean up before service is destroyed from stopService()
     */
    @Override
    public void onDestroy() {
        if (apiClient.isConnected()) apiClient.disconnect();
        hideNotification();
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "Released CPU wakelock");
            }
        }
        Log.d(TAG, "Service done");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        getUserLocation(false, LOCATION_REQUEST_INTERVAL);
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
