package com.yaawaaa.penbatteryremapper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.yaawaaa.penbatteryremapper.remapper.AppIconCache
import com.yaawaaa.penbatteryremapper.remapper.RootHelper
import com.yaawaaa.penbatteryremapper.ui.BatteryScreen
import com.yaawaaa.penbatteryremapper.ui.theme.Test2Theme
import com.yaawaaa.penbatteryremapper.worker.BatteryPollingWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val selectedTabState = mutableIntStateOf(0)
    private val batteryViewModel: BatteryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        configureLockscreen()
        handleIntent(intent)

        setContent {
            Test2Theme {
                AppScreen(initialTab = selectedTabState.intValue, viewModel = batteryViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        batteryViewModel.checkBatteryOptimizationStatus(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            AppIconCache.clear()
            lifecycleScope.launch(Dispatchers.IO) {
                System.gc()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        configureLockscreen()
        handleIntent(intent)
    }

    private fun configureLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "android.service.quicksettings.action.QS_TILE_PREFERENCES") {
            selectedTabState.intValue = 0
        }
    }
}

@Composable
fun AppScreen(
    initialTab: Int = 0,
    viewModel: BatteryViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        BatteryPollingWorker.schedule(context)
        requestSystemLockScreenPermission(context)
    }

    // Request POST_NOTIFICATIONS permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { /* Permission result handled */ }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BatteryScreen(initialTab = initialTab, viewModel = viewModel)
}

fun requestSystemLockScreenPermission(context: Context) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val hasPrompted = prefs.getBoolean("HAS_PROMPTED_LOCKSCREEN_PERMISSION", false)
    val canDrawOverlays = Settings.canDrawOverlays(context)

    if (hasPrompted && canDrawOverlays) return

    val packageName = context.packageName

    CoroutineScope(Dispatchers.Main).launch {
        if (RootHelper.isRootAvailable()) {
            RootHelper.protectProcessFromBackgroundKill(packageName)
            prefs.edit().putBoolean("HAS_PROMPTED_LOCKSCREEN_PERMISSION", true).apply()
            return@launch
        }

        if (hasPrompted) return@launch

        prefs.edit().putBoolean("HAS_PROMPTED_LOCKSCREEN_PERMISSION", true).apply()

        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        val isColorOS = manufacturer.contains("oppo") || manufacturer.contains("oneplus") || manufacturer.contains("realme") ||
                        brand.contains("oppo") || brand.contains("oneplus") || brand.contains("realme")
        val isMiui = manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") ||
                     brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")

        val intentsToTry = mutableListOf<Intent>()

        if (isColorOS) {
            intentsToTry.add(Intent("com.oplus.permissionmanager.permissions.PermissionDetailsActivity").apply {
                putExtra("extra_pkgname", packageName)
            })
            intentsToTry.add(Intent("android.intent.action.MANAGE_APP_PERMISSIONS").apply {
                putExtra("android.intent.extra.PACKAGE_NAME", packageName)
            })
            intentsToTry.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } else if (isMiui) {
            intentsToTry.add(Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", packageName)
            })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                intentsToTry.add(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
            intentsToTry.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } else {
            // Samsung One UI / Stock Android / Others
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                intentsToTry.add(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
            intentsToTry.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        }

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                break
            } catch (_: Exception) {
                // Try next intent
            }
        }
    }
}
