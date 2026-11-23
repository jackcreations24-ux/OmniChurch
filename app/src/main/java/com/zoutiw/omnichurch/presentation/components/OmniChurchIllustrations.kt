package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue

/**
 * Official OmniChurch PRO Brand Header.
 * Uses the pure transparent [OmniChurchLogo] assets directly without artificial boxes or halos.
 */
@Composable
fun OmniChurchBrandHeader(
  badgeText: String,
  isProStyle: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    OmniChurchLogo(
      variant = LogoVariant.Icon,
      modifier = Modifier.size(if (isProStyle) 36.dp else 34.dp),
      contentDescription = "OmniChurch PRO Logo",
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "OmniChurch",
      style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isProStyle) 20.sp else 18.sp),
      color = PureWhite,
      fontWeight = FontWeight.ExtraBold,
    )
    Spacer(modifier = Modifier.width(6.dp))
    OmniChurchProBadge(label = if (isProStyle && badgeText.isNotBlank()) badgeText.uppercase() else "PRO")
  }
}

/**
 * Official Standalone OmniChurch Flame Symbol (`R.drawable.omnichurch_icon`).
 * Rendered directly on transparent background with zero artificial halo, border, or shadow.
 */
@Composable
fun OmniChurch3DFlameIcon(size: Dp = 36.dp, modifier: Modifier = Modifier) {
  OmniChurchLogo(
    variant = LogoVariant.Icon,
    modifier = modifier.size(size),
    contentDescription = "OmniChurch PRO Official Icon",
  )
}

/**
 * ASSET 03 — Official Cinematic Home Background (`R.drawable.omnichurch_home_bg`)
 * featuring modern church architecture, illuminated golden cross, sunset sky,
 * and blue/orange neon light waves, with controlled dark gradient overlay for text contrast.
 */
@Composable
fun OmniChurchSanctuaryHeroCanvas(
  modifier: Modifier = Modifier,
  showLeftTextScrim: Boolean = true,
) {
  Box(modifier = modifier) {
    Image(
      painter = painterResource(id = R.drawable.omnichurch_home_bg),
      contentDescription = "OmniChurch Sanctuary",
      contentScale = ContentScale.Crop,
      alignment = Alignment.TopEnd,
      modifier = Modifier.fillMaxSize(),
    )

    if (showLeftTextScrim) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Controlled dark navy scrim on left side so white & orange welcome typography is crisp
        drawRect(
          brush =
            Brush.horizontalGradient(
              colors =
                listOf(
                  DeepNavy.copy(alpha = 0.82f),
                  PrimaryNavy.copy(alpha = 0.48f),
                  Color.Transparent,
                ),
              startX = 0f,
              endX = w * 0.65f,
            )
        )
        // Smooth bottom transition into DeepBackgroundGradient (PrimaryNavy / DeepNavy)
        drawRect(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color.Transparent,
                  PrimaryNavy.copy(alpha = 0.65f),
                  DeepNavy,
                ),
              startY = h * 0.52f,
              endY = h,
            )
        )
      }
    }
  }
}

/**
 * Renders a vibrant 3D worship service stage banner when a service document does not provide an
 * external image URL.
 */
@Composable
fun WorshipStageBannerCanvas(modifier: Modifier = Modifier) {
  Box(modifier = modifier) {
    Image(
      painter = painterResource(id = R.drawable.omnichurch_home_bg),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      drawRect(
        brush =
          Brush.linearGradient(
            colors =
              listOf(
                Color(0x990A1D42),
                Color(0x66193C7A),
                Color(0xAA07132B),
              )
          )
      )

      // Stage spotlights
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                BlueNeon.copy(alpha = 0.38f),
                ElectricBlue.copy(alpha = 0.14f),
                Color.Transparent,
              ),
            center = Offset(w * 0.28f, h * 0.25f),
            radius = w * 0.45f,
          ),
        radius = w * 0.45f,
        center = Offset(w * 0.28f, h * 0.25f),
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors =
              listOf(
                BrandGold.copy(alpha = 0.35f),
                BrandOrange.copy(alpha = 0.12f),
                Color.Transparent,
              ),
            center = Offset(w * 0.75f, h * 0.25f),
            radius = w * 0.42f,
          ),
        radius = w * 0.42f,
        center = Offset(w * 0.75f, h * 0.25f),
      )

      // Soundwave / worship horizon arcs
      val cx = w * 0.5f
      for (i in 1..3) {
        drawArc(
          color = BlueNeon.copy(alpha = 0.22f / i),
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = Offset(cx - w * (0.14f * i), h * 0.62f - h * (0.08f * i)),
          size = Size(w * (0.28f * i), h * (0.25f * i)),
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
      }
    }
  }
}

/**
 * Renders a circular 3D member avatar with metallic/neon ring, real photograph support
 * (`photoUrl` as either a PC/Web Base64 Data URL `data:image/...;base64,...` or an HTTP/HTTPS URL),
 * initials fallback, and optional verified green badge.
 */
