package com.zoutiw.omnichurch.presentation.screens.member

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassQRContainer
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberIdCard
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.screens.auth.GoogleAccountPayload
import com.zoutiw.omnichurch.presentation.screens.auth.launchOptionalGoogleSignIn
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun DigitalMemberIdScreen(
  memberState: UiState<Member?>,
  qrBitmap: Bitmap?,
  onBackClick: () -> Unit,
  onOpenFullScreenQrGallery: () -> Unit,
  onOpenProfileSettings: () -> Unit,
  biometricEnabled: Boolean = false,
  onToggleBiometric: (Boolean) -> Unit = {},
  onLinkGoogleAccount: (GoogleAccountPayload) -> Unit = {},
  onUpdateMemberPhoto: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val activityContext = remember(context) { context.findActivity() ?: context }
  val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
  val scope = rememberCoroutineScope()
  var isLinkingGoogle by remember { mutableStateOf(false) }
  var showPhotoSourceDialog by remember { mutableStateOf(false) }
  var isCompressingPhoto by remember { mutableStateOf(false) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          isCompressingPhoto = true
          val result = ImageCompressor.compressUriToBase64(context, uri)
          isCompressingPhoto = false
          if (result.isSuccess) {
            onUpdateMemberPhoto(result.getOrThrow())
          } else {
            Toast.makeText(
                context,
                "Erè foto: ${result.exceptionOrNull()?.localizedMessage}",
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
          onUpdateMemberPhoto(base64)
        }
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        try {
          takePictureLauncher.launch(null)
        } catch (e: Exception) {
          Toast.makeText(context, "Erè kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
      } else {
        Toast.makeText(
            context,
            "Pèmisyon kamera nesesè pou pran yon foto pwofil.",
            Toast.LENGTH_SHORT,
          )
          .show()
      }
    }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .navigationBarsPadding(),
  ) {
    GlassTopBar(
      title = stringResource(R.string.member_card_screen_title),
      onBackClick = onBackClick,
      onMenuClick = onOpenProfileSettings,
    )

    when (memberState) {
      is UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          GlassLoadingIndicator()
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(20.dp),
          contentAlignment = Alignment.Center,
        ) {
          PremiumEmptyState(
            title = stringResource(R.string.member_no_card_title),
            subtitle = memberState.message,
            icon = Icons.Default.Badge,
          )
        }
      }
      is UiState.Success -> {
        val member = memberState.data
        if (member == null || member.effectiveBarcodeToken.isBlank()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center,
          ) {
            PremiumEmptyState(
              title = stringResource(R.string.member_no_card_title),
              subtitle = stringResource(R.string.member_no_card_subtitle),
              icon = Icons.Default.Badge,
            )
          }
        } else {
          val matrikil = member.effectiveMatrikil
          Column(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            MemberIdCard(
              member = member,
              qrBitmap = qrBitmap,
              onCopyMemberCode = {
                val clipboard =
                  context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OmniChurch Member ID", matrikil))
                Toast.makeText(
                    context,
                    context.getString(R.string.member_id_copied),
                    Toast.LENGTH_SHORT,
                  )
                  .show()
              },
              onChangePhotoClick = { showPhotoSourceDialog = true },
              onQrClick = onOpenFullScreenQrGallery,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              GlassButton(
                text = stringResource(R.string.member_btn_qr_gallery),
                subtitle = stringResource(R.string.member_btn_qr_gallery_sub),
                icon = Icons.Default.Fullscreen,
                isPrimaryNeon = true,
                onClick = onOpenFullScreenQrGallery,
                modifier = Modifier.weight(1f).height(94.dp),
                testTag = "btn_open_qr_gallery",
              )

              GlassButton(
                text = stringResource(R.string.member_btn_save_qr),
                subtitle = stringResource(R.string.member_btn_save_qr_sub),
                icon = Icons.Default.Download,
                isPrimaryNeon = false,
                enabled = qrBitmap != null,
                onClick = {
                  val bmp = qrBitmap ?: return@GlassButton
                  scope.launch {
                    val result = QrCodeGenerator.saveQrToGallery(context, bmp, matrikil)
                    val msg =
                      if (result.isSuccess) context.getString(R.string.member_qr_saved_success)
                      else context.getString(R.string.member_qr_saved_error)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier.weight(1f).height(94.dp),
                testTag = "btn_save_qr_gallery",
              )

              GlassButton(
                text = stringResource(R.string.member_btn_share_card),
                subtitle = stringResource(R.string.member_btn_share_card_sub),
                icon = Icons.Default.Share,
                isPrimaryNeon = false,
                enabled = qrBitmap != null,
                onClick = {
                  val bmp = qrBitmap ?: return@GlassButton
                  scope.launch {
                    QrCodeGenerator.shareQrCardImage(
                      context = context,
                      bitmap = bmp,
                      memberName = member.fullName,
                      memberCode = matrikil,
                      churchName = member.churchName,
                    )
                  }
                },
                modifier = Modifier.weight(1f).height(94.dp),
                testTag = "btn_share_member_card",
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (member.googleEmail.isNullOrBlank()) {
              GlassButton(
                text = "Lye yon kont Google (Opsyonèl)",
                subtitle = "Rekipere kat ou fasil sou nenpòt telefòn",
                icon = Icons.Default.Link,
                isPrimaryNeon = false,
                isLoading = isLinkingGoogle,
                onClick = {
                  isLinkingGoogle = true
                  launchOptionalGoogleSignIn(
                    context = activityContext,
                    credentialManager = credentialManager,
                    scope = scope,
                    onSuccess = { payload ->
                      isLinkingGoogle = false
                      onLinkGoogleAccount(payload)
                    },
                    onError = { err ->
                      isLinkingGoogle = false
                      Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    },
                  )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "card_link_google_optional_btn",
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
              ) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Kont Google lye: ${member.googleEmail}",
                  style = MaterialTheme.typography.bodySmall,
                  color = PureWhite,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PremiumGlassCard(
              cornerRadius = 18.dp,
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
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(22.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = "Deblokaj Biyometrik (Anprent / Face ID)",
                      style = MaterialTheme.typography.titleSmall,
                      color = PureWhite,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "Konekte rapidman san retape PIN ou",
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
                        title = "Aktive Anprent / Face ID",
                        subtitle = "Konfime idantite w pou lye ak ID $matrikil",
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
                  modifier = Modifier.testTag("card_biometric_toggle_switch"),
                )
              }
            }

            Spacer(modifier = Modifier.height(24.dp))
          }
        }
      }
    }

    if (showPhotoSourceDialog) {
      GlassModalBottomSheet(
        onDismissRequest = { showPhotoSourceDialog = false },
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 14.dp)
              .navigationBarsPadding(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Chanje Foto Pwofil Manm",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Chwazi kijan ou vle pran oswa chwazi nouvo foto paspò w la pou mete sou kat la.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
          )
          Spacer(modifier = Modifier.height(4.dp))

          GlassButton(
            text = "Pran yon Foto ak Kamera",
            subtitle = "Louvri kamera telefòn lan dirèkteman",
            icon = Icons.Default.CameraAlt,
            isPrimaryNeon = true,
            onClick = {
              showPhotoSourceDialog = false
              val hasCamPermission =
                androidx.core.content.ContextCompat.checkSelfPermission(
                  context,
                  android.Manifest.permission.CAMERA,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
              if (hasCamPermission) {
                try {
                  takePictureLauncher.launch(null)
                } catch (e: Exception) {
                  Toast.makeText(context, "Erè kamera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
              } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
              }
            },
            modifier = Modifier.fillMaxWidth().height(68.dp),
            testTag = "btn_take_photo_camera",
          )

          GlassButton(
            text = "Chwazi nan Galri",
            subtitle = "Chwazi yon foto ki deja sou aparèy la",
            icon = Icons.Default.Download,
            isPrimaryNeon = false,
            onClick = {
              showPhotoSourceDialog = false
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.fillMaxWidth().height(68.dp),
            testTag = "btn_pick_photo_gallery",
          )

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
  }
}

@Composable
fun MemberQRGalleryScreen(
  member: Member?,
  qrBitmap: Bitmap?,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onClose)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  DisposableEffect(Unit) {
    val activity = context.findActivity()
    val window = activity?.window
    val previousBrightness = window?.attributes?.screenBrightness ?: -1f
    if (window != null) {
      val params = window.attributes
      params.screenBrightness = 1.0f
      window.attributes = params
    }
    onDispose {
      if (window != null) {
        val params = window.attributes
        params.screenBrightness = previousBrightness
        window.attributes = params
      }
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    GlassTopBar(
      title = stringResource(R.string.qr_gallery_title),
      onBackClick = onClose,
    )

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Color(0x4400C8FF))
            .border(1.dp, BlueNeon, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Default.LightMode,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.qr_gallery_brightness_note),
          style = MaterialTheme.typography.labelMedium,
          color = PureWhite,
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      GlassQRContainer(
        qrBitmap = qrBitmap,
        size = 300.dp,
        showLaserLine = false,
      )

      Spacer(modifier = Modifier.height(20.dp))

      if (member != null) {
        Text(
          text = member.fullName,
          style = MaterialTheme.typography.headlineLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(R.string.member_id_prefix, member.effectiveMatrikil),
          style = MaterialTheme.typography.titleMedium,
          color = BrandGold,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Verified,
          contentDescription = null,
          tint = SuccessGreen,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.qr_gallery_offline_cached),
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite,
        )
      }
    }

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassButton(
          text = stringResource(R.string.member_btn_save_qr),
          icon = Icons.Default.Download,
          enabled = qrBitmap != null,
          onClick = {
            val bmp = qrBitmap ?: return@GlassButton
            val code = member?.effectiveMatrikil ?: "OMNICHURCH"
            scope.launch {
              val res = QrCodeGenerator.saveQrToGallery(context, bmp, code)
              val msg =
                if (res.isSuccess) context.getString(R.string.member_qr_saved_success)
                else context.getString(R.string.member_qr_saved_error)
              Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f),
          testTag = "gallery_save_qr_btn",
        )
        GlassButton(
          text = stringResource(R.string.member_btn_share_card),
          icon = Icons.Default.Share,
          isPrimaryNeon = false,
          enabled = qrBitmap != null,
          onClick = {
            val bmp = qrBitmap ?: return@GlassButton
            scope.launch {
              QrCodeGenerator.shareQrCardImage(
                context = context,
                bitmap = bmp,
                memberName = member?.fullName ?: "Manm OmniChurch",
                memberCode = member?.effectiveMatrikil ?: "OM",
                churchName = member?.churchName ?: "OmniChurch Pro",
              )
            }
          },
          modifier = Modifier.weight(1f),
          testTag = "gallery_share_qr_btn",
        )
      }

      GlassButton(
        text = stringResource(R.string.qr_gallery_close),
        isPrimaryNeon = false,
        onClick = onClose,
        modifier = Modifier.fillMaxWidth(),
        testTag = "gallery_close_btn",
      )
    }
  }
}
