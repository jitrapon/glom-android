package com.abborg.glom.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import com.abborg.glom.ApplicationState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.adapters.BoardItemAction;
import com.abborg.glom.di.ComponentInjector;
import com.abborg.glom.interfaces.FileDownloadListener;
import com.abborg.glom.interfaces.ResponseListener;
import com.abborg.glom.model.BaseChatMessage;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.CheckedItem;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.model.CloudProvider;
import com.abborg.glom.model.DiscoverItem;
import com.abborg.glom.model.DrawItem;
import com.abborg.glom.model.EventItem;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.model.FileItem;
import com.abborg.glom.model.LinkItem;
import com.abborg.glom.model.ListItem;
import com.abborg.glom.model.Movie;
import com.abborg.glom.model.NoteItem;
import com.abborg.glom.model.User;
import com.abborg.glom.model.WatchableFeed;
import com.abborg.glom.model.WatchableImage;
import com.abborg.glom.model.WatchableRating;
import com.abborg.glom.model.WatchableVideo;
import com.abborg.glom.utils.FileTransfer;
import com.abborg.glom.utils.FileUtils;
import com.abborg.glom.utils.HttpClient;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

/**
 * Controller class that wraps around model to perform CRUD operations on database and
 * make necessary network operations.
 *
 * Created by Jitrapon Tiachunpun on 22/9/58.
 */
public class DataProvider {

    @Inject
    GoogleCloudMessaging gcm;
    
    @Inject
    HttpClient httpClient;

    @Inject
    ApplicationState appState;

    private FileTransfer fileTransfer;

    private static final String TAG = "DATA PROVIDER";

    private Context context;

    private Handler handler;

    private ExecutorService threadPool;

    /* Database */
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private String[] circleColumns = { DBHelper.CIRCLE_COLUMN_ID,
            DBHelper.CIRCLE_COLUMN_NAME, DBHelper.CIRCLE_COLUMN_BROADCAST_LOCATION };
    private String[] userCircleColumns = { DBHelper.USERCIRCLE_COLUMN_USER_ID, DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID,
            DBHelper.USERCIRCLE_COLUMN_LATITUDE, DBHelper.USERCIRCLE_COLUMN_LONGITUDE };

    /* Determines the type of app start */
    private enum AppStart {
        FIRST_TIME, FIRST_TIME_VERSION, NORMAL;
    }

    /* The app version code (not the name) used on the last start of the app */
    private static final String LAST_APP_VERSION = "last_app_version";

    /*************************************************
     * INITIALIZATION OPERATIONS
     *************************************************/

    public DataProvider(Context ctx) {
        ComponentInjector.INSTANCE.getApplicationComponent().inject(this);

        context = ctx;
        dbHelper = new DBHelper(context);
        threadPool = Executors.newCachedThreadPool();
        fileTransfer = new FileTransfer(context, appState, this);
    }

    public void setHandler(Handler h) {
        handler = h;
        fileTransfer.setHandler(h);
    }

    public Handler getHandler() { return handler; }

    public void loadDataAsync() {
        run(new Runnable() {
            @Override
            public void run() {
                try {
                    openDB();

                    // initialize the activeUser info
                    User user = getActiveUser();
                    if (user == null) {
                        user = createUser(getActiveUserId());
                    }

                    // determine if the user has launched the app before and what version
                    AppStart appStart = checkAppStart();
                    switch (appStart) {
                        case NORMAL:
                            Log.d("INIT", "App has launched normally, version is the same");
                            break;
                        case FIRST_TIME_VERSION:
                            Log.d("INIT", "App has been upgraded! Version is different");
                            break;
                        case FIRST_TIME:
                            Log.d("INIT", "App has not been launched before, resetting the state to default");
                            resetCircles();
                            createCircle(context.getResources().getString(R.string.friends_circle_title), null, Const.TEST_CIRCLE_ID);
                            break;
                        default:
                    }

                    // finally before finishing this async task, set the appropriate fields in the AppState
                    List<CircleInfo> circleInfoList = getCirclesInfo();
                    Circle circle = getCircleById(Const.TEST_CIRCLE_ID);
                    appState.setActiveUser(user);
                    appState.setCircleInfos(circleInfoList);
                    appState.setActiveCircle(circle);

                    if (handler != null) {
                        handler.sendEmptyMessage(Const.MSG_INIT_SUCCESS);
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG, ex.getMessage());
                }
            }
        });
    }

    //FIXME this is a debug-convenience method to create a user
    private User createUser(String id) {
        Location location = new Location("");
        location.setLatitude(0);
        location.setLongitude(0);
        User user = new User("", id, location, User.TYPE_USER);
        user.setAvatar("");
        return user;
    }

