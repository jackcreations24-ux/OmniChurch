package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue

/**
 * Create New Church dialog matching Section 6.A:
 * Tab 1 (Default): Email + Password (with multi-church owner reuse).
 * Tab 2: Google Sign-In (Credential Manager).
 */
@Composable
fun CreateChurchCloudDialog(
  isBusy: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onClearError: () -> Unit,
  onCreateWithEmailPassword: (
    churchName: String,
    churchCode: String,
    churchAddress: String,
    email: String,
    password: String,
    confirmPassword: String,
  ) -> Unit,
  onCreateWithGoogle: (
    churchName: String,
    churchCode: String,
    churchAddress: String,
  ) -> Unit,
  onLocalError: (String) -> Unit,
) {
  val rawContext = LocalContext.current
  val context = remember(rawContext) { rawContext.findActivity() ?: rawContext }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var useEmailTab by remember { mutableStateOf(true) }
  var churchName by remember { mutableStateOf("") }
  var churchCode by remember { mutableStateOf("") }
  var churchAddress by remember { mutableStateOf("") }
  var ownerEmail by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }

  GlassDialog(
    onDismissRequest = onDismiss,
    title = "Kreye yon Nouvo Legliz",
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DeepNavy.copy(alpha = 0.85f))
            .border(1.dp, GlassWhite.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (useEmailTab) Brush.horizontalGradient(listOf(RoyalBlue, ElectricBlue))
                else Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
              )
              .clickable {
                useEmailTab = true
                onClearError()
              }
              .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Imèl & Modpas",
            style = MaterialTheme.typography.labelMedium,
            color = if (useEmailTab) PureWhite else MutedGlassText,
            fontWeight = FontWeight.Bold,
          )
        }
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (!useEmailTab) Brush.horizontalGradient(listOf(BrandOrange, BrandGold))
                else Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
              )
              .clickable {
                useEmailTab = false
                onClearError()
              }
              .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Kont Google",
            style = MaterialTheme.typography.labelMedium,
            color = if (!useEmailTab) DeepNavy else MutedGlassText,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ErrorRed.copy(alpha = 0.22f))
              .border(1.dp, ErrorRed, RoundedCornerShape(12.dp))
              .padding(10.dp)
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite,
          )
        }
      }

      GlassInputField(
        value = churchName,
        onValueChange = {
          churchName = it
          onClearError()
        },
        label = "Non Ofisyèl Legliz la",
        placeholder = "Legliz Tabènak Delmas",
        leadingIcon = Icons.Default.AccountBalance,
        testTag = "create_church_name_input",
      )
      GlassInputField(
        value = churchCode,
        onValueChange = {
          churchCode = it.uppercase()
          onClearError()
        },
        label = "Kòd Legliz (Opsyonèl)",
        placeholder = "Egz: CH001",
        leadingIcon = Icons.Default.AddBusiness,
        testTag = "create_church_code_input",
      )
      GlassInputField(
        value = churchAddress,
        onValueChange = { churchAddress = it },
        label = "Adrès / Vil (Opsyonèl)",
        placeholder = "Port-au-Prince",
      )

      if (useEmailTab) {
        GlassInputField(
          value = ownerEmail,
          onValueChange = {
            ownerEmail = it
            onClearError()
          },
          label = "Imèl Pwopriyetè a",
          placeholder = "pastor@legliz.org",
          leadingIcon = Icons.Default.Email,
          keyboardType = KeyboardType.Email,
          testTag = "create_church_email_input",
        )
        GlassInputField(
          value = password,
          onValueChange = {
            password = it
            onClearError()
          },
          label = "Modpas (min. 6 karaktè)",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "create_church_password_input",
        )
        GlassInputField(
          value = confirmPassword,
          onValueChange = {
            confirmPassword = it
            onClearError()
          },
          label = "Konfime Modpas",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "create_church_confirm_password_input",
        )

        if (isBusy) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(34.dp))
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
              text = "Anile",
              isPrimaryNeon = false,
              onClick = onDismiss,
              modifier = Modifier.weight(1f),
            )
            GlassButton(
              text = "Kreye Legliz",
              isPrimaryNeon = true,
              onClick = {
                onCreateWithEmailPassword(
                  churchName,
                  churchCode,
                  churchAddress,
                  ownerEmail,
                  password,
                  confirmPassword,
                )
              },
              modifier = Modifier.weight(1f),
              testTag = "create_church_email_submit_btn",
            )
          }
        }
      } else {
        Text(
          text = "Konekte ak kont Google ou pou asosye legliz sa a dirèkteman ak UID Google ou.",
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
          textAlign = TextAlign.Center,
        )
        if (isBusy) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(34.dp))
          }
        } else {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Premium3DGoogleIconButton(
              caption = "Kreye Legliz ak Google",
              onClick = {
                if (churchName.trim().length < 2) {
                  onLocalError("Tanpri antre non ofisyèl legliz la anvan.")
                  return@Premium3DGoogleIconButton
                }
                launchOptionalGoogleSignIn(
                  context = context,
                  credentialManager = credentialManager,
                  scope = scope,
                  onSuccess = {
                    onCreateWithGoogle(churchName, churchCode, churchAddress)
                  },
                  onError = { err -> onLocalError(err) },
                )
              },
              testTag = "create_church_google_submit_btn",
            )
            GlassButton(
              text = "Anile",
              isPrimaryNeon = false,
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}
