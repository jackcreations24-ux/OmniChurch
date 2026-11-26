package com.zoutiw.omnichurch.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
internal fun AnnouncementOptionsAndAlertSection(
  isPinned: Boolean,
  onPinnedChange: (Boolean) -> Unit,
  showToastOnLogin: Boolean,
  onShowToastChange: (Boolean) -> Unit,
  selectedAlertLevel: String,
  onAlertLevelChange: (String) -> Unit,
  alertLevels: List<Pair<String, Pair<String, Color>>>,
  copy: CreateAnnouncementCopy,
) {
  SectionTitle(title = copy.optionsSectionTitle)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF1E293B).copy(alpha = 0.5f))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = copy.pinTitle,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = copy.pinDesc,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF94A3B8),
      )
    }
    Switch(
      checked = isPinned,
      onCheckedChange = onPinnedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = PureWhite,
        checkedTrackColor = Color(0xFFF59E0B),
        uncheckedThumbColor = Color(0xFF64748B),
        uncheckedTrackColor = Color(0xFF1E293B),
      ),
      modifier = Modifier.testTag("announcement_toggle_pinned"),
    )
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF1E293B).copy(alpha = 0.5f))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = copy.popupTitle,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = copy.popupDesc,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF94A3B8),
      )
    }
    Switch(
      checked = showToastOnLogin,
      onCheckedChange = onShowToastChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = PureWhite,
        checkedTrackColor = Color(0xFFEF4444),
        uncheckedThumbColor = Color(0xFF64748B),
        uncheckedTrackColor = Color(0xFF1E293B),
      ),
      modifier = Modifier.testTag("announcement_toggle_toast"),
    )
  }

  AnimatedVisibility(visible = showToastOnLogin) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = copy.alertLevelHeader,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF94A3B8),
        fontWeight = FontWeight.Bold,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        alertLevels.forEach { (lvlKey, lvlPair) ->
          val (lvlLabel, lvlColor) = lvlPair
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (selectedAlertLevel == lvlKey) lvlColor.copy(alpha = 0.25f)
                else Color(0xFF1E293B).copy(alpha = 0.6f)
              )
              .border(
                width = 1.dp,
                color = if (selectedAlertLevel == lvlKey) lvlColor else Color(0xFF334155),
                shape = RoundedCornerShape(10.dp),
              )
              .clickable { onAlertLevelChange(lvlKey) }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = lvlLabel,
              style = MaterialTheme.typography.labelSmall,
              color = if (selectedAlertLevel == lvlKey) lvlColor else Color(0xFFCBD5E1),
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun AnnouncementMediaAndLinksSection(
  title: String,
  posterUrl: String,
  onPosterUrlChange: (String) -> Unit,
  isCompressingPoster: Boolean,
  onPickImageClick: () -> Unit,
  showAdvancedOptions: Boolean,
  onToggleAdvancedOptions: () -> Unit,
  availableServices: List<ChurchService>,
  selectedServiceId: String?,
  onSelectServiceId: (String?) -> Unit,
  copy: CreateAnnouncementCopy,
) {
  SectionTitle(title = copy.mediaSectionTitle)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier =
        Modifier.weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF1E293B))
          .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.65f), RoundedCornerShape(12.dp))
          .clickable(enabled = !isCompressingPoster, onClick = onPickImageClick)
          .padding(horizontal = 14.dp, vertical = 12.dp)
          .testTag("announcement_pick_image_btn"),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (isCompressingPoster) {
          CircularProgressIndicator(
            color = Color(0xFF38BDF8),
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp),
          )
        } else {
          Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = copy.pickImageCd,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (posterUrl.isNotBlank()) copy.changeImageBtn
            else copy.chooseImageBtn,
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    if (posterUrl.isNotBlank()) {
      IconButton(
        onClick = { onPosterUrlChange("") },
        modifier =
          Modifier.size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.18f))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f), RoundedCornerShape(12.dp))
            .testTag("announcement_clear_image_btn"),
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = copy.removeImageCd,
          tint = Color(0xFFEF4444),
        )
      }
    }
  }

  if (posterUrl.isNotBlank()) {
    AnnouncementMediaShowcase(
      imageSource = posterUrl,
      title = title.ifBlank { copy.posterPreviewTitle },
      announcementId = "preview",
      onImageClick = {},
    )
  }

  // Toggle plis detay (Lyen URL dirèk ak Sèvis)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onToggleAdvancedOptions)
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text =
        if (showAdvancedOptions) copy.hideAdvancedOptions
        else copy.showAdvancedOptions,
      style = MaterialTheme.typography.bodySmall,
      color = Color(0xFF38BDF8),
      fontWeight = FontWeight.SemiBold,
    )
  }

  AnimatedVisibility(visible = showAdvancedOptions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      MinimalTextField(
        value = if (posterUrl.startsWith("data:", ignoreCase = true)) "" else posterUrl,
        onValueChange = onPosterUrlChange,
        label = copy.posterUrlLabel,
        placeholder = "https://...",
        testTag = "announcement_input_poster_url",
      )

      if (availableServices.isNotEmpty()) {
        SectionTitle(title = copy.linkServiceSectionTitle)
        availableServices.take(4).forEach { srv ->
          val isSelected = selectedServiceId == srv.id
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF1E293B))
              .border(
                1.dp,
                if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155),
                RoundedCornerShape(10.dp),
              )
              .clickable {
                onSelectServiceId(if (isSelected) null else srv.id)
              }
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Link,
              contentDescription = null,
              tint = if (isSelected) Color(0xFFFBBF24) else Color(0xFF94A3B8),
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = srv.effectiveTitle,
              style = MaterialTheme.typography.bodySmall,
              color = if (isSelected) PureWhite else Color(0xFFCBD5E1),
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun AnnouncementSubmitFooter(
  isEditing: Boolean,
  submitEnabled: Boolean,
  copy: CreateAnnouncementCopy,
  onDismissRequest: () -> Unit,
  onSubmitClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF0F172A))
      .border(width = 1.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(0.dp))
      .padding(horizontal = 20.dp, vertical = 14.dp)
      .navigationBarsPadding(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Button(
      onClick = onDismissRequest,
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF1E293B),
        contentColor = Color(0xFFCBD5E1),
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(48.dp),
    ) {
      Text(copy.cancelBtn, fontWeight = FontWeight.SemiBold)
    }

    Button(
      onClick = onSubmitClick,
      enabled = submitEnabled,
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFF59E0B),
        contentColor = Color(0xFF0F172A),
        disabledContainerColor = Color(0xFF334155),
        disabledContentColor = Color(0xFF64748B),
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .weight(1f)
        .height(48.dp)
        .testTag("announcement_bottom_sheet_submit_btn"),
    ) {
      Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (isEditing) copy.submitEditBtn else copy.submitCreateBtn,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
