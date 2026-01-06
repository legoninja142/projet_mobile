package com.example.myapplication.view;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class MotivationActivity extends AppCompatActivity implements SensorEventListener {

    private float lastMagnitude = 0f;
    private long lastMoveTime = 0L;
    private boolean isMoving = false;

    private MediaPlayer startPlayer;
    private MediaPlayer stopPlayer;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motivation);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (accelerometer == null) {
            Toast.makeText(this, "Accéléromètre non disponible", Toast.LENGTH_LONG).show();
        }

        startPlayer = MediaPlayer.create(this, R.raw.motivation_start);
        stopPlayer = MediaPlayer.create(this, R.raw.motivation_stop);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = Math.abs(magnitude - lastMagnitude);

            if (delta > 1.5f) {
                if (!isMoving) {
                    if (startPlayer != null) startPlayer.start();
                    isMoving = true;
                }
                lastMoveTime = System.currentTimeMillis();
            } else {
                if (isMoving && System.currentTimeMillis() - lastMoveTime > 3000) {
                    if (stopPlayer != null && !stopPlayer.isPlaying()) {
                        stopPlayer.start();
                    }
                    isMoving = false;
                }
            }
            lastMagnitude = magnitude;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (startPlayer != null) startPlayer.release();
        if (stopPlayer != null) stopPlayer.release();
    }
}
