package com.yaawaaa.penbatteryremapper.ui

import com.yaawaaa.penbatteryremapper.remapper.rememberAppIcon
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaawaaa.penbatteryremapper.remapper.AppInfo
import com.yaawaaa.penbatteryremapper.remapper.AppProfile
import com.yaawaaa.penbatteryremapper.remapper.RemapAction
import org.json.JSONArray
import org.json.JSONObject

data class AirCommandItem(
    val type: String = "ACTION", // "ACTION" or "APP"
    val actionName: String = "",
    val packageName: String = "",
    val label: String = "",
    val payload: String = ""
) {
    companion object {
        fun defaultList(): List<AirCommandItem> = listOf(
            AirCommandItem(type = "ACTION", actionName = RemapAction.SCREENSHOT.name, label = "Screenshot"),
            AirCommandItem(type = "ACTION", actionName = RemapAction.PLAY_PAUSE.name, label = "Play / Pause"),
            AirCommandItem(type = "ACTION", actionName = RemapAction.FLASHLIGHT.name, label = "Flashlight"),
            AirCommandItem(type = "ACTION", actionName = RemapAction.VOLUME_UP.name, label = "Volume Up"),
            AirCommandItem(type = "ACTION", actionName = RemapAction.VOLUME_DOWN.name, label = "Volume Down"),
            AirCommandItem(type = "ACTION", actionName = RemapAction.HOME.name, label = "Home"),
            AirCommandItem(type = "ACTION", actionName = RemapAction.BACK.name, label = "Back")
        )

        fun encodeList(items: List<AirCommandItem>): String {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("type", item.type)
                    put("actionName", item.actionName)
                    put("packageName", item.packageName)
                    put("label", item.label)
                    put("payload", item.payload)
                }
                array.put(obj)
            }
            return array.toString()
        }

        fun decodeList(jsonStr: String): List<AirCommandItem> {
            if (jsonStr.isBlank()) return defaultList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<AirCommandItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val type = obj.optString("type", "ACTION")
                    val actionName = obj.optString("actionName", "")
                    val packageName = obj.optString("packageName", "")
                    val label = obj.optString("label", "")
                    var payload = obj.optString("payload", "")

                    val finalActionName = if (actionName.isNotBlank()) {
                        actionName
                    } else if (type == "APP" || packageName.isNotBlank()) {
                        RemapAction.LAUNCH_APP.name
                    } else {
                        RemapAction.PASSTHROUGH.name
                    }

                    if (payload.isBlank()) {
                        payload = packageName
                    }

                    list.add(
                        AirCommandItem(
                            type = type,
                            actionName = finalActionName,
                            packageName = packageName,
                            label = label,
                            payload = payload
                        )
                    )
                }
                if (list.isEmpty()) defaultList() else list
            } catch (e: Exception) {
                defaultList()
            }
        }
    }
}

