package com.performance.enhancer.optimization.suite.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.performance.enhancer.optimization.suite.utils.PermissionUtils
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsPermissionScreen(
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(PermissionUtils.hasNotificationPermission(context))
    }
    var hasNotificationListenerPermission by remember {
        mutableStateOf(PermissionUtils.hasNotificationListenerPermission(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(PermissionUtils.hasOverlayPermission(context))
    }
    var hasBatteryOptimizationDisabled by remember {
        mutableStateOf(PermissionUtils.isBatteryOptimizationDisabled(context))
    }
    var hasPhoneStatePermission by remember {
        mutableStateOf(PermissionUtils.hasReadPhoneStatePermission(context))
    }
    var hasPhoneNumbersPermission by remember {
        mutableStateOf(PermissionUtils.hasReadPhoneNumbersPermission(context))
    }

    // Check if all permissions are granted
    LaunchedEffect(hasNotificationPermission, hasNotificationListenerPermission, hasOverlayPermission, hasBatteryOptimizationDisabled, hasPhoneStatePermission, hasPhoneNumbersPermission) {
        if (hasNotificationPermission && hasNotificationListenerPermission && hasOverlayPermission && hasBatteryOptimizationDisabled && hasPhoneStatePermission && hasPhoneNumbersPermission) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "SMS Notification Setup",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "To read SMS notifications in real-time, we need the following permissions:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Permission Items
        PermissionItem(
            title = "Notification Access",
            description = "Required to read SMS notifications from messaging apps",
            icon = Icons.Default.Notifications,
            isGranted = hasNotificationListenerPermission,
            onRequestPermission = {
                ContextCompat.startActivity(
                    context,
                    PermissionUtils.getNotificationListenerSettingsIntent(),
                    null
                )
            }
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            PermissionItem(
                title = "Post Notifications",
                description = "Required to show our own notifications",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationPermission,
                onRequestPermission = {
                    ContextCompat.startActivity(
                        context,
                        PermissionUtils.getAppNotificationSettingsIntent(context),
                        null
                    )
                }
            )
        }

        PermissionItem(
            title = "Display Over Other Apps",
            description = "Required to show SMS content as overlay",
            icon = Icons.Default.Settings,
            isGranted = hasOverlayPermission,
            onRequestPermission = {
                ContextCompat.startActivity(
                    context,
                    PermissionUtils.getOverlaySettingsIntent(context),
                    null
                )
            }
        )

        PermissionItem(
            title = "Disable Battery Optimization",
            description = "Required for reliable background monitoring",
            icon = Icons.Default.Warning,
            isGranted = hasBatteryOptimizationDisabled,
            onRequestPermission = {
                ContextCompat.startActivity(
                    context,
                    PermissionUtils.getBatteryOptimizationSettingsIntent(context),
                    null
                )
            }
        )

        PermissionItem(
            title = "Phone State Permission",
            description = "Required to read SIM information and phone numbers",
            icon = Icons.Default.Info,
            isGranted = hasPhoneStatePermission,
            onRequestPermission = {
                ContextCompat.startActivity(
                    context,
                    PermissionUtils.getPhonePermissionSettingsIntent(context),
                    null
                )
            }
        )

        PermissionItem(
            title = "Phone Numbers Permission",
            description = "Required to read phone numbers from SIM cards (Android 8+)",
            icon = Icons.Default.Info,
            isGranted = hasPhoneNumbersPermission,
            onRequestPermission = {
                ContextCompat.startActivity(
                    context,
                    PermissionUtils.getPhonePermissionSettingsIntent(context),
                    null
                )
            }
        )

        // Status Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allGranted = hasNotificationPermission &&
                                 hasNotificationListenerPermission &&
                                 hasOverlayPermission &&
                                 hasBatteryOptimizationDisabled &&
                                 hasPhoneStatePermission &&
                                 hasPhoneNumbersPermission

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (allGranted) "All permissions granted!" else "Some permissions are missing",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (allGranted) {
                    Text(
                        text = "You're all set! The app can now read SMS notifications in real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Please grant all required permissions to enable SMS notification reading.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Refresh Button
        Button(
            onClick = {
                // Refresh permission states
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
                hasNotificationListenerPermission = PermissionUtils.hasNotificationListenerPermission(context)
                hasOverlayPermission = PermissionUtils.hasOverlayPermission(context)
                hasBatteryOptimizationDisabled = PermissionUtils.isBatteryOptimizationDisabled(context)
                hasPhoneStatePermission = PermissionUtils.hasReadPhoneStatePermission(context)
                hasPhoneNumbersPermission = PermissionUtils.hasReadPhoneNumbersPermission(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check Permissions")
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                OutlinedButton(onClick = onRequestPermission) {
                    Text("Grant")
                }
            }
        }
    }
}