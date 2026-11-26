package com.zoutiw.omnichurch.presentation.screens.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.core.utilities.OmniLocalization
import com.zoutiw.omnichurch.data.models.CrossChurchVisitItem
import com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem
import com.zoutiw.omnichurch.data.models.MemberAttendanceStatus
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Immutable
internal data class MemberQrGalleryCopy(
  val title: String,
  val qrContentDescription: String,
  val memberIdFormat: String,
  val offlineCachedLabel: String,
  val copyLinkLabel: String,
  val copyLinkToast: String,
  val shareQrLabel: String,
  val verifyCardLabel: String,
  val attendanceHistoryTitle: String,
  val legendPresent: String,
  val legendLate: String,
  val legendAbsent: String,
  val emptyHistoryMsg: String,
  val crossChurchVisitsTitle: String,
  val emptyVisitsMsg: String,
  val statusPresent: String,
  val statusLate: String,
  val statusAbsent: String,
  val statusExcused: String,
  val visitorBadge: String,
)

internal object MemberQrGalleryStrings {
  fun forLanguage(languageCode: String): MemberQrGalleryCopy =
    when (OmniLocalization.normalize(languageCode)) {
      "fr" ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Code QR Officiel Membre",
          memberIdFormat = "ID Membre : %s",
          offlineCachedLabel = "Jeton QR Vérifié • Prêt à Scanner",
          copyLinkLabel = "Copier Lien",
          copyLinkToast = "Lien de vérification officiel copié !",
          shareQrLabel = "Partager QR",
          verifyCardLabel = "Vérifier Carte",
          attendanceHistoryTitle = "Historique",
          legendPresent = "Présent",
          legendLate = "Retard",
          legendAbsent = "Absent",
          emptyHistoryMsg = "Aucun historique de présence pour les dates de culte.",
          crossChurchVisitsTitle = "Mes Visites dans d'Autres Églises",
          emptyVisitsMsg = "Aucune visite enregistrée dans d'autres églises.",
          statusPresent = "Présent",
          statusLate = "En Retard",
          statusAbsent = "Absent",
          statusExcused = "Excusé",
          visitorBadge = "Visiteur ✓",
        )
      "en" ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Official Member QR Code",
          memberIdFormat = "Member ID: %s",
          offlineCachedLabel = "Verified QR Token • Ready to Scan",
          copyLinkLabel = "Copy Link",
          copyLinkToast = "Official verification link copied!",
          shareQrLabel = "Share QR",
          verifyCardLabel = "Verify Card",
          attendanceHistoryTitle = "Attendance History",
          legendPresent = "Present",
          legendLate = "Late",
          legendAbsent = "Absent",
          emptyHistoryMsg = "No attendance history recorded for service dates yet.",
          crossChurchVisitsTitle = "My Visits to Other Churches",
          emptyVisitsMsg = "No visits recorded at other churches yet.",
          statusPresent = "Present",
          statusLate = "Late",
          statusAbsent = "Absent",
          statusExcused = "Excused",
          visitorBadge = "Visitor ✓",
        )
      "es" ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Código QR Oficial de Miembro",
          memberIdFormat = "ID Miembro: %s",
          offlineCachedLabel = "Token QR Verificado • Listo para Escanear",
          copyLinkLabel = "Copiar Enlace",
          copyLinkToast = "¡Enlace de verificación oficial copiado!",
          shareQrLabel = "Compartir QR",
          verifyCardLabel = "Verificar Tarjeta",
          attendanceHistoryTitle = "Historial",
          legendPresent = "Presente",
          legendLate = "Tarde",
          legendAbsent = "Ausente",
          emptyHistoryMsg = "Aún no hay historial de asistencia para las fechas de servicio.",
          crossChurchVisitsTitle = "Mis Visitas a Otras Iglesias",
          emptyVisitsMsg = "Aún no hay visitas registradas en otras iglesias.",
          statusPresent = "Presente",
          statusLate = "Tarde",
          statusAbsent = "Ausente",
          statusExcused = "Excusado",
          visitorBadge = "Visitante ✓",
        )
      else ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Kòd QR Ofisyèl Manm",
          memberIdFormat = "ID Manm: %s",
          offlineCachedLabel = "Token QR Verifye • Pare pou Eskane",
          copyLinkLabel = "Kopye Lyen",
          copyLinkToast = "Lyen verifikasyon ofisyèl la kopye!",
          shareQrLabel = "Pataje QR",
          verifyCardLabel = "Verifye Kat",
          attendanceHistoryTitle = "Istwa Prezans",
          legendPresent = "Prezan",
          legendLate = "An Reta",
          legendAbsent = "Absan",
          emptyHistoryMsg = "Poko gen okenn istwa prezans pou dat sèvis yo.",
          crossChurchVisitsTitle = "Vizit Mwen nan Lòt Legliz",
          emptyVisitsMsg = "Poko gen okenn vizit anrejistre nan lòt legliz.",
          statusPresent = "Prezan",
          statusLate = "An Reta",
          statusAbsent = "Absan",
          statusExcused = "Ekskize",
          visitorBadge = "Vizitè ✓",
        )
    }
}

