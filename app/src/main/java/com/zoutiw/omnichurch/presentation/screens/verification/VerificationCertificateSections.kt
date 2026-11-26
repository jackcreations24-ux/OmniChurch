package com.zoutiw.omnichurch.presentation.screens.verification

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.core.utilities.VerificationPdfPrinter
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassPanel
import com.zoutiw.omnichurch.presentation.components.OmniChurch3DFlameIcon
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.util.Locale

@Composable
internal fun VerificationDeclarationCard(
  church: Church,
  verificationConfig: RemoteMemberVerificationConfig,
  labels: VerificationStrings,
  seal: String,
  verifiedAtTimestamp: String,
  qrBitmap: Bitmap?,
) {
  GlassPanel(
    modifier = Modifier.fillMaxWidth(),
    borderColors = listOf(BrandGold.copy(alpha = 0.55f), BlueNeon.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = labels.declarationTitle,
          style = MaterialTheme.typography.titleSmall,
          color = BrandGold,
          fontWeight = FontWeight.Bold,
        )
      }
      Text(
        text = labels.declarationBody,
        style = MaterialTheme.typography.bodySmall,
        color = GlassWhite,
      )
      if (verificationConfig.customAuthorityNotice.isNotBlank()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(ElectricBlue.copy(alpha = 0.16f))
              .border(1.dp, BlueNeon.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
              .padding(10.dp),
        ) {
          Text(
            text = verificationConfig.customAuthorityNotice,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      HorizontalDivider(color = PureWhite.copy(alpha = 0.12f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          val sigUrl = church.signatureUrl?.trim()?.takeIf { it.isNotEmpty() }
          val sigBitmap =
            remember(sigUrl) { ImageCompressor.decodeBase64DataUrlToBitmap(sigUrl) }
          if (sigBitmap != null) {
            Image(
              bitmap = sigBitmap.asImageBitmap(),
              contentDescription = labels.authorizedDirectionLabel,
              contentScale = ContentScale.Fit,
              modifier = Modifier.height(40.dp).fillMaxWidth(0.75f),
            )
          } else if (sigUrl != null) {
            AsyncImage(
              model = sigUrl,
              contentDescription = labels.authorizedDirectionLabel,
              contentScale = ContentScale.Fit,
              modifier = Modifier.height(40.dp).fillMaxWidth(0.75f),
            )
          } else {
            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(
              modifier = Modifier.fillMaxWidth(0.75f),
              thickness = 1.dp,
              color = GlassWhite.copy(alpha = 0.6f),
            )
          }
          Text(
            text = labels.authorizedDirectionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MutedGlassText,
          )
          Text(
            text = "${labels.securitySealLabel}: $seal",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = SuccessGreen,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${labels.verifiedAtLabel}: $verifiedAtTimestamp",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MutedGlassText,
            fontFamily = FontFamily.Monospace,
          )
        }

        qrBitmap?.let { bmp ->
          Box(
            modifier =
              Modifier.size(86.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PureWhite)
                .border(1.dp, BlueNeon.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = "QR Verification URL",
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun VerificationActionButtons(
  context: Context,
  church: Church,
  member: Member,
  verifyUrl: String,
  qrBitmap: Bitmap?,
  isLicensedChurch: Boolean,
  verificationConfig: RemoteMemberVerificationConfig,
  activeLang: String,
  labels: VerificationStrings,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedButton(
      onClick = {
        val clipboard =
          context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
          ClipData.newPlainText("OmniChurch Verification Link", verifyUrl)
        )
        Toast.makeText(context, labels.copiedToast, Toast.LENGTH_SHORT).show()
      },
      modifier =
        Modifier.weight(1f)
          .minimumInteractiveComponentSize()
          .testTag("btn_verification_copy_link"),
      shape = RoundedCornerShape(14.dp),
    ) {
      Icon(
        imageVector = Icons.Default.ContentCopy,
        contentDescription = labels.copyLinkLabel,
        tint = BlueNeon,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = labels.copyLinkLabel,
        color = PureWhite,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    OutlinedButton(
      onClick = {
        val shareBody =
          "${church.name} • ${member.effectiveFullName} (${member.effectiveMatrikil})\n$verifyUrl"
        val sendIntent =
          Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareBody)
          }
        context.startActivity(
          Intent.createChooser(sendIntent, labels.shareLinkLabel).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        )
      },
      modifier =
        Modifier.weight(1f)
          .minimumInteractiveComponentSize()
          .testTag("btn_verification_share"),
      shape = RoundedCornerShape(14.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Share,
        contentDescription = labels.shareLinkLabel,
        tint = BrandGold,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = labels.shareLinkLabel,
        color = PureWhite,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }

  Button(
    onClick = {
      VerificationPdfPrinter.printMemberCertificate(
        context = context,
        church = church,
        member = member,
        qrBitmap = qrBitmap,
        isLicensedChurch = isLicensedChurch,
        verificationConfig = verificationConfig,
        languageCode = activeLang,
      )
    },
    modifier =
      Modifier.fillMaxWidth()
        .height(50.dp)
        .testTag("btn_verification_print_pdf"),
    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
    shape = RoundedCornerShape(14.dp),
  ) {
    Icon(
      imageVector = Icons.Default.Print,
      contentDescription = labels.printPdfLabel,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = labels.printPdfLabel,
      fontWeight = FontWeight.Bold,
    )
  }
}
