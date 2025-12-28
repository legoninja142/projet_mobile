package com.example.myapplication.ui.course;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.model.Course;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class AddCourseActivity extends AppCompatActivity {

    private EditText etDistance, etDuration, etAvgSpeed, etMaxSpeed, etSteps, etCalories;
    private Button btnSaveCourse;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        // Initialize Firestore and SessionManager
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Course");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        etDistance = findViewById(R.id.et_distance);
        etDuration = findViewById(R.id.et_duration);
        etAvgSpeed = findViewById(R.id.et_avg_speed);
        etMaxSpeed = findViewById(R.id.et_max_speed);
        etSteps = findViewById(R.id.et_steps);
        etCalories = findViewById(R.id.et_calories);
        btnSaveCourse = findViewById(R.id.btn_save_course);

        // Set up save button click listener
        btnSaveCourse.setOnClickListener(v -> saveCourse());
    }

    private void saveCourse() {
        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        try {
            // Get values from input fields
            float distance = Float.parseFloat(etDistance.getText().toString());
            long duration = parseDuration(etDuration.getText().toString());
            float avgSpeed = Float.parseFloat(etAvgSpeed.getText().toString());
            float maxSpeed = Float.parseFloat(etMaxSpeed.getText().toString());
            int steps = Integer.parseInt(etSteps.getText().toString());
            int calories = Integer.parseInt(etCalories.getText().toString());

            // Get current time
            long currentTime = System.currentTimeMillis();
            long startTime = currentTime - duration;
            long endTime = currentTime;

            // Get user email
            String userEmail = sessionManager.getLoggedInEmail();
            if (userEmail == null || userEmail.isEmpty()) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create course object
            String courseId = UUID.randomUUID().toString();
            Course course = new Course(courseId, userEmail, startTime, endTime, duration,
                    distance, avgSpeed, maxSpeed, steps, calories);

            // Save to Firestore
            db.collection("courses")
                    .document(courseId)
                    .set(course)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddCourseActivity.this, "Course saved successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddCourseActivity.this,
                                "Error saving course: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs() {
        if (etDistance.getText().toString().trim().isEmpty() ||
                etDuration.getText().toString().trim().isEmpty() ||
                etAvgSpeed.getText().toString().trim().isEmpty() ||
                etMaxSpeed.getText().toString().trim().isEmpty() ||
                etSteps.getText().toString().trim().isEmpty() ||
                etCalories.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private long parseDuration(String durationStr) {
        // Expected format: HH:MM:SS or MM:SS
        String[] parts = durationStr.split(":");
        long hours = 0, minutes = 0, seconds = 0;

        if (parts.length == 3) {
            hours = Long.parseLong(parts[0]);
            minutes = Long.parseLong(parts[1]);
            seconds = Long.parseLong(parts[2]);
        } else if (parts.length == 2) {
            minutes = Long.parseLong(parts[0]);
            seconds = Long.parseLong(parts[1]);
        } else {
            minutes = Long.parseLong(parts[0]);
        }

        return (hours * 3600000) + (minutes * 60000) + (seconds * 1000);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
