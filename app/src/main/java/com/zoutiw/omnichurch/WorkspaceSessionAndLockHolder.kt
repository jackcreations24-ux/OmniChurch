package com.zoutiw.omnichurch

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem
import com.zoutiw.omnichurch.data.models.OmniRoleEvaluator
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.navigation.AdminAuthViewModel
import com.zoutiw.omnichurch.presentation.navigation.OmniChurchViewModel
import com.zoutiw.omnichurch.presentation.screens.auth.AutoLockWarningDialog
import com.zoutiw.omnichurch.presentation.screens.auth.BlockingAdminAuthGateOverlay
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.CoroutineScope

internal data class WorkspaceNavContext(
  val context: Context,
  val scope: CoroutineScope,
  val navController: NavHostController,
  val viewModel: OmniChurchViewModel,
  val adminAuthViewModel: AdminAuthViewModel,
  val preferencesManager: OmniPreferencesManager,
  val adMobController: AdMobRemoteController,
  val verificationConfig: RemoteMemberVerificationConfig,
  val donationConfig: DeveloperDonationConfig,
  val currentLanguageCode: String,
  val sessionState: OmniSessionState,
  val activeChurch: Church?,
  val userProfile: UserProfile?,
  val member: Member?,
  val currentMemberState: UiState<Member?>,
  val servicesState: UiState<List<ChurchService>>,
  val availableServicesList: List<ChurchService>,
  val activeAttendanceServices: List<ChurchService>,
  val churchMembers: List<Member>,
  val servicePickerMembers: List<Member>,
  val churchMembersUiState: UiState<List<Member>>,
  val announcementsState: UiState<List<Announcement>>,
  val unreadAnnouncementIds: Set<String>,
  val unreadNotificationsCount: Int,
  val reactionSummaries: Map<String, TargetReactionSummary>,
  val commentCounts: Map<String, Int>,
  val notificationsState: UiState<List<ChurchNotification>>,
  val memberQrBitmap: Bitmap?,
  val scannerOutcome: ScannerValidationOutcome,
  val cardVerificationState: UiState<PublicVerificationResolution?>,
  val selectedScannerServiceId: String?,
  val memberAttendanceHistory: List<MemberAttendanceHistoryItem>,
  val isAdminSession: Boolean,
  val isActiveDeacon: Boolean,
  val canManageServices: Boolean,
  val canPublishAnnouncements: Boolean,
  val canScanAttendance: Boolean,
  val currentAuthUid: String,
  val resolvedProfileAvatarUrl: String?,
  val adminAuthBusy: Boolean,
  val resolvedChurchId: String,
  val resolvedChurchName: String,
  val resolvedChurchLogoUrl: String?,
  val resolvedAuthorName: String,
  val isChromeVisible: Boolean,
  val onChromeVisibilityChange: (Boolean) -> Unit,
  val navigateToTab: (String) -> Unit,
  val navigateBackOrHome: () -> Unit,
  val onOpenDonate: () -> Unit,
  val performFullSignOut: () -> Unit,
)

@Immutable
internal data class ResolvedWorkspaceIdentity(
  val isAdminSession: Boolean,
  val isActiveDeacon: Boolean,
  val canManageServices: Boolean,
  val canPublishAnnouncements: Boolean,
  val canScanAttendance: Boolean,
  val isAdminLocked: Boolean,
  val resolvedChurchId: String,
  val resolvedChurchName: String,
  val resolvedChurchLogoUrl: String?,
  val resolvedAuthorName: String,
  val resolvedBottomNavAvatarUrl: String?,
  val resolvedBottomNavDisplayName: String,
)

