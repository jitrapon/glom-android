package com.abborg.glom.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.data.DataProvider;
import com.abborg.glom.fragments.BoardFragment;
import com.abborg.glom.fragments.CircleFragment;
import com.abborg.glom.fragments.DiscoverFragment;
import com.abborg.glom.fragments.DrawerFragment;
import com.abborg.glom.fragments.LocationFragment;
import com.abborg.glom.interfaces.BoardItemChangeListener;
import com.abborg.glom.interfaces.BroadcastLocationListener;
import com.abborg.glom.interfaces.DiscoverItemChangeListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.model.CloudProvider;
import com.abborg.glom.model.DiscoverItem;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.model.User;
import com.abborg.glom.service.CirclePushService;
import com.abborg.glom.service.RegistrationIntentService;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        DrawerFragment.FragmentDrawerListener,
        Handler.Callback,
        AdapterView.OnItemClickListener,
        ActionMode.Callback {

    protected static final String TAG = "MainActivity";

    /**
     * The global class at the application context level
     */
    private ApplicationState appState;

    /**
     * Abstracts data management via SQLite and network operations
     */
    public DataProvider dataProvider;

    /**
     * A receiver for incoming location updates and other various GCM push updates
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * Adapter for the tab view
     */
    private ViewPagerAdapter adapter;

    /**
     * Handler that indicates when asynchronous events are completed to update UI
     */
    private Handler handler;

    /**
     * Board item change listeners
     */
    private List<BoardItemChangeListener> boardItemChangeListeners;

    /**
     * Broadcast location listeners
     */
    private List<BroadcastLocationListener> broadcastLocationListeners;

    /**
     * Discover item change listeners
     */
    private List<DiscoverItemChangeListener> discoverItemChangeListeners;

    // UI elements
    private TabLayout tabLayout;
    private int[] tabIcons = {
            R.drawable.ic_tab_circle,
            R.drawable.ic_tab_location,
            R.drawable.ic_tab_event,
            R.drawable.ic_tab_discover
    };
    private BottomSheetLayout bottomSheet;
    private View broadcastLocationSheetLayout;
    private SwitchCompat broadcastLocationToggle;
    private CoordinatorLayout mainCoordinatorLayout;
    private FloatingActionMenu avatarActionMenu;
    private Animation fadeInAnim;
    private Animation fadeOutAnim;
    private RelativeLayout overlayLayout;
    private ImageView menuOverlay;
    private ImageView avatarIcon;
    private SubActionButton.Builder lCSubBuilder;
    private FrameLayout.LayoutParams blueContentParams;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private android.support.design.widget.FloatingActionButton fab;
    private boolean isRadialMenuOptionsOpening;
    private ActionMode actionMode;

    private static final boolean SHOW_TAB_TITLE = false;
    private static final int MENU_OVERLAY_ANIM_TIME = 150;
    private static final boolean START_YOUTUBE_VIDEO_LIGHTBOX = false;

    /**************************************************
     * View pager adapter that controls the pages
     **************************************************/

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragmentList = new ArrayList<>();
        private final List<String> fragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            fragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return SHOW_TAB_TITLE ? fragmentTitleList.get(position) : null;
        }
    }

    /**********************************************************
     * VIEW INITIALIZATIONS
     **********************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up handler for receiving all messages
        handler = new Handler(this);

        setupView();

        appState = ApplicationState.init(this, handler);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // connect to the Google Play API
        if (appState != null) {
            appState.connectGoogleApiClient();
            appState.setKeepGoogleApiClientAlive(false);
        }

        // register local broadcast receiver
        if (broadcastReceiver != null) {
            registerBroadcastReceiver(broadcastReceiver);
        }

        // get database writable object, if already initialized
        if (dataProvider != null) {
            try {
                dataProvider.open();
            } catch (SQLException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }
    }

    @Override
    protected void onStop() {
        // unregister the local broadcast receiver
        if (broadcastReceiver != null) {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            broadcastManager.unregisterReceiver(broadcastReceiver);
        }

        // disconnect google api client
        if (appState != null)
            if (!appState.shouldKeepGoogleApiAlive()) appState.disconnectGoogleApiClient();

        // close database
        if (dataProvider != null) dataProvider.close();

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void setupEventListeners() {
        addEventChangeListener((LocationFragment) adapter.getItem(1));
        addEventChangeListener((BoardFragment) adapter.getItem(2));

        addBroadcastLocationListener((CircleFragment) adapter.getItem(0));
        addBroadcastLocationListener((LocationFragment) adapter.getItem(1));

        addDiscoverItemChangeListener((DiscoverFragment) adapter.getItem(3));
    }

    private void setupView() {
        setContentView(R.layout.activity_main);

        mainCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.parentView);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        overlayLayout = (RelativeLayout) findViewById(R.id.overlayLayout);

        setSupportActionBar(toolbar);

        fab.hide();
    }

    private void updateView() {
        // set up tabs
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(tabIcons[0]).setTag(Const.TAB_CIRCLE);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]).setTag(Const.TAB_MAP);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]).setTag(Const.TAB_BOARD);
        tabLayout.getTabAt(3).setIcon(tabIcons[3]).setTag(Const.TAB_DISCOVER);
        tabLayout.setOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        if (tab.getTag().equals(Const.TAB_BOARD)) {
                            if (actionMode != null) actionMode.finish();
                        }
                    }
                });

        // set up the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(appState.getActiveCircle().getTitle()
                    + " (" + appState.getActiveCircle().getUsers().size() + ")");
        }

        // set up the navigation drawer
        DrawerFragment drawerFragment = (DrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);
        drawerFragment.setDrawerListener(this);

        // set up the bottom sheets
        bottomSheet.setShouldDimContentView(false);
        broadcastLocationSheetLayout = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_broadcast_location, bottomSheet, false);
        broadcastLocationToggle = (SwitchCompat) broadcastLocationSheetLayout.findViewById(R.id.toggleBroadcastLocationSwitch);
        broadcastLocationToggle.setChecked(appState.getActiveCircle().isUserBroadcastingLocation());

        // set up broadcast location sheet
        final ImageButton endTimePickerHourIncr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerHourIncr);
        final TextView endTimePickerHour = (TextView) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerHour);
        endTimePickerHourIncr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = Integer.parseInt(endTimePickerHour.getText().toString());
                int incrHour = hour+1 > 12 ? 1 : hour+1;
                endTimePickerHour.setText(incrHour + "");
            }
        });
        final ImageButton endTimePickerMinuteIncr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerMinuteIncr);
        final TextView endTimePickerMinute = (TextView) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerMinute);
        endTimePickerMinuteIncr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int minute = Integer.parseInt(endTimePickerMinute.getText().toString());
                int incrMinute = minute+1 > 59 ? 0 : minute+1;
                endTimePickerMinute.setText(String.format("%02d", incrMinute));
            }
        });
        final ImageButton endTimePickerHourDecr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerHourDecr);
        endTimePickerHourDecr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = Integer.parseInt(endTimePickerHour.getText().toString());
                int decrHour = hour-1 < 1 ? 12 : hour-1;
                endTimePickerHour.setText(decrHour + "");
            }
        });
        final ImageButton endTimePickerMinuteDecr = (ImageButton) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerMinuteDecr);
        endTimePickerMinuteDecr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int minute = Integer.parseInt(endTimePickerMinute.getText().toString());
                int decrMinute = minute-1 < 0 ? 59 : minute-1;
                endTimePickerMinute.setText(String.format("%02d", decrMinute));
            }
        });
        final TextView endTimeAMPMPicker = (TextView) broadcastLocationSheetLayout.findViewById(R.id.endTimePickerAMPM);
        endTimeAMPMPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amPm = endTimeAMPMPicker.getText().toString();
                if (amPm.equals(getResources().getString(R.string.time_unit_before_noon))) {
                    endTimeAMPMPicker.setText(getResources().getString(R.string.time_unit_after_noon));
                }
                else {
                    endTimeAMPMPicker.setText(getResources().getString(R.string.time_unit_before_noon));
                }
            }
        });

        // set up broadcast location toggle
        final Context context = this;
        broadcastLocationToggle.setOnClickListener(new CompoundButton.OnClickListener() {

            @Override
            public void onClick(View buttonView) {

                Intent intent = new Intent(context, CirclePushService.class);
                intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_USER_ID), appState.getActiveUser().getId());
                intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_CIRCLE_ID), appState.getActiveCircle().getId());

                // enabling broadcast location
                if (broadcastLocationToggle.isChecked()) {
                    DateTime now = new DateTime();

                    // if end hour - start hour is negative, add 24 to get the duration from current hour
                    // convert to 24-hour time
                    String amPm = endTimeAMPMPicker.getText().toString();
                    int endHour = Integer.parseInt(endTimePickerHour.getText().toString());
                    if (amPm.equals(getResources().getString(R.string.time_unit_after_noon)) && endHour != 12) {
                        endHour += 12;
                    }
                    else if (amPm.equals(getResources().getString(R.string.time_unit_before_noon)) && endHour == 12) {
                        endHour = 0;
                    }

                    int hourDiff = endHour - now.getHourOfDay();
                    if (hourDiff < 0) {
                        hourDiff += 24;
                    }
                    DateTime endTime = now.plusHours(hourDiff);

                    Duration durationFromNow = new Duration(now, endTime);
                    Long duration = durationFromNow.getMillis();

                    // tell all listeners to update their UI accordingly
                    if (broadcastLocationListeners != null) {
                        for (BroadcastLocationListener listener : broadcastLocationListeners) {
                            listener.onBroadcastLocationEnabled(duration);
                        }
                    }

                    // update DB telling it that this circle is broadcasting
                    Toast.makeText(context, "Broadcasting location updates to "
                            + appState.getActiveCircle().getTitle(), Toast.LENGTH_LONG).show();
                    appState.getActiveCircle().setBroadcastingLocation(true);

                    // update DB about broadcast location change to this circle
                    //TODO update time of broadcast to in DB
                    dataProvider.updateCircleLocationBroadcast(appState.getActiveCircle().getId(), true);

                    // start the push service, telling it to add the user's current circle to start broadcasting location to it
                    intent.putExtra(getResources().getString(R.string.EXTRA_BROADCAST_LOCATION_DURATION), duration);
                    intent.setAction(getResources().getString(R.string.ACTION_CIRCLE_ENABLE_LOCATION_BROADCAST));
                    startService(intent);
                }

                // disabling broadcast location
                else {
                    if (broadcastLocationListeners != null) {
                        for (BroadcastLocationListener listener : broadcastLocationListeners) {
                            listener.onBroadcastLocationDisabled();
                        }
                    }

                    // update DB telling it that this circle is no longer broadcasting
                    Toast.makeText(context, "Stopped broadcasting location updates to "
                            + appState.getActiveCircle().getTitle(), Toast.LENGTH_LONG).show();
                    appState.getActiveCircle().setBroadcastingLocation(false);

                    // update DB about broadcast location change to this cirlce
                    dataProvider.updateCircleLocationBroadcast(appState.getActiveCircle().getId(), false);

                    // informs the push service to remove the user's current circle to stop broadcasting location to it
                    intent.setAction(getResources().getString(R.string.ACTION_CIRCLE_DISABLE_LOCATION_BROADCAST));
                    startService(intent);
                }
            }
        });

        // set up the floating action button for all fragments
        if (fab != null) {
            fab.show();
            fab.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    // click event for circle fragment
                    if (adapter.getItem(viewPager.getCurrentItem()) instanceof CircleFragment) {
                        showRadialMenuOptions(appState.getActiveUser());
                    } else if (adapter.getItem(viewPager.getCurrentItem()) instanceof LocationFragment) {

                    } else if (adapter.getItem(viewPager.getCurrentItem()) instanceof BoardFragment) {
                        showRadialMenuOptions(appState.getActiveUser());
                    }
                }
            });
        }

        // initialize the overlay imageview
        menuOverlay = new ImageView(this);
        RelativeLayout.LayoutParams menuOverlayParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        menuOverlay.setLayoutParams(menuOverlayParams);
        menuOverlay.setBackgroundColor(getResources().getColor(R.color.menuOverlay));

        // initialize the overlay avatar icon with radial menu
        avatarIcon = new ImageView(this);
        RelativeLayout.LayoutParams avatarIconParams = new RelativeLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.avatar_menu_size),
                getResources().getDimensionPixelSize(R.dimen.avatar_menu_size));
        avatarIconParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        avatarIcon.setLayoutParams(avatarIconParams);

        // add fade-in / fade-out animation when visibilty changes
        fadeInAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        fadeInAnim.setDuration(MENU_OVERLAY_ANIM_TIME);
        fadeOutAnim.setDuration(MENU_OVERLAY_ANIM_TIME);

        menuOverlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isRadialMenuOptionsOpening) hideMenuOverlay(true);
            }
        });

        // add the overlay and avatar icon to the layout
        overlayLayout.addView(menuOverlay, 0);
        overlayLayout.addView(avatarIcon, 1);

        // hide the layout for now until an avatar is clicked
        overlayLayout.setVisibility(RelativeLayout.GONE);

        int blueSubActionButtonSize = getResources().getDimensionPixelSize(R.dimen.blue_sub_action_button_size);
        int blueSubActionButtonContentMargin = getResources().getDimensionPixelSize(R.dimen.blue_sub_action_button_content_margin);

        lCSubBuilder = new SubActionButton.Builder(this);
        lCSubBuilder.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_action_blue_selector));

        blueContentParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        blueContentParams.setMargins(blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin,
                blueSubActionButtonContentMargin);
        lCSubBuilder.setLayoutParams(blueContentParams);

        // Set custom layout params
        FrameLayout.LayoutParams blueParams = new FrameLayout.LayoutParams(blueSubActionButtonSize, blueSubActionButtonSize);
        lCSubBuilder.setLayoutParams(blueParams);
    }

    private void setupBroadcastReceiver() {

        // register the local broadcast receiver for our gcm listener service updates
        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                // location updates from MessageService
                // updates from OTHER users
                if ( intent.getAction().equals(getResources().getString(R.string.ACTION_RECEIVE_LOCATION)) ) {
                    List<User> users = dataProvider.getLocationUpdates(intent, appState.getActiveCircle());

                    if (users != null && users.size() > 0) {

                        // only update the location markers when the map is visible and this is the current circle
                        LocationFragment map = (LocationFragment) adapter.getItem(1);
                        if (map.isFragmentVisible) {
                            map.updateUserMarkers(users);
                        }

                        Toast.makeText(context, users.get(0).getId() + ": " + users.get(0).getLocation().getLatitude()
                                + ", " + users.get(0).getLocation().getLongitude(), Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(context, "Received location update from server", Toast.LENGTH_SHORT).show();
                    }
                }

                // location updates from CirclePushService
                // user's OWN location updates
                else if (intent.getAction().equals(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE))) {
                    Location location = intent.getParcelableExtra(getResources().getString(R.string.EXTRA_USER_LOCATION_UPDATE));
                    List<String> circleBroadcastList = intent.getStringArrayListExtra(getResources().getString(R.string.EXTRA_CIRCLES_LOCATION_UPDATE));

                    // only update the location markers when the map is visible
                    // and this circle is in the broadcast list
                    String circleId = appState.getActiveCircle().getId();
                    LocationFragment map = (LocationFragment) adapter.getItem(1);

                    // update the user's location in this circle
                    User currentUser = null;
                    for (User user : appState.getActiveCircle().getUsers()) {
                        if (user.getId().equals(appState.getActiveUser().getId())) {
                            currentUser = user;
                            currentUser.setLocation(location);
                        }
                    }

                    if (map.isFragmentVisible && circleBroadcastList.contains(circleId)) {
                        if (currentUser != null) {
                            map.updateUserMarkers(Arrays.asList(currentUser));
                        }
                    }
                }

                // incoming message
                else if (intent.getAction().equals(getResources().getString(R.string.ACTION_NEW_MESSAGE))) {

                }
            }
        };

        registerBroadcastReceiver(broadcastReceiver);
    }

    private void registerBroadcastReceiver(BroadcastReceiver receiver) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getResources().getString(R.string.ACTION_RECEIVE_LOCATION));
        intentFilter.addAction(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE));
        intentFilter.addAction(getResources().getString(R.string.ACTION_NEW_MESSAGE));
        broadcastManager.registerReceiver(receiver, intentFilter);
    }

    private void setupService() {
        // start IntentService to register this application with GCM
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(getResources().getString(R.string.EXTRA_SEND_TOKEN_USER_ID), appState.getActiveUser().getId());
        startService(intent);
    }

    private void addEventChangeListener(BoardItemChangeListener listener) {
        if (listener != null) {
            if (boardItemChangeListeners == null) {
                boardItemChangeListeners = new ArrayList<>();
                boardItemChangeListeners.add(listener);
            }
            else boardItemChangeListeners.add(listener);
        }
    }

    private void addBroadcastLocationListener(BroadcastLocationListener listener) {
        if (listener != null) {
            if (broadcastLocationListeners == null) {
                broadcastLocationListeners = new ArrayList<>();
                broadcastLocationListeners.add(listener);
            }
            else broadcastLocationListeners.add(listener);
        }
    }

    private void addDiscoverItemChangeListener(DiscoverItemChangeListener listener) {
        if (listener != null) {
            if (discoverItemChangeListeners == null) {
                discoverItemChangeListeners = new ArrayList<>();
            }
            discoverItemChangeListeners.add(listener);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new CircleFragment(), Const.TAB_CIRCLE);
        adapter.addFragment(new LocationFragment(), Const.TAB_MAP);
        adapter.addFragment(new BoardFragment(), Const.TAB_BOARD);
        adapter.addFragment(new DiscoverFragment(), Const.TAB_DISCOVER);
        viewPager.setAdapter(adapter);
    }

    public void showRadialMenuOptions(User user) {
        showMenuOverlay(true);

        // load the avatar picture
        Glide.with(this)
                .load(user.getAvatar()).fitCenter()
                .transform(new CircleTransform(this))
                .override((int) getResources().getDimension(R.dimen.user_avatar_width),
                        (int) getResources().getDimension(R.dimen.user_avatar_height))
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .crossFade(1000)
                .into(avatarIcon);

        // load the menu based on user permission list
        avatarActionMenu = setupAvatarOptionMenu(this, lCSubBuilder, blueContentParams, user, avatarIcon);
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                if (!avatarActionMenu.isOpen())
                    avatarActionMenu.open(true);
            }
        }, 50);
    }

    private SubActionButton setIconFromPermission(final Activity activity, SubActionButton.Builder builder,
                                                  FrameLayout.LayoutParams params, final User user, int userPerm) {
        ImageView icon = new ImageView(activity);
        SubActionButton actionButton;

        switch(userPerm) {
            case User.POST_IMAGE_GALLERY:
                icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_picture));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openFileBrowser("image/*");
                        hideMenuOverlay(false);
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
            case User.NOTE_RECEIVE: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_grease_pencil));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(activity, DrawActivity.class);
                        intent.setAction(getResources().getString(R.string.ACTION_CREATE_DRAWING));
                        startActivityForResult(intent, Const.DRAW_RESULT_CODE);
                        hideMenuOverlay(false);
                    }
                });
                break;
            case User.REQUEST_LOCATION: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_place));
                actionButton = builder.setContentView(icon, params).build();
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (user.getId().equals(appState.getActiveUser().getId())) {
                            //TODO broadcast location dialog setting interval and duration of updates
                            hideMenuOverlay(false);
                            showBroadcastLocationMenuOptions();
                        }
                        else {
                            hideMenuOverlay(false);
                            Toast.makeText(activity, "Location request sent to " + user.getName(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;
            case User.CREATE_EVENT: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_planner));
                actionButton = builder.setContentView(icon, params).build();
                final Context context = this;
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideMenuOverlay(false);
                        Intent intent = new Intent(context, EventActivity.class);
                        intent.setAction(getResources().getString(R.string.ACTION_CREATE_EVENT));

                        // make sure to not disconnect Google Api just yet
                        appState.setKeepGoogleApiClientAlive(true);
                        startActivityForResult(intent, Const.CREATE_EVENT_RESULT_CODE);
                    }
                });
                break;
            default: return null;
        }

        return actionButton;
    }

    private void openFileBrowser(String fileType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType(fileType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.intent_select_images)), Const.IMAGE_SELECTED_RESULT_CODE);
    }

    private void showBroadcastLocationMenuOptions() {
        bottomSheet.showWithSheetView(broadcastLocationSheetLayout);
//            float peekTranslation = bottomSheetLayout.findViewById(R.id.toggleBroadcastLocationSwitch).getHeight();
//            bottomSheet.setPeekSheetTranslation(peekTranslation);
//            bottomSheet.peekSheet();
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
                isRadialMenuOptionsOpening = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isRadialMenuOptionsOpening = false;
                    }
                }, 700);
            }

            @Override
            public void onMenuClosed(FloatingActionMenu floatingActionMenu) {
                isRadialMenuOptionsOpening = false;
            }
        });

        return menuBuilder.setRadius(getResources().getDimensionPixelSize(R.dimen.avatar_menu_radius_large))
                .setStartAngle(0)
                .setEndAngle(360)
                .attachTo(avatarIcon)
                .build();
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
        if (animated) menuOverlay.startAnimation(fadeOutAnim);
        else overlayLayout.setVisibility(RelativeLayout.GONE);
        if (avatarActionMenu != null) avatarActionMenu.close(animated);
    }

    public void setFloatingActionButtonVisible(boolean visible) {
        if (fab != null) {
            if (visible) fab.show();
            else fab.hide();
        }
    }

    /**
     * Forces updates of all fragments and UI. Use only if selecting a new circle to display.
     */
    private void updateView(Circle circle) {
        appState.setActiveCircle(circle);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(circle.getTitle() + " (" + circle.getUsers().size() + ")");

        // update broadcast location sheet
        broadcastLocationToggle.setChecked(appState.getActiveCircle().isUserBroadcastingLocation());

        // refresh all fragments
        ((CircleFragment) adapter.getItem(0)).update();
        ((LocationFragment) adapter.getItem(1)).update(true, true);
        ((BoardFragment) adapter.getItem(2)).update();
    }


    /**********************************************************
     * Handler
     **********************************************************/
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            /* On first load */
            case Const.MSG_INIT_SUCCESS:
                try {
                    dataProvider = appState.getDataProvider();
                    dataProvider.open();
                } catch (SQLException ex) {
                    Log.e(TAG, ex.getMessage());
                }

                updateView();

                setupBroadcastReceiver();

                setupEventListeners();

                setupService();

                dataProvider.requestGetUsersInCircle(appState.getActiveCircle());

                break;

            /* Show toast message */
            case Const.MSG_SHOW_TOAST: {
                String message = msg.obj == null ? null : (String) msg.obj;

                if (!TextUtils.isEmpty(message)) {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }

                break;
            }

            /* Request: get list of users in circle */
            case Const.MSG_GET_USERS:
                Circle circle = appState.getActiveCircle();

                if (getSupportActionBar() != null)
                    getSupportActionBar().setTitle(circle.getTitle() + " (" + circle.getUsers().size() + ")");
                CircleFragment circleFragment = (CircleFragment) adapter.getItem(0);
                circleFragment.update();

                LocationFragment mapFragment = (LocationFragment) adapter.getItem(1);
                mapFragment.updateUserMarkers(circle.getUsers());
                break;

            /* Request: get board item in a circle */
            case Const.MSG_GET_ITEMS:
                ((LocationFragment) adapter.getItem(1)).update(false, true);
                ((BoardFragment) adapter.getItem(2)).update();
                break;

            case Const.MSG_EVENT_CREATED: {
                final EventItem event = msg.obj == null ? null : (EventItem) msg.obj;

                if (event != null) {
                    appState.getActiveCircle().addItem(event);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemAdded(event.getId());
                        }
                    }
                }

                break;
            }

            /* Request: create event successfully synced with server */
            case Const.MSG_EVENT_CREATED_SUCCESS: {
                EventItem item = msg.obj == null ? null : (EventItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.notification_created_item_success),
                        Toast.LENGTH_LONG).show();
                break;
            }

            /* Request: create event failed to sync with server */
            case Const.MSG_EVENT_CREATED_FAILED: {
                final EventItem item = msg.obj == null ? null : (EventItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                Snackbar.make(mainCoordinatorLayout, getResources().getString(R.string.notification_created_item_failed),
                        Snackbar.LENGTH_LONG)
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.requestCreateEvent(appState.getActiveCircle(), item);
                                }
                            }
                        })
                        .show();
                break;
            }

            /* Request: update event successfully */
            case Const.MSG_EVENT_UPDATED: {
                final EventItem event = msg.obj == null ? null : (EventItem) msg.obj;

                if (event != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(event.getId());
                        }
                    }
                }

                break;
            }

            /* Request: update event successfully synced with server */
            case Const.MSG_EVENT_UPDATED_SUCCESS: {
                EventItem item = msg.obj == null ? null : (EventItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.notification_updated_item_success),
                        Toast.LENGTH_LONG).show();
                break;
            }

            /* Request: update event failed to sync with server */
            case Const.MSG_EVENT_UPDATED_FAILED: {
                final EventItem item = msg.obj == null ? null : (EventItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                Snackbar.make(mainCoordinatorLayout, getResources().getString(R.string.notification_updated_item_failed),
                        Snackbar.LENGTH_LONG)
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.requestUpdateEvent(appState.getActiveCircle(), item);
                                }
                            }
                        })
                        .show();
                break;
            }

            /* Request: delete board item from a circle */
            case Const.MSG_ITEM_TO_DELETE: {
                String id = (String) msg.obj;

                dataProvider.deleteItemAsync(id, appState.getActiveCircle(), true);

                break;
            }

            /* Request: delete board item successfully */
            case Const.MSG_ITEM_DELETED_SUCCESS: {
                BoardItem item = msg.obj == null ? null : (BoardItem) msg.obj;
                if (item != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemDeleted(item.getId());
                        }
                    }
                    appState.getActiveCircle().removeItem(item);

                    Snackbar.make(mainCoordinatorLayout, getResources().getQuantityString(R.plurals.notification_delete_item, 1, 1),
                            Snackbar.LENGTH_LONG)
                            .setAction(getResources().getString(R.string.menu_item_undo), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //TODO
                                }
                            }).show();
                }

                break;
            }

            /* Request: delete board item failed */
            case Const.MSG_ITEM_DELETED_FAILED: {
                final BoardItem item = msg.obj==null ? null : (BoardItem) msg.obj;

                Snackbar.make(mainCoordinatorLayout, getResources().getString(R.string.notification_delete_item_failed),
                        Snackbar.LENGTH_LONG)
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.deleteItemAsync(item.getId(), appState.getActiveCircle(), true);
                                }
                            }
                        }).show();
                break;
            }

            /* Request: discover items received */
            case Const.MSG_DISCOVER_ITEM: {
                int type = msg.arg1;
                boolean ok = msg.arg2 == 0;
                List<DiscoverItem> results = null;
                if (msg.obj != null && ok) {
                    results = (List<DiscoverItem>) msg.obj;
                }

                if (discoverItemChangeListeners != null) {
                    for (DiscoverItemChangeListener listener : discoverItemChangeListeners) {
                        listener.onItemsReceived(type, results);
                    }
                }

                break;
            }

            /* Play Youtube video */
            case Const.MSG_PLAY_YOUTUBE_VIDEO: {
                String videoId = (String) msg.obj;
                Intent intent = YouTubeStandalonePlayer.createVideoIntent(
                        this, appState.getGoogleApiKey(), videoId, 0, true, START_YOUTUBE_VIDEO_LIGHTBOX);
                startActivity(intent);
                break;
            }

            /* File posted */
            case Const.MSG_FILE_POSTED: {
                final FileItem file = msg.obj == null ? null : (FileItem) msg.obj;

                if (file != null) {
                    appState.getActiveCircle().addItem(file);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemAdded(file.getId());
                        }
                    }
                }

                break;
            }

            /* File is in sync progress with the server */
            case Const.MSG_FILE_POST_IN_PROGRESS: {
                FileItem file = msg.obj == null ? null : (FileItem) msg.obj;
                int status = msg.arg1;
                int progress = msg.arg2;

                if (file != null) {
                    file.setSyncStatus(status);
                    file.setProgress(progress);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(file.getId());
                        }
                    }
                }

                break;
            }

            /* File has been posted and synced to the server successfully */
            case Const.MSG_FILE_POST_SUCCESS: {
                FileItem file = msg.obj == null ? null : (FileItem) msg.obj;
                int status = msg.arg1;

                if (file != null) {
                    file.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(file.getId());
                        }
                    }
                }

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.notification_created_item_success),
                        Toast.LENGTH_LONG).show();
                break;
            }

            /* File failed to sync to the server */
            case Const.MSG_FILE_POST_FAILED: {
                final FileItem item = msg.obj == null ? null : (FileItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                Snackbar.make(mainCoordinatorLayout, getResources().getString(R.string.notification_created_item_failed),
                        Snackbar.LENGTH_LONG).show();
                break;
            }

            /* Start downloading of a file */
            case Const.MSG_DOWNLOAD_ITEM: {
                final FileItem item = msg.obj == null ? null : (FileItem) msg.obj;

                if (item != null) {
                    dataProvider.requestDownloadFileRemote(appState.getActiveCircle(), item, CloudProvider.AMAZON_S3);
                }

                break;
            }

            /* When download completes successfully */
            case Const.MSG_FILE_DOWNLOAD_COMPLETE: {
                final FileItem item = msg.obj == null ? null : (FileItem) msg.obj;

                if (item != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }

                    Toast.makeText(getApplicationContext(),
                            String.format(getResources().getString(R.string.notification_download_item_success), item.getName()),
                            Toast.LENGTH_LONG).show();
                }

                break;
            }

            /* When download failed */
            case Const.MSG_FILE_DOWNLOAD_FAILED: {
                final FileItem item = msg.obj == null ? null : (FileItem) msg.obj;

                if (item != null) {
                    Toast.makeText(getApplicationContext(),
                            String.format(getResources().getString(R.string.notification_download_item_failed), item.getName()),
                            Toast.LENGTH_LONG).show();
                }

                break;
            }

            /* When a drawing is created/updated */
            case Const.MSG_DRAWING_POSTED: {
                final DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;

                if (item != null) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.notification_created_item_success),
                            Toast.LENGTH_LONG).show();

                    appState.getActiveCircle().addItem(item);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemAdded(item.getId());
                        }
                    }
                }

                break;
            }

            /* User has updated the drawing */
            case Const.MSG_DRAWING_UPDATED: {
                final DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;

                if (item != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                break;
            }

            /* Drawing is syncing */
            case Const.MSG_DRAWING_POST_IN_PROGRESS: {
                DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                break;
            }

            /* Drawing has been posted and synced to the server successfully */
            case Const.MSG_DRAWING_POST_SUCCESS: {
                DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                Toast.makeText(getApplicationContext(), getResources().getString(R.string.notification_updated_item_success),
                        Toast.LENGTH_LONG).show();
                break;
            }

            /* Drawing failed to sync to the server */
            case Const.MSG_DRAWING_POST_FAILED: {
                final DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                Snackbar.make(mainCoordinatorLayout, getResources().getString(R.string.notification_updated_item_failed),
                        Snackbar.LENGTH_LONG).show();
                break;
            }

            /* Starting of ACTION MODE */
            case Const.MSG_START_ACTION_MODE: {
                if (actionMode == null) {
                    actionMode = this.startSupportActionMode(this);
                    actionMode.setTitle(String.format(getString(R.string.title_action_mode_board_items), 1));
                }

                break;
            }

            /* Board item selection */
            case Const.MSG_SELECT_BOARD_ITEM: {
                if (actionMode != null) {
                    int selected = (Integer) msg.obj;
                    actionMode.setTitle(String.format(getString(R.string.title_action_mode_board_items), selected));
                    actionMode.invalidate();
                }

                break;
            }
        }

        return false;
    }

    public Handler getHandler() { return handler; }

    /**********************************************************
     * Menu Handler
     **********************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }
        else if (id == R.id.action_chat) {
            if (appState != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                startActivity(intent);
            }
            return true;
        }
        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**********************************************************
     * Drawer Handler
     **********************************************************/
    @Override
    public void onDrawerItemSelected(View view, int position) {
        List<CircleInfo> circles = appState.getAllCircleInfo();
        CircleInfo c = circles.get(position);
        Circle selected = dataProvider.getCircleById(c.id);

        if (selected != null) {
            //TODO save date to SQLITE for this circle

            // update the view
            updateView(selected);
        }
    }

    /**********************************************************
     * Activity Finish Handler
     **********************************************************/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            /* User has created an event */
            case Const.CREATE_EVENT_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    long create = data.getLongExtra(getResources().getString(R.string.EXTRA_ITEM_CREATE_TIME), 0L);
                    DateTime createTime = create == 0L ? DateTime.now() : new DateTime(create);
                    String name = data.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_NAME));
                    long start = data.getLongExtra(getResources().getString(R.string.EXTRA_EVENT_START_TIME), 0L);
                    DateTime startTime = start == 0L ? null : new DateTime(start);
                    long end = data.getLongExtra(getResources().getString(R.string.EXTRA_EVENT_END_TIME), 0L);
                    DateTime endTime = end == 0L ? null : new DateTime(end);
                    String placeId = data.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_PLACE_ID));
                    Location location = data.getParcelableExtra(getResources().getString(R.string.EXTRA_EVENT_LOCATION));
                    String note = data.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_NOTE));
                    dataProvider.createEventAsync(appState.getActiveCircle(), createTime, null, name, startTime, endTime,
                            placeId, location, note, true);
                }
                break;

            /* User has updated an event */
            case Const.UPDATE_EVENT_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    String id = data.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_ID));
                    long update = data.getLongExtra(getResources().getString(R.string.EXTRA_ITEM_CREATE_TIME), 0L);
                    DateTime updateTime = update == 0L ? DateTime.now() : new DateTime(update);
                    String name = data.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_NAME));
                    long start = data.getLongExtra(getResources().getString(R.string.EXTRA_EVENT_START_TIME), 0L);
                    DateTime startTime = start == 0L ? null : new DateTime(start);
                    long end = data.getLongExtra(getResources().getString(R.string.EXTRA_EVENT_END_TIME), 0L);
                    DateTime endTime = end == 0L ? null : new DateTime(end);
                    String placeId = data.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_PLACE_ID));
                    Location location = data.getParcelableExtra(getResources().getString(R.string.EXTRA_EVENT_LOCATION));
                    String note = data.getStringExtra(getResources().getString(R.string.EXTRA_EVENT_NOTE));
                    dataProvider.updateEventAsync(appState.getActiveCircle(), updateTime, id, name,
                            startTime, endTime, placeId, location, note, true);
                }
                break;

            /* User has selected image(s) from gallery */
            case Const.IMAGE_SELECTED_RESULT_CODE: {
                if (resultCode == RESULT_OK && data != null) {
                    onFilesSelected(data);
                }
                break;
            }

            /* User has done with the drawing */
            case Const.DRAW_RESULT_CODE: {
                if (resultCode == RESULT_OK && data != null) {
                    String id = data.getStringExtra(getString(R.string.EXTRA_DRAWING_ID));
                    String name = data.getStringExtra(getString(R.string.EXTRA_DRAWING_NAME));
                    String path = data.getStringExtra(getString(R.string.EXTRA_DRAWING_FILE));
                    long time = data.getLongExtra(getString(R.string.EXTRA_DRAWING_TIME), 0L);
                    DateTime updateTime = time == 0L ? DateTime.now() : new DateTime(time);
                    boolean shouldCreateDrawing = data.getBooleanExtra(getString(R.string.EXTRA_DRAWING_MODE), false);

                    if (shouldCreateDrawing) {
                        dataProvider.postDrawingAsync(id, name, path, appState.getActiveCircle(), updateTime, true);
                    }
                    else {
                        dataProvider.updateDrawingAsync(id, name, path, appState.getActiveCircle(), updateTime, true);
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    private void onFilesSelected(Intent data) {
        List<Uri> uriList = new ArrayList<>();
        try {
            if (data.getData() != null) {
                uriList.add(data.getData());
            }
            else {
                // when selecting multiple images, this will be populated
                if (data.getClipData() != null) {
                    ClipData clipData = data.getClipData();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        uriList.add(clipData.getItemAt(i).getUri());
                    }
                }
            }

            // begin creating file board item(s)
            if (uriList.size() > 0) {
                Log.d(TAG, "Selected " + uriList.size() + " file(s)");
                dataProvider.postFilesAsync(uriList, appState.getActiveCircle(), true);
            }
            else {
                Log.d(TAG, "No files selected");
                Toast.makeText(this, getString(R.string.warning_no_image_selected), Toast.LENGTH_LONG).show();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, ex.getMessage());
        }
    }

    /**********************************************************
     * User Grid Click Handler
     **********************************************************/
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        User user = appState.getActiveCircle().getUsers().get(position);
        showRadialMenuOptions(user);
    }

    /**********************************************************
     * CONTEXTUAL ACTION MODE CALLBACKS
     **********************************************************/

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_context_board_item, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        BoardFragment fragment = ((BoardFragment) adapter.getItem(2));
        boolean show = fragment.getSelectedItemCount() > 0;
        menu.findItem(R.id.action_delete).setVisible(show);
        menu.findItem(R.id.action_star).setVisible(show);
        menu.findItem(R.id.action_copy).setVisible(show);
        menu.findItem(R.id.action_share).setVisible(show);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        BoardFragment fragment = ((BoardFragment) adapter.getItem(2));
        switch (item.getItemId()) {
            case R.id.action_delete:
                List<String> ids= fragment.getSelectedItemsId();
                for (String id : ids) {
                    Log.d(TAG, "Attempting to delete item " + id);
                    dataProvider.deleteItemAsync(id, appState.getActiveCircle(), true);
                }
                mode.finish();
                return true;
            default: return true;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;

        BoardFragment fragment = ((BoardFragment) adapter.getItem(2));
        fragment.clearItemSelections();
        fragment.setActionModeEnabled(false);
    }
}
