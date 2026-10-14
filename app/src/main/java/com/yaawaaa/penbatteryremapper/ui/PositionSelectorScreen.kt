package com.yaawaaa.penbatteryremapper.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID
import kotlin.math.roundToInt

data class PositionPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val x: Int,
    val y: Int
)

object PositionPresetManager {
    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }
    private val type = Types.newParameterizedType(List::class.java, PositionPreset::class.java)
    private val adapter by lazy { moshi.adapter<List<PositionPreset>>(type) }

    fun parsePresets(json: String): List<PositionPreset> {
        if (json.isBlank()) return emptyList()
        return try {
            adapter.fromJson(json.trim()) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializePresets(presets: List<PositionPreset>): String {
        return try {
            adapter.toJson(presets)
        } catch (e: Exception) {
            ""
        }
    }
}

@Composable
fun InteractiveScreenshotCanvas(
    bitmap: Bitmap,
    selectedX: Int,
    selectedY: Int,
    onPositionTapped: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerWidthPx by remember { mutableFloatStateOf(1f) }
    var containerHeightPx by remember { mutableFloatStateOf(1f) }

    fun updatePositionFromOffset(touchX: Float, touchY: Float) {
        if (containerWidthPx > 0f && containerHeightPx > 0f && bitmap.width > 0 && bitmap.height > 0) {
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            val bitmapRatio = bitmapWidth / bitmapHeight
            val containerRatio = containerWidthPx / containerHeightPx

            val displayedWidth: Float
            val displayedHeight: Float
            val padX: Float
            val padY: Float

            if (containerRatio > bitmapRatio) {
                displayedHeight = containerHeightPx
                displayedWidth = containerHeightPx * bitmapRatio
                padX = (containerWidthPx - displayedWidth) / 2f
                padY = 0f
            } else {
                displayedWidth = containerWidthPx
                displayedHeight = containerWidthPx / bitmapRatio
                padX = 0f
                padY = (containerHeightPx - displayedHeight) / 2f
            }

            if (displayedWidth <= 0f || displayedHeight <= 0f) return

            val normX = ((touchX - padX) / displayedWidth).coerceIn(0f, 1f)
            val normY = ((touchY - padY) / displayedHeight).coerceIn(0f, 1f)

            val pixelX = (normX * bitmapWidth).roundToInt().coerceIn(0, bitmap.width)
            val pixelY = (normY * bitmapHeight).roundToInt().coerceIn(0, bitmap.height)

            onPositionTapped(pixelX, pixelY)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .onGloballyPositioned { layoutCoordinates ->
                containerWidthPx = layoutCoordinates.size.width.toFloat()
                containerHeightPx = layoutCoordinates.size.height.toFloat()
            }
            .pointerInput(bitmap, containerWidthPx, containerHeightPx) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    updatePositionFromOffset(down.position.x, down.position.y)
                    val pointer = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointer } ?: break
                        if (!change.pressed) break
                        change.consume()
                        updatePositionFromOffset(change.position.x, change.position.y)
                    }
                }
            },
        contentAlignment = Alignment.TopStart
    ) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = imageBitmap,
            contentDescription = "Target Screenshot",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Render Target Position Marker
        if (containerWidthPx > 0f && containerHeightPx > 0f && bitmap.width > 0 && bitmap.height > 0) {
            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            val bitmapRatio = bitmapWidth / bitmapHeight
            val containerRatio = containerWidthPx / containerHeightPx

            val displayedWidth: Float
            val displayedHeight: Float
            val padX: Float
            val padY: Float

            if (containerRatio > bitmapRatio) {
                displayedHeight = containerHeightPx
                displayedWidth = containerHeightPx * bitmapRatio
                padX = (containerWidthPx - displayedWidth) / 2f
                padY = 0f
            } else {
                displayedWidth = containerWidthPx
                displayedHeight = containerWidthPx / bitmapRatio
                padX = 0f
                padY = (containerHeightPx - displayedHeight) / 2f
            }

            val markerX = padX + (selectedX.toFloat() / bitmapWidth) * displayedWidth
            val markerY = padY + (selectedY.toFloat() / bitmapHeight) * displayedHeight

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        val halfSizePx = 18.dp.toPx()
                        IntOffset(
                            (markerX - halfSizePx).roundToInt(),
                            (markerY - halfSizePx).roundToInt()
                        )
                    }
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Target Location",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        val rawWidth = options.outWidth
        val rawHeight = options.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            this.inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PositionSelectorDialog(
    initialCoords: String = "",
    initialPresetsJson: String = "",
    onPositionSaved: (String) -> Unit,
    onSavePresetsJson: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    val realScreenSize = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wm != null) {
            val bounds = wm.currentWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val realDm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealMetrics(realDm)
            if (realDm.widthPixels > 0 && realDm.heightPixels > 0) {
                Pair(realDm.widthPixels, realDm.heightPixels)
            } else {
                val dm = context.resources.displayMetrics
                Pair(dm.widthPixels, dm.heightPixels)
            }
        }
    }
    val screenW = realScreenSize.first
    val screenH = realScreenSize.second

    @Suppress("DEPRECATION")
    val rotation = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation ?: wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        } else {
            wm?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    } catch (e: Exception) {
        Surface.ROTATION_0
    }

    val cleanCoords = initialCoords.removePrefix("CENTER:")
    val parsedCoords = remember(initialCoords, screenW, screenH) {
        val parts = cleanCoords.split(",", "x", "X", " ").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size >= 2) {
            Pair(parts[0], parts[1])
        } else {
            Pair(screenW / 2, screenH / 2)
        }
    }

    var selectedX by remember { mutableIntStateOf(parsedCoords.first) }
    var selectedY by remember { mutableIntStateOf(parsedCoords.second) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFullscreenMode by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            selectedBitmap?.recycle()
            selectedBitmap = null
        }
    }

    var presets by remember(initialPresetsJson) {
        mutableStateOf(PositionPresetManager.parsePresets(initialPresetsJson))
    }
    var showAddPresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val reqW = screenW.coerceAtLeast(1080)
                val reqH = screenH.coerceAtLeast(1920)
                val bitmap = decodeSampledBitmapFromUri(context, it, reqW, reqH)
                if (bitmap != null) {
                    selectedBitmap?.recycle()
                    selectedBitmap = bitmap
                    if (initialCoords.isBlank() || cleanCoords.isBlank()) {
                        selectedX = bitmap.width / 2
                        selectedY = bitmap.height / 2
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(8.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Visual Position Selector",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // UI Notice Alert
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Notice",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Notice: The screenshot resolution and scaling must match the exact native resolution and scaling of the active device display to ensure pixel coordinates map accurately without distortion or misalignment.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Upload / Delete Screenshot Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedBitmap == null) "Select Screenshot" else "Change Image",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (selectedBitmap != null) {
                        OutlinedButton(
                            onClick = {
                                selectedBitmap?.recycle()
                                selectedBitmap = null
                                selectedX = screenW / 2
                                selectedY = screenH / 2
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Screenshot",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Position Profiles / Presets Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Position Presets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = {
                            newPresetName = "Spot ${presets.size + 1}"
                            showAddPresetDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Save Preset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save as Preset", fontSize = 12.sp)
                    }
                }

                if (presets.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = (selectedX == preset.x && selectedY == preset.y),
                                onClick = {
                                    selectedX = preset.x
                                    selectedY = preset.y
                                },
                                label = { Text("${preset.name} (${preset.x},${preset.y})") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val updated = presets.filter { it.id != preset.id }
                                            presets = updated
                                            onSavePresetsJson?.invoke(PositionPresetManager.serializePresets(updated))
                                        },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Preset",
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Interactive Image Display Area
                if (selectedBitmap != null) {
                    val bitmap = selectedBitmap!!

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap or drag crosshair to select spot:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { isFullscreenMode = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen View",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fullscreen Mode", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InteractiveScreenshotCanvas(
                        bitmap = bitmap,
                        selectedX = selectedX,
                        selectedY = selectedY,
                        onPositionTapped = { x, y ->
                            selectedX = x
                            selectedY = y
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap 'Select Screenshot' above to choose display image",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Coordinate Display and Adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = selectedX.toString(),
                        onValueChange = { selectedX = it.toIntOrNull() ?: 0 },
                        label = { Text("Pixel X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = selectedY.toString(),
                        onValueChange = { selectedY = it.toIntOrNull() ?: 0 },
                        label = { Text("Pixel Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saveW = selectedBitmap?.width ?: screenW
                    val saveH = selectedBitmap?.height ?: screenH
                    onPositionSaved("${selectedX},${selectedY},$saveW,$saveH,$rotation")
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Location ($selectedX, $selectedY)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Save Preset Name Dialog
    if (showAddPresetDialog) {
        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            title = { Text("Save Position Preset") },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Preset Name (e.g. Spot A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            val newPreset = PositionPreset(
                                name = newPresetName.trim(),
                                x = selectedX,
                                y = selectedY
                            )
                            val updated = presets + newPreset
                            presets = updated
                            onSavePresetsJson?.invoke(PositionPresetManager.serializePresets(updated))
                        }
                        showAddPresetDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fullscreen Viewer Dialog
    if (isFullscreenMode && selectedBitmap != null) {
        Dialog(
            onDismissRequest = { isFullscreenMode = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                InteractiveScreenshotCanvas(
                    bitmap = selectedBitmap!!,
                    selectedX = selectedX,
                    selectedY = selectedY,
                    onPositionTapped = { x, y ->
                        selectedX = x
                        selectedY = y
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay Controls Top Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fullscreen Position Picker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Tapped Pixel: X=$selectedX, Y=$selectedY",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { isFullscreenMode = false },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedPositionDisplayRow(
    positionCoords: String,
    useCustomPositions: Boolean = true,
    onToggleCustomPositions: ((Boolean) -> Unit)? = null,
    onSelectVisualLocation: () -> Unit
) {
    val cleanCoords = positionCoords.removePrefix("CENTER:")
    val displayCoords = cleanCoords.split(",").take(2).joinToString(", ")

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = if (useCustomPositions) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (useCustomPositions) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (useCustomPositions && positionCoords.isNotBlank()) "Target Location: $displayCoords" else if (!useCustomPositions) "Target Location: Screen Defaults" else "Custom Gesture Target Location",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (!useCustomPositions) "Custom position disabled (using default screen edge offsets)" else if (positionCoords.isNotBlank()) "Saved Pixel Coordinates (X,Y)" else "Tap to select target spot visually on screenshot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onToggleCustomPositions != null) {
                    Switch(
                        checked = useCustomPositions,
                        onCheckedChange = onToggleCustomPositions
                    )
                }
            }

            if (useCustomPositions) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onSelectVisualLocation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (positionCoords.isNotBlank()) "Change Visual Location" else "Select Location Visually")
                }
            }
        }
    }
}
