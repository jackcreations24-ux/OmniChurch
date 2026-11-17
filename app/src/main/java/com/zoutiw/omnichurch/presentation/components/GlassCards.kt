package com.zoutiw.omnichurch.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.LiveBadgeRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

const val OMNICHURCH_OFFICIAL_SHARE_URL =
  "https://ais-pre-hzeqr4nb7corovn7vkwgnr-412075223902.us-east5.run.app"

fun shareOmniChurchContent(
  context: Context,
  title: String,
  body: String,
) {
  val fullMessage = buildString {
    append(title.trim())
    if (body.isNotBlank()) {
      append("\n\n")
      append(body.trim())
    }
    append("\n\n📲 OmniChurch Pro (Legliz la nan poch ou):\n")
    append(OMNICHURCH_OFFICIAL_SHARE_URL)
  }
  runCatching {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, fullMessage)
      }
    val chooser =
      Intent.createChooser(sendIntent, "Pataje sou OmniChurch").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }.onFailure {
    copyOmniChurchTextToClipboard(context, title, fullMessage)
  }
}

fun shareOmniChurchGeneralApp(
  context: Context,
  churchName: String? = null,
) {
  val header =
    if (!churchName.isNullOrBlank()) {
      "🔥 Rejwenn $churchName sou OmniChurch Mobile!"
    } else {
      "🔥 Dekouvri OmniChurch Mobile — Legliz la nan poch ou!"
    }
  val body =
    "Swiv tout sèvis adorasyon an dirèk, anons ofisyèl, kat idantite manm dijital QR, ak prezans legliz ou sou OmniChurch Pro."
  shareOmniChurchContent(context, header, body)
}

fun copyOmniChurchTextToClipboard(
  context: Context,
  label: String,
  text: String,
) {
  runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Tèks la kopye avèk siksè!", Toast.LENGTH_SHORT).show()
  }
}

@Composable
fun PremiumProfileCard(
  memberName: String,
  subtitle: String,
  unreadNotificationsCount: Int,
  onProfileClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  photoUrl: String? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        Modifier.weight(1f)
          .clip(RoundedCornerShape(50))
          .clickable(onClick = onProfileClick)
          .padding(vertical = 4.dp)
          .testTag("header_profile_row"),
    ) {
      MemberAvatar3D(
        fullName = memberName,
        size = 50.dp,
        photoUrl = photoUrl,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(R.string.home_greeting, memberName.substringBefore(" ")),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(text = " 👋", fontSize = 17.sp)
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.85f),
        )
      }
    }

    NeonIconButton(
      icon = Icons.Default.Notifications,
      contentDescription = stringResource(R.string.nav_notifications),
      badgeCount = unreadNotificationsCount,
      onClick = onNotificationsClick,
      testTag = "header_notifications_button",
    )
  }
}

