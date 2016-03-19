package com.abborg.glom.fragments;

import android.app.Activity;
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

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.adapters.UserAvatarAdapter;
import com.abborg.glom.interfaces.BroadcastLocationListener;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.LayoutUtils;
import com.nhaarman.listviewanimations.appearance.simple.ScaleInAnimationAdapter;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * This fragment contains layout for the circle (group) in a circle.
 * The fragment can show group of users in grid style, circle style,
 * or a traditional scroll view style.
 * */
public class CircleFragment extends Fragment implements BroadcastLocationListener {

    private static final String TAG = "CIRCLE_FRAGMENT";

    private List<User> users;

    /* Whether or not the fragment is visible */
    public boolean isFragmentVisible;

    private UserAvatarAdapter avatarAdapter;

    // Required empty public constructor
    public CircleFragment() {}

    private AppState appState;

    private GridView gridView;

    private AdapterView.OnItemClickListener listener;

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

        appState = AppState.getInstance();

        if (appState.getActiveCircle() != null)
            users = appState.getActiveCircle().getUsers();
    }

    public void update() {
        if (appState != null) {
            users = appState.getActiveCircle().getUsers();
            avatarAdapter.update(users);
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
        gridView.setOnItemClickListener(listener);

        // add the layout
        rootView.addView(gridView, 0);

        return rootView;
    }

    @Override
    public void onBroadcastLocationEnabled(long duration) {
        setAvatarBroadcastingAnimation(true);
    }

    @Override
    public void onBroadcastLocationDisabled() {
        setAvatarBroadcastingAnimation(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            try {
                listener = (AdapterView.OnItemClickListener) context;
            }
            catch (ClassCastException ex) {
                Log.e(TAG, "The attached activity does not implement listener for GridView!");
            }
        }
    }

    private void setAvatarBroadcastingAnimation(boolean isBroadcasting) {
        int avatarIndex = 0;
        for (User user : users) {
            if (user.getId().equals(AppState.getInstance().getActiveUser().getId())) {
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
}
