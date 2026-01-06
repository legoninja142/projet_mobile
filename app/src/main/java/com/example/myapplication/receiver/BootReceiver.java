package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.service.AutoStopSleepService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREF_NAME = "sleep_tracker_prefs";
    private static final String KEY_IS_MONITORING = "is_monitoring";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "Device boot detected: " + action);

            /* -----------------------------
             * Start StepCounterService
             * ----------------------------- */
            try {
                Intent stepServiceIntent = new Intent();
                stepServiceIntent.setClassName(
                        context.getPackageName(),
                        "com.example.myapplication.service.StepCounterService"
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(stepServiceIntent);
                } else {
                    context.startService(stepServiceIntent);
                }

                Log.d(TAG, "StepCounterService started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start StepCounterService", e);
            }

            /* ----------------------------------------
             * Restore sleep monitoring if it was active
             * ---------------------------------------- */
            try {
                SharedPreferences prefs =
                        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

                boolean wasMonitoring =
                        prefs.getBoolean(KEY_IS_MONITORING, false);

                if (wasMonitoring) {
                    Log.d(TAG, "Resuming sleep monitoring after boot");

                    Intent autoStopIntent =
                            new Intent(context, AutoStopSleepService.class);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(autoStopIntent);
                    } else {
                        context.startService(autoStopIntent);
                    }

                    Log.d(TAG, "AutoStopSleepService started after boot");
                } else {
                    Log.d(TAG, "Sleep monitoring was not active before shutdown");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error restoring sleep monitoring state", e);
            }
        }
    }
}