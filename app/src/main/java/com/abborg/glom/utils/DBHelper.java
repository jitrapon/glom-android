package com.abborg.glom.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class is responsible for creating the circles database.
 *
 * Created by Boat on 22/9/58.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "app.db";
    private static final int DATABASE_VERSION = 2;

    // Circle table
    public static final String TABLE_CIRCLES = "circles";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_USERS = "users";

    // User table

    // Userlocation table

    private static final String DATABASE_CREATE = "CREATE TABLE " + TABLE_CIRCLES + " (" +
            COLUMN_ID + " TEXT, " +
            COLUMN_NAME + " TEXT, " +
            COLUMN_USERS + " TEXT," +
            "UNIQUE (" + COLUMN_ID + ", " + COLUMN_NAME + ") ON CONFLICT IGNORE" +
            ");";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CIRCLES);
        onCreate(db);
    }
}
