package com.example.cs205_g1t2;

import java.io.IOException;
import java.io.InputStream;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;

import java.util.*;

public class Process {
    private static int nextId = 1;

    private final String label;
    private final float x, y;
    private final float radius = 80f;
    private final Paint paint;
    private Bitmap emojiHappy, emojiWorried, emojiPanic, emojiClock;
    private final Context context;
    private boolean selected = false;
    private boolean executing = false;
    private boolean completed = false;
    private long executionStartTime;
    private final long executingDuration;
    private final long creationTime;
    private final long pendingDuration; // time allowed in red area
    private ProcessListener listener;
    private final Map<Resource.Type, Integer> requiredResources = new HashMap<>();
    private final Map<Resource.Type, Integer> allocatedResources = new HashMap<>();
    private boolean resourcesSatisfied = false;
    private final List<Resource> allocatedResourceObjects = new ArrayList<>();
    private float dialogAlpha = 0;
    private static final float DIALOG_ANIMATION_SPEED = 0.1f;

    public interface ProcessListener {
        void onTimerFinished(Process process);
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public boolean getResourcesSatisfied(){
        return resourcesSatisfied;
    }

    public Process(Context context, float x, float y, int color) {
        this.context = context;
        this.x = x;
        this.y = y;
        int processId = nextId++;
        this.label = "P" + processId;
        this.executingDuration = 1000 + (long) (Math.random() * 5000); // random duration between 5 and 10 seconds
        this.pendingDuration = 15000 + (long)(Math.random() * 5000);
        this.creationTime = System.currentTimeMillis();

        paint = new Paint();
        paint.setColor(color);

        try {
            emojiHappy = loadEmojiFromAssets("emoji_happy.png");
            emojiWorried = loadEmojiFromAssets("emoji_worried.png");
            emojiPanic = loadEmojiFromAssets("emoji_panic.png");
            emojiClock = loadEmojiFromAssets("emoji_clock.png");
        } catch (IOException e) {
            e.printStackTrace();
        }

        requiredResources.put(Resource.Type.CPU, (int)(Math.random() * 2) + 1);  // 1-2 CPUs
        requiredResources.put(Resource.Type.MEMORY, (int)(Math.random() * 2) + 1);  // 1-2 Memory

        // Initialize allocated resources
        for (Resource.Type type : Resource.Type.values()) {
            allocatedResources.put(type, 0);
        }
    }

    private Bitmap loadEmojiFromAssets(String fileName) throws IOException {
        InputStream inputStream = context.getAssets().open(fileName);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
        return bitmap;
    }

    public boolean allocateResource(Resource resource) {
        Resource.Type type = resource.getType();

        if (allocatedResources.get(type) < requiredResources.get(type)) {
            allocatedResources.put(type, allocatedResources.get(type) + 1);
            this.allocatedResourceObjects.add(resource); // Keep track of allocated resources
            resource.setAllocated(true);
            checkResourceSatisfaction();
            return true;
        }

        return false;
    }

    public void resetAllocatedResources() {
        for (Resource resource : allocatedResourceObjects) {
            resource.resetPosition();
            resource.setAllocated(false);
        }
        // Clear both collections
        allocatedResourceObjects.clear();

        // Reset counts in the map
        for (Resource.Type type : Resource.Type.values()) {
            allocatedResources.put(type, 0);
        }
    }

    public void checkResourceSatisfaction() {
        resourcesSatisfied = true;

        for (Resource.Type type : Resource.Type.values()) {
            if (allocatedResources.get(type) < requiredResources.get(type)) {
                resourcesSatisfied = false;
                break;
            }
        }

        if (resourcesSatisfied && !executing) {
            executing = true;
            executionStartTime = System.currentTimeMillis();
        }
    }

