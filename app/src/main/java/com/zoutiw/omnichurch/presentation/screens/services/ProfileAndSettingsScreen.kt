package com.zoutiw.omnichurch.presentation.screens.services

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.core.utilities.ProfileStrings
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.screens.auth.ChurchAdminSecuritySettingsCard
import com.zoutiw.omnichurch.presentation.screens.auth.GoogleAccountPayload
import kotlinx.coroutines.launch

@Composable
fun ProfileAndSettingsScreen(
  userProfile: UserProfile?,
  member: Member?,
  currentLanguageCode: String,
  onLanguageChange: (String) -> Unit,
  onSaveMemberChanges: (fullName: String, department: String, role: String) -> Unit,
  onSignOut: () -> Unit,
  onBackClick: () -> Unit,
  biometricEnabled: Boolean = false,
  onToggleBiometric: (Boolean) -> Unit = {},
  onLinkGoogleAccount: (GoogleAccountPayload) -> Unit = {},
  onUnlinkGoogleAccount: () -> Unit = {},
  onUpdateMemberPhoto: ((String) -> Unit)? = null,
  onUpdateAdminPhoto: ((String) -> Unit)? = null,
  onUpdateChurchLogo: ((String) -> Unit)? = null,
  onRegisterNewMember:
    ((
      fullName: String,
      customMatrikil: String?,
      pin: String,
      phone: String?,
      department: String,
      ministryRole: String,
    ) -> Unit)? =
    null,
  churchMembers: List<Member> = emptyList(),
  onAdminUpdateMember:
    ((
      memberDocId: String,
      fullName: String,
      department: String,
      ministryRole: String,
      phone: String?,
      titlePrefix: String?,
    ) -> Unit)? =
    null,
  activeChurch: Church? = null,
  isAdminSession: Boolean = false,
  isAdminSecurityBusy: Boolean = false,
  onSaveChurchSecuritySettings:
    ((quickPinEnabled: Boolean, newPinOrNull: String?, autoLockMinutes: Int) -> Unit)? =
    null,
  onLockAdminNow: (() -> Unit)? = null,
  unreadNotificationsCount: Int = 0,
  onOpenNotifications: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val activityContext = remember(context) { context.findActivity() ?: context }
  val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
  val scope = rememberCoroutineScope()
  val copy = remember(currentLanguageCode) { ProfileStrings.forLanguage(currentLanguageCode) }

  val isAdmin = isAdminSession || userProfile?.isAdminOrSuperAdmin == true
  val fullName =
    remember(member?.fullName, userProfile?.displayName, activeChurch?.adminGoogleName, activeChurch?.name) {
      member?.fullName?.takeIf { it.isNotBlank() }
        ?: userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: activeChurch?.adminGoogleName?.takeIf { it.isNotBlank() }
        ?: activeChurch?.name.orEmpty()
    }
  val activeProfilePhotoUrl: String? =
    when {
      member != null -> member.effectivePhotoUrl
      activeChurch?.adminGooglePhoto != null -> activeChurch.adminGooglePhoto.trim().takeIf { it.isNotEmpty() }
      else -> userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
    }
  val activeChurchLogoUrl: String? = activeChurch?.logoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val canEditProfilePhoto =
    (member != null && onUpdateMemberPhoto != null) || (isAdmin && onUpdateAdminPhoto != null)

  var isLinkingGoogle by remember { mutableStateOf(false) }
  var showPhotoSourceDialog by remember { mutableStateOf(false) }
  var photoPickerTarget by remember { mutableStateOf("profile") } // "profile" | "church_logo"
  var isCompressingPhoto by remember { mutableStateOf(false) }

  val dispatchPhotoChange: (String) -> Unit = { dataOrEmpty ->
    if (photoPickerTarget == "church_logo") {
      onUpdateChurchLogo?.invoke(dataOrEmpty)
    } else if (member != null && onUpdateMemberPhoto != null) {
      onUpdateMemberPhoto.invoke(dataOrEmpty)
    } else if (isAdmin && onUpdateAdminPhoto != null) {
      onUpdateAdminPhoto.invoke(dataOrEmpty)
    }
  }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          isCompressingPhoto = true
          val result = ImageCompressor.compressUriToBase64(context, uri)
          isCompressingPhoto = false
          if (result.isSuccess) {
            dispatchPhotoChange(result.getOrThrow())
          } else {
            Toast.makeText(
                context,
                copy.photoError.format(result.exceptionOrNull()?.localizedMessage.orEmpty()),
                Toast.LENGTH_SHORT,
              )
              .show()
          }
        }
      }
    }

  val takePictureLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
      if (bitmap != null) {
        scope.launch {
          isCompressingPhoto = true
          val base64 = ImageCompressor.compressBitmapToBase64(bitmap)
          isCompressingPhoto = false
          dispatchPhotoChange(base64)
        }
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        try {
          takePictureLauncher.launch(null)
        } catch (e: Exception) {
          Toast.makeText(context, copy.cameraError.format(e.localizedMessage.orEmpty()), Toast.LENGTH_SHORT).show()
        }
      } else {
        Toast.makeText(context, copy.cameraDenied, Toast.LENGTH_LONG).show()
      }
    }

  val churchNameDisplay =
    activeChurch?.name?.takeIf { it.isNotBlank() }
      ?: member?.churchName?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchName?.takeIf { it.isNotBlank() }
      ?: "OmniChurch Pro"

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 110.dp),
  ) {
    GlassTopBar(
      title = copy.screenTitle,
      onBackClick = onBackClick,
    )

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Member / Admin Summary Card + Church Logo + Notifications Shortcut
      ProfileSummaryHeaderCard(
        context = context,
        fullName = fullName,
        activeProfilePhotoUrl = activeProfilePhotoUrl,
        activeChurchLogoUrl = activeChurchLogoUrl,
        canEditProfilePhoto = canEditProfilePhoto,
        isCompressingPhoto = isCompressingPhoto,
        isAdmin = isAdmin,
        activeChurch = activeChurch,
        hasChurchLogoHandler = onUpdateChurchLogo != null,
        member = member,
        userProfile = userProfile,
        churchNameDisplay = churchNameDisplay,
        currentLanguageCode = currentLanguageCode,
        copy = copy,
        unreadNotificationsCount = unreadNotificationsCount,
        onOpenNotifications = onOpenNotifications,
        onPickProfilePhoto = {
          photoPickerTarget = "profile"
          showPhotoSourceDialog = true
        },
        onPickChurchLogo = {
          photoPickerTarget = "church_logo"
          showPhotoSourceDialog = true
        },
      )

      // 1b. Optional Google Account Linking & Biometric Quick Unlock
      if (member != null) {
        ProfileGoogleAndBiometricSecurityCard(
          member = member,
          copy = copy,
          context = context,
          activityContext = activityContext,
          credentialManager = credentialManager,
          scope = scope,
          isLinkingGoogle = isLinkingGoogle,
          onLinkingGoogleChange = { isLinkingGoogle = it },
          onLinkGoogleAccount = onLinkGoogleAccount,
          onUnlinkGoogleAccount = onUnlinkGoogleAccount,
          biometricEnabled = biometricEnabled,
          onToggleBiometric = onToggleBiometric,
        )
      }

      if (isAdmin && activeChurch != null && onSaveChurchSecuritySettings != null && onLockAdminNow != null) {
        ChurchAdminSecuritySettingsCard(
          church = activeChurch,
          isSaving = isAdminSecurityBusy,
          currentLanguageCode = currentLanguageCode,
          onSaveSettings = onSaveChurchSecuritySettings,
          onLockAdminNow = onLockAdminNow,
        )
      }

      // 2. Multilingual Language Switcher
      ProfileLanguageSwitcherCard(
        currentLanguageCode = currentLanguageCode,
        copy = copy,
        onLanguageChange = onLanguageChange,
      )

      // 3. Member Personal Info
      if (member != null) {
        ProfileMemberPersonalInfoCard(
          member = member,
          churchNameDisplay = churchNameDisplay,
          currentLanguageCode = currentLanguageCode,
          copy = copy,
        )
      }

      // 4. Sign Out
      GlassButton(
        text = copy.signOut,
        icon = Icons.AutoMirrored.Filled.Logout,
        isPrimaryNeon = false,
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth(),
        testTag = "profile_sign_out_btn",
      )
    }

    if (showPhotoSourceDialog) {
      val isLogoMode = photoPickerTarget == "church_logo"
      val hasTargetPhoto =
        if (isLogoMode) !activeChurchLogoUrl.isNullOrBlank() else !activeProfilePhotoUrl.isNullOrBlank()
      val logoSheetTitle =
        when (currentLanguageCode.trim().lowercase()) {
          "fr" -> "Logo de l'église"
          "es" -> "Logo de la iglesia"
          "en" -> "Church Logo"
          else -> "Logo Legliz la"
        }
      ProfilePhotoSourceBottomSheet(
        copy = copy,
        isCompressingPhoto = isCompressingPhoto,
        hasExistingPhoto = hasTargetPhoto,
        languageCode = currentLanguageCode,
        titleOverride = if (isLogoMode) logoSheetTitle else null,
        onTakePhotoClick = {
          showPhotoSourceDialog = false
          val hasCamPermission =
            ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.CAMERA,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
          if (hasCamPermission) {
            try {
              takePictureLauncher.launch(null)
            } catch (e: Exception) {
              Toast.makeText(context, copy.cameraError.format(e.localizedMessage.orEmpty()), Toast.LENGTH_SHORT).show()
            }
          } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
          }
        },
        onPickGalleryClick = {
          showPhotoSourceDialog = false
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        onRemovePhotoClick = {
          showPhotoSourceDialog = false
          dispatchPhotoChange("")
        },
        onDismiss = { showPhotoSourceDialog = false },
      )
    }
  }
}
