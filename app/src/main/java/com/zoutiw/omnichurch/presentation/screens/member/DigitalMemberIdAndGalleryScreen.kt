package com.zoutiw.omnichurch.presentation.screens.member

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.CrossChurchVisitItem
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem
import com.zoutiw.omnichurch.data.models.MemberAttendanceStatus
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassQRContainer
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.OmniChurchBrandHeader
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.minimumInteractiveComponentSize
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.data.models.buildCanonicalQrPayload
import com.zoutiw.omnichurch.presentation.components.MemberIdCard
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import kotlinx.coroutines.launch

@Composable
fun DigitalMemberIdScreen(
  memberState: UiState<Member?>,
  qrBitmap: Bitmap?,
  onBackClick: () -> Unit,
  onOpenFullScreenQrGallery: () -> Unit,
  onOpenProfileSettings: () -> Unit,
  onUpdateMemberPhoto: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var showPhotoSourceDialog by remember { mutableStateOf(false) }
  var isCompressingPhoto by remember { mutableStateOf(false) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          isCompressingPhoto = true
          val result = ImageCompressor.compressUriToBase64(context, uri)
          isCompressingPhoto = false
          if (result.isSuccess) {
            onUpdateMemberPhoto(result.getOrThrow())
          } else {
            Toast.makeText(
                context,
                "Erè foto: ${result.exceptionOrNull()?.localizedMessage}",
                Toast.LENGTH_SHORT,
              )
              .show()
          }
        }
      }
    }

  val takePictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
      if (bitmap != null) {
        scope.launch {
          isCompressingPhoto = true
          val base64 = ImageCompressor.compressBitmapToBase64(bitmap)
          isCompressingPhoto = false
          onUpdateMemberPhoto(base64)
        }
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        try {
          takePictureLauncher.launch(null)
        } catch (e: Exception) {
          Toast.makeText(context, "Erè kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
      } else {
        Toast.makeText(
            context,
            "Pèmisyon kamera nesesè pou pran yon foto pwofil.",
            Toast.LENGTH_SHORT,
          )
          .show()
      }
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .navigationBarsPadding(),
  ) {
    GlassTopBar(
      title = stringResource(R.string.member_card_screen_title),
      onBackClick = onBackClick,
      onMenuClick = onOpenProfileSettings,
    )

    when (memberState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          GlassLoadingIndicator()
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(20.dp),
          contentAlignment = Alignment.Center,
        ) {
          PremiumEmptyState(
            title = stringResource(R.string.member_no_card_title),
            subtitle = memberState.message,
            icon = Icons.Default.Badge,
          )
        }
      }
      is UiState.Success -> {
        val member = memberState.data
        if (member == null || member.effectiveBarcodeToken.isBlank()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center,
          ) {
            PremiumEmptyState(
              title = stringResource(R.string.member_no_card_title),
              subtitle = stringResource(R.string.member_no_card_subtitle),
              icon = Icons.Default.Badge,
            )
          }
        } else {
          val matrikil = member.effectiveMatrikil
          Box(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
          ) {
            // 3D DIGITAL MEMBER ID CARD WITH FLIP (RECTO / VERSO)
            MemberIdCard(
              member = member,
              qrBitmap = qrBitmap,
              onCopyMemberCode = {
                val clipboard =
                  context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OmniChurch Member ID", matrikil))
                Toast.makeText(
                    context,
                    context.getString(R.string.member_id_copied),
                    Toast.LENGTH_SHORT,
                  )
                  .show()
              },
              onChangePhotoClick = { showPhotoSourceDialog = true },
              onQrClick = onOpenFullScreenQrGallery,
            )
          }
        }
      }
    }

    if (showPhotoSourceDialog) {
      GlassModalBottomSheet(
        onDismissRequest = { showPhotoSourceDialog = false },
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 14.dp)
              .navigationBarsPadding(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Chanje Foto Pwofil Manm",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Chwazi kijan ou vle pran oswa chwazi nouvo foto paspò w la pou mete sou kat la.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
          )
          Spacer(modifier = Modifier.height(4.dp))

          GlassButton(
            text = "Pran yon Foto ak Kamera",
            subtitle = "Louvri kamera telefòn lan dirèkteman",
            icon = Icons.Default.CameraAlt,
            isPrimaryNeon = true,
            onClick = {
              showPhotoSourceDialog = false
              val hasCamPermission =
                androidx.core.content.ContextCompat.checkSelfPermission(
                  context,
                  android.Manifest.permission.CAMERA,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
              if (hasCamPermission) {
                try {
                  takePictureLauncher.launch(null)
                } catch (e: Exception) {
                  Toast.makeText(context, "Erè kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
              } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
              }
            },
            modifier = Modifier.fillMaxWidth().height(68.dp),
            testTag = "btn_take_photo_camera",
          )

          GlassButton(
            text = "Chwazi nan Galri",
            subtitle = "Chwazi yon foto ki deja sou aparèy la",
            icon = Icons.Default.Download,
            isPrimaryNeon = false,
            onClick = {
              showPhotoSourceDialog = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.fillMaxWidth().height(68.dp),
            testTag = "btn_pick_photo_gallery",
          )

          if (isCompressingPhoto) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(8.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = BlueNeon)
            }
          }
        }
      }
    }
  }
}

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
  effectiveVerificationBaseUrl: String = AdMobRemoteController.OFFICIAL_PLATFORM_VERIFICATION_URL,
  onOpenVerificationCertificate: ((Member) -> Unit)? = null,
) {
  BackHandler(onBack = onClose)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
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
        title = stringResource(R.string.qr_gallery_title),
        onBackClick = onClose,
      )

      Spacer(modifier = Modifier.height(6.dp))

      // High-contrast QR positioned higher up on screen and completely fixed
      GlassQRContainer(
        qrBitmap = qrBitmap,
        size = 216.dp,
        showLaserLine = false,
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
          text = stringResource(R.string.member_id_prefix, member.effectiveMatrikil),
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
          text = stringResource(R.string.qr_gallery_offline_cached),
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
            label = "Kopye Lyen",
            icon = Icons.Default.ContentCopy,
            accentColor = BlueNeon,
            onClick = {
              val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(
                ClipData.newPlainText("OmniChurch Official Verification URL", officialVerificationUrl)
              )
              Toast.makeText(context, "Lyen verifikasyon ofisyèl la kopye!", Toast.LENGTH_SHORT)
                .show()
            },
            modifier = Modifier.testTag("qr_gallery_copy_link_btn"),
          )
          if (qrBitmap != null) {
            QrGalleryCompactActionPill(
              label = "Pataje QR",
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
              label = stringResource(R.string.qr_gallery_verify_card),
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
        text = "Istwa Prezans",
        style = MaterialTheme.typography.titleSmall,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FlatAttendanceLegendCount(
          label = "Prezan",
          count = presentCount,
          dotColor = SuccessGreen,
        )
        FlatAttendanceLegendCount(
          label = "An Reta",
          count = lateCount,
          dotColor = BrandGold,
        )
        FlatAttendanceLegendCount(
          label = "Absan",
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
              text = "Poko gen okenn istwa prezans pou dat sèvis yo.",
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
          FlatAttendanceHistoryRow(item = item)
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
            text = "Vizit Mwen nan Lòt Legliz",
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
            text = "Poko gen okenn vizit anrejistre nan lòt legliz.",
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
          FlatCrossChurchVisitRow(visit = visit)
          HorizontalDivider(
            thickness = 0.5.dp,
            color = PureWhite.copy(alpha = 0.07f),
          )
        }
      }
    }
  }
}

