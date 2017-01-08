package com.abborg.glom.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.adapters.ViewPagerAdapter;
import com.abborg.glom.fragments.BoardFragment;
import com.abborg.glom.fragments.CircleFragment;
import com.abborg.glom.fragments.DrawerFragment;
import com.abborg.glom.fragments.ExploreFragment;
import com.abborg.glom.fragments.LocationFragment;
import com.abborg.glom.interfaces.ActionModeCallbacks;
import com.abborg.glom.interfaces.BoardItemChangeListener;
import com.abborg.glom.interfaces.CategoryBarListener;
import com.abborg.glom.interfaces.CircleChangeListener;
import com.abborg.glom.interfaces.CircleListListener;
import com.abborg.glom.interfaces.CircleMenuListener;
import com.abborg.glom.interfaces.ExploreItemChangeListener;
import com.abborg.glom.interfaces.MainActivityCallbacks;
import com.abborg.glom.interfaces.UsersChangeListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.Category;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.model.CloudProvider;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.ExploreItem;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.model.LinkItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.model.MenuActionItem;
import com.abborg.glom.model.NavMenuItem;
import com.abborg.glom.model.NoteItem;
import com.abborg.glom.model.User;
import com.abborg.glom.service.RegistrationIntentService;
import com.abborg.glom.utils.CircleTransform;
import com.abborg.glom.utils.TaskUtils;
import com.abborg.glom.utils.ViewUtils;
import com.abborg.glom.views.CircleMenu;
import com.bumptech.glide.Glide;
import com.google.android.gms.common.GoogleApiAvailability;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * This is the default entry point launching activity that contains all the tabs to display
 * information to the user
 *
 * @author jitrapon
 */
