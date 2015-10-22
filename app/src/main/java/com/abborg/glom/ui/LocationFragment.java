package com.abborg.glom.ui;


import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.abborg.glom.R;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.CircleTransform;
import com.abborg.glom.utils.LayoutUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocationFragment extends SupportMapFragment implements OnMapReadyCallback {
    
    /* Might be null if Google Play services APK is not available. */
    private GoogleMap googleMap;

    /* This context's tag */
    public static final String TAG = "MAP_FRAGMENT";

    /* Stored shared preferences for this app */
    private SharedPreferences sharedPref;

    /* This profile's user */
    private User currentUser;

    /* Map of all the accepted users' locations for easily updating their markers */
    private Map<String, Marker> userMarkers;

    /* Map of all the events in the circle */
    private Map<String, Marker> eventMarkers;

    /* Whether or not this fragment is visible */
    public boolean isFragmentVisible;

    /* The custom user marker view constructed from the custom layour */
    private View userMarkerView;

    /*******************************************************************
     * CONSTANTS
     *******************************************************************/

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
        eventMarkers = new ConcurrentHashMap<>();
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
        mapView.setPadding(0, (int) getResources().getDimension(R.dimen.fragment_margin_top), 0, 0);

        // zoom-to-fit button
        Button zoomToFitBtn = (Button) overlay.findViewById(R.id.zoom_to_fit_btn);
        zoomToFitBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();
                for (Marker marker : userMarkers.values()) {
                    boundBuilder.include(marker.getPosition());
                }
                LatLngBounds bounds = boundBuilder.build();
                int padding = LayoutUtils.dpToPx(getContext(), CAMERA_CENTER_PADDING);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }
        });

        mapView.addView(overlay);

        userMarkerView = inflater.inflate(R.layout.user_marker, null);

        return mapView;
    }

    private void setUpClusterer() {

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
        googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        updateMap(false);
    }

    /**
     * Updates the map by clearing all markers
     *
     * @param clear
     */
    public void updateMap(boolean clear) {
        // initialize the default user's marker from sqlite
        if (googleMap != null) {
            if (clear) {
                userMarkers.clear();
                eventMarkers.clear();
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
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_blue))
                    .position(new LatLng(location.getLatitude(), location.getLongitude()));
            Marker marker = googleMap.addMarker(options);
            setUserMarkerIconAvatar(currentUser.getAvatar(), marker);
            userMarkers.put(currentUser.getId(), marker);

            // update boundary
            boundBuilder.include(marker.getPosition());

            // initialize other markers in the circle
            Circle circle = ((MainActivity) getActivity()).getCurrentCircle();
            List<User> users = circle.getUsers();
            for (User user : users) {
                // we don't need to add another marker for ourselves
                if ( user.getId().equals(currentUser.getId()) ) continue;

                // add the marker
                options = new MarkerOptions()
                        .title(user.getId())
                        .snippet(markerSnippet)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_blue))
                        .position(new LatLng(user.getLocation().getLatitude(), user.getLocation().getLongitude()));
                marker = googleMap.addMarker(options);
                setUserMarkerIconAvatar(user.getAvatar(), marker);
                userMarkers.put(user.getId(), marker);

                // update the boundary
                boundBuilder.include(marker.getPosition());
            }

            // initialize events position markers TODO
            List<Event> events = circle.getEvents();
            DateTimeFormatter formatter = DateTimeFormat.forPattern(getContext().getResources().getString(R.string.card_event_datetime_format));
            for (Event event : events) {

                // only show events with location info or place info
                if (event.getLocation() != null || event.getPlace() != null) {
                    LatLng eventLocation = new LatLng(1, 103);
                    StringBuffer dateLocation = new StringBuffer();
                    if (event.getDateTime() != null) {
                        dateLocation.append(formatter.print(event.getDateTime()) + "\n");
                    }
                    if (event.getPlace() != null) {
                        dateLocation.append(event.getPlace());
                    }
                    else {
                        eventLocation = new LatLng(event.getLocation().getLatitude(), event.getLocation().getLongitude());
                    }

                    // add the marker for event
                    options = new MarkerOptions()
                            .title(event.getName())
                            .snippet(dateLocation.toString())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_red))
                            .position(eventLocation);

                    marker = googleMap.addMarker(options);
                    eventMarkers.put(event.getId(), marker);
                }
            }

            // center the camera around the built boundary of markers
            // if there is only one user in the circle, zoom instead
            if (userMarkers.size() == 1) {
                marker = userMarkers.get(currentUser.getId());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), CAMERA_ZOOM_LEVEL));
            }
            else {
                LatLngBounds bounds = boundBuilder.build();
                int padding = LayoutUtils.dpToPx(getContext(), CAMERA_CENTER_PADDING);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }
        }
    }

    /**
     * Loads the user avatar into the marker
     *
     * @param avatar
     * @param marker
     */
    private void setUserMarkerIconAvatar(String avatar, final Marker marker) {
        Glide.with(getContext())
            .load(avatar).asBitmap().fitCenter()
            .transform(new CircleTransform(getActivity()))
            .into(new SimpleTarget<Bitmap>(getResources().getDimensionPixelSize(R.dimen.marker_avatar_width),
                    getResources().getDimensionPixelSize(R.dimen.marker_avatar_height)) {
                public void onResourceReady(Bitmap bitmap, GlideAnimation animation) {
                    ImageView userMarkerAvatar = (ImageView) userMarkerView.findViewById(R.id.markerAvatar);
                    userMarkerAvatar.setImageBitmap(bitmap);
                    Bitmap userMarker = createDrawableFromView(userMarkerView);
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(userMarker));
                }
            });
    }

    private Bitmap createDrawableFromView(View view) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay()
                .getMetrics(displayMetrics);
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels,
                displayMetrics.heightPixels);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
                view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /**
     * Updates the markers position with the user info. Call this when map has already been set up
     *
     * @param users The list of users with new locations to update
     */
    public void updateUserMarkers(List<User> users) {
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
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_blue))
                            .position(latLng);
                    marker = googleMap.addMarker(options);
                    setUserMarkerIconAvatar(user.getAvatar(), marker);
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
                int padding = LayoutUtils.dpToPx(getContext(), CAMERA_CENTER_PADDING);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }
        }
    }

}
