package com.yaawaaa.penbatteryremapper.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartButton
import com.yaawaaa.penbatteryremapper.service.AirCommandController
import com.yaawaaa.penbatteryremapper.service.AirCommandOverlayService
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yaawaaa.penbatteryremapper.remapper.ActionExecutor
import com.yaawaaa.penbatteryremapper.remapper.AppInfo
import com.yaawaaa.penbatteryremapper.remapper.AppProfile
import com.yaawaaa.penbatteryremapper.remapper.EngineMode
import com.yaawaaa.penbatteryremapper.remapper.KeyEventInfo
import com.yaawaaa.penbatteryremapper.remapper.RemapAction
import com.yaawaaa.penbatteryremapper.remapper.RemapperBackupManager
import com.yaawaaa.penbatteryremapper.service.PenRemapperAccessibilityService
import kotlinx.coroutines.delay
import com.yaawaaa.penbatteryremapper.remapper.RemapperSettings
import com.yaawaaa.penbatteryremapper.remapper.RemapperViewModel
import com.yaawaaa.penbatteryremapper.remapper.rememberAppIcon
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun isVisualPositionAction(action: RemapAction): Boolean {
    return action == RemapAction.SCREEN_TAP ||
           action == RemapAction.SCREEN_TAP_LEFT ||
           action == RemapAction.SCREEN_TAP_RIGHT ||
           action == RemapAction.SCREEN_DOUBLE_TAP_LEFT ||
           action == RemapAction.SCREEN_DOUBLE_TAP_RIGHT ||
           action == RemapAction.SCREEN_LONG_PRESS_LEFT ||
           action == RemapAction.SCREEN_LONG_PRESS_RIGHT
}

enum class ActionTargetSlot {
    PRIMARY_CLICK_1,
    PRIMARY_CLICK_2,
    PRIMARY_DOUBLE_CLICK_1,
    PRIMARY_DOUBLE_CLICK_2,
    PRIMARY_TRIPLE_CLICK_1,
    PRIMARY_TRIPLE_CLICK_2,
    PRIMARY_LONG_PRESS_1,
    PRIMARY_LONG_PRESS_2,

    SECONDARY_CLICK_1,
    SECONDARY_CLICK_2,
    SECONDARY_DOUBLE_CLICK_1,
    SECONDARY_DOUBLE_CLICK_2,
    SECONDARY_TRIPLE_CLICK_1,
    SECONDARY_TRIPLE_CLICK_2,
    SECONDARY_LONG_PRESS_1,
    SECONDARY_LONG_PRESS_2
}

fun getPayloadForSlot(slot: ActionTargetSlot, settings: RemapperSettings): String {
    return when (slot) {
        ActionTargetSlot.PRIMARY_CLICK_1 -> settings.primaryClickShortcut.ifBlank { settings.primaryAppPackage }
        ActionTargetSlot.PRIMARY_CLICK_2 -> settings.primaryClickAppPackage2
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1 -> settings.primaryDoubleClickShortcut.ifBlank { settings.primaryAppPackage }
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2 -> settings.primaryDoubleClickAppPackage2
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1 -> settings.primaryTripleClickShortcut.ifBlank { settings.primaryAppPackage }
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2 -> settings.primaryTripleClickAppPackage2
        ActionTargetSlot.PRIMARY_LONG_PRESS_1 -> settings.primaryLongPressShortcut.ifBlank { settings.primaryAppPackage }
        ActionTargetSlot.PRIMARY_LONG_PRESS_2 -> settings.primaryLongPressAppPackage2

        ActionTargetSlot.SECONDARY_CLICK_1 -> settings.secondaryClickShortcut.ifBlank { settings.secondaryAppPackage }
        ActionTargetSlot.SECONDARY_CLICK_2 -> settings.secondaryClickAppPackage2
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1 -> settings.secondaryDoubleClickShortcut.ifBlank { settings.secondaryAppPackage }
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2 -> settings.secondaryDoubleClickAppPackage2
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1 -> settings.secondaryTripleClickShortcut.ifBlank { settings.secondaryAppPackage }
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2 -> settings.secondaryTripleClickAppPackage2
        ActionTargetSlot.SECONDARY_LONG_PRESS_1 -> settings.secondaryLongPressShortcut.ifBlank { settings.secondaryAppPackage }
        ActionTargetSlot.SECONDARY_LONG_PRESS_2 -> settings.secondaryLongPressAppPackage2
    }
}

fun savePayloadForSlot(slot: ActionTargetSlot, payload: String, viewModel: RemapperViewModel) {
    when (slot) {
        ActionTargetSlot.PRIMARY_CLICK_1 -> viewModel.setPrimaryClickShortcut(payload)
        ActionTargetSlot.PRIMARY_CLICK_2 -> viewModel.setPrimaryClickAppPackage2(payload)
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1 -> viewModel.setPrimaryDoubleClickShortcut(payload)
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2 -> viewModel.setPrimaryDoubleClickAppPackage2(payload)
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1 -> viewModel.setPrimaryTripleClickShortcut(payload)
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2 -> viewModel.setPrimaryTripleClickAppPackage2(payload)
        ActionTargetSlot.PRIMARY_LONG_PRESS_1 -> viewModel.setPrimaryLongPressShortcut(payload)
        ActionTargetSlot.PRIMARY_LONG_PRESS_2 -> viewModel.setPrimaryLongPressAppPackage2(payload)

        ActionTargetSlot.SECONDARY_CLICK_1 -> viewModel.setSecondaryClickShortcut(payload)
        ActionTargetSlot.SECONDARY_CLICK_2 -> viewModel.setSecondaryClickAppPackage2(payload)
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1 -> viewModel.setSecondaryDoubleClickShortcut(payload)
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2 -> viewModel.setSecondaryDoubleClickAppPackage2(payload)
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1 -> viewModel.setSecondaryTripleClickShortcut(payload)
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2 -> viewModel.setSecondaryTripleClickAppPackage2(payload)
        ActionTargetSlot.SECONDARY_LONG_PRESS_1 -> viewModel.setSecondaryLongPressShortcut(payload)
        ActionTargetSlot.SECONDARY_LONG_PRESS_2 -> viewModel.setSecondaryLongPressAppPackage2(payload)
    }
}

fun getCustomPosForSlot(slot: ActionTargetSlot, settings: RemapperSettings): Boolean {
    return when (slot) {
        ActionTargetSlot.PRIMARY_CLICK_1 -> settings.primaryClickCustomPos1
        ActionTargetSlot.PRIMARY_CLICK_2 -> settings.primaryClickCustomPos2
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1 -> settings.primaryDoubleClickCustomPos1
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2 -> settings.primaryDoubleClickCustomPos2
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1 -> settings.primaryTripleClickCustomPos1
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2 -> settings.primaryTripleClickCustomPos2
        ActionTargetSlot.PRIMARY_LONG_PRESS_1 -> settings.primaryLongPressCustomPos1
        ActionTargetSlot.PRIMARY_LONG_PRESS_2 -> settings.primaryLongPressCustomPos2

        ActionTargetSlot.SECONDARY_CLICK_1 -> settings.secondaryClickCustomPos1
        ActionTargetSlot.SECONDARY_CLICK_2 -> settings.secondaryClickCustomPos2
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1 -> settings.secondaryDoubleClickCustomPos1
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2 -> settings.secondaryDoubleClickCustomPos2
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1 -> settings.secondaryTripleClickCustomPos1
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2 -> settings.secondaryTripleClickCustomPos2
        ActionTargetSlot.SECONDARY_LONG_PRESS_1 -> settings.secondaryLongPressCustomPos1
        ActionTargetSlot.SECONDARY_LONG_PRESS_2 -> settings.secondaryLongPressCustomPos2
    }
}

fun saveCustomPosForSlot(slot: ActionTargetSlot, enabled: Boolean, viewModel: RemapperViewModel) {
    viewModel.setSlotCustomPos(slot, enabled)
}

