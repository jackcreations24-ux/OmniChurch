package com.zoutiw.omnichurch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OmniChurch3DColorScheme =
  darkColorScheme(
    primary = ElectricBlue,
    onPrimary = PureWhite,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = GlassWhite,
    secondary = BrandOrange,
    onSecondary = DeepNavy,
    secondaryContainer = BrandGold,
    onSecondaryContainer = DeepNavy,
    tertiary = BlueNeon,
    onTertiary = DeepNavy,
    background = DeepNavy,
    onBackground = PureWhite,
    surface = PrimaryNavy,
    onSurface = PureWhite,
    surfaceVariant = MidnightNavy,
    onSurfaceVariant = GlassWhite,
    error = ErrorRed,
    onError = PureWhite,
  )

@Composable
fun OmniChurchTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = OmniChurch3DColorScheme,
    typography = Typography,
    content = content,
  )
}
