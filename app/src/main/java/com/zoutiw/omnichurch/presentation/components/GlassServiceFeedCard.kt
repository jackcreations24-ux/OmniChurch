package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.LiveBadgeRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

/**
 * 12. PremiumServiceCard — Self-sufficient 3D Church Service card matching the 3D quality,
 *     borderless full-bleed Portrait/Landscape media showcase, and balanced layout of
 *     PremiumAnnouncementCard while preserving its dedicated Liturgical & Attendance Service role.
 */
@Composable
fun PremiumServiceCard(
  service: ChurchService,
  onReactClick: () -> Unit,
  onCommentClick: () -> Unit,
  modifier: Modifier = Modifier,
  reactionSummary: TargetReactionSummary? = null,
  commentsCountOverride: Int? = null,
  onSelectReaction: ((OmniReactionType) -> Unit)? = null,
  isAdmin: Boolean = false,
  canEdit: Boolean = isAdmin,
  canDelete: Boolean = isAdmin,
  isScanner: Boolean = false,
  churchMembers: List<Member> = emptyList(),
  churchName: String? = null,
  churchLogoUrl: String? = null,
  languageCode: String = "ht",
  onToggleAttendanceWindow: ((Boolean) -> Unit)? = null,
  onOpenScanner: (() -> Unit)? = null,
  onViewAttendances: (() -> Unit)? = null,
  onShareClick: (() -> Unit)? = null,
  onEditClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var showReactionSelector by remember { mutableStateOf(false) }
  var showMoreMenuDialog by remember { mutableStateOf(false) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }
  var showFullImageDialog by remember { mutableStateOf(false) }

  var currentEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val isPermanentlyLocked = service.isPermanentlyLocked(currentEpochMillis)
  val isBeforeStart = service.isBeforeStart(currentEpochMillis)
  val hasPassedEndTime = service.hasPassedEndTime(currentEpochMillis)
  val isLiveActive = service.isLiveBadgeActive(currentEpochMillis)
  val isTimerTicking = service.isTimerTicking(currentEpochMillis)
  val timerLabel = service.elapsedTimerLabel(currentEpochMillis)

  LaunchedEffect(
    service.id,
    service.status,
    service.isOpenForAttendance,
    service.date,
    service.startTime,
    service.endTime,
  ) {
    currentEpochMillis = System.currentTimeMillis()
    while (true) {
      delay(1000L)
      val now = System.currentTimeMillis()
      currentEpochMillis = now
      if (service.isPermanentlyLocked(now)) break
    }
  }

  val serviceDateSubtitle =
    remember(
      service.date,
      service.dateFormatted,
      service.startTime,
      service.endTime,
      service.startTimeEpochMillis,
      languageCode,
    ) {
      service.formattedScheduleWithDay(languageCode)
    }
  val publishedTimestampText =
    remember(service.createdAt, service.dateFormatted, service.date, languageCode) {
      formatServiceTimestamp(service, languageCode)
    }
  val mediaUrl = service.effectiveImageUrl

  val resolvedChurchTitle =
    churchName?.trim()?.takeIf { it.isNotBlank() }
      ?: service.churchName.trim().takeIf { it.isNotBlank() }
      ?: service.categoryLabelHt

  val shareTitle =
    "⛪ ${service.effectiveTitle}" +
      (resolvedChurchTitle.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
  val shareBody = buildString {
    append("📌 Kategori: ${service.categoryLabelHt}")
    if (serviceDateSubtitle.isNotBlank()) {
      append("\n📅 Dat & Lè: $serviceDateSubtitle")
    }
    service.theme?.takeIf { it.isNotBlank() }?.let { append("\n✨ Tèm: $it") }
    service.passage?.takeIf { it.isNotBlank() }?.let { append("\n📖 Pasaj: $it") }
    service.preacher?.takeIf { it.isNotBlank() }?.let { append("\n🎙️ Predikatè: $it") }
    service.leader?.takeIf { it.isNotBlank() }?.let { append("\n🙏 Dirijan: $it") }
    if (!service.description.isNullOrBlank()) {
      append("\n\n${service.description}")
    }
  }

  val triggerShare: () -> Unit = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    if (onShareClick != null) {
      onShareClick()
    } else {
      shareOmniChurchContent(
        context = context,
        title = shareTitle,
        body = shareBody,
      )
    }
  }

  PremiumGlassCard(
    cornerRadius = 22.dp,
    isHighlighted = isLiveActive,
    accentColor =
      when {
        isLiveActive -> LiveBadgeRed
        else -> ElectricBlue
      },
    contentPadding = PaddingValues(0.dp),
    modifier = modifier.fillMaxWidth().testTag("feed_service_card_${service.id}"),
  ) {
    val langNorm = languageCode.trim().lowercase()
    val statusEndedLabel =
      when (langNorm) {
        "fr" -> "TERMINÉ"
        "en" -> "ENDED"
        "es" -> "FINALIZADO"
        else -> "FINI"
      }
    val statusAttendanceOpenLabel =
      when (langNorm) {
        "fr" -> "PRÉSENCE OUVERTE"
        "en" -> "ATTENDANCE OPEN"
        "es" -> "ASISTENCIA ABIERTA"
        else -> "PREZANS OUVRI"
      }
    val statusScheduledLabel =
      when (langNorm) {
        "fr" -> "PROGRAMMÉ"
        "en" -> "SCHEDULED"
        "es" -> "PROGRAMADO"
        else -> "PWOGWAME"
      }

    // Header displays Church Avatar (logoUrl -> initials) & Church Name without repeating service.leader
    ServiceCardHeaderRow(
      serviceId = service.id,
      primaryAuthorTitle = resolvedChurchTitle,
      secondaryAuthorLabel = null,
      churchLogoUrl = churchLogoUrl,
      publishedTimestampText = publishedTimestampText,
    )

    ServiceCardStatusBadgesRow(
      service = service,
      isLiveActive = isLiveActive,
      isTimerTicking = isTimerTicking,
      timerLabel = timerLabel,
      isPermanentlyLocked = isPermanentlyLocked,
      isBeforeStart = isBeforeStart,
      statusEndedLabel = statusEndedLabel,
      statusAttendanceOpenLabel = statusAttendanceOpenLabel,
      statusScheduledLabel = statusScheduledLabel,
    )

    val preacherLabel =
      when (langNorm) {
        "fr" -> "Prédicateur"
        "en" -> "Preacher"
        "es" -> "Predicador"
        else -> "Predikatè"
      }
    val leaderLabel =
      when (langNorm) {
        "fr" -> "Dirigeant"
        "en" -> "Leader"
        "es" -> "Dirigente"
        else -> "Dirijan"
      }
    val readerLabel =
      when (langNorm) {
        "fr" -> "Lecteur"
        "en" -> "Reader"
        "es" -> "Lector"
        else -> "Lektè"
      }

    val officialRoleAvatars =
      remember(service.preacher, service.leader, service.reader, churchMembers, langNorm) {
        buildList {
          service.preacher?.trim()?.takeIf { it.isNotBlank() }?.let { personName ->
            add(
              ServiceOfficialRoleDisplay(
                roleLabel = preacherLabel,
                personName = personName,
                photoUrl = resolveMemberPhotoForRole(personName, churchMembers),
                accentColor = BrandGold,
                badgeBackgroundColor = Color.Transparent,
              )
            )
          }
          service.leader?.trim()?.takeIf { it.isNotBlank() }?.let { personName ->
            add(
              ServiceOfficialRoleDisplay(
                roleLabel = leaderLabel,
                personName = personName,
                photoUrl = resolveMemberPhotoForRole(personName, churchMembers),
                accentColor = BlueNeon,
                badgeBackgroundColor = Color.Transparent,
              )
            )
          }
          service.reader?.trim()?.takeIf { it.isNotBlank() }?.let { personName ->
            add(
              ServiceOfficialRoleDisplay(
                roleLabel = readerLabel,
                personName = personName,
                photoUrl = resolveMemberPhotoForRole(personName, churchMembers),
                accentColor = SuccessGreen,
                badgeBackgroundColor = Color.Transparent,
              )
            )
          }
        }
      }

    ServiceCardHeadlineAndMedia(
      service = service,
      serviceDateSubtitle = serviceDateSubtitle,
      mediaUrl = mediaUrl,
      officialRoleAvatars = officialRoleAvatars,
      onImageClick = { showFullImageDialog = true },
    )

    if (
      (isAdmin && onToggleAttendanceWindow != null) ||
        (isScanner && (onOpenScanner != null || onViewAttendances != null))
    ) {
      ServiceCardAttendanceControls(
        service = service,
        isAdmin = isAdmin,
        isScanner = isScanner,
        isLiveActive = isLiveActive,
        isPermanentlyLocked = isPermanentlyLocked,
        isBeforeStart = isBeforeStart,
        hasPassedEndTime = hasPassedEndTime,
        languageCode = languageCode,
        onToggleAttendanceWindow = onToggleAttendanceWindow,
        onOpenScanner = onOpenScanner,
        onViewAttendances = onViewAttendances,
      )
    }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp)
    ) {
      Floating3DReactionSelectorBar(
        visible = showReactionSelector,
        selectedReaction = reactionSummary?.currentUserReaction,
        onSelectReaction = { chosenReaction ->
          if (onSelectReaction != null) {
            onSelectReaction(chosenReaction)
          } else {
            onReactClick()
          }
        },
        onDismiss = { showReactionSelector = false },
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Stacked3DReactionCluster(
          reactionSummary = reactionSummary,
          fallbackCount = service.reactionsCount,
          onTriggerClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showReactionSelector = !showReactionSelector
          },
          testTag = "service_react_btn_${service.id}",
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable(onClick = onCommentClick)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("service_comment_btn_${service.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.ChatBubbleOutline,
              contentDescription = stringResource(R.string.feed_comment_hint),
              tint = GlassWhite,
              modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            val displayedCommentsCount =
              maxOf(service.commentsCount, (commentsCountOverride ?: 0).toLong())
            Text(
              text = displayedCommentsCount.toString(),
              style = MaterialTheme.typography.labelLarge,
              color = GlassWhite,
            )
          }

          Box(
            modifier =
              Modifier.clip(CircleShape)
                .clickable(onClick = triggerShare)
                .padding(6.dp)
                .testTag("service_share_btn_${service.id}"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Pataje Sèvis la",
              tint = GlassWhite.copy(alpha = 0.90f),
              modifier = Modifier.size(19.dp),
            )
          }

          Box(
            modifier =
              Modifier.clip(CircleShape)
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  showMoreMenuDialog = true
                }
                .padding(6.dp)
                .testTag("service_more_btn_${service.id}"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.MoreHoriz,
              contentDescription = "Plis Opsyon",
              tint = GlassWhite.copy(alpha = 0.90f),
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }

  if (showFullImageDialog && !mediaUrl.isNullOrBlank()) {
    AnnouncementMediaShowcaseLightbox(
      imageSource = mediaUrl,
      title = service.effectiveTitle,
      department = "${service.categoryLabelHt} • $serviceDateSubtitle",
      onDismiss = { showFullImageDialog = false },
    )
  }

  if (showMoreMenuDialog) {
    ServiceCardMoreMenuDialog(
      context = context,
      service = service,
      serviceDateSubtitle = serviceDateSubtitle,
      shareTitle = shareTitle,
      shareBody = shareBody,
      canEdit = canEdit,
      canDelete = canDelete && onDeleteClick != null,
      onTriggerShare = triggerShare,
      onEditClick = onEditClick,
      onRequestDelete = { showConfirmDeleteDialog = true },
      onDismiss = { showMoreMenuDialog = false },
    )
  }

  if (showConfirmDeleteDialog && onDeleteClick != null) {
    ServiceCardConfirmDeleteDialog(
      service = service,
      onConfirmDelete = onDeleteClick,
      onDismiss = { showConfirmDeleteDialog = false },
    )
  }
}
