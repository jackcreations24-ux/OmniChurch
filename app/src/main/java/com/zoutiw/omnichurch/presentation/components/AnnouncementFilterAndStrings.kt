package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PureWhite
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
        "es" -> Locale.forLanguageTag("es")
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
