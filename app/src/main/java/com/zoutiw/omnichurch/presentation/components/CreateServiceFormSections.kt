package com.zoutiw.omnichurch.presentation.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.util.Calendar
import java.util.Locale

@Composable
internal fun CreateServiceScheduleSection(
  date: String,
  onDateChange: (String) -> Unit,
  startTime: String,
  startIsPm: Boolean,
  onStartTimeChange: (String, Boolean) -> Unit,
  endTime: String,
  endIsPm: Boolean,
  onEndTimeChange: (String, Boolean) -> Unit,
  copy: CreateServiceCopy,
  languageCode: String = "ht",
) {
  val context = LocalContext.current

  SectionTitle(title = copy.scheduleSectionTitle)
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 3a. Dat Sèvis la
    val openDatePicker = {
      val cal = Calendar.getInstance()
      val parts = date.split("-")
      if (parts.size == 3) {
        val y = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()
        val d = parts[2].toIntOrNull()
        if (y != null && m != null && d != null && m in 1..12) {
          cal.set(y, m - 1, d)
        }
      }
      DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
          onDateChange(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth))
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH),
      ).show()
    }

    MinimalTextField(
      value = date,
      onValueChange = onDateChange,
      label = copy.dateLabel,
      placeholder = copy.datePlaceholder,
      leadingIcon = Icons.Default.CalendarMonth,
      trailingActionIcon = Icons.Default.CalendarMonth,
      trailingActionDescription = copy.pickCalendarCd,
      onTrailingActionClick = openDatePicker,
      keyboardType = KeyboardType.Number,
      modifier = Modifier.fillMaxWidth(),
      testTag = "service_input_date",
    )

    val naturalCreoleDate = remember(date, languageCode, copy.serviceDateFallback) {
      com.zoutiw.omnichurch.data.models.formatLocalizedServiceDate(date, languageCode)
        .ifBlank { date.trim().ifBlank { copy.serviceDateFallback } }
    }
    if (naturalCreoleDate.isNotBlank() && naturalCreoleDate != date) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0284C7).copy(alpha = 0.15f))
            .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Event,
          contentDescription = null,
          tint = Color(0xFF38BDF8),
          modifier = Modifier.size(15.dp),
        )
        Text(
          text = naturalCreoleDate,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFE0F2FE),
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    // 3b. Lè Kòmanse + Seleksyon AM / PM
    val openStartTimePicker = {
      val (h24, m) = parseHourMinute24(startTime, startIsPm, defaultHour24 = 9, defaultMinute = 0)
      TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
          val (formatted12, pmFlag) = format24HourTo12HourParts(hourOfDay, minute)
          onStartTimeChange(formatted12, pmFlag)
        },
        h24,
        m,
        false,
      ).show()
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      MinimalTextField(
        value = startTime,
        onValueChange = { input ->
          val trimmedUpper = input.trim().uppercase(Locale.US)
          when {
            trimmedUpper.endsWith("PM") ->
              onStartTimeChange(input.replace(Regex("(?i)\\s*pm$"), "").trim(), true)
            trimmedUpper.endsWith("AM") ->
              onStartTimeChange(input.replace(Regex("(?i)\\s*am$"), "").trim(), false)
            else -> onStartTimeChange(input, startIsPm)
          }
        },
        label = copy.startTimeLabel,
        placeholder = copy.startTimePlaceholder,
        leadingIcon = Icons.Default.AccessTime,
        trailingActionIcon = Icons.Default.Schedule,
        trailingActionDescription = copy.pickStartTimeCd,
        onTrailingActionClick = openStartTimePicker,
        keyboardType = KeyboardType.Number,
        modifier = Modifier.weight(1f),
        testTag = "service_input_start_time",
      )
      AmPmSegmentedToggle(
        isPm = startIsPm,
        onSelectPm = { onStartTimeChange(startTime, it) },
        testTagPrefix = "service_start_ampm",
        modifier = Modifier.padding(top = 6.dp),
      )
    }

    // 3c. Lè Fini + Seleksyon AM / PM
    val openEndTimePicker = {
      val (h24, m) = parseHourMinute24(endTime, endIsPm, defaultHour24 = 11, defaultMinute = 30)
      TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
          val (formatted12, pmFlag) = format24HourTo12HourParts(hourOfDay, minute)
          onEndTimeChange(formatted12, pmFlag)
        },
        h24,
        m,
        false,
      ).show()
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      MinimalTextField(
        value = endTime,
        onValueChange = { input ->
          val trimmedUpper = input.trim().uppercase(Locale.US)
          when {
            trimmedUpper.endsWith("PM") ->
              onEndTimeChange(input.replace(Regex("(?i)\\s*pm$"), "").trim(), true)
            trimmedUpper.endsWith("AM") ->
              onEndTimeChange(input.replace(Regex("(?i)\\s*am$"), "").trim(), false)
            else -> onEndTimeChange(input, endIsPm)
          }
        },
        label = copy.endTimeLabel,
        placeholder = copy.endTimePlaceholder,
        leadingIcon = Icons.Default.AccessTime,
        trailingActionIcon = Icons.Default.Schedule,
        trailingActionDescription = copy.pickEndTimeCd,
        onTrailingActionClick = openEndTimePicker,
        keyboardType = KeyboardType.Number,
        modifier = Modifier.weight(1f),
        testTag = "service_input_end_time",
      )
      AmPmSegmentedToggle(
        isPm = endIsPm,
        onSelectPm = { onEndTimeChange(endTime, it) },
        testTagPrefix = "service_end_ampm",
        modifier = Modifier.padding(top = 6.dp),
      )
    }

    // Live Schedule Preview (Dat + Lè AM/PM)
    val startPreviewLabel = formatDisplay12HourLabel(startTime, startIsPm)
    val endPreviewLabel = formatDisplay12HourLabel(endTime, endIsPm)
    val timeRangePreview =
      when {
        startPreviewLabel.isNotBlank() && endPreviewLabel.isNotBlank() ->
          "$startPreviewLabel - $endPreviewLabel"
        startPreviewLabel.isNotBlank() -> startPreviewLabel
        else -> ""
      }
    if (timeRangePreview.isNotBlank()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF59E0B).copy(alpha = 0.13f))
            .border(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.42f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("service_time_ampm_preview"),
      ) {
        Icon(
          imageVector = Icons.Default.Schedule,
          contentDescription = null,
          tint = Color(0xFFFBBF24),
          modifier = Modifier.size(15.dp),
        )
        Text(
          text =
            if (naturalCreoleDate.isNotBlank() && naturalCreoleDate != date)
              "$naturalCreoleDate • $timeRangePreview"
            else timeRangePreview,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFFEF3C7),
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
internal fun CreateServiceAttendanceAndMediaSection(
  isAttendanceLocked: Boolean,
  isOpenForAttendance: Boolean,
  onOpenForAttendanceChange: (Boolean) -> Unit,
  sendReminder24h: Boolean,
  onSendReminder24hChange: (Boolean) -> Unit,
  lateToleranceMinutesStr: String,
  onLateToleranceChange: (String) -> Unit,
  title: String,
  imageUrl: String,
  onImageUrlChange: (String) -> Unit,
  copy: CreateServiceCopy,
  isBeforeScheduledStart: Boolean = false,
  startTimePreview: String = "",
  languageCode: String = "ht",
) {
  SectionTitle(title = copy.attendanceSectionTitle)

  val beforeStartDesc =
    when (languageCode.trim().lowercase(Locale.US)) {
      "fr" ->
        if (startTimePreview.isNotBlank()) "Bloqué avant le début du service ($startTimePreview)."
        else "Bloqué avant l'heure prévue du service."
      "es" ->
        if (startTimePreview.isNotBlank()) "Bloqueado antes del inicio del servicio ($startTimePreview)."
        else "Bloqueado antes de la hora programada."
      "en" ->
        if (startTimePreview.isNotBlank()) "Locked until the scheduled start time ($startTimePreview)."
        else "Locked until the scheduled start time."
      else ->
        if (startTimePreview.isNotBlank()) "Bloke anvan lè sèvis la kòmanse ($startTimePreview)."
        else "Bloke anvan lè sèvis la pwograme a."
    }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF1E293B).copy(alpha = 0.5f))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = copy.openAttendanceTitle,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text =
          when {
            isAttendanceLocked -> copy.openAttendanceLockedDesc
            isBeforeScheduledStart -> beforeStartDesc
            else -> copy.openAttendanceActiveDesc
          },
        style = MaterialTheme.typography.bodySmall,
        color = if (isBeforeScheduledStart) Color(0xFFFBBF24) else Color(0xFF94A3B8),
      )
    }
    Switch(
      checked = isOpenForAttendance && !isBeforeScheduledStart,
      enabled = !isAttendanceLocked && !isBeforeScheduledStart,
      onCheckedChange = onOpenForAttendanceChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = PureWhite,
        checkedTrackColor = SuccessGreen,
        uncheckedThumbColor = Color(0xFF64748B),
        uncheckedTrackColor = Color(0xFF1E293B),
      ),
      modifier = Modifier.testTag("service_toggle_open_attendance"),
    )
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF1E293B).copy(alpha = 0.5f))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = copy.reminderTitle,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = copy.reminderDesc,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF94A3B8),
      )
    }
    Switch(
      checked = sendReminder24h,
      onCheckedChange = onSendReminder24hChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = PureWhite,
        checkedTrackColor = Color(0xFFF59E0B),
        uncheckedThumbColor = Color(0xFF64748B),
        uncheckedTrackColor = Color(0xFF1E293B),
      ),
      modifier = Modifier.testTag("service_toggle_reminder"),
    )
  }

  MinimalTextField(
    value = lateToleranceMinutesStr,
    onValueChange = onLateToleranceChange,
    label = copy.toleranceLabel,
    placeholder = "15",
    keyboardType = KeyboardType.Number,
    testTag = "service_input_tolerance",
  )

  SectionTitle(title = copy.mediaSectionTitle)
  MinimalTextField(
    value = if (imageUrl.startsWith("data:image")) "" else imageUrl,
    onValueChange = onImageUrlChange,
    label = if (imageUrl.startsWith("data:image")) copy.imageGalleryImportedLabel else copy.imageUrlLabel,
    placeholder = "https://...",
    leadingIcon = Icons.Default.Image,
    trailingActionIcon = if (imageUrl.isNotBlank()) Icons.Default.DeleteOutline else null,
    trailingActionDescription = copy.removeImageCd,
    onTrailingActionClick = { onImageUrlChange("") },
    testTag = "service_input_image_url",
  )

  if (imageUrl.isNotBlank()) {
    AnnouncementMediaShowcase(
      imageSource = imageUrl,
      title = title.ifBlank { copy.posterPreviewTitle },
      announcementId = "preview_srv",
      onImageClick = {},
      useFitScale = true,
      modifier = Modifier.testTag("service_image_preview"),
    )
  }
}

