package com.example.cs205_g1t2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    private Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen setup (same as original MainActivity)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Move original MainActivity code here
        game = new Game(this);
//        createSampleProcesses();
        setContentView(game);
    }

    // Move process creation from original MainActivity
//    private void createSampleProcesses() {
//        game.post(() -> {
//            int centerX = game.getWidth() / 2;
//            int centerY = game.getHeight() / 2;
//
//            Process p1 = new Process(centerX - 200, centerY, Color.rgb(255, 87, 51));
//            Process p2 = new Process(centerX + 200, centerY, Color.rgb(92, 107, 192));
//
//            game.addProcess(p1);
//            game.addProcess(p2);
//        });
//    }

//    public void restartGame() {
//        finish();
//        startActivity(new Intent(this, GameActivity.class));
//        overridePendingTransition(0, 0);
//    }
}

