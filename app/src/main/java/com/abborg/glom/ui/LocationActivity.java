package com.abborg.glom.ui;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.abborg.glom.R;
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

/**
 * Main activity that renders map and show the user's friends' locations
 */
public class LocationActivity extends FragmentActivity implements OnMapReadyCallback,
        LocationListener, ConnectionCallbacks, OnConnectionFailedListener {

    /* Might be null if Google Play services APK is not available. */
    private GoogleMap mMap;

    /* Current user's location */
    private Location userLocation;

    /* Current user's coordinates (lat and long) */
    private LatLng userLatLng;

    private LocationRequest mLocationRequest;

    private long locationRequestInterval = 1500;

    private GoogleApiClient mGoogleApiClient;

    private static final float CAMERA_ZOOM_LEVEL = 16;

    public static final String TAG = LocationActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        setUpMapIfNeeded();

        /**
         * Find me location button callback
         */
        final Button locateBtn = (Button) findViewById(R.id.locate_me_btn);
        locateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getUserLocation(false, locationRequestInterval);
//                getUserLocation(true, 0);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
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
        if (location != null) updateUI(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
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
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    /**
     * Retrieves user's current location based on interval, intent, or last known location
     * @param lastLocation if true, use the last known location
     * @param interval The interval in milliseconds to poll for location updates, specify 0 to make a single request
     */
    private void getUserLocation(boolean lastLocation, long interval) {
        if (lastLocation) {
            userLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (userLocation == null) {
                Log.i(TAG, "Requesting new user location with interval of " + interval);
                if (interval > 0) mLocationRequest.setInterval(interval);
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
            else {
                Log.i(TAG, "User last location is intact");
                updateUI(userLocation);
            }
        }
        else {
            Log.i(TAG, "Requesting new user location with interval of " + interval);
            if (interval > 0) mLocationRequest.setInterval(interval);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
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
        String markerSnippet = getResources().getString(R.string.marker_found_snippet);

        MarkerOptions options = new MarkerOptions()
                .title(markerTitle)
                .snippet(markerSnippet)
                .position(latLng);
        mMap.addMarker(options);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, CAMERA_ZOOM_LEVEL));
    }

    @Override
    public void onMapReady(GoogleMap map) {

    }
}