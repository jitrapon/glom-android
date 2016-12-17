package com.abborg.glom.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.RelativeLayout;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.UserAvatarAdapter;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.BroadcastLocationListener;
import com.abborg.glom.interfaces.CircleChangeListener;
import com.abborg.glom.interfaces.MainActivityCallbacks;
import com.abborg.glom.interfaces.UsersChangeListener;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.ViewUtils;
import com.nhaarman.listviewanimations.appearance.simple.ScaleInAnimationAdapter;

import java.util.List;

import javax.inject.Inject;

public class CircleFragment extends Fragment implements
        BroadcastLocationListener,
        CircleChangeListener,
        UsersChangeListener,
        AdapterView.OnItemClickListener {

    @Inject
    ApplicationState appState;
    private List<User> users;

    public boolean isFragmentVisible;

    private UserAvatarAdapter avatarAdapter;
    private GridView gridView;
    private MainActivityCallbacks activityCallback;

    private static final String TAG = "CIRCLE_FRAGMENT";

    /**********************************************************
     * View Initializations
     **********************************************************/

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isFragmentVisible = true;

            activityCallback.onSetFabVisible(false);
            setActivityToolbar();
        }
        else {
            isFragmentVisible = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        if (appState != null && appState.getActiveCircle() != null) {
            users = appState.getActiveCircle().getUsers();
        }
    }

    @Override
    // based on Grid, Circle, or List
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RelativeLayout rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_circle, container, false);

        // initialize the USERLIST view
        gridView = new GridView(getActivity());
//        gridView.setId();
        gridView.setLayoutParams(new GridView.LayoutParams(
                GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.MATCH_PARENT));
        gridView.setBackgroundColor(Color.TRANSPARENT);
        gridView.setNumColumns(3);
        gridView.setColumnWidth(GridView.AUTO_FIT);
        gridView.setVerticalSpacing(ViewUtils.getPxFromDp(getContext(), 10));
        gridView.setHorizontalSpacing(ViewUtils.getPxFromDp(getContext(), 10));
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gridView.setGravity(Gravity.CENTER);
        gridView.setPadding(0, ViewUtils.getPxFromDp(getContext(), (int) getResources().getDimension(R.dimen.user_avatar_padding_bottom)), 0, 0);

        // set the adapter for this view
        avatarAdapter = new UserAvatarAdapter(getContext(), users);
        ScaleInAnimationAdapter animationAdapter = new ScaleInAnimationAdapter(avatarAdapter);
        animationAdapter.setAbsListView(gridView);
        gridView.setAdapter(animationAdapter);

        // set callback for each avatar
        // here we display the radial menu for user and show overlay
        // show menu based on user permission
        gridView.setOnItemClickListener(this);

        // add the layout
        rootView.addView(gridView, 0);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            activityCallback = (MainActivityCallbacks) context;
            if (isFragmentVisible) {
                activityCallback.onSetFabVisible(false);
            }
        }
        catch (ClassCastException ex) {
            Log.e(TAG, "Attached activity has to implement " + MainActivityCallbacks.class.getName());
        }
    }

    /**********************************************************
     * Callbacks
     **********************************************************/

    @Override
    public void onBroadcastLocationEnabled(long duration) {
        setAvatarBroadcastingAnimation(true);
    }

    @Override
    public void onBroadcastLocationDisabled() {
        setAvatarBroadcastingAnimation(false);
    }

    @Override
    public void onCircleChanged() {
        update();

        setActivityToolbar();
    }

    @Override
    public void onUsersChanged() {
        update();

        setActivityToolbar();
    }

    private void setActivityToolbar() {
        if (isFragmentVisible) {
            activityCallback.onToolbarTitleChanged(appState.getActiveCircle().getTitle());

            int memberCount = appState.getActiveCircle().getUsers().size();
            activityCallback.onToolbarSubtitleChanged(
                    getContext().getResources().getQuantityString(R.plurals.subtitle_circle_fragment,
                            memberCount, memberCount));
        }
    }

    /**********************************************************
     * Helpers
     **********************************************************/

    private void update() {
        if (appState != null) {
            users = appState.getActiveCircle().getUsers();
            avatarAdapter.update(users);
        }
    }

    private void setAvatarBroadcastingAnimation(boolean isBroadcasting) {
        int avatarIndex = 0;
        for (User user : users) {
            if (user.getId().equals(appState.getActiveUser().getId())) {
                View avatar = getAvatarByPosition(avatarIndex);
                Log.d(TAG, "Setting broadcast animation for " + avatarIndex);
                avatarAdapter.setUserIsBroadcastingLocation(avatar, isBroadcasting);
            }
            avatarIndex++;
        }
    }

    private View getAvatarByPosition(int pos) {
        final int firstListItemPosition = gridView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + gridView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return gridView.getAdapter().getView(pos, null, gridView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return gridView.getChildAt(childIndex);
        }
    }

    /**********************************************************
     * User Grid Click Handler
     **********************************************************/

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

}
