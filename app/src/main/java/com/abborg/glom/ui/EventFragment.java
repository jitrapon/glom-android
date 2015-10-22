package com.abborg.glom.ui;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.abborg.glom.R;
import com.abborg.glom.adapter.EventRecyclerViewAdapter;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.Event;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class EventFragment extends Fragment
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "EventFragment";

    /* Whether or not this fragment is visible */
    public boolean isFragmentVisible;

    /* The view to be used for listing event cards */
    private RecyclerView recyclerView;

    /* Adapter to the recycler view */
    private EventRecyclerViewAdapter adapter;

    /* Layout manager for the view */
    private RecyclerView.LayoutManager layoutManager;

    /* Main activity's data updater */
    private DataUpdater dataUpdater;

    /* The current circle */
    private Circle circle;

    /* The list of events in this circle */
    private List<Event> events;

    private MainActivity activity;

    /* Helper class that verifies Google's Api client */
    private GoogleApiAvailability apiAvailability;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    public EventFragment() {
        // Required empty public constructor
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

    @Override
    public void onStart() {
        super.onStart();
        if (apiClient != null && !apiClient.isConnected()) apiClient.connect();
    }

    @Override
    public void onStop() {
        if (apiClient != null && apiClient.isConnected()) {
            apiClient.disconnect();
            Log.d(TAG, "Google Places API disconnected");
        }
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataUpdater = ((MainActivity) getActivity()).dataUpdater;
        circle = ((MainActivity) getActivity()).getCurrentCircle();

        apiAvailability = GoogleApiAvailability.getInstance();

        Context context = getContext();

        adapter = new EventRecyclerViewAdapter(getContext(), getEvents());

        if (verifyGooglePlayServices(context)) {
            apiClient = new GoogleApiClient
                    .Builder(context)
                    .addApi(Places.GEO_DATA_API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_event, container, false);
        recyclerView = (RecyclerView) root.findViewById(R.id.circle_event_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new EventRecyclerViewAdapter
                .EventClickListener() {

            @Override
            public void onItemClick(int position, View v) {
                Log.i(TAG, "Clicked on event " + position);
            }
        });

        return root;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isFragmentVisible = true;
            adapter.update(null);   // force re-updating of events
            Log.i(TAG, "Event is now visible to user");
        }
        else {
            isFragmentVisible = false;
            Log.i(TAG, "Event is now INVISIBLE to user");
        }
    }

    public List<Event> getEvents() {
        events = circle.getEvents();
        return events;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context instanceof Activity ? (MainActivity) context : null;
    }

    public void update() {
        if (activity != null) {
            circle = activity.getCurrentCircle();
            events = circle.getEvents();
            adapter.update(events);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        adapter.setGoogleApiClient(apiClient);
        Log.d(TAG, "Google Places API connected.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Google Places API connection failed with error code: "
                + result.getErrorCode());

        Toast.makeText(getActivity(),
                "Google Places API connection failed with error code:" +
                        result.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        adapter.setGoogleApiClient(null);
        Log.e(TAG, "Google Places API connection suspended.");
    }
}
