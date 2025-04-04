package com.example.cs205_g1t2.leaderboard;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.example.cs205_g1t2.R;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends Activity {

    private ListView listView;
    private Button btnNext, btnPrev;
    private TextView txtPage;
    private LeaderboardDbHelper dbHelper;
    private LeaderboardAdapter adapter;
    private int currentPage = 0;
    private final int entriesPerPage = 10;
    private final int maxEntries = 100; // only display top 100 entries

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        listView = findViewById(R.id.listViewLeaderboard);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        txtPage = findViewById(R.id.txtPageNumber);

        // Ensure dbHelper is initialized before use
        dbHelper = new LeaderboardDbHelper(this);

        // Load the first page (page index 0)
        List<LeaderboardEntry> entries = loadEntriesForPage(currentPage);
        adapter = new LeaderboardAdapter(this, entries);
        listView.setAdapter(adapter);
        updatePageNumber();

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ensure we do not go past the maximum page limit
                if ((currentPage + 1) * entriesPerPage < maxEntries) {
                    currentPage++;
                    List<LeaderboardEntry> newEntries = loadEntriesForPage(currentPage);
                    adapter.updateEntries(newEntries);
                    updatePageNumber();
                }
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 0) {
                    currentPage--;
                    List<LeaderboardEntry> newEntries = loadEntriesForPage(currentPage);
                    adapter.updateEntries(newEntries);
                    updatePageNumber();
                }
            }
        });
    }

    private void updatePageNumber() {
        try {
            int totalEntries = getTotalEntryCount();
            int totalPages = (totalEntries == 0) ? 1 : (int) Math.ceil((double) totalEntries / entriesPerPage);
            totalPages = Math.min(totalPages, maxEntries / entriesPerPage);
            txtPage.setText("Page " + (currentPage + 1) + " of " + totalPages);

            // Disable/enable buttons based on pagination state
            btnPrev.setEnabled(currentPage > 0);
            btnNext.setEnabled((currentPage + 1) * entriesPerPage < Math.min(totalEntries, maxEntries));
        } catch (Exception e) {
            Log.e("LeaderboardActivity", "Error updating page number: " + e.getMessage());
            txtPage.setText("Page 1 of 1"); // Fallback
        }
    }

    private int getTotalEntryCount() {
        int count = 0;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + LeaderboardDbHelper.TABLE_LEADERBOARD, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            Log.e("LeaderboardActivity", "Error getting entry count: " + e.getMessage());
        }
        return count;
    }


    private List<LeaderboardEntry> loadEntriesForPage(int page) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try {
            int offset = page * entriesPerPage;
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String query = "SELECT " + LeaderboardDbHelper.COLUMN_SCORE + ", " +
                    LeaderboardDbHelper.COLUMN_DATETIME +
                    " FROM " + LeaderboardDbHelper.TABLE_LEADERBOARD +
                    " ORDER BY " + LeaderboardDbHelper.COLUMN_SCORE + " DESC" +
                    " LIMIT " + entriesPerPage +
                    " OFFSET " + offset;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        int scoreColumn = cursor.getColumnIndex(LeaderboardDbHelper.COLUMN_SCORE);
                        int datetimeColumn = cursor.getColumnIndex(LeaderboardDbHelper.COLUMN_DATETIME);
                        if (scoreColumn != -1 && datetimeColumn != -1) {
                            int score = cursor.getInt(scoreColumn);
                            String datetime = cursor.getString(datetimeColumn);
                            entries.add(new LeaderboardEntry(score, datetime));
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();
        } catch (Exception e) {
            Log.e("LeaderboardActivity", "Error loading leaderboard entries: " + e.getMessage());
        }
        return entries;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure database is closed
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

}
