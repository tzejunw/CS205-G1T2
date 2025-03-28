package com.example.cs205_g1t2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fullscreen before setting content view
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Initialize game first
        game = new Game(this);

        // Create sample processes
        createSampleProcesses();

        // Set game view after process creation
        setContentView(game);
    }

    public void restartGame() {
        finish();
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(0, 0);
    }

    private void createSampleProcesses() {
        // Get screen dimensions (wait until view is laid out)
        game.post(new Runnable() {
            @Override
            public void run() {
                int centerX = game.getWidth() / 2;
                int centerY = game.getHeight() / 2;

                // Create processes with positions relative to screen size
                Process p1 = new Process(
                        centerX - 200,
                        centerY,
                        Color.rgb(255, 87, 51)  // Orange
                );

                Process p2 = new Process(
                        centerX + 200,
                        centerY,
                        Color.rgb(92, 107, 192) // Blue
                );

                game.addProcess(p1);
                game.addProcess(p2);
            }
        });
    }
}
