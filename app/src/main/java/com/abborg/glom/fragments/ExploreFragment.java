package com.abborg.glom.fragments;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.ExploreRecyclerViewAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.CategoryBarListener;
import com.abborg.glom.interfaces.ExploreItemChangeListener;
import com.abborg.glom.interfaces.MainActivityCallbacks;
import com.abborg.glom.model.Category;
import com.abborg.glom.model.ExploreItem;

import java.util.List;

import javax.inject.Inject;

public class ExploreFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener,
        ExploreItemChangeListener,
        CategoryBarListener {

    @Inject
    ApplicationState appState;

    @Inject
    DataProvider dataProvider;

    private static final String TAG = "ExploreFragment";

    public boolean isFragmentVisible;
    private SwipeRefreshLayout refreshView;
    private ExploreRecyclerViewAdapter adapter;
    private boolean firstView;

    private MainActivityCallbacks activityCallback;

    private Handler handler;

    private int currentCategoryId;
    private List<Category> categories;
    private List<ExploreItem> items;

    public ExploreFragment() {}

    /**********************************************************
     * View Initializations
     **********************************************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            activityCallback = (MainActivityCallbacks) context;
            if (isFragmentVisible) {
                activityCallback.onSetFabVisible(false);
            }
            handler = activityCallback.getThreadHandler();
        }
        catch (ClassCastException ex) {
            Log.e(TAG, "Attached activity has to implement " + MainActivityCallbacks.class.getName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        adapter = new ExploreRecyclerViewAdapter(getContext(), handler);
        currentCategoryId = -1;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_explore, container, false);
        refreshView = (SwipeRefreshLayout) root.findViewById(R.id.explore_refresh_layout);
        refreshView.setOnRefreshListener(this);
        refreshView.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark);
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.explore_recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        return root;
    }

    /**********************************************************
     * REFRESH CALLBACKS
     **********************************************************/

    private void refreshData() {
        if (categories == null || categories.isEmpty() || currentCategoryId == -1) {
            dataProvider.requestExploreItems();
        }
        else {
            dataProvider.requestExploreItems(currentCategoryId);
        }
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d(TAG, "ExploreFragment is visible to user and first view is " + firstView);
            isFragmentVisible = true;

            if (dataProvider != null) {
                if (!firstView && appState.getConnectionStatus() == ApplicationState.ConnectivityStatus.CONNECTED) {
                    if (refreshView != null) {
                        if (handler != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    refreshView.setRefreshing(true);
                                }
                            });
                        }
                    }
                    refreshData();
                }
            }

            activityCallback.onSetFabVisible(false);
            setActivityToolbar();
            firstView = true;
        }
        else {
            isFragmentVisible = false;
        }
    }

    private void setActivityToolbar() {
        if (isFragmentVisible) {
            activityCallback.onToolbarTitleChanged(appState.getActiveCircle().getTitle());
            activityCallback.onToolbarSubtitleChanged(null);
        }
    }

    /**********************************************************
     * ITEM CHANGE EVENTS FROM NETWORK REQUESTS
     **********************************************************/

    @Override
    public void onItemsReceived(int categoryId, List<ExploreItem> exploreItems) {
        currentCategoryId = categoryId;
        items = exploreItems;
        if (refreshView != null && refreshView.isRefreshing()) {
            refreshView.setRefreshing(false);
        }

        if (adapter != null) adapter.update(categoryId, items);
    }

    /**********************************************************
     * CATEGORY BAR
     **********************************************************/

    @Override
    public void onCategoryBarRequireUpdate(List<Category> items, RecyclerView recyclerView) {
        categories = items;

        // notify adapter

        activityCallback.onShowCategoryBar();
    }

    @Override
    public boolean shouldShowCategoryBar() {
//        return categories != null && !categories.isEmpty();
        return true;
    }
}
