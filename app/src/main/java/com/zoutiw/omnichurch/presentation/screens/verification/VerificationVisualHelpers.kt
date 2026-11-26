package com.zoutiw.omnichurch.presentation.screens.verification

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.OmniChurch3DFlameIcon
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import java.util.Locale

@Composable
internal fun ChurchLogoAvatar(logoUrl: String?, churchName: String) {
  val clean = logoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val bitmap = remember(clean) { ImageCompressor.decodeBase64DataUrlToBitmap(clean) }
  var remoteFailed by remember(clean) { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.size(64.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          if (bitmap != null || (clean != null && !remoteFailed)) {
            Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.14f), PrimaryNavy.copy(alpha = 0.85f)))
          } else {
            Brush.linearGradient(listOf(RoyalBlue, PrimaryNavy, DeepNavy))
          }
        )
        .border(1.5.dp, BlueNeon.copy(alpha = 0.75f), RoundedCornerShape(16.dp)),
    contentAlignment = Alignment.Center,
  ) {
    when {
      bitmap != null ->
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = churchName,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(12.dp)),
        )
      clean != null && !remoteFailed ->
        AsyncImage(
          model = clean,
          contentDescription = churchName,
          contentScale = ContentScale.Fit,
          onError = { remoteFailed = true },
          modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(12.dp)),
        )
      else -> OmniChurch3DFlameIcon(size = 34.dp)
    }
  }
}

@Composable
internal fun VerificationMemberPhoto(member: Member) {
  val photoUrl = member.effectivePhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val bitmap = remember(photoUrl) { ImageCompressor.decodeBase64DataUrlToBitmap(photoUrl) }
  var remoteFailed by remember(photoUrl) { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.size(74.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(listOf(RoyalBlue, DeepNavy)))
        .border(2.dp, BlueNeon.copy(alpha = 0.8f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    when {
      bitmap != null ->
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = member.effectiveFullName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      photoUrl != null && !remoteFailed ->
        AsyncImage(
          model = photoUrl,
          contentDescription = member.effectiveFullName,
          contentScale = ContentScale.Crop,
          onError = { remoteFailed = true },
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      else -> {
        val initials =
          member.effectiveFullName
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase(Locale.US) }
            .ifEmpty { "OC" }
        Text(
          text = initials,
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }
  }
}

@Composable
internal fun AuthorityPillBadge(
  text: String,
  accentColor: Color,
  icon: ImageVector,
) {
  Row(
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(accentColor.copy(alpha = 0.16f))
        .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = text,
      tint = accentColor,
      modifier = Modifier.size(14.dp),
    )
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
internal fun MetadataPairRow(
  leftLabel: String,
  leftValue: String,
  rightLabel: String,
  rightValue: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(leftLabel, style = MaterialTheme.typography.labelSmall, color = MutedGlassText)
      Text(
        text = leftValue,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(rightLabel, style = MaterialTheme.typography.labelSmall, color = MutedGlassText)
      Text(
        text = rightValue,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
internal fun MetadataSingleRow(label: String, value: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGlassText)
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = PureWhite,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
internal fun ContactInfoLine(
  icon: ImageVector,
  value: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MutedGlassText,
      modifier = Modifier.size(16.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = GlassWhite,
    )
  }
}
