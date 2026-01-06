package com.example.myapplication.model;

import java.util.Date;
import java.util.List;

public class SleepSensorData {
    private String id;
    private String userId;
    private Date sessionStartTime;
    private Date sessionEndTime;
    private List<MovementData> movementData;
    private List<LightData> lightData;
    private int estimatedSleepQuality;
    private float averageMovementPerHour;
    private float averageLightLevel;
    private boolean wasDarkEnvironment;

    // Constructors
    public SleepSensorData() {}

    public SleepSensorData(String userId, Date sessionStartTime) {
        this.userId = userId;
        this.sessionStartTime = sessionStartTime;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getSessionStartTime() { return sessionStartTime; }
    public void setSessionStartTime(Date sessionStartTime) { this.sessionStartTime = sessionStartTime; }

    public Date getSessionEndTime() { return sessionEndTime; }
    public void setSessionEndTime(Date sessionEndTime) { this.sessionEndTime = sessionEndTime; }

    public List<MovementData> getMovementData() { return movementData; }
    public void setMovementData(List<MovementData> movementData) { this.movementData = movementData; }

    public List<LightData> getLightData() { return lightData; }
    public void setLightData(List<LightData> lightData) { this.lightData = lightData; }

    public int getEstimatedSleepQuality() { return estimatedSleepQuality; }
    public void setEstimatedSleepQuality(int estimatedSleepQuality) { this.estimatedSleepQuality = estimatedSleepQuality; }

    public float getAverageMovementPerHour() { return averageMovementPerHour; }
    public void setAverageMovementPerHour(float averageMovementPerHour) { this.averageMovementPerHour = averageMovementPerHour; }

    public float getAverageLightLevel() { return averageLightLevel; }
    public void setAverageLightLevel(float averageLightLevel) { this.averageLightLevel = averageLightLevel; }

    public boolean isWasDarkEnvironment() { return wasDarkEnvironment; }
    public void setWasDarkEnvironment(boolean wasDarkEnvironment) { this.wasDarkEnvironment = wasDarkEnvironment; }
}