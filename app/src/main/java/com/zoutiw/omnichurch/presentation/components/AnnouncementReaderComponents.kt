package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ZoomIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
internal fun ReaderMetaChip(
  icon: ImageVector,
  text: String,
  tint: Color,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(tint.copy(alpha = 0.14f))
        .border(0.8.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 5.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(13.dp),
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = PureWhite,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
internal fun ReaderPosterPreview(
  imageSource: String,
  title: String,
  caption: String?,
  tapHint: String,
  onClick: () -> Unit,
) {
  val cleanSource = remember(imageSource) { imageSource.trim() }
  val decodedBitmap = remember(cleanSource) { ImageCompressor.decodeBase64DataUrlToBitmap(cleanSource) }
  var remoteRatio by remember(cleanSource) { mutableStateOf<Float?>(null) }
  var loadError by remember(cleanSource) { mutableStateOf(false) }

  if (loadError && decodedBitmap == null) return

  val naturalRatio =
    remember(decodedBitmap, remoteRatio) {
      if (decodedBitmap != null && decodedBitmap.width > 0 && decodedBitmap.height > 0) {
        decodedBitmap.width.toFloat() / decodedBitmap.height.toFloat()
      } else {
        remoteRatio
      }
    }?.coerceIn(0.65f, 2.2f)

  val imageModifier =
    if (naturalRatio != null) Modifier.fillMaxWidth().aspectRatio(naturalRatio)
    else Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp)

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(
      modifier =
        imageModifier
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, GlassWhite.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
          .clickable(onClick = onClick)
          .testTag("announcement_reader_image"),
    ) {
      if (decodedBitmap != null) {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize(),
        )
      } else {
        AsyncImage(
          model = cleanSource,
          contentDescription = title,
          contentScale = if (naturalRatio != null) ContentScale.Crop else ContentScale.FillWidth,
          onSuccess = { state ->
            val d = state.result.drawable
            if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
              remoteRatio = d.intrinsicWidth.toFloat() / d.intrinsicHeight.toFloat()
            }
          },
          onError = { loadError = true },
          modifier = Modifier.matchParentSize(),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xB8040A18))
            .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Icon(
          imageVector = Icons.Default.ZoomIn,
          contentDescription = tapHint,
          tint = PureWhite,
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = tapHint,
          style = MaterialTheme.typography.labelSmall,
          color = PureWhite,
        )
      }
    }

    if (!caption.isNullOrBlank()) {
      Text(
        text = caption.trim(),
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
    }
  }
}

@Composable
fun AnnouncementReadMorePrompt(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 4.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = BlueNeon,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.width(2.dp))
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = label,
      tint = BlueNeon,
      modifier = Modifier.size(17.dp),
    )
  }
}