@Composable
fun AirCommandItemsEditorDialog(
    currentJson: String,
    installedApps: List<AppInfo>,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val itemStateList = remember { mutableStateListOf(*AirCommandItem.decodeList(currentJson).toTypedArray()) }
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Air Command Sidebar Items",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Item", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            itemStateList.clear()
                            itemStateList.addAll(AirCommandItem.defaultList())
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (itemStateList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No toolbar items configured.\nClick 'Add Item' or 'Reset Default'.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(itemStateList) { index, item ->
                            val effectivePkg = item.payload.ifBlank { item.packageName }
                            val appInfo = if (item.type == "APP" || item.actionName == RemapAction.LAUNCH_APP.name) {
                                installedApps.find { it.packageName == effectivePkg }
                            } else null
                            val appBitmap = if (item.type == "APP" || item.actionName == RemapAction.LAUNCH_APP.name) {
                                rememberAppIcon(effectivePkg)
                            } else null

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (appBitmap != null) {
                                            Image(
                                                bitmap = appBitmap,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (item.type == "APP" || item.actionName == RemapAction.LAUNCH_APP.name) Icons.Default.Apps else Icons.Default.SmartButton,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.label.ifBlank { item.actionName },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (item.actionName == RemapAction.LAUNCH_APP.name) {
                                                "App: ${appInfo?.appName ?: effectivePkg}"
                                            } else if (item.payload.isNotBlank()) {
                                                "${item.actionName} (${item.payload})"
                                            } else {
                                                "Action: ${item.actionName}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val temp = itemStateList[index]
                                                itemStateList[index] = itemStateList[index - 1]
                                                itemStateList[index - 1] = temp
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < itemStateList.lastIndex) {
                                                val temp = itemStateList[index]
                                                itemStateList[index] = itemStateList[index + 1]
                                                itemStateList[index + 1] = temp
                                            }
                                        },
                                        enabled = index < itemStateList.lastIndex,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { itemStateList.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(AirCommandItem.encodeList(itemStateList))
                    onDismiss()
                }
            ) {
                Text("Save Toolbar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showAddDialog) {
        AddAirCommandItemDialog(
            installedApps = installedApps,
            onItemAdded = { newItem ->
                itemStateList.add(newItem)
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun AddAirCommandItemDialog(
    installedApps: List<AppInfo>,
    onItemAdded: (AirCommandItem) -> Unit,
    onDismiss: () -> Unit
) {
    val selectableActions = remember {
        RemapAction.entries.filter { it != RemapAction.PASSTHROUGH && it != RemapAction.AIR_COMMAND }
    }

    var selectedAction by remember { mutableStateOf(RemapAction.SCREENSHOT) }
    var selectedAppPackage by remember { mutableStateOf("") }
    var payloadText by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf(selectedAction.displayName) }
    var showAppPicker by remember { mutableStateOf(false) }
    var isActionDropdownExpanded by remember { mutableStateOf(false) }

    val isConfirmEnabled = when (selectedAction) {
        RemapAction.LAUNCH_APP -> selectedAppPackage.isNotBlank()
        RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> payloadText.isNotBlank()
        RemapAction.TOGGLE_PEN_ERASER -> true
        else -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Quick Action",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Action Type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isActionDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedAction.displayName, fontWeight = FontWeight.Medium)
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = isActionDropdownExpanded,
                        onDismissRequest = { isActionDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        selectableActions.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.displayName) },
                                onClick = {
                                    selectedAction = action
                                    isActionDropdownExpanded = false
                                    if (action != RemapAction.LAUNCH_APP) {
                                        customLabel = action.displayName
                                    }
                                    if (action == RemapAction.CUSTOM_KEYBOARD_SHORTCUT && payloadText.isBlank()) {
                                        payloadText = "CTRL+Z"
                                    }
                                    if (action == RemapAction.TOGGLE_PEN_ERASER && payloadText.isBlank()) {
                                        payloadText = "CTRL+E"
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedAction) {
                    RemapAction.LAUNCH_APP -> {
                        Text(
                            text = "Target Application",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val selectedAppInfo = installedApps.find { it.packageName == selectedAppPackage }
                        val appBitmap = rememberAppIcon(selectedAppPackage)

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAppPicker = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (appBitmap != null) {
                                        Image(
                                            bitmap = appBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Text(
                                        text = selectedAppInfo?.appName ?: if (selectedAppPackage.isNotBlank()) selectedAppPackage else "Choose App...",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(imageVector = Icons.Default.Apps, contentDescription = null)
                            }
                        }
                    }

                    RemapAction.CUSTOM_KEYBOARD_SHORTCUT -> {
                        Text(
                            text = "Keyboard Shortcut Spec",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = payloadText,
                            onValueChange = { payloadText = it },
                            placeholder = { Text("e.g. A, 1, Backspace, Space, Enter, or Ctrl+Z") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    RemapAction.TOGGLE_PEN_ERASER -> {
                        Text(
                            text = "Pen/Eraser Preset or Key",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = payloadText,
                            onValueChange = { payloadText = it },
                            placeholder = { Text("e.g. CTRL+E or E") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    RemapAction.LOCKSCREEN_AWAKE, RemapAction.TRIGGER_EXTERNAL_APP -> {
                        Text(
                            text = "Target Package / Action (optional)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = payloadText,
                            onValueChange = { payloadText = it },
                            placeholder = { Text("e.g. com.yaawaaa.lockscreenawake/action.TOGGLE") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    else -> { /* Standard one-shot actions require no extra payload input */ }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Display Label",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isConfirmEnabled,
                onClick = {
                    val finalPayload = when (selectedAction) {
                        RemapAction.LAUNCH_APP -> selectedAppPackage
                        RemapAction.CUSTOM_KEYBOARD_SHORTCUT, RemapAction.TOGGLE_PEN_ERASER, RemapAction.LOCKSCREEN_AWAKE, RemapAction.TRIGGER_EXTERNAL_APP -> payloadText
                        else -> ""
                    }
                    val type = if (selectedAction == RemapAction.LAUNCH_APP) "APP" else "ACTION"
                    val item = AirCommandItem(
                        type = type,
                        actionName = selectedAction.name,
                        packageName = if (selectedAction == RemapAction.LAUNCH_APP) selectedAppPackage else "",
                        label = customLabel.ifBlank { selectedAction.displayName },
                        payload = finalPayload
                    )
                    onItemAdded(item)
                    onDismiss()
                }
            ) {
                Text("Add to Toolbar")
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
                selectedAppPackage = appInfo.packageName
                customLabel = appInfo.appName
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@Composable
fun ExportBackupDialog(
    jsonString: String,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Backup JSON",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Copy this JSON string to share or save your backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = jsonString,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopy()
                    onDismiss()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Backup String")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ImportBackupDialog(
    onRestore: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var jsonInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Backup JSON",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paste your exported JSON backup string below to restore all settings and per-app profiles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = {
                        jsonInput = it
                        isError = false
                    },
                    placeholder = { Text("Paste JSON backup here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    isError = isError
                )

                if (isError) {
                    Text(
                        text = "Invalid JSON format! Please verify your backup text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onRestore(jsonInput)
                    if (!success) {
                        isError = true
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomKeyboardShortcutDialog(
    initialShortcut: String,
    onShortcutSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var ctrlChecked by remember { mutableStateOf(initialShortcut.contains("CTRL", ignoreCase = true)) }
    var altChecked by remember { mutableStateOf(initialShortcut.contains("ALT", ignoreCase = true)) }
    var shiftChecked by remember { mutableStateOf(initialShortcut.contains("SHIFT", ignoreCase = true)) }

    var selectedKey by remember {
        val extracted = initialShortcut
            .replace("CTRL", "", ignoreCase = true)
            .replace("ALT", "", ignoreCase = true)
            .replace("SHIFT", "", ignoreCase = true)
            .replace("+", "")
            .trim()
        mutableStateOf(if (extracted.isNotBlank()) extracted else "C")
    }

    val computedShortcut = remember(ctrlChecked, altChecked, shiftChecked, selectedKey) {
        val parts = mutableListOf<String>()
        if (ctrlChecked) parts.add("CTRL")
        if (altChecked) parts.add("ALT")
        if (shiftChecked) parts.add("SHIFT")
        if (selectedKey.isNotBlank()) parts.add(selectedKey.uppercase())
        parts.joinToString("+")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Custom Keyboard Shortcut",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select modifiers and target key to simulate a keyboard shortcut (e.g. A, 1, Backspace, Space, Enter, or Ctrl+Z).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shortcut Preview:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = computedShortcut.ifBlank { "None" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = "Modifiers",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = ctrlChecked,
                        onClick = { ctrlChecked = !ctrlChecked },
                        label = { Text("CTRL") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = altChecked,
                        onClick = { altChecked = !altChecked },
                        label = { Text("ALT") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = shiftChecked,
                        onClick = { shiftChecked = !shiftChecked },
                        label = { Text("SHIFT") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Arrow Keys",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val arrowKeys = listOf("Up" to "UP", "Down" to "DOWN", "Left" to "LEFT", "Right" to "RIGHT")
                    arrowKeys.forEach { (label, keyToken) ->
                        FilterChip(
                            selected = selectedKey.equals(keyToken, ignoreCase = true),
                            onClick = { selectedKey = keyToken },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Text(
                    text = "Special / Navigation Keys",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val specialKeys = listOf(
                        "Enter" to "ENTER",
                        "Esc" to "ESC",
                        "Tab" to "TAB",
                        "Space" to "SPACE",
                        "Backspace" to "BACKSPACE",
                        "Delete" to "DELETE",
                        "Page Up" to "PAGE_UP",
                        "Page Down" to "PAGE_DOWN"
                    )
                    specialKeys.forEach { (label, keyToken) ->
                        FilterChip(
                            selected = selectedKey.equals(keyToken, ignoreCase = true),
                            onClick = { selectedKey = keyToken },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Text(
                    text = "Common Shortcut Combos",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val commonCombos = listOf(
                        Triple("Ctrl+C (Copy)", "C", true),
                        Triple("Ctrl+V (Paste)", "V", true),
                        Triple("Ctrl+Z (Undo)", "Z", true),
                        Triple("Ctrl+A (Select All)", "A", true)
                    )
                    commonCombos.forEach { (label, keyToken, isCtrl) ->
                        val isComboSelected = ctrlChecked && !altChecked && !shiftChecked && selectedKey.equals(keyToken, ignoreCase = true)
                        FilterChip(
                            selected = isComboSelected,
                            onClick = {
                                ctrlChecked = isCtrl
                                altChecked = false
                                shiftChecked = false
                                selectedKey = keyToken
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                Text(
                    text = "Key / Character",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = selectedKey,
                    onValueChange = { selectedKey = it.uppercase() },
                    label = { Text("Key / Character") },
                    placeholder = { Text("e.g. A, 1, Backspace, Space, Enter, or Ctrl+Z") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onShortcutSaved(computedShortcut)
                    onDismiss()
                }
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
fun PenEraserPresetDialog(
    initialPreset: String,
    onPresetSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember {
        mutableStateOf(
            when {
                initialPreset.contains("CTRL", ignoreCase = true) -> "CTRL_E"
                initialPreset.equals("E", ignoreCase = true) -> "E_KEY"
                initialPreset.contains("202") || initialPreset.contains("Stylus", ignoreCase = true) -> "STYLUS_202"
                initialPreset.isNotBlank() -> "CUSTOM"
                else -> "E_KEY"
            }
        )
    }

    var customInput by remember {
        mutableStateOf(if (selectedOption == "CUSTOM") initialPreset else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Note-Taking Pen / Eraser Preset",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select your note app's pen/eraser toggle key shortcut.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "E_KEY" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedOption == "E_KEY") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == "E_KEY"),
                            onClick = { selectedOption = "E_KEY" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "\"E\" Key", fontWeight = FontWeight.Bold)
                            Text(
                                text = "Squid, Concepts, Clip Studio Paint, Kilonotes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "CTRL_E" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedOption == "CTRL_E") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == "CTRL_E"),
                            onClick = { selectedOption = "CTRL_E" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "\"CTRL + E\"", fontWeight = FontWeight.Bold)
                            Text(
                                text = "GoodNotes, OneNote",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "STYLUS_202" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedOption == "STYLUS_202") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == "STYLUS_202"),
                            onClick = { selectedOption = "STYLUS_202" }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "\"Stylus Button 1 (202)\"", fontWeight = FontWeight.Bold)
                            Text(
                                text = "Standard Android Stylus Eraser keycode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = "CUSTOM" },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedOption == "CUSTOM") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = (selectedOption == "CUSTOM"),
                                onClick = { selectedOption = "CUSTOM" }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Custom Keycode / Shortcut", fontWeight = FontWeight.Bold)
                        }

                        if (selectedOption == "CUSTOM") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customInput,
                                onValueChange = { customInput = it },
                                label = { Text("Keycode or combination (e.g. 202 or CTRL+E)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPreset = when (selectedOption) {
                        "E_KEY" -> "E"
                        "CTRL_E" -> "CTRL+E"
                        "STYLUS_202" -> "202"
                        else -> customInput.ifBlank { "E" }
                    }
                    onPresetSaved(finalPreset)
                    onDismiss()
                }
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
