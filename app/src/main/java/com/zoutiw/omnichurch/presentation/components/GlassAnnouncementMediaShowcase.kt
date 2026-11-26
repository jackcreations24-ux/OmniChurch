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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

/**
 * Edge-to-edge borderless Image Showcase for Announcements supporting both
 * Portrait ("Potre") and Landscape ("Peyizaj") with exact aspect-ratio measurement
 * and zero vertical/horizontal overflow.
 */
@Composable
fun AnnouncementMediaShowcase(
  imageSource: String,
  title: String,
  announcementId: String,
  onImageClick: () -> Unit,
  modifier: Modifier = Modifier,
  useFitScale: Boolean = false,
) {
  val cleanSource = remember(imageSource) { imageSource.trim() }
  val decodedBitmap =
    remember(cleanSource) { ImageCompressor.decodeBase64DataUrlToBitmap(cleanSource) }
  var remoteAspectRatio by remember(cleanSource) { mutableStateOf<Float?>(null) }
  var loadError by remember(cleanSource) { mutableStateOf(false) }

  val naturalRatio: Float? =
    remember(decodedBitmap, remoteAspectRatio) {
      if (decodedBitmap != null && decodedBitmap.height > 0 && decodedBitmap.width > 0) {
        decodedBitmap.width.toFloat() / decodedBitmap.height.toFloat()
      } else {
        remoteAspectRatio
      }
    }

  if (loadError && decodedBitmap == null) return

  val effectiveRatio =
    if (useFitScale) {
      naturalRatio?.coerceIn(0.45f, 3.0f)
    } else {
      naturalRatio?.coerceIn(0.65f, 2.25f)
    }

  val sizingModifier =
    if (effectiveRatio != null) {
      Modifier.fillMaxWidth().aspectRatio(effectiveRatio)
    } else {
      Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 420.dp)
    }

  val resolvedScale = if (useFitScale) ContentScale.Fit else ContentScale.Crop

  Box(
    modifier =
      modifier
        .then(sizingModifier)
        .clipToBounds()
        .clickable(onClick = onImageClick)
        .testTag("announcement_image_${announcementId}"),
    contentAlignment = Alignment.Center,
  ) {
    if (decodedBitmap != null) {
      Image(
        bitmap = decodedBitmap.asImageBitmap(),
        contentDescription = title,
        contentScale = resolvedScale,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      AsyncImage(
        model = cleanSource,
        contentDescription = title,
        contentScale =
          if (useFitScale) ContentScale.Fit
          else if (effectiveRatio != null) ContentScale.Crop
          else ContentScale.FillWidth,
        onSuccess = { state ->
          val d = state.result.drawable
          if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
            remoteAspectRatio = d.intrinsicWidth.toFloat() / d.intrinsicHeight.toFloat()
          }
        },
        onError = { loadError = true },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
internal fun AnnouncementMediaShowcaseLightbox(
  imageSource: String,
  title: String,
  department: String,
  onDismiss: () -> Unit,
) {
  val cleanSource = remember(imageSource) { imageSource.trim() }
  val decodedBitmap =
    remember(cleanSource) { ImageCompressor.decodeBase64DataUrlToBitmap(cleanSource) }

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
      ),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Color(0xF2040A18))
          .clickable(onClick = onDismiss)
          .padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (department.isNotBlank()) {
              Text(
                text = department,
                style = MaterialTheme.typography.bodySmall,
                color = BrandGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          IconButton(
            onClick = onDismiss,
            modifier =
              Modifier.size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.dp, PureWhite.copy(alpha = 0.35f), CircleShape),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fèmen",
              tint = PureWhite,
            )
          }
        }

        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .padding(vertical = 12.dp)
              .clip(RoundedCornerShape(18.dp)),
          contentAlignment = Alignment.Center,
        ) {
          if (decodedBitmap != null) {
            Image(
              bitmap = decodedBitmap.asImageBitmap(),
              contentDescription = title,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            AsyncImage(
              model = cleanSource,
              contentDescription = title,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        Text(
          text = "Peze nenpòt kote pou fèmen",
          style = MaterialTheme.typography.labelSmall,
          color = MutedGlassText,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      }
    }
  }
}
