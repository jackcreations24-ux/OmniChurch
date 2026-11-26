package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.PureWhite
import java.util.Locale

@Composable
internal fun CreateServiceSheetHeader(
  isEditing: Boolean,
  copy: CreateServiceCopy,
  onCloseClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = if (isEditing) copy.headerTitleEdit else copy.headerTitleCreate,
        style = MaterialTheme.typography.titleLarge,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = if (isEditing) copy.headerSubtitleEdit else copy.headerSubtitleCreate,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF94A3B8), // Slate-400
      )
    }
    IconButton(
      onClick = onCloseClick,
      modifier = Modifier
        .size(48.dp)
        .testTag("service_bottom_sheet_close_btn"),
    ) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = copy.closeContentDescription,
        tint = Color(0xFFCBD5E1),
      )
    }
  }
}

@Composable
internal fun CreateServiceCategoryGrid(
  categories: List<Pair<String, String>>,
  selectedCategory: String,
  copy: CreateServiceCopy,
  onSelectCategory: (String) -> Unit,
) {
  SectionTitle(title = copy.categorySectionTitle)
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    categories.take(3).forEach { (catKey, catLabel) ->
      CategoryChip(
        label = catLabel,
        isSelected = selectedCategory == catKey,
        onClick = { onSelectCategory(catKey) },
        modifier = Modifier.weight(1f),
      )
    }
  }
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    categories.drop(3).forEach { (catKey, catLabel) ->
      CategoryChip(
        label = catLabel,
        isSelected = selectedCategory == catKey,
        onClick = { onSelectCategory(catKey) },
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
internal fun CreateServiceRolePickerOverlay(
  target: ServiceRolePickerTarget,
  selectableMembers: List<Member>,
  currentVal: String,
  copy: CreateServiceCopy,
  onApplyRoleSelection: (ServiceRolePickerTarget, String) -> Unit,
  onDismiss: () -> Unit,
) {
  val orderedMembers = remember(selectableMembers, target) {
    if (target == ServiceRolePickerTarget.USHERS) {
      selectableMembers.sortedWith(
        compareByDescending<Member> { it.isUsherRole }
          .thenBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
      )
    } else {
      selectableMembers
    }
  }
  ServiceMemberPickerDialog(
    title = copy.roleModalTitle(target),
    currentValue = currentVal,
    members = orderedMembers,
    copy = copy,
    prioritizeUshers = target == ServiceRolePickerTarget.USHERS,
    helperSubtitle =
      if (target == ServiceRolePickerTarget.USHERS) copy.usherHelperSubtitle else null,
    onSelectValue = { chosenName ->
      onApplyRoleSelection(target, chosenName)
      onDismiss()
    },
    onDismiss = onDismiss,
  )
}

internal fun toggleMultiRoleValue(existing: String, chosen: String): String {
  if (chosen.isBlank()) return ""
  val currentTokens = existing.split(",").map { it.trim() }.filter { it.isNotBlank() }
  val matchIndex = currentTokens.indexOfFirst { it.equals(chosen, ignoreCase = true) }
  return if (matchIndex >= 0) {
    currentTokens.filterIndexed { index, _ -> index != matchIndex }.joinToString(", ")
  } else {
    (currentTokens + chosen).joinToString(", ")
  }
}
