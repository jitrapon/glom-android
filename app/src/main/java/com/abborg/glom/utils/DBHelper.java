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

    // Circle table
    public static final String TABLE_CIRCLES = "circles";
    public static final String CIRCLE_COLUMN_ID = "id";
    public static final String CIRCLE_COLUMN_NAME = "name";

    // User table
    public static final String TABLE_USERS = "users";
    public static final String USER_COLUMN_ID = "id";
    public static final String USER_COLUMN_NAME = "name";
    public static final String USER_COLUMN_AVATAR_ID = "avatarId";

//    // Avatar table
//    public static final String TABLE_AVATARS = "avatars";
//    public static final String AVATAR_COLUMN_ID = "id";
//    public static final String AVATAR_COLUMN_DEFAULT = "default";
//    public static final String AVATAR_COLUMN_HAPPY = "happy";
//    public static final String AVATAR_COLUMN_SAD = "sad";

    // UserCircle table - mapping userId to circleId to location (x,y)
    public static final String TABLE_USER_CIRCLE = "userCircle";
    public static final String USERCIRCLE_COLUMN_USER_ID = "userId";
    public static final String USERCIRCLE_COLUMN_CIRCLE_ID = "circleId";
    public static final String USERCIRCLE_COLUMN_LATITUDE = "latitude";
    public static final String USERCIRCLE_COLUMN_LONGITUDE = "longitude";

    private static final String TAG = "DB";

    private static final String DATABASE_CREATE_CIRCLES_TABLE = "CREATE TABLE " + TABLE_CIRCLES + " (" +
            CIRCLE_COLUMN_ID + " TEXT, " +
            CIRCLE_COLUMN_NAME + " TEXT, " +
            "UNIQUE (" + CIRCLE_COLUMN_ID + ", " + CIRCLE_COLUMN_NAME + ")" +
            ");";

    private static final String DATABASE_CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " (" +
            USER_COLUMN_ID + " TEXT, " +
            USER_COLUMN_NAME + " TEXT, " +
            USER_COLUMN_AVATAR_ID + " TEXT, " +
            "UNIQUE (" + USER_COLUMN_ID + ", " + USER_COLUMN_NAME + ")" +
            ");";

//    private static final String DATABASE_CREATE_AVATARS_TABLE = "CREATE TABLE " + TABLE_AVATARS + " (" +
//            AVATAR_COLUMN_ID + " TEXT, " +
//            AVATAR_COLUMN_DEFAULT + " TEXT, " +
//            AVATAR_COLUMN_HAPPY + " TEXT, " +
//            AVATAR_COLUMN_SAD + " TEXT" +
//            ");";

    private static final String DATABASE_CREATE_USERCIRCLE_TABLE = "CREATE TABLE " + TABLE_USER_CIRCLE + " (" +
            USERCIRCLE_COLUMN_USER_ID + " TEXT, " +
            USERCIRCLE_COLUMN_CIRCLE_ID + " TEXT, " +
            USERCIRCLE_COLUMN_LATITUDE + " REAL, " +
            USERCIRCLE_COLUMN_LONGITUDE + " REAL, " +
            "FOREIGN KEY (" + USERCIRCLE_COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + USER_COLUMN_ID + "), " +
            "FOREIGN KEY (" + USERCIRCLE_COLUMN_CIRCLE_ID + ") REFERENCES " + TABLE_CIRCLES + "(" + CIRCLE_COLUMN_ID + ")" +
            ");";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        try {
            database.beginTransaction();

            database.execSQL(DATABASE_CREATE_CIRCLES_TABLE);
            database.execSQL(DATABASE_CREATE_USERS_TABLE);
//          database.execSQL(DATABASE_CREATE_AVATARS_TABLE);
            database.execSQL(DATABASE_CREATE_USERCIRCLE_TABLE);

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
//        database.execSQL("DROP TABLE IF EXISTS " + TABLE_AVATARS);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_CIRCLE);
        onCreate(database);
    }
}