@Composable
internal fun FlatAttendanceLegendCount(
  label: String,
  count: Int,
  dotColor: Color,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier =
        Modifier.size(6.dp)
          .clip(CircleShape)
          .background(dotColor),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = "$label $count",
      style = MaterialTheme.typography.labelSmall,
      color = MutedGlassText,
    )
  }
}

@Composable
internal fun FlatAttendanceHistoryRow(
  item: MemberAttendanceHistoryItem,
  copy: MemberQrGalleryCopy,
) {
  val (statusLabel, statusColor) =
    when (item.status) {
      MemberAttendanceStatus.PRESENT -> copy.statusPresent to SuccessGreen
      MemberAttendanceStatus.LATE -> copy.statusLate to BrandGold
      MemberAttendanceStatus.ABSENT -> copy.statusAbsent to ErrorRed
      MemberAttendanceStatus.EXCUSED -> copy.statusExcused to BrandGold
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(vertical = 11.dp)
        .testTag("attendance_history_row_${item.id}"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = 12.dp),
    ) {
      Text(
        text = item.dateLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.Medium,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val subtitle =
        if (!item.timeLabel.isNullOrBlank()) {
          "${item.serviceTitle} • ${item.timeLabel}"
        } else {
          item.serviceTitle
        }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier.size(7.dp)
            .clip(CircleShape)
            .background(statusColor),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = statusLabel,
        style = MaterialTheme.typography.bodyMedium,
        color = statusColor,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
internal fun FlatCrossChurchVisitRow(
  visit: CrossChurchVisitItem,
  copy: MemberQrGalleryCopy,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(vertical = 11.dp)
        .testTag("cross_church_visit_row_${visit.id}"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = 12.dp),
    ) {
      Text(
        text = visit.hostChurchName,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val subtitle =
        buildList {
          if (visit.dateLabel.isNotBlank()) add(visit.dateLabel)
          if (visit.serviceTitle.isNotBlank()) add(visit.serviceTitle)
          if (visit.timeLabel.isNotBlank()) add(visit.timeLabel)
        }.joinToString(" • ")
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(50))
          .background(ElectricBlue.copy(alpha = 0.24f))
          .border(0.9.dp, BlueNeon, RoundedCornerShape(50))
          .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
      Text(
        text = copy.visitorBadge,
        style = MaterialTheme.typography.labelSmall,
        color = BlueNeon,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
internal fun QrGalleryCompactActionPill(
  label: String,
  icon: ImageVector,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(50))
        .background(DeepNavy.copy(alpha = 0.85f))
        .border(1.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = accentColor,
      modifier = Modifier.size(15.dp),
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
    )
  }
}
