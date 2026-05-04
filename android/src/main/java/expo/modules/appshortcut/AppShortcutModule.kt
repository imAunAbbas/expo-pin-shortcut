package expo.modules.appshortcut

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PinShortcutOptions : Record {
  @Field var id: String = ""
  @Field var label: String = ""
  @Field var uri: String = ""
  @Field var iconUrl: String? = null
}

class AppShortcutModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("AppShortcut")

    Function("isSupported") {
      val ctx = appContext.reactContext ?: return@Function false
      ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)
    }

    AsyncFunction("pin").Coroutine { options: PinShortcutOptions ->
      pinShortcut(options)
    }
  }

  private suspend fun pinShortcut(options: PinShortcutOptions): Boolean = withContext(Dispatchers.IO) {
    try {
      val ctx = appContext.reactContext
          ?: throw CodedException("E_NO_CONTEXT", "React context unavailable", null)

      if (options.id.isBlank() || options.label.isBlank() || options.uri.isBlank()) {
        throw CodedException("E_INVALID_ARGS", "id, label, and uri are required", null)
      }

      if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) {
        throw CodedException("E_UNSUPPORTED", "Launcher does not support pinned shortcuts", null)
      }

      val deepLink = try {
        Uri.parse(options.uri)
      } catch (e: Exception) {
        throw CodedException("E_INVALID_URI", "Could not parse uri: ${options.uri}", e)
      }

      val intent = Intent(Intent.ACTION_VIEW, deepLink).apply {
        setPackage(ctx.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }

      val icon = buildIcon(ctx, options.iconUrl)
      val shortcutId = "app-${options.id}"

      val shortcut = ShortcutInfoCompat.Builder(ctx, shortcutId)
          .setShortLabel(options.label)
          .setLongLabel(options.label)
          .setIcon(icon)
          .setIntent(intent)
          .setActivity(ComponentName(ctx, ShortcutHostActivity::class.java))
          .build()

      val callback = ShortcutManagerCompat.createShortcutResultIntent(ctx, shortcut)
      val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }
      val pending = PendingIntent.getBroadcast(ctx, 0, callback, flags)

      val accepted = ShortcutManagerCompat.requestPinShortcut(ctx, shortcut, pending.intentSender)
      Log.i(TAG, "requestPinShortcut accepted=$accepted id=${options.id}")
      accepted
    } catch (e: CodedException) {
      Log.e(TAG, "pinShortcut failed code=${e.code}", e)
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "pinShortcut unexpected", e)
      throw CodedException("E_PIN_FAILED", "${e.javaClass.simpleName}: ${e.message ?: "unknown"}", e)
    }
  }

  companion object {
    private const val TAG = "AppShortcut"
  }

  private fun buildIcon(ctx: Context, iconUrl: String?): IconCompat {
    val bitmap = downloadBitmap(iconUrl)
    return if (bitmap != null) {
      IconCompat.createWithAdaptiveBitmap(toAdaptiveBitmap(bitmap))
    } else {
      val fallbackId = ctx.resources.getIdentifier("ic_launcher", "mipmap", ctx.packageName)
      if (fallbackId != 0) {
        IconCompat.createWithResource(ctx, fallbackId)
      } else {
        IconCompat.createWithAdaptiveBitmap(blankAdaptiveBitmap())
      }
    }
  }

  private fun downloadBitmap(urlString: String?): Bitmap? {
    if (urlString.isNullOrBlank()) return null
    return try {
      val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 8000
        instanceFollowRedirects = true
      }
      conn.inputStream.use { BitmapFactory.decodeStream(it) }
    } catch (_: Exception) {
      null
    }
  }

  // Adaptive icons fill 108x108dp with the inner ~72x72dp visible after launcher
  // masking. Render the favicon centered on a white square so it survives any
  // mask shape the launcher applies.
  private fun toAdaptiveBitmap(source: Bitmap): Bitmap {
    val size = 432
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(Color.WHITE)

    val padding = (size * 0.20f).toInt()
    val target = RectF(
        padding.toFloat(),
        padding.toFloat(),
        (size - padding).toFloat(),
        (size - padding).toFloat()
    )
    val src = Rect(0, 0, source.width, source.height)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, src, target, paint)
    return output
  }

  private fun blankAdaptiveBitmap(): Bitmap {
    val size = 432
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    Canvas(output).drawColor(Color.WHITE)
    return output
  }
}
