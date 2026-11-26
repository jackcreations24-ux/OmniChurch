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
