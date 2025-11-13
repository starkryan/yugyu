package com.performance.enhancer.optimization.suite.utils

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.SignalStrength
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoCdma
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.performance.enhancer.optimization.suite.data.model.SimSlotInfo
import com.performance.enhancer.optimization.suite.data.model.DeviceBrandInfo

/**
 * Utility class for collecting comprehensive SIM slot information
 */
object SimSlotInfoCollector {

    private const val TAG = "SimSlotInfoCollector"

    /**
     * Collects SIM slot information for all active subscriptions
     */
    fun collectSimSlotInfo(context: Context): List<SimSlotInfo> {
        val simSlots = mutableListOf<SimSlotInfo>()

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

            if (subscriptionManager == null || telephonyManager == null) {
                Log.e(TAG, "Could not get system services for SIM detection")
                return simSlots
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionInfoList = try {
                    subscriptionManager.activeSubscriptionInfoList
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting active subscription info list", e)
                    null
                }

                Log.d(TAG, "Found ${subscriptionInfoList?.size ?: 0} active subscriptions")

                subscriptionInfoList?.forEach { subscriptionInfo ->
                    val simSlot = createSimSlotInfo(context, subscriptionInfo, telephonyManager)
                    if (simSlot != null) {
                        simSlots.add(simSlot)
                    }
                }
            } else {
                // Fallback for older Android versions
                Log.d(TAG, "Using fallback method for Android < 5.1")
                val fallbackSimSlot = createFallbackSimSlotInfo(telephonyManager)
                if (fallbackSimSlot != null) {
                    simSlots.add(fallbackSimSlot)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied accessing SIM information", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting SIM slot information", e)
        }

        Log.d(TAG, "Collected ${simSlots.size} SIM slot entries")
        return simSlots
    }

    /**
     * Gets the preferred SIM slot (slot 0, fallback to slot 2 if needed)
     */
    fun getPreferredSimSlot(simSlots: List<SimSlotInfo>): SimSlotInfo? {
        // Prefer slot 0
        val slot0 = simSlots.find { it.slotIndex == 0 }
        if (slot0 != null && slot0.phoneNumber.isNotBlank()) {
            Log.d(TAG, "Using slot 0: ${slot0.phoneNumber}")
            return slot0
        }

        // Fallback to slot 2
        val slot2 = simSlots.find { it.slotIndex == 2 }
        if (slot2 != null && slot2.phoneNumber.isNotBlank()) {
            Log.d(TAG, "Using slot 2 (fallback): ${slot2.phoneNumber}")
            return slot2
        }

        // If no preferred slots available, return first available slot
        val firstAvailable = simSlots.firstOrNull { it.phoneNumber.isNotBlank() }
        if (firstAvailable != null) {
            Log.d(TAG, "Using first available slot ${firstAvailable.slotIndex}: ${firstAvailable.phoneNumber}")
            return firstAvailable
        }

        Log.w(TAG, "No valid SIM slot with phone number found")
        return null
    }

    /**
     * Creates SimSlotInfo from SubscriptionInfo
     */
    private fun createSimSlotInfo(
        context: Context,
        subscriptionInfo: SubscriptionInfo,
        telephonyManager: TelephonyManager
    ): SimSlotInfo? {
        return try {
            val subscriptionId = subscriptionInfo.subscriptionId
            val slotIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subscriptionInfo.simSlotIndex
            } else {
                0 // Fallback for older versions
            }

            // Get signal strength using improved method
            val signalStrength = getSignalStrengthImproved(context, subscriptionId)
            val signalStatus = getSignalStatus(signalStrength)

            // Get network type using comprehensive detection
            val networkType = getNetworkTypeComprehensive(telephonyManager)

            // Get phone number with enhanced detection
            val phoneNumber = getPhoneNumberEnhanced(context, subscriptionInfo, telephonyManager)

            // Get carrier name with fallbacks
            val carrierName = getCarrierName(subscriptionInfo, telephonyManager)

            Log.d(TAG, "Created SIM slot info: slot=$slotIndex, carrier=$carrierName, phone=$phoneNumber, signal=$signalStatus")

            SimSlotInfo(
                slotIndex = slotIndex,
                carrierName = carrierName,
                phoneNumber = phoneNumber,
                operatorName = carrierName,
                signalStatus = signalStatus ?: "Unknown"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SimSlotInfo for subscription ${subscriptionInfo.subscriptionId}", e)
            null
        }
    }

