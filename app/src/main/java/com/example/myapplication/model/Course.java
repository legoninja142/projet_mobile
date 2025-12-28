package com.example.myapplication.model;

public class Course {
    private String id;
    private String userId;
    private long startTime;
    private long endTime;
    private long duration;
    private float distance;
    private float avgSpeed;
    private float maxSpeed;
    private int steps;
    private int calories;

    // Required for Firestore serialization
    public Course() {}

    public Course(String id, String userId, long startTime, long endTime, long duration,
                  float distance, float avgSpeed, float maxSpeed, int steps, int calories) {
        this.id = id;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.distance = distance;
        this.avgSpeed = avgSpeed;
        this.maxSpeed = maxSpeed;
        this.steps = steps;
        this.calories = calories;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    // Helper method to format duration
    public String getFormattedDuration() {
        long hours = duration / 3600000;
        long minutes = (duration % 3600000) / 60000;
        long seconds = (duration % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Helper method to format distance
    public String getFormattedDistance() {
        return String.format("%.2f km", distance);
    }

    // Helper method to format speed
    public String getFormattedAvgSpeed() {
        return String.format("%.2f km/h", avgSpeed);
    }
}
