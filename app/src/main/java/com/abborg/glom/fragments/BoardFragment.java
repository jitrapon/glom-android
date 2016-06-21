package com.abborg.glom.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.activities.DrawActivity;
import com.abborg.glom.activities.EventActivity;
import com.abborg.glom.activities.MainActivity;
import com.abborg.glom.adapters.BoardRecyclerViewAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.interfaces.BoardItemChangeListener;
import com.abborg.glom.interfaces.BoardItemClickListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FileItem;

import java.io.File;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardFragment extends Fragment implements BoardItemClickListener, BoardItemChangeListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "BoardFragment";

    /* Whether or not this fragment is visible */
    public boolean isFragmentVisible;

    /* The swipe refresh layout */
    private SwipeRefreshLayout refreshView;

    /* The view to be used for listing event cards */
    private RecyclerView recyclerView;

    /* Adapter to the recycler view */
    private BoardRecyclerViewAdapter adapter;

    /* Layout manager for the view */
    private RecyclerView.LayoutManager layoutManager;

    /* Main activity's data updater */
    private DataProvider dataProvider;

    /* The list of items in this circle */
    private List<BoardItem> items;

    private boolean firstView;

    private MainActivity activity;

    private Handler handler;

    private ApplicationState appState;

    private static final int ITEM_APPEARANCE_ANIM_TIME = 650;
    private static final long ITEM_ADD_ANIM_TIME = 100;
    private static final long ITEM_REMOVE_ANIM_TIME = 100;
    private static final long ITEM_MOVE_ANIM_TIME = 100;
    private static final long ITEM_CHANGE_ANIM_TIME = 100;

    /**********************************************************
     * View Initializations
     **********************************************************/

    public BoardFragment() {
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
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context instanceof Activity ? (MainActivity) context : null;
        if (activity != null) handler = activity.getHandler();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appState = ApplicationState.getInstance();
        adapter = new BoardRecyclerViewAdapter(getContext(), getItems(), this, handler);
        dataProvider = appState.getDataProvider();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_board, container, false);
        refreshView = (SwipeRefreshLayout) root.findViewById(R.id.board_refresh_layout);
        refreshView.setOnRefreshListener(this);
        recyclerView = (RecyclerView) root.findViewById(R.id.circle_board_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // set up adapter and its appearance animation
        recyclerView.setAdapter(adapter);

        // set up item animations
//        recyclerView.setItemAnimator(new SlideInUpAnimator(new OvershootInterpolator(1f)));
//        recyclerView.getItemAnimator().setAddDuration(ITEM_ADD_ANIM_TIME);
//        recyclerView.getItemAnimator().setRemoveDuration(ITEM_REMOVE_ANIM_TIME);
//        recyclerView.getItemAnimator().setMoveDuration(ITEM_MOVE_ANIM_TIME);
//        recyclerView.getItemAnimator().setChangeDuration(ITEM_CHANGE_ANIM_TIME);

        return root;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isFragmentVisible = true;

            // force re-updating of items that need to be updated
            adapter.update(null);

            // send request to server to get board items
            //TODO delay by some timer for request
            if (dataProvider != null) {
                if (!firstView) {
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
                    dataProvider.requestBoardItems(appState.getActiveCircle());
                }
            }

            firstView = true;
            Log.i(TAG, "EventItem is now visible to user");
        }
        else {
            isFragmentVisible = false;
            Log.i(TAG, "EventItem is now INVISIBLE to user");
        }
    }

    /**********************************************************
     * Item Click Handler
     **********************************************************/

    @Override
    public void onItemClicked(BoardItem selected) {
        if (selected != null) {
            if (selected instanceof EventItem) {
                EventItem event = (EventItem) selected;
                Log.d(TAG, "EventItem (" + event.getName() + ") selected");
                Intent intent = new Intent(activity, EventActivity.class);
                intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_ID), event.getId());
                intent.setAction(getResources().getString(R.string.ACTION_UPDATE_EVENT));
                appState.setKeepGoogleApiClientAlive(true);
                getActivity().startActivityForResult(intent, Const.UPDATE_EVENT_RESULT_CODE);
            }
            else if (selected instanceof FileItem) {
                FileItem item = (FileItem) selected;
                Log.d(TAG, "FileItem (" + item.getName() + ") selected");

                // if file does not exist, download, otherwise view it
                if (item.getLocalCache() == null || !item.getLocalCache().exists()) {
                    if (handler != null) handler.sendMessage(handler.obtainMessage(Const.MSG_DOWNLOAD_ITEM, item));
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(item.getLocalCache()), item.getMimetype());
                    getActivity().startActivity(Intent.createChooser(intent, getString(R.string.card_select_app_to_launch)));
                }
            }
            else if (selected instanceof DrawItem) {
                DrawItem item = (DrawItem) selected;
                Log.d(TAG, "DrawItem (" + item.getId() + ") selected");
                //TODO if file does not exist, download, otherwise view it
                String path = (item.getLocalCache() == null) ? null :
                        new File(item.getLocalCache().getPath()).exists() ? item.getLocalCache().getPath() : null;
                Intent intent = new Intent(activity, DrawActivity.class);
                intent.setAction(getString(R.string.ACTION_JOIN_DRAWING));
                intent.putExtra(getString(R.string.EXTRA_DRAWING_ID), item.getId());
                intent.putExtra(getString(R.string.EXTRA_DRAWING_PATH), path);
                getActivity().startActivityForResult(intent, Const.DRAW_RESULT_CODE);
            }
        }
    }

    /**********************************************************
     * Refresh callback
     **********************************************************/

    @Override
    public void onRefresh() {
        dataProvider.requestBoardItems(appState.getActiveCircle());
    }

    /**********************************************************
     * Item Change Handler
     **********************************************************/

    @Override
    public void onItemAdded(String id) {
        if (activity != null && id != null) {
            adapter.addItem(id);
        }

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }
    }

    @Override
    public void onItemModified(String id) {
        if (activity != null && id != null) {
            adapter.updateItem(id);
        }

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }
    }

    @Override
    public void onItemDeleted(String id) {
        if (activity != null && id != null) {
            adapter.deleteItem(id);
        }

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }
    }

    /**********************************************************
     * Helpers
     **********************************************************/

    public List<BoardItem> getItems() {
        items = appState.getActiveCircle().getItems();
        return items;
    }

    public void update() {
        if (activity != null && adapter != null) {
            items = appState.getActiveCircle().getItems();
            adapter.update(items);
            if (refreshView != null) {
                if (refreshView.isRefreshing()) refreshView.setRefreshing(false);
            }
        }
    }
}
