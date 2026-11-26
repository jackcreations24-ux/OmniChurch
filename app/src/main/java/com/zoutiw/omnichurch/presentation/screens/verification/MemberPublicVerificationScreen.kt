package com.zoutiw.omnichurch.presentation.screens.verification

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassPanel
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassCardBorderSubtle
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MidnightNavy
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import java.util.Locale

@Composable
fun MemberPublicVerificationScreen(
  verificationState: UiState<PublicVerificationResolution?>,
  verificationConfig: RemoteMemberVerificationConfig,
  isChurchLicensed: (String?, String?) -> Boolean,
  initialLanguageCode: String = "ht",
  onRetry: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onClose)
  val context = LocalContext.current
  var activeLang by remember(initialLanguageCode) {
    mutableStateOf(
      initialLanguageCode.trim().lowercase(Locale.US).takeIf { it in setOf("ht", "fr", "en", "es") }
        ?: "ht"
    )
  }
  val labels = remember(activeLang) { resolveVerificationStrings(activeLang) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .statusBarsPadding()
        .navigationBarsPadding(),
  ) {
    VerificationTopBar(
      title = labels.topBarTitle,
      activeLang = activeLang,
      onSelectLang = { activeLang = it },
      onClose = onClose,
    )

    when (verificationState) {
      is UiState.Loading -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            GlassLoadingIndicator()
            Text(
              text = labels.loadingLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = GlassWhite,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(20.dp),
          contentAlignment = Alignment.Center,
        ) {
          GlassPanel(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            borderColors = listOf(ErrorRed.copy(alpha = 0.7f), ErrorRed.copy(alpha = 0.2f)),
            backgroundBrush = OmniBrushes.ErrorGlassBrush,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = labels.errorTitle,
                tint = ErrorRed,
                modifier = Modifier.size(48.dp),
              )
              Text(
                text = labels.errorTitle,
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
              Text(
                text = verificationState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = GlassWhite,
                textAlign = TextAlign.Center,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = onClose,
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("btn_verification_error_close"),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text(labels.closeButton, color = PureWhite, fontWeight = FontWeight.SemiBold)
                }
                Button(
                  onClick = onRetry,
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("btn_verification_error_retry"),
                  colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(labels.retryButton, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
      is UiState.Success -> {
        val resolved = verificationState.data
        if (resolved == null) {
          Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center,
          ) {
            Button(
              onClick = onClose,
              modifier = Modifier.minimumInteractiveComponentSize(),
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            ) {
              Text(labels.closeButton)
            }
          }
        } else {
          val church = resolved.church
          val member = resolved.member
          val isEnabledForThisChurch =
            verificationConfig.isEnabledForChurch(church.id, church.effectiveCode)

          if (!isEnabledForThisChurch) {
            Box(
              modifier = Modifier.fillMaxSize().padding(20.dp),
              contentAlignment = Alignment.Center,
            ) {
              GlassPanel(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                borderColors = listOf(BrandGold.copy(alpha = 0.7f), BrandOrange.copy(alpha = 0.3f)),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = labels.disabledTitle,
                    tint = BrandGold,
                    modifier = Modifier.size(48.dp),
                  )
                  Text(
                    text = labels.disabledTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                  )
                  Text(
                    text = labels.disabledBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassWhite,
                    textAlign = TextAlign.Center,
                  )
                  Button(
                    onClick = onClose,
                    modifier =
                      Modifier.fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_verification_disabled_close"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(14.dp),
                  ) {
                    Text(labels.closeButton, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          } else {
            OfficialCertificateContent(
              context = context,
              church = church,
              member = member,
              verificationConfig = verificationConfig,
              isLicensedChurch = isChurchLicensed(church.id, church.effectiveCode),
              activeLang = activeLang,
              labels = labels,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VerificationTopBar(
  title: String,
  activeLang: String,
  onSelectLang: (String) -> Unit,
  onClose: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      IconButton(
        onClick = onClose,
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(MidnightNavy.copy(alpha = 0.7f))
            .testTag("btn_verification_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = PureWhite,
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "verify.omnichurch.download",
          style = MaterialTheme.typography.labelSmall,
          color = BlueNeon,
          maxLines = 1,
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("ht", "fr", "en", "es").forEach { code ->
        val selected = activeLang == code
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(
                if (selected) ElectricBlue else MidnightNavy.copy(alpha = 0.7f)
              )
              .border(
                width = 1.dp,
                color = if (selected) BlueNeon else GlassCardBorderSubtle,
                shape = RoundedCornerShape(50),
              )
              .clickable { onSelectLang(code) }
              .padding(horizontal = 9.dp, vertical = 6.dp)
              .testTag("btn_verification_lang_$code"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = code.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) PureWhite else MutedGlassText,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
