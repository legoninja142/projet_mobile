package com.example.myapplication.ui.run;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.google.android.gms.location.*;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class RunFragment extends Fragment {

    private MaterialButton btnStart, btnStop;
    private TextView tvTime, tvDistance, tvSpeed;
    private RunViewModel viewModel;
    private SessionManager sessionManager;

    private static final int LOCATION_REQUEST = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Chrono
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private long startTimeInMillis = 0L;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_run, container, false);

        btnStart = view.findViewById(R.id.btnStart);
        btnStop = view.findViewById(R.id.btnStop);

        tvTime = view.findViewById(R.id.tvTime);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvSpeed = view.findViewById(R.id.tvSpeed);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(RunViewModel.class);

        // ================= GPS INIT =================
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000
        ).setMinUpdateIntervalMillis(1000).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    float speed = location.hasSpeed() ? location.getSpeed() : 0f;
                    viewModel.onLocationUpdate(lat, lon, speed);
                }
            }
        };

        // ================= OBSERVERS =================
        viewModel.isRunning().observe(getViewLifecycleOwner(), this::showRunButtons);

        viewModel.getDistanceLive().observe(getViewLifecycleOwner(), dist ->
                tvDistance.setText(String.format(Locale.getDefault(), "%.1f", dist )));

        viewModel.getAvgSpeedLive().observe(getViewLifecycleOwner(), speed ->
                tvSpeed.setText(String.format(Locale.getDefault(), "%.2f", speed )));

        viewModel.getDurationLive().observe(getViewLifecycleOwner(), duration -> {
            long seconds = duration / 1000;
            long hrs = seconds / 3600;
            long mins = (seconds % 3600) / 60;
            long secs = seconds % 60;
            tvTime.setText(String.format("%02d:%02d:%02d", hrs, mins, secs));
        });

        // ================= BUTTONS =================
        btnStart.setOnClickListener(v -> {
            if (!hasLocationPermission()) {
                requestLocationPermission();
                return;
            }

            String email = sessionManager.getLoggedInEmail();
            if (email == null || email.isEmpty()) {
                Toast.makeText(getContext(),
                        "Utilisateur non connecté ❌",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.startRun(email);

            startTimeInMillis = System.currentTimeMillis();
            startTimer();
            startLocationUpdates();
        });

        btnStop.setOnClickListener(v -> {
            stopLocationUpdates();
            stopTimer();
            viewModel.stopRun();
        });

        return view;
    }

    // ================= CHRONO =================
    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTimeInMillis;
                int seconds = (int) (elapsed / 1000) % 60;
                int minutes = (int) ((elapsed / (1000*60)) % 60);
                int hours = (int) ((elapsed / (1000*60*60)) % 24);
                tvTime.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

    // ================= GPS =================
    private void startLocationUpdates() {
        if (!hasLocationPermission()) return;
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireActivity().getMainLooper());
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // ================= UI =================
    private void showRunButtons(boolean running) {
        btnStart.setVisibility(running ? View.GONE : View.VISIBLE);
        btnStop.setVisibility(running ? View.VISIBLE : View.GONE);
    }

    // ================= PERMISSIONS =================
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(getContext(), "Permission GPS accordée ✔", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getContext(), "Permission GPS refusée ❌", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }
}
