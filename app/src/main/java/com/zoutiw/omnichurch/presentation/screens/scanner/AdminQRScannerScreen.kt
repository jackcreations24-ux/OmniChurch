package com.zoutiw.omnichurch.presentation.screens.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.NfcAttendanceHelper
import com.zoutiw.omnichurch.core.utilities.NfcIntentBus
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.AttendanceErrorType
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.UserProfile
import kotlinx.coroutines.flow.Flow
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.OmniChurch3DFlameIcon
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.util.concurrent.Executors

@Composable
fun AdminQRScannerScreen(
  userProfile: UserProfile?,
  activeServices: List<ChurchService>,
  selectedServiceId: String?,
  validationOutcome: ScannerValidationOutcome,
  onSelectService: (String) -> Unit,
  onScanOrSubmitCode: (rawTokenOrCode: String, service: ChurchService, method: String) -> Unit,
  onResetOutcome: () -> Unit,
  onOpenServicesManagement: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
  churchMembers: List<Member> = emptyList(),
  allServices: List<ChurchService> = activeServices,
  currentLanguageCode: String = "ht",
  observeServiceAttendances: ((serviceId: String) -> Flow<List<AttendanceRecord>>)? = null,
  onBindNfcCardToMember: ((memberDocId: String, nfcCardUid: String, onLinked: () -> Unit) -> Unit)? =
    null,
  onVerifyCard: ((rawTokenOrCode: String) -> Unit)? = null,
  onOpenMemberVerification: ((Member) -> Unit)? = null,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val lifecycleOwner = LocalLifecycleOwner.current
  val haptic = LocalHapticFeedback.current
  val focusManager = LocalFocusManager.current

  var scannerPurpose by remember { mutableStateOf("attendance") }
  var scanMode by remember { mutableStateOf("qr_scan") }
  var manualCodeInput by remember { mutableStateOf("") }
  var isTorchEnabled by remember { mutableStateOf(false) }
  var lastScannedNfcToken by remember { mutableStateOf<String?>(null) }
  var showBindNfcMemberSheet by remember { mutableStateOf(false) }
  var nfcBindSearchQuery by remember { mutableStateOf("") }
  var hasDispatchedVerifyScan by remember { mutableStateOf(false) }

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }

  // Automatic single-active-service detection (only one service is open for attendance at a time)
  val activeService =
    remember(activeServices, selectedServiceId) {
      activeServices.firstOrNull { it.isOpenForAttendance && it.isLive }
        ?: activeServices.firstOrNull { it.isOpenForAttendance }
        ?: activeServices.firstOrNull { it.id == selectedServiceId }
        ?: activeServices.firstOrNull()
    }

  LaunchedEffect(activeService?.id) {
    val autoId = activeService?.id
    if (!autoId.isNullOrBlank() && autoId != selectedServiceId) {
      onSelectService(autoId)
    }
  }

  val isVerifyMode = scannerPurpose == "verify" && onVerifyCard != null
  val currentActiveService by rememberUpdatedState(activeService)
  val currentValidationOutcome by rememberUpdatedState(validationOutcome)
  val currentScanMode by rememberUpdatedState(scanMode)
  val currentIsVerifyMode by rememberUpdatedState(isVerifyMode)
  val currentOnVerifyCard by rememberUpdatedState(onVerifyCard)

  val nfcHelper =
    remember(activity) {
      activity?.let { act ->
        NfcAttendanceHelper(act) { scannedToken ->
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          lastScannedNfcToken = scannedToken
          if (currentIsVerifyMode) {
            currentOnVerifyCard?.invoke(scannedToken)
          } else {
            val srv = currentActiveService
            if (srv != null && currentValidationOutcome !is ScannerValidationOutcome.Processing) {
              onResetOutcome()
              onScanOrSubmitCode(scannedToken, srv, "nfc_scan")
            }
          }
        }
      }
    }

  val isNfcSupported = nfcHelper?.isNfcSupported() == true
  var isNfcEnabled by remember(nfcHelper) { mutableStateOf(nfcHelper?.isNfcEnabled() == true) }
  val latestNfcIntent by NfcIntentBus.latestIntent.collectAsStateWithLifecycle()

  LaunchedEffect(latestNfcIntent) {
    val incomingIntent = latestNfcIntent ?: return@LaunchedEffect
    if (currentScanMode != "nfc_scan") {
      scanMode = "nfc_scan"
    }
    nfcHelper?.handleIntent(incomingIntent)
    NfcIntentBus.consume()
  }

  DisposableEffect(lifecycleOwner, nfcHelper, scanMode) {
    isNfcEnabled = nfcHelper?.isNfcEnabled() == true
    if (scanMode == "nfc_scan" && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      nfcHelper?.onResume()
    } else {
      nfcHelper?.onPause()
    }

    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          hasDispatchedVerifyScan = false
          isNfcEnabled = nfcHelper?.isNfcEnabled() == true
          if (currentScanMode == "nfc_scan") {
            nfcHelper?.onResume()
          }
        }
        Lifecycle.Event.ON_PAUSE -> {
          nfcHelper?.onPause()
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      nfcHelper?.onPause()
    }
  }

  val submitInlineCode: () -> Unit = {
    val trimmed = manualCodeInput.trim()
    if (trimmed.isNotEmpty()) {
      focusManager.clearFocus()
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      if (isVerifyMode) {
        onVerifyCard?.invoke(trimmed)
      } else if (activeService != null) {
        onScanOrSubmitCode(trimmed, activeService, "manual")
      }
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .navigationBarsPadding()
        .imePadding(),
  ) {
    GlassTopBar(
      title = stringResource(R.string.scanner_screen_title_compact),
      onBackClick = onBackClick,
      trailingContent = {
        if (onVerifyCard != null) {
          CompactPurposeBadgeSwitch(
            isVerifyMode = isVerifyMode,
            onSelectPurpose = { purpose ->
              scannerPurpose = purpose
              hasDispatchedVerifyScan = false
              onResetOutcome()
            },
          )
        }
      },
    )

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // 1. Auto-detected single active service / Verify mode slim pill strip
        ScannerStatusStrip(
          isVerifyMode = isVerifyMode,
          activeService = activeService,
          onOpenServices = onOpenServicesManagement,
        )

        // 2. Clean 2-tab switch: Eskane QR | Kat NFC
        ScannerTwoTabBar(
          selectedMode = scanMode,
          isVerifyMode = isVerifyMode,
          onSelectMode = { mode ->
            scanMode = mode
            hasDispatchedVerifyScan = false
            if (mode != "nfc_scan") {
              lastScannedNfcToken = null
            }
            onResetOutcome()
          },
        )

        // 3. Primary viewport: QR Camera + refined inline code bar, OR NFC tap radar
        if (scanMode == "qr_scan") {
          if (hasCameraPermission) {
            CameraXQrViewportCard(
              isScanningEnabled =
                if (isVerifyMode) {
                  !hasDispatchedVerifyScan
                } else {
                  activeService != null && validationOutcome is ScannerValidationOutcome.Idle
                },
              isTorchEnabled = isTorchEnabled,
              isVerifyMode = isVerifyMode,
              onToggleTorch = { isTorchEnabled = !isTorchEnabled },
              onQrDetected = { rawToken ->
                if (isVerifyMode) {
                  if (!hasDispatchedVerifyScan) {
                    hasDispatchedVerifyScan = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onVerifyCard?.invoke(rawToken)
                  }
                } else if (activeService != null && validationOutcome is ScannerValidationOutcome.Idle) {
                  onScanOrSubmitCode(rawToken, activeService, "qr_scan")
                }
              },
            )
          } else {
            PremiumEmptyState(
              title = stringResource(R.string.scanner_camera_permission_title),
              subtitle = stringResource(R.string.scanner_camera_permission_msg),
              icon = Icons.Default.CameraAlt,
              actionLabel = stringResource(R.string.scanner_grant_permission),
              onActionClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            )
          }

          // Refined single-row inline member code entry directly inside the Scan tab
          RefinedInlineMemberCodeBar(
            value = manualCodeInput,
            onValueChange = { manualCodeInput = it },
            isVerifyMode = isVerifyMode,
            canSubmit =
              manualCodeInput.isNotBlank() &&
                (isVerifyMode || activeService != null) &&
                validationOutcome !is ScannerValidationOutcome.Processing,
            onSubmit = submitInlineCode,
          )
        } else {
          NfcTapViewportCard(
            isNfcSupported = isNfcSupported,
            isNfcEnabled = isNfcEnabled,
            hasActiveService = isVerifyMode || activeService != null,
            lastScannedToken = lastScannedNfcToken,
            isVerifyMode = isVerifyMode,
            onOpenNfcSettings = {
              runCatching {
                context.startActivity(
                  Intent(Settings.ACTION_NFC_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  }
                )
              }
            },
          )
        }

        // 4. Attendance validation feedback card (attendance mode only)
        if (!isVerifyMode) {
          val unlinkedNfcUid =
            lastScannedNfcToken?.takeIf {
              scanMode == "nfc_scan" &&
                it.startsWith("NFC-UID:", ignoreCase = true) &&
                validationOutcome is ScannerValidationOutcome.ValidationError &&
                validationOutcome.errorType == AttendanceErrorType.INVALID_QR
            }

          AttendanceValidationResultCard(
            outcome = validationOutcome,
            unlinkedNfcUid = unlinkedNfcUid,
            canBindNfcToMember = onBindNfcCardToMember != null && churchMembers.isNotEmpty(),
            onBindNfcClick = { showBindNfcMemberSheet = true },
            onVerifyMemberCertificate = onOpenMemberVerification,
            onScanNextClick = {
              manualCodeInput = ""
              lastScannedNfcToken = null
              onResetOutcome()
            },
          )

          // 5. Simple borderless Service History section with 4 small tabs (Prezans, Reta, Absans, Vizitè)
          if (observeServiceAttendances != null) {
            ScannerServiceHistorySection(
              activeService = activeService,
              allServices = allServices,
              churchMembers = churchMembers,
              languageCode = currentLanguageCode,
              observeServiceAttendances = observeServiceAttendances,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  if (showBindNfcMemberSheet && onBindNfcCardToMember != null) {
    val rawUid = lastScannedNfcToken.orEmpty().removePrefix("NFC-UID:").trim()
    val filteredMembers =
      remember(churchMembers, nfcBindSearchQuery) {
        val q = nfcBindSearchQuery.trim().lowercase()
        if (q.isEmpty()) churchMembers
        else {
          churchMembers.filter {
            it.effectiveFullName.lowercase().contains(q) ||
              it.effectiveMatrikil.lowercase().contains(q) ||
              it.department.lowercase().contains(q)
          }
        }
      }

    GlassModalBottomSheet(
      onDismissRequest = { showBindNfcMemberSheet = false },
      title = "Lye Kat NFC ($rawUid)",
    ) {
      Text(
        text = "Chwazi manm nan pou asosye kat NFC sa a avèk li pou pwochen eskanè yo:",
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(10.dp))
      GlassInputField(
        value = nfcBindSearchQuery,
        onValueChange = { nfcBindSearchQuery = it },
        label = "Chèche non oswa Matrikil...",
        leadingIcon = Icons.Default.Search,
        testTag = "nfc_bind_member_search",
      )
      Spacer(modifier = Modifier.height(10.dp))
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(filteredMembers, key = { it.id }) { mem ->
          PremiumGlassCard(
            cornerRadius = 14.dp,
            contentPadding = PaddingValues(12.dp),
            onClick = {
              onBindNfcCardToMember(mem.id, rawUid) {
                showBindNfcMemberSheet = false
                nfcBindSearchQuery = ""
                val srv = activeService
                if (srv != null && lastScannedNfcToken != null) {
                  onResetOutcome()
                  onScanOrSubmitCode(lastScannedNfcToken!!, srv, "nfc_scan")
                } else {
                  onResetOutcome()
                }
              }
            },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                MemberAvatar3D(
                  fullName = mem.effectiveFullName,
                  size = 40.dp,
                  photoUrl = mem.effectivePhotoUrl,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = mem.effectiveFullName,
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${mem.effectiveMatrikil} • ${mem.department.ifBlank { "Manm" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGlassText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Lye kat",
                tint = BrandGold,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Compact header badge switch toggling between Prezans (Attendance) and Verifye (Verify Card)
 * without taking up vertical space or crowding narrow screens.
 */
@Composable
private fun CompactPurposeBadgeSwitch(
  isVerifyMode: Boolean,
  onSelectPurpose: (String) -> Unit,
) {
  val borderColor by
    animateColorAsState(
      targetValue = if (isVerifyMode) SuccessGreen.copy(alpha = 0.6f) else BlueNeon.copy(alpha = 0.5f),
      animationSpec = tween(220),
      label = "purpose_border",
    )

  Row(
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(Color(0xCC06132B))
        .border(1.dp, borderColor, RoundedCornerShape(50))
        .padding(3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    CompactPurposeChip(
      label = stringResource(R.string.scanner_purpose_attendance),
      icon = Icons.Default.QrCodeScanner,
      selected = !isVerifyMode,
      activeBrush = OmniBrushes.ActiveBlueNeonBrush,
      inactiveColor = MutedGlassText,
      onClick = { onSelectPurpose("attendance") },
      testTag = "scanner_purpose_attendance",
    )
    CompactPurposeChip(
      label = stringResource(R.string.scanner_purpose_verify_short),
      icon = Icons.Default.VerifiedUser,
      selected = isVerifyMode,
      activeBrush =
        Brush.horizontalGradient(
          listOf(SuccessGreen.copy(alpha = 0.9f), ElectricBlue.copy(alpha = 0.85f))
        ),
      inactiveColor = BrandGold,
      onClick = { onSelectPurpose("verify") },
      testTag = "scanner_purpose_verify",
    )
  }
}

@Composable
private fun CompactPurposeChip(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  activeBrush: Brush,
  inactiveColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val bgMod =
    if (selected) {
      Modifier.clip(RoundedCornerShape(50))
        .background(activeBrush)
        .border(0.8.dp, PureWhite.copy(alpha = 0.6f), RoundedCornerShape(50))
    } else {
      Modifier.clip(RoundedCornerShape(50))
    }

  Row(
    modifier =
      Modifier.then(bgMod)
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 7.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) PureWhite else inactiveColor,
      modifier = Modifier.size(13.dp),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) PureWhite else inactiveColor,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Slim single-line status indicator automatically reflecting either the single active service
 * (no manual selector needed) or Official Card Verification mode.
 */
@Composable
private fun ScannerStatusStrip(
  isVerifyMode: Boolean,
  activeService: ChurchService?,
  onOpenServices: () -> Unit,
) {
  val accentColor =
    when {
      isVerifyMode -> SuccessGreen
      activeService != null -> SuccessGreen
      else -> BrandOrange
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xA6081836))
        .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
        .padding(horizontal = 12.dp, vertical = 9.dp)
        .testTag(if (isVerifyMode) "scanner_verify_mode_banner" else "scanner_auto_service_strip"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      if (isVerifyMode) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = SuccessGreen,
          modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.scanner_verify_banner_subtitle),
          style = MaterialTheme.typography.labelMedium,
          color = GlassWhite,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      } else if (activeService != null) {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(SuccessGreen),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = activeService.effectiveTitle,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (activeService.dateFormatted.isNotBlank()) {
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = activeService.dateFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = MutedGlassText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      } else {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(BrandOrange),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.scanner_auto_service_none),
          style = MaterialTheme.typography.labelMedium,
          color = BrandGold,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (!isVerifyMode && activeService == null) {
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(ElectricBlue.copy(alpha = 0.28f))
            .border(1.dp, BlueNeon.copy(alpha = 0.65f), RoundedCornerShape(50))
            .clickable(onClick = onOpenServices)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .testTag("scanner_create_active_service_btn"),
      ) {
        Text(
          text = stringResource(R.string.scanner_open_services_short),
          style = MaterialTheme.typography.labelSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }
  }
}

/**
 * Ultra-clean 2-tab segmented control: Eskane QR | Kat NFC.
 */
@Composable
private fun ScannerTwoTabBar(
  selectedMode: String,
  isVerifyMode: Boolean,
  onSelectMode: (String) -> Unit,
) {
  val activeBrush =
    if (isVerifyMode) {
      Brush.horizontalGradient(listOf(SuccessGreen.copy(alpha = 0.85f), ElectricBlue.copy(alpha = 0.88f)))
    } else {
      OmniBrushes.ActiveBlueNeonBrush
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xA606132E))
        .border(1.dp, GlassWhite.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    ScannerModePill(
      label = stringResource(R.string.scanner_tab_qr),
      icon = Icons.Default.QrCodeScanner,
      selected = selectedMode == "qr_scan",
      activeBrush = activeBrush,
      onClick = { onSelectMode("qr_scan") },
      testTag = "scanner_tab_qr_scan",
      modifier = Modifier.weight(1f),
    )
    ScannerModePill(
      label = stringResource(R.string.scanner_tab_nfc),
      icon = Icons.Default.Nfc,
      selected = selectedMode == "nfc_scan",
      activeBrush = activeBrush,
      onClick = { onSelectMode("nfc_scan") },
      testTag = "scanner_tab_nfc",
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun ScannerModePill(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  activeBrush: Brush,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val bgMod =
    if (selected) {
      Modifier.shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = ElectricBlue)
        .clip(RoundedCornerShape(12.dp))
        .background(activeBrush)
        .border(1.dp, PureWhite.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
    } else {
      Modifier.clip(RoundedCornerShape(12.dp))
    }

  Row(
    modifier =
      modifier
        .then(bgMod)
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 10.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) PureWhite else MutedGlassText,
      modifier = Modifier.size(17.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) PureWhite else MutedGlassText,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Refined single-row glass input bar directly inside the QR Scan tab for fast manual code entry
 * without switching tabs or adding bulky multi-card layouts.
 */
@Composable
private fun RefinedInlineMemberCodeBar(
  value: String,
  onValueChange: (String) -> Unit,
  isVerifyMode: Boolean,
  canSubmit: Boolean,
  onSubmit: () -> Unit,
) {
  val accentColor = if (isVerifyMode) SuccessGreen else BlueNeon
  val borderColor =
    if (value.isNotBlank()) accentColor.copy(alpha = 0.72f)
    else GlassWhite.copy(alpha = 0.22f)

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xCC071530))
        .border(1.1.dp, borderColor, RoundedCornerShape(16.dp))
        .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = if (isVerifyMode) Icons.Default.VerifiedUser else Icons.Default.Badge,
      contentDescription = null,
      tint = if (value.isNotBlank()) accentColor else MutedGlassText,
      modifier = Modifier.size(19.dp),
    )

    Spacer(modifier = Modifier.width(10.dp))

    Box(
      modifier = Modifier.weight(1f).padding(vertical = 8.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      if (value.isEmpty()) {
        Text(
          text =
            if (isVerifyMode) stringResource(R.string.scanner_inline_verify_placeholder)
            else stringResource(R.string.scanner_inline_code_placeholder),
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle =
          MaterialTheme.typography.bodyMedium.copy(
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
          ),
        cursorBrush = SolidColor(accentColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() }),
        modifier = Modifier.fillMaxWidth().testTag("scanner_manual_code_input"),
      )
    }

    if (value.isNotEmpty()) {
      IconButton(
        onClick = { onValueChange("") },
        modifier = Modifier.size(36.dp).testTag("scanner_clear_code_btn"),
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(R.string.common_close),
          tint = MutedGlassText,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    Spacer(modifier = Modifier.width(4.dp))

    Box(
      modifier =
        Modifier.size(42.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            if (canSubmit) {
              if (isVerifyMode) {
                Brush.linearGradient(listOf(SuccessGreen, ElectricBlue))
              } else {
                OmniBrushes.ActiveBlueNeonBrush
              }
            } else {
              Brush.linearGradient(listOf(Color(0x551B2E52), Color(0x5512203B)))
            }
          )
          .border(
            width = 1.dp,
            color = if (canSubmit) PureWhite.copy(alpha = 0.65f) else GlassWhite.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
          )
          .clickable(enabled = canSubmit, onClick = onSubmit)
          .testTag(if (isVerifyMode) "scanner_verify_manual_submit_btn" else "scanner_manual_submit_btn"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector =
          if (isVerifyMode) Icons.Default.VerifiedUser else Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription =
          if (isVerifyMode) stringResource(R.string.scanner_verify_manual_submit)
          else stringResource(R.string.scanner_manual_submit),
        tint = if (canSubmit) PureWhite else MutedGlassText.copy(alpha = 0.55f),
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun NfcTapViewportCard(
  isNfcSupported: Boolean,
  isNfcEnabled: Boolean,
  hasActiveService: Boolean,
  lastScannedToken: String?,
  isVerifyMode: Boolean = false,
  onOpenNfcSettings: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "nfc_radar")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.25f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "nfc_pulse_scale",
    )
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.65f,
      targetValue = 0.05f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "nfc_pulse_alpha",
    )

  val isReadyToTap = isNfcSupported && isNfcEnabled && hasActiveService
  val readyAccent = if (isVerifyMode) SuccessGreen else BlueNeon

  PremiumGlassCard(
    cornerRadius = 24.dp,
    isHighlighted = isReadyToTap,
    accentColor = if (isReadyToTap) readyAccent else BrandOrange,
    contentPadding = PaddingValues(20.dp),
    modifier = Modifier.fillMaxWidth().testTag("nfc_scanner_viewport"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (isReadyToTap) {
          Box(
            modifier =
              Modifier.size(110.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .border(2.dp, readyAccent, CircleShape)
          )
          Box(
            modifier =
              Modifier.size(90.dp)
                .clip(CircleShape)
                .background(readyAccent.copy(alpha = 0.16f))
                .border(1.2.dp, readyAccent.copy(alpha = 0.45f), CircleShape)
          )
        }
        Box(
          modifier =
            Modifier.size(68.dp)
              .shadow(14.dp, CircleShape, ambientColor = if (isReadyToTap) readyAccent else BrandOrange)
              .clip(CircleShape)
              .background(
                if (isReadyToTap) {
                  if (isVerifyMode) Brush.verticalGradient(listOf(SuccessGreen, ElectricBlue))
                  else OmniBrushes.ActiveBlueNeonBrush
                } else {
                  Brush.verticalGradient(listOf(BrandGold.copy(alpha = 0.3f), BrandOrange.copy(alpha = 0.25f)))
                }
              )
              .border(1.5.dp, PureWhite.copy(alpha = 0.78f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Contactless,
            contentDescription = stringResource(R.string.scanner_nfc_title),
            tint = PureWhite,
            modifier = Modifier.size(34.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = stringResource(R.string.scanner_nfc_title),
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text =
          when {
            !isNfcSupported -> stringResource(R.string.scanner_nfc_unsupported)
            !isNfcEnabled ->
              "NFC fèmen nan paramèt telefòn lan. Aktive NFC pou w kapab tape kat manm yo."
            !hasActiveService -> stringResource(R.string.scanner_no_active_service)
            isVerifyMode ->
              "Apwoche kat manm nan oswa braslè NFC a sou do telefòn lan pou verifye otantisite kat la otomatikman."
            else -> stringResource(R.string.scanner_nfc_subtitle)
          },
        style = MaterialTheme.typography.bodySmall,
        color = if (isReadyToTap) GlassWhite else BrandGold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(14.dp))

      if (isNfcSupported && !isNfcEnabled) {
        GlassButton(
          text = "Aktive NFC nan Paramèt",
          icon = Icons.Default.Settings,
          isPrimaryNeon = true,
          onClick = onOpenNfcSettings,
          modifier = Modifier.fillMaxWidth(),
          testTag = "nfc_open_settings_btn",
        )
      } else if (isReadyToTap) {
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(SuccessGreen.copy(alpha = 0.18f))
              .border(1.dp, SuccessGreen.copy(alpha = 0.65f), RoundedCornerShape(50))
              .padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.scanner_nfc_listening),
            style = MaterialTheme.typography.labelMedium,
            color = SuccessGreen,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (!lastScannedToken.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Dènye lekti NFC: $lastScannedToken",
          style = MaterialTheme.typography.labelSmall,
          color = MutedGlassText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraXQrViewportCard(
  isScanningEnabled: Boolean,
  isTorchEnabled: Boolean,
  isVerifyMode: Boolean = false,
  onToggleTorch: () -> Unit,
  onQrDetected: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  val previewView = remember { PreviewView(context) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }

  val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
  val laserYRatio by
    infiniteTransition.animateFloat(
      initialValue = 0.16f,
      targetValue = 0.84f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "scanner_laser_y",
    )

  LaunchedEffect(boundCamera, isTorchEnabled) {
    boundCamera?.cameraControl?.enableTorch(isTorchEnabled)
  }

  DisposableEffect(lifecycleOwner, isScanningEnabled) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val options =
      BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val barcodeScanner = BarcodeScanning.getClient(options)

    val listener = Runnable {
      try {
        val cameraProvider = cameraProviderFuture.get()
        val preview =
          Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis =
          ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
          val mediaImage = imageProxy.image
          if (mediaImage != null && isScanningEnabled) {
            val image =
              InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner
              .process(image)
              .addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (!rawValue.isNullOrBlank()) {
                  onQrDetected(rawValue)
                }
              }
              .addOnCompleteListener { imageProxy.close() }
          } else {
            imageProxy.close()
          }
        }

        cameraProvider.unbindAll()
        boundCamera =
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
          )
      } catch (_: Exception) {
      }
    }

    cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

    onDispose {
      try {
        cameraProviderFuture.get().unbindAll()
      } catch (_: Exception) {}
      barcodeScanner.close()
    }
  }

  val frameAccent = if (isVerifyMode) SuccessGreen else BlueNeon

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(256.dp)
        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = frameAccent, spotColor = DeepNavy)
        .clip(RoundedCornerShape(24.dp))
        .background(DeepNavy)
        .border(
          1.4.dp,
          Brush.linearGradient(listOf(frameAccent.copy(alpha = 0.65f), GlassWhite.copy(alpha = 0.25f))),
          RoundedCornerShape(24.dp),
        )
        .testTag("camerax_qr_viewport"),
  ) {
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize(),
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isTorchEnabled) BrandGold else Color(0x990A1936))
            .border(1.dp, GlassWhite.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onToggleTorch)
            .testTag("scanner_torch_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.FlashOn,
          contentDescription = stringResource(R.string.scanner_torch_toggle),
          tint = if (isTorchEnabled) DeepNavy else PureWhite,
          modifier = Modifier.size(19.dp),
        )
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Color(0x990A1936))
            .border(1.dp, frameAccent.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Text(
          text =
            if (isVerifyMode) stringResource(R.string.scanner_purpose_verify)
            else stringResource(R.string.scanner_tab_qr),
          style = MaterialTheme.typography.labelSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }

    Box(
      modifier =
        Modifier.align(Alignment.Center)
          .size(32.dp)
          .clip(CircleShape)
          .background(DeepNavy.copy(alpha = 0.45f)),
      contentAlignment = Alignment.Center,
    ) {
      OmniChurch3DFlameIcon(size = 22.dp)
    }

    Canvas(modifier = Modifier.size(186.dp).align(Alignment.Center)) {
      val w = size.width
      val h = size.height
      val len = w * 0.24f
      val rad = 18.dp.toPx()
      val stroke = 4.dp.toPx()
      val bracketBrush =
        if (isVerifyMode) {
          Brush.linearGradient(listOf(PureWhite, SuccessGreen, BrandGold))
        } else {
          Brush.linearGradient(listOf(PureWhite, BlueNeon, ElectricBlue))
        }

      val tl =
        Path().apply {
          moveTo(0f, len)
          lineTo(0f, rad)
          quadraticTo(0f, 0f, rad, 0f)
          lineTo(len, 0f)
        }
      drawPath(tl, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val tr =
        Path().apply {
          moveTo(w - len, 0f)
          lineTo(w - rad, 0f)
          quadraticTo(w, 0f, w, rad)
          lineTo(w, len)
        }
      drawPath(tr, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val bl =
        Path().apply {
          moveTo(0f, h - len)
          lineTo(0f, h - rad)
          quadraticTo(0f, h, rad, h)
          lineTo(len, h)
        }
      drawPath(bl, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val br =
        Path().apply {
          moveTo(w - len, h)
          lineTo(w - rad, h)
          quadraticTo(w, h, w, h - rad)
          lineTo(w, h - len)
        }
      drawPath(br, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val laserY = h * laserYRatio
      val laserColor = if (isVerifyMode) BrandGold else SuccessGreen
      drawLine(
        brush =
          Brush.horizontalGradient(
            listOf(Color.Transparent, laserColor, PureWhite, laserColor, Color.Transparent)
          ),
        start = Offset(-w * 0.06f, laserY),
        end = Offset(w * 1.06f, laserY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
private fun AttendanceValidationResultCard(
  outcome: ScannerValidationOutcome,
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
            text = stringResource(R.string.common_loading),
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite,
          )
        }
      }
    }
    is ScannerValidationOutcome.RegisteredSuccess -> {
      val isOnTime = outcome.record.status == "on_time"
      val statusBadgeText =
        if (isOnTime) stringResource(R.string.attendance_status_on_time)
        else stringResource(R.string.attendance_status_late)
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
            text = "${stringResource(R.string.attendance_registered_title)} ✓",
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
              text = stringResource(R.string.attendance_service_prefix),
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
            text = stringResource(R.string.scanner_verify_certificate_btn),
            icon = Icons.Default.VerifiedUser,
            isPrimaryNeon = false,
            onClick = { onVerifyMemberCertificate(outcome.member) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "attendance_verify_certificate_btn",
          )
        }

        GlassButton(
          text = stringResource(R.string.attendance_scan_next),
          icon = Icons.Default.QrCodeScanner,
          isPrimaryNeon = true,
          onClick = onScanNextClick,
          modifier = Modifier.fillMaxWidth(),
          testTag = "attendance_scan_next_btn",
        )
      }
    }
    is ScannerValidationOutcome.ValidationError -> {
      val errorMsg =
        outcome.details?.takeIf { it.isNotBlank() }
          ?: when (outcome.errorType) {
            AttendanceErrorType.INVALID_QR -> stringResource(R.string.attendance_err_invalid_qr)
            AttendanceErrorType.WRONG_CHURCH -> "SEKIRITE: Kòd sa a fè pati yon lòt legliz!"
            AttendanceErrorType.DUPLICATE_ATTENDANCE ->
              stringResource(R.string.attendance_err_duplicate)
            AttendanceErrorType.SERVICE_CLOSED ->
              stringResource(R.string.attendance_err_service_closed)
            AttendanceErrorType.UNAUTHORIZED -> stringResource(R.string.attendance_err_unauthorized)
            AttendanceErrorType.NETWORK_ERROR -> stringResource(R.string.attendance_err_network)
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
            text = "Lye Kat NFC (${unlinkedNfcUid.removePrefix("NFC-UID:")}) ak yon Manm",
            icon = Icons.Default.Link,
            isPrimaryNeon = true,
            onClick = onBindNfcClick,
            modifier = Modifier.fillMaxWidth(),
            testTag = "nfc_bind_member_btn",
          )
        }
        GlassButton(
          text = stringResource(R.string.attendance_scan_next),
          isPrimaryNeon = false,
          onClick = onScanNextClick,
          modifier = Modifier.fillMaxWidth(),
          testTag = "attendance_error_retry_btn",
        )
      }
    }
  }
}
