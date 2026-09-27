package com.yaawaaa.penbatteryremapper.remapper

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class BackupData(
    val version: Int = 1,
    val settings: RemapperSettingsData = RemapperSettingsData(),
    val appProfiles: List<AppProfileData> = emptyList()
)

data class RemapperSettingsData(
    val remapperEnabled: Boolean = false,
    val engineMode: String = EngineMode.ACCESSIBILITY.name,
    val primaryClickAction: String = RemapAction.PASSTHROUGH.name,
    val primaryClickShortcut: String = "",
    val primaryDoubleClickAction: String = RemapAction.PASSTHROUGH.name,
    val primaryDoubleClickShortcut: String = "",
    val primaryTripleClickAction: String = RemapAction.PASSTHROUGH.name,
    val primaryTripleClickShortcut: String = "",
    val primaryLongPressAction: String = RemapAction.PASSTHROUGH.name,
    val primaryLongPressShortcut: String = "",
    val primaryClickAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryClickAppPackage2: String = "",
    val primaryDoubleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryDoubleClickAppPackage2: String = "",
    val primaryTripleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryTripleClickAppPackage2: String = "",
    val primaryLongPressAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryLongPressAppPackage2: String = "",
    val secondaryClickAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryClickShortcut: String = "",
    val secondaryDoubleClickAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryDoubleClickShortcut: String = "",
    val secondaryTripleClickAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryTripleClickShortcut: String = "",
    val secondaryLongPressAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryLongPressShortcut: String = "",
    val secondaryClickAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryClickAppPackage2: String = "",
    val secondaryDoubleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryDoubleClickAppPackage2: String = "",
    val secondaryTripleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryTripleClickAppPackage2: String = "",
    val secondaryLongPressAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryLongPressAppPackage2: String = "",
    val longPressDurationMs: Int = 500,
    val doubleClickTimeoutMs: Int = 300,
    val tripleClickTimeoutMs: Int = 400,
    val primaryAppPackage: String = "",
    val secondaryAppPackage: String = "",
    val deviceNameFilter: String = "Pen"
)

data class AppProfileData(
    val packageName: String = "",
    val appName: String = "",
    val primaryClickAction: String = RemapAction.PASSTHROUGH.name,
    val primaryClickShortcut: String = "",
    val primaryClickAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryClickAppPackage2: String = "",
    val primaryDoubleClickAction: String = RemapAction.PASSTHROUGH.name,
    val primaryDoubleClickShortcut: String = "",
    val primaryDoubleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryDoubleClickAppPackage2: String = "",
    val primaryTripleClickAction: String = RemapAction.PASSTHROUGH.name,
    val primaryTripleClickShortcut: String = "",
    val primaryTripleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryTripleClickAppPackage2: String = "",
    val primaryLongPressAction: String = RemapAction.PASSTHROUGH.name,
    val primaryLongPressShortcut: String = "",
    val primaryLongPressAction2: String = RemapAction.PASSTHROUGH.name,
    val primaryLongPressAppPackage2: String = "",
    val secondaryClickAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryClickShortcut: String = "",
    val secondaryClickAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryClickAppPackage2: String = "",
    val secondaryDoubleClickAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryDoubleClickShortcut: String = "",
    val secondaryDoubleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryDoubleClickAppPackage2: String = "",
    val secondaryTripleClickAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryTripleClickShortcut: String = "",
    val secondaryTripleClickAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryTripleClickAppPackage2: String = "",
    val secondaryLongPressAction: String = RemapAction.PASSTHROUGH.name,
    val secondaryLongPressShortcut: String = "",
    val secondaryLongPressAction2: String = RemapAction.PASSTHROUGH.name,
    val secondaryLongPressAppPackage2: String = "",
    val primaryAppPackage: String = "",
    val secondaryAppPackage: String = ""
)

object RemapperBackupManager {

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val backupAdapter by lazy {
        moshi.adapter(BackupData::class.java).indent("  ")
    }

