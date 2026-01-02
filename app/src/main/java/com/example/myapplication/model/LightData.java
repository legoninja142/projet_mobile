// Create: app/src/main/java/com/example/myapplication/model/LightData.java
package com.example.myapplication.model;

import java.util.Date;

public class LightData {
    private Date timestamp;
    private float lightLevel; // in lux
    private boolean isDark; // < 10 lux

    public LightData() {}

    public LightData(Date timestamp, float lightLevel) {
        this.timestamp = timestamp;
        this.lightLevel = lightLevel;
        this.isDark = lightLevel < 10.0f; // 10 lux threshold for darkness
    }

    // Getters and setters
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public float getLightLevel() { return lightLevel; }
    public void setLightLevel(float lightLevel) { this.lightLevel = lightLevel; }

    public boolean isDark() { return isDark; }
    public void setDark(boolean dark) { isDark = dark; }
}