package com.example.myapplication

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.service.FocusForegroundService
import com.example.myapplication.util.AndroidAppPickerManager
import com.focal.app.data.FocalRepository
import com.focal.app.engine.FocusEngine
import com.focal.app.model.SessionState
import com.focal.app.model.TriggerType

class MainActivity : ComponentActivity() {

    companion object {
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        fun openUsageAccessSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        fun openOverlaySettings(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {}
        }
    }

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize Android NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Initialize build flavor configuration
        com.focal.app.config.FocalConfig.isDemoMode = BuildConfig.IS_DEMO_MODE
        com.focal.app.config.FocalConfig.isDebug = BuildConfig.DEBUG

        // Bind Context to Permissions Helper
        com.focal.app.ui.components.PlatformPermissionsHelper.appContext = applicationContext
        com.focal.app.ui.components.PlatformPermissionsHelper.currentActivity = java.lang.ref.WeakReference(this)

        // Discover and load installed apps for the Policy App Picker & listen for installs/uninstalls
        AndroidAppPickerManager.loadInstalledAppsIntoRepository(this)
        AndroidAppPickerManager.registerPackageReceiver(this)

        // Handle if launched via NFC Tag intent or blocked app intercept
        intent?.let {
            handleNfcIntent(it)
            handleBlockedAppIntent(it)
        }

        // Allow screenshots and screen capture cleanly during all focus sessions and app usage
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            // Synchronize background ForegroundService lifecycle with FocusEngine
            val engine = FocusEngine.instance
            LaunchedEffect(engine.sessionState) {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

                if (engine.sessionState == SessionState.ACTIVE) {
                    FocusForegroundService.start(this@MainActivity)
                } else {
                    FocusForegroundService.stop(this@MainActivity)
                }
            }

            App()
        }
    }

    override fun onResume() {
        super.onResume()
        com.focal.app.ui.components.PlatformPermissionsHelper.currentActivity = java.lang.ref.WeakReference(this)
        enableNfcForegroundDispatch()
        com.example.myapplication.nfc.AndroidNfcManager.enableReaderMode(this) { tag ->
            com.example.myapplication.nfc.AndroidNfcManager.handleTagDiscovered(tag, this)
        }
        FocusEngine.instance.checkAndUpdatePermissions()
    }

    override fun onPause() {
        super.onPause()
        com.example.myapplication.nfc.AndroidNfcManager.disableReaderMode(this)
        disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
        handleBlockedAppIntent(intent)
    }

    private fun handleBlockedAppIntent(intent: Intent) {
        val blockedAppName = intent.getStringExtra("EXTRA_BLOCKED_APP")
        if (!blockedAppName.isNullOrBlank()) {
            FocusEngine.instance.blockedAppOverlayName = blockedAppName
            vibrateDevice()
        }
    }

    private fun enableNfcForegroundDispatch() {
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
                val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    addDataScheme("vnd.android.nfc")
                    addDataAuthority("ext", null)
                    addDataPath("/nfcguard.io:card", android.os.PatternMatcher.PATTERN_PREFIX)
                }
                val filters = arrayOf(
                    ndefFilter,
                    IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                    IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                )
                val techLists = arrayOf(arrayOf(android.nfc.tech.NfcA::class.java.name))
                adapter.enableForegroundDispatch(this, pendingIntent, filters, techLists)
            }
        }
    }

    private fun disableNfcForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun handleNfcIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action
        ) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            tag?.let {
                // Trigger vibration feedback
                vibrateDevice()

                // Process NFC tag with AndroidNfcManager (writes NDEF if programming or toggles Focus)
                com.example.myapplication.nfc.AndroidNfcManager.handleTagDiscovered(it, this)
            }
        }
    }

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}