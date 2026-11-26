package com.zoutiw.omnichurch.presentation.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.data.models.AttendanceErrorType
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun AttendanceValidationResultCard(
  outcome: ScannerValidationOutcome,
  copy: ScannerCopy,
  unlinkedNfcUid: String? = null,
  canBindNfcToMember: Boolean = false,
  onBindNfcClick: () -> Unit = {},
  onVerifyMemberCertificate: ((Member) -> Unit)? = null,
  onScanNextClick: () -> Unit,
) {
  val haptic = LocalHapticFeedback.current

  LaunchedEffect(outcome) {
    when (outcome) {
      is ScannerValidationOutcome.RegisteredSuccess -> {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      }
      is ScannerValidationOutcome.ValidationError -> {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      else -> Unit
    }
  }

  when (outcome) {
    is ScannerValidationOutcome.Idle -> Unit
    is ScannerValidationOutcome.Processing -> {
      PremiumGlassCard(
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(18.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = copy.loading,
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite,
          )
        }
      }
    }
    is ScannerValidationOutcome.RegisteredSuccess -> {
      val isOnTime = outcome.record.status == "on_time"
      val statusBadgeText = if (isOnTime) copy.statusOnTime else copy.statusLate
      val statusBadgeColor = if (isOnTime) SuccessGreen else BrandOrange

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .shadow(
              elevation = 18.dp,
              shape = RoundedCornerShape(22.dp),
              ambientColor = SuccessGreen,
              spotColor = SuccessGreen,
            )
            .clip(RoundedCornerShape(22.dp))
            .background(OmniBrushes.SuccessGlassBrush)
            .border(
              1.5.dp,
              Brush.linearGradient(listOf(PureWhite, SuccessGreen, ElectricBlue)),
              RoundedCornerShape(22.dp),
            )
            .padding(16.dp)
            .testTag("attendance_success_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${copy.registeredTitle} ✓",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.ExtraBold,
          )
        }

        HorizontalDivider(color = GlassWhite.copy(alpha = 0.2f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            val photoUrl = outcome.member.effectivePhotoUrl
            if (!photoUrl.isNullOrBlank()) {
              AsyncImage(
                model = photoUrl,
                contentDescription = outcome.member.fullName,
                contentScale = ContentScale.Crop,
                modifier =
                  Modifier.size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, SuccessGreen, CircleShape),
              )
            } else {
              MemberAvatar3D(fullName = outcome.member.fullName, size = 48.dp)
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = outcome.member.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "ID: ${outcome.member.effectiveMatrikil}",
                style = MaterialTheme.typography.bodySmall,
                color = GlassWhite.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(10.dp))
                .background(statusBadgeColor.copy(alpha = 0.28f))
                .border(1.2.dp, statusBadgeColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = statusBadgeText,
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = copy.servicePrefix,
              style = MaterialTheme.typography.bodySmall,
              color = MutedGlassText,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = outcome.service.title,
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = GlassWhite,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = outcome.formattedTime,
              style = MaterialTheme.typography.labelMedium,
              color = PureWhite,
            )
          }
        }

        if (onVerifyMemberCertificate != null) {
          GlassButton(
            text = copy.verifyCertificateBtn,
            icon = Icons.Default.VerifiedUser,
            isPrimaryNeon = false,
            onClick = { onVerifyMemberCertificate(outcome.member) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "attendance_verify_certificate_btn",
          )
        }

        GlassButton(
          text = copy.scanNext,
          icon = Icons.Default.QrCodeScanner,
          isPrimaryNeon = true,
          onClick = onScanNextClick,
          modifier = Modifier.fillMaxWidth(),
          testTag = "attendance_scan_next_btn",
        )
      }
    }
    is ScannerValidationOutcome.ValidationError -> {
      val rawDetails = outcome.details?.trim().orEmpty()
      val errorMsg =
        when {
          rawDetails.startsWith("INACTIVE_MEMBER:") -> {
            val status = rawDetails.removePrefix("INACTIVE_MEMBER:").trim()
            String.format(copy.errInactiveMemberFormat, status)
          }
          rawDetails.isNotEmpty() -> rawDetails
          else ->
            when (outcome.errorType) {
              AttendanceErrorType.INVALID_QR -> copy.errInvalidQr
              AttendanceErrorType.WRONG_CHURCH -> copy.errWrongChurchSecurity
              AttendanceErrorType.DUPLICATE_ATTENDANCE -> copy.errDuplicate
              AttendanceErrorType.SERVICE_CLOSED -> copy.errServiceClosed
              AttendanceErrorType.UNAUTHORIZED -> copy.errUnauthorized
              AttendanceErrorType.NETWORK_ERROR -> copy.errNetwork
            }
        }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = ErrorRed, spotColor = ErrorRed)
            .clip(RoundedCornerShape(22.dp))
            .background(OmniBrushes.ErrorGlassBrush)
            .border(1.4.dp, ErrorRed, RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("attendance_error_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = ErrorRed,
          modifier = Modifier.size(32.dp),
        )
        Text(
          text = errorMsg,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          textAlign = TextAlign.Center,
          fontWeight = FontWeight.Bold,
        )
        if (!unlinkedNfcUid.isNullOrBlank() && canBindNfcToMember) {
          GlassButton(
            text = String.format(copy.bindNfcBtnFormat, unlinkedNfcUid.removePrefix("NFC-UID:")),
            icon = Icons.Default.Link,
            isPrimaryNeon = true,
            onClick = onBindNfcClick,
            modifier = Modifier.fillMaxWidth(),
            testTag = "nfc_bind_member_btn",
          )
        }
        GlassButton(
          text = copy.scanNext,
          isPrimaryNeon = false,
          onClick = onScanNextClick,
          modifier = Modifier.fillMaxWidth(),
          testTag = "attendance_error_retry_btn",
        )
      }
    }
  }
}
