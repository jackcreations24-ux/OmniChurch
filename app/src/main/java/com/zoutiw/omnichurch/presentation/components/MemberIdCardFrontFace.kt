package com.zoutiw.omnichurch.presentation.components

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun MemberCardReferenceFrontFace(
  member: Member,
  qrBitmap: Bitmap?,
  onCopyMemberCode: () -> Unit,
  onChangePhotoClick: () -> Unit,
  onQrClick: () -> Unit,
  onFlipCard: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxWidth()) {
    // 1. Top Cinematic Church Sanctuary Backdrop on the upper header
    Image(
      painter = painterResource(id = R.drawable.omnichurch_home_bg),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alignment = Alignment.TopEnd,
      modifier =
        Modifier.fillMaxWidth()
          .height(175.dp)
          .align(Alignment.TopCenter),
    )

    // 2. Custom 3D Waves: Deep Royal Blue top-left wave, Golden-Orange 3D metallic ribbon,
    //    Silver-Pearl curved body, and bottom-left Golden-Orange metallic wave
    Canvas(modifier = Modifier.matchParentSize()) {
      val w = size.width
      val h = size.height

      val topLeftBlueWave =
        Path().apply {
          moveTo(0f, 0f)
          lineTo(w * 0.72f, 0f)
          cubicTo(w * 0.48f, h * 0.08f, w * 0.25f, h * 0.18f, 0f, h * 0.27f)
          close()
        }
      drawPath(
        path = topLeftBlueWave,
        brush =
          Brush.linearGradient(
            colors =
              listOf(
                PrimaryNavy.copy(alpha = 0.94f),
                RoyalBlue.copy(alpha = 0.88f),
                ElectricBlue.copy(alpha = 0.55f),
              ),
            start = Offset(0f, 0f),
            end = Offset(w * 0.65f, h * 0.25f),
          ),
      )

      val goldRibbonPath =
        Path().apply {
          moveTo(0f, h * 0.21f)
          cubicTo(
            w * 0.28f,
            h * 0.14f,
            w * 0.68f,
            h * 0.27f,
            w,
            h * 0.22f,
          )
          lineTo(w, h * 0.30f)
          cubicTo(
            w * 0.65f,
            h * 0.33f,
            w * 0.28f,
            h * 0.21f,
            0f,
            h * 0.29f,
          )
          close()
        }
      drawPath(
        path = goldRibbonPath,
        brush =
          Brush.horizontalGradient(
            colors = listOf(BrandOrange, BrandGold, PureWhite, BrandGold, BrandOrange)
          ),
      )

      val pearlBodyPath =
        Path().apply {
          moveTo(0f, h * 0.27f)
          cubicTo(
            w * 0.30f,
            h * 0.20f,
            w * 0.66f,
            h * 0.31f,
            w,
            h * 0.25f,
          )
          lineTo(w, h)
          lineTo(0f, h)
          close()
        }
      drawPath(
        path = pearlBodyPath,
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0xFFF3F8FF),
                Color(0xFFDCEBFF),
                Color(0xFFC7E0FC),
              ),
            startY = h * 0.22f,
            endY = h,
          ),
      )

      val bottomGoldWave =
        Path().apply {
          moveTo(0f, h * 0.78f)
          quadraticTo(w * 0.22f, h * 0.88f, w * 0.46f, h)
          lineTo(0f, h)
          close()
        }
      drawPath(
        path = bottomGoldWave,
        brush =
          Brush.linearGradient(
            colors = listOf(BrandGold, BrandOrange, Color(0xFFB85D00)),
            start = Offset(0f, h * 0.78f),
            end = Offset(w * 0.45f, h),
          ),
      )

      val bottomBlueWave =
        Path().apply {
          moveTo(w, h * 0.80f)
          quadraticTo(w * 0.76f, h * 0.90f, w * 0.48f, h)
          lineTo(w, h)
          close()
        }
      drawPath(
        path = bottomBlueWave,
        brush =
          Brush.linearGradient(
            colors = listOf(ElectricBlue.copy(alpha = 0.32f), RoyalBlue.copy(alpha = 0.65f)),
            start = Offset(w * 0.5f, h * 0.82f),
            end = Offset(w, h),
          ),
      )
    }

    // 3. Foreground Card Content
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top Header Row: OmniChurch Pro Brand + Flip to Verso Button
      Box(modifier = Modifier.fillMaxWidth()) {
        OmniChurchBrandHeader(
          badgeText = stringResource(R.string.app_subtitle_pro),
          isProStyle = true,
          modifier = Modifier.align(Alignment.Center),
        )

        Box(
          modifier =
            Modifier.align(Alignment.TopEnd)
              .size(34.dp)
              .clip(CircleShape)
              .background(DeepNavy.copy(alpha = 0.68f))
              .border(1.dp, BrandGold.copy(alpha = 0.75f), CircleShape)
              .clickable(onClick = onFlipCard)
              .testTag("btn_flip_member_card"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Vire Kat la (Verso)",
            tint = BrandGold,
            modifier = Modifier.size(17.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Circular Member Photograph in 3D Silver/White Ring + Verified Green Badge + Camera Button
      Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center,
      ) {
        val photoUrl = member.effectivePhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
        val base64Bitmap = remember(photoUrl) { ImageCompressor.decodeBase64DataUrlToBitmap(photoUrl) }
        var remoteImageFailed by remember(photoUrl) { mutableStateOf(false) }
        Box(
          modifier =
            Modifier.size(108.dp)
              .shadow(14.dp, CircleShape, ambientColor = ElectricBlue, spotColor = DeepNavy)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(listOf(RoyalBlue, PrimaryNavy, DeepNavy))
              )
              .border(
                width = 3.8.dp,
                brush = Brush.verticalGradient(listOf(PureWhite, Color(0xFFDCEBFF), PureWhite)),
                shape = CircleShape,
              ),
          contentAlignment = Alignment.Center,
        ) {
          if (base64Bitmap != null) {
            Image(
              bitmap = base64Bitmap.asImageBitmap(),
              contentDescription = member.effectiveFullName,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
          } else if (photoUrl != null && !remoteImageFailed) {
            AsyncImage(
              model = photoUrl,
              contentDescription = member.effectiveFullName,
              contentScale = ContentScale.Crop,
              onError = { remoteImageFailed = true },
              modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
          } else {
            val initials =
              member.effectiveFullName
                .trim()
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifEmpty { "OC" }
            Text(
              text = initials,
              style = MaterialTheme.typography.headlineLarge,
              color = PureWhite,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }

        Box(
          modifier =
            Modifier.align(Alignment.BottomEnd)
              .padding(end = 2.dp, bottom = 2.dp)
              .size(30.dp)
              .shadow(6.dp, CircleShape, ambientColor = SuccessGreen)
              .clip(CircleShape)
              .background(
                Brush.verticalGradient(listOf(Color(0xFF19E68C), SuccessGreen, Color(0xFF069E56)))
              )
              .border(2.2.dp, PureWhite, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Verified",
            tint = PureWhite,
            modifier = Modifier.size(17.dp),
          )
        }

        Box(
          modifier =
            Modifier.align(Alignment.BottomStart)
              .padding(start = 2.dp, bottom = 2.dp)
              .size(30.dp)
              .shadow(6.dp, CircleShape, ambientColor = BrandGold)
              .clip(CircleShape)
              .background(Brush.verticalGradient(listOf(BrandGold, BrandOrange)))
              .border(2.dp, PureWhite, CircleShape)
              .clickable(onClick = onChangePhotoClick)
              .testTag("btn_change_member_photo"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "Chanje Foto Pwofil",
            tint = DeepNavy,
            modifier = Modifier.size(15.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      val displayTitleAndName =
        buildString {
          if (!member.titlePrefix.isNullOrBlank()) {
            append(member.titlePrefix.trim())
            append(" ")
          }
          append(member.effectiveFullName.ifBlank { "Manm OmniChurch" })
        }

      Text(
        text = displayTitleAndName,
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 23.sp),
        color = DeepNavy,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(5.dp))

      Box(
        modifier =
          Modifier.shadow(8.dp, RoundedCornerShape(50), ambientColor = SuccessGreen)
            .clip(RoundedCornerShape(50))
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF00D878), Color(0xFF08B865), Color(0xFF00D878))
              )
            )
            .border(1.2.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 4.dp)
      ) {
        Text(
          text = stringResource(R.string.member_status_active),
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CardMetadataColumn(
          icon = Icons.Default.Groups,
          label = stringResource(R.string.member_label_department),
          value = member.department.ifBlank { "Adorasyon" },
          modifier = Modifier.weight(1f),
        )
        CardMetadataColumn(
          icon = Icons.Default.Person,
          label = stringResource(R.string.member_label_role),
          value = member.role.replaceFirstChar { it.uppercase() }.ifBlank { "Manm" },
          modifier = Modifier.weight(1f),
        )
        CardMetadataColumn(
          icon = Icons.Default.CalendarMonth,
          label = stringResource(R.string.member_label_join_date),
          value = member.effectiveJoinedDate,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      PremiumQRContainer(
        qrBitmap = qrBitmap,
        size = 192.dp,
        showLaserLine = false,
        showCenterFlameBadge = true,
        modifier = Modifier.clickable(onClick = onQrClick),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(Color(0xD9D0E3FA))
            .border(1.dp, ElectricBlue.copy(alpha = 0.45f), RoundedCornerShape(50))
            .clickable(onClick = onCopyMemberCode)
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .testTag("copy_member_code_pill"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.member_id_prefix, member.effectiveMatrikil),
          style = MaterialTheme.typography.titleSmall,
          color = DeepNavy,
          fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = stringResource(R.string.member_id_copied),
          tint = RoyalBlue,
          modifier = Modifier.size(17.dp),
        )
      }
    }
  }
}

@Composable
private fun CardMetadataColumn(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.padding(horizontal = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = RoyalBlue,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
        color = Color(0xFF3E5A82),
        maxLines = 1,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
        color = DeepNavy,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
