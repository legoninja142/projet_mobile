package com.example.myapplication.ui.home;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentHomeBinding;

import java.util.Locale;

public class HomeFragment extends Fragment implements SensorEventListener {

    private FragmentHomeBinding binding;
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private ImageView ivHumanPose;
    private TextView tvRotationX;
    private TextView tvRotationY;
    private TextView tvGyroStatus;
    
    // Rotation angles
    private float currentRotationX = 0f;
    private float currentRotationY = 0f;
    private float lastRotationX = 0f;
    private float lastRotationY = 0f;
    
    // Smoothing factor
    private static final float SMOOTHING_FACTOR = 0.1f;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // Initialize gyroscope components
        initializeGyroscope(root);

        // Setup navigation buttons
        setupNavigationButtons(root);

        return root;
    }

    private void initializeGyroscope(View root) {
        // Get references to views
        ivHumanPose = root.findViewById(R.id.ivHumanPose);
        tvRotationX = root.findViewById(R.id.tvRotationX);
        tvRotationY = root.findViewById(R.id.tvRotationY);
        tvGyroStatus = root.findViewById(R.id.tvGyroStatus);

        // Initialize sensor manager
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        
        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            
            if (gyroscope != null) {
                tvGyroStatus.setText("🔄 Active");
                tvGyroStatus.setTextColor(0xFF4CAF50);
            } else {
                tvGyroStatus.setText("⚠️ Not Available");
                tvGyroStatus.setTextColor(0xFFFF9800);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register gyroscope listener
        if (sensorManager != null && gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister gyroscope listener to save battery
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Get gyroscope data (rotation rate in rad/s around each axis)
            float rotationRateX = event.values[0]; // Rotation around X-axis (pitch)
            float rotationRateY = event.values[1]; // Rotation around Y-axis (roll)
            float rotationRateZ = event.values[2]; // Rotation around Z-axis (yaw)

            // Calculate time delta in seconds
            float dT = 0.02f; // Approximation for SENSOR_DELAY_GAME

            // Integrate rotation rates to get angles (convert from rad/s to degrees)
            float deltaX = (float) Math.toDegrees(rotationRateX * dT);
            float deltaY = (float) Math.toDegrees(rotationRateY * dT);

            // Apply smoothing
            currentRotationX = currentRotationX - deltaX * SMOOTHING_FACTOR;
            currentRotationY = currentRotationY + deltaY * SMOOTHING_FACTOR;

            // Limit rotation angles for better UX
            currentRotationX = Math.max(-45, Math.min(45, currentRotationX));
            currentRotationY = Math.max(-45, Math.min(45, currentRotationY));

            // Apply rotation to the image
            if (ivHumanPose != null) {
                // Combine X and Y rotations for 3D-like effect
                // Use rotationX for vertical tilt, rotationY for horizontal tilt
                ivHumanPose.setRotationX(currentRotationX);
                ivHumanPose.setRotationY(currentRotationY);
                
                // Also apply a subtle Z-axis rotation for more dynamic feel
                ivHumanPose.setRotation(currentRotationY * 0.3f);
            }

            // Update rotation text displays
            if (tvRotationX != null) {
                tvRotationX.setText(String.format(Locale.getDefault(), "X: %.1f°", currentRotationX));
            }
            if (tvRotationY != null) {
                tvRotationY.setText(String.format(Locale.getDefault(), "Y: %.1f°", currentRotationY));
            }

            // Store last rotation for next calculation
            lastRotationX = currentRotationX;
            lastRotationY = currentRotationY;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
        if (tvGyroStatus != null) {
            switch (accuracy) {
                case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                    tvGyroStatus.setText("🔄 Active (High)");
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                    tvGyroStatus.setText("🔄 Active (Medium)");
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                    tvGyroStatus.setText("⚠️ Active (Low)");
                    break;
                case SensorManager.SENSOR_STATUS_UNRELIABLE:
                    tvGyroStatus.setText("⚠️ Unreliable");
                    break;
            }
        }
    }

    private void setupNavigationButtons(View root) {
        // Courses button
        CardView cardCourses = root.findViewById(R.id.card_courses);
        cardCourses.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_courses);
        });

        // Settings button
        CardView cardSettings = root.findViewById(R.id.card_settings);
        cardSettings.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_settings);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister sensor listener
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        binding = null;
    }
}