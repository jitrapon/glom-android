package com.abborg.glom.ui;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.adapter.UserAvatarAdapter;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.User;
import com.abborg.glom.service.CirclePushService;
import com.abborg.glom.utils.CircleTransform;
import com.abborg.glom.utils.LayoutUtils;
import com.bumptech.glide.Glide;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.nhaarman.listviewanimations.appearance.simple.ScaleInAnimationAdapter;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

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

    FloatingActionMenu avatarActionMenu;

    // Required empty public constructor
    public CircleFragment() {}

    Animation fadeInAnim;

    Animation fadeOutAnim;

    RelativeLayout overlayLayout;

    ImageView menuOverlay;

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

        // initialize the second relative layout for overlay and avatar menu
        overlayLayout = new RelativeLayout(getActivity());
        RelativeLayout.LayoutParams overlayLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        overlayLayout.setLayoutParams(overlayLayoutParams);

        // initialize the overlay imageview
        menuOverlay = new ImageView(getActivity());
        RelativeLayout.LayoutParams menuOverlayParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        menuOverlay.setLayoutParams(menuOverlayParams);
        menuOverlay.setBackgroundColor(getResources().getColor(R.color.menuOverlay));

        // initialize the overlay avatar icon with radial menu
        final ImageView avatarIcon = new ImageView(getActivity());
        RelativeLayout.LayoutParams avatarIconParams = new RelativeLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.avatar_menu_size),
                getResources().getDimensionPixelSize(R.dimen.avatar_menu_size));
        avatarIconParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        avatarIcon.setLayoutParams(avatarIconParams);
