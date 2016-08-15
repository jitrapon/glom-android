package com.abborg.glom.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.activities.EventActivity;
import com.abborg.glom.activities.MainActivity;
import com.abborg.glom.adapters.BoardItemAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.interfaces.BoardItemChangeListener;
import com.abborg.glom.interfaces.BoardItemClickListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.model.LinkItem;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /* Adapter to the recycler view */
    private BoardItemAdapter adapter;

    /* Main activity's data updater */
    private DataProvider dataProvider;

    /* The list of items in this circle */
    private List<BoardItem> items;

    private ImageView emptyView;
    private boolean firstView;

    private MainActivity activity;
    private Handler handler;
    private ApplicationState appState;

    private boolean isActionModeEnabled;

//    private static final int ITEM_APPEARANCE_ANIM_TIME = 650;
//    private static final long ITEM_ADD_ANIM_TIME = 100;
//    private static final long ITEM_REMOVE_ANIM_TIME = 100;
//    private static final long ITEM_MOVE_ANIM_TIME = 100;
//    private static final long ITEM_CHANGE_ANIM_TIME = 100;

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
        adapter = new BoardItemAdapter(getContext(), getItems(), this);
        dataProvider = appState.getDataProvider();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_board, container, false);

        refreshView = (SwipeRefreshLayout) root.findViewById(R.id.board_refresh_layout);
        refreshView.setOnRefreshListener(this);
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.board_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // set up adapter and its appearance animation
        recyclerView.setAdapter(adapter);

        // set up item animations
//        recyclerView.setItemAnimator(new SlideInUpAnimator(new OvershootInterpolator(1f)));
//        recyclerView.getItemAnimator().setAddDuration(ITEM_ADD_ANIM_TIME);
//        recyclerView.getItemAnimator().setRemoveDuration(ITEM_REMOVE_ANIM_TIME);
//        recyclerView.getItemAnimator().setMoveDuration(ITEM_MOVE_ANIM_TIME);
//        recyclerView.getItemAnimator().setChangeDuration(ITEM_CHANGE_ANIM_TIME);

        // set up empty board icon
        emptyView = (ImageView) root.findViewById(R.id.board_empty_view);
        showOrHideEmptyIcon();

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

    private void showOrHideEmptyIcon() {
        if (emptyView != null) {
            emptyView.setVisibility(items != null && items.size() > 0 ? View.GONE: View.VISIBLE);
        }
    }

    /**********************************************************
     * Item Click Handler
     **********************************************************/

    @Override
    public void onItemClicked(BoardItem selected, int position) {
        if (!isActionModeEnabled) {
            if (selected != null) {
                if (selected instanceof EventItem) {
                    EventItem event = (EventItem) selected;
                    Intent intent = new Intent(activity, EventActivity.class);
                    intent.putExtra(getResources().getString(R.string.EXTRA_EVENT_ID), event.getId());
                    intent.setAction(getResources().getString(R.string.ACTION_UPDATE_EVENT));
                    appState.setKeepGoogleApiClientAlive(true);
                    getActivity().startActivityForResult(intent, Const.UPDATE_EVENT_RESULT_CODE);
                }
                else if (selected instanceof FileItem) {
                    FileItem item = (FileItem) selected;

                    // if file does not exist, download, otherwise view it
                    if (item.getLocalCache() == null || !item.getLocalCache().exists()) {
                        if (handler != null)
                            handler.sendMessage(handler.obtainMessage(Const.MSG_DOWNLOAD_ITEM, item));
                    }
                    else {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(item.getLocalCache()), item.getMimetype());
                        getActivity().startActivity(Intent.createChooser(intent, getString(R.string.card_select_app_to_launch)));
                    }
                }
                else if (selected instanceof DrawItem) {
                    DrawItem item = (DrawItem) selected;
                    if (handler != null)
                        handler.sendMessage(handler.obtainMessage(Const.MSG_DOWNLOAD_DRAWING, item));
                }
                else if (selected instanceof LinkItem) {
                    LinkItem item = (LinkItem) selected;
                    if (handler != null)
                        handler.sendMessage(handler.obtainMessage(Const.MSG_OPEN_LINK, item.getUrl()));
                }
            }
        }
        else {
            adapter.toggleSelection(position);

            if (handler != null) {
                handler.sendMessage(handler.obtainMessage((Const.MSG_SELECT_BOARD_ITEM), adapter.getSelectedItemCount()));
            }
        }
    }

    @Override
    public boolean onItemLongClicked(BoardItem item, int position) {

        // if this is the first long press, mark this item as selected
        if (!isActionModeEnabled) {
            isActionModeEnabled = true;

            handler.sendMessage(handler.obtainMessage(Const.MSG_START_ACTION_MODE, item.getId()));
            adapter.toggleSelection(position);
        }
        return true;
    }

    @Override
    public void onActionButtonClicked(BoardItem item, @IdRes int button) {
        if (!isActionModeEnabled) {
            switch (button) {
                case R.id.action_get_directions: {
                    if (item instanceof EventItem) {
                        EventItem event = (EventItem) item;
                        final String placeId = event.getPlace();
                        if (!TextUtils.isEmpty(placeId)) {
                            GoogleApiClient apiClient = ApplicationState.getInstance().getGoogleApiClient();
                            if (apiClient != null && apiClient.isConnected()) {
                                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(apiClient, placeId);
                                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {

                                    @Override
                                    public void onResult(@NonNull PlaceBuffer places) {
                                        if (!places.getStatus().isSuccess()) {
                                            Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                                            places.release();
                                            return;
                                        }

                                        final Place place = places.get(0);
                                        Log.d(TAG, "Place query succeeded for " + place.getName());
                                        double lat = place.getLatLng().latitude;
                                        double lng = place.getLatLng().longitude;
                                        places.release();

                                        launchGoogleMapsNavigation(lat, lng);
                                    }
                                });
                            }
                        }
                        else if (event.getLocation() != null) {
                            launchGoogleMapsNavigation(event.getLocation().getLatitude(), event.getLocation().getLongitude());
                        }
                    }

                    break;
                }
                case R.id.action_edit_link: {
                    LinkItem link = (LinkItem) item;
                    if (handler != null) {
                        handler.sendMessage(handler.obtainMessage(Const.MSG_EDIT_LINK, link));
                    }

                    break;
                }
                case R.id.action_copy_link: {
                    LinkItem link = (LinkItem) item;
                    if (handler != null) {
                        handler.sendMessage(handler.obtainMessage(Const.MSG_COPY_LINK, link));
                    }

                    break;
                }
                default: break;
            }
        }
    }

    private void launchGoogleMapsNavigation(double lat, double lng) {
        Uri gmmIntentUri = Uri.parse(
                String.format(Locale.ENGLISH, "google.navigation:q=%1f,%2f", lat, lng));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
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

        showOrHideEmptyIcon();
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

        showOrHideEmptyIcon();
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

        showOrHideEmptyIcon();
    }

    public List<String> getSelectedItemsId() {
        List<String> ids = new ArrayList<>(adapter.getSelectedItems().size());
        for (Integer position : adapter.getSelectedItems()) {
            ids.add(items.get(position).getId());
        }
        return ids;
    }

    public int getSelectedItemCount() { return adapter.getSelectedItemCount(); }

    public void setActionModeEnabled(boolean enabled) {
        isActionModeEnabled = enabled;
        refreshView.setEnabled(!enabled);
    }

    public void clearItemSelections() {
        adapter.clearSelections();
    }
}
