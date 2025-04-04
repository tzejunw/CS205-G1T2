package com.example.cs205_g1t2.leaderboard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LeaderboardDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "leaderboard.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_LEADERBOARD = "leaderboard";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SCORE = "score";
    public static final String COLUMN_DATETIME = "datetime";

    // SQL statement to create the leaderboard table
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_LEADERBOARD + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_SCORE + " INTEGER, " +
                    COLUMN_DATETIME + " TEXT" +
                    ");";

    public LeaderboardDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create the leaderboard table when the database is first created
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If the table schema changes, drop the old table and create a new one
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEADERBOARD);
        onCreate(db);
    }

    // Inserts a new record into the leaderboard table
    public long insertRecord(int score) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SCORE, score);
        // Get the current date and time in a formatted string
        String currentDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put(COLUMN_DATETIME, currentDatetime);
        long id = db.insert(TABLE_LEADERBOARD, null, values);
        db.close();
        return id;
    }

    public boolean tableExists() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{TABLE_LEADERBOARD}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }



}
