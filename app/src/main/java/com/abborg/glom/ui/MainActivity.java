package com.abborg.glom.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.User;
import com.abborg.glom.service.BaseInstanceIDListenerService;
import com.abborg.glom.service.MessageListenerService;
import com.abborg.glom.service.RegistrationIntentService;
import com.abborg.glom.utils.CircleTransform;
import com.bumptech.glide.Glide;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.gson.Gson;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements DrawerFragment.FragmentDrawerListener {

    private Toolbar toolbar;

    private TabLayout tabLayout;

    private ViewPager viewPager;

    public DataUpdater dataUpdater;

    private int[] tabIcons = {
            R.drawable.ic_tab_circle,
            R.drawable.ic_tab_location,
            R.drawable.ic_tab_event,
            R.drawable.ic_tab_discover
    };

    private AppState appState;

    private BroadcastReceiver broadcastReceiver;

    private static final String TAG = "MainActivity";

    /* GSON java-to-JSON converter */
    private Gson gson;

    private ViewPagerAdapter adapter;

    private DrawerFragment drawerFragment;

    private BottomSheetLayout bottomSheet;

    private View broadcastLocationSheetLayout;

    private SwitchCompat broadcastLocationToggle;

    private android.support.design.widget.FloatingActionButton fab;

    FloatingActionMenu avatarActionMenu;

    Animation fadeInAnim;

    Animation fadeOutAnim;

    RelativeLayout overlayLayout;

    ImageView menuOverlay;

    ImageView avatarIcon;

    SubActionButton.Builder lCSubBuilder;

    FrameLayout.LayoutParams blueContentParams;

    /**
     * TODO set default state for DEMO purposes
     */
    private void loadDefaultCircleInfo() {
        // initialize app state
        appState = AppState.getInstance(this);
        appState.init();
        dataUpdater = appState.getDataUpdater();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // It's important to initialize the ResourceZoneInfoProvider; otherwise
        // joda-time-android will not work.
        JodaTimeAndroid.init(this);

        loadDefaultCircleInfo();

        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(appState.getCurrentCircle().getTitle() + " (" + appState.getCurrentCircle().getUsers().size() + ")");
//        getSupportActionBar().setDisplayShowHomeEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();

        // set up the navigation drawer
        drawerFragment = (DrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);
        drawerFragment.setDrawerListener(this);

        // set up the bottom sheets
        bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);
        bottomSheet.setShouldDimContentView(false);
        broadcastLocationSheetLayout = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_broadcast_location, bottomSheet, false);
        broadcastLocationToggle = (SwitchCompat) broadcastLocationSheetLayout.findViewById(R.id.toggleBroadcastLocationSwitch);
        broadcastLocationToggle.setChecked(appState.getCurrentCircle().isUserBroadcastingLocation());

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
        broadcastLocationToggle.setOnClickListener(new CompoundButton.OnClickListener() {

            @Override
            public void onClick(View buttonView) {
                CircleFragment circleFragment = (CircleFragment) adapter.getItem(0);
                if (circleFragment != null) {
                    DateTime now = new DateTime();

                    // if end hour - start hour is negative, add 24 to get the duration from current hour
                    // convert to 24-hour time
                    String amPm = endTimeAMPMPicker.getText().toString();
                    int endHour = Integer.parseInt(endTimePickerHour.getText().toString());
                    int endMinute = Integer.parseInt(endTimePickerMinute.getText().toString());
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
                    circleFragment.toggleBroadcastingLocation(duration);
                }
            }
        });

        // register the local broadcast receiver for our gcm listener service updates
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            //TODO keep track of received message of user and circleId to show appropriate
            //TODO notifications
            //TODO retrive notification counts from USER table
            public void onReceive(Context context, Intent intent) {

                // location updates from MessageListenerService
                // updates from OTHER users
                if ( intent.getAction().equals(getResources().getString(R.string.ACTION_RECEIVE_LOCATION)) ) {
                    List<User> users = dataUpdater.getLocationUpdates(intent, appState.getCurrentCircle());

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
                    String circleId = appState.getCurrentCircle().getId();
                    LocationFragment map = (LocationFragment) adapter.getItem(1);

                    // update the user's location in this circle
                    User currentUser = null;
                    for (User user : appState.getCurrentCircle().getUsers()) {
                        if (user.getId().equals(appState.getUser().getId())) {
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
            }
        };

        // set up FAB
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CircleFragment circleFragment = getCircleFragment();
                LocationFragment mapFragment = getMapFragment();
                EventFragment eventFragment = getEventFragment();
                List<User> users = appState.getCurrentCircle().getUsers();
                User currentUser = null;
                for (User user : users) {
                    if (user.getId().equals(appState.getUser().getId()))
                        currentUser = user;
                }

                if (circleFragment != null && circleFragment.isFragmentVisible) {
                    showMenuOptions(currentUser);
                }
                else if (mapFragment != null && mapFragment.isFragmentVisible) {
                    Toast.makeText(getApplicationContext(), "Map fragment action button clicked", Toast.LENGTH_SHORT).show();
                }
                else if (eventFragment != null && eventFragment.isFragmentVisible) {
                    showMenuOptions(currentUser);
                }
            }
        });

        // initialize the second relative layout for overlay and avatar menu
        overlayLayout = (RelativeLayout) findViewById(R.id.overlayLayout);

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
//        avatarIcon.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                //TODO show user profile activity
//                Log.d(TAG, "Clicked avatar icon");
//            }
//        });

        // add fade-in / fade-out animation when visibilty changes
        fadeInAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
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

        gson = new Gson();

        // start IntentService to register this application with GCM
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(getResources().getString(R.string.EXTRA_SEND_TOKEN_USER_ID), appState.getUser().getId());
        startService(intent);
        startService(new Intent(this, BaseInstanceIDListenerService.class));
        startService(new Intent(this, MessageListenerService.class));
    }

    public void showMenuOptions(User user) {
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
            case User.CREATE_EVENT: icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_planner));
                actionButton = builder.setContentView(icon, params).build();
                final Context context = this;
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideMenuOverlay(false);
                        Intent intent = new Intent(context, CreateEventActivity.class);
                        startActivity(intent);
                    }
                });
                break;
            default: return null;
        }

        return actionButton;
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

    @Override
    protected void onStart() {
        super.onStart();

        // connect to the Google Play API
        appState.connectGoogleApiClient();

        // register local broadcast receiver
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getResources().getString(R.string.ACTION_RECEIVE_LOCATION));
        intentFilter.addAction(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE));
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);

        // get database writable object
        try {
            dataUpdater.open();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }

        Log.d(TAG, "OnStart is called");
    }

    @Override
    protected void onStop() {
        // unregister the local broadcast receiver
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(broadcastReceiver);

        // disconnect google api client
        appState.disconnectGoogleApiClient();

        // close database
        dataUpdater.close();

        super.onStop();
        Log.d(TAG, "OnStop is called");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private void setupTabIcons() {
        tabLayout.getTabAt(0).setIcon(tabIcons[0]);
        tabLayout.getTabAt(1).setIcon(tabIcons[1]);
        tabLayout.getTabAt(2).setIcon(tabIcons[2]);
        tabLayout.getTabAt(3).setIcon(tabIcons[3]);
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new CircleFragment(), "Circle");
        adapter.addFragment(new LocationFragment(), "Map");
        adapter.addFragment(new EventFragment(), "Event");
        adapter.addFragment(new DiscoverFragment(), "Discover");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
//            return mFragmentTitleList.get(position);
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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
            return true;
        }
        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    //TODO load everything from SQLite
    public void onDrawerItemSelected(View view, int position) {
        TextView textView = (TextView) view.findViewById(R.id.circleTitle);
        String displayTitle = textView.getText().toString();
        displayTitle = displayTitle.substring(0, displayTitle.indexOf('(')).trim();
        Circle selected = dataUpdater.getCircleByName(displayTitle);

        if (selected != null) {
            //TODO save date to SQLITE for this circle

            // update the view
            updateView(selected);
            Toast.makeText(this, "Select circle ID: " + selected.getId() + ", title: " + selected.getTitle() +
                    ", users: " + selected.getUsers().size(), Toast.LENGTH_SHORT).show();
        }
    }

    public LocationFragment getMapFragment() {
        return (LocationFragment) adapter.getItem(1);
    }

    public CircleFragment getCircleFragment() { return (CircleFragment) adapter.getItem(0); }

    public EventFragment getEventFragment() { return (EventFragment) adapter.getItem(2); }

    private void updateView(Circle circle) {
        AppState.getInstance(this).setCurrentCircle(circle);

        getSupportActionBar().setTitle(circle.getTitle() + " (" + circle.getUsers().size() + ")");

        // update broadcast location sheet
        broadcastLocationToggle.setChecked(appState.getCurrentCircle().isUserBroadcastingLocation());

        // only update the locations when the map is visible
        LocationFragment mapFragment = (LocationFragment) adapter.getItem(1);
        mapFragment.updateMap(true);    //TODO this needs to be retrieved from SQLite

        // refresh the circle fragment
        CircleFragment circleFragment = (CircleFragment) adapter.getItem(0);
        circleFragment.update();

        // refresh the event lists
        EventFragment eventFragment = (EventFragment) adapter.getItem(2);
        eventFragment.update();
    }
}
