package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class AnnouncementFilterMode {
  ALL,
  UNREAD,
  PINNED,
}

data class AnnouncementLocalizedCopy(
  val screenTitle: String,
  val filterAll: String,
  val filterUnread: String,
  val filterPinned: String,
  val markAllRead: String,
  val newBadge: String,
  val pinnedBadge: String,
  val readFullAction: String,
  val readStatusLabel: String,
  val publishedByLabel: String,
  val audienceLabel: String,
  val eventDateLabel: String,
  val shareAction: String,
  val shareAppAction: String,
  val copyTextAction: String,
  val editAction: String,
  val deleteAction: String,
  val deleteDialogTitle: String,
  val deleteConfirmYes: String,
  val closeLabel: String,
  val retryLabel: String,
  val loadErrorTitle: String,
  val allReadEmptyTitle: String,
  val allReadEmptySubtitle: String,
  val noPinnedEmptyTitle: String,
  val noPinnedEmptySubtitle: String,
  val todayLabel: String,
  val yesterdayLabel: String,
  val justNowLabel: String,
  val draftBadge: String,
  val tapToZoomHint: String,
)

object AnnouncementStrings {
  fun forLanguage(languageCode: String): AnnouncementLocalizedCopy =
    when (languageCode.trim().lowercase()) {
      "fr" ->
        AnnouncementLocalizedCopy(
          screenTitle = "Annonces",
          filterAll = "Toutes",
          filterUnread = "Non lues",
          filterPinned = "Épinglées",
          markAllRead = "Tout marquer comme lu",
          newBadge = "NOUVEAU",
          pinnedBadge = "Épinglé",
          readFullAction = "Lire l'annonce complète",
          readStatusLabel = "Lu",
          publishedByLabel = "Publié par",
          audienceLabel = "Public cible",
          eventDateLabel = "Date",
          shareAction = "Partager l'annonce",
          shareAppAction = "Partager OmniChurch",
          copyTextAction = "Copier le texte",
          editAction = "Modifier l'annonce",
          deleteAction = "Supprimer l'annonce (Admin)",
          deleteDialogTitle = "Supprimer l'annonce ?",
          deleteConfirmYes = "Oui, supprimer",
          closeLabel = "Fermer",
          retryLabel = "Réessayer",
          loadErrorTitle = "Impossible de charger les annonces",
          allReadEmptyTitle = "Tout est à jour",
          allReadEmptySubtitle = "Vous avez lu toutes les annonces officielles de votre église.",
          noPinnedEmptyTitle = "Aucune annonce épinglée",
          noPinnedEmptySubtitle = "Les annonces importantes épinglées apparaîtront ici.",
          todayLabel = "Aujourd'hui",
          yesterdayLabel = "Hier",
          justNowLabel = "À l'instant",
          draftBadge = "BROUILLON",
          tapToZoomHint = "Appuyez pour agrandir",
        )
      "en" ->
        AnnouncementLocalizedCopy(
          screenTitle = "Announcements",
          filterAll = "All",
          filterUnread = "Unread",
          filterPinned = "Pinned",
          markAllRead = "Mark all read",
          newBadge = "NEW",
          pinnedBadge = "Pinned",
          readFullAction = "Read full announcement",
          readStatusLabel = "Read",
          publishedByLabel = "Published by",
          audienceLabel = "Audience",
          eventDateLabel = "Date",
          shareAction = "Share announcement",
          shareAppAction = "Share OmniChurch App",
          copyTextAction = "Copy text",
          editAction = "Edit announcement",
          deleteAction = "Delete announcement (Admin)",
          deleteDialogTitle = "Delete announcement?",
          deleteConfirmYes = "Yes, delete",
          closeLabel = "Close",
          retryLabel = "Retry",
          loadErrorTitle = "Unable to load announcements",
          allReadEmptyTitle = "All caught up",
          allReadEmptySubtitle = "You have read all official announcements from your church.",
          noPinnedEmptyTitle = "No pinned announcements",
          noPinnedEmptySubtitle = "Important pinned announcements will appear here.",
          todayLabel = "Today",
          yesterdayLabel = "Yesterday",
          justNowLabel = "Just now",
          draftBadge = "DRAFT",
          tapToZoomHint = "Tap image to zoom",
        )
      "es" ->
        AnnouncementLocalizedCopy(
          screenTitle = "Anuncios",
          filterAll = "Todos",
          filterUnread = "No leídos",
          filterPinned = "Fijados",
          markAllRead = "Marcar todo leído",
          newBadge = "NUEVO",
          pinnedBadge = "Fijado",
          readFullAction = "Leer anuncio completo",
          readStatusLabel = "Leído",
          publishedByLabel = "Publicado por",
          audienceLabel = "Público",
          eventDateLabel = "Fecha",
          shareAction = "Compartir anuncio",
          shareAppAction = "Compartir OmniChurch",
          copyTextAction = "Copiar texto",
          editAction = "Editar anuncio",
          deleteAction = "Eliminar anuncio (Admin)",
          deleteDialogTitle = "¿Eliminar anuncio?",
          deleteConfirmYes = "Sí, eliminar",
          closeLabel = "Cerrar",
          retryLabel = "Reintentar",
          loadErrorTitle = "No se pudieron cargar los anuncios",
          allReadEmptyTitle = "Todo al día",
          allReadEmptySubtitle = "Ya leíste todos los anuncios oficiales de tu iglesia.",
          noPinnedEmptyTitle = "Sin anuncios fijados",
          noPinnedEmptySubtitle = "Los anuncios importantes fijados aparecerán aquí.",
          todayLabel = "Hoy",
          yesterdayLabel = "Ayer",
          justNowLabel = "Ahora mismo",
          draftBadge = "BORRADOR",
          tapToZoomHint = "Toca para ampliar",
        )
      else ->
        AnnouncementLocalizedCopy(
          screenTitle = "Anons Legliz",
          filterAll = "Tout",
          filterUnread = "Pa li",
          filterPinned = "Epingle",
          markAllRead = "Make tout kòm li",
          newBadge = "NOUVO",
          pinnedBadge = "Epingle",
          readFullAction = "Li tout anons la",
          readStatusLabel = "Li",
          publishedByLabel = "Pibliye pa",
          audienceLabel = "Piblik sib",
          eventDateLabel = "Dat",
          shareAction = "Pataje Anons sa a",
          shareAppAction = "Pataje Aplikasyon OmniChurch la",
          copyTextAction = "Kopye Tèks Anons la",
          editAction = "Modifye Anons sa a",
          deleteAction = "Efase Anons sa a (Admin)",
          deleteDialogTitle = "Efase Anons la?",
          deleteConfirmYes = "Wi, Efase",
          closeLabel = "Fèmen",
          retryLabel = "Re-eseye",
          loadErrorTitle = "Pa ka chaje anons yo",
          allReadEmptyTitle = "Ou deja li tout anons yo",
          allReadEmptySubtitle = "Tout anons ofisyèl legliz ou a deja make kòm li.",
          noPinnedEmptyTitle = "Okenn anons epingle",
          noPinnedEmptySubtitle = "Anons enpòtan ki epingle yo ap parèt isit la.",
          todayLabel = "Jodi a",
          yesterdayLabel = "Yè",
          justNowLabel = "Kounye a",
          draftBadge = "BOUYON",
          tapToZoomHint = "Peze foto a pou agrandi",
        )
    }

