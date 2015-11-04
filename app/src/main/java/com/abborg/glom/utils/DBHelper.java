package com.abborg.glom.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class is responsible for creating the tables in the database.
 *
 * Created by Boat on 22/9/58.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "app.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TAG = "DB";

    // Circle table columns
    public static final String TABLE_CIRCLES = "circles";
    public static final String CIRCLE_COLUMN_ID = "id";
    public static final String CIRCLE_COLUMN_NAME = "name";
    public static final String CIRCLE_COLUMN_BROADCAST_LOCATION = "broadcastLocation";

    // User table columns
    public static final String TABLE_USERS = "users";
    public static final String USER_COLUMN_ID = "id";
    public static final String USER_COLUMN_NAME = "name";
    public static final String USER_COLUMN_AVATAR_ID = "avatar";

    // UserCircle table - mapping userId to circleId to location (x,y) columns
    public static final String TABLE_USER_CIRCLE = "userCircle";
    public static final String USERCIRCLE_COLUMN_USER_ID = "userId";
    public static final String USERCIRCLE_COLUMN_CIRCLE_ID = "circleId";
    public static final String USERCIRCLE_COLUMN_LATITUDE = "latitude";
    public static final String USERCIRCLE_COLUMN_LONGITUDE = "longitude";

    // Event table
    public static final String TABLE_EVENTS = "events";
    public static final String EVENT_COLUMN_ID = "id";
    public static final String EVENT_COLUMN_CIRCLE_ID = "circle";
    public static final String EVENT_COLUMN_NAME = "name";
    public static final String EVENT_COLUMN_DATETIME = "time";
    public static final String EVENT_COLUMN_PLACE = "place";
    public static final String EVENT_COLUMN_LATITUDE = "latitude";
    public static final String EVENT_COLUMN_LONGITUDE = "longitude";
    public static final String EVENT_COLUMN_NOTE = "note";

    /* Create circle table statement */
    private static final String DATABASE_CREATE_CIRCLES_TABLE = "CREATE TABLE " + TABLE_CIRCLES + " (" +
            CIRCLE_COLUMN_ID + " TEXT, " +
            CIRCLE_COLUMN_NAME + " TEXT, " +
            CIRCLE_COLUMN_BROADCAST_LOCATION + " INTEGER NOT NULL DEFAULT 0, " +
            "UNIQUE (" + CIRCLE_COLUMN_ID + ", " + CIRCLE_COLUMN_NAME + ")" +
            ");";

    /* Create user table statement */
    private static final String DATABASE_CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
            USER_COLUMN_ID + " TEXT, " +
            USER_COLUMN_NAME + " TEXT, " +
            USER_COLUMN_AVATAR_ID + " TEXT, " +
            "UNIQUE (" + USER_COLUMN_ID + ", " + USER_COLUMN_NAME + ")" +
            ");";

    /* Create usercircle table statement */
    private static final String DATABASE_CREATE_USERCIRCLE_TABLE = "CREATE TABLE " + TABLE_USER_CIRCLE + " (" +
            USERCIRCLE_COLUMN_USER_ID + " TEXT, " +
            USERCIRCLE_COLUMN_CIRCLE_ID + " TEXT, " +
            USERCIRCLE_COLUMN_LATITUDE + " REAL, " +
            USERCIRCLE_COLUMN_LONGITUDE + " REAL, " +
            "FOREIGN KEY (" + USERCIRCLE_COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + USER_COLUMN_ID + "), " +
            "FOREIGN KEY (" + USERCIRCLE_COLUMN_CIRCLE_ID + ") REFERENCES " + TABLE_CIRCLES + "(" + CIRCLE_COLUMN_ID + ")" +
            ");";

    /* Create event table statement */
    private static final String DATABASE_CREATE_EVENTS_TABLE = "CREATE TABLE " + TABLE_EVENTS + " (" +
            EVENT_COLUMN_ID + " TEXT UNIQUE, " +
            EVENT_COLUMN_CIRCLE_ID + " TEXT, " +
            EVENT_COLUMN_NAME + " TEXT, " +
            EVENT_COLUMN_DATETIME + " INTEGER, " +
            EVENT_COLUMN_PLACE + " TEXT, " +
            EVENT_COLUMN_LATITUDE + " REAL, " +
            EVENT_COLUMN_LONGITUDE + " REAL, " +
            EVENT_COLUMN_NOTE + " TEXT" +
            ");";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        Log.d(TAG, "Creating table");
        try {
            database.beginTransaction();

            database.execSQL(DATABASE_CREATE_CIRCLES_TABLE);
            database.execSQL(DATABASE_CREATE_USERS_TABLE);
            database.execSQL(DATABASE_CREATE_USERCIRCLE_TABLE);
            database.execSQL(DATABASE_CREATE_EVENTS_TABLE);

            database.setTransactionSuccessful();
        }
        finally {
            database.endTransaction();
        }

        Log.d(TAG, "Created tables succesfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_CIRCLES);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_CIRCLE);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(database);
    }
}
