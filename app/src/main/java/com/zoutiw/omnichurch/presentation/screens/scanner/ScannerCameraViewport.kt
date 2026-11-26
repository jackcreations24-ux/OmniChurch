package com.zoutiw.omnichurch.presentation.screens.scanner

import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.OmniChurch3DFlameIcon
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.util.concurrent.Executors

@Composable
internal fun NfcTapViewportCard(
  isNfcSupported: Boolean,
  isNfcEnabled: Boolean,
  hasActiveService: Boolean,
  lastScannedToken: String?,
  isVerifyMode: Boolean = false,
  copy: ScannerCopy,
  onOpenNfcSettings: () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "nfc_radar")
  val pulseScale by
    infiniteTransition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.25f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "nfc_pulse_scale",
    )
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.65f,
      targetValue = 0.05f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "nfc_pulse_alpha",
    )

  val isReadyToTap = isNfcSupported && isNfcEnabled && hasActiveService
  val readyAccent = if (isVerifyMode) SuccessGreen else BlueNeon

  PremiumGlassCard(
    cornerRadius = 24.dp,
    isHighlighted = isReadyToTap,
    accentColor = if (isReadyToTap) readyAccent else BrandOrange,
    contentPadding = PaddingValues(20.dp),
    modifier = Modifier.fillMaxWidth().testTag("nfc_scanner_viewport"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center,
      ) {
        if (isReadyToTap) {
          Box(
            modifier =
              Modifier.size(110.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .border(2.dp, readyAccent, CircleShape)
          )
          Box(
            modifier =
              Modifier.size(90.dp)
                .clip(CircleShape)
                .background(readyAccent.copy(alpha = 0.16f))
                .border(1.2.dp, readyAccent.copy(alpha = 0.45f), CircleShape)
          )
        }
        Box(
          modifier =
            Modifier.size(68.dp)
              .shadow(14.dp, CircleShape, ambientColor = if (isReadyToTap) readyAccent else BrandOrange)
              .clip(CircleShape)
              .background(
                if (isReadyToTap) {
                  if (isVerifyMode) Brush.verticalGradient(listOf(SuccessGreen, ElectricBlue))
                  else OmniBrushes.ActiveBlueNeonBrush
                } else {
                  Brush.verticalGradient(listOf(BrandGold.copy(alpha = 0.3f), BrandOrange.copy(alpha = 0.25f)))
                }
              )
              .border(1.5.dp, PureWhite.copy(alpha = 0.78f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Contactless,
            contentDescription = copy.nfcTitle,
            tint = PureWhite,
            modifier = Modifier.size(34.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = copy.nfcTitle,
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text =
          when {
            !isNfcSupported -> copy.nfcUnsupported
            !isNfcEnabled -> copy.nfcDisabled
            !hasActiveService -> copy.noActiveService
            isVerifyMode -> copy.nfcVerifySubtitle
            else -> copy.nfcSubtitle
          },
        style = MaterialTheme.typography.bodySmall,
        color = if (isReadyToTap) GlassWhite else BrandGold,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(14.dp))

      if (isNfcSupported && !isNfcEnabled) {
        GlassButton(
          text = copy.nfcOpenSettings,
          icon = Icons.Default.Settings,
          isPrimaryNeon = true,
          onClick = onOpenNfcSettings,
          modifier = Modifier.fillMaxWidth(),
          testTag = "nfc_open_settings_btn",
        )
      } else if (isReadyToTap) {
        Row(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(SuccessGreen.copy(alpha = 0.18f))
              .border(1.dp, SuccessGreen.copy(alpha = 0.65f), RoundedCornerShape(50))
              .padding(horizontal = 14.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = copy.nfcListening,
            style = MaterialTheme.typography.labelMedium,
            color = SuccessGreen,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (!lastScannedToken.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = String.format(copy.nfcLastReadFormat, lastScannedToken),
          style = MaterialTheme.typography.labelSmall,
          color = MutedGlassText,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@OptIn(ExperimentalGetImage::class)
@Composable
internal fun CameraXQrViewportCard(
  isScanningEnabled: Boolean,
  isTorchEnabled: Boolean,
  isVerifyMode: Boolean = false,
  copy: ScannerCopy,
  onToggleTorch: () -> Unit,
  onQrDetected: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  val previewView = remember { PreviewView(context) }
  var boundCamera by remember { mutableStateOf<Camera?>(null) }

  val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
  val laserYRatio by
    infiniteTransition.animateFloat(
      initialValue = 0.16f,
      targetValue = 0.84f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1800, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "scanner_laser_y",
    )

  LaunchedEffect(boundCamera, isTorchEnabled) {
    boundCamera?.cameraControl?.enableTorch(isTorchEnabled)
  }

  DisposableEffect(lifecycleOwner, isScanningEnabled) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val options =
      BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val barcodeScanner = BarcodeScanning.getClient(options)

    val listener = Runnable {
      try {
        val cameraProvider = cameraProviderFuture.get()
        val preview =
          Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis =
          ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
          val mediaImage = imageProxy.image
          if (mediaImage != null && isScanningEnabled) {
            val image =
              InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner
              .process(image)
              .addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (!rawValue.isNullOrBlank()) {
                  onQrDetected(rawValue)
                }
              }
              .addOnCompleteListener { imageProxy.close() }
          } else {
            imageProxy.close()
          }
        }

        cameraProvider.unbindAll()
        boundCamera =
          cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
          )
      } catch (_: Exception) {
      }
    }

    cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

    onDispose {
      try {
        cameraProviderFuture.get().unbindAll()
      } catch (_: Exception) {}
      barcodeScanner.close()
    }
  }

  val frameAccent = if (isVerifyMode) SuccessGreen else BlueNeon

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(256.dp)
        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = frameAccent, spotColor = DeepNavy)
        .clip(RoundedCornerShape(24.dp))
        .background(DeepNavy)
        .border(
          1.4.dp,
          Brush.linearGradient(listOf(frameAccent.copy(alpha = 0.65f), GlassWhite.copy(alpha = 0.25f))),
          RoundedCornerShape(24.dp),
        )
        .testTag("camerax_qr_viewport"),
  ) {
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize(),
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isTorchEnabled) BrandGold else Color(0x990A1936))
            .border(1.dp, GlassWhite.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onToggleTorch)
            .testTag("scanner_torch_btn"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.FlashOn,
          contentDescription = copy.torchToggle,
          tint = if (isTorchEnabled) DeepNavy else PureWhite,
          modifier = Modifier.size(19.dp),
        )
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Color(0x990A1936))
            .border(1.dp, frameAccent.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
      ) {
        Text(
          text = if (isVerifyMode) copy.purposeVerify else copy.tabQr,
          style = MaterialTheme.typography.labelSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }

    Box(
      modifier =
        Modifier.align(Alignment.Center)
          .size(32.dp)
          .clip(CircleShape)
          .background(DeepNavy.copy(alpha = 0.45f)),
      contentAlignment = Alignment.Center,
    ) {
      OmniChurch3DFlameIcon(size = 22.dp)
    }

    Canvas(modifier = Modifier.size(186.dp).align(Alignment.Center)) {
      val w = size.width
      val h = size.height
      val len = w * 0.24f
      val rad = 18.dp.toPx()
      val stroke = 4.dp.toPx()
      val bracketBrush =
        if (isVerifyMode) {
          Brush.linearGradient(listOf(PureWhite, SuccessGreen, BrandGold))
        } else {
          Brush.linearGradient(listOf(PureWhite, BlueNeon, ElectricBlue))
        }

      val tl =
        Path().apply {
          moveTo(0f, len)
          lineTo(0f, rad)
          quadraticTo(0f, 0f, rad, 0f)
          lineTo(len, 0f)
        }
      drawPath(tl, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val tr =
        Path().apply {
          moveTo(w - len, 0f)
          lineTo(w - rad, 0f)
          quadraticTo(w, 0f, w, rad)
          lineTo(w, len)
        }
      drawPath(tr, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val bl =
        Path().apply {
          moveTo(0f, h - len)
          lineTo(0f, h - rad)
          quadraticTo(0f, h, rad, h)
          lineTo(len, h)
        }
      drawPath(bl, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val br =
        Path().apply {
          moveTo(w - len, h)
          lineTo(w - rad, h)
          quadraticTo(w, h, w, h - rad)
          lineTo(w, h - len)
        }
      drawPath(br, bracketBrush, style = Stroke(stroke, cap = StrokeCap.Round))

      val laserY = h * laserYRatio
      val laserColor = if (isVerifyMode) BrandGold else SuccessGreen
      drawLine(
        brush =
          Brush.horizontalGradient(
            listOf(Color.Transparent, laserColor, PureWhite, laserColor, Color.Transparent)
          ),
        start = Offset(-w * 0.06f, laserY),
        end = Offset(w * 1.06f, laserY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
      )
    }
  }
}
