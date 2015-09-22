package com.abborg.glom.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import com.abborg.glom.model.Circle;
import com.abborg.glom.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface to do CRUD operations on Circle instances for UI, DB and network operations.
 *
 * Created by Boat on 22/9/58.
 */
public class CircleProvider {

    // Database fields
    private User currentUser;
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private String[] columns = { DBHelper.COLUMN_ID,
            DBHelper.COLUMN_NAME, DBHelper.COLUMN_USERS };

//    private UserProvider userProvider;

    public CircleProvider(Context context, User currentUser) {
        this.currentUser = currentUser;
//        this.userProvider = userProvider;
        dbHelper = new DBHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void resetCircles() {
        database.execSQL("DELETE FROM " + DBHelper.TABLE_CIRCLES);
    }

    public Circle createCircle(String name, User currentUser, List<User> users) {
        Circle circle = Circle.createCircle(name, currentUser);
        circle.addUsers(users);

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_ID, circle.getId());
        values.put(DBHelper.COLUMN_NAME, circle.getTitle());
        values.put(DBHelper.COLUMN_USERS, circle.getUserListString());

        long insertId = database.insertWithOnConflict(DBHelper.TABLE_CIRCLES, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        Log.d("DATABASE", "Inserted circle with _id: " + insertId + ", id: " + circle.getId() + ", name: " +
                circle.getTitle() + ", userlist: " + circle.getUserListString());

        //TODO send request to GCM and server to create new group

        return circle;
    }

    public void deleteCircle(Circle circle) {
        String id = circle.getId();
        database.delete(DBHelper.TABLE_CIRCLES, DBHelper.COLUMN_ID + " = " + id, null);

        //TODO send request to GCM and server to delete group
    }

    public List<Circle> getCircles() {
        List<Circle> circles = new ArrayList<Circle>();

        Cursor cursor = database.query(DBHelper.TABLE_CIRCLES,
                columns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Circle circle = cursorToCircle(cursor);
            circles.add(circle);
            cursor.moveToNext();
        }
        cursor.close();
        return circles;
    }

    private Circle cursorToCircle(Cursor cursor) {
        Circle circle = Circle.createCircle(null, currentUser);
        circle.setId(cursor.getString(0));
        circle.setTitle(cursor.getString(1));

        List<User> users = new ArrayList<User>();
        String[] userList = cursor.getString(2).split(",");
        for (String s : userList) {
//            users.add(userProvider.getUserFromId(s));
            Location location = new Location("");
            location.setLatitude(1);
            location.setLongitude(103.1);
            users.add(new User("TestUser", s, location));
        }

        circle.setUsers(users);
        return circle;
    }
}
