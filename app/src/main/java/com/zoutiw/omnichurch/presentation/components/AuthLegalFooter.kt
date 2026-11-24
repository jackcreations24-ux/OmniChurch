package com.zoutiw.omnichurch.presentation.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.core.legal.LegalLinksConfig
import com.zoutiw.omnichurch.core.legal.LegalUrlOpener

/**
 * Localized copy for the official Privacy Policy and Terms of Service links
 * across all 4 OmniChurch supported languages (`ht`, `fr`, `en`, `es`).
 */
data class AuthLegalStrings(
  val privacyLabel: String,
  val termsLabel: String,
  val unavailableMessage: String,
) {
  companion object {
    fun forLanguage(languageCode: String): AuthLegalStrings =
      when (languageCode.trim().lowercase()) {
        "fr" ->
          AuthLegalStrings(
            privacyLabel = "Politique de confidentialité",
            termsLabel = "Conditions d'utilisation",
            unavailableMessage =
              "Le lien officiel n'est pas encore disponible. Veuillez réessayer plus tard.",
          )
        "en" ->
          AuthLegalStrings(
            privacyLabel = "Privacy Policy",
            termsLabel = "Terms of Service",
            unavailableMessage = "Official link is not available yet. Please try again later.",
          )
        "es" ->
          AuthLegalStrings(
            privacyLabel = "Política de privacidad",
            termsLabel = "Términos de servicio",
            unavailableMessage = "El enlace oficial aún no está disponible. Inténtalo más tarde.",
          )
        else ->
          AuthLegalStrings(
            privacyLabel = "Politik Konfidansyalite",
            termsLabel = "Kondisyon Itilizasyon",
            unavailableMessage = "Lyen ofisyèl la poko disponib. Eseye ankò talè.",
          )
      }
  }
}

private val LegalLinkDefaultColor = Color(0xFF94A3B8)
private val LegalLinkPressedColor = Color(0xFF38BDF8)
private val LegalDotSeparatorColor = Color(0xFF475569)

/**
 * Minimal, native, borderless bottom footer displaying clickable official
 * Privacy Policy and Terms of Service links on the Authentication screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthLegalFooter(
  legalLinks: LegalLinksConfig,
  languageCode: String,
  modifier: Modifier = Modifier,
  onUnavailableMessage: ((String) -> Unit)? = null,
) {
  val context = LocalContext.current
  val copy = remember(languageCode) { AuthLegalStrings.forLanguage(languageCode) }

  val handleUnavailable: () -> Unit = {
    if (onUnavailableMessage != null) {
      onUnavailableMessage(copy.unavailableMessage)
    } else {
      Toast.makeText(context, copy.unavailableMessage, Toast.LENGTH_SHORT).show()
    }
  }

  FlowRow(
    modifier = modifier.fillMaxWidth().testTag("auth_legal_footer"),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    verticalArrangement = Arrangement.Center,
  ) {
    LegalTextLink(
      text = copy.privacyLabel,
      testTag = "auth_privacy_policy_link",
      onClick = {
        LegalUrlOpener.openOfficialUrl(
          context = context,
          rawUrl = legalLinks.officialPrivacyUrl,
          onUnavailable = handleUnavailable,
        )
      },
    )

    Box(
      modifier = Modifier.heightIn(min = 44.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "•",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.5.sp),
        color = LegalDotSeparatorColor,
      )
    }

    LegalTextLink(
      text = copy.termsLabel,
      testTag = "auth_terms_of_service_link",
      onClick = {
        LegalUrlOpener.openOfficialUrl(
          context = context,
          rawUrl = legalLinks.officialTermsUrl,
          onUnavailable = handleUnavailable,
        )
      },
    )
  }
}

@Composable
private fun LegalTextLink(
  text: String,
  testTag: String,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  Box(
    modifier =
      Modifier.heightIn(min = 44.dp)
        .semantics { role = Role.Button }
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .padding(horizontal = 8.dp, vertical = 10.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontSize = 12.5.sp,
          fontWeight = FontWeight.Medium,
          textDecoration = TextDecoration.Underline,
        ),
      color = if (isPressed) LegalLinkPressedColor else LegalLinkDefaultColor,
      maxLines = 1,
    )
  }
}
