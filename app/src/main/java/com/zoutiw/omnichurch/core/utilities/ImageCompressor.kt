package com.zoutiw.omnichurch.core.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageCompressor {
  private const val MAX_DIMENSION = 400
  private const val COMPRESS_QUALITY = 80

  fun compressBitmapToBase64(
    bitmap: Bitmap,
    maxDimension: Int = MAX_DIMENSION,
    quality: Int = COMPRESS_QUALITY,
  ): String {
    val origWidth = bitmap.width
    val origHeight = bitmap.height

    val scale =
      if (origWidth > maxDimension || origHeight > maxDimension) {
        val ratio = maxDimension.toFloat() / maxOf(origWidth, origHeight).toFloat()
        ratio
      } else {
        1.0f
      }

    val targetWidth = (origWidth * scale).toInt().coerceAtLeast(1)
    val targetHeight = (origHeight * scale).toInt().coerceAtLeast(1)

    val scaledBitmap =
      if (scale < 1.0f) {
        Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
      } else {
        bitmap
      }

    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    val bytes = outputStream.toByteArray()
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64"
  }

  suspend fun compressUriToBase64(
    context: Context,
    uri: Uri,
    maxDimension: Int = MAX_DIMENSION,
    quality: Int = COMPRESS_QUALITY,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
          return@withContext Result.failure(IllegalArgumentException("Imposib pou li imaj la."))
        }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val sampleSize = calculateInSampleSize(options, maxDimension, maxDimension)

        val readStream: InputStream? = context.contentResolver.openInputStream(uri)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = BitmapFactory.decodeStream(readStream, null, decodeOptions)
        readStream?.close()

        if (decoded == null) {
          return@withContext Result.failure(IllegalStateException("Fòma imaj la pa rekonèt."))
        }

        val orientationAdjusted =
          try {
            context.contentResolver.openInputStream(uri)?.use { exifStream ->
              val exif = ExifInterface(exifStream)
              val orientation =
                exif.getAttributeInt(
                  ExifInterface.TAG_ORIENTATION,
                  ExifInterface.ORIENTATION_NORMAL,
                )
              rotateBitmapIfRequired(decoded, orientation)
            } ?: decoded
          } catch (_: Exception) {
            decoded
          }

        val base64DataUrl = compressBitmapToBase64(orientationAdjusted, maxDimension, quality)
        Result.success(base64DataUrl)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
  ): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
      val halfHeight = height / 2
      val halfWidth = width / 2
      while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
        inSampleSize *= 2
      }
    }
    return inSampleSize
  }

  private val base64BitmapCache = android.util.LruCache<Int, Bitmap>(32)

  fun decodeBase64DataUrlToBitmap(photoSource: String?): Bitmap? {
    val clean = photoSource?.trim().orEmpty()
    if (clean.isEmpty()) return null
    if (
      clean.startsWith("http://", ignoreCase = true) ||
        clean.startsWith("https://", ignoreCase = true) ||
        clean.startsWith("content://", ignoreCase = true) ||
        clean.startsWith("file://", ignoreCase = true) ||
        clean.startsWith("gs://", ignoreCase = true)
    ) {
      return null
    }

    val cacheKey = clean.hashCode()
    base64BitmapCache.get(cacheKey)?.let { cached ->
      if (!cached.isRecycled) return cached
    }

    return try {
      val base64Payload =
        if (clean.contains("base64,", ignoreCase = true)) {
          clean.substringAfter("base64,").trim()
        } else if (clean.startsWith("data:", ignoreCase = true) && clean.contains(",")) {
          clean.substringAfter(",").trim()
        } else {
          clean
        }
      if (base64Payload.length < 32) return null
      val decodedBytes = Base64.decode(base64Payload, Base64.DEFAULT)
      if (decodedBytes.isEmpty()) return null
      val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
      if (bitmap != null) {
        base64BitmapCache.put(cacheKey, bitmap)
      }
      bitmap
    } catch (_: Exception) {
      null
    }
  }

  private fun rotateBitmapIfRequired(bitmap: Bitmap, orientation: Int): Bitmap {
    return when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
      else -> bitmap
    }
  }

  private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
}
