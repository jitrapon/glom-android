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

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.activities.EventActivity;
import com.abborg.glom.activities.MainActivity;
import com.abborg.glom.adapters.BoardRecyclerViewAdapter;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.interfaces.BoardItemChangeListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FileItem;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardFragment extends Fragment implements View.OnClickListener, BoardItemChangeListener,
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
    private DataUpdater dataUpdater;

    /* The list of items in this circle */
    private List<BoardItem> items;

    private boolean firstView;

    private MainActivity activity;

    private Handler handler;

    private AppState appState;

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
        appState = AppState.getInstance();
        adapter = new BoardRecyclerViewAdapter(getContext(), getItems(), this, handler);
        dataUpdater = appState.getDataUpdater();
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
            if (dataUpdater != null) {
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
                    dataUpdater.requestBoardItems(appState.getActiveCircle());
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
    public void onClick(View view) {
        int position = recyclerView.getChildAdapterPosition(view);
        BoardItem selected = items.get(position - 1);
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
                if (item.getFile() == null || !item.getFile().exists()) {
                    if (handler != null) handler.sendMessage(handler.obtainMessage(Const.MSG_DOWNLOAD_ITEM, item));
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(item.getFile()), item.getMimetype());
                    startActivity(Intent.createChooser(intent, getString(R.string.card_select_app_to_launch)));
                }
            }
        }
    }

    /**********************************************************
     * Refresh callback
     **********************************************************/

    @Override
    public void onRefresh() {
        dataUpdater.requestBoardItems(appState.getActiveCircle());
    }

    /**********************************************************
     * Item Change Handler
     **********************************************************/

    @Override
    public void onItemAdded(String id) {
        if (activity != null && id != null) {
            adapter.notifyItemInserted(0);
            layoutManager.scrollToPosition(0);
            Log.d(TAG, "Inserted item at " + 0);
        }

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }
    }

    @Override
    public void onItemModified(String id) {
        if (activity != null && id != null) {
            int index = -1;
            items = appState.getActiveCircle().getItems();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(id)) {
                    index = i;
                    break;
                }
            }
            if (index == -1) return;

            index = index + 1;
            layoutManager.scrollToPosition(index);
            adapter.notifyItemChanged(index);
            Log.d(TAG, "Updated item at " + index);
        }

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }
    }

    @Override
    public void onItemDeleted(String id) {
        if (activity != null && id != null) {
            int index = -1;
            items = appState.getActiveCircle().getItems();
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId().equals(id)) {
                    index = i;
                    break;
                }
            }
            if (index == -1) return;

            index = index + 1;
            Log.d(TAG, "Removed item at " + index);
            adapter.notifyItemRemoved(index);
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
