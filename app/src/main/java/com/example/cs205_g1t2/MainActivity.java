package com.example.cs205_g1t2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Game game;
    private MusicService musicService;
    private boolean isBound = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start and bind the music service
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // Fullscreen setup (keep existing)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main); // Set menu layout

        // Play button click handler
        findViewById(R.id.btn_play).setOnClickListener(v -> {
            startActivity(new Intent(this, GameActivity.class));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

//    public void restartGame() {
//        finish();
//        startActivity(new Intent(this, MainActivity.class));
//        overridePendingTransition(0, 0);
//    }
//
//    private void createSampleProcesses() {
//        // Get screen dimensions (wait until view is laid out)
//        game.post(new Runnable() {
//            @Override
//            public void run() {
//                int centerX = game.getWidth() / 2;
//                int centerY = game.getHeight() / 2;
//
//                // Create processes with positions relative to screen size
//                Process p1 = new Process(
//                        centerX - 200,
//                        centerY,
//                        Color.rgb(255, 87, 51)  // Orange
//                );
//
//                Process p2 = new Process(
//                        centerX + 200,
//                        centerY,
//                        Color.rgb(92, 107, 192) // Blue
//                );
//
//                game.addProcess(p1);
//                game.addProcess(p2);
//            }
//        });
//    }
}
