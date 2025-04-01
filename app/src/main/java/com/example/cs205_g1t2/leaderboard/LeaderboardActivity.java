package com.example.cs205_g1t2.leaderboard;

import com.example.cs205_g1t2.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
    private final int maxEntries = 100;  // only display top 100 entries

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        listView = findViewById(R.id.listViewLeaderboard);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        txtPage = findViewById(R.id.txtPageNumber);

        dbHelper = new LeaderboardDbHelper(this);

        // Load the first page (page index 0)
        List<LeaderboardEntry> entries = loadEntriesForPage(currentPage);
        adapter = new LeaderboardAdapter(this, entries);
        listView.setAdapter(adapter);

        updatePageNumber();

        btnNext.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // Ensure we do not go past 100 entries
                if ((currentPage + 1) * entriesPerPage < maxEntries) {
                    currentPage++;
                    List<LeaderboardEntry> newEntries = loadEntriesForPage(currentPage);
                    adapter.updateEntries(newEntries);
                    updatePageNumber();
                }
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener(){
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
        int totalPages = maxEntries / entriesPerPage;
        txtPage.setText("Page " + (currentPage + 1) + " of " + totalPages);
    }

    private List<LeaderboardEntry> loadEntriesForPage(int page) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        int offset = page * entriesPerPage;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Use LIMIT and OFFSET to fetch 10 records per page ordered by score (highest first)
        String query = "SELECT " + LeaderboardDbHelper.COLUMN_SCORE + ", " +
                LeaderboardDbHelper.COLUMN_DATETIME +
                " FROM " + LeaderboardDbHelper.TABLE_LEADERBOARD +
                " ORDER BY " + LeaderboardDbHelper.COLUMN_SCORE + " DESC" +
                " LIMIT " + entriesPerPage +
                " OFFSET " + offset;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                int score = cursor.getInt(0);
                String datetime = cursor.getString(1);
                entries.add(new LeaderboardEntry(score, datetime));
            } while(cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return entries;
    }
}
