package com.zoutiw.omnichurch.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite

/**
 * Fas Dèyè (Verso): Official Back Verification Face with 190dp QR, Emergency Contact, and Pastor Signature.
 */
@Composable
internal fun MemberCardBackFace(
  member: Member,
  qrBitmap: Bitmap?,
  onQrClick: () -> Unit,
  onFlipCard: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF0A1936), PrimaryNavy, DeepNavy)
          )
        )
        .padding(horizontal = 20.dp, vertical = 18.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      OmniChurch3DFlameIcon(size = 28.dp)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = member.churchName.ifBlank { "OMNICHURCH PRO" }.uppercase(),
          style = MaterialTheme.typography.titleMedium,
          color = PureWhite,
          fontWeight = FontWeight.Black,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "SISTÈM VERIFIKASYON & PREZANS OFISYÈL",
          style = MaterialTheme.typography.labelSmall,
          color = BrandGold,
          fontWeight = FontWeight.Bold,
          fontSize = 9.5.sp,
        )
      }
      Box(
        modifier =
          Modifier.size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B))
            .border(1.dp, BrandGold.copy(alpha = 0.7f), CircleShape)
            .clickable(onClick = onFlipCard),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Sync,
          contentDescription = "Retounen sou Recto",
          tint = BrandGold,
          modifier = Modifier.size(16.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    PremiumQRContainer(
      qrBitmap = qrBitmap,
      size = 190.dp,
      showLaserLine = false,
      showCenterFlameBadge = true,
      modifier =
        Modifier.clickable(onClick = onQrClick)
          .testTag("member_card_back_qr_box"),
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "ESKANE NAN PÒT LA • SCAN AT ENTRANCE",
      style = MaterialTheme.typography.labelSmall,
      color = BrandGold,
      fontWeight = FontWeight.Black,
      letterSpacing = 0.5.sp,
    )

    Spacer(modifier = Modifier.height(10.dp))

    val canonicalBarcode = member.canonicalBarcodeDisplay
    if (canonicalBarcode.isNotBlank()) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureWhite)
            .border(1.dp, BlueNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        CanonicalBarcodeStrip(
          value = canonicalBarcode,
          modifier = Modifier.fillMaxWidth().height(28.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = canonicalBarcode,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
          color = DeepNavy,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
    }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF040B1A))
          .border(1.dp, GlassWhite.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "📞 Telefòn: ${member.phone?.ifBlank { null } ?: "Pa defini"}",
          style = MaterialTheme.typography.labelSmall,
          color = GlassWhite,
        )
        Text(
          text = "🚨 Dijans: ${member.emergencyContact?.ifBlank { null } ?: "Sant Legliz"}",
          style = MaterialTheme.typography.labelSmall,
          color = BrandGold,
          fontWeight = FontWeight.SemiBold,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Siyati Ofisyèl: Direksyon",
          style = MaterialTheme.typography.labelSmall,
          color = GlassWhite.copy(alpha = 0.75f),
        )
        Text(
          text = "ID: ${member.effectiveMatrikil}",
          style = MaterialTheme.typography.labelSmall,
          color = BlueNeon,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier =
        Modifier.clip(RoundedCornerShape(50))
          .background(Color(0xFF142850))
          .border(1.dp, BrandGold.copy(alpha = 0.5f), RoundedCornerShape(50))
          .clickable(onClick = onFlipCard)
          .padding(horizontal = 14.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Default.Sync,
        contentDescription = null,
        tint = BrandGold,
        modifier = Modifier.size(15.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "↶ Retounen sou Fas Devan Kat la (Recto)",
        style = MaterialTheme.typography.labelSmall,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