fun setActionForSlot(slot: ActionTargetSlot, action: RemapAction, viewModel: RemapperViewModel) {
    when (slot) {
        ActionTargetSlot.PRIMARY_CLICK_1 -> viewModel.setPrimaryClickAction(action)
        ActionTargetSlot.PRIMARY_CLICK_2 -> viewModel.setPrimaryClickAction2(action)
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1 -> viewModel.setPrimaryDoubleClickAction(action)
        ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2 -> viewModel.setPrimaryDoubleClickAction2(action)
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1 -> viewModel.setPrimaryTripleClickAction(action)
        ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2 -> viewModel.setPrimaryTripleClickAction2(action)
        ActionTargetSlot.PRIMARY_LONG_PRESS_1 -> viewModel.setPrimaryLongPressAction(action)
        ActionTargetSlot.PRIMARY_LONG_PRESS_2 -> viewModel.setPrimaryLongPressAction2(action)

        ActionTargetSlot.SECONDARY_CLICK_1 -> viewModel.setSecondaryClickAction(action)
        ActionTargetSlot.SECONDARY_CLICK_2 -> viewModel.setSecondaryClickAction2(action)
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1 -> viewModel.setSecondaryDoubleClickAction(action)
        ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2 -> viewModel.setSecondaryDoubleClickAction2(action)
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1 -> viewModel.setSecondaryTripleClickAction(action)
        ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2 -> viewModel.setSecondaryTripleClickAction2(action)
        ActionTargetSlot.SECONDARY_LONG_PRESS_1 -> viewModel.setSecondaryLongPressAction(action)
        ActionTargetSlot.SECONDARY_LONG_PRESS_2 -> viewModel.setSecondaryLongPressAction2(action)
    }
}

