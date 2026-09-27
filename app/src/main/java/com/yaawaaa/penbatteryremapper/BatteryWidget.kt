package com.yaawaaa.penbatteryremapper

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.Text
import java.io.File

class BatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            BatteryWidgetContent()
        }
    }
}

@Composable
fun BatteryWidgetContent() {
    val batteryLevel = getPenBatteryLevel()

    Column(
        modifier = GlanceModifier
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Pen Battery"
        )
        Text(
            text = if (batteryLevel >= 0) "$batteryLevel%" else "Unknown"
        )
    }
}

fun getPenBatteryLevel(): Int {
    return try {
        val file = File("/sys/class/power_supply/idt/reverse_pen_soc")
        if (file.exists()) {
            file.readText().trim().toInt()
        } else {
            -1
        }
    } catch (e: Exception) {
        -1
    }
}
