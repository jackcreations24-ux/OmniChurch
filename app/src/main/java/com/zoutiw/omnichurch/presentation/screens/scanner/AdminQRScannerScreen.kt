package com.zoutiw.omnichurch.presentation.screens.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.core.utilities.NfcAttendanceHelper
import com.zoutiw.omnichurch.core.utilities.NfcIntentBus
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.AttendanceErrorType
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import kotlinx.coroutines.flow.Flow

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
  val copy = remember(currentLanguageCode) { ScannerStrings.forLanguage(currentLanguageCode) }

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

  var nowMillis by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(activeServices) {
    nowMillis = System.currentTimeMillis()
    while (true) {
      kotlinx.coroutines.delay(10_000L)
      nowMillis = System.currentTimeMillis()
    }
  }

  // Strict LIVE attendance window enforcement: ONLY services currently live (open, reached startTime, before endTime)
  val liveActiveServices =
    remember(activeServices, nowMillis) {
      activeServices.filter { it.isLiveBadgeActive(nowMillis) }
    }
  val activeService =
    remember(liveActiveServices, selectedServiceId) {
      liveActiveServices.firstOrNull { it.id == selectedServiceId }
        ?: liveActiveServices.firstOrNull { it.isLive }
        ?: liveActiveServices.firstOrNull()
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
            val scanTime = System.currentTimeMillis()
            if (srv != null && srv.isLiveBadgeActive(scanTime) && currentValidationOutcome !is ScannerValidationOutcome.Processing) {
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
      } else if (activeService != null && activeService.isLiveBadgeActive(System.currentTimeMillis())) {
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
      title = copy.screenTitleCompact,
      onBackClick = onBackClick,
      trailingContent = {
        if (onVerifyCard != null) {
          CompactPurposeBadgeSwitch(
            isVerifyMode = isVerifyMode,
            copy = copy,
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
          copy = copy,
          onOpenServices = onOpenServicesManagement,
        )

        // 2. Clean 2-tab switch: Eskane QR | Kat NFC
        ScannerTwoTabBar(
          selectedMode = scanMode,
          isVerifyMode = isVerifyMode,
          copy = copy,
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
              copy = copy,
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
              title = copy.cameraPermissionTitle,
              subtitle = copy.cameraPermissionMsg,
              icon = Icons.Default.CameraAlt,
              actionLabel = copy.grantPermission,
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
            copy = copy,
            onSubmit = submitInlineCode,
          )
        } else {
          NfcTapViewportCard(
            isNfcSupported = isNfcSupported,
            isNfcEnabled = isNfcEnabled,
            hasActiveService = isVerifyMode || activeService != null,
            lastScannedToken = lastScannedNfcToken,
            isVerifyMode = isVerifyMode,
            copy = copy,
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
            copy = copy,
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
    NfcBindMemberBottomSheet(
      rawUid = rawUid,
      churchMembers = churchMembers,
      searchQuery = nfcBindSearchQuery,
      copy = copy,
      onSearchQueryChange = { nfcBindSearchQuery = it },
      onDismiss = { showBindNfcMemberSheet = false },
      onBindMember = { mem ->
        onBindNfcCardToMember(mem.id, rawUid) {
          showBindNfcMemberSheet = false
          nfcBindSearchQuery = ""
          val srv = activeService
          val scannedToken = lastScannedNfcToken
          if (srv != null && scannedToken != null) {
            onResetOutcome()
            onScanOrSubmitCode(scannedToken, srv, "nfc_scan")
          } else {
            onResetOutcome()
          }
        }
      },
    )
  }
}
