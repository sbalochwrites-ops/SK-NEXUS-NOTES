package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.BackupSerializer
import com.example.data.local.NoteEntity
import com.example.ui.theme.DarkOnPrimary
import com.example.ui.theme.DarkOnPrimaryContainer
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.DarkPrimaryContainer
import com.example.ui.util.NotificationHelper

val REMINDER_TIME_OPTIONS = listOf("08:00 AM", "09:00 AM", "01:00 PM", "06:00 PM", "09:00 PM")

@Composable
fun SettingsScreen(
    allNotes: List<NoteEntity>,
    userEmail: String? = null,
    onSignOut: () -> Unit = {},
    cloudSyncStatus: String? = null,
    onSyncNow: () -> Unit = {},
    backupStatus: String? = null,
    onUpdateBackupStatus: (String?) -> Unit = {},
    onImportBackupContent: (String, (Int, String) -> Unit) -> Unit = { _, _ -> },
    // Theme options
    themeMode: String = "system", // "system", "light", "dark"
    onThemeModeChange: (String) -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    isDynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    // Biometric & Lock options
    isBiometricLockEnabled: Boolean = false,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    lockTimeoutMs: Long = 15_000L,
    onUpdateLockTimeout: (Long) -> Unit = {},
    savedPin: String = "1234",
    onUpdatePin: (String) -> Unit = {},
    onLockAppNow: () -> Unit = {},
    // Notification options
    notificationsEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    dailyReminderEnabled: Boolean = true,
    onToggleDailyReminder: (Boolean) -> Unit = {},
    reminderTime: String = "09:00 AM",
    onUpdateReminderTime: (String) -> Unit = {},
    pinnedNotesAlertEnabled: Boolean = true,
    onTogglePinnedNotesAlert: (Boolean) -> Unit = {},
    syncAlertsEnabled: Boolean = true,
    onToggleSyncAlerts: (Boolean) -> Unit = {},
    soundVibrateEnabled: Boolean = true,
    onToggleSoundVibrate: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var newPinDraft by remember(savedPin) { mutableStateOf(savedPin) }
    var showProjectTree by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onToggleNotifications(isGranted)
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled for SK Nexus Notes", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission was denied in system settings", Toast.LENGTH_LONG).show()
        }
    }

    // 1. JSON Backup Document Creator Launcher
    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonContent = BackupSerializer.exportToJson(allNotes)
                context.contentResolver.openOutputStream(uri)?.use { outStream ->
                    outStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                }
                val msg = context.getString(R.string.backup_success)
                onUpdateBackupStatus(msg)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                val err = "${context.getString(R.string.backup_error)}: ${e.localizedMessage}"
                onUpdateBackupStatus(err)
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    // 2. Plain Text (.txt) Backup Document Creator Launcher
    val txtExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            try {
                val txtContent = BackupSerializer.exportToText(allNotes)
                context.contentResolver.openOutputStream(uri)?.use { outStream ->
                    outStream.write(txtContent.toByteArray(Charsets.UTF_8))
                }
                val msg = context.getString(R.string.backup_txt_success)
                onUpdateBackupStatus(msg)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                val err = "${context.getString(R.string.backup_error)}: ${e.localizedMessage}"
                onUpdateBackupStatus(err)
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    // 3. Import / Restore Notes (JSON or TXT) Document Picker Launcher
    val importNotesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val rawContent = context.contentResolver.openInputStream(uri)?.use { inStream ->
                    inStream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                onImportBackupContent(rawContent) { _, statusMessage ->
                    Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val err = "Failed to read backup file: ${e.localizedMessage}"
                onUpdateBackupStatus(err)
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings & Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize SK Nexus Notes, theme, security & notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ==========================================
        // 1. THEME SELECTION & APPEARANCE CARD
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(id = R.string.theme_appearance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "System auto-preference or manual Dark/Light mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Theme Selection",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3-Way Segmented Mode Selector: System / Light / Dark
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // System Mode Chip
                        FilterChip(
                            selected = themeMode == "system",
                            onClick = { onThemeModeChange("system") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.BrightnessAuto,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(stringResource(id = R.string.theme_system), fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkPrimaryContainer,
                                selectedLabelColor = DarkOnPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = themeMode == "system",
                                borderColor = DarkOutline,
                                selectedBorderColor = DarkPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_system_chip")
                        )

                        // Light Mode Chip
                        FilterChip(
                            selected = themeMode == "light",
                            onClick = { onThemeModeChange("light") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(stringResource(id = R.string.theme_light), fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkPrimaryContainer,
                                selectedLabelColor = DarkOnPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = themeMode == "light",
                                borderColor = DarkOutline,
                                selectedBorderColor = DarkPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_light_chip")
                        )

                        // Dark Mode Chip
                        FilterChip(
                            selected = themeMode == "dark",
                            onClick = { onThemeModeChange("dark") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(stringResource(id = R.string.theme_dark), fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkPrimaryContainer,
                                selectedLabelColor = DarkOnPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = themeMode == "dark",
                                borderColor = DarkOutline,
                                selectedBorderColor = DarkPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_dark_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explanatory badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, DarkOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when (themeMode) {
                                "system" -> stringResource(id = R.string.theme_system_desc)
                                "light" -> stringResource(id = R.string.theme_light_desc)
                                else -> stringResource(id = R.string.theme_dark_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = DarkOutline)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Quick Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (isDarkTheme) "Dark Theme Active" else "Light Theme Active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (themeMode == "system") "Currently following system mode" else "Manually configured override",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { enabled ->
                                onToggleDarkTheme(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkOnPrimary,
                                checkedTrackColor = DarkPrimary
                            ),
                            modifier = Modifier.testTag("theme_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Color Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Material 3 Dynamic Wallpaper Color",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Harmonizes palette with Android 12+ wallpaper tones",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDynamicColor,
                            onCheckedChange = onToggleDynamicColor,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkOnPrimary,
                                checkedTrackColor = DarkPrimary
                            ),
                            modifier = Modifier.testTag("dynamic_color_switch")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. NOTIFICATION PREFERENCES CARD
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_settings_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(id = R.string.notification_preferences),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Control reminders, pinned note alerts, and sounds",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Master Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.enable_notifications),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Receive timely reminders and status alerts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { isEnabled ->
                                if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onToggleNotifications(isEnabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkOnPrimary,
                                checkedTrackColor = DarkPrimary
                            ),
                            modifier = Modifier.testTag("master_notification_switch")
                        )
                    }

                    AnimatedVisibility(visible = notificationsEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = DarkOutline)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Daily Note Review Reminder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.daily_reminders),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Daily prompt to review active notes & ideas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = dailyReminderEnabled,
                                    onCheckedChange = onToggleDailyReminder,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkOnPrimary,
                                        checkedTrackColor = DarkPrimary
                                    ),
                                    modifier = Modifier.testTag("daily_reminder_switch")
                                )
                            }

                            if (dailyReminderEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Scheduled Review Time: $reminderTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    REMINDER_TIME_OPTIONS.forEach { timeSlot ->
                                        FilterChip(
                                            selected = reminderTime == timeSlot,
                                            onClick = { onUpdateReminderTime(timeSlot) },
                                            label = { Text(timeSlot, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DarkPrimaryContainer,
                                                selectedLabelColor = DarkOnPrimaryContainer,
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("reminder_time_$timeSlot")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // High-Priority Pinned Alerts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.pinned_alerts),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Priority notifications for notes pinned at the top",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = pinnedNotesAlertEnabled,
                                    onCheckedChange = onTogglePinnedNotesAlert,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkOnPrimary,
                                        checkedTrackColor = DarkPrimary
                                    ),
                                    modifier = Modifier.testTag("pinned_alerts_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sync & Backup Status Alerts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.sync_alerts),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Notify when cloud sync or backup completes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = syncAlertsEnabled,
                                    onCheckedChange = onToggleSyncAlerts,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkOnPrimary,
                                        checkedTrackColor = DarkPrimary
                                    ),
                                    modifier = Modifier.testTag("sync_alerts_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sound & Vibration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.sound_vibrate),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Play notification tone and vibrate",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = soundVibrateEnabled,
                                    onCheckedChange = onToggleSoundVibrate,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkOnPrimary,
                                        checkedTrackColor = DarkPrimary
                                    ),
                                    modifier = Modifier.testTag("sound_vibrate_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Send Test Notification Button
                            OutlinedButton(
                                onClick = {
                                    val sent = NotificationHelper.sendTestNotification(
                                        context = context,
                                        title = "SK Nexus Notes Reminder",
                                        message = "You have ${allNotes.count { it.isPinned }} pinned notes in your vault. Keep up the momentum!"
                                    )
                                    if (sent) {
                                        Toast.makeText(context, "Test notification dispatched to status bar", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            Toast.makeText(context, "Unable to post notification. Check device settings.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DarkPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_test_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = DarkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send Test Reminder Notification",
                                    color = DarkPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. PRIVACY POLICY & DATA SECURITY CARD
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_policy_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(id = R.string.privacy_policy),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zero-trust privacy architecture & user data rights",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(id = R.string.privacy_policy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // In-App Privacy Reader Button
                        Button(
                            onClick = { showPrivacyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPrimaryContainer,
                                contentColor = DarkOnPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("view_privacy_policy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Read Policy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Web Link to Privacy Policy
                        OutlinedButton(
                            onClick = {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://policies.google.com/privacy")
                                )
                                context.startActivity(browserIntent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_privacy_policy_web_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Web Link",
                                color = DarkPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. BIOMETRIC & PIN VAULT LOCK CARD
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    text = "Biometric & PIN Auto-Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lock app after background timeout with Biometrics or PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isBiometricLockEnabled,
                            onCheckedChange = onToggleBiometricLock,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkOnPrimary,
                                checkedTrackColor = DarkPrimary
                            ),
                            modifier = Modifier.testTag("biometric_lock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Background Auto-Lock Timeout",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AUTO_LOCK_TIMEOUT_OPTIONS.forEach { (timeoutMs, label) ->
                            val isSelected = lockTimeoutMs == timeoutMs
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateLockTimeout(timeoutMs) },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkPrimaryContainer,
                                    selectedLabelColor = DarkOnPrimaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("timeout_chip_$timeoutMs")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPinDraft,
                            onValueChange = {
                                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                    newPinDraft = it
                                }
                            },
                            label = { Text("Security PIN (4-6 digits)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = DarkPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkPrimary,
                                unfocusedBorderColor = DarkOutline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_pin_input")
                        )

                        Button(
                            onClick = {
                                if (newPinDraft.length >= 4) {
                                    onUpdatePin(newPinDraft)
                                    Toast.makeText(context, "Security PIN updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPrimaryContainer,
                                contentColor = DarkOnPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_pin_button")
                        ) {
                            Text("Save PIN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (isBiometricLockEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onLockAppNow,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_vault_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lock Vault Now",
                                color = DarkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. LOCAL BACKUP (JSON/TXT) & RESTORE
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.backup_all),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.backup_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val fileName = "nexus_notes_backup_${System.currentTimeMillis()}.json"
                                jsonExportLauncher.launch(fileName)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPrimary,
                                contentColor = DarkOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export JSON",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val fileName = "nexus_notes_export_${System.currentTimeMillis()}.txt"
                                txtExportLauncher.launch(fileName)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DarkPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_txt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export TXT",
                                color = DarkPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            importNotesLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_notes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import & Restore Notes (.JSON or .TXT)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    if (!backupStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = backupStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkPrimary,
                            modifier = Modifier.testTag("backup_status_text")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 6. CLOUD ACCOUNT & FIRESTORE SYNC
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Firebase Firestore Cloud",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = userEmail ?: "Google Account Connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSyncNow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimary,
                            contentColor = DarkOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_firestore_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sync Now with Cloud Firestore",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!cloudSyncStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cloudSyncStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = DarkPrimary,
                            modifier = Modifier.testTag("cloud_sync_status_text")
                        )
                    }
                }
            }
        }

        // ==========================================
        // 7. ROOM DATABASE STATUS
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Local Room Persistence (v3)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Notes + Recent Search History SQLite Tables",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, DarkOutline),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Total Notes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${allNotes.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, DarkOutline),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Pinned Notes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${allNotes.count { it.isPinned }}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. ARCHITECTURE TREE VIEWER
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Project Architecture Tree",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = { showProjectTree = !showProjectTree },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("toggle_project_tree_button")
                        ) {
                            Text(
                                text = if (showProjectTree) "Hide Tree" else "View Tree",
                                fontSize = 11.sp,
                                color = DarkPrimary
                            )
                        }
                    }

                    if (showProjectTree) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, DarkOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = """
                                    com.example/
                                    ├── MainActivity.kt (Theme Mode, Auth & Biometric Lock)
                                    ├── data/
                                    │   ├── local/
                                    │   │   ├── AppDatabase.kt (Room DB v3)
                                    │   │   ├── NoteEntity.kt (isPinned, tags, dates)
                                    │   │   ├── NoteDao.kt (Notes queries & search)
                                    │   │   ├── RecentSearchEntity.kt & Dao.kt
                                    │   │   └── BackupSerializer.kt (JSON & TXT)
                                    │   ├── firestore/ (FirestoreNote & Repo)
                                    │   └── repository/NoteRepository.kt
                                    └── ui/
                                        ├── NoteViewModel.kt (StateFlow, Gemini, Sync)
                                        ├── screens/
                                        │   ├── AuthScreen.kt (Google Sign-In)
                                        │   ├── NotesScreen.kt (Notes & Tabs)
                                        │   └── SettingsScreen.kt (Theme, Notifs, Privacy)
                                        ├── components/
                                        │   ├── AddEditNoteDialog.kt (Speech-to-Text)
                                        │   ├── SearchBarComponent.kt (Recent Queries)
                                        │   ├── NoteCard.kt (Pinned Priority)
                                        │   └── NexusBottomNavBar.kt
                                        └── util/NotificationHelper.kt
                                """.trimIndent(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 9. APP INFO & BUILD BADGE
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SK Nexus Notes v1.2.0",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "SK Nexus Notes features Jetpack Compose, System/Light/Dark theme modes, Notification preferences, Zero-Trust Privacy, Speech-to-Text dictation, Room Database v3, and Firebase Firestore cloud backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // In-App Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = DarkPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "SK Nexus Notes Privacy Policy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Zero-Trust Local Storage",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "All notes, labels, and recent searches are persisted locally on your device in a secure Room SQLite database. Your private notes never leave your phone unless you explicitly sign in and initiate cloud synchronization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "2. Firebase Firestore Cloud Protection",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Cloud backup utilizes strict zero-trust Firestore security rules. Every note document is mapped directly to users/{userId}/notes and can only be read or modified by your authenticated Google account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "3. Speech-to-Text & Microphone",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Audio used for voice dictation is processed strictly on-device through Android SpeechRecognizer. Audio data is never saved, uploaded, or transmitted to any third party.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "4. Biometric & Device Lock",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Your fingerprint, facial biometric data, and device PIN never leave your phone's hardware security module (Keyguard).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "5. Data Export & Deletion Rights",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "You maintain complete ownership of your data. You can export complete backups in standard JSON or plain text formats, or wipe all notes at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPrimary,
                        contentColor = DarkOnPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://policies.google.com/privacy")
                        )
                        context.startActivity(browserIntent)
                    }
                ) {
                    Text("Open Online", color = DarkPrimary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
