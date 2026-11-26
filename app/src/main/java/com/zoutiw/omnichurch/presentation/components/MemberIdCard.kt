package com.zoutiw.omnichurch.presentation.components

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite

/**
 * Reusable 3D Digital Member Card component (`PremiumMemberCard` / `MemberIdCard`).
 * Reproduces the exact middle smartphone design from the official reference image on its Front
 * face (Recto), and smoothly flips 180° around the Y-axis to reveal the official Back verification
 * face (Verso).
 */
@Composable
fun PremiumMemberCard(
  member: Member,
  qrBitmap: Bitmap?,
  onCopyMemberCode: () -> Unit,
  onChangePhotoClick: () -> Unit,
  onQrClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  MemberIdCard(
    member = member,
    qrBitmap = qrBitmap,
    onCopyMemberCode = onCopyMemberCode,
    onChangePhotoClick = onChangePhotoClick,
    onQrClick = onQrClick,
    modifier = modifier,
  )
}

@Composable
fun MemberIdCard(
  member: Member,
  qrBitmap: Bitmap?,
  onCopyMemberCode: () -> Unit,
  onChangePhotoClick: () -> Unit,
  onQrClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var isFlipped by remember { mutableStateOf(false) }

  val rotation by
    animateFloatAsState(
      targetValue = if (isFlipped) 180f else 0f,
      animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
      label = "card_flip_animation",
    )

  val cardShape = RoundedCornerShape(30.dp)

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .graphicsLayer {
          rotationY = rotation
          cameraDistance = 16f * density
        }
        .shadow(
          elevation = 24.dp,
          shape = cardShape,
          ambientColor = BlueNeon,
          spotColor = ElectricBlue,
        )
        .clip(cardShape)
        .background(OmniBrushes.MemberCardPearlBrush)
        .border(
          width = 2.2.dp,
          brush =
            Brush.linearGradient(
              listOf(PureWhite, BlueNeon, ElectricBlue, BrandGold, BlueNeon)
            ),
          shape = cardShape,
        )
        .drawBehind {
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(
                  Color.Transparent,
                  BlueNeon.copy(alpha = 0.7f),
                  PureWhite,
                  BlueNeon.copy(alpha = 0.7f),
                  Color.Transparent,
                )
              ),
            start = Offset(size.width * 0.22f, 1.5.dp.toPx()),
            end = Offset(size.width * 0.78f, 1.5.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
          )
        }
        .testTag("digital_member_id_card"),
  ) {
    if (rotation <= 90f) {
      MemberCardReferenceFrontFace(
        member = member,
        qrBitmap = qrBitmap,
        onCopyMemberCode = onCopyMemberCode,
        onChangePhotoClick = onChangePhotoClick,
        onQrClick = onQrClick,
        onFlipCard = { isFlipped = true },
      )
    } else {
      Box(
        modifier =
          Modifier.fillMaxWidth().graphicsLayer {
            rotationY = 180f
          }
      ) {
        MemberCardBackFace(
          member = member,
          qrBitmap = qrBitmap,
          onQrClick = onQrClick,
          onFlipCard = { isFlipped = false },
        )
      }
    }
  }
}

@Composable
fun CanonicalBarcodeStrip(
  value: String,
  modifier: Modifier = Modifier,
  barColor: Color = DeepNavy,
) {
  val barsPattern =
    remember(value) {
      val clean = value.ifBlank { "OMNICHURCH" }
      val weights = mutableListOf<Pair<Boolean, Float>>()
      weights.add(true to 2f)
      weights.add(false to 1f)
      weights.add(true to 1f)
      weights.add(false to 2f)
      clean.forEachIndexed { idx, ch ->
        val code = ch.code
        for (bit in 0 until 6) {
          val isBlack = ((code shr bit) and 1) == 1 || (bit == 0 && idx % 2 == 0)
          val w = if (((code + bit) % 3) == 0) 2.2f else 1.1f
          weights.add(isBlack to w)
          weights.add(false to if (((code + bit) % 2) == 0) 1.0f else 1.7f)
        }
      }
      weights.add(true to 2f)
      weights.add(false to 1f)
      weights.add(true to 2f)
      weights
    }

  Canvas(modifier = modifier) {
    val totalUnits = barsPattern.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1f)
    val unitWidth = size.width / totalUnits
    var x = 0f
    barsPattern.forEach { (isDark, weight) ->
      val w = weight * unitWidth
      if (isDark) {
        drawRect(
          color = barColor,
          topLeft = Offset(x, 0f),
          size = Size(w.coerceAtLeast(1f), size.height),
        )
      }
      x += w
    }
  }
}
