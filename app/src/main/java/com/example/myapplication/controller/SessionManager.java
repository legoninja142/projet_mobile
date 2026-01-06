package com.example.myapplication.controller;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.myapplication.model.User;

public class SessionManager {
    private static final String PREF_NAME = "app_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_ID = "user_id";

    private SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(User user) {
        if (user == null || user.getEmail() == null) return;
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_USER_ID, user.getUserId())
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getLoggedInEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getLoggedInUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
