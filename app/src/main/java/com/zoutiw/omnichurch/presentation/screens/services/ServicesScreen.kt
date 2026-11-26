package com.zoutiw.omnichurch.presentation.screens.services

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.ads.EffectiveMobileAdMobState
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.formatCreoleDateWithDay
import com.zoutiw.omnichurch.presentation.components.CreateServiceBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.OmniAdMobBanner
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumServiceCard
import com.zoutiw.omnichurch.presentation.components.rememberFeedChromeNestedScrollConnection
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

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
  isDeaconSession: Boolean = false,
  canScanAttendance: Boolean = isAdminSession || userProfile?.isAdminOrSuperAdmin == true,
  currentAuthUid: String = "",
  canCreateService: Boolean = isAdminSession || isDeaconSession || userProfile?.isAdminOrSuperAdmin == true,
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
  commentCounts: Map<String, Int> = emptyMap(),
  onReactToService: (serviceId: String, reactionType: String) -> Unit = { _, _ -> },
  observeComments: (targetId: String) -> Flow<List<AnnouncementComment>> = { emptyFlow() },
  onPostComment: (serviceId: String, content: String) -> Unit = { _, _ -> },
  onReactToComment: (commentId: String, reactionType: String) -> Unit = { _, _ -> },
  churchMembers: List<Member> = emptyList(),
  activeChurchName: String? = null,
  activeChurchLogoUrl: String? = null,
  currentLanguageCode: String = "ht",
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
            items(
              items = list,
              key = { it.id },
              contentType = { "service_card" },
            ) { service ->
              val canScanThisService =
                canScanAttendance ||
                  (service.id in temporaryAuthorizedServiceIds && !service.isServiceEnded())
              val canDeleteThisService =
                com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.canDeleteService(
                  service = service,
                  isAdmin = isAdmin,
                  isDeacon = isDeaconSession,
                  currentAuthUid = currentAuthUid,
                )
              PremiumServiceCard(
                service = service,
                reactionSummary = reactionSummaries[service.id],
                commentsCountOverride = commentCounts[service.id],
                onReactClick = { onReactToService(service.id, "pray") },
                onSelectReaction = { reactionType ->
                  onReactToService(service.id, reactionType.key)
                },
                onCommentClick = { activeCommentServiceId = service.id },
                isAdmin = isAdmin,
                canEdit = canEditService,
                canDelete = canDeleteThisService,
                isScanner = canScanThisService,
                churchMembers = churchMembers,
                churchName = activeChurchName,
                churchLogoUrl = activeChurchLogoUrl,
                languageCode = currentLanguageCode,
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
                  if (canDeleteThisService) {
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
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
      ) + fadeIn(animationSpec = tween(durationMillis = 120)),
      exit = slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
      ) + fadeOut(animationSpec = tween(durationMillis = 110)),
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

  activeCommentServiceId?.let { targetServiceId ->
    FeedCommentsBottomSheet(
      targetId = targetServiceId,
      observeComments = observeComments,
      reactionSummaries = reactionSummaries,
      onPostComment = onPostComment,
      onReactToComment = onReactToComment,
      onDismiss = { activeCommentServiceId = null },
      testTagPrefix = "service_comment",
    )
  }

  selectedAttendanceService?.let { srv ->
    ServiceAttendanceBottomSheet(
      service = srv,
      churchMembers = churchMembers,
      observeAttendances = observeAttendances,
      onDismiss = { selectedAttendanceService = null },
    )
  }

  if (showCreateDialog) {
    CreateServiceBottomSheet(
      onDismissRequest = { showCreateDialog = false },
      churchMembers = churchMembers,
      currentLanguageCode = currentLanguageCode,
      onSubmit = { sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol, sImageUrl ->
        val sDateFormatted = formatServiceSheetScheduleLabel(sDate, sStartTime, sEndTime, currentLanguageCode)
        onCreateService(
          sTitle, sTheme, sDateFormatted, sTol, sIsOpen, true, sCategory, sDate, sStartTime, sEndTime,
          sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sSendReminder, sImageUrl,
        )
      },
    )
  }

  editingService?.let { targetService ->
    CreateServiceBottomSheet(
      onDismissRequest = { editingService = null },
      churchMembers = churchMembers,
      initialService = targetService,
      currentLanguageCode = currentLanguageCode,
      onSubmit = { sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol, sImageUrl ->
        val sDateFormatted = formatServiceSheetScheduleLabel(sDate, sStartTime, sEndTime, currentLanguageCode)
        onUpdateService(
          targetService, sTitle, sTheme, sDateFormatted, sTol, sIsOpen, sCategory, sDate, sStartTime, sEndTime,
          sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sSendReminder, sImageUrl,
        )
      },
    )
  }
}

private fun formatServiceSheetScheduleLabel(
  sDate: String,
  sStartTime: String,
  sEndTime: String,
  currentLanguageCode: String,
): String {
  val startFormatted = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(sStartTime)
  val endFormatted = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(sEndTime)
  val timeSpan =
    when {
      startFormatted.isNotBlank() && endFormatted.isNotBlank() -> "$startFormatted - $endFormatted"
      startFormatted.isNotBlank() -> startFormatted
      else -> ""
    }
  val naturalDate =
    com.zoutiw.omnichurch.data.models.formatLocalizedServiceDate(sDate, currentLanguageCode)
      .ifBlank { formatCreoleDateWithDay(sDate) }
  return if (timeSpan.isNotBlank()) "$naturalDate • $timeSpan" else naturalDate
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
      modifier = Modifier.fillMaxWidth().padding(16.dp),
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
