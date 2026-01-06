package com.example.myapplication.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.model.ActivityData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ActivityFragment extends Fragment implements SensorEventListener {

    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String KEY_BASELINE = "baseline";
    private static final String KEY_TOTAL_STEPS = "total_steps";
    private static final String KEY_IS_COUNTING = "is_counting";
    private static final String KEY_START_TIME = "start_time";
    private static final int DAILY_GOAL = 10000;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView tvStepCount;
    private TextView tvActivityLevel;
    private TextView tvActiveTime;
    private TextView tvGoalProgress;
    private TextView tvHistoryEmpty;
    private ProgressBar progressBar;
    private Button btnStart;
    private Button btnStop;
    private Button btnReset;
    private Button btnTestAdd;
    private LinearLayout historyContainer;
    private com.github.mikephil.charting.charts.BarChart activityChart;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences prefs;
    private SessionManager sessionManager;
    private FirebaseFirestore db;

    private int baseline = -1;
    private int totalSteps = 0;
    private boolean isCounting = false;
    private long startTime = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_activity, container, false);
        
        // Initialize views
        tvStepCount = root.findViewById(R.id.tv_step_count);
        tvActivityLevel = root.findViewById(R.id.tv_activity_level);
        tvActiveTime = root.findViewById(R.id.tv_active_time);
        tvGoalProgress = root.findViewById(R.id.tv_goal_progress);
        tvHistoryEmpty = root.findViewById(R.id.tv_history_empty);
        progressBar = root.findViewById(R.id.progress_bar);
        btnStart = root.findViewById(R.id.btn_start);
        btnStop = root.findViewById(R.id.btn_stop);
        btnReset = root.findViewById(R.id.btn_reset);
        btnTestAdd = root.findViewById(R.id.btn_test_add);
        Button btnRefreshHistory = root.findViewById(R.id.btn_refresh_history);
        historyContainer = root.findViewById(R.id.history_container);
        
        // Initialize chart
        com.github.mikephil.charting.charts.BarChart activityChart = root.findViewById(R.id.activity_chart);
        setupChart(activityChart);

        // Initialize sensor
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // Initialize SharedPreferences
        prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Initialize SessionManager and Firestore
        sessionManager = new SessionManager(requireActivity());
        db = FirebaseFirestore.getInstance();

        // Check if sensor is available
        if (stepSensor == null) {
            Toast.makeText(requireContext(), 
                "Step counter sensor not available. Using test mode.", 
                Toast.LENGTH_LONG).show();
            // Show test button for manual testing
            btnTestAdd.setVisibility(View.VISIBLE);
            android.util.Log.w("ActivityFragment", "No step counter sensor found - TEST MODE ENABLED");
        } else {
            android.util.Log.d("ActivityFragment", "Step counter sensor found: " + stepSensor.getName());
        }

        // Setup buttons
        btnStart.setOnClickListener(v -> startCounting());
        btnStop.setOnClickListener(v -> stopCounting());
        btnReset.setOnClickListener(v -> resetCounter());
        btnTestAdd.setOnClickListener(v -> testAddSteps());
        btnRefreshHistory.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Refreshing history...", Toast.LENGTH_SHORT).show();
            loadHistory();
        });
        
        // Long press test button to test Firebase write
        btnTestAdd.setOnLongClickListener(v -> {
            testFirebaseWrite();
            return true;
        });

        // Diagnostic: Check login status
        String diagUserId = sessionManager.getLoggedInUserId();
        String diagEmail = sessionManager.getLoggedInEmail();
        String safeEmail = diagEmail != null ? diagEmail.replace(".", "_") : "NULL";
        
        android.util.Log.d("ActivityFragment", "========== DIAGNOSTIC ==========");
        android.util.Log.d("ActivityFragment", "Logged in: " + sessionManager.isLoggedIn());
        android.util.Log.d("ActivityFragment", "UserId: " + diagUserId);
        android.util.Log.d("ActivityFragment", "Email: " + diagEmail);
        android.util.Log.d("ActivityFragment", "Safe Email (for Firebase): " + safeEmail);
        android.util.Log.d("ActivityFragment", "");
        android.util.Log.d("ActivityFragment", "📍 IN FIREBASE CONSOLE:");
        android.util.Log.d("ActivityFragment", "1. Open Firestore Database");
        android.util.Log.d("ActivityFragment", "2. Click 'users' collection");
        android.util.Log.d("ActivityFragment", "3. Find document: " + safeEmail);
        android.util.Log.d("ActivityFragment", "4. Inside that document, look for 'activities' subcollection");
        android.util.Log.d("ActivityFragment", "================================");


        // Load saved state
        loadSavedState();
        
        // Update UI
        updateUI();
        updateButtons();
        
        // Load history
        loadHistory();

        return root;
    }

    private void startCounting() {
        android.util.Log.d("ActivityFragment", "startCounting called");
        
        // Check permission for Android Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("ActivityFragment", "Requesting ACTIVITY_RECOGNITION permission");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        if (stepSensor == null) {
            Toast.makeText(requireContext(), 
                "No sensor available. Please test on a real device.", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isCounting) {
            boolean registered = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            android.util.Log.d("ActivityFragment", "Sensor registration result: " + registered);
            
            if (registered) {
                isCounting = true;
                startTime = System.currentTimeMillis();
                saveState();
                updateButtons();
                Toast.makeText(requireContext(), "Step counting started - Start walking!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Failed to register sensor listener", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopCounting() {
        if (isCounting) {
            sensorManager.unregisterListener(this);
            isCounting = false;
            saveState();
            
            // Save to Firestore
            saveActivityToFirestore();
            
            updateButtons();
            Toast.makeText(requireContext(), "Activity saved!", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetCounter() {
        if (!isCounting) {
            baseline = -1;
            totalSteps = 0;
            startTime = 0;
            saveState();
            updateUI();
            Toast.makeText(requireContext(), "Counter reset", Toast.LENGTH_SHORT).show();
        }
    }

    private void testAddSteps() {
        if (!isCounting) {
            Toast.makeText(requireContext(), "Please start counting first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        totalSteps += 10;
        android.util.Log.d("ActivityFragment", "TEST: Manually added 10 steps. Total: " + totalSteps);
        saveState();
        updateUI();
        Toast.makeText(requireContext(), "Added +10 steps (TEST)", Toast.LENGTH_SHORT).show();
    }

    private void saveActivityToFirestore() {
        String email = sessionManager.getLoggedInEmail();
        
        if (email == null || email.isEmpty()) {
            Toast.makeText(requireContext(), "Please login to save activity", Toast.LENGTH_SHORT).show();
            android.util.Log.e("ActivityFragment", "No email found - user not logged in");
            return;
        }

        // Use same pattern as RunHistoryFragment - email with dots replaced
        String safeUserId = email.replace(".", "_");
        
        android.util.Log.d("ActivityFragment", "========== SAVE ACTIVITY ==========");
        android.util.Log.d("ActivityFragment", "Email: " + email);
        android.util.Log.d("ActivityFragment", "Safe UserId: " + safeUserId);

        long activeTimeMinutes = startTime > 0 ? (System.currentTimeMillis() - startTime) / (1000 * 60) : 0;
        String activityLevel = getActivityLevelText();
        
        android.util.Log.d("ActivityFragment", "Steps: " + totalSteps);
        android.util.Log.d("ActivityFragment", "Activity Level: " + activityLevel);
        android.util.Log.d("ActivityFragment", "Active Time: " + activeTimeMinutes + " min");
        
        // Create the activity data object
        ActivityData activityData = new ActivityData(
            safeUserId,
            totalSteps,
            activityLevel,
            activeTimeMinutes,
            System.currentTimeMillis()
        );

        // Log the exact path
        String firestorePath = "users/" + safeUserId + "/activities/";
        android.util.Log.d("ActivityFragment", "==========================================");
        android.util.Log.d("ActivityFragment", "🔥 FIREBASE PATH: " + firestorePath);
        android.util.Log.d("ActivityFragment", "🔥 LOOK IN FIREBASE CONSOLE AT THIS EXACT PATH:");
        android.util.Log.d("ActivityFragment", "🔥 Collection: users");
        android.util.Log.d("ActivityFragment", "🔥 Document: " + safeUserId);
        android.util.Log.d("ActivityFragment", "🔥 Subcollection: activities");
        android.util.Log.d("ActivityFragment", "==========================================");

        // Save to Firestore
        db.collection("users")
                .document(safeUserId)
                .collection("activities")
                .add(activityData)
                .addOnSuccessListener(documentReference -> {
                    String docId = documentReference.getId();
                    android.util.Log.d("ActivityFragment", "==========================================");
                    android.util.Log.d("ActivityFragment", "✅ SUCCESS! Document ID: " + docId);
                    android.util.Log.d("ActivityFragment", "✅ FULL PATH: users/" + safeUserId + "/activities/" + docId);
                    android.util.Log.d("ActivityFragment", "✅ Check Firebase Console NOW!");
                    android.util.Log.d("ActivityFragment", "==========================================");
                    
                    Toast.makeText(requireContext(), "Saved! Check: users/" + safeUserId + "/activities/" + docId, Toast.LENGTH_LONG).show();
                    loadHistory();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ActivityFragment", "==========================================");
                    android.util.Log.e("ActivityFragment", "❌ FIREBASE WRITE FAILED!");
                    android.util.Log.e("ActivityFragment", "❌ Error: " + e.getClass().getSimpleName());
                    android.util.Log.e("ActivityFragment", "❌ Message: " + e.getMessage());
                    android.util.Log.e("ActivityFragment", "==========================================", e);
                    
                    Toast.makeText(requireContext(), "FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Test method to verify Firebase write
    private void testFirebaseWrite() {
        String email = sessionManager.getLoggedInEmail();
        if (email == null) {
            Toast.makeText(requireContext(), "Not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String safeUserId = email.replace(".", "_");
        java.util.Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("test", "Hello Firebase!");
        testData.put("timestamp", System.currentTimeMillis());
        
        android.util.Log.d("ActivityFragment", "🔥 TEST WRITE to users/" + safeUserId + "/activities/");
        
        db.collection("users")
                .document(safeUserId)
                .collection("activities")
                .add(testData)
                .addOnSuccessListener(ref -> {
                    String msg = "✅ TEST SUCCESS! Doc ID: " + ref.getId();
                    android.util.Log.d("ActivityFragment", msg);
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    String msg = "❌ TEST FAILED: " + e.getMessage();
                    android.util.Log.e("ActivityFragment", msg, e);
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                });
    }


    private void loadHistory() {
        String email = sessionManager.getLoggedInEmail();
        
        if (email == null || email.isEmpty()) {
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            tvHistoryEmpty.setText("Please login to view activity history");
            android.util.Log.d("ActivityFragment", "No email - showing login prompt");
            return;
        }

        // Use same pattern as RunHistoryFragment
        String safeUserId = email.replace(".", "_");
        
        android.util.Log.d("ActivityFragment", "========== LOAD HISTORY ==========");
        android.util.Log.d("ActivityFragment", "Email: " + email);
        android.util.Log.d("ActivityFragment", "Safe UserId: " + safeUserId);
        android.util.Log.d("ActivityFragment", "Loading from: users/" + safeUserId + "/activities");
        
        // Force fetch from server, not cache
        db.collection("users")
                .document(safeUserId)
                .collection("activities")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get(com.google.firebase.firestore.Source.SERVER) // Force server fetch
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("ActivityFragment", "Firestore query success - " + queryDocumentSnapshots.size() + " documents found");
                    historyContainer.removeAllViews();
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                        tvHistoryEmpty.setText("No activity history yet. Start tracking to see your progress!");
                        updateChart(null); // Clear chart
                        return;
                    }
                    
                    tvHistoryEmpty.setVisibility(View.GONE);
                    
                    // Collect all activities for chart
                    java.util.List<ActivityData> activities = new java.util.ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ActivityData activity = document.toObject(ActivityData.class);
                        android.util.Log.d("ActivityFragment", "Loaded activity: " + activity.getStepCount() + " steps on " + activity.getFormattedDate());
                        activities.add(activity);
                        addHistoryItem(activity);
                    }
                    
                    // Update chart with loaded activities
                    updateChart(activities);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ActivityFragment", "Error loading history: " + e.getMessage(), e);
                    tvHistoryEmpty.setVisibility(View.VISIBLE);
                    tvHistoryEmpty.setText("Error loading history: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to load history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addHistoryItem(ActivityData activity) {
        View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.item_activity_history, historyContainer, false);
        
        TextView tvDate = itemView.findViewById(R.id.tv_history_date);
        TextView tvSteps = itemView.findViewById(R.id.tv_history_steps);
        TextView tvLevel = itemView.findViewById(R.id.tv_history_level);
        TextView tvTime = itemView.findViewById(R.id.tv_history_time);
        
        tvDate.setText(activity.getFormattedDate());
        tvSteps.setText(activity.getStepCount() + " pas");
        tvLevel.setText("Niveau: " + activity.getActivityLevel());
        tvTime.setText("Temps: " + activity.getActiveTime() + " min");
        
        historyContainer.addView(itemView);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int currentSteps = (int) event.values[0];
            
            android.util.Log.d("ActivityFragment", "=== SENSOR EVENT ===");
            android.util.Log.d("ActivityFragment", "Current sensor value: " + currentSteps);
            android.util.Log.d("ActivityFragment", "Is counting: " + isCounting);
            android.util.Log.d("ActivityFragment", "Current baseline: " + baseline);
            android.util.Log.d("ActivityFragment", "Current totalSteps: " + totalSteps);

            // Only process if we're actively counting
            if (!isCounting) {
                android.util.Log.d("ActivityFragment", "Not counting, ignoring sensor data");
                return;
            }

            // Set baseline on first reading when we start counting
            if (baseline == -1) {
                baseline = currentSteps;
                android.util.Log.d("ActivityFragment", "*** BASELINE SET TO: " + baseline + " ***");
                saveState();
                // Don't calculate steps on the first reading, just set baseline
                return;
            }

            // Calculate steps since we started counting
            totalSteps = currentSteps - baseline;
            
            // Handle sensor reset (rare but possible)
            if (totalSteps < 0) {
                android.util.Log.w("ActivityFragment", "Negative steps detected! Sensor may have reset. Resetting baseline.");
                baseline = currentSteps;
                totalSteps = 0;
            }

            android.util.Log.d("ActivityFragment", "*** CALCULATED STEPS: " + totalSteps + " ***");

            saveState();
            
            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::updateUI);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for step counter
    }

    private void updateUI() {
        tvStepCount.setText(String.valueOf(totalSteps));

        // Update progress
        int progress = Math.min((totalSteps * 100) / DAILY_GOAL, 100);
        progressBar.setProgress(progress);
        tvGoalProgress.setText(progress + "% de 10,000");

        // Update activity level
        tvActivityLevel.setText(String.valueOf(getActivityLevelNumber()));

        // Update active time
        if (isCounting && startTime > 0) {
            long elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60);
            tvActiveTime.setText(elapsedMinutes + " min");
        } else if (startTime > 0) {
            long elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60);
            tvActiveTime.setText(elapsedMinutes + " min");
        } else {
            tvActiveTime.setText("0 min");
        }
    }

    private int getActivityLevelNumber() {
        if (totalSteps < 2000) return 0;
        else if (totalSteps < 5000) return 1;
        else if (totalSteps < 7500) return 2;
        else if (totalSteps < 10000) return 3;
        else return 4;
    }

    private String getActivityLevelText() {
        int level = getActivityLevelNumber();
        switch (level) {
            case 0: return "Sedentary";
            case 1: return "Lightly Active";
            case 2: return "Moderately Active";
            case 3: return "Active";
            case 4: return "Very Active";
            default: return "Unknown";
        }
    }

    private void updateButtons() {
        if (isCounting) {
            btnStart.setEnabled(false);
            btnStart.setText("DÉMARRER");
            btnStop.setEnabled(true);
            btnReset.setVisibility(View.GONE);
        } else {
            btnStart.setEnabled(true);
            btnStart.setText("DÉMARRER");
            btnStop.setEnabled(false);
            btnReset.setVisibility(View.VISIBLE);
        }
    }

    private void saveState() {
        prefs.edit()
                .putInt(KEY_BASELINE, baseline)
                .putInt(KEY_TOTAL_STEPS, totalSteps)
                .putBoolean(KEY_IS_COUNTING, isCounting)
                .putLong(KEY_START_TIME, startTime)
                .apply();
    }

    private void loadSavedState() {
        baseline = prefs.getInt(KEY_BASELINE, -1);
        totalSteps = prefs.getInt(KEY_TOTAL_STEPS, 0);
        isCounting = prefs.getBoolean(KEY_IS_COUNTING, false);
        startTime = prefs.getLong(KEY_START_TIME, 0);

        // If we were counting before, resume
        if (isCounting && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCounting();
            } else {
                Toast.makeText(requireContext(), "Permission required to count steps", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isCounting && stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        updateUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isCounting) {
            sensorManager.unregisterListener(this);
        }
    }

    private void setupChart(com.github.mikephil.charting.charts.BarChart chart) {
        this.activityChart = chart;
        
        // Chart styling
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setHighlightFullBarEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawValueAboveBar(true);
        
        // X-Axis
        com.github.mikephil.charting.components.XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(new String[]{""}));
        
        // Y-Axis
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        
        // Legend
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT);
        
        chart.animateY(1000);
    }

    private void updateChart(java.util.List<ActivityData> activities) {
        if (activityChart == null || activities == null || activities.isEmpty()) {
            if (activityChart != null) {
                activityChart.clear();
                activityChart.invalidate();
            }
            return;
        }

        java.util.List<com.github.mikephil.charting.data.BarEntry> entries = new java.util.ArrayList<>();
        java.util.List<String> labels = new java.util.ArrayList<>();
        
        // Reverse to show oldest first (left to right)
        java.util.List<ActivityData> reversedList = new java.util.ArrayList<>(activities);
        java.util.Collections.reverse(reversedList);
        
        // Take last 7 activities
        int size = Math.min(reversedList.size(), 7);
        for (int i = 0; i < size; i++) {
            ActivityData activity = reversedList.get(i);
            entries.add(new com.github.mikephil.charting.data.BarEntry(i, activity.getStepCount()));
            
            // Format date to MM/DD
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(activity.getFormattedDate());
                labels.add(outputFormat.format(date));
            } catch (Exception e) {
                labels.add(activity.getFormattedDate());
            }
        }

        com.github.mikephil.charting.data.BarDataSet dataSet = new com.github.mikephil.charting.data.BarDataSet(entries, "Steps");
        dataSet.setColor(android.graphics.Color.parseColor("#6200EE"));
        dataSet.setValueTextColor(android.graphics.Color.BLACK);
        dataSet.setValueTextSize(10f);

        com.github.mikephil.charting.data.BarData barData = new com.github.mikephil.charting.data.BarData(dataSet);
        barData.setBarWidth(0.8f);

        activityChart.setData(barData);
        activityChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        activityChart.getXAxis().setLabelCount(labels.size());
        activityChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isCounting) {
            sensorManager.unregisterListener(this);
        }
    }
}