@Composable
internal fun CreateServiceStickyFooter(
  isEditing: Boolean,
  isCompressingImage: Boolean,
  imageUrl: String,
  canSubmit: Boolean,
  copy: CreateServiceCopy,
  onPickGalleryImage: () -> Unit,
  onSubmitClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF0F172A))
      .border(width = 1.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(0.dp))
      .padding(horizontal = 20.dp, vertical = 14.dp)
      .navigationBarsPadding(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    OutlinedButton(
      onClick = onPickGalleryImage,
      enabled = !isCompressingImage,
      border = BorderStroke(
        width = 1.dp,
        color = if (imageUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.7f) else Color(0xFF38BDF8).copy(alpha = 0.55f),
      ),
      colors = ButtonDefaults.outlinedButtonColors(
        containerColor = if (imageUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.14f) else Color(0xFF1E293B),
        contentColor = PureWhite,
      ),
      shape = RoundedCornerShape(12.dp),
      contentPadding = PaddingValues(horizontal = 14.dp),
      modifier = Modifier
        .height(48.dp)
        .testTag("service_pick_gallery_image_btn"),
    ) {
      if (isCompressingImage) {
        CircularProgressIndicator(
          modifier = Modifier.size(16.dp),
          strokeWidth = 2.dp,
          color = Color(0xFF38BDF8),
        )
      } else {
        Icon(
          imageVector = if (imageUrl.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
          contentDescription = copy.importImageCd,
          tint = if (imageUrl.isNotBlank()) SuccessGreen else Color(0xFF38BDF8),
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = when {
          isCompressingImage -> copy.processingImageBtn
          imageUrl.isNotBlank() -> copy.imageReadyBtn
          else -> copy.importImageBtn
        },
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
    }

    Button(
      onClick = onSubmitClick,
      enabled = canSubmit,
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFF59E0B), // Amber-500
        contentColor = Color(0xFF0F172A), // Slate-950
        disabledContainerColor = Color(0xFF334155),
        disabledContentColor = Color(0xFF64748B),
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .weight(1f)
        .height(48.dp)
        .testTag("service_bottom_sheet_submit_btn"),
    ) {
      Icon(
        imageVector = Icons.Default.AutoAwesome,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (isEditing) copy.submitEditBtn else copy.submitCreateBtn,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
      )
    }
  }
}
