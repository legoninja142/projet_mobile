// Create: app/src/main/java/com/example/myapplication/service/SleepSensorService.java
package com.example.myapplication.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.LightData;
import com.example.myapplication.model.MovementData;
import com.example.myapplication.model.SleepSensorData;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SleepSensorService extends Service implements SensorEventListener {
    private static final String PREF_NAME = "sleep_tracker_prefs";
    private static final String KEY_TRACKING_START_TIME = "tracking_start_time";

    private static final String KEY_IS_MONITORING = "is_monitoring";
    private static final String TAG = "SleepSensorService";
    private static final String CHANNEL_ID = "SleepSensorChannel";
    private static final int NOTIFICATION_ID = 101;

    // Sampling intervals (in milliseconds)
    private static final int ACCELEROMETER_INTERVAL = 1000; // 1 second - collect more frequently
    private static final int LIGHT_SENSOR_INTERVAL = 5000; // 5 seconds - collect more frequently
    private static final long WAKE_LOCK_TIMEOUT = 12 * 60 * 60 * 1000; // 12 hours max
    private static final long PERIODIC_SAVE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor lightSensor;
    private PowerManager.WakeLock wakeLock;
    private Handler handler;
    private AudioManager audioManager;

    private boolean isMonitoring = false;
    private String userId;
    private SleepSensorData currentSession;

    private List<MovementData> movementBuffer = new ArrayList<>();
    private List<LightData> lightBuffer = new ArrayList<>();

    private long lastAccelerometerTime = 0;
    private long lastLightSensorTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        // Initialize handler for periodic tasks
        handler = new Handler(Looper.getMainLooper());

        // Initialize audio manager for DND mode
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");

        if (intent != null) {
            String action = intent.getStringExtra("action");
            userId = intent.getStringExtra("user_id");
            boolean isAutoStop = intent.getBooleanExtra("auto_stop", false);

            if ("start".equals(action)) {
                startSleepMonitoring();

                // Send notification if auto-stopped
                if (isAutoStop) {
                    sendAutoStopNotification();
                }
            } else if ("stop".equals(action)) {
                stopSleepMonitoring();
            }
        }

        return START_STICKY;
    }
    private void sendAutoStopNotification() {
        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping notification");
                return;
            }
        }

        // Create a notification to inform user that recording was auto-stopped
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sleep Recording Stopped")
                .setContentText("Recording stopped because phone was opened")
                .setSmallIcon(R.drawable.ic_sleep)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + 1, builder.build());
            Log.d(TAG, "Auto-stop notification sent");
        }
    }
    private void saveMonitoringState(boolean isMonitoring) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_MONITORING, isMonitoring).apply();
        Log.d(TAG, "Monitoring state saved: " + isMonitoring);
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

        // Acquire wake lock with timeout to ensure cleanup
        if (wakeLock != null && !wakeLock.isHeld()) {
            try {
                wakeLock.acquire(WAKE_LOCK_TIMEOUT);
                Log.d(TAG, "Wake lock acquired with 12-hour timeout");
            } catch (Exception e) {
                Log.e(TAG, "Error acquiring wake lock", e);
            }
        }

        // Initialize new session
        currentSession = new SleepSensorData(userId, new Date());
        movementBuffer.clear();
        lightBuffer.clear();

        // Register sensor listeners with appropriate delays
        try {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_UI);
            Log.d(TAG, "Accelerometer registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering accelerometer listener", e);
        }

        if (lightSensor != null) {
            try {
                sensorManager.registerListener(this, lightSensor,
                        SensorManager.SENSOR_DELAY_UI);
                Log.d(TAG, "Light sensor enabled");
            } catch (Exception e) {
                Log.e(TAG, "Error registering light sensor listener", e);
            }
        } else {
            Log.w(TAG, "Light sensor not available on this device");
        }

        isMonitoring = true;
        saveMonitoringState(true);
        saveTrackingStartTime();

        // Start periodic save task
        try {
            startPeriodicSave();
        } catch (Exception e) {
            Log.e(TAG, "Error starting periodic save", e);
        }

        // Enable Do Not Disturb mode
        try {
            enableDoNotDisturb();
        } catch (Exception e) {
            Log.e(TAG, "Error enabling DND mode", e);
        }

        Log.d(TAG, "Sleep monitoring started successfully - sensors registered");
    }

    private void enableDoNotDisturb() {
        if (audioManager != null) {
            try {
                // Set ringer mode to silent (No Sound/Vibration)
                // This works on API 23+ without special permissions
                audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                Log.d(TAG, "Do Not Disturb mode enabled - Ringer set to SILENT");
            } catch (SecurityException e) {
                // If permission denied, just log it and continue
                Log.w(TAG, "Cannot change ringer mode - permission may be denied", e);
            } catch (Exception e) {
                Log.e(TAG, "Error enabling Do Not Disturb mode", e);
            }
        }
    }

    private void disableDoNotDisturb() {
        if (audioManager != null) {
            try {
                // Reset ringer mode to normal (Sound + Vibration)
                audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                Log.d(TAG, "Do Not Disturb mode disabled - Ringer set to NORMAL");
            } catch (SecurityException e) {
                // If permission denied, just log it and continue
                Log.w(TAG, "Cannot change ringer mode - permission may be denied", e);
            } catch (Exception e) {
                Log.e(TAG, "Error disabling Do Not Disturb mode", e);
            }
        }
    }

    private void startPeriodicSave() {
        handler.postDelayed(periodicSaveRunnable, PERIODIC_SAVE_INTERVAL);
        Log.d(TAG, "Periodic save scheduled");
    }

    private final Runnable periodicSaveRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (isMonitoring && currentSession != null && (!movementBuffer.isEmpty() || !lightBuffer.isEmpty())) {
                    Log.d(TAG, "Periodic save: " + movementBuffer.size() + " movement readings");
                    saveSessionData();
                }
                // Reschedule next save if still monitoring
                if (isMonitoring && handler != null) {
                    handler.postDelayed(this, PERIODIC_SAVE_INTERVAL);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in periodic save runnable", e);
            }
        }
    };
    // Add this method to save start time
    private void saveTrackingStartTime() {
        long startTime = System.currentTimeMillis();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_TRACKING_START_TIME, startTime).apply();
        Log.d(TAG, "Tracking start time saved: " + startTime);
    }

    private void stopSleepMonitoring() {
        if (!isMonitoring) {
            Log.w(TAG, "Not currently monitoring");
            return;
        }

        Log.d(TAG, "Stopping sleep monitoring");

        // Disable Do Not Disturb mode
        try {
            disableDoNotDisturb();
        } catch (Exception e) {
            Log.e(TAG, "Error disabling DND mode", e);
        }

        // Cancel periodic save task
        try {
            handler.removeCallbacks(periodicSaveRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error removing periodic save callback", e);
        }

        // Unregister sensor listeners
        try {
            sensorManager.unregisterListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering sensors", e);
        }

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
                Log.d(TAG, "Wake lock released");
            } catch (Exception e) {
                Log.e(TAG, "Error releasing wake lock", e);
            }
        }

        // Save final session data
        try {
            saveSessionData();
        } catch (Exception e) {
            Log.e(TAG, "Error saving session data", e);
        }

        // Stop foreground service
        try {
            stopForeground(true);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping foreground", e);
        }

        isMonitoring = false;
        saveMonitoringState(false);

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
                    Log.v(TAG, "Accelerometer data processed: x=" + event.values[0] +
                            " y=" + event.values[1] + " z=" + event.values[2]);
                }
                break;

            case Sensor.TYPE_LIGHT:
                // Throttle light sensor readings
                if (currentTime - lastLightSensorTime >= LIGHT_SENSOR_INTERVAL) {
                    processLightData(event.values[0]);
                    lastLightSensorTime = currentTime;
                    Log.v(TAG, "Light sensor data processed: " + event.values[0] + " lux");
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


    private void saveSessionData() {
        if (userId == null || currentSession == null) {
            Log.w(TAG, "Cannot save data: No user or session");
            return;
        }

        // Update session end time
        currentSession.setSessionEndTime(new Date());

        // Calculate metrics if we have data
        if (!movementBuffer.isEmpty() || !lightBuffer.isEmpty()) {
            try {
                calculateSleepMetrics();

                // Save to Firebase
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                if (db != null) {
                    db.collection("sleep_sensor_sessions")
                            .add(currentSession)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Sleep sensor data saved: " + documentReference.getId());

                                // Clear buffers after successful save
                                movementBuffer.clear();
                                lightBuffer.clear();
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error saving sleep sensor data", e));
                } else {
                    Log.w(TAG, "FirebaseFirestore not initialized");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in saveSessionData", e);
            }
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
        if (significantMovements < 10) {
            quality += 1; // Less movement = better
        } else if (significantMovements > 50) {
            quality -= 1; // More movement = worse
        }

        // Adjust based on light
        if (percentDark > 90) {
            quality += 1; // Dark environment = better
        } else if (percentDark < 50) {
            quality -= 1; // Bright environment = worse
        } else if (avgLight > 100) {
            quality -= 1; // Very bright = worse
        }

        // Clamp to 1-5 range
        return Math.min(5, Math.max(1, quality));
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