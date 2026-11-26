package com.zoutiw.omnichurch.presentation.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.LiveBadgeRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun ServiceCardStatusBadgesRow(
  service: ChurchService,
  isLiveActive: Boolean,
  isTimerTicking: Boolean,
  timerLabel: String,
  isPermanentlyLocked: Boolean,
  isBeforeStart: Boolean,
  statusEndedLabel: String,
  statusAttendanceOpenLabel: String,
  statusScheduledLabel: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
        .testTag("service_status_badges_row_${service.id}"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (isLiveActive) {
      Box(
        modifier =
          Modifier.shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = LiveBadgeRed)
            .clip(RoundedCornerShape(8.dp))
            .background(
              Brush.horizontalGradient(listOf(LiveBadgeRed, Color(0xFFD91432)))
            )
            .border(1.dp, PureWhite.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("service_live_badge_${service.id}"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(
            modifier = Modifier.size(6.dp).clip(CircleShape).background(PureWhite)
          )
          Text(
            text = stringResource(R.string.feed_live_badge),
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
          )
        }
      }

      if (isTimerTicking) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
              .background(Color(0xCC09142B))
              .border(0.9.dp, LiveBadgeRed.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
              .padding(horizontal = 7.dp, vertical = 3.dp)
              .testTag("service_live_timer_${service.id}"),
        ) {
          Text(
            text = timerLabel,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontFeatureSettings = "tnum",
              ),
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    }

    if (isPermanentlyLocked) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Color(0x661E293B))
            .border(0.9.dp, MutedGlassText.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = statusEndedLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MutedGlassText,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    } else if (isLiveActive) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(SuccessGreen.copy(alpha = 0.2f))
            .border(1.dp, SuccessGreen.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(
            modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen)
          )
          Text(
            text = statusAttendanceOpenLabel,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
    } else if (isBeforeStart) {
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(ElectricBlue.copy(alpha = 0.18f))
            .border(0.9.dp, BlueNeon.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = statusScheduledLabel,
          style = MaterialTheme.typography.labelSmall,
          color = BlueNeon,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }

    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
          .background(ElectricBlue.copy(alpha = 0.22f))
          .border(0.8.dp, BlueNeon.copy(alpha = 0.52f), RoundedCornerShape(8.dp))
          .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      Text(
        text = service.categoryLabelHt,
        style = MaterialTheme.typography.labelSmall,
        color = BlueNeon,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
    }
  }
}

@Composable
internal fun ServiceCardAttendanceControls(
  service: ChurchService,
  isAdmin: Boolean,
  isScanner: Boolean,
  isLiveActive: Boolean,
  isPermanentlyLocked: Boolean,
  isBeforeStart: Boolean = false,
  hasPassedEndTime: Boolean = false,
  languageCode: String = "ht",
  onToggleAttendanceWindow: ((Boolean) -> Unit)?,
  onOpenScanner: (() -> Unit)?,
  onViewAttendances: (() -> Unit)?,
) {
  val lang = languageCode.trim().lowercase()
  val startClockLabel = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(service.startTime)
  val scheduledBlockedLabel =
    when (lang) {
      "fr" -> if (startClockLabel.isNotBlank()) "Démarre à $startClockLabel" else "Service pas encore commencé"
      "en" -> if (startClockLabel.isNotBlank()) "Starts at $startClockLabel" else "Not started yet"
      "es" -> if (startClockLabel.isNotBlank()) "Comienza a las $startClockLabel" else "Aún no ha comenzado"
      else -> if (startClockLabel.isNotBlank()) "Kòmanse a $startClockLabel" else "Sèvis la poko kòmanse"
    }
  val endedLabel =
    when (lang) {
      "fr" -> "Ce service est terminé"
      "en" -> "This service has ended"
      "es" -> "Este servicio ha finalizado"
      else -> "Sèvis sa fini"
    }
  val canToggleAttendance = !isPermanentlyLocked && !hasPassedEndTime && (!isBeforeStart || service.isOpenForAttendance)

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(start = 14.dp, end = 14.dp, top = 10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (isAdmin && onToggleAttendanceWindow != null) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B1933).copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f).padding(end = 8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(8.dp)
                .clip(CircleShape)
                .background(
                  when {
                    isPermanentlyLocked || hasPassedEndTime -> ErrorRed
                    isBeforeStart -> BlueNeon
                    isLiveActive -> SuccessGreen
                    else -> MutedGlassText
                  }
                )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              when {
                isPermanentlyLocked || hasPassedEndTime -> endedLabel
                isBeforeStart -> scheduledBlockedLabel
                isLiveActive -> stringResource(R.string.scanner_open_for_attendance)
                else -> stringResource(R.string.scanner_closed_for_attendance)
              },
            style = MaterialTheme.typography.labelMedium,
            color =
              when {
                isPermanentlyLocked || hasPassedEndTime -> ErrorRed.copy(alpha = 0.9f)
                isBeforeStart -> BlueNeon
                isLiveActive -> SuccessGreen
                else -> MutedGlassText
              },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Switch(
          checked = isLiveActive,
          enabled = canToggleAttendance,
          onCheckedChange = { wantOpen ->
            if (!isPermanentlyLocked && !hasPassedEndTime && !(wantOpen && isBeforeStart)) {
              onToggleAttendanceWindow(wantOpen)
            }
          },
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = PureWhite,
              checkedTrackColor = SuccessGreen,
              disabledUncheckedThumbColor = MutedGlassText.copy(alpha = 0.5f),
              disabledUncheckedTrackColor = Color(0xFF1E293B),
            ),
          modifier = Modifier.testTag("toggle_service_open_${service.id}"),
        )
      }
    }

    if (isScanner && (onOpenScanner != null || onViewAttendances != null)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (isLiveActive && !isPermanentlyLocked && !isBeforeStart && !hasPassedEndTime && onOpenScanner != null) {
          GlassButton(
            text = stringResource(R.string.action_scan_qr),
            icon = Icons.Default.QrCodeScanner,
            onClick = onOpenScanner,
            modifier = Modifier.weight(1f),
            testTag = "scan_service_btn_${service.id}",
          )
        }
        if (onViewAttendances != null) {
          GlassButton(
            text = stringResource(R.string.attendance_registered_title),
            icon = Icons.Default.People,
            isPrimaryNeon = false,
            onClick = onViewAttendances,
            modifier = Modifier.weight(1f),
            testTag = "view_attendances_btn_${service.id}",
          )
        }
      }
    }
  }
}

