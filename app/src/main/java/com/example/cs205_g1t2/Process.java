package com.example.cs205_g1t2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.*;

public class Process {
    private static int nextId = 1;

    private final int processId;
    private final String label;
    private float x, y;
    private final float radius = 80f;
    private final Paint paint;
    private boolean selected = false;
    private boolean executing = false;
    private boolean completed = false;

    private float originalX, originalY;

    private long executionStartTime;
    private long duration = 5000; // default 5 seconds execution
    // New fields for pending (red) processes.
    private final long creationTime;
    private static final long PENDING_DURATION = 20000; // time allowed in red area

    private ProcessListener listener;

    private Map<Resource.Type, Integer> requiredResources = new HashMap<>();
    private Map<Resource.Type, Integer> allocatedResources = new HashMap<>();
    private boolean resourcesSatisfied = false;

    // Also track the actual Resource objects for position resetting
    private List<Resource> allocatedResourceObjects = new ArrayList<>();

    private boolean showDialog = false;
    private float dialogAlpha = 0;
    private static final float DIALOG_ANIMATION_SPEED = 0.1f;

    // Implemented by Game
    public interface ProcessListener {
        void onTimerFinished(Process process);
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public Process(float x, float y, int color) {
        this.x = x;
        this.y = y;
        this.originalX = x;
        this.originalY = y;

        this.processId = nextId++;
        this.label = "P" + processId;

        this.creationTime = System.currentTimeMillis();

        paint = new Paint();
        paint.setColor(color);

        requiredResources.put(Resource.Type.CPU, (int)(Math.random() * 2) + 1);  // 1-2 CPUs
        requiredResources.put(Resource.Type.MEMORY, (int)(Math.random() * 2) + 1);  // 1-2 Memory

        // Initialize allocated resources
        for (Resource.Type type : Resource.Type.values()) {
            allocatedResources.put(type, 0);
        }
    }

    // New methods for resource management
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

    private void checkResourceSatisfaction() {
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

    public void setShowDialog(boolean showDialog) {
        this.showDialog = showDialog;
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

        // Only draw resource requirements when process is at the top (not being dragged)
        if (y < 200) { // This checks if the process is positioned at the top of the screen
            // Create dialog box background below the process
            float dialogWidth = 180;
            float dialogHeight = 120;
            float dialogX = x - dialogWidth/2;
            float dialogY = y + radius + 10; // Position below the process

            // Draw dialog background
            Paint dialogPaint = new Paint();
            dialogPaint.setColor(Color.argb((int)(220 * dialogAlpha), 255, 255, 255));

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
            float startX = dialogX + 20;
            float startY = dialogY + 30;
            float textOffset = 75;

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(24);

            // Draw each resource type in the dialog
            for (Resource.Type type : Resource.Type.values()) {
                Paint resourcePaint = new Paint();
                resourcePaint.setColor(type.getColor());

                canvas.drawCircle(startX + iconSize/2, startY, iconSize/2, resourcePaint);

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

        // Draw execution progress arc for red processes before they expire
        else if (!executing && !completed && paint.getColor() == Color.RED) {
            long elapsed = System.currentTimeMillis() - creationTime;
            float progress = Math.min(1f, (float) elapsed / PENDING_DURATION);
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
        if (y < 200 && !completed) { // Process is at top
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
            if (currentTime - creationTime >= PENDING_DURATION) {
                if (listener != null) {
                    listener.onTimerFinished(this);
                }
            }
        }
        if (executing && resourcesSatisfied && !completed) {
            long elapsed = System.currentTimeMillis() - executionStartTime;
            if (elapsed >= duration) {
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

    public int getProcessId() {
        return processId;
    }

    public String getLabel() {
        return label;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    // Add getters
    public Map<Resource.Type, Integer> getRequiredResources() {
        return requiredResources;
    }

    public Map<Resource.Type, Integer> getAllocatedResources() {
        return allocatedResources;
    }
}
