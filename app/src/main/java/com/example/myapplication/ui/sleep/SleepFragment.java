package com.example.myapplication.ui.sleep;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.WAKE_LOCK
    };
    private boolean isSensorMonitoring = false;

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

        // Load sleep records
        String userId = sessionManager.getLoggedInUserId(); // Changed to getLoggedInUserId
        if (userId != null) {
            sleepViewModel.loadSleepRecords(userId);
        }
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
        }
    }

    private void stopSensorTracking() {
        Intent serviceIntent = new Intent(requireContext(), SleepSensorService.class);
        serviceIntent.putExtra("action", "stop");

        try {
            requireContext().startService(serviceIntent);
            isSensorMonitoring = false;
            updateSensorUI();
            Toast.makeText(requireContext(),
                    "Sensor tracking stopped. Data saved.",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Failed to stop sensor tracking: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
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
        String userId = sessionManager.getLoggedInUserId(); // Changed to getLoggedInUserId
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}