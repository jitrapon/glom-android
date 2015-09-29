package com.abborg.glom.ui;


import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.RequestHandler;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocationFragment extends SupportMapFragment implements OnMapReadyCallback,
        LocationListener  {
    
    /* Might be null if Google Play services APK is not available. */
    private GoogleMap googleMap;

    /* Current user's location */
    private Location userLocation;

    /* The location request that will be sent to retrieve location */
    private LocationRequest locationRequest;

    /* This context's tag */
    public static final String TAG = "MAP";

    /* Stored shared preferences for this app */
    private SharedPreferences sharedPref;

    /* This profile's user */
    private User currentUser;

    /* Map of all the accepted users' locations for easily updating their markers */
    private Map<String, Marker> userMarkers;

    public boolean isFragmentVisible;

    /*******************************************************************
     * CONSTANTS
     *******************************************************************/

    /* Polling interval of the location request to update the location */
    public static final long LOCATION_REQUEST_INTERVAL = 1500;

    /* Google Map zoom level when a user's location has been identified */
    private static final float CAMERA_ZOOM_LEVEL = 16;

    /* Google Map camera padding offset from the edges of the screen when set bounds in DP */
    private static final float CAMERA_CENTER_PADDING = 35;


    public LocationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedBundleState) {
        super.onCreate(savedBundleState);

        userMarkers = new ConcurrentHashMap<>();

        currentUser = ((MainActivity) getActivity()).getUser();
    }

    @Override
    /**
     * Override to encapsulate the map fragment in a relative layout so that
     * we can add other UI elements to this fragment
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FrameLayout mapView = (FrameLayout) super.onCreateView(inflater, container, savedInstanceState);
        View overlay = inflater.inflate(R.layout.fragment_location_overlay, null);

//        int marginTopDp = (int) TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP, getResources().getDimension(R.dimen.fragment_margin_top), getResources()
//                        .getDisplayMetrics());
        mapView.setPadding(0, (int)getResources().getDimension(R.dimen.fragment_margin_top), 0, 0);

        // add locate button
        Button locateBtn = (Button) overlay.findViewById(R.id.locate_btn);
        locateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getUserLocation(false, LOCATION_REQUEST_INTERVAL);
            }
        });

        mapView.addView(overlay);

        return mapView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("MyMap", "onResume");
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {

        if (googleMap == null) {

            Log.d("MyMap", "setUpMapIfNeeded");

            getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("MyMap", "onMapReady");
        this.googleMap = googleMap;
        setUpMap();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "Location has changed.");
        if (location != null) {
            currentUser.setLocation(location);
            updateUI(Arrays.asList(currentUser));
            if (currentUser.isBroadcastingLocation()) sendLocationUpdateRequest(location);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isFragmentVisible = true;
            Log.i(TAG, "Map is now visible to user");
        }
        else {
            isFragmentVisible = false;
            Log.i(TAG, "Map is now INVISIBLE to user");
        }
    }

    private void setUpMap() {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        updateMap(false);
    }

    public void updateMap(boolean clear) {
        // initialize the default user's marker from sqlite
        if (googleMap != null) {
            if (clear) {
                userMarkers.clear();
                googleMap.clear();
            }

            String markerTitle = currentUser.getId();
            Location location = currentUser.getLocation();
            if (location == null) {
                location.setLatitude(0);
                location.setLongitude(0);
            }

            // marker's default snippet
            String markerSnippet = getResources().getString(R.string.marker_msg_placeholder);

            // set bounds for centering the map around markers
            LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();

            // initialize current user's marker
            MarkerOptions options = new MarkerOptions()
                    .title(markerTitle)
                    .snippet(markerSnippet)
                    .position(new LatLng(location.getLatitude(), location.getLongitude()));
            Marker marker = googleMap.addMarker(options);
            userMarkers.put(currentUser.getId(), marker);

            // update boundary
            boundBuilder.include(marker.getPosition());

            // initialize other markers in the circle
            List<User> users = ((MainActivity) getActivity()).getCurrentCircle().getUsers();
            for (User user : users) {
                // we don't need to add another marker for ourselves
                if ( user.getId().equals(currentUser.getId()) ) continue;

                // add the marker
                options = new MarkerOptions()
                        .title(user.getId())
                        .snippet(markerSnippet)
                        .position(new LatLng(user.getLocation().getLatitude(), user.getLocation().getLongitude()));
                marker = googleMap.addMarker(options);
                userMarkers.put(user.getId(), marker);

                // update the boundary
                boundBuilder.include(marker.getPosition());
            }

            // center the camera around the built boundary of markers
            // if there is only one user in the circle, zoom instead
            if (userMarkers.size() == 1) {
                marker = userMarkers.get(currentUser.getId());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), CAMERA_ZOOM_LEVEL));
            }
            else {
                LatLngBounds bounds = boundBuilder.build();

                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, dpToPx(CAMERA_CENTER_PADDING)));
            }
        }
    }

    public int dpToPx(float dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    /**
     * Retrieves user's current location based on interval, intent, or last known location
     * @param lastLocation if true, use the last known location
     * @param interval The interval in milliseconds to poll for location updates, specify 0 to make a single request
     */
    private void getUserLocation(boolean lastLocation, long interval) {
        GoogleApiClient apiClient = ((MainActivity) getActivity()).getApiClient();

        if (lastLocation) {
            userLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            if (userLocation == null) {
                Log.i(TAG, "Requesting new user location with interval of " + interval);
                if (interval > 0) locationRequest.setInterval(interval);
                LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
            }
            else {
                Log.i(TAG, "User last location is intact");
                currentUser.setLocation(userLocation);
                updateUI(Arrays.asList(currentUser));

                if (currentUser.isBroadcastingLocation()) sendLocationUpdateRequest(userLocation);
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
            body.put("id", currentUser.getId());
            body.put("circle", currentUser.getCurrentCircle().getId());

            JSONObject loc = new JSONObject();
            loc.put("lat", location.getLatitude());
            loc.put("long", location.getLongitude());

            body.put("location", loc);
        }
        catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
        }

        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Const.HOST_ADDRESS, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        RequestHandler.getInstance(getActivity()).handleResponse(getActivity(), response);
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        RequestHandler.getInstance(getActivity()).handleError(error);
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

        RequestHandler.getInstance(getActivity()).addToRequestQueue(request);
    }

    /**
     * Update UI by updating the marking and animating the camera to focus on the new location specified
     *
     * @param users The list of users with new locations to update
     */
    public void updateUI(List<User> users) {
        if (googleMap != null) {

            LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();

            for (User user : users) {
                //TODO ignore the current user location update from the request

                String id = user.getId();
                Location location = user.getLocation();
                Marker marker = userMarkers.get(id);

                double currentLatitude = location.getLatitude();
                double currentLongitude = location.getLongitude();
                LatLng latLng = new LatLng(currentLatitude, currentLongitude);

                // add a new marker belonging to the user if it's not there before
                // TODO marker should show options of time-since-last-update, status message, nearby-place, time-to-destination (ongoing events)
                if (marker == null) {
                    String markerTitle = id;
                    String markerSnippet = getResources().getString(R.string.marker_msg_placeholder);
                    MarkerOptions options = new MarkerOptions()
                            .title(markerTitle)
                            .snippet(markerSnippet)
                            .position(latLng);
                    marker = googleMap.addMarker(options);
                    userMarkers.put(id, marker);
                }
                else {
                    marker.setPosition(latLng);
                }

                boundBuilder.include(marker.getPosition());
            }

            // animate the camera to center around all markers
            // if there is only one then zoom instead
            Log.d(TAG, "USER MARKERS: " + userMarkers.size());
            Marker userMarker = userMarkers.get(currentUser.getId());
            if (userMarkers.size() == 1) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), CAMERA_ZOOM_LEVEL));
            }
            else {
                boundBuilder.include(userMarker.getPosition());
                LatLngBounds bounds = boundBuilder.build();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, dpToPx(CAMERA_CENTER_PADDING)));
            }
        }
    }

}
