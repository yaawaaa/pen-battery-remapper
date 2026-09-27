package com.yaawaaa.penbatteryremapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yaawaaa.penbatteryremapper.remapper.RootHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServiceKeepAliveReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isEnabled = PenRemapperAccessibilityService.isServiceEnabled(context)
                    if (!isEnabled && RootHelper.isRootAvailable()) {
                        RootHelper.enableAccessibilityServiceViaRoot(context.packageName)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
