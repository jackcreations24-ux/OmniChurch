package com.zoutiw.omnichurch.presentation.screens.services

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.formatCreoleDateWithDay
import com.zoutiw.omnichurch.presentation.components.CreateAnnouncementBottomSheet
import com.zoutiw.omnichurch.presentation.components.CreateServiceBottomSheet
import com.zoutiw.omnichurch.presentation.components.Floating3DReactionSelectorBar
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.core.ads.EffectiveMobileAdMobState
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.presentation.components.OmniAdMobBanner
import com.zoutiw.omnichurch.presentation.components.PremiumAnnouncementCard
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.components.PremiumServiceCard
import com.zoutiw.omnichurch.presentation.components.Stacked3DReactionCluster
import com.zoutiw.omnichurch.presentation.components.rememberFeedChromeNestedScrollConnection
import com.zoutiw.omnichurch.presentation.components.shareOmniChurchContent
import com.zoutiw.omnichurch.presentation.components.shareOmniChurchGeneralApp
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.presentation.screens.auth.GoogleAccountPayload
import com.zoutiw.omnichurch.presentation.screens.auth.launchOptionalGoogleSignIn
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.LiveBadgeRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun ServicesScreen(
  userProfile: UserProfile?,
  servicesState: UiState<List<ChurchService>>,
  onCreateService: (
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    category: String,
    date: String,
    startTime: String,
    endTime: String,
    theme: String?,
    passage: String?,
    leader: String?,
    preacher: String?,
    reader: String?,
    musicians: String?,
    ushers: String?,
    sendReminder24h: Boolean,
    imageUrl: String?,
  ) -> Unit,
  onToggleAttendanceWindow: (ChurchService, Boolean) -> Unit,
  observeAttendances: (serviceId: String) -> Flow<List<AttendanceRecord>>,
  onOpenScannerForService: (String) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
  onDeleteService: (String) -> Unit = {},
  isAdminSession: Boolean = false,
  canCreateService: Boolean = isAdminSession,
  canEditService: Boolean = canCreateService,
  onUpdateService: (
    existingService: ChurchService,
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    category: String,
    date: String,
    startTime: String,
    endTime: String,
    theme: String?,
    passage: String?,
    leader: String?,
    preacher: String?,
    reader: String?,
    musicians: String?,
    ushers: String?,
    sendReminder24h: Boolean,
    imageUrl: String?,
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
  temporaryAuthorizedServiceIds: Set<String> = emptySet(),
  reactionSummaries: Map<String, TargetReactionSummary> = emptyMap(),
  onReactToService: (serviceId: String, reactionType: String) -> Unit = { _, _ -> },
  observeComments: (targetId: String) -> Flow<List<AnnouncementComment>> = { emptyFlow() },
  onPostComment: (serviceId: String, content: String) -> Unit = { _, _ -> },
  onReactToComment: (commentId: String, reactionType: String) -> Unit = { _, _ -> },
  churchMembers: List<Member> = emptyList(),
  isChromeVisible: Boolean = true,
  onChromeVisibilityChange: (Boolean) -> Unit = {},
  adMobState: EffectiveMobileAdMobState = EffectiveMobileAdMobState(),
) {
  BackHandler(onBack = onBackClick)
  val isAdmin = isAdminSession || userProfile?.isAdminOrSuperAdmin == true

  var showCreateDialog by remember { mutableStateOf(false) }
  var editingService by remember { mutableStateOf<ChurchService?>(null) }
  var selectedAttendanceService by remember { mutableStateOf<ChurchService?>(null) }
  var activeCommentServiceId by remember { mutableStateOf<String?>(null) }

  val listState = rememberLazyListState()
  val feedScrollConnection = rememberFeedChromeNestedScrollConnection(
    listState = listState,
    onChromeVisibilityChange = onChromeVisibilityChange,
  )
  val density = LocalDensity.current
  var topBarHeightPx by remember { mutableIntStateOf(0) }
  val topBarHeightDp = with(density) {
    if (topBarHeightPx > 0) topBarHeightPx.toDp() else 72.dp
  }
  val isAtTop by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
    }
  }
  val topBarBackdropAlpha by animateFloatAsState(
    targetValue = if (isAtTop) 0f else 0.94f,
    animationSpec = tween(durationMillis = 160),
    label = "services_topbar_backdrop",
  )

  Box(
    modifier = modifier.fillMaxSize(),
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().nestedScroll(feedScrollConnection),
      contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = topBarHeightDp + 6.dp, bottom = 110.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (adMobState.shouldShowBanner(OmniAdPlacement.SERVICES)) {
        item(key = "services_admob_banner") {
          OmniAdMobBanner(
            placement = OmniAdPlacement.SERVICES,
            adMobState = adMobState,
          )
        }
      }

      if (canCreateService) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            GlassButton(
              text = stringResource(R.string.services_create_title),
              icon = Icons.Default.Add,
              onClick = { showCreateDialog = true },
              modifier = Modifier.fillMaxWidth(),
              testTag = "services_create_new_btn",
            )
          }
        }
      }

      when (servicesState) {
        is UiState.Loading -> item { GlassLoadingIndicator() }
        is UiState.Error ->
          item {
            PremiumEmptyState(
              title = stringResource(R.string.services_empty_title),
              subtitle = servicesState.message,
              icon = Icons.Default.CalendarMonth,
            )
          }
        is UiState.Success -> {
          val list = servicesState.data
          if (list.isEmpty()) {
            item {
              PremiumEmptyState(
                title = stringResource(R.string.services_empty_title),
                subtitle = stringResource(R.string.services_empty_subtitle),
                icon = Icons.Default.CalendarMonth,
                actionLabel =
                  if (canCreateService) stringResource(R.string.services_create_title) else null,
                onActionClick =
                  if (canCreateService) ({ showCreateDialog = true }) else null,
              )
            }
          } else {
            items(list, key = { it.id }) { service ->
              val canScanThisService =
                isAdmin ||
                  (service.id in temporaryAuthorizedServiceIds && !service.isServiceEnded())
              PremiumServiceCard(
                service = service,
                reactionSummary = reactionSummaries[service.id],
                onReactClick = { onReactToService(service.id, "pray") },
                onSelectReaction = { reactionType ->
                  onReactToService(service.id, reactionType.key)
                },
                onCommentClick = { activeCommentServiceId = service.id },
                isAdmin = isAdmin,
                canEdit = canEditService,
                isScanner = canScanThisService,
                churchMembers = churchMembers,
                onToggleAttendanceWindow =
                  if (isAdmin) {
                    { isOpen -> onToggleAttendanceWindow(service, isOpen) }
                  } else null,
                onOpenScanner =
                  if (canScanThisService) {
                    { onOpenScannerForService(service.id) }
                  } else null,
                onViewAttendances =
                  if (canScanThisService) {
                    { selectedAttendanceService = service }
                  } else null,
                onEditClick =
                  if (canEditService) {
                    { editingService = service }
                  } else null,
                onDeleteClick =
                  if (isAdmin) {
                    { onDeleteService(service.id) }
                  } else null,
              )
            }
          }
        }
      }
    }

    AnimatedVisibility(
      visible = isChromeVisible,
      enter = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = tween(durationMillis = 180),
      ) + fadeIn(animationSpec = tween(durationMillis = 160)),
      exit = slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(durationMillis = 180),
      ) + fadeOut(animationSpec = tween(durationMillis = 140)),
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF061124).copy(alpha = topBarBackdropAlpha))
          .onSizeChanged { size ->
            if (size.height > 0) topBarHeightPx = size.height
          }
          .statusBarsPadding(),
      ) {
        GlassTopBar(
          title = stringResource(R.string.services_screen_title),
          onBackClick = onBackClick,
        )
      }
    }
  }

  // Service Comments Modal Bottom Sheet
  if (activeCommentServiceId != null) {
    val targetServiceId = activeCommentServiceId!!
    val comments by
      observeComments(targetServiceId).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentInput by remember { mutableStateOf("") }
    var activeReactionCommentId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    GlassModalBottomSheet(
      onDismissRequest = { activeCommentServiceId = null },
      title = stringResource(R.string.feed_comments_title, comments.size),
    ) {
      if (comments.isEmpty()) {
        Text(
          text = stringResource(R.string.feed_no_comments),
          style = MaterialTheme.typography.bodyMedium,
          color = MutedGlassText,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(comments, key = { it.id }) { comment ->
            val commentReactionSummary = reactionSummaries[comment.id]
            val isSelectorOpen = activeReactionCommentId == comment.id

            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x660B1D3D))
                  .border(0.8.dp, GlassWhite.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                  .padding(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.Top,
                  modifier = Modifier.weight(1f),
                ) {
                  MemberAvatar3D(
                    fullName = comment.userName,
                    size = 34.dp,
                    photoUrl = comment.userPhotoUrl,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = comment.userName,
                      style = MaterialTheme.typography.labelLarge,
                      color = BrandGold,
                    )
                    Text(
                      text = comment.content,
                      style = MaterialTheme.typography.bodyMedium,
                      color = PureWhite,
                    )
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Stacked3DReactionCluster(
                  reactionSummary = commentReactionSummary,
                  fallbackCount = (commentReactionSummary?.totalCount ?: 0).toLong(),
                  isCompact = true,
                  onTriggerClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeReactionCommentId =
                      if (isSelectorOpen) null else comment.id
                  },
                  testTag = "service_comment_react_btn_${comment.id}",
                )
              }

              if (isSelectorOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                Floating3DReactionSelectorBar(
                  visible = true,
                  selectedReaction = commentReactionSummary?.currentUserReaction,
                  onSelectReaction = { chosenReaction ->
                    onReactToComment(comment.id, chosenReaction.key)
                    activeReactionCommentId = null
                  },
                  onDismiss = { activeReactionCommentId = null },
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GlassInputField(
          value = commentInput,
          onValueChange = { commentInput = it },
          label = stringResource(R.string.feed_comment_hint),
          modifier = Modifier.weight(1f),
          testTag = "service_comment_input_field",
        )
        IconButton(
          onClick = {
            if (commentInput.isNotBlank()) {
              onPostComment(targetServiceId, commentInput)
              commentInput = ""
            }
          },
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(ElectricBlue)
              .testTag("service_comment_send_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.feed_comment_send),
            tint = PureWhite,
          )
        }
      }
    }
  }

  // View real-time attendance records for selected service (with isolated Vizitè KPIs & filters)
  if (selectedAttendanceService != null) {
    val srv = selectedAttendanceService!!
    val records by
      observeAttendances(srv.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var activeFilter by remember(srv.id) { mutableStateOf("tout") }

    val regularRecords = remember(records) { records.filter { !it.isCrossChurchVisitor } }
    val visitorRecords = remember(records) { records.filter { it.isCrossChurchVisitor } }
    val regularMembers = remember(churchMembers) {
      churchMembers.filter { !it.id.startsWith("ADMIN-", ignoreCase = true) }
    }

    val onTimeCount = remember(regularRecords) {
      regularRecords.count { it.effectiveStatus.equals("on_time", ignoreCase = true) || it.effectiveStatus.equals("present", ignoreCase = true) }
    }
    val lateCount = remember(regularRecords) {
      regularRecords.count { it.effectiveStatus.equals("late", ignoreCase = true) || it.effectiveStatus.equals("an_reta", ignoreCase = true) }
    }
    val presentCount = regularRecords.size

    val absentMembers = remember(srv, regularRecords, regularMembers) {
      if (!srv.isFinishedForAbsence() || regularRecords.isEmpty()) {
        emptyList()
      } else {
        val accountedKeys = mutableSetOf<String>()
        regularRecords.forEach { rec ->
          listOf(
              rec.effectiveMemberDbId,
              rec.memberDocId,
              rec.memberId,
              rec.memberCode,
              rec.userId,
              rec.barcodeToken,
            )
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .forEach { accountedKeys.add(it) }
        }
        regularMembers
          .filter { mem ->
            val memKeys =
              listOfNotNull(
                  mem.id,
                  mem.effectiveMatrikil,
                  mem.memberCode,
                  mem.memberId,
                  mem.userId,
                  mem.effectiveBarcodeToken,
                )
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
            memKeys.none { it in accountedKeys }
          }
          .sortedBy { it.fullName.lowercase() }
      }
    }
    val absentCount = absentMembers.size
    val ratePercent =
      if (regularMembers.isEmpty()) 0
      else ((presentCount * 100.0) / regularMembers.size).toInt().coerceIn(0, 100)

    val filteredAttendanceRecords = remember(records, regularRecords, visitorRecords, activeFilter) {
      when (activeFilter) {
        "ale" -> regularRecords.filter { !it.effectiveStatus.equals("late", ignoreCase = true) && !it.effectiveStatus.equals("an_reta", ignoreCase = true) }
        "anreta" -> regularRecords.filter { it.effectiveStatus.equals("late", ignoreCase = true) || it.effectiveStatus.equals("an_reta", ignoreCase = true) }
        "vizite" -> visitorRecords
        "absan" -> emptyList()
        else -> records
      }
    }
    val showAbsentList = activeFilter == "tout" || activeFilter == "absan"

    GlassModalBottomSheet(
      onDismissRequest = { selectedAttendanceService = null },
      title = "${srv.effectiveTitle} (${presentCount + visitorRecords.size})",
    ) {
      // KPI Strip (Regular members strictly isolated from Visitor count)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x660B1D3D))
            .border(0.8.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        ServiceAttendanceKpiCell(
          label = "Prezan",
          value = presentCount.toString(),
          subValue = if (visitorRecords.isNotEmpty()) "+${visitorRecords.size} Vizitè" else null,
          color = SuccessGreen,
          modifier = Modifier.weight(1f),
        )
        ServiceAttendanceKpiCell(
          label = "Alè",
          value = onTimeCount.toString(),
          color = ElectricBlue,
          modifier = Modifier.weight(0.9f),
        )
        ServiceAttendanceKpiCell(
          label = "Anreta",
          value = lateCount.toString(),
          color = BrandGold,
          modifier = Modifier.weight(0.9f),
        )
        ServiceAttendanceKpiCell(
          label = "Absan",
          value = absentCount.toString(),
          color = ErrorRed,
          modifier = Modifier.weight(0.9f),
        )
        ServiceAttendanceKpiCell(
          label = "To %",
          value = "$ratePercent%",
          color = PureWhite,
          modifier = Modifier.weight(0.9f),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Filter Pills
      val filterOptions = listOf(
        "tout" to "Tout",
        "ale" to "Alè",
        "anreta" to "Anreta",
        "absan" to "Absan",
        "vizite" to "Vizitè (${visitorRecords.size})",
      )
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        items(filterOptions, key = { it.first }) { (key, label) ->
          val isSelected = activeFilter == key
          val accent = if (key == "vizite") BlueNeon else ElectricBlue
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(if (isSelected) accent.copy(alpha = 0.30f) else Color(0x550B1D3D))
                .border(
                  1.dp,
                  if (isSelected) accent else GlassWhite.copy(alpha = 0.22f),
                  RoundedCornerShape(50),
                )
                .clickable { activeFilter = key }
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("service_attendance_filter_$key"),
          ) {
            Text(
              text = label,
              style = MaterialTheme.typography.labelMedium,
              color = if (isSelected) PureWhite else MutedGlassText,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (filteredAttendanceRecords.isEmpty() && (!showAbsentList || absentMembers.isEmpty())) {
        Text(
          text =
            when (activeFilter) {
              "vizite" -> "Poko gen okenn vizitè ki eskane nan sèvis sa a."
              "absan" -> "Pa gen okenn manm absan pou sèvis sa a."
              else -> "Poko gen prezans anrejistre pou sèvis sa a."
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MutedGlassText,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(filteredAttendanceRecords, key = { it.id }) { rec ->
            val isOnTime = !rec.effectiveStatus.equals("late", ignoreCase = true) &&
              !rec.effectiveStatus.equals("an_reta", ignoreCase = true)
            val isVisitor = rec.isCrossChurchVisitor
            val originChurch =
              rec.homeChurchName?.takeIf { it.isNotBlank() } ?: rec.homeChurchId.orEmpty()
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x660B1D3D))
                  .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                MemberAvatar3D(fullName = rec.memberName, size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = rec.memberName,
                      style = MaterialTheme.typography.titleSmall,
                      color = PureWhite,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isVisitor) {
                      Box(
                        modifier =
                          Modifier.clip(RoundedCornerShape(50))
                            .background(ElectricBlue.copy(alpha = 0.25f))
                            .border(0.8.dp, BlueNeon.copy(alpha = 0.8f), RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                      ) {
                        Text(
                          text = "Vizitè",
                          style = MaterialTheme.typography.labelSmall,
                          color = BlueNeon,
                          fontWeight = FontWeight.Bold,
                        )
                      }
                    }
                  }
                  Text(
                    text =
                      if (isVisitor && originChurch.isNotBlank()) {
                        "Soti nan: $originChurch • ${rec.effectiveMemberCode}"
                      } else {
                        rec.effectiveMemberCode
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isVisitor) BlueNeon.copy(alpha = 0.9f) else MutedGlassText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(50))
                    .background(
                      (if (isOnTime) SuccessGreen else BrandOrange).copy(alpha = 0.25f)
                    )
                    .border(
                      1.dp,
                      if (isOnTime) SuccessGreen else BrandOrange,
                      RoundedCornerShape(50),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text(
                  text =
                    if (isOnTime) stringResource(R.string.attendance_status_on_time)
                    else stringResource(R.string.attendance_status_late),
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          if (showAbsentList) {
            items(absentMembers, key = { "abs_${it.id}" }) { mem ->
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x4D2A0D18))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  MemberAvatar3D(fullName = mem.effectiveFullName, size = 38.dp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = mem.effectiveFullName,
                      style = MaterialTheme.typography.titleSmall,
                      color = PureWhite,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = mem.effectiveMatrikil,
                      style = MaterialTheme.typography.bodySmall,
                      color = MutedGlassText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(50))
                      .background(ErrorRed.copy(alpha = 0.25f))
                      .border(1.dp, ErrorRed, RoundedCornerShape(50))
                      .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "Absan",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Create Service Modern Bottom Sheet (Anti-slop, clean linear flow)
  if (showCreateDialog) {
    CreateServiceBottomSheet(
      onDismissRequest = { showCreateDialog = false },
      churchMembers = churchMembers,
      onSubmit = {
        sTitle,
        sCategory,
        sDate,
        sStartTime,
        sEndTime,
        sTheme,
        sPassage,
        sLeader,
        sPreacher,
        sReader,
        sMusicians,
        sUshers,
        sIsOpen,
        sSendReminder,
        sTol,
        sImageUrl ->
        val timeSpan =
          when {
            sStartTime.isNotBlank() && sEndTime.isNotBlank() -> "$sStartTime - $sEndTime"
            sStartTime.isNotBlank() -> sStartTime
            else -> ""
          }
        val naturalDate = formatCreoleDateWithDay(sDate)
        val sDateFormatted =
          if (timeSpan.isNotBlank()) "$naturalDate • $timeSpan" else naturalDate
        onCreateService(
          sTitle,
          sTheme,
          sDateFormatted,
          sTol,
          sIsOpen,
          true,
          sCategory,
          sDate,
          sStartTime,
          sEndTime,
          sTheme,
          sPassage,
          sLeader,
          sPreacher,
          sReader,
          sMusicians,
          sUshers,
          sSendReminder,
          sImageUrl,
        )
      },
    )
  }

  if (editingService != null) {
    val targetService = editingService!!
    CreateServiceBottomSheet(
      onDismissRequest = { editingService = null },
      churchMembers = churchMembers,
      initialService = targetService,
      onSubmit = {
        sTitle,
        sCategory,
        sDate,
        sStartTime,
        sEndTime,
        sTheme,
        sPassage,
        sLeader,
        sPreacher,
        sReader,
        sMusicians,
        sUshers,
        sIsOpen,
        sSendReminder,
        sTol,
        sImageUrl ->
        val timeSpan =
          when {
            sStartTime.isNotBlank() && sEndTime.isNotBlank() -> "$sStartTime - $sEndTime"
            sStartTime.isNotBlank() -> sStartTime
            else -> ""
          }
        val naturalDate = formatCreoleDateWithDay(sDate)
        val sDateFormatted =
          if (timeSpan.isNotBlank()) "$naturalDate • $timeSpan" else naturalDate
        onUpdateService(
          targetService,
          sTitle,
          sTheme,
          sDateFormatted,
          sTol,
          sIsOpen,
          sCategory,
          sDate,
          sStartTime,
          sEndTime,
          sTheme,
          sPassage,
          sLeader,
          sPreacher,
          sReader,
          sMusicians,
          sUshers,
          sSendReminder,
          sImageUrl,
        )
      },
    )
  }
}

@Composable
private fun ServiceAttendanceKpiCell(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
  subValue: String? = null,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = color,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MutedGlassText,
      maxLines = 1,
    )
    if (!subValue.isNullOrBlank()) {
      Text(
        text = subValue,
        style = MaterialTheme.typography.labelSmall,
        color = BlueNeon,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun NotificationsScreen(
  notificationsState: UiState<List<ChurchNotification>>,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: (List<ChurchNotification>) -> Unit,
  onBackClick: () -> Unit,
  isAdmin: Boolean = false,
  canPublishAnnouncement: Boolean = isAdmin,
  canEditAnnouncement: Boolean = canPublishAnnouncement,
  availableServices: List<ChurchService> = emptyList(),
  onCreateAnnouncement:
    ((
      title: String,
      content: String,
      department: String,
      isPinned: Boolean,
      posterUrl: String?,
    ) -> Unit)? =
    null,
  onUpdateAnnouncement:
    ((
      existingAnnouncement: Announcement,
      title: String,
      content: String,
      department: String,
      category: String,
      date: String,
      targetAudience: String,
      isPinned: Boolean,
      showToastOnLogin: Boolean,
      toastAlertLevel: String,
      isScheduledNotification: Boolean,
      scheduledNotificationDate: String?,
      posterUrl: String?,
      serviceId: String?,
    ) -> Unit)? =
    null,
  announcementsState: UiState<List<Announcement>> = UiState.Success(emptyList()),
  unreadAnnouncementIds: Set<String> = emptySet(),
  onMarkAnnouncementRead: (announcementId: String) -> Unit = {},
  reactionSummaries: Map<String, TargetReactionSummary> = emptyMap(),
  onReactToAnnouncement: (announcementId: String, reactionType: String) -> Unit = { _, _ -> },
  observeComments: ((targetId: String) -> Flow<List<AnnouncementComment>>)? = null,
  onPostComment: (targetId: String, content: String) -> Unit = { _, _ -> },
  onReactToComment: (commentId: String, reactionType: String) -> Unit = { _, _ -> },
  onDeleteAnnouncement: (announcementId: String) -> Unit = {},
  isChromeVisible: Boolean = true,
  onChromeVisibilityChange: (Boolean) -> Unit = {},
  adMobState: EffectiveMobileAdMobState = EffectiveMobileAdMobState(),
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  var showCreateAnnouncementDialog by remember { mutableStateOf(false) }
  var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
  var activeCommentTargetId by remember { mutableStateOf<String?>(null) }

  val listState = rememberLazyListState()
  val feedScrollConnection = rememberFeedChromeNestedScrollConnection(
    listState = listState,
    onChromeVisibilityChange = onChromeVisibilityChange,
  )
  val density = LocalDensity.current
  var topBarHeightPx by remember { mutableIntStateOf(0) }
  val topBarHeightDp = with(density) {
    if (topBarHeightPx > 0) topBarHeightPx.toDp() else 72.dp
  }
  val isAtTop by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
    }
  }
  val topBarBackdropAlpha by animateFloatAsState(
    targetValue = if (isAtTop) 0f else 0.94f,
    animationSpec = tween(durationMillis = 160),
    label = "notifications_topbar_backdrop",
  )

  val isLoading = announcementsState is UiState.Loading
  val announcementsList = (announcementsState as? UiState.Success)?.data ?: emptyList()

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize().nestedScroll(feedScrollConnection),
      contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = topBarHeightDp + 6.dp, bottom = 110.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      if (adMobState.shouldShowBanner(OmniAdPlacement.ANNOUNCEMENTS)) {
        item(key = "announcements_admob_banner") {
          OmniAdMobBanner(
            placement = OmniAdPlacement.ANNOUNCEMENTS,
            adMobState = adMobState,
          )
        }
      }

      if (isLoading) {
        item { GlassLoadingIndicator() }
      } else if (announcementsList.isEmpty()) {
        item {
          PremiumEmptyState(
            title = stringResource(R.string.notifications_empty_title),
            subtitle = stringResource(R.string.notifications_empty_subtitle),
            icon = Icons.Default.Campaign,
            actionLabel =
              if (canPublishAnnouncement && onCreateAnnouncement != null)
                stringResource(R.string.feed_add_announcement)
              else null,
            onActionClick =
              if (canPublishAnnouncement && onCreateAnnouncement != null)
                ({ showCreateAnnouncementDialog = true })
              else null,
          )
        }
      } else {
        // Unified self-sufficient 3D Church Announcements with unread sign, timestamp, and click-to-read
        items(announcementsList, key = { "ann_${it.id}" }) { announcement ->
          val isUnread = !isAdmin && (announcement.id in unreadAnnouncementIds)
          PremiumAnnouncementCard(
            announcement = announcement,
            reactionSummary = reactionSummaries[announcement.id],
            onReactClick = {
              onMarkAnnouncementRead(announcement.id)
              onReactToAnnouncement(announcement.id, "pray")
            },
            onSelectReaction = { reactionType ->
              onMarkAnnouncementRead(announcement.id)
              onReactToAnnouncement(announcement.id, reactionType.key)
            },
            onCommentClick = {
              onMarkAnnouncementRead(announcement.id)
              activeCommentTargetId = announcement.id
            },
            isAdmin = isAdmin,
            canEdit = canEditAnnouncement && onUpdateAnnouncement != null,
            isUnread = isUnread,
            onCardClick = { onMarkAnnouncementRead(announcement.id) },
            onEditClick =
              if (canEditAnnouncement && onUpdateAnnouncement != null) {
                { editingAnnouncement = announcement }
              } else null,
            onDeleteClick =
              if (isAdmin) {
                { onDeleteAnnouncement(announcement.id) }
              } else null,
          )
        }
      }
    }

    AnimatedVisibility(
      visible = isChromeVisible,
      enter = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = tween(durationMillis = 180),
      ) + fadeIn(animationSpec = tween(durationMillis = 160)),
      exit = slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(durationMillis = 180),
      ) + fadeOut(animationSpec = tween(durationMillis = 140)),
      modifier = Modifier.align(Alignment.TopCenter),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF061124).copy(alpha = topBarBackdropAlpha))
          .onSizeChanged { size ->
            if (size.height > 0) topBarHeightPx = size.height
          }
          .statusBarsPadding(),
      ) {
        GlassTopBar(
          title = stringResource(R.string.notifications_screen_title),
          onBackClick = onBackClick,
          trailingContent =
            if (canPublishAnnouncement && onCreateAnnouncement != null) {
              {
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(50))
                      .background(ElectricBlue.copy(alpha = 0.28f))
                      .border(1.dp, BlueNeon.copy(alpha = 0.65f), RoundedCornerShape(50))
                      .clickable { showCreateAnnouncementDialog = true }
                      .padding(horizontal = 12.dp, vertical = 6.dp)
                      .testTag("notifications_create_announcement_btn"),
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Add,
                      contentDescription = stringResource(R.string.feed_add_announcement),
                      tint = BlueNeon,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = stringResource(R.string.feed_add_announcement),
                      style = MaterialTheme.typography.labelMedium,
                      color = PureWhite,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            } else null,
        )
      }
    }
  }

  // Comments Modal Bottom Sheet for Announcement cards
  if (activeCommentTargetId != null && observeComments != null) {
    val targetId = activeCommentTargetId!!
    val comments by
      observeComments(targetId).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentInput by remember { mutableStateOf("") }
    var activeReactionCommentId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    GlassModalBottomSheet(
      onDismissRequest = { activeCommentTargetId = null },
      title = stringResource(R.string.feed_comments_title, comments.size),
    ) {
      if (comments.isEmpty()) {
        Text(
          text = stringResource(R.string.feed_no_comments),
          style = MaterialTheme.typography.bodyMedium,
          color = MutedGlassText,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(comments, key = { it.id }) { comment ->
            val commentReactionSummary = reactionSummaries[comment.id]
            val isSelectorOpen = activeReactionCommentId == comment.id

            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x660B1D3D))
                  .border(0.8.dp, GlassWhite.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                  .padding(10.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.Top,
                  modifier = Modifier.weight(1f),
                ) {
                  MemberAvatar3D(
                    fullName = comment.userName,
                    size = 34.dp,
                    photoUrl = comment.userPhotoUrl,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = comment.userName,
                      style = MaterialTheme.typography.labelLarge,
                      color = BrandGold,
                    )
                    Text(
                      text = comment.content,
                      style = MaterialTheme.typography.bodyMedium,
                      color = PureWhite,
                    )
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Stacked3DReactionCluster(
                  reactionSummary = commentReactionSummary,
                  fallbackCount = (commentReactionSummary?.totalCount ?: 0).toLong(),
                  isCompact = true,
                  onTriggerClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeReactionCommentId =
                      if (isSelectorOpen) null else comment.id
                  },
                  testTag = "comment_react_btn_${comment.id}",
                )
              }

              if (isSelectorOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                Floating3DReactionSelectorBar(
                  visible = true,
                  selectedReaction = commentReactionSummary?.currentUserReaction,
                  onSelectReaction = { chosenReaction ->
                    onReactToComment(comment.id, chosenReaction.key)
                    activeReactionCommentId = null
                  },
                  onDismiss = { activeReactionCommentId = null },
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GlassInputField(
          value = commentInput,
          onValueChange = { commentInput = it },
          label = stringResource(R.string.feed_comment_hint),
          modifier = Modifier.weight(1f),
          testTag = "comment_input_field",
        )
        IconButton(
          onClick = {
            if (commentInput.isNotBlank()) {
              onPostComment(targetId, commentInput)
              commentInput = ""
            }
          },
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(ElectricBlue)
              .testTag("comment_send_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(R.string.feed_comment_send),
            tint = PureWhite,
          )
        }
      }
    }
  }

  if (showCreateAnnouncementDialog && onCreateAnnouncement != null) {
    CreateAnnouncementBottomSheet(
      onDismissRequest = { showCreateAnnouncementDialog = false },
      availableServices = availableServices,
      onSubmit = { aTitle, aContent, aDept, _, _, _, aPinned, _, _, _, _, aPosterUrl, _ ->
        onCreateAnnouncement(aTitle, aContent, aDept, aPinned, aPosterUrl)
      },
    )
  }

  if (editingAnnouncement != null && onUpdateAnnouncement != null) {
    val targetAnnouncement = editingAnnouncement!!
    CreateAnnouncementBottomSheet(
      onDismissRequest = { editingAnnouncement = null },
      availableServices = availableServices,
      initialAnnouncement = targetAnnouncement,
      onSubmit = {
        aTitle,
        aContent,
        aDept,
        aCategory,
        aDate,
        aAudience,
        aPinned,
        aShowToast,
        aAlertLevel,
        aScheduled,
        aScheduledDate,
        aPosterUrl,
        aServiceId ->
        onUpdateAnnouncement(
          targetAnnouncement,
          aTitle,
          aContent,
          aDept,
          aCategory,
          aDate,
          aAudience,
          aPinned,
          aShowToast,
          aAlertLevel,
          aScheduled,
          aScheduledDate,
          aPosterUrl,
          aServiceId,
        )
      },
    )
  }
}

@Composable
fun ProfileAndSettingsScreen(
  userProfile: UserProfile?,
  member: Member?,
  currentLanguageCode: String,
  onLanguageChange: (String) -> Unit,
  onSaveMemberChanges: (fullName: String, department: String, role: String) -> Unit,
  onSignOut: () -> Unit,
  onBackClick: () -> Unit,
  biometricEnabled: Boolean = false,
  onToggleBiometric: (Boolean) -> Unit = {},
  onLinkGoogleAccount: (GoogleAccountPayload) -> Unit = {},
  onUnlinkGoogleAccount: () -> Unit = {},
  onUpdateMemberPhoto: ((String) -> Unit)? = null,
  onRegisterNewMember:
    ((
      fullName: String,
      customMatrikil: String?,
      pin: String,
      phone: String?,
      department: String,
      ministryRole: String,
    ) -> Unit)? =
    null,
  churchMembers: List<Member> = emptyList(),
  onAdminUpdateMember:
    ((
      memberDocId: String,
      fullName: String,
      department: String,
      ministryRole: String,
      phone: String?,
      titlePrefix: String?,
    ) -> Unit)? =
    null,
  activeChurch: com.zoutiw.omnichurch.data.models.Church? = null,
  isAdminSecurityBusy: Boolean = false,
  onSaveChurchSecuritySettings:
    ((quickPinEnabled: Boolean, newPinOrNull: String?, autoLockMinutes: Int) -> Unit)? =
    null,
  onLockAdminNow: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val activityContext = remember(context) { context.findActivity() ?: context }
  val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
  val scope = rememberCoroutineScope()

  val fullName =
    remember(member?.fullName, userProfile?.displayName) {
      member?.fullName ?: userProfile?.displayName ?: ""
    }
  var isLinkingGoogle by remember { mutableStateOf(false) }
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
            onUpdateMemberPhoto?.invoke(result.getOrThrow())
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
          onUpdateMemberPhoto?.invoke(base64)
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
            "Otorizasyon kamera refize. Ou ka toujou chwazi yon foto nan Galri a.",
            Toast.LENGTH_LONG,
          )
          .show()
      }
    }

  val isAdmin = userProfile?.isAdminOrSuperAdmin == true
  val churchNameDisplay =
    member?.churchName?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchName?.takeIf { it.isNotBlank() }
      ?: "OmniChurch Pro"

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 110.dp),
  ) {
    GlassTopBar(
      title = stringResource(R.string.profile_screen_title),
      onBackClick = onBackClick,
    )

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Member Summary Card
      PremiumGlassCard(
        cornerRadius = 22.dp,
        isHighlighted = true,
        contentPadding = PaddingValues(18.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.BottomEnd,
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable(
                  enabled = onUpdateMemberPhoto != null,
                  onClick = { showPhotoSourceDialog = true },
                )
                .testTag("profile_avatar_box"),
          ) {
            MemberAvatar3D(
              fullName = fullName,
              size = 68.dp,
              photoUrl = member?.effectivePhotoUrl ?: userProfile?.photoUrl,
              showVerifiedBadge = true,
            )
            if (onUpdateMemberPhoto != null) {
              Box(
                modifier =
                  Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue)
                    .border(1.5.dp, PureWhite, CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.CameraAlt,
                  contentDescription = "Chanje Foto Pwofil",
                  tint = PureWhite,
                  modifier = Modifier.size(13.dp),
                )
              }
            }
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = fullName.ifBlank { "Manm OmniChurch" },
              style = MaterialTheme.typography.headlineMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            val secondaryInfo =
              member?.googleEmail?.takeIf { it.isNotBlank() }
                ?: userProfile?.email?.takeIf { it.isNotBlank() }
                ?: member?.phone?.takeIf { it.isNotBlank() }
                ?: "Sesyon ID Matrikil + PIN"
            Text(
              text = secondaryInfo,
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite.copy(alpha = 0.85f),
            )
            if (member != null) {
              val matrikil = member.effectiveMatrikil
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier.clickable {
                    val clipboard =
                      context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                      ClipData.newPlainText("OmniChurch Matrikil", matrikil)
                    )
                    Toast.makeText(context, "ID Manm: $matrikil", Toast.LENGTH_SHORT)
                      .show()
                  },
              ) {
                Text(
                  text = "$churchNameDisplay • ID: $matrikil",
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandGold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = null,
                  tint = BrandGold,
                  modifier = Modifier.size(14.dp),
                )
              }
            }
          }
        }

        if (isCompressingPhoto) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x3300E5FF))
                .padding(horizontal = 12.dp, vertical = 8.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = ElectricBlue,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Mete ajou foto pwofil la...",
              style = MaterialTheme.typography.bodySmall,
              color = PureWhite,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GlassButton(
          text = "Pataje Aplikasyon OmniChurch la",
          icon = Icons.Default.Share,
          isPrimaryNeon = false,
          onClick = { shareOmniChurchGeneralApp(context, churchNameDisplay) },
          modifier = Modifier.fillMaxWidth(),
          testTag = "profile_share_app_btn",
        )
      }

      // 1b. Optional Google Account Linking & Biometric Quick Unlock (Section 1.B)
      if (member != null) {
        val matrikil = member.effectiveMatrikil
        PremiumGlassCard(
          cornerRadius = 22.dp,
          contentPadding = PaddingValues(16.dp),
        ) {
          Text(
            text = "Sekirite & Kont Lye (Opsyonèl)",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(10.dp))

          if (member.googleEmail.isNullOrBlank()) {
            GlassButton(
              text = "Lye yon kont Google (Opsyonèl)",
              subtitle = "Konekte ak Google sou nenpòt telefòn",
              icon = Icons.Default.Link,
              isPrimaryNeon = false,
              isLoading = isLinkingGoogle,
              onClick = {
                isLinkingGoogle = true
                launchOptionalGoogleSignIn(
                  context = activityContext,
                  credentialManager = credentialManager,
                  scope = scope,
                  onSuccess = { payload ->
                    isLinkingGoogle = false
                    onLinkGoogleAccount(payload)
                  },
                  onError = { err ->
                    isLinkingGoogle = false
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                  },
                )
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "profile_link_google_optional_btn",
            )
          } else {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x3300D878))
                  .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                  .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Kont Google lye: ${member.googleEmail}",
                  style = MaterialTheme.typography.bodySmall,
                  color = PureWhite,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              TextButton(
                onClick = onUnlinkGoogleAccount,
                modifier = Modifier.testTag("profile_unlink_google_btn"),
              ) {
                Text(
                  text = "Delye",
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandGold,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = BrandGold,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Deblokaj Biyometrik (Anprent / Face ID)",
                  style = MaterialTheme.typography.titleSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Debloke sesyon ID $matrikil san retape PIN",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedGlassText,
                )
              }
            }
            Switch(
              checked = biometricEnabled,
              onCheckedChange = { wantEnabled ->
                if (wantEnabled && BiometricQuickUnlockHelper.isBiometricAvailable(context)) {
                  BiometricQuickUnlockHelper.promptBiometricUnlock(
                    context = activityContext,
                    title = "Aktive Anprent / Face ID",
                    subtitle = "Konfime idantite w pou lye ak ID $matrikil",
                    onSuccess = { onToggleBiometric(true) },
                    onError = { err ->
                      Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    },
                  )
                } else {
                  onToggleBiometric(wantEnabled)
                }
              },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = PureWhite,
                  checkedTrackColor = SuccessGreen,
                ),
              modifier = Modifier.testTag("profile_biometric_toggle_switch"),
            )
          }
        }
      }

      if (isAdmin && activeChurch != null && onSaveChurchSecuritySettings != null && onLockAdminNow != null) {
        com.zoutiw.omnichurch.presentation.screens.auth.ChurchAdminSecuritySettingsCard(
          church = activeChurch,
          isSaving = isAdminSecurityBusy,
          onSaveSettings = onSaveChurchSecuritySettings,
          onLockAdminNow = onLockAdminNow,
        )
      }

      // 2. Multilingual Language Switcher (Kreyòl Ayisyen, Français, English, Español)
      PremiumGlassCard(
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = BlueNeon,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.profile_language_label),
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
        Spacer(modifier = Modifier.height(12.dp))

        val languages =
          listOf(
            "ht" to stringResource(R.string.profile_lang_ht),
            "fr" to stringResource(R.string.profile_lang_fr),
            "en" to stringResource(R.string.profile_lang_en),
            "es" to stringResource(R.string.profile_lang_es),
          )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          languages.forEach { (code, name) ->
            val isSelected = currentLanguageCode == code
            Box(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(
                    if (isSelected) ElectricBlue else Color(0x660B1D3D)
                  )
                  .border(
                    1.dp,
                    if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.25f),
                    RoundedCornerShape(14.dp),
                  )
                  .clickable { onLanguageChange(code) }
                  .padding(vertical = 10.dp, horizontal = 4.dp)
                  .testTag("lang_option_$code"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = name.substringBefore(" "),
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
              )
            }
          }
        }
      }

      // 3. Member Personal Info: Display member details & church admin contact guidance
      if (member != null) {
        PremiumGlassCard(
          cornerRadius = 22.dp,
          contentPadding = PaddingValues(16.dp),
          modifier = Modifier.testTag("profile_member_personal_info_card"),
        ) {
          Text(
            text = "Enfòmasyon Pèsonèl Manm nan",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x44091936))
                .border(1.dp, ElectricBlue.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .testTag("profile_contact_admin_notice"),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              tint = ElectricBlue,
              modifier = Modifier.size(18.dp).padding(top = 1.dp),
            )
            Text(
              text =
                "Ou gen dwa klike sou foto pwofil ou anlè a pou chanje l nenpòt kilè. Pou nenpòt lòt done pèsonèl ou (Non, Matrikil, Depatman, Wòl), se administrasyon legliz la sèlman ki gen otorizasyon pou modifye yo.",
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite.copy(alpha = 0.92f),
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          ReadOnlyMemberFieldRow(
            label = stringResource(R.string.setup_member_fullname_label),
            value = member.effectiveFullName,
          )
          Spacer(modifier = Modifier.height(8.dp))
          ReadOnlyMemberFieldRow(
            label = "ID Matrikil Ofisyèl",
            value = member.effectiveMatrikil,
          )
          Spacer(modifier = Modifier.height(8.dp))
          ReadOnlyMemberFieldRow(
            label = "Legliz",
            value = churchNameDisplay,
          )
          Spacer(modifier = Modifier.height(8.dp))
          ReadOnlyMemberFieldRow(
            label = stringResource(R.string.setup_member_department_label),
            value = member.department.ifBlank { "Adorasyon" },
          )
          Spacer(modifier = Modifier.height(8.dp))
          ReadOnlyMemberFieldRow(
            label = stringResource(R.string.setup_member_role_label),
            value = member.role.ifBlank { "Manm" },
          )
          if (!member.phone.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            ReadOnlyMemberFieldRow(
              label = "Telefòn Kontak",
              value = member.phone,
            )
          }
        }
      }

      // 4. Sign Out ("Dekonekte")
      GlassButton(
        text = stringResource(R.string.auth_sign_out),
        icon = Icons.AutoMirrored.Filled.Logout,
        isPrimaryNeon = false,
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth(),
        testTag = "profile_sign_out_btn",
      )
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
            text = "Chwazi kijan ou vle pran oswa chwazi nouvo foto paspò w la pou mete sou pwofil ak kat idantite w.",
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
                ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.CAMERA,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
              if (hasCamPermission) {
                try {
                  takePictureLauncher.launch(null)
                } catch (e: Exception) {
                  Toast.makeText(context, "Erè kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
              } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
              }
            },
            modifier = Modifier.fillMaxWidth().height(68.dp),
            testTag = "profile_btn_take_photo_camera",
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
            testTag = "profile_btn_pick_photo_gallery",
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

@Composable
private fun ReadOnlyMemberFieldRow(
  label: String,
  value: String,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0x550A1836))
        .border(1.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MutedGlassText,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyLarge,
      color = PureWhite,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
fun ServiceDetailsScreen(
  serviceId: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
      Text(
        text = "Detay Sèvis",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "ID Sèvis: $serviceId",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
      )
      Spacer(modifier = Modifier.height(24.dp))
      Button(onClick = onBack) {
        Text("Retounen")
      }
    }
  }
}

