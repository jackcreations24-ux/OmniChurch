package com.zoutiw.omnichurch.presentation.screens.home

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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
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
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.Neumorphic3DActionButton
import com.zoutiw.omnichurch.core.ads.EffectiveMobileAdMobState
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.OmniAdMobBanner
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.OmniChurchProBadge
import com.zoutiw.omnichurch.presentation.components.OmniChurchSanctuaryHeroCanvas
import com.zoutiw.omnichurch.presentation.components.PremiumAnnouncementCard
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumProfileCard
import com.zoutiw.omnichurch.presentation.components.PremiumSectionHeader
import com.zoutiw.omnichurch.presentation.components.PremiumServiceCard
import com.zoutiw.omnichurch.presentation.components.Stacked3DReactionCluster
import com.zoutiw.omnichurch.presentation.components.rememberFeedChromeNestedScrollConnection
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.LiveBadgeRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
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
  canCreateService: Boolean = isAdminSession || userProfile?.isAdminOrSuperAdmin == true,
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
) {
  val displayName =
    member?.effectiveFullName?.takeIf { it.isNotBlank() }
      ?: userProfile?.displayName?.takeIf { it.isNotBlank() }
      ?: "Manm"
  val memberPhotoUrl = member?.effectivePhotoUrl ?: userProfile?.photoUrl
  val isAdmin = isAdminSession || userProfile?.isAdminOrSuperAdmin == true
  val spaceSubtitle =
    if (isAdmin) stringResource(R.string.home_admin_space)
    else stringResource(R.string.home_member_space)
  val churchDisplayName =
    activeChurchName?.takeIf { it.isNotBlank() }
      ?: member?.churchName?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchName?.takeIf { it.isNotBlank() }

  var activeCommentTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
  var showCreateAnnouncementDialog by remember { mutableStateOf(false) }
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
          text = if (isAdmin) "Lis Manm" else stringResource(R.string.action_member_card),
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
            if (canCreateService || isAdmin) {
              {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.padding(end = 8.dp),
                ) {
                  if (isAdmin) {
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(50))
                          .background(BrandGold.copy(alpha = 0.22f))
                          .border(1.dp, BrandGold.copy(alpha = 0.65f), RoundedCornerShape(50))
                          .clickable { showCreateAnnouncementDialog = true }
                          .padding(horizontal = 9.dp, vertical = 4.dp)
                          .testTag("home_add_announcement_chip"),
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = stringResource(R.string.feed_add_announcement),
                          tint = BrandGold,
                          modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                          text = stringResource(R.string.feed_add_announcement),
                          style = MaterialTheme.typography.labelSmall,
                          color = PureWhite,
                        )
                      }
                    }
                  }
                  if (canCreateService) {
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
        items(servicesList, key = { "srv_${it.id}" }) { service ->
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
            churchMembers = churchMembers,
            churchLogoUrl = activeChurchLogoUrl ?: member?.churchLogoUrl,
            languageCode = currentLanguageCode,
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

    // Floating Auto-Hide/Show Top Header (synchronized with Bottom Menu on feed scroll)
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
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF061124).copy(alpha = headerBackdropAlpha))
          .onSizeChanged { size ->
            if (size.height > 0) headerHeightPx = size.height
          }
          .statusBarsPadding()
          .padding(horizontal = 16.dp, vertical = 4.dp),
      ) {
        BoxWithConstraints(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          val isTabletOrExpanded = maxWidth >= 600.dp
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            if (isTabletOrExpanded) {
              OmniChurchLogo(
                variant = LogoVariant.Full,
                modifier = Modifier.height(44.dp),
                contentDescription = "OmniChurch PRO Official Logo",
              )
              Spacer(modifier = Modifier.width(8.dp))
              OmniChurchProBadge()
            } else {
              OmniChurchLogo(
                variant = LogoVariant.Icon,
                modifier = Modifier.size(32.dp),
                contentDescription = "OmniChurch PRO Official Icon",
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "OmniChurch",
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold,
              )
              Spacer(modifier = Modifier.width(6.dp))
              OmniChurchProBadge()
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        PremiumProfileCard(
          memberName = displayName,
          subtitle = spaceSubtitle,
          unreadNotificationsCount = unreadNotificationsCount,
          photoUrl = memberPhotoUrl,
          onProfileClick = onOpenProfile,
          onNotificationsClick = onOpenNotifications,
        )

        if (headerBackdropAlpha > 0.1f) {
          Spacer(modifier = Modifier.height(6.dp))
          HorizontalDivider(color = GlassWhite.copy(alpha = 0.12f), thickness = 0.8.dp)
        }
      }
    }
  }

  // Comments Modal Bottom Sheet
  if (activeCommentTarget != null) {
    val (targetId, isService) = activeCommentTarget!!
    val comments by
      observeComments(targetId).collectAsStateWithLifecycle(initialValue = emptyList())
    var commentInput by remember { mutableStateOf("") }
    var activeReactionCommentId by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    GlassModalBottomSheet(
      onDismissRequest = { activeCommentTarget = null },
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
              onPostComment(targetId, isService, commentInput)
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

  // Admin Publish Announcement Bottom Sheet (Modern & Minimalist)
  if (showCreateAnnouncementDialog) {
    val servicesList = (servicesState as? UiState.Success)?.data ?: emptyList()
    CreateAnnouncementBottomSheet(
      onDismissRequest = { showCreateAnnouncementDialog = false },
      availableServices = servicesList,
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
        aPoster,
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
          aPoster,
          aServiceId,
        )
      },
    )
  }

  // Admin / Dyak Create Service Bottom Sheet (Modern & Minimalist)
  if (showCreateServiceDialog) {
    CreateServiceBottomSheet(
      onDismissRequest = { showCreateServiceDialog = false },
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

  // Admin / Dyak Edit Service Bottom Sheet
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
private fun HeroSanctuaryBannerCard(churchName: String?) {
  val resolvedChurchTitle =
    churchName?.trim()?.takeIf { it.isNotBlank() } ?: "OmniChurch"
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(top = 4.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)
  ) {
    Text(
      text = stringResource(R.string.home_hero_welcome),
      style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp),
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text =
        buildAnnotatedString {
          withStyle(
            SpanStyle(
              brush = Brush.horizontalGradient(listOf(BrandGold, BrandOrange, BrandGold)),
              fontWeight = FontWeight.ExtraBold,
            )
          ) {
            append(resolvedChurchTitle)
          }
        },
      style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp, lineHeight = 34.sp),
    )
  }
}
