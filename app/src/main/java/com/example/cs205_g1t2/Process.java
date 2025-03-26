package com.example.cs205_g1t2;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Process {
    private static int nextId = 1;

    private final int processId;
    private final String label;
    private float x, y;
    private final float radius = 50f;
    private final Paint paint;
    private boolean selected = false;
    private boolean executing = false;
    private boolean completed = false;

    private float originalX, originalY;

    private long executionStartTime;
    private long duration = 5000; // default 5 seconds execution

    public Process(float x, float y, int color) {
        this.x = x;
        this.y = y;
        this.originalX = x;
        this.originalY = y;

        this.processId = nextId++;
        this.label = "P" + processId;

        paint = new Paint();
        paint.setColor(color);
    }

    public void draw(Canvas canvas) {
        // Draw the process circle
        canvas.drawCircle(x, y, radius, paint);

        // Draw a white border if selected
        if (selected) {
            Paint border = new Paint();
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(5);
            border.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, radius + 5, border);
        }

        // Draw execution progress arc if executing
        if (executing && !completed) {
            long elapsed = System.currentTimeMillis() - executionStartTime;
            float progress = Math.min(1f, (float) elapsed / duration);

            Paint arcPaint = new Paint();
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeWidth(6);

            // Optional: gradient from red to green based on progress
//            int green = (int) (progress * 255);
//            int red = 255 - green;
//            arcPaint.setColor(android.graphics.Color.rgb(red, green, 0));

            // Arc bounds
            float left = x - radius + 8;
            float top = y - radius + 8;
            float right = x + radius - 8;
            float bottom = y + radius - 8;

            canvas.drawArc(left, top, right, bottom, -90, 360 * progress, false, arcPaint);
        }

        // Draw label text
        Paint textPaint = new Paint();
        textPaint.setColor(0xFFFFFFFF); // white
        textPaint.setTextSize(30);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, y + 10, textPaint);
    }

    public boolean contains(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isExecuting() {
        return executing;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void moveToExecution(float execX, float execY) {
        this.x = execX;
        this.y = execY;
        this.executing = true;
        this.executionStartTime = System.currentTimeMillis();
        this.completed = false;
    }

    public void returnToDown() {
        this.x = originalX;
        this.y = originalY;
        this.executing = false;
        this.completed = false;
    }

    public void update() {
        if (executing && !completed) {
            long elapsed = System.currentTimeMillis() - executionStartTime;
            if (elapsed >= duration) {
                completed = true;
                paint.setColor(0xFF00FF00); // turn green when done
            }
        }
    }

    public Paint getPaint() {
        return paint;
    }

    public int getProcessId() {
        return processId;
    }

    public String getLabel() {
        return label;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
