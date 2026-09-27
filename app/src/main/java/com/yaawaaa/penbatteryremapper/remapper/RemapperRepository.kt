package com.yaawaaa.penbatteryremapper.remapper

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.remapperDataStore: DataStore<Preferences> by preferencesDataStore(name = "remapper_settings")

class RemapperRepository(private val context: Context) {

    private object PreferencesKeys {
        val REMAPPER_ENABLED = booleanPreferencesKey("remapper_enabled")
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
        val PRIMARY_CLICK_ACTION = stringPreferencesKey("primary_click_action")
        val PRIMARY_DOUBLE_CLICK_ACTION = stringPreferencesKey("primary_double_click_action")
        val PRIMARY_TRIPLE_CLICK_ACTION = stringPreferencesKey("primary_triple_click_action")
        val PRIMARY_LONG_PRESS_ACTION = stringPreferencesKey("primary_long_press_action")

        val PRIMARY_CLICK_SHORTCUT = stringPreferencesKey("primary_click_shortcut")
        val PRIMARY_DOUBLE_CLICK_SHORTCUT = stringPreferencesKey("primary_double_click_shortcut")
        val PRIMARY_TRIPLE_CLICK_SHORTCUT = stringPreferencesKey("primary_triple_click_shortcut")
        val PRIMARY_LONG_PRESS_SHORTCUT = stringPreferencesKey("primary_long_press_shortcut")

        val PRIMARY_CLICK_ACTION_2 = stringPreferencesKey("primary_click_action_2")
        val PRIMARY_CLICK_APP_PACKAGE_2 = stringPreferencesKey("primary_click_app_package_2")
        val PRIMARY_DOUBLE_CLICK_ACTION_2 = stringPreferencesKey("primary_double_click_action_2")
        val PRIMARY_DOUBLE_CLICK_APP_PACKAGE_2 = stringPreferencesKey("primary_double_click_app_package_2")
        val PRIMARY_TRIPLE_CLICK_ACTION_2 = stringPreferencesKey("primary_triple_click_action_2")
        val PRIMARY_TRIPLE_CLICK_APP_PACKAGE_2 = stringPreferencesKey("primary_triple_click_app_package_2")
        val PRIMARY_LONG_PRESS_ACTION_2 = stringPreferencesKey("primary_long_press_action_2")
        val PRIMARY_LONG_PRESS_APP_PACKAGE_2 = stringPreferencesKey("primary_long_press_app_package_2")

        val SECONDARY_CLICK_ACTION = stringPreferencesKey("secondary_click_action")
        val SECONDARY_CLICK_SHORTCUT = stringPreferencesKey("secondary_click_shortcut")
        val SECONDARY_DOUBLE_CLICK_ACTION = stringPreferencesKey("secondary_double_click_action")
        val SECONDARY_DOUBLE_CLICK_SHORTCUT = stringPreferencesKey("secondary_double_click_shortcut")
        val SECONDARY_TRIPLE_CLICK_ACTION = stringPreferencesKey("secondary_triple_click_action")
        val SECONDARY_TRIPLE_CLICK_SHORTCUT = stringPreferencesKey("secondary_triple_click_shortcut")
        val SECONDARY_LONG_PRESS_ACTION = stringPreferencesKey("secondary_long_press_action")
        val SECONDARY_LONG_PRESS_SHORTCUT = stringPreferencesKey("secondary_long_press_shortcut")

        val SECONDARY_CLICK_ACTION_2 = stringPreferencesKey("secondary_click_action_2")
        val SECONDARY_CLICK_APP_PACKAGE_2 = stringPreferencesKey("secondary_click_app_package_2")
        val SECONDARY_DOUBLE_CLICK_ACTION_2 = stringPreferencesKey("secondary_double_click_action_2")
        val SECONDARY_DOUBLE_CLICK_APP_PACKAGE_2 = stringPreferencesKey("secondary_double_click_app_package_2")
        val SECONDARY_TRIPLE_CLICK_ACTION_2 = stringPreferencesKey("secondary_triple_click_action_2")
        val SECONDARY_TRIPLE_CLICK_APP_PACKAGE_2 = stringPreferencesKey("secondary_triple_click_app_package_2")
        val SECONDARY_LONG_PRESS_ACTION_2 = stringPreferencesKey("secondary_long_press_action_2")
        val SECONDARY_LONG_PRESS_APP_PACKAGE_2 = stringPreferencesKey("secondary_long_press_app_package_2")

