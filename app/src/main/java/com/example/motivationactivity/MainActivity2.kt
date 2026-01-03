package com.example.motivationactivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity(), android.hardware.SensorEventListener {
    private var lastMagnitude = 0f
    private var lastMoveTime = 0L

    private var startPlayer: android.media.MediaPlayer? = null
    private var stopPlayer: android.media.MediaPlayer? = null
    private var isMoving = false



    private lateinit var sensorManager: android.hardware.SensorManager
    private var accelerometer: android.hardware.Sensor? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            android.widget.Toast.makeText(this, "Accéléromètre non disponible", android.widget.Toast.LENGTH_LONG).show()
        }
        startPlayer = android.media.MediaPlayer.create(this, R.raw.motivation_start)
        stopPlayer = android.media.MediaPlayer.create(this, R.raw.motivation_stop)


    }
    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
        if (event?.sensor?.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = kotlin.math.sqrt((x*x + y*y + z*z).toDouble()).toFloat()
            val delta = kotlin.math.abs(magnitude - lastMagnitude)

            if (delta > 1.5f) {
                if (!isMoving) {
                    startPlayer?.start()
                    isMoving = true
                }
                lastMoveTime = System.currentTimeMillis()
            } else {
                if (isMoving && System.currentTimeMillis() - lastMoveTime > 3000) {
                    if (stopPlayer?.isPlaying == false) {
                        stopPlayer?.start()
                    }
                    isMoving = false
                }
            }

            lastMagnitude = magnitude
        }
    }


    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {
        // pas utilisé
    }
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                android.hardware.SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        startPlayer?.release()
        stopPlayer?.release()
    }


}
