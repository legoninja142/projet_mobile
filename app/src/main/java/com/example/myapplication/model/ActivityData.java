package com.example.myapplication.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActivityData {
    private String id;
    private String userId;
    private int stepCount;
    private String activityLevel;
    private long activeTime; // in minutes
    private long date; // timestamp
    
    // Required empty constructor for Firestore
    public ActivityData() {}
    
    public ActivityData(String userId, int stepCount, String activityLevel, long activeTime, long date) {
        this.userId = userId;
        this.stepCount = stepCount;
        this.activityLevel = activityLevel;
        this.activeTime = activeTime;
        this.date = date;
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public int getStepCount() { return stepCount; }
    public String getActivityLevel() { return activityLevel; }
    public long getActiveTime() { return activeTime; }
    public long getDate() { return date; }
    
    // Setters for Firestore
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setStepCount(int stepCount) { this.stepCount = stepCount; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
    public void setActiveTime(long activeTime) { this.activeTime = activeTime; }
    public void setDate(long date) { this.date = date; }
    
    // Helper method to format date - exclude from Firestore
    @com.google.firebase.firestore.Exclude
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(date));
    }
}