        val LONG_PRESS_DURATION_MS = intPreferencesKey("long_press_duration_ms")
        val DOUBLE_CLICK_TIMEOUT_MS = intPreferencesKey("double_click_timeout_ms")
        val TRIPLE_CLICK_TIMEOUT_MS = intPreferencesKey("triple_click_timeout_ms")
        val PRIMARY_APP_PACKAGE = stringPreferencesKey("primary_app_package")
        val SECONDARY_APP_PACKAGE = stringPreferencesKey("secondary_app_package")
        val DEVICE_NAME_FILTER = stringPreferencesKey("device_name_filter")
        val APP_PROFILES_JSON = stringPreferencesKey("app_profiles_json")

        val AIR_COMMAND_ENABLED = booleanPreferencesKey("air_command_enabled")
        val SHOW_EDGE_HANDLE = booleanPreferencesKey("show_edge_handle")
        val AIR_COMMAND_SCALE = floatPreferencesKey("air_command_scale")
        val AIR_COMMAND_OPACITY = floatPreferencesKey("air_command_opacity")
        val AIR_COMMAND_APPS = stringPreferencesKey("air_command_apps")
        val IS_TILE_ADDED = booleanPreferencesKey("is_tile_added")
    }

    val isTileAdded: Flow<Boolean> = context.remapperDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_TILE_ADDED] ?: false
        }

    suspend fun setTileAdded(added: Boolean) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_TILE_ADDED] = added
        }
    }

    val remapperSettings: Flow<RemapperSettings> = context.remapperDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            RemapperSettings(
                remapperEnabled = preferences[PreferencesKeys.REMAPPER_ENABLED] ?: false,
                engineMode = EngineMode.fromNameOrDefault(
                    preferences[PreferencesKeys.ENGINE_MODE],
                    EngineMode.ACCESSIBILITY
                ),
                primaryClickAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_CLICK_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                primaryClickShortcut = preferences[PreferencesKeys.PRIMARY_CLICK_SHORTCUT] ?: "",
                primaryDoubleClickAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                primaryDoubleClickShortcut = preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_SHORTCUT] ?: "",
                primaryTripleClickAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                primaryTripleClickShortcut = preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_SHORTCUT] ?: "",
                primaryLongPressAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_LONG_PRESS_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                primaryLongPressShortcut = preferences[PreferencesKeys.PRIMARY_LONG_PRESS_SHORTCUT] ?: "",
                primaryClickAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_CLICK_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                primaryClickAppPackage2 = preferences[PreferencesKeys.PRIMARY_CLICK_APP_PACKAGE_2] ?: "",
                primaryDoubleClickAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                primaryDoubleClickAppPackage2 = preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_APP_PACKAGE_2] ?: "",
                primaryTripleClickAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                primaryTripleClickAppPackage2 = preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_APP_PACKAGE_2] ?: "",
                primaryLongPressAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.PRIMARY_LONG_PRESS_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                primaryLongPressAppPackage2 = preferences[PreferencesKeys.PRIMARY_LONG_PRESS_APP_PACKAGE_2] ?: "",
                secondaryClickAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_CLICK_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                secondaryClickShortcut = preferences[PreferencesKeys.SECONDARY_CLICK_SHORTCUT] ?: "",
                secondaryDoubleClickAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                secondaryDoubleClickShortcut = preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_SHORTCUT] ?: "",
                secondaryTripleClickAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                secondaryTripleClickShortcut = preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_SHORTCUT] ?: "",
                secondaryLongPressAction = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_LONG_PRESS_ACTION],
                    RemapAction.PASSTHROUGH
                ),
                secondaryLongPressShortcut = preferences[PreferencesKeys.SECONDARY_LONG_PRESS_SHORTCUT] ?: "",
                secondaryClickAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_CLICK_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                secondaryClickAppPackage2 = preferences[PreferencesKeys.SECONDARY_CLICK_APP_PACKAGE_2] ?: "",
                secondaryDoubleClickAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                secondaryDoubleClickAppPackage2 = preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_APP_PACKAGE_2] ?: "",
                secondaryTripleClickAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                secondaryTripleClickAppPackage2 = preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_APP_PACKAGE_2] ?: "",
                secondaryLongPressAction2 = RemapAction.fromNameOrDefault(
                    preferences[PreferencesKeys.SECONDARY_LONG_PRESS_ACTION_2],
                    RemapAction.PASSTHROUGH
                ),
                secondaryLongPressAppPackage2 = preferences[PreferencesKeys.SECONDARY_LONG_PRESS_APP_PACKAGE_2] ?: "",
                longPressDurationMs = preferences[PreferencesKeys.LONG_PRESS_DURATION_MS] ?: 500,
                doubleClickTimeoutMs = preferences[PreferencesKeys.DOUBLE_CLICK_TIMEOUT_MS] ?: 300,
                tripleClickTimeoutMs = preferences[PreferencesKeys.TRIPLE_CLICK_TIMEOUT_MS] ?: 400,
                primaryAppPackage = preferences[PreferencesKeys.PRIMARY_APP_PACKAGE] ?: "",
                secondaryAppPackage = preferences[PreferencesKeys.SECONDARY_APP_PACKAGE] ?: "",
                deviceNameFilter = preferences[PreferencesKeys.DEVICE_NAME_FILTER] ?: "Pen",
                airCommandEnabled = preferences[PreferencesKeys.AIR_COMMAND_ENABLED] ?: false,
                showEdgeHandle = preferences[PreferencesKeys.SHOW_EDGE_HANDLE] ?: true,
                airCommandScale = preferences[PreferencesKeys.AIR_COMMAND_SCALE] ?: 1.0f,
                airCommandOpacity = preferences[PreferencesKeys.AIR_COMMAND_OPACITY] ?: 1.0f,
                airCommandApps = preferences[PreferencesKeys.AIR_COMMAND_APPS] ?: ""
            )
        }

    val appProfiles: Flow<List<AppProfile>> = context.remapperDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val json = preferences[PreferencesKeys.APP_PROFILES_JSON] ?: ""
            RemapperBackupManager.parseAppProfilesJson(json)
        }

    suspend fun saveAppProfiles(profiles: List<AppProfile>) {
        val json = RemapperBackupManager.serializeAppProfilesJson(profiles)
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_PROFILES_JSON] = json
        }
    }

    suspend fun addOrUpdateAppProfile(profile: AppProfile) {
        context.remapperDataStore.edit { preferences ->
            val json = preferences[PreferencesKeys.APP_PROFILES_JSON] ?: ""
            val currentList = RemapperBackupManager.parseAppProfilesJson(json).toMutableList()
            val index = currentList.indexOfFirst { it.packageName == profile.packageName }
            if (index >= 0) {
                currentList[index] = profile
            } else {
                currentList.add(profile)
            }
            preferences[PreferencesKeys.APP_PROFILES_JSON] = RemapperBackupManager.serializeAppProfilesJson(currentList)
        }
    }

    suspend fun deleteAppProfile(packageName: String) {
        context.remapperDataStore.edit { preferences ->
            val json = preferences[PreferencesKeys.APP_PROFILES_JSON] ?: ""
            val currentList = RemapperBackupManager.parseAppProfilesJson(json).filter { it.packageName != packageName }
            preferences[PreferencesKeys.APP_PROFILES_JSON] = RemapperBackupManager.serializeAppProfilesJson(currentList)
        }
    }

    suspend fun saveAllSettings(settings: RemapperSettings) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMAPPER_ENABLED] = settings.remapperEnabled
            preferences[PreferencesKeys.ENGINE_MODE] = settings.engineMode.name
            preferences[PreferencesKeys.PRIMARY_CLICK_ACTION] = settings.primaryClickAction.name
            preferences[PreferencesKeys.PRIMARY_CLICK_SHORTCUT] = settings.primaryClickShortcut
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_ACTION] = settings.primaryDoubleClickAction.name
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_SHORTCUT] = settings.primaryDoubleClickShortcut
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_ACTION] = settings.primaryTripleClickAction.name
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_SHORTCUT] = settings.primaryTripleClickShortcut
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_ACTION] = settings.primaryLongPressAction.name
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_SHORTCUT] = settings.primaryLongPressShortcut
            preferences[PreferencesKeys.PRIMARY_CLICK_ACTION_2] = settings.primaryClickAction2.name
            preferences[PreferencesKeys.PRIMARY_CLICK_APP_PACKAGE_2] = settings.primaryClickAppPackage2
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_ACTION_2] = settings.primaryDoubleClickAction2.name
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_APP_PACKAGE_2] = settings.primaryDoubleClickAppPackage2
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_ACTION_2] = settings.primaryTripleClickAction2.name
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_APP_PACKAGE_2] = settings.primaryTripleClickAppPackage2
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_ACTION_2] = settings.primaryLongPressAction2.name
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_APP_PACKAGE_2] = settings.primaryLongPressAppPackage2

            preferences[PreferencesKeys.SECONDARY_CLICK_ACTION] = settings.secondaryClickAction.name
            preferences[PreferencesKeys.SECONDARY_CLICK_SHORTCUT] = settings.secondaryClickShortcut
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_ACTION] = settings.secondaryDoubleClickAction.name
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_SHORTCUT] = settings.secondaryDoubleClickShortcut
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_ACTION] = settings.secondaryTripleClickAction.name
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_SHORTCUT] = settings.secondaryTripleClickShortcut
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_ACTION] = settings.secondaryLongPressAction.name
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_SHORTCUT] = settings.secondaryLongPressShortcut
            preferences[PreferencesKeys.SECONDARY_CLICK_ACTION_2] = settings.secondaryClickAction2.name
            preferences[PreferencesKeys.SECONDARY_CLICK_APP_PACKAGE_2] = settings.secondaryClickAppPackage2
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_ACTION_2] = settings.secondaryDoubleClickAction2.name
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_APP_PACKAGE_2] = settings.secondaryDoubleClickAppPackage2
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_ACTION_2] = settings.secondaryTripleClickAction2.name
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_APP_PACKAGE_2] = settings.secondaryTripleClickAppPackage2
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_ACTION_2] = settings.secondaryLongPressAction2.name
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_APP_PACKAGE_2] = settings.secondaryLongPressAppPackage2

            preferences[PreferencesKeys.LONG_PRESS_DURATION_MS] = settings.longPressDurationMs
            preferences[PreferencesKeys.DOUBLE_CLICK_TIMEOUT_MS] = settings.doubleClickTimeoutMs
            preferences[PreferencesKeys.TRIPLE_CLICK_TIMEOUT_MS] = settings.tripleClickTimeoutMs
            preferences[PreferencesKeys.PRIMARY_APP_PACKAGE] = settings.primaryAppPackage
            preferences[PreferencesKeys.SECONDARY_APP_PACKAGE] = settings.secondaryAppPackage
            preferences[PreferencesKeys.DEVICE_NAME_FILTER] = settings.deviceNameFilter
            
            preferences[PreferencesKeys.AIR_COMMAND_ENABLED] = settings.airCommandEnabled
            preferences[PreferencesKeys.SHOW_EDGE_HANDLE] = settings.showEdgeHandle
            preferences[PreferencesKeys.AIR_COMMAND_SCALE] = settings.airCommandScale
            preferences[PreferencesKeys.AIR_COMMAND_OPACITY] = settings.airCommandOpacity
            preferences[PreferencesKeys.AIR_COMMAND_APPS] = settings.airCommandApps
        }
    }

    suspend fun setRemapperEnabled(enabled: Boolean) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMAPPER_ENABLED] = enabled
        }
    }

    suspend fun setAirCommandApps(apps: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.AIR_COMMAND_APPS] = apps
        }
    }

    suspend fun setAirCommandScale(scale: Float) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.AIR_COMMAND_SCALE] = scale
        }
    }

    suspend fun setEngineMode(mode: EngineMode) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.ENGINE_MODE] = mode.name
        }
    }

    suspend fun setPrimaryClickAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_CLICK_ACTION] = action.name
        }
    }

    suspend fun setPrimaryDoubleClickAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_ACTION] = action.name
        }
    }

    suspend fun setPrimaryTripleClickAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_ACTION] = action.name
        }
    }

    suspend fun setPrimaryLongPressAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_ACTION] = action.name
        }
    }

    suspend fun setPrimaryClickAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_CLICK_ACTION_2] = action.name
        }
    }

    suspend fun setPrimaryClickAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_CLICK_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setPrimaryDoubleClickAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_ACTION_2] = action.name
        }
    }

    suspend fun setPrimaryDoubleClickAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setPrimaryTripleClickAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_ACTION_2] = action.name
        }
    }

    suspend fun setPrimaryTripleClickAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setPrimaryLongPressAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_ACTION_2] = action.name
        }
    }

    suspend fun setPrimaryLongPressAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setSecondaryClickAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_CLICK_ACTION] = action.name
        }
    }

    suspend fun setSecondaryDoubleClickAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_ACTION] = action.name
        }
    }

    suspend fun setSecondaryTripleClickAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_ACTION] = action.name
        }
    }

    suspend fun setSecondaryLongPressAction(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_ACTION] = action.name
        }
    }

    suspend fun setSecondaryClickAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_CLICK_ACTION_2] = action.name
        }
    }

    suspend fun setSecondaryClickAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_CLICK_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setSecondaryDoubleClickAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_ACTION_2] = action.name
        }
    }

    suspend fun setSecondaryDoubleClickAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setSecondaryTripleClickAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_ACTION_2] = action.name
        }
    }

    suspend fun setSecondaryTripleClickAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setSecondaryLongPressAction2(action: RemapAction) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_ACTION_2] = action.name
        }
    }

    suspend fun setSecondaryLongPressAppPackage2(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_APP_PACKAGE_2] = appPackage
        }
    }

    suspend fun setLongPressDurationMs(durationMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.LONG_PRESS_DURATION_MS] = durationMs
        }
    }

    suspend fun setDoubleClickTimeoutMs(timeoutMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.DOUBLE_CLICK_TIMEOUT_MS] = timeoutMs
        }
    }

    suspend fun setTripleClickTimeoutMs(timeoutMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.TRIPLE_CLICK_TIMEOUT_MS] = timeoutMs
        }
    }

    suspend fun setPrimaryAppPackage(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_APP_PACKAGE] = appPackage
        }
    }

    suspend fun setSecondaryAppPackage(appPackage: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_APP_PACKAGE] = appPackage
        }
    }

    suspend fun setDeviceNameFilter(filter: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVICE_NAME_FILTER] = filter
        }
    }

    suspend fun setPrimaryClickShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_CLICK_SHORTCUT] = shortcut
        }
    }

    suspend fun setPrimaryDoubleClickShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_SHORTCUT] = shortcut
        }
    }

    suspend fun setPrimaryTripleClickShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_SHORTCUT] = shortcut
        }
    }

    suspend fun setPrimaryLongPressShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_SHORTCUT] = shortcut
        }
    }

    suspend fun setSecondaryClickShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_CLICK_SHORTCUT] = shortcut
        }
    }

    suspend fun setSecondaryDoubleClickShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_SHORTCUT] = shortcut
        }
    }

    suspend fun setSecondaryTripleClickShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_SHORTCUT] = shortcut
        }
    }

    suspend fun setSecondaryLongPressShortcut(shortcut: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_SHORTCUT] = shortcut
        }
    }
}
