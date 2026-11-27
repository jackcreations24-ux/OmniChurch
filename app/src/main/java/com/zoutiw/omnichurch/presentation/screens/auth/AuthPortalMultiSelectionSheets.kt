package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.core.utilities.ChurchLocationDetector
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.util.Locale

@Composable
internal fun AuthMultiChurchSelectionSheet(
  title: String,
  subtitle: String,
  ownedChurches: List<Church>,
  onSelectChurch: (Church) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeLanguage = remember {
    ChurchLocationDetector.normalizeLanguage(Locale.getDefault().language)
  }
  GlassModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OmniChurchLogo(
          variant = LogoVariant.Icon,
          modifier = Modifier.size(28.dp),
          contentDescription = "OmniChurch PRO",
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = GlassWhite.copy(alpha = 0.88f),
      )
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ownedChurches.forEach { ownedChurch ->
          val formattedLocation =
            ChurchLocationDetector.formatChurchModelLocation(
              city = ownedChurch.city,
              address = ownedChurch.address,
              language = activeLanguage,
              explicitCountry = ownedChurch.country,
              context = context,
            ).takeIf { it.isNotBlank() }
          PremiumGlassCard(
            cornerRadius = 16.dp,
            isHighlighted = true,
            accentColor = BrandGold,
            onClick = { onSelectChurch(ownedChurch) },
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier.testTag("owned_church_item_${ownedChurch.id}"),
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
                Icon(
                  imageVector = Icons.Default.AccountBalance,
                  contentDescription = null,
                  tint = BrandGold,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = ownedChurch.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  val rowSubtitle =
                    listOfNotNull(
                      ownedChurch.effectiveCode.takeIf { it.isNotBlank() },
                      formattedLocation,
                    ).joinToString(" • ")
                  if (rowSubtitle.isNotBlank()) {
                    Text(
                      text = rowSubtitle,
                      style = MaterialTheme.typography.labelMedium,
                      color = BrandGold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun AuthMatchedMemberSelectionSheet(
  title: String,
  subtitle: String,
  matchedSessions: List<AuthenticatedMemberSession>,
  onSelectSession: (AuthenticatedMemberSession) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val activeLanguage = remember {
    ChurchLocationDetector.normalizeLanguage(Locale.getDefault().language)
  }
  GlassModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OmniChurchLogo(
          variant = LogoVariant.Icon,
          modifier = Modifier.size(28.dp),
          contentDescription = "OmniChurch PRO",
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = GlassWhite.copy(alpha = 0.88f),
      )
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        matchedSessions.forEach { matchedSession ->
          val matchedChurch = matchedSession.church
          val matchedMember = matchedSession.member
          val formattedLocation =
            ChurchLocationDetector.formatChurchModelLocation(
              city = matchedChurch.city,
              address = matchedChurch.address,
              language = activeLanguage,
              explicitCountry = matchedChurch.country,
              context = context,
            ).takeIf { it.isNotBlank() }
          PremiumGlassCard(
            cornerRadius = 16.dp,
            isHighlighted = true,
            accentColor = BlueNeon,
            onClick = { onSelectSession(matchedSession) },
            contentPadding = PaddingValues(14.dp),
            modifier = Modifier.testTag("matched_member_church_item_${matchedChurch.id}"),
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
                Icon(
                  imageVector = Icons.Default.AccountBalance,
                  contentDescription = null,
                  tint = BrandGold,
                  modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = matchedChurch.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  val rowSubtitle =
                    listOfNotNull(
                      matchedChurch.effectiveCode.takeIf { it.isNotBlank() },
                      matchedMember.effectiveMatrikil.takeIf { it.isNotBlank() },
                      formattedLocation,
                    ).joinToString(" • ")
                  if (rowSubtitle.isNotBlank()) {
                    Text(
                      text = rowSubtitle,
                      style = MaterialTheme.typography.labelMedium,
                      color = BrandGold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }
}
