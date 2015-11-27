package com.abborg.glom.ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.adapter.EventRecyclerViewAdapter;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.Event;

import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;

/**
 * A simple {@link Fragment} subclass.
 */
public class EventFragment extends Fragment implements View.OnClickListener {

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

    /* The list of events in this circle */
    private List<Event> events;

    private MainActivity activity;

    private static final int ITEM_APPEARANCE_ANIM_TIME = 650;

    private static final long ITEM_ADD_ANIM_TIME = 650;

    private static final long ITEM_REMOVE_ANIM_TIME = 350;

    private static final long ITEM_MOVE_ANIM_TIME = 350;

    private static final long ITEM_CHANGE_ANIM_TIME = 350;

    public EventFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataUpdater = AppState.getInstance(getContext()).getDataUpdater();
        adapter = new EventRecyclerViewAdapter(getContext(), getEvents(), this);
        Log.d(TAG, "OnCreate events");
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
        adapter.setEventChangedListener(activity);

        // set up adapter and its appearance animation
        recyclerView.setAdapter(adapter);

        // set up item animations
        recyclerView.setItemAnimator(new SlideInUpAnimator(new OvershootInterpolator(1f)));
        recyclerView.getItemAnimator().setAddDuration(ITEM_ADD_ANIM_TIME);
        recyclerView.getItemAnimator().setRemoveDuration(ITEM_REMOVE_ANIM_TIME);
        recyclerView.getItemAnimator().setMoveDuration(ITEM_MOVE_ANIM_TIME);
        recyclerView.getItemAnimator().setChangeDuration(ITEM_CHANGE_ANIM_TIME);

        Log.d(TAG, "OnCreateView " + events.size() + " events");

        return root;
    }

    @Override
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        Event selected = events.get(position - 1);
        if (selected != null) {
            Log.d(TAG, "Event (" + selected.getName() + ") selected");
            Intent intent = new Intent(activity, EventActivity.class);
            intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_ID), selected.getId());
            intent.setAction(getResources().getString(R.string.ACTION_UPDATE_EVENT));
            AppState.getInstance(getActivity()).setKeepGoogleApiClientAlive(true);
            getActivity().startActivityForResult(intent, Const.UPDATE_EVENT_RESULT_CODE);
        }
    }

    /**
     * Called when the UI is to be updated when an item is to be added
     * @param index
     */
    public void onItemAdded(int index) {
        layoutManager.scrollToPosition(0);
        adapter.notifyItemInserted(index);
        Log.d(TAG, "Inserted item at " + index);
    }

    public void onItemChanged(int index) {
        layoutManager.scrollToPosition(index);
        adapter.notifyItemChanged(index);
        Log.d(TAG, "Updated item at " + index);
    }

    /**
     * Called when the UI is to be updated when an item is to be deleted
     * @param index
     */
    public void onItemDeleted(int index) {
        Log.d(TAG, "Removed item at " + index);
        adapter.notifyItemRemoved(index);
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

    @Override
    // we get hold of activity instance to know that the fragment has been attached
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context instanceof Activity ? (MainActivity) context : null;
    }

    public List<Event> getEvents() {
        events = AppState.getInstance(getContext()).getCurrentCircle().getEvents();
        return events;
    }

    public void update() {
        if (activity != null) {
            events = AppState.getInstance(getContext()).getCurrentCircle().getEvents();
            adapter.update(events);
        }
    }
}
