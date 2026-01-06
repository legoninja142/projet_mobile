package com.example.myapplication.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

public class PermissionUtils {
    private static final String TAG = "PermissionUtils";
    
    // Permission request codes
    public static final int PERMISSION_REQUEST_CODE = 1001;
    
    // Required permissions for step counter
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_HEALTH,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
            Manifest.permission.WAKE_LOCK
    };

    // Check if all required permissions are granted
    public static boolean hasRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Request all required permissions
    public static void requestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check which permissions we need to request
            boolean shouldShowRationale = false;
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }

            if (shouldShowRationale) {
                // Show explanation dialog
                showPermissionRationaleDialog(activity);
            } else {
                // No explanation needed, request the permissions
                requestAllPermissions(activity);
            }
        }
    }

    // Show dialog explaining why permissions are needed
    private static void showPermissionRationaleDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_needed)
                .setMessage(R.string.step_counter_permission_rationale)
                .setPositiveButton(R.string.grant_permission, (dialog, which) -> {
                    requestAllPermissions(activity);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    // Request all permissions
    private static void requestAllPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
        );
    }

    // Check if we should show a dialog explaining why we need permissions
    public static boolean shouldShowRequestPermissionRationale(Activity activity) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    // Show dialog to open app settings for manual permission granting
    public static void showSettingsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.permission_denied_message)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    openAppSettings(activity);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    // Open app settings for manual permission management
    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    // Handle the result of permission request
    public static boolean handlePermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
