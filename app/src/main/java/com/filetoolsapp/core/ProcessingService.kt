package com.filetoolsapp.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.filetoolsapp.R

class ProcessingService : Service() {

    companion object {
        const val CHANNEL_ID = "processing_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_TASK_NAME = "task_name"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskName = intent?.getStringExtra(EXTRA_TASK_NAME) ?: "Processing..."

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FileTools")
            .setContentText(taskName)
            .setSmallIcon(R.drawable.ic_processing)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "File Processing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress of file processing tasks"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