  fun formatTimestamp(announcement: Announcement, languageCode: String): String {
    val copy = forLanguage(languageCode)
    val locale =
      when (languageCode.trim().lowercase()) {
        "fr" -> Locale.FRENCH
        "en" -> Locale.ENGLISH
        "es" -> Locale("es")
        else -> Locale.getDefault()
      }
    val ts = announcement.createdAt ?: announcement.updatedAt
    if (ts != null) {
      val dateObj = ts.toDate()
      val nowCal = Calendar.getInstance()
      val targetCal = Calendar.getInstance().apply { time = dateObj }
      val timeStr = SimpleDateFormat("hh:mm a", locale).format(dateObj)

      val isSameDay =
        nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
          nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
      if (isSameDay) {
        return "${copy.todayLabel} • $timeStr"
      }

      val yesterdayCal =
        (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
      val isYesterday =
        yesterdayCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
          yesterdayCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
      if (isYesterday) {
        return "${copy.yesterdayLabel} • $timeStr"
      }

      return SimpleDateFormat("dd MMM yyyy • hh:mm a", locale).format(dateObj)
    }

    val fallbackDate =
      listOfNotNull(
          announcement.date?.trim()?.takeIf { it.isNotEmpty() },
          announcement.time?.trim()?.takeIf { it.isNotEmpty() },
        )
        .joinToString(" • ")
    return fallbackDate.ifEmpty { copy.justNowLabel }
  }
}

@Composable
fun AnnouncementFilterBar(
  selectedFilter: AnnouncementFilterMode,
  totalCount: Int,
  unreadCount: Int,
  pinnedCount: Int,
  languageCode: String,
  onSelectFilter: (AnnouncementFilterMode) -> Unit,
  onMarkAllRead: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val copy = remember(languageCode) { AnnouncementStrings.forLanguage(languageCode) }
  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AnnouncementFilterPill(
        label = "${copy.filterAll} ($totalCount)",
        selected = selectedFilter == AnnouncementFilterMode.ALL,
        onClick = { onSelectFilter(AnnouncementFilterMode.ALL) },
        testTag = "announcements_filter_all",
      )
      AnnouncementFilterPill(
        label = if (unreadCount > 0) "${copy.filterUnread} ($unreadCount)" else copy.filterUnread,
        selected = selectedFilter == AnnouncementFilterMode.UNREAD,
        accentColor = BlueNeon,
        onClick = { onSelectFilter(AnnouncementFilterMode.UNREAD) },
        testTag = "announcements_filter_unread",
      )
      AnnouncementFilterPill(
        label = if (pinnedCount > 0) "${copy.filterPinned} ($pinnedCount)" else copy.filterPinned,
        selected = selectedFilter == AnnouncementFilterMode.PINNED,
        accentColor = BrandGold,
        onClick = { onSelectFilter(AnnouncementFilterMode.PINNED) },
        testTag = "announcements_filter_pinned",
      )
    }

