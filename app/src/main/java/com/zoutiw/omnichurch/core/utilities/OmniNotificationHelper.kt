package com.zoutiw.omnichurch.core.utilities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zoutiw.omnichurch.MainActivity
import com.zoutiw.omnichurch.R

/**
 * Handles Android system notifications for OmniChurch Pro members when a Church Admin
 * publishes a new Announcement (Anons) or Service (Sèvis).
 *
 * Security & Business Rule:
 * - Church Admins who publish an announcement or service MUST NEVER receive the notification
 *   because they are the sender.
 */
object OmniNotificationHelper {
  const val CHANNEL_ANNOUNCEMENTS = "omnichurch_announcements_channel"
  const val CHANNEL_SERVICES = "omnichurch_services_channel"
  const val CHANNEL_ATTENDANCE = "omnichurch_attendance_channel"

  const val EXTRA_TARGET_DESTINATION = "omni_notif_target_destination"
  const val EXTRA_NOTIFICATION_ID = "omni_notif_doc_id"

  fun ensureChannelsCreated(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val announcementChannel =
      NotificationChannel(
          CHANNEL_ANNOUNCEMENTS,
          "Anons Legliz",
          NotificationManager.IMPORTANCE_HIGH,
        )
        .apply {
          description = "Notifikasyon lè administratè legliz la pibliye yon nouvo anons"
          enableLights(true)
          lightColor = Color.rgb(0, 174, 239)
          enableVibration(true)
        }

    val serviceChannel =
      NotificationChannel(
          CHANNEL_SERVICES,
          "Sèvis Legliz",
          NotificationManager.IMPORTANCE_HIGH,
        )
        .apply {
          description = "Notifikasyon lè administratè legliz la pwograme oswa pibliye yon sèvis"
          enableLights(true)
          lightColor = Color.rgb(245, 158, 11)
          enableVibration(true)
        }

    val attendanceChannel =
      NotificationChannel(
          CHANNEL_ATTENDANCE,
          "Prezans Manm",
          NotificationManager.IMPORTANCE_DEFAULT,
        )
        .apply {
          description = "Konfimasyon lè prezans ou anrejistre nan yon sèvis"
        }

    manager.createNotificationChannels(
      listOf(announcementChannel, serviceChannel, attendanceChannel)
    )
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun showMemberNotification(
    context: Context,
    uniqueKey: String,
    title: String,
    message: String,
    type: String,
    notificationDocId: String? = null,
  ): Boolean {
    ensureChannelsCreated(context)
    if (!hasNotificationPermission(context)) return false
    val notificationManager = NotificationManagerCompat.from(context)
    if (!notificationManager.areNotificationsEnabled()) return false

    val normalizedType = type.trim().lowercase()
    val channelId =
      when (normalizedType) {
        "service" -> CHANNEL_SERVICES
        "attendance" -> CHANNEL_ATTENDANCE
        else -> CHANNEL_ANNOUNCEMENTS
      }

    // Services open on HOME (Fil Aktyalite); Announcements open on NOTIFICATIONS (Anons)
    val targetDestination =
      when (normalizedType) {
        "service" -> "HOME"
        "announcement" -> "NOTIFICATIONS"
        else -> "NOTIFICATIONS"
      }

    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_TARGET_DESTINATION, targetDestination)
        if (!notificationDocId.isNullOrBlank()) {
          putExtra(EXTRA_NOTIFICATION_ID, notificationDocId)
        }
      }

    val pendingIntentFlags =
      PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    val notificationId = uniqueKey.hashCode()
    val pendingIntent =
      PendingIntent.getActivity(context, notificationId, intent, pendingIntentFlags)

    val notification =
      NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_omni_notification)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(title))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(
          if (normalizedType == "service") NotificationCompat.CATEGORY_EVENT
          else NotificationCompat.CATEGORY_MESSAGE
        )
        .setColor(Color.rgb(0, 174, 239))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    return try {
      notificationManager.notify(notificationId, notification)
      true
    } catch (_: SecurityException) {
      false
    }
  }
}
