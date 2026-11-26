package com.zoutiw.omnichurch.core.utilities

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrCodeGenerator {
  /**
   * Generates a high-contrast pure white background QR code with dark modules and an optional
   * subtle center OmniChurch flame emblem (using High error correction level H so scannability is
   * 100% preserved under all lighting conditions).
   */
  suspend fun generateQrBitmap(
    token: String,
    sizePx: Int = 768,
    includeCenterEmblem: Boolean = true,
  ): Bitmap? =
    withContext(Dispatchers.Default) {
      if (token.isBlank()) return@withContext null
      try {
        val hints =
          mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
          )
        val bitMatrix = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        val darkModuleColor = Color.rgb(7, 21, 47) // Deep OmniChurch Navy #07152F for crisp contrast
        for (y in 0 until height) {
          val offset = y * width
          for (x in 0 until width) {
            pixels[offset + x] = if (bitMatrix.get(x, y)) darkModuleColor else Color.WHITE
          }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        if (includeCenterEmblem) {
          drawCenterFlameBadge(bitmap)
        }
        bitmap
      } catch (e: Exception) {
        null
      }
    }

  private fun drawCenterFlameBadge(bitmap: Bitmap) {
    val canvas = Canvas(bitmap)
    val cx = bitmap.width / 2f
    val cy = bitmap.height / 2f
    val badgeRadius = bitmap.width * 0.105f

    val bgPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
      }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 174, 239)
        style = Paint.Style.STROKE
        strokeWidth = bitmap.width * 0.008f
      }
    val rect = RectF(cx - badgeRadius, cy - badgeRadius, cx + badgeRadius, cy + badgeRadius)
    val corner = badgeRadius * 0.35f
    canvas.drawRoundRect(rect, corner, corner, bgPaint)
    canvas.drawRoundRect(rect, corner, corner, borderPaint)

    // Left orange-gold flame wing
    val flamePaintLeft =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            cx - badgeRadius * 0.55f,
            cy - badgeRadius * 0.65f,
            cx,
            cy + badgeRadius * 0.65f,
            Color.rgb(255, 194, 71),
            Color.rgb(255, 140, 0),
            Shader.TileMode.CLAMP,
          )
        style = Paint.Style.FILL
      }
    val leftPath =
      Path().apply {
        moveTo(cx, cy - badgeRadius * 0.65f)
        cubicTo(
          cx - badgeRadius * 0.55f,
          cy - badgeRadius * 0.15f,
          cx - badgeRadius * 0.55f,
          cy + badgeRadius * 0.45f,
          cx - badgeRadius * 0.04f,
          cy + badgeRadius * 0.62f,
        )
        cubicTo(
          cx - badgeRadius * 0.25f,
          cy + badgeRadius * 0.25f,
          cx - badgeRadius * 0.15f,
          cy - badgeRadius * 0.15f,
          cx,
          cy - badgeRadius * 0.65f,
        )
        close()
      }
    canvas.drawPath(leftPath, flamePaintLeft)

    // Right electric-blue flame wing
    val flamePaintRight =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            cx,
            cy - badgeRadius * 0.45f,
            cx + badgeRadius * 0.55f,
            cy + badgeRadius * 0.65f,
            Color.rgb(0, 200, 255),
            Color.rgb(8, 123, 255),
            Shader.TileMode.CLAMP,
          )
        style = Paint.Style.FILL
      }
    val rightPath =
      Path().apply {
        moveTo(cx + badgeRadius * 0.08f, cy - badgeRadius * 0.42f)
        cubicTo(
          cx + badgeRadius * 0.55f,
          cy - badgeRadius * 0.05f,
          cx + badgeRadius * 0.55f,
          cy + badgeRadius * 0.48f,
          cx + badgeRadius * 0.04f,
          cy + badgeRadius * 0.62f,
        )
        cubicTo(
          cx + badgeRadius * 0.24f,
          cy + badgeRadius * 0.25f,
          cx + badgeRadius * 0.18f,
          cy - badgeRadius * 0.1f,
          cx + badgeRadius * 0.08f,
          cy - badgeRadius * 0.42f,
        )
        close()
      }
    canvas.drawPath(rightPath, flamePaintRight)
  }

  /**
   * Saves the member QR bitmap to the Android Gallery using MediaStore without requiring broad
   * storage permissions.
   */
  suspend fun saveQrToGallery(
    context: Context,
    bitmap: Bitmap,
    memberCode: String,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      try {
        val filename = "OmniChurch_QR_${memberCode}_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val contentValues =
          ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/OmniChurch",
              )
              put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
          }

        val imageUri =
          resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext Result.failure(IllegalStateException("MediaStore URI null"))

        resolver.openOutputStream(imageUri)?.use { outStream ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
          resolver.update(imageUri, contentValues, null, null)
        }
        Result.success(filename)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  /** Shares the QR bitmap and optional canonical verification link via Android FileProvider chooser. */
  suspend fun shareQrCardImage(
    context: Context,
    bitmap: Bitmap,
    memberName: String,
    memberCode: String,
    churchName: String,
    verificationUrl: String? = null,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        val cacheDir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
        val file = File(cacheDir, "OmniChurch_ID_${memberCode}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val uri =
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        withContext(Dispatchers.Main) {
          val shareText =
            if (!verificationUrl.isNullOrBlank()) {
              "$churchName • $memberName (ID: $memberCode)\n$verificationUrl"
            } else {
              "$churchName • $memberName (ID: $memberCode)"
            }
          val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "image/png"
              putExtra(Intent.EXTRA_STREAM, uri)
              putExtra(Intent.EXTRA_TEXT, shareText)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          val chooser =
            Intent.createChooser(shareIntent, churchName).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
          context.startActivity(chooser)
        }
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
}