//        avatarIcon.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                //TODO show user profile activity
//                Log.d(TAG, "Clicked avatar icon");
//            }
//        });

        // add fade-in / fade-out animation when visibilty changes
        fadeInAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        fadeInAnim.setDuration(150);
        fadeOutAnim.setDuration(150);

        menuOverlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                hideMenuOverlay(true);
            }
        });

        // add the overlay and avatar icon to the layout
        overlayLayout.addView(menuOverlay, 0);
        overlayLayout.addView(avatarIcon, 1);

        // hide the layout for now until an avatar is clicked
        overlayLayout.setVisibility(RelativeLayout.GONE);

        int blueSubActionButtonSize = getResources().getDimensionPixelSize(R.dimen.blue_sub_action_button_size);
        int blueSubActionButtonContentMargin = getResources().getDimensionPixelSize(R.dimen.blue_sub_action_button_content_margin);

        final SubActionButton.Builder lCSubBuilder = new SubActionButton.Builder(getActivity());
        lCSubBuilder.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_action_blue_selector));

        final FrameLayout.LayoutParams blueContentParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        blueContentParams.setMargins(blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin);
        lCSubBuilder.setLayoutParams(blueContentParams);

        // Set custom layout params
        FrameLayout.LayoutParams blueParams = new FrameLayout.LayoutParams(blueSubActionButtonSize, blueSubActionButtonSize);
        lCSubBuilder.setLayoutParams(blueParams);

        // set callback for each avatar
        // here we display the radial menu for user and show overlay
        // show menu based on user permission
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User user = appState.getCurrentCircle().getUsers().get(position);

                showMenuOverlay(true);

                // load the avatar picture
                Glide.with(getActivity())
                        .load(user.getAvatar()).fitCenter()
                        .transform(new CircleTransform(getActivity()))
                        .override((int) getActivity().getResources().getDimension(R.dimen.user_avatar_width),
                                (int) getActivity().getResources().getDimension(R.dimen.user_avatar_height))
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .crossFade(1000)
                        .into(avatarIcon);

                // load the menu based on user permission list
                avatarActionMenu = setupAvatarOptionMenu(getActivity(), lCSubBuilder, blueContentParams, user, avatarIcon);
            }
        });

        // add the layouts
        rootView.addView(gridView, 0);
        rootView.addView(overlayLayout, 1);

        return rootView;
    }

    private FloatingActionMenu setupAvatarOptionMenu(final Activity activity, SubActionButton.Builder builder,
                                       FrameLayout.LayoutParams params, User user, ImageView avatarIcon) {
        FloatingActionMenu.Builder menuBuilder =  new FloatingActionMenu.Builder(activity);
        List<Integer> userMenuOptions = user.getUserPermission();
        for (int option : userMenuOptions) {
            SubActionButton actionButton = setIconFromPermission(activity, builder, params, user, option);
            menuBuilder.addSubActionView(actionButton);
        }

        menuBuilder.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu floatingActionMenu) {
                Log.d(TAG, "Menu is opened");
            }

            @Override
            public void onMenuClosed(FloatingActionMenu floatingActionMenu) {
                Log.d(TAG, "Menu is closed");
            }
        });

        return menuBuilder.setRadius(getResources().getDimensionPixelSize(R.dimen.avatar_menu_radius_large))
                .setStartAngle(0)
                .setEndAngle(360)
                .attachTo(avatarIcon)
                .build();
    }

    private SubActionButton setIconFromPermission(final Activity activity, SubActionButton.Builder builder,
                                            FrameLayout.LayoutParams params, final User user, int userPerm) {
        ImageView icon = new ImageView(activity);
        SubActionButton actionButton = null;

        switch(userPerm) {
            case User.MEDIA_IMAGE_RECEIVE:
                icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_picture));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending image is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.MEDIA_AUDIO_RECEIVE:
                icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_audio));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending audio is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.MEDIA_VIDEO_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_video));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending video is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.ALARM_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_alarm));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending alarm is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.NOTE_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_note));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending note is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.LOCATION_REQUEST_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_place));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (user.getId().equals(appState.getUser().getId())) {
                            //TODO broadcast location dialog setting interval and duration of updates
                            hideMenuOverlay(true);
                            showBroadcastLocationMenuOptions();
                        }
                        else {
                            hideMenuOverlay(true);
                            Toast.makeText(activity, "Location request sent to " + user.getName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case User.SHOUT_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_important));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending shout is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.SECRET_MESSAGE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_text));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending message is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.SONG_SNIPPET_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_music));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Sending song snippet is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case User.POLL_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_poll));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(activity, "Asking is not supported yet", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            default: return null;
        }

        return actionButton;
    }

    private void showMenuOverlay(boolean animated) {
        fadeInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d(TAG, "Opening avatar menu");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }
        });
        overlayLayout.setVisibility(RelativeLayout.VISIBLE);
        menuOverlay.startAnimation(fadeInAnim);
    }

    private void hideMenuOverlay(boolean animated) {
        fadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                Log.d(TAG, "Clicked outside of avatar icon");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                overlayLayout.setVisibility(RelativeLayout.GONE);
            }
        });
        menuOverlay.startAnimation(fadeOutAnim);
        if (avatarActionMenu != null) avatarActionMenu.close(animated);
    }

    private void showBroadcastLocationMenuOptions() {
        if (activity != null && activity instanceof MainActivity) {
            final BottomSheetLayout bottomSheet = ((MainActivity) activity).getBottomSheet();
            final View bottomSheetLayout = ((MainActivity) activity).getBroadcastLocationSheetLayout();
            bottomSheet.showWithSheetView(bottomSheetLayout);
//            float peekTranslation = bottomSheetLayout.findViewById(R.id.toggleBroadcastLocationSwitch).getHeight();
//            bottomSheet.setPeekSheetTranslation(peekTranslation);
//            bottomSheet.peekSheet();
        }
    }

    public boolean toggleBroadcastingLocation() {
        Intent intent = new Intent(getActivity(), CirclePushService.class);
        intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_USER_ID), appState.getUser().getId());
        intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_CIRCLE_ID), appState.getCurrentCircle().getId());

        DataUpdater dataUpdater = appState.getDataUpdater();

        if (!appState.getCurrentCircle().isUserBroadcastingLocation()) {
            // update DB telling it that this circle is broadcasting
            Toast.makeText(getActivity(), "Broadcasting location updates to " + appState.getCurrentCircle().getTitle(), Toast.LENGTH_LONG).show();
            appState.getCurrentCircle().setBroadcastingLocation(true);

            // update DB about broadcast location change to this circle
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

    private void setOnBroadcastLocationListener(boolean enabled, OnBroadcastLocationListener listener) {
        if (listener != null) {
            if (enabled)
                listener.onBroadcastLocationEnabled();
            else
                listener.onBroadcastLocationDisabled();
        }
    }

    public interface OnBroadcastLocationListener {
        void onBroadcastLocationEnabled();

        void onBroadcastLocationDisabled();
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
