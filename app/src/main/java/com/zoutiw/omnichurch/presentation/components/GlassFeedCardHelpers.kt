package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Church
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.PureWhite

internal data class ServiceOfficialRoleDisplay(
  val roleLabel: String,
  val personName: String,
  val photoUrl: String?,
  val accentColor: Color,
  val badgeBackgroundColor: Color,
)

@Composable
internal fun ServiceOfficialAvatarBadge(
  official: ServiceOfficialRoleDisplay,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(vertical = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    MemberAvatar3D(
      fullName = official.personName,
      size = 44.dp,
      photoUrl = official.photoUrl,
      borderColor = official.accentColor.copy(alpha = 0.72f),
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = official.roleLabel,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          lineHeight = 12.sp,
          fontWeight = FontWeight.Bold,
        ),
      color = official.accentColor,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )

    Text(
      text = official.personName,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.5.sp,
          lineHeight = 14.sp,
          fontWeight = FontWeight.SemiBold,
        ),
      color = PureWhite,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
  }
}

internal fun resolveMemberPhotoForRole(personName: String, churchMembers: List<Member>): String? {
  val cleanTarget = personName.trim()
  if (cleanTarget.isBlank() || churchMembers.isEmpty()) return null
  val normalizedTarget = cleanTarget.lowercase(java.util.Locale.getDefault())
  val exactMatch =
    churchMembers.firstOrNull { member ->
      val full = member.effectiveFullName.trim().lowercase(java.util.Locale.getDefault())
      val rawFull = member.fullName.trim().lowercase(java.util.Locale.getDefault())
      val prefixed =
        listOfNotNull(
            member.titlePrefix?.trim()?.takeIf { it.isNotBlank() },
            member.effectiveFullName.trim().takeIf { it.isNotBlank() },
          )
          .joinToString(" ")
          .lowercase(java.util.Locale.getDefault())
      normalizedTarget == full ||
        normalizedTarget == rawFull ||
        (prefixed.isNotBlank() && normalizedTarget == prefixed)
    }
  val resolvedMember =
    exactMatch
      ?: churchMembers.firstOrNull { member ->
        val full = member.effectiveFullName.trim().lowercase(java.util.Locale.getDefault())
        full.length >= 4 && normalizedTarget.contains(full)
      }
  return resolvedMember?.effectivePhotoUrl
}

@Composable
fun ChurchProfileAvatar(
  logoUrl: String?,
  churchName: String,
  size: Dp = 44.dp,
  modifier: Modifier = Modifier,
) {
  val cleanUrl = remember(logoUrl) { logoUrl?.trim()?.takeIf { it.isNotEmpty() } }
  val decodedBitmap =
    remember(cleanUrl) {
      cleanUrl?.let { ImageCompressor.decodeBase64DataUrlToBitmap(it) }
    }
  var remoteLoadFailed by remember(cleanUrl) { mutableStateOf(false) }
  val initials =
    remember(churchName) {
      churchName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "" }
    }

  Box(
    modifier =
      modifier
        .size(size)
        .shadow(8.dp, CircleShape, ambientColor = ElectricBlue)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(
              BlueNeon,
              ElectricBlue,
              Color(0xFF1D4ED8),
            )
          )
        )
        .border(1.2.dp, PureWhite.copy(alpha = 0.72f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    when {
      decodedBitmap != null -> {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = churchName.ifBlank { "Logo Legliz" },
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      cleanUrl != null && !remoteLoadFailed -> {
        AsyncImage(
          model = cleanUrl,
          contentDescription = churchName.ifBlank { "Logo Legliz" },
          contentScale = ContentScale.Crop,
          onError = { remoteLoadFailed = true },
          modifier = Modifier.fillMaxSize(),
        )
      }
      initials.isNotBlank() -> {
        Text(
          text = initials,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
        )
      }
      else -> {
        Icon(
          imageVector = Icons.Default.Church,
          contentDescription = churchName.ifBlank { "Logo Legliz" },
          tint = PureWhite,
          modifier = Modifier.size(size * 0.5f),
        )
      }
    }
  }
}

internal fun formatServiceTimestamp(
  service: ChurchService,
  languageCode: String = "ht",
): String {
  val lang = languageCode.trim().lowercase()
  val ts = service.createdAt
  if (ts != null) {
    val dateObj = ts.toDate()
    val nowCal = java.util.Calendar.getInstance()
    val targetCal = java.util.Calendar.getInstance().apply { time = dateObj }
    val timeFormatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    val timeStr = timeFormatter.format(dateObj)

    val isSameDay =
      nowCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        nowCal.get(java.util.Calendar.DAY_OF_YEAR) == targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isSameDay) {
      return when (lang) {
        "fr" -> "Publié aujourd'hui • $timeStr"
        "en" -> "Published today • $timeStr"
        "es" -> "Publicado hoy • $timeStr"
        else -> "Pibliye jodi a • $timeStr"
      }
    }

    val yesterdayCal =
      (nowCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val isYesterday =
      yesterdayCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) ==
          targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isYesterday) {
      return when (lang) {
        "fr" -> "Publié hier • $timeStr"
        "en" -> "Published yesterday • $timeStr"
        "es" -> "Publicado ayer • $timeStr"
        else -> "Pibliye yè • $timeStr"
      }
    }

    val dateFormatter =
      java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault())
    val formatted = dateFormatter.format(dateObj)
    return when (lang) {
      "fr" -> "Publié le $formatted"
      "en" -> "Published $formatted"
      "es" -> "Publicado $formatted"
      else -> "Pibliye $formatted"
    }
  }
  val prefix =
    when (lang) {
      "fr" -> "Publié"
      "en" -> "Published"
      "es" -> "Publicado"
      else -> "Pibliye"
    }
  return "$prefix • ${service.formattedScheduleWithDay(languageCode)}"
}

internal fun formatAnnouncementTimestamp(announcement: Announcement): String {
  val ts = announcement.createdAt
  if (ts != null) {
    val dateObj = ts.toDate()
    val nowCal = java.util.Calendar.getInstance()
    val targetCal = java.util.Calendar.getInstance().apply { time = dateObj }
    val timeFormatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    val timeStr = timeFormatter.format(dateObj)

    val isSameDay =
      nowCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        nowCal.get(java.util.Calendar.DAY_OF_YEAR) == targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isSameDay) {
      return "Jodi a • $timeStr"
    }

    val yesterdayCal =
      (nowCal.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.DAY_OF_YEAR, -1)
      }
    val isYesterday =
      yesterdayCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) ==
          targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isYesterday) {
      return "Yè • $timeStr"
    }

    val fullFormatter =
      java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault())
    return fullFormatter.format(dateObj)
  }

  val fallbackDate = announcement.date?.trim().orEmpty()
  return if (fallbackDate.isNotEmpty()) fallbackDate else "Kounye a"
}
