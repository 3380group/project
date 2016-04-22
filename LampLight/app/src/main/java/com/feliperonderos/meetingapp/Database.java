package com.feliperonderos.meetingapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Felipe on 8/26/2015.
 */
public class Database extends SQLiteOpenHelper {
    private static int version = 1;
    private static String name = "DB";
    public static final String TABLE_NAME = "meetings";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PLACE_NAME = "place_name";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_API_ID = "_API_id";
    private static final String DATABASE_CREATE = "create table "
            + TABLE_NAME + " (" + COLUMN_ID
            + " integer primary key autoincrement , "
            + COLUMN_TIME
            + " integer not null,"
            + COLUMN_API_ID
            + " integer not null,"
            + COLUMN_LATITUDE
            + " real not null,"
            + COLUMN_LONGITUDE
            + " real not null,"
            + COLUMN_PLACE_NAME
            + " text not null,"
            + COLUMN_NAME
            + " text not null);";

    public Database(Context context){
        super(context, name, null, version);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}