    /**
     * Finds out started for the first time (ever or in the current version).<br/>
     * <br/>
     * Note: This method is <b>not idempotent</b> only the first call will
     * determine the proper result. Any subsequent calls will only return
     * {@link AppStart#NORMAL} until the app is started again. So you might want
     * to consider caching the result!
     *
     * @return the type of app start
     */
    private AppStart checkAppStart() {
        PackageInfo pInfo;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        AppStart appStart = AppStart.NORMAL;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int lastVersionCode = sharedPreferences.getInt(LAST_APP_VERSION, -1);
            int currentVersionCode = pInfo.versionCode;
            appStart = checkAppStart(currentVersionCode, lastVersionCode);

            // Update version in preferences
            sharedPreferences.edit()
                    .putInt(LAST_APP_VERSION, currentVersionCode).apply();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("ERROR",
                    "Unable to determine current app version from package manager. Defensively assuming normal app start.");
        }
        return appStart;
    }

    private AppStart checkAppStart(int currentVersionCode, int lastVersionCode) {
        if (lastVersionCode == -1) {
            return AppStart.FIRST_TIME;
        }
        else if (lastVersionCode < currentVersionCode) {
            return AppStart.FIRST_TIME_VERSION;
        }
        else if (lastVersionCode > currentVersionCode) {
            Log.w("ERROR", "Current version code (" + currentVersionCode
                    + ") is less then the one recognized on last startup ("
                    + lastVersionCode
                    + "). Defenisvely assuming normal app start.");
            return AppStart.NORMAL;
        }
        else {
            return AppStart.NORMAL;
        }
    }

    public void openDB() {
        try {
            Log.d(TAG, "Opening database");
            database = dbHelper.getWritableDatabase();
        }
        catch (SQLiteException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void closeDB() {
        Log.d(TAG, "Closing database");
        dbHelper.close();
    }

    public void cancelAllNetworkRequests() {
        httpClient.getRequestQueue().cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    private void handleNetworkError(VolleyError error) {
        boolean connectivityError = httpClient.handleError(error);
        if (connectivityError) {
            appState.setConnectivityStatus(ApplicationState.ConnectivityStatus.DISCONNECTED);
            if (handler != null) handler.sendEmptyMessage(Const.MSG_SERVER_DISCONNECTED);
        }
    }

    private void handleNetworkSuccess() {
        if (handler != null && appState.getConnectionStatus() != ApplicationState.ConnectivityStatus.CONNECTED) {
            appState.setConnectivityStatus(ApplicationState.ConnectivityStatus.CONNECTED);
            handler.sendEmptyMessage(Const.MSG_SERVER_CONNECTED);
        }
    }

    public void requestServerStatus() {
        httpClient.get("Check Status", Const.API_SERVER_STATUS, new ResponseListener() {
            @Override
            public void onSuccess(JSONObject response) {
                handleNetworkSuccess();
            }

            @Override
            public void onError(VolleyError error) {
                handleNetworkError(error);
            }
        });
    }

    public void run(Runnable runnable) {
        threadPool.submit(runnable);
    }

    /*************************************************
     * CIRCLE OPERATIONS
     *************************************************/
    public void resetCircles() {
        try {
            database.beginTransaction();

            database.execSQL("DELETE FROM " + DBHelper.TABLE_USER_CIRCLE);
            database.execSQL("DELETE FROM " + DBHelper.TABLE_CIRCLES);
            database.execSQL("DELETE FROM " + DBHelper.TABLE_USERS);
            database.execSQL("DELETE FROM " + DBHelper.TABLE_EVENTS);

            database.setTransactionSuccessful();
        }
        finally {
            database.endTransaction();
        }
    }

    public Circle createCircle(String name, List<User> users, String id) {
        Circle circle = Circle.createCircle(name, appState.getActiveUser());
        if (id != null) circle.setId(id);
        if (users != null && !users.isEmpty()) circle.addUsers(users);

        database.beginTransaction();

        try {
            // insert new record into CIRCLES table (record is unique per circle)
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLE_COLUMN_ID, circle.getId());
            values.put(DBHelper.CIRCLE_COLUMN_NAME, circle.getTitle());
            long insertId = database.insert(DBHelper.TABLE_CIRCLES, null, values);
            Log.d(TAG, "Inserted circle with _id: " + insertId + ", id: " + circle.getId() + ", name: " +
                    circle.getTitle() + ", userlist: " + circle.getUserListString());

            for (User user : circle.getUsers()) {
                user.setCurrentCircle(circle);

                // insert new users into USER table if unique (record is unique per user)
                values.clear();
                values.put(DBHelper.USER_COLUMN_ID, user.getId());
                values.put(DBHelper.USER_COLUMN_NAME, user.getName());
                values.put(DBHelper.USER_COLUMN_AVATAR, user.getAvatar());
                values.put(DBHelper.USER_COLUMN_TYPE, user.getType());
                insertId = database.insert(DBHelper.TABLE_USERS, null, values);
                Log.d(TAG, "Inserted user with _id: " +  insertId + ", id: " + user.getId() + " into " + DBHelper.TABLE_USERS);

                // insert the user-circle association into the USERCIRCLE table (record is unique per association)
                values.clear();
                values.put(DBHelper.USERCIRCLE_COLUMN_USER_ID, user.getId());
                values.put(DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID, circle.getId());
                values.put(DBHelper.USERCIRCLE_COLUMN_LATITUDE, user.getLocation().getLatitude());
                values.put(DBHelper.USERCIRCLE_COLUMN_LONGITUDE, user.getLocation().getLongitude());
                insertId = database.insert(DBHelper.TABLE_USER_CIRCLE, null, values);
                Log.d(TAG, "Inserted user with _id: " + insertId + ", userId: " + user.getId()
                        + ", circleId: " + circle.getId() + " into " + DBHelper.TABLE_USER_CIRCLE);
            }

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }

        //TODO send request to GCM and server to create new group

        return circle;
    }

    @SuppressWarnings("unused")
    public void deleteCircle(Circle circle) {
        String id = circle.getId();
        database.delete(DBHelper.TABLE_CIRCLES, DBHelper.CIRCLE_COLUMN_ID + " = " + id, null);

        //TODO send request to GCM and server to delete group
    }

    public List<CircleInfo> getCirclesInfo() {
        List<CircleInfo> circleInfo = new ArrayList<>();

        Cursor cursor = database.query(DBHelper.TABLE_CIRCLES,
                circleColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            CircleInfo info = serializeCircleInfo(cursor);
            circleInfo.add(info);
            cursor.moveToNext();
        }
        cursor.close();
        return circleInfo;
    }

    private Circle serializeCircle(Cursor cursor) {
        Circle circle = Circle.createCircle(null, appState.getActiveUser());
        circle.setId(cursor.getString(0));
        circle.setTitle(cursor.getString(1));
        circle.setBroadcastingLocation((cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLE_COLUMN_BROADCAST_LOCATION)) == 1));

        List<User> users = getUsersInCircle(circle);
        List<BoardItem> items = getCircleItems(circle);
        circle.setUsers(users);
        circle.setItems(items);
        return circle;
    }

    private CircleInfo serializeCircleInfo(Cursor cursor) {
        CircleInfo info = new CircleInfo();
        info.id = cursor.getString(0);
        info.title = cursor.getString(1);

        Cursor userListCursor = database.query(DBHelper.TABLE_USER_CIRCLE, userCircleColumns, DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "='" + info.id + "'",
                null, null, null, null);
        info.numUsers = userListCursor.getCount();
        userListCursor.close();
        return info;
    }

    public Circle getCircleById(String id) {
        Circle circle = null;
        Cursor cursor = database.query(DBHelper.TABLE_CIRCLES,
                circleColumns, DBHelper.CIRCLE_COLUMN_ID + "='" + id + "'", null, null, null, null);

        if (cursor.moveToFirst()) {
            circle = serializeCircle(cursor);
            cursor.close();
            Log.d(TAG, "Get circle (" + circle.getTitle() + ") with " + circle.getUsers().size() + " users, isBroadcastingLocation: " +
                    circle.isUserBroadcastingLocation());
        }

        return circle;
    }

    public void getCircleByIdAsync(final String id) {
        run(new Runnable() {

            @Override
            public void run() {
                Circle circle = getCircleById(id);
                if (handler != null) {
                    handler.sendMessage(handler.obtainMessage(Const.MSG_GET_CIRCLE, circle));
                }
            }
        });
    }

    /*************************************************
     * USER OPERATIONS
     *************************************************/

    public User getActiveUser() {
        String id = getActiveUserId();
        User user = null;
        Cursor cursor = database.query(DBHelper.TABLE_USERS, null, DBHelper.USER_COLUMN_ID + " = '" + id + "'", null, null, null ,null);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_NAME));
            String userId = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_ID));
            String avatar = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_AVATAR));
            int type = cursor.getInt(cursor.getColumnIndex(DBHelper.USER_COLUMN_TYPE));
            user = new User(name, userId, null, type);
            user.setAvatar(avatar);
        }
        cursor.close();

        return user;
    }

    private String getActiveUserId() {
        return Const.TEST_USER_ID;
    }

    public List<BoardItemAction> getFavoriteBoardItemActions() {
        return Arrays.asList(
                BoardItemAction.EVENT,
                BoardItemAction.DRAW,
                BoardItemAction.LIST
        );
    }

    public void addUsersToCircleDB(Circle circle, List<User> users) {
        if (circle != null && users != null) {
            database.beginTransaction();

            try {
                ContentValues values = new ContentValues();
                for (User user : users) {
                    user.setCurrentCircle(circle);

                    values.put(DBHelper.USER_COLUMN_ID, user.getId());
                    values.put(DBHelper.USER_COLUMN_NAME, user.getName());
                    values.put(DBHelper.USER_COLUMN_AVATAR, user.getAvatar());
                    values.put(DBHelper.USER_COLUMN_TYPE, user.getType());
                    database.insert(DBHelper.TABLE_USERS, null, values);
                    values.clear();

                    values.put(DBHelper.USERCIRCLE_COLUMN_USER_ID, user.getId());
                    values.put(DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID, circle.getId());
                    if (user.getLocation() != null) {
                        values.put(DBHelper.USERCIRCLE_COLUMN_LATITUDE, user.getLocation().getLatitude());
                        values.put(DBHelper.USERCIRCLE_COLUMN_LONGITUDE, user.getLocation().getLongitude());
                    }
                    database.insert(DBHelper.TABLE_USER_CIRCLE, null, values);
                    values.clear();
                }

                database.setTransactionSuccessful();
            }
            catch (SQLException ex) {
                Log.e(TAG, ex.getMessage());
            }
            finally {
                database.endTransaction();
            }
        }
    }

    public void modifyUsersInCircleDB(Circle circle, List<User> users) {
        if (circle != null && users != null) {
            database.beginTransaction();

            try {
                ContentValues values = new ContentValues();
                for (User user : users) {
                    user.setCurrentCircle(circle);

                    values.put(DBHelper.USER_COLUMN_NAME, user.getName());
                    values.put(DBHelper.USER_COLUMN_AVATAR, user.getAvatar());
                    values.put(DBHelper.USER_COLUMN_TYPE, user.getType());
                    database.update(DBHelper.TABLE_USERS, values, DBHelper.USER_COLUMN_ID + "='" + user.getId() + "'", null);
                    values.clear();
                }

                database.setTransactionSuccessful();
            }
            catch (SQLException ex) {
                Log.e(TAG, ex.getMessage());
            }
            finally {
                database.endTransaction();
            }
        }
    }

    public void removeUsersFromCircleDB(Circle circle, List<User> users) {
        if (circle != null && users != null) {
            database.beginTransaction();

            try {
                for (User user : users) {
                    database.delete(DBHelper.TABLE_USER_CIRCLE, DBHelper.USERCIRCLE_COLUMN_USER_ID + "='" + user.getId() + "' AND "
                            + DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "='" + circle.getId() + "'", null);
                }

                database.setTransactionSuccessful();
            }
            catch (SQLException ex) {
                Log.e(TAG, ex.getMessage());
            }
            finally {
                database.endTransaction();
            }
        }
    }

    public List<User> getUsersInCircle(final Circle circle) {
        List<User> users = new ArrayList<>();

        // get the user info from USERS table and the user location from USERCIRCLE table
        // SELECT id,name,avatarId,location
        String selectColumns = DBHelper.USER_COLUMN_ID + "," + DBHelper.USER_COLUMN_NAME + ","
                + DBHelper.USER_COLUMN_AVATAR + "," + DBHelper.USER_COLUMN_TYPE + "," +
                DBHelper.USERCIRCLE_COLUMN_LATITUDE + "," + DBHelper.USERCIRCLE_COLUMN_LONGITUDE;

        String query = "SELECT " + selectColumns + " FROM " + DBHelper.TABLE_USERS + ", " + DBHelper.TABLE_USER_CIRCLE + " WHERE " +
                DBHelper.TABLE_USERS + "." + DBHelper.USER_COLUMN_ID + "=" + DBHelper.TABLE_USER_CIRCLE + "." +
                DBHelper.USERCIRCLE_COLUMN_USER_ID + " AND " + DBHelper.TABLE_USER_CIRCLE + "." + DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "=" +
                "'" + circle.getId() + "'";
        Cursor userCursor = database.rawQuery(query, null);

        userCursor.moveToFirst();
        while (!userCursor.isAfterLast()) {
            User user = serializeUser(userCursor, circle);
            user.setDirty(true);    // mark this user as 'dirty' to indicate that the user may be deleted
            users.add(user);
            userCursor.moveToNext();
        }
        userCursor.close();

        return users;
    }

    public void requestGetUsersInCircle(final Circle circle) {
        httpClient.get("Get Users", String.format(Const.API_GET_USERS, circle.getId()),
                new ResponseListener() {
                    @Override
                    public void onSuccess(JSONObject resp) {
                        handleNetworkSuccess();

                        final JSONObject response = resp;
                        run(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    if (response != null) {
                                        JSONArray jsonArray = response.getJSONArray(Const.JSON_SERVER_USERS);
                                        List<User> toModify = new ArrayList<>();        // list of users to modify in DB
                                        List<User> toAdd = new ArrayList<>();       // list of users to update in DB
                                        List<User> toDelete = new ArrayList<>();    // list of users to be deleted from DB
                                        int unchanged = 0;

                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            JSONObject json = jsonArray.getJSONObject(i);
                                            String id = json.getString(Const.JSON_SERVER_USERID);
                                            String name = json.getString(Const.JSON_SERVER_USERNAME);
                                            String avatar = json.getString(Const.JSON_SERVER_USER_AVATAR);
                                            int type = json.getInt(Const.JSON_SERVER_USERTYPE);
                                            id = TextUtils.isEmpty(id) ? "" : id;
                                            name = TextUtils.isEmpty(name) ? "" : name;
                                            avatar = TextUtils.isEmpty(avatar) ? "" : avatar;

                                            User user = circle.getUser(id);

                                            // if the user already exists in this circle, modify them
                                            if (user != null) {

                                                // update only if necessary
                                                if (!name.equals(user.getName()) || !avatar.equals(user.getAvatar())) {
                                                    if (id.equals(appState.getActiveUser().getId())) {
                                                        appState.getActiveUser().setName(name);
                                                        appState.getActiveUser().setAvatar(avatar);
                                                        appState.getActiveUser().setType(type);
                                                    }
                                                    user.setName(name);
                                                    user.setAvatar(avatar);
                                                    user.setType(type);

                                                    // add to the list of ones to be modified;
                                                    toModify.add(user);
                                                } else unchanged += 1;

                                                // mark user to be stable
                                                user.setDirty(false);
                                            }

                                            // if there's a new user, add them
                                            else {
                                                if (!TextUtils.isEmpty(id)) {
                                                    Location location = new Location("");
                                                    location.setLatitude(0);
                                                    location.setLongitude(0);
                                                    user = new User(name, id, location, type);
                                                    user.setDirty(false);
                                                    if (user.getAvatar() == null || !user.getAvatar().equals(avatar))
                                                        user.setAvatar(avatar);

                                                    circle.addUser(user);

                                                    // add to the list of ones to be added
                                                    toAdd.add(user);
                                                }
                                            }
                                        }

                                        // otherwise, if the user is not found in the list of response, delete them
                                        for (User user : circle.getUsers()) {
                                            if (user.isDirty()) {
                                                toDelete.add(user);
                                            }
                                        }
                                        circle.removeDirtyUsers();

                                        // update DB copy
                                        if (!toModify.isEmpty()) modifyUsersInCircleDB(circle, toModify);
                                        if (!toAdd.isEmpty()) addUsersToCircleDB(circle, toAdd);
                                        if (!toDelete.isEmpty()) removeUsersFromCircleDB(circle, toDelete);
                                        Log.d(TAG, "Added " + toModify.size() + " new user(s)");
                                        Log.d(TAG, "Modified " + toModify.size() + " user(s)");
                                        Log.d(TAG, "Removed " + toDelete.size() + " user(s)");
                                        Log.d(TAG, unchanged + " user(s) remained unchanged");

                                        // alert UI that we may have some changes to user list
                                        if (handler != null) handler.sendEmptyMessage(Const.MSG_GET_USERS);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    Log.e(TAG, ex.getMessage());
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(VolleyError error) {
                        handleNetworkError(error);
                    }
                });
    }

    private User serializeUser(Cursor cursor, Circle circle) {
        User user = new User(null, null, null, -1);
        user.setId(cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_ID)));
        user.setName(cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_NAME)));
        user.setAvatar(cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_AVATAR)));
        user.setType(cursor.getInt(cursor.getColumnIndex(DBHelper.USER_COLUMN_TYPE)));

        Location location = new Location("");
        location.setLatitude(cursor.getDouble(cursor.getColumnIndex(DBHelper.USERCIRCLE_COLUMN_LATITUDE)));
        location.setLongitude(cursor.getDouble(cursor.getColumnIndex(DBHelper.USERCIRCLE_COLUMN_LONGITUDE)));
        user.setLocation(location);
        user.setCurrentCircle(circle);

        return user;
    }

    /*************************************************
     * LOCATION OPERATIONS
     *************************************************/

    public void updateCircleLocationBroadcast(String circleId, boolean enabled) {
        ContentValues values = new ContentValues();
        int broadcastingLocation = enabled ? 1 : 0;
        values.put(DBHelper.CIRCLE_COLUMN_BROADCAST_LOCATION, broadcastingLocation);
        int rowAffected = database.update(DBHelper.TABLE_CIRCLES, values,
                DBHelper.CIRCLE_COLUMN_ID + "='" + circleId + "'", null);
        Log.d(TAG, "Updated " + rowAffected + " row(s) in " + DBHelper.TABLE_USER_CIRCLE);
    }

    public void onLocationUpdateReceived(Bundle data) {
        String userJson = data.getString(Const.JSON_SERVER_USERIDS);
        String circleId = data.getString(Const.JSON_SERVER_CIRCLEID);

        // we first find the circle from its ID
        Cursor circleCursor = database.query(DBHelper.TABLE_CIRCLES,
                circleColumns, DBHelper.CIRCLE_COLUMN_ID + "='" + circleId + "'", null, null, null, null);

        // if we find a circle in the DB
        if (circleCursor.moveToFirst()) {
            try {
                // update the location markers
                JSONArray users = new JSONArray(userJson);

                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);
                    String userId = user.getString(Const.JSON_SERVER_USERID);
                    if (userId.equals(getActiveUserId())) continue;

                    // verify that the user is in a circle and the ID is valid
                    Cursor userInCircleCursor = database.query(DBHelper.TABLE_USER_CIRCLE,
                            userCircleColumns, DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "='" + circleId + "' AND " +
                            DBHelper.USERCIRCLE_COLUMN_USER_ID + "='" + userId + "'", null, null, null, null);

                    if (userInCircleCursor.moveToFirst()) {
                        JSONObject locationJson = user.getJSONObject(Const.JSON_SERVER_LOCATION);
                        Location location = new Location("");
                        location.setLatitude(locationJson.getDouble(Const.JSON_SERVER_LOCATION_LAT));
                        location.setLongitude(locationJson.getDouble(Const.JSON_SERVER_LOCATION_LONG));

                        // update user location in DB
                        updateUserLocationDB(userId, circleId, location.getLatitude(), location.getLongitude());

                        Log.i(TAG, "User ID: " + userId + "\nLat: " + locationJson.getDouble(Const.JSON_SERVER_LOCATION_LAT) + "\nLong: " +
                                locationJson.getDouble(Const.JSON_SERVER_LOCATION_LONG));
                    }
                    else {
                        Log.e(TAG, "Received user ID of " + userId + " does not exist in circle (" + circleId + ")");
                    }

                    userInCircleCursor.close();
                }
            }
            catch (JSONException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }

        // if we cannot find ANY circle in the DB for our user
        else {
            Log.e(TAG, "Could not find circle (" + circleId + ") under this user!");
        }

        circleCursor.close();
    }

    public void updateUserLocationDB(String userId, String circleId, Double lat, Double lng) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.USERCIRCLE_COLUMN_LATITUDE, lat);
        values.put(DBHelper.USERCIRCLE_COLUMN_LONGITUDE, lng);
        int rowAffected = database.update(DBHelper.TABLE_USER_CIRCLE, values,
                DBHelper.USERCIRCLE_COLUMN_USER_ID + "='" + userId + "' AND " +
                        DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "='" + circleId + "'", null);
        Log.d(TAG, "Updated " + rowAffected + " row(s) in " + DBHelper.TABLE_USER_CIRCLE);
    }

    /*************************************************
     * CIRCLE ITEMS OPERATIONS
     *************************************************/
    public void requestBoardItems(final Circle circle) {
        if (circle != null) {
            httpClient.get("Get Board", String.format(Const.API_BOARD, circle.getId()),
                new ResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        handleNetworkSuccess();

                        final JSONObject respJson = response;
                        run(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (respJson != null) {
                                        JSONArray jsonArray = respJson.getJSONArray(Const.JSON_SERVER_ITEMS);
                                        int numItem = jsonArray.length();
                                        List<BoardItem> items = circle.getItems();

                                        // mark all board items as dirty so we know which ones to delete later
                                        if (items != null) {
                                            for (BoardItem item : items) {
                                                if (item.getSyncStatus() != BoardItem.NO_SYNC &&
                                                        item.getSyncStatus() != BoardItem.SYNC_ERROR)
                                                    Log.d(TAG, "Setting item " + item.getId() + " dirty");
                                                    item.setDirty(true);
                                            }
                                        }
                                        else return;

                                        if (numItem != 0) {
                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                JSONObject json = jsonArray.getJSONObject(i);
                                                String error = json.optString(Const.JSON_SERVER_ERROR);
                                                if (TextUtils.isEmpty(error)) {
                                                    String id = json.getString(Const.JSON_SERVER_ITEM_ID);
                                                    int type = json.getInt(Const.JSON_SERVER_ITEM_TYPE);
                                                    Long createdMillis = json.getLong(Const.JSON_SERVER_CREATED_TIME);
                                                    Long updatedMillis = json.getLong(Const.JSON_SERVER_UPDATED_TIME);
                                                    JSONObject info = json.getJSONObject(Const.JSON_SERVER_INFO);

                                                    switch (type) {
                                                        case BoardItem.TYPE_EVENT: {
                                                            String name = parseJsonString(info.getString(Const.JSON_SERVER_EVENT_NAME));
                                                            long start = info.optLong(Const.JSON_SERVER_EVENT_START_TIME);
                                                            long end = info.optLong(Const.JSON_SERVER_EVENT_END_TIME);
                                                            String placeId = parseJsonString(info.getString(Const.JSON_SERVER_EVENT_PLACE_ID));
                                                            JSONObject locationJson = info.getJSONObject(Const.JSON_SERVER_LOCATION);
                                                            double lat = locationJson.optDouble(Const.JSON_SERVER_LOCATION_LAT, -1);
                                                            double lng = locationJson.optDouble(Const.JSON_SERVER_LOCATION_LONG, -1);
                                                            String note = parseJsonString(info.getString(Const.JSON_SERVER_EVENT_NOTE));

                                                            DateTime startTime = null;
                                                            DateTime endTime = null;
                                                            if (start != 0L)
                                                                startTime = new DateTime(start);
                                                            if (end != 0L)
                                                                endTime = new DateTime(end);
                                                            Location location = null;
                                                            if (lat != -1 && lng != -1) {
                                                                location = new Location("");
                                                                location.setLatitude(lat);
                                                                location.setLongitude(lng);
                                                            }

                                                            EventItem event = null;
                                                            for (BoardItem item : items) {
                                                                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_EVENT) {
                                                                    event = (EventItem) item;
                                                                }
                                                            }
                                                            if (event == null) {
                                                                DateTime createdTime = new DateTime(createdMillis);
                                                                createEvent(circle, createdTime, id, name, startTime, endTime, placeId, location,
                                                                        note);
                                                            }
                                                            else if (event.getSyncStatus() != BoardItem.NO_SYNC) {
                                                                DateTime updatedTime = new DateTime(updatedMillis);
                                                                updateEvent(circle, updatedTime, id, name, startTime, endTime,
                                                                        placeId, location, note);
                                                                event.setDirty(false);
                                                            }
                                                            break;
                                                        }

                                                        case BoardItem.TYPE_FILE: {
                                                            String name = parseJsonString(info.getString(Const.JSON_SERVER_FILE_NAME));
                                                            long size = info.getLong(Const.JSON_SERVER_FILE_SIZE);
                                                            String mimetype = parseJsonString(info.getString(Const.JSON_SERVER_FILE_MIMETYPE));
                                                            String note = parseJsonString(info.getString(Const.JSON_SERVER_FILE_NOTE));
                                                            int provider = info.getInt(Const.JSON_SERVER_FILE_PROVIDER);

                                                            FileItem file = null;
                                                            for (BoardItem item : items) {
                                                                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_FILE) {
                                                                    file = (FileItem) item;
                                                                }
                                                            }
                                                            if (file == null) {
                                                                DateTime createdTime = new DateTime(createdMillis);
                                                                createFile(circle, createdTime, id, name, size, mimetype, note, provider);
                                                            }
                                                            else if (file.getSyncStatus() != BoardItem.NO_SYNC) {
                                                                DateTime updatedTime = new DateTime(updatedMillis);
                                                                updateFile(circle, updatedTime, id, name, size, mimetype, note, provider);
                                                                file.setDirty(false);
                                                            }

                                                            break;
                                                        }

                                                        case BoardItem.TYPE_DRAWING: {
                                                            String name = parseJsonString(info.getString(Const.JSON_SERVER_DRAWING_NAME));

                                                            DrawItem drawItem = null;
                                                            for (BoardItem item : items) {
                                                                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_DRAWING) {
                                                                    drawItem = (DrawItem) item;
                                                                }
                                                            }
                                                            if (drawItem == null) {
                                                                DateTime createdTime = new DateTime(createdMillis);
                                                                createDrawing(circle, createdTime, id, name);
                                                            }
                                                            else if (drawItem.getSyncStatus() != BoardItem.NO_SYNC) {
                                                                DateTime updatedTime = new DateTime(updatedMillis);
                                                                //TODO
                                                                drawItem.setDirty(false);
                                                            }
                                                            break;
                                                        }

                                                        case BoardItem.TYPE_LINK: {
                                                            String url = parseJsonString(info.getString(Const.JSON_SERVER_LINK_URL));
                                                            String description = parseJsonString(info.getString(Const.JSON_SERVER_LINK_DESCRIPTION));
                                                            String title = parseJsonString(info.getString(Const.JSON_SERVER_LINK_TITLE));
                                                            String thumbnail = parseJsonString(info.getString(Const.JSON_SERVER_LINK_THUMBNAIL));

                                                            LinkItem linkItem = null;
                                                            for (BoardItem item : items) {
                                                                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_LINK) {
                                                                    linkItem = (LinkItem) item;
                                                                }
                                                            }
                                                            if (linkItem == null) {
                                                                DateTime createdTime = new DateTime(createdMillis);
                                                                createLink(circle, createdTime, id, url, thumbnail, title, description);
                                                            }
                                                            else if (linkItem.getSyncStatus() != BoardItem.NO_SYNC) {
                                                                DateTime updatedTime = new DateTime(updatedMillis);
                                                                updateLink(circle, updatedTime, id, url, thumbnail, title, description);
                                                                linkItem.setDirty(false);
                                                            }
                                                            break;
                                                        }

                                                        case BoardItem.TYPE_LIST: {
                                                            String title = parseJsonString(info.getString(Const.JSON_SERVER_LIST_TITLE));

                                                            JSONArray jsonArrayItems = info.getJSONArray(Const.JSON_SERVER_LIST_ITEMS);
                                                            List<CheckedItem> checkedItems = new ArrayList<>();
                                                            for (int index = 0; index < jsonArrayItems.length(); index++) {
                                                                JSONObject jsonItem = jsonArrayItems.getJSONObject(index);
                                                                int state = jsonItem.getInt(Const.JSON_SERVER_LISTITEM_STATE);
                                                                String text = parseJsonString(jsonItem.getString(Const.JSON_SERVER_LISTITEM_TEXT));
                                                                checkedItems.add(new CheckedItem(state, text));
                                                            }

                                                            ListItem listItem = null;
                                                            for (BoardItem item : items) {
                                                                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_LIST) {
                                                                    listItem = (ListItem) item;
                                                                }
                                                            }
                                                            if (listItem == null) {
                                                                DateTime createdTime = new DateTime(createdMillis);
                                                                createList(circle, createdTime, id, title, checkedItems);
                                                            }
                                                            else if (listItem.getSyncStatus() != BoardItem.NO_SYNC) {
                                                                DateTime updatedTime = new DateTime(updatedMillis);
                                                                updateList(circle, updatedTime, id, title, checkedItems);
                                                                listItem.setDirty(false);
                                                            }

                                                            break;
                                                        }

                                                        case BoardItem.TYPE_NOTE: {
                                                            String title = parseJsonString(info.getString(Const.JSON_SERVER_NOTE_TITLE));
                                                            String text = parseJsonString(info.getString(Const.JSON_SERVER_NOTE_TEXT));

                                                            NoteItem noteItem = null;
                                                            for (BoardItem item : items) {
                                                                if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_NOTE) {
                                                                    noteItem = (NoteItem) item;
                                                                }
                                                            }
                                                            if (noteItem == null) {
                                                                DateTime createdTime = new DateTime(createdMillis);
                                                                createNote(circle, createdTime, id, title, text);
                                                            }
                                                            else if (noteItem.getSyncStatus() != BoardItem.NO_SYNC) {
                                                                DateTime updatedTime = new DateTime(updatedMillis);
                                                                updateNote(circle, updatedTime, id, title, text);
                                                                noteItem.setDirty(false);
                                                            }

                                                            break;
                                                        }
                                                        default:
                                                            break;
                                                    }
                                                }
                                            }
                                        }
                                        Log.d(TAG, "Found " + numItem + " items");

                                        // board items that are marked dirty and not marked as no-sync
                                        for (BoardItem item : items) {
                                            if (item.isDirty()) {
                                                deleteItemDB(item);
                                                Log.d(TAG, "Item " + item.getId() + " is dirty, deleting...");
                                            }
                                        }
                                        circle.removeDirtyItems();

                                        // update UI
                                        if (handler != null)
                                            handler.sendEmptyMessage(Const.MSG_GET_ITEMS);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    if (handler != null) handler.sendEmptyMessage(Const.MSG_GET_ITEMS);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(VolleyError error) {
                        if (handler != null) {
                            handler.sendEmptyMessage(Const.MSG_GET_ITEMS);
                            handleNetworkError(error);
                        }
                    }
                }
            );
        }
    }

    private String parseJsonString(String value) {
        return TextUtils.isEmpty(value) || value.equals("null") ? null : value;
    }

    public List<BoardItem> getCircleItems(Circle circle) {
        List<BoardItem> items = new ArrayList<>();

        // default sorting is order by event start time ascending
        // credit http://stackoverflow.com/questions/2440448/sql-join-different-tables-depending-on-row-information
        String query =
                "SELECT * FROM " + DBHelper.TABLE_CIRCLE_ITEMS + " " +
                "JOIN " + DBHelper.TABLE_EVENTS + " ON " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_ITEMID + "=" + DBHelper.TABLE_EVENTS + "." + DBHelper.EVENT_COLUMN_ID + " " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_CIRCLEID + "='" + circle.getId() + "' " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_TYPE + "=" + BoardItem.TYPE_EVENT + " " +
                "ORDER BY " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME + " ASC";

        Cursor cursor = database.rawQuery(query, null);
        String result = DatabaseUtils.dumpCursorToString(cursor);
        Log.d(TAG, "Found events: " + result);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            EventItem event = serializeEvent(cursor, circle);
            int action = event.getUpdatedTime().equals(event.getCreatedTime()) ? FeedAction.CREATE :
                    FeedAction.EDITED;
            event.setLastAction(new FeedAction(action, appState.getActiveUser(), event.getUpdatedTime()));
            items.add(event);
            cursor.moveToNext();
        }
        cursor.close();

        query =
                "SELECT * FROM " + DBHelper.TABLE_CIRCLE_ITEMS + " " +
                "JOIN " + DBHelper.TABLE_FILES + " ON " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_ITEMID + "=" + DBHelper.TABLE_FILES + "." + DBHelper.FILE_COLUMN_ID + " " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_CIRCLEID + "='" + circle.getId() + "' " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_TYPE + "=" + BoardItem.TYPE_FILE + " " +
                "ORDER BY " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME + " ASC";
        cursor = database.rawQuery(query, null);
        result = DatabaseUtils.dumpCursorToString(cursor);
        Log.d(TAG, "Found files: " + result);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            FileItem file = serializeFile(cursor, circle);
            items.add(file);
            cursor.moveToNext();
        }
        cursor.close();

        query =
                "SELECT * FROM " + DBHelper.TABLE_CIRCLE_ITEMS + " " +
                        "JOIN " + DBHelper.TABLE_DRAWINGS + " ON " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_ITEMID + "=" + DBHelper.TABLE_DRAWINGS + "." + DBHelper.DRAWING_COLUMN_ID + " " +
                        "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_CIRCLEID + "='" + circle.getId() + "' " +
                        "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_TYPE + "=" + BoardItem.TYPE_DRAWING + " " +
                        "ORDER BY " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME + " ASC";
        cursor = database.rawQuery(query, null);
        result = DatabaseUtils.dumpCursorToString(cursor);
        Log.d(TAG, "Found drawings: " + result);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            DrawItem item = serializeDrawing(cursor, circle);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        query = "SELECT * FROM " + DBHelper.TABLE_CIRCLE_ITEMS + " " +
                "JOIN " + DBHelper.TABLE_LINKS + " ON " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_ITEMID + "=" + DBHelper.TABLE_LINKS + "." + DBHelper.LINK_COLUMN_ID + " " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_CIRCLEID + "='" + circle.getId() + "' " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_TYPE + "=" + BoardItem.TYPE_LINK + " " +
                "ORDER BY " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME + " ASC";
        cursor = database.rawQuery(query, null);
        result = DatabaseUtils.dumpCursorToString(cursor);
        Log.d(TAG, "Found links: " + result);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LinkItem item = serializeLink(cursor, circle);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        query = "SELECT * FROM " + DBHelper.TABLE_CIRCLE_ITEMS + " " +
                "JOIN " + DBHelper.TABLE_LISTS + " ON " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_ITEMID + "=" + DBHelper.TABLE_LISTS + "." + DBHelper.LIST_COLUMN_ID + " " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_CIRCLEID + "='" + circle.getId() + "' " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_TYPE + "=" + BoardItem.TYPE_LIST + " " +
                "ORDER BY " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME + " ASC";
        cursor = database.rawQuery(query, null);
        result = DatabaseUtils.dumpCursorToString(cursor);
        Log.d(TAG, "Found lists: " + result);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ListItem item = serializeList(cursor, circle);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        query = "SELECT * FROM " + DBHelper.TABLE_CIRCLE_ITEMS + " " +
                "JOIN " + DBHelper.TABLE_NOTES + " ON " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_ITEMID + "=" + DBHelper.TABLE_NOTES + "." + DBHelper.NOTE_COLUMN_ID + " " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_CIRCLEID + "='" + circle.getId() + "' " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_TYPE + "=" + BoardItem.TYPE_NOTE + " " +
                "ORDER BY " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME + " ASC";
        cursor = database.rawQuery(query, null);
        result = DatabaseUtils.dumpCursorToString(cursor);
        Log.d(TAG, "Found notes: " + result);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            NoteItem item = serializeNote(cursor, circle);
            items.add(item);
            cursor.moveToNext();
        }
        cursor.close();

        return items;
    }

    public void setSyncStatus(final BoardItem item, final int action, final int status) {
        if (item != null) {
            run(new Runnable() {
                @Override
                public void run() {
                    if (!database.isOpen()) openDB();

                    try {
                        ContentValues values = new ContentValues();
                        values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, status);
                        int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID
                                + "='" + item.getId() + "'", null);
                        Log.d(TAG, "Updated item sync status to " + status + ", " + rows + " rows affected");

                        if (handler != null) {
                            handler.sendMessage(handler.obtainMessage(action, status, -1, item));
                        }
                    }
                    catch (SQLException ex) {
                        Log.e(TAG, ex.getMessage());
                    }
                }
            });
        }
    }

    /*************************************************
     * EVENT OPERATIONS
     *************************************************/

    public void createEventAsync(final Circle circle, final DateTime createdTime,
                                 final String id, final String name, final DateTime startTime, final DateTime endTime, final String placeId,
                             final Location location, final String note, final boolean sync) {
        final EventItem event = TextUtils.isEmpty(id) ? EventItem.createEvent(circle, createdTime, createdTime)
                : EventItem.createEvent(id, circle, createdTime, createdTime);
        event.setEventInfo(name, startTime, endTime, placeId, location, note);
        event.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));

        run(new Runnable() {
            @Override
            public void run() {
                createEventDB(circle, createdTime, event, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                // this 1000 ms delayed is set due to recyclerview animation bug where it needs some time
                // for animation to work
                if (handler != null && sync)
                    handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_EVENT_CREATED, event), 1000);

                // whether or not to sync with the server
                if (sync) requestCreateEvent(circle, event);
            }
        });
    }

    public void createEventDB(Circle circle, DateTime createdTime, EventItem event, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, event.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, event.getType());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            values.put(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME, createdTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, createdTime.getMillis());
            long insertId = database.insert(DBHelper.TABLE_CIRCLE_ITEMS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new item to circle (" + circle.getId() + ") with id = " + event.getId());

            values.clear();
            values.put(DBHelper.EVENT_COLUMN_ID, event.getId());
            values.put(DBHelper.EVENT_COLUMN_NAME, event.getName());
            if (event.getStartTime() != null) {
                values.put(DBHelper.EVENT_COLUMN_STARTTIME, event.getStartTime().getMillis());
            }
            if (event.getEndTime() != null) {
                values.put(DBHelper.EVENT_COLUMN_ENDTIME, event.getEndTime().getMillis());
            }
            values.put(DBHelper.EVENT_COLUMN_PLACE, event.getPlace());
            if (event.getLocation() != null) {
                values.put(DBHelper.EVENT_COLUMN_LATITUDE, event.getLocation().getLatitude());
                values.put(DBHelper.EVENT_COLUMN_LONGITUDE, event.getLocation().getLongitude());
            }
            else {
                values.put(DBHelper.EVENT_COLUMN_LATITUDE, -1.0);
                values.put(DBHelper.EVENT_COLUMN_LONGITUDE, -1.0);
            }
            values.put(DBHelper.EVENT_COLUMN_NOTE, event.getNote());
            insertId = database.insert(DBHelper.TABLE_EVENTS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new event successfully");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    private EventItem createEvent(Circle circle, DateTime createdTime, String id, String name, DateTime startTime, DateTime endTime, String placeId,
                                  Location location, String note) {
        createdTime = createdTime==null ? DateTime.now() : createdTime;
        final EventItem event = TextUtils.isEmpty(id) ? EventItem.createEvent(circle, createdTime, createdTime)
                : EventItem.createEvent(id, circle, createdTime, createdTime);
        event.setEventInfo(name, startTime, endTime, placeId, location, note);
        event.setSyncStatus(BoardItem.SYNC_COMPLETE);
        event.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));
        circle.addItem(event);

        createEventDB(circle, createdTime, event, BoardItem.SYNC_COMPLETE);

        return event;
    }

    public void requestCreateEvent(Circle circle, final EventItem event) {
        if (event != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_ITEM_ID, event.getId());
                body.put(Const.JSON_SERVER_ITEM_TYPE, BoardItem.TYPE_EVENT);
                body.put(Const.JSON_SERVER_TIME, event.getCreatedTime().getMillis());
                JSONObject info = new JSONObject();
                info.put(Const.JSON_SERVER_EVENT_NAME, event.getName());
                if (event.getStartTime() != null)
                    info.put(Const.JSON_SERVER_EVENT_START_TIME, event.getStartTime().getMillis());
                else
                    info.put(Const.JSON_SERVER_EVENT_START_TIME, JSONObject.NULL);
                if (event.getEndTime() != null)
                    info.put(Const.JSON_SERVER_EVENT_END_TIME, event.getEndTime().getMillis());
                else
                    info.put(Const.JSON_SERVER_EVENT_END_TIME, JSONObject.NULL);
                if (!TextUtils.isEmpty(event.getPlace()))
                    info.put(Const.JSON_SERVER_EVENT_PLACE_ID, event.getPlace());
                else
                    info.put(Const.JSON_SERVER_EVENT_PLACE_ID, JSONObject.NULL);
                JSONObject locationJson = new JSONObject();
                if (event.getLocation() != null) {
                    locationJson.put(Const.JSON_SERVER_LOCATION_LAT, event.getLocation().getLatitude());
                    locationJson.put(Const.JSON_SERVER_LOCATION_LONG, event.getLocation().getLongitude());
                }
                else {
                    locationJson.put(Const.JSON_SERVER_LOCATION_LAT, JSONObject.NULL);
                    locationJson.put(Const.JSON_SERVER_LOCATION_LONG, JSONObject.NULL);
                }
                info.put(Const.JSON_SERVER_LOCATION, locationJson);
                if (!TextUtils.isEmpty(event.getNote()))
                    info.put(Const.JSON_SERVER_EVENT_NOTE, event.getNote());
                else
                    info.put(Const.JSON_SERVER_EVENT_NOTE, JSONObject.NULL);
                body.put(Const.JSON_SERVER_INFO, info);

                httpClient.post("Create EventItem", String.format(Const.API_BOARD, circle.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(event, Const.MSG_EVENT_CREATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(event, Const.MSG_EVENT_CREATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(event, Const.MSG_EVENT_CREATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    public void updateEventAsync(final Circle circle, final DateTime updatedTime, String id, String name, DateTime startTime, DateTime endTime,
                                 String place, Location location, String note, final boolean sync) {
        List<BoardItem> events = circle.getItems();
        EventItem e = null;
        for (BoardItem item : events) {
            if (item.getId().equals(id) && item instanceof EventItem) {
                e = (EventItem) item;
                break;
            }
        }

        if (e != null) {
            final EventItem event = e;
            event.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));
            event.setName(name);
            event.setStartTime(startTime);
            event.setEndTime(endTime);
            event.setPlace(place);
            event.setLocation(location);
            event.setNote(note);

            run(new Runnable() {
                @Override
                public void run() {
                    updateEventDB(updatedTime, event, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                    if (handler != null)
                        handler.sendMessage(handler.obtainMessage(Const.MSG_EVENT_UPDATED, event));

                    // whether or not to sync this update with the server
                    if (sync) requestUpdateEvent(circle, event);
                }
            });
        }
    }

    private EventItem updateEvent(Circle circle, DateTime updatedTime, String id, String name, DateTime startTime, DateTime endTime,
                                  String place, Location location, String note) {
        List<BoardItem> events = circle.getItems();
        EventItem event = null;
        for (BoardItem item : events) {
            if (item.getId().equals(id) && item instanceof EventItem) {
                event = (EventItem) item;
                break;
            }
        }

        if (event != null) {
            updatedTime = updatedTime==null? DateTime.now() : updatedTime;
            event.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));
            event.setName(name);
            event.setStartTime(startTime);
            event.setEndTime(endTime);
            event.setPlace(place);
            event.setLocation(location);
            event.setNote(note);
            event.setSyncStatus(BoardItem.SYNC_COMPLETE);

            updateEventDB(updatedTime, event, BoardItem.SYNC_COMPLETE);
        }

        return event;
    }

    private void updateEventDB(DateTime updatedTime, EventItem event, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, updatedTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" +
                    event.getId() + "'", null);
            Log.d(TAG, "Updated item with id " + event.getId() + " with status " + syncStatus + ", " + rows + " row(s) affected");
            values.clear();

            values.put(DBHelper.EVENT_COLUMN_NAME, event.getName());
            if (event.getStartTime() != null) {
                values.put(DBHelper.EVENT_COLUMN_STARTTIME, event.getStartTime().getMillis());
            }
            if (event.getEndTime() != null) {
                values.put(DBHelper.EVENT_COLUMN_ENDTIME, event.getEndTime().getMillis());
            }
            values.put(DBHelper.EVENT_COLUMN_PLACE, event.getPlace());
            if (event.getLocation() != null) {
                values.put(DBHelper.EVENT_COLUMN_LATITUDE, event.getLocation().getLatitude());
                values.put(DBHelper.EVENT_COLUMN_LONGITUDE, event.getLocation().getLongitude());
            }
            else {
                values.put(DBHelper.EVENT_COLUMN_LATITUDE, -1.0);
                values.put(DBHelper.EVENT_COLUMN_LONGITUDE, -1.0);
            }
            values.put(DBHelper.EVENT_COLUMN_NOTE, event.getNote());
            rows = database.update(DBHelper.TABLE_EVENTS, values,
                    DBHelper.EVENT_COLUMN_ID + "='" + event.getId() + "'", null);
            Log.d(TAG, "Updated event id: " + event.getId() + ", name: " +
                    event.getName() + ", time: " + event.getStartTime() + ", place: " + event.getPlace() + ", " + rows + " row(s) affected");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    public void requestUpdateEvent(Circle circle, final EventItem event) {
        if (event != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_EVENT_NAME, event.getName());
                if (event.getStartTime() != null)
                    body.put(Const.JSON_SERVER_EVENT_START_TIME, event.getStartTime().getMillis());
                else
                    body.put(Const.JSON_SERVER_EVENT_START_TIME, JSONObject.NULL);
                if (event.getEndTime() != null)
                    body.put(Const.JSON_SERVER_EVENT_END_TIME, event.getEndTime().getMillis());
                else
                    body.put(Const.JSON_SERVER_EVENT_END_TIME, JSONObject.NULL);
                if (!TextUtils.isEmpty(event.getPlace()))
                    body.put(Const.JSON_SERVER_EVENT_PLACE_ID, event.getPlace());
                else
                    body.put(Const.JSON_SERVER_EVENT_PLACE_ID, JSONObject.NULL);
                JSONObject locationJson = new JSONObject();
                if (event.getLocation() != null) {
                    locationJson.put(Const.JSON_SERVER_LOCATION_LAT, event.getLocation().getLatitude());
                    locationJson.put(Const.JSON_SERVER_LOCATION_LONG, event.getLocation().getLongitude());
                }
                else {
                    locationJson.put(Const.JSON_SERVER_LOCATION_LAT, JSONObject.NULL);
                    locationJson.put(Const.JSON_SERVER_LOCATION_LONG, JSONObject.NULL);
                }
                body.put(Const.JSON_SERVER_LOCATION, locationJson);
                if (!TextUtils.isEmpty(event.getNote()))
                    body.put(Const.JSON_SERVER_EVENT_NOTE, event.getNote());
                else
                    body.put(Const.JSON_SERVER_EVENT_NOTE, JSONObject.NULL);

                httpClient.post("Update EventItem", String.format(Const.API_BOARD_ITEM, circle.getId(), event.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(event, Const.MSG_EVENT_UPDATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(event, Const.MSG_EVENT_UPDATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(event, Const.MSG_EVENT_UPDATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    public void deleteItemAsync(final String id, final Circle circle, final boolean sync) {
        run(new Runnable() {
            @Override
            public void run() {
                List<BoardItem> items = circle.getItems();
                BoardItem item = null;
                for (int index = 0; index < items.size(); index++) {
                    if (items.get(index).getId().equals(id)) {
                        item = items.get(index);
                        break;
                    }
                }

                if (item == null) return;
                if (item.getSyncStatus() == BoardItem.NO_SYNC || item.getSyncStatus() == BoardItem.SYNC_ERROR
                        || item.getSyncStatus() == BoardItem.SYNC_IN_PROGRESS) {
                    Log.d(TAG, "Deleting item because sync status is either none or error");
                    deleteItemDB(item);

                    if (handler != null) {
                        handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_DELETED_SUCCESS, -1, -1, item));
                    }
                }
                else {
                    Log.d(TAG, "Sending request to delete item " + id);
                    if (item.getType() == BoardItem.TYPE_FILE)
                        requestDeleteFileRemote(circle, (FileItem) item, CloudProvider.AMAZON_S3);
                    else if (item.getType() == BoardItem.TYPE_DRAWING)
                        requestDeleteDrawingRemote(circle, (DrawItem) item, CloudProvider.AMAZON_S3);
                    else requestDeleteItem(circle, item);
                }
            }
        });
    }

    public void deleteItemDB(BoardItem item) {
        if (item != null) {
            String id = item.getId();
            if (!database.isOpen()) openDB();

            int rows = database.delete(DBHelper.TABLE_CIRCLE_ITEMS, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" + id + "'", null);
            Log.d(TAG, "Deleted item id " + id + " from circle items table, affected " + rows + " row(s)");

            if (item.getType() == BoardItem.TYPE_EVENT) {
                rows = database.delete(DBHelper.TABLE_EVENTS, DBHelper.EVENT_COLUMN_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted event id " + id + " from event table, affected " + rows + " row(s)");
            }
            else if (item.getType() == BoardItem.TYPE_FILE) {
                rows = database.delete(DBHelper.TABLE_FILES, DBHelper.FILE_COLUMN_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted file id " + id + " from file table, affected " + rows + " row(s)");
            }
            else if (item.getType() == BoardItem.TYPE_DRAWING) {
                rows = database.delete(DBHelper.TABLE_DRAWINGS, DBHelper.DRAWING_COLUMN_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted drawing id " + id + " from drawing table, affected " + rows + " row(s)");
            }
            else if (item.getType() == BoardItem.TYPE_LINK) {
                rows = database.delete(DBHelper.TABLE_LINKS, DBHelper.LINK_COLUMN_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted link id " + id + " from link table, affected " + rows + " row(s)");
            }
            else if (item.getType() == BoardItem.TYPE_LIST) {
                rows = database.delete(DBHelper.TABLE_LISTS, DBHelper.LIST_COLUMN_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted list id " + id + " from link table, affected " + rows + " row(s)");

                rows = database.delete(DBHelper.TABLE_LIST_ITEMS, DBHelper.LISTITEM_COLUMN_LIST_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted all list items under list id " + id + " from list item table, affected " + rows + " row(s)");
            }
            else if (item.getType() == BoardItem.TYPE_NOTE) {
                rows = database.delete(DBHelper.TABLE_NOTES, DBHelper.NOTE_COLUMN_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted note id " + id + " from note table, affected " + rows + " row(s)");
            }
        }
    }

    public void requestDeleteItem(Circle circle, final BoardItem item) {
        if (item != null) {
            httpClient.delete("Delete Item", String.format(Const.API_BOARD_ITEM, circle.getId(), item.getId()), null,
                    new ResponseListener() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            run(new Runnable() {
                                @Override
                                public void run() {
                                    deleteItemDB(item);

                                    if (handler != null) {
                                        handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_DELETED_SUCCESS, -1, -1, item));
                                    }
                                }
                            });
                            handleNetworkSuccess();
                        }

                        @Override
                        public void onError(VolleyError error) {
                            if (handler != null) {
                                handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_DELETED_FAILED, item));
                                handleNetworkError(error);
                            }
                        }
                    });
        }
    }

    private EventItem serializeEvent(Cursor cursor, Circle circle) {
        int syncColumn = cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_SYNC);
        int createdTimeColumn = cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME);
        int updatedTimeColumn = cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME);

        int idColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_ID);
        int nameColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_NAME);
        int dateColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_STARTTIME);
        int endDateColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_ENDTIME);
        int placeColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_PLACE);
        int latColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_LATITUDE);
        int longColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_LONGITUDE);
        int noteColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_NOTE);

        int syncStatus = cursor.getInt(syncColumn);
        DateTime createdTime = new DateTime(cursor.getLong(createdTimeColumn));
        DateTime updatedTime = new DateTime(cursor.getLong(updatedTimeColumn));
        String id = cursor.getString(idColumn);
        String name = cursor.getString(nameColumn);
        DateTime time = null;
        DateTime endTime = null;
        if (cursor.getLong(dateColumn) != 0)
           time = new DateTime(cursor.getLong(dateColumn));
        if (cursor.getLong(endDateColumn) != 0)
            endTime = new DateTime(cursor.getLong(endDateColumn));
        String place = cursor.getString(placeColumn);
        Location location = null;
        if (cursor.getDouble(latColumn) != -1.0 && cursor.getDouble(longColumn) != -1.0) {
            location = new Location("");
            location.setLatitude(cursor.getDouble(latColumn));
            location.setLongitude(cursor.getDouble(longColumn));
        }
        String note = cursor.getString(noteColumn);

        EventItem event = EventItem.createEvent(id, circle, createdTime, updatedTime);
        event.setEventInfo(name, time, endTime, place,location, note);
        event.setSyncStatus(syncStatus);
        return event;
    }

    /*************************************************
     * FILE OPERATIONS
     *************************************************/
    public void postFilesAsync(final List<Uri> uriList, final Circle circle, final boolean sync) {
        run(new Runnable() {
            @Override
            public void run() {
                if (uriList != null && uriList.size() > 0) {
                    for (Uri uri : uriList) {
                        Cursor cursor = null;
                        try {
                            final FileItem item = FileItem.createFile(circle);
                            File file = FileUtils.getFile(context, uri);
                            if (file != null) {
                                item.setName(file.getName());
                                item.setSize(file.length());
                                item.setPath(file.getPath());
                                item.setMimetype(FileUtils.getMimeType(file));

                                //update db
                                createFileDB(circle, item.getCreatedTime(), item,
                                        sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                                // this 1000 ms delayed is set due to recyclerview animation bug where it needs some time
                                // for animation to work
                                if (handler != null && sync)
                                    handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_FILE_POSTED, item), 1000);

                                // whether or not to sync with the server
                                if (sync) {
                                    Log.d(TAG, "Proceed to uploading file to remote server for item " + item.getName());
                                    requestUploadFileRemote(circle, item, CloudProvider.AMAZON_S3);
                                }
                            }
                            else {
                                retrieveFile(uri, new FileDownloadListener() {
                                    @Override
                                    public void onDownloadStarted(String path) {}

                                    @Override
                                    public void onDownloadInProgress(String path, int progress) {}

                                    @Override
                                    public void onDownloadCompleted(String path) {
                                        File tempFile = new File(path);
                                        if (tempFile.exists()) {
                                            item.setName(tempFile.getName());
                                            item.setSize(tempFile.length());
                                            item.setPath(tempFile.getPath());
                                            item.setMimetype(FileUtils.getMimeType(tempFile));

                                            //update db
                                            createFileDB(circle, item.getCreatedTime(), item,
                                                    sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                                            // this 1000 ms delayed is set due to recyclerview animation bug where it needs some time
                                            // for animation to work
                                            if (handler != null && sync)
                                                handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_FILE_POSTED, item), 1000);

                                            // whether or not to sync with the server
                                            if (sync) {
                                                Log.d(TAG, "Proceed to uploading file to remote server for item " + item.getName());
                                                requestUploadFileRemote(circle, item, CloudProvider.AMAZON_S3);
                                            }
                                        }
                                        else {
                                            if (handler != null) {
                                                handler.sendMessage(handler.obtainMessage(
                                                        Const.MSG_SHOW_TOAST,
                                                        context.getResources().getString(R.string.notification_retrieve_file_failed)));
                                            }
                                        }
                                    }

                                    @Override
                                    public void onDownloadFailed(String error) {
                                        if (handler != null) {
                                            handler.sendMessage(handler.obtainMessage(
                                                    Const.MSG_SHOW_TOAST,
                                                    context.getResources().getString(R.string.notification_retrieve_file_failed)));
                                        }
                                    }
                                });
                            }
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                            Log.e(TAG, ex.getMessage());
                        }
                        finally {
                            if (cursor != null) cursor.close();
                        }
                    }
                }
            }
        });
    }

    public void retrieveFile(final Uri uri, final FileDownloadListener listener) throws FileNotFoundException {
        String mimeType = context.getContentResolver().getType(uri);
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            cursor.moveToFirst();
            final String name = cursor.getString(nameIndex);
            final Long size = cursor.getLong(sizeIndex);
            cursor.close();
            run(new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream in = context.getContentResolver().openInputStream(uri);
                        final File tempFile = new File(appState.getExternalFilesDir().getPath() + "/" + name);
                        listener.onDownloadStarted(tempFile.getPath());
                        OutputStream out = new FileOutputStream(tempFile);
                        byte[] buf = new byte[1024];
                        int len, count = 0;
                        if (in != null) {
                            while((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                                count += len;
                                listener.onDownloadInProgress(tempFile.getPath(),
                                        size == 0 ? 0 : (int) ((count / size) * 100));
                            }
                        }
                        out.close();
                        if (in != null) {
                            in.close();
                        }
                        listener.onDownloadCompleted(tempFile.getPath());
                    }
                    catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        listener.onDownloadFailed(e.getMessage());
                    }
                }
            });
        }
        else {
            listener.onDownloadFailed("File cannot be retrieved, please choose another file");
        }
    }

    private FileItem createFile(Circle circle, DateTime createdTime,
                                String id, String name, long size, String mimetype, String note, int provider) {
        createdTime = createdTime==null ? DateTime.now() : createdTime;
        final FileItem file = TextUtils.isEmpty(id) ? FileItem.createFile(circle)
                : FileItem.createFile(id, circle, null, createdTime, createdTime);
        file.setName(name);
        file.setSize(size);
        file.setMimetype(mimetype);
        file.setNote(note);
        file.setSyncStatus(BoardItem.SYNC_COMPLETE);
        circle.addItem(file);

        createFileDB(circle, createdTime, file, BoardItem.SYNC_COMPLETE);

        return file;
    }

    private void updateFile(Circle circle, DateTime updatedTime,
                            String id, String name, long size, String mimetype, String note, int provider) {
        List<BoardItem> items = circle.getItems();
        FileItem file = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof FileItem) {
                file = (FileItem) item;
                break;
            }
        }

        if (file != null) {
            updatedTime = updatedTime==null? DateTime.now() : updatedTime;
            file.setName(name);
            file.setSize(size);
            file.setMimetype(mimetype);
            file.setNote(note);
            file.setSyncStatus(BoardItem.SYNC_COMPLETE);

            updateFileDB(updatedTime, file, BoardItem.SYNC_COMPLETE);
        }
    }

    public void updateFilePath(Circle circle, String id, String path) {
        List<BoardItem> items = circle.getItems();
        FileItem file = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof FileItem) {
                file = (FileItem) item;
                break;
            }
        }

        if (file != null) {
            file.setPath(path);
            updateFileDB(file.getUpdatedTime(), file, BoardItem.SYNC_COMPLETE);

            if (handler != null)
                handler.sendMessage(handler.obtainMessage(Const.MSG_FILE_DOWNLOAD_COMPLETE, file));
        }
    }

    private void updateFileDB(DateTime updatedTime, FileItem file, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, updatedTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" +
                    file.getId() + "'", null);
            Log.d(TAG, "Updated item with id " + file.getId() + " with status " + syncStatus + ", " + rows + " row(s) affected");
            values.clear();

            values.put(DBHelper.FILE_COLUMN_NAME, file.getName());
            values.put(DBHelper.FILE_COLUMN_SIZE, file.getSize());
            values.put(DBHelper.FILE_COLUMN_PATH, file.getLocalCache()==null ? null : file.getLocalCache().getPath());
            values.put(DBHelper.FILE_COLUMN_MIMETYPE, file.getMimetype());
            values.put(DBHelper.FILE_COLUMN_NOTE, file.getNote());
            rows = database.update(DBHelper.TABLE_FILES, values,
                    DBHelper.FILE_COLUMN_ID + "='" + file.getId() + "'", null);
            Log.d(TAG, "Updated file id: " + file.getId() + ", name: " +
                    file.getName() + ", size: " + file.getSize() + ", mimetype: " + file.getMimetype() + ", " + rows + " row(s) affected");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    private void createFileDB(Circle circle, DateTime createdTime, FileItem item, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, item.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, item.getType());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            values.put(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME, createdTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, createdTime.getMillis());
            long insertId = database.insert(DBHelper.TABLE_CIRCLE_ITEMS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new item to circle (" + circle.getId() + ") with id = " + item.getId());

            values.clear();
            values.put(DBHelper.FILE_COLUMN_ID, item.getId());
            values.put(DBHelper.FILE_COLUMN_NAME, item.getName());
            values.put(DBHelper.FILE_COLUMN_SIZE, item.getSize());
            values.put(DBHelper.FILE_COLUMN_PATH, item.getLocalCache()==null ? null : item.getLocalCache().getPath());
            values.put(DBHelper.FILE_COLUMN_MIMETYPE, item.getMimetype());
            values.put(DBHelper.FILE_COLUMN_NOTE, item.getNote());
            insertId = database.insert(DBHelper.TABLE_FILES, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new file successfully");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    private FileItem serializeFile(Cursor cursor, Circle circle) {
        int syncStatus = cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_SYNC));
        DateTime created = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME)));
        DateTime updated = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME)));
        String id = cursor.getString(cursor.getColumnIndex(DBHelper.FILE_COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndex(DBHelper.FILE_COLUMN_NAME));
        long size = cursor.getLong(cursor.getColumnIndex(DBHelper.FILE_COLUMN_SIZE));
        String path = cursor.getString(cursor.getColumnIndex(DBHelper.FILE_COLUMN_PATH));
        String mimetype = cursor.getString(cursor.getColumnIndex(DBHelper.FILE_COLUMN_MIMETYPE));
        String note = cursor.getString(cursor.getColumnIndex(DBHelper.FILE_COLUMN_NOTE));

        FileItem file = FileItem.createFile(id, circle, path, created, updated);
        file.setName(name);
        file.setSize(size);
        file.setMimetype(mimetype);
        if (!TextUtils.isEmpty(path)) file.setPath(path);
        file.setNote(note);
        file.setSyncStatus(syncStatus);
        return file;
    }

    public void requestUploadFileRemote(final Circle circle, final FileItem file, final CloudProvider provider) {
        run(new Runnable() {
            @Override
            public void run() {
                fileTransfer.upload(provider, circle, file);
            }
        });
    }

    public void requestDeleteFileRemote(final Circle circle, final FileItem file, final CloudProvider provider) {
        run(new Runnable() {
            @Override
            public void run() {
                fileTransfer.delete(provider, circle, file);
            }
        });
    }

    public void requestDownloadFileRemote(final Circle circle, final FileItem file, final CloudProvider provider) {
        run(new Runnable() {
            @Override
            public void run() {
                fileTransfer.download(provider, circle, file);
            }
        });
    }

    public void requestPostFile(final Circle circle, final FileItem file, final CloudProvider provider) {
        try {
            JSONObject info = new JSONObject()
                    .put(Const.JSON_SERVER_FILE_NAME, file.getName())
                    .put(Const.JSON_SERVER_FILE_SIZE, file.getSize())
                    .put(Const.JSON_SERVER_FILE_MIMETYPE, file.getMimetype())
                    .put(Const.JSON_SERVER_FILE_NOTE, file.getNote())
                    .put(Const.JSON_SERVER_FILE_PROVIDER, provider.getId());
            JSONObject body = new JSONObject()
                    .put(Const.JSON_SERVER_ITEM_ID, file.getId())
                    .put(Const.JSON_SERVER_ITEM_TYPE, BoardItem.TYPE_FILE)
                    .put(Const.JSON_SERVER_TIME, file.getCreatedTime().getMillis())
                    .put(Const.JSON_SERVER_INFO, info);

            httpClient.post("Post File", String.format(Const.API_BOARD, circle.getId()), body,
                    new ResponseListener() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            setSyncStatus(file, Const.MSG_FILE_POST_SUCCESS, BoardItem.SYNC_COMPLETE);
                            handleNetworkSuccess();
                        }

                        @Override
                        public void onError(VolleyError error) {
                            setSyncStatus(file, Const.MSG_FILE_POST_FAILED, BoardItem.SYNC_ERROR);

                            // send delete request to s3 because sync has failed
                            requestDeleteFileRemote(circle, file, provider);

                            handleNetworkError(error);
                        }
                    });
        }
        catch (Exception ex) {
            ex.printStackTrace();
            setSyncStatus(file, Const.MSG_FILE_POST_FAILED, BoardItem.SYNC_ERROR);
        }
    }

    /*************************************************
     * DRAWING OPERATIONS
     *************************************************/
    private DrawItem createDrawing(Circle circle, DateTime createdTime,
                                   String id, String name) {
        createdTime = createdTime==null ? DateTime.now() : createdTime;
        final DrawItem drawItem = TextUtils.isEmpty(id) ? DrawItem.createDrawing(circle, createdTime, createdTime)
                : DrawItem.createDrawing(id, circle, createdTime, createdTime);
        drawItem.setName(name);
        drawItem.setSyncStatus(BoardItem.SYNC_COMPLETE);
        circle.addItem(drawItem);

        createDrawingDB(circle, createdTime, drawItem, BoardItem.SYNC_COMPLETE);

        return drawItem;
    }

    public void postDrawingAsync(final String id, final String name, final String path,
                                 final Circle circle, final DateTime time, final boolean sync) {
        run(new Runnable() {
            @Override
            public void run() {
                if (!TextUtils.isEmpty(id)) {
                    try {
                        DrawItem item = DrawItem.createDrawing(id, circle, time, time);
                        item.setPath(path);
                        item.setName(name);

                        //update db
                        createDrawingDB(circle, item.getCreatedTime(), item,
                                sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                        // this 1000 ms delayed is set due to recyclerview animation bug where it needs some time
                        // for animation to work
                        if (handler != null && sync)
                            handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_DRAWING_POSTED, item), 1000);

                        // whether or not to sync with the server
                        if (sync) {
                            Log.d(TAG, "Proceed to uploading drawing to remote server for item " + item.getName());
                            requestUploadDrawingRemote(circle, item, CloudProvider.AMAZON_S3);
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        Log.e(TAG, ex.getMessage());
                    }
                }
            }
        });
    }

    public void requestDeleteDrawingRemote(final Circle circle, final DrawItem item, final CloudProvider provider) {
        run(new Runnable() {
            @Override
            public void run() {
                fileTransfer.delete(provider, circle, item);
            }
        });
    }

    public void requestUploadDrawingRemote(final Circle circle, final DrawItem item, final CloudProvider provider) {
        run(new Runnable() {
            @Override
            public void run() {
                fileTransfer.upload(provider, circle, item);
            }
        });
    }

    public void updateDrawingAsync(final String id, final String name, final String path,
                                   final Circle circle, final DateTime time, final boolean sync) {
        List<BoardItem> items = circle.getItems();
        DrawItem e = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof DrawItem) {
                e = (DrawItem) item;
                break;
            }
        }

        if (e != null) {
            final DrawItem drawing = e;
            drawing.setPath(path);
            drawing.setUpdatedTime(time);
            drawing.setName(name);

            run(new Runnable() {
                @Override
                public void run() {
                    // update db
                    updateDrawingDB(time, drawing, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                    if (handler != null)
                        handler.sendMessage(handler.obtainMessage(Const.MSG_DRAWING_UPDATED, drawing));

                    // whether or not to sync this update with the server
                    if (sync) requestUploadDrawingRemote(circle, drawing, CloudProvider.AMAZON_S3);
                }
            });
        }
    }

    public void requestUpdateDrawing(Circle circle, DateTime updatedTime, final DrawItem item) {
        if (item != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_DRAWING_NAME, TextUtils.isEmpty(item.getName()) ? JSONObject.NULL : item.getName());

                httpClient.post("Update DrawItem", String.format(Const.API_BOARD_ITEM, circle.getId(), item.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(item, Const.MSG_DRAWING_POST_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(item, Const.MSG_DRAWING_POST_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(item, Const.MSG_DRAWING_POST_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    private void createDrawingDB(Circle circle, DateTime createdTime, DrawItem item, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, item.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, item.getType());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            values.put(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME, createdTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, createdTime.getMillis());
            long insertId = database.insert(DBHelper.TABLE_CIRCLE_ITEMS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new item to circle (" + circle.getId() + ") with id = " + item.getId());

            values.clear();
            values.put(DBHelper.DRAWING_COLUMN_ID, item.getId());
            values.put(DBHelper.DRAWING_COLUMN_NAME, item.getName());
            values.put(DBHelper.DRAWING_COLUMN_PATH, item.getLocalCache()==null ? null : item.getLocalCache().getPath());
            insertId = database.insert(DBHelper.TABLE_DRAWINGS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new drawing successfully");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    private void updateDrawingDB(DateTime updatedTime, DrawItem item, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, updatedTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" +
                    item.getId() + "'", null);
            Log.d(TAG, "Updated item with id " + item.getId() + " with status " + syncStatus + ", " + rows + " row(s) affected");
            values.clear();

            values.put(DBHelper.DRAWING_COLUMN_NAME, item.getName());
            values.put(DBHelper.DRAWING_COLUMN_PATH, item.getLocalCache()==null ? null : item.getLocalCache().getPath());
            rows = database.update(DBHelper.TABLE_DRAWINGS, values,
                    DBHelper.DRAWING_COLUMN_ID + "='" + item.getId() + "'", null);
            Log.d(TAG, "Updated drawing id: " + item.getId() + ", name: " +
                    item.getName() + ", " + rows + " row(s) affected");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    private DrawItem serializeDrawing(Cursor cursor, Circle circle) {
        int syncStatus = cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_SYNC));
        DateTime created = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME)));
        DateTime updated = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME)));
        String id = cursor.getString(cursor.getColumnIndex(DBHelper.DRAWING_COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndex(DBHelper.DRAWING_COLUMN_NAME));
        String path = cursor.getString(cursor.getColumnIndex(DBHelper.DRAWING_COLUMN_PATH));

        DrawItem item = DrawItem.createDrawing(id, circle, created, updated);
        item.setName(name);
        if (!TextUtils.isEmpty(path)) item.setPath(path);
        item.setSyncStatus(syncStatus);
        return item;
    }

    public void updateDrawingPath(Circle circle, String id, String path) {
        List<BoardItem> items = circle.getItems();
        DrawItem drawItem = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof DrawItem) {
                drawItem = (DrawItem) item;
                break;
            }
        }

        if (drawItem != null) {
            drawItem.setPath(path);
            updateDrawingDB(drawItem.getUpdatedTime(), drawItem, BoardItem.SYNC_COMPLETE);

            if (handler != null)
                handler.sendMessage(handler.obtainMessage(Const.MSG_DRAWING_DOWNLOAD_COMPLETE, drawItem));
        }
    }

    public void requestDownloadDrawingRemote(final Circle circle, final DrawItem drawItem, final CloudProvider provider) {
        run(new Runnable() {
            @Override
            public void run() {
                fileTransfer.download(provider, circle, drawItem);
            }
        });
    }

    /*************************************************
     * LINK OPERATIONS
     *************************************************/

    public void createLinkAsync(final Circle circle, final DateTime createdTime, String url, final boolean sync) {
        final LinkItem link = LinkItem.createLink(circle);
        link.setLinkInfo(url, null, null, null);
        link.setSyncStatus(sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);
        link.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));

        run(new Runnable() {
            @Override
            public void run() {
                createLinkDB(circle, createdTime, link, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                // this 1000 ms delayed is set due to recyclerview animation bug where it needs some time
                // for animation to work
                if (handler != null && sync)
                    handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_LINK_CREATED, link), 1000);

                // whether or not to sync with the server
                if (sync) requestCreateLink(circle, link);
            }
        });
    }

    private void requestCreateLink(final Circle circle, final LinkItem link) {
        if (link != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_ITEM_ID, link.getId());
                body.put(Const.JSON_SERVER_ITEM_TYPE, BoardItem.TYPE_LINK);
                body.put(Const.JSON_SERVER_TIME, link.getCreatedTime().getMillis());
                JSONObject info = new JSONObject();
                info.put(Const.JSON_SERVER_LINK_URL, link.getUrl());
                info.put(Const.JSON_SERVER_LINK_MAX_FETCH_PAGES, link.getMaxFetchPages());
                info.put(Const.JSON_SERVER_LINK_MAX_LINK_DEPTH, link.getMaxLinkDepth());
                body.put(Const.JSON_SERVER_INFO, info);

                httpClient.post("Create LinkItem", String.format(Const.API_BOARD, circle.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(link, Const.MSG_LINK_CREATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(link, Const.MSG_LINK_CREATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(link, Const.MSG_LINK_CREATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    private void createLinkDB(final Circle circle, DateTime createdTime, LinkItem link, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, link.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, link.getType());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            values.put(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME, createdTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, createdTime.getMillis());
            long insertId = database.insert(DBHelper.TABLE_CIRCLE_ITEMS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new item to circle (" + circle.getId() + ") with id = " + link.getId());

            values.clear();
            values.put(DBHelper.LINK_COLUMN_ID, link.getId());
            values.put(DBHelper.LINK_COLUMN_URL, link.getUrl());
            values.put(DBHelper.LINK_COLUMN_TITLE, link.getTitle());
            values.put(DBHelper.LINK_COLUMN_DESCRIPTION, link.getDescription());
            values.put(DBHelper.LINK_COLUMN_THUMBNAIL, link.getThumbnail());
            insertId = database.insert(DBHelper.TABLE_LINKS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new link successfully");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    private LinkItem serializeLink(Cursor cursor, Circle circle) {
        int syncStatus = cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_SYNC));
        DateTime created = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME)));
        DateTime updated = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME)));
        String id = cursor.getString(cursor.getColumnIndex(DBHelper.LINK_COLUMN_ID));
        String url = cursor.getString(cursor.getColumnIndex(DBHelper.LINK_COLUMN_URL));
        String title = cursor.getString(cursor.getColumnIndex(DBHelper.LINK_COLUMN_TITLE));
        String description = cursor.getString(cursor.getColumnIndex(DBHelper.LINK_COLUMN_DESCRIPTION));
        String thumbnail = cursor.getString(cursor.getColumnIndex(DBHelper.LINK_COLUMN_THUMBNAIL));

        LinkItem item = LinkItem.createLink(id, circle, created, updated);
        item.setLinkInfo(url, thumbnail, title, description);
        item.setSyncStatus(syncStatus);
        return item;
    }

    private LinkItem createLink(Circle circle, DateTime createdTime, String id, String url,
                                String thumbnail, String title, String description) {
        createdTime = createdTime==null ? DateTime.now() : createdTime;
        final LinkItem link = TextUtils.isEmpty(id) ? LinkItem.createLink(circle)
                : LinkItem.createLink(id, circle, createdTime, createdTime);
        link.setLinkInfo(url, thumbnail, title, description);
        link.setSyncStatus(BoardItem.SYNC_COMPLETE);
        link.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));
        circle.addItem(link);

        createLinkDB(circle, createdTime, link, BoardItem.SYNC_COMPLETE);

        return link;
    }

    public void updateLinkAsync(final Circle circle, final DateTime updatedTime, String id, String url, final boolean sync) {
        List<BoardItem> items = circle.getItems();
        LinkItem e = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof LinkItem) {
                e = (LinkItem) item;
                break;
            }
        }

        if (e != null) {
            final LinkItem link = e;
            link.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));
            link.setLinkInfo(url, null, null, null);

            run(new Runnable() {
                @Override
                public void run() {
                    updateLinkDB(updatedTime, link, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                    if (handler != null)
                        handler.sendMessage(handler.obtainMessage(Const.MSG_LINK_UPDATED, link));

                    // whether or not to sync this update with the server
                    if (sync) requestUpdateLink(circle, link);
                }
            });
        }
    }

    private void updateLinkDB(DateTime updatedTime, LinkItem link, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, updatedTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" +
                    link.getId() + "'", null);
            Log.d(TAG, "Updated item with id " + link.getId() + " with status " + syncStatus + ", " + rows + " row(s) affected");
            values.clear();

            values.put(DBHelper.LINK_COLUMN_URL, link.getUrl());
            values.put(DBHelper.LINK_COLUMN_TITLE, link.getTitle());
            values.put(DBHelper.LINK_COLUMN_DESCRIPTION, link.getDescription());
            values.put(DBHelper.LINK_COLUMN_THUMBNAIL, link.getThumbnail());
            rows = database.update(DBHelper.TABLE_LINKS, values,
                    DBHelper.LINK_COLUMN_ID + "='" + link.getId() + "'", null);
            Log.d(TAG, "Updated link id: " + link.getId() + ", url: " + link.getUrl() + ", " + rows + " row(s) affected");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    public void requestUpdateLink(Circle circle, final LinkItem link) {
        if (link != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_LINK_URL, link.getUrl());
                body.put(Const.JSON_SERVER_LINK_MAX_FETCH_PAGES, link.getMaxFetchPages());
                body.put(Const.JSON_SERVER_LINK_MAX_LINK_DEPTH, link.getMaxLinkDepth());

                httpClient.post("Update LinkItem", String.format(Const.API_BOARD_ITEM, circle.getId(), link.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(link, Const.MSG_LINK_UPDATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(link, Const.MSG_LINK_UPDATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(link, Const.MSG_LINK_UPDATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    private LinkItem updateLink(Circle circle, DateTime updatedTime, String id, String url,
                            String thumbnail, String title, String description) {
        List<BoardItem> items = circle.getItems();
        LinkItem link = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof LinkItem) {
                link = (LinkItem) item;
                break;
            }
        }

        if (link != null) {
            updatedTime = updatedTime==null? DateTime.now() : updatedTime;
            link.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));
            link.setLinkInfo(url, thumbnail, title, description);
            link.setSyncStatus(BoardItem.SYNC_COMPLETE);

            updateLinkDB(updatedTime, link, BoardItem.SYNC_COMPLETE);
        }

        return link;
    }

    /*************************************************
     * LIST ITEM
     *************************************************/

    public void createListAsync(final Circle circle, final DateTime createdTime, final ListItem list, final boolean sync) {
        list.setSyncStatus(sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);
        list.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));

        run(new Runnable() {
            @Override
            public void run() {
                createListDB(circle, createdTime, list, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                // this 1000 ms delayed is set due to recyclerview animation bug where it needs some time
                // for animation to work
                if (handler != null && sync)
                    handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_LIST_CREATED, list), 1000);

                // whether or not to sync with the server
                if (sync) requestCreateList(circle, list);
            }
        });
    }

    private void createList(Circle circle, DateTime createdTime, String id, String title, List<CheckedItem> checkedItems) {
        createdTime = createdTime==null ? DateTime.now() : createdTime;
        final ListItem item = TextUtils.isEmpty(id) ? ListItem.createList(circle)
                : ListItem.createList(id, circle, createdTime, createdTime);
        item.setSyncStatus(BoardItem.SYNC_COMPLETE);
        item.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));
        circle.addItem(item);

        createListDB(circle, createdTime, item, BoardItem.SYNC_COMPLETE);
    }

    private void updateList(Circle circle, DateTime updatedTime, String id, String title, List<CheckedItem> checkedItems) {
        List<BoardItem> items = circle.getItems();
        ListItem list = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof ListItem) {
                list = (ListItem) item;
                break;
            }
        }

        if (list != null) {
            updatedTime = updatedTime==null? DateTime.now() : updatedTime;
            list.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));
            list.setTitle(title);
            list.setItems(checkedItems);
            list.setSyncStatus(BoardItem.SYNC_COMPLETE);

            updateListDB(updatedTime, list, BoardItem.SYNC_COMPLETE);
        }
    }

    public void requestCreateList(final Circle circle, final ListItem list) {
        if (list != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_ITEM_ID, list.getId());
                body.put(Const.JSON_SERVER_ITEM_TYPE, BoardItem.TYPE_LIST);
                body.put(Const.JSON_SERVER_TIME, list.getCreatedTime().getMillis());
                JSONObject info = new JSONObject();
                info.put(Const.JSON_SERVER_LIST_TITLE, list.getTitle());

                JSONArray jsonArrayItems = new JSONArray();
                for (CheckedItem checkedItem : list.getItems()) {
                    JSONObject jsonItem = new JSONObject();
                    jsonItem.put(Const.JSON_SERVER_LISTITEM_STATE, checkedItem.getState());
                    jsonItem.put(Const.JSON_SERVER_LISTITEM_TEXT, checkedItem.getText());
                    jsonArrayItems.put(jsonItem);
                }
                info.put(Const.JSON_SERVER_LIST_ITEMS, jsonArrayItems);

                body.put(Const.JSON_SERVER_INFO, info);

                httpClient.post("Create ListItem", String.format(Const.API_BOARD, circle.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(list, Const.MSG_LIST_CREATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(list, Const.MSG_LIST_CREATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(list, Const.MSG_LIST_CREATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    public void requestUpdateList(final Circle circle, final ListItem list) {
        if (list != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_LIST_TITLE, list.getTitle());

                JSONArray jsonArrayItems = new JSONArray();
                for (CheckedItem item : list.getItems()) {
                    JSONObject jsonItem = new JSONObject();
                    jsonItem.put(Const.JSON_SERVER_LISTITEM_STATE, item.getState());
                    jsonItem.put(Const.JSON_SERVER_LISTITEM_TEXT, item.getText());
                    jsonArrayItems.put(jsonItem);
                }
                body.put(Const.JSON_SERVER_LIST_ITEMS, jsonArrayItems);

                httpClient.post("Update LinkItem", String.format(Const.API_BOARD_ITEM, circle.getId(), list.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(list, Const.MSG_LIST_UPDATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(list, Const.MSG_LIST_UPDATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(list, Const.MSG_LIST_UPDATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    private void createListDB(final Circle circle, DateTime createdTime, ListItem item, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, item.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, item.getType());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            values.put(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME, createdTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, createdTime.getMillis());
            long insertId = database.insert(DBHelper.TABLE_CIRCLE_ITEMS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new item to circle (" + circle.getId() + ") with id = " + item.getId());

            values.clear();
            values.put(DBHelper.LIST_COLUMN_ID, item.getId());
            values.put(DBHelper.LIST_COLUMN_TITLE, item.getTitle());
            insertId = database.insert(DBHelper.TABLE_LISTS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new list with title: " + item.getTitle());

            for (int i = 0; i < item.getItems().size(); i++) {
                values.clear();
                values.put(DBHelper.LISTITEM_COLUMN_LIST_ID, item.getId());
                values.put(DBHelper.LISTITEM_COLUMN_STATE, item.getItem(i).getState());
                values.put(DBHelper.LISTITEM_COLUMN_TEXT, item.getItem(i).getText());
                values.put(DBHelper.LISTITEM_COLUMN_RANK, i);
                insertId = database.insert(DBHelper.TABLE_LIST_ITEMS, null, values);
                Log.d(TAG, "[" + insertId + "] Inserted new list item: " + item.getItem(i).getText());
            }

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    private ListItem serializeList(Cursor cursor, Circle circle) {
        int syncStatus = cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_SYNC));
        DateTime created = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME)));
        DateTime updated = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME)));
        String id = cursor.getString(cursor.getColumnIndex(DBHelper.LIST_COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndex(DBHelper.LIST_COLUMN_TITLE));

        ListItem item = ListItem.createList(id, circle, created, updated);
        item.setTitle(title);
        item.setSyncStatus(syncStatus);

        String query = "SELECT * FROM " + DBHelper.TABLE_LIST_ITEMS + " WHERE " +
                DBHelper.TABLE_LIST_ITEMS + "." + DBHelper.LISTITEM_COLUMN_LIST_ID + "='" + id + "' " +
                "ORDER BY " + DBHelper.TABLE_LIST_ITEMS + "." + DBHelper.LISTITEM_COLUMN_RANK + " ASC";
        Cursor listItemCursor = database.rawQuery(query, null);
        listItemCursor.moveToFirst();
        while (!listItemCursor.isAfterLast()) {
            CheckedItem checkedItem = serializeListItem(listItemCursor);
            item.addItem(checkedItem);
            listItemCursor.moveToNext();
        }
        listItemCursor.close();

        return item;
    }

    private CheckedItem serializeListItem(Cursor cursor) {
        Log.d(TAG, "Starting serializing item ");
        DatabaseUtils.dumpCursorToString(cursor);
        int state = cursor.getInt(cursor.getColumnIndex(DBHelper.LISTITEM_COLUMN_STATE));
        String text = cursor.getString(cursor.getColumnIndex(DBHelper.LISTITEM_COLUMN_TEXT));

        return new CheckedItem(state, text);
    }

    public void updateListAsync(final Circle circle, final DateTime updatedTime, final ListItem item, final boolean sync) {
        item.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));

        run(new Runnable() {
            @Override
            public void run() {
                updateListDB(updatedTime, item, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                if (handler != null)
                    handler.sendMessage(handler.obtainMessage(Const.MSG_LIST_UPDATED, item));

                // whether or not to sync this update with the server
                if (sync) requestUpdateList(circle, item);
            }
        });
    }

    private void updateListDB(DateTime updatedTime, ListItem item, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, updatedTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" +
                    item.getId() + "'", null);
            Log.d(TAG, "Updated item with id " + item.getId() + " with status " + syncStatus + ", " + rows + " row(s) affected");
            values.clear();

            values.put(DBHelper.LIST_COLUMN_TITLE, item.getTitle());
            rows = database.update(DBHelper.TABLE_LISTS, values,
                    DBHelper.LIST_COLUMN_ID + "='" + item.getId() + "'", null);
            Log.d(TAG, "Updated list item with title " + item.getTitle() + ", " + rows + " row(s) affected");
            values.clear();

            database.delete(DBHelper.TABLE_LIST_ITEMS, DBHelper.LISTITEM_COLUMN_LIST_ID + "='" + item.getId() + "'", null);
            long insertId;
            for (int i = 0; i < item.getItems().size(); i++) {
                values.clear();
                values.put(DBHelper.LISTITEM_COLUMN_LIST_ID, item.getId());
                values.put(DBHelper.LISTITEM_COLUMN_STATE, item.getItem(i).getState());
                values.put(DBHelper.LISTITEM_COLUMN_TEXT, item.getItem(i).getText());
                values.put(DBHelper.LISTITEM_COLUMN_RANK, i);
                insertId = database.insert(DBHelper.TABLE_LIST_ITEMS, null, values);
                Log.d(TAG, "[" + insertId + "] Inserted new list item: " + item.getItem(i).getText());
            }

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    /*************************************************
     * NOTE ITEM
     *************************************************/

    public void createNoteAsync(final Circle circle, final DateTime createdTime, final NoteItem item, final boolean sync) {
        item.setSyncStatus(sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);
        item.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));

        run(new Runnable() {
            @Override
            public void run() {
                createNoteDB(circle, createdTime, item, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                // this 1000 ms delayed is set due to recyclerview animation bug where it needs some time
                // for animation to work
                if (handler != null && sync)
                    handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_NOTE_CREATED, item), 1000);

                // whether or not to sync with the server
                if (sync) requestCreateNote(circle, item);
            }
        });
    }

    private void createNote(Circle circle, DateTime createdTime, String id, String title, String text) {
        createdTime = createdTime==null ? DateTime.now() : createdTime;
        final NoteItem item = TextUtils.isEmpty(id) ? NoteItem.createNote(circle)
                : NoteItem.createNote(id, circle, createdTime, createdTime);
        item.setSyncStatus(BoardItem.SYNC_COMPLETE);
        item.setTitle(title);
        item.setText(text);
        item.setLastAction(new FeedAction(FeedAction.CREATE, appState.getActiveUser(), createdTime));
        circle.addItem(item);

        createNoteDB(circle, createdTime, item, BoardItem.SYNC_COMPLETE);
    }

    private void createNoteDB(Circle circle, DateTime createdTime, NoteItem item, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, item.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, item.getType());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            values.put(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME, createdTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, createdTime.getMillis());
            long insertId = database.insert(DBHelper.TABLE_CIRCLE_ITEMS, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new item to circle (" + circle.getId() + ") with id = " + item.getId());

            values.clear();
            values.put(DBHelper.NOTE_COLUMN_ID, item.getId());
            values.put(DBHelper.NOTE_COLUMN_TITLE, item.getTitle());
            values.put(DBHelper.NOTE_COLUMN_CONTENT, item.getText());
            insertId = database.insert(DBHelper.TABLE_NOTES, null, values);
            Log.d(TAG, "[" + insertId + "] Inserted new note with title: " + item.getTitle());

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    public void requestCreateNote(final Circle circle, final NoteItem item) {
        if (item != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_ITEM_ID, item.getId());
                body.put(Const.JSON_SERVER_ITEM_TYPE, BoardItem.TYPE_NOTE);
                body.put(Const.JSON_SERVER_TIME, item.getCreatedTime().getMillis());
                JSONObject info = new JSONObject();
                info.put(Const.JSON_SERVER_NOTE_TITLE, item.getTitle());
                info.put(Const.JSON_SERVER_NOTE_TEXT, item.getText());

                body.put(Const.JSON_SERVER_INFO, info);

                httpClient.post("Create NoteItem", String.format(Const.API_BOARD, circle.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(item, Const.MSG_NOTE_CREATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(item, Const.MSG_NOTE_CREATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(item, Const.MSG_NOTE_CREATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    public void updateNoteAsync(final Circle circle, final DateTime updatedTime, final NoteItem item, final boolean sync) {
        item.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));

        run(new Runnable() {
            @Override
            public void run() {
                updateNoteDB(updatedTime, item, sync ? BoardItem.SYNC_IN_PROGRESS : BoardItem.NO_SYNC);

                if (handler != null)
                    handler.sendMessage(handler.obtainMessage(Const.MSG_NOTE_UPDATED, item));

                // whether or not to sync this update with the server
                if (sync) requestUpdateNote(circle, item);
            }
        });
    }

    private NoteItem updateNote(Circle circle, DateTime updatedTime, String id, String title, String text) {
        List<BoardItem> items = circle.getItems();
        NoteItem note = null;
        for (BoardItem item : items) {
            if (item.getId().equals(id) && item instanceof NoteItem) {
                note = (NoteItem) item;
                break;
            }
        }

        if (note != null) {
            updatedTime = updatedTime==null? DateTime.now() : updatedTime;
            note.setLastAction(new FeedAction(FeedAction.EDITED, appState.getActiveUser(), updatedTime));
            note.setTitle(title);
            note.setText(text);
            note.setSyncStatus(BoardItem.SYNC_COMPLETE);

            updateNoteDB(updatedTime, note, BoardItem.SYNC_COMPLETE);
        }

        return note;
    }

    private void updateNoteDB(DateTime updatedTime, NoteItem item, int syncStatus) {
        if (!database.isOpen()) openDB();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, updatedTime.getMillis());
            values.put(DBHelper.CIRCLEITEM_COLUMN_SYNC, syncStatus);
            int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" +
                    item.getId() + "'", null);
            Log.d(TAG, "Updated item with id " + item.getId() + " with status " + syncStatus + ", " + rows + " row(s) affected");
            values.clear();

            values.put(DBHelper.NOTE_COLUMN_TITLE, item.getTitle());
            values.put(DBHelper.NOTE_COLUMN_CONTENT, item.getText());
            rows = database.update(DBHelper.TABLE_NOTES, values,
                    DBHelper.NOTE_COLUMN_ID + "='" + item.getId() + "'", null);
            Log.d(TAG, "Updated note id: " + item.getId() + rows + " row(s) affected");

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }
    }

    public void requestUpdateNote(final Circle circle, final NoteItem item) {
        if (item != null) {
            try {
                JSONObject body = new JSONObject();
                body.put(Const.JSON_SERVER_NOTE_TITLE, item.getTitle());
                body.put(Const.JSON_SERVER_NOTE_TEXT, item.getText());

                httpClient.post("Update NoteItem", String.format(Const.API_BOARD_ITEM, circle.getId(), item.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                setSyncStatus(item, Const.MSG_NOTE_UPDATED_SUCCESS, BoardItem.SYNC_COMPLETE);
                                handleNetworkSuccess();
                            }

                            @Override
                            public void onError(VolleyError error) {
                                setSyncStatus(item, Const.MSG_NOTE_UPDATED_FAILED, BoardItem.SYNC_ERROR);
                                handleNetworkError(error);
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                setSyncStatus(item, Const.MSG_NOTE_UPDATED_FAILED, BoardItem.SYNC_ERROR);
            }
        }
    }

    private NoteItem serializeNote(Cursor cursor, Circle circle) {
        int syncStatus = cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_SYNC));
        DateTime created = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_CREATED_TIME)));
        DateTime updated = new DateTime(cursor.getLong(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME)));
        String id = cursor.getString(cursor.getColumnIndex(DBHelper.NOTE_COLUMN_ID));
        String title = cursor.getString(cursor.getColumnIndex(DBHelper.NOTE_COLUMN_TITLE));
        String text = cursor.getString(cursor.getColumnIndex(DBHelper.NOTE_COLUMN_CONTENT));

        NoteItem item = NoteItem.createNote(id, circle, created, updated);
        item.setTitle(title);
        item.setText(text);
        item.setSyncStatus(syncStatus);

        return item;
    }

    /*************************************************
     * XMPP MESSAGE HANDLER
     *************************************************/

    public void sendUpstreamMessage(final BaseChatMessage message) {
        if (message != null) {
            run(new Runnable() {
                @Override
                public void run() {
                    try {
                        String senderId = context.getResources().getString(R.string.gcm_senderId);
                        String domain = context.getResources().getString(R.string.gcm_ccs_domain);
                        Bundle data = new Bundle();

                        // save in DB

                        data.putString(Const.JSON_SERVER_MESSAGE_ID, message.getId());
                        data.putString(Const.JSON_SERVER_MESSAGE_TYPE, message.getType());
                        data.putString(Const.JSON_SERVER_SENDER, appState.getActiveUser().getId());
                        data.putString(Const.JSON_SERVER_CIRCLEID, appState.getActiveCircle().getId());
                        data.putString(Const.JSON_SERVER_MESSAGE, message.getContent());
                        gcm.send(senderId + "@" + domain, message.getId(), data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());
                    }
                }
            });
        }
    }

    /*************************************************
     * DISCOVER ITEMS
     *************************************************/
    public void requestMovies() {
        httpClient.get("Get Movies", Const.API_GET_MOVIES,
                new ResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        handleNetworkSuccess();

                        final JSONObject respJson = response;
                        run(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    List<Movie> movies = new ArrayList<>();
                                    int type = 0;
                                    if (respJson != null) {
                                        JSONArray itemsJson = respJson.getJSONArray(Const.JSON_SERVER_ITEMS);
                                        for (int i = 0; i < itemsJson.length(); i++) {
                                            JSONObject itemJson = itemsJson.getJSONObject(i);
                                            type = itemJson.getInt(Const.JSON_SERVER_ITEM_TYPE);
                                            if (type == DiscoverItem.TYPE_MOVIE) {
                                                JSONArray ratingsJson = itemJson.getJSONArray(Const.JSON_SERVER_RATINGS);
                                                List<WatchableRating> ratings = new ArrayList<>();
                                                for (int j = 0; j < ratingsJson.length(); j++) {
                                                    WatchableRating rating = new WatchableRating();
                                                    JSONObject ratingJson = ratingsJson.getJSONObject(j);
                                                    rating.score = ratingJson.getDouble(Const.JSON_SERVER_RATING_SCORE);
                                                    rating.source = ratingJson.getString(Const.JSON_SERVER_RATING_SOURCE);
                                                    ratings.add(rating);
                                                }
                                                JSONArray imagesJson = itemJson.getJSONArray(Const.JSON_SERVER_IMAGES);
                                                List<WatchableImage> images = new ArrayList<>();
                                                for (int j = 0; j < imagesJson.length(); j++) {
                                                    WatchableImage image = new WatchableImage();
                                                    JSONObject imageJson = imagesJson.getJSONObject(j);
                                                    image.type = imageJson.getInt(Const.JSON_SERVER_IMAGE_TYPE);
                                                    image.thumbnail = imageJson.getString(Const.JSON_SERVER_IMAGE_THUMBNAIL);
                                                    image.url = imageJson.getString(Const.JSON_SERVER_IMAGE_URL);
                                                    images.add(image);
                                                }
                                                JSONArray videosJson = itemJson.getJSONArray(Const.JSON_SERVER_VIDEOS);
                                                List<WatchableVideo> videos = new ArrayList<>();
                                                for (int j = 0; j < videosJson.length(); j++) {
                                                    WatchableVideo video = new WatchableVideo();
                                                    JSONObject videoJson = videosJson.getJSONObject(j);
                                                    video.type = videoJson.getInt(Const.JSON_SERVER_VIDEO_TYPE);
                                                    video.lang = videoJson.getString(Const.JSON_SERVER_VIDEO_LANG);
                                                    video.source = videoJson.getString(Const.JSON_SERVER_VIDEO_SOURCE);
                                                    video.url = videoJson.getString(Const.JSON_SERVER_VIDEO_URL);
                                                    videos.add(video);
                                                }
                                                JSONArray feedsJson = itemJson.getJSONArray(Const.JSON_SERVER_FEEDS);
                                                List<WatchableFeed> feeds = new ArrayList<>();
                                                for (int j = 0; j < feedsJson.length(); j++) {
                                                    WatchableFeed feed = new WatchableFeed();
                                                    JSONObject feedJson = feedsJson.getJSONObject(j);
                                                    feed.name = feedJson.getString(Const.JSON_SERVER_FEED_NAME);
                                                    feed.source = feedJson.getString(Const.JSON_SERVER_FEED_SOURCE);
                                                    feeds.add(feed);
                                                }

                                                Movie movie = new Movie();
                                                movie.setId(itemJson.getString(Const.JSON_SERVER_ITEM_ID))
                                                        .setUpdatedTime(null)
                                                        .setCreatedTime(null)
                                                        .setTitle(itemJson.getString(Const.JSON_SERVER_TITLE))
                                                        .setSummary(itemJson.getString(Const.JSON_SERVER_SUMMARY))
                                                        .setGenre(itemJson.getString(Const.JSON_SERVER_GENRE))
                                                        .setLang(itemJson.getString(Const.JSON_SERVER_LANG))
                                                        .setReleaseDate(new DateTime(itemJson.getLong(Const.JSON_SERVER_RELEASE_DATE)))
                                                        .setDirector(itemJson.getString(Const.JSON_SERVER_DIRECTOR))
                                                        .setCast(itemJson.getString(Const.JSON_SERVER_CAST))
                                                        .setRatings(ratings)
                                                        .setImages(images)
                                                        .setVideos(videos)
                                                        .setFeeds(feeds);
                                                movies.add(movie);
                                            }
                                        }
                                    }

                                    if (handler != null)
                                        handler.sendMessage(
                                                handler.obtainMessage(Const.MSG_DISCOVER_ITEM, DiscoverItem.TYPE_MOVIE, 0, movies));
                                }
                                catch (Exception ex) {
                                    ex.printStackTrace();
                                    if (handler != null)
                                        handler.sendMessage(
                                                handler.obtainMessage(Const.MSG_DISCOVER_ITEM, DiscoverItem.TYPE_MOVIE, -1, null));
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(VolleyError error) {
                        if (handler != null) {
                            handler.sendMessage(handler.obtainMessage(Const.MSG_DISCOVER_ITEM, DiscoverItem.TYPE_MOVIE, -1, null));
                            handleNetworkError(error);
                        }
                    }
                }
        );
    }
}