    public void setListener(ProcessListener listener) {
        this.listener = listener;
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

        float dialogWidth = 200;
        float dialogHeight = 130;
        float dialogX = x - dialogWidth / 2;
        float dialogY = y + radius + 10; // Position below the process

        // Draw dialog background
        Paint dialogPaint = new Paint();
        dialogPaint.setColor(Color.argb((int) (220 * dialogAlpha), 255, 255, 255));

        // Draw rounded rectangle for dialog
        RectF dialogRect = new RectF(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight);
        canvas.drawRoundRect(dialogRect, 15, 15, dialogPaint);

        // Draw dialog border
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        borderPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(dialogRect, 15, 15, borderPaint);

        // Draw resource requirements inside dialog
        float iconSize = 30f;
        float startX = dialogX + 25;
        float startY = dialogY + 40;
        float textOffset = 90;

        // For the text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24);

        // Draw each resource type in the dialog
        for (Resource.Type type : Resource.Type.values()) {
            Paint resourcePaint = new Paint();
            resourcePaint.setColor(type.getColor());

            canvas.drawCircle(startX + iconSize / 2, startY, iconSize / 2, resourcePaint);

            String text = allocatedResources.get(type) + "/" + requiredResources.get(type);
            canvas.drawText(text, startX + textOffset, startY + 8, textPaint);

            // You could add visual indicators for fulfilled requirements
            if (allocatedResources.get(type) >= requiredResources.get(type)) {
                Paint checkPaint = new Paint();
                checkPaint.setColor(Color.GREEN);
                checkPaint.setStrokeWidth(3);
                // Draw a checkmark or other indicator
                canvas.drawLine(startX + textOffset + 50, startY - 5, startX + textOffset + 60, startY + 5, checkPaint);
                canvas.drawLine(startX + textOffset + 60, startY + 5, startX + textOffset + 70, startY - 10, checkPaint);
            }

            // Move to next resource type (next row in dialog)
            startY += 50;
        }

        // Draw execution progress arc if executing
        if (executing && !completed) {
            long elapsed = System.currentTimeMillis() - executionStartTime;
            float progress = Math.min(1f, (float) elapsed / executingDuration);

            Paint arcPaint = new Paint();
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeWidth(6);

            // Arc bounds
            float left = x - radius + 8;
            float top = y - radius + 8;
            float right = x + radius - 8;
            float bottom = y + radius - 8;

            canvas.drawArc(left, top, right, bottom, -90, 360 * progress, false, arcPaint);
        }

        // Draw execution progress arc for red processes before they expire
        else if (!executing && !completed) {
            long elapsed = System.currentTimeMillis() - creationTime;
            float progress = Math.min(1f, (float) elapsed / pendingDuration);
            Paint arcPaint = new Paint();
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeWidth(6);
            // Use red for pending progress
            float left = x - radius + 8;
            float top = y - radius + 8;
            float right = x + radius - 8;
            float bottom = y + radius - 8;
            canvas.drawArc(left, top, right, bottom, -90, 360 * progress, false, arcPaint);
        }

        Bitmap emoji;
        if (executing && !completed) {
            emoji = emojiClock;
        } else {
            long elapsed = System.currentTimeMillis() - creationTime;
            if (elapsed < pendingDuration * 0.5f) {
                emoji = emojiHappy;
            } else if (elapsed < pendingDuration * 0.75f) {
                emoji = emojiWorried;
            } else {
                emoji = emojiPanic;
            }
        }

        if (emoji != null) {
            float size = 40f;
            float left = x + radius - size;
            float top = y - radius;
            RectF dst = new RectF(left, top, left + size, top + size);
            canvas.drawBitmap(emoji, null, dst, null);
        }

        // Draw label text
        Paint textPaint1 = new Paint();
        textPaint1.setColor(Color.BLACK);
        textPaint1.setTextSize(30);
        textPaint1.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x, y + 10, textPaint1);
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


    public void update() {
        if (y < 300 && !completed) {
            if (dialogAlpha < 1.0f) {
                dialogAlpha += DIALOG_ANIMATION_SPEED;
                if (dialogAlpha > 1.0f) dialogAlpha = 1.0f;
            }
        } else {
            dialogAlpha = 0;
        }

        long currentTime = System.currentTimeMillis();
        // If not executing (red) and not already completed, check pending time.
        if (!executing && !completed) {
            if (currentTime - creationTime >= pendingDuration) {
                if (listener != null) {
                    listener.onTimerFinished(this);
                    this.completed = true;
                }
            }
        }
        if (executing && resourcesSatisfied && !completed) {
            long elapsed = System.currentTimeMillis() - executionStartTime;
            if (elapsed >= executingDuration) {
                completed = true;
                paint.setColor(0xFF00FF00); // turn green when done
                if (listener != null) {
                    listener.onTimerFinished(this);
                }
            }
        }
    }

    public Paint getPaint() {
        return paint;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getPendingDuration() {
        return pendingDuration;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
