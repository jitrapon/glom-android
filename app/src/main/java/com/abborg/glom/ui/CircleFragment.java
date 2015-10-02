package com.abborg.glom.ui;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.abborg.glom.R;
import com.abborg.glom.adapter.UserAvatarAdapter;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.CircleTransform;
import com.abborg.glom.utils.LayoutUtils;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * This fragment contains layout for the circle (group) in a circle.
 * The fragment can show group of users in grid style, circle style,
 * or a traditional scroll view style.
 * */
public class CircleFragment extends Fragment {

    private static final String TAG = "CIRCLE_FRAGMENT";

    /* Stored shared preferences for this app */
    private SharedPreferences sharedPref;

    /* This profile's user */
    private User currentUser;

    /* This active circle */
    private Circle circle;

    private List<User> users;

    /* Whether or not the fragment is visible */
    public boolean isFragmentVisible;

    private UserAvatarAdapter avatarAdapter;

    // Required empty public constructor
    public CircleFragment() {}

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
        currentUser = ((MainActivity) getActivity()).getUser();
        circle = ((MainActivity) getActivity()).getCurrentCircle();
        users = circle.getUsers();
    }

    public void update() {
        circle = ((MainActivity) getActivity()).getCurrentCircle();
        users = circle.getUsers();
        avatarAdapter.update(users);
    }

    @Override
    // based on Grid, Circle, or List
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RelativeLayout rootView = (RelativeLayout) inflater.inflate(R.layout.fragment_circle, container, false);

        // currently default to GridView
        // initialize the user view
        GridView gridView = new GridView(getActivity());
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
        gridView.setPadding(0, LayoutUtils.pxToDp(getContext(), (int)getResources().getDimension(R.dimen.user_avatar_padding_bottom)), 0, 0);

        // set the adapter for this view
        avatarAdapter = new UserAvatarAdapter(getContext(), users);
        gridView.setAdapter(avatarAdapter);

        // set callback for each avatar
        // here we display the radial menu for user
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User selected = circle.getUsers().get(position);

                // load the cached? avatar
                Context context = getActivity();
                ImageView avatarIcon = new ImageView(context);
                Glide.with(context)
                        .load(selected.getAvatar()).fitCenter()
                        .transform(new CircleTransform(context))
//                        .override((int) context.getResources().getDimension(R.dimen.user_avatar_width),
//                                (int) context.getResources().getDimension(R.dimen.user_avatar_height))
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .crossFade(1000)
                        .into(avatarIcon);

                AvatarActionButton avatarActionButton = new AvatarActionButton.Builder(getActivity())
                        .setContentView(avatarIcon)
                        .build();

                Toast.makeText(getActivity(), "Selected " + selected.getName() + ", " + selected.getId(), Toast.LENGTH_SHORT).show();
            }
        });

        // add the user view to the root layout
        rootView.addView(gridView);

        return rootView;
    }

}
