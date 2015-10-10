package com.abborg.glom.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import com.abborg.glom.model.DataUpdater;
import com.abborg.glom.model.User;
import com.abborg.glom.service.BaseInstanceIDListenerService;
import com.abborg.glom.service.MessageListenerService;
import com.abborg.glom.service.RegistrationIntentService;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DrawerFragment.FragmentDrawerListener {

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

    /* This profile's user */
    private User user;

    /* The currently active circle */
    private Circle currentCircle;

    /* The active list of circles */
    private List<Circle> circles;

    private BroadcastReceiver broadcastReceiver;

    private static final String TAG = "GLOM-HOME-ACTIVITY";

    private SharedPreferences sharedPref;

    /* GSON java-to-JSON converter */
    private Gson gson;

    private ViewPagerAdapter adapter;

    private DrawerFragment drawerFragment;

    /**
     * TODO set default state for DEMO purposes
     */
    private void setAppDefaultState() {
        // create our new user
        user = createUser(Const.TEST_USER_NAME, Const.TEST_USER_ID, Const.TEST_USER_AVATAR, Const.TEST_USER_LAT, Const.TEST_USER_LONG,
                Const.TEST_USER_BROADCAST_LOCATION, Const.TEST_USER_DISCOVERABLE);

        // update all list of this user's circles
        try {
            dataUpdater = new DataUpdater(this, user);
            dataUpdater.open();
            dataUpdater.resetCircles();

            // populate with sample circles
            Circle circle1 = dataUpdater.createCircle(getResources().getString(R.string.default_first_circle_title),
                        new ArrayList<User>(Arrays.asList(
                                createUser("Pro", "phubes", "https://lh3.googleusercontent.com/-6l-FS58CNYY/AAAAAAAAAAI/AAAAAAAAAI8/U-GeqasIr3E/photo.jpg", 1.003, 103.0, false, false),
                                createUser("Mario", "mario", "http://mario.nintendo.com/img/mario_logo.png", 1.15, 101.352, false, false),
                                createUser("Scarlet", "scarlett_johansson", "http://www.biografiasyvidas.com/biografia/j/fotos/johansson_scarlett_2.jpg", 1.1531, 101.161, false, false),
                                createUser("Taylor", "taylor_swift", "http://static.wixstatic.com/media/3a0a07_6fccabc0fb6a4e12b73db71a789aabb3.jpg_256", 1.15134, 101.614, false, false),
                                createUser("Jessica", "jess_alba", "https://38.media.tumblr.com/avatar_4933209feef5_128.png", 1.1345, 101.715, false, false),
                                createUser("Emma Watson", "emma_hermione", "https://31.media.tumblr.com/avatar_098de71d2e8e_128.png", 1.1621, 101.515, false, false)
                        )), "my-circle"
                    );

            Circle circle2 = dataUpdater.createCircle("My Love",
                    new ArrayList<User>(Arrays.asList(
                            createUser("Sunadda", "fatcat18", "http://images8.cpcache.com/image/17244178_155x155_pad.png", 1.0, 102.1441, false ,false)
                    )), "my-love"
            );

            Circle circle3 = dataUpdater.createCircle("Small Room",
                    new ArrayList<User>(), "small-room"
            );

            circles = dataUpdater.getCircles();

            // set default circle to be the first one
            currentCircle = circles.get(0);
        }
        catch (SQLException ex) {
            Toast.makeText(this, "Error accessing database: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private User createUser(String name, String id, String avatar, double latitude, double longitude,
                            boolean isBroadcastingLocation, boolean isDiscoverable) {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        User user = new User(name, id, location);
        user.setAvatar(avatar);
        return user;
    }

    public Circle getCurrentCircle() { return currentCircle; }

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

                // location updates from MessageListenerService
                if ( intent.getAction().equals(getResources().getString(R.string.ACTION_RECEIVE_LOCATION)) ) {
                    String userJson = intent.getStringExtra(getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_USERS));
                    String circleId = intent.getStringExtra(getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_CIRCLE_ID));
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

                                    for (User s : circle.getUsers()) {
                                        if (s.getId().equals(user.getString("id"))) {
                                            s.setLocation(location);
                                            dataUpdater.updateUserLocation(s, circle);
                                        }
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
                                    map.updateUserMarkers(userList);
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

                // location updates from CirclePushService
                else if (intent.getAction().equals(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE))) {
                    Location location = intent.getParcelableExtra(getResources().getString(R.string.EXTRA_USER_LOCATION_UPDATE));
                    user.setLocation(location); //TODO others might not see your location like you do

                    // only update the location markers when the map is visible
                    LocationFragment map = (LocationFragment) adapter.getItem(1);
                    if (map.isFragmentVisible) {
                        map.updateUserMarkers(Arrays.asList(user));
                    }
                }
            }
        };

        // retrieve the shared preferences
        sharedPref = getSharedPreferences(getString(R.string.PREFERENCE_FILE), Context.MODE_PRIVATE);

        gson = new Gson();

        // start IntentService to register this application with GCM
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(getResources().getString(R.string.EXTRA_SEND_TOKEN_USER_ID), user.getId());
        startService(intent);
        startService(new Intent(this, BaseInstanceIDListenerService.class));
        startService(new Intent(this, MessageListenerService.class));
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
    }

    @Override
    protected void onStop() {
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

    @Override
    protected void onPause() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(broadcastReceiver);

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
        Circle selected = getCircleFromTitle(displayTitle);

        if (selected != null) {
            //TODO save date to SQLITE for this circle

            // update the view
            updateView(selected);
            Toast.makeText(this, "Select circle ID: " + selected.getId() + ", title: " + selected.getTitle() +
                    ", users: " + selected.getUsers().size(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateView(Circle circle) {
        this.currentCircle = circle;

        getSupportActionBar().setTitle(currentCircle.getTitle() + " (" + currentCircle.getUsers().size() + ")");

        // only update the locations when the map is visible
        LocationFragment mapFragment = (LocationFragment) adapter.getItem(1);
        mapFragment.updateMap(true);    //TODO this needs to be retrieved from SQLite

        // refresh the circle fragment
        CircleFragment circleFragment = (CircleFragment) adapter.getItem(0);
        circleFragment.update();
    }
}
