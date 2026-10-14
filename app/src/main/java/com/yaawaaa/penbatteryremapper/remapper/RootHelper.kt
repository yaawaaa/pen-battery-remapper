package com.yaawaaa.penbatteryremapper.remapper

import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream

object RootHelper {

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun enableAccessibilityServiceViaRoot(packageName: String = "com.yaawaaa.penbatteryremapper"): Boolean = withContext(Dispatchers.IO) {
        val serviceComponent = "$packageName/.service.PenRemapperAccessibilityService"
        val cmd1 = "settings put secure enabled_accessibility_services $serviceComponent"
        val cmd2 = "settings put secure accessibility_enabled 1"
        val cmd3 = "pm grant $packageName android.permission.INJECT_EVENTS"
        val cmd4 = "pm grant $packageName android.permission.SYSTEM_ALERT_WINDOW"

        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$cmd1\n")
            os.writeBytes("$cmd2\n")
            os.writeBytes("$cmd3\n")
            os.writeBytes("$cmd4\n")
            os.writeBytes("exit\n")
            os.flush()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun protectProcessFromBackgroundKill(packageName: String = "com.yaawaaa.penbatteryremapper"): Boolean = withContext(Dispatchers.IO) {
        val pid = Process.myPid()
        val cmds = listOf(
            "dumpsys deviceidle whitelist +$packageName",
            "cmd deviceidle whitelist +$packageName",
            "am set-standby-bucket $packageName active",
            "cmd appops set $packageName RUN_IN_BACKGROUND allow",
            "cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow",
            "pm grant $packageName android.permission.INJECT_EVENTS",
            "echo -1000 > /proc/$pid/oom_score_adj",
            "echo -17 > /proc/$pid/oom_adj",
            "cmd appops set $packageName SHOW_WHEN_LOCKED allow",
            "cmd appops set $packageName 10020 allow",
            "cmd appops set $packageName 10021 allow",
            "cmd appops set $packageName SYSTEM_ALERT_WINDOW allow",
            "cmd appops set $packageName USE_FULL_SCREEN_INTENT allow",
            "pm grant $packageName android.permission.SYSTEM_ALERT_WINDOW",
            "pm grant $packageName android.permission.USE_FULL_SCREEN_INTENT"
        )

        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (cmd in cmds) {
                os.writeBytes("$cmd\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun protectCurrentProcessFromLMK(): Boolean = withContext(Dispatchers.IO) {
        val pid = Process.myPid()
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("echo -1000 > /proc/$pid/oom_score_adj\n")
            os.writeBytes("echo -17 > /proc/$pid/oom_adj\n")
            os.writeBytes("exit\n")
            os.flush()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
}
