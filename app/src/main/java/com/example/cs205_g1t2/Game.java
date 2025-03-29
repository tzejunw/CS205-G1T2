package com.example.cs205_g1t2;

import static android.os.SystemClock.sleep;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Game extends SurfaceView implements SurfaceHolder.Callback, Process.ProcessListener {
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
    private boolean gameOver = false;

    private long lastProcessSpawnTime = System.currentTimeMillis();
    private static final long PROCESS_SPAWN_INTERVAL = 5000; // every 5 seconds

    // In Game class variables
    private final boolean[] blockedSlots = new boolean[4]; // Track blocked slots
    private static final long BLOCK_DURATION = 5000; // 5 seconds block
    private static final float BLOCK_CHANCE = 0.005f; // 0.1% chance per frame

    private List<Resource> resources = new ArrayList<>();
    private Resource selectedResource = null;
    private static final int RESOURCES_PER_TYPE = 5;

    private static final int MAX_PROCESSES = 8; // Maximum number of processes allowed

    public Game(Context context) {
        super(context);
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        // Initialize target positions array
        targetPositions = new PointF[4];

        // Get screen dimensions for dynamic positioning
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        downY = metrics.heightPixels - 200; // 200px from bottom

        createResources();

        gameLoop = new GameLoop(this, surfaceHolder);
        player = new Player(getContext(), metrics.widthPixels/2f, metrics.heightPixels/2f, 30);
        setFocusable(true);

    }

    private void createResources() {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float screenHeight = metrics.heightPixels;

        // Add CPU resources (blue) on left side
        float startX = 100;
        float bottomY = screenHeight - 350;
        float spacing = 160f;

        for (int i = 0; i < RESOURCES_PER_TYPE; i++) {
            Resource cpuResource = new Resource(getContext(), Resource.Type.CPU,
                    startX + (i % 3) * spacing,
                    bottomY - (i / 3) * spacing);
            resources.add(cpuResource);
        }

        // Add Memory resources (green) on right side
        startX = screenWidth - 1600;

        for (int i = 0; i < RESOURCES_PER_TYPE; i++) {
            Resource memoryResource = new Resource(getContext(), Resource.Type.MEMORY,
                    startX + (i % 3) * spacing,
                    bottomY - (i / 3) * spacing);
            resources.add(memoryResource);
        }
    }

    public void addProcess(Process process) {
        // Position processes evenly at bottom
        float x = 200 + (processes.size() * HORIZONTAL_SPACING);
        process.setListener(this);
        processes.add(new Process(x, downY, Color.RED));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                // Check if restart button is pressed
                if (x > getWidth()/2 - 200 && x < getWidth()/2 + 200 &&
                        y > getHeight()/2 && y < getHeight()/2 + 150) {
                    resetGame();
                    return true;
                }
            }
            return true; // Consume all touches when game over
        }
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = x;
                initialTouchY = y;

                // Check for resource selection first
                selectedResource = getResourceAt(x, y);

                if (selectedResource != null) {
                    selectedResource.setSelected(true);
                } else {
                    // Then check for process selection
                    selectedProcess = getProcessAt(x, y);
                    if (selectedProcess != null) {
                        selectedProcess.setSelected(true);
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (selectedResource != null) {
                    selectedResource.setPosition(x, y);
                } else if (selectedProcess != null) {
                    // Keep existing process movement code
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (selectedResource != null) {
                    Process targetProcess = getProcessAt(x, y);
                    if (targetProcess != null) {
                        if (targetProcess.allocateResource(selectedResource)) {
                            selectedResource.setAllocated(true);
                        } else {
                            selectedResource.resetPosition();
                        }
                    } else {
                        selectedResource.resetPosition();
                    }

                    selectedResource.setSelected(false);
                    selectedResource = null;
                } else if (selectedProcess != null) {
                    // Keep existing process release code
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private Resource getResourceAt(float x, float y) {
        for (Resource resource : resources) {
            if (!resource.isAllocated() && resource.contains(x, y)) {
                return resource;
            }
        }
        return null;
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
                if(occupiedSlots[i] == null && !blockedSlots[i]) {
                    process.moveToExecution(executionSlots[i].x, executionSlots[i].y);
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

        // Draw processes
        for(Process p : processes) {
            p.draw(canvas);
        }

        // Draw resources
        for (Resource r : resources) {
            r.draw(canvas);
        }

        player.draw(canvas);
        drawFPS(canvas);

        if (gameOver) {
            drawGameOver(canvas);
        }
    }

    public void drawFPS(Canvas canvas) {
        String averageFPS = Integer.toString((int) gameLoop.getAverageFPS());
        Paint paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.magenta));
        paint.setTextSize(50);
        canvas.drawText("FPS: " + averageFPS, 1700, 100, paint);
    }

    //    Original
//    public void update() {
//        player.update();
//        for (Process p : processes) {
//            p.update();
//        }
//    }

    private void drawGameOver(Canvas canvas) {
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(100);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", canvas.getWidth()/2f, canvas.getHeight()/2f - 100, textPaint);

        // Draw restart button
        Paint buttonPaint = new Paint();
        buttonPaint.setColor(Color.BLUE);
        Rect restartButton = new Rect(
                canvas.getWidth()/2 - 200,
                canvas.getHeight()/2,
                canvas.getWidth()/2 + 200,
                canvas.getHeight()/2 + 150
        );
        canvas.drawRect(restartButton, buttonPaint);

        // Button text
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60);
        canvas.drawText("RESTART", canvas.getWidth()/2f, canvas.getHeight()/2 + 90, textPaint);
    }

    // After a process runs for a while (is green for sometime), terminate it (it disappears)
    public void update() {
        if (gameOver) return;

        // Existing process updates
        player.update();
        Iterator<Process> iterator = processes.iterator();
        while (iterator.hasNext()) {
            Process p = iterator.next();
            p.update();
            if (p.isCompleted()) {
                p.resetAllocatedResources(); // Reset resources used by this process
                iterator.remove();
            }
        }

        // New slot blocking logic
        if (Math.random() < BLOCK_CHANCE) {
            int slot = (int) (Math.random() * 4);
            if (!blockedSlots[slot]) {
                blockedSlots[slot] = true;

                // Vibration code
                Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50); // Legacy vibration
                    }
                }

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    blockedSlots[slot] = false;
                }, BLOCK_DURATION);
            }
        }

        // Existing process spawning
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProcessSpawnTime >= PROCESS_SPAWN_INTERVAL) {
            spawnNewProcess();
            lastProcessSpawnTime = currentTime;
        }
    }

    private static final float PROCESS_RADIUS = 50; // Example radius
    private static final float MIN_DISTANCE = 6 * PROCESS_RADIUS;
    private static final float PROCESS_GAP = 25;

    private void spawnNewProcess() {
        if (processes.size() >= MAX_PROCESSES) {
            return; // Don't spawn more processes if we've reached the limit
        }

        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float horizontalPadding = 200;

        // Top positioning - use a fixed Y value near the top
        float topY = 100; // This places processes at the top of the screen

        // Grid configuration
        float availableWidth = screenWidth - 2 * horizontalPadding;
        int processesPerRow = (int) (availableWidth / MIN_DISTANCE);
        processesPerRow = Math.max(processesPerRow, 1); // Ensure at least 1 per row
        float horizontalSpacing = availableWidth / processesPerRow;

        // Calculate X position based on existing processes at the top
        int topProcessCount = 0;

        for (Process p : processes) {
            if (p.getY() < 150) { // Count only processes at the top
                topProcessCount++;
            }
        }

        // Calculate new position
        float x = horizontalPadding + (topProcessCount % processesPerRow) * horizontalSpacing;
        float y = topY;

        // Collision detection and repositioning
        boolean positionValid = false;
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts && !positionValid; attempt++) {
            positionValid = true;
            float MIN_SEPARATION = 2 * PROCESS_RADIUS + PROCESS_GAP;

            // Check for collisions with other processes
            for (Process p : processes) {
                if (p.getY() < 150) { // Only check against top processes
                    float dx = x - p.getX();
                    float dy = y - p.getY();
                    float distanceSq = dx*dx + dy*dy;

                    if (distanceSq < MIN_SEPARATION * MIN_SEPARATION) {
                        positionValid = false;
                        x += horizontalSpacing;

                        if (x > screenWidth - horizontalPadding - PROCESS_RADIUS) {
                            x = horizontalPadding;
                            y += MIN_DISTANCE; // Move to next row if needed
                        }
                        break;
                    }
                }
            }
        }

        if (positionValid) {
            Process newProcess = new Process(x, y, Color.RED);
            newProcess.setListener(this);
            processes.add(newProcess);
        }
    }


    @Override
    public void onTimerFinished(Process process) {
        // A timer on a red process has expired.
        // Set game over and stop the game loop.
        if (!process.isExecuting()) {
            gameOver = true;
//          // gameLoop.stopLoop();
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void resetGame() {
        gameOver = false;

        // Reset processes
        processes.clear();
        for(int i = 0; i < occupiedSlots.length; i++) {
            occupiedSlots[i] = null;
        }

        // Reset player position
        Context context = getContext();
        if(context == null) {
            return;
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        player.setPosition(metrics.widthPixels/2f, metrics.heightPixels/2f);

        for (Resource r : resources) {
            r.resetPosition();
            r.setAllocated(false);
        }

        // Restart game loop if needed
        if (!gameLoop.isAlive()) {
            gameLoop = new GameLoop(this, getHolder());
            gameLoop.startLoop();
        }
    }

}
