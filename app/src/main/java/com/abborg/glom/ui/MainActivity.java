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
import com.abborg.glom.service.BaseGcmListenerService;
import com.abborg.glom.service.BaseInstanceIDListenerService;
import com.abborg.glom.service.RegistrationIntentService;
import com.abborg.glom.utils.Connection;
import com.google.android.gms.common.api.GoogleApiClient;
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
                                createUser("bot1", "BotTester1", "http://www.adiumxtras.com/images/pictures/portal_2_turret_1_36512_8105_thumb_12583.png", 1.15123, 101.352, false, false),
                                createUser("bot2", "BotTester2", "http://i1.theportalwiki.net/img/thumb/6/6b/Portal2_CompanionCube.png/180px-Portal2_CompanionCube.png", 1.15413, 100.352, false, false),
                                createUser("bot3", "BotTester3", "http://www.globalrobots.com/uploads/images/Safety%20robot.jpg", 1.1565, 101.234, false, false),
                                createUser("Scarlet", "scarlett_johansson", "http://www.biografiasyvidas.com/biografia/j/fotos/johansson_scarlett_2.jpg", 1.1531, 101.161, false, false),
                                createUser("Lara", "crofty8080", "http://mujweb.cz/pavlicd/XNALara/face_smile_thumb.jpg", 1.15413, 101.362, false, false),
                                createUser("Kim", "kimberlim", "http://1.bp.blogspot.com/-YD3tUFLWcHs/UN1E-NSi4II/AAAAAAAAAYI/wpzBlIhlqGU/s1600/IMG_3473.jpg", 1.15232, 101.712, false, false),
                                createUser("RandomGirl", "wildgirl1991", "http://cdnstatic.visualizeus.com/thumbs/e0/d8/inspiration,faces,sunglasses,women,retrato,girl-e0d89711421170a6c57fa599653e58e5_h.jpg", 1.15513, 101.251, false, false),
                                createUser("John", "johnyboy10", "http://a3.files.biography.com/image/upload/c_fill,cs_srgb,dpr_1.0,g_face,h_300,q_80,w_300/MTIwNjA4NjMzNzAzNzI4NjUy.jpg", 1.1521, 101.612, false, false),
                                createUser("Jake", "jakeTheSnake", "http://www2.pictures.zimbio.com/gp/Jake+Gyllenhaal+Taylor+Swift+fling+28FkwSPy8pvl.jpg", 1.15134, 101.624, false, false),
                                createUser("Robert", "RobertSan", "http://s3.stliq.com/c/l/6/60/33334214_robert-downey-jr-anti-eroe-di-avengers-age-of-ultron-4.jpg", 1.1524, 101.342, false, false),
                                createUser("Taylor", "taylor_swift", "http://static.wixstatic.com/media/3a0a07_6fccabc0fb6a4e12b73db71a789aabb3.jpg_256", 1.15134, 101.614, false, false),
                                createUser("Jessica", "jess_alba", "https://38.media.tumblr.com/avatar_4933209feef5_128.png", 1.1345, 101.715, false, false),
                                createUser("TheRealDonaldThump", "therealdonald", "http://www.adiumxtras.com/images/pictures/portal_2_turret_1_36512_8105_thumb_12583.png", 1.1255, 102.455, false, false),
                                createUser("Emma Watson", "emma_hermione", "https://31.media.tumblr.com/avatar_098de71d2e8e_128.png", 1.1621, 101.515, false, false),
                                createUser("Harry Potter", "potter", "http://a2.mzstatic.com/us/r30/Purple/v4/a5/f3/95/a5f395b4-a34f-0feb-4494-e7f381517ae8/icon128-2x.png", 1.1751, 101.516, false, false),
                                createUser("Capt. America", "steve_rogers", "https://static-s.aa-cdn.net/img/ios/808237503/0b3e7167358170f4d4a37eb592a76da7?v=1", 1.1612, 101.293, false, false),
                                createUser("IronMan", "ironman20", "https://lh3.googleusercontent.com/-JP-wab1X11M/AAAAAAAAAAI/AAAAAAAAAAA/sPryRDMTipE/photo.jpg", 1.029, 103.236, false ,false)
                        ))
                    );

            Circle circle2 = dataUpdater.createCircle("My Love",
                    new ArrayList<User>(Arrays.asList(
                            createUser("Sunadda", "fatcat18", "http://images8.cpcache.com/image/17244178_155x155_pad.png", 1.0, 102.1441, false ,false)
                    ))
            );

            Circle circle3 = dataUpdater.createCircle("Small Room",
                    new ArrayList<User>()
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
            dataUpdater.open();
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

        dataUpdater.close();
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
