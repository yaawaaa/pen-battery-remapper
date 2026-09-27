package com.yaawaaa.penbattchecker2

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.yaawaaa.penbattchecker2.ui.BatteryScreen
import com.yaawaaa.penbattchecker2.ui.theme.Test2Theme
import com.yaawaaa.penbattchecker2.worker.BatteryPollingWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Test2Theme {
                AppScreen()
            }
        }
    }
}

@Composable
fun AppScreen() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        BatteryPollingWorker.schedule(context)
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

    BatteryScreen()
}
