package com.abborg.glom.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.interfaces.ResponseListener;
import com.abborg.glom.model.BoardItem;
import com.abborg.glom.model.Circle;
import com.abborg.glom.model.CircleInfo;
import com.abborg.glom.model.Event;
import com.abborg.glom.model.FeedAction;
import com.abborg.glom.model.User;
import com.abborg.glom.utils.RequestHandler;
import com.android.volley.VolleyError;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that wraps around model to perform CRUD operations on database and 
 * make necessary network operations
 *
 * Created by Jitrapon Tiachunpun on 22/9/58.
 */
public class DataUpdater {

    private static final String TAG = "DATA PROVIDER";
    private Context context;

    /* Currently active user */
    private User activeUser;

    /* Database stuff */
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private String[] circleColumns = { DBHelper.CIRCLE_COLUMN_ID,
            DBHelper.CIRCLE_COLUMN_NAME, DBHelper.CIRCLE_COLUMN_BROADCAST_LOCATION };
    private String[] userColumns = { DBHelper.USER_COLUMN_ID, DBHelper.USER_COLUMN_NAME,
            DBHelper.USER_COLUMN_AVATAR};
    private String[] userCircleColumns = { DBHelper.USERCIRCLE_COLUMN_USER_ID, DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID,
            DBHelper.USERCIRCLE_COLUMN_LATITUDE, DBHelper.USERCIRCLE_COLUMN_LONGITUDE };

    /* Handler from main UI thread to pass messages back to main thread */
    private Handler handler;

    /* Global app states */
    private AppState appState;

    /* Executor service thread pool */
    private final ExecutorService threadPool;

    /* Determines the type of app start */
    public enum AppStart {
        FIRST_TIME, FIRST_TIME_VERSION, NORMAL;
    }

    /* The app version code (not the name) used on the last start of the app */
    private static final String LAST_APP_VERSION = "last_app_version";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public void run(Runnable runnable) {
        threadPool.submit(runnable);
    }

    public static void init(final AppState appState, final Context context, final Handler handler) {
        final DataUpdater instance = new DataUpdater(appState, context, handler);
        instance.run(new Runnable() {
            @Override
            public void run() {
                instance.dbHelper = new DBHelper(context);
                instance.open();

                // initialize the activeUser info
                // if activeUser is not there, FIXME sign in again
                instance.activeUser = instance.getActiveUser(Const.TEST_USER_ID);
                if (instance.activeUser == null) {
                    instance.activeUser = instance.createUser(Const.TEST_USER_ID);
                }

                // determine if the user has launched the app before and what version
                AppStart appStart = instance.checkAppStart();
                switch (appStart) {
                    case NORMAL:
                        Log.d("INIT", "App has launched normally, version is the same");
                        break;
                    case FIRST_TIME_VERSION:
                        Log.d("INIT", "App has been upgraded! Version is different");
                        break;
                    case FIRST_TIME:
                        Log.d("INIT", "App has not been launched before, resetting the state to default");
                        instance.resetCircles();
                        instance.createCircle(
                                context.getResources().getString(R.string.friends_circle_title), null,
                                Const.TEST_CIRCLE_ID
                        );
                        break;
                    default:

                }

                // finally before finishing this async task, set the appropriate fields in the AppState
                List<CircleInfo> circleInfoList = instance.getCirclesInfo();
                Circle circle = instance.getCircleById(Const.TEST_CIRCLE_ID);
                appState.setActiveUser(instance.activeUser);
                appState.setCircleInfos(circleInfoList);
                appState.setActiveCircle(circle);
                appState.setDataUpdater(instance);

                if (handler != null) {
                    handler.sendEmptyMessage(Const.MSG_INIT_SUCCESS);
                }
            }
        });
    }

    private DataUpdater(AppState appState, Context context, Handler handler) {
        this.context = context;
        this.appState = appState;
        this.handler = handler;
        threadPool = Executors.newCachedThreadPool();
    }

