package com.performance.enhancer.optimization.suite.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class SMSMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var monitoringJob: Job? = null

    companion object {
        private const val TAG = "SMSMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_monitor_channel"
        private const val MONITORING_INTERVAL = 30000L // 30 seconds
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors SMS notifications in the background"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("SMS Monitor Active")
        .setContentText("Monitoring SMS notifications...")
        .setSmallIcon(com.performance.enhancer.optimization.suite.R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkNotificationListenerStatus()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                    delay(MONITORING_INTERVAL) // Wait before retrying
                }
            }
        }
    }

    private suspend fun checkNotificationListenerStatus() {
        val componentName = "com.performance.enhancer.optimization.suite/com.performance.enhancer.optimization.suite.service.SMSNotificationService"
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )

        val isListenerEnabled = enabledListeners?.contains(componentName) == true

        if (!isListenerEnabled) {
            Log.w(TAG, "Notification listener is not enabled")
            // Could send a broadcast to notify user or show a notification
        } else {
            Log.d(TAG, "Notification listener is working")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
    }
}