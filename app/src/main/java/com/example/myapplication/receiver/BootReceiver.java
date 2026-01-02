package com.example.myapplication.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.myapplication.service.AutoStopSleepService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final String PREF_NAME = "sleep_tracker_prefs";
    private static final String KEY_IS_MONITORING = "is_monitoring";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed");

            // Check if sleep monitoring was active before the device shutdown
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean wasMonitoring = prefs.getBoolean(KEY_IS_MONITORING, false);

            if (wasMonitoring) {
                Log.d(TAG, "Resuming sleep monitoring after device boot");

                // Restore the monitoring state and start the services
                // Start AutoStopSleepService to handle screen state monitoring
                Intent autoStopIntent = new Intent(context, AutoStopSleepService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(autoStopIntent);
                } else {
                    context.startService(autoStopIntent);
                }

                Log.d(TAG, "AutoStopSleepService started after boot");
            }
        }
    }
}

