package com.zoutiw.omnichurch.presentation.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

/**
 * 18. PremiumQRContainer & GlassQRContainer — High-contrast pure white QR container framed by
 *     glowing electric-blue 3D corner brackets and an optional centered OmniChurch flame badge
 *     (matching Phone 2 in the reference image while remaining 100% scannable).
 */
@Composable
fun PremiumQRContainer(
  qrBitmap: Bitmap?,
  size: Dp = 200.dp,
  showLaserLine: Boolean = false,
  showCenterFlameBadge: Boolean = true,
  qrContentDescription: String = "Official Member QR Code",
  modifier: Modifier = Modifier,
) {
  GlassQRContainer(
    qrBitmap = qrBitmap,
    size = size,
    showLaserLine = showLaserLine,
    showCenterFlameBadge = showCenterFlameBadge,
    qrContentDescription = qrContentDescription,
    modifier = modifier,
  )
}

@Composable
fun GlassQRContainer(
  qrBitmap: Bitmap?,
  size: Dp = 210.dp,
  showLaserLine: Boolean = false,
  showCenterFlameBadge: Boolean = true,
  qrContentDescription: String = "Official Member QR Code",
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "qr_laser")
  val laserProgress by
    infiniteTransition.animateFloat(
      initialValue = 0.12f,
      targetValue = 0.88f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2200, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "laser_y",
    )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    // Pure white inner card so QR contrast is never compromised by glass effects
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(8.dp)
          .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = BlueNeon, spotColor = ElectricBlue)
          .clip(RoundedCornerShape(22.dp))
          .background(PureWhite)
          .padding(10.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (qrBitmap != null) {
        Image(
          bitmap = qrBitmap.asImageBitmap(),
          contentDescription = qrContentDescription,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().testTag("official_qr_bitmap"),
        )
        if (showCenterFlameBadge) {
          Box(
            modifier =
              Modifier.size(size * 0.16f)
                .clip(RoundedCornerShape(8.dp))
                .background(PureWhite)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
          ) {
            OmniChurch3DFlameIcon(size = size * 0.14f)
          }
        }
      } else {
        CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(36.dp))
      }
    }

    // Glowing Electric-Blue 3D Corner Brackets
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val bracketLen = w * 0.23f
      val stroke = 4.2.dp.toPx()
      val cornerRad = 22.dp.toPx()
      val pad = 3.dp.toPx()

      val bracketBrush = Brush.linearGradient(listOf(BlueNeon, ElectricBlue, BlueNeon))

      // Top-Left Bracket
      val tlPath =
        Path().apply {
          moveTo(pad, pad + bracketLen)
          lineTo(pad, pad + cornerRad)
          quadraticTo(pad, pad, pad + cornerRad, pad)
          lineTo(pad + bracketLen, pad)
        }
      drawPath(tlPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      // Top-Right Bracket
      val trPath =
        Path().apply {
          moveTo(w - pad - bracketLen, pad)
          lineTo(w - pad - cornerRad, pad)
          quadraticTo(w - pad, pad, w - pad, pad + cornerRad)
          lineTo(w - pad, pad + bracketLen)
        }
      drawPath(trPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      // Bottom-Left Bracket
      val blPath =
        Path().apply {
          moveTo(pad, h - pad - bracketLen)
          lineTo(pad, h - pad - cornerRad)
          quadraticTo(pad, h - pad, pad + cornerRad, h - pad)
          lineTo(pad + bracketLen, h - pad)
        }
      drawPath(blPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      // Bottom-Right Bracket
      val brPath =
        Path().apply {
          moveTo(w - pad - bracketLen, h - pad)
          lineTo(w - pad - cornerRad, h - pad)
          quadraticTo(w - pad, h - pad, w - pad, h - pad - cornerRad)
          lineTo(w - pad, h - pad - bracketLen)
        }
      drawPath(brPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      if (showLaserLine) {
        val y = h * laserProgress
        drawLine(
          brush =
            Brush.horizontalGradient(
              listOf(Color.Transparent, SuccessGreen, PureWhite, SuccessGreen, Color.Transparent)
            ),
          start = Offset(w * 0.08f, y),
          end = Offset(w * 0.92f, y),
          strokeWidth = 3.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
    }
  }
}
