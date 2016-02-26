package com.abborg.glom.model;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.abborg.glom.AppState;
import com.abborg.glom.Const;
import com.abborg.glom.R;
import com.abborg.glom.utils.DBHelper;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that wraps around model to perform CRUD operations on database and 
 * make necessary network operations
 *
 * Most operation functions in this class will call runOnUiThread() to ease off operations
 * off the main thread for optimization
 *
 * Created by Boat on 22/9/58.
 */
public class DataUpdater {

    private User currentUser;

    private SQLiteDatabase database;
    
    private DBHelper dbHelper;

    private static final String TAG = "DATA PROVIDER";
    
    private String[] circleColumns = { DBHelper.CIRCLE_COLUMN_ID,
            DBHelper.CIRCLE_COLUMN_NAME, DBHelper.CIRCLE_COLUMN_BROADCAST_LOCATION };
    
    private String[] userColumns = { DBHelper.USER_COLUMN_ID, DBHelper.USER_COLUMN_NAME,
            DBHelper.USER_COLUMN_AVATAR_ID };

    private String[] userCircleColumns = { DBHelper.USERCIRCLE_COLUMN_USER_ID, DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID,
            DBHelper.USERCIRCLE_COLUMN_LATITUDE, DBHelper.USERCIRCLE_COLUMN_LONGITUDE };

    private Context context;

    /**
     * Creates a new instance of the DataUpdater
     * Call set
     *
     * @param context
     */
    public DataUpdater(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
    }

    /**
     * Call this method to start performing operations to the database
     * //TODO don't call this on main thread
     *
     * @throws SQLException
     */
    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    /**
     * Close operation on the database
     */
    public void close() {
        dbHelper.close();
    }

    /**
     * Deletes all record from all table in order to start fresh. The method does re-create the table however
     */
    public void resetCircles() {
        try {
            database.beginTransaction();

            database.execSQL("DELETE FROM " + DBHelper.TABLE_CIRCLES);
            database.execSQL("DELETE FROM " + DBHelper.TABLE_USERS);
            database.execSQL("DELETE FROM " + DBHelper.TABLE_USER_CIRCLE);
            database.execSQL("DELETE FROM " + DBHelper.TABLE_EVENTS);

//            database.execSQL("DROP TABLE IF EXISTS " + DBHelper.TABLE_CIRCLES);
//            database.execSQL("DROP TABLE IF EXISTS " + DBHelper.TABLE_USERS);
//            database.execSQL("DROP TABLE IF EXISTS " + DBHelper.TABLE_USER_CIRCLE);
//            database.execSQL("DROP TABLE IF EXISTS " + DBHelper.TABLE_EVENTS);

            database.setTransactionSuccessful();
        }
        finally {
            database.endTransaction();
        }
    }

