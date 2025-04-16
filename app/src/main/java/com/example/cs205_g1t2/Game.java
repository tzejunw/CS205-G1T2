package com.example.cs205_g1t2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Paint;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import com.example.cs205_g1t2.leaderboard.LeaderboardDbHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Game extends SurfaceView implements SurfaceHolder.Callback, Process.ProcessListener {
    public GameLoop gameLoop;
    private final ScoreManager scoreManager;
    private final MoneyManager moneyManager;
    private final List<Process> processes = new ArrayList<>();
    private boolean gameOver = false;
    private long lastProcessSpawnTime = System.currentTimeMillis();
    private final boolean[] blockedSlots = new boolean[4]; // Track blocked slots
    private static final long PROCESS_SPAWN_INTERVAL = 3500; // every 3.5 seconds
    private static final long BLOCK_DURATION = 5000; // 5 seconds block
    private static final float BLOCK_CHANCE = 0.005f; // 0.5% chance per frame
    private final List<Resource> resources = new ArrayList<>();
    private Resource selectedResource = null;
    private static final int RESOURCES_PER_TYPE = 5;
    private static final int MAX_PROCESSES = 8; // Maximum number of processes allowed
    private int currentHealth = 3;
    private final int maxHealth = 3;
    private Bitmap healthFilledBitmap;
    private Bitmap healthEmptyBitmap;
    private final PointF healthIconPosition = new PointF(1875, 975); // Top-left position
    private static final float HEALTH_ICON_SIZE_DP = 50f;
    private Bitmap attackerBitmap;
    private Bitmap fireBitmap;
    private Resource blockedResource;
    private static final float FIRE_SIZE_DP = 40f;
    private static final float ATTACKER_WIDTH_DP = 300f; // Width-based scaling
    private final long gameStartTime;
    private static final long ATTACKER_DELAY = 5000; // 5 seconds in milliseconds
    private long lastAttackTime = 0;
    private static final long ATTACKER_COOLDOWN = 10000; // 10 seconds between attacks
    private SoundPool soundPool;
    private final int attackerSoundId;
    private boolean soundsLoaded = false;
    private Bitmap backgroundBitmap;
    private Rect restartButton;
    private Rect homeButton;
    private static final float PROCESS_RADIUS = 50;
    private static final float MIN_DISTANCE = 5 * PROCESS_RADIUS;

    public Game(Context context) {
        super(context);
        scoreManager = new ScoreManager(context);
        moneyManager = new MoneyManager(context);
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        gameStartTime = System.currentTimeMillis();
        try {
            // Load from assets folder
            Bitmap originalFilled = BitmapFactory.decodeStream(context.getAssets().open("health_filled.png"));
            Bitmap originalEmpty = BitmapFactory.decodeStream(context.getAssets().open("health_empty.png"));
            Bitmap originalAttacker = BitmapFactory.decodeStream(context.getAssets().open("attacker.png"));
            Bitmap originalFire = BitmapFactory.decodeStream(context.getAssets().open("fire.png"));

            // Convert dp to pixels
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            float density = metrics.density;
            float healthIconSizePx = HEALTH_ICON_SIZE_DP * density;

            try {
                // Load background from assets
                InputStream bgStream = context.getAssets().open("background.png");
                backgroundBitmap = BitmapFactory.decodeStream(bgStream);
                bgStream.close();

                // Scale to screen size
                backgroundBitmap = Bitmap.createScaledBitmap(
                        backgroundBitmap,
                        metrics.widthPixels,
                        metrics.heightPixels,
                        true
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Scale bitmaps proportionally
            healthFilledBitmap = Bitmap.createScaledBitmap(
                    originalFilled,
                    (int) healthIconSizePx,
                    (int) healthIconSizePx,
                    true
            );
            healthEmptyBitmap = Bitmap.createScaledBitmap(
                    originalEmpty,
                    (int) healthIconSizePx,
                    (int) healthIconSizePx,
                    true
            );

            int fireSizePx = (int)(FIRE_SIZE_DP * density);
            fireBitmap = scaleBitmapMaintainAspect(originalFire, fireSizePx, fireSizePx);

            int attackerWidthPx = (int)(ATTACKER_WIDTH_DP * density);
            attackerBitmap = scaleBitmapMaintainAspect(originalAttacker, attackerWidthPx, 0);

            // Recycle original bitmaps to save memory
            originalFilled.recycle();
            originalEmpty.recycle();
            originalAttacker.recycle();
            originalFire.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        }

        createResources();
        gameLoop = new GameLoop(this, surfaceHolder);
        setFocusable(true);

        // Initialize SoundPool
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build();

        // Load sound file
        attackerSoundId = soundPool.load(context, R.raw.attacker_appear, 1);

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            if (status == 0) soundsLoaded = true;
        });
    }

    private Bitmap scaleBitmapMaintainAspect(Bitmap source, int targetWidth, int targetHeight) {
        // Calculate aspect-ratio preserved dimensions
        float aspectRatio = (float) source.getWidth() / source.getHeight();
        int width = targetWidth;
        int height = targetHeight;

        if(targetHeight == 0) {
            height = Math.round(targetWidth / aspectRatio);
        } else if(targetWidth == 0) {
            width = Math.round(targetHeight * aspectRatio);
        }

        return Bitmap.createScaledBitmap(
                source,
                width,
                height,
                true // Enable filtering for smooth scaling
        );
    }

    private void createResources() {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float screenHeight = metrics.heightPixels;

        // Add CPU resources (blue) on left side
        float startX = 240;
        float bottomY = screenHeight - 250;
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                if (restartButton != null && restartButton.contains(x, y)) {
                    resetGame();
                    return true;
                } else if (homeButton != null && homeButton.contains(x, y)) {
                    returnToMainMenu();
                    return true;
                }
            }
            return true; // Consume all touches when game over
        }

        // check if buy CPU button is pressed
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            if (x >= 150 && x <= 500 &&
                    y >= getHeight() - 150 && y <= getHeight() - 50) {
                synchronized (resources) { // Synchronize resource addition
                    addResource(Resource.Type.CPU);
                }
                return true;
            }
        }

        // check if buy RAM button is pressed
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            if (x >= 1300 && x <= 1700 &&
                    y >= getHeight() - 150 && y <= getHeight() - 50) {
                synchronized (resources) { // Synchronize resource addition
                    addResource(Resource.Type.MEMORY);
                }
                return true;
            }
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                // Check for resource selection first
                selectedResource = getResourceAt(x, y);

                if (selectedResource != null) {
                    selectedResource.setSelected(true);
                } else {
                    // Then check for process selection
                    Process selectedProcess = getProcessAt(x, y);
                    if (selectedProcess != null) {
                        selectedProcess.setSelected(true);
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (selectedResource != null) {
                    selectedResource.setPosition(x, y);
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
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private Resource getResourceAt(float x, float y) {
        synchronized (resources) { // Synchronize access when iterating
            for (Resource resource : resources) {
                if (!resource.isBlocked() && !resource.isAllocated() && resource.contains(x, y)) {
                    return resource;
                }
            }
        }
        return null;
    }

    private void blockRandomResource() {
        new Handler(Looper.getMainLooper()).post(() -> {
            // Select random unblocked resource
            List<Resource> unblockedResources = new ArrayList<>();
            for (Resource r : resources) {
                if (!r.isBlocked() && !r.isAllocated()) {
                    unblockedResources.add(r);
                }
            }

            if (!unblockedResources.isEmpty()) {
                blockedResource = unblockedResources.get(
                        (int) (Math.random() * unblockedResources.size()));
                blockedResource.block(fireBitmap);

                // Unblock after 10 seconds
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    blockedResource.unblock();
                    blockedResource = null;
                }, 10000);
            }
        });
    }

    private Process getProcessAt(float x, float y) {
        for (Process process : processes) {
            if (process.contains(x, y)) {
                return process;
            }
        }
        return null;
    }

    public void triggerAttacker() {
        // Show attacker with fade-out animation
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(1500);
        fadeOut.setFillAfter(true);

        if(soundsLoaded) {
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            float volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volume /= audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            soundPool.play(attackerSoundId, volume, volume, 1, 0, 1f);
        }

        post(() -> {
            ImageView attackerView = new ImageView(getContext());
            attackerView.setImageBitmap(attackerBitmap);
            attackerView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
            ));

            ((ViewGroup) getParent()).addView(attackerView);
            attackerView.startAnimation(fadeOut);

            // Block random resource after animation
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    post(() -> {  // Remove view on the main thread.
                        ((ViewGroup) getParent()).removeView(attackerView);
                        blockRandomResource();
                    });
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        });
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

        // Draw background
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);

        // Draw processes
        for(Process p : processes) {
            p.draw(canvas);
            long elapsed = System.currentTimeMillis() - p.getCreationTime();
            long pendingDuration = p.getPendingDuration();

            if (p.isExecuting()) {
                p.getPaint().setColor(Color.CYAN);
            }
            else if (elapsed < pendingDuration * 0.5) {
                p.getPaint().setColor(Color.GREEN);
            } else if (elapsed < pendingDuration * 0.75) {
                p.getPaint().setColor(Color.YELLOW);
            } else {
                p.getPaint().setColor(Color.RED);
            }
        }

        // Draw resources
        synchronized (resources){
            for (Resource r : resources) {
                r.draw(canvas);
            }
        }

        drawHealthSystem(canvas);
        scoreManager.draw(canvas, getWidth());

        drawBuyResource(canvas);
        moneyManager.draw(canvas, getWidth());

        if (gameOver) {
            drawGameOver(canvas);
        }
    }

    public void drawBuyResource(Canvas canvas) {
        // Draw CPU button(blue)
        Paint buttonPaint = new Paint();
        buttonPaint.setColor(Color.BLUE);
        RectF cpuButton = new RectF(
                150,
                canvas.getHeight()-150,
                500,
                canvas.getHeight()-50
        );
        canvas.drawRoundRect(cpuButton, 20, 20, buttonPaint);

        // CPU button text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60);
        canvas.drawText("Buy CPU", 180, getHeight()/2 + 600, textPaint);

        // Draw Memory button (green)
        buttonPaint.setColor(Color.GREEN);
        RectF memoryButton = new RectF(
                1300,
                canvas.getHeight()-150,
                1700,
                canvas.getHeight()-50
        );
        canvas.drawRoundRect(memoryButton, 20, 20, buttonPaint);

        // Memory button text
        canvas.drawText("Buy RAM", 1330, canvas.getHeight()-80, textPaint);
    }

    private void drawHealthSystem(Canvas canvas) {
        for (int i = 0; i < maxHealth; i++) {
            Bitmap healthBitmap = (i < currentHealth) ? healthFilledBitmap : healthEmptyBitmap;
            // Space between health icons
            float healthIconSpacing = 120f;
            canvas.drawBitmap(
                    healthBitmap,
                    healthIconPosition.x + (i * healthIconSpacing),
                    healthIconPosition.y,
                    null
            );
        }
    }

    public void cleanup() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private void drawGameOver(Canvas canvas) {
        Paint textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(100);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("GAME OVER", canvas.getWidth()/2f, canvas.getHeight()/2f - 100, textPaint);

        // Draw restart button
        Paint buttonPaint = new Paint();
        buttonPaint.setColor(Color.BLUE);
        int buttonWidth = 200;
        int buttonHeight = 150;
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;
        restartButton = new Rect(
                centerX - buttonWidth,
                centerY,
                centerX + buttonWidth,
                centerY + buttonHeight
        );

        canvas.drawRect(restartButton, buttonPaint);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60);
        canvas.drawText("RESTART", canvas.getWidth() / 2f, centerY + buttonHeight / 2f + 20, textPaint);

        buttonPaint.setColor(Color.DKGRAY);
        int gap = 20; // Gap between buttons
        int homeButtonTop = centerY + buttonHeight + gap;
        homeButton = new Rect(
                centerX - buttonWidth,
                homeButtonTop,
                centerX + buttonWidth,
                homeButtonTop + buttonHeight
        );
        canvas.drawRect(homeButton, buttonPaint);
        canvas.drawText("HOME", canvas.getWidth() / 2f, homeButtonTop + buttonHeight / 2f + 20, textPaint);
    }

    public void update() {
        if (gameOver) return;

        long currentTime = System.currentTimeMillis();

        // Existing process updates
        Iterator<Process> iterator = processes.iterator();
        while (iterator.hasNext()) {
            Process p = iterator.next();
            p.update();
            if (p.isCompleted()) {
                if (p.getResourcesSatisfied()) {
                    moneyManager.addMoney(10);
                    scoreManager.addScore(10);
                } else {
                    if (moneyManager.getMoney() >= 10) {
                        moneyManager.minusMoney(10);
                    }
                }
                p.resetAllocatedResources(); // Reset resources used by this process
                iterator.remove();
            }
        }

        if ((currentTime - gameStartTime) >= ATTACKER_DELAY &&
                (currentTime - lastAttackTime) >= ATTACKER_COOLDOWN) {

            if (Math.random() < BLOCK_CHANCE) {
                int slot = (int) (Math.random() * 4);
                if (!blockedSlots[slot]) {
                    blockedSlots[slot] = true;
                    lastAttackTime = currentTime;

                    // Trigger attacker animation
                    triggerAttacker();

                    // Vibration code
                    Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(50);
                        }
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        blockedSlots[slot] = false;
                    }, BLOCK_DURATION);
                }
            }
        }

        long adjustedSpawnInterval = processes.size() < MAX_PROCESSES / 2 ? PROCESS_SPAWN_INTERVAL / 5 : PROCESS_SPAWN_INTERVAL;

        if (currentTime - lastProcessSpawnTime >= adjustedSpawnInterval) {
            spawnNewProcess();
            lastProcessSpawnTime = currentTime;
        }
    }

    public void addResource(Resource.Type type) {

        int money = moneyManager.getMoney();

        synchronized (resources) {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            float screenWidth = metrics.widthPixels;
            float screenHeight = metrics.heightPixels;
            float spacing = 160f;
            float bottomY = screenHeight - 250;

            // Count existing resources of this type to determine position
            int count = 0;
            for (Resource r : resources) {
                if (r.getType() == type) {
                    count++;
                }
            }

            if (count < 9 && money >= 10) {
                // Calculate position based on type
                float startX = (type == Resource.Type.CPU) ? 240 : screenWidth - 1600;
                float x = startX + (count % 3) * spacing;
                float y = bottomY - (count / 3) * spacing;

                // Create and add new resource
                Resource newResource = new Resource(getContext(), type, x, y);
                moneyManager.minusMoney(10);
                resources.add(newResource);
            }
        }
    }

    private void spawnNewProcess() {
        if (processes.size() >= MAX_PROCESSES) {
            return; // Don't spawn more processes if reached limit
        }

        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        float screenWidth = metrics.widthPixels;
        float horizontalPadding = 270;

        float topY = 220;

        // Grid configuration
        float availableWidth = screenWidth - 2 * horizontalPadding;
        int processesPerRow = (int) (availableWidth / MIN_DISTANCE);
        float horizontalSpacing = availableWidth / processesPerRow;

        boolean[] occupied = new boolean[processesPerRow];

        for (Process p : processes) {
            if (Math.abs(p.getY() - topY) < 10) {
                // Calculate the column index based on its x position.
                int col = (int) ((p.getX() - horizontalPadding + horizontalSpacing/2) / horizontalSpacing);
                if (col >= 0 && col < processesPerRow) {
                    occupied[col] = true;
                }
            }
        }

        // Find the first free column.
        int freeCol = -1;
        for (int i = 0; i < processesPerRow; i++) {
            if (!occupied[i]) {
                freeCol = i;
                break;
            }
        }

        if (freeCol == -1) {
            // All top-row spots are occupied; do not spawn a new process.
            return;
        }

        float x = horizontalPadding + freeCol * horizontalSpacing;

        Process newProcess = new Process(getContext(), x, topY, Color.GREEN);
        newProcess.setListener(this);
        processes.add(newProcess);
    }

    @Override
    public void onTimerFinished(Process process) {
        // A timer on a red process has expired.
        // Set game over and stop the game loop.
        if (!process.isExecuting() && !process.isCompleted()) {
            // Only deduct health if process is in pending state (red)
            currentHealth = Math.max(0, currentHealth - 1);

            // Mark process as completed to prevent multiple deductions
            process.setCompleted(true);

            if (currentHealth <= 0) {
                gameOver = true;

                // code for leaderboard
                LeaderboardDbHelper dbHelper = new LeaderboardDbHelper(this.getContext());
                dbHelper.insertRecord(scoreManager.getScore());
            }
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void resetGame() {
        gameOver = false;
        scoreManager.resetScore();
        currentHealth = maxHealth;

        // reset CPU and MEM back to 5
        synchronized (resources) {
            resources.clear();
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            float screenWidth = metrics.widthPixels;
            float screenHeight = metrics.heightPixels;
            float spacing = 160f;
            float bottomY = screenHeight - 250;
            float startXCpu = 240; // Starting x for CPU resources
            float startXMemory = screenWidth - 1600; // Starting x for MEMORY resources

            for (int i = 0; i < 5; i++) {
                // Position CPU resources:
                float xCpu = startXCpu + (i % 3) * spacing; // Arrange in a grid (up to 3 per row)
                float yCpu = bottomY - (i / 3) * spacing;
                resources.add(new Resource(getContext(), Resource.Type.CPU, xCpu, yCpu));

                // Position MEMORY resources:
                float xMemory = startXMemory + (i % 3) * spacing;
                float yMemory = bottomY - (i / 3) * spacing;
                resources.add(new Resource(getContext(), Resource.Type.MEMORY, xMemory, yMemory));
            }
        }

        moneyManager.resetMoney();

        // Reset processes
        processes.clear();

        // Reset player position
        Context context = getContext();
        if(context == null) {
            return;
        }

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

    private void returnToMainMenu() {
        Context context = getContext();
        if (context instanceof GameActivity) {
            ((Activity) getContext()).finish();
        }
    }

}

