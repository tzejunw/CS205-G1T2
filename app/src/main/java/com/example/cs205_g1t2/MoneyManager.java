package com.example.cs205_g1t2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;

public class MoneyManager {
    private int money;
    private Paint paint;

    public MoneyManager(Context context) {
        money = 50;
        paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.RIGHT);
    }

    public void addMoney(int amount) {
        money += amount;
    }

    public void minusMoney(int amount){
        money -= amount;
    }

    // Reset the score to zero
    public void resetMoney() {
        money = 50;
    }

    public int getMoney() {
        return money;
    }

    // Draw the score at the specified canvas width (top right corner)
    public void draw(Canvas canvas, int canvasWidth) {
        // Position the text a few pixels inset from the edge
        canvas.drawText("Cash: $" + money, canvasWidth - 200, 180, paint);
    }
}