package com.yaawaaa.penbatteryremapper.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.graphics.drawable.Drawable
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.yaawaaa.penbatteryremapper.remapper.ActionExecutor
import com.yaawaaa.penbatteryremapper.remapper.RemapAction
import com.yaawaaa.penbatteryremapper.remapper.RemapperRepository
import com.yaawaaa.penbatteryremapper.remapper.RemapperSettings
import com.yaawaaa.penbatteryremapper.remapper.RootHelper
import com.yaawaaa.penbatteryremapper.ui.AirCommandItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class AirCommandOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootFrameLayout: FrameLayout? = null
    private var cardContainer: LinearLayout? = null
    private var gridLayout: GridLayout? = null
    private var itemCountBadge: TextView? = null
    private var isViewAttached = false

    private val appIconCache = ConcurrentHashMap<String, Drawable>()
    private lateinit var repository: RemapperRepository
    private var currentSettings = RemapperSettings()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val ACTION_SHOW_HOLD = "com.yaawaaa.penbatteryremapper.action.AIR_COMMAND_SHOW_HOLD"
        const val ACTION_HIDE_HOLD = "com.yaawaaa.penbatteryremapper.action.AIR_COMMAND_HIDE_HOLD"
        const val ACTION_TOGGLE = "com.yaawaaa.penbatteryremapper.action.AIR_COMMAND_TOGGLE"
        const val ACTION_SHOW = "com.yaawaaa.penbatteryremapper.action.AIR_COMMAND_SHOW"
        const val ACTION_HIDE = "com.yaawaaa.penbatteryremapper.action.AIR_COMMAND_HIDE"

        var isServiceRunning = false
            private set

        fun showHold(context: Context) {
            ensureServiceRunning(context)
            AirCommandController.startHold()
        }

        fun hideHold(context: Context) {
            AirCommandController.stopHold()
        }

        fun toggle(context: Context) {
            ensureServiceRunning(context)
            AirCommandController.toggle()
        }

        fun show(context: Context) {
            ensureServiceRunning(context)
            AirCommandController.show()
        }

        fun hide(context: Context) {
            AirCommandController.hide()
        }

        fun ensureServiceRunning(context: Context) {
            if (!isServiceRunning && Settings.canDrawOverlays(context)) {
                try {
                    val intent = Intent(context, AirCommandOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        repository = RemapperRepository(applicationContext)

        createNotificationChannel()
        startForegroundNotification()
        serviceScope.launch(Dispatchers.IO) {
            RootHelper.protectCurrentProcessFromLMK()
        }

        serviceScope.launch {
            repository.remapperSettings.collect { settings ->
                currentSettings = settings
                if (isViewAttached) {
                    updateCardWidthAndItems(settings)
                }
            }
        }

        serviceScope.launch {
            AirCommandController.isVisible.collect { visible ->
                if (visible) {
                    ensureOverlayViewCreated()
                    updateCardWidthAndItems(currentSettings)
                    rootFrameLayout?.visibility = View.VISIBLE
                    rootFrameLayout?.alpha = 0f
                    rootFrameLayout?.translationX = 250f
                    rootFrameLayout?.animate()
                        ?.alpha(1f)
                        ?.translationX(0f)
                        ?.setDuration(160)
                        ?.start()
                } else {
                    rootFrameLayout?.animate()
                        ?.alpha(0f)
                        ?.translationX(250f)
                        ?.setDuration(160)
                        ?.withEndAction {
                            rootFrameLayout?.visibility = View.GONE
                        }
                        ?.start()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "air_command_channel",
                "Air Command Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = Notification.Builder(this, "air_command_channel")
                .setContentTitle("Air Command")
                .setContentText("Overlay active")
                .setSmallIcon(R.drawable.ic_menu_compass)
                .setOngoing(true)
                .build()
            try {
                startForeground(2002, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_HOLD -> showHold(this)
            ACTION_HIDE_HOLD -> hideHold(this)
            ACTION_TOGGLE -> toggle(this)
            ACTION_SHOW -> show(this)
            ACTION_HIDE -> hide(this)
        }
        return START_STICKY
    }

    private fun ensureOverlayViewCreated() {
        if (!Settings.canDrawOverlays(this) || isViewAttached) return

        val rootParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 24
        }

        val root = FrameLayout(this).apply {
            setOnClickListener { AirCommandController.hide() }
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0242424"))
                cornerRadius = 48f
                setStroke(2, Color.parseColor("#38FFFFFF"))
            }
            setOnClickListener { /* consume click inside card */ }
        }

        // Header Row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 16)
        }

        val titleText = TextView(this).apply {
            text = "Quick Actions"
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
        }

        val badge = TextView(this).apply {
            text = "0"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setPadding(12, 4, 12, 4)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2200E5FF"))
                cornerRadius = 20f
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 8
                rightMargin = 8
            }
            layoutParams = params
        }
        itemCountBadge = badge

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(12, 6, 12, 6)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22FFFFFF"))
                shape = GradientDrawable.OVAL
            }
            setOnClickListener { AirCommandController.hide() }
        }

        headerRow.addView(titleText)
        headerRow.addView(badge)
        headerRow.addView(spacer)
        headerRow.addView(closeBtn)
        card.addView(headerRow)

        val grid = GridLayout(this).apply {
            columnCount = 3
            alignmentMode = GridLayout.ALIGN_BOUNDS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        card.addView(grid)

        val density = resources.displayMetrics.density
        val baseWidthPx = (190f * density).toInt()

        val cardParams = FrameLayout.LayoutParams(
            baseWidthPx,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = 24
        }

        root.addView(card, cardParams)

        try {
            windowManager?.addView(root, rootParams)
            rootFrameLayout = root
            cardContainer = card
            gridLayout = grid
            isViewAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateCardWidthAndItems(settings: RemapperSettings) {
        val card = cardContainer ?: return
        val grid = gridLayout ?: return

        val density = resources.displayMetrics.density
        val baseWidthPx = (190f * density).toInt()
        val scale = settings.airCommandScale.coerceIn(0.65f, 1.30f)

        val params = card.layoutParams as? FrameLayout.LayoutParams
        if (params != null) {
            params.width = baseWidthPx
            card.layoutParams = params
        }

        card.pivotX = baseWidthPx.toFloat()
        card.pivotY = if (card.height > 0) card.height.toFloat() / 2f else 0f
        card.scaleX = scale
        card.scaleY = scale

        grid.removeAllViews()

        val toolbarItems = AirCommandItem.decodeList(settings.airCommandApps)
        itemCountBadge?.text = "${toolbarItems.size}"

        val tileWidthPx = (48f * density * scale).toInt().coerceAtLeast(36)
        val tileHeightPx = (54f * density * scale).toInt().coerceAtLeast(42)

        for (item in toolbarItems) {
            val tileLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(4, 8, 4, 8)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1AFFFFFF"))
                    cornerRadius = (14f * density * scale).coerceAtLeast(10f)
                }
                val gridParams = GridLayout.LayoutParams().apply {
                    width = tileWidthPx
                    height = tileHeightPx
                    setMargins(4, 4, 4, 4)
                }
                layoutParams = gridParams
                isClickable = true
                isFocusable = true
            }

            val iconContainer = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#3300E5FF"))
                    shape = GradientDrawable.OVAL
                }
                val p = (10f * density * scale).toInt().coerceAtLeast(4)
                setPadding(p, p, p, p)
            }

            val iconView = ImageView(this)
            val iconSizePx = (24f * density * scale).toInt().coerceAtLeast(16)
            val iconParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx).apply {
                gravity = Gravity.CENTER
            }

            val effectivePayload = item.payload.ifBlank { item.packageName }

            if ((item.type == "APP" || item.actionName == RemapAction.LAUNCH_APP.name) && effectivePayload.isNotBlank()) {
                val cachedIcon = appIconCache[effectivePayload]
                if (cachedIcon != null) {
                    iconView.setImageDrawable(cachedIcon)
                } else {
                    try {
                        val appIcon = packageManager.getApplicationIcon(effectivePayload)
                        appIconCache[effectivePayload] = appIcon
                        iconView.setImageDrawable(appIcon)
                    } catch (e: Exception) {
                        iconView.setImageResource(R.drawable.ic_menu_compass)
                    }
                }
            } else {
                val iconRes = getBuiltInActionIconRes(item.actionName)
                iconView.setImageResource(iconRes)
            }
            iconContainer.addView(iconView, iconParams)

            val labelText = TextView(this).apply {
                text = item.label.ifBlank { item.actionName }
                setTextColor(Color.WHITE)
                textSize = (9f * scale).coerceIn(7f, 13f)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, 4, 0, 0)
            }

            tileLayout.addView(iconContainer)
            tileLayout.addView(labelText)

            tileLayout.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                val defaultBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#1AFFFFFF"))
                    cornerRadius = (14f * density * scale).coerceAtLeast(10f)
                }
                val activeBg = GradientDrawable().apply {
                    setColor(Color.parseColor("#00E5FF"))
                    cornerRadius = (14f * density * scale).coerceAtLeast(10f)
                    setStroke(3, Color.WHITE)
                }

                v.background = activeBg
                tileLayout.animate().scaleX(1.12f).scaleY(1.12f).setDuration(100).withEndAction {
                    tileLayout.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }.start()

                v.postDelayed({
                    v.background = defaultBg
                }, 250)

                val action = RemapAction.fromNameOrDefault(item.actionName)
                ActionExecutor.executeAction(action, applicationContext, effectivePayload, null)

                if (AirCommandController.isContinuousHold) {
                    AirCommandController.hide()
                }
            }

            grid.addView(tileLayout)
        }
    }

    private fun getBuiltInActionIconRes(actionName: String): Int {
        return when (actionName) {
            RemapAction.SCREENSHOT.name -> R.drawable.ic_menu_camera
            RemapAction.PLAY_PAUSE.name -> R.drawable.ic_media_play
            RemapAction.NEXT_TRACK.name -> R.drawable.ic_media_next
            RemapAction.PREV_TRACK.name -> R.drawable.ic_media_previous
            RemapAction.VOLUME_UP.name -> R.drawable.ic_lock_silent_mode_off
            RemapAction.VOLUME_DOWN.name -> R.drawable.ic_lock_silent_mode
            RemapAction.FLASHLIGHT.name -> R.drawable.ic_menu_day
            RemapAction.HOME.name -> R.drawable.ic_menu_view
            RemapAction.BACK.name -> R.drawable.ic_menu_revert
            RemapAction.RECENTS.name -> R.drawable.ic_menu_recent_history
            else -> R.drawable.ic_menu_compass
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        if (rootFrameLayout != null && isViewAttached) {
            try {
                windowManager?.removeView(rootFrameLayout)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            rootFrameLayout = null
            isViewAttached = false
        }
        super.onDestroy()
    }
}
