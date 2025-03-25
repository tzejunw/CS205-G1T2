package com.example.cs205_g1t2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Game extends SurfaceView implements SurfaceHolder.Callback {
    private final Player player;
    private GameLoop gameLoop;
    private final List<Process> processes = new ArrayList<>();
    private Process selectedProcess;
    private float initialTouchX, initialTouchY;
    private static final float CLICK_THRESHOLD = 20f;

    // Position configuration
    private final PointF[] targetPositions;
    private int nextTargetIndex = 0;
    private float downY;
    private static final float UP_Y = 200;
    private static final float HORIZONTAL_SPACING = 300;
    // Execution slots (4 positions, 2 processes each)
    private final PointF[] executionSlots = {
            new PointF(200, 200), new PointF(500, 200),
            new PointF(200, 400), new PointF(500, 400)
    };

    // Process layout configuration
    private final Process[] occupiedSlots = new Process[4]; // Track slot usage

    public Game(Context context) {
        super(context);
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        // Initialize target positions array
        targetPositions = new PointF[4];

        // Get screen dimensions for dynamic positioning
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        downY = metrics.heightPixels - 200; // 200px from bottom

        // Initialize target positions
        // Create 8 processes at bottom
        float startX = 100;
        float spacing = (metrics.widthPixels - 200) / 7;
        for(int i = 0; i < 8; i++) {
            processes.add(new Process(
                    startX + (i * spacing),
                    metrics.heightPixels - 200,
                    Color.RED
            ));
        }



        gameLoop = new GameLoop(this, surfaceHolder);
        player = new Player(getContext(), metrics.widthPixels/2f, metrics.heightPixels/2f, 30);
        setFocusable(true);



    }

    public void addProcess(Process process) {
        // Position processes evenly at bottom
        float x = 200 + (processes.size() * HORIZONTAL_SPACING);
        processes.add(new Process(x, downY, Color.RED));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = x;
                initialTouchY = y;
                selectedProcess = getProcessAt(x, y);
                if (selectedProcess != null) {
                    selectedProcess.setSelected(true);
                } else {
                    player.setPosition(x, y);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (selectedProcess == null) {
                    player.setPosition(x, y);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (selectedProcess != null) {
                    if (isClick(x, y)) {
                        moveProcessToTarget(selectedProcess);
                    }
                    selectedProcess.setSelected(false);
                    selectedProcess = null;
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean isClick(float x, float y) {
        float dx = x - initialTouchX;
        float dy = y - initialTouchY;
        return Math.hypot(dx, dy) < CLICK_THRESHOLD;
    }

    private Process getProcessAt(float x, float y) {
        for (Process process : processes) {
            if (process.contains(x, y)) {
                return process;
            }
        }
        return null;
    }

    private void moveProcessToTarget(Process process) {
        if (process.isExecuting()) {
            // Return to down position
            returnProcess(process);
        } else {
            // Find first empty slot
            for(int i = 0; i < occupiedSlots.length; i++) {
                if(occupiedSlots[i] == null) {
                    process.moveToExecution(
                            executionSlots[i].x,
                            executionSlots[i].y
                    );
                    occupiedSlots[i] = process;
                    break;
                }
            }
        }
    }

    private void returnProcess(Process process) {
        // Find and clear the slot
        for(int i = 0; i < occupiedSlots.length; i++) {
            if(occupiedSlots[i] == process) {
                occupiedSlots[i] = null;
                break;
            }
        }
        process.returnToDown();
    }


    // SurfaceView callbacks
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        gameLoop.startLoop();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {}

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawColor(Color.BLACK);

        // Draw execution slots
        Paint slotPaint = new Paint();
        slotPaint.setColor(Color.argb(50, 0, 255, 0));
        for(PointF pos : executionSlots) {
            canvas.drawCircle(pos.x, pos.y, 60, slotPaint);
        }

        // Draw processes
        for(Process p : processes) {
            p.getPaint().setColor(p.isExecuting() ? Color.GREEN : Color.RED);
            p.draw(canvas);
        }
        player.draw(canvas);
        drawFPS(canvas);
    }

    public void drawFPS(Canvas canvas) {
        String averageFPS = Integer.toString((int) gameLoop.getAverageFPS());
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.magenta));
        paint.setTextSize(50);
        canvas.drawText("FPS: " + averageFPS, 1700, 100, paint);
    }

    public void update() {
        player.update();
    }
}
