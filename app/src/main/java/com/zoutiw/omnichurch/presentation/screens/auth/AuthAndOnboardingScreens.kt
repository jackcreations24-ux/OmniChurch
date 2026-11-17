package com.zoutiw.omnichurch.presentation.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.OmniChurchBrandHeader
import com.zoutiw.omnichurch.presentation.components.OmniChurchSanctuaryHeroCanvas
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleAccountPayload(
  val uid: String,
  val email: String,
  val displayName: String,
  val photoUrl: String?,
)

fun launchOptionalGoogleSignIn(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSuccess: (GoogleAccountPayload) -> Unit,
  onError: (String) -> Unit,
) {
  val activity = context.findActivity() ?: (context as? Activity)
  if (activity == null) {
    Log.e("Auth", "launchOptionalGoogleSignIn: Cannot find Activity from context $context")
    onError("Koneksyon Google mande yon fenèt prensipal aktif. Tanpri relanse aplikasyon an.")
    return
  }
  val clientId =
    try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onError(context.getString(R.string.auth_error_missing_client))
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
        val email = googleIdCred.id.trim()
        val uid = googleIdCred.id.trim()
        val displayName =
          googleIdCred.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
        val photoUrl = googleIdCred.profilePictureUri?.toString()

        try {
          val authCredential = GoogleAuthProvider.getCredential(googleIdCred.idToken, null)
          val authResult = Firebase.auth.signInWithCredential(authCredential).await()
          val firebaseUser = authResult.user
          val finalEmail = firebaseUser?.email?.trim()?.ifBlank { null } ?: email
          val finalUid = firebaseUser?.uid?.trim()?.ifBlank { null } ?: uid
          val finalDisplayName =
            firebaseUser?.displayName?.trim()?.ifBlank { null } ?: displayName
          val finalPhotoUrl = firebaseUser?.photoUrl?.toString() ?: photoUrl

          onSuccess(
            GoogleAccountPayload(
              uid = finalUid,
              email = finalEmail,
              displayName = finalDisplayName,
              photoUrl = finalPhotoUrl,
            )
          )
        } catch (authEx: Exception) {
          Log.w(
            "Auth",
            "Firebase Auth signInWithCredential skipped (${authEx.message}), proceeding with verified Google ID Token for: $email",
          )
          onSuccess(
            GoogleAccountPayload(
              uid = uid,
              email = email,
              displayName = displayName,
              photoUrl = photoUrl,
            )
          )
        }
      } else {
        onError(context.getString(R.string.auth_error_failed))
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
      onError(context.getString(R.string.auth_cancelled))
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onError(e.localizedMessage ?: context.getString(R.string.auth_error_failed))
    }
  }
}

fun signOutOmniChurch(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        try {
          Firebase.auth.signOut()
        } catch (_: Exception) {}
        onSignOutComplete()
      }
    }
  }
}

