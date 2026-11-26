package com.zoutiw.omnichurch.presentation.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
internal fun AnnouncementCardMoreMenuDialog(
  context: Context,
  announcement: Announcement,
  resolvedTitle: String,
  shareTitle: String,
  shareBody: String,
  copy: AnnouncementLocalizedCopy,
  canEdit: Boolean,
  isAdmin: Boolean,
  canDelete: Boolean = isAdmin,
  onTriggerShare: () -> Unit,
  onEditClick: (() -> Unit)?,
  onDeleteClick: (() -> Unit)?,
  onRequestDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  GlassDialog(
    onDismissRequest = onDismiss,
    title = resolvedTitle,
  ) {
    if (announcement.department.isNotBlank()) {
      Text(
        text = announcement.department,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(12.dp))
    }

    GlassButton(
      text = copy.shareAction,
      icon = Icons.Default.Share,
      onClick = {
        onDismiss()
        onTriggerShare()
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "announcement_menu_share_${announcement.id}",
    )

    Spacer(modifier = Modifier.height(8.dp))

    GlassButton(
      text = copy.shareAppAction,
      icon = Icons.Default.Share,
      isPrimaryNeon = false,
      onClick = {
        onDismiss()
        shareOmniChurchGeneralApp(context)
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "announcement_menu_share_app_${announcement.id}",
    )

    Spacer(modifier = Modifier.height(8.dp))

    GlassButton(
      text = copy.copyTextAction,
      icon = Icons.Default.ContentCopy,
      isPrimaryNeon = false,
      onClick = {
        onDismiss()
        copyOmniChurchTextToClipboard(
          context = context,
          label = resolvedTitle,
          text = "$shareTitle\n\n$shareBody",
        )
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "announcement_menu_copy_${announcement.id}",
    )

    if (canEdit && onEditClick != null) {
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
      Spacer(modifier = Modifier.height(10.dp))
      GlassButton(
        text = copy.editAction,
        icon = Icons.Default.Edit,
        isPrimaryNeon = false,
        onClick = {
          onDismiss()
          onEditClick()
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_edit_${announcement.id}",
      )
    }

    if (canDelete && onDeleteClick != null) {
      Spacer(modifier = Modifier.height(10.dp))
      if (!(canEdit && onEditClick != null)) {
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
        Spacer(modifier = Modifier.height(10.dp))
      }
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ErrorRed.copy(alpha = 0.22f))
            .border(1.dp, ErrorRed.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
            .clickable {
              onDismiss()
              onRequestDelete()
            }
            .padding(vertical = 12.dp, horizontal = 14.dp)
            .testTag("announcement_menu_delete_${announcement.id}"),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = copy.deleteAction,
            tint = PureWhite,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = copy.deleteAction,
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
internal fun AnnouncementCardConfirmDeleteDialog(
  announcementId: String,
  resolvedTitle: String,
  copy: AnnouncementLocalizedCopy,
  onConfirmDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  GlassDialog(
    onDismissRequest = onDismiss,
    title = copy.deleteDialogTitle,
  ) {
    Text(
      text = "\"$resolvedTitle\"",
      style = MaterialTheme.typography.bodyMedium,
      color = GlassWhite,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      GlassButton(
        text = stringResource(R.string.common_cancel),
        isPrimaryNeon = false,
        onClick = onDismiss,
        modifier = Modifier.weight(1f),
      )
      GlassButton(
        text = copy.deleteConfirmYes,
        icon = Icons.Default.DeleteOutline,
        onClick = {
          onDismiss()
          onConfirmDelete()
        },
        modifier = Modifier.weight(1f),
        testTag = "announcement_confirm_delete_btn_$announcementId",
      )
    }
  }
}
