package com.abborg.glom.data;

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
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "DB";

    // circle table columns
    public static final String TABLE_CIRCLES = "circles";
    public static final String CIRCLE_COLUMN_ID = "id";
    public static final String CIRCLE_COLUMN_NAME = "name";
    public static final String CIRCLE_COLUMN_BROADCAST_LOCATION = "broadcast_location";

    // user table columns
    public static final String TABLE_USERS = "users";
    public static final String USER_COLUMN_ID = "id";
    public static final String USER_COLUMN_NAME = "name";
    public static final String USER_COLUMN_AVATAR = "avatar";
    public static final String USER_COLUMN_TYPE = "type";

    // user_circle table - mapping userId to circleId to location (x,y) columns
    public static final String TABLE_USER_CIRCLE = "user_circle";
    public static final String USERCIRCLE_COLUMN_USER_ID = "user_id";
    public static final String USERCIRCLE_COLUMN_CIRCLE_ID = "circle_id";
    public static final String USERCIRCLE_COLUMN_LATITUDE = "latitude";
    public static final String USERCIRCLE_COLUMN_LONGITUDE = "longitude";

    // circle_items
    public static final String TABLE_CIRCLE_ITEMS = "circle_items";
    public static final String CIRCLEITEM_COLUMN_CIRCLEID = "circle_id";
    public static final String CIRCLEITEM_COLUMN_ITEMID = "id";
    public static final String CIRCLEITEM_COLUMN_TYPE = "type";
    public static final String CIRCLEITEM_COLUMN_SYNC = "sync";
    public static final String CIRCLEITEM_COLUMN_CREATED_TIME = "created_time";
    public static final String CIRCLEITEM_COLUMN_UPDATED_TIME = "updated_time";

    // item_events table
    public static final String TABLE_EVENTS = "events";
    public static final String EVENT_COLUMN_ID = "id";
    public static final String EVENT_COLUMN_NAME = "name";
    public static final String EVENT_COLUMN_STARTTIME = "start_time";
    public static final String EVENT_COLUMN_ENDTIME = "end_time";
    public static final String EVENT_COLUMN_PLACE = "place_id";
    public static final String EVENT_COLUMN_LATITUDE = "latitude";
    public static final String EVENT_COLUMN_LONGITUDE = "longitude";
    public static final String EVENT_COLUMN_NOTE = "note";

    // item_file table
    public static final String TABLE_FILES = "files";
    public static final String FILE_COLUMN_ID = "id";
    public static final String FILE_COLUMN_NAME = "name";
    public static final String FILE_COLUMN_SIZE = "size";
    public static final String FILE_COLUMN_PATH = "path";
    public static final String FILE_COLUMN_MIMETYPE = "mimetype";
    public static final String FILE_COLUMN_NOTE = "note";

    private static final String DATABASE_CASCADE = "ON DELETE CASCADE ON UPDATE CASCADE";

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
            USER_COLUMN_AVATAR + " TEXT, " +
            USER_COLUMN_TYPE + " INTEGER, " +
            "UNIQUE (" + USER_COLUMN_ID + ", " + USER_COLUMN_NAME + ")" +
            ");";

    /* Create user_circle table statement */
    private static final String DATABASE_CREATE_USERCIRCLE_TABLE = "CREATE TABLE " + TABLE_USER_CIRCLE + " (" +
            USERCIRCLE_COLUMN_USER_ID + " TEXT, " +
            USERCIRCLE_COLUMN_CIRCLE_ID + " TEXT, " +
            USERCIRCLE_COLUMN_LATITUDE + " REAL, " +
            USERCIRCLE_COLUMN_LONGITUDE + " REAL, " +
            "FOREIGN KEY (" + USERCIRCLE_COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + USER_COLUMN_ID + ") " + DATABASE_CASCADE + ", " +
            "FOREIGN KEY (" + USERCIRCLE_COLUMN_CIRCLE_ID + ") REFERENCES " + TABLE_CIRCLES + "(" + CIRCLE_COLUMN_ID + ") " + DATABASE_CASCADE +
            ");";

    /* Create circle_items table statement */
    private static final String DATABASE_CREATE_CIRCLE_ITEMS_TABLE = "CREATE TABLE " + TABLE_CIRCLE_ITEMS + " (" +
            CIRCLEITEM_COLUMN_CIRCLEID + " TEXT, " +
            CIRCLEITEM_COLUMN_ITEMID + " TEXT UNIQUE, " +
            CIRCLEITEM_COLUMN_TYPE + " INTEGER, " +
            CIRCLEITEM_COLUMN_SYNC + " INTEGER, " +
            CIRCLEITEM_COLUMN_CREATED_TIME + " INTEGER, " +
            CIRCLEITEM_COLUMN_UPDATED_TIME + " INTEGER, " +
            "FOREIGN KEY (" + CIRCLEITEM_COLUMN_CIRCLEID + ") REFERENCES " + TABLE_CIRCLES + "(" + CIRCLE_COLUMN_ID + ") " + DATABASE_CASCADE +
            ");";

    /* Create item_events table statement */
    private static final String DATABASE_CREATE_EVENTS_TABLE = "CREATE TABLE " + TABLE_EVENTS + " (" +
            EVENT_COLUMN_ID + " TEXT, " +
            EVENT_COLUMN_NAME + " TEXT, " +
            EVENT_COLUMN_STARTTIME + " INTEGER, " +
            EVENT_COLUMN_ENDTIME + " INTEGER, " +
            EVENT_COLUMN_PLACE + " TEXT, " +
            EVENT_COLUMN_LATITUDE + " REAL, " +
            EVENT_COLUMN_LONGITUDE + " REAL, " +
            EVENT_COLUMN_NOTE + " TEXT, " +
            "FOREIGN KEY (" + EVENT_COLUMN_ID + ") REFERENCES " + TABLE_CIRCLE_ITEMS + "(" + CIRCLEITEM_COLUMN_ITEMID + ") " + DATABASE_CASCADE +
            ");";

    /* Create item_files table statement */
    private static final String DATABASE_CREATE_FILES_TABLE = "CREATE TABLE " + TABLE_FILES + " (" +
            FILE_COLUMN_ID + " TEXT, " +
            FILE_COLUMN_NAME + " TEXT, " +
            FILE_COLUMN_SIZE + " INTEGER, " +
            FILE_COLUMN_MIMETYPE + "TEXT, " +
            FILE_COLUMN_PATH + "TEXT, " +
            FILE_COLUMN_NOTE + "TEXT, " +
            "FOREIGN KEY (" + FILE_COLUMN_ID + ") REFERENCES " + TABLE_CIRCLE_ITEMS + "(" + CIRCLEITEM_COLUMN_ITEMID + ") " + DATABASE_CASCADE +
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
            database.execSQL(DATABASE_CREATE_CIRCLE_ITEMS_TABLE);
            database.execSQL(DATABASE_CREATE_EVENTS_TABLE);
            database.execSQL(DATABASE_CREATE_FILES_TABLE);

            database.setTransactionSuccessful();
        }
        finally {
            database.endTransaction();
        }

        Log.d(TAG, "Created tables succesfully");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onOpen(db);
//        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
    }
}
