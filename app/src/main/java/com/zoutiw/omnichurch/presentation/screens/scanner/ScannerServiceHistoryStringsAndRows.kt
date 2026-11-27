package com.zoutiw.omnichurch.presentation.screens.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date

internal enum class ScannerHistoryTab(val tagSuffix: String) {
  PRESENT("present"),
  LATE("late"),
  ABSENT("absent"),
  VISITOR("visitor"),
}

internal data class ScannerHistoryCopy(
  val sectionTitle: String,
  val tabPresent: String,
  val tabLate: String,
  val tabAbsent: String,
  val tabVisitor: String,
  val pickServiceTitle: String,
  val liveLabel: String,
  val emptyServiceLabel: String,
  val emptyPresent: String,
  val emptyLate: String,
  val emptyAbsent: String,
  val emptyAbsentPendingService: String,
  val emptyVisitor: String,
  val statusOnTime: String,
  val statusLate: String,
  val statusAbsent: String,
  val statusNotCheckedInYet: String,
  val statusVisitor: String,
  val showAllFormat: String,
  val showLessLabel: String,
  val methodQr: String,
  val methodNfc: String,
  val methodManual: String,
)

internal object ScannerHistoryStrings {
  fun forLanguage(languageCode: String): ScannerHistoryCopy =
    when (languageCode.trim().lowercase()) {
      "fr" ->
        ScannerHistoryCopy(
          sectionTitle = "Historique des services",
          tabPresent = "Présents",
          tabLate = "Retard",
          tabAbsent = "Absents",
          tabVisitor = "Visiteurs",
          pickServiceTitle = "Choisir un service",
          liveLabel = "Actif",
          emptyServiceLabel = "Aucun service enregistré pour le moment.",
          emptyPresent = "Aucune présence enregistrée.",
          emptyLate = "Aucun retard enregistré.",
          emptyAbsent = "Aucun absent pour ce service.",
          emptyAbsentPendingService =
            "Ce service n'est pas encore passé. Les absences s'affichent uniquement après la fin du service.",
          emptyVisitor = "Aucun visiteur enregistré.",
          statusOnTime = "À l'heure",
          statusLate = "En retard",
          statusAbsent = "Absent",
          statusNotCheckedInYet = "Pas encore scanné",
          statusVisitor = "Visiteur",
          showAllFormat = "Voir tout (%d)",
          showLessLabel = "Réduire",
          methodQr = "QR",
          methodNfc = "NFC",
          methodManual = "Code",
        )
      "en" ->
        ScannerHistoryCopy(
          sectionTitle = "Service History",
          tabPresent = "Present",
          tabLate = "Late",
          tabAbsent = "Absent",
          tabVisitor = "Visitors",
          pickServiceTitle = "Select a service",
          liveLabel = "Active",
          emptyServiceLabel = "No services recorded yet.",
          emptyPresent = "No attendance recorded yet.",
          emptyLate = "No late arrivals recorded.",
          emptyAbsent = "No absent members for this service.",
          emptyAbsentPendingService =
            "This service has not passed yet. Absences are only shown after the service ends.",
          emptyVisitor = "No visitors recorded yet.",
          statusOnTime = "On time",
          statusLate = "Late",
          statusAbsent = "Absent",
          statusNotCheckedInYet = "Not checked in",
          statusVisitor = "Visitor",
          showAllFormat = "Show all (%d)",
          showLessLabel = "Show less",
          methodQr = "QR",
          methodNfc = "NFC",
          methodManual = "Code",
        )
      "es" ->
        ScannerHistoryCopy(
          sectionTitle = "Historial de servicios",
          tabPresent = "Presentes",
          tabLate = "Tarde",
          tabAbsent = "Ausentes",
          tabVisitor = "Visitantes",
          pickServiceTitle = "Seleccionar servicio",
          liveLabel = "Activo",
          emptyServiceLabel = "Aún no hay servicios registrados.",
          emptyPresent = "Sin asistencias registradas.",
          emptyLate = "Sin llegadas tarde.",
          emptyAbsent = "Sin ausentes en este servicio.",
          emptyAbsentPendingService =
            "Este servicio aún no ha pasado. Las ausencias solo se muestran después de finalizar el servicio.",
          emptyVisitor = "Sin visitantes registrados.",
          statusOnTime = "A tiempo",
          statusLate = "Tarde",
          statusAbsent = "Ausente",
          statusNotCheckedInYet = "Pendiente",
          statusVisitor = "Visitante",
          showAllFormat = "Ver todo (%d)",
          showLessLabel = "Mostrar menos",
          methodQr = "QR",
          methodNfc = "NFC",
          methodManual = "Código",
        )
      else ->
        ScannerHistoryCopy(
          sectionTitle = "Istwa Sèvis yo",
          tabPresent = "Prezans",
          tabLate = "Reta",
          tabAbsent = "Absans",
          tabVisitor = "Vizitè",
          pickServiceTitle = "Chwazi yon sèvis",
          liveLabel = "Aktif",
          emptyServiceLabel = "Poko gen okenn sèvis anrejistre.",
          emptyPresent = "Poko gen okenn prezans anrejistre.",
          emptyLate = "Okenn manm an reta pou sèvis sa a.",
          emptyAbsent = "Okenn absans pou sèvis sa a.",
          emptyAbsentPendingService =
            "Sèvis sa a poko pase. Absans yo parèt sèlman pou istwa sèvis ki deja pase.",
          emptyVisitor = "Poko gen okenn vizitè anrejistre.",
          statusOnTime = "Alè",
          statusLate = "Reta",
          statusAbsent = "Absan",
          statusNotCheckedInYet = "Poko la",
          statusVisitor = "Vizitè",
          showAllFormat = "Wè tout (%d)",
          showLessLabel = "Redwi",
          methodQr = "QR",
          methodNfc = "NFC",
          methodManual = "Kòd",
        )
    }
}

