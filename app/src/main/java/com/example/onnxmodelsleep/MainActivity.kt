package com.example.onnxmodelsleep

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Solicitar permissões
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1)
        }

        // Iniciar Foreground Service
        val intent = Intent(this, SensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }

        // Mostrar resumo, se existir
        val summaryTextView = findViewById<TextView>(R.id.statusText)
        val summaryFile = File(filesDir, "summary.txt")
        if (summaryFile.exists()) {
            val resumo = summaryFile.readText()
            summaryTextView.text = "Resumo de hoje:\n$resumo"
        } else {
            summaryTextView.text = "Monitorando Sono..."
        }
    }
}