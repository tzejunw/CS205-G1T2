package com.example.cs205_g1t2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

import java.io.IOException;
import java.io.InputStream;

public class Resource {
    public enum Type {
        CPU(android.graphics.Color.BLUE),
        MEMORY(android.graphics.Color.GREEN);
        private final int color;
        Type(int color) {
            this.color = color;
        }
        public int getColor() {
            return color;
        }
    }

    private final Type type;
    private float x, y;
    private final float radius = 60f;
    private final Paint paint;
    private boolean selected = false;
    private boolean allocated = false;
    private final float originalX, originalY;
    private Bitmap resourceImage;
    private final Context context;
    private boolean blocked = false;
    private Bitmap originalBitmap;
    private Bitmap bitmap;

    public Resource(Context context, Type type, float x, float y) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.originalX = x;
        this.originalY = y;
        this.context = context;

        // Load the image based on resource type
        loadImageFromAssets();

        paint = new Paint();
        paint.setColor(type.getColor());
    }

    // Load image from assets
    private void loadImageFromAssets() {
        try {
            String fileName = "";
            if (type == Type.CPU) {
                fileName = "resource_blue.png";
            } else if (type == Type.MEMORY) {
                fileName = "resource_green.png";
            }

            // Open an input stream from the assets folder
            InputStream inputStream = context.getAssets().open(fileName);

            // Decode the stream into a bitmap
            resourceImage = BitmapFactory.decodeStream(inputStream);

            // Close the input stream to free resources
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(Canvas canvas) {
        if (allocated) {
            return;
        }

        if (resourceImage != null) {
            // Calculate image position (center the image at the current coordinates)
            float left = x - radius;
            float top = y - radius;
            float right = x + radius;
            float bottom = y + radius;

            canvas.drawBitmap(resourceImage, null, new RectF(left, top, right, bottom), null);
        } else {
            canvas.drawCircle(x, y, radius, paint);
        }

        if (selected) {
            Paint border = new Paint();
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(5);
            border.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, radius + 5, border);
        }

        if (blocked) {
            // Draw fire bitmap instead of normal resource
            canvas.drawBitmap(bitmap, x - bitmap.getWidth()/2, y - bitmap.getHeight()/2, null);
        }
    }

    public boolean contains(float touchX, float touchY) {
        float dx = touchX - x;
        float dy = touchY - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void resetPosition() {
        this.x = originalX;
        this.y = originalY;
        this.allocated = false;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public void setAllocated(boolean allocated) {
        this.allocated = allocated;
    }

    public Type getType() {
        return type;
    }

    public void block(Bitmap fireBitmap) {
        this.blocked = true;
        this.originalBitmap = this.bitmap; // Save original bitmap
        this.bitmap = fireBitmap;
    }

    public void unblock() {
        this.blocked = false;
        this.bitmap = originalBitmap;
    }

    public boolean isBlocked() {
        return blocked;
    }

}

