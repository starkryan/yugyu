package com.performance.enhancer.optimization.suite.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var dismissJob: Job? = null

    companion object {
        private const val TAG = "OverlayService"
        const val ACTION_SHOW_SMS = "SHOW_SMS"
        const val ACTION_HIDE_OVERLAY = "HIDE_OVERLAY"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_CONTENT = "content"
        const val EXTRA_SOURCE_APP = "source_app"
        private const val OVERLAY_DURATION = 5000L // 5 seconds
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_SMS -> showSmsOverlay(
                intent.getStringExtra(EXTRA_SENDER) ?: "Unknown",
                intent.getStringExtra(EXTRA_CONTENT) ?: "",
                intent.getStringExtra(EXTRA_SOURCE_APP) ?: ""
            )
            ACTION_HIDE_OVERLAY -> hideOverlay()
        }
    }

    private fun showSmsOverlay(sender: String, content: String, sourceApp: String) {
        // Cancel any existing dismiss job
        dismissJob?.cancel()

        // Remove existing overlay if present
        hideOverlay()

        try {
            // Inflate overlay layout
            overlayView = LayoutInflater.from(this).inflate(
                com.performance.enhancer.optimization.suite.R.layout.sms_overlay_layout,
                null
            )

            // Set message content
            overlayView?.findViewById<TextView>(
                com.performance.enhancer.optimization.suite.R.id.tv_sender
            )?.text = sender

            overlayView?.findViewById<TextView>(
                com.performance.enhancer.optimization.suite.R.id.tv_content
            )?.text = content

            overlayView?.findViewById<TextView>(
                com.performance.enhancer.optimization.suite.R.id.tv_source_app
            )?.text = sourceApp

            // Set up dismiss on click
            overlayView?.setOnClickListener {
                hideOverlay()
            }

            // Create layout parameters for overlay
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 200 // Position from top
            }

            // Add overlay to window
            windowManager?.addView(overlayView, params)

            // Auto-dismiss after duration
            dismissJob = serviceScope.launch {
                delay(OVERLAY_DURATION)
                hideOverlay()
            }

        } catch (e: Exception) {
            // Handle overlay display errors (e.g., permission denied)
            e.printStackTrace()
        }
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                // View might already be removed
                e.printStackTrace()
            }
            overlayView = null
        }
        dismissJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        serviceScope.cancel()
    }
}