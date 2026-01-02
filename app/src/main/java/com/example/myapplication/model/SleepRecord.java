package com.example.myapplication.model;

import java.util.Date;

public class SleepRecord {
    private String id;
    private String userId;
    private Date sleepStartTime;
    private Date sleepEndTime;
    private int sleepDuration; // in minutes
    private int sleepQuality; // 1-5 scale
    private String notes;
    private Date recordDate;

    public SleepRecord() {
        // Required for Firestore
    }

    public SleepRecord(String userId, Date sleepStartTime, Date sleepEndTime,
                       int sleepQuality, String notes) {
        this.userId = userId;
        this.sleepStartTime = sleepStartTime;
        this.sleepEndTime = sleepEndTime;
        this.sleepQuality = sleepQuality;
        this.notes = notes;
        this.recordDate = new Date();

        // Calculate duration in minutes (handles overnight sleep)
        if (sleepStartTime != null && sleepEndTime != null) {
            long diff = sleepEndTime.getTime() - sleepStartTime.getTime();
            // If diff is negative, it means end time is before start time,
            // which shouldn't happen with proper date selection
            // But we'll still calculate absolute value
            this.sleepDuration = (int) (Math.abs(diff) / (60 * 1000));
        }
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getSleepStartTime() { return sleepStartTime; }
    public void setSleepStartTime(Date sleepStartTime) { this.sleepStartTime = sleepStartTime; }

    public Date getSleepEndTime() { return sleepEndTime; }
    public void setSleepEndTime(Date sleepEndTime) { this.sleepEndTime = sleepEndTime; }

    public int getSleepDuration() { return sleepDuration; }
    public void setSleepDuration(int sleepDuration) { this.sleepDuration = sleepDuration; }

    public int getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(int sleepQuality) { this.sleepQuality = sleepQuality; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getRecordDate() { return recordDate; }
    public void setRecordDate(Date recordDate) { this.recordDate = recordDate; }
}