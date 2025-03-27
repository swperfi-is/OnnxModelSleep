package com.example.onnxmodelsleep

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.*

class SensorService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var ortSession: OrtSession
    private lateinit var ortEnv: OrtEnvironment
    private val handler = Handler()
    private val interval = 60000L // 1 minuto
    private var latestData = FloatArray(8)

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        // Foreground Service com notificação
        val channelId = "sleep_tracking_channel"
        val channelName = "Sleep Tracking Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitorando sono")
            .setContentText("O app está registrando dados dos sensores.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL)

        val modelFile = File(filesDir, "random_forest_sleep.onnx")
        assets.open("random_forest_sleep.onnx").use { input ->
            FileOutputStream(modelFile, false).use { output ->
                input.copyTo(output)
            }
        }

        ortEnv = OrtEnvironment.getEnvironment()
        ortSession = ortEnv.createSession(modelFile.absolutePath)

        handler.post(dataCollector)
    }

    private val dataCollector = object : Runnable {
        override fun run() {
            runInference()
            handler.postDelayed(this, interval)
        }
    }

    private fun runInference() {
        Log.d("SensorService", "Executando inferência...")

        val buffer = FloatBuffer.wrap(latestData)
        val inputTensor = OnnxTensor.createTensor(ortEnv, buffer, longArrayOf(1, 8))
        val output = ortSession.run(mapOf("float_input" to inputTensor))
        val prediction = (output[0].value as LongArray)[0].toInt()

        Log.d("SensorService", "Predição ONNX: $prediction")
        Log.d("SensorService", "Dados de entrada: ${latestData.joinToString()}")

        saveToLog(prediction.toLong())
    }


    private fun saveToLog(prediction: Long) {
        val timestamp = System.currentTimeMillis()
        val log = "$timestamp,$prediction\n"
        try {
            openFileOutput("sleep_log.csv", MODE_APPEND).use { fos ->
                fos.write(log.toByteArray())
            }
            Log.d("ONNX", "Log salvo: $log")
        } catch (e: Exception) {
            Log.e("ONNX", "Erro ao salvar log: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_LIGHT -> latestData[0] = it.values[0]
                Sensor.TYPE_PROXIMITY -> latestData[1] = it.values[0]
                Sensor.TYPE_ACCELEROMETER -> {
                    latestData[2] = it.values[0]
                    latestData[3] = it.values[1]
                    latestData[4] = it.values[2]
                }
                Sensor.TYPE_GYROSCOPE -> {
                    latestData[5] = it.values[0]
                    latestData[6] = it.values[1]
                    latestData[7] = it.values[2]
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        handler.removeCallbacks(dataCollector)
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}
