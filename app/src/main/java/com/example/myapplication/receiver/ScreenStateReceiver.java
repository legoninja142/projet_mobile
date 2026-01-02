// Create: app/src/main/java/com/example/myapplication/receiver/ScreenStateReceiver.java
package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.myapplication.service.SleepSensorService;

public class ScreenStateReceiver extends BroadcastReceiver {
    private static final String TAG = "ScreenStateReceiver";
    private static final String PREF_NAME = "sleep_tracker_prefs";
    private static final String KEY_IS_MONITORING = "is_monitoring";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_SCREEN_ON.equals(action)) {
            Log.d(TAG, "Screen turned ON - stopping sleep tracking");
            stopSleepTracking(context);
        } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            Log.d(TAG, "Screen turned OFF");
            // You could potentially start monitoring when screen goes off
        } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            Log.d(TAG, "User unlocked device - stopping sleep tracking");
            stopSleepTracking(context);
        }
    }

    private void stopSleepTracking(Context context) {
        // Check if sleep monitoring is active
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isMonitoring = prefs.getBoolean(KEY_IS_MONITORING, false);

        if (isMonitoring) {
            Log.d(TAG, "Stopping sleep monitoring - phone woke up");

            // Stop the sleep sensor service
            Intent serviceIntent = new Intent(context, SleepSensorService.class);
            serviceIntent.putExtra("action", "stop");
            serviceIntent.putExtra("auto_stop", true); // Flag for auto-stop

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // Clear monitoring flag
            prefs.edit().putBoolean(KEY_IS_MONITORING, false).apply();

            Log.d(TAG, "Sleep tracking stopped and monitoring flag cleared");
        }
    }
}