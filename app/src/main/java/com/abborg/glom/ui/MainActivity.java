package com.abborg.glom.ui;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.location.Location;
import android.os.Bundle;
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
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.abborg.glom.AppState;
import com.abborg.glom.R;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.User;
import com.abborg.glom.service.BaseInstanceIDListenerService;
import com.abborg.glom.service.MessageListenerService;
import com.abborg.glom.service.RegistrationIntentService;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import net.danlew.android.joda.JodaTimeAndroid;

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

    private static final String TAG = "GLOM-HOME-ACTIVITY";

    /* GSON java-to-JSON converter */
    private Gson gson;

    private ViewPagerAdapter adapter;

    private DrawerFragment drawerFragment;

    private BottomSheetLayout bottomSheet;

    private View broadcastLocationSheetLayout;

    private SwitchCompat broadcastLocationToggle;

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

        // set up our drawer
        drawerFragment = (DrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);
        drawerFragment.setDrawerListener(this);

        // set up the bottom sheets
        bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);
        bottomSheet.setShouldDimContentView(false);
        broadcastLocationSheetLayout = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_broadcast_location, bottomSheet, false);
        broadcastLocationToggle = (SwitchCompat) broadcastLocationSheetLayout.findViewById(R.id.toggleBroadcastLocationSwitch);
        final TimePicker intervalPicker = (TimePicker) broadcastLocationSheetLayout.findViewById(R.id.intervalTimePicker);
        intervalPicker.setIs24HourView(true);
        broadcastLocationToggle.setChecked(appState.getCurrentCircle().isUserBroadcastingLocation());
        broadcastLocationToggle.setOnClickListener(new CompoundButton.OnClickListener() {

            @Override
            public void onClick(View buttonView) {
                CircleFragment circleFragment = (CircleFragment) adapter.getItem(0);
                if (circleFragment != null) {
                    circleFragment.toggleBroadcastingLocation();
                }
            }
        });

        // register the broadcast receiver for our gcm listener service updates
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

        gson = new Gson();

        // start IntentService to register this application with GCM
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(getResources().getString(R.string.EXTRA_SEND_TOKEN_USER_ID), appState.getUser().getId());
        startService(intent);
        startService(new Intent(this, BaseInstanceIDListenerService.class));
        startService(new Intent(this, MessageListenerService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        GoogleApiClient apiClient = AppState.getInstance(this).getGoogleApiClient();
        if (apiClient != null && !apiClient.isConnected()) apiClient.connect();
    }

    @Override
    protected void onStop() {
        GoogleApiClient apiClient = AppState.getInstance(this).getGoogleApiClient();
        if (apiClient != null && apiClient.isConnected()) apiClient.disconnect();

        super.onStop();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getResources().getString(R.string.ACTION_RECEIVE_LOCATION));
        intentFilter.addAction(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE));
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);

        try {
            dataUpdater.open();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        super.onResume();
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

    @Override
    protected void onPause() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(broadcastReceiver);

        appState.cleanup();

        dataUpdater.close();

        super.onPause();
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

    public BottomSheetLayout getBottomSheet() {
        return bottomSheet;
    }

    public View getBroadcastLocationSheetLayout() {
        return broadcastLocationSheetLayout;
    }

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
