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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.NfcAttendanceHelper
import com.zoutiw.omnichurch.core.utilities.NfcIntentBus
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.AttendanceErrorType
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.OmniChurch3DFlameIcon
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.components.WorshipStageBannerCanvas
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
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
  onBindNfcCardToMember: ((memberDocId: String, nfcCardUid: String, onLinked: () -> Unit) -> Unit)? =
    null,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val lifecycleOwner = LocalLifecycleOwner.current
  val haptic = LocalHapticFeedback.current

  var scanMode by remember { mutableStateOf("qr_scan") }
  var manualCodeInput by remember { mutableStateOf("") }
  var showServicePickerSheet by remember { mutableStateOf(false) }
  var isTorchEnabled by remember { mutableStateOf(false) }
  var lastScannedNfcToken by remember { mutableStateOf<String?>(null) }
  var showBindNfcMemberSheet by remember { mutableStateOf(false) }
  var nfcBindSearchQuery by remember { mutableStateOf("") }

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

  val selectedService =
    remember(activeServices, selectedServiceId) {
      activeServices.firstOrNull { it.id == selectedServiceId } ?: activeServices.firstOrNull()
    }

  val currentSelectedService by rememberUpdatedState(selectedService)
  val currentValidationOutcome by rememberUpdatedState(validationOutcome)
  val currentScanMode by rememberUpdatedState(scanMode)

  val nfcHelper =
    remember(activity) {
      activity?.let { act ->
        NfcAttendanceHelper(act) { scannedToken ->
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          lastScannedNfcToken = scannedToken
          val srv = currentSelectedService
          if (
            srv != null &&
              currentValidationOutcome !is ScannerValidationOutcome.Processing
          ) {
            onResetOutcome()
            onScanOrSubmitCode(scannedToken, srv, "nfc_scan")
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

  val roleBadgeText =
    if (userProfile?.role == "usher") stringResource(R.string.scanner_role_usher)
    else stringResource(R.string.scanner_role_admin)

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .navigationBarsPadding(),
  ) {
    GlassTopBar(
      title = stringResource(R.string.scanner_screen_title),
      onBackClick = onBackClick,
      rightBadgeText = roleBadgeText,
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      ScannerModeTabBar(
        selectedMode = scanMode,
        onSelectMode = { mode ->
          scanMode = mode
          if (mode != "nfc_scan") {
            lastScannedNfcToken = null
          }
          onResetOutcome()
        },
      )

      ActiveServiceSelectorCard(
        selectedService = selectedService,
        onChangeServiceClick = {
          if (activeServices.size > 1) {
            showServicePickerSheet = true
          } else {
            onOpenServicesManagement()
          }
        },
        onCreateServiceClick = onOpenServicesManagement,
      )

      when (scanMode) {
        "qr_scan" -> {
          if (hasCameraPermission) {
            CameraXQrViewportCard(
              isScanningEnabled =
                selectedService != null && validationOutcome is ScannerValidationOutcome.Idle,
              isTorchEnabled = isTorchEnabled,
              onToggleTorch = { isTorchEnabled = !isTorchEnabled },
              onQrDetected = { rawToken ->
                if (selectedService != null && validationOutcome is ScannerValidationOutcome.Idle) {
                  onScanOrSubmitCode(rawToken, selectedService, "qr_scan")
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
        }
        "nfc_scan" -> {
          NfcTapViewportCard(
            isNfcSupported = isNfcSupported,
            isNfcEnabled = isNfcEnabled,
            hasActiveService = selectedService != null,
            lastScannedToken = lastScannedNfcToken,
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
        else -> {
          PremiumGlassCard(
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(18.dp),
          ) {
            Text(
              text = stringResource(R.string.scanner_manual_input_label),
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassInputField(
              value = manualCodeInput,
              onValueChange = { manualCodeInput = it },
              label = stringResource(R.string.scanner_tab_manual),
              placeholder = stringResource(R.string.scanner_manual_input_hint),
              testTag = "scanner_manual_code_input",
            )
            Spacer(modifier = Modifier.height(14.dp))
            GlassButton(
              text = stringResource(R.string.scanner_manual_submit),
              icon = Icons.Default.CheckCircle,
              enabled = manualCodeInput.isNotBlank() && selectedService != null,
              onClick = {
                if (selectedService != null) {
                  onScanOrSubmitCode(manualCodeInput, selectedService, "manual")
                }
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "scanner_manual_submit_btn",
            )
          }
        }
      }

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
        onScanNextClick = {
          manualCodeInput = ""
          lastScannedNfcToken = null
          onResetOutcome()
        },
      )

      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  if (showServicePickerSheet) {
    GlassModalBottomSheet(
      onDismissRequest = { showServicePickerSheet = false },
      title = stringResource(R.string.scanner_select_service_label),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        activeServices.forEach { srv ->
          PremiumGlassCard(
            cornerRadius = 16.dp,
            isHighlighted = srv.id == selectedService?.id,
            onClick = {
              onSelectService(srv.id)
              showServicePickerSheet = false
            },
            contentPadding = PaddingValues(14.dp),
          ) {
            Text(
              text = srv.title,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = srv.dateFormatted,
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite,
            )
          }
        }
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
                val srv = selectedService
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
                  )
                  Text(
                    text = "${mem.effectiveMatrikil} • ${mem.department.ifBlank { "Manm" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGlassText,
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

@Composable
private fun ScannerModeTabBar(
  selectedMode: String,
  onSelectMode: (String) -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(Color(0x9906132E))
        .border(1.dp, GlassWhite.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    ScannerModePill(
      label = stringResource(R.string.scanner_tab_qr),
      icon = Icons.Default.QrCodeScanner,
      selected = selectedMode == "qr_scan",
      onClick = { onSelectMode("qr_scan") },
      testTag = "scanner_tab_qr_scan",
      modifier = Modifier.weight(1f),
    )
    ScannerModePill(
      label = stringResource(R.string.scanner_tab_nfc),
      icon = Icons.Default.Nfc,
      selected = selectedMode == "nfc_scan",
      onClick = { onSelectMode("nfc_scan") },
      testTag = "scanner_tab_nfc",
      modifier = Modifier.weight(1f),
    )
    ScannerModePill(
      label = "Kòd Manm",
      icon = Icons.Default.Keyboard,
      selected = selectedMode == "manual",
      onClick = { onSelectMode("manual") },
      testTag = "scanner_tab_manual",
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun ScannerModePill(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val bgMod =
    if (selected) {
      Modifier.shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = ElectricBlue)
        .clip(RoundedCornerShape(14.dp))
        .background(OmniBrushes.ActiveBlueNeonBrush)
        .border(1.dp, PureWhite.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
    } else {
      Modifier.clip(RoundedCornerShape(14.dp))
    }

  Row(
    modifier =
      modifier
        .then(bgMod)
        .clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 11.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) PureWhite else MutedGlassText,
      modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) PureWhite else MutedGlassText,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun NfcTapViewportCard(
  isNfcSupported: Boolean,
  isNfcEnabled: Boolean,
  hasActiveService: Boolean,
  lastScannedToken: String?,
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

  PremiumGlassCard(
    cornerRadius = 24.dp,
    isHighlighted = isReadyToTap,
    accentColor = if (isReadyToTap) BlueNeon else BrandOrange,
    contentPadding = PaddingValues(22.dp),
    modifier = Modifier.fillMaxWidth().testTag("nfc_scanner_viewport"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier.size(132.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (isReadyToTap) {
          Box(
            modifier =
              Modifier.size(116.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .border(2.dp, BlueNeon, CircleShape)
          )
          Box(
            modifier =
              Modifier.size(94.dp)
                .clip(CircleShape)
                .background(ElectricBlue.copy(alpha = 0.16f))
                .border(1.2.dp, BlueNeon.copy(alpha = 0.45f), CircleShape)
          )
        }
        Box(
          modifier =
            Modifier.size(72.dp)
              .shadow(14.dp, CircleShape, ambientColor = if (isReadyToTap) BlueNeon else BrandOrange)
              .clip(CircleShape)
              .background(
                if (isReadyToTap) OmniBrushes.ActiveBlueNeonBrush
                else Brush.verticalGradient(listOf(BrandGold.copy(alpha = 0.3f), BrandOrange.copy(alpha = 0.25f)))
              )
              .border(1.5.dp, PureWhite.copy(alpha = 0.78f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Contactless,
            contentDescription = stringResource(R.string.scanner_nfc_title),
            tint = PureWhite,
            modifier = Modifier.size(36.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = stringResource(R.string.scanner_nfc_title),
        style = MaterialTheme.typography.titleLarge,
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
            else -> stringResource(R.string.scanner_nfc_subtitle)
          },
        style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun ActiveServiceSelectorCard(
  selectedService: ChurchService?,
  onChangeServiceClick: () -> Unit,
  onCreateServiceClick: () -> Unit,
) {
  PremiumGlassCard(
    cornerRadius = 22.dp,
    contentPadding = PaddingValues(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = null,
          tint = GlassWhite,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.scanner_select_service_label),
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Color(0x99173163))
            .border(1.dp, GlassWhite.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onChangeServiceClick)
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .testTag("scanner_change_service_btn"),
      ) {
        Text(
          text = stringResource(R.string.scanner_change_service),
          style = MaterialTheme.typography.labelMedium,
          color = PureWhite,
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (selectedService == null) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.scanner_no_active_service),
          style = MaterialTheme.typography.bodyMedium,
          color = MutedGlassText,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        GlassButton(
          text = stringResource(R.string.feed_add_service),
          onClick = onCreateServiceClick,
          testTag = "scanner_create_active_service_btn",
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(width = 76.dp, height = 56.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, GlassWhite.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
        ) {
          if (!selectedService.imageUrl.isNullOrBlank()) {
            AsyncImage(
              model = selectedService.imageUrl,
              contentDescription = selectedService.effectiveTitle,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            WorshipStageBannerCanvas(modifier = Modifier.fillMaxSize())
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = selectedService.title,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(SuccessGreen.copy(alpha = 0.22f))
                .border(1.dp, SuccessGreen.copy(alpha = 0.65f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier.size(7.dp).clip(CircleShape).background(SuccessGreen)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = stringResource(R.string.scanner_open_for_attendance),
              style = MaterialTheme.typography.labelSmall,
              color = SuccessGreen,
              fontWeight = FontWeight.Bold,
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = MutedGlassText,
              modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = selectedService.dateFormatted,
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite.copy(alpha = 0.85f),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraXQrViewportCard(
  isScanningEnabled: Boolean,
  isTorchEnabled: Boolean,
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

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(275.dp)
        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = ElectricBlue, spotColor = DeepNavy)
        .clip(RoundedCornerShape(24.dp))
        .background(DeepNavy)
        .border(
          1.4.dp,
          Brush.linearGradient(listOf(BlueNeon.copy(alpha = 0.6f), GlassWhite.copy(alpha = 0.25f))),
          RoundedCornerShape(24.dp),
        )
        .testTag("camerax_qr_viewport"),
  ) {
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize(),
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp).align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Box(
        modifier =
          Modifier.size(42.dp)
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
          modifier = Modifier.size(20.dp),
        )
      }

      Box(
        modifier =
          Modifier.size(42.dp)
            .clip(CircleShape)
            .background(Color(0x990A1936))
            .border(1.dp, GlassWhite.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = null,
          tint = PureWhite,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Box(
      modifier =
        Modifier.align(Alignment.Center)
          .size(34.dp)
          .clip(CircleShape)
          .background(DeepNavy.copy(alpha = 0.45f)),
      contentAlignment = Alignment.Center,
    ) {
      OmniChurch3DFlameIcon(size = 24.dp)
    }

    Canvas(modifier = Modifier.size(200.dp).align(Alignment.Center)) {
      val w = size.width
      val h = size.height
      val len = w * 0.24f
      val rad = 20.dp.toPx()
      val stroke = 4.5.dp.toPx()
      val bracketBrush = Brush.linearGradient(listOf(PureWhite, BlueNeon, ElectricBlue))

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
      drawLine(
        brush =
          Brush.horizontalGradient(
            listOf(Color.Transparent, SuccessGreen, PureWhite, SuccessGreen, Color.Transparent)
          ),
        start = Offset(-w * 0.08f, laserY),
        end = Offset(w * 1.08f, laserY),
        strokeWidth = 3.5.dp.toPx(),
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
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(20.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(28.dp))
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.common_loading),
            style = MaterialTheme.typography.titleMedium,
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
              elevation = 20.dp,
              shape = RoundedCornerShape(24.dp),
              ambientColor = SuccessGreen,
              spotColor = SuccessGreen,
            )
            .clip(RoundedCornerShape(24.dp))
            .background(OmniBrushes.SuccessGlassBrush)
            .border(
              1.6.dp,
              Brush.linearGradient(listOf(PureWhite, SuccessGreen, ElectricBlue)),
              RoundedCornerShape(24.dp),
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
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${stringResource(R.string.attendance_registered_title)} ✓",
            style = MaterialTheme.typography.titleLarge,
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
                  Modifier.size(52.dp)
                    .clip(CircleShape)
                    .border(2.dp, SuccessGreen, CircleShape),
              )
            } else {
              MemberAvatar3D(fullName = outcome.member.fullName, size = 52.dp)
            }

            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = outcome.member.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "ID: ${outcome.member.effectiveMatrikil}",
                style = MaterialTheme.typography.bodySmall,
                color = GlassWhite.copy(alpha = 0.85f),
              )
            }
          }

          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(12.dp))
                .background(statusBadgeColor.copy(alpha = 0.28f))
                .border(1.2.dp, statusBadgeColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 7.dp)
          ) {
            Text(
              text = statusBadgeText,
              style = MaterialTheme.typography.titleSmall,
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
          Row(verticalAlignment = Alignment.CenterVertically) {
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
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = GlassWhite,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = outcome.formattedTime,
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
            )
          }
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
            .shadow(18.dp, RoundedCornerShape(24.dp), ambientColor = ErrorRed, spotColor = ErrorRed)
            .clip(RoundedCornerShape(24.dp))
            .background(OmniBrushes.ErrorGlassBrush)
            .border(1.5.dp, ErrorRed, RoundedCornerShape(24.dp))
            .padding(16.dp)
            .testTag("attendance_error_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = null,
          tint = ErrorRed,
          modifier = Modifier.size(34.dp),
        )
        Text(
          text = errorMsg,
          style = MaterialTheme.typography.titleMedium,
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
