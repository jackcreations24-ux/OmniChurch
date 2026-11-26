package com.zoutiw.omnichurch.presentation.screens.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.CrossChurchVisitItem
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem
import com.zoutiw.omnichurch.data.models.MemberAttendanceStatus
import com.zoutiw.omnichurch.data.models.buildCanonicalQrPayload
import com.zoutiw.omnichurch.presentation.components.GlassQRContainer
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

/**
 * QR Screen & Member Attendance History (Section 5.4):
 * - Automatically boosts window brightness to 100% (1.0f) and restores original brightness on exit.
 * - Top section is fixed/pinned: "QR" title bar and elevated high-contrast QR container that never moves on scroll.
 * - Bottom section is a simple, flat, borderless scrollable list directly on the background showing
 *   the member's attendance history (Prezan, An Reta, Absan) for each service date.
 */
@Composable
fun MemberQRGalleryScreen(
  member: Member?,
  qrBitmap: Bitmap?,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  attendanceHistory: List<MemberAttendanceHistoryItem> = emptyList(),
  crossChurchVisits: List<CrossChurchVisitItem> = emptyList(),
  currentLanguageCode: String = "ht",
  effectiveVerificationBaseUrl: String = AdMobRemoteController.OFFICIAL_PLATFORM_VERIFICATION_URL,
  onOpenVerificationCertificate: ((Member) -> Unit)? = null,
) {
  BackHandler(onBack = onClose)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val copy = remember(currentLanguageCode) { MemberQrGalleryStrings.forLanguage(currentLanguageCode) }
  val officialVerificationUrl =
    remember(member?.churchId, member?.effectiveMatrikil, effectiveVerificationBaseUrl) {
      if (member != null && member.churchId.isNotBlank() && member.effectiveMatrikil.isNotBlank()) {
        buildCanonicalQrPayload(
          churchId = member.churchId,
          memberId = member.effectiveMatrikil,
          verificationBaseUrl = effectiveVerificationBaseUrl,
        )
      } else {
        ""
      }
    }

  // Automatic maximum brightness management with guaranteed restoration on exit
  DisposableEffect(Unit) {
    val activity = context.findActivity()
    val window = activity?.window
    val previousBrightness = window?.attributes?.screenBrightness ?: -1f
    if (window != null) {
      val params = window.attributes
      params.screenBrightness = 1.0f
      window.attributes = params
    }
    onDispose {
      if (window != null) {
        val params = window.attributes
        params.screenBrightness = previousBrightness
        window.attributes = params
      }
    }
  }

  val presentCount =
    remember(attendanceHistory) {
      attendanceHistory.count { it.status == MemberAttendanceStatus.PRESENT }
    }
  val lateCount =
    remember(attendanceHistory) {
      attendanceHistory.count { it.status == MemberAttendanceStatus.LATE }
    }
  val absentCount =
    remember(attendanceHistory) {
      attendanceHistory.count { it.status == MemberAttendanceStatus.ABSENT }
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .statusBarsPadding()
        .navigationBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // 1. Pinned Top Bar ("QR") + Static QR Section (never scrolls)
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      GlassTopBar(
        title = copy.title,
        onBackClick = onClose,
      )

      Spacer(modifier = Modifier.height(6.dp))

      // High-contrast QR positioned higher up on screen and completely fixed
      GlassQRContainer(
        qrBitmap = qrBitmap,
        size = 216.dp,
        showLaserLine = false,
        qrContentDescription = copy.qrContentDescription,
      )

      Spacer(modifier = Modifier.height(10.dp))

      if (member != null) {
        Text(
          text = member.fullName,
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = String.format(copy.memberIdFormat, member.effectiveMatrikil),
          style = MaterialTheme.typography.bodyMedium,
          color = BrandGold,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Verified,
          contentDescription = null,
          tint = SuccessGreen,
          modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = copy.offlineCachedLabel,
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite,
        )
      }

      if (member != null && officialVerificationUrl.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          QrGalleryCompactActionPill(
            label = copy.copyLinkLabel,
            icon = Icons.Default.ContentCopy,
            accentColor = BlueNeon,
            onClick = {
              val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(
                ClipData.newPlainText("OmniChurch Official Verification URL", officialVerificationUrl)
              )
              Toast.makeText(context, copy.copyLinkToast, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("qr_gallery_copy_link_btn"),
          )
          if (qrBitmap != null) {
            QrGalleryCompactActionPill(
              label = copy.shareQrLabel,
              icon = Icons.Default.Share,
              accentColor = BrandGold,
              onClick = {
                scope.launch {
                  QrCodeGenerator.shareQrCardImage(
                    context = context,
                    bitmap = qrBitmap,
                    memberName = member.effectiveFullName,
                    memberCode = member.effectiveMatrikil,
                    churchName = member.churchName.ifBlank { "OmniChurch" },
                    verificationUrl = officialVerificationUrl,
                  )
                }
              },
              modifier = Modifier.testTag("qr_gallery_share_btn"),
            )
          }
          if (onOpenVerificationCertificate != null) {
            QrGalleryCompactActionPill(
              label = copy.verifyCardLabel,
              icon = Icons.Default.VerifiedUser,
              accentColor = SuccessGreen,
              onClick = { onOpenVerificationCertificate(member) },
              modifier = Modifier.testTag("qr_gallery_verify_card_btn"),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Subtle flat separator between fixed QR area and scrollable attendance history
    HorizontalDivider(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      thickness = 0.5.dp,
      color = PureWhite.copy(alpha = 0.12f),
    )

    // 2. Simple, flat, borderless Attendance History header directly on background
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = copy.attendanceHistoryTitle,
        style = MaterialTheme.typography.titleSmall,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FlatAttendanceLegendCount(
          label = copy.legendPresent,
          count = presentCount,
          dotColor = SuccessGreen,
        )
        FlatAttendanceLegendCount(
          label = copy.legendLate,
          count = lateCount,
          dotColor = BrandGold,
        )
        FlatAttendanceLegendCount(
          label = copy.legendAbsent,
          count = absentCount,
          dotColor = ErrorRed,
        )
      }
    }

    // 3. Scrollable flat Attendance History & Cross-Church Visits (QR stays pinned at top)
    LazyColumn(
      modifier =
        Modifier.fillMaxWidth()
          .weight(1f)
          .testTag("member_attendance_history_list"),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
    ) {
      if (attendanceHistory.isEmpty()) {
        item(key = "attendance_empty_state") {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = copy.emptyHistoryMsg,
              style = MaterialTheme.typography.bodyMedium,
              color = MutedGlassText,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else {
        items(
          items = attendanceHistory,
          key = { it.id },
        ) { item ->
          FlatAttendanceHistoryRow(item = item, copy = copy)
          HorizontalDivider(
            thickness = 0.5.dp,
            color = PureWhite.copy(alpha = 0.07f),
          )
        }
      }

      // 4. Read-only Cross-Church Visitor Section ("Vizit Mwen nan Lòt Legliz")
      item(key = "cross_church_visits_header") {
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(
          thickness = 0.5.dp,
          color = PureWhite.copy(alpha = 0.14f),
        )
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .padding(top = 12.dp, bottom = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = copy.crossChurchVisitsTitle,
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
          )
          if (crossChurchVisits.isNotEmpty()) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(50))
                  .background(ElectricBlue.copy(alpha = 0.22f))
                  .border(0.8.dp, BlueNeon.copy(alpha = 0.7f), RoundedCornerShape(50))
                  .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
              Text(
                text = "${crossChurchVisits.size}",
                style = MaterialTheme.typography.labelSmall,
                color = BlueNeon,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      if (crossChurchVisits.isEmpty()) {
        item(key = "cross_church_visits_empty") {
          Text(
            text = copy.emptyVisitsMsg,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
            modifier = Modifier.padding(vertical = 8.dp),
          )
        }
      } else {
        items(
          items = crossChurchVisits,
          key = { "visit_${it.id}" },
        ) { visit ->
          FlatCrossChurchVisitRow(visit = visit, copy = copy)
          HorizontalDivider(
            thickness = 0.5.dp,
            color = PureWhite.copy(alpha = 0.07f),
          )
        }
      }
    }
  }
}
