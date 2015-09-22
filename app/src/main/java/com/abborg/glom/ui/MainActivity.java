package com.abborg.glom.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;
import com.abborg.glom.service.BaseGcmListenerService;
import com.abborg.glom.service.BaseInstanceIDListenerService;
import com.abborg.glom.service.RegistrationIntentService;
import com.abborg.glom.utils.CircleProvider;
import com.abborg.glom.utils.Connection;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DrawerFragment.FragmentDrawerListener {

    private Toolbar toolbar;

    private TabLayout tabLayout;

    private ViewPager viewPager;

    public CircleProvider circleProvider;

    private int[] tabIcons = {
            R.drawable.ic_tab_circle,
            R.drawable.ic_tab_location,
            R.drawable.ic_tab_event,
            R.drawable.ic_tab_discover
    };

    /* This profile's user */
    private User user;

    /* The currently active circle */
    private Circle currentCircle;

    /* The active list of circles */
    private List<Circle> circles;

    private BroadcastReceiver broadcastReceiver;

    private static final String TAG = "GLOM-HOME-ACTIVITY";

    private SharedPreferences sharedPref;

    /* Google Play API client */
    private GoogleApiClient apiClient;

    /* GSON java-to-JSON converter */
    private Gson gson;

    private ViewPagerAdapter adapter;

    private DrawerFragment drawerFragment;

    /**
     * TODO set default state for DEMO purposes
     */
    private void setAppDefaultState() {
        // create our new user
        user = createUser(Const.TEST_USER_NAME, Const.TEST_USER_ID, Const.TEST_USER_LAT, Const.TEST_USER_LONG,
                Const.TEST_USER_BROADCAST_LOCATION, Const.TEST_USER_DISCOVERABLE);

        // update all list of this user's circles
        try {
            circleProvider = new CircleProvider(this, user);
            circleProvider.open();

            circleProvider.resetCircles();

            // populate with sample circles
            Circle circle1 = circleProvider.createCircle(getResources().getString(R.string.default_first_circle_title), user,
                        new ArrayList<User>(Arrays.asList(
                                new User("TestName1", "TestId1", new Location("")),
                                new User("TestName2", "TestId2", new Location("")),
                                new User("Sunadda", "fatcat18", new Location(""))
                        ))
                    );

            Circle circle2 = circleProvider.createCircle("My Love", user,
                    new ArrayList<User>(Arrays.asList(
                            new User("Sunadda", "fatcat18", new Location(""))
                    ))
            );

            circles = circleProvider.getCircles();

            // set default circle to be the first one
            currentCircle = circles.get(0);
        }
        catch (SQLException ex) {
            Toast.makeText(this, "Error accessing database: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private User createUser(String name, String id, double latitude, double longitude,
                            boolean isBroadcastingLocation, boolean isDiscoverable) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        User user = new User(name, id, location);
        user.setBroadcastingLocation(isBroadcastingLocation);
        user.setDiscoverable(isDiscoverable);
        return user;
    }

    public Circle getCurrentCircle() { return currentCircle; }

    //TODO has to load from SQLITE
    public List<Circle> getCircles() { return circles; }

    public Circle getCircleFromTitle(String title) {
        if (circles != null) {
            for (Circle c : circles) {
                if (c.getTitle().equalsIgnoreCase(title)) return c;
            }
        }

        return null;
    }

    public User getUser() {
        return user;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setAppDefaultState();

        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(currentCircle.getTitle() + " (" + currentCircle.getUsers().size() + ")");
//        getSupportActionBar().setDisplayShowHomeEnabled(true);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        setupTabIcons();

        drawerFragment = (DrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);
        drawerFragment.setDrawerListener(this);

        // register the broadcast receiver for our gcm listener service updates
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ( intent.getAction().equals(getResources().getString(R.string.gcm_location_update_intent_key)) ) {
                    String userJson = intent.getStringExtra(getResources().getString(R.string.gcm_location_update_intent_extra_users));
                    String circleId = intent.getStringExtra(getResources().getString(R.string.gcm_location_update_intent_extra_circleId));
                    Circle circle = findCircle(circleId);

                    if (circle != null) {
                        try {
                            // update the location markers
                            JSONArray users = new JSONArray(userJson);
                            List<User> userList = new ArrayList<User>();

                            for (int i = 0; i < users.length(); i++) {
                                JSONObject user = users.getJSONObject(i);

                                // verify that the user is in a circle and the ID is valid
                                if (Circle.circleContainsUserId(user.getString("id"), circle)) {
                                    JSONObject locationJson = user.getJSONObject("location");
                                    Location location = new Location("");
                                    location.setLatitude(locationJson.getDouble("lat"));
                                    location.setLongitude(locationJson.getDouble("long"));
                                    userList.add(new User(null, user.getString("id"), location));

                                    //TODO update the user's location to SQLITE if not current circle
                                    // otherwise update now
                                    if (circle.getId().equals(currentCircle.getId())) {
                                        for (User s : circle.getUsers()) {
                                            if (s.getId().equals(user.getString("id"))) {
                                                s.setLocation(location);
                                            }
                                        }
                                    }
                                    else {

                                    }

                                    Log.i(TAG, "User ID: " + user.getString("id") + "\nLat: " + user.getJSONObject("location").getDouble("lat") + "\nLong: " +
                                            user.getJSONObject("location").getDouble("long"));
                                }
                                else {
                                    Log.e(TAG, "Received user ID of " + user.getString("id") + " does not exist in circle (" + circleId + ")");
                                    Toast.makeText(context, "Received user ID of " + user.getString("id") + " does not exist in circle (" + circleId + ")", Toast.LENGTH_SHORT).show();
                                }
                            }

                            if (userList.size() > 0) {

                                // only update the location markers when the map is visible and this is the current circle
                                LocationFragment map = (LocationFragment) adapter.getItem(1);
                                if (map.isFragmentVisible && circle.getId().equals(currentCircle.getId())) {
                                    map.updateUI(userList);
                                }

                                Toast.makeText(context, userList.get(0).getId() + ": " + userList.get(0).getLocation().getLatitude()
                                        + ", " + userList.get(0).getLocation().getLongitude(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        catch (JSONException ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                    }
                    else {
                        Log.e(TAG, "Could not find circle (" + circleId + ") under this user!");
                        Toast.makeText(context, "Could not find circle (" + circleId + ") under this user!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };

        // retrieve the shared preferences
        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        if ( Connection.getInstance(this).verifyGooglePlayServices(this) ) {
            apiClient = Connection.getInstance(this).getApiClient();

            gson = new Gson();

            // start IntentService to register this application with GCM
            startService(new Intent(this, RegistrationIntentService.class));
            startService(new Intent(this, BaseInstanceIDListenerService.class));
            startService(new Intent(this, BaseGcmListenerService.class));
        }
        else {
            finish();
        }
    }

    /**
     * Check if the specified circle ID exists in this user's context
     *
     * @param circleId
     * @return
     */
    private Circle findCircle(String circleId) {
        for (Circle c : circles) {
            if (c.getId().equals(circleId)) return c;
        }
        return null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!apiClient.isConnected()) apiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(apiClient.isConnected()) {
            apiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getResources().getString(R.string.gcm_location_update_intent_key));
        broadcastManager.registerReceiver(broadcastReceiver, intentFilter);

        try {
            circleProvider.open();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        super.onResume();
        if (!apiClient.isConnected()) apiClient.connect();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(broadcastReceiver);

        circleProvider.close();
        super.onPause();
        if (apiClient.isConnected()) {
            apiClient.disconnect();
        }
    }

    public GoogleApiClient getApiClient() {
        return apiClient;
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
        if (id == R.id.action_settings) {
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
        Circle selected = getCircleFromTitle(displayTitle);

        if (selected != null) {
            //TODO save date to SQLITE for this circle

            // update the view
            updateView(selected);
            Toast.makeText(this, "Select circle ID: " + selected.getId() + ", title: " + selected.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateView(Circle circle) {
        this.currentCircle = circle;

        getSupportActionBar().setTitle(currentCircle.getTitle() + " (" + currentCircle.getUsers().size() + ")");

        // only update the locations when the map is visible
        LocationFragment map = (LocationFragment) adapter.getItem(1);
        map.updateMap(true);    //TODO this needs to be retrieved from SQLite
    }
}
