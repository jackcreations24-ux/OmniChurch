package com.zoutiw.omnichurch.presentation.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.ads.EffectiveMobileAdMobState
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.utilities.OmniLocalization
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.CreateServiceBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.Neumorphic3DActionButton
import com.zoutiw.omnichurch.presentation.components.OmniAdMobBanner
import com.zoutiw.omnichurch.presentation.components.OmniChurchSanctuaryHeroCanvas
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumSectionHeader
import com.zoutiw.omnichurch.presentation.components.PremiumServiceCard
import com.zoutiw.omnichurch.presentation.components.rememberFeedChromeNestedScrollConnection
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun MemberHomeScreen(
  userProfile: UserProfile?,
  member: Member?,
  servicesState: UiState<List<ChurchService>>,
  announcementsState: UiState<List<Announcement>>,
  unreadNotificationsCount: Int,
  onOpenMemberCard: () -> Unit,
  onOpenScannerOrGallery: () -> Unit,
  onOpenServices: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenMemberList: () -> Unit = {},
  onReactToItem: (targetId: String, isService: Boolean, reactionType: String) -> Unit,
  observeComments: (targetId: String) -> Flow<List<AnnouncementComment>>,
  onPostComment: (targetId: String, isService: Boolean, content: String) -> Unit,
  onCreateAnnouncement:
    (
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
    ) -> Unit,
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
  modifier: Modifier = Modifier,
  reactionSummaries: Map<String, TargetReactionSummary> = emptyMap(),
  commentCounts: Map<String, Int> = emptyMap(),
  onReactToComment: (commentId: String, reactionType: String) -> Unit = { _, _ -> },
  onDeleteService: (serviceId: String) -> Unit = {},
  onDeleteAnnouncement: (announcementId: String) -> Unit = {},
  isAdminSession: Boolean = false,
  isDeaconSession: Boolean = false,
  currentAuthUid: String = "",
  headerPhotoUrl: String? = null,
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
  churchMembers: List<Member> = emptyList(),
  activeChurchName: String? = null,
  activeChurchLogoUrl: String? = null,
  currentLanguageCode: String = "ht",
  isChromeVisible: Boolean = true,
  onChromeVisibilityChange: (Boolean) -> Unit = {},
  adMobState: EffectiveMobileAdMobState = EffectiveMobileAdMobState(),
  donationConfig: DeveloperDonationConfig = DeveloperDonationConfig(),
  onOpenDonate: () -> Unit = {},
) {
  val displayName =
    remember(member?.effectiveFullName, userProfile?.displayName, currentLanguageCode) {
      member?.effectiveFullName?.takeIf { it.isNotBlank() }
        ?: userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: OmniLocalization.defaultMemberName(currentLanguageCode)
    }
  val memberPhotoUrl =
    remember(headerPhotoUrl, member?.effectivePhotoUrl, userProfile?.photoUrl) {
      headerPhotoUrl ?: (member?.effectivePhotoUrl ?: userProfile?.photoUrl)
    }
  val isAdmin = isAdminSession || userProfile?.isAdminOrSuperAdmin == true
  val spaceSubtitle =
    remember(currentLanguageCode, isAdmin) {
      OmniLocalization.homeSpaceSubtitle(currentLanguageCode, isAdmin)
    }
  val churchDisplayName =
    remember(activeChurchName, member?.churchName, userProfile?.churchName) {
      activeChurchName?.takeIf { it.isNotBlank() }
        ?: member?.churchName?.takeIf { it.isNotBlank() }
        ?: userProfile?.churchName?.takeIf { it.isNotBlank() }
    }

  var activeCommentTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
  var showCreateServiceDialog by remember { mutableStateOf(false) }
  var editingService by remember { mutableStateOf<ChurchService?>(null) }

  var currentEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val allServices = (servicesState as? UiState.Success)?.data ?: emptyList()
  val isLoadingFeed = servicesState is UiState.Loading
  val servicesList =
    remember(allServices, currentEpochMillis) {
      allServices.filter { !it.isServiceEnded(currentEpochMillis) }
    }
  LaunchedEffect(allServices) {
    currentEpochMillis = System.currentTimeMillis()
    if (allServices.isNotEmpty()) {
      while (true) {
        delay(15_000L)
        currentEpochMillis = System.currentTimeMillis()
      }
    }
  }

  val listState = rememberLazyListState()
  val feedScrollConnection = rememberFeedChromeNestedScrollConnection(
    listState = listState,
    onChromeVisibilityChange = onChromeVisibilityChange,
  )
  val density = LocalDensity.current
  var headerHeightPx by remember { mutableIntStateOf(0) }
  val headerHeightDp = with(density) {
    if (headerHeightPx > 0) headerHeightPx.toDp() else 124.dp
  }
  val isAtTop by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
    }
  }
  val headerBackdropAlpha by animateFloatAsState(
    targetValue = if (isAtTop) 0f else 0.94f,
    animationSpec = tween(durationMillis = 160),
    label = "home_header_backdrop",
  )

  Box(
    modifier = modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
  ) {
    // Flat full-bleed 3D Sanctuary Hero Backdrop (identical to Auth Screen)
    OmniChurchSanctuaryHeroCanvas(
      modifier = Modifier.fillMaxWidth().height(340.dp).align(Alignment.TopCenter),
      showLeftTextScrim = true,
    )

    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .nestedScroll(feedScrollConnection),
      contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 110.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1–3. Top spacing for overlaid auto-hiding header + Church Hero Banner
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        ) {
          Spacer(modifier = Modifier.height(headerHeightDp + 12.dp))
          HeroSanctuaryBannerCard(churchName = churchDisplayName)
        }
      }

      // 4. Dedicated "Kat Manm" (or "Lis Manm" for Admin) Borderless 3D Neumorphic Action Button
      item {
        Neumorphic3DActionButton(
          text =
            if (isAdmin) OmniLocalization.memberListAction(currentLanguageCode)
            else OmniLocalization.memberCardAction(currentLanguageCode),
          icon = if (isAdmin) Icons.Default.People else Icons.Default.Badge,
          onClick = if (isAdmin) onOpenMemberList else onOpenMemberCard,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(68.dp),
          testTag = if (isAdmin) "quick_action_member_list" else "quick_action_member_card",
        )
      }

      // Remote-controlled AdMob Banner for Home Feed (renders 0.dp when disabled or Church is Pro Ad-Free)
      if (adMobState.shouldShowBanner(OmniAdPlacement.HOME)) {
        item(key = "home_admob_banner") {
          OmniAdMobBanner(
            placement = OmniAdPlacement.HOME,
            adMobState = adMobState,
          )
        }
      }

      // 5. "Fil Aktyalite" Section Header + Admin Create Service Chip + "Tout >"
      item {
        PremiumSectionHeader(
          title = stringResource(R.string.feed_section_title),
          actionText = stringResource(R.string.feed_see_all),
          onActionClick = onOpenServices,
          modifier = Modifier.padding(horizontal = 14.dp),
          trailingExtraContent =
            if (canCreateService) {
              {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.padding(end = 8.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(50))
                        .background(ElectricBlue.copy(alpha = 0.25f))
                        .border(1.dp, BlueNeon.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable { showCreateServiceDialog = true }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("home_add_service_chip"),
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.feed_add_service),
                        tint = BlueNeon,
                        modifier = Modifier.size(14.dp),
                      )
                      Spacer(modifier = Modifier.width(2.dp))
                      Text(
                        text = stringResource(R.string.feed_add_service),
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                      )
                    }
                  }
                }
              }
            } else null,
        )
      }

      // 6. Live Feed Items (Full-bleed 2.dp edge-to-edge active/upcoming Service Cards only — finished services live in Sèvis & Prezans)
      if (isLoadingFeed) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            GlassLoadingIndicator()
          }
        }
      } else if (servicesList.isEmpty()) {
        item {
          Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            PremiumEmptyState(
              title = stringResource(R.string.feed_empty_title),
              subtitle = stringResource(R.string.feed_empty_subtitle),
              icon = Icons.Default.Campaign,
              actionLabel =
                if (canCreateService) stringResource(R.string.feed_add_service) else null,
              onActionClick = if (canCreateService) ({ showCreateServiceDialog = true }) else null,
            )
          }
        }
      } else {
        items(
          items = servicesList,
          key = { "srv_${it.id}" },
          contentType = { "service_feed_card" },
        ) { service ->
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
            onReactClick = { onReactToItem(service.id, true, "pray") },
            onSelectReaction = { reactionType ->
              onReactToItem(service.id, true, reactionType.key)
            },
            onCommentClick = { activeCommentTarget = service.id to true },
            isAdmin = isAdmin,
            canEdit = canEditService,
            canDelete = canDeleteThisService,
            churchMembers = churchMembers,
            churchName = churchDisplayName,
            churchLogoUrl = activeChurchLogoUrl ?: member?.churchLogoUrl,
            languageCode = currentLanguageCode,
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

    // Floating Auto-Hide/Show Top Header (synchronized with Bottom Menu on feed scroll)
    MemberHomeTopHeader(
      isChromeVisible = isChromeVisible,
      headerBackdropAlpha = headerBackdropAlpha,
      displayName = displayName,
      spaceSubtitle = spaceSubtitle,
      donationConfig = donationConfig,
      currentLanguageCode = currentLanguageCode,
      memberPhotoUrl = memberPhotoUrl,
      onHeaderHeightMeasured = { headerHeightPx = it },
      onOpenProfile = onOpenProfile,
      onOpenDonate = onOpenDonate,
      modifier = Modifier.align(Alignment.TopCenter),
    )
  }

  // Comments Modal Bottom Sheet (null-safe)
  activeCommentTarget?.let { (targetId, isService) ->
    MemberHomeCommentsSheet(
      targetId = targetId,
      isService = isService,
      observeComments = observeComments,
      reactionSummaries = reactionSummaries,
      onPostComment = onPostComment,
      onReactToComment = onReactToComment,
      onDismiss = { activeCommentTarget = null },
    )
  }

  // Admin / Dyak Create Service Bottom Sheet (Modern & Minimalist)
  if (showCreateServiceDialog) {
    CreateServiceBottomSheet(
      onDismissRequest = { showCreateServiceDialog = false },
      churchMembers = churchMembers,
      currentLanguageCode = currentLanguageCode,
      onSubmit = { sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol, sImageUrl ->
        val sDateFormatted = formatHomeServiceDateLabel(sDate, sStartTime, sEndTime, currentLanguageCode)
        onCreateService(
          sTitle, sTheme, sDateFormatted, sTol, sIsOpen, true, sCategory, sDate, sStartTime, sEndTime,
          sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sSendReminder, sImageUrl,
        )
      },
    )
  }

  // Admin / Dyak Edit Service Bottom Sheet (null-safe)
  editingService?.let { targetService ->
    CreateServiceBottomSheet(
      onDismissRequest = { editingService = null },
      churchMembers = churchMembers,
      initialService = targetService,
      currentLanguageCode = currentLanguageCode,
      onSubmit = { sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol, sImageUrl ->
        val sDateFormatted = formatHomeServiceDateLabel(sDate, sStartTime, sEndTime, currentLanguageCode)
        onUpdateService(
          targetService, sTitle, sTheme, sDateFormatted, sTol, sIsOpen, sCategory, sDate, sStartTime, sEndTime,
          sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sSendReminder, sImageUrl,
        )
      },
    )
  }
}