@Composable
internal fun rememberResolvedWorkspaceIdentity(
  sessionState: OmniSessionState,
  userProfile: UserProfile?,
  member: Member?,
  currentChurch: Church?,
  lockedChurches: Map<String, Boolean>,
  adminFirebaseUser: FirebaseUser?,
): ResolvedWorkspaceIdentity =
  remember(
    sessionState,
    userProfile,
    member,
    currentChurch,
    lockedChurches,
    adminFirebaseUser,
  ) {
    val isAdminSession = sessionState.isAdminSession || userProfile?.isAdminOrSuperAdmin == true
    val isActiveDeacon = !isAdminSession && OmniRoleEvaluator.isActiveDeacon(member)
    val canManageServices =
      OmniRoleEvaluator.canManageServices(
        member = member,
        isAdmin = isAdminSession,
      )
    val canPublishAnnouncements =
      OmniRoleEvaluator.canPublishAnnouncements(
        member = member,
        isAdmin = isAdminSession,
      )
    val canScanAttendance =
      OmniRoleEvaluator.canScanAttendance(
        member = member,
        isAdmin = isAdminSession,
      ) || userProfile?.isAuthorizedScanner == true

    val isAdminLocked =
      isAdminSession &&
        currentChurch != null &&
        (
          lockedChurches[currentChurch.id] == true ||
            (adminFirebaseUser == null && currentChurch.hasCloudOwner)
        )

    val resolvedChurchId =
      currentChurch?.id?.trim()?.takeIf { it.isNotBlank() }
        ?: sessionState.activeChurchId.trim().takeIf { it.isNotBlank() }
        ?: member?.churchId?.trim()?.takeIf { it.isNotBlank() }
        ?: userProfile?.churchId?.trim().orEmpty()
    val resolvedChurchName =
      currentChurch?.name?.trim()?.takeIf { it.isNotBlank() }
        ?: member?.churchName?.trim()?.takeIf { it.isNotBlank() }
        ?: userProfile?.churchName?.trim()?.takeIf { it.isNotBlank() }
        ?: "OmniChurch Pro"
    val resolvedChurchLogoUrl =
      currentChurch?.logoUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: member?.churchLogoUrl?.trim()?.takeIf { it.isNotEmpty() }
    val resolvedAuthorName =
      userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
        ?: member?.effectiveFullName?.trim()?.takeIf { it.isNotBlank() }
        ?: currentChurch?.pastor?.trim()?.takeIf { it.isNotBlank() }
        ?: "Admin"

    val resolvedBottomNavAvatarUrl: String? =
      if (member != null) {
        val hasExplicitMemberPhotoState =
          member.avatarUrl != null || member.photoUrl != null || member.googlePhoto != null
        member.effectivePhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
          ?: if (hasExplicitMemberPhotoState) {
            null
          } else {
            userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
          }
      } else if (isAdminSession) {
        if (currentChurch?.adminGooglePhoto != null) {
          currentChurch.adminGooglePhoto.trim().takeIf { it.isNotEmpty() }
        } else {
          userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: adminFirebaseUser?.photoUrl?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }
      } else {
        userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
      }

    val resolvedBottomNavDisplayName =
      member?.effectiveFullName?.trim()?.takeIf { it.isNotBlank() }
        ?: userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
        ?: currentChurch?.adminGoogleName?.trim()?.takeIf { it.isNotBlank() }
        ?: resolvedAuthorName

    ResolvedWorkspaceIdentity(
      isAdminSession = isAdminSession,
      isActiveDeacon = isActiveDeacon,
      canManageServices = canManageServices,
      canPublishAnnouncements = canPublishAnnouncements,
      canScanAttendance = canScanAttendance,
      isAdminLocked = isAdminLocked,
      resolvedChurchId = resolvedChurchId,
      resolvedChurchName = resolvedChurchName,
      resolvedChurchLogoUrl = resolvedChurchLogoUrl,
      resolvedAuthorName = resolvedAuthorName,
      resolvedBottomNavAvatarUrl = resolvedBottomNavAvatarUrl,
      resolvedBottomNavDisplayName = resolvedBottomNavDisplayName,
    )
  }