    if (unreadCount > 0) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(BlueNeon.copy(alpha = 0.16f))
              .border(1.dp, BlueNeon.copy(alpha = 0.55f), RoundedCornerShape(50))
              .clickable(onClick = onMarkAllRead)
              .padding(horizontal = 12.dp, vertical = 7.dp)
              .testTag("announcements_mark_all_read_btn"),
        ) {
          Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = copy.markAllRead,
            tint = BlueNeon,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = copy.markAllRead,
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun AnnouncementFilterPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
  accentColor: Color = ElectricBlue,
) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(
          if (selected) accentColor.copy(alpha = 0.28f) else Color(0x660B1D3D)
        )
        .border(
          width = if (selected) 1.2.dp else 0.8.dp,
          color = if (selected) accentColor else GlassWhite.copy(alpha = 0.22f),
          shape = RoundedCornerShape(50),
        )
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) PureWhite else GlassWhite.copy(alpha = 0.80f),
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementReaderSheet(
  announcement: Announcement,
  languageCode: String,
  reactionSummary: TargetReactionSummary?,
  commentsCountOverride: Int?,
  onDismiss: () -> Unit,
  onMarkRead: (String) -> Unit,
  onSelectReaction: (OmniReactionType) -> Unit,
  onOpenComments: () -> Unit,
  onShareClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val copy = remember(languageCode) { AnnouncementStrings.forLanguage(languageCode) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showReactionSelector by remember { mutableStateOf(false) }
  var showFullImage by remember { mutableStateOf(false) }

  LaunchedEffect(announcement.id) {
    onMarkRead(announcement.id)
  }

  val resolvedTitle = remember(announcement, languageCode) { announcement.localizedTitle(languageCode) }
  val resolvedContent = remember(announcement, languageCode) { announcement.localizedContent(languageCode) }
  val timestampLabel = remember(announcement, languageCode) {
    AnnouncementStrings.formatTimestamp(announcement, languageCode)
  }
  val effectiveImage = announcement.effectiveImageUrl
  val displayedCommentsCount =
    maxOf(announcement.commentsCount, (commentsCountOverride ?: 0).toLong())

  val shareTitle =
    if (announcement.department.isNotBlank()) "📢 $resolvedTitle (${announcement.department})"
    else "📢 $resolvedTitle"

  val triggerShare: () -> Unit = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    if (onShareClick != null) {
      onShareClick()
    } else {
      shareOmniChurchContent(
        context = context,
        title = shareTitle,
        body = resolvedContent,
      )
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF071630),
    contentColor = PureWhite,
    dragHandle = {
      Box(
        modifier =
          Modifier.padding(top = 10.dp, bottom = 6.dp)
            .width(42.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(GlassWhite.copy(alpha = 0.35f))
      )
    },
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 6.dp)
          .testTag("announcement_reader_sheet_${announcement.id}"),
    ) {
      // Top Header Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(38.dp)
                .shadow(6.dp, CircleShape, ambientColor = BrandOrange)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(BrandGold, BrandOrange)))
                .border(1.2.dp, PureWhite.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Campaign,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            if (announcement.department.isNotBlank()) {
              Text(
                text = announcement.department,
                style = MaterialTheme.typography.labelLarge,
                color = BrandGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = BlueNeon.copy(alpha = 0.85f),
                modifier = Modifier.size(12.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = timestampLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MutedGlassText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(SuccessGreen.copy(alpha = 0.16f))
                .border(1.dp, SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = copy.readStatusLabel,
                tint = SuccessGreen,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = copy.readStatusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("announcement_reader_close_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = copy.closeLabel,
              tint = GlassWhite,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = GlassWhite.copy(alpha = 0.14f))

      // Scrollable Reading Body
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f, fill = false)
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Badges row (Pinned / Draft / Audience / Event Date)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (announcement.isPinned) {
            ReaderMetaChip(
              icon = Icons.Default.PushPin,
              text = copy.pinnedBadge,
              tint = BrandGold,
            )
          }
          if (!announcement.isPublished) {
            ReaderMetaChip(
              icon = Icons.Default.Campaign,
              text = copy.draftBadge,
              tint = BrandOrange,
            )
          }
          if (announcement.targetAudience.isNotBlank()) {
            ReaderMetaChip(
              icon = Icons.Default.Groups,
              text = announcement.targetAudience,
              tint = BlueNeon,
            )
          }
          val eventDate =
            listOfNotNull(
                announcement.date?.trim()?.takeIf { it.isNotEmpty() },
                announcement.time?.trim()?.takeIf { it.isNotEmpty() },
              )
              .joinToString(" • ")
          if (eventDate.isNotEmpty()) {
            ReaderMetaChip(
              icon = Icons.Default.CalendarToday,
              text = eventDate,
              tint = GlassWhite,
            )
          }
        }

        // Selectable Title + Full Body Content
        SelectionContainer {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = resolvedTitle,
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp,
              ),
              color = PureWhite,
              modifier = Modifier.testTag("announcement_reader_title"),
            )

            if (announcement.authorName.isNotBlank()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = MutedGlassText,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = "${copy.publishedByLabel}: ${announcement.authorName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedGlassText,
                )
              }
            }

            if (!effectiveImage.isNullOrBlank()) {
              ReaderPosterPreview(
                imageSource = effectiveImage,
                title = resolvedTitle,
                caption = announcement.imageCaption,
                tapHint = copy.tapToZoomHint,
                onClick = { showFullImage = true },
              )
            }

            if (resolvedContent.isNotBlank()) {
              Text(
                text = resolvedContent,
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontSize = 16.sp,
                  lineHeight = 25.sp,
                ),
                color = GlassWhite,
                modifier = Modifier.fillMaxWidth().testTag("announcement_reader_content"),
              )
            }
          }
        }
      }

      HorizontalDivider(color = GlassWhite.copy(alpha = 0.14f))
      Spacer(modifier = Modifier.height(10.dp))

      // Reaction selector bar inside reader
      Floating3DReactionSelectorBar(
        visible = showReactionSelector,
        selectedReaction = reactionSummary?.currentUserReaction,
        onSelectReaction = { chosen ->
          onSelectReaction(chosen)
          showReactionSelector = false
        },
        onDismiss = { showReactionSelector = false },
      )

      // Action Bar (Reactions, Comments, Copy, Share)
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
          testTag = "reader_react_btn_${announcement.id}",
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(Color(0x55132952))
                .border(0.8.dp, GlassWhite.copy(alpha = 0.2f), RoundedCornerShape(50))
                .clickable(onClick = onOpenComments)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("reader_comment_btn_${announcement.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.ChatBubbleOutline,
              contentDescription = stringResource(R.string.feed_comment_hint),
              tint = GlassWhite,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = displayedCommentsCount.toString(),
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }

          IconButton(
            onClick = {
              copyOmniChurchTextToClipboard(
                context = context,
                label = resolvedTitle,
                text = "$shareTitle\n\n$resolvedContent",
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("reader_copy_btn_${announcement.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = copy.copyTextAction,
              tint = GlassWhite.copy(alpha = 0.90f),
              modifier = Modifier.size(19.dp),
            )
          }

          IconButton(
            onClick = triggerShare,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("reader_share_btn_${announcement.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = copy.shareAction,
              tint = BlueNeon,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }

  if (showFullImage && !effectiveImage.isNullOrBlank()) {
    AnnouncementMediaShowcaseLightbox(
      imageSource = effectiveImage,
      title = resolvedTitle,
      department = announcement.department,
      onDismiss = { showFullImage = false },
    )
  }
}

@Composable
private fun ReaderMetaChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
  tint: Color,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(tint.copy(alpha = 0.14f))
        .border(0.8.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 5.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(13.dp),
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = PureWhite,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ReaderPosterPreview(
  imageSource: String,
  title: String,
  caption: String?,
  tapHint: String,
  onClick: () -> Unit,
) {
  val cleanSource = remember(imageSource) { imageSource.trim() }
  val decodedBitmap = remember(cleanSource) { ImageCompressor.decodeBase64DataUrlToBitmap(cleanSource) }
  var remoteRatio by remember(cleanSource) { mutableStateOf<Float?>(null) }
  var loadError by remember(cleanSource) { mutableStateOf(false) }

  if (loadError && decodedBitmap == null) return

  val naturalRatio =
    remember(decodedBitmap, remoteRatio) {
      if (decodedBitmap != null && decodedBitmap.width > 0 && decodedBitmap.height > 0) {
        decodedBitmap.width.toFloat() / decodedBitmap.height.toFloat()
      } else {
        remoteRatio
      }
    }?.coerceIn(0.65f, 2.2f)

  val imageModifier =
    if (naturalRatio != null) Modifier.fillMaxWidth().aspectRatio(naturalRatio)
    else Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp)

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(
      modifier =
        imageModifier
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, GlassWhite.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
          .clickable(onClick = onClick)
          .testTag("announcement_reader_image"),
    ) {
      if (decodedBitmap != null) {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize(),
        )
      } else {
        AsyncImage(
          model = cleanSource,
          contentDescription = title,
          contentScale = if (naturalRatio != null) ContentScale.Crop else ContentScale.FillWidth,
          onSuccess = { state ->
            val d = state.result.drawable
            if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
              remoteRatio = d.intrinsicWidth.toFloat() / d.intrinsicHeight.toFloat()
            }
          },
          onError = { loadError = true },
          modifier = Modifier.matchParentSize(),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .padding(10.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xB8040A18))
            .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Icon(
          imageVector = Icons.Default.ZoomIn,
          contentDescription = tapHint,
          tint = PureWhite,
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = tapHint,
          style = MaterialTheme.typography.labelSmall,
          color = PureWhite,
        )
      }
    }

    if (!caption.isNullOrBlank()) {
      Text(
        text = caption.trim(),
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
    }
  }
}

@Composable
fun AnnouncementReadMorePrompt(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 4.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = BlueNeon,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.width(2.dp))
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = label,
      tint = BlueNeon,
      modifier = Modifier.size(17.dp),
    )
  }
}
