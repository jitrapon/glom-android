package com.abborg.glom.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.RequestHandler;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Main activity that renders map and show the user's friends' locations
 */
public class LocationActivity extends FragmentActivity implements OnMapReadyCallback,
        LocationListener, ConnectionCallbacks, OnConnectionFailedListener {

    /* Might be null if Google Play services APK is not available. */
    private GoogleMap googleMap;

    /* Current user's location */
    private Location userLocation;

    /* The location request that will be sent to retrieve location */
    private LocationRequest locationRequest;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* This context's tag */
    public static final String TAG = LocationActivity.class.getSimpleName();

    /* Stored shared preferences for this app */
    private SharedPreferences sharedPref;

    /* This profile's user */
    private User user;

    /* GSON java-to-JSON converter */
    private Gson gson;

    /*******************************************************************
     * CONSTANTS
     *******************************************************************/

    /* Polling interval of the location request to update the location */
    public static final long LOCATION_REQUEST_INTERVAL = 1500;

    /* Google Map zoom level when a user's location has been identified */
    private static final float CAMERA_ZOOM_LEVEL = 16;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // initialize stuff
        init();

        // retrieve the shared preferences
        sharedPref = this.getSharedPreferences( getString(R.string.preference_file_key), Context.MODE_PRIVATE );

        // create and initialize the Google Play Location service
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // initialize the map's default settings
        setUpMapIfNeeded();

        /**
         * Locate button callback
         */
        final Button locateBtn = (Button) findViewById(R.id.locate_me_btn);
        locateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getUserLocation(false, LOCATION_REQUEST_INTERVAL);
//                getUserLocation(true, 0);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        apiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(apiClient.isConnected()) {
            apiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        apiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (apiClient.isConnected()) {
            apiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location has changed.");
        if (location != null) {
            user.setLocation(location);
            updateUI(location);
            sendLocationUpdateRequest(location);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void init() {
        gson = new Gson();

        // retrieve the current user
        user = (User)getIntent().getExtras().getSerializable( getString(R.string.main_user_intent_key) );
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #googleMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (googleMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (googleMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #googleMap} is not null.
     */
    private void setUpMap() {
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
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
                user.setLocation(userLocation);
                updateUI(userLocation);
                sendLocationUpdateRequest(userLocation);
            }
        }
        else {
            Log.i(TAG, "Requesting new user location with interval of " + interval);
            if (interval > 0) locationRequest.setInterval(interval);
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
        }
    }

    private void sendLocationUpdateRequest(Location location) {
        // initialize the body
        JSONObject body =  new JSONObject();

        try {
            body.put("fullName", user.getFullName());
            body.put("name", user.getFunName());

            JSONObject loc = new JSONObject();
            loc.put("lat", location.getLatitude());
            loc.put("long", location.getLongitude());

            body.put("location", loc);
        }
        catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Const.HOST_ADDRESS, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, response.toString());
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, error.getMessage());
                    }
                })

        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("AUTHORIZATION", "GLOM-AUTH-TOKEN abcdefghijklmnopqrstuvwxyz0123456789");
                return headers;
            }

//            @Override
//            protected Map<String, String> getParams() {
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("email", "rm@test.com.br");
//                params.put("senha", "aaa");
//                return params;
//            }
        };

        RequestHandler.getInstance(this).addToRequestQueue(request);
    }

    /**
     * Update UI by adding marking and animating the camera to focus on the new location specified
     * @param location The new location to update the UI with
     */
    private void updateUI(Location location) {
        Toast.makeText(getApplicationContext(), location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        String markerTitle = getResources().getString(R.string.marker_found_title);
        String markerSnippet = user.getFullName() + " (" + user.getFunName() + ")";

        MarkerOptions options = new MarkerOptions()
                .title(markerTitle)
                .snippet(markerSnippet)
                .position(latLng);
        googleMap.addMarker(options);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL));
    }

    @Override
    public void onMapReady(GoogleMap map) {

    }
}