@Composable
fun RemapperScreen(viewModel: RemapperViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    val appProfiles by viewModel.appProfiles.collectAsState()
    val isAccessibilityEnabled by viewModel.isAccessibilityServiceEnabled.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()
    val recentKeyEvent by viewModel.recentKeyEvent.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAppPickerDialog by remember { mutableStateOf(false) }
    var activeAppPickerSlot by remember { mutableStateOf<ActionTargetSlot?>(null) }

    var showCustomShortcutDialog by remember { mutableStateOf(false) }
    var activeShortcutSlot by remember { mutableStateOf<ActionTargetSlot?>(null) }
    var currentShortcutInitial by remember { mutableStateOf("") }

    var showPenEraserDialog by remember { mutableStateOf(false) }
    var activePenEraserSlot by remember { mutableStateOf<ActionTargetSlot?>(null) }
    var currentPenEraserInitial by remember { mutableStateOf("") }

    var showPositionSelectorDialog by remember { mutableStateOf(false) }
    var activePositionSlot by remember { mutableStateOf<ActionTargetSlot?>(null) }
    var currentPositionInitial by remember { mutableStateOf("") }

    var showExternalActionDialog by remember { mutableStateOf(false) }
    var activeExternalActionSlot by remember { mutableStateOf<ActionTargetSlot?>(null) }
    var currentExternalActionInitial by remember { mutableStateOf("") }

    // SAF Backup & Restore Launchers
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = RemapperBackupManager.exportBackup(settings, appProfiles)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Backup saved successfully!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to export backup: ${e.localizedMessage}")
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                val result = RemapperBackupManager.importBackup(jsonString)
                if (result != null) {
                    viewModel.restoreBackup(result.first, result.second)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Backup restored successfully!")
                    }
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Failed to import backup: Invalid backup format.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Failed to import backup: ${e.localizedMessage}")
                }
            }
        }
    }

    // Air Command Dialog State
    var showAirCommandDialog by remember { mutableStateOf(false) }

    // Per-App Profile Editor Dialog States
    var showAddProfileAppPicker by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<AppProfile?>(null) }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Buttons", "Timeouts", "Profiles")

    val handleActionChange: (ActionTargetSlot, RemapAction) -> Unit = remember(viewModel, settings) {
        { slot, newAction ->
            setActionForSlot(slot, newAction, viewModel)
            when {
                newAction == RemapAction.LAUNCH_APP -> {
                    activeAppPickerSlot = slot
                    showAppPickerDialog = true
                }
                newAction == RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> {
                    activeShortcutSlot = slot
                    currentShortcutInitial = getPayloadForSlot(slot, settings)
                    showCustomShortcutDialog = true
                }
                newAction == RemapAction.TOGGLE_PEN_ERASER -> {
                    activePenEraserSlot = slot
                    currentPenEraserInitial = getPayloadForSlot(slot, settings)
                    showPenEraserDialog = true
                }
                isVisualPositionAction(newAction) -> {
                    activePositionSlot = slot
                    currentPositionInitial = getPayloadForSlot(slot, settings)
                    showPositionSelectorDialog = true
                }
                newAction == RemapAction.LOCKSCREEN_AWAKE || newAction == RemapAction.TRIGGER_EXTERNAL_APP -> {
                    activeExternalActionSlot = slot
                    currentExternalActionInitial = getPayloadForSlot(slot, settings)
                    showExternalActionDialog = true
                }
                else -> {}
            }
        }
    }

    val handleConfigureClick: (ActionTargetSlot, RemapAction) -> Unit = remember(settings) {
        { slot, action ->
            when {
                action == RemapAction.LAUNCH_APP -> {
                    activeAppPickerSlot = slot
                    showAppPickerDialog = true
                }
                action == RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> {
                    activeShortcutSlot = slot
                    currentShortcutInitial = getPayloadForSlot(slot, settings)
                    showCustomShortcutDialog = true
                }
                action == RemapAction.TOGGLE_PEN_ERASER -> {
                    activePenEraserSlot = slot
                    currentPenEraserInitial = getPayloadForSlot(slot, settings)
                    showPenEraserDialog = true
                }
                isVisualPositionAction(action) -> {
                    activePositionSlot = slot
                    currentPositionInitial = getPayloadForSlot(slot, settings)
                    showPositionSelectorDialog = true
                }
                action == RemapAction.LOCKSCREEN_AWAKE || action == RemapAction.TRIGGER_EXTERNAL_APP -> {
                    activeExternalActionSlot = slot
                    currentExternalActionInitial = getPayloadForSlot(slot, settings)
                    showExternalActionDialog = true
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkServiceAndRootState()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.applySettings()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Settings applied successfully!")
                    }
                },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text("Apply Settings", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val shouldShowAccessibilityWarning by remember(isAccessibilityEnabled, settings.engineMode, isRootAvailable) {
                    derivedStateOf {
                        !isAccessibilityEnabled && !(settings.engineMode == EngineMode.ROOT && isRootAvailable)
                    }
                }
                if (shouldShowAccessibilityWarning) {
                    AccessibilityWarningCard(
                        onOpenSettings = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
                
                when (selectedTabIndex) {
                    0 -> {
                        // 1. Master Toggle Card
                        MasterToggleCard(
                            enabled = settings.remapperEnabled,
                            onToggle = { viewModel.toggleRemapper(it) }
                        )
            
                        // 2. Engine Selector Card & Root Status Badge
                        EngineSelectorCard(
                            currentEngine = settings.engineMode,
                            isRootAvailable = isRootAvailable,
                            onEngineSelected = { viewModel.setEngineMode(it) },
                            onEnableViaRoot = { viewModel.enableViaRoot() }
                        )

                        // 3. Air Command Sidebar Customization Card
                        AirCommandCard(
                            settings = settings,
                            onCustomizeClick = { showAirCommandDialog = true },
                            onScaleChanged = { viewModel.setAirCommandScale(it) }
                        )
            
                        // 4. Primary Button Mapping Card
                        PrimaryButtonMappingSection(
                            settings = settings,
                            installedApps = installedApps,
                            viewModel = viewModel,
                            onActionChange = handleActionChange,
                            onConfigure = handleConfigureClick
                        )
            
                        // 5. Secondary Button Mapping Card
                        SecondaryButtonMappingSection(
                            settings = settings,
                            installedApps = installedApps,
                            viewModel = viewModel,
                            onActionChange = handleActionChange,
                            onConfigure = handleConfigureClick
                        )
                        
                        // 11. Device Name Filter Card
                        DeviceFilterCard(
                            filter = settings.deviceNameFilter,
                            onFilterChanged = { viewModel.setDeviceNameFilter(it) }
                        )
            
                        // 12. Interactive Key Tester Card
                        KeyTesterCard(
                            recentKeyEvent = recentKeyEvent,
                            onClear = { viewModel.clearRecentKeyEvent() },
                            onSimulatePress = { keyCode, keyName ->
                                viewModel.simulateTestKeyPress(keyCode, keyName)
                            }
                        )
                    }
                    1 -> {
                        CustomGestureTimeoutsToggleCard(
                            useCustom = settings.useCustomGestureTimeouts,
                            onToggleChanged = { viewModel.setUseCustomGestureTimeouts(it) }
                        )

                        if (settings.useCustomGestureTimeouts) {
                            ButtonSpecificTimeoutsCard(
                                buttonName = "Primary Button (PAGE_UP)",
                                doubleClickMs = settings.primaryDoubleClickTimeoutMs,
                                onDoubleClickChanged = { viewModel.setPrimaryDoubleClickTimeoutMs(it) },
                                tripleClickMs = settings.primaryTripleClickTimeoutMs,
                                onTripleClickChanged = { viewModel.setPrimaryTripleClickTimeoutMs(it) },
                                longPressMs = settings.primaryLongPressDurationMs,
                                onLongPressChanged = { viewModel.setPrimaryLongPressDurationMs(it) }
                            )

                            ButtonSpecificTimeoutsCard(
                                buttonName = "Secondary Button (PAGE_DOWN)",
                                doubleClickMs = settings.secondaryDoubleClickTimeoutMs,
                                onDoubleClickChanged = { viewModel.setSecondaryDoubleClickTimeoutMs(it) },
                                tripleClickMs = settings.secondaryTripleClickTimeoutMs,
                                onTripleClickChanged = { viewModel.setSecondaryTripleClickTimeoutMs(it) },
                                longPressMs = settings.secondaryLongPressDurationMs,
                                onLongPressChanged = { viewModel.setSecondaryLongPressDurationMs(it) }
                            )
                        } else {
                            DoubleClickTimeoutCard(
                                timeoutMs = settings.doubleClickTimeoutMs,
                                onTimeoutChanged = { viewModel.setDoubleClickTimeoutMs(it) }
                            )

                            TripleClickTimeoutCard(
                                timeoutMs = settings.tripleClickTimeoutMs,
                                onTimeoutChanged = { viewModel.setTripleClickTimeoutMs(it) }
                            )

                            LongPressDurationCard(
                                durationMs = settings.longPressDurationMs,
                                onDurationChanged = { viewModel.setLongPressDurationMs(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        CustomGesturePositionsToggleCard(
                            useCustom = settings.useCustomGesturePositions,
                            onToggleChanged = { viewModel.setUseCustomGesturePositions(it) }
                        )
                    }
                    2 -> {
                        // 6. Per-App Profiles Section
                        PerAppProfilesSection(
                            appProfiles = appProfiles,
                            installedApps = installedApps,
                            onAddProfileClick = { showAddProfileAppPicker = true },
                            onEditProfile = { profile -> editingProfile = profile },
                            onDeleteProfile = { packageName -> viewModel.deleteAppProfile(packageName) }
                        )
            
                        // 7. Backup & Restore Feature Card
                        BackupRestoreCard(
                            onExportBackup = {
                                createDocumentLauncher.launch("pen_remapper_backup.json")
                            },
                            onImportBackup = {
                                openDocumentLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // App Picker Dialog
    if (showAppPickerDialog) {
        AppPickerDialog(
            apps = installedApps,
            onAppSelected = { appInfo ->
                activeAppPickerSlot?.let { slot ->
                    savePayloadForSlot(slot, appInfo.packageName, viewModel)
                }
                showAppPickerDialog = false
            },
            onDismiss = { showAppPickerDialog = false }
        )
    }

    // Add Profile App Picker Dialog
    if (showAddProfileAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            onAppSelected = { appInfo ->
                showAddProfileAppPicker = false
                val newProfile = AppProfile(
                    packageName = appInfo.packageName,
                    appName = appInfo.appName
                )
                editingProfile = newProfile
            },
            onDismiss = { showAddProfileAppPicker = false }
        )
    }

    // App Profile Editor Dialog
    editingProfile?.let { profileToEdit ->
        AppProfileEditorDialog(
            profile = profileToEdit,
            settings = settings,
            viewModel = viewModel,
            installedApps = installedApps,
            onSave = { updatedProfile ->
                PenRemapperAccessibilityService.updateAppProfileInMemory(updatedProfile)
                viewModel.addOrUpdateAppProfile(updatedProfile)
                editingProfile = null
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val snackbarJob = launch {
                        snackbarHostState.showSnackbar("Profile for ${updatedProfile.appName} saved!")
                    }
                    delay(1000)
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarJob.cancel()
                }
            },
            onDismiss = { editingProfile = null }
        )
    }



    // Air Command Sidebar Customization Dialog
    if (showAirCommandDialog) {
        AirCommandItemsEditorDialog(
            currentJson = settings.airCommandApps,
            installedApps = installedApps,
            onSave = { json -> viewModel.setAirCommandItemsJson(json) },
            onDismiss = { showAirCommandDialog = false }
        )
    }

    // Custom Keyboard Shortcut Dialog
    if (showCustomShortcutDialog) {
        CustomKeyboardShortcutDialog(
            initialShortcut = currentShortcutInitial,
            onShortcutSaved = { shortcut ->
                activeShortcutSlot?.let { slot ->
                    savePayloadForSlot(slot, shortcut, viewModel)
                }
                showCustomShortcutDialog = false
            },
            onDismiss = { showCustomShortcutDialog = false }
        )
    }

    // Note-Taking Pen/Eraser Preset Dialog
    if (showPenEraserDialog) {
        PenEraserPresetDialog(
            initialPreset = currentPenEraserInitial,
            onPresetSaved = { preset ->
                activePenEraserSlot?.let { slot ->
                    savePayloadForSlot(slot, preset, viewModel)
                }
                showPenEraserDialog = false
            },
            onDismiss = { showPenEraserDialog = false }
        )
    }

    // Visual Position Selector Dialog
    if (showPositionSelectorDialog) {
        PositionSelectorDialog(
            initialCoords = currentPositionInitial,
            initialPresetsJson = settings.positionPresetsJson,
            onPositionSaved = { coords ->
                activePositionSlot?.let { slot ->
                    savePayloadForSlot(slot, coords, viewModel)
                    saveCustomPosForSlot(slot, true, viewModel)
                }
                showPositionSelectorDialog = false
            },
            onSavePresetsJson = { viewModel.setPositionPresetsJson(it) },
            onDismiss = { showPositionSelectorDialog = false }
        )
    }

    // External App Action Dialog
    if (showExternalActionDialog) {
        ExternalActionDialog(
            initialValue = currentExternalActionInitial,
            onSaved = { payload ->
                activeExternalActionSlot?.let { slot ->
                    savePayloadForSlot(slot, payload, viewModel)
                }
                showExternalActionDialog = false
            },
            onDismiss = { showExternalActionDialog = false }
        )
    }
}

@Composable
fun BackupRestoreCard(
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Export or import your remapper settings, gesture shortcuts, and per-app profiles via JSON.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExportBackup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Backup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onImportBackup,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Backup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun PerAppProfilesSection(
    appProfiles: List<AppProfile>,
    installedApps: List<AppInfo>,
    onAddProfileClick: () -> Unit,
    onEditProfile: (AppProfile) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Per-App Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onAddProfileClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Profile", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Automatically activates custom gesture mappings when specific apps are open in foreground.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (appProfiles.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No app profiles configured. Tap '+ Add Profile' to create custom mappings for apps like GoodNotes, Concepts, Squid, or OneNote.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    appProfiles.forEach { profile ->
                        PerAppProfileItemRow(
                            profile = profile,
                            installedApps = installedApps,
                            onEditProfile = onEditProfile,
                            onDeleteProfile = onDeleteProfile
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PerAppProfileItemRow(
    profile: AppProfile,
    installedApps: List<AppInfo>,
    onEditProfile: (AppProfile) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    val matchingApp = remember(profile.packageName, installedApps) {
        installedApps.find { it.packageName == profile.packageName }
    }
    val appBitmap = rememberAppIcon(profile.packageName)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appBitmap != null) {
                Image(
                    bitmap = appBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.appName.ifBlank { matchingApp?.appName ?: profile.packageName },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = profile.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { onEditProfile(profile) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = { onDeleteProfile(profile.packageName) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Profile",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AppProfileEditorDialog(
    profile: AppProfile,
    settings: RemapperSettings,
    viewModel: RemapperViewModel,
    installedApps: List<AppInfo>,
    onSave: (AppProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var editedProfile by remember(profile) { mutableStateOf(profile) }

    var configuringSlot by remember { mutableStateOf<String?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showShortcutDialog by remember { mutableStateOf(false) }
    var showPenEraserDialog by remember { mutableStateOf(false) }
    var showPositionSelectorDialog by remember { mutableStateOf(false) }
    var showExternalActionDialog by remember { mutableStateOf(false) }
    var currentShortcutInitial by remember { mutableStateOf("") }
    var currentPresetInitial by remember { mutableStateOf("") }
    var currentPositionInitial by remember { mutableStateOf("") }
    var currentExternalActionInitial by remember { mutableStateOf("") }

    fun getPayloadForProfileSlot(slot: String): String {
        return when (slot) {
            "primary_click_1" -> editedProfile.primaryClickShortcut
            "primary_click_2" -> editedProfile.primaryClickAppPackage2
            "primary_double_click_1" -> editedProfile.primaryDoubleClickShortcut
            "primary_double_click_2" -> editedProfile.primaryDoubleClickAppPackage2
            "primary_triple_click_1" -> editedProfile.primaryTripleClickShortcut
            "primary_triple_click_2" -> editedProfile.primaryTripleClickAppPackage2
            "primary_long_press_1" -> editedProfile.primaryLongPressShortcut
            "primary_long_press_2" -> editedProfile.primaryLongPressAppPackage2

            "secondary_click_1" -> editedProfile.secondaryClickShortcut
            "secondary_click_2" -> editedProfile.secondaryClickAppPackage2
            "secondary_double_click_1" -> editedProfile.secondaryDoubleClickShortcut
            "secondary_double_click_2" -> editedProfile.secondaryDoubleClickAppPackage2
            "secondary_triple_click_1" -> editedProfile.secondaryTripleClickShortcut
            "secondary_triple_click_2" -> editedProfile.secondaryTripleClickAppPackage2
            "secondary_long_press_1" -> editedProfile.secondaryLongPressShortcut
            "secondary_long_press_2" -> editedProfile.secondaryLongPressAppPackage2
            else -> ""
        }
    }

    fun updatePayloadForProfileSlot(slot: String, newValue: String) {
        editedProfile = when (slot) {
            "primary_click_1" -> editedProfile.copy(primaryClickShortcut = newValue)
            "primary_click_2" -> editedProfile.copy(primaryClickAppPackage2 = newValue)
            "primary_double_click_1" -> editedProfile.copy(primaryDoubleClickShortcut = newValue)
            "primary_double_click_2" -> editedProfile.copy(primaryDoubleClickAppPackage2 = newValue)
            "primary_triple_click_1" -> editedProfile.copy(primaryTripleClickShortcut = newValue)
            "primary_triple_click_2" -> editedProfile.copy(primaryTripleClickAppPackage2 = newValue)
            "primary_long_press_1" -> editedProfile.copy(primaryLongPressShortcut = newValue)
            "primary_long_press_2" -> editedProfile.copy(primaryLongPressAppPackage2 = newValue)

            "secondary_click_1" -> editedProfile.copy(secondaryClickShortcut = newValue)
            "secondary_click_2" -> editedProfile.copy(secondaryClickAppPackage2 = newValue)
            "secondary_double_click_1" -> editedProfile.copy(secondaryDoubleClickShortcut = newValue)
            "secondary_double_click_2" -> editedProfile.copy(secondaryDoubleClickAppPackage2 = newValue)
            "secondary_triple_click_1" -> editedProfile.copy(secondaryTripleClickShortcut = newValue)
            "secondary_triple_click_2" -> editedProfile.copy(secondaryTripleClickAppPackage2 = newValue)
            "secondary_long_press_1" -> editedProfile.copy(secondaryLongPressShortcut = newValue)
            "secondary_long_press_2" -> editedProfile.copy(secondaryLongPressAppPackage2 = newValue)
            else -> editedProfile
        }
    }

    fun updateCustomPosForProfileSlot(slot: String, enabled: Boolean) {
        editedProfile = when (slot) {
            "primary_click_1" -> editedProfile.copy(primaryClickCustomPos1 = enabled)
            "primary_click_2" -> editedProfile.copy(primaryClickCustomPos2 = enabled)
            "primary_double_click_1" -> editedProfile.copy(primaryDoubleClickCustomPos1 = enabled)
            "primary_double_click_2" -> editedProfile.copy(primaryDoubleClickCustomPos2 = enabled)
            "primary_triple_click_1" -> editedProfile.copy(primaryTripleClickCustomPos1 = enabled)
            "primary_triple_click_2" -> editedProfile.copy(primaryTripleClickCustomPos2 = enabled)
            "primary_long_press_1" -> editedProfile.copy(primaryLongPressCustomPos1 = enabled)
            "primary_long_press_2" -> editedProfile.copy(primaryLongPressCustomPos2 = enabled)

            "secondary_click_1" -> editedProfile.copy(secondaryClickCustomPos1 = enabled)
            "secondary_click_2" -> editedProfile.copy(secondaryClickCustomPos2 = enabled)
            "secondary_double_click_1" -> editedProfile.copy(secondaryDoubleClickCustomPos1 = enabled)
            "secondary_double_click_2" -> editedProfile.copy(secondaryDoubleClickCustomPos2 = enabled)
            "secondary_triple_click_1" -> editedProfile.copy(secondaryTripleClickCustomPos1 = enabled)
            "secondary_triple_click_2" -> editedProfile.copy(secondaryTripleClickCustomPos2 = enabled)
            "secondary_long_press_1" -> editedProfile.copy(secondaryLongPressCustomPos1 = enabled)
            "secondary_long_press_2" -> editedProfile.copy(secondaryLongPressCustomPos2 = enabled)
            else -> editedProfile
        }
    }

    fun openConfigureSlot(slot: String, action: RemapAction) {
        configuringSlot = slot
        when {
            action == RemapAction.LAUNCH_APP -> {
                showAppPicker = true
            }
            action == RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> {
                currentShortcutInitial = getPayloadForProfileSlot(slot)
                showShortcutDialog = true
            }
            action == RemapAction.TOGGLE_PEN_ERASER -> {
                currentPresetInitial = getPayloadForProfileSlot(slot)
                showPenEraserDialog = true
            }
            isVisualPositionAction(action) -> {
                currentPositionInitial = getPayloadForProfileSlot(slot)
                showPositionSelectorDialog = true
            }
            action == RemapAction.LOCKSCREEN_AWAKE || action == RemapAction.TRIGGER_EXTERNAL_APP -> {
                currentExternalActionInitial = getPayloadForProfileSlot(slot)
                showExternalActionDialog = true
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Profile: ${editedProfile.appName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configure custom gesture mappings when ${editedProfile.appName} is active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Primary Button
                Text(
                    text = "Primary Button (Page Up / 92)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                GestureActionConfigRow(
                    title = "Single Click",
                    action1 = editedProfile.primaryClickAction,
                    action2 = editedProfile.primaryClickAction2,
                    payload1 = editedProfile.primaryClickShortcut,
                    payload2 = editedProfile.primaryClickAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.primaryClickCustomPos1,
                    useCustomPositions2 = editedProfile.primaryClickCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(primaryClickCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(primaryClickCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryClickAction = newAction)
                        openConfigureSlot("primary_click_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryClickAction2 = newAction)
                        openConfigureSlot("primary_click_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("primary_click_1", editedProfile.primaryClickAction) },
                    onConfigure2 = { openConfigureSlot("primary_click_2", editedProfile.primaryClickAction2) }
                )

                GestureActionConfigRow(
                    title = "Double Click",
                    action1 = editedProfile.primaryDoubleClickAction,
                    action2 = editedProfile.primaryDoubleClickAction2,
                    payload1 = editedProfile.primaryDoubleClickShortcut,
                    payload2 = editedProfile.primaryDoubleClickAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.primaryDoubleClickCustomPos1,
                    useCustomPositions2 = editedProfile.primaryDoubleClickCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(primaryDoubleClickCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(primaryDoubleClickCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryDoubleClickAction = newAction)
                        openConfigureSlot("primary_double_click_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryDoubleClickAction2 = newAction)
                        openConfigureSlot("primary_double_click_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("primary_double_click_1", editedProfile.primaryDoubleClickAction) },
                    onConfigure2 = { openConfigureSlot("primary_double_click_2", editedProfile.primaryDoubleClickAction2) }
                )

                GestureActionConfigRow(
                    title = "Triple Click",
                    action1 = editedProfile.primaryTripleClickAction,
                    action2 = editedProfile.primaryTripleClickAction2,
                    payload1 = editedProfile.primaryTripleClickShortcut,
                    payload2 = editedProfile.primaryTripleClickAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.primaryTripleClickCustomPos1,
                    useCustomPositions2 = editedProfile.primaryTripleClickCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(primaryTripleClickCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(primaryTripleClickCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryTripleClickAction = newAction)
                        openConfigureSlot("primary_triple_click_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryTripleClickAction2 = newAction)
                        openConfigureSlot("primary_triple_click_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("primary_triple_click_1", editedProfile.primaryTripleClickAction) },
                    onConfigure2 = { openConfigureSlot("primary_triple_click_2", editedProfile.primaryTripleClickAction2) }
                )

                GestureActionConfigRow(
                    title = "Long Press",
                    action1 = editedProfile.primaryLongPressAction,
                    action2 = editedProfile.primaryLongPressAction2,
                    payload1 = editedProfile.primaryLongPressShortcut,
                    payload2 = editedProfile.primaryLongPressAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.primaryLongPressCustomPos1,
                    useCustomPositions2 = editedProfile.primaryLongPressCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(primaryLongPressCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(primaryLongPressCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryLongPressAction = newAction)
                        openConfigureSlot("primary_long_press_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(primaryLongPressAction2 = newAction)
                        openConfigureSlot("primary_long_press_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("primary_long_press_1", editedProfile.primaryLongPressAction) },
                    onConfigure2 = { openConfigureSlot("primary_long_press_2", editedProfile.primaryLongPressAction2) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Button
                Text(
                    text = "Secondary Button (Page Down / 93)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                GestureActionConfigRow(
                    title = "Single Click",
                    action1 = editedProfile.secondaryClickAction,
                    action2 = editedProfile.secondaryClickAction2,
                    payload1 = editedProfile.secondaryClickShortcut,
                    payload2 = editedProfile.secondaryClickAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.secondaryClickCustomPos1,
                    useCustomPositions2 = editedProfile.secondaryClickCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(secondaryClickCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(secondaryClickCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryClickAction = newAction)
                        openConfigureSlot("secondary_click_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryClickAction2 = newAction)
                        openConfigureSlot("secondary_click_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("secondary_click_1", editedProfile.secondaryClickAction) },
                    onConfigure2 = { openConfigureSlot("secondary_click_2", editedProfile.secondaryClickAction2) }
                )

                GestureActionConfigRow(
                    title = "Double Click",
                    action1 = editedProfile.secondaryDoubleClickAction,
                    action2 = editedProfile.secondaryDoubleClickAction2,
                    payload1 = editedProfile.secondaryDoubleClickShortcut,
                    payload2 = editedProfile.secondaryDoubleClickAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.secondaryDoubleClickCustomPos1,
                    useCustomPositions2 = editedProfile.secondaryDoubleClickCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(secondaryDoubleClickCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(secondaryDoubleClickCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryDoubleClickAction = newAction)
                        openConfigureSlot("secondary_double_click_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryDoubleClickAction2 = newAction)
                        openConfigureSlot("secondary_double_click_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("secondary_double_click_1", editedProfile.secondaryDoubleClickAction) },
                    onConfigure2 = { openConfigureSlot("secondary_double_click_2", editedProfile.secondaryDoubleClickAction2) }
                )

                GestureActionConfigRow(
                    title = "Triple Click",
                    action1 = editedProfile.secondaryTripleClickAction,
                    action2 = editedProfile.secondaryTripleClickAction2,
                    payload1 = editedProfile.secondaryTripleClickShortcut,
                    payload2 = editedProfile.secondaryTripleClickAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.secondaryTripleClickCustomPos1,
                    useCustomPositions2 = editedProfile.secondaryTripleClickCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(secondaryTripleClickCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(secondaryTripleClickCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryTripleClickAction = newAction)
                        openConfigureSlot("secondary_triple_click_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryTripleClickAction2 = newAction)
                        openConfigureSlot("secondary_triple_click_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("secondary_triple_click_1", editedProfile.secondaryTripleClickAction) },
                    onConfigure2 = { openConfigureSlot("secondary_triple_click_2", editedProfile.secondaryTripleClickAction2) }
                )

                GestureActionConfigRow(
                    title = "Long Press",
                    action1 = editedProfile.secondaryLongPressAction,
                    action2 = editedProfile.secondaryLongPressAction2,
                    payload1 = editedProfile.secondaryLongPressShortcut,
                    payload2 = editedProfile.secondaryLongPressAppPackage2,
                    installedApps = installedApps,
                    useCustomPositions1 = editedProfile.secondaryLongPressCustomPos1,
                    useCustomPositions2 = editedProfile.secondaryLongPressCustomPos2,
                    onToggleCustomPositions1 = { editedProfile = editedProfile.copy(secondaryLongPressCustomPos1 = it) },
                    onToggleCustomPositions2 = { editedProfile = editedProfile.copy(secondaryLongPressCustomPos2 = it) },
                    onAction1Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryLongPressAction = newAction)
                        openConfigureSlot("secondary_long_press_1", newAction)
                    },
                    onAction2Changed = { newAction ->
                        editedProfile = editedProfile.copy(secondaryLongPressAction2 = newAction)
                        openConfigureSlot("secondary_long_press_2", newAction)
                    },
                    onConfigure1 = { openConfigureSlot("secondary_long_press_1", editedProfile.secondaryLongPressAction) },
                    onConfigure2 = { openConfigureSlot("secondary_long_press_2", editedProfile.secondaryLongPressAction2) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(editedProfile)
                    onDismiss()
                }
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAppPicker) {
        AppPickerDialog(
            apps = installedApps,
            onAppSelected = { appInfo ->
                configuringSlot?.let { slot ->
                    updatePayloadForProfileSlot(slot, appInfo.packageName)
                }
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showShortcutDialog) {
        CustomKeyboardShortcutDialog(
            initialShortcut = currentShortcutInitial,
            onShortcutSaved = { shortcut ->
                configuringSlot?.let { slot ->
                    updatePayloadForProfileSlot(slot, shortcut)
                }
                showShortcutDialog = false
            },
            onDismiss = { showShortcutDialog = false }
        )
    }

    if (showPenEraserDialog) {
        PenEraserPresetDialog(
            initialPreset = currentPresetInitial,
            onPresetSaved = { preset ->
                configuringSlot?.let { slot ->
                    updatePayloadForProfileSlot(slot, preset)
                }
                showPenEraserDialog = false
            },
            onDismiss = { showPenEraserDialog = false }
        )
    }

    if (showPositionSelectorDialog) {
        PositionSelectorDialog(
            initialCoords = currentPositionInitial,
            initialPresetsJson = settings.positionPresetsJson,
            onPositionSaved = { coords ->
                configuringSlot?.let { slot ->
                    updatePayloadForProfileSlot(slot, coords)
                    updateCustomPosForProfileSlot(slot, true)
                }
                showPositionSelectorDialog = false
            },
            onSavePresetsJson = { viewModel.setPositionPresetsJson(it) },
            onDismiss = { showPositionSelectorDialog = false }
        )
    }

    if (showExternalActionDialog) {
        ExternalActionDialog(
            initialValue = currentExternalActionInitial,
            onSaved = { payload ->
                configuringSlot?.let { slot ->
                    updatePayloadForProfileSlot(slot, payload)
                }
                showExternalActionDialog = false
            },
            onDismiss = { showExternalActionDialog = false }
        )
    }
}

@Composable
fun PrimaryButtonMappingSection(
    settings: RemapperSettings,
    installedApps: List<AppInfo>,
    viewModel: RemapperViewModel,
    onActionChange: (ActionTargetSlot, RemapAction) -> Unit,
    onConfigure: (ActionTargetSlot, RemapAction) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Primary Button (Page Up / 92)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Xiaomi Stylus Pen Top Hardware Button",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Single Click
            GestureActionConfigRow(
                title = "Single Click",
                action1 = settings.primaryClickAction,
                action2 = settings.primaryClickAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.PRIMARY_CLICK_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.PRIMARY_CLICK_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_CLICK_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_CLICK_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_CLICK_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_CLICK_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.PRIMARY_CLICK_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.PRIMARY_CLICK_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.PRIMARY_CLICK_1, settings.primaryClickAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.PRIMARY_CLICK_2, settings.primaryClickAction2) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Double Click
            GestureActionConfigRow(
                title = "Double Click (within ${settings.doubleClickTimeoutMs}ms)",
                action1 = settings.primaryDoubleClickAction,
                action2 = settings.primaryDoubleClickAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_1, settings.primaryDoubleClickAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.PRIMARY_DOUBLE_CLICK_2, settings.primaryDoubleClickAction2) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Triple Click
            GestureActionConfigRow(
                title = "Triple Click (within ${settings.tripleClickTimeoutMs}ms)",
                action1 = settings.primaryTripleClickAction,
                action2 = settings.primaryTripleClickAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_1, settings.primaryTripleClickAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.PRIMARY_TRIPLE_CLICK_2, settings.primaryTripleClickAction2) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Long Press
            GestureActionConfigRow(
                title = "Long Press (${settings.longPressDurationMs}ms)",
                action1 = settings.primaryLongPressAction,
                action2 = settings.primaryLongPressAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.PRIMARY_LONG_PRESS_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.PRIMARY_LONG_PRESS_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_LONG_PRESS_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.PRIMARY_LONG_PRESS_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_LONG_PRESS_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.PRIMARY_LONG_PRESS_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.PRIMARY_LONG_PRESS_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.PRIMARY_LONG_PRESS_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.PRIMARY_LONG_PRESS_1, settings.primaryLongPressAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.PRIMARY_LONG_PRESS_2, settings.primaryLongPressAction2) }
            )
        }
    }
}

@Composable
fun SecondaryButtonMappingSection(
    settings: RemapperSettings,
    installedApps: List<AppInfo>,
    viewModel: RemapperViewModel,
    onActionChange: (ActionTargetSlot, RemapAction) -> Unit,
    onConfigure: (ActionTargetSlot, RemapAction) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Secondary Button (Page Down / 93)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Xiaomi Stylus Pen Bottom Hardware Button",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Single Click
            GestureActionConfigRow(
                title = "Single Click",
                action1 = settings.secondaryClickAction,
                action2 = settings.secondaryClickAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.SECONDARY_CLICK_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.SECONDARY_CLICK_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_CLICK_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_CLICK_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_CLICK_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_CLICK_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.SECONDARY_CLICK_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.SECONDARY_CLICK_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.SECONDARY_CLICK_1, settings.secondaryClickAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.SECONDARY_CLICK_2, settings.secondaryClickAction2) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Double Click
            GestureActionConfigRow(
                title = "Double Click (within ${settings.doubleClickTimeoutMs}ms)",
                action1 = settings.secondaryDoubleClickAction,
                action2 = settings.secondaryDoubleClickAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_1, settings.secondaryDoubleClickAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.SECONDARY_DOUBLE_CLICK_2, settings.secondaryDoubleClickAction2) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Triple Click
            GestureActionConfigRow(
                title = "Triple Click (within ${settings.tripleClickTimeoutMs}ms)",
                action1 = settings.secondaryTripleClickAction,
                action2 = settings.secondaryTripleClickAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_1, settings.secondaryTripleClickAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.SECONDARY_TRIPLE_CLICK_2, settings.secondaryTripleClickAction2) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Long Press
            GestureActionConfigRow(
                title = "Long Press (${settings.longPressDurationMs}ms)",
                action1 = settings.secondaryLongPressAction,
                action2 = settings.secondaryLongPressAction2,
                payload1 = getPayloadForSlot(ActionTargetSlot.SECONDARY_LONG_PRESS_1, settings),
                payload2 = getPayloadForSlot(ActionTargetSlot.SECONDARY_LONG_PRESS_2, settings),
                installedApps = installedApps,
                useCustomPositions1 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_LONG_PRESS_1, settings),
                useCustomPositions2 = getCustomPosForSlot(ActionTargetSlot.SECONDARY_LONG_PRESS_2, settings),
                onToggleCustomPositions1 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_LONG_PRESS_1, it, viewModel) },
                onToggleCustomPositions2 = { saveCustomPosForSlot(ActionTargetSlot.SECONDARY_LONG_PRESS_2, it, viewModel) },
                onAction1Changed = { onActionChange(ActionTargetSlot.SECONDARY_LONG_PRESS_1, it) },
                onAction2Changed = { onActionChange(ActionTargetSlot.SECONDARY_LONG_PRESS_2, it) },
                onConfigure1 = { onConfigure(ActionTargetSlot.SECONDARY_LONG_PRESS_1, settings.secondaryLongPressAction) },
                onConfigure2 = { onConfigure(ActionTargetSlot.SECONDARY_LONG_PRESS_2, settings.secondaryLongPressAction2) }
            )
        }
    }
}

@Composable
fun GestureActionConfigRow(
    title: String,
    action1: RemapAction,
    action2: RemapAction,
    payload1: String,
    payload2: String,
    installedApps: List<AppInfo>,
    useCustomPositions1: Boolean = true,
    useCustomPositions2: Boolean = true,
    onToggleCustomPositions1: ((Boolean) -> Unit)? = null,
    onToggleCustomPositions2: ((Boolean) -> Unit)? = null,
    onAction1Changed: (RemapAction) -> Unit,
    onAction2Changed: (RemapAction) -> Unit,
    onConfigure1: () -> Unit,
    onConfigure2: () -> Unit
) {
    var isDualActionExpanded by remember(action2) {
        mutableStateOf(action2 != RemapAction.PASSTHROUGH)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (action1 != RemapAction.PASSTHROUGH || action2 != RemapAction.PASSTHROUGH) {
                IconButton(
                    onClick = {
                        onAction1Changed(RemapAction.PASSTHROUGH)
                        onAction2Changed(RemapAction.PASSTHROUGH)
                        isDualActionExpanded = false
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Gesture",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action 1 Picker
        ActionDropdownPicker(
            label = "Action 1",
            selectedAction = action1,
            onActionSelected = onAction1Changed
        )

        if (action1 == RemapAction.LAUNCH_APP) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectedAppDisplayRow(
                packageName = payload1,
                installedApps = installedApps,
                onSelectApp = onConfigure1
            )
        } else if (action1 == RemapAction.CUSTOM_KEYBOARD_SHORTCUT) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectedShortcutDisplayRow(
                shortcut = payload1,
                onConfigure = onConfigure1
            )
        } else if (action1 == RemapAction.TOGGLE_PEN_ERASER) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectedPenEraserDisplayRow(
                preset = payload1,
                onConfigure = onConfigure1
            )
        } else if (isVisualPositionAction(action1)) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectedPositionDisplayRow(
                positionCoords = payload1,
                useCustomPositions = useCustomPositions1,
                onToggleCustomPositions = onToggleCustomPositions1,
                onSelectVisualLocation = onConfigure1
            )
        } else if (action1 == RemapAction.LOCKSCREEN_AWAKE || action1 == RemapAction.TRIGGER_EXTERNAL_APP) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectedExternalActionDisplayRow(
                action = action1,
                payload = payload1,
                onConfigure = onConfigure1
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isDualActionExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Action 2 (Dual Action)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        onAction2Changed(RemapAction.PASSTHROUGH)
                        isDualActionExpanded = false
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Remove Dual Action",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove Dual Action", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action 2 Picker
            ActionDropdownPicker(
                label = "",
                selectedAction = action2,
                onActionSelected = onAction2Changed
            )

            if (action2 == RemapAction.LAUNCH_APP) {
                Spacer(modifier = Modifier.height(8.dp))
                SelectedAppDisplayRow(
                    packageName = payload2,
                    installedApps = installedApps,
                    onSelectApp = onConfigure2
                )
            } else if (action2 == RemapAction.CUSTOM_KEYBOARD_SHORTCUT) {
                Spacer(modifier = Modifier.height(8.dp))
                SelectedShortcutDisplayRow(
                    shortcut = payload2,
                    onConfigure = onConfigure2
                )
            } else if (action2 == RemapAction.TOGGLE_PEN_ERASER) {
                Spacer(modifier = Modifier.height(8.dp))
                SelectedPenEraserDisplayRow(
                    preset = payload2,
                    onConfigure = onConfigure2
                )
            } else if (isVisualPositionAction(action2)) {
                Spacer(modifier = Modifier.height(8.dp))
                SelectedPositionDisplayRow(
                    positionCoords = payload2,
                    useCustomPositions = useCustomPositions2,
                    onToggleCustomPositions = onToggleCustomPositions2,
                    onSelectVisualLocation = onConfigure2
                )
            } else if (action2 == RemapAction.LOCKSCREEN_AWAKE || action2 == RemapAction.TRIGGER_EXTERNAL_APP) {
                Spacer(modifier = Modifier.height(8.dp))
                SelectedExternalActionDisplayRow(
                    action = action2,
                    payload = payload2,
                    onConfigure = onConfigure2
                )
            }
        } else {
            OutlinedButton(
                onClick = { isDualActionExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Add Dual Action", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SelectedShortcutDisplayRow(
    shortcut: String,
    onConfigure: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigure() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartButton,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (shortcut.isNotBlank()) "Shortcut: $shortcut" else "Custom Keyboard Shortcut",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (shortcut.isNotBlank()) "Tap to change shortcut combo" else "Tap to configure (e.g. CTRL+C, ALT+TAB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onConfigure) {
                Text(if (shortcut.isNotBlank()) "Change" else "Configure")
            }
        }
    }
}

@Composable
fun SelectedPenEraserDisplayRow(
    preset: String,
    onConfigure: () -> Unit
) {
    val displayDescription = when {
        preset.isBlank() -> "Tap to choose note app pen/eraser toggle shortcut"
        preset.contains("202") || preset.contains("Stylus", ignoreCase = true) -> "Stylus Button 1 (202) - Standard Stylus Eraser"
        preset.equals("E", ignoreCase = true) -> "'E' Key - Squid, Concepts, CSP, Kilonotes"
        preset.contains("CTRL", ignoreCase = true) -> "'CTRL + E' - GoodNotes, OneNote"
        else -> "Custom Preset / Keycode: $preset"
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigure() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pen / Eraser Toggle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = displayDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onConfigure) {
                Text(if (preset.isNotBlank()) "Change" else "Configure")
            }
        }
    }
}

@Composable
fun AirCommandCard(
    settings: RemapperSettings,
    onCustomizeClick: () -> Unit,
    onScaleChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartButton,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Air Command Sidebar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Customize floating toolbar items and shortcuts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Permission Status Badge
            Surface(
                color = if (hasOverlayPermission) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasOverlayPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasOverlayPermission) "Overlay Permission Granted" else "Overlay Permission Required",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasOverlayPermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    if (!hasOverlayPermission) {
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Toolbar Size Slider Control
            var sliderScale by remember(settings.airCommandScale) {
                mutableFloatStateOf(settings.airCommandScale.coerceIn(0.65f, 1.30f))
            }
            val percentage = (sliderScale * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Toolbar Size: $percentage%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (sliderScale != 1.0f) {
                    TextButton(
                        onClick = {
                            sliderScale = 1.0f
                            onScaleChanged(1.0f)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset (100%)", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Slider(
                value = sliderScale,
                onValueChange = { sliderScale = it },
                onValueChangeFinished = { onScaleChanged(sliderScale) },
                valueRange = 0.65f..1.30f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                        if (hasOverlayPermission) {
                            AirCommandOverlayService.ensureServiceRunning(context)
                            AirCommandController.toggle()
                        } else {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Overlay", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onCustomizeClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Customize", fontSize = 12.sp)
                }
            }
        }
    }
}
@Composable
fun MasterToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (enabled) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (enabled) Color(0xFFA5D6A7) else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (enabled) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hardware Button Remapper",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (enabled) "Remapper active: intercepting Xiaomi Pen Gen 2 keys" else "Remapper disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun EngineSelectorCard(
    currentEngine: EngineMode,
    isRootAvailable: Boolean,
    onEngineSelected: (EngineMode) -> Unit,
    onEnableViaRoot: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remapper Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isRootAvailable) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isRootAvailable) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRootAvailable) "Root Available" else "No Root",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRootAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            EngineMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEngineSelected(mode) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (currentEngine == mode),
                        onClick = { onEngineSelected(mode) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (mode == EngineMode.ACCESSIBILITY)
                                "Standard system accessibility key event listener"
                            else "Auto-enables accessibility service automatically via root (su)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (currentEngine == EngineMode.ROOT) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEnableViaRoot,
                    enabled = isRootAvailable,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRootAvailable) "Grant & Auto-Enable via Root" else "Root Not Available")
                }
            }
        }
    }
}

@Composable
fun AccessibilityWarningCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Accessibility Service Required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "To intercept stylus hardware button clicks, enable 'Pen Button Remapper' in system accessibility settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open Accessibility Settings")
            }
        }
    }
}

@Composable
fun CustomGesturePositionsToggleCard(
    useCustom: Boolean,
    onToggleChanged: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Custom Visual Touch Locations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (useCustom) "Enabled: using saved X/Y pixel coordinates for screen gestures" else "Disabled: falling back to standard screen edge defaults",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = useCustom,
                onCheckedChange = onToggleChanged
            )
        }
    }
}

@Composable
fun CustomGestureTimeoutsToggleCard(
    useCustom: Boolean,
    onToggleChanged: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Custom Per-Gesture Timeouts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure double-click, triple-click, and long-press speeds independently per button.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = useCustom,
                onCheckedChange = onToggleChanged
            )
        }
    }
}

@Composable
fun CustomTimingInputDialog(
    title: String,
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue.toString()) }
    val parsedValue = textValue.toIntOrNull()
    val isValid = parsedValue != null && parsedValue in 50..5000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Enter custom value in milliseconds (50ms - 5000ms):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input.filter { it.isDigit() }
                    },
                    label = { Text("Duration (ms)") },
                    singleLine = true,
                    isError = !isValid && textValue.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isValid && textValue.isNotEmpty()) {
                    Text(
                        text = "Must be a number between 50 and 5000 ms",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(parsedValue)
                        onDismiss()
                    }
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimingBadge(
    msValue: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$msValue ms",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit custom value",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ButtonSpecificTimeoutsCard(
    buttonName: String,
    doubleClickMs: Int,
    onDoubleClickChanged: (Int) -> Unit,
    tripleClickMs: Int,
    onTripleClickChanged: (Int) -> Unit,
    longPressMs: Int,
    onLongPressChanged: (Int) -> Unit
) {
    var editDialogTitle by remember { mutableStateOf<String?>(null) }
    var editDialogValue by remember { mutableIntStateOf(0) }
    var editDialogCallback by remember { mutableStateOf<(Int) -> Unit>({}) }

    if (editDialogTitle != null) {
        CustomTimingInputDialog(
            title = editDialogTitle!!,
            currentValue = editDialogValue,
            onDismiss = { editDialogTitle = null },
            onConfirm = { editDialogCallback(it) }
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$buttonName Timing Overrides",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Double click speed (100ms - 1500ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Double-Click Speed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                TimingBadge(
                    msValue = doubleClickMs,
                    onClick = {
                        editDialogTitle = "$buttonName Double-Click Speed"
                        editDialogValue = doubleClickMs
                        editDialogCallback = onDoubleClickChanged
                    }
                )
            }
            Slider(
                value = doubleClickMs.toFloat().coerceIn(100f, 1500f),
                onValueChange = {
                    val stepped = (Math.round(it / 50f) * 50).coerceIn(100, 1500)
                    onDoubleClickChanged(stepped)
                },
                valueRange = 100f..1500f,
                steps = 27,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("100 ms (Fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("1500 ms (Slow)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Triple click speed (150ms - 2000ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Triple-Click Speed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                TimingBadge(
                    msValue = tripleClickMs,
                    onClick = {
                        editDialogTitle = "$buttonName Triple-Click Speed"
                        editDialogValue = tripleClickMs
                        editDialogCallback = onTripleClickChanged
                    }
                )
            }
            Slider(
                value = tripleClickMs.toFloat().coerceIn(150f, 2000f),
                onValueChange = {
                    val stepped = (Math.round(it / 50f) * 50).coerceIn(150, 2000)
                    onTripleClickChanged(stepped)
                },
                valueRange = 150f..2000f,
                steps = 36,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("150 ms (Fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("2000 ms (Slow)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Long press duration (200ms - 3000ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Long-Press Hold Duration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                TimingBadge(
                    msValue = longPressMs,
                    onClick = {
                        editDialogTitle = "$buttonName Long-Press Duration"
                        editDialogValue = longPressMs
                        editDialogCallback = onLongPressChanged
                    }
                )
            }
            Slider(
                value = longPressMs.toFloat().coerceIn(200f, 3000f),
                onValueChange = {
                    val stepped = (Math.round(it / 50f) * 50).coerceIn(200, 3000)
                    onLongPressChanged(stepped)
                },
                valueRange = 200f..3000f,
                steps = 55,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("200 ms (Fast)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("3000 ms (Slow)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DoubleClickTimeoutCard(
    timeoutMs: Int,
    onTimeoutChanged: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CustomTimingInputDialog(
            title = "Global Double-Click Speed",
            currentValue = timeoutMs,
            onDismiss = { showDialog = false },
            onConfirm = { onTimeoutChanged(it) }
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Global Double-Click Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TimingBadge(
                    msValue = timeoutMs,
                    onClick = { showDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Maximum time window between consecutive clicks (100ms - 1500ms).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = timeoutMs.toFloat().coerceIn(100f, 1500f),
                onValueChange = {
                    val stepped = (Math.round(it / 50f) * 50).coerceIn(100, 1500)
                    onTimeoutChanged(stepped)
                },
                valueRange = 100f..1500f,
                steps = 27,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "100 ms (Fast)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "1500 ms (Slow)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TripleClickTimeoutCard(
    timeoutMs: Int,
    onTimeoutChanged: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CustomTimingInputDialog(
            title = "Global Triple-Click Speed",
            currentValue = timeoutMs,
            onDismiss = { showDialog = false },
            onConfirm = { onTimeoutChanged(it) }
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Global Triple-Click Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TimingBadge(
                    msValue = timeoutMs,
                    onClick = { showDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Maximum time window between consecutive clicks (150ms - 2000ms).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = timeoutMs.toFloat().coerceIn(150f, 2000f),
                onValueChange = {
                    val stepped = (Math.round(it / 50f) * 50).coerceIn(150, 2000)
                    onTimeoutChanged(stepped)
                },
                valueRange = 150f..2000f,
                steps = 36,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "150 ms (Fast)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "2000 ms (Slow)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LongPressDurationCard(
    durationMs: Int,
    onDurationChanged: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CustomTimingInputDialog(
            title = "Global Long-Press Duration",
            currentValue = durationMs,
            onDismiss = { showDialog = false },
            onConfirm = { onDurationChanged(it) }
        )
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Global Long-Press Duration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TimingBadge(
                    msValue = durationMs,
                    onClick = { showDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Hold duration required to trigger long-press action (200ms - 3000ms).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = durationMs.toFloat().coerceIn(200f, 3000f),
                onValueChange = {
                    val stepped = (Math.round(it / 50f) * 50).coerceIn(200, 3000)
                    onDurationChanged(stepped)
                },
                valueRange = 200f..3000f,
                steps = 55,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "200 ms (Fast)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "3000 ms (Slow)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionDropdownPicker(
    label: String,
    selectedAction: RemapAction,
    onActionSelected: (RemapAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedAction.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                RemapAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = action.displayName,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = action.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onActionSelected(action)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectedAppDisplayRow(
    packageName: String,
    installedApps: List<AppInfo>,
    onSelectApp: () -> Unit
) {
    val selectedApp = remember(packageName, installedApps) {
        installedApps.find { it.packageName == packageName }
    }
    val appBitmap = rememberAppIcon(packageName)

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectApp() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appBitmap != null) {
                Image(
                    bitmap = appBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedApp?.appName ?: "No Application Selected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (packageName.isNotBlank()) packageName else "Tap to choose application",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onSelectApp) {
                Text("Change")
            }
        }
    }
}

@Composable
fun DeviceFilterCard(
    filter: String,
    onFilterChanged: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Name Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "To avoid hijacking standard keyboards, remapper only handles events from devices matching this filter name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = filter,
                onValueChange = onFilterChanged,
                label = { Text("Filter String (e.g. Pen)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

@Composable
fun KeyTesterCard(
    recentKeyEvent: KeyEventInfo?,
    onClear: () -> Unit,
    onSimulatePress: (keyCode: Int, keyName: String) -> Unit
) {
    val isHighlighted = recentKeyEvent != null
    val cardBgColor by animateColorAsState(
        targetValue = if (isHighlighted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceContainer,
        label = "KeyTesterBgAnimation"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartButton,
                        contentDescription = null,
                        tint = if (isHighlighted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interactive Key Tester",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (recentKeyEvent != null) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Test Log",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentKeyEvent != null) {
                val timeStr = remember(recentKeyEvent.timestamp) {
                    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(recentKeyEvent.timestamp))
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF2E7D32), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Detected: ${recentKeyEvent.keyName} (${recentKeyEvent.keyCode})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Device: ${recentKeyEvent.deviceName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Time: $timeStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Press any hardware button on your stylus (KEYCODE_PAGE_UP or KEYCODE_PAGE_DOWN) to test key detection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSimulatePress(92, "KEYCODE_PAGE_UP") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Primary (92)", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onSimulatePress(93, "KEYCODE_PAGE_DOWN") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Secondary (93)", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun SelectedExternalActionDisplayRow(
    action: RemapAction,
    payload: String,
    onConfigure: () -> Unit
) {
    val (targetPkg, targetAct) = ActionExecutor.parsePackageAndAction(
        payload,
        defaultPackage = "com.yaawaaa.lockscreenawake",
        defaultAction = "com.yaawaaa.lockscreenawake.action.TOGGLE"
    )
    val stateExtra = ActionExecutor.parseBooleanState(payload)
    val displayActionName = when {
        targetAct.endsWith(".TOGGLE", ignoreCase = true) || targetAct.equals("TOGGLE", ignoreCase = true) -> "Toggle"
        targetAct.endsWith(".START", ignoreCase = true) || targetAct.equals("START", ignoreCase = true) -> "Turn On / Start"
        targetAct.endsWith(".STOP", ignoreCase = true) || targetAct.equals("STOP", ignoreCase = true) -> "Turn Off / Stop"
        targetAct.endsWith(".QUERY_STATE", ignoreCase = true) || targetAct.equals("QUERY_STATE", ignoreCase = true) -> "Query State"
        targetAct.endsWith(".SET_STATE", ignoreCase = true) || targetAct.contains("SET_STATE", ignoreCase = true) -> {
            if (stateExtra == true) "Set State On" else if (stateExtra == false) "Set State Off" else "Set State"
        }
        else -> targetAct
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigure() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (action == RemapAction.LOCKSCREEN_AWAKE) "Lockscreen Awake ($displayActionName)" else "External App Action ($displayActionName)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Package: $targetPkg\nAction: $targetAct",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(onClick = onConfigure) {
                Text("Configure Action")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalActionDialog(
    initialValue: String,
    onSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val (initialPkg, initialAct) = ActionExecutor.parsePackageAndAction(
        initialValue,
        defaultPackage = "com.yaawaaa.lockscreenawake",
        defaultAction = "com.yaawaaa.lockscreenawake.action.TOGGLE"
    )

    var packageName by remember { mutableStateOf(initialPkg) }
    var actionName by remember { mutableStateOf(initialAct) }

    val presets = listOf(
        "Toggle" to "TOGGLE",
        "Turn On" to "START",
        "Turn Off" to "STOP",
        "Query State" to "QUERY_STATE",
        "Set State On" to "SET_STATE:TRUE",
        "Set State Off" to "SET_STATE:FALSE"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Lockscreen Awake / External Action",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select preset action for Lockscreen Awake:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.take(3).forEach { (label, actionToken) ->
                            val isSelected = actionName.equals(actionToken, ignoreCase = true) ||
                                    actionName.endsWith(".$actionToken", ignoreCase = true) ||
                                    (actionToken == "TOGGLE" && actionName.endsWith(".TOGGLE", ignoreCase = true))

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    packageName = "com.yaawaaa.lockscreenawake"
                                    actionName = actionToken
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.drop(3).forEach { (label, actionToken) ->
                            val isSelected = actionName.equals(actionToken, ignoreCase = true) ||
                                    actionName.contains(actionToken, ignoreCase = true)

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    packageName = "com.yaawaaa.lockscreenawake"
                                    actionName = actionToken
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Or configure explicit Intent details manually:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Target Package Name") },
                    placeholder = { Text("com.yaawaaa.lockscreenawake") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = actionName,
                    onValueChange = { actionName = it },
                    label = { Text("Target Action Name / Token") },
                    placeholder = { Text("com.yaawaaa.lockscreenawake.action.TOGGLE") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPkg = packageName.trim().ifBlank { "com.yaawaaa.lockscreenawake" }
                    val finalAct = actionName.trim().ifBlank { "com.yaawaaa.lockscreenawake.action.TOGGLE" }
                    onSaved("$finalPkg/$finalAct")
                }
            ) {
                Text("Save Action")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
