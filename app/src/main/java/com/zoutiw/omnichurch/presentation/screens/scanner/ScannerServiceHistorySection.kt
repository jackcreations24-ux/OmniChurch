package com.zoutiw.omnichurch.presentation.screens.scanner

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private const val DEFAULT_VISIBLE_ROWS = 12

internal fun resolveScannerServiceAbsentMembers(
  service: ChurchService?,
  regularRecords: List<AttendanceRecord>,
  activeChurchMembersWithKeys: List<Pair<Member, List<String>>>,
  nowMillis: Long = System.currentTimeMillis(),
): List<Member> {
  if (service == null || !service.isFinishedForAbsence(nowMillis) || activeChurchMembersWithKeys.isEmpty()) {
    return emptyList()
  }
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
  return activeChurchMembersWithKeys.mapNotNull { (mem, memKeys) ->
    if (memKeys.none { it in presentKeys }) mem else null
  }
}

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
  nowMillis: Long = System.currentTimeMillis(),
  modifier: Modifier = Modifier,
) {
  val copy = remember(languageCode) { ScannerHistoryStrings.forLanguage(languageCode) }

  // Combine activeService + allServices deduplicated by ID:
  // 1) Live active service first, 2) Past/started services newest first, 3) Future unstarted services last
  val orderedServices =
    remember(activeService, allServices, nowMillis) {
      val map = LinkedHashMap<String, ChurchService>()
      if (activeService != null && activeService.id.isNotBlank()) {
        map[activeService.id] = activeService
      }
      allServices
        .sortedWith(
          compareByDescending<ChurchService> { it.isLiveBadgeActive(nowMillis) }
            .thenByDescending { !it.isBeforeStart(nowMillis) }
            .thenByDescending { it.scheduledStartMillis() ?: it.startTimeEpochMillis }
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

  // Default to live active service (or latest past/started service) unless user explicitly picks another
  val inspectedService =
    remember(orderedServices, activeService?.id, manuallyPickedServiceId, nowMillis) {
      val picked =
        manuallyPickedServiceId?.let { id -> orderedServices.firstOrNull { it.id == id } }
      picked
        ?: activeService
        ?: orderedServices.firstOrNull { !it.isBeforeStart(nowMillis) }
        ?: orderedServices.firstOrNull()
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

  val activeChurchMembersWithKeys =
    remember(churchMembers) {
      churchMembers
        .filter { member ->
          !member.id.startsWith("ADMIN-", ignoreCase = true) &&
            !member.status.equals("inactive", ignoreCase = true) &&
            !member.status.equals("suspended", ignoreCase = true)
        }
        .sortedBy { it.effectiveFullName.lowercase() }
        .map { mem ->
          val keys =
            listOfNotNull(
                mem.id,
                mem.memberId,
                mem.effectiveMatrikil,
                mem.userId,
                mem.barcodeToken,
              )
              .map { it.trim().uppercase() }
              .filter { it.isNotEmpty() }
          mem to keys
        }
    }

  val hasServicePassedForAbsence =
    remember(inspectedService, nowMillis) {
      inspectedService?.isFinishedForAbsence(nowMillis) == true
    }

  val absentMembers =
    remember(inspectedService, regularRecords, activeChurchMembersWithKeys, nowMillis) {
      resolveScannerServiceAbsentMembers(
        service = inspectedService,
        regularRecords = regularRecords,
        activeChurchMembersWithKeys = activeChurchMembersWithKeys,
        nowMillis = nowMillis,
      )
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
            key(rec.id.ifBlank { "${rec.effectiveMemberDbId}_${rec.timestamp}_$index" }) {
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
            key(rec.id.ifBlank { "${rec.effectiveMemberDbId}_${rec.timestamp}_$index" }) {
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
        if (!hasServicePassedForAbsence) {
          BorderlessEmptyNotice(copy.emptyAbsentPendingService)
        } else if (absentMembers.isEmpty()) {
          BorderlessEmptyNotice(copy.emptyAbsent)
        } else {
          val displayed =
            if (isExpanded) absentMembers else absentMembers.take(DEFAULT_VISIBLE_ROWS)
          displayed.forEachIndexed { index, member ->
            key(member.id.ifBlank { "${member.effectiveMatrikil}_$index" }) {
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
                trailingStatus = copy.statusAbsent,
                statusColor = ErrorRed,
                showDivider = index < displayed.lastIndex,
              )
            }
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
            key(rec.id.ifBlank { "${rec.effectiveMemberDbId}_${rec.timestamp}_$index" }) {
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
    ScannerServicePickerSheet(
      orderedServices = orderedServices,
      inspectedServiceId = inspectedService?.id,
      copy = copy,
      onSelectService = { id ->
        manuallyPickedServiceId = id
        showServicePicker = false
      },
      onDismiss = { showServicePicker = false },
    )
  }
}
