package com.performance.enhancer.optimization.suite.utils

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    private const val TAG = "PermissionUtils"

    fun hasNotificationListenerPermission(context: Context): Boolean {
        val packageName = context.packageName
        val flatPackageName = "$packageName/com.performance.enhancer.optimization.suite.service.SMSNotificationService"

        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )

        return enabledListeners?.contains(flatPackageName) == true ||
               enabledListeners?.contains(packageName) == true
    }

    fun hasNotificationListenerServicePermission(context: Context): Boolean {
        return try {
            val componentName = android.content.ComponentName(context, com.performance.enhancer.optimization.suite.service.SMSNotificationService::class.java)
            val flatName = componentName.flattenToString()

            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )

            enabledListeners?.contains(flatName) == true
        } catch (e: Exception) {
            Log.w(TAG, "Error checking notification listener service permission", e)
            false
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return true // Overlay functionality removed
    }

    fun hasSystemAlertWindowPermission(context: Context): Boolean {
        return true // Overlay functionality removed
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return true // Notifications no longer used in this app
    }

    fun getNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun getOverlaySettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun getAppNotificationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

  
    fun hasReadPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReadPhoneNumbersPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // READ_PHONE_NUMBERS not available before Android 8
        }
    }

    fun hasPhonePermissions(context: Context): Boolean {
        return hasReadPhoneStatePermission(context) && hasReadPhoneNumbersPermission(context)
    }

    fun getPhonePermissionSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasNotificationListenerPermission(context) &&
                hasOverlayPermission(context) &&
                hasNotificationPermission(context) &&
                hasPhonePermissions(context)
    }

    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!hasNotificationListenerPermission(context)) {
            missing.add("Notification Listener Access")
        }

        if (!hasOverlayPermission(context)) {
            missing.add("Overlay Permission")
        }

        if (!hasNotificationPermission(context)) {
            missing.add("Notification Permission")
        }

        if (!hasReadPhoneStatePermission(context)) {
            missing.add("Phone State Permission")
        }

        if (!hasReadPhoneNumbersPermission(context)) {
            missing.add("Phone Numbers Permission")
        }

        return missing
    }

    /**
     * Enhanced permission handling methods inspired by EarnbySMS
     */

    /**
     * Gets all critical permissions required for the app to function
     */
    fun getCriticalPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.FOREGROUND_SERVICE
        )
    }

    /**
     * Gets all permissions (critical + non-critical)
     */
    fun getAllPermissions(): Array<String> {
        val permissions = mutableListOf<String>().apply {
            addAll(getCriticalPermissions())
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        }

        return permissions.toTypedArray()
    }

    /**
     * Checks if all critical permissions are granted
     */
    fun hasAllCriticalPermissions(context: Context): Boolean {
        val permissions = getCriticalPermissions()
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks if a specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Enhanced battery optimization check
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val packageName = context.packageName
                powerManager.isIgnoringBatteryOptimizations(packageName)
            } else {
                true // Battery optimization not available before Android 6
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking battery optimization status", e)
            false
        }
    }

    /**
     * Gets battery optimization settings intent
     */
    fun getBatteryOptimizationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * Requests battery optimization exemption smoothly
     */
    fun requestBatteryOptimizationExemption(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val packageName = context.packageName
                val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)

                if (!isIgnoringBatteryOptimizations) {
                    Log.d(TAG, "Requesting battery optimization exemption")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    context.startActivity(intent)
                } else {
                    Log.d(TAG, "Battery optimization already exempted")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request battery optimization exemption", e)
            // Continue anyway - this is not critical for service operation
        }
    }

    /**
     * Get missing permissions with user-friendly names
     */
    fun getMissingPermissionsWithNames(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (!isPermissionGranted(context, Manifest.permission.READ_PHONE_STATE)) {
            missing.add("Phone State Access")
        }

        if (!isPermissionGranted(context, Manifest.permission.READ_PHONE_NUMBERS)) {
            missing.add("Phone Numbers Access")
        }

        if (!hasNotificationListenerServicePermission(context)) {
            missing.add("Notification Listener Service Access")
        }

        if (!hasSystemAlertWindowPermission(context)) {
            missing.add("Display Over Other Apps (System Alert Window)")
        }

        if (!hasNotificationPermission(context)) {
            missing.add("Post Notifications")
        }

        if (!isBatteryOptimizationDisabled(context)) {
            missing.add("Battery Optimization Exemption")
        }

        return missing
    }

    /**
     * Enhanced methods for handling special permissions
     */

    /**
     * Get specific intent for notification listener service settings
     */
    fun getNotificationListenerServiceSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    /**
     * Get specific intent for system alert window (overlay) settings
     */
    fun getSystemAlertWindowSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * Check if notification listener service permission needs to be requested
     */
    fun needsNotificationListenerServicePermission(context: Context): Boolean {
        return !hasNotificationListenerServicePermission(context)
    }

    /**
     * Check if system alert window permission needs to be requested
     */
    fun needsSystemAlertWindowPermission(context: Context): Boolean {
        return !hasSystemAlertWindowPermission(context)
    }

    /**
     * Check if all special permissions are granted
     */
    fun hasAllSpecialPermissions(context: Context): Boolean {
        return hasNotificationListenerServicePermission(context) &&
               isBatteryOptimizationDisabled(context)
    }

    /**
     * Get missing special permissions with their intent handlers
     */
    fun getMissingSpecialPermissions(context: Context): Map<String, () -> Intent> {
        val missing = mutableMapOf<String, () -> Intent>()

        if (needsNotificationListenerServicePermission(context)) {
            missing["Notification Listener Service Access"] = { getNotificationListenerServiceSettingsIntent() }
        }

        if (!isBatteryOptimizationDisabled(context)) {
            missing["Battery Optimization Exemption"] = { getBatteryOptimizationSettingsIntent(context) }
        }

        return missing
    }

    /**
     * Check if app can proceed with core functionality
     */
    fun canProceedWithCoreFunctionality(context: Context): Boolean {
        return hasNotificationListenerServicePermission(context) &&
               hasAllCriticalPermissions(context)
    }
}