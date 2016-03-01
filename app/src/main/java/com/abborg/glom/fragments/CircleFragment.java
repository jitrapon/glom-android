package com.abborg.glom.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.activities.MainActivity;
import com.abborg.glom.adapters.UserAvatarAdapter;
import com.abborg.glom.data.DataUpdater;
import com.abborg.glom.interfaces.BroadcastLocationListener;
import com.abborg.glom.model.User;
import com.abborg.glom.service.CirclePushService;
import com.abborg.glom.utils.LayoutUtils;
import com.nhaarman.listviewanimations.appearance.simple.ScaleInAnimationAdapter;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * This fragment contains layout for the circle (group) in a circle.
 * The fragment can show group of users in grid style, circle style,
 * or a traditional scroll view style.
 * */
public class CircleFragment extends Fragment {

    private static final String TAG = "CIRCLE_FRAGMENT";

    private List<User> users;

    /* Whether or not the fragment is visible */
    public boolean isFragmentVisible;

    private UserAvatarAdapter avatarAdapter;

    // Required empty public constructor
    public CircleFragment() {}

    private AppState appState;

    private GridView gridView;

    private Activity activity;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            isFragmentVisible = true;
            Log.i(TAG, "Circle is now visible to user");
        }
        else {
            isFragmentVisible = false;
            Log.i(TAG, "Circle is now INVISIBLE to user");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appState = AppState.getInstance(getContext());

        users = appState.getCurrentCircle().getUsers();
    }

    public void update() {
        users = appState.getCurrentCircle().getUsers();
        avatarAdapter.update(users);
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
                GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.WRAP_CONTENT));
        gridView.setBackgroundColor(Color.TRANSPARENT);
        gridView.setNumColumns(3);
        gridView.setColumnWidth(GridView.AUTO_FIT);
        gridView.setVerticalSpacing(LayoutUtils.pxToDp(getContext(), 10));
        gridView.setHorizontalSpacing(LayoutUtils.pxToDp(getContext(), 10));
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        gridView.setGravity(Gravity.CENTER);
        gridView.setPadding(0, LayoutUtils.pxToDp(getContext(), (int) getResources().getDimension(R.dimen.user_avatar_padding_bottom)), 0, 0);

        // set the adapter for this view
        avatarAdapter = new UserAvatarAdapter(getContext(), users);
        ScaleInAnimationAdapter animationAdapter = new ScaleInAnimationAdapter(avatarAdapter);
        animationAdapter.setAbsListView(gridView);
        gridView.setAdapter(animationAdapter);

        // set callback for each avatar
        // here we display the radial menu for user and show overlay
        // show menu based on user permission
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User user = appState.getCurrentCircle().getUsers().get(position);
                if (activity != null && activity instanceof MainActivity)
                    ((MainActivity) activity).showRadialMenuOptions(user);
            }
        });

        // add the layout
        rootView.addView(gridView, 0);

        return rootView;
    }

    public boolean toggleBroadcastingLocation(long duration) {
        Intent intent = new Intent(getActivity(), CirclePushService.class);
        intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_USER_ID), appState.getUser().getId());
        intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_CIRCLE_ID), appState.getCurrentCircle().getId());
        intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_DURATION), duration);

        DataUpdater dataUpdater = appState.getDataUpdater();

        if (!appState.getCurrentCircle().isUserBroadcastingLocation()) {
            // update DB telling it that this circle is broadcasting
            Toast.makeText(getActivity(), "Broadcasting location updates to " + appState.getCurrentCircle().getTitle(), Toast.LENGTH_LONG).show();
            appState.getCurrentCircle().setBroadcastingLocation(true);

            // update DB about broadcast location change to this circle
            //TODO update time of broadcast to in DB
            dataUpdater.updateCircleLocationBroadcast(appState.getCurrentCircle(), true);

            // start the push service, telling it to add the user's current circle to start broadcasting location to it
            intent.setAction(getResources().getString(R.string.ACTION_CIRCLE_ENABLE_LOCATION_BROADCAST));
            getActivity().startService(intent);

            // update icon avatar of the current user and in the map
            setAvatarBroadcastingAnimation(true);

            // notify map fragment about the broadcast enabling
            if (activity != null && activity instanceof MainActivity) {
                LocationFragment mapFragment = ((MainActivity) activity).getMapFragment();
                setOnBroadcastLocationListener(true, mapFragment);
            }

            return true;
        }
        else {
            // update DB telling it that this circle is no longer broadcasting
            Toast.makeText(getActivity(), "Stopped broadcasting location updates to " + appState.getCurrentCircle().getTitle(), Toast.LENGTH_LONG).show();
            appState.getCurrentCircle().setBroadcastingLocation(false);

            // update DB about broadcast location change to this cirlce
            dataUpdater.updateCircleLocationBroadcast(appState.getCurrentCircle(), false);

            // informs the push service to remove the user's current circle to stop broadcasting location to it
            intent.setAction(getResources().getString(R.string.ACTION_CIRCLE_DISABLE_LOCATION_BROADCAST));
            getActivity().startService(intent);

            // update icon avatar of the user and in the map
            setAvatarBroadcastingAnimation(false);

            // notify map fragment about the broadcast disabling
            if (activity != null && activity instanceof MainActivity) {
                LocationFragment mapFragment = ((MainActivity) activity).getMapFragment();
                setOnBroadcastLocationListener(false, mapFragment);
            }

            return false;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            activity = (Activity) context;
        }
    }

    private void setOnBroadcastLocationListener(boolean enabled, BroadcastLocationListener listener) {
        if (listener != null) {
            if (enabled)
                listener.onBroadcastLocationEnabled();
            else
                listener.onBroadcastLocationDisabled();
        }
    }

    private void setAvatarBroadcastingAnimation(boolean isBroadcasting) {
        int avatarIndex = 0;
        for (User user : users) {
            if (user.getId().equals(AppState.getInstance(getContext()).getUser().getId())) {
                View avatar = getAvatarByPosition(avatarIndex);
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
}
