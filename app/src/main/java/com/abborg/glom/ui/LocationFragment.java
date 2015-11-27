package com.abborg.glom.ui;


import android.animation.Animator;
import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.CircleTransform;
import com.abborg.glom.utils.LayoutUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocationFragment extends SupportMapFragment implements OnMapReadyCallback, CircleFragment.OnBroadcastLocationListener {
    
    /* Might be null if Google Play services APK is not available. */
    private GoogleMap googleMap;

    /* This context's tag */
    public static final String TAG = "MAP_FRAGMENT";

    /* Map of all the accepted users' locations for easily updating their markers */
    private Map<String, Marker> userMarkers;

    /* Map of all the events in the circle */
    private Map<String, Marker> eventMarkers;

    /* Whether or not this fragment is visible */
    public boolean isFragmentVisible;

    /* The custom user marker view constructed from the custom layour */
    private View userMarkerView;

    private AppState appState;

    /* List of event markers that need to be updated */
    private List<Event> staleEvents;

    /* Datetime formatter for displaying formatted time in the info window */
    private DateTimeFormatter formatter;

    /* The current visible radius of the map, which is updated on every camera change */
    private float visibleMapRadius;

    /* The circle displayed to show the current user is broadcasting */
    private com.google.android.gms.maps.model.Circle broadcastCircle;

    /* Holds a temporary zoom level to detect zoom level change */
    private float zoomLevel = -1.0f;

    /* Broadcast circle animator */
    private ValueAnimator animator;

    /*******************************************************************
     * CONSTANTS
     *******************************************************************/

    /* Google Map zoom level when a user's location has been identified */
    private static final float CAMERA_ZOOM_LEVEL = 12;

    /* Google Map camera padding offset from the edges of the screen when set bounds in DP */
    private static final float CAMERA_CENTER_PADDING = 35;

    /* Ratio of broadcasting-location circle to the visible map */
    private static final float CIRCLE_TO_MAP_RATIO = 0.6f;


    public LocationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedBundleState) {
        super.onCreate(savedBundleState);
        userMarkers = new ConcurrentHashMap<>();
        eventMarkers = new ConcurrentHashMap<>();
        staleEvents = new ArrayList<>();
        appState = AppState.getInstance(getContext());
        formatter = DateTimeFormat.forPattern(getContext().getResources().getString(R.string.card_event_datetime_format));
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

            // TODO depending on settings, start CirclePushService to start tracking user location without broadcasting

            updateMarkersInfo();

            if (userMarkers != null && broadcastCircle == null && appState.getCurrentCircle().isUserBroadcastingLocation()) {
                Marker userMarker = userMarkers.get(appState.getUser().getId());
                if (userMarker != null && appState.getCurrentCircle().isUserBroadcastingLocation()) {
                    addBroadcastLocationCircle(userMarker);
                }
            }
            if (animator != null && !animator.isRunning()) animator.start();
        }
        else {
            isFragmentVisible = false;
            // TODO depending on settings, stop CirclePushService
            Log.i(TAG, "Map is now INVISIBLE to user");

            if (animator != null && animator.isRunning()) {
                animator.end();
            }
            if (broadcastCircle != null) {
                broadcastCircle.remove();
                broadcastCircle = null;

                Log.d(TAG, "Removed broadcast circle");
            }
        }
    }

    /**
     * Called when the map is ready after initialization
     */
    private void setUpMap() {
        googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setBuildingsEnabled(true);
        googleMap.setIndoorEnabled(true);

        // set up custom info window style
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Context context = getContext().getApplicationContext();

                LinearLayout info = new LinearLayout(context);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(context);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });

        // on event window info click
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                for (String eventId : eventMarkers.keySet()) {
                    if (eventMarkers.get(eventId).getId().equals(marker.getId())) {
                        Log.d(TAG, "Event (" + eventId + ") selected");
                        Intent intent = new Intent(getActivity(), EventActivity.class);
                        intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_ID), eventId);
                        intent.setAction(getResources().getString(R.string.ACTION_UPDATE_EVENT));
                        AppState.getInstance(getActivity()).setKeepGoogleApiClientAlive(true);
                        getActivity().startActivityForResult(intent, Const.UPDATE_EVENT_RESULT_CODE);
                    }
                }

            }
        });

        // initialize the visible map radius
        visibleMapRadius = getVisibleMapRadius(googleMap);

        // set up camera change event listener
        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (zoomLevel != cameraPosition.zoom) {
                    visibleMapRadius = getVisibleMapRadius(googleMap);
                    Log.d(TAG, "Visible map radius is approximately " + visibleMapRadius + " meters");
                }
                zoomLevel = cameraPosition.zoom;
            }
        });

        update(false);
    }

    /**
     * Called when the UI is to be updated when an event is added
     * @param id
     */
    public void onEventAdded(String id) {
        List<Event> events = appState.getCurrentCircle().getEvents();
        Event newEvent = null;
        for (Event event : events) {
            if (event.getId().equals(id)) newEvent = event;
        }
        if (newEvent == null) return;

        addMarker(newEvent);
    }

    private void addMarker(Event event) {
        if (event.getLocation() != null || event.getPlace() != null) {
            MarkerOptions options = new MarkerOptions()
                    .title(event.getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_meetup))
                    .position(new LatLng(0, 0));
            Marker eventMarker = googleMap.addMarker(options);

            StringBuffer dateLocation = new StringBuffer();
            if (event.getDateTime() != null) {
                dateLocation.append(formatter.print(event.getDateTime()) + "\n");
            }
            if (event.getPlace() != null) {
                staleEvents.add(event);
            }
            if (event.getLocation() != null) {
                dateLocation.append(event.getLocation().getLatitude() + ", " + event.getLocation().getLongitude());
                LatLng eventLocation = new LatLng(event.getLocation().getLatitude(), event.getLocation().getLongitude());
                eventMarker.setPosition(eventLocation);
            }

            eventMarker.setSnippet(dateLocation.toString());
            eventMarkers.put(event.getId(), eventMarker);

            Log.d(TAG, "Marker for event " + event.getName() + " added");
        }
        else {
            Log.d(TAG, "Marker for event " + event.getName() + " NOT added because a place or a coordinate has not been set");
        }
    }

    /**
     * Called when the UI is to be updated when an event is to be changed
     * @param id
     */
    public void onEventChanged(String id) {
        List<Event> events = appState.getCurrentCircle().getEvents();
        Event event = null;
        for (Event e : events) {
            if (e.getId().equals(id)) event = e;
        }
        if (event == null) return;

        Marker changedEventMarker = eventMarkers.get(id);

        // if null, it means that we haven't added the marker yet, which can be due to the user
        // not setting location prior to this change. We proceed to add the marker to the map.
        if (event.getPlace() != null || event.getLocation() != null) {
            if (changedEventMarker == null) {
                addMarker(event);
            }
            else {
                StringBuffer dateLocation = new StringBuffer();
                if (event.getDateTime() != null) {
                    dateLocation.append(formatter.print(event.getDateTime()) + "\n");
                }
                if (event.getPlace() != null) {
                    staleEvents.add(event);
                }
                if (event.getLocation() != null) {
                    dateLocation.append(event.getLocation().getLatitude() + ", " + event.getLocation().getLongitude());
                    LatLng eventLocation = new LatLng(event.getLocation().getLatitude(), event.getLocation().getLongitude());
                    changedEventMarker.setPosition(eventLocation);
                }
                changedEventMarker.setSnippet(dateLocation.toString());
                changedEventMarker.hideInfoWindow();
                changedEventMarker.showInfoWindow();

                Log.d(TAG, "Marker for event " + id + " changed");
            }
        }
        else {
            if (changedEventMarker != null) {
                changedEventMarker.remove();
                eventMarkers.remove(event.getId());
            }
            Log.d(TAG, "Marker for event " + id + " removed because a location is not provided");
        }
    }

    /**
     * Called when the UI is to be updated when an event is to be removed
     */
    public void onEventRemoved(String id) {
        Marker removedEventMarker = eventMarkers.get(id);
        if (removedEventMarker == null) return;
        else {
            removedEventMarker.remove();
            eventMarkers.remove(id);
        }
        Log.d(TAG, "Marker for event " + id + " removed");
    }


    /**
     * Called when the UI is to be updated when broadcasting location is enabled
     */
    @Override
    public void onBroadcastLocationEnabled() {
        Log.d(TAG, "Broadcast location is enabled");

        if (userMarkers != null && broadcastCircle == null) {
            Marker userMarker = userMarkers.get(appState.getUser().getId());
            if (userMarker != null && appState.getCurrentCircle().isUserBroadcastingLocation()) {
                addBroadcastLocationCircle(userMarker);
            }
        }
        if (animator != null && !animator.isRunning()) animator.start();
    }

    /**
     * Called when the UI is to be updated when broadcasting location is disabled
     */
    @Override
    public void onBroadcastLocationDisabled() {
        Log.d(TAG, "Broadcast location is disabled");

        if (animator != null && animator.isRunning()) {
            animator.end();
        }
        if (broadcastCircle != null) {
            broadcastCircle.remove();
            broadcastCircle = null;

            Log.d(TAG, "Removed broadcast circle");
        }
    }

    /**
     * Calculates the visible map's radius and returns it in meters
     *
     * @return The approximate radius of the visible map in meters
     */
    private float getVisibleMapRadius(GoogleMap googleMap) {
        LatLngBounds mapBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;

        Location center = new Location("center");
        center.setLatitude(mapBounds.getCenter().latitude);
        center.setLongitude(mapBounds.getCenter().longitude);

        Location centerLeft = new Location("center");
        centerLeft.setLatitude(center.getLatitude());
        centerLeft.setLongitude(mapBounds.southwest.longitude);

        return center.distanceTo(centerLeft);
    }

    private void updateMarkersInfo() {
        // update list of stale event markers
        if (!staleEvents.isEmpty()) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (Event event : staleEvents) {
                        updateEventMarkers(event);
                    }
                }
            }, 100);
        }

        // update list of user markers if set to show places nearby
        if (true) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (Marker marker : userMarkers.values()) {
                        setMarkerSnippet(marker);
                    }
                }
            }, 100);
        }
    }

    /**
     * Updates the map by clearing all markers
     *
     * @param clear
     */
    public void update(boolean clear) {
        // initialize the default user's marker from sqlite
        if (googleMap != null) {
            if (clear) {
                userMarkers.clear();
                eventMarkers.clear();
                googleMap.clear();
            }

            // set bounds for centering the map around markers
            LatLngBounds.Builder boundBuilder = new LatLngBounds.Builder();

            MarkerOptions options = null;
            Marker marker = null;

            // initialize other markers in the circle
            Circle circle = appState.getCurrentCircle();
            for (User user : circle.getUsers()) {

                // for current user
                if ( user.getId().equals(appState.getUser().getId()) ) {
                    options = new MarkerOptions()
                            .title(user.getId())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_red))
                            .position(new LatLng(user.getLocation().getLatitude(), user.getLocation().getLongitude()));
                    marker = googleMap.addMarker(options);
                    setUserMarkerIconAvatar(appState.getUser().getAvatar(), marker, "red");
                    marker.showInfoWindow();

                    // if the user is currently broadcasting location, animate circle around the marker
                    if (appState.getCurrentCircle().isUserBroadcastingLocation()) {
                        if (broadcastCircle == null) {
                            addBroadcastLocationCircle(marker);
                        }
                        if (animator != null && !animator.isRunning()) animator.start();
                    }
                    else {
                        if (animator != null && animator.isRunning()) {
                            animator.end();
                        }
                        if (broadcastCircle != null) {
                            broadcastCircle.remove();
                            broadcastCircle = null;
                            Log.d(TAG, "Removed broadcast circle");
                        }
                    }
                }

                // for other users
                else {
                    options = new MarkerOptions()
                            .title(user.getId())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_blue))
                            .position(new LatLng(user.getLocation().getLatitude(), user.getLocation().getLongitude()));
                    marker = googleMap.addMarker(options);
                    setUserMarkerIconAvatar(user.getAvatar(), marker, "blue");
                }
                userMarkers.put(user.getId(), marker);

                // update the boundary
                boundBuilder.include(marker.getPosition());
            }

            // initialize events position markers TODO
            List<Event> events = circle.getEvents();
            for (Event event : events) {

                options = new MarkerOptions()
                        .title(event.getName())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_meetup))
                        .position(new LatLng(0, 0));
                Marker eventMarker = googleMap.addMarker(options);

                // only show events with location info or place info
                if (event.getLocation() != null || event.getPlace() != null) {
                    StringBuffer dateLocation = new StringBuffer();
                    if (event.getDateTime() != null) {
                        dateLocation.append(formatter.print(event.getDateTime()) + "\n");
                    }
                    if (event.getPlace() != null) {
                        staleEvents.add(event);
                    }
                    if (event.getLocation() != null) {
                        dateLocation.append(event.getLocation().getLatitude() + ", " + event.getLocation().getLongitude());
                        LatLng eventLocation = new LatLng(event.getLocation().getLatitude(), event.getLocation().getLongitude());
                        eventMarker.setPosition(eventLocation);
                    }

                    eventMarker.setSnippet(dateLocation.toString());
                    eventMarkers.put(event.getId(), eventMarker);
                }
            }

            // center the camera around the built boundary of markers
            // if there is only one user in the circle, zoom instead
            if (userMarkers.size() == 1) {
                marker = userMarkers.get(appState.getUser().getId());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), CAMERA_ZOOM_LEVEL));
            }
            else {
                LatLngBounds bounds = boundBuilder.build();
                int padding = LayoutUtils.dpToPx(getContext(), CAMERA_CENTER_PADDING);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }

            updateMarkersInfo();
        }
    }

    private void addBroadcastLocationCircle(Marker marker) {
        CircleOptions circleOptions = new CircleOptions()
                .center(marker.getPosition())
                .radius(CIRCLE_TO_MAP_RATIO * visibleMapRadius)           // radius in meters
                .fillColor(Color.TRANSPARENT)  // 55 represents percentage of transparency. For 100% transparency, specify 00.
                        // For 0% transparency ( ie, opaque ) , specify ff
                        // The remaining 6 characters(00ff00) specify the fill color
                .strokeColor(0x55000000)
                .strokeWidth(5);
        broadcastCircle = googleMap.addCircle(circleOptions);

        // add animation to the circle
        animator = new ValueAnimator();
//                    animator.setRepeatCount(ValueAnimator.INFINITE);
//                    animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setIntValues(0, Math.round(CIRCLE_TO_MAP_RATIO * visibleMapRadius));  // minimum and maximum radius of circle to animate
        animator.setDuration(2000);
        animator.setStartDelay(1000);
        animator.setFrameDelay(30);
        animator.setEvaluator(new IntEvaluator());
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (broadcastCircle != null) {
                    int animatedValue = (Integer) valueAnimator.getAnimatedValue();
                    broadcastCircle.setRadius(animatedValue);
                }
            }
        });
        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "Animation STARTED");
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (appState.getCurrentCircle().isUserBroadcastingLocation()) {
                    animator.setIntValues(0, Math.round(CIRCLE_TO_MAP_RATIO * visibleMapRadius));
                    animator.setStartDelay(1000);
                    animator.start();
                }
                Log.d(TAG, "Animation ENDED");
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                Log.d(TAG, "Animation CANCELED");
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                Log.d(TAG, "Animation REPEATED");
            }
        });

        Log.d(TAG, "Added broadcast circle");
    }

    private void updateEventMarkers(final Event event) {
        // connect to Google's PlaceAPI to update the location
        GoogleApiClient apiClient = AppState.getInstance(getContext()).getGoogleApiClient();
        if (apiClient != null && apiClient.isConnected()) {
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(apiClient, event.getPlace());
            placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {

                @Override
                public void onResult(PlaceBuffer places) {
                    if (!places.getStatus().isSuccess()) {
                        Log.e(TAG, "Update place marker did not complete. Error: " + places.getStatus().toString());
                        places.release();
                        return;
                    }

                    // display the first place in the list
                    final Place place = places.get(0);
                    Marker marker = eventMarkers.get(event.getId());
                    if (marker != null && place != null) {
                        marker.setPosition(place.getLatLng());
                        StringBuffer dateLocation = new StringBuffer();
                        if (event.getDateTime() != null) {
                            dateLocation.append(formatter.print(event.getDateTime()) + "\n");
                        }
                        dateLocation.append(place.getName());
                        marker.setSnippet(dateLocation.toString());

                        int updatedEventIndex = staleEvents.indexOf(event);
                        if (updatedEventIndex >= 0 && updatedEventIndex < staleEvents.size())
                            staleEvents.remove(updatedEventIndex);

                        Log.d(TAG, "Place marker query succeeded for " + place.getName());
                    }

                    places.release();
                }
            });
        }
        else {
            Log.e(TAG, "Google API client is not connected, error retrieving place info");
        }
    }

    /**
     * Loads the user avatar into the marker
     *
     * @param avatar
     * @param marker
     */
    private void setUserMarkerIconAvatar(String avatar, final Marker marker, final String color) {
        Glide.with(getContext())
            .load(avatar).asBitmap().fitCenter()
            .transform(new CircleTransform(getActivity()))
                .into(new SimpleTarget<Bitmap>(getResources().getDimensionPixelSize(R.dimen.marker_avatar_width),
                        getResources().getDimensionPixelSize(R.dimen.marker_avatar_height)) {
                    public void onResourceReady(Bitmap bitmap, GlideAnimation animation) {
                        ImageView userMarkerBasePin = (ImageView) userMarkerView.findViewById(R.id.markerBasePin);
                        if (color.equals("blue")) {
                            userMarkerBasePin.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_marker_blue));
                        } else if (color.equals("green")) {
                            userMarkerBasePin.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_marker_green));
                        } else if (color.equals("grey")) {
                            userMarkerBasePin.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_marker_grey));
                        } else if (color.equals("orange")) {
                            userMarkerBasePin.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_marker_orange));
                        } else if (color.equals("purple")) {
                            userMarkerBasePin.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_marker_purple));
                        } else if (color.equals("red")) {
                            userMarkerBasePin.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_marker_red));
                        }

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

    private void setMarkerSnippet(final Marker marker) {
        // TODO marker snippet shows options of time-since-last-update, status message, nearby-place, time-to-destination (ongoing events)
        // show nearby-place for this user
        if (true) {
            if (userMarkers.get(appState.getUser().getId()).getId().equals(marker.getId())) {
                GoogleApiClient apiClient = AppState.getInstance(getContext()).getGoogleApiClient();
                if (apiClient != null && apiClient.isConnected()) {
                    PendingResult<PlaceLikelihoodBuffer> placeResult = Places.PlaceDetectionApi.getCurrentPlace(apiClient, null);
                    placeResult.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {

                        @Override
                        public void onResult(PlaceLikelihoodBuffer places) {
                            if (!places.getStatus().isSuccess()) {
                                Log.e(TAG, "Update user marker place did not complete. Error: " + places.getStatus().toString());
                                places.release();
                                return;
                            }

                            // display the first place in the list
                            final Place place = places.get(0).getPlace();
                            marker.setSnippet(place.getName() + "");

                            Log.d(TAG, "User place marker query succeeded for " + place.getName());

                            places.release();
                        }
                    });
                }
            }
            else {
                marker.setSnippet(marker.getPosition().latitude + ", " + marker.getPosition().longitude);
            }
        }

        // show user status
        //TODO
        else if (false) {
            // find the user
            User markerUser = null;
            for (User user : appState.getCurrentCircle().getUsers()) {
                if (userMarkers.get(user.getId()).getId().equals(marker.getId()))
                   markerUser = user;
            }

            if (markerUser != null) {
                marker.setSnippet("What's up?");
            }
        }

        // show user's distance to destination
        //TODO
        else if (true) {

        }

        // show user's time to destination
        //TODO
        else if (true) {

        }

        // show time since last update
        else {

        }
    }



    /**
     * Updates the markers position with the user info. Call this when map has already been set up
     *
     * @param users The list of users with new locations to update
     */
    public void updateUserMarkers(List<User> users) {
        if (googleMap != null) {

            for (User user : users) {
                //TODO ignore the current user location update from the request

                String id = user.getId();
                Location location = user.getLocation();
                Marker marker = userMarkers.get(id);

                double currentLatitude = location.getLatitude();
                double currentLongitude = location.getLongitude();
                LatLng latLng = new LatLng(currentLatitude, currentLongitude);

                // update broadcast circle location if this is the current user
                if (id.equals(appState.getUser().getId()) && broadcastCircle != null) {
                    broadcastCircle.setCenter(latLng);
                }

                // add a new marker belonging to the user if it's not there before
                if (marker == null) {
                    String markerTitle = id;

                    MarkerOptions options = new MarkerOptions()
                            .title(markerTitle)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_blue))
                            .position(latLng);
                    marker = googleMap.addMarker(options);
                    setUserMarkerIconAvatar(user.getAvatar(), marker, "blue");
                    marker.showInfoWindow();
                    userMarkers.put(id, marker);
                    setMarkerSnippet(marker);
                }
                else {
                    marker.setPosition(latLng);
                    marker.showInfoWindow();
                }
            }

            // animate the camera to center around all markers
            // if there is only one then zoom instead
            Log.d(TAG, "Total user markers in the map is " + userMarkers.size());
            Marker userMarker = userMarkers.get(appState.getUser().getId());
            if (userMarkers.size() == 1) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), CAMERA_ZOOM_LEVEL));
            }
        }
    }
}