    /**
     * Retrieves main user from DB. The user is a generic instance whose location and circle may not be set.
     * TODO may need to check from server to sync info.
     * @param id
     * @return
     */
    public User initCurrentUser(String id) {
        User user = null;
        Cursor cursor = database.query(DBHelper.TABLE_USERS, null, DBHelper.USER_COLUMN_ID + " = '" + id + "'", null, null, null ,null);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_NAME));
            String userId = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_ID));
            String avatar = cursor.getString(cursor.getColumnIndex(DBHelper.USER_COLUMN_AVATAR_ID));
            user = new User(name, userId, null);
            user.setAvatar(avatar);
        }
        cursor.close();

        return user;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Creates a new circle, with the current user, and the specified users in it.
     *
     * @param name The name or title of the circle to create with
     * @param users The list of users to add at the time of creation along with current user
     * @param id The id of this circle. Leave null to randomly generate one
     * @return The created circle
     */
    public Circle createCircle(String name, List<User> users, String id) {
        Circle circle = Circle.createCircle(name, currentUser);
        if (id != null) circle.setId(id);
        if (users != null && !users.isEmpty()) circle.addUsers(users);

        database.beginTransaction();

        try {
            // insert new record into CIRCLES table (record is unique per circle)
            ContentValues values = new ContentValues();
            values.put(DBHelper.CIRCLE_COLUMN_ID, circle.getId());
            values.put(DBHelper.CIRCLE_COLUMN_NAME, circle.getTitle());
            long insertId = database.insert(DBHelper.TABLE_CIRCLES, null, values);
//            Log.d(TAG, "Inserted circle with _id: " + insertId + ", id: " + circle.getId() + ", name: " +
//                    circle.getTitle() + ", userlist: " + circle.getUserListString());

            for (User user : circle.getUsers()) {
                user.setCurrentCircle(circle);

                // insert new users into USER table if unique (record is unique per user)
                values.clear();
                values.put(DBHelper.USER_COLUMN_ID, user.getId());
                values.put(DBHelper.USER_COLUMN_NAME, user.getName());
                values.put(DBHelper.USER_COLUMN_AVATAR_ID, user.getAvatar());
                insertId = database.insert(DBHelper.TABLE_USERS, null, values);
//                Log.d(TAG, "Inserted user with _id: " +  insertId + ", id: " + user.getId() + " into " + DBHelper.TABLE_USERS);

                // insert the user-circle association into the USERCIRCLE table (record is unique per association)
                values.clear();
                values.put(DBHelper.USERCIRCLE_COLUMN_USER_ID, user.getId());
                values.put(DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID, circle.getId());
                values.put(DBHelper.USERCIRCLE_COLUMN_LATITUDE, user.getLocation().getLatitude());
                values.put(DBHelper.USERCIRCLE_COLUMN_LONGITUDE, user.getLocation().getLongitude());
                insertId = database.insert(DBHelper.TABLE_USER_CIRCLE, null, values);
//                Log.d(TAG, "Inserted user with _id: " + insertId + ", userId: " + user.getId() + ", circleId: " + circle.getId() + " into " + DBHelper.TABLE_USER_CIRCLE);
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

    /**
     * Delete the circle and remove all association of users within this circle
     * TODO
     *
     * @param circle
     */
    public void deleteCircle(Circle circle) {
        String id = circle.getId();
        database.delete(DBHelper.TABLE_CIRCLES, DBHelper.CIRCLE_COLUMN_ID + " = " + id, null);

        //TODO send request to GCM and server to delete group
    }

    /**
     * TODO
     *
     * @param circle
     * @param users
     */
    public Circle addUsersToCircle(Circle circle, List<User> users) { return null; }

    /**
     * TODO
     *
     * @param circle
     * @param users
     * @return
     */
    public Circle removeUsersFromCircle(Circle circle, List<User> users) { return null; }

    /**
     * Retrieves the list of users in the specified circle
     *
     * @param circle
     * @return
     */
    public List<User> getUsersInCircle(Circle circle) {
        List<User> users = new ArrayList<User>();

        // get the user info from USERS table and the user location from USERCIRCLE table
        // SELECT id,name,avatarId,location
        String selectColumns = DBHelper.USER_COLUMN_ID + "," + DBHelper.USER_COLUMN_NAME + "," + DBHelper.USER_COLUMN_AVATAR_ID + "," +
                DBHelper.USERCIRCLE_COLUMN_LATITUDE + "," + DBHelper.USERCIRCLE_COLUMN_LONGITUDE;

        String query = "SELECT " + selectColumns + " FROM " + DBHelper.TABLE_USERS + ", " + DBHelper.TABLE_USER_CIRCLE + " WHERE " +
                DBHelper.TABLE_USERS + "." + DBHelper.USER_COLUMN_ID + "=" + DBHelper.TABLE_USER_CIRCLE + "." +
                DBHelper.USERCIRCLE_COLUMN_USER_ID + " AND " + DBHelper.TABLE_USER_CIRCLE + "." + DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "=" +
                "'" + circle.getId() + "'";
        Cursor userCursor = database.rawQuery(query, null);

        userCursor.moveToFirst();
        while (!userCursor.isAfterLast()) {
            User user = serializeUser(userCursor, circle);
            users.add(user);
            userCursor.moveToNext();
        }
        userCursor.close();

        //TODO sync with server to make sure the circle is updated

        return users;
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
        List<Integer> userPerm = new ArrayList<Integer>();
        userPerm.add(User.MEDIA_IMAGE_RECEIVE);
        userPerm.add(User.MEDIA_AUDIO_RECEIVE);
        userPerm.add(User.MEDIA_VIDEO_RECEIVE);
        userPerm.add(User.ALARM_RECEIVE);
        userPerm.add(User.NOTE_RECEIVE);
        userPerm.add(User.LOCATION_REQUEST_RECEIVE);
        if (currentUser.getId().equals(user.getId())) {
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
//        Log.d(TAG, "Query user for circle(" + circle.getId() + ") id: " + user.getId() + ", name: " + user.getName() + ", avatarId: " + cursor.getString(2)
//                + ", location: " + user.getLocation().getLatitude() + ", " + user.getLocation().getLongitude());

        return user;
    }

    public List<Circle> getCircles() {
        List<Circle> circles = new ArrayList<Circle>();

        Cursor cursor = database.query(DBHelper.TABLE_CIRCLES,
                circleColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Circle circle = serializeCircle(cursor);
            circles.add(circle);
            cursor.moveToNext();
        }
        cursor.close();
        return circles;
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

    private CircleInfo serializeCircleInfo(Cursor cursor) {
        CircleInfo info = new CircleInfo();
        info.title = cursor.getString(1);

        Cursor userListCursor = database.query(DBHelper.TABLE_USER_CIRCLE, userCircleColumns, DBHelper.USERCIRCLE_COLUMN_CIRCLE_ID + "='" + cursor.getString(0) + "'",
                null, null, null, null);
        Log.e(TAG, info.title);
        info.numUsers = userListCursor.getCount();
        userListCursor.close();
        return info;
    }

    public Circle getCircleByName(String name) {
        Cursor cursor = database.query(DBHelper.TABLE_CIRCLES,
                circleColumns, DBHelper.CIRCLE_COLUMN_NAME + "='" + name + "'", null, null, null, null);

        if (cursor.moveToFirst()) {
            Circle circle = serializeCircle(cursor);
            cursor.close();
            Log.d(TAG, "Get circle (" + circle.getTitle() + ") with " + circle.getUsers().size() + " users, isBroadcastingLocation: " +
                circle.isUserBroadcastingLocation());
            return circle;
        }
        else {
            return null;
        }
    }

    public void updateCircleLocationBroadcast(Circle circle, boolean enabled) {
        ContentValues values = new ContentValues();
        int broadcastingLocation = enabled ? 1 : 0;
        values.put(DBHelper.CIRCLE_COLUMN_BROADCAST_LOCATION, broadcastingLocation);
        int rowAffected = database.update(DBHelper.TABLE_CIRCLES, values,
                DBHelper.CIRCLE_COLUMN_ID + "='" + circle.getId() + "'", null);
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

    private Circle serializeCircle(Cursor cursor) {
        Circle circle = Circle.createCircle(null, currentUser);
        circle.setId(cursor.getString(0));
        circle.setTitle(cursor.getString(1));
        circle.setBroadcastingLocation((cursor.getInt(cursor.getColumnIndex(DBHelper.CIRCLE_COLUMN_BROADCAST_LOCATION)) == 1));

        List<User> users = getUsersInCircle(circle);
        List<Event> events = getCircleEvents(circle);
        circle.setUsers(users);
        circle.setEvents(events);
        return circle;
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

    /**
     * Create a new event under an optionally specified circle
     */
    public Event createEvent(String name, Circle circle, List<User> hosts, DateTime time, DateTime endTime, String place, Location location,
                             int discoverType, List<User> invitees, boolean showHosts,
                             boolean showInvitees, boolean showAttendees, String note) {
        // TODO notify server
        Event event = Event.createEvent(name, circle, hosts, time, place, location, discoverType, invitees, showHosts,  showInvitees,
                showAttendees, note);
        event.setEndTime(endTime);

        //TODO retrieve last action and action timestamp from server
        //TODO for now hardcode this
        event.setLastAction(new FeedAction(FeedAction.CREATE_EVENT, currentUser, new DateTime()));
        circle.addEvent(event);     // this add event to the circle, automatically updating the recyclerview adapter

        database.beginTransaction();

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.EVENT_COLUMN_ID, event.getId());
            if (circle != null) values.put(DBHelper.EVENT_COLUMN_CIRCLE_ID, circle.getId());
            values.put(DBHelper.EVENT_COLUMN_NAME, event.getName());
            if (event.getDateTime() != null) {
                values.put(DBHelper.EVENT_COLUMN_DATETIME, event.getDateTime().getMillis());
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
            long insertId = database.insert(DBHelper.TABLE_EVENTS, null, values);
//            Log.d(TAG, "Inserted event with _id: " + insertId + ", id: " + event.getId() + ", name: " +
//                    event.getName() + ", time: " + event.getDateTime() + ", place: " + event.getPlace());

            database.setTransactionSuccessful();
        }
        catch (SQLException ex) {
            Log.e(TAG, ex.getMessage());
        }
        finally {
            database.endTransaction();
        }

        //TODO send update to server of the created event

        return event;
    }

    public Event updateEvent(String eventId, Circle eventCircle, String newName, List<User> newHosts, DateTime newStartTime, DateTime newEndTime,
                             String newPlace, Location newLocation, int newDiscoverType, List<User> newInvitees, boolean newShowHosts,
                             boolean newShowInvitees, boolean newShowAttendees, String newNote) {
        //TODO update server

        List<Event> events = eventCircle.getEvents();
        Event updatedEvent = null;
        for (Event event : events) {
            if (event.getId().equals(eventId)) {
                updatedEvent = event;
                break;
            }
        }

        if (updatedEvent != null) {
            //TODO retrieve last action and action timestamp from server
            //TODO for now hardcode this
            updatedEvent.setLastAction(new FeedAction(FeedAction.UPDATE_EVENT, currentUser, new DateTime()));
            updatedEvent.setName(newName);
            updatedEvent.setHosts(newHosts);
            updatedEvent.setDateTime(newStartTime);
            updatedEvent.setEndTime(newEndTime);
            updatedEvent.setPlace(newPlace);
            updatedEvent.setLocation(newLocation);
            updatedEvent.setDiscoverType(newDiscoverType);
            updatedEvent.setInvitees(newInvitees);
            updatedEvent.setHostsShown(newShowHosts);
            updatedEvent.setInviteesShown(newShowInvitees);
            updatedEvent.setAttendeesShown(newShowAttendees);
            updatedEvent.setNote(newNote);

            database.beginTransaction();

            try {
                ContentValues values = new ContentValues();
                values.put(DBHelper.EVENT_COLUMN_NAME, updatedEvent.getName());
                if (updatedEvent.getDateTime() != null) {
                    values.put(DBHelper.EVENT_COLUMN_DATETIME, updatedEvent.getDateTime().getMillis());
                }
                if (updatedEvent.getEndTime() != null) {
                    values.put(DBHelper.EVENT_COLUMN_ENDTIME, updatedEvent.getEndTime().getMillis());
                }
                values.put(DBHelper.EVENT_COLUMN_PLACE, updatedEvent.getPlace());
                if (updatedEvent.getLocation() != null) {
                    values.put(DBHelper.EVENT_COLUMN_LATITUDE, updatedEvent.getLocation().getLatitude());
                    values.put(DBHelper.EVENT_COLUMN_LONGITUDE, updatedEvent.getLocation().getLongitude());
                }
                else {
                    values.put(DBHelper.EVENT_COLUMN_LATITUDE, -1.0);
                    values.put(DBHelper.EVENT_COLUMN_LONGITUDE, -1.0);
                }
                values.put(DBHelper.EVENT_COLUMN_NOTE, updatedEvent.getNote());
                long updateId = database.update(DBHelper.TABLE_EVENTS, values,
                        DBHelper.EVENT_COLUMN_ID + "='" + updatedEvent.getId() + "'", null);
                Log.d(TAG, "Updated event with _id: " + updateId + ", id: " + updatedEvent.getId() + ", name: " +
                    updatedEvent.getName() + ", time: " + updatedEvent.getDateTime() + ", place: " + updatedEvent.getPlace());

                database.setTransactionSuccessful();
            }
            catch (SQLException ex) {
                Log.e(TAG, ex.getMessage());
            }
            finally {
                database.endTransaction();
            }
        }

        return updatedEvent;
    }

    public void deleteEvent(String id, Circle circle) {
        List<Event> events = circle.getEvents();
        Event deleted = null;
        for (int index = 0; index < events.size(); index++) {
            if (events.get(index).getId().equals(id)) {
                deleted = events.remove(index);
            }
        }
        if (deleted != null) id = deleted.getId();

        database.delete(DBHelper.TABLE_EVENTS, DBHelper.EVENT_COLUMN_ID + "='" + id + "'", null);
    }

    /**
     * Retrieves list of events within this circle that are cached
     *
     * @param circle
     * @return
     */
    public List<Event> getCircleEvents(Circle circle) {
        //TODO send request to server
        List<Event> events = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormat.forPattern(context.getResources().getString(R.string.action_create_event_datetime_format));
        DateTime postTime = formatter.parseDateTime("20/11/2015 12:00:00");

        // default sorting is order by event start time ascending
        String query = "SELECT * FROM " + DBHelper.TABLE_EVENTS + " WHERE " +
                DBHelper.TABLE_EVENTS + "." + DBHelper.EVENT_COLUMN_CIRCLE_ID + "='" + circle.getId() + "' ORDER BY " +
                DBHelper.TABLE_EVENTS + "." + DBHelper.EVENT_COLUMN_DATETIME + " ASC";
        Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Event event = serializeEvent(cursor, circle);

            //TODO retrieve last action and action timestamp from server
            //TODO for now hardcode this
            event.setLastAction(new FeedAction(FeedAction.CREATE_EVENT, currentUser, postTime));
            events.add(event);
            cursor.moveToNext();
        }
        cursor.close();

        return events;
    }

    private Event serializeEvent(Cursor cursor, Circle circle) {
        Log.d(TAG, "Serializing an event...");
        int idColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_ID);
        int nameColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_NAME);
        int dateColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_DATETIME);
        int endDateColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_ENDTIME);
        int placeColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_PLACE);
        int latColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_LATITUDE);
        int longColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_LONGITUDE);
        int noteColumn = cursor.getColumnIndex(DBHelper.EVENT_COLUMN_NOTE);

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

        Event event = Event.createEvent(id, circle, name, time, place, location, note);
        event.setEndTime(endTime);
        return event;
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
                    if (userId.equals(AppState.getInstance(context).getUser().getId())) {
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
}
