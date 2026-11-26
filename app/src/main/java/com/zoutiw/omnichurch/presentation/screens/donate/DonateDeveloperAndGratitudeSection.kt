package com.zoutiw.omnichurch.presentation.screens.donate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.donations.LocalizedDonationCopy
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
internal fun DonateHeroSection(
  kicker: String,
  title: String,
  subtitle: String,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(BrandGold.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Favorite,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(20.dp),
        )
      }
      Text(
        text = kicker,
        style = MaterialTheme.typography.labelMedium,
        color = BrandGold,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
      )
    }

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 32.sp),
      color = PureWhite,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.testTag("donate_hero_title"),
    )

    if (subtitle.isNotBlank()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = MutedGlassText,
        modifier = Modifier.testTag("donate_hero_subtitle"),
      )
    }
  }
}

@Composable
internal fun DonateDeveloperSection(
  config: DeveloperDonationConfig,
  localizedCopy: LocalizedDonationCopy,
  copy: DonateScreenCopy,
  copiedKey: String?,
  onCopy: (key: String, label: String, value: String) -> Unit,
) {
  val initials =
    remember(config.developerName) {
      config.developerName
        .trim()
        .split(Regex("\\s+"))
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)
        .ifEmpty { "OC" }
    }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = copy.developerSectionLabel,
      style = MaterialTheme.typography.labelLarge,
      color = BrandGold,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.4.sp,
    )

    Surface(
      modifier = Modifier.fillMaxWidth().testTag("donate_developer_section"),
      shape = RoundedCornerShape(20.dp),
      color = DonateFlatSurface,
      tonalElevation = 0.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandGold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = initials,
              style = MaterialTheme.typography.titleMedium,
              color = BrandGold,
              fontWeight = FontWeight.ExtraBold,
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = config.developerName,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            if (localizedCopy.developerRole.isNotBlank()) {
              Text(
                text = localizedCopy.developerRole,
                style = MaterialTheme.typography.bodySmall,
                color = BrandGold,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }

        if (localizedCopy.developerBio.isNotBlank()) {
          Text(
            text = localizedCopy.developerBio,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = GlassWhite.copy(alpha = 0.90f),
          )
        }

        val hasEmail = config.developerEmail.isNotBlank()
        val hasPhone = config.developerPhone.isNotBlank()
        if (hasEmail || hasPhone) {
          HorizontalDivider(color = DonateFlatDivider, thickness = 0.8.dp)
          Text(
            text = copy.directContactLabel,
            style = MaterialTheme.typography.labelMedium,
            color = BrandGold,
            fontWeight = FontWeight.SemiBold,
          )
        }

        if (hasEmail) {
          DonateCopyableDetailRow(
            fieldKey = "developer_email",
            label = copy.developerEmailLabel,
            value = config.developerEmail,
            isCopied = copiedKey == "developer_email",
            copyLabel = copy.copyButtonCd,
            copiedLabel = copy.copiedFeedback,
            onCopy = { onCopy("developer_email", copy.developerEmailLabel, config.developerEmail) },
            leadingIcon = Icons.Default.Email,
          )
        }

        if (hasEmail && hasPhone) {
          HorizontalDivider(color = DonateFlatDivider, thickness = 0.8.dp)
        }

        if (hasPhone) {
          DonateCopyableDetailRow(
            fieldKey = "developer_phone",
            label = copy.developerPhoneLabel,
            value = config.developerPhone,
            isCopied = copiedKey == "developer_phone",
            copyLabel = copy.copyButtonCd,
            copiedLabel = copy.copiedFeedback,
            onCopy = { onCopy("developer_phone", copy.developerPhoneLabel, config.developerPhone) },
            leadingIcon = Icons.Default.Phone,
          )
        }
      }
    }
  }
}

@Composable
internal fun DonateGratitudeFooter(
  sectionLabel: String,
  customNote: String,
  fallbackQuote: String,
  fallbackRef: String,
) {
  val hasCustomNote = customNote.isNotBlank()

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("donate_gratitude_footer"),
    shape = RoundedCornerShape(20.dp),
    color = DonateFlatSurface,
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = sectionLabel,
        style = MaterialTheme.typography.labelMedium,
        color = BrandGold,
        fontWeight = FontWeight.SemiBold,
      )

      if (hasCustomNote) {
        Text(
          text = customNote,
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
          color = PureWhite,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Box(
          modifier =
            Modifier
              .width(3.dp)
              .height(48.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(BrandGold.copy(alpha = 0.65f)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "“$fallbackQuote”",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            fontStyle = FontStyle.Italic,
            color = GlassWhite.copy(alpha = 0.82f),
          )
          Text(
            text = "— $fallbackRef",
            style = MaterialTheme.typography.labelSmall,
            color = BrandGold,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}
