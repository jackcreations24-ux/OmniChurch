package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
internal fun ServiceCardHeaderRow(
  serviceId: String,
  primaryAuthorTitle: String,
  secondaryAuthorLabel: String?,
  churchLogoUrl: String?,
  publishedTimestampText: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ChurchProfileAvatar(
      logoUrl = churchLogoUrl,
      churchName = primaryAuthorTitle,
      size = 44.dp,
      modifier = Modifier.testTag("service_church_avatar_$serviceId"),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = primaryAuthorTitle,
        style = MaterialTheme.typography.titleSmall,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("service_author_title_$serviceId"),
      )
      if (!secondaryAuthorLabel.isNullOrBlank()) {
        Text(
          text = secondaryAuthorLabel,
          style = MaterialTheme.typography.labelSmall,
          color = BlueNeon.copy(alpha = 0.9f),
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(top = 1.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Schedule,
          contentDescription = null,
          tint = BlueNeon.copy(alpha = 0.85f),
          modifier = Modifier.size(12.dp),
        )
        Text(
          text = publishedTimestampText,
          style = MaterialTheme.typography.labelSmall,
          color = MutedGlassText.copy(alpha = 0.92f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("service_timestamp_$serviceId"),
        )
      }
    }
  }
}

@Composable
internal fun ServiceCardHeadlineAndMedia(
  service: ChurchService,
  serviceDateSubtitle: String,
  mediaUrl: String?,
  officialRoleAvatars: List<ServiceOfficialRoleDisplay>,
  onImageClick: () -> Unit,
) {
  val hasThemeOrPassage = !service.theme.isNullOrBlank() || !service.passage.isNullOrBlank()
  val hasDescription =
    !service.description.isNullOrBlank() && service.description != service.theme

  Column(
    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (serviceDateSubtitle.isNotBlank()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
          Modifier.fillMaxWidth()
            .padding(bottom = 2.dp)
            .testTag("service_schedule_header_${service.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = serviceDateSubtitle,
          style = MaterialTheme.typography.titleSmall,
          color = BrandGold,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Text(
      text = service.effectiveTitle,
      style = MaterialTheme.typography.titleLarge,
      color = PureWhite,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.testTag("service_headline_title_${service.id}"),
    )

    if (hasThemeOrPassage) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!service.theme.isNullOrBlank()) {
          Text(
            text = "✨ Tèm: ${service.theme}",
            style = MaterialTheme.typography.bodyMedium,
            color = BrandGold,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f, fill = false),
          )
        }
        if (!service.passage.isNullOrBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.MenuBook,
              contentDescription = null,
              tint = BrandGold,
              modifier = Modifier.size(13.dp),
            )
            Text(
              text = service.passage,
              style = MaterialTheme.typography.labelMedium,
              color = GlassWhite.copy(alpha = 0.92f),
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }

    if (hasDescription) {
      Text(
        text = service.description.orEmpty(),
        style = MaterialTheme.typography.bodyMedium,
        color = GlassWhite.copy(alpha = 0.92f),
      )
    }
  }

  if (!mediaUrl.isNullOrBlank()) {
    AnnouncementMediaShowcase(
      imageSource = mediaUrl,
      title = service.effectiveTitle,
      announcementId = "srv_${service.id}",
      onImageClick = onImageClick,
      useFitScale = true,
      modifier = Modifier.fillMaxWidth().testTag("service_media_${service.id}"),
    )
  } else {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .height(144.dp),
    ) {
      WorshipStageBannerCanvas(modifier = Modifier.fillMaxSize())
    }
  }

  if (officialRoleAvatars.isNotEmpty()) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 10.dp)
          .testTag("service_officials_overlay_${service.id}"),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.Top,
    ) {
      officialRoleAvatars.forEach { official ->
        ServiceOfficialAvatarBadge(
          official = official,
          modifier =
            if (officialRoleAvatars.size == 1) {
              Modifier.widthIn(min = 96.dp, max = 160.dp)
            } else {
              Modifier.weight(1f)
            },
        )
      }
    }
  }
}