    private val profileListAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, AppProfileData::class.java)
        moshi.adapter<List<AppProfileData>>(type)
    }

    fun exportBackup(settings: RemapperSettings, appProfiles: List<AppProfile>): String {
        val backupData = BackupData(
            version = 1,
            settings = settings.toData(),
            appProfiles = appProfiles.map { it.toData() }
        )
        return backupAdapter.toJson(backupData)
    }

    fun importBackup(jsonString: String): Pair<RemapperSettings, List<AppProfile>>? {
        if (jsonString.isBlank()) return null
        return try {
            val backupData = backupAdapter.fromJson(jsonString.trim()) ?: return null
            Pair(
                backupData.settings.toRemapperSettings(),
                backupData.appProfiles.map { it.toAppProfile() }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun serializeAppProfilesJson(profiles: List<AppProfile>): String {
        return try {
            profileListAdapter.toJson(profiles.map { it.toData() })
        } catch (e: Exception) {
            ""
        }
    }

    fun parseAppProfilesJson(jsonString: String): List<AppProfile> {
        if (jsonString.isBlank()) return emptyList()
        return try {
            val dataList = profileListAdapter.fromJson(jsonString.trim()) ?: emptyList()
            dataList.map { it.toAppProfile() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun RemapperSettings.toData() = RemapperSettingsData(
        remapperEnabled = remapperEnabled,
        engineMode = engineMode.name,
        primaryClickAction = primaryClickAction.name,
        primaryClickShortcut = primaryClickShortcut,
        primaryDoubleClickAction = primaryDoubleClickAction.name,
        primaryDoubleClickShortcut = primaryDoubleClickShortcut,
        primaryTripleClickAction = primaryTripleClickAction.name,
        primaryTripleClickShortcut = primaryTripleClickShortcut,
        primaryLongPressAction = primaryLongPressAction.name,
        primaryLongPressShortcut = primaryLongPressShortcut,
        primaryClickAction2 = primaryClickAction2.name,
        primaryClickAppPackage2 = primaryClickAppPackage2,
        primaryDoubleClickAction2 = primaryDoubleClickAction2.name,
        primaryDoubleClickAppPackage2 = primaryDoubleClickAppPackage2,
        primaryTripleClickAction2 = primaryTripleClickAction2.name,
        primaryTripleClickAppPackage2 = primaryTripleClickAppPackage2,
        primaryLongPressAction2 = primaryLongPressAction2.name,
        primaryLongPressAppPackage2 = primaryLongPressAppPackage2,
        secondaryClickAction = secondaryClickAction.name,
        secondaryClickShortcut = secondaryClickShortcut,
        secondaryDoubleClickAction = secondaryDoubleClickAction.name,
        secondaryDoubleClickShortcut = secondaryDoubleClickShortcut,
        secondaryTripleClickAction = secondaryTripleClickAction.name,
        secondaryTripleClickShortcut = secondaryTripleClickShortcut,
        secondaryLongPressAction = secondaryLongPressAction.name,
        secondaryLongPressShortcut = secondaryLongPressShortcut,
        secondaryClickAction2 = secondaryClickAction2.name,
        secondaryClickAppPackage2 = secondaryClickAppPackage2,
        secondaryDoubleClickAction2 = secondaryDoubleClickAction2.name,
        secondaryDoubleClickAppPackage2 = secondaryDoubleClickAppPackage2,
        secondaryTripleClickAction2 = secondaryTripleClickAction2.name,
        secondaryTripleClickAppPackage2 = secondaryTripleClickAppPackage2,
        secondaryLongPressAction2 = secondaryLongPressAction2.name,
        secondaryLongPressAppPackage2 = secondaryLongPressAppPackage2,
        longPressDurationMs = longPressDurationMs,
        doubleClickTimeoutMs = doubleClickTimeoutMs,
        tripleClickTimeoutMs = tripleClickTimeoutMs,
        primaryAppPackage = primaryAppPackage,
        secondaryAppPackage = secondaryAppPackage,
        deviceNameFilter = deviceNameFilter
    )

    private fun RemapperSettingsData.toRemapperSettings() = RemapperSettings(
        remapperEnabled = remapperEnabled,
        engineMode = EngineMode.fromNameOrDefault(engineMode),
        primaryClickAction = RemapAction.fromNameOrDefault(primaryClickAction),
        primaryClickShortcut = primaryClickShortcut,
        primaryDoubleClickAction = RemapAction.fromNameOrDefault(primaryDoubleClickAction),
        primaryDoubleClickShortcut = primaryDoubleClickShortcut,
        primaryTripleClickAction = RemapAction.fromNameOrDefault(primaryTripleClickAction),
        primaryTripleClickShortcut = primaryTripleClickShortcut,
        primaryLongPressAction = RemapAction.fromNameOrDefault(primaryLongPressAction),
        primaryLongPressShortcut = primaryLongPressShortcut,
        primaryClickAction2 = RemapAction.fromNameOrDefault(primaryClickAction2),
        primaryClickAppPackage2 = primaryClickAppPackage2,
        primaryDoubleClickAction2 = RemapAction.fromNameOrDefault(primaryDoubleClickAction2),
        primaryDoubleClickAppPackage2 = primaryDoubleClickAppPackage2,
        primaryTripleClickAction2 = RemapAction.fromNameOrDefault(primaryTripleClickAction2),
        primaryTripleClickAppPackage2 = primaryTripleClickAppPackage2,
        primaryLongPressAction2 = RemapAction.fromNameOrDefault(primaryLongPressAction2),
        primaryLongPressAppPackage2 = primaryLongPressAppPackage2,
        secondaryClickAction = RemapAction.fromNameOrDefault(secondaryClickAction),
        secondaryClickShortcut = secondaryClickShortcut,
        secondaryDoubleClickAction = RemapAction.fromNameOrDefault(secondaryDoubleClickAction),
        secondaryDoubleClickShortcut = secondaryDoubleClickShortcut,
        secondaryTripleClickAction = RemapAction.fromNameOrDefault(secondaryTripleClickAction),
        secondaryTripleClickShortcut = secondaryTripleClickShortcut,
        secondaryLongPressAction = RemapAction.fromNameOrDefault(secondaryLongPressAction),
        secondaryLongPressShortcut = secondaryLongPressShortcut,
        secondaryClickAction2 = RemapAction.fromNameOrDefault(secondaryClickAction2),
        secondaryClickAppPackage2 = secondaryClickAppPackage2,
        secondaryDoubleClickAction2 = RemapAction.fromNameOrDefault(secondaryDoubleClickAction2),
        secondaryDoubleClickAppPackage2 = secondaryDoubleClickAppPackage2,
        secondaryTripleClickAction2 = RemapAction.fromNameOrDefault(secondaryTripleClickAction2),
        secondaryTripleClickAppPackage2 = secondaryTripleClickAppPackage2,
        secondaryLongPressAction2 = RemapAction.fromNameOrDefault(secondaryLongPressAction2),
        secondaryLongPressAppPackage2 = secondaryLongPressAppPackage2,
        longPressDurationMs = longPressDurationMs,
        doubleClickTimeoutMs = doubleClickTimeoutMs,
        tripleClickTimeoutMs = tripleClickTimeoutMs,
        primaryAppPackage = primaryAppPackage,
        secondaryAppPackage = secondaryAppPackage,
        deviceNameFilter = deviceNameFilter
    )

    private fun AppProfile.toData() = AppProfileData(
        packageName = packageName,
        appName = appName,
        primaryClickAction = primaryClickAction.name,
        primaryClickShortcut = primaryClickShortcut,
        primaryClickAction2 = primaryClickAction2.name,
        primaryClickAppPackage2 = primaryClickAppPackage2,
        primaryDoubleClickAction = primaryDoubleClickAction.name,
        primaryDoubleClickShortcut = primaryDoubleClickShortcut,
        primaryDoubleClickAction2 = primaryDoubleClickAction2.name,
        primaryDoubleClickAppPackage2 = primaryDoubleClickAppPackage2,
        primaryTripleClickAction = primaryTripleClickAction.name,
        primaryTripleClickShortcut = primaryTripleClickShortcut,
        primaryTripleClickAction2 = primaryTripleClickAction2.name,
        primaryTripleClickAppPackage2 = primaryTripleClickAppPackage2,
        primaryLongPressAction = primaryLongPressAction.name,
        primaryLongPressShortcut = primaryLongPressShortcut,
        primaryLongPressAction2 = primaryLongPressAction2.name,
        primaryLongPressAppPackage2 = primaryLongPressAppPackage2,
        secondaryClickAction = secondaryClickAction.name,
        secondaryClickShortcut = secondaryClickShortcut,
        secondaryClickAction2 = secondaryClickAction2.name,
        secondaryClickAppPackage2 = secondaryClickAppPackage2,
        secondaryDoubleClickAction = secondaryDoubleClickAction.name,
        secondaryDoubleClickShortcut = secondaryDoubleClickShortcut,
        secondaryDoubleClickAction2 = secondaryDoubleClickAction2.name,
        secondaryDoubleClickAppPackage2 = secondaryDoubleClickAppPackage2,
        secondaryTripleClickAction = secondaryTripleClickAction.name,
        secondaryTripleClickShortcut = secondaryTripleClickShortcut,
        secondaryTripleClickAction2 = secondaryTripleClickAction2.name,
        secondaryTripleClickAppPackage2 = secondaryTripleClickAppPackage2,
        secondaryLongPressAction = secondaryLongPressAction.name,
        secondaryLongPressShortcut = secondaryLongPressShortcut,
        secondaryLongPressAction2 = secondaryLongPressAction2.name,
        secondaryLongPressAppPackage2 = secondaryLongPressAppPackage2,
        primaryAppPackage = primaryAppPackage,
        secondaryAppPackage = secondaryAppPackage
    )

    private fun AppProfileData.toAppProfile() = AppProfile(
        packageName = packageName,
        appName = appName,
        primaryClickAction = RemapAction.fromNameOrDefault(primaryClickAction),
        primaryClickShortcut = primaryClickShortcut,
        primaryClickAction2 = RemapAction.fromNameOrDefault(primaryClickAction2),
        primaryClickAppPackage2 = primaryClickAppPackage2,
        primaryDoubleClickAction = RemapAction.fromNameOrDefault(primaryDoubleClickAction),
        primaryDoubleClickShortcut = primaryDoubleClickShortcut,
        primaryDoubleClickAction2 = RemapAction.fromNameOrDefault(primaryDoubleClickAction2),
        primaryDoubleClickAppPackage2 = primaryDoubleClickAppPackage2,
        primaryTripleClickAction = RemapAction.fromNameOrDefault(primaryTripleClickAction),
        primaryTripleClickShortcut = primaryTripleClickShortcut,
        primaryTripleClickAction2 = RemapAction.fromNameOrDefault(primaryTripleClickAction2),
        primaryTripleClickAppPackage2 = primaryTripleClickAppPackage2,
        primaryLongPressAction = RemapAction.fromNameOrDefault(primaryLongPressAction),
        primaryLongPressShortcut = primaryLongPressShortcut,
        primaryLongPressAction2 = RemapAction.fromNameOrDefault(primaryLongPressAction2),
        primaryLongPressAppPackage2 = primaryLongPressAppPackage2,
        secondaryClickAction = RemapAction.fromNameOrDefault(secondaryClickAction),
        secondaryClickShortcut = secondaryClickShortcut,
        secondaryClickAction2 = RemapAction.fromNameOrDefault(secondaryClickAction2),
        secondaryClickAppPackage2 = secondaryClickAppPackage2,
        secondaryDoubleClickAction = RemapAction.fromNameOrDefault(secondaryDoubleClickAction),
        secondaryDoubleClickShortcut = secondaryDoubleClickShortcut,
        secondaryDoubleClickAction2 = RemapAction.fromNameOrDefault(secondaryDoubleClickAction2),
        secondaryDoubleClickAppPackage2 = secondaryDoubleClickAppPackage2,
        secondaryTripleClickAction = RemapAction.fromNameOrDefault(secondaryTripleClickAction),
        secondaryTripleClickShortcut = secondaryTripleClickShortcut,
        secondaryTripleClickAction2 = RemapAction.fromNameOrDefault(secondaryTripleClickAction2),
        secondaryTripleClickAppPackage2 = secondaryTripleClickAppPackage2,
        secondaryLongPressAction = RemapAction.fromNameOrDefault(secondaryLongPressAction),
        secondaryLongPressShortcut = secondaryLongPressShortcut,
        secondaryLongPressAction2 = RemapAction.fromNameOrDefault(secondaryLongPressAction2),
        secondaryLongPressAppPackage2 = secondaryLongPressAppPackage2,
        primaryAppPackage = primaryAppPackage,
        secondaryAppPackage = secondaryAppPackage
    )
}