@Composable
private fun FlatAttendanceLegendCount(
  label: String,
  count: Int,
  dotColor: Color,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier =
        Modifier.size(6.dp)
          .clip(CircleShape)
          .background(dotColor),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = "$label $count",
      style = MaterialTheme.typography.labelSmall,
      color = MutedGlassText,
    )
  }
}

@Composable
private fun FlatAttendanceHistoryRow(
  item: MemberAttendanceHistoryItem,
) {
  val (statusLabel, statusColor) =
    when (item.status) {
      MemberAttendanceStatus.PRESENT -> "Prezan" to SuccessGreen
      MemberAttendanceStatus.LATE -> "An Reta" to BrandGold
      MemberAttendanceStatus.ABSENT -> "Absan" to ErrorRed
      MemberAttendanceStatus.EXCUSED -> "Ekskize" to BrandGold
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(vertical = 11.dp)
        .testTag("attendance_history_row_${item.id}"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = 12.dp),
    ) {
      Text(
        text = item.dateLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.Medium,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val subtitle =
        if (!item.timeLabel.isNullOrBlank()) {
          "${item.serviceTitle} • ${item.timeLabel}"
        } else {
          item.serviceTitle
        }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier.size(7.dp)
            .clip(CircleShape)
            .background(statusColor),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = statusLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = statusColor,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun FlatCrossChurchVisitRow(
  visit: CrossChurchVisitItem,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(vertical = 11.dp)
        .testTag("cross_church_visit_row_${visit.id}"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = 12.dp),
    ) {
      Text(
        text = visit.hostChurchName,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val subtitle =
        buildList {
          if (visit.dateLabel.isNotBlank()) add(visit.dateLabel)
          if (visit.serviceTitle.isNotBlank()) add(visit.serviceTitle)
          if (visit.timeLabel.isNotBlank()) add(visit.timeLabel)
        }.joinToString(" • ")
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(50))
          .background(ElectricBlue.copy(alpha = 0.24f))
          .border(0.9.dp, BlueNeon, RoundedCornerShape(50))
          .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
      Text(
        text = "Vizitè ✓",
        style = MaterialTheme.typography.labelSmall,
        color = BlueNeon,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun QrGalleryCompactActionPill(
  label: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(50))
        .background(DeepNavy.copy(alpha = 0.85f))
        .border(1.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = accentColor,
      modifier = Modifier.size(15.dp),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}

