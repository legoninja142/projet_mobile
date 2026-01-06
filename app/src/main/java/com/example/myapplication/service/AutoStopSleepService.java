package com.example.myapplication.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

public class AutoStopSleepService extends Service {
    private static final String TAG = "AutoStopSleepService";
    private static final String CHANNEL_ID = "AutoStopChannel";
    private static final int NOTIFICATION_ID = 102;

    private BroadcastReceiver screenStateReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");

        // Create notification channel for Android O+
        createNotificationChannel();

        // Register screen state receiver
        screenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Log.d(TAG, "Screen turned ON");
                    checkAndStopSleepRecording();
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Log.d(TAG, "Screen turned OFF");
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Log.d(TAG, "User unlocked device");
                    checkAndStopSleepRecording();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenStateReceiver, filter);

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sleep Auto-Stop Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Monitors screen state to auto-stop sleep recording");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ...existing code...

    private void checkAndStopSleepRecording() {
        // Check if SleepSensorService is running
        if (isSleepSensorServiceRunning()) {
            Log.d(TAG, "Stopping sleep recording due to screen state change");

            // Stop the sleep sensor service
            Intent serviceIntent = new Intent(this, SleepSensorService.class);
            serviceIntent.putExtra("action", "stop");
            serviceIntent.putExtra("auto_stop", true);

            startService(serviceIntent);

            // Show notification
            showAutoStopNotification();
        }
    }

    private boolean isSleepSensorServiceRunning() {
        // Check shared preferences if monitoring is active
        // Or use ActivityManager to check if service is running
        // For simplicity, we'll check shared preferences

        return getSharedPreferences("sleep_tracker_prefs", MODE_PRIVATE)
                .getBoolean("is_monitoring", false);
    }

    private void showAutoStopNotification() {
        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sleep Recording Complete")
                .setContentText("Recording stopped automatically when phone was opened")
                .setSmallIcon(R.drawable.ic_sleep)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID + 2, builder.build());
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sleep Auto-Stop Active")
                .setContentText("Will stop recording when phone is opened")
                .setSmallIcon(R.drawable.ic_sleep)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenStateReceiver != null) {
            unregisterReceiver(screenStateReceiver);
        }
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}