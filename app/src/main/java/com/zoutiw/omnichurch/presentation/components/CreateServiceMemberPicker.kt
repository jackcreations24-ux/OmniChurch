package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.util.Locale

internal enum class ServiceRolePickerTarget(
  val allowAppendMultiple: Boolean = false,
) {
  LEADER,
  PREACHER,
  READER,
  MUSICIANS(allowAppendMultiple = true),
  USHERS(allowAppendMultiple = true),
}

@Composable
internal fun ServiceMemberPickerDialog(
  title: String,
  currentValue: String,
  members: List<Member>,
  copy: CreateServiceCopy,
  onSelectValue: (String) -> Unit,
  onDismiss: () -> Unit,
  prioritizeUshers: Boolean = false,
  helperSubtitle: String? = null,
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredMembers = remember(members, searchQuery) {
    val q = searchQuery.trim().lowercase(Locale.getDefault())
    if (q.isEmpty()) {
      members
    } else {
      members.filter { m ->
        m.effectiveFullName.lowercase(Locale.getDefault()).contains(q) ||
          m.effectiveMatrikil.lowercase(Locale.getDefault()).contains(q) ||
          m.department.lowercase(Locale.getDefault()).contains(q) ||
          m.role.lowercase(Locale.getDefault()).contains(q)
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = Color(0xFF0F172A),
      border = BorderStroke(1.dp, Color(0xFF334155)),
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .heightIn(min = 300.dp, max = 560.dp)
        .imePadding(),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = helperSubtitle ?: String.format(copy.pickerMembersAvailableFormat, members.size),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          if (currentValue.isNotBlank()) {
            TextButton(onClick = { onSelectValue("") }) {
              Text(
                text = copy.pickerClearBtn,
                color = Color(0xFFF87171),
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = copy.closeContentDescription,
              tint = Color(0xFFCBD5E1),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Guest ("Envite") Action Strip
        val trimmedQuery = searchQuery.trim()
        val guestLabelToAssign =
          if (trimmedQuery.isNotEmpty()) String.format(copy.guestWithNameFormat, trimmedQuery)
          else copy.guestShortLabel

        Surface(
          onClick = { onSelectValue(guestLabelToAssign) },
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFFF59E0B).copy(alpha = 0.14f),
          border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("picker_select_guest_btn"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.PersonAddAlt1,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  if (trimmedQuery.isNotEmpty())
                    String.format(copy.guestSetNamedFormat, trimmedQuery)
                  else copy.guestEmptyTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (trimmedQuery.isNotEmpty()) copy.guestSetNamedSubtitle
                  else copy.guestEmptySubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
              )
            }
            if (listOf(copy.guestShortLabel, "Envite", "Invité", "Guest", "Invitado")
                .any { currentValue.equals(it, ignoreCase = true) }
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = copy.pickerSearchPlaceholder,
              color = Color(0xFF64748B),
              style = MaterialTheme.typography.bodySmall,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(18.dp),
            )
          },
          trailingIcon = if (searchQuery.isNotBlank()) {
            {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = copy.pickerClearSearchCd,
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          } else null,
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f),
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            cursorColor = Color(0xFF38BDF8),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("picker_member_search_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredMembers.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text =
                  if (members.isEmpty()) copy.pickerNoMembersLoaded
                  else String.format(copy.pickerNoSearchMatchFormat, trimmedQuery),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = copy.pickerUseGuestHint,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(
              items = filteredMembers,
              key = { it.id.ifBlank { it.effectiveMatrikil.ifBlank { it.effectiveFullName } } },
            ) { mem ->
              val isSelected =
                currentValue.equals(mem.effectiveFullName, ignoreCase = true) ||
                  currentValue.split(",").any { it.trim().equals(mem.effectiveFullName, ignoreCase = true) }
              val initials = remember(mem.effectiveFullName) {
                mem.effectiveFullName
                  .split(" ")
                  .filter { it.isNotBlank() }
                  .take(2)
                  .joinToString("") { it.first().uppercaseChar().toString() }
                  .ifBlank { "M" }
              }

              Surface(
                onClick = { onSelectValue(mem.effectiveFullName) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color(0xFF1E293B).copy(alpha = 0.55f),
                border = BorderStroke(
                  width = 1.dp,
                  color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.65f) else Color(0xFF334155).copy(alpha = 0.45f),
                ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  val avatarUrl = mem.effectivePhotoUrl
                  if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                      model = avatarUrl,
                      contentDescription = mem.effectiveFullName,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                          Brush.linearGradient(
                            colors = listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                          )
                        ),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = initials,
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = mem.effectiveFullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                      )
                      if (prioritizeUshers && mem.isUsherRole) {
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF0EA5E9).copy(alpha = 0.22f))
                            .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                          Text(
                            text = copy.pickerUsherBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                          )
                        }
                      }
                    }
                    val metaParts = buildList {
                      if (mem.department.isNotBlank()) add(mem.department)
                      if (mem.effectiveMatrikil.isNotBlank()) add(mem.effectiveMatrikil)
                      if (mem.role.isNotBlank() && !mem.role.equals("member", ignoreCase = true)) {
                        add(mem.role.replaceFirstChar { it.uppercase() })
                      }
                    }
                    if (metaParts.isNotEmpty()) {
                      Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }

                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = copy.pickerSelectedCd,
                      tint = Color(0xFF38BDF8),
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
