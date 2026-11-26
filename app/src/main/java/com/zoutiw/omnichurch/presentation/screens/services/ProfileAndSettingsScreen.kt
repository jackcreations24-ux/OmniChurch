package com.zoutiw.omnichurch.presentation.screens.services

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.core.utilities.OmniLocalization
import com.zoutiw.omnichurch.core.utilities.ProfileStrings
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.components.shareOmniChurchGeneralApp
import com.zoutiw.omnichurch.presentation.screens.auth.ChurchAdminSecuritySettingsCard
import com.zoutiw.omnichurch.presentation.screens.auth.GoogleAccountPayload
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PureWhite
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
  val copy = remember(currentLanguageCode) {
    ProfileStrings.forLanguage(currentLanguageCode)
  }

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
        Toast.makeText(
            context,
            copy.cameraDenied,
            Toast.LENGTH_LONG,
          )
          .show()
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
      // 1. Member Summary Card
      PremiumGlassCard(
        cornerRadius = 22.dp,
        isHighlighted = true,
        contentPadding = PaddingValues(18.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.BottomEnd,
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable(
                  enabled = canEditProfilePhoto,
                  onClick = {
                    photoPickerTarget = "profile"
                    showPhotoSourceDialog = true
                  },
                )
                .testTag("profile_avatar_box"),
          ) {
            MemberAvatar3D(
              fullName = fullName,
              size = 68.dp,
              photoUrl = activeProfilePhotoUrl,
              showVerifiedBadge = true,
            )
            if (canEditProfilePhoto) {
              Box(
                modifier =
                  Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue)
                    .border(1.5.dp, PureWhite, CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.CameraAlt,
                  contentDescription = copy.changePhoto,
                  tint = PureWhite,
                  modifier = Modifier.size(13.dp),
                )
              }
            }
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = fullName.ifBlank { "${OmniLocalization.defaultMemberName(currentLanguageCode)} OmniChurch" },
              style = MaterialTheme.typography.headlineMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            val secondaryInfo =
              member?.googleEmail?.takeIf { it.isNotBlank() }
                ?: userProfile?.email?.takeIf { it.isNotBlank() }
                ?: member?.phone?.takeIf { it.isNotBlank() }
                ?: copy.sessionPinSub
            Text(
              text = secondaryInfo,
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite.copy(alpha = 0.85f),
            )
            if (member != null) {
              val matrikil = member.effectiveMatrikil
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier.clickable {
                    val clipboard =
                      context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                      ClipData.newPlainText("OmniChurch Matrikil", matrikil)
                    )
                    Toast.makeText(context, copy.copiedToast.format(matrikil), Toast.LENGTH_SHORT)
                      .show()
                  },
              ) {
                Text(
                  text = "$churchNameDisplay • ID: $matrikil",
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandGold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = null,
                  tint = BrandGold,
                  modifier = Modifier.size(14.dp),
                )
              }
            }
          }
        }

        if (isCompressingPhoto) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x3300E5FF))
                .padding(horizontal = 12.dp, vertical = 8.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = ElectricBlue,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = copy.updatingPhoto,
              style = MaterialTheme.typography.bodySmall,
              color = PureWhite,
            )
          }
        }

        if (isAdmin && activeChurch != null && onUpdateChurchLogo != null) {
          val churchLogoTitle =
            when (currentLanguageCode.trim().lowercase()) {
              "fr" -> "Logo de l'église"
              "es" -> "Logo de la iglesia"
              "en" -> "Church Logo"
              else -> "Logo Legliz la"
            }
          val churchLogoAction =
            when (currentLanguageCode.trim().lowercase()) {
              "fr" -> if (activeChurchLogoUrl != null) "Modifier / Supprimer" else "Ajouter un logo"
              "es" -> if (activeChurchLogoUrl != null) "Cambiar / Eliminar" else "Subir logo"
              "en" -> if (activeChurchLogoUrl != null) "Update / Remove" else "Upload Logo"
              else -> if (activeChurchLogoUrl != null) "Chanje / Retire" else "Mete Logo"
            }
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x440A1A38))
                .border(1.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .clickable {
                  photoPickerTarget = "church_logo"
                  showPhotoSourceDialog = true
                }
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("profile_church_logo_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            MemberAvatar3D(
              fullName = churchNameDisplay,
              size = 42.dp,
              photoUrl = activeChurchLogoUrl,
              showVerifiedBadge = false,
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = churchLogoTitle,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = churchLogoAction,
                style = MaterialTheme.typography.bodySmall,
                color = BrandGold,
              )
            }
            Icon(
              imageVector = Icons.Default.CameraAlt,
              contentDescription = churchLogoTitle,
              tint = ElectricBlue,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        if (onOpenNotifications != null) {
          val notifTitle =
            when (currentLanguageCode.trim().lowercase()) {
              "fr" -> "Notifications & Alertes"
              "es" -> "Notificaciones y Alertas"
              "en" -> "Notifications & Alerts"
              else -> "Notifikasyon & Alèt Legliz la"
            }
          val notifSubtitle =
            if (unreadNotificationsCount > 0) {
              when (currentLanguageCode.trim().lowercase()) {
                "fr" -> "$unreadNotificationsCount non lue(s)"
                "es" -> "$unreadNotificationsCount sin leer"
                "en" -> "$unreadNotificationsCount unread"
                else -> "$unreadNotificationsCount poko li"
              }
            } else {
              when (currentLanguageCode.trim().lowercase()) {
                "fr" -> "Voir toutes les annonces de l'église"
                "es" -> "Ver todos los anuncios de la iglesia"
                "en" -> "View all church announcements"
                else -> "Wè tout anons ak alèt legliz la"
              }
            }
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x440A1A38))
                .border(1.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenNotifications)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("profile_notifications_row"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier =
                Modifier.size(38.dp)
                  .clip(CircleShape)
                  .background(ElectricBlue.copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = notifTitle,
                tint = BrandGold,
                modifier = Modifier.size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = notifTitle,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = notifSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (unreadNotificationsCount > 0) BrandGold else GlassWhite.copy(alpha = 0.75f),
              )
            }
            if (unreadNotificationsCount > 0) {
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(50))
                    .background(BrandGold)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString(),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF030B1C),
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            }
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = GlassWhite.copy(alpha = 0.7f),
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GlassButton(
          text = copy.shareApp,
          icon = Icons.Default.Share,
          isPrimaryNeon = false,
          onClick = { shareOmniChurchGeneralApp(context, churchNameDisplay) },
          modifier = Modifier.fillMaxWidth(),
          testTag = "profile_share_app_btn",
        )
      }

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