@Composable
internal fun ObserveAdminWorkspaceAutoLock(
  adminAuthViewModel: AdminAuthViewModel,
  isAdminSession: Boolean,
  isAdminLocked: Boolean,
  currentChurch: Church?,
) {
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(
    lifecycleOwner,
    isAdminSession,
    currentChurch?.id,
    currentChurch?.effectiveAutoLockMinutes,
  ) {
    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_STOP -> adminAuthViewModel.onAppStopped(isAdminSession)
          Lifecycle.Event.ON_START -> adminAuthViewModel.onAppStarted(isAdminSession, currentChurch)
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(
    isAdminSession,
    isAdminLocked,
    currentChurch?.id,
    currentChurch?.effectiveAutoLockMinutes,
  ) {
    adminAuthViewModel.updateInactivityWatcher(
      isAdminSession = isAdminSession,
      isAdminUnlocked = !isAdminLocked,
      church = currentChurch,
    )
  }
}

@Composable
internal fun WorkspaceAdminSecurityOverlays(
  identity: ResolvedWorkspaceIdentity,
  currentChurch: Church?,
  adminAuthViewModel: AdminAuthViewModel,
  adminFirebaseUser: FirebaseUser?,
  adminAuthBusy: Boolean,
  adminAuthError: String?,
  adminAuthInfo: String?,
  autoLockWarningSeconds: Int?,
  legacyVerifiedChurchId: String?,
  legacyVerifiedPin: String?,
  currentLanguageCode: String,
  onExitToAuthPortal: () -> Unit,
) {
  if (identity.isAdminSession && !identity.isAdminLocked && autoLockWarningSeconds != null && currentChurch != null) {
    AutoLockWarningDialog(
      secondsRemaining = autoLockWarningSeconds,
      currentLanguageCode = currentLanguageCode,
      onStayActive = { adminAuthViewModel.recordUserInteraction() },
      onLockNow = { adminAuthViewModel.lockChurchNow(currentChurch.id) },
    )
  }

  if (identity.isAdminLocked && currentChurch != null) {
    BlockingAdminAuthGateOverlay(
      church = currentChurch,
      firebaseUser = adminFirebaseUser,
      isBusy = adminAuthBusy,
      errorMessage = adminAuthError,
      infoMessage = adminAuthInfo,
      legacyVerifiedChurchId = legacyVerifiedChurchId,
      legacyVerifiedPin = legacyVerifiedPin,
      currentLanguageCode = currentLanguageCode,
      onClearMessages = { adminAuthViewModel.clearMessages() },
      onSignInEmailPassword = { email, password ->
        adminAuthViewModel.signInAdminWithEmailPassword(
          church = currentChurch,
          email = email,
          password = password,
          languageCode = currentLanguageCode,
          onSuccess = {},
        )
      },
      onSendPasswordReset = { email ->
        adminAuthViewModel.sendPasswordResetEmail(email, currentLanguageCode)
      },
      onGoogleVerified = {
        adminAuthViewModel.verifySignedInAdminGoogleAccount(
          church = currentChurch,
          languageCode = currentLanguageCode,
          onSuccess = {},
        )
      },
      onQuickPinSubmit = { enteredPin ->
        adminAuthViewModel.unlockWithQuickPin(
          church = currentChurch,
          pin = enteredPin,
          languageCode = currentLanguageCode,
          onSuccess = {},
        )
      },
      onBiometricSuccess = {
        adminAuthViewModel.authorizeViaBiometric(
          church = currentChurch,
          languageCode = currentLanguageCode,
          onSuccess = {},
        )
      },
      onVerifyLegacyPin = { pin ->
        adminAuthViewModel.verifyLegacyPinStep(currentChurch, pin, currentLanguageCode)
      },
      onCompleteLegacyMigration = { email, newPass, confirmPass ->
        adminAuthViewModel.completeLegacyMigration(
          church = currentChurch,
          adminEmail = email,
          newPassword = newPass,
          confirmPassword = confirmPass,
          languageCode = currentLanguageCode,
          onSuccess = {},
        )
      },
      onResetLegacyMigration = { adminAuthViewModel.resetLegacyMigrationStep() },
      onLocalError = { msg -> adminAuthViewModel.setAuthError(msg) },
      onExitToAuthPortal = onExitToAuthPortal,
    )
  }
}

internal object WorkspaceFeedbackMessages {
  fun googleLinked(lang: String, email: String): String =
    when (lang.lowercase()) {
      "fr" -> "Compte Google ($email) lié avec succès !"
      "en" -> "Google account ($email) linked successfully!"
      "es" -> "¡Cuenta de Google ($email) vinculada con éxito!"
      else -> "Kont Google ($email) lye avèk siksè!"
    }

  fun memberPhotoUpdated(lang: String, cleared: Boolean): String =
    when (lang.lowercase()) {
      "fr" -> if (cleared) "Photo de profil supprimée !" else "Photo de profil mise à jour avec succès !"
      "en" -> if (cleared) "Profile photo removed!" else "Profile photo updated successfully!"
      "es" -> if (cleared) "¡Foto de perfil eliminada!" else "¡Foto de perfil actualizada con éxito!"
      else -> if (cleared) "Foto pwofil ou retire avèk siksè!" else "Foto pwofil ou mete ajou avèk siksè!"
    }

  fun adminPhotoUpdated(lang: String, cleared: Boolean): String =
    when (lang.lowercase()) {
      "fr" -> if (cleared) "Photo administrateur supprimée !" else "Photo administrateur mise à jour !"
      "en" -> if (cleared) "Admin photo removed!" else "Admin photo updated!"
      "es" -> if (cleared) "¡Foto de administrador eliminada!" else "¡Foto de administrador actualizada!"
      else -> if (cleared) "Foto administratè a retire avèk siksè!" else "Foto administratè a mete ajou avèk siksè!"
    }

  fun churchLogoUpdated(lang: String, cleared: Boolean): String =
    when (lang.lowercase()) {
      "fr" -> if (cleared) "Logo de l'église supprimé !" else "Logo de l'église mis à jour !"
      "en" -> if (cleared) "Church logo removed!" else "Church logo updated!"
      "es" -> if (cleared) "¡Logo de la iglesia eliminado!" else "¡Logo de la iglesia actualizado!"
      else -> if (cleared) "Logo legliz la retire avèk siksè!" else "Logo legliz la mete ajou avèk siksè!"
    }

  fun memberRegistered(lang: String, fullName: String, matrikil: String): String =
    when (lang.lowercase()) {
      "fr" -> "Membre $fullName enregistré ! ID Matricule : $matrikil"
      "en" -> "Member $fullName registered! Member ID: $matrikil"
      "es" -> "¡Miembro $fullName registrado! ID Matrícula: $matrikil"
      else -> "Manm $fullName anrejistre! ID Matrikil: $matrikil"
    }

  fun securitySettingsSaved(lang: String): String =
    when (lang.lowercase()) {
      "fr" -> "Paramètres de sécurité enregistrés !"
      "en" -> "Security settings saved!"
      "es" -> "¡Configuración de seguridad guardada!"
      else -> "Paramèt sekirite ak PIN rapid yo anrejistre!"
    }
}