@Composable
internal fun ServiceCardMoreMenuDialog(
  context: Context,
  service: ChurchService,
  serviceDateSubtitle: String,
  shareTitle: String,
  shareBody: String,
  canEdit: Boolean,
  canDelete: Boolean,
  onTriggerShare: () -> Unit,
  onEditClick: (() -> Unit)?,
  onRequestDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  GlassDialog(
    onDismissRequest = onDismiss,
    title = service.effectiveTitle,
  ) {
    Text(
      text = "${service.categoryLabelHt} • $serviceDateSubtitle",
      style = MaterialTheme.typography.bodySmall,
      color = MutedGlassText,
    )
    Spacer(modifier = Modifier.height(12.dp))

    GlassButton(
      text = "Pataje Sèvis sa a",
      icon = Icons.Default.Share,
      onClick = {
        onDismiss()
        onTriggerShare()
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "service_menu_share_${service.id}",
    )

    Spacer(modifier = Modifier.height(8.dp))

    GlassButton(
      text = "Pataje Aplikasyon OmniChurch la",
      icon = Icons.Default.Share,
      isPrimaryNeon = false,
      onClick = {
        onDismiss()
        shareOmniChurchGeneralApp(context, service.churchName)
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "service_menu_share_app_${service.id}",
    )

    Spacer(modifier = Modifier.height(8.dp))

    GlassButton(
      text = "Kopye Tèks Piblikasyon an",
      icon = Icons.Default.ContentCopy,
      isPrimaryNeon = false,
      onClick = {
        onDismiss()
        copyOmniChurchTextToClipboard(
          context = context,
          label = service.effectiveTitle,
          text = "$shareTitle\n$shareBody",
        )
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "service_menu_copy_${service.id}",
    )

    if (canEdit && onEditClick != null) {
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
      Spacer(modifier = Modifier.height(10.dp))
      GlassButton(
        text = "Modifye Sèvis sa a",
        icon = Icons.Default.Edit,
        isPrimaryNeon = false,
        onClick = {
          onDismiss()
          onEditClick()
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "service_menu_edit_${service.id}",
      )
    }

    if (canDelete) {
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
            .testTag("service_menu_delete_${service.id}"),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Efase Sèvis la",
            tint = PureWhite,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Efase Sèvis sa a",
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
internal fun ServiceCardConfirmDeleteDialog(
  service: ChurchService,
  onConfirmDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  GlassDialog(
    onDismissRequest = onDismiss,
    title = "Efase Sèvis la?",
  ) {
    Text(
      text = "Èske ou sèten ou vle efase sèvis \"${service.effectiveTitle}\" nan baz done legliz la? Aksyon sa a pa ka defèt.",
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
        text = "Wi, Efase",
        icon = Icons.Default.DeleteOutline,
        onClick = {
          onDismiss()
          onConfirmDelete()
        },
        modifier = Modifier.weight(1f),
        testTag = "service_confirm_delete_btn_${service.id}",
      )
    }
  }
}
