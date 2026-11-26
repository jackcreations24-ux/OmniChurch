package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.ui.theme.PureWhite
import java.util.Locale

@Composable
internal fun SectionTitle(title: String) {
  Text(
    text = title.uppercase(Locale.getDefault()),
    style = MaterialTheme.typography.labelSmall.copy(
      fontSize = 11.sp,
      letterSpacing = 1.sp,
      fontWeight = FontWeight.Bold,
    ),
    color = Color(0xFF94A3B8), // Slate-400
  )
}

@Composable
internal fun CategoryChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(
        if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.22f)
        else Color(0xFF1E293B).copy(alpha = 0.6f)
      )
      .border(
        width = 1.dp,
        color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155).copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
      )
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
    )
  }
}

@Composable
internal fun MemberRoleSelectorField(
  value: String,
  label: String,
  placeholder: String,
  leadingIcon: ImageVector? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier.fillMaxWidth(),
  testTag: String = "",
) {
  val isGuest =
    listOf("Envite", "Invité", "Guest", "Invitado").any { value.contains(it, ignoreCase = true) }
  Box(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      readOnly = true,
      label = { Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      placeholder = {
        Text(
          text = placeholder,
          color = Color(0xFF64748B),
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      leadingIcon = leadingIcon?.let {
        {
          Icon(
            imageVector = if (isGuest) Icons.Default.Badge else it,
            contentDescription = null,
            tint = if (isGuest) Color(0xFFF59E0B) else Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp),
          )
        }
      },
      trailingIcon = {
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = label,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(20.dp),
        )
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.72f),
        unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.55f),
        focusedBorderColor = Color(0xFFF59E0B).copy(alpha = 0.7f),
        unfocusedBorderColor = if (value.isNotBlank()) Color(0xFF38BDF8).copy(alpha = 0.55f) else Color(0xFF334155).copy(alpha = 0.6f),
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedLabelColor = Color(0xFFFBBF24),
        unfocusedLabelColor = Color(0xFF94A3B8),
      ),
      modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .padding(top = 6.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
    )
  }
}

@Composable
internal fun MinimalTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String = "",
  leadingIcon: ImageVector? = null,
  trailingActionIcon: ImageVector? = null,
  trailingActionDescription: String? = null,
  onTrailingActionClick: (() -> Unit)? = null,
  singleLine: Boolean = true,
  keyboardType: KeyboardType = KeyboardType.Text,
  modifier: Modifier = Modifier.fillMaxWidth(),
  testTag: String = "",
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
    placeholder = {
      if (placeholder.isNotBlank()) {
        Text(placeholder, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
      }
    },
    leadingIcon = leadingIcon?.let {
      {
        Icon(
          imageVector = it,
          contentDescription = null,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(18.dp),
        )
      }
    },
    trailingIcon = if (trailingActionIcon != null && onTrailingActionClick != null) {
      {
        IconButton(onClick = onTrailingActionClick) {
          Icon(
            imageVector = trailingActionIcon,
            contentDescription = trailingActionDescription,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    } else null,
    singleLine = singleLine,
    keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.Sentences,
      keyboardType = keyboardType,
    ),
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = Color(0xFF1E293B),
      unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.55f),
      focusedBorderColor = Color(0xFFF59E0B).copy(alpha = 0.7f),
      unfocusedBorderColor = Color(0xFF334155).copy(alpha = 0.6f),
      focusedTextColor = PureWhite,
      unfocusedTextColor = PureWhite,
      focusedLabelColor = Color(0xFFFBBF24),
      unfocusedLabelColor = Color(0xFF94A3B8),
      cursorColor = Color(0xFFF59E0B),
    ),
    modifier = modifier.testTag(testTag),
  )
}

