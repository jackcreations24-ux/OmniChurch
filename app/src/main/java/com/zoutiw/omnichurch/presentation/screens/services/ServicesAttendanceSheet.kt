package com.zoutiw.omnichurch.presentation.screens.services

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.Flow

@Composable
internal fun ServiceAttendanceBottomSheet(
  service: ChurchService,
  churchMembers: List<Member>,
  observeAttendances: (serviceId: String) -> Flow<List<AttendanceRecord>>,
  onDismiss: () -> Unit,
) {
  val records by
    observeAttendances(service.id).collectAsStateWithLifecycle(initialValue = emptyList())
  var activeFilter by remember(service.id) { mutableStateOf("tout") }

  val regularRecords = remember(records) { records.filter { !it.isCrossChurchVisitor } }
  val visitorRecords = remember(records) { records.filter { it.isCrossChurchVisitor } }
  val regularMembers = remember(churchMembers) {
    churchMembers.filter { !it.id.startsWith("ADMIN-", ignoreCase = true) }
  }

  val onTimeCount = remember(regularRecords) {
    regularRecords.count {
      it.effectiveStatus.equals("on_time", ignoreCase = true) ||
        it.effectiveStatus.equals("present", ignoreCase = true)
    }
  }
  val lateCount = remember(regularRecords) {
    regularRecords.count {
      it.effectiveStatus.equals("late", ignoreCase = true) ||
        it.effectiveStatus.equals("an_reta", ignoreCase = true)
    }
  }
  val presentCount = regularRecords.size

  val absentMembers = remember(service, regularRecords, regularMembers) {
    if (!service.isFinishedForAbsence() || regularRecords.isEmpty()) {
      emptyList()
    } else {
      val accountedKeys = mutableSetOf<String>()
      regularRecords.forEach { rec ->
        listOf(
            rec.effectiveMemberDbId,
            rec.memberDocId,
            rec.memberId,
            rec.memberCode,
            rec.userId,
            rec.barcodeToken,
          )
          .map { it.trim().uppercase() }
          .filter { it.isNotEmpty() }
          .forEach { accountedKeys.add(it) }
      }
      regularMembers
        .filter { mem ->
          val memKeys =
            listOfNotNull(
                mem.id,
                mem.effectiveMatrikil,
                mem.memberCode,
                mem.memberId,
                mem.userId,
                mem.effectiveBarcodeToken,
              )
              .map { it.trim().uppercase() }
              .filter { it.isNotEmpty() }
          memKeys.none { it in accountedKeys }
        }
        .sortedBy { it.fullName.lowercase() }
    }
  }
  val absentCount = absentMembers.size
  val ratePercent =
    if (regularMembers.isEmpty()) 0
    else ((presentCount * 100.0) / regularMembers.size).toInt().coerceIn(0, 100)

  val filteredAttendanceRecords = remember(records, regularRecords, visitorRecords, activeFilter) {
    when (activeFilter) {
      "ale" -> regularRecords.filter {
        !it.effectiveStatus.equals("late", ignoreCase = true) &&
          !it.effectiveStatus.equals("an_reta", ignoreCase = true)
      }
      "anreta" -> regularRecords.filter {
        it.effectiveStatus.equals("late", ignoreCase = true) ||
          it.effectiveStatus.equals("an_reta", ignoreCase = true)
      }
      "vizite" -> visitorRecords
      "absan" -> emptyList()
      else -> records
    }
  }
  val showAbsentList = activeFilter == "tout" || activeFilter == "absan"

  GlassModalBottomSheet(
    onDismissRequest = onDismiss,
    title = "${service.effectiveTitle} (${presentCount + visitorRecords.size})",
  ) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0x660B1D3D))
          .border(0.8.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
          .padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      ServiceAttendanceKpiCell(
        label = "Prezan",
        value = presentCount.toString(),
        subValue = if (visitorRecords.isNotEmpty()) "+${visitorRecords.size} Vizitè" else null,
        color = SuccessGreen,
        modifier = Modifier.weight(1f),
      )
      ServiceAttendanceKpiCell(
        label = "Alè",
        value = onTimeCount.toString(),
        color = ElectricBlue,
        modifier = Modifier.weight(0.9f),
      )
      ServiceAttendanceKpiCell(
        label = "Anreta",
        value = lateCount.toString(),
        color = BrandGold,
        modifier = Modifier.weight(0.9f),
      )
      ServiceAttendanceKpiCell(
        label = "Absan",
        value = absentCount.toString(),
        color = ErrorRed,
        modifier = Modifier.weight(0.9f),
      )
      ServiceAttendanceKpiCell(
        label = "To %",
        value = "$ratePercent%",
        color = PureWhite,
        modifier = Modifier.weight(0.9f),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    val filterOptions = listOf(
      "tout" to "Tout",
      "ale" to "Alè",
      "anreta" to "Anreta",
      "absan" to "Absan",
      "vizite" to "Vizitè (${visitorRecords.size})",
    )
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(filterOptions, key = { it.first }) { (key, label) ->
        val isSelected = activeFilter == key
        val accent = if (key == "vizite") BlueNeon else ElectricBlue
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(if (isSelected) accent.copy(alpha = 0.30f) else Color(0x550B1D3D))
              .border(
                1.dp,
                if (isSelected) accent else GlassWhite.copy(alpha = 0.22f),
                RoundedCornerShape(50),
              )
              .clickable { activeFilter = key }
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("service_attendance_filter_$key"),
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) PureWhite else MutedGlassText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (filteredAttendanceRecords.isEmpty() && (!showAbsentList || absentMembers.isEmpty())) {
      Text(
        text =
          when (activeFilter) {
            "vizite" -> "Poko gen okenn vizitè ki eskane nan sèvis sa a."
            "absan" ->
              if (!service.isFinishedForAbsence()) {
                "Sèvis sa a poko pase. Absans yo parèt sèlman pou istwa sèvis ki deja pase."
              } else {
                "Pa gen okenn manm absan pou sèvis sa a."
              }
            else -> "Poko gen prezans anrejistre pou sèvis sa a."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = MutedGlassText,
        modifier = Modifier.padding(vertical = 16.dp),
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(filteredAttendanceRecords, key = { it.id }) { rec ->
          val isOnTime = !rec.effectiveStatus.equals("late", ignoreCase = true) &&
            !rec.effectiveStatus.equals("an_reta", ignoreCase = true)
          val isVisitor = rec.isCrossChurchVisitor
          val originChurch =
            rec.homeChurchName?.takeIf { it.isNotBlank() } ?: rec.homeChurchId.orEmpty()
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x660B1D3D))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              MemberAvatar3D(fullName = rec.memberName, size = 38.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = rec.memberName,
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  if (isVisitor) {
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(50))
                          .background(ElectricBlue.copy(alpha = 0.25f))
                          .border(0.8.dp, BlueNeon.copy(alpha = 0.8f), RoundedCornerShape(50))
                          .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                      Text(
                        text = "Vizitè",
                        style = MaterialTheme.typography.labelSmall,
                        color = BlueNeon,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
                Text(
                  text =
                    if (isVisitor && originChurch.isNotBlank()) {
                      "Soti nan: $originChurch • ${rec.effectiveMemberCode}"
                    } else {
                      rec.effectiveMemberCode
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isVisitor) BlueNeon.copy(alpha = 0.9f) else MutedGlassText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(50))
                  .background(
                    (if (isOnTime) SuccessGreen else BrandOrange).copy(alpha = 0.25f)
                  )
                  .border(
                    1.dp,
                    if (isOnTime) SuccessGreen else BrandOrange,
                    RoundedCornerShape(50),
                  )
                  .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
              Text(
                text =
                  if (isOnTime) stringResource(R.string.attendance_status_on_time)
                  else stringResource(R.string.attendance_status_late),
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }

        if (showAbsentList) {
          items(absentMembers, key = { "abs_${it.id}" }) { mem ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x4D2A0D18))
                  .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                MemberAvatar3D(fullName = mem.effectiveFullName, size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = mem.effectiveFullName,
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = mem.effectiveMatrikil,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGlassText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(50))
                    .background(ErrorRed.copy(alpha = 0.25f))
                    .border(1.dp, ErrorRed, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "Absan",
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ServiceAttendanceKpiCell(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier,
  subValue: String? = null,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      color = color,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MutedGlassText,
      maxLines = 1,
    )
    if (!subValue.isNullOrBlank()) {
      Text(
        text = subValue,
        style = MaterialTheme.typography.labelSmall,
        color = BlueNeon,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
