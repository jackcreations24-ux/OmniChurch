package com.zoutiw.omnichurch.presentation.screens.services

import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.OmniLocalization
import com.zoutiw.omnichurch.core.utilities.ProfileStringValues
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.screens.auth.GoogleAccountPayload
import com.zoutiw.omnichurch.presentation.screens.auth.launchOptionalGoogleSignIn
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ProfileGoogleAndBiometricSecurityCard(
  member: Member,
  copy: ProfileStringValues,
  context: Context,
  activityContext: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  isLinkingGoogle: Boolean,
  onLinkingGoogleChange: (Boolean) -> Unit,
  onLinkGoogleAccount: (GoogleAccountPayload) -> Unit,
  onUnlinkGoogleAccount: () -> Unit,
  biometricEnabled: Boolean,
  onToggleBiometric: (Boolean) -> Unit,
) {
  val matrikil = member.effectiveMatrikil
  PremiumGlassCard(
    cornerRadius = 22.dp,
    contentPadding = PaddingValues(16.dp),
  ) {
    Text(
      text = copy.securityTitle,
      style = MaterialTheme.typography.titleMedium,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (member.googleEmail.isNullOrBlank()) {
      GlassButton(
        text = copy.linkGoogle,
        subtitle = copy.linkGoogleSub,
        icon = Icons.Default.Link,
        isPrimaryNeon = false,
        isLoading = isLinkingGoogle,
        onClick = {
          onLinkingGoogleChange(true)
          launchOptionalGoogleSignIn(
            context = activityContext,
            credentialManager = credentialManager,
            scope = scope,
            onSuccess = { payload ->
              onLinkingGoogleChange(false)
              onLinkGoogleAccount(payload)
            },
            onError = { err ->
              onLinkingGoogleChange(false)
              Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            },
          )
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "profile_link_google_optional_btn",
      )
    } else {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x3300D878))
            .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Default.Verified,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = copy.linkedGoogle.format(member.googleEmail.orEmpty()),
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite,
            fontWeight = FontWeight.SemiBold,
          )
        }
        TextButton(
          onClick = onUnlinkGoogleAccount,
          modifier = Modifier.testTag("profile_unlink_google_btn"),
        ) {
          Text(
            text = copy.unlink,
            style = MaterialTheme.typography.labelMedium,
            color = BrandGold,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

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
          imageVector = Icons.Default.Fingerprint,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = copy.biometricTitle,
            style = MaterialTheme.typography.titleSmall,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = copy.biometricSub.format(matrikil),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
          )
        }
      }
      Switch(
        checked = biometricEnabled,
        onCheckedChange = { wantEnabled ->
          if (wantEnabled && BiometricQuickUnlockHelper.isBiometricAvailable(context)) {
            BiometricQuickUnlockHelper.promptBiometricUnlock(
              context = activityContext,
              title = copy.biometricPromptTitle,
              subtitle = copy.biometricPromptSub.format(matrikil),
              onSuccess = { onToggleBiometric(true) },
              onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
              },
            )
          } else {
            onToggleBiometric(wantEnabled)
          }
        },
        colors =
          SwitchDefaults.colors(
            checkedThumbColor = PureWhite,
            checkedTrackColor = SuccessGreen,
          ),
        modifier = Modifier.testTag("profile_biometric_toggle_switch"),
      )
    }
  }
}

@Composable
internal fun ProfileLanguageSwitcherCard(
  currentLanguageCode: String,
  copy: ProfileStringValues,
  onLanguageChange: (String) -> Unit,
) {
  PremiumGlassCard(
    cornerRadius = 22.dp,
    contentPadding = PaddingValues(16.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.Language,
        contentDescription = null,
        tint = BlueNeon,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = copy.languageLabel,
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
    }
    Spacer(modifier = Modifier.height(12.dp))

    val normalizedLang = remember(currentLanguageCode) { OmniLocalization.normalize(currentLanguageCode) }
    val languages =
      remember {
        listOf(
          "ht" to "Kreyòl",
          "fr" to "Français",
          "en" to "English",
          "es" to "Español",
        )
      }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      languages.forEach { (code, name) ->
        val isSelected = normalizedLang == code
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(
                if (isSelected) ElectricBlue else Color(0x660B1D3D)
              )
              .border(
                1.dp,
                if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp),
              )
              .clickable { onLanguageChange(code) }
              .padding(vertical = 12.dp, horizontal = 4.dp)
              .testTag("lang_option_$code"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
          )
        }
      }
    }
  }
}

