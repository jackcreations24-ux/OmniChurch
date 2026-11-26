package com.zoutiw.omnichurch.presentation.screens.auth

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

/**
 * Admin Security Settings card displayed in ProfileAndSettingsScreen for Church Owners/Admins:
 * Quick PIN Fast Activity Resume toggle + 4-6 digit PIN setup/update + Auto-Lock pause timeout + Manual Lock.
 */
@Composable
fun ChurchAdminSecuritySettingsCard(
  church: Church,
  isSaving: Boolean,
  currentLanguageCode: String = "ht",
  onSaveSettings: (quickPinEnabled: Boolean, newPinOrNull: String?, autoLockMinutes: Int) -> Unit,
  onLockAdminNow: () -> Unit,
) {
  val lang = currentLanguageCode.lowercase()
  var showPinSetupDialog by remember { mutableStateOf(false) }
  var pendingEnableAfterPin by remember { mutableStateOf(false) }

  val quickPinOn = church.effectiveQuickPinEnabled
  val currentAutoLock = church.effectiveAutoLockMinutes
  val autoLockOptions = listOf(0 to "Off", 1 to "1m", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m")

  PremiumGlassCard(
    cornerRadius = 22.dp,
    accentColor = ElectricBlue,
    contentPadding = PaddingValues(16.dp),
    modifier = Modifier.testTag("church_admin_security_card"),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = null,
        tint = BrandGold,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text =
          when (lang) {
            "fr" -> "Reprise Rapide (PIN) & Pause Auto"
            "en" -> "Quick Resume (PIN) & Auto-Lock"
            "es" -> "Reanudación Rápida (PIN) y Pausa"
            else -> "Reprann Aktivite Rapid (PIN) & Poz"
          },
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Quick PIN Resume Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text =
            when (lang) {
              "fr" -> "Reprise rapide avec PIN (4–6 chiffres)"
              "en" -> "Quick Resume with PIN (4–6 digits)"
              "es" -> "Reanudación rápida con PIN (4–6 dígitos)"
              else -> "Reprann Aktivite Rapid ak PIN (4–6 chif)"
            },
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            if (quickPinOn) {
              when (lang) {
                "fr" -> "Actif • Reprenez votre session admin par PIN après une pause"
                "en" -> "Enabled • Resume your admin session with PIN after auto-lock"
                "es" -> "Activo • Reanuda tu sesión admin con PIN tras la pausa"
                else -> "Aktive • Reprann aktivite admin lan rapid ak PIN apre tan de poz la"
              }
            } else {
              when (lang) {
                "fr" -> "Désactivé • Demande e-mail + mot de passe après verrouillage"
                "en" -> "Disabled • Requires Email + Password after lock"
                "es" -> "Desactivado • Solicita correo + contraseña al bloquear"
                else -> "Dezaktive • Mande Imèl + Modpas apre verouyaj"
              }
            },
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
        )
      }
      Switch(
        checked = quickPinOn,
        enabled = !isSaving,
        onCheckedChange = { wantEnable ->
          val targetAutoLock = if (wantEnable && currentAutoLock <= 0) 5 else currentAutoLock
          if (wantEnable && church.adminPasscode.isBlank()) {
            pendingEnableAfterPin = true
            showPinSetupDialog = true
          } else {
            onSaveSettings(wantEnable, null, targetAutoLock)
          }
        },
        colors =
          SwitchDefaults.colors(
            checkedThumbColor = PureWhite,
            checkedTrackColor = SuccessGreen,
          ),
        modifier = Modifier.testTag("admin_quick_pin_switch"),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    GlassButton(
      text =
        if (church.adminPasscode.isBlank()) {
          when (lang) {
            "fr" -> "Créer un code PIN de reprise"
            "en" -> "Create Quick Resume PIN"
            "es" -> "Crear PIN de reanudación"
            else -> "Kreye yon Kòd PIN Rapid"
          }
        } else {
          when (lang) {
            "fr" -> "Modifier le code PIN de reprise"
            "en" -> "Change Quick Resume PIN"
            "es" -> "Cambiar PIN de reanudación"
            else -> "Chanje Kòd PIN Rapid la"
          }
        },
      icon = Icons.Default.Pin,
      isPrimaryNeon = false,
      enabled = !isSaving,
      onClick = {
        pendingEnableAfterPin = quickPinOn
        showPinSetupDialog = true
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "admin_change_quick_pin_btn",
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text =
        when (lang) {
          "fr" -> "Temps de pause automatique (Inactivité)"
          "en" -> "Auto-Lock Pause Timeout (Inactivity)"
          "es" -> "Tiempo de pausa automática (Inactividad)"
          else -> "Tan de Poz Otomatik (Inaktivite)"
        },
      style = MaterialTheme.typography.titleSmall,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (currentAutoLock > 0) {
          when (lang) {
            "fr" -> "Verrouillage automatique actif après $currentAutoLock min d'inactivité"
            "en" -> "Auto-lock active after $currentAutoLock min of inactivity"
            "es" -> "Bloqueo automático activo tras $currentAutoLock min de inactividad"
            else -> "Poz otomatik aktif apre $currentAutoLock minit inaktivite"
          }
        } else {
          when (lang) {
            "fr" -> "Désactivé (Off) • Le verrouillage se fait uniquement manuellement"
            "en" -> "Disabled (Off) • Session only locks when you lock manually"
            "es" -> "Desactivado (Off) • Solo se bloquea de forma manual"
            else -> "Dezaktive (Off) • Verouyaj la ap fèt sèlman lè w klike manyèlman"
          }
        },
      style = MaterialTheme.typography.bodySmall,
      color = if (currentAutoLock > 0) SuccessGreen else MutedGlassText,
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      autoLockOptions.forEach { (minutes, label) ->
        val selected = currentAutoLock == minutes
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(12.dp))
              .background(if (selected) ElectricBlue else Color(0x660B1D3D))
              .border(
                1.dp,
                if (selected) PureWhite else GlassWhite.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp),
              )
              .clickable(enabled = !isSaving) {
                onSaveSettings(quickPinOn, null, minutes)
              }
              .padding(vertical = 10.dp)
              .testTag("autolock_option_$minutes"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    GlassButton(
      text =
        when (lang) {
          "fr" -> "Mettre en pause / Verrouiller maintenant"
          "en" -> "Pause / Lock Admin Now"
          "es" -> "Pausar / Bloquear ahora"
          else -> "Mete an Poz / Verouye Kounye a"
        },
      icon = Icons.Default.Lock,
      isPrimaryNeon = false,
      accentColor = BrandOrange,
      onClick = onLockAdminNow,
      modifier = Modifier.fillMaxWidth(),
      testTag = "admin_lock_now_btn",
    )
  }

  if (showPinSetupDialog) {
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var dialogErr by remember { mutableStateOf<String?>(null) }

    GlassDialog(
      onDismissRequest = { showPinSetupDialog = false },
      title =
        when (lang) {
          "fr" -> "Configurer le PIN de reprise rapide"
          "en" -> "Set Up Quick Resume PIN"
          "es" -> "Configurar PIN de reanudación"
          else -> "Konfigire PIN Reprann Aktivite"
        },
    ) {
      Text(
        text =
          when (lang) {
            "fr" -> "Choisissez 4 à 6 chiffres pour reprendre rapidement votre espace admin après une pause automatique."
            "en" -> "Choose 4 to 6 digits to rapidly resume your admin workspace after an auto-lock pause."
            "es" -> "Elige de 4 a 6 dígitos para reanudar rápidamente tu espacio admin tras una pausa."
            else -> "Chwazi 4 a 6 chif pou reprann espas admin lan rapid apre tan de poz otomatik la."
          },
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(10.dp))
      dialogErr?.let { errText ->
        Text(
          text = errText,
          style = MaterialTheme.typography.bodySmall,
          color = ErrorRed,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
      GlassInputField(
        value = pinInput,
        onValueChange = {
          if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it
          dialogErr = null
        },
        label =
          when (lang) {
            "fr" -> "Nouveau PIN (4–6 chiffres)"
            "en" -> "New PIN (4–6 digits)"
            "es" -> "Nuevo PIN (4–6 dígitos)"
            else -> "Nouvo PIN (4–6 chif)"
          },
        leadingIcon = Icons.Default.Pin,
        visualTransformation = PasswordVisualTransformation(),
        keyboardType = KeyboardType.NumberPassword,
        testTag = "setup_quick_pin_input",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = pinConfirmInput,
        onValueChange = {
          if (it.length <= 6 && it.all { c -> c.isDigit() }) pinConfirmInput = it
          dialogErr = null
        },
        label =
          when (lang) {
            "fr" -> "Confirmer le PIN"
            "en" -> "Confirm PIN"
            "es" -> "Confirmar PIN"
            else -> "Konfime PIN nan"
          },
        leadingIcon = Icons.Default.Pin,
        visualTransformation = PasswordVisualTransformation(),
        keyboardType = KeyboardType.NumberPassword,
        testTag = "setup_quick_pin_confirm_input",
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Annuler"
              "en" -> "Cancel"
              "es" -> "Cancelar"
              else -> "Anile"
            },
          isPrimaryNeon = false,
          onClick = { showPinSetupDialog = false },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Enregistrer"
              "en" -> "Save"
              "es" -> "Guardar"
              else -> "Anrejistre"
            },
          isPrimaryNeon = true,
          onClick = {
            if (!pinInput.matches(Regex("^\\d{4,6}$"))) {
              dialogErr =
                when (lang) {
                  "fr" -> "Le PIN doit contenir entre 4 et 6 chiffres."
                  "en" -> "PIN must be 4 to 6 digits."
                  "es" -> "El PIN debe tener entre 4 y 6 dígitos."
                  else -> "PIN lan dwe gen 4 a 6 chif."
                }
              return@GlassButton
            }
            if (pinInput != pinConfirmInput) {
              dialogErr =
                when (lang) {
                  "fr" -> "Les deux codes PIN ne correspondent pas."
                  "en" -> "The two PINs do not match."
                  "es" -> "Los dos PIN no coinciden."
                  else -> "De PIN yo pa menm."
                }
              return@GlassButton
            }
            showPinSetupDialog = false
            val nextEnabled = if (pendingEnableAfterPin) true else quickPinOn
            val nextAutoLock = if (nextEnabled && currentAutoLock <= 0) 5 else currentAutoLock
            onSaveSettings(nextEnabled, pinInput, nextAutoLock)
          },
          modifier = Modifier.weight(1f),
          testTag = "setup_quick_pin_save_btn",
        )
      }
    }
  }
}
