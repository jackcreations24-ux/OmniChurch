package com.zoutiw.omnichurch.presentation.screens.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
internal fun NfcBindMemberBottomSheet(
  rawUid: String,
  churchMembers: List<Member>,
  searchQuery: String,
  copy: ScannerCopy,
  onSearchQueryChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onBindMember: (Member) -> Unit,
) {
  val filteredMembers =
    remember(churchMembers, searchQuery) {
      val q = searchQuery.trim().lowercase()
      if (q.isEmpty()) churchMembers
      else {
        churchMembers.filter {
          it.effectiveFullName.lowercase().contains(q) ||
            it.effectiveMatrikil.lowercase().contains(q) ||
            it.department.lowercase().contains(q)
        }
      }
    }

  GlassModalBottomSheet(
    onDismissRequest = onDismiss,
    title = String.format(copy.nfcBindTitleFormat, rawUid),
  ) {
    Text(
      text = copy.nfcBindSubtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MutedGlassText,
    )
    Spacer(modifier = Modifier.height(10.dp))
    GlassInputField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      label = copy.nfcBindSearchLabel,
      leadingIcon = Icons.Default.Search,
      testTag = "nfc_bind_member_search",
    )
    Spacer(modifier = Modifier.height(10.dp))
    LazyColumn(
      modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(filteredMembers, key = { it.id }) { mem ->
        PremiumGlassCard(
          cornerRadius = 14.dp,
          contentPadding = PaddingValues(12.dp),
          onClick = { onBindMember(mem) },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              MemberAvatar3D(
                fullName = mem.effectiveFullName,
                size = 40.dp,
                photoUrl = mem.effectivePhotoUrl,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = mem.effectiveFullName,
                  style = MaterialTheme.typography.titleSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "${mem.effectiveMatrikil} • ${mem.department.ifBlank { copy.defaultMemberLabel }}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedGlassText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            Icon(
              imageVector = Icons.Default.Link,
              contentDescription = copy.nfcBindActionCd,
              tint = BrandGold,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}
