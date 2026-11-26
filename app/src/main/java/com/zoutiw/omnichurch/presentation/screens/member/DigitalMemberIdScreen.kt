package com.zoutiw.omnichurch.presentation.screens.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberIdCard
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import kotlinx.coroutines.launch

@Composable
fun DigitalMemberIdScreen(
  memberState: UiState<Member?>,
  qrBitmap: Bitmap?,
  onBackClick: () -> Unit,
  onOpenFullScreenQrGallery: () -> Unit,
  onOpenProfileSettings: () -> Unit,
  onUpdateMemberPhoto: (String) -> Unit = {},
  currentLanguageCode: String = "ht",
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
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
          Box(
            modifier =
              Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
          ) {
            // 3D DIGITAL MEMBER ID CARD WITH FLIP (RECTO / VERSO)
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

          val currentMemberPhoto = (memberState as? UiState.Success)?.data?.effectivePhotoUrl
          if (!currentMemberPhoto.isNullOrBlank()) {
            val removePhotoLabel =
              when (currentLanguageCode.trim().lowercase()) {
                "fr" -> "Supprimer la photo"
                "es" -> "Eliminar foto"
                "en" -> "Remove Photo"
                else -> "Retire Foto a"
              }
            GlassButton(
              text = removePhotoLabel,
              icon = Icons.Default.DeleteOutline,
              isPrimaryNeon = false,
              onClick = {
                showPhotoSourceDialog = false
                onUpdateMemberPhoto("")
              },
              modifier = Modifier.fillMaxWidth().height(54.dp),
              testTag = "btn_remove_member_photo",
            )
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
  }
}
