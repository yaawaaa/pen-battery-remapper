package com.yaawaaa.penbatteryremapper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yaawaaa.penbatteryremapper.BatteryState
import com.yaawaaa.penbatteryremapper.BatteryViewModel
import com.yaawaaa.penbatteryremapper.ChargingStatus
import com.yaawaaa.penbatteryremapper.data.BatteryLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(viewModel: BatteryViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery"
                        )
                    },
                    label = { Text("Battery") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.SmartButton,
                            contentDescription = "Remapper"
                        )
                    },
                    label = { Text("Remapper") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTab == 0) {
                BatteryDashboardContent(viewModel = viewModel)
            } else {
                RemapperScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryDashboardContent(viewModel: BatteryViewModel = viewModel()) {
    val batteryState by viewModel.batteryState.collectAsState()
    val chargingStatus by viewModel.chargingStatus.collectAsState()
    val batteryLogs by viewModel.batteryLogs.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val isActivelyCharging by viewModel.isActivelyCharging.collectAsState()
    val isLoggingEnabled by viewModel.isLoggingEnabled.collectAsState()
    val isTileAdded by viewModel.isTileAdded.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Stylus Pen Dashboard",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Battery"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Battery Level Gauge Card
            HeroBatteryCard(
                batteryState = batteryState,
                isActivelyCharging = isActivelyCharging
            )

            // 2. Quick Settings Tile Card
            QuickSettingsTileCard(
                isTileAdded = isTileAdded,
                onRequestAddTile = { viewModel.requestAddTile(context) }
            )

            // 3. Charging Status / Fallback Section
            ChargingStatusSection(
                chargingStatus = chargingStatus,
                isActivelyCharging = isActivelyCharging
            )

            // 4. Battery Discharge History Chart Section
            BatteryDischargeHistorySection(
                logs = batteryLogs,
                onAddSampleLogs = { viewModel.addSampleLogs() },
                onClearLogs = { viewModel.clearLogs() }
            )

            // 5. Quick Controls Section
            QuickControlsSection(
                isServiceRunning = isServiceRunning,
                onToggleService = { viewModel.toggleService(context) },
                isLoggingEnabled = isLoggingEnabled,
                onToggleLogging = { viewModel.toggleLogging(it) },
                onClearLogs = { viewModel.clearLogs() },
                onRefresh = { viewModel.refresh() },
                onAddSampleLogs = { viewModel.addSampleLogs() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuickSettingsTileCard(
    isTileAdded: Boolean,
    onRequestAddTile: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isTileAdded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTileAdded) Icons.Default.CheckCircle else Icons.Default.Dashboard,
                        contentDescription = null,
                        tint = if (isTileAdded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Quick Settings Tile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isTileAdded) "Tile added to Quick Settings" else "Add pen battery tile to Quick Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isTileAdded) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tile Added",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRequestAddTile,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Tile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HeroBatteryCard(
    batteryState: BatteryState,
    isActivelyCharging: Boolean = false
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Stylus Pen Battery Level",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (batteryState) {
                is BatteryState.Loading -> {
                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is BatteryState.Success -> {
                    val level = batteryState.level
                    val isConnected = batteryState.isConnected
                    val timeStr = remember(batteryState.lastUpdated) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(batteryState.lastUpdated))
                    }

                    val gaugeColor = when {
                        level > 50 -> Color(0xFF4CAF50)
                        level >= 20 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }

                    Box(
                        modifier = Modifier.size(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom Gauge Arc
                        val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 16.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(
                                x = (size.width - diameter) / 2f,
                                y = (size.height - diameter) / 2f
                            )
                            val arcSize = Size(diameter, diameter)

                            // Background Track Arc
                            drawArc(
                                color = trackColor,
                                startAngle = 150f,
                                sweepAngle = 240f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Progress Arc
                            val progressSweep = (level.coerceIn(0, 100) / 100f) * 240f
                            drawArc(
                                color = gaugeColor,
                                startAngle = 150f,
                                sweepAngle = progressSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // Center text inside gauge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$level%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isConnected) gaugeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (isConnected) gaugeColor else MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isConnected) "Connected" else "Disconnected",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isConnected) gaugeColor else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    if (isActivelyCharging) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E7D32)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⚡ Actively Charging (Battery Increasing)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Last updated: $timeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is BatteryState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reading Unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = batteryState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChargingStatusSection(
    chargingStatus: ChargingStatus,
    isActivelyCharging: Boolean = false
) {
    when (chargingStatus) {
        is ChargingStatus.DockedAndCharging -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFFA5D6A7),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Connected & Docked (Charging)",
                            tint = Color(0xFF1B5E20),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isActivelyCharging) "⚡ Actively Charging (Battery Increasing)" else "Connected & Docked (Charging)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Charging on Magnetic Strip (${chargingStatus.level}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        is ChargingStatus.Disconnected -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PowerOff,
                            contentDescription = "Disconnected (Off Magnetic Strip)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Disconnected (Off Magnetic Strip)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pen is detached from magnetic charging strip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryDischargeHistorySection(
    logs: List<BatteryLog>,
    onAddSampleLogs: () -> Unit,
    onClearLogs: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Discharge History Chart",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (logs.isNotEmpty()) {
                    IconButton(onClick = onClearLogs) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (logs.isEmpty()) {
                // Empty state handling
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Battery Logs Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Battery level changes will be recorded over time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onAddSampleLogs) {
                            Icon(
                                imageVector = Icons.Default.AddChart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Sample History")
                        }
                    }
                }
            } else {
                // Summary Stats Row
                val sorted = remember(logs) { logs.sortedBy { it.timestamp } }
                val minLevel = sorted.minOf { it.level }
                val maxLevel = sorted.maxOf { it.level }
                val avgLevel = sorted.map { it.level }.average().toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(label = "Min", value = "$minLevel%")
                    StatPill(label = "Avg", value = "$avgLevel%")
                    StatPill(label = "Max", value = "$maxLevel%")
                    StatPill(label = "Logs", value = "${logs.size}")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Canvas Line/Curve Chart
                BatteryDischargeChart(
                    logs = sorted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun BatteryDischargeChart(
    logs: List<BatteryLog>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var touchX by remember { mutableStateOf<Float?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(logs) {
                    detectTapGestures { offset ->
                        touchX = offset.x
                    }
                }
                .pointerInput(logs) {
                    detectDragGestures(
                        onDragStart = { offset -> touchX = offset.x },
                        onDrag = { change, _ ->
                            touchX = change.position.x
                        },
                        onDragEnd = { touchX = null },
                        onDragCancel = { touchX = null }
                    )
                }
        ) {
            val paddingLeft = 44.dp.toPx()
            val paddingRight = 16.dp.toPx()
            val paddingTop = 16.dp.toPx()
            val paddingBottom = 28.dp.toPx()

            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            // 1. Draw Y-Axis Grid & Labels (0%, 25%, 50%, 75%, 100%)
            val ySteps = listOf(100, 75, 50, 25, 0)
            for (step in ySteps) {
                val y = paddingTop + (1f - step / 100f) * chartHeight

                // Grid line
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(paddingLeft + chartWidth, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                // Label
                val labelResult = textMeasurer.measure(
                    text = "$step%",
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
                drawText(
                    textLayoutResult = labelResult,
                    topLeft = Offset(
                        paddingLeft - labelResult.size.width - 6.dp.toPx(),
                        y - labelResult.size.height / 2f
                    )
                )
            }

            if (logs.isEmpty()) return@Canvas

            // 2. Map data points
            val minTime = logs.first().timestamp
            val maxTime = logs.last().timestamp
            val timeRange = (maxTime - minTime).coerceAtLeast(1L)

            val points = logs.map { log ->
                val x = if (logs.size == 1 || timeRange == 0L) {
                    paddingLeft + chartWidth / 2f
                } else {
                    paddingLeft + ((log.timestamp - minTime).toFloat() / timeRange) * chartWidth
                }
                val y = paddingTop + (1f - (log.level.coerceIn(0, 100) / 100f)) * chartHeight
                Offset(x, y)
            }

            // Determine selected point if touched
            val currentTouchX = touchX
            var activeIndex: Int? = null
            if (currentTouchX != null && points.isNotEmpty()) {
                activeIndex = points.indices.minByOrNull { Math.abs(points[it].x - currentTouchX) }
            }
            selectedIndex = activeIndex

            // 3. Construct Smooth Cubic Curve Path
            val linePath = Path()
            val fillPath = Path()

            if (points.isNotEmpty()) {
                linePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, paddingTop + chartHeight)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX1 = p0.x + (p1.x - p0.x) / 2f
                    val controlY1 = p0.y
                    val controlX2 = p0.x + (p1.x - p0.x) / 2f
                    val controlY2 = p1.y

                    linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, paddingTop + chartHeight)
                fillPath.close()

                // Draw Gradient Fill under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                // Draw Curve Line
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Dots at Data Points
                points.forEachIndexed { idx, pt ->
                    val isSelected = (idx == activeIndex)
                    val pointColor = when {
                        logs[idx].level > 50 -> Color(0xFF4CAF50)
                        logs[idx].level >= 20 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }

                    if (isSelected) {
                        drawCircle(
                            color = pointColor.copy(alpha = 0.3f),
                            radius = 12.dp.toPx(),
                            center = pt
                        )
                    }

                    drawCircle(
                        color = surfaceColor,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = pointColor,
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                }

                // 4. Draw X-Axis Timestamps (Earliest, Mid, Latest)
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeIndices = when {
                    logs.size <= 2 -> logs.indices.toList()
                    else -> listOf(0, logs.size / 2, logs.size - 1)
                }

                timeIndices.distinct().forEach { idx ->
                    val log = logs[idx]
                    val timeStr = dateFormat.format(Date(log.timestamp))
                    val labelResult = textMeasurer.measure(
                        text = timeStr,
                        style = TextStyle(color = textColor, fontSize = 10.sp)
                    )
                    val ptX = points[idx].x
                    val clampedX = (ptX - labelResult.size.width / 2f)
                        .coerceIn(paddingLeft, paddingLeft + chartWidth - labelResult.size.width)

                    drawText(
                        textLayoutResult = labelResult,
                        topLeft = Offset(clampedX, size.height - labelResult.size.height)
                    )
                }

                // 5. Draw Touch Highlight Guide Line
                if (activeIndex != null) {
                    val selPt = points[activeIndex]
                    drawLine(
                        color = primaryColor,
                        start = Offset(selPt.x, paddingTop),
                        end = Offset(selPt.x, paddingTop + chartHeight),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
            }
        }

        // Floating Tooltip overlay on active point
        val activeIdx = selectedIndex
        if (activeIdx != null && activeIdx in logs.indices) {
            val log = logs[activeIdx]
            val timeStr = remember(log.timestamp) {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$timeStr - Level: ${log.level}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickControlsSection(
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    isLoggingEnabled: Boolean,
    onToggleLogging: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    onRefresh: () -> Unit,
    onAddSampleLogs: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Quick Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Background Service Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isServiceRunning) Color(0xFFA5D6A7) else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsSystemDaydream,
                            contentDescription = null,
                            tint = if (isServiceRunning) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Monitor Service",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceRunning) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isServiceRunning) "Running (polls state & posts alerts)" else "Stopped",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isServiceRunning) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { onToggleService() }
                    )
                }
            }

            // Battery Logging Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLoggingEnabled) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isLoggingEnabled) Color(0xFF90CAF9) else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            tint = if (isLoggingEnabled) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Logging",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLoggingEnabled) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLoggingEnabled) "Enabled (Recording battery logs)" else "Paused (No new logs written)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLoggingEnabled) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isLoggingEnabled,
                        onCheckedChange = { onToggleLogging(it) }
                    )
                }
            }

            // Refresh & Data Management Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refresh", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onAddSampleLogs,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddChart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sample", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Log History", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}
