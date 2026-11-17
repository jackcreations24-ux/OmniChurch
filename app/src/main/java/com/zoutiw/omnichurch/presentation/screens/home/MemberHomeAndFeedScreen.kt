package com.zoutiw.omnichurch.presentation.screens.home

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
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
import com.zoutiw.omnichurch.presentation.components.CreateAnnouncementBottomSheet
import com.zoutiw.omnichurch.presentation.components.CreateServiceBottomSheet
import com.zoutiw.omnichurch.presentation.components.Floating3DReactionSelectorBar
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.Neumorphic3DActionButton
import com.zoutiw.omnichurch.presentation.components.OmniChurchBrandHeader
import com.zoutiw.omnichurch.presentation.components.OmniChurchSanctuaryHeroCanvas
import com.zoutiw.omnichurch.presentation.components.PremiumAnnouncementCard
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumProfileCard
import com.zoutiw.omnichurch.presentation.components.PremiumSectionHeader
import com.zoutiw.omnichurch.presentation.components.PremiumServiceCard
import com.zoutiw.omnichurch.presentation.components.Stacked3DReactionCluster
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
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
  onCreateAnnouncement: (title: String, content: String, department: String, isPinned: Boolean) -> Unit,
  onCreateService: (
    title: String,
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
    isOpenForAttendance: Boolean,
    sendReminder24h: Boolean,
    lateToleranceMinutes: Long,
    isLive: Boolean
  ) -> Unit,
  modifier: Modifier = Modifier,
  reactionSummaries: Map<String, TargetReactionSummary> = emptyMap(),
  onReactToComment: (commentId: String, reactionType: String) -> Unit = { _, _ -> },
  onDeleteService: (serviceId: String) -> Unit = {},
  onDeleteAnnouncement: (announcementId: String) -> Unit = {},
  onSelectServiceCard: (String) -> Unit = {},
  onUpdateService: (
    serviceId: String,
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
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
  isAdminSession: Boolean = false,
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
    member?.churchName?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchName?.takeIf { it.isNotBlank() }

  var activeCommentTarget by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
  var showCreateAnnouncementDialog by remember { mutableStateOf(false) }
  var showCreateServiceDialog by remember { mutableStateOf(false) }
  var serviceBeingEdited by remember { mutableStateOf<ChurchService?>(null) }

  Box(
    modifier = modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
  ) {
    OmniChurchSanctuaryHeroCanvas(
      modifier = Modifier.fillMaxWidth().height(340.dp).align(Alignment.TopCenter),
      showLeftTextScrim = true,
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize().statusBarsPadding(),
      contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 110.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            OmniChurchBrandHeader(
              badgeText = stringResource(R.string.app_subtitle_mobile),
              isProStyle = false,
            )
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

          Spacer(modifier = Modifier.height(22.dp))

          HeroSanctuaryBannerCard(
            churchName = churchDisplayName
          )
        }
      }

      item {
        Neumorphic3DActionButton(
          text = if (isAdmin) "Lis Manm" else stringResource(R.string.action_member_card),
          icon = if (isAdmin) Icons.Default.People else Icons.Default.Badge,
          onClick = if (isAdmin) onOpenMemberList else onOpenMemberCard,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(68.dp),
          testTag = if (isAdmin) "quick_action_member_list" else "quick_action_member_card",
        )
      }

      val isLoadingFeed =
        servicesState is UiState.Loading && announcementsState is UiState.Loading
      val servicesList = (servicesState as? UiState.Success)?.data ?: emptyList()
      val announcementsList = (announcementsState as? UiState.Success)?.data ?: emptyList()

      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          if (isAdmin) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(ElectricBlue.copy(alpha = 0.25f))
                  .border(1.dp, BlueNeon.copy(alpha = 0.6f), RoundedCornerShape(50))
                  .clickable { showCreateServiceDialog = true }
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BlueNeon, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(text = stringResource(R.string.feed_add_service), style = MaterialTheme.typography.labelSmall, color = PureWhite)
                }
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(50))
                  .background(ElectricBlue.copy(alpha = 0.25f))
                  .border(1.dp, BlueNeon.copy(alpha = 0.6f), RoundedCornerShape(50))
                  .clickable { showCreateAnnouncementDialog = true }
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = BlueNeon, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(text = stringResource(R.string.feed_add_announcement), style = MaterialTheme.typography.labelSmall, color = PureWhite)
                }
              }
            }
          } else {
            Text(
              text = stringResource(R.string.feed_section_title),
              style = MaterialTheme.typography.headlineMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (isLoadingFeed) {
        item { GlassLoadingIndicator() }
      } else if (servicesList.isEmpty() && announcementsList.isEmpty()) {
        item {
          PremiumEmptyState(
            title = stringResource(R.string.feed_empty_title),
            subtitle = stringResource(R.string.feed_empty_subtitle),
            icon = Icons.Default.Campaign,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      } else {
        items(servicesList, key = { "srv_${it.id}" }) { service ->
          PremiumServiceCard(
            service = service,
            reactionSummary = reactionSummaries[service.id],
            onReactClick = { onReactToItem(service.id, true, "pray") },
            onSelectReaction = { reactionType ->
              onReactToItem(service.id, true, reactionType.key)
            },
            onCommentClick = { activeCommentTarget = service.id to true },
            isAdmin = isAdmin,
            onDeleteClick =
              if (isAdmin) {
                { onDeleteService(service.id) }
              } else null,
            onClick = { onSelectServiceCard(service.id) },
            onEditClick = if (isAdmin) ({ serviceBeingEdited = service }) else null,
          )
        }

        items(announcementsList, key = { "ann_${it.id}" }) { announcement ->
          PremiumAnnouncementCard(
            announcement = announcement,
            reactionSummary = reactionSummaries[announcement.id],
            onReactClick = { onReactToItem(announcement.id, false, "pray") },
            onSelectReaction = { reactionType ->
              onReactToItem(announcement.id, false, reactionType.key)
            },
            onCommentClick = { activeCommentTarget = announcement.id to false },
            isAdmin = isAdmin,
            onDeleteClick =
              if (isAdmin) {
                { onDeleteAnnouncement(announcement.id) }
              } else null,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      }
    }
  }

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
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (comments.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MutedGlassText,
                modifier = Modifier.size(42.dp),
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = stringResource(R.string.feed_no_comments),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGlassText,
                textAlign = TextAlign.Center,
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(comments, key = { it.id }) { comment ->
              val commentReactionSummary = reactionSummaries[comment.id]
              val isSelectorOpen = activeReactionCommentId == comment.id

              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x550B1D3D))
                    .border(1.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
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
                      size = 38.dp,
                      photoUrl = comment.userPhotoUrl,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandGold,
                        fontWeight = FontWeight.Bold,
                      )
                      Spacer(modifier = Modifier.height(2.dp))
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
                  Spacer(modifier = Modifier.height(10.dp))
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

        HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          GlassInputField(
            value = commentInput,
            onValueChange = { commentInput = it },
            label = stringResource(R.string.feed_comment_hint),
            modifier = Modifier.weight(1f),
            testTag = "comment_input_field",
          )

          Box(
            modifier =
              Modifier.size(52.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = BlueNeon)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  Brush.verticalGradient(listOf(Color(0xFF22ACFF), ElectricBlue, com.zoutiw.omnichurch.ui.theme.RoyalBlue))
                )
                .border(1.2.dp, PureWhite.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .clickable {
                  if (commentInput.isNotBlank()) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPostComment(targetId, isService, commentInput)
                    commentInput = ""
                  }
                }
                .testTag("comment_send_button"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Send,
              contentDescription = stringResource(R.string.feed_comment_send),
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }

  if (showCreateAnnouncementDialog) {
    val servicesList = (servicesState as? UiState.Success)?.data ?: emptyList()
    CreateAnnouncementBottomSheet(
      onDismissRequest = { showCreateAnnouncementDialog = false },
      availableServices = servicesList,
      onSubmit = { aTitle, aContent, aDept, aCategory, aDate, aAudience, aPinned, aToast, aToastLevel, aSchedNotif, aSchedDate, aPoster, aServiceId ->
        onCreateAnnouncement(aTitle, aContent, aDept, aPinned)
      },
    )
  }

  if (showCreateServiceDialog) {
    CreateServiceBottomSheet(
      onDismissRequest = { showCreateServiceDialog = false },
      onSubmit = { sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol ->
        onCreateService(sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol, true)
      },
    )
  }

  if (serviceBeingEdited != null) {
    CreateServiceBottomSheet(
      onDismissRequest = { serviceBeingEdited = null },
      serviceToEdit = serviceBeingEdited,
      onSubmit = { sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol ->
        onUpdateService(
          serviceBeingEdited!!.id,
          sTitle,
          sTheme,
          "$sDate • $sStartTime",
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
        )
      },
    )
  }
}

@Composable
private fun HeroSanctuaryBannerCard(churchName: String?) {
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
            append(stringResource(R.string.home_hero_title))
          }
        },
      style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp),
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (!churchName.isNullOrBlank()) {
          "${stringResource(R.string.home_hero_tagline)} • $churchName"
        } else {
          stringResource(R.string.home_hero_tagline)
        },
      style = MaterialTheme.typography.bodyMedium,
      color = GlassWhite.copy(alpha = 0.92f),
    )
  }
}
