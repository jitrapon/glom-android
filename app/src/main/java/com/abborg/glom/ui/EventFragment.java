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

import com.abborg.glom.R;
import com.abborg.glom.adapter.EventRecyclerViewAdapter;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.Event;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class EventFragment extends Fragment {

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

    public EventFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataUpdater = ((MainActivity) getActivity()).dataUpdater;
        circle = ((MainActivity) getActivity()).getCurrentCircle();
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
        adapter = new EventRecyclerViewAdapter(getContext(), getEvents());
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
            Log.i(TAG, "Event is now visible to user");
        }
        else {
            isFragmentVisible = false;
            Log.i(TAG, "Event is now INVISIBLE to user");
        }
    }

    private List<Event> getEvents() {
        events = dataUpdater.getCircleEvents(circle);
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
            events = dataUpdater.getCircleEvents(circle);
            adapter.update(events);
        }
    }
}
