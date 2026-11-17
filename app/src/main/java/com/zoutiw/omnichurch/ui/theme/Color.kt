package com.zoutiw.omnichurch.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Official OmniChurch Pro 3D Color Palette
val PrimaryNavy = Color(0xFF07152F)
val DeepNavy = Color(0xFF030B1C)
val MidnightNavy = Color(0xFF0A1E40)
val SkyBlue = Color(0xFF00AEEF)
val ElectricBlue = Color(0xFF087BFF)
val BlueNeon = Color(0xFF00C8FF)
val RoyalBlue = Color(0xFF1748D8)
val BrandOrange = Color(0xFFFF8C00)
val BrandGold = Color(0xFFFFC247)
val PureWhite = Color(0xFFFFFFFF)
val GlassWhite = Color(0xFFDCEBFF)
val MutedGlassText = Color(0xFF9AB4D8)
val SuccessGreen = Color(0xFF00D878)
val SuccessGreenDark = Color(0xFF044E32)
val ErrorRed = Color(0xFFFF435B)
val ErrorRedDark = Color(0xFF521020)
val LiveBadgeRed = Color(0xFFFF2A4B)

// Translucent Glass Surfaces & Rim Lighting
val GlassSurfaceTop = Color(0x381A3B70)
val GlassSurfaceBottom = Color(0x24091936)
val GlassCardHighlight = Color(0x6600C8FF)
val GlassCardBorderSubtle = Color(0x33DCEBFF)
val GlassInnerShadow = Color(0x40000000)

object OmniBrushes {
  val DeepBackgroundGradient =
    Brush.verticalGradient(
      colors =
        listOf(
          Color(0xFF081A3A),
          PrimaryNavy,
          DeepNavy,
        )
    )

  val GlassCardBrush =
    Brush.linearGradient(
      colors =
        listOf(
          Color(0x4D1E437A),
          Color(0x2E0D2246),
          Color(0x3B07152F),
        )
    )

  val ActiveBlueNeonBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          Color(0xFF1AA8FF),
          ElectricBlue,
          RoyalBlue,
        )
    )

  val GoldAccentBrush =
    Brush.horizontalGradient(
      colors =
        listOf(
          BrandGold,
          BrandOrange,
        )
    )

  val SuccessGlassBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          Color(0x5500D878),
          Color(0x28044E32),
          Color(0x4007152F),
        )
    )

  val ErrorGlassBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          Color(0x55FF435B),
          Color(0x28521020),
          Color(0x4007152F),
        )
    )

  val MemberCardPearlBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          Color(0xFFEAF3FF),
          Color(0xFFD5E6FA),
          Color(0xFFC2DCFA),
        )
    )
}
