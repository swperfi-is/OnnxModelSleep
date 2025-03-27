package com.example.onnxmodelsleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class SleepSummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val internalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val sleepDir = File(internalDir, "SleepResults")

        if (!sleepDir.exists()) {
            sleepDir.mkdirs()
        }

        val summaryFile = File(sleepDir, "summary.txt")

        val file = File(context.filesDir, "sleep_log.csv")
        if (!file.exists()) return

        val entries = file.readLines().mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) Pair(parts[0].toLong(), parts[1].toInt()) else null
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val midnight = calendar.timeInMillis

        val todayEntries = entries.filter { it.first >= midnight }
        var sleepTime: Long? = null
        var wakeTime: Long? = null
        for (i in 1 until todayEntries.size) {
            val prev = todayEntries[i - 1]
            val curr = todayEntries[i]
            if (prev.second == 0 && curr.second == 1 && sleepTime == null) {
                sleepTime = prev.first
            }
            if (prev.second == 1 && curr.second == 0) {
                wakeTime = curr.first
                break
            }
        }

        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val result = "Dormiu: ${sdf.format(Date(sleepTime ?: 0))}, Acordou: ${sdf.format(Date(wakeTime ?: 0))}"

        try {
            FileWriter(summaryFile, false).use { writer ->
                writer.write(result)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}