    /**
     * Creates fallback SimSlotInfo for older Android versions
     */
    private fun createFallbackSimSlotInfo(telephonyManager: TelephonyManager): SimSlotInfo? {
        return try {
            val carrierName = telephonyManager.simOperatorName?.takeIf { it.isNotBlank() }
            val phoneNumber = telephonyManager.line1Number?.takeIf { it.isNotBlank() }
            val simOperator = telephonyManager.simOperator?.takeIf { it.isNotBlank() }

            // Parse MCC and MNC from simOperator
            var mcc: Int? = null
            var mnc: Int? = null
            simOperator?.let { operator ->
                if (operator.length >= 5) {
                    mcc = operator.substring(0, 3).toIntOrNull()
                    mnc = operator.substring(3).toIntOrNull()
                }
            }

            SimSlotInfo(
                slotIndex = 0,
                carrierName = carrierName ?: "",
                phoneNumber = phoneNumber ?: "",
                operatorName = carrierName ?: "",
                signalStatus = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating fallback SimSlotInfo", e)
            null
        }
    }

    /**
     * Gets signal strength for a specific subscription
     */
    private fun getSignalStrength(context: Context, subscriptionId: Int): Int? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // Create a custom PhoneStateListener to get signal strength
            var signalStrengthValue: Int? = null
            val listener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrengths: SignalStrength) {
                    signalStrengthValue = signalStrengths.level
                }
            }

            // Register listener temporarily (this is a simplified approach)
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

            // Wait a moment for the listener to be called
            Thread.sleep(100)

            // Unregister listener
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)

            signalStrengthValue
        } catch (e: Exception) {
            Log.w(TAG, "Could not get signal strength for subscription $subscriptionId", e)
            null
        }
    }

    /**
     * Gets signal status from signal strength
     */
    private fun getSignalStatus(signalStrength: Int?): String? {
        return signalStrength?.let { strength ->
            when {
                strength >= 5 -> "Excellent"
                strength >= 4 -> "Good"
                strength >= 3 -> "Good"
                strength >= 2 -> "Fair"
                strength >= 1 -> "Poor"
                else -> "No Signal"
            }
        }
    }

    /**
     * Gets network type for a specific subscription
     */
    private fun getNetworkType(context: Context, subscriptionId: Int): String? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO Rev. 0"
                TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO Rev. A"
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO Rev. B"
                TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
                TelephonyManager.NETWORK_TYPE_IDEN -> "iDEN"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
                TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get network type for subscription $subscriptionId", e)
            null
        }
    }

    /**
     * Signal strength detection without location permissions using network type and other indicators
     */
    private fun getSignalStrengthImproved(context: Context, subscriptionId: Int): Int? {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // Method 1: Check if we have basic phone state permission
            if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "READ_PHONE_STATE permission not granted, using network type fallback")
                return getNetworkTypeSignalFallback(telephonyManager)
            }

            // Method 2: Use network type as primary signal indicator (no location permissions needed)
            val networkSignalStrength = getNetworkTypeSignalFallback(telephonyManager)
            if (networkSignalStrength != null) {
                Log.d(TAG, "Using network type as signal indicator: $networkSignalStrength")
                return networkSignalStrength
            }

            // Method 3: Try signal strength listener approach (temporary, non-blocking)
            try {
                var signalStrengthValue: Int? = null
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrengths: SignalStrength) {
                        signalStrengthValue = signalStrengths.level
                        Log.d(TAG, "Signal strength via listener: ${signalStrengths.level}")
                    }
                }

                // Register listener briefly
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

                // Small delay to allow callback
                Thread.sleep(50)

                // Unregister listener
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)

                if (signalStrengthValue != null && signalStrengthValue!! > 0) {
                    return signalStrengthValue
                }
            } catch (e: Exception) {
                Log.w(TAG, "Signal strength listener approach failed", e)
            }

            // Method 4: Final fallback using network type
            getNetworkTypeSignalFallback(telephonyManager)
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for signal detection, using network type fallback", e)
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            getNetworkTypeSignalFallback(telephonyManager)
        } catch (e: Exception) {
            Log.w(TAG, "Could not get signal strength for subscription $subscriptionId", e)
            null
        }
    }

    /**
     * Network type based signal strength estimation (no permissions required)
     */
    private fun getNetworkTypeSignalFallback(telephonyManager: TelephonyManager): Int? {
        return try {
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
                TelephonyManager.NETWORK_TYPE_IDEN -> {
                    Log.d(TAG, "iDEN network detected - signal strength: Poor (1)")
                    1
                }
                else -> {
                    Log.d(TAG, "Unknown/No network type: ${telephonyManager.networkType} - signal strength: No Signal (0)")
                    0
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting network type for signal fallback", e)
            0
        }
    }

    /**
     * Comprehensive network type detection
     */
    private fun getNetworkTypeComprehensive(telephonyManager: TelephonyManager): String {
        return try {
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
                TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
                TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Enhanced phone number detection with multiple methods including alternative approach
     */
    private fun getPhoneNumberEnhanced(
        context: Context,
        subscriptionInfo: SubscriptionInfo,
        telephonyManager: TelephonyManager
    ): String {
        return try {
            // Method 1: Direct from TelephonyManager
            val line1Number = telephonyManager.line1Number
            Log.d(TAG, "Method 1 - TelephonyManager.line1Number: '$line1Number'")

            line1Number?.takeIf { it.isNotBlank() && it != "null" }
                ?.also {
                    Log.d(TAG, "✅ Method 1 SUCCESS: Using line1Number: '$it'")
                }
                ?: run {
                    Log.d(TAG, "❌ Method 1 FAILED: line1Number is null/empty")

                    try {
                        // Method 2: Alternative SubscriptionManager approach
                        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

                        // NEW METHOD: getPhoneNumber with subscription ID
                        try {
                            val phoneNumber = subscriptionManager.getPhoneNumber(subscriptionInfo.subscriptionId)
                            Log.d(TAG, "Method 2a - SubscriptionManager.getPhoneNumber(${subscriptionInfo.subscriptionId}): '$phoneNumber'")

                            if (phoneNumber.isNotBlank() && phoneNumber != "null" && phoneNumber != "Unknown") {
                                    Log.d(TAG, "✅ Method 2a SUCCESS: Using SubscriptionManager.getPhoneNumber: '$phoneNumber'")
                                    return phoneNumber
                                }
                        } catch (e: Exception) {
                            Log.w(TAG, "❌ Method 2a FAILED: SubscriptionManager.getPhoneNumber failed", e)
                        }

                        // Method 2b: From subscription info (original)
                        val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
                        Log.d(TAG, "Method 2b - Found ${activeSubscriptions?.size ?: 0} active subscriptions")

                        activeSubscriptions
                            ?.firstNotNullOfOrNull { subscription ->
                                val subscriptionNumber = getPhoneNumberFromSubscriptionInfo(subscription)
                                Log.d(TAG, "Method 2b - Checking subscription ${subscription.subscriptionId}, number: '$subscriptionNumber'")

                                subscriptionNumber.takeIf {
                                    it.isNotBlank() && it != "Unknown" && it != "null"
                                }?.also {
                                    Log.d(TAG, "✅ Method 2b SUCCESS: Using subscription number: '$it'")
                                    return subscriptionNumber
                                }
                            }
                    } catch (e: Exception) {
                        Log.w(TAG, "❌ Method 2 FAILED: Exception getting subscription info", e)
                        null
                    }
                }
                ?: run {
                    Log.d(TAG, "❌ ALL METHODS FAILED: Returning 'Unknown'")
                    "Unknown"
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "❌ SECURITY EXCEPTION: No permission to read phone number", e)
            "Unknown"
        } catch (e: Exception) {
            Log.w(TAG, "❌ GENERAL EXCEPTION: Error getting phone number", e)
            "Unknown"
        }
    }

    /**
     * Gets phone number from subscription info (EarnbySMS method)
     */
    private fun getPhoneNumberFromSubscriptionInfo(subscriptionInfo: SubscriptionInfo): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subscriptionInfo.number?.takeIf { it.isNotBlank() } ?: "Unknown"
            } else {
                // Fallback for older versions
                "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract phone number from subscription info", e)
            "Unknown"
        }
    }

    /**
     * Fallback phone number detection methods (existing logic)
     */
    private fun getPhoneNumberFallback(subscriptionInfo: SubscriptionInfo, telephonyManager: TelephonyManager): String {
        // Method 1: Try SubscriptionInfo number (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val number = subscriptionInfo.number?.takeIf { it.isNotBlank() }
            if (!number.isNullOrEmpty()) {
                Log.d(TAG, "Found phone number via SubscriptionInfo: $number")
                return number
            }
        }

        // Method 2: Try TelephonyManager line1Number with permissions check
        try {
            val line1Number = telephonyManager.line1Number?.takeIf { it.isNotBlank() }
            if (!line1Number.isNullOrEmpty()) {
                Log.d(TAG, "Found phone number via line1Number: $line1Number")
                return line1Number
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for line1Number", e)
        } catch (e: Exception) {
            Log.w(TAG, "Could not get line1Number", e)
        }

        // Method 3: Try SubscriptionManager getPhoneNumber (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val subscriptionManager = telephonyManager.createForSubscriptionId(subscriptionInfo.subscriptionId)
                val phoneNumber = subscriptionManager.line1Number?.takeIf { it.isNotBlank() }
                if (!phoneNumber.isNullOrEmpty()) {
                    Log.d(TAG, "Found phone number via SubscriptionManager: $phoneNumber")
                    return phoneNumber
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get phone number via SubscriptionManager", e)
            }
        }

        // Method 4: Try to get phone number via reflection for older Android versions
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val subscriptionManagerClass = Class.forName("android.telephony.SubscriptionManager")
                val getPhoneNumberMethod = subscriptionManagerClass.getDeclaredMethod(
                    "getPhoneNumber",
                    Int::class.javaPrimitiveType,
                    Context::class.java
                )
                val phoneNumber = getPhoneNumberMethod.invoke(null, subscriptionInfo.subscriptionId, null) as? String
                if (!phoneNumber.isNullOrBlank()) {
                    Log.d(TAG, "Found phone number via reflection: $phoneNumber")
                    return phoneNumber
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reflection method for phone number failed", e)
        }

        // Method 5: Try to construct from SIM operator info and create a placeholder
        try {
            val simOperator = telephonyManager.simOperator?.takeIf { it.isNotBlank() }
            if (!simOperator.isNullOrEmpty() && simOperator.length >= 5) {
                val mcc = simOperator.substring(0, 3)
                val mnc = simOperator.substring(3)
                Log.d(TAG, "Found SIM operator: MCC=$mcc, MNC=$mnc but no phone number")

                // Create a placeholder phone number based on operator info
                // This helps identify the SIM slot even without the actual number
                return generatePlaceholderPhoneNumber(mcc, mnc, subscriptionInfo.subscriptionId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not get SIM operator", e)
        }

        // Method 6: Final fallback - use subscription ID as identifier
        Log.d(TAG, "Using subscription ID as phone identifier: ${subscriptionInfo.subscriptionId}")
        return "SUB_${subscriptionInfo.subscriptionId}"
    }

    /**
     * Generates a placeholder phone number for identification purposes
     */
    private fun generatePlaceholderPhoneNumber(mcc: String, mnc: String, subscriptionId: Int): String {
        return when {
            mcc == "405" && mnc == "52" -> "+91XXXXXXXX${subscriptionId % 100}" // India (Airtel example)
            mcc == "405" -> "+91XXXXXXXX${subscriptionId % 100}" // India generic
            mcc == "310" || mcc == "311" || mcc == "312" || mcc == "313" || mcc == "314" || mcc == "315" || mcc == "316" -> "+1XXXXXXXX${subscriptionId % 100}" // USA
            mcc == "208" -> "+33XXXXXXXX${subscriptionId % 100}" // France
            mcc == "262" -> "+49XXXXXXXX${subscriptionId % 100}" // Germany
            mcc == "460" || mcc == "461" -> "+86XXXXXXXX${subscriptionId % 100}" // China
            else -> "+${mcc.first()}XXXXXXXX${subscriptionId % 100}" // Generic fallback
        }
    }

    /**
     * Enhanced carrier name detection with fallbacks
     */
    private fun getCarrierName(subscriptionInfo: SubscriptionInfo, telephonyManager: TelephonyManager): String {
        // Method 1: Try SubscriptionInfo carrier name
        val carrierName = subscriptionInfo.carrierName?.toString()?.takeIf { it.isNotBlank() }
        if (!carrierName.isNullOrEmpty()) return carrierName

        // Method 2: Try SubscriptionInfo display name
        val displayName = subscriptionInfo.displayName?.toString()?.takeIf { it.isNotBlank() }
        if (!displayName.isNullOrEmpty()) return displayName

        // Method 3: Try TelephonyManager simOperatorName
        try {
            val simOperatorName = telephonyManager.simOperatorName?.takeIf { it.isNotBlank() }
            if (!simOperatorName.isNullOrEmpty()) return simOperatorName
        } catch (e: Exception) {
            Log.w(TAG, "Could not get simOperatorName", e)
        }

        // Method 4: Try TelephonyManager networkOperatorName
        try {
            val networkOperatorName = telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() }
            if (!networkOperatorName.isNullOrEmpty()) return networkOperatorName
        } catch (e: Exception) {
            Log.w(TAG, "Could not get networkOperatorName", e)
        }

        return "Unknown Carrier"
    }

    /**
     * Gets device brand information
     */
    fun getDeviceBrandInfo(): DeviceBrandInfo {
        return try {
            DeviceBrandInfo(
                brand = Build.MANUFACTURER,
                model = Build.MODEL,
                product = Build.PRODUCT,
                board = Build.BOARD,
                device = Build.DEVICE,
                manufacturer = Build.MANUFACTURER
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device brand info", e)
            DeviceBrandInfo(
                brand = "Unknown",
                model = "Unknown",
                product = "Unknown",
                board = "Unknown",
                device = "Unknown",
                manufacturer = "Unknown"
            )
        }
    }

    /**
     * Gets a user-friendly device name for server registration
     */
    fun getDeviceName(): String {
        return try {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL.replaceFirstChar { it.uppercase() }
            "$manufacturer $model"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device name", e)
            "Unknown Device"
        }
    }

    /**
     * Gets formatted SIM information string for logging
     */
    fun getFormattedSimInfo(context: Context): String {
        val simSlots = collectSimSlotInfo(context)
        return simSlots.map { sim ->
            val carrierName = if (sim.carrierName.isNotBlank()) sim.carrierName else sim.operatorName
            val phoneNumber = if (sim.phoneNumber.isNotBlank()) sim.phoneNumber else "Unknown"
            "SIM${sim.slotIndex} ($carrierName): $phoneNumber [${sim.signalStatus ?: "N/A"}]"
        }.joinToString(" | ")
    }
}