@Composable
internal fun AmPmSegmentedToggle(
  isPm: Boolean,
  onSelectPm: (Boolean) -> Unit,
  testTagPrefix: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .height(54.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF1E293B).copy(alpha = 0.72f))
        .border(1.dp, Color(0xFF334155).copy(alpha = 0.75f), RoundedCornerShape(12.dp))
        .padding(3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    val segments = listOf(false to "AM", true to "PM")
    segments.forEach { (pmValue, label) ->
      val selected = isPm == pmValue
      Box(
        modifier =
          Modifier
            .width(46.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
              if (selected) Color(0xFFF59E0B).copy(alpha = 0.24f) else Color.Transparent
            )
            .border(
              width = if (selected) 1.dp else 0.dp,
              color = if (selected) Color(0xFFF59E0B) else Color.Transparent,
              shape = RoundedCornerShape(9.dp),
            )
            .clickable { onSelectPm(pmValue) }
            .testTag("${testTagPrefix}_${label.lowercase(Locale.US)}"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = if (selected) Color(0xFFFBBF24) else Color(0xFF94A3B8),
          fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
          maxLines = 1,
        )
      }
    }
  }
}

internal fun format24HourTo12HourParts(hour24: Int, minute: Int): Pair<String, Boolean> {
  val safeHour = hour24.coerceIn(0, 23)
  val safeMinute = minute.coerceIn(0, 59)
  val isPm = safeHour >= 12
  val hour12 =
    when {
      safeHour == 0 -> 12
      safeHour > 12 -> safeHour - 12
      else -> safeHour
    }
  return String.format(Locale.US, "%02d:%02d", hour12, safeMinute) to isPm
}

internal fun parseServiceTimeTo12Hour(
  raw: String,
  fallbackTime12: String,
  fallbackIsPm: Boolean,
): Pair<String, Boolean> {
  val cleaned = raw.trim().replace("h", ":", ignoreCase = true)
  if (cleaned.isBlank()) return fallbackTime12 to fallbackIsPm

  val upper = cleaned.uppercase(Locale.US)
  val explicitPm = upper.contains("PM")
  val explicitAm = upper.contains("AM")
  val numericPart = upper.replace("AM", "").replace("PM", "").trim()
  val parts = numericPart.split(":")
  val hourRaw = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return fallbackTime12 to fallbackIsPm
  val minuteRaw = (parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0).coerceIn(0, 59)

  return when {
    explicitPm || explicitAm -> {
      val h12 =
        when {
          hourRaw == 0 -> 12
          hourRaw > 12 -> hourRaw - 12
          else -> hourRaw.coerceIn(1, 12)
        }
      String.format(Locale.US, "%02d:%02d", h12, minuteRaw) to explicitPm
    }
    hourRaw in 0..23 -> format24HourTo12HourParts(hourRaw, minuteRaw)
    else -> fallbackTime12 to fallbackIsPm
  }
}

internal fun parseHourMinute24(
  time12Or24: String,
  isPm: Boolean,
  defaultHour24: Int,
  defaultMinute: Int,
): Pair<Int, Int> {
  val cleaned = time12Or24.trim().replace("h", ":", ignoreCase = true)
  val parts = cleaned.split(":")
  val rawHour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return defaultHour24 to defaultMinute
  val minute = (parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0).coerceIn(0, 59)
  val hour24 =
    when {
      rawHour in 13..23 -> rawHour
      rawHour == 0 -> if (isPm) 12 else 0
      rawHour == 12 -> if (isPm) 12 else 0
      rawHour in 1..11 -> if (isPm) rawHour + 12 else rawHour
      else -> defaultHour24
    }
  return hour24 to minute
}

internal fun convert12HourToCanonical24Hour(timeInput: String, isPm: Boolean): String {
  val cleaned = timeInput.trim().replace("h", ":", ignoreCase = true)
  if (cleaned.isBlank()) return ""
  val parts = cleaned.split(":")
  val rawHour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return cleaned
  val minute = (parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0).coerceIn(0, 59)
  val hour24 =
    when {
      rawHour in 13..23 -> rawHour
      rawHour == 0 -> if (isPm) 12 else 0
      rawHour == 12 -> if (isPm) 12 else 0
      rawHour in 1..11 -> if (isPm) rawHour + 12 else rawHour
      else -> return cleaned
    }
  return String.format(Locale.US, "%02d:%02d", hour24, minute)
}

internal fun formatDisplay12HourLabel(timeInput: String, isPm: Boolean): String {
  val canonical24 = convert12HourToCanonical24Hour(timeInput, isPm)
  val parts = canonical24.split(":")
  val h24 = parts.getOrNull(0)?.toIntOrNull() ?: return ""
  val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
  val (t12, pmFlag) = format24HourTo12HourParts(h24, m)
  return "$t12 ${if (pmFlag) "PM" else "AM"}"
}
