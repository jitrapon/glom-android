package com.abborg.glom.fragments;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.activities.MainActivity;
import com.abborg.glom.adapters.DiscoverRecyclerViewAdapter;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.interfaces.DiscoverItemChangeListener;
import com.abborg.glom.model.DiscoverItem;

import java.util.List;

public class DiscoverFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        DiscoverItemChangeListener {

    private static final String TAG = "DiscoverFragment";

    /* Whether or not this fragment is visible */
    public boolean isFragmentVisible;

    /* The swipe refresh layout */
    private SwipeRefreshLayout refreshView;

    /* The view to be used for listing event cards */
    private RecyclerView recyclerView;

    /* Layout manager for the view */
    private RecyclerView.LayoutManager layoutManager;

    /* Adapter to the recycler view */
    private DiscoverRecyclerViewAdapter adapter;

    /* Main activity's data updater */
    private DataUpdater dataUpdater;

    private boolean firstView;

    private MainActivity activity;

    private Handler handler;

    private AppState appState;

    public DiscoverFragment() {}

    /**********************************************************
     * View Initializations
     **********************************************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context instanceof Activity ? (MainActivity) context : null;
        if (activity != null) handler = activity.getHandler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appState = AppState.getInstance();
        dataUpdater = appState.getDataUpdater();
        adapter = new DiscoverRecyclerViewAdapter(getContext(), handler);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_discover, container, false);
        refreshView = (SwipeRefreshLayout) root.findViewById(R.id.discover_refresh_layout);
        refreshView.setOnRefreshListener(this);
        recyclerView = (RecyclerView) root.findViewById(R.id.circle_discover_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // set up adapter and its appearance animation
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onRefresh() {
        dataUpdater.requestMovies();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isFragmentVisible = true;

//            // force re-updating of items that need to be updated
//            adapter.update(null);
//
//            // send request to server to get board items
//            //TODO delay by some timer for request
//            if (dataUpdater != null) {
//                if (!firstView) {
//                    if (refreshView != null) {
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                refreshView.setRefreshing(true);
//                            }
//                        });
//                    }
//                    dataUpdater.requestDiscoverItems();
//                }
//            }
//
//            firstView = true;
        }
        else {
            isFragmentVisible = false;
        }
    }

    /**********************************************************
     * ITEM CHANGE EVENTS FROM NETWORK REQUESTS
     **********************************************************/

    @Override
    public void onItemsReceived(int type, List<DiscoverItem> items) {
        if (refreshView != null && refreshView.isRefreshing()) {
            refreshView.setRefreshing(false);
        }

        int size = items == null ? 0 : items.size();
        adapter.update(type, items);
        Toast.makeText(getContext(), "Received " + size + " items", Toast.LENGTH_LONG).show();
    }
}