@Composable
fun PremiumSectionHeader(
  title: String,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  trailingExtraContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (trailingExtraContent != null) {
        trailingExtraContent()
      }
      if (actionText != null && onActionClick != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable(onClick = onActionClick)
              .padding(horizontal = 6.dp, vertical = 4.dp)
              .testTag("feed_see_all_button"),
        ) {
          Text(
            text = actionText,
            style = MaterialTheme.typography.titleSmall,
            color = BrandGold,
            fontWeight = FontWeight.Bold,
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
fun PremiumServiceCard(
  service: ChurchService,
  onReactClick: () -> Unit,
  onCommentClick: () -> Unit,
  modifier: Modifier = Modifier,
  reactionSummary: TargetReactionSummary? = null,
  onSelectReaction: ((OmniReactionType) -> Unit)? = null,
  isAdmin: Boolean = false,
  onShareClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
  onClick: (() -> Unit)? = null,
  onEditClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var showReactionSelector by remember { mutableStateOf(false) }
  var showMoreMenuDialog by remember { mutableStateOf(false) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }

  val serviceDateSubtitle =
    service.dateFormatted.ifBlank { "${service.date} • ${service.startTime}".trim(' ', '•') }
  val shareTitle =
    "⛪ ${service.effectiveTitle}" +
      (service.churchName.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
  val shareBody = buildString {
    if (serviceDateSubtitle.isNotBlank()) {
      append("📅 Dat & Lè: $serviceDateSubtitle")
    }
    if (!service.description.isNullOrBlank()) {
      if (isNotEmpty()) append("\n")
      append(service.description)
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
    cornerRadius = 0.dp,
    contentPadding = PaddingValues(0.dp),
    onClick = onClick,
    modifier = modifier.fillMaxWidth().testTag("feed_service_card_${service.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        MemberAvatar3D(
          fullName = service.effectiveTitle,
          size = 42.dp,
          photoUrl = service.imageUrl,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = service.effectiveTitle,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = serviceDateSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
          )
        }
      }

      if (service.isLive) {
        Box(
          modifier =
            Modifier.shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = LiveBadgeRed)
              .clip(RoundedCornerShape(8.dp))
              .background(
                Brush.horizontalGradient(listOf(LiveBadgeRed, Color(0xFFD91432)))
              )
              .border(1.dp, PureWhite.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.feed_live_badge),
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier.fillMaxWidth()
    ) {
      if (!service.imageUrl.isNullOrBlank()) {
        AsyncImage(
          model = service.imageUrl,
          contentDescription = service.effectiveTitle,
          contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
          modifier = Modifier.fillMaxWidth()
        )
      } else {
        WorshipStageBannerCanvas(modifier = Modifier.fillMaxWidth().height(152.dp))
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

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
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
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
          Text(
            text = service.commentsCount.toString(),
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

  if (showMoreMenuDialog) {
    GlassDialog(
      onDismissRequest = { showMoreMenuDialog = false },
      title = service.effectiveTitle,
    ) {
      Text(
        text = serviceDateSubtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(12.dp))

      if (isAdmin && onEditClick != null) {
        GlassButton(
          text = "Modifye Sèvis sa a (Admin)",
          icon = Icons.Default.Edit,
          isPrimaryNeon = true,
          onClick = {
            showMoreMenuDialog = false
            onEditClick()
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "service_menu_edit_${service.id}",
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      GlassButton(
        text = "Pataje Sèvis sa a",
        icon = Icons.Default.Share,
        onClick = {
          showMoreMenuDialog = false
          triggerShare()
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "service_menu_share_${service.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Pataje Aplikasyon OmniChurch la",
        icon = Icons.Default.Share,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          shareOmniChurchGeneralApp(context, service.churchName)
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "service_menu_share_app_${service.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Kopye Tèks Piblikasyon an",
        icon = Icons.Default.ContentCopy,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          copyOmniChurchTextToClipboard(
            context = context,
            label = service.effectiveTitle,
            text = "$shareTitle\n$shareBody",
          )
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "service_menu_copy_${service.id}",
      )

      if (isAdmin && onDeleteClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
        Spacer(modifier = Modifier.height(10.dp))
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(ErrorRed.copy(alpha = 0.22f))
              .border(1.dp, ErrorRed.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
              .clickable {
                showMoreMenuDialog = false
                showConfirmDeleteDialog = true
              }
              .padding(vertical = 12.dp, horizontal = 14.dp)
              .testTag("service_menu_delete_${service.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Efase Sèvis la",
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Efase Sèvis sa a (Admin)",
              style = MaterialTheme.typography.titleSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }

  if (showConfirmDeleteDialog && onDeleteClick != null) {
    GlassDialog(
      onDismissRequest = { showConfirmDeleteDialog = false },
      title = "Efase Sèvis la?",
    ) {
      Text(
        text = "Èske ou sèten ou vle efase sèvis \"${service.effectiveTitle}\" nan baz done legliz la? Aksyon sa a pa ka defèt.",
        style = MaterialTheme.typography.bodyMedium,
        color = GlassWhite,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text = stringResource(R.string.common_cancel),
          isPrimaryNeon = false,
          onClick = { showConfirmDeleteDialog = false },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text = "Wi, Efase",
          icon = Icons.Default.DeleteOutline,
          onClick = {
            showConfirmDeleteDialog = false
            onDeleteClick()
          },
          modifier = Modifier.weight(1f),
          testTag = "service_confirm_delete_btn_${service.id}",
        )
      }
    }
  }
}

@Composable
fun PremiumAnnouncementCard(
  announcement: Announcement,
  onReactClick: () -> Unit,
  onCommentClick: () -> Unit,
  modifier: Modifier = Modifier,
  reactionSummary: TargetReactionSummary? = null,
  onSelectReaction: ((OmniReactionType) -> Unit)? = null,
  isAdmin: Boolean = false,
  onShareClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var showReactionSelector by remember { mutableStateOf(false) }
  var showMoreMenuDialog by remember { mutableStateOf(false) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }

  val shareTitle = "📢 ${announcement.title} (${announcement.department})"
  val shareBody = announcement.content

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
    contentPadding = PaddingValues(16.dp),
    modifier = modifier.fillMaxWidth().testTag("feed_announcement_card_${announcement.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.size(42.dp)
              .shadow(8.dp, CircleShape, ambientColor = BrandOrange)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(listOf(BrandGold, BrandOrange))
              )
              .border(1.5.dp, PureWhite.copy(alpha = 0.75f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = announcement.title,
            tint = PureWhite,
            modifier = Modifier.size(22.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = announcement.title,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = announcement.department,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
          )
        }
      }

      if (announcement.isPinned) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = announcement.content,
      style = MaterialTheme.typography.bodyLarge,
      color = GlassWhite,
    )

    Spacer(modifier = Modifier.height(10.dp))

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
        fallbackCount = announcement.reactionsCount,
        showLabelWhenUnselected = stringResource(R.string.feed_react_amen),
        onTriggerClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          showReactionSelector = !showReactionSelector
        },
        testTag = "announcement_react_btn_${announcement.id}",
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable(onClick = onCommentClick)
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("announcement_comment_btn_${announcement.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = stringResource(R.string.feed_comment_hint),
            tint = GlassWhite,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = announcement.commentsCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = GlassWhite,
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
              .testTag("announcement_more_btn_${announcement.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Plis Opsyon",
            tint = GlassWhite.copy(alpha = 0.90f),
            modifier = Modifier.size(19.dp),
          )
        }
      }
    }
  }

  if (showMoreMenuDialog) {
    GlassDialog(
      onDismissRequest = { showMoreMenuDialog = false },
      title = announcement.title,
    ) {
      Text(
        text = announcement.department,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(12.dp))

      GlassButton(
        text = "Pataje Anons sa a",
        icon = Icons.Default.Share,
        onClick = {
          showMoreMenuDialog = false
          triggerShare()
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_share_${announcement.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Pataje Aplikasyon OmniChurch la",
        icon = Icons.Default.Share,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          shareOmniChurchGeneralApp(context)
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_share_app_${announcement.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Kopye Tèks Anons la",
        icon = Icons.Default.ContentCopy,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          copyOmniChurchTextToClipboard(
            context = context,
            label = announcement.title,
            text = "$shareTitle\n\n$shareBody",
          )
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_copy_${announcement.id}",
      )

      if (isAdmin && onDeleteClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
        Spacer(modifier = Modifier.height(10.dp))
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(ErrorRed.copy(alpha = 0.22f))
              .border(1.dp, ErrorRed.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
              .clickable {
                showMoreMenuDialog = false
                showConfirmDeleteDialog = true
              }
              .padding(vertical = 12.dp, horizontal = 14.dp)
              .testTag("announcement_menu_delete_${announcement.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Efase Anons la",
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Efase Anons sa a (Admin)",
              style = MaterialTheme.typography.titleSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }

  if (showConfirmDeleteDialog && onDeleteClick != null) {
    GlassDialog(
      onDismissRequest = { showConfirmDeleteDialog = false },
      title = "Efase Anons la?",
    ) {
      Text(
        text = "Èske ou sèten ou vle efase anons \"${announcement.title}\" nan baz done legliz la?",
        style = MaterialTheme.typography.bodyMedium,
        color = GlassWhite,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text = stringResource(R.string.common_cancel),
          isPrimaryNeon = false,
          onClick = { showConfirmDeleteDialog = false },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text = "Wi, Efase",
          icon = Icons.Default.DeleteOutline,
          onClick = {
            showConfirmDeleteDialog = false
            onDeleteClick()
          },
          modifier = Modifier.weight(1f),
          testTag = "announcement_confirm_delete_btn_${announcement.id}",
        )
      }
    }
  }
}

@Composable
fun PremiumSuccessPanel(
  member: Member,
  serviceTitle: String,
  recordedTimeFormatted: String,
  status: String,
  modifier: Modifier = Modifier,
) {
  val isLate = status == "late"
  val badgeLabel =
    if (isLate) stringResource(R.string.attendance_status_late)
    else stringResource(R.string.attendance_status_on_time)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = SuccessGreen)
        .clip(RoundedCornerShape(22.dp))
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xE60B4F35),
              Color(0xF2072E22),
              Color(0xFA061826),
            )
          )
        )
        .border(
          1.6.dp,
          Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.85f), SuccessGreen, Color(0xFF0A6E46))),
          RoundedCornerShape(22.dp),
        )
        .drawBehind {
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(Color.Transparent, PureWhite.copy(alpha = 0.7f), SuccessGreen, Color.Transparent)
              ),
            start = Offset(size.width * 0.2f, 1.dp.toPx()),
            end = Offset(size.width * 0.8f, 1.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
          )
        }
        .padding(14.dp)
        .testTag("scanner_success_confirmation_card"),
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

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        MemberAvatar3D(
          fullName = member.effectiveFullName,
          size = 52.dp,
          photoUrl = member.effectivePhotoUrl,
          borderColor = SuccessGreen,
        )

        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = member.effectiveFullName,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "ID: ${member.effectiveMatrikil}",
            style = MaterialTheme.typography.bodySmall,
            color = GlassWhite.copy(alpha = 0.88f),
          )
        }
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(10.dp))
            .background(
              if (isLate) BrandOrange.copy(alpha = 0.28f)
              else Color(0xFF0A6E46)
            )
            .border(
              1.2.dp,
              if (isLate) BrandGold else SuccessGreen,
              RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text(
          text = badgeLabel,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = GlassWhite.copy(alpha = 0.14f))
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "${stringResource(R.string.attendance_service_prefix)} $serviceTitle",
        style = MaterialTheme.typography.bodySmall,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Schedule,
          contentDescription = null,
          tint = GlassWhite.copy(alpha = 0.85f),
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = recordedTimeFormatted,
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}
