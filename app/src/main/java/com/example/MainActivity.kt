package com.example

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.configureAppCheck
import com.example.ui.NoteViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.theme.DarkOnPrimary
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkPrimary
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.util.NotificationHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CRITICAL: Configure Firebase App Check before any Firebase or ViewModel access
        configureAppCheck(this, intent)
        NotificationHelper.createNotificationChannel(this)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("nexus_ui_prefs", Context.MODE_PRIVATE)

        setContent {
            var themeMode by remember {
                mutableStateOf(prefs.getString("theme_mode", "system") ?: "system")
            }
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemInDark
            }

            var isDynamicColor by remember {
                mutableStateOf(prefs.getBoolean("dynamic_color", false))
            }
            var isBiometricLockEnabled by remember {
                mutableStateOf(prefs.getBoolean("biometric_lock", false))
            }
            var lockTimeoutMs by remember {
                mutableLongStateOf(prefs.getLong("lock_timeout_ms", 15_000L))
            }
            var savedPin by remember {
                mutableStateOf(prefs.getString("app_lock_pin", "1234") ?: "1234")
            }
            var isAppUnlocked by remember {
                mutableStateOf(!isBiometricLockEnabled)
            }
            var lastBackgroundTime by remember {
                mutableLongStateOf(0L)
            }

            // Notification preferences
            var notificationsEnabled by remember {
                mutableStateOf(prefs.getBoolean("notif_enabled", true))
            }
            var dailyReminderEnabled by remember {
                mutableStateOf(prefs.getBoolean("notif_daily_reminder", true))
            }
            var reminderTime by remember {
                mutableStateOf(prefs.getString("notif_reminder_time", "09:00 AM") ?: "09:00 AM")
            }
            var pinnedNotesAlertEnabled by remember {
                mutableStateOf(prefs.getBoolean("notif_pinned_alerts", true))
            }
            var syncAlertsEnabled by remember {
                mutableStateOf(prefs.getBoolean("notif_sync_alerts", true))
            }
            var soundVibrateEnabled by remember {
                mutableStateOf(prefs.getBoolean("notif_sound_vibrate", true))
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, isBiometricLockEnabled, lockTimeoutMs) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            if (isBiometricLockEnabled) {
                                lastBackgroundTime = System.currentTimeMillis()
                            }
                        }
                        Lifecycle.Event.ON_START -> {
                            if (isBiometricLockEnabled && lastBackgroundTime > 0L) {
                                val elapsed = System.currentTimeMillis() - lastBackgroundTime
                                if (elapsed >= lockTimeoutMs) {
                                    isAppUnlocked = false
                                }
                                lastBackgroundTime = 0L
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = isDynamicColor
            ) {
                AppRoot(
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        prefs.edit().putString("theme_mode", newMode).apply()
                    },
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { enabled ->
                        val newMode = if (enabled) "dark" else "light"
                        themeMode = newMode
                        prefs.edit().putString("theme_mode", newMode).putBoolean("dark_theme", enabled).apply()
                    },
                    isDynamicColor = isDynamicColor,
                    onToggleDynamicColor = { enabled ->
                        isDynamicColor = enabled
                        prefs.edit().putBoolean("dynamic_color", enabled).apply()
                    },
                    isBiometricLockEnabled = isBiometricLockEnabled,
                    onToggleBiometricLock = { enabled ->
                        isBiometricLockEnabled = enabled
                        if (!enabled) isAppUnlocked = true
                        prefs.edit().putBoolean("biometric_lock", enabled).apply()
                    },
                    lockTimeoutMs = lockTimeoutMs,
                    onUpdateLockTimeout = { newTimeoutMs ->
                        lockTimeoutMs = newTimeoutMs
                        prefs.edit().putLong("lock_timeout_ms", newTimeoutMs).apply()
                    },
                    savedPin = savedPin,
                    onUpdatePin = { newPin ->
                        savedPin = newPin
                        prefs.edit().putString("app_lock_pin", newPin).apply()
                    },
                    isAppUnlocked = isAppUnlocked,
                    onUnlockSuccess = { isAppUnlocked = true },
                    onLockAppNow = { isAppUnlocked = false },
                    notificationsEnabled = notificationsEnabled,
                    onToggleNotifications = { enabled ->
                        notificationsEnabled = enabled
                        prefs.edit().putBoolean("notif_enabled", enabled).apply()
                    },
                    dailyReminderEnabled = dailyReminderEnabled,
                    onToggleDailyReminder = { enabled ->
                        dailyReminderEnabled = enabled
                        prefs.edit().putBoolean("notif_daily_reminder", enabled).apply()
                    },
                    reminderTime = reminderTime,
                    onUpdateReminderTime = { newTime ->
                        reminderTime = newTime
                        prefs.edit().putString("notif_reminder_time", newTime).apply()
                    },
                    pinnedNotesAlertEnabled = pinnedNotesAlertEnabled,
                    onTogglePinnedNotesAlert = { enabled ->
                        pinnedNotesAlertEnabled = enabled
                        prefs.edit().putBoolean("notif_pinned_alerts", enabled).apply()
                    },
                    syncAlertsEnabled = syncAlertsEnabled,
                    onToggleSyncAlerts = { enabled ->
                        syncAlertsEnabled = enabled
                        prefs.edit().putBoolean("notif_sync_alerts", enabled).apply()
                    },
                    soundVibrateEnabled = soundVibrateEnabled,
                    onToggleSoundVibrate = { enabled ->
                        soundVibrateEnabled = enabled
                        prefs.edit().putBoolean("notif_sound_vibrate", enabled).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun AppRoot(
    themeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    isDynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    isBiometricLockEnabled: Boolean = false,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    lockTimeoutMs: Long = 15_000L,
    onUpdateLockTimeout: (Long) -> Unit = {},
    savedPin: String = "1234",
    onUpdatePin: (String) -> Unit = {},
    isAppUnlocked: Boolean = true,
    onUnlockSuccess: () -> Unit = {},
    onLockAppNow: () -> Unit = {},
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
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else if (isBiometricLockEnabled && !isAppUnlocked) {
        BiometricLockGateScreen(
            expectedPin = savedPin,
            onUnlockSuccess = onUnlockSuccess
        )
    } else {
        val noteViewModel: NoteViewModel = viewModel()
        NotesScreen(
            viewModel = noteViewModel,
            userEmail = currentUser?.email,
            onSignOut = {
                Firebase.auth.signOut()
            },
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            isDynamicColor = isDynamicColor,
            onToggleDynamicColor = onToggleDynamicColor,
            isBiometricLockEnabled = isBiometricLockEnabled,
            onToggleBiometricLock = onToggleBiometricLock,
            lockTimeoutMs = lockTimeoutMs,
            onUpdateLockTimeout = onUpdateLockTimeout,
            savedPin = savedPin,
            onUpdatePin = onUpdatePin,
            onLockAppNow = onLockAppNow,
            notificationsEnabled = notificationsEnabled,
            onToggleNotifications = onToggleNotifications,
            dailyReminderEnabled = dailyReminderEnabled,
            onToggleDailyReminder = onToggleDailyReminder,
            reminderTime = reminderTime,
            onUpdateReminderTime = onUpdateReminderTime,
            pinnedNotesAlertEnabled = pinnedNotesAlertEnabled,
            onTogglePinnedNotesAlert = onTogglePinnedNotesAlert,
            syncAlertsEnabled = syncAlertsEnabled,
            onToggleSyncAlerts = onToggleSyncAlerts,
            soundVibrateEnabled = soundVibrateEnabled,
            onToggleSoundVibrate = onToggleSoundVibrate
        )
    }
}

@Composable
fun BiometricLockGateScreen(
    expectedPin: String = "1234",
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlockSuccess()
        }
    }

    val verifyPin = {
        if (pinInput.trim() == expectedPin) {
            pinError = null
            onUnlockSuccess()
        } else {
            pinError = "Incorrect PIN. Please try again."
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("biometric_lock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, DarkOutline)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                color = DarkPrimary.copy(alpha = 0.16f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Sensor",
                            tint = DarkPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SK Nexus Vault Locked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Session timed out in background or locked manually. Authenticate with your PIN or biometrics to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // PIN Entry Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                    pinInput = it
                                    pinError = null
                                }
                            },
                            label = { Text("Enter Security PIN") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = DarkPrimary
                                )
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { verifyPin() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkPrimary,
                                unfocusedBorderColor = DarkOutline,
                                focusedLabelColor = DarkPrimary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pin_unlock_input")
                        )

                        OutlinedButton(
                            onClick = { verifyPin() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DarkPrimary),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("pin_unlock_button")
                        ) {
                            Text(
                                text = "Verify PIN",
                                color = DarkPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!pinError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.testTag("pin_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DarkOutline)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val keyguardManager =
                                context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                            @Suppress("DEPRECATION")
                            val confirmIntent = keyguardManager?.createConfirmDeviceCredentialIntent(
                                "Unlock SK Nexus Notes",
                                "Verify your biometric or device credential"
                            )
                            if (confirmIntent != null) {
                                credentialLauncher.launch(confirmIntent)
                            } else {
                                // Fallback when emulator has no hardware lock enrolled
                                onUnlockSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkPrimary,
                            contentColor = DarkOnPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("biometric_unlock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock with Biometrics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
