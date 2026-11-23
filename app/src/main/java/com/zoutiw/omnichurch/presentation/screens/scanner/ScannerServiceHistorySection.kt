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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private enum class ScannerHistoryTab(val tagSuffix: String) {
  PRESENT("present"),
  LATE("late"),
  ABSENT("absent"),
  VISITOR("visitor"),
}

private data class ScannerHistoryCopy(
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

private object ScannerHistoryStrings {
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

private const val DEFAULT_VISIBLE_ROWS = 12

/**
 * Simple, borderless Service History section embedded directly inside the QR Scanner screen
 * with 4 compact tabs: Prezans, Reta, Absans, Vizitè.
 */
@Composable
internal fun ScannerServiceHistorySection(
  activeService: ChurchService?,
  allServices: List<ChurchService>,
  churchMembers: List<Member>,
  languageCode: String,
  observeServiceAttendances: (serviceId: String) -> Flow<List<AttendanceRecord>>,
  modifier: Modifier = Modifier,
) {
  val copy = remember(languageCode) { ScannerHistoryStrings.forLanguage(languageCode) }

  // Combine activeService + allServices deduplicated by ID, active service first then newest
  val orderedServices =
    remember(activeService, allServices) {
      val map = LinkedHashMap<String, ChurchService>()
      if (activeService != null && activeService.id.isNotBlank()) {
        map[activeService.id] = activeService
      }
      allServices
        .sortedWith(
          compareByDescending<ChurchService> { it.isOpenForAttendance && !it.isPermanentlyEnded }
            .thenByDescending { it.startTimeEpochMillis }
            .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
        )
        .forEach { srv ->
          if (srv.id.isNotBlank() && !map.containsKey(srv.id)) {
            map[srv.id] = srv
          }
        }
      map.values.toList()
    }

  var manuallyPickedServiceId by remember { mutableStateOf<String?>(null) }
  var showServicePicker by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableStateOf(ScannerHistoryTab.PRESENT) }
  var isExpanded by remember { mutableStateOf(false) }

  // Default to live active service (or latest service) unless user explicitly picks another
  val inspectedService =
    remember(orderedServices, activeService?.id, manuallyPickedServiceId) {
      val picked =
        manuallyPickedServiceId?.let { id -> orderedServices.firstOrNull { it.id == id } }
      picked ?: activeService ?: orderedServices.firstOrNull()
    }

  // Reset expansion when switching service or tab
  LaunchedEffect(inspectedService?.id, selectedTab) {
    isExpanded = false
  }

  val attendanceFlow =
    remember(inspectedService?.id, observeServiceAttendances) {
      val srvId = inspectedService?.id?.trim().orEmpty()
      if (srvId.isEmpty()) flowOf(emptyList()) else observeServiceAttendances(srvId)
    }
  val records by attendanceFlow.collectAsStateWithLifecycle(initialValue = emptyList())

  val regularRecords = remember(records) { records.filter { !it.isCrossChurchVisitor } }
  val lateRecords =
    remember(regularRecords) {
      regularRecords.filter {
        it.effectiveStatus.equals("late", ignoreCase = true) ||
          it.effectiveStatus.equals("an_reta", ignoreCase = true)
      }
    }
  val visitorRecords = remember(records) { records.filter { it.isCrossChurchVisitor } }

  val activeChurchMembers =
    remember(churchMembers) {
      churchMembers.filter { member ->
        !member.id.startsWith("ADMIN-", ignoreCase = true) &&
          !member.status.equals("inactive", ignoreCase = true) &&
          !member.status.equals("suspended", ignoreCase = true)
      }
    }

  val absentMembers =
    remember(regularRecords, activeChurchMembers) {
      if (activeChurchMembers.isEmpty()) {
        emptyList()
      } else {
        val presentKeys = HashSet<String>(regularRecords.size * 4)
        regularRecords.forEach { rec ->
          listOf(
              rec.memberDocId,
              rec.memberId,
              rec.memberCode,
              rec.userId,
              rec.barcodeToken,
            )
            .forEach { raw ->
              val clean = raw.trim().uppercase()
              if (clean.isNotEmpty()) presentKeys.add(clean)
            }
        }
        activeChurchMembers
          .filter { mem ->
            val memKeys =
              listOfNotNull(
                  mem.id,
                  mem.memberId,
                  mem.effectiveMatrikil,
                  mem.userId,
                  mem.barcodeToken,
                )
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
            memKeys.none { it in presentKeys }
          }
          .sortedBy { it.effectiveFullName.lowercase() }
      }
    }

  val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(top = 6.dp)
        .testTag("scanner_service_history_section"),
  ) {
    HorizontalDivider(color = GlassWhite.copy(alpha = 0.12f), thickness = 0.75.dp)
    Spacer(modifier = Modifier.height(12.dp))

    // Borderless Header Row: Title + Compact Service Picker
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.History,
          contentDescription = null,
          tint = BlueNeon,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = copy.sectionTitle,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (inspectedService != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable(enabled = orderedServices.size > 1) { showServicePicker = true }
              .padding(horizontal = 6.dp, vertical = 6.dp)
              .testTag("scanner_history_service_picker_btn"),
        ) {
          if (inspectedService.isOpenForAttendance && !inspectedService.isPermanentlyEnded) {
            Box(
              modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen)
            )
            Spacer(modifier = Modifier.width(5.dp))
          }
          Text(
            text = inspectedService.effectiveTitle,
            style = MaterialTheme.typography.labelMedium,
            color = BlueNeon,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = if (orderedServices.size > 1) 2.dp else 0.dp),
          )
          if (orderedServices.size > 1) {
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = copy.pickServiceTitle,
              tint = BlueNeon,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }

    if (inspectedService == null) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = copy.emptyServiceLabel,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
        modifier = Modifier.padding(vertical = 8.dp),
      )
      return@Column
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 4 Small Borderless Tabs: Prezans | Reta | Absans | Vizitè
    Row(
      modifier = Modifier.fillMaxWidth().testTag("scanner_history_tabs_row"),
      horizontalArrangement = Arrangement.spacedBy(2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      BorderlessHistoryTab(
        label = copy.tabPresent,
        count = regularRecords.size,
        selected = selectedTab == ScannerHistoryTab.PRESENT,
        accentColor = SuccessGreen,
        onClick = { selectedTab = ScannerHistoryTab.PRESENT },
        testTag = "scanner_history_tab_${ScannerHistoryTab.PRESENT.tagSuffix}",
        modifier = Modifier.weight(1f),
      )
      BorderlessHistoryTab(
        label = copy.tabLate,
        count = lateRecords.size,
        selected = selectedTab == ScannerHistoryTab.LATE,
        accentColor = BrandGold,
        onClick = { selectedTab = ScannerHistoryTab.LATE },
        testTag = "scanner_history_tab_${ScannerHistoryTab.LATE.tagSuffix}",
        modifier = Modifier.weight(1f),
      )
      BorderlessHistoryTab(
        label = copy.tabAbsent,
        count = absentMembers.size,
        selected = selectedTab == ScannerHistoryTab.ABSENT,
        accentColor = ErrorRed,
        onClick = { selectedTab = ScannerHistoryTab.ABSENT },
        testTag = "scanner_history_tab_${ScannerHistoryTab.ABSENT.tagSuffix}",
        modifier = Modifier.weight(1f),
      )
      BorderlessHistoryTab(
        label = copy.tabVisitor,
        count = visitorRecords.size,
        selected = selectedTab == ScannerHistoryTab.VISITOR,
        accentColor = BlueNeon,
        onClick = { selectedTab = ScannerHistoryTab.VISITOR },
        testTag = "scanner_history_tab_${ScannerHistoryTab.VISITOR.tagSuffix}",
        modifier = Modifier.weight(1f),
      )
    }

    HorizontalDivider(color = GlassWhite.copy(alpha = 0.08f), thickness = 0.75.dp)
    Spacer(modifier = Modifier.height(4.dp))

    // Borderless Tab Content
    when (selectedTab) {
      ScannerHistoryTab.PRESENT -> {
        if (regularRecords.isEmpty()) {
          BorderlessEmptyNotice(copy.emptyPresent)
        } else {
          val displayed =
            if (isExpanded) regularRecords else regularRecords.take(DEFAULT_VISIBLE_ROWS)
          displayed.forEachIndexed { index, rec ->
            val isLate =
              rec.effectiveStatus.equals("late", ignoreCase = true) ||
                rec.effectiveStatus.equals("an_reta", ignoreCase = true)
            BorderlessAttendanceRow(
              fullName = rec.memberName.ifBlank { rec.effectiveMemberCode },
              photoUrl = rec.memberPhotoUrl,
              subtitle = buildRecordSubtitle(rec, copy),
              trailingTop = formatRecordTime(rec, timeFormatter),
              trailingStatus = if (isLate) copy.statusLate else copy.statusOnTime,
              statusColor = if (isLate) BrandGold else SuccessGreen,
              showDivider = index < displayed.lastIndex,
            )
          }
          if (regularRecords.size > DEFAULT_VISIBLE_ROWS) {
            BorderlessExpandToggle(
              isExpanded = isExpanded,
              totalCount = regularRecords.size,
              copy = copy,
              onToggle = { isExpanded = !isExpanded },
            )
          }
        }
      }
      ScannerHistoryTab.LATE -> {
        if (lateRecords.isEmpty()) {
          BorderlessEmptyNotice(copy.emptyLate)
        } else {
          val displayed = if (isExpanded) lateRecords else lateRecords.take(DEFAULT_VISIBLE_ROWS)
          displayed.forEachIndexed { index, rec ->
            BorderlessAttendanceRow(
              fullName = rec.memberName.ifBlank { rec.effectiveMemberCode },
              photoUrl = rec.memberPhotoUrl,
              subtitle = buildRecordSubtitle(rec, copy),
              trailingTop = formatRecordTime(rec, timeFormatter),
              trailingStatus = copy.statusLate,
              statusColor = BrandGold,
              showDivider = index < displayed.lastIndex,
            )
          }
          if (lateRecords.size > DEFAULT_VISIBLE_ROWS) {
            BorderlessExpandToggle(
              isExpanded = isExpanded,
              totalCount = lateRecords.size,
              copy = copy,
              onToggle = { isExpanded = !isExpanded },
            )
          }
        }
      }
      ScannerHistoryTab.ABSENT -> {
        if (absentMembers.isEmpty()) {
          BorderlessEmptyNotice(copy.emptyAbsent)
        } else {
          val isServiceStillOpen =
            inspectedService.isOpenForAttendance && !inspectedService.isPermanentlyEnded
          val displayed =
            if (isExpanded) absentMembers else absentMembers.take(DEFAULT_VISIBLE_ROWS)
          displayed.forEachIndexed { index, member ->
            val subtitle =
              listOfNotNull(
                  member.effectiveMatrikil.takeIf { it.isNotBlank() },
                  member.department.takeIf { it.isNotBlank() },
                )
                .joinToString(" • ")
            BorderlessAttendanceRow(
              fullName = member.effectiveFullName,
              photoUrl = member.effectivePhotoUrl,
              subtitle = subtitle,
              trailingTop = null,
              trailingStatus =
                if (isServiceStillOpen) copy.statusNotCheckedInYet else copy.statusAbsent,
              statusColor = if (isServiceStillOpen) MutedGlassText else ErrorRed,
              showDivider = index < displayed.lastIndex,
            )
          }
          if (absentMembers.size > DEFAULT_VISIBLE_ROWS) {
            BorderlessExpandToggle(
              isExpanded = isExpanded,
              totalCount = absentMembers.size,
              copy = copy,
              onToggle = { isExpanded = !isExpanded },
            )
          }
        }
      }
      ScannerHistoryTab.VISITOR -> {
        if (visitorRecords.isEmpty()) {
          BorderlessEmptyNotice(copy.emptyVisitor)
        } else {
          val displayed =
            if (isExpanded) visitorRecords else visitorRecords.take(DEFAULT_VISIBLE_ROWS)
          displayed.forEachIndexed { index, rec ->
            val originChurch =
              rec.homeChurchName?.takeIf { it.isNotBlank() }
                ?: rec.homeChurchId?.takeIf { it.isNotBlank() }
                ?: rec.effectiveMemberCode
            BorderlessAttendanceRow(
              fullName = rec.memberName.ifBlank { rec.effectiveMemberCode },
              photoUrl = rec.memberPhotoUrl,
              subtitle = originChurch,
              trailingTop = formatRecordTime(rec, timeFormatter),
              trailingStatus = copy.statusVisitor,
              statusColor = BlueNeon,
              showDivider = index < displayed.lastIndex,
            )
          }
          if (visitorRecords.size > DEFAULT_VISIBLE_ROWS) {
            BorderlessExpandToggle(
              isExpanded = isExpanded,
              totalCount = visitorRecords.size,
              copy = copy,
              onToggle = { isExpanded = !isExpanded },
            )
          }
        }
      }
    }
  }

  if (showServicePicker) {
    GlassModalBottomSheet(
      onDismissRequest = { showServicePicker = false },
      title = copy.pickServiceTitle,
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
      ) {
        items(orderedServices, key = { it.id }) { srv ->
          val isSelected = srv.id == inspectedService?.id
          val isLiveOpen = srv.isOpenForAttendance && !srv.isPermanentlyEnded
          Column {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    manuallyPickedServiceId = srv.id
                    showServicePicker = false
                  }
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
}

@Composable
private fun BorderlessHistoryTab(
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
private fun BorderlessAttendanceRow(
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
private fun BorderlessEmptyNotice(message: String) {
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
private fun BorderlessExpandToggle(
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

private fun formatRecordTime(
  record: AttendanceRecord,
  formatter: SimpleDateFormat,
): String {
  val millis = record.createdAt?.toDate()?.time ?: record.timestamp
  if (millis <= 0L) return ""
  return formatter.format(Date(millis))
}

private fun buildRecordSubtitle(
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
