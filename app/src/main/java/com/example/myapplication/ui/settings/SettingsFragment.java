package com.example.myapplication.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.controller.SessionManager;
import com.example.myapplication.view.WelcomeActivity;

public class SettingsFragment extends Fragment {

    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        sessionManager = new SessionManager(requireContext());

        // Setup settings options
        setupSettingsOptions(root);

        return root;
    }

    private void setupSettingsOptions(View root) {
        // Profile settings
        CardView cardProfile = root.findViewById(R.id.card_profile);
        cardProfile.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Profile settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // Notifications settings
        CardView cardNotifications = root.findViewById(R.id.card_notifications);
        cardNotifications.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Notification settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // Privacy settings
        CardView cardPrivacy = root.findViewById(R.id.card_privacy);
        cardPrivacy.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });

        // About
        CardView cardAbout = root.findViewById(R.id.card_about);
        cardAbout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Fitness Club v1.0", Toast.LENGTH_SHORT).show();
        });

        // Display user info
        TextView tvUserEmail = root.findViewById(R.id.tv_user_email);
        String userEmail = sessionManager.getLoggedInEmail();
        if (userEmail != null && !userEmail.isEmpty()) {
            tvUserEmail.setText(userEmail);
        }
    }
}
