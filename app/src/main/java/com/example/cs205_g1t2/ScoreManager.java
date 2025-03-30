package com.example.cs205_g1t2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class ScoreManager {
    private int score;
    private Paint paint;

    public ScoreManager(Context context) {
        score = 50;
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.RIGHT);
    }

    public void addScore(int amount) {
        score += amount;
    }

    public void minusScore(int amount){
        score -= amount;
    }

    // Reset the score to zero
    public void resetScore() {
        score = 0;
    }

    public int getScore() {
        return score;
    }

    // Draw the score at the specified canvas width (top right corner)
    public void draw(Canvas canvas, int canvasWidth) {
        // Position the text a few pixels inset from the edge
        canvas.drawText("Cash: $" + score, canvasWidth - 20, 60, paint);
    }
}