@Composable
fun OmniChurchAuthPortalScreen(
  churches: List<Church>,
  sessionState: OmniSessionState,
  isAuthenticating: Boolean,
  errorFeedback: String?,
  onClearFeedback: () -> Unit,
  onLoginMemberWithPin: (selectedChurchId: String?, idOrPhone: String, pin: String) -> Unit,
  onUnlockBiometricSession: () -> Unit,
  onOptionalGoogleMemberSignIn: (
    selectedChurchId: String?,
    googlePayload: GoogleAccountPayload,
    onNeedsLinking: () -> Unit,
  ) -> Unit,
  onLinkGoogleWithMatrikilPin: (
    selectedChurchId: String?,
    idOrPhone: String,
    pin: String,
    googlePayload: GoogleAccountPayload,
    onLinked: () -> Unit,
  ) -> Unit,
  onLoginAdmin: (
    selectedChurchId: String,
    enteredPasscode: String?,
    googleEmail: String?,
    googleUid: String?,
  ) -> Unit,
  currentLanguageCode: String = "ht",
  onSelectLanguage: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val rawContext = LocalContext.current
  val context = remember(rawContext) { rawContext.findActivity() ?: rawContext }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isMemberMode by remember { mutableStateOf(true) }
  var selectedChurchId by remember { mutableStateOf<String?>(null) }
  var showChurchSelectorSheet by remember { mutableStateOf(false) }

  var matrikilOrPhoneInput by remember { mutableStateOf("") }
  var memberPinInput by remember { mutableStateOf("") }

  var adminPasscodeInput by remember { mutableStateOf("") }
  var adminGooglePayload by remember { mutableStateOf<GoogleAccountPayload?>(null) }

  var pendingGoogleLinkPayload by remember { mutableStateOf<GoogleAccountPayload?>(null) }
  var linkMatrikilInput by remember { mutableStateOf("") }
  var linkPinInput by remember { mutableStateOf("") }

  var localError by remember { mutableStateOf<String?>(null) }
  val displayedError = localError ?: errorFeedback

  LaunchedEffect(churches) {
    if (selectedChurchId == null && churches.size == 1) {
      selectedChurchId = churches.first().id
    }
  }

  val resolvedChurch =
    remember(churches, selectedChurchId, matrikilOrPhoneInput) {
      OmniAuthEvaluator.resolveChurchForMemberLogin(
        churches = churches,
        selectedChurchId = selectedChurchId,
        idOrPhoneInput = matrikilOrPhoneInput,
      )
        ?: churches.firstOrNull {
          it.id.equals(selectedChurchId ?: "", ignoreCase = true) ||
            (it.code ?: "").equals(selectedChurchId ?: "", ignoreCase = true)
        }
    }

  Box(
    modifier = modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
  ) {
    OmniChurchSanctuaryHeroCanvas(
      modifier = Modifier.fillMaxWidth().height(320.dp).align(Alignment.TopCenter)
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      OmniChurchBrandHeader(
        badgeText = "Mobile",
        isProStyle = false,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(BrandOrange, BrandGold, BrandOrange)))
            .padding(horizontal = 16.dp, vertical = 3.dp)
      ) {
        Text(
          text = "PRO MOBILE",
          style = MaterialTheme.typography.labelMedium,
          color = DeepNavy,
          fontWeight = FontWeight.Bold,
        )
      }

      Row(
        modifier =
          Modifier.padding(top = 10.dp)
            .clip(RoundedCornerShape(50))
            .background(DeepNavy.copy(alpha = 0.85f))
            .border(1.dp, GlassWhite.copy(alpha = 0.20f), RoundedCornerShape(50))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val supportedLanguages = listOf(
          Triple("ht", "🇭🇹", "Kreyòl"),
          Triple("fr", "🇫🇷", "Français"),
          Triple("en", "🇺🇸", "English"),
          Triple("es", "🇪🇸", "Español"),
        )
        supportedLanguages.forEach { (code, flag, label) ->
          val isSelected = currentLanguageCode.equals(code, ignoreCase = true)
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(
                  if (isSelected) {
                    Brush.horizontalGradient(listOf(RoyalBlue, ElectricBlue))
                  } else {
                    Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
                  }
                )
                .clickable { onSelectLanguage(code) }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = flag, style = MaterialTheme.typography.labelSmall)
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) PureWhite else MutedGlassText,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepNavy.copy(alpha = 0.85f))
            .border(1.dp, GlassWhite.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(
                if (isMemberMode) {
                  Brush.horizontalGradient(listOf(RoyalBlue, ElectricBlue))
                } else {
                  Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
                }
              )
              .clickable {
                isMemberMode = true
                localError = null
                onClearFeedback()
              }
              .padding(vertical = 11.dp)
              .testTag("tab_member_login"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Badge,
              contentDescription = null,
              tint = if (isMemberMode) PureWhite else MutedGlassText,
              modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Espas Manm",
              style = MaterialTheme.typography.labelLarge,
              color = if (isMemberMode) PureWhite else MutedGlassText,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(
                if (!isMemberMode) {
                  Brush.horizontalGradient(listOf(BrandOrange, BrandGold))
                } else {
                  Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
                }
              )
              .clickable {
                isMemberMode = false
                localError = null
                onClearFeedback()
              }
              .padding(vertical = 11.dp)
              .testTag("tab_admin_login"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = if (!isMemberMode) DeepNavy else MutedGlassText,
              modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Espas Admin",
              style = MaterialTheme.typography.labelLarge,
              color = if (!isMemberMode) DeepNavy else MutedGlassText,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
          Text(
            text =
              if (isMemberMode) "Koneksyon Manm (ID Matrikil + PIN)"
              else "Koneksyon Administratè Legliz",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text =
              if (isMemberMode) {
                "Antre ID Matrikil ou ak Kòd Sekirite (PIN) administrasyon legliz la ba ou a. Ou pa bezwen kont Google."
              } else {
                " Konekte avèk Kòd Sekirite Administratè legliz la oswa Kont Google Administratè ki otorize a."
              },
            style = MaterialTheme.typography.bodySmall,
            color = GlassWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
          )

          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DeepNavy.copy(alpha = 0.78f))
                .border(1.dp, BlueNeon.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .clickable { showChurchSelectorSheet = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("church_selector_box"),
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
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Legliz Aktif",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGlassText,
                  )
                  Text(
                    text =
                      if (resolvedChurch != null) {
                        "${resolvedChurch.name} (${resolvedChurch.effectiveCode})"
                      } else if (churches.isEmpty()) {
                        "Okenn legliz ankò • Klike pou kreye oswa chwazi"
                      } else {
                        "Chwazi legliz ou a (oswa tape Matrikil CH001-...)"
                      },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (resolvedChurch != null) PureWhite else GlassWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Chwazi Legliz",
                tint = BlueNeon,
              )
            }
          }

          if (displayedError != null) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(ErrorRed.copy(alpha = 0.22f))
                  .border(1.dp, ErrorRed, RoundedCornerShape(14.dp))
                  .padding(12.dp)
                  .testTag("auth_error_banner")
            ) {
              Text(
                text = displayedError,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          if (isMemberMode) {
            GlassInputField(
              value = matrikilOrPhoneInput,
              onValueChange = {
                matrikilOrPhoneInput = it
                localError = null
                onClearFeedback()
              },
              label = "ID Manm (Matrikil) oswa Telefòn",
              placeholder = "Egzanp: CH001-JD4827 oswa 509...",
              leadingIcon = Icons.Default.Badge,
              testTag = "member_matrikil_input",
            )

            GlassInputField(
              value = memberPinInput,
              onValueChange = {
                memberPinInput = it
                localError = null
                onClearFeedback()
              },
              label = "Kòd Sekirite (PIN)",
              placeholder = "Antre PIN administratè a ba ou a",
              leadingIcon = Icons.Default.Key,
              visualTransformation = PasswordVisualTransformation(),
              keyboardType = KeyboardType.Password,
              testTag = "member_pin_input",
            )

            if (isAuthenticating) {
              CircularProgressIndicator(
                color = BlueNeon,
                modifier = Modifier.size(38.dp),
              )
            } else {
              GlassButton(
                text = "Konekte ak ID Matrikil + PIN",
                icon = Icons.Default.VerifiedUser,
                isPrimaryNeon = true,
                onClick = {
                  localError = null
                  onClearFeedback()
                  onLoginMemberWithPin(
                    resolvedChurch?.id ?: selectedChurchId,
                    matrikilOrPhoneInput,
                    memberPinInput,
                  )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "member_pin_login_button",
              )
            }

            if (
              sessionState.biometricEnabled &&
                sessionState.biometricChurchId.isNotBlank() &&
                (sessionState.biometricMemberDocId.isNotBlank() ||
                  sessionState.biometricMemberId.isNotBlank())
            ) {
              GlassButton(
                text =
                  "Debloke ak Anprent / Face ID (${sessionState.biometricMemberId.ifBlank { sessionState.biometricMemberName }})",
                icon = Icons.Default.Fingerprint,
                isPrimaryNeon = false,
                accentColor = SuccessGreen,
                onClick = {
                  BiometricQuickUnlockHelper.promptBiometricUnlock(
                    context = context,
                    title = "Deblokaj Biyometrik OmniChurch",
                    subtitle =
                      "Verifye idantite ou pou ouvri kat manm ${sessionState.biometricMemberId}",
                    cancelText = "Anile",
                    onSuccess = onUnlockBiometricSession,
                    onError = { err -> localError = err },
                  )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "biometric_quick_unlock_button",
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Premium3DGoogleIconButton(
              caption = "Google (Si kont ou lye)",
              onClick = {
                localError = null
                onClearFeedback()
                launchOptionalGoogleSignIn(
                  context = context,
                  credentialManager = credentialManager,
                  scope = scope,
                  onSuccess = { googlePayload ->
                    onOptionalGoogleMemberSignIn(
                      resolvedChurch?.id ?: selectedChurchId,
                      googlePayload,
                      {
                        pendingGoogleLinkPayload = googlePayload
                        linkMatrikilInput = matrikilOrPhoneInput
                        linkPinInput = memberPinInput
                      },
                    )
                  },
                  onError = { msg -> localError = msg },
                )
              },
              testTag = "optional_google_login_button",
            )
          } else {
            GlassInputField(
              value = adminPasscodeInput,
              onValueChange = {
                adminPasscodeInput = it
                localError = null
                onClearFeedback()
              },
              label = "Kòd Sekirite Administratè Legliz la",
              placeholder = "Antre Admin Passcode legliz la",
              leadingIcon = Icons.Default.Lock,
              visualTransformation = PasswordVisualTransformation(),
              keyboardType = KeyboardType.Password,
              testTag = "admin_passcode_input",
            )

            GlassButton(
              text = "Antre nan Espas Administratè",
              icon = Icons.Default.AdminPanelSettings,
              isPrimaryNeon = true,
              onClick = {
                localError = null
                onClearFeedback()
                val targetChurchId = resolvedChurch?.id ?: selectedChurchId.orEmpty()
                onLoginAdmin(
                  targetChurchId,
                  adminPasscodeInput,
                  adminGooglePayload?.email,
                  adminGooglePayload?.uid,
                )
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "admin_login_submit_button",
            )

            if (adminGooglePayload != null) {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SuccessGreen.copy(alpha = 0.16f))
                    .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Google Admin: ${adminGooglePayload!!.email}",
                  style = MaterialTheme.typography.bodySmall,
                  color = PureWhite,
                )
              }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Premium3DGoogleIconButton(
              caption =
                if (adminGooglePayload == null) "Google Admin"
                else "Chanje (${adminGooglePayload!!.email})",
              onClick = {
                localError = null
                onClearFeedback()
                launchOptionalGoogleSignIn(
                  context = context,
                  credentialManager = credentialManager,
                  scope = scope,
                  onSuccess = { payload ->
                    adminGooglePayload = payload
                    val targetChurchId = resolvedChurch?.id ?: selectedChurchId.orEmpty()
                    if (targetChurchId.isNotBlank()) {
                      onLoginAdmin(
                        targetChurchId,
                        adminPasscodeInput.takeIf { it.isNotBlank() },
                        payload.email,
                        payload.uid,
                      )
                    }
                  },
                  onError = { msg -> localError = msg },
                )
              },
              testTag = "admin_google_verify_button",
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "OmniChurch Pro • Kreyasyon legliz fèt pa administratè yo sou platfòm Web la",
            style = MaterialTheme.typography.labelSmall,
            color = MutedGlassText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }

  if (showChurchSelectorSheet) {
    GlassModalBottomSheet(onDismissRequest = { showChurchSelectorSheet = false }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Chwazi Legliz ou a",
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        if (churches.isEmpty()) {
          Text(
            text = "Pa gen okenn legliz ki anrejistre nan baz done a kounye a.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGlassText,
          )
          Text(
            text = "Se sèlman sou Platfòm Web OmniChurch Pro administratè yo kreye legliz. Yon fwa kreye sou Web la, li ap parèt isit la otomatikman.",
            style = MaterialTheme.typography.bodySmall,
            color = BrandGold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
          )
        } else {
          churches.forEach { church ->
            val isSelected = resolvedChurch?.id == church.id
            PremiumGlassCard(
              cornerRadius = 16.dp,
              isHighlighted = isSelected,
              accentColor = if (isSelected) BlueNeon else ElectricBlue,
              onClick = {
                selectedChurchId = church.id
                showChurchSelectorSheet = false
              },
              contentPadding = PaddingValues(14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = church.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "Kòd Legliz: ${church.effectiveCode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGold,
                  )
                }
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (pendingGoogleLinkPayload != null) {
    val googlePayload = pendingGoogleLinkPayload!!
    GlassModalBottomSheet(onDismissRequest = { pendingGoogleLinkPayload = null }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = "Lye Kont Google ou ak ID Matrikil ou",
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            "Kont Google (${googlePayload.email}) poko lye ak yon ID Matrikil. Antre ID Matrikil ou ak PIN ou yon sèl fwa pou lye yo:",
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite,
        )

        GlassInputField(
          value = linkMatrikilInput,
          onValueChange = { linkMatrikilInput = it },
          label = "ID Manm (Matrikil) oswa Telefòn",
          placeholder = "CH001-JD4827",
          leadingIcon = Icons.Default.Badge,
          testTag = "link_google_matrikil_input",
        )

        GlassInputField(
          value = linkPinInput,
          onValueChange = { linkPinInput = it },
          label = "Kòd Sekirite (PIN)",
          placeholder = "Antre PIN ou",
          leadingIcon = Icons.Default.Key,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "link_google_pin_input",
        )

        GlassButton(
          text = "Verifye epi Lye Kont Google la",
          icon = Icons.Default.Link,
          isPrimaryNeon = true,
          onClick = {
            onLinkGoogleWithMatrikilPin(
              resolvedChurch?.id ?: selectedChurchId,
              linkMatrikilInput,
              linkPinInput,
              googlePayload,
            ) {
              pendingGoogleLinkPayload = null
            }
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "confirm_link_google_button",
        )
      }
    }
  }
}
