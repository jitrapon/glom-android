package com.abborg.glom.fragments;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.activities.EventActivity;
import com.abborg.glom.activities.ListItemActivity;
import com.abborg.glom.activities.NoteActivity;
import com.abborg.glom.adapters.BoardItemAdapter;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.ActionModeCallbacks;
import com.abborg.glom.interfaces.BoardItemChangeListener;
import com.abborg.glom.interfaces.BoardItemClickListener;
import com.abborg.glom.interfaces.CircleChangeListener;
import com.abborg.glom.interfaces.MainActivityCallbacks;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.model.LinkItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.model.NoteItem;
import com.abborg.glom.model.PlaceInfo;
import com.abborg.glom.utils.TaskUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardFragment extends Fragment implements
        BoardItemClickListener,
        BoardItemChangeListener,
        SwipeRefreshLayout.OnRefreshListener,
        CircleChangeListener,
        ActionModeCallbacks {

    @Inject
    ApplicationState appState;

    @Inject
    DataProvider dataProvider;

    public boolean isFragmentVisible;
    private SwipeRefreshLayout refreshView;
    private BoardItemAdapter adapter;
    private ImageView emptyView;
    private boolean firstView;
    private boolean isActionModeEnabled;

    private List<BoardItem> items;

    private MainActivityCallbacks activityCallback;

    private Handler handler;

    private static final String TAG = "BoardFragment";

    /**********************************************************
     * View Initializations
     **********************************************************/

    public BoardFragment() {}

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

        try {
            activityCallback = (MainActivityCallbacks) context;
            if (isFragmentVisible) {
                activityCallback.onSetFabVisible(true);
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

        adapter = new BoardItemAdapter(getContext(), getItems(), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_board, container, false);

        refreshView = (SwipeRefreshLayout) root.findViewById(R.id.board_refresh_layout);
        refreshView.setOnRefreshListener(this);
        refreshView.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark);
        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.board_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // set up adapter and its appearance animation
        recyclerView.setAdapter(adapter);

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

            activityCallback.onSetFabVisible(true);
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

            int itemCount = appState.getActiveCircle().getItems().size();
            activityCallback.onToolbarSubtitleChanged(
                    getContext().getResources().getQuantityString(R.plurals.subtitle_board_fragment,
                            itemCount, itemCount));
        }
    }

    /**********************************************************
     * Circle Change Callback
     **********************************************************/

    @Override
    public void onCircleChanged() {
        update();

        setActivityToolbar();
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
                    Intent intent = new Intent(getActivity(), EventActivity.class);
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
                        if (handler != null)
                            handler.sendMessage(handler.obtainMessage(Const.MSG_VIEW_FILE, item));
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
                else if (selected instanceof ListItem) {
                    ListItem item = (ListItem) selected;
                    Intent intent = new Intent(getActivity(), ListItemActivity.class);
                    intent.putExtra(getString(R.string.EXTRA_LIST_ID), item.getId());
                    intent.setAction(getString(R.string.ACTION_UPDATE_LIST));
                    getActivity().startActivity(intent);
                }
                else if (selected instanceof NoteItem) {
                    NoteItem item = (NoteItem) selected;
                    Intent intent = new Intent(getActivity(), NoteActivity.class);
                    intent.putExtra(getString(R.string.EXTRA_NOTE_ID), item.getId());
                    intent.setAction(getString(R.string.ACTION_UPDATE_NOTE));
                    getActivity().startActivity(intent);
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
                            TaskUtils.getLocationFromPlaceId(appState.getGoogleApiClient(), placeId, new TaskUtils.OnLocationReceivedListener() {
                                @Override
                                public void onLocationReceived(List<PlaceInfo> locations) {
                                    if (locations != null && !locations.isEmpty()) {
                                        double lat = locations.get(0).getLat();
                                        double lng = locations.get(0).getLng();
                                        TaskUtils.launchGoogleMapsNavigation(getContext(), lat, lng);
                                    }
                                }

                                @Override
                                public void onLocationFailed() {

                                }
                            });
                        }
                        else if (event.getLocation() != null) {
                            TaskUtils.launchGoogleMapsNavigation(getContext(), event.getLocation().getLatitude(), event.getLocation().getLongitude());
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
        adapter.addItem(id);

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }

        showOrHideEmptyIcon();

        setActivityToolbar();
    }

    @Override
    public void onItemModified(String id) {
        adapter.updateItem(id);

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }

        setActivityToolbar();
    }

    @Override
    public void onItemDeleted(String id) {
        adapter.deleteItem(id);

        if (refreshView != null) {
            if (refreshView.isRefreshing())
                refreshView.setRefreshing(false);
        }

        showOrHideEmptyIcon();

        setActivityToolbar();
    }

    @Override
    public void onItemsChanged() {
        update();

        setActivityToolbar();
    }

    /**********************************************************
     * Action Mode Callbacks
     **********************************************************/

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_context_board_item, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        boolean shouldShowMenu = adapter.getSelectedItemCount() > 0;
        menu.findItem(R.id.action_delete).setVisible(shouldShowMenu);
        menu.findItem(R.id.action_star).setVisible(shouldShowMenu);
        menu.findItem(R.id.action_copy).setVisible(shouldShowMenu);
        menu.findItem(R.id.action_share).setVisible(shouldShowMenu);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        List<String> ids = new ArrayList<>(adapter.getSelectedItems().size());
        for (Integer position : adapter.getSelectedItems()) {
            ids.add(items.get(position).getId());
        }

        switch (item.getItemId()) {
            case R.id.action_delete:
                for (String id : ids) {
                    Log.d(TAG, "Attempting to delete item " + id);
                    if (handler != null) {
                        handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_TO_DELETE, id));
                    }
                }
                mode.finish();
                return true;
            default: return true;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.clearSelections();
        setActionModeEnabled(false);
    }

    /**********************************************************
     * Helpers
     **********************************************************/

    private void showOrHideEmptyIcon() {
        if (emptyView != null) {
            emptyView.setVisibility(items != null && items.size() > 0 ? View.GONE: View.VISIBLE);
        }
    }

    private List<BoardItem> getItems() {
        items = appState.getActiveCircle().getItems();
        return items;
    }

    private void update() {
        if (appState != null) {
            items = appState.getActiveCircle().getItems();
            adapter.update(items);
            if (refreshView != null) {
                if (refreshView.isRefreshing()) refreshView.setRefreshing(false);
            }

            showOrHideEmptyIcon();
        }
    }

    public void setActionModeEnabled(boolean enabled) {
        isActionModeEnabled = enabled;
        refreshView.setEnabled(!enabled);
    }
}