@SuppressWarnings("ConstantConditions")
public class MainActivity extends BaseActivity implements
        DrawerFragment.NavMenuClickListener,
        Handler.Callback,
        ActionMode.Callback,
        CircleMenuListener,
        MainActivityCallbacks {

    protected static final String TAG = "MainActivity";

    private BroadcastReceiver localBroadcastReceiver;
    private BroadcastReceiver globalBroadcastReceiver;

    private ViewPagerAdapter adapter;

    private ActionMode actionMode;

    private Handler handler;

    // Callbacks
    private List<CircleListListener> circleListListeners;
    private List<UsersChangeListener> usersChangeListeners;
    private List<CircleChangeListener> circleChangeListeners;
    private List<BoardItemChangeListener> boardItemChangeListeners;
    private List<ExploreItemChangeListener> exploreItemChangeListeners;
    private ActionModeCallbacks actionModeCallbacks;
    private CategoryBarListener categoryBarListener;

    // UI elements
    private CircleMenu circleMenu;
    private RelativeLayout circleMenuLayout;
    private DrawerFragment drawerFragment;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private ImageView toolbarNavIcon;
    private TextView toolbarTitle;
    private TextView toolbarSubtitle;
    private ImageView toolbarMenuButton1;
    private ImageView toolbarMenuButton2;
    private FloatingActionButton fab;
    private View notificationBar;
    private TextView notificationText;
    private boolean firstLaunch;
    private CoordinatorLayout mainCoordinatorLayout;
    private Snackbar snackbar;
    private RelativeLayout tagBar;
    private RelativeLayout categoryBar;
    private RecyclerView tagRecyclerView;
    private RecyclerView categoryRecyclerView;

    /**********************************************************
     * VIEW INITIALIZATIONS
     **********************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firstLaunch = true;

        // set up handler for receiving all messages
        handler = new Handler(this);

        setupView();

        setupBroadcastReceiver();

        // begin loading and fetching data
        dataProvider.setHandler(handler);
        dataProvider.openDB();

        updateView();

        setupCallbackListeners();

        setupService();

        if (appState.getConnectionStatus() == ApplicationState.ConnectivityStatus.DISCONNECTED) {
            handler.sendEmptyMessage(Const.MSG_SERVER_DISCONNECTED);
        }
        else {
            dataProvider.requestGetCirclesInfo();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register local broadcast receiver
        registerBroadcastReceivers();

        appState.setKeepGoogleApiClientAlive(false);
    }

    @Override
    protected void onStop() {
        // unregister the broadcast receivers
        unregisterBroadcastReceivers();

        // closeDB database and cancells all network operations
        dataProvider.cancelAllNetworkRequests();
        dataProvider.closeDB();

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dataProvider != null) dataProvider.setHandler(handler);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @SuppressLint("InflateParams")
    private void setupView() {
        setContentView(R.layout.activity_main);

        mainCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbarNavIcon = (ImageView) findViewById(R.id.nav_icon);
        toolbarTitle = (TextView) findViewById(R.id.title);
        toolbarSubtitle = (TextView) findViewById(R.id.subtitle);
        toolbarMenuButton1 = (ImageView) findViewById(R.id.menu_icon_1);
        toolbarMenuButton2 = (ImageView) findViewById(R.id.menu_icon_2);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        circleMenuLayout = (RelativeLayout) findViewById(R.id.circle_menu_layout);
        tagBar = (RelativeLayout) findViewById(R.id.tag_bar);
        tagRecyclerView = (RecyclerView) findViewById(R.id.tag_recycler_view);
        categoryBar = (RelativeLayout) findViewById(R.id.category_bar);
        categoryRecyclerView = (RecyclerView) findViewById(R.id.category_recycler_view);

        setSupportActionBar(toolbar);

        fab.hide();
    }

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new BoardFragment(), Const.TAB_BOARD);
        adapter.addFragment(new LocationFragment(), Const.TAB_MAP);
        adapter.addFragment(new ExploreFragment(), Const.TAB_EXPLORE);
        adapter.addFragment(new CircleFragment(), Const.TAB_CIRCLE);
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_tab_event).setTag(Const.TAB_BOARD);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_tab_location).setTag(Const.TAB_MAP);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_tab_discover).setTag(Const.TAB_EXPLORE);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_tab_circle).setTag(Const.TAB_CIRCLE);
        tabLayout.setOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        if (Const.TAB_BOARD.equals(tab.getTag())) {
                            if (actionMode != null) actionMode.finish();
                        }
                    }
                });

        final int translationY = getResources().getDimensionPixelSize(R.dimen.bottom_bar_height);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // show or hide tag bar
                if (position != adapter.getItemIndex(Const.TAB_BOARD)) {
                    if (((int) tagBar.getTag()) == View.VISIBLE) {
                        ViewUtils.animateTranslateY(tagBar, 0, translationY, 200);
                        tagBar.setTag(View.INVISIBLE);
                    }
                }
                else {
                    ViewUtils.animateTranslateY(tagBar, translationY, 0, 200);
                    tagBar.setTag(View.VISIBLE);
                }

                // show or hide category bar
                if (position != adapter.getItemIndex(Const.TAB_EXPLORE)) {
                    if (((int) categoryBar.getTag()) == View.VISIBLE) {
                        ViewUtils.animateTranslateY(categoryBar, 0, translationY, 200);
                        categoryBar.setTag(View.INVISIBLE);
                    }
                }
                else {
                    if (categoryBarListener != null) {
                        if (categoryBarListener.shouldShowCategoryBar()) {
                            ViewUtils.animateTranslateY(categoryBar, translationY, 0, 200);
                            categoryBar.setTag(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void updateView() {
        super.setupBroadcastLocationSheet();

        // set up tabs
        setupViewPager(viewPager);

        // set up the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        onToolbarNavIconChanged(appState.getActiveCircle().getAvatar());
        toolbarMenuButton1.setVisibility(View.GONE);
        toolbarMenuButton2.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_action_chat_2));
        toolbarMenuButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                startActivity(intent);
            }
        });

        // set up the navigation drawer
        drawerFragment = (DrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_drawer);
        drawerFragment.setupView(R.id.fragment_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar,
                R.id.nav_icon);
        drawerFragment.setDrawerListener(this);

        // set up circular menu
        circleMenu = CircleMenu.init()
                .setMenuItems(dataProvider.getFavoriteBoardItemActions())
                .setHandler(handler)
                .setMenuOptionsClickedListener(this)
                .setActivity(this)
                .setCenterImageSource(appState.getActiveUser().getAvatar())
                .setLayout(circleMenuLayout)
                .setRadiusSize(CircleMenu.Size.LARGE)
                .setStartEndAngle(0, 360)
                .create();

        // set up the floating action button for all fragments
        fab.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (adapter.getItem(viewPager.getCurrentItem()) instanceof BoardFragment) {
                    if (circleMenu.isOpened()) {
                        circleMenu.close(true);
                    }
                    else {
                        circleMenu.open(true);
                    }
                }
            }
        });

        // set up bar
        tagBar.setTag(View.VISIBLE);                //FIXME whether or not to set visible depends on preference's last opened tab
        categoryBar.setTag(View.INVISIBLE);         // FIXME whether or not to set visible depends on preference's last opened tab
        categoryBar.setTranslationY(getResources().getDimensionPixelSize(R.dimen.bottom_bar_height));
        ViewCompat.setElevation(tagBar, ViewUtils.convertDpToPx(this, 40));
        ViewCompat.setElevation(categoryBar, ViewUtils.convertDpToPx(this, 40));

        // set up fab margin
        CoordinatorLayout.LayoutParams fabParams = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        fabParams.setMargins(fabParams.leftMargin,
                fabParams.topMargin,
                fabParams.rightMargin,
                fabParams.bottomMargin + getResources().getDimensionPixelSize(R.dimen.bottom_bar_height));
        fab.setLayoutParams(fabParams);

        // setup snackbar bottom margin
        snackbar = Snackbar.make(mainCoordinatorLayout, "", Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarView.getLayoutParams();
        params.setMargins(params.leftMargin,
                params.topMargin,
                params.rightMargin,
                params.bottomMargin + getResources().getDimensionPixelSize(R.dimen.bottom_bar_height));
        snackbarView.setLayoutParams(params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (circleMenu != null) {
            circleMenu.destroy();
        }
    }

    /**************************************************
     * Broadcast Receivers
     **************************************************/

    private void setupBroadcastReceiver() {

        // setup the local broadcast receiver
        localBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                // location updates from MessageService
                // updates from OTHER users
                if (intent.getAction().equals(getResources().getString(R.string.ACTION_RECEIVE_LOCATION))) {
                    String userJsonString = intent.getStringExtra(context.getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_USERS));
                    String circleId = intent.getStringExtra(context.getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_CIRCLE_ID));

                    try {
                        Circle circle = appState.getActiveCircle();
                        if (!circleId.equals(circle.getId())) return;

                        JSONArray userJsonArray = new JSONArray(userJsonString);

                        for (int i = 0; i < userJsonArray.length(); i++) {
                            JSONObject userJson = userJsonArray.getJSONObject(i);
                            String userId = userJson.getString(Const.JSON_SERVER_USERID);
                            JSONObject locationJson = userJson.getJSONObject(Const.JSON_SERVER_LOCATION);
                            double lat = locationJson.getDouble(Const.JSON_SERVER_LOCATION_LAT);
                            double lng = locationJson.getDouble(Const.JSON_SERVER_LOCATION_LONG);

                            // verify that each user in the JSON belongs to this circle
                            // don't update if it's the user's own location
                            for (User user : circle.getUsers()) {
                                if (userId.equals(appState.getActiveUser().getId())) {
                                    Log.d(TAG, "Skipping updating user's own location");
                                    break;
                                }
                                else if (userId.equals(user.getId())) {
                                    Location location = new Location("");
                                    location.setLatitude(lat);
                                    location.setLongitude(lng);
                                    user.setLocation(location);
                                    Log.d(TAG, "Updated user " + userId + " to new location of " + location);
                                }
                            }
                        }

                        if (usersChangeListeners != null) {
                            for (UsersChangeListener listener : usersChangeListeners) {
                                listener.onUsersChanged();
                            }
                        }

                        Toast.makeText(context, "Received location update from server", Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception ex) {
                        Log.e(TAG, ex.getMessage());
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

                    // update the user's location in this circle
                    for (User user : appState.getActiveCircle().getUsers()) {
                        if (user.getId().equals(appState.getActiveUser().getId())) {
                            user.setLocation(location);
                            Log.d(TAG, "Updated current user location to be " + location);
                            break;
                        }
                    }

                    if (circleBroadcastList.contains(circleId)) {
                        if (usersChangeListeners != null) {
                            for (UsersChangeListener listener : usersChangeListeners) {
                                listener.onUsersChanged();
                            }
                        }
                    }
                }

                // incoming message
                //TODO
                else if (intent.getAction().equals(getResources().getString(R.string.ACTION_NEW_MESSAGE))) {

                }
            }
        };

        // set up the global broadcast receivers to receive OS broadcasts
        globalBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // device connectivity state change as broadcasted by the OS
                if (intent.getAction().equals(getResources().getString(R.string.ACTION_CONNECTIVITY_STATE_CHANGE))) {
                    Log.d(TAG, "Incoming network connectivity change broadcast");

                    if (!appState.isNetworkAvailable()) {
                        appState.setConnectivityStatus(ApplicationState.ConnectivityStatus.DISCONNECTED);
                        handler.sendEmptyMessage(Const.MSG_SERVER_DISCONNECTED);
                    }
                    else {
                        if (!firstLaunch && appState.getConnectionStatus() != ApplicationState.ConnectivityStatus.CONNECTED) {
                            appState.setConnectivityStatus(ApplicationState.ConnectivityStatus.CONNECTING);
                            handler.sendEmptyMessage(Const.MSG_SERVER_CONNECTING);
                        }
                    }
                    firstLaunch = false;
                }
            }
        };
    }

    private void registerBroadcastReceivers() {
        if (localBroadcastReceiver != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(getResources().getString(R.string.ACTION_RECEIVE_LOCATION));
            intentFilter.addAction(getResources().getString(R.string.ACTION_USER_LOCATION_UPDATE));
            intentFilter.addAction(getResources().getString(R.string.ACTION_NEW_MESSAGE));
            LocalBroadcastManager.getInstance(this).registerReceiver(localBroadcastReceiver, intentFilter);
        }

        if (globalBroadcastReceiver != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(getResources().getString(R.string.ACTION_CONNECTIVITY_STATE_CHANGE));
            registerReceiver(globalBroadcastReceiver, intentFilter);
        }
    }

    private void unregisterBroadcastReceivers() {
        if (localBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceiver);
        }

        if (globalBroadcastReceiver != null) {
            unregisterReceiver(globalBroadcastReceiver);
        }
    }

    /**************************************************
     * Service
     **************************************************/

    private void setupService() {
        // start IntentService to register this application with GCM
        Intent intent = new Intent(this, RegistrationIntentService.class);
        intent.putExtra(getResources().getString(R.string.EXTRA_SEND_TOKEN_USER_ID), appState.getActiveUser().getId());
        startService(intent);
    }

    /**************************************************
     * Event Callbacks
     **************************************************/

    private void setupCallbackListeners() {
        CircleFragment circleFragment = (CircleFragment) adapter.getItem(Const.TAB_CIRCLE);
        BoardFragment boardFragment = (BoardFragment) adapter.getItem(Const.TAB_BOARD);
        LocationFragment mapFragment = (LocationFragment) adapter.getItem(Const.TAB_MAP);
        ExploreFragment exploreFragment = (ExploreFragment) adapter.getItem(Const.TAB_EXPLORE);

        addCircleListListener(drawerFragment);

        addUsersChangeListener(circleFragment);
        addUsersChangeListener(mapFragment);

        addCircleChangeListener(circleFragment);
        addCircleChangeListener(mapFragment);
        addCircleChangeListener(boardFragment);

        addItemChangeListener(mapFragment);
        addItemChangeListener(boardFragment);

        addBroadcastLocationListener(circleFragment);
        addBroadcastLocationListener(mapFragment);

        addExploreItemChangeListener(exploreFragment);

        addCategoryBarListener(exploreFragment);

        actionModeCallbacks = boardFragment;
    }

    private void addCircleListListener(CircleListListener listener) {
        if (circleListListeners == null) {
            circleListListeners = new ArrayList<>();
        }
        circleListListeners.add(listener);
    }

    private void addUsersChangeListener(UsersChangeListener listener) {
        if (usersChangeListeners == null) {
            usersChangeListeners = new ArrayList<>();
        }
        usersChangeListeners.add(listener);
    }

    private void addCircleChangeListener(CircleChangeListener listener) {
        if (circleChangeListeners == null) {
            circleChangeListeners = new ArrayList<>();
        }
        circleChangeListeners.add(listener);
    }

    private void addItemChangeListener(BoardItemChangeListener listener) {
        if (boardItemChangeListeners == null) {
            boardItemChangeListeners = new ArrayList<>();
        }
        boardItemChangeListeners.add(listener);
    }

    private void addExploreItemChangeListener(ExploreItemChangeListener listener) {
        if (exploreItemChangeListeners == null) {
            exploreItemChangeListeners = new ArrayList<>();
        }
        exploreItemChangeListeners.add(listener);
    }

    private void addCategoryBarListener(CategoryBarListener listener) {
        categoryBarListener = listener;
    }

    /**************************************************
     * Circular Menu
     **************************************************/

    @Override
    public void onCircleMenuOptionsClicked(MenuActionItem item) {
        handleMenuActionItem(item);
    }

    @Override
    public void onOtherCircleMenuOptionClicked() {
        showBoardItemBottomSheet();
    }

    @Override
    public void onCircleMenuOptionsOpening() {
        ViewCompat.animate(fab)
                .rotation(45f)
                .withLayer()
                .setDuration(300L)
                .start();
    }

    @Override
    public void onCircleMenuOptionsClosing() {
        ViewCompat.animate(fab)
                .rotation(0f)
                .withLayer()
                .setDuration(300L)
                .start();
    }

    /**********************************************************
     * Handler Callbacks
     **********************************************************/

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            /* On circle list populated */
            case Const.MSG_GET_CIRCLES: {
                List<CircleInfo> circles = (List<CircleInfo>) msg.obj;
                appState.setCircleList(circles);
                Log.d(TAG, "AppState circle list size: " + appState.getCircleList().size());
                if (circles.isEmpty()) {
                    //TODO show screen that user is not in any circles
                }
                else {
                    for (CircleInfo circle : circles) {
                        if (circle.id.equals(appState.getActiveCircle().getId())) {
                            Circle activeCircle = appState.getActiveCircle();
                            activeCircle.setTitle(circle.name);
                            activeCircle.setAvatar(circle.avatar);
                            activeCircle.setInfo(circle.info);

                            onToolbarNavIconChanged(circle.avatar);
                            onToolbarTitleChanged(circle.name);

                            break;
                        }
                    }

                    if (circleListListeners != null) {
                        for (CircleListListener listener : circleListListeners) {
                            listener.onCircleListChanged();
                        }
                    }

                    dataProvider.requestGetUsersInCircle(appState.getActiveCircle());
                }

                break;
            }

            /* No Google Play Services available  */
            case Const.MSG_GOOGLE_PLAY_SERVICES_UNAVAILABLE: {
                int resultCode = msg.arg1;
                GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
                if (apiAvailability.isUserResolvableError(resultCode)) {
                    apiAvailability.showErrorDialogFragment(this, resultCode,
                            Const.GOOGLE_PLAY_SERVICES_REQUEST_CODE);
                }
                else {
                    new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                            .setMessage(getString(R.string.dialog_google_play_services_message))
                            .setPositiveButton(R.string.dialog_google_play_services_ok,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    android.os.Process.killProcess(android.os.Process.myPid());
                                }
                            })
                            .setCancelable(false)
                            .show();
                }

                break;
            }

            /* Get Circle */
            case Const.MSG_GET_CIRCLE: {
                Circle circle = (Circle) msg.obj;

                if (circle != null) {
                    Log.d(TAG, "Done retrieving circle info with id: " + circle.getId() + ", name: " + circle.getTitle()
                            + ", " + circle.getUsers().size() + " users, " + circle.getItems().size() + " items");
                    appState.setActiveCircle(circle);
                    invalidateViews();

                    // refresh users
                    dataProvider.requestGetUsersInCircle(appState.getActiveCircle());
                }

                break;
            }

            /* Show toast message */
            case Const.MSG_SHOW_TOAST: {
                String message = msg.obj == null ? null : (String) msg.obj;

                if (!TextUtils.isEmpty(message)) {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }

                break;
            }

            /* Diconnected from server */
            case Const.MSG_SERVER_DISCONNECTED: {
                Log.d(TAG, "Disconnected from server due to connection problem or server not running");

                if (dataProvider != null) {
                    dataProvider.cancelAllNetworkRequests();
                }

                onShowNotificationBar(ContextCompat.getColor(getApplicationContext(), R.color.notificationWarningBackground),
                        getResources().getString(R.string.notification_offline), -1L);

                break;
            }

            /* Connecting to server */
            case Const.MSG_SERVER_CONNECTING: {
                Log.d(TAG, "Attempting to establish connection to server...");

                if (dataProvider != null) {
                    dataProvider.requestServerStatus();
                }

                onShowNotificationBar(ContextCompat.getColor(getApplicationContext(), R.color.notificationWarningBackground),
                        getResources().getString(R.string.notification_connecting), -1L);

                break;
            }

            /* Connected to server */
            case Const.MSG_SERVER_CONNECTED: {
                Log.d(TAG, "Connection established to server successfully!");

                onShowNotificationBar(ContextCompat.getColor(getApplicationContext(), R.color.notificationSuccessBackground),
                        getResources().getString(R.string.notification_connected), 3000);

                break;
            }

            /* Request: get list of users in circle */
            case Const.MSG_GET_USERS:
                if (usersChangeListeners != null) {
                    for (UsersChangeListener listener : usersChangeListeners) {
                        listener.onUsersChanged();
                    }
                }

                break;

            /* Request: get board item in a circle */
            case Const.MSG_GET_ITEMS:
                if (boardItemChangeListeners != null) {
                    for (BoardItemChangeListener listener : boardItemChangeListeners) {
                        listener.onItemsChanged();
                    }
                }

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


                snackbar.setText(getResources().getString(R.string.notification_created_item_failed))
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

                snackbar.setText(getResources().getString(R.string.notification_updated_item_failed))
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
                    appState.getActiveCircle().removeItem(item);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemDeleted(item.getId());
                        }
                    }

                    snackbar.setText(getResources().getQuantityString(R.plurals.notification_delete_item, 1, 1))
                            .setAction(getResources().getString(R.string.menu_item_undo), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    //TODO
                                }
                            }).show();

                    if (item.getType() == BoardItem.TYPE_DRAWING) {
                        refreshSystemGallery(((DrawItem) item).getLocalFile());
                    }
                }

                break;
            }

            /* Request: delete board item failed */
            case Const.MSG_ITEM_DELETED_FAILED: {
                final BoardItem item = msg.obj==null ? null : (BoardItem) msg.obj;

                snackbar.setText(getResources().getString(R.string.notification_delete_item_failed))
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

            /* Request: explore category received */
            case Const.MSG_EXPLORE_CATEGORIES: {
                List<Category> results = msg.obj != null ? (List<Category>) msg.obj : null;
                if (categoryBarListener != null) {
                    categoryBarListener.onCategoryBarRequireUpdate(results, categoryRecyclerView);
                }

                break;
            }

            /* Request: explore items received */
            case Const.MSG_EXPLORE_ITEMS: {
                int type = msg.arg1;
                List<ExploreItem> results = msg.obj != null ? (List<ExploreItem>) msg.obj : null;

                if (exploreItemChangeListeners != null) {
                    for (ExploreItemChangeListener listener : exploreItemChangeListeners) {
                        listener.onItemsReceived(type, results);
                    }
                }

                break;
            }

            /* Play Youtube video */
            case Const.MSG_PLAY_YOUTUBE_VIDEO: {
                String videoId = (String) msg.obj;
                playYoutubeVideo(videoId);

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

                refreshSystemGallery(file.getLocalFile());

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

                snackbar.setText(getResources().getString(R.string.notification_created_item_failed))
                        .show();
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

                    openFileViewer(item.getLocalFile(), item.getMimetype());

                    Toast.makeText(getApplicationContext(),
                            String.format(getResources().getString(R.string.notification_download_item_success), item.getName()),
                            Toast.LENGTH_LONG).show();
                }

                break;
            }

            case Const.MSG_VIEW_FILE: {
                final FileItem item = msg.obj == null ? null : (FileItem) msg.obj;

                if (item != null) {
                    openFileViewer(item.getLocalFile(), item.getMimetype());
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

                    refreshSystemGallery(item.getLocalFile());
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
                int progress = msg.arg2;

                if (item != null) {
                    item.setSyncStatus(status);
                    item.setProgress(progress);
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

                snackbar.setText(getResources().getString(R.string.notification_updated_item_failed))
                        .show();
                break;
            }

            /* Starting of ACTION MODE */
            case Const.MSG_START_ACTION_MODE: {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(this);
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

            /* Begin downloading draw item */
            case Const.MSG_DOWNLOAD_DRAWING: {
                final DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;

                if (item != null) {
                    dataProvider.requestDownloadDrawingRemote(appState.getActiveCircle(), item, CloudProvider.AMAZON_S3);
                }

                break;
            }

            /* Drawing download complete */
            case Const.MSG_DRAWING_DOWNLOAD_COMPLETE: {
                final DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;

                if (item != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }

                    String path = (item.getLocalFile() == null) ? null :
                            new File(item.getLocalFile().getPath()).exists() ? item.getLocalFile().getPath() : null;
                    Intent intent = new Intent(this, DrawActivity.class);
                    intent.setAction(getString(R.string.ACTION_JOIN_DRAWING));
                    intent.putExtra(getString(R.string.EXTRA_DRAWING_ID), item.getId());
                    intent.putExtra(getString(R.string.EXTRA_DRAWING_PATH), path);
                    startActivityForResult(intent, Const.DRAW_RESULT_CODE);
                }

                break;
            }

            /* Drawing download failed */
            case Const.MSG_DRAWING_DOWNLOAD_FAILED: {
                final DrawItem item = msg.obj == null ? null : (DrawItem) msg.obj;

                if (item != null) {
                    String name = TextUtils.isEmpty(item.getName()) ? "drawing" : item.getName();
                    Toast.makeText(getApplicationContext(),
                            String.format(getResources().getString(R.string.notification_download_item_failed), name),
                            Toast.LENGTH_LONG).show();
                }

                break;
            }

            /* Opening a link */
            case Const.MSG_OPEN_LINK: {
                String url = (String) msg.obj;
                launchThirdPartyUrlApp(url);

                break;
            }

            /* Creating a link */
            case Const.MSG_LINK_CREATED: {
                final LinkItem item = msg.obj == null ? null : (LinkItem) msg.obj;

                if (item != null) {
                    appState.getActiveCircle().addItem(item);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemAdded(item.getId());
                        }
                    }
                }

                break;
            }

            /* Synced creating link successfully */
            case Const.MSG_LINK_CREATED_SUCCESS: {
                LinkItem item = msg.obj == null ? null : (LinkItem) msg.obj;
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

            /* Editing a link */
            case Const.MSG_EDIT_LINK: {
                final LinkItem item = msg.obj == null ? null : (LinkItem) msg.obj;

                if (item != null) {
                    showLinkDialog(item);
                }

                break;
            }

            /* Link edited */
            case Const.MSG_LINK_UPDATED: {
                final LinkItem item = msg.obj == null ? null : (LinkItem) msg.obj;

                if (item != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                break;
            }

            /* Link update sync success */
            case Const.MSG_LINK_UPDATED_SUCCESS: {
                LinkItem item = msg.obj == null ? null : (LinkItem) msg.obj;
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

            /* Request: update link failed to sync with server */
            case Const.MSG_LINK_UPDATED_FAILED: {
                final LinkItem item = msg.obj == null ? null : (LinkItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                snackbar.setText(getResources().getString(R.string.notification_updated_item_failed))
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.requestUpdateLink(appState.getActiveCircle(), item);
                                }
                            }
                        })
                        .show();
                break;
            }

            /* Copy a link */
            case Const.MSG_COPY_LINK: {
                final LinkItem item = msg.obj == null ? null : (LinkItem) msg.obj;

                if (item != null) {
                    TaskUtils.copyToClipboard(this, item.getUrl());
                    Toast.makeText(getApplicationContext(), getString(R.string.notification_copy_link), Toast.LENGTH_SHORT).show();
                }

                break;
            }

            /* Create a list */
            case Const.MSG_LIST_CREATED: {
                final ListItem item = msg.obj == null ? null : (ListItem) msg.obj;
                if (item != null) {
                    appState.getActiveCircle().addItem(item);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemAdded(item.getId());
                        }
                    }
                }

                break;
            }

            case Const.MSG_LIST_CREATED_SUCCESS: {
                ListItem item = msg.obj == null ? null : (ListItem) msg.obj;
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

            case Const.MSG_LIST_CREATED_FAILED: {
                final ListItem item = msg.obj == null ? null : (ListItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                snackbar.setText(getResources().getString(R.string.notification_created_item_failed))
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.requestCreateList(appState.getActiveCircle(), item);
                                }
                            }
                        })
                        .show();
                break;
            }

            /* Updated a list */
            case Const.MSG_LIST_UPDATED: {
                final ListItem item = msg.obj == null ? null : (ListItem) msg.obj;
                if (item != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                break;
            }

            case Const.MSG_LIST_UPDATED_SUCCESS: {
                ListItem item = msg.obj == null ? null : (ListItem) msg.obj;
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

            case Const.MSG_LIST_UPDATED_FAILED: {
                final ListItem item = msg.obj == null ? null : (ListItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                snackbar.setText(getResources().getString(R.string.notification_updated_item_failed))
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.requestUpdateList(appState.getActiveCircle(), item);
                                }
                            }
                        })
                        .show();
                break;
            }

            case Const.MSG_NOTE_CREATED: {
                final NoteItem item = msg.obj == null ? null : (NoteItem) msg.obj;
                if (item != null) {
                    appState.getActiveCircle().addItem(item);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemAdded(item.getId());
                        }
                    }
                }

                break;
            }

            case Const.MSG_NOTE_CREATED_SUCCESS: {
                NoteItem item = msg.obj == null ? null : (NoteItem) msg.obj;
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

            case Const.MSG_NOTE_CREATED_FAILED: {
                final NoteItem item = msg.obj == null ? null : (NoteItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                snackbar.setText(getResources().getString(R.string.notification_created_item_failed))
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.requestCreateNote(appState.getActiveCircle(), item);
                                }
                            }
                        })
                        .show();
                break;
            }

            case Const.MSG_NOTE_UPDATED: {
                final NoteItem item = msg.obj == null ? null : (NoteItem) msg.obj;
                if (item != null) {
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                break;
            }

            case Const.MSG_NOTE_UPDATED_SUCCESS: {
                NoteItem item = msg.obj == null ? null : (NoteItem) msg.obj;
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

            case Const.MSG_NOTE_UPDATED_FAILED: {
                final NoteItem item = msg.obj == null ? null : (NoteItem) msg.obj;
                int status = msg.arg1;

                if (item != null) {
                    item.setSyncStatus(status);
                    if (boardItemChangeListeners != null) {
                        for (BoardItemChangeListener listener : boardItemChangeListeners) {
                            listener.onItemModified(item.getId());
                        }
                    }
                }

                snackbar.setText(getResources().getString(R.string.notification_updated_item_failed))
                        .setAction(getResources().getString(R.string.menu_item_try_again), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (item != null) {
                                    dataProvider.requestUpdateNote(appState.getActiveCircle(), item);
                                }
                            }
                        })
                        .show();
                break;
            }
        }

        return false;
    }

    public Handler getHandler() { return handler; }

    /**********************************************************
     * Other View Callbacks from Fragment/etc
     * (Toolbar, toolbar menu, notification, etc.
     **********************************************************/

    @Override
    public void onToolbarNavIconChanged(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .transform(new CircleTransform(this))
                .override(getResources().getDimensionPixelSize(R.dimen.toolbar_nav_icon_size),
                        getResources().getDimensionPixelSize(R.dimen.toolbar_nav_icon_size))
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .crossFade(1000)
                .into(toolbarNavIcon);
    }

    @Override
    public void onToolbarTitleChanged(String title) {
        toolbarTitle.setText(title);
    }

    @Override
    public void onToolbarSubtitleChanged(String subtitle) {
        toolbarSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);
        if (toolbarSubtitle.getVisibility() == View.VISIBLE) {
            toolbarSubtitle.setText(subtitle);
        }
    }

    @Override
    public void onShowNotificationBar(int bgColor, String text, long duration) {
        if (notificationBar != null) {
            notificationBar.setVisibility(View.VISIBLE);
            notificationText.setText(text);
            notificationText.setBackgroundColor(bgColor);

            if (duration > 0L) {
                notificationBar.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        notificationBar.setVisibility(View.GONE);
                    }
                }, duration);
            }
        }
        else {
            View view = findViewById(R.id.stub_notification_bar);
            if (view != null && view instanceof ViewStub) {
                if (notificationBar == null) {
                    notificationBar = ((ViewStub) view).inflate();

                    notificationText = (TextView) notificationBar.findViewById(R.id.notification_text);
                    ImageView notificationCloseBtn = (ImageView) notificationBar.findViewById(R.id.notification_close_btn);

                    notificationText.setText(text);
                    notificationText.setBackgroundColor(bgColor);
                    notificationCloseBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            notificationBar.setVisibility(View.GONE);
                        }
                    });

                    if (duration > 0L) {
                        notificationBar.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                notificationBar.setVisibility(View.GONE);
                            }
                        }, duration);
                    }
                }
            }
        }
    }

    @Override
    public void onOpenCircleMenu() {
        circleMenu.open(true);
    }

    @Override
    public void onSetFabVisible(boolean visible) {
        if (fab.isShown() && !visible) {
            fab.hide();
        }
        else if (!fab.isShown() && visible) {
            fab.show();
        }
    }

    @Override
    public Handler getThreadHandler() {
        return handler;
    }

    @Override
    public void onShowCategoryBar() {
        if (((int)categoryBar.getTag()) == View.INVISIBLE) {
            final int translationY = getResources().getDimensionPixelSize(R.dimen.bottom_bar_height);
            ViewUtils.animateTranslateY(categoryBar, translationY, 0, 200);
            categoryBar.setTag(View.VISIBLE);
        }
    }

    /**********************************************************
     * Drawer Handler
     **********************************************************/

    @Override
    public void onNavMenuItemClicked(NavMenuItem item) {
        if (item instanceof CircleInfo) {
            dataProvider.cancelAllNetworkRequests();
            dataProvider.getCircleByIdAsync(((CircleInfo) item).id);
        }
    }

    /**
     * Forces updates of all fragments and UI. Use only if selecting a new circle to display.
     */
    private void invalidateViews() {
        updateBroadcastLocationSheet();

        onToolbarNavIconChanged(appState.getActiveCircle().getAvatar());

        // refresh all fragments
        if (circleChangeListeners != null) {
            for (CircleChangeListener listener : circleChangeListeners) {
                listener.onCircleChanged();
            }
        }
    }

    /**********************************************************
     * Activity Finish Handler
     **********************************************************/
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            /* User has changed their permission settings */
            case EasyPermissions.SETTINGS_REQ_CODE: {
                // nothing yet

                break;
            }

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

            /* User has done taking photo */
            case Const.CAMERA_RESULT_CODE: {
                if (resultCode == RESULT_OK && data != null) {
                    String path = data.getStringExtra(getResources().getString(R.string.EXTRA_CAMERA_MEDIA_PATH));
                    dataProvider.postCameraAsync(path, appState.getActiveCircle(), true);
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
     * CONTEXTUAL ACTION MODE CALLBACKS
     **********************************************************/

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (actionModeCallbacks != null) {
            return actionModeCallbacks.onCreateActionMode(mode, menu);
        }

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (actionModeCallbacks != null) {
            return actionModeCallbacks.onPrepareActionMode(mode, menu);
        }

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (actionModeCallbacks != null) {
            return actionModeCallbacks.onActionItemClicked(mode, item);
        }

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;

        if (actionModeCallbacks != null) {
            actionModeCallbacks.onDestroyActionMode(mode);
        }
    }

    /**********************************************************
     * PERMISSION CALLBACKS
     * Needs to be declared here because EasyPermissions
     * traces back method calls to be child class and attempts
     * to find the annotated methods here
     **********************************************************/

    @AfterPermissionGranted(PERMISSION_WRITE_STORAGE)
    protected void startDrawActivity() {
        super.startDrawActivity();
    }

    @AfterPermissionGranted(PERMISSION_READ_STORAGE)
    protected void openImageBrowser() {
        super.openImageBrowser();
    }

    @AfterPermissionGranted(PERMISSION_LOCATION)
    protected void showBroadcastLocationMenuOptions() {
        super.showBroadcastLocationMenuOptions();
    }

    @AfterPermissionGranted(PERMISSION_CAMERA)
    protected void launchCamera() {
        super.launchCamera();
    }
}