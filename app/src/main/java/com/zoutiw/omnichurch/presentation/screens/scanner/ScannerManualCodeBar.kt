package com.zoutiw.omnichurch.presentation.screens.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun RefinedInlineMemberCodeBar(
  value: String,
  onValueChange: (String) -> Unit,
  isVerifyMode: Boolean,
  canSubmit: Boolean,
  copy: ScannerCopy,
  onSubmit: () -> Unit,
) {
  val accentColor = if (isVerifyMode) SuccessGreen else BlueNeon
  val borderColor =
    if (value.isNotBlank()) accentColor.copy(alpha = 0.72f)
    else GlassWhite.copy(alpha = 0.22f)

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xCC071530))
        .border(1.1.dp, borderColor, RoundedCornerShape(16.dp))
        .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = if (isVerifyMode) Icons.Default.VerifiedUser else Icons.Default.Badge,
      contentDescription = null,
      tint = if (value.isNotBlank()) accentColor else MutedGlassText,
      modifier = Modifier.size(19.dp),
    )

    Spacer(modifier = Modifier.width(10.dp))

    Box(
      modifier = Modifier.weight(1f).padding(vertical = 8.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      if (value.isEmpty()) {
        Text(
          text = if (isVerifyMode) copy.inlineVerifyPlaceholder else copy.inlineCodePlaceholder,
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle =
          MaterialTheme.typography.bodyMedium.copy(
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
          ),
        cursorBrush = SolidColor(accentColor),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() }),
        modifier = Modifier.fillMaxWidth().testTag("scanner_manual_code_input"),
      )
    }

    if (value.isNotEmpty()) {
      IconButton(
        onClick = { onValueChange("") },
        modifier = Modifier.size(36.dp).testTag("scanner_clear_code_btn"),
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = copy.commonClose,
          tint = MutedGlassText,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    Spacer(modifier = Modifier.width(4.dp))

    Box(
      modifier =
        Modifier.size(42.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            if (canSubmit) {
              if (isVerifyMode) {
                Brush.linearGradient(listOf(SuccessGreen, ElectricBlue))
              } else {
                OmniBrushes.ActiveBlueNeonBrush
              }
            } else {
              Brush.linearGradient(listOf(Color(0x551B2E52), Color(0x5512203B)))
            }
          )
          .border(
            width = 1.dp,
            color = if (canSubmit) PureWhite.copy(alpha = 0.65f) else GlassWhite.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
          )
          .clickable(enabled = canSubmit, onClick = onSubmit)
          .testTag(if (isVerifyMode) "scanner_verify_manual_submit_btn" else "scanner_manual_submit_btn"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector =
          if (isVerifyMode) Icons.Default.VerifiedUser else Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription =
          if (isVerifyMode) copy.verifyManualSubmit else copy.manualSubmit,
        tint = if (canSubmit) PureWhite else MutedGlassText.copy(alpha = 0.55f),
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

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