@Composable
internal fun ProfileMemberPersonalInfoCard(
  member: Member,
  churchNameDisplay: String,
  currentLanguageCode: String,
  copy: ProfileStringValues,
) {
  PremiumGlassCard(
    cornerRadius = 22.dp,
    contentPadding = PaddingValues(16.dp),
    modifier = Modifier.testTag("profile_member_personal_info_card"),
  ) {
    Text(
      text = copy.personalInfoTitle,
      style = MaterialTheme.typography.titleMedium,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0x44091936))
          .border(1.dp, ElectricBlue.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
          .padding(12.dp)
          .testTag("profile_contact_admin_notice"),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null,
        tint = ElectricBlue,
        modifier = Modifier.size(18.dp).padding(top = 1.dp),
      )
      Text(
        text = copy.adminOnlyNotice,
        style = MaterialTheme.typography.bodySmall,
        color = GlassWhite.copy(alpha = 0.92f),
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    ReadOnlyMemberFieldRow(
      label = copy.fullNameLabel,
      value = member.effectiveFullName,
    )
    Spacer(modifier = Modifier.height(8.dp))
    ReadOnlyMemberFieldRow(
      label = copy.matriculeLabel,
      value = member.effectiveMatrikil,
    )
    Spacer(modifier = Modifier.height(8.dp))
    ReadOnlyMemberFieldRow(
      label = copy.churchLabel,
      value = churchNameDisplay,
    )
    Spacer(modifier = Modifier.height(8.dp))
    ReadOnlyMemberFieldRow(
      label = copy.departmentLabel,
      value = member.department.ifBlank { OmniLocalization.defaultDepartmentName(currentLanguageCode) },
    )
    Spacer(modifier = Modifier.height(8.dp))
    ReadOnlyMemberFieldRow(
      label = copy.roleLabel,
      value = member.role.ifBlank { OmniLocalization.defaultMemberName(currentLanguageCode) },
    )
    if (!member.phone.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(8.dp))
      ReadOnlyMemberFieldRow(
        label = copy.phoneLabel,
        value = member.phone,
      )
    }
  }
}

@Composable
internal fun ProfilePhotoSourceBottomSheet(
  copy: ProfileStringValues,
  isCompressingPhoto: Boolean,
  onTakePhotoClick: () -> Unit,
  onPickGalleryClick: () -> Unit,
  onDismiss: () -> Unit,
  hasExistingPhoto: Boolean = false,
  languageCode: String = "ht",
  titleOverride: String? = null,
  subtitleOverride: String? = null,
  onRemovePhotoClick: (() -> Unit)? = null,
) {
  val removePhotoLabel =
    when (languageCode.trim().lowercase()) {
      "fr" -> "Supprimer la photo"
      "es" -> "Eliminar foto"
      "en" -> "Remove Photo"
      else -> "Retire Foto a"
    }
  GlassModalBottomSheet(
    onDismissRequest = onDismiss,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp)
          .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = titleOverride ?: copy.changePhoto,
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = subtitleOverride ?: copy.changePhotoSub,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(4.dp))

      GlassButton(
        text = copy.takePhoto,
        subtitle = copy.takePhotoSub,
        icon = Icons.Default.CameraAlt,
        isPrimaryNeon = true,
        onClick = onTakePhotoClick,
        modifier = Modifier.fillMaxWidth().height(68.dp),
        testTag = "profile_btn_take_photo_camera",
      )

      GlassButton(
        text = copy.pickGallery,
        subtitle = copy.pickGallerySub,
        icon = Icons.Default.Download,
        isPrimaryNeon = false,
        onClick = onPickGalleryClick,
        modifier = Modifier.fillMaxWidth().height(68.dp),
        testTag = "profile_btn_pick_photo_gallery",
      )

      if (hasExistingPhoto && onRemovePhotoClick != null) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(ErrorRed.copy(alpha = 0.20f))
              .border(1.dp, ErrorRed.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
              .clickable(onClick = onRemovePhotoClick)
              .padding(vertical = 13.dp, horizontal = 14.dp)
              .testTag("profile_btn_remove_photo"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = removePhotoLabel,
              tint = PureWhite,
              modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = removePhotoLabel,
              style = MaterialTheme.typography.titleSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (isCompressingPhoto) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(8.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = BlueNeon)
        }
      }
    }
  }
}

@Composable
private fun ReadOnlyMemberFieldRow(
  label: String,
  value: String,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0x550A1836))
        .border(1.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MutedGlassText,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyLarge,
      color = PureWhite,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
