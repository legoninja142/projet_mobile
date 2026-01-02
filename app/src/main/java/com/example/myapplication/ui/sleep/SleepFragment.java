package com.example.myapplication.ui.sleep;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.databinding.FragmentSleepBinding;
import com.example.myapplication.model.SleepRecord;
import com.example.myapplication.service.SleepSensorService;
import com.example.myapplication.ui.sleep.adapter.SleepRecordAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SleepFragment extends Fragment {

    private FragmentSleepBinding binding;
    private SleepViewModel sleepViewModel;
    private SleepRecordAdapter adapter;
    private SessionManager sessionManager;
    private Calendar startDateTimeCalendar;
    private Calendar endDateTimeCalendar;

    // Sensor tracking variables
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
            new String[]{
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.BODY_SENSORS
            } :
            new String[]{
                    Manifest.permission.WAKE_LOCK,
                    Manifest.permission.BODY_SENSORS
            };
    private boolean isSensorMonitoring = false;

    // Chronometer variables
    private Chronometer chronometer;
    private long chronometerBaseTime = 0;
    private static final String CHRONOMETER_BASE = "chronometer_base";
    private static final String CHRONOMETER_STARTED = "chronometer_started";
    private static final String PREF_NAME = "sleep_tracker_prefs";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSleepBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        sleepViewModel = new ViewModelProvider(this).get(SleepViewModel.class);

        initializeDateTimePickers();
        setupRecyclerView();
        setupListeners();
        setupObservers();
        setupSensorTracking();
        setupChronometer();

        // Load sleep records
        String userId = sessionManager.getLoggedInUserId();
        if (userId != null) {
            sleepViewModel.loadSleepRecords(userId);
        }

        // Check if chronometer should be running from previous session
        checkAndRestoreChronometer();
    }

    private void initializeDateTimePickers() {
        startDateTimeCalendar = Calendar.getInstance();
        endDateTimeCalendar = Calendar.getInstance();

        // Set default start time to yesterday 22:00
        startDateTimeCalendar.add(Calendar.DAY_OF_YEAR, -1);
        startDateTimeCalendar.set(Calendar.HOUR_OF_DAY, 22);
        startDateTimeCalendar.set(Calendar.MINUTE, 0);

        // Set default end time to today 07:00
        endDateTimeCalendar.set(Calendar.HOUR_OF_DAY, 7);
        endDateTimeCalendar.set(Calendar.MINUTE, 0);

        updateDateTimeButtons();
    }

    private void setupRecyclerView() {
        adapter = new SleepRecordAdapter(record -> {
            // Handle item click - for editing or viewing details
            showEditDialog(record);
        }, record -> {
            // Handle delete
            sleepViewModel.deleteSleepRecord(record.getId(), record.getUserId());
        });

        binding.recyclerViewSleepRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewSleepRecords.setAdapter(adapter);
    }

    private void setupListeners() {
        // Time pickers
        binding.btnSelectStartTime.setOnClickListener(v -> showTimePicker(true));
        binding.btnSelectEndTime.setOnClickListener(v -> showTimePicker(false));

        // Date pickers
        binding.btnSelectStartDate.setOnClickListener(v -> showDatePicker(true));
        binding.btnSelectEndDate.setOnClickListener(v -> showDatePicker(false));

        binding.btnSaveSleepRecord.setOnClickListener(v -> saveSleepRecord());

        // Setup sleep quality spinner
        ArrayAdapter<CharSequence> qualityAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.sleep_quality_array,
                android.R.layout.simple_spinner_item
        );
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSleepQuality.setAdapter(qualityAdapter);
    }

    private void setupObservers() {
        sleepViewModel.getSleepRecords().observe(getViewLifecycleOwner(), records -> {
            adapter.submitList(records);
            binding.textNoRecords.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        });

        sleepViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSaveSleepRecord.setEnabled(!isLoading);
            binding.btnStartSensorTracking.setEnabled(!isLoading);
            binding.btnStopSensorTracking.setEnabled(!isLoading);
        });

        sleepViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSensorTracking() {
        binding.btnStartSensorTracking.setOnClickListener(v -> {
            if (checkPermissions()) {
                startSensorTracking();
            } else {
                requestPermissions();
            }
        });

        binding.btnStopSensorTracking.setOnClickListener(v -> {
            stopSensorTracking();
        });

        updateSensorUI();
    }

    private void setupChronometer() {
        chronometer = binding.chronometer;

        // Set chronometer format
        chronometer.setFormat("%s");

        // Add chronometer listener to update formatting
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
                updateChronometerText(elapsedMillis);
            }
        });
    }

    private void updateChronometerText(long elapsedMillis) {
        long hours = elapsedMillis / (1000 * 60 * 60);
        long minutes = (elapsedMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (elapsedMillis % (1000 * 60)) / 1000;

        String timeText;
        if (hours > 0) {
            timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            timeText = String.format("%02d:%02d", minutes, seconds);
        }

        chronometer.setText(timeText);
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startSensorTracking();
            } else {
                Toast.makeText(requireContext(),
                        "Permissions required for sensor tracking",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSensorTracking() {
        String userId = sessionManager.getLoggedInUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if sensors are available
        if (!isAccelerometerAvailable()) {
            Toast.makeText(requireContext(),
                    "Accelerometer not available on this device",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Start chronometer
        startChronometer();

        Intent serviceIntent = new Intent(requireContext(), SleepSensorService.class);
        serviceIntent.putExtra("action", "start");
        serviceIntent.putExtra("user_id", userId);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(serviceIntent);
            } else {
                requireContext().startService(serviceIntent);
            }

            isSensorMonitoring = true;
            updateSensorUI();
            Toast.makeText(requireContext(),
                    "Sensor tracking started. Place phone on bedside table.",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Failed to start sensor tracking: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            stopChronometer();
        }
    }

    private void stopSensorTracking() {
        Intent serviceIntent = new Intent(requireContext(), SleepSensorService.class);
        serviceIntent.putExtra("action", "stop");

        try {
            requireContext().startService(serviceIntent);
            isSensorMonitoring = false;
            updateSensorUI();
            stopChronometer();
            Toast.makeText(requireContext(),
                    "Sensor tracking stopped. Data saved.",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Failed to stop sensor tracking: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startChronometer() {
        // Start chronometer
        chronometerBaseTime = SystemClock.elapsedRealtime();
        chronometer.setBase(chronometerBaseTime);
        chronometer.start();

        // Save chronometer state
        saveChronometerState(true, chronometerBaseTime);

        // Show chronometer layout
        binding.layoutChronometer.setVisibility(View.VISIBLE);

        // Update tracking hint
        binding.textTrackingHint.setText("Phone is monitoring your sleep...");
    }

    private void stopChronometer() {
        // Stop chronometer
        chronometer.stop();

        // Clear chronometer state
        saveChronometerState(false, 0);

        // Hide chronometer layout
        binding.layoutChronometer.setVisibility(View.GONE);
    }

    private void saveChronometerState(boolean isRunning, long baseTime) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(CHRONOMETER_STARTED, isRunning);
        editor.putLong(CHRONOMETER_BASE, baseTime);
        editor.apply();
    }

    private void checkAndRestoreChronometer() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean wasRunning = prefs.getBoolean(CHRONOMETER_STARTED, false);
        long savedBaseTime = prefs.getLong(CHRONOMETER_BASE, 0);

        if (wasRunning && savedBaseTime > 0) {
            // Calculate elapsed time
            long currentTime = SystemClock.elapsedRealtime();
            long elapsedTime = currentTime - savedBaseTime;

            // Show chronometer with elapsed time
            binding.layoutChronometer.setVisibility(View.VISIBLE);
            chronometerBaseTime = currentTime - elapsedTime;
            chronometer.setBase(chronometerBaseTime);
            chronometer.start();

            // Update tracking hint
            binding.textTrackingHint.setText("Resumed tracking...");

            isSensorMonitoring = true;
            updateSensorUI();

            Log.d("SleepFragment", "Chronometer restored. Elapsed time: " + formatTime(elapsedTime));
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        return String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    private boolean isAccelerometerAvailable() {
        android.hardware.SensorManager sensorManager =
                (android.hardware.SensorManager) requireContext().getSystemService(android.content.Context.SENSOR_SERVICE);
        return sensorManager != null && sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) != null;
    }

    private void updateSensorUI() {
        if (isSensorMonitoring) {
            binding.btnStartSensorTracking.setVisibility(View.GONE);
            binding.btnStopSensorTracking.setVisibility(View.VISIBLE);
            binding.textSensorStatus.setText("🟢 Sensor tracking active");
            binding.textSensorStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else {
            binding.btnStartSensorTracking.setVisibility(View.VISIBLE);
            binding.btnStopSensorTracking.setVisibility(View.GONE);
            binding.textSensorStatus.setText("⚪ Sensor tracking idle");
            binding.textSensorStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));

            // Ensure chronometer is hidden when not tracking
            binding.layoutChronometer.setVisibility(View.GONE);
        }
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = isStartDate ? startDateTimeCalendar : endDateTimeCalendar;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeButtons();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker(boolean isStartTime) {
        Calendar calendar = isStartTime ? startDateTimeCalendar : endDateTimeCalendar;

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    updateDateTimeButtons();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        binding.btnSelectStartDate.setText(
                String.format("Start Date: %s", dateFormat.format(startDateTimeCalendar.getTime()))
        );

        binding.btnSelectStartTime.setText(
                String.format("Start Time: %s", timeFormat.format(startDateTimeCalendar.getTime()))
        );

        binding.btnSelectEndDate.setText(
                String.format("End Date: %s", dateFormat.format(endDateTimeCalendar.getTime()))
        );

        binding.btnSelectEndTime.setText(
                String.format("End Time: %s", timeFormat.format(endDateTimeCalendar.getTime()))
        );

        // Show total duration preview
        long durationMillis = endDateTimeCalendar.getTimeInMillis() - startDateTimeCalendar.getTimeInMillis();
        if (durationMillis > 0) {
            int hours = (int) (durationMillis / (60 * 60 * 1000));
            int minutes = (int) ((durationMillis / (60 * 1000)) % 60);
            binding.btnSaveSleepRecord.setText(String.format("Save Sleep Record (%dh %dm)", hours, minutes));
        } else {
            binding.btnSaveSleepRecord.setText("Save Sleep Record");
        }
    }

    private void saveSleepRecord() {
        String userId = sessionManager.getLoggedInUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate end time is after start time
        if (endDateTimeCalendar.getTimeInMillis() <= startDateTimeCalendar.getTimeInMillis()) {
            Toast.makeText(requireContext(), "End date/time must be after start date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        int sleepQuality = binding.spinnerSleepQuality.getSelectedItemPosition() + 1; // 1-5
        String notes = binding.editTextNotes.getText() != null ?
                binding.editTextNotes.getText().toString().trim() : "";

        SleepRecord record = new SleepRecord(
                userId,
                startDateTimeCalendar.getTime(),
                endDateTimeCalendar.getTime(),
                sleepQuality,
                notes
        );

        sleepViewModel.addSleepRecord(record, userId);

        // Clear form and reset to default times
        binding.editTextNotes.setText("");
        binding.spinnerSleepQuality.setSelection(0);

        // Show success message
        Toast.makeText(requireContext(), "Sleep record saved successfully!", Toast.LENGTH_SHORT).show();

        // Reset calendars for next entry (default to last night's sleep)
        initializeDateTimePickers();
    }

    private void showEditDialog(SleepRecord record) {
        Toast.makeText(requireContext(), "Edit feature coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Save chronometer state when fragment pauses
        if (isSensorMonitoring && chronometer != null) {
            saveChronometerState(true, chronometerBaseTime);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if we need to restore chronometer
        checkAndRestoreChronometer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}