package com.example.cs205_g1t2.leaderboard;

import com.example.cs205_g1t2.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.List;

public class LeaderboardAdapter extends BaseAdapter {
    private Context context;
    private List<LeaderboardEntry> entries;

    public LeaderboardAdapter(Context context, List<LeaderboardEntry> entries) {
        this.context = context;
        this.entries = entries;
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void updateEntries(List<LeaderboardEntry> newEntries) {
        this.entries = newEntries;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if(row == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            row = inflater.inflate(R.layout.row_leaderboard, parent, false);
        }
        LeaderboardEntry entry = entries.get(position);

        TextView txtScore = row.findViewById(R.id.txtScore);
        TextView txtDatetime = row.findViewById(R.id.txtDatetime);

        txtScore.setText(String.valueOf(entry.score));
        txtDatetime.setText(entry.datetime);

        return row;
    }
}
