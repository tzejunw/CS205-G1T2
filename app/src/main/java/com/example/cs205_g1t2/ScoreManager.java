package com.example.cs205_g1t2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class ScoreManager {
    private int score;
    private final Paint paint;

    public ScoreManager(Context context) {
        score = 0;
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.RIGHT);
    }

    public void addScore(int amount) {
        score += amount;
    }

    // Reset the score to zero
    public void resetScore() {
        score = 0;
    }

    public int getScore() {
        return score;
    }

    // Draw score
    public void draw(Canvas canvas, int canvasHeight) {
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        canvas.drawText("" + score, 250, canvas.getHeight() - fontMetrics.descent, paint);
    }
}
