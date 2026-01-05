package com.example.myapplication.model;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Course {

    private String id;
    private String userId;

    private long startTime;
    private long endTime;
    private long duration;

    private float distance;     // en mètres
    private float avgSpeed;     // m/s
    private float maxSpeed;     // m/s

    private boolean completed;

    // Constructeur vide requis par Firestore
    public Course() {}

    public Course(String userId, long startTime) {
        this.userId = userId;
        this.startTime = startTime;
        this.distance = 0f;
        this.avgSpeed = 0f;
        this.maxSpeed = 0f;
        this.completed = false;
    }

    // Appelé à la fin de la course pour calculer duration et avgSpeed
    public void finish(long endTime) {
        this.endTime = endTime;
        this.duration = endTime - startTime;
        this.completed = true;

        if (duration > 0) {
            this.avgSpeed = distance / (duration / 1000f); // m/s
        }
    }

    public void addDistance(float meters) {
        this.distance += meters;
    }

    public void updateSpeed(float speed) {
        if (speed > maxSpeed) maxSpeed = speed;
    }

    /* ================= GETTERS POUR FIRESTORE ================= */
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public long getDuration() { return duration; }
    public float getDistance() { return distance; }
    public float getAvgSpeed() { return avgSpeed; }
    public float getMaxSpeed() { return maxSpeed; }
    public boolean isCompleted() { return completed; }

    /* ================= SETTERS ================= */
    public void setId(String id) { this.id = id; }
    public static String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }

}
