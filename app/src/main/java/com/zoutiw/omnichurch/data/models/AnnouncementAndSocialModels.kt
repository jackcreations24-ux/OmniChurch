package com.zoutiw.omnichurch.data.models

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Immutable
@IgnoreExtraProperties
data class Announcement(
  @DocumentId val id: String = "",
  val churchId: String = "",
  val title: String = "",
  val content: String = "",
  val category: String = "anons",
  val department: String = "",
  val date: String? = null,
  val time: String? = null,
  val targetAudience: String = "Tout moun",
  val priority: String = "normal",
  val expiresAt: String? = null,
  @JvmField @field:PropertyName("isPinned") val isPinned: Boolean = false,
  @JvmField @field:PropertyName("isPublished") val isPublished: Boolean = true,
  @JvmField @field:PropertyName("showToastOnLogin") val showToastOnLogin: Boolean = false,
  val toastAlertLevel: String = "info",
  @JvmField @field:PropertyName("isScheduledNotification") val isScheduledNotification: Boolean = false,
  val scheduledNotificationDate: String? = null,
  val posterUrl: String? = null,
  val imageUrl: String? = null,
  val imageCaption: String? = null,
  val serviceId: String? = null,
  val authorId: String = "",
  val authorName: String = "",
  val authorPhotoUrl: String? = null,
  val titleHt: String? = null,
  val titleFr: String? = null,
  val titleEn: String? = null,
  val titleEs: String? = null,
  val contentHt: String? = null,
  val contentFr: String? = null,
  val contentEn: String? = null,
  val contentEs: String? = null,
  val reactionsCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val effectiveImageUrl: String?
    get() = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
      ?: posterUrl?.trim()?.takeIf { it.isNotEmpty() }

  fun localizedTitle(languageCode: String): String {
    val localized =
      when (languageCode.trim().lowercase()) {
        "fr" -> titleFr
        "en" -> titleEn
        "es" -> titleEs
        else -> titleHt
      }?.trim()
    return localized?.takeIf { it.isNotEmpty() } ?: title.trim()
  }

  fun localizedContent(languageCode: String): String {
    val localized =
      when (languageCode.trim().lowercase()) {
        "fr" -> contentFr
        "en" -> contentEn
        "es" -> contentEs
        else -> contentHt
      }?.trim()
    return localized?.takeIf { it.isNotEmpty() } ?: content.trim()
  }

  val sortTimestampMillis: Long
    get() = createdAt?.toDate()?.time ?: updatedAt?.toDate()?.time ?: 0L
}

@Immutable
@IgnoreExtraProperties
data class AnnouncementReaction(
  @DocumentId val id: String = "",
  val targetId: String = "",
  val churchId: String = "",
  val userId: String = "",
  val userName: String = "",
  val reactionType: String = "pray",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class OmniReactionType(val key: String, val labelHt: String) {
  LIKE("like", "J'aime"),
  LOVE("love", "Lanmou"),
  PRAY("pray", "Amèn"),
  HAHA("haha", "Haha"),
  WOW("wow", "Waw"),
  SAD("sad", "Tris"),
  CLAP("clap", "Bravo");

  companion object {
    fun fromKey(raw: String?): OmniReactionType {
      return when (raw?.trim()?.lowercase()) {
        "like" -> LIKE
        "love" -> LOVE
        "pray", "amen" -> PRAY
        "haha" -> HAHA
        "wow" -> WOW
        "sad" -> SAD
        "clap" -> CLAP
        else -> PRAY
      }
    }
  }
}

@Immutable
data class TargetReactionSummary(
  val totalCount: Int = 0,
  val topReactions: List<OmniReactionType> = emptyList(),
  val currentUserReaction: OmniReactionType? = null,
)

@Immutable
@IgnoreExtraProperties
data class AnnouncementComment(
  @DocumentId val id: String = "",
  val targetId: String = "",
  val churchId: String = "",
  val userId: String = "",
  val userName: String = "",
  val userPhotoUrl: String? = null,
  val content: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Immutable
@IgnoreExtraProperties
data class ChurchNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val churchId: String = "",
  val title: String = "",
  val message: String = "",
  val type: String = "system",
  @JvmField @field:PropertyName("isRead") val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)
