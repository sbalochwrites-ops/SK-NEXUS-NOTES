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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CRITICAL: Configure Firebase App Check before any Firebase or ViewModel access
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("nexus_ui_prefs", Context.MODE_PRIVATE)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("dark_theme", true))
            }
            var isDynamicColor by remember {
                mutableStateOf(prefs.getBoolean("dynamic_color", false))
            }
            var isBiometricLockEnabled by remember {
                mutableStateOf(prefs.getBoolean("biometric_lock", false))
            }
            var isAppUnlocked by remember {
                mutableStateOf(!isBiometricLockEnabled)
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = isDynamicColor
            ) {
                AppRoot(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { enabled ->
                        isDarkTheme = enabled
                        prefs.edit().putBoolean("dark_theme", enabled).apply()
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
                    isAppUnlocked = isAppUnlocked,
                    onUnlockSuccess = { isAppUnlocked = true },
                    onLockAppNow = { isAppUnlocked = false }
                )
            }
        }
    }
}

@Composable
fun AppRoot(
    isDarkTheme: Boolean = true,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    isDynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    isBiometricLockEnabled: Boolean = false,
    onToggleBiometricLock: (Boolean) -> Unit = {},
    isAppUnlocked: Boolean = true,
    onUnlockSuccess: () -> Unit = {},
    onLockAppNow: () -> Unit = {}
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
        BiometricLockGateScreen(onUnlockSuccess = onUnlockSuccess)
    } else {
        val noteViewModel: NoteViewModel = viewModel()
        NotesScreen(
            viewModel = noteViewModel,
            userEmail = currentUser?.email,
            onSignOut = {
                Firebase.auth.signOut()
            },
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            isDynamicColor = isDynamicColor,
            onToggleDynamicColor = onToggleDynamicColor,
            isBiometricLockEnabled = isBiometricLockEnabled,
            onToggleBiometricLock = onToggleBiometricLock,
            onLockAppNow = onLockAppNow
        )
    }
}

@Composable
fun BiometricLockGateScreen(
    onUnlockSuccess: () -> Unit
) {
    val context = LocalContext.current
    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onUnlockSuccess()
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

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "SK Nexus Vault Locked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Authenticate with your fingerprint, face unlock, or device credentials to access your private notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

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
