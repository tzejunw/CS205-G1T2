package com.example.cs205_g1t2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