    //FIXME this is a debug-convenience method to create a user
    private User createUser(String id) {
        Location location = new Location("");
        location.setLatitude(0);
        location.setLongitude(0);
        User user = new User("", id, location);
        user.setAvatar("");

        List<Integer> userPerm = new ArrayList<>();
        userPerm.add(User.MEDIA_IMAGE_RECEIVE);
        userPerm.add(User.MEDIA_AUDIO_RECEIVE);
        userPerm.add(User.MEDIA_VIDEO_RECEIVE);
        userPerm.add(User.ALARM_RECEIVE);
        userPerm.add(User.NOTE_RECEIVE);
        userPerm.add(User.LOCATION_REQUEST_RECEIVE);
        userPerm.add(User.CREATE_EVENT);
        user.setUserPermission(userPerm);
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
    public AppStart checkAppStart() {
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

    public AppStart checkAppStart(int currentVersionCode, int lastVersionCode) {
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

    public void open() throws SQLException {
        Log.d(TAG, "Opening database");
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        Log.d(TAG, "Closing database");
        dbHelper.close();
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
        Circle circle = Circle.createCircle(name, activeUser);
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
        Circle circle = Circle.createCircle(null, activeUser);
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

    /*************************************************
     * USER OPERATIONS
     *************************************************/

    public User getActiveUser(String id) {
        User user = null;
        Cursor cursor = database.query(DBHelper.TABLE_USERS, null, DBHelper.USER_COLUMN_ID + " = '" + id + "'", null, null, null ,null);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_NAME));
            String userId = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_ID));
            String avatar = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_AVATAR));
            user = new User(name, userId, null);
            user.setAvatar(avatar);

            List<Integer> userPerm = new ArrayList<>();
            userPerm.add(User.MEDIA_IMAGE_RECEIVE);
            userPerm.add(User.MEDIA_AUDIO_RECEIVE);
            userPerm.add(User.MEDIA_VIDEO_RECEIVE);
            userPerm.add(User.ALARM_RECEIVE);
            userPerm.add(User.NOTE_RECEIVE);
            userPerm.add(User.LOCATION_REQUEST_RECEIVE);
            userPerm.add(User.CREATE_EVENT);
            user.setUserPermission(userPerm);
        }
        cursor.close();

        return user;
    }

    public void addUsersToCircle(Circle circle, List<User> users) {
        if (circle != null && users != null) {
            database.beginTransaction();

            try {
                ContentValues values = new ContentValues();
                for (User user : users) {
                    user.setCurrentCircle(circle);

                    values.put(DBHelper.USER_COLUMN_ID, user.getId());
                    values.put(DBHelper.USER_COLUMN_NAME, user.getName());
                    values.put(DBHelper.USER_COLUMN_AVATAR, user.getAvatar());
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

    public void modifyUsersInCircle(Circle circle, List<User> users) {
        if (circle != null && users != null) {
            database.beginTransaction();

            try {
                ContentValues values = new ContentValues();
                for (User user : users) {
                    user.setCurrentCircle(circle);

                    values.put(DBHelper.USER_COLUMN_NAME, user.getName());
                    values.put(DBHelper.USER_COLUMN_AVATAR, user.getAvatar());
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

    public void removeUsersFromCircle(Circle circle, List<User> users) {
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
        String selectColumns = DBHelper.USER_COLUMN_ID + "," + DBHelper.USER_COLUMN_NAME + "," + DBHelper.USER_COLUMN_AVATAR + "," +
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
        RequestHandler.getInstance(context).get("Get Users", String.format(Const.API_GET_USERS, circle.getId()),
                new ResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
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
                                            }
                                            user.setName(name);
                                            user.setAvatar(avatar);

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
                                            user = new User(name, id, location);
                                            user.setDirty(false);
                                            if (user.getAvatar() == null || !user.getAvatar().equals(avatar))
                                                user.setAvatar(avatar);

                                            circle.addUser(user);

                                            // add to the list of ones to be added
                                            toAdd.add(user);
                                        }
                                    }
                                }

                                if (!toModify.isEmpty()) modifyUsersInCircle(circle, toModify);
                                if (!toAdd.isEmpty()) addUsersToCircle(circle, toAdd);
                                for (Iterator<User> iter = circle.getUsers().iterator(); iter.hasNext(); ) {
                                    User user = iter.next();
                                    if (user.isDirty()) {
                                        toDelete.add(user);
                                        iter.remove();
                                    }
                                }
                                if (!toDelete.isEmpty()) removeUsersFromCircle(circle, toDelete);
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

                    @Override
                    public void onError(VolleyError error) {
                        RequestHandler.getInstance(context).handleError(error);
                    }
                });
    }

    private User serializeUser(Cursor cursor, Circle circle) {
        User user = new User(null, null, null);
        user.setId(cursor.getString(0));
        user.setName(cursor.getString(1));
        user.setAvatar(cursor.getString(2));

        Location location = new Location("");
        location.setLatitude(cursor.getDouble(3));
        location.setLongitude(cursor.getDouble(4));
        user.setLocation(location);

        //TODO set all user permission to receive everything
        List<Integer> userPerm = new ArrayList<>();
        userPerm.add(User.MEDIA_IMAGE_RECEIVE);
        userPerm.add(User.MEDIA_AUDIO_RECEIVE);
        userPerm.add(User.MEDIA_VIDEO_RECEIVE);
        userPerm.add(User.ALARM_RECEIVE);
        userPerm.add(User.NOTE_RECEIVE);
        userPerm.add(User.LOCATION_REQUEST_RECEIVE);
        if (activeUser.getId().equals(user.getId())) {
            userPerm.add(User.CREATE_EVENT);
//            userPerm.add(User.CREATE_TODO);
//             userPerm.add(User.CREATE_CALENDAR);
//            userPerm.add(User.INVITE_GAME);
        }
//        userPerm.add(User.POST_LINK);
//        userPerm.add(User.SHOUT_RECEIVE);
//        userPerm.add(User.SECRET_MESSAGE);
//        userPerm.add(User.SONG_SNIPPET_RECEIVE);
//        userPerm.add(User.POLL_RECEIVE);
        user.setUserPermission(userPerm);
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
                    if (userId.equals(appState.getActiveUser().getId())) continue;

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
                        updateUserLocation(userId, circleId, location.getLatitude(), location.getLongitude());

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

    public void updateUserLocation(String userId, String circleId, Double lat, Double lng) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.USERCIRCLE_COLUMN_LATITUDE, lat);
        values.put(DBHelper.USERCIRCLE_COLUMN_LONGITUDE, lng);
        int rowAffected = database.update(DBHelper.TABLE_USER_CIRCLE, values,
                DBHelper.USERCIRCLE_COLUMN_USER_ID + "='" + userId + "' AND " +
                        DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "='" + circleId + "'", null);
        Log.d(TAG, "Updated " + rowAffected + " row(s) in " + DBHelper.TABLE_USER_CIRCLE);
    }

    public List<User> getLocationUpdates(Intent intent, Circle circle) {
        List<User> userList = new ArrayList<>();
        String userJson = intent.getStringExtra(context.getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_USERS));
        String circleId = intent.getStringExtra(context.getResources().getString(R.string.EXTRA_RECEIVE_LOCATION_CIRCLE_ID));

        try {
            // we don't update anything in-memory if this is not the current circle
            if (!circleId.equals(circle.getId())) return null;

            JSONArray users = new JSONArray(userJson);

            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);

                // verify that each user in the JSON belongs to this circle
                // don't update if it's the user's own location
                for (User s : circle.getUsers()) {
                    String userId = user.getString(Const.JSON_SERVER_USERID);
                    if (userId.equals(appState.getActiveUser().getId())) {
                        Log.d(TAG, "Skipping updating user's own location");
                        continue;
                    }
                    if (userId.equals(s.getId())) {
                        JSONObject locationJson = user.getJSONObject(Const.JSON_SERVER_LOCATION);
                        Location location = new Location("");
                        location.setLatitude(locationJson.getDouble(Const.JSON_SERVER_LOCATION_LAT));
                        location.setLongitude(locationJson.getDouble(Const.JSON_SERVER_LOCATION_LONG));
                        userList.add(new User(null, userId, location));
                        s.setLocation(location);
                    }
                }
            }
        }
        catch (JSONException ex) {
            Log.e(TAG, ex.getMessage());
        }

        return userList;
    }

    /*************************************************
     * CIRCLE ITEMS OPERATIONS
     *************************************************/
    public void requestBoardItems(final Circle circle) {
        if (circle != null) {
            RequestHandler.getInstance(context).get("Get Board", String.format(Const.API_BOARD, circle.getId()),
                new ResponseListener() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        final JSONObject respJson = response;
                        run(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (respJson != null) {
                                        JSONArray jsonArray = respJson.getJSONArray(Const.JSON_SERVER_ITEMS);
                                        int numItem = jsonArray.length();
                                        List<BoardItem> items = circle.getItems();
                                        if (items != null) {
                                            for (BoardItem item : items) {
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
                                                        case BoardItem.TYPE_EVENT:
                                                            String name = info.getString(Const.JSON_SERVER_EVENT_NAME);
                                                            long start = info.optLong(Const.JSON_SERVER_EVENT_START_TIME);
                                                            long end = info.optLong(Const.JSON_SERVER_EVENT_END_TIME);
                                                            String placeId = info.getString(Const.JSON_SERVER_EVENT_PLACE_ID);
                                                            JSONObject locationJson = info.getJSONObject(Const.JSON_SERVER_LOCATION);
                                                            double lat = locationJson.optDouble(Const.JSON_SERVER_LOCATION_LAT, -1);
                                                            double lng = locationJson.optDouble(Const.JSON_SERVER_LOCATION_LONG, -1);
                                                            String note = info.getString(Const.JSON_SERVER_EVENT_NOTE);

                                                            name = name.equals("null") ? null : name;
                                                            DateTime startTime = null;
                                                            DateTime endTime = null;
                                                            if (start != 0L)
                                                                startTime = new DateTime(start);
                                                            if (end != 0L) endTime = new DateTime(end);
                                                            placeId = placeId.equals("null") ? null : placeId;
                                                            Location location = null;
                                                            if (lat != -1 && lng != -1) {
                                                                location = new Location("");
                                                                location.setLatitude(lat);
                                                                location.setLongitude(lng);
                                                            }
                                                            note = note.equals("null") ? null : note;

                                                            Event event = null;

                                                            if (items != null) {
                                                                for (BoardItem item : items) {
                                                                    if (item.getId().equals(id) && item.getType() == BoardItem.TYPE_EVENT) {
                                                                        event = (Event) item;
                                                                    }
                                                                }
                                                                if (event == null) {
                                                                    DateTime createdTime = createdMillis==null ? null : new DateTime(createdMillis);
                                                                    createEvent(circle, createdTime, id, name, startTime, endTime, placeId, location,
                                                                            note, false);
                                                                }
                                                                else {
                                                                    DateTime updatedTime = updatedMillis==null ? null : new DateTime(updatedMillis);
                                                                    updateEvent(circle, updatedTime, id, name, startTime, endTime,
                                                                            placeId, location, note, false);
                                                                    event.setDirty(false);
                                                                }
                                                            }
                                                            break;
                                                        default:
                                                            break;
                                                    }
                                                }
                                            }
                                        }
                                        Log.d(TAG, "Found " + numItem + " items");

                                        // remove dirty BoardItems
                                        Iterator<BoardItem> iterator = items.iterator();
                                        while (iterator.hasNext()) {
                                            BoardItem item = iterator.next();
                                            if (item.isDirty()) {
                                                iterator.remove();
                                                deleteItem(item);
                                            }
                                        }

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
                        RequestHandler.getInstance(context).handleError(error);
                        if (handler != null) handler.sendEmptyMessage(Const.MSG_GET_ITEMS);
                    }
                }
            );
        }
    }

    public List<BoardItem> getCircleItems(Circle circle) {
        List<BoardItem> items = new ArrayList<>();

        // default sorting is order by event start time ascending
        // credit http://stackoverflow.com/questions/2440448/sql-join-different-tables-depending-on-row-information
        String query =
                "SELECT * FROM " + DBHelper.TABLE_CIRCLE_ITEMS + " " +
                "LEFT JOIN " + DBHelper.TABLE_EVENTS + " ON " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_ITEMID + "=" + DBHelper.TABLE_EVENTS + "." + DBHelper.EVENT_COLUMN_ID + " " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_CIRCLEID + "='" + circle.getId() + "' " +
                "AND " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_TYPE + "=" + BoardItem.TYPE_EVENT + " " +
                "ORDER BY " + DBHelper.TABLE_CIRCLE_ITEMS + "." + DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME + " ASC";
        Cursor cursor = database.rawQuery(query, null);
        String result = DatabaseUtils.dumpCursorToString(cursor);
        Log.d(TAG, "Result found is " + result);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int type = cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLEITEM_COLUMN_TYPE));
            switch (type) {
                case BoardItem.TYPE_EVENT:
                    Event event = serializeEvent(cursor, circle);
                    int action = event.getUpdatedTime().equals(event.getCreatedTime()) ? FeedAction.CREATE_EVENT :
                            FeedAction.UPDATE_EVENT;
                    event.setLastAction(new FeedAction(action, activeUser, event.getUpdatedTime()));
                    items.add(event);
                    break;
                default: break;
            }
            cursor.moveToNext();
        }
        cursor.close();

        return items;
    }

    /*************************************************
     * EVENT OPERATIONS
     *************************************************/

    public void createEventAsync(final Circle circle, final DateTime createdTime,
                                 final String id, final String name, final DateTime startTime, final DateTime endTime, final String placeId,
                             final Location location, final String note, final boolean sync) {
        final Event event = TextUtils.isEmpty(id) ? Event.createEvent(circle, createdTime, createdTime)
                : Event.createEvent(id, circle, createdTime, createdTime);
        event.setEventInfo(name, startTime, endTime, placeId, location, note);
        event.setLastAction(new FeedAction(FeedAction.CREATE_EVENT, activeUser, createdTime));

        run(new Runnable() {
            @Override
            public void run() {
                if (!database.isOpen()) open();
                database.beginTransaction();

                try {
                    ContentValues values = new ContentValues();
                    values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
                    values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, event.getId());
                    values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, event.getType());
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

                    if (handler != null && sync)
                        handler.sendMessageDelayed(handler.obtainMessage(Const.MSG_EVENT_CREATED, event), 1000);
                }
                catch (SQLException ex) {
                    Log.e(TAG, ex.getMessage());
                }
                finally {
                    database.endTransaction();
                }

                // whether or not to sync with the server
                if (sync) requestCreateEvent(circle, event);
            }
        });
    }

    public Event createEvent(Circle circle, DateTime createdTime, String id, String name, DateTime startTime, DateTime endTime, String placeId,
                             Location location, String note, boolean sync) {
        createdTime = createdTime==null ? DateTime.now() : createdTime;
        Event event = TextUtils.isEmpty(id) ? Event.createEvent(circle, createdTime, createdTime)
                : Event.createEvent(id, circle, createdTime, createdTime);
        event.setEventInfo(name, startTime, endTime, placeId, location, note);
        event.setLastAction(new FeedAction(FeedAction.CREATE_EVENT, activeUser, createdTime));
        circle.addItem(event);

        if (!database.isOpen()) open();
        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLEITEM_COLUMN_CIRCLEID, circle.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_ITEMID, event.getId());
            values.put(DBHelper.CIRCLEITEM_COLUMN_TYPE, event.getType());
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

        // whether or not to sync with the server
        if (sync) requestCreateEvent(circle, event);

        return event;
    }

    public void requestCreateEvent(Circle circle, final Event event) {
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

                RequestHandler.getInstance(context).post("Create Event", String.format(Const.API_BOARD, circle.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                if (handler != null) {
                                    handler.sendMessage(handler.obtainMessage(Const.MSG_EVENT_CREATED_SUCCESS, event));
                                }
                            }

                            @Override
                            public void onError(VolleyError error) {
                                if (handler != null) {
                                    handler.sendMessage(handler.obtainMessage(Const.MSG_EVENT_CREATED_FAILED, event));
                                }
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                if (handler != null) handler.sendMessage(handler.obtainMessage(Const.MSG_EVENT_CREATED_FAILED, event));
            }
        }
    }

    public Event updateEvent(Circle circle, DateTime updatedTime, String id, String name, DateTime startTime, DateTime endTime,
                             String place, Location location, String note, boolean sync) {
        List<BoardItem> events = circle.getItems();
        Event event = null;
        for (BoardItem item : events) {
            if (item.getId().equals(id) && item instanceof Event) {
                event = (Event) item;
                break;
            }
        }

        if (event != null) {
            updatedTime = updatedTime==null? DateTime.now() : updatedTime;
            event.setLastAction(new FeedAction(FeedAction.UPDATE_EVENT, activeUser, updatedTime));
            event.setName(name);
            event.setStartTime(startTime);
            event.setEndTime(endTime);
            event.setPlace(place);
            event.setLocation(location);
            event.setNote(note);

            if (!database.isOpen()) open();
            database.beginTransaction();

            try {
                ContentValues values = new ContentValues();
                values.put(DBHelper.CIRCLEITEM_COLUMN_UPDATED_TIME, updatedTime.getMillis());
                int rows = database.update(DBHelper.TABLE_CIRCLE_ITEMS, values, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" +
                        event.getId() + "'", null);
                Log.d(TAG, "Updated item with id " + event.getId() + ", " + rows + " row(s) affected");
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

            // whether or not to sync this update with the server
            if (sync) requestUpdateEvent(circle, event);
        }

        return event;
    }

    public void requestUpdateEvent(Circle circle, final Event event) {
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

                RequestHandler.getInstance(context).post("Update Event", String.format(Const.API_BOARD_ITEM, circle.getId(), event.getId()), body,
                        new ResponseListener() {
                            @Override
                            public void onSuccess(JSONObject response) {
                                if (handler != null) handler.sendEmptyMessage(Const.MSG_EVENT_UPDATED_SUCCESS);
                            }

                            @Override
                            public void onError(VolleyError error) {
                                if (handler != null) {
                                    handler.sendMessage(handler.obtainMessage(Const.MSG_EVENT_UPDATED_FAILED, event));
                                }
                            }
                        });
            }
            catch (Exception ex) {
                ex.printStackTrace();
                if (handler != null) handler.sendMessage(handler.obtainMessage(Const.MSG_EVENT_UPDATED_FAILED, event));
            }
        }
    }

    public void deleteItem(BoardItem item) {
        if (item != null) {
            String id = item.getId();
            if (!database.isOpen()) open();

            int rows = database.delete(DBHelper.TABLE_CIRCLE_ITEMS, DBHelper.CIRCLEITEM_COLUMN_ITEMID + "='" + id + "'", null);
            Log.d(TAG, "Deleted item id " + id + " from circle items table, affected " + rows + " row(s)");

            if (item.getType() == BoardItem.TYPE_EVENT) {
                rows = database.delete(DBHelper.TABLE_EVENTS, DBHelper.EVENT_COLUMN_ID + "='" + id + "'", null);
                Log.d(TAG, "Deleted event id " + id + " from event table, affected " + rows + " row(s)");
            }
        }
    }

    public void deleteItem(String id, Circle circle, boolean sync) {
        List<BoardItem> items = circle.getItems();
        BoardItem deleted = null;
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).getId().equals(id) && items.get(index).getType() == BoardItem.TYPE_EVENT) {
                deleted = items.remove(index);
                break;
            }
        }

        deleteItem(deleted);
        if (sync) requestDeleteItem(circle, deleted);
    }

    public void requestDeleteItem(Circle circle, final BoardItem item) {
        if (item != null) {
            RequestHandler.getInstance(context).delete("Delete Item", String.format(Const.API_BOARD_ITEM, circle.getId(), item.getId()), null,
                    new ResponseListener() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            if (handler != null)
                                handler.sendEmptyMessage(Const.MSG_ITEM_DELETED_SUCCESS);
                        }

                        @Override
                        public void onError(VolleyError error) {
                            if (handler != null) {
                                handler.sendMessage(handler.obtainMessage(Const.MSG_ITEM_DELETED_FAILED, item));
                            }
                        }
                    });
        }
    }

    private Event serializeEvent(Cursor cursor, Circle circle) {
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

        Event event = Event.createEvent(id, circle, createdTime, updatedTime);
        event.setEventInfo(name, time, endTime, place,location, note);
        return event;
    }
}