@Composable
internal fun ScannerServicePickerSheet(
  orderedServices: List<ChurchService>,
  inspectedServiceId: String?,
  copy: ScannerHistoryCopy,
  onSelectService: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  GlassModalBottomSheet(
    onDismissRequest = onDismiss,
    title = copy.pickServiceTitle,
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
    ) {
      items(orderedServices, key = { it.id }) { srv ->
        val isSelected = srv.id == inspectedServiceId
        val isLiveOpen = srv.isOpenForAttendance && !srv.isPermanentlyEnded
        Column {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onSelectService(srv.id) }
                .padding(vertical = 12.dp, horizontal = 4.dp)
                .testTag("scanner_history_pick_service_${srv.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                if (isLiveOpen) {
                  Box(
                    modifier = Modifier.size(7.dp).clip(CircleShape).background(SuccessGreen)
                  )
                }
                Text(
                  text = srv.effectiveTitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isSelected) BlueNeon else PureWhite,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              val sub =
                listOfNotNull(
                    srv.effectiveScheduleLabel.takeIf { it.isNotBlank() },
                    if (isLiveOpen) copy.liveLabel else null,
                  )
                  .joinToString(" • ")
              if (sub.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = sub,
                  style = MaterialTheme.typography.labelSmall,
                  color = MutedGlassText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            if (isSelected) {
              Spacer(modifier = Modifier.width(8.dp))
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = BlueNeon,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          HorizontalDivider(color = GlassWhite.copy(alpha = 0.08f), thickness = 0.6.dp)
        }
      }
    }
  }
}

@Composable
internal fun BorderlessHistoryTab(
  label: String,
  count: Int,
  selected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val textColor by
    animateColorAsState(
      targetValue = if (selected) PureWhite else MutedGlassText,
      animationSpec = tween(160),
      label = "history_tab_text",
    )
  val countColor by
    animateColorAsState(
      targetValue = if (selected) accentColor else GlassWhite.copy(alpha = 0.72f),
      animationSpec = tween(160),
      label = "history_tab_count",
    )

  Column(
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(top = 6.dp, bottom = 4.dp, start = 2.dp, end = 2.dp)
        .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.labelLarge,
      color = countColor,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      softWrap = false,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(5.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth(0.68f)
          .height(2.dp)
          .clip(RoundedCornerShape(50))
          .background(if (selected) accentColor else Color.Transparent)
    )
  }
}

@Composable
internal fun BorderlessAttendanceRow(
  fullName: String,
  photoUrl: String?,
  subtitle: String,
  trailingTop: String?,
  trailingStatus: String,
  statusColor: Color,
  showDivider: Boolean,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        MemberAvatar3D(
          fullName = fullName,
          size = 34.dp,
          photoUrl = photoUrl,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = fullName,
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (subtitle.isNotBlank()) {
            Text(
              text = subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MutedGlassText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        if (!trailingTop.isNullOrBlank()) {
          Text(
            text = trailingTop,
            style = MaterialTheme.typography.labelSmall,
            color = GlassWhite.copy(alpha = 0.85f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
          Text(
            text = trailingStatus,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
          )
        }
      }
    }
    if (showDivider) {
      HorizontalDivider(color = GlassWhite.copy(alpha = 0.07f), thickness = 0.6.dp)
    }
  }
}

@Composable
internal fun BorderlessEmptyNotice(message: String) {
  Box(
    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MutedGlassText,
    )
  }
}

@Composable
internal fun BorderlessExpandToggle(
  isExpanded: Boolean,
  totalCount: Int,
  copy: ScannerHistoryCopy,
  onToggle: () -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 44.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onToggle)
        .padding(vertical = 8.dp)
        .testTag("scanner_history_expand_toggle"),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = if (isExpanded) copy.showLessLabel else String.format(copy.showAllFormat, totalCount),
      style = MaterialTheme.typography.labelMedium,
      color = BlueNeon,
      fontWeight = FontWeight.Bold,
    )
  }
}

internal fun formatRecordTime(
  record: AttendanceRecord,
  formatter: SimpleDateFormat,
): String {
  val millis = record.createdAt?.toDate()?.time ?: record.timestamp
  if (millis <= 0L) return ""
  return formatter.format(Date(millis))
}

internal fun buildRecordSubtitle(
  record: AttendanceRecord,
  copy: ScannerHistoryCopy,
): String {
  val methodLabel =
    when (record.checkInMethod.trim().lowercase()) {
      "nfc_scan" -> copy.methodNfc
      "manual", "kiosk" -> copy.methodManual
      else -> copy.methodQr
    }
  return listOfNotNull(
      record.effectiveMemberCode.takeIf { it.isNotBlank() },
      methodLabel,
    )
    .joinToString(" • ")
}
