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
import com.yaawaaa.penbatteryremapper.ui.ActionTargetSlot
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
        val USE_CUSTOM_GESTURE_TIMEOUTS = booleanPreferencesKey("use_custom_gesture_timeouts")
        val USE_CUSTOM_GESTURE_POSITIONS = booleanPreferencesKey("use_custom_gesture_positions")

        val PRIMARY_CLICK_CUSTOM_POS_1 = booleanPreferencesKey("primary_click_custom_pos_1")
        val PRIMARY_CLICK_CUSTOM_POS_2 = booleanPreferencesKey("primary_click_custom_pos_2")
        val PRIMARY_DOUBLE_CLICK_CUSTOM_POS_1 = booleanPreferencesKey("primary_double_click_custom_pos_1")
        val PRIMARY_DOUBLE_CLICK_CUSTOM_POS_2 = booleanPreferencesKey("primary_double_click_custom_pos_2")
        val PRIMARY_TRIPLE_CLICK_CUSTOM_POS_1 = booleanPreferencesKey("primary_triple_click_custom_pos_1")
        val PRIMARY_TRIPLE_CLICK_CUSTOM_POS_2 = booleanPreferencesKey("primary_triple_click_custom_pos_2")
        val PRIMARY_LONG_PRESS_CUSTOM_POS_1 = booleanPreferencesKey("primary_long_press_custom_pos_1")
        val PRIMARY_LONG_PRESS_CUSTOM_POS_2 = booleanPreferencesKey("primary_long_press_custom_pos_2")

        val SECONDARY_CLICK_CUSTOM_POS_1 = booleanPreferencesKey("secondary_click_custom_pos_1")
        val SECONDARY_CLICK_CUSTOM_POS_2 = booleanPreferencesKey("secondary_click_custom_pos_2")
        val SECONDARY_DOUBLE_CLICK_CUSTOM_POS_1 = booleanPreferencesKey("secondary_double_click_custom_pos_1")
        val SECONDARY_DOUBLE_CLICK_CUSTOM_POS_2 = booleanPreferencesKey("secondary_double_click_custom_pos_2")
        val SECONDARY_TRIPLE_CLICK_CUSTOM_POS_1 = booleanPreferencesKey("secondary_triple_click_custom_pos_1")
        val SECONDARY_TRIPLE_CLICK_CUSTOM_POS_2 = booleanPreferencesKey("secondary_triple_click_custom_pos_2")
        val SECONDARY_LONG_PRESS_CUSTOM_POS_1 = booleanPreferencesKey("secondary_long_press_custom_pos_1")
        val SECONDARY_LONG_PRESS_CUSTOM_POS_2 = booleanPreferencesKey("secondary_long_press_custom_pos_2")

        val PRIMARY_DOUBLE_CLICK_TIMEOUT = intPreferencesKey("primary_double_click_timeout")
        val PRIMARY_TRIPLE_CLICK_TIMEOUT = intPreferencesKey("primary_triple_click_timeout")
        val PRIMARY_LONG_PRESS_DURATION = intPreferencesKey("primary_long_press_duration")

        val SECONDARY_DOUBLE_CLICK_TIMEOUT = intPreferencesKey("secondary_double_click_timeout")
        val SECONDARY_TRIPLE_CLICK_TIMEOUT = intPreferencesKey("secondary_triple_click_timeout")
        val SECONDARY_LONG_PRESS_DURATION = intPreferencesKey("secondary_long_press_duration")

        val PRIMARY_APP_PACKAGE = stringPreferencesKey("primary_app_package")
        val SECONDARY_APP_PACKAGE = stringPreferencesKey("secondary_app_package")
        val DEVICE_NAME_FILTER = stringPreferencesKey("device_name_filter")
        val APP_PROFILES_JSON = stringPreferencesKey("app_profiles_json")

        val AIR_COMMAND_ENABLED = booleanPreferencesKey("air_command_enabled")
        val SHOW_EDGE_HANDLE = booleanPreferencesKey("show_edge_handle")
        val AIR_COMMAND_SCALE = floatPreferencesKey("air_command_scale")
        val AIR_COMMAND_OPACITY = floatPreferencesKey("air_command_opacity")
        val AIR_COMMAND_APPS = stringPreferencesKey("air_command_apps")
        val POSITION_PRESETS_JSON = stringPreferencesKey("position_presets_json")
        val IS_TILE_ADDED = booleanPreferencesKey("is_tile_added")
    }

    companion object {
        fun saveFastCacheSettings(context: Context, settings: RemapperSettings) {
            try {
                val prefs = context.getSharedPreferences("remapper_fast_cache", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putBoolean("remapper_enabled", settings.remapperEnabled)
                    putString("engine_mode", settings.engineMode.name)
                    putString("primary_click_action", settings.primaryClickAction.name)
                    putString("primary_click_shortcut", settings.primaryClickShortcut)
                    putString("primary_double_click_action", settings.primaryDoubleClickAction.name)
                    putString("primary_double_click_shortcut", settings.primaryDoubleClickShortcut)
                    putString("primary_triple_click_action", settings.primaryTripleClickAction.name)
                    putString("primary_triple_click_shortcut", settings.primaryTripleClickShortcut)
                    putString("primary_long_press_action", settings.primaryLongPressAction.name)
                    putString("primary_long_press_shortcut", settings.primaryLongPressShortcut)
                    putString("primary_click_action_2", settings.primaryClickAction2.name)
                    putString("primary_click_app_package_2", settings.primaryClickAppPackage2)
                    putString("primary_double_click_action_2", settings.primaryDoubleClickAction2.name)
                    putString("primary_double_click_app_package_2", settings.primaryDoubleClickAppPackage2)
                    putString("primary_triple_click_action_2", settings.primaryTripleClickAction2.name)
                    putString("primary_triple_click_app_package_2", settings.primaryTripleClickAppPackage2)
                    putString("primary_long_press_action_2", settings.primaryLongPressAction2.name)
                    putString("primary_long_press_app_package_2", settings.primaryLongPressAppPackage2)

                    putString("secondary_click_action", settings.secondaryClickAction.name)
                    putString("secondary_click_shortcut", settings.secondaryClickShortcut)
                    putString("secondary_double_click_action", settings.secondaryDoubleClickAction.name)
                    putString("secondary_double_click_shortcut", settings.secondaryDoubleClickShortcut)
                    putString("secondary_triple_click_action", settings.secondaryTripleClickAction.name)
                    putString("secondary_triple_click_shortcut", settings.secondaryTripleClickShortcut)
                    putString("secondary_long_press_action", settings.secondaryLongPressAction.name)
                    putString("secondary_long_press_shortcut", settings.secondaryLongPressShortcut)
                    putString("secondary_click_action_2", settings.secondaryClickAction2.name)
                    putString("secondary_click_app_package_2", settings.secondaryClickAppPackage2)
                    putString("secondary_double_click_action_2", settings.secondaryDoubleClickAction2.name)
                    putString("secondary_double_click_app_package_2", settings.secondaryDoubleClickAppPackage2)
                    putString("secondary_triple_click_action_2", settings.secondaryTripleClickAction2.name)
                    putString("secondary_triple_click_app_package_2", settings.secondaryTripleClickAppPackage2)
                    putString("secondary_long_press_action_2", settings.secondaryLongPressAction2.name)
                    putString("secondary_long_press_app_package_2", settings.secondaryLongPressAppPackage2)

                    putInt("long_press_duration_ms", settings.longPressDurationMs)
                    putInt("double_click_timeout_ms", settings.doubleClickTimeoutMs)
                    putInt("triple_click_timeout_ms", settings.tripleClickTimeoutMs)
                    putBoolean("use_custom_gesture_timeouts", settings.useCustomGestureTimeouts)
                    putBoolean("use_custom_gesture_positions", settings.useCustomGesturePositions)

                    putBoolean("primary_click_custom_pos_1", settings.primaryClickCustomPos1)
                    putBoolean("primary_click_custom_pos_2", settings.primaryClickCustomPos2)
                    putBoolean("primary_double_click_custom_pos_1", settings.primaryDoubleClickCustomPos1)
                    putBoolean("primary_double_click_custom_pos_2", settings.primaryDoubleClickCustomPos2)
                    putBoolean("primary_triple_click_custom_pos_1", settings.primaryTripleClickCustomPos1)
                    putBoolean("primary_triple_click_custom_pos_2", settings.primaryTripleClickCustomPos2)
                    putBoolean("primary_long_press_custom_pos_1", settings.primaryLongPressCustomPos1)
                    putBoolean("primary_long_press_custom_pos_2", settings.primaryLongPressCustomPos2)

                    putBoolean("secondary_click_custom_pos_1", settings.secondaryClickCustomPos1)
                    putBoolean("secondary_click_custom_pos_2", settings.secondaryClickCustomPos2)
                    putBoolean("secondary_double_click_custom_pos_1", settings.secondaryDoubleClickCustomPos1)
                    putBoolean("secondary_double_click_custom_pos_2", settings.secondaryDoubleClickCustomPos2)
                    putBoolean("secondary_triple_click_custom_pos_1", settings.secondaryTripleClickCustomPos1)
                    putBoolean("secondary_triple_click_custom_pos_2", settings.secondaryTripleClickCustomPos2)
                    putBoolean("secondary_long_press_custom_pos_1", settings.secondaryLongPressCustomPos1)
                    putBoolean("secondary_long_press_custom_pos_2", settings.secondaryLongPressCustomPos2)

                    putInt("primary_double_click_timeout", settings.primaryDoubleClickTimeoutMs)
                    putInt("primary_triple_click_timeout", settings.primaryTripleClickTimeoutMs)
                    putInt("primary_long_press_duration", settings.primaryLongPressDurationMs)
                    putInt("secondary_double_click_timeout", settings.secondaryDoubleClickTimeoutMs)
                    putInt("secondary_triple_click_timeout", settings.secondaryTripleClickTimeoutMs)
                    putInt("secondary_long_press_duration", settings.secondaryLongPressDurationMs)

                    putString("primary_app_package", settings.primaryAppPackage)
                    putString("secondary_app_package", settings.secondaryAppPackage)
                    putString("device_name_filter", settings.deviceNameFilter)

                    putBoolean("air_command_enabled", settings.airCommandEnabled)
                    putBoolean("show_edge_handle", settings.showEdgeHandle)
                    putFloat("air_command_scale", settings.airCommandScale)
                    putFloat("air_command_opacity", settings.airCommandOpacity)
                    putString("air_command_apps", settings.airCommandApps)
                    putString("position_presets_json", settings.positionPresetsJson)
                    apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun saveFastCacheProfiles(context: Context, json: String) {
            try {
                val prefs = context.getSharedPreferences("remapper_fast_cache", Context.MODE_PRIVATE)
                prefs.edit().putString("app_profiles_json", json).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
            val settings = RemapperSettings(
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
                tripleClickTimeoutMs = preferences[PreferencesKeys.TRIPLE_CLICK_TIMEOUT_MS] ?: 350,
                useCustomGestureTimeouts = preferences[PreferencesKeys.USE_CUSTOM_GESTURE_TIMEOUTS] ?: false,
                useCustomGesturePositions = preferences[PreferencesKeys.USE_CUSTOM_GESTURE_POSITIONS] ?: false,
                primaryClickCustomPos1 = preferences[PreferencesKeys.PRIMARY_CLICK_CUSTOM_POS_1] ?: false,
                primaryClickCustomPos2 = preferences[PreferencesKeys.PRIMARY_CLICK_CUSTOM_POS_2] ?: false,
                primaryDoubleClickCustomPos1 = preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_CUSTOM_POS_1] ?: false,
                primaryDoubleClickCustomPos2 = preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_CUSTOM_POS_2] ?: false,
                primaryTripleClickCustomPos1 = preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_CUSTOM_POS_1] ?: false,
                primaryTripleClickCustomPos2 = preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_CUSTOM_POS_2] ?: false,
                primaryLongPressCustomPos1 = preferences[PreferencesKeys.PRIMARY_LONG_PRESS_CUSTOM_POS_1] ?: false,
                primaryLongPressCustomPos2 = preferences[PreferencesKeys.PRIMARY_LONG_PRESS_CUSTOM_POS_2] ?: false,
                secondaryClickCustomPos1 = preferences[PreferencesKeys.SECONDARY_CLICK_CUSTOM_POS_1] ?: false,
                secondaryClickCustomPos2 = preferences[PreferencesKeys.SECONDARY_CLICK_CUSTOM_POS_2] ?: false,
                secondaryDoubleClickCustomPos1 = preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_CUSTOM_POS_1] ?: false,
                secondaryDoubleClickCustomPos2 = preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_CUSTOM_POS_2] ?: false,
                secondaryTripleClickCustomPos1 = preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_CUSTOM_POS_1] ?: false,
                secondaryTripleClickCustomPos2 = preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_CUSTOM_POS_2] ?: false,
                secondaryLongPressCustomPos1 = preferences[PreferencesKeys.SECONDARY_LONG_PRESS_CUSTOM_POS_1] ?: false,
                secondaryLongPressCustomPos2 = preferences[PreferencesKeys.SECONDARY_LONG_PRESS_CUSTOM_POS_2] ?: false,
                primaryDoubleClickTimeoutMs = preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_TIMEOUT] ?: 300,
                primaryTripleClickTimeoutMs = preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_TIMEOUT] ?: 350,
                primaryLongPressDurationMs = preferences[PreferencesKeys.PRIMARY_LONG_PRESS_DURATION] ?: 500,
                secondaryDoubleClickTimeoutMs = preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_TIMEOUT] ?: 300,
                secondaryTripleClickTimeoutMs = preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_TIMEOUT] ?: 350,
                secondaryLongPressDurationMs = preferences[PreferencesKeys.SECONDARY_LONG_PRESS_DURATION] ?: 500,
                primaryAppPackage = preferences[PreferencesKeys.PRIMARY_APP_PACKAGE] ?: "",
                secondaryAppPackage = preferences[PreferencesKeys.SECONDARY_APP_PACKAGE] ?: "",
                deviceNameFilter = preferences[PreferencesKeys.DEVICE_NAME_FILTER] ?: "Pen",
                airCommandEnabled = preferences[PreferencesKeys.AIR_COMMAND_ENABLED] ?: false,
                showEdgeHandle = preferences[PreferencesKeys.SHOW_EDGE_HANDLE] ?: true,
                airCommandScale = preferences[PreferencesKeys.AIR_COMMAND_SCALE] ?: 1.0f,
                airCommandOpacity = preferences[PreferencesKeys.AIR_COMMAND_OPACITY] ?: 1.0f,
                airCommandApps = preferences[PreferencesKeys.AIR_COMMAND_APPS] ?: "",
                positionPresetsJson = preferences[PreferencesKeys.POSITION_PRESETS_JSON] ?: ""
            )
            saveFastCacheSettings(context, settings)
            settings
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
            saveFastCacheProfiles(context, json)
            RemapperBackupManager.parseAppProfilesJson(json)
        }

    suspend fun saveAppProfiles(profiles: List<AppProfile>) {
        val json = RemapperBackupManager.serializeAppProfilesJson(profiles)
        saveFastCacheProfiles(context, json)
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
            val newJson = RemapperBackupManager.serializeAppProfilesJson(currentList)
            saveFastCacheProfiles(context, newJson)
            preferences[PreferencesKeys.APP_PROFILES_JSON] = newJson
        }
    }

    suspend fun deleteAppProfile(packageName: String) {
        context.remapperDataStore.edit { preferences ->
            val json = preferences[PreferencesKeys.APP_PROFILES_JSON] ?: ""
            val currentList = RemapperBackupManager.parseAppProfilesJson(json).filter { it.packageName != packageName }
            val newJson = RemapperBackupManager.serializeAppProfilesJson(currentList)
            saveFastCacheProfiles(context, newJson)
            preferences[PreferencesKeys.APP_PROFILES_JSON] = newJson
        }
    }

    suspend fun saveAllSettings(settings: RemapperSettings) {
        saveFastCacheSettings(context, settings)
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
            preferences[PreferencesKeys.USE_CUSTOM_GESTURE_TIMEOUTS] = settings.useCustomGestureTimeouts
            preferences[PreferencesKeys.USE_CUSTOM_GESTURE_POSITIONS] = settings.useCustomGesturePositions

            preferences[PreferencesKeys.PRIMARY_CLICK_CUSTOM_POS_1] = settings.primaryClickCustomPos1
            preferences[PreferencesKeys.PRIMARY_CLICK_CUSTOM_POS_2] = settings.primaryClickCustomPos2
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_CUSTOM_POS_1] = settings.primaryDoubleClickCustomPos1
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_CUSTOM_POS_2] = settings.primaryDoubleClickCustomPos2
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_CUSTOM_POS_1] = settings.primaryTripleClickCustomPos1
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_CUSTOM_POS_2] = settings.primaryTripleClickCustomPos2
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_CUSTOM_POS_1] = settings.primaryLongPressCustomPos1
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_CUSTOM_POS_2] = settings.primaryLongPressCustomPos2

            preferences[PreferencesKeys.SECONDARY_CLICK_CUSTOM_POS_1] = settings.secondaryClickCustomPos1
            preferences[PreferencesKeys.SECONDARY_CLICK_CUSTOM_POS_2] = settings.secondaryClickCustomPos2
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_CUSTOM_POS_1] = settings.secondaryDoubleClickCustomPos1
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_CUSTOM_POS_2] = settings.secondaryDoubleClickCustomPos2
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_CUSTOM_POS_1] = settings.secondaryTripleClickCustomPos1
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_CUSTOM_POS_2] = settings.secondaryTripleClickCustomPos2
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_CUSTOM_POS_1] = settings.secondaryLongPressCustomPos1
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_CUSTOM_POS_2] = settings.secondaryLongPressCustomPos2

            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_TIMEOUT] = settings.primaryDoubleClickTimeoutMs
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_TIMEOUT] = settings.primaryTripleClickTimeoutMs
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_DURATION] = settings.primaryLongPressDurationMs
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_TIMEOUT] = settings.secondaryDoubleClickTimeoutMs
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_TIMEOUT] = settings.secondaryTripleClickTimeoutMs
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_DURATION] = settings.secondaryLongPressDurationMs
            preferences[PreferencesKeys.PRIMARY_APP_PACKAGE] = settings.primaryAppPackage
            preferences[PreferencesKeys.SECONDARY_APP_PACKAGE] = settings.secondaryAppPackage
            preferences[PreferencesKeys.DEVICE_NAME_FILTER] = settings.deviceNameFilter
            
            preferences[PreferencesKeys.AIR_COMMAND_ENABLED] = settings.airCommandEnabled
            preferences[PreferencesKeys.SHOW_EDGE_HANDLE] = settings.showEdgeHandle
            preferences[PreferencesKeys.AIR_COMMAND_SCALE] = settings.airCommandScale
            preferences[PreferencesKeys.AIR_COMMAND_OPACITY] = settings.airCommandOpacity
            preferences[PreferencesKeys.AIR_COMMAND_APPS] = settings.airCommandApps
            preferences[PreferencesKeys.POSITION_PRESETS_JSON] = settings.positionPresetsJson
        }
    }

    suspend fun setRemapperEnabled(enabled: Boolean) {
        try {
            val prefs = context.getSharedPreferences("remapper_fast_cache", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("remapper_enabled", enabled).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.REMAPPER_ENABLED] = enabled
        }
    }

    suspend fun setAirCommandApps(apps: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.AIR_COMMAND_APPS] = apps
        }
    }

    suspend fun setPositionPresetsJson(json: String) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.POSITION_PRESETS_JSON] = json
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

    suspend fun setUseCustomGestureTimeouts(useCustom: Boolean) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_CUSTOM_GESTURE_TIMEOUTS] = useCustom
        }
    }

    suspend fun setUseCustomGesturePositions(useCustom: Boolean) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_CUSTOM_GESTURE_POSITIONS] = useCustom
        }
    }

    suspend fun setPrimaryDoubleClickTimeoutMs(timeoutMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_TIMEOUT] = timeoutMs
        }
    }

    suspend fun setPrimaryTripleClickTimeoutMs(timeoutMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_TIMEOUT] = timeoutMs
        }
    }

    suspend fun setPrimaryLongPressDurationMs(durationMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIMARY_LONG_PRESS_DURATION] = durationMs
        }
    }

    suspend fun setSecondaryDoubleClickTimeoutMs(timeoutMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_TIMEOUT] = timeoutMs
        }
    }

    suspend fun setSecondaryTripleClickTimeoutMs(timeoutMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_TIMEOUT] = timeoutMs
        }
    }

    suspend fun setSecondaryLongPressDurationMs(durationMs: Int) {
        context.remapperDataStore.edit { preferences ->
            preferences[PreferencesKeys.SECONDARY_LONG_PRESS_DURATION] = durationMs
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

    suspend fun setSlotCustomPos(slot: ActionTargetSlot, enabled: Boolean) {
        context.remapperDataStore.edit { preferences ->
            when (slot) {
                ActionTargetSlot.PRIMARY_CLICK_1 -> preferences[PreferencesKeys.PRIMARY_CLICK_CUSTOM_POS_1] = enabled
                ActionTargetSlot.PRIMARY_CLICK_2 -> preferences[PreferencesKeys.PRIMARY_CLICK_CUSTOM_POS_2] = enabled
                ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1 -> preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_CUSTOM_POS_1] = enabled
                ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2 -> preferences[PreferencesKeys.PRIMARY_DOUBLE_CLICK_CUSTOM_POS_2] = enabled
                ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1 -> preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_CUSTOM_POS_1] = enabled
                ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2 -> preferences[PreferencesKeys.PRIMARY_TRIPLE_CLICK_CUSTOM_POS_2] = enabled
                ActionTargetSlot.PRIMARY_LONG_PRESS_1 -> preferences[PreferencesKeys.PRIMARY_LONG_PRESS_CUSTOM_POS_1] = enabled
                ActionTargetSlot.PRIMARY_LONG_PRESS_2 -> preferences[PreferencesKeys.PRIMARY_LONG_PRESS_CUSTOM_POS_2] = enabled

                ActionTargetSlot.SECONDARY_CLICK_1 -> preferences[PreferencesKeys.SECONDARY_CLICK_CUSTOM_POS_1] = enabled
                ActionTargetSlot.SECONDARY_CLICK_2 -> preferences[PreferencesKeys.SECONDARY_CLICK_CUSTOM_POS_2] = enabled
                ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1 -> preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_CUSTOM_POS_1] = enabled
                ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2 -> preferences[PreferencesKeys.SECONDARY_DOUBLE_CLICK_CUSTOM_POS_2] = enabled
                ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1 -> preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_CUSTOM_POS_1] = enabled
                ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2 -> preferences[PreferencesKeys.SECONDARY_TRIPLE_CLICK_CUSTOM_POS_2] = enabled
                ActionTargetSlot.SECONDARY_LONG_PRESS_1 -> preferences[PreferencesKeys.SECONDARY_LONG_PRESS_CUSTOM_POS_1] = enabled
                ActionTargetSlot.SECONDARY_LONG_PRESS_2 -> preferences[PreferencesKeys.SECONDARY_LONG_PRESS_CUSTOM_POS_2] = enabled
            }
        }
    }
}