@Composable
fun MemberAvatar3D(
  fullName: String,
  size: Dp = 48.dp,
  photoUrl: String? = null,
  showVerifiedBadge: Boolean = false,
  borderColor: Color = BlueNeon,
  modifier: Modifier = Modifier,
) {
  val initials =
    fullName
      .trim()
      .split("\\s+".toRegex())
      .filter { it.isNotEmpty() }
      .take(2)
      .joinToString("") { it.first().uppercase() }
      .ifEmpty { "OC" }

  val cleanPhotoUrl = photoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val base64Bitmap = remember(cleanPhotoUrl) { ImageCompressor.decodeBase64DataUrlToBitmap(cleanPhotoUrl) }
  var remoteImageFailed by remember(cleanPhotoUrl) { mutableStateOf(false) }

  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(RoyalBlue, PrimaryNavy, DeepNavy)
            )
          )
          .border(2.2.dp, Brush.linearGradient(listOf(PureWhite, borderColor, GlassWhite)), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      if (base64Bitmap != null) {
        Image(
          bitmap = base64Bitmap.asImageBitmap(),
          contentDescription = fullName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      } else if (cleanPhotoUrl != null && !remoteImageFailed) {
        AsyncImage(
          model = cleanPhotoUrl,
          contentDescription = fullName,
          contentScale = ContentScale.Crop,
          onError = { remoteImageFailed = true },
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      } else {
        Text(
          text = initials,
          style =
            if (size >= 72.dp) MaterialTheme.typography.headlineLarge
            else MaterialTheme.typography.titleMedium,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    if (showVerifiedBadge) {
      Box(
        modifier =
          Modifier.align(Alignment.BottomEnd)
            .size(size * 0.28f)
            .clip(CircleShape)
            .background(Color(0xFF00D878))
            .border(2.dp, PureWhite, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "✓",
          color = PureWhite,
          fontSize = (size.value * 0.15f).sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/**
 * Compact 3D Premium Google Icon Button (`Premium3DGoogleIconButton`).
 * Replaces bulky wide Google login buttons with a realistic 3D glass-and-pearl orb
 * containing the official 4-color 3D Google 'G' emblem and an optional subtle caption.
 */
@Composable
fun Premium3DGoogleIconButton(
  onClick: () -> Unit,
  caption: String? = null,
  size: Dp = 56.dp,
  testTag: String = "optional_google_login_button",
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier =
        Modifier.size(size)
          .shadow(
            elevation = 16.dp,
            shape = CircleShape,
            ambientColor = ElectricBlue,
            spotColor = BrandGold,
          )
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xFF1E3C72),
                  Color(0xFF0E224A),
                  DeepNavy,
                )
            )
          )
          .border(
            width = 1.8.dp,
            brush =
              Brush.linearGradient(
                listOf(
                  PureWhite.copy(alpha = 0.90f),
                  BlueNeon,
                  BrandGold.copy(alpha = 0.85f),
                  ElectricBlue,
                )
              ),
            shape = CircleShape,
          )
          .clickable(onClick = onClick)
          .testTag(testTag),
      contentAlignment = Alignment.Center,
    ) {
      // Inner 3D pearl disc + 4-color Google 'G' vector canvas
      Canvas(modifier = Modifier.size(size * 0.68f)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w * 0.48f

        // 3D Pearl White Inner Sphere with subtle top-left specular sheen
        drawCircle(
          brush =
            Brush.linearGradient(
              colors = listOf(PureWhite, Color(0xFFEAF2FF), Color(0xFFD5E5FA)),
              start = Offset(0f, 0f),
              end = Offset(w, h),
            ),
          radius = radius,
          center = center,
        )

        // Official 4-color Google 'G' arcs inside the pearl sphere
        val gStroke = w * 0.165f
        val arcRadius = w * 0.27f
        val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
        val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

        // Red top arc (215° to 315°)
        drawArc(
          color = Color(0xFFEA4335),
          startAngle = 210f,
          sweepAngle = 108f,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = gStroke, cap = StrokeCap.Butt),
        )
        // Yellow left arc (145° to 215°)
        drawArc(
          color = Color(0xFFFBBC05),
          startAngle = 142f,
          sweepAngle = 72f,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = gStroke, cap = StrokeCap.Butt),
        )
        // Green bottom arc (45° to 145°)
        drawArc(
          color = Color(0xFF34A853),
          startAngle = 42f,
          sweepAngle = 104f,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = gStroke, cap = StrokeCap.Butt),
        )
        // Blue right arc (-12° to 45°)
        drawArc(
          color = Color(0xFF4285F4),
          startAngle = -12f,
          sweepAngle = 58f,
          useCenter = false,
          topLeft = arcTopLeft,
          size = arcSize,
          style = Stroke(width = gStroke, cap = StrokeCap.Butt),
        )
        // Horizontal blue crossbar of 'G'
        drawLine(
          color = Color(0xFF4285F4),
          start = Offset(center.x, center.y),
          end = Offset(center.x + arcRadius + gStroke * 0.35f, center.y),
          strokeWidth = gStroke * 0.92f,
          cap = StrokeCap.Butt,
        )
      }
    }

    if (!caption.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        text = caption,
        style = MaterialTheme.typography.labelSmall,
        color = MutedGlassText,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
