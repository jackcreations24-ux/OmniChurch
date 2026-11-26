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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.ads.EffectiveMobileAdMobState
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.presentation.components.AnnouncementFilterBar
import com.zoutiw.omnichurch.presentation.components.AnnouncementFilterMode
import com.zoutiw.omnichurch.presentation.components.AnnouncementReaderSheet
import com.zoutiw.omnichurch.presentation.components.AnnouncementStrings
import com.zoutiw.omnichurch.presentation.components.CreateAnnouncementBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.OmniAdMobBanner
import com.zoutiw.omnichurch.presentation.components.PremiumAnnouncementCard
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.rememberFeedChromeNestedScrollConnection
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.PureWhite
import kotlinx.coroutines.flow.Flow

@Composable
fun NotificationsScreen(
  notificationsState: UiState<List<ChurchNotification>>,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: (List<ChurchNotification>) -> Unit,
  onBackClick: () -> Unit,
  isAdmin: Boolean = false,
  isDeaconSession: Boolean = false,
  currentAuthUid: String = "",
  deaconAuthorLabel: String? = null,
  canPublishAnnouncement: Boolean = isAdmin || isDeaconSession,
  canEditAnnouncement: Boolean = canPublishAnnouncement,
  availableServices: List<ChurchService> = emptyList(),
  onCreateAnnouncement:
    ((
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
  onMarkAllAnnouncementsRead: () -> Unit = {},
  onRetryLoadAnnouncements: () -> Unit = {},
  currentLanguageCode: String = "ht",
  reactionSummaries: Map<String, TargetReactionSummary> = emptyMap(),
  commentCounts: Map<String, Int> = emptyMap(),
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
  val copy = remember(currentLanguageCode) {
    AnnouncementStrings.forLanguage(currentLanguageCode)
  }
  var showCreateAnnouncementDialog by remember { mutableStateOf(false) }
  var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
  var readingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
  var activeCommentTargetId by remember { mutableStateOf<String?>(null) }
  var selectedFilter by remember { mutableStateOf(AnnouncementFilterMode.ALL) }

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
  val loadErrorMessage = (announcementsState as? UiState.Error)?.message
  val rawAnnouncementsList = (announcementsState as? UiState.Success)?.data ?: emptyList()
  val visibleAnnouncements = remember(rawAnnouncementsList, isAdmin) {
    if (isAdmin) rawAnnouncementsList else rawAnnouncementsList.filter { it.isPublished }
  }
  val unreadCount = remember(visibleAnnouncements, unreadAnnouncementIds) {
    visibleAnnouncements.count { it.id in unreadAnnouncementIds }
  }
  val pinnedCount = remember(visibleAnnouncements) {
    visibleAnnouncements.count { it.isPinned }
  }
  val filteredAnnouncements = remember(visibleAnnouncements, selectedFilter, unreadAnnouncementIds) {
    when (selectedFilter) {
      AnnouncementFilterMode.ALL -> visibleAnnouncements
      AnnouncementFilterMode.UNREAD -> visibleAnnouncements.filter { it.id in unreadAnnouncementIds }
      AnnouncementFilterMode.PINNED -> visibleAnnouncements.filter { it.isPinned }
    }
  }

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

      if (!isLoading && loadErrorMessage == null && visibleAnnouncements.isNotEmpty()) {
        item(key = "announcements_filter_bar") {
          AnnouncementFilterBar(
            selectedFilter = selectedFilter,
            totalCount = visibleAnnouncements.size,
            unreadCount = unreadCount,
            pinnedCount = pinnedCount,
            languageCode = currentLanguageCode,
            onSelectFilter = { selectedFilter = it },
            onMarkAllRead = onMarkAllAnnouncementsRead,
          )
        }
      }

      if (isLoading) {
        item { GlassLoadingIndicator() }
      } else if (loadErrorMessage != null) {
        item {
          PremiumEmptyState(
            title = copy.loadErrorTitle,
            subtitle = loadErrorMessage,
            icon = Icons.Default.Campaign,
            actionLabel = copy.retryLabel,
            onActionClick = onRetryLoadAnnouncements,
          )
        }
      } else if (visibleAnnouncements.isEmpty()) {
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
      } else if (filteredAnnouncements.isEmpty()) {
        item {
          val isUnreadTab = selectedFilter == AnnouncementFilterMode.UNREAD
          PremiumEmptyState(
            title = if (isUnreadTab) copy.allReadEmptyTitle else copy.noPinnedEmptyTitle,
            subtitle = if (isUnreadTab) copy.allReadEmptySubtitle else copy.noPinnedEmptySubtitle,
            icon = Icons.Default.Campaign,
          )
        }
      } else {
        items(filteredAnnouncements, key = { "ann_${it.id}" }) { announcement ->
          val isUnread = announcement.id in unreadAnnouncementIds
          val canModifyThisAnnouncement =
            com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.canEditOrDeleteAnnouncement(
              announcement = announcement,
              isAdmin = isAdmin,
              isDeacon = isDeaconSession,
              currentAuthUid = currentAuthUid,
            )
          PremiumAnnouncementCard(
            announcement = announcement,
            reactionSummary = reactionSummaries[announcement.id],
            commentsCountOverride = commentCounts[announcement.id],
            languageCode = currentLanguageCode,
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
            canEdit = canModifyThisAnnouncement && canEditAnnouncement && onUpdateAnnouncement != null,
            canDelete = canModifyThisAnnouncement,
            isUnread = isUnread,
            onMarkRead = { onMarkAnnouncementRead(announcement.id) },
            onCardClick = {
              onMarkAnnouncementRead(announcement.id)
              readingAnnouncement = announcement
            },
            onEditClick =
              if (canModifyThisAnnouncement && canEditAnnouncement && onUpdateAnnouncement != null) {
                { editingAnnouncement = announcement }
              } else null,
            onDeleteClick =
              if (canModifyThisAnnouncement) {
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
          title = copy.screenTitle,
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

  readingAnnouncement?.let { currentReadingAnnouncement ->
    val liveAnnouncement =
      visibleAnnouncements.firstOrNull { it.id == currentReadingAnnouncement.id }
        ?: currentReadingAnnouncement
    AnnouncementReaderSheet(
      announcement = liveAnnouncement,
      languageCode = currentLanguageCode,
      reactionSummary = reactionSummaries[liveAnnouncement.id],
      commentsCountOverride = commentCounts[liveAnnouncement.id],
      onDismiss = { readingAnnouncement = null },
      onMarkRead = onMarkAnnouncementRead,
      onSelectReaction = { reactionType ->
        onMarkAnnouncementRead(liveAnnouncement.id)
        onReactToAnnouncement(liveAnnouncement.id, reactionType.key)
      },
      onOpenComments = {
        onMarkAnnouncementRead(liveAnnouncement.id)
        val targetId = liveAnnouncement.id
        readingAnnouncement = null
        activeCommentTargetId = targetId
      },
    )
  }

  if (observeComments != null) {
    activeCommentTargetId?.let { targetId ->
      FeedCommentsBottomSheet(
        targetId = targetId,
        observeComments = observeComments,
        reactionSummaries = reactionSummaries,
        onPostComment = onPostComment,
        onReactToComment = onReactToComment,
        onDismiss = { activeCommentTargetId = null },
        testTagPrefix = "comment",
      )
    }
  }

  if (showCreateAnnouncementDialog && onCreateAnnouncement != null) {
    CreateAnnouncementBottomSheet(
      onDismissRequest = { showCreateAnnouncementDialog = false },
      availableServices = availableServices,
      currentLanguageCode = currentLanguageCode,
      isDeaconMode = isDeaconSession && !isAdmin,
      lockedDeaconDepartment = com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.DEFAULT_DEACON_DEPARTMENT,
      lockedDeaconAuthorLabel = deaconAuthorLabel,
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
        onCreateAnnouncement(
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

  if (onUpdateAnnouncement != null) {
    editingAnnouncement?.let { targetAnnouncement ->
      CreateAnnouncementBottomSheet(
        onDismissRequest = { editingAnnouncement = null },
        availableServices = availableServices,
        initialAnnouncement = targetAnnouncement,
        currentLanguageCode = currentLanguageCode,
        isDeaconMode = isDeaconSession && !isAdmin,
        lockedDeaconDepartment = com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.DEFAULT_DEACON_DEPARTMENT,
        lockedDeaconAuthorLabel = deaconAuthorLabel,
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
}
