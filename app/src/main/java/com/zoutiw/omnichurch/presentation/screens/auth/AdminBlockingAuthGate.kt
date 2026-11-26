package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite

/**
 * Full-screen Anti-Bypass Gate shown over the Admin workspace whenever the Admin session is locked
 * or the underlying Firebase Auth user is missing / mismatched.
 */
@Composable
fun BlockingAdminAuthGateOverlay(
  church: Church,
  firebaseUser: FirebaseUser?,
  isBusy: Boolean,
  errorMessage: String?,
  infoMessage: String?,
  legacyVerifiedChurchId: String?,
  legacyVerifiedPin: String?,
  onClearMessages: () -> Unit,
  onSignInEmailPassword: (email: String, password: String) -> Unit,
  onSendPasswordReset: (email: String) -> Unit,
  onGoogleVerified: () -> Unit,
  onQuickPinSubmit: (pin: String) -> Unit,
  onBiometricSuccess: () -> Unit,
  onVerifyLegacyPin: (pin: String) -> Unit,
  onCompleteLegacyMigration: (email: String, newPass: String, confirmPass: String) -> Unit,
  onResetLegacyMigration: () -> Unit,
  onLocalError: (String) -> Unit,
  onExitToAuthPortal: () -> Unit,
  currentLanguageCode: String = "ht",
) {
  val lang = currentLanguageCode.lowercase()
  val isQuickResumeReady =
    remember(church, firebaseUser) {
      SessionLockManager.canQuickPinUnlock(church, firebaseUser)
    }

  BackHandler(enabled = true) {
    // Prevent hardware back bypass while session is paused/locked.
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    PremiumGlassCard(
      cornerRadius = 24.dp,
      isHighlighted = true,
      accentColor = BrandGold,
      contentPadding = PaddingValues(20.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OmniChurchLogo(
          variant = LogoVariant.Icon,
          modifier = Modifier.size(46.dp),
          contentDescription = "OmniChurch PRO",
        )
        Text(
          text =
            if (isQuickResumeReady) {
              when (lang) {
                "fr" -> "Reprise d'activité rapide"
                "en" -> "Quick Activity Resume"
                "es" -> "Reanudación rápida"
                else -> "Reprann Aktivite Rapid"
              }
            } else {
              when (lang) {
                "fr" -> "Espace Administrateur Verrouillé"
                "en" -> "Admin Workspace Locked"
                "es" -> "Espacio de Administrador Bloqueado"
                else -> "Espas Administratè an Poz"
              }
            },
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "${church.name} (${church.effectiveCode})",
          style = MaterialTheme.typography.labelLarge,
          color = BlueNeon,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
        )
        Text(
          text =
            if (isQuickResumeReady) {
              when (lang) {
                "fr" -> "Votre session est en pause pour votre sécurité. Entrez votre code PIN pour reprendre immédiatement."
                "en" -> "Your session paused for security. Enter your Quick PIN to resume right where you left off."
                "es" -> "Tu sesión se pausó por seguridad. Ingresa tu PIN rápido para continuar al instante."
                else -> "Sesyon ou a pran yon poz pou sekirite. Antre PIN ou pou reprann aktivite w imedyatman."
              }
            } else {
              when (lang) {
                "fr" -> "Vérifiez votre identité administrateur pour déverrouiller la gestion de votre église."
                "en" -> "Verify your administrator identity to unlock church management."
                "es" -> "Verifica tu identidad de administrador para desbloquear la gestión de tu iglesia."
                else -> "Verifye idantite administratè w pou debloke jesyon legliz ou a."
              }
            },
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
          textAlign = TextAlign.Center,
        )

        AdminAuthGateFormContent(
          church = church,
          firebaseUser = firebaseUser,
          isBusy = isBusy,
          errorMessage = errorMessage,
          infoMessage = infoMessage,
          legacyVerifiedChurchId = legacyVerifiedChurchId,
          legacyVerifiedPin = legacyVerifiedPin,
          onClearMessages = onClearMessages,
          onSignInEmailPassword = onSignInEmailPassword,
          onSendPasswordReset = onSendPasswordReset,
          onGoogleVerified = onGoogleVerified,
          onQuickPinSubmit = onQuickPinSubmit,
          onBiometricSuccess = onBiometricSuccess,
          onVerifyLegacyPin = onVerifyLegacyPin,
          onCompleteLegacyMigration = onCompleteLegacyMigration,
          onResetLegacyMigration = onResetLegacyMigration,
          onLocalError = onLocalError,
          currentLanguageCode = currentLanguageCode,
        )

        Spacer(modifier = Modifier.height(4.dp))

        GlassButton(
          text =
            when (lang) {
              "fr" -> "Se déconnecter / Changer de compte"
              "en" -> "Sign Out / Switch Account"
              "es" -> "Cerrar sesión / Cambiar cuenta"
              else -> "Dekonekte / Chanje Kont"
            },
          icon = Icons.AutoMirrored.Filled.Logout,
          isPrimaryNeon = false,
          onClick = onExitToAuthPortal,
          modifier = Modifier.fillMaxWidth(),
          testTag = "admin_gate_sign_out_button",
        )
      }
    }
  }
}

/**
 * 30-second countdown warning dialog before automatic inactivity lockout.
 */
@Composable
fun AutoLockWarningDialog(
  secondsRemaining: Int,
  currentLanguageCode: String = "ht",
  onStayActive: () -> Unit,
  onLockNow: () -> Unit,
) {
  val lang = currentLanguageCode.lowercase()
  GlassDialog(
    onDismissRequest = onStayActive,
    title =
      when (lang) {
        "fr" -> "Mise en pause automatique"
        "en" -> "Automatic Pause"
        "es" -> "Pausa automática"
        else -> "Poz Otomatik"
      },
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Default.LockClock,
        contentDescription = null,
        tint = BrandGold,
        modifier = Modifier.size(40.dp),
      )
      Text(
        text =
          when (lang) {
            "fr" -> "Votre session Admin va se verrouiller dans ${secondsRemaining}s pour cause d'inactivité."
            "en" -> "Your Admin session will pause in ${secondsRemaining}s due to inactivity."
            "es" -> "Tu sesión Admin se bloqueará en ${secondsRemaining}s por inactividad."
            else -> "Sesyon Admin ou a pral mete an poz nan ${secondsRemaining}s poutèt inaktivite."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        textAlign = TextAlign.Center,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Verrouiller"
              "en" -> "Lock Now"
              "es" -> "Bloquear"
              else -> "Verouye"
            },
          isPrimaryNeon = false,
          accentColor = BrandOrange,
          onClick = onLockNow,
          modifier = Modifier.weight(1f),
          testTag = "autolock_warning_lock_now",
        )
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Continuer"
              "en" -> "Stay Active"
              "es" -> "Continuar"
              else -> "Kontinye"
            },
          isPrimaryNeon = true,
          onClick = onStayActive,
          modifier = Modifier.weight(1f),
          testTag = "autolock_warning_continue",
        )
      }
    }
  }
}
