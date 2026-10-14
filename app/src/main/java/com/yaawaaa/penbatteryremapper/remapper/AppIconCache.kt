package com.yaawaaa.penbatteryremapper.remapper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    // Cache max 80 downscaled icon bitmaps (approx. ~30KB each = ~2.4MB max memory)
    private val lruCache = LruCache<String, Bitmap>(80)

    fun get(packageName: String): Bitmap? {
        return lruCache.get(packageName)
    }

    fun put(packageName: String, bitmap: Bitmap) {
        lruCache.put(packageName, bitmap)
    }

    fun clear() {
        lruCache.evictAll()
    }

    suspend fun getOrLoadIcon(context: Context, packageName: String, targetSizePx: Int = 96): Bitmap? = withContext(Dispatchers.IO) {
        if (packageName.isBlank()) return@withContext null
        val cached = lruCache.get(packageName)
        if (cached != null) return@withContext cached

        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable, targetSizePx, targetSizePx)
            if (bitmap != null) {
                lruCache.put(packageName, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, widthPx: Int, heightPx: Int): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                Bitmap.createScaledBitmap(drawable.bitmap, widthPx, heightPx, true)
            } else {
                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    val bitmapState = remember(packageName) { mutableStateOf<ImageBitmap?>(AppIconCache.get(packageName)?.asImageBitmap()) }

    LaunchedEffect(packageName) {
        if (bitmapState.value == null && packageName.isNotBlank()) {
            val bitmap = AppIconCache.getOrLoadIcon(context, packageName)
            if (bitmap != null) {
                bitmapState.value = bitmap.asImageBitmap()
            }
        }
    }

    return bitmapState.value
}
