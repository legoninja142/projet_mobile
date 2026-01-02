// Create: app/src/main/java/com/example/myapplication/service/SleepSensorService.java
package com.example.myapplication.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.LightData;
import com.example.myapplication.model.MovementData;
import com.example.myapplication.model.SleepSensorData;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SleepSensorService extends Service implements SensorEventListener {
    private static final String TAG = "SleepSensorService";
    private static final String CHANNEL_ID = "SleepSensorChannel";
    private static final int NOTIFICATION_ID = 101;

    // Sensor thresholds
    private static final float MOVEMENT_THRESHOLD = 1.5f; // Adjust based on testing
    private static final float LIGHT_DARK_THRESHOLD = 10.0f; // Lux

    // Sampling intervals (in milliseconds)
    private static final int ACCELEROMETER_INTERVAL = 5000; // 5 seconds
    private static final int LIGHT_SENSOR_INTERVAL = 30000; // 30 seconds
    private static final int SAVE_INTERVAL = 300000; // 5 minutes

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor lightSensor;
    private PowerManager.WakeLock wakeLock;

    private boolean isMonitoring = false;
    private String userId;
    private SleepSensorData currentSession;

    private List<MovementData> movementBuffer = new ArrayList<>();
    private List<LightData> lightBuffer = new ArrayList<>();

    private long lastAccelerometerTime = 0;
    private long lastLightSensorTime = 0;
    private long lastSaveTime = 0;

    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        // Create notification channel for Android O+
        createNotificationChannel();

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        // Initialize wake lock to keep CPU running
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SleepTrackerApp::SleepSensorWakeLock"
            );
        }

        // Initialize scheduler for periodic tasks
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        if (intent != null) {
            String action = intent.getStringExtra("action");
            userId = intent.getStringExtra("user_id");

            if ("start".equals(action)) {
                startSleepMonitoring();
            } else if ("stop".equals(action)) {
                stopSleepMonitoring();
            }
        }

        // Return START_STICKY to restart if killed
        return START_STICKY;
    }

    private void startSleepMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring");
            return;
        }

        if (accelerometer == null) {
            Log.e(TAG, "Accelerometer not available on this device");
            return;
        }

        Log.d(TAG, "Starting sleep monitoring");

        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Acquire wake lock
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d(TAG, "Wake lock acquired");
        }

        // Initialize new session
        currentSession = new SleepSensorData(userId, new Date());
        movementBuffer.clear();
        lightBuffer.clear();

        // Register sensor listeners with appropriate delays
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Light sensor enabled");
        } else {
            Log.w(TAG, "Light sensor not available on this device");
        }

        isMonitoring = true;

        // Schedule periodic tasks
        scheduler.scheduleAtFixedRate(this::checkAndSaveData,
                SAVE_INTERVAL / 1000, SAVE_INTERVAL / 1000, TimeUnit.SECONDS);

        Log.d(TAG, "Sleep monitoring started successfully");
    }

    private void stopSleepMonitoring() {
        if (!isMonitoring) {
            Log.w(TAG, "Not currently monitoring");
            return;
        }

        Log.d(TAG, "Stopping sleep monitoring");

        // Unregister sensor listeners
        sensorManager.unregisterListener(this);

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }

        // Stop scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }

        // Save final session data
        saveSessionData();

        // Stop foreground service
        stopForeground(true);

        isMonitoring = false;

        Log.d(TAG, "Sleep monitoring stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // Throttle accelerometer readings to save battery
                if (currentTime - lastAccelerometerTime >= ACCELEROMETER_INTERVAL) {
                    processAccelerometerData(event.values[0], event.values[1], event.values[2]);
                    lastAccelerometerTime = currentTime;
                }
                break;

            case Sensor.TYPE_LIGHT:
                // Throttle light sensor readings
                if (currentTime - lastLightSensorTime >= LIGHT_SENSOR_INTERVAL) {
                    processLightData(event.values[0]);
                    lastLightSensorTime = currentTime;
                }
                break;
        }
    }

    private void processAccelerometerData(float x, float y, float z) {
        MovementData movement = new MovementData(new Date(), x, y, z);
        movementBuffer.add(movement);

        // Log significant movements
        if (movement.isSignificantMovement()) {
            Log.d(TAG, "Significant movement detected: " + movement.getMagnitude());
        }

        // Keep buffer size manageable
        if (movementBuffer.size() > 1000) {
            movementBuffer = movementBuffer.subList(500, 1000);
        }
    }

    private void processLightData(float lightLevel) {
        LightData lightData = new LightData(new Date(), lightLevel);
        lightBuffer.add(lightData);

        Log.d(TAG, "Light level: " + lightLevel + " lux, Is dark: " + lightData.isDark());

        // Keep buffer size manageable
        if (lightBuffer.size() > 500) {
            lightBuffer = lightBuffer.subList(250, 500);
        }
    }

    private void checkAndSaveData() {
        long currentTime = System.currentTimeMillis();

        // Save data every SAVE_INTERVAL
        if (currentTime - lastSaveTime >= SAVE_INTERVAL) {
            saveSessionData();
            lastSaveTime = currentTime;
        }
    }

    private void saveSessionData() {
        if (userId == null || currentSession == null) {
            Log.w(TAG, "Cannot save data: No user or session");
            return;
        }

        // Update session end time
        currentSession.setSessionEndTime(new Date());

        // Calculate metrics if we have data
        if (!movementBuffer.isEmpty() || !lightBuffer.isEmpty()) {
            calculateSleepMetrics();

            // Save to Firebase
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("sleep_sensor_sessions")
                    .add(currentSession)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Sleep sensor data saved: " + documentReference.getId());

                        // Clear buffers after successful save
                        movementBuffer.clear();
                        lightBuffer.clear();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving sleep sensor data", e);
                    });
        }
    }

    private void calculateSleepMetrics() {
        // Calculate movement metrics
        int significantMovements = 0;
        float totalMovement = 0;

        for (MovementData movement : movementBuffer) {
            totalMovement += movement.getMagnitude();
            if (movement.isSignificantMovement()) {
                significantMovements++;
            }
        }

        float avgMovement = movementBuffer.isEmpty() ? 0 : totalMovement / movementBuffer.size();

        // Calculate light metrics
        int darkReadings = 0;
        float totalLight = 0;

        for (LightData light : lightBuffer) {
            totalLight += light.getLightLevel();
            if (light.isDark()) {
                darkReadings++;
            }
        }

        float avgLight = lightBuffer.isEmpty() ? 0 : totalLight / lightBuffer.size();
        float percentDark = lightBuffer.isEmpty() ? 0 : (darkReadings * 100f) / lightBuffer.size();

        // Estimate sleep quality based on metrics
        int estimatedQuality = estimateSleepQuality(significantMovements, avgLight, percentDark);

        // Update session data
        currentSession.setMovementData(new ArrayList<>(movementBuffer));
        currentSession.setLightData(new ArrayList<>(lightBuffer));
        currentSession.setEstimatedSleepQuality(estimatedQuality);
        currentSession.setAverageMovementPerHour(avgMovement * 12); // Convert to per hour (5 sec intervals)
        currentSession.setAverageLightLevel(avgLight);
        currentSession.setWasDarkEnvironment(percentDark > 80); // More than 80% darkness

        Log.d(TAG, String.format("Metrics - Movements: %d, Avg Light: %.2f lux, " +
                        "Darkness: %.1f%%, Quality: %d",
                significantMovements, avgLight, percentDark, estimatedQuality));
    }

    private int estimateSleepQuality(int significantMovements, float avgLight, float percentDark) {
        int quality = 3; // Start with average

        // Adjust based on movement
        if (significantMovements < 10) quality += 1; // Less movement = better
        else if (significantMovements > 50) quality -= 1; // More movement = worse

        // Adjust based on light
        if (percentDark > 90) quality += 1; // Dark environment = better
        else if (percentDark < 50) quality -= 1; // Bright environment = worse
        else if (avgLight > 100) quality -= 1; // Very bright = worse

        // Clamp to 1-5 range
        return Math.max(1, Math.min(5, quality));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sleep Sensor Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitoring sleep patterns using sensors");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sleep Tracker Active")
                .setContentText("Monitoring your sleep patterns")
                .setSmallIcon(R.drawable.ic_sleep) // You need to add this icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true);

        return builder.build();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        if (isMonitoring) {
            stopSleepMonitoring();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}