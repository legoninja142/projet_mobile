// Create: app/src/main/java/com/example/myapplication/model/MovementData.java
package com.example.myapplication.model;

import java.util.Date;

public class MovementData {
    private Date timestamp;
    private float x;
    private float y;
    private float z;
    private float magnitude;
    private boolean isSignificantMovement;

    public MovementData() {}

    public MovementData(Date timestamp, float x, float y, float z) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.magnitude = (float) Math.sqrt(x*x + y*y + z*z);
        this.isSignificantMovement = magnitude > 1.2f; // Threshold for significant movement
    }

    // Getters and setters
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getZ() { return z; }
    public void setZ(float z) { this.z = z; }

    public float getMagnitude() { return magnitude; }
    public void setMagnitude(float magnitude) { this.magnitude = magnitude; }

    public boolean isSignificantMovement() { return isSignificantMovement; }
    public void setSignificantMovement(boolean significantMovement) { isSignificantMovement = significantMovement; }
}
