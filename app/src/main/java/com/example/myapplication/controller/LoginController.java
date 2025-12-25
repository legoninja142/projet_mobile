package com.example.myapplication.controller;

import android.util.Patterns;

import com.example.myapplication.model.User;

public class LoginController {
    // Demo credentials - replace with real auth in production
    private static final String DEMO_EMAIL = "user@example.com";
    private static final String DEMO_PASSWORD = "password123";

    public boolean authenticate(User user) {
        if (user == null || !user.isValid()) return false;
        if (!Patterns.EMAIL_ADDRESS.matcher(user.getEmail()).matches()) return false;
        // For demo purposes, check against a hard-coded credential set
        return DEMO_EMAIL.equals(user.getEmail()) && DEMO_PASSWORD.equals(user.getPassword());
    }
}

