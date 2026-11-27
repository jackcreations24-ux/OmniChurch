package com.zoutiw.omnichurch.presentation.screens.verification

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.buildCanonicalQrPayload
import com.zoutiw.omnichurch.data.models.computeDeterministicVerificationSeal
import com.zoutiw.omnichurch.data.models.maskLegalDocumentNumber
import com.zoutiw.omnichurch.presentation.components.CanonicalBarcodeStrip
import com.zoutiw.omnichurch.presentation.components.GlassPanel
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun OfficialCertificateContent(
  context: Context,
  church: Church,
  member: Member,
  verificationConfig: RemoteMemberVerificationConfig,
  isLicensedChurch: Boolean,
  activeLang: String,
  labels: VerificationStrings,
) {
  val effectiveChurchId = church.id.ifBlank { member.churchId }
  val verifyUrl =
    remember(effectiveChurchId, member.effectiveMatrikil, verificationConfig.effectiveVerificationBaseUrl) {
      buildCanonicalQrPayload(
        churchId = effectiveChurchId,
        memberId = member.effectiveMatrikil,
        verificationBaseUrl = verificationConfig.effectiveVerificationBaseUrl,
      )
    }

  var qrBitmap by remember(verifyUrl) { mutableStateOf<Bitmap?>(null) }
  LaunchedEffect(verifyUrl) {
    qrBitmap = QrCodeGenerator.generateQrBitmap(token = verifyUrl, sizePx = 512)
  }

  val normalizedStatus = member.status.trim().lowercase(Locale.US)
  val isMemberActive = normalizedStatus !in setOf("suspended", "inactive")
  val seal =
    remember(effectiveChurchId, member.id, member.effectiveMatrikil, isMemberActive) {
      computeDeterministicVerificationSeal(
        churchId = effectiveChurchId,
        memberDocId = member.id,
        memberId = member.effectiveMatrikil,
        isActive = isMemberActive,
      )
    }
  val verifiedAtTimestamp =
    remember(activeLang) {
      SimpleDateFormat("yyyy-MM-dd • HH:mm", Locale.getDefault()).format(Date())
    }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 620.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Church Header & Authority Badges
      GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColors = listOf(BlueNeon.copy(alpha = 0.65f), BrandGold.copy(alpha = 0.35f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            val resolvedVerificationLogo =
              church.logoUrl?.trim()?.takeIf { it.isNotEmpty() }
                ?: member.churchLogoUrl?.trim()?.takeIf { it.isNotEmpty() }
            ChurchLogoAvatar(logoUrl = resolvedVerificationLogo, churchName = church.name)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = church.name.ifBlank { member.churchName.ifBlank { "OmniChurch" } },
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              val locText =
                com.zoutiw.omnichurch.core.utilities.ChurchLocationDetector.formatChurchModelLocation(
                  city = church.city,
                  address = church.address,
                  language = activeLang,
                  explicitCountry = church.country,
                  context = context,
                )
              if (locText.isNotEmpty()) {
                Text(
                  text = locText,
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedGlassText,
                )
              }
              Text(
                text = labels.officialCertificateSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = BrandGold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
              )
            }
          }

          if (isLicensedChurch || (verificationConfig.showChurchRncBadge && church.rnc.isNotBlank())) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              if (isLicensedChurch) {
                AuthorityPillBadge(
                  text = labels.licensedChurchBadge,
                  accentColor = SuccessGreen,
                  icon = Icons.Default.Verified,
                )
              }
              if (verificationConfig.showChurchRncBadge && church.rnc.isNotBlank()) {
                val rncText = "RNC: ${church.rnc}${if (church.rncVerified) " ✓" else ""}"
                AuthorityPillBadge(
                  text = rncText,
                  accentColor = BrandGold,
                  icon = Icons.Default.Shield,
                )
              }
            }
          }
        }
      }

      // 2. Status Banner (enforced or advisory depending on enforceActiveStatusCheck)
      val bannerColor =
        when {
          isMemberActive -> SuccessGreen
          verificationConfig.enforceActiveStatusCheck -> ErrorRed
          else -> BrandGold
        }
      val bannerBrush =
        when {
          isMemberActive -> OmniBrushes.SuccessGlassBrush
          verificationConfig.enforceActiveStatusCheck -> OmniBrushes.ErrorGlassBrush
          else -> OmniBrushes.GlassCardBrush
        }
      GlassPanel(
        modifier = Modifier.fillMaxWidth().testTag("verification_status_banner"),
        backgroundBrush = bannerBrush,
        borderColors = listOf(bannerColor.copy(alpha = 0.8f), bannerColor.copy(alpha = 0.25f)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector =
              when {
                isMemberActive -> Icons.Default.CheckCircle
                verificationConfig.enforceActiveStatusCheck -> Icons.Default.Block
                else -> Icons.Default.WarningAmber
              },
            contentDescription = null,
            tint = bannerColor,
            modifier = Modifier.size(34.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                if (isMemberActive) {
                  labels.activeBannerTitle
                } else if (verificationConfig.enforceActiveStatusCheck) {
                  labels.inactiveBannerTitle
                } else {
                  "${labels.statusAdvisoryTitle}: ${member.status.uppercase(Locale.US)}"
                },
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text =
                if (isMemberActive) labels.activeBannerSubtitle else labels.inactiveBannerSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite,
            )
          }
        }
      }

      // 3. Member Identity Card & Canonical 1D Barcode
      PremiumGlassCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = true,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (verificationConfig.showMemberPhoto) {
            VerificationMemberPhoto(member = member)
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val fullTitleName =
              buildString {
                if (!member.titlePrefix.isNullOrBlank()) {
                  append(member.titlePrefix.trim())
                  append(" ")
                }
                append(member.effectiveFullName.ifBlank { "Manm OmniChurch" })
              }
            Text(
              text = fullTitleName,
              style = MaterialTheme.typography.titleLarge,
              color = PureWhite,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text = "${labels.memberIdLabel}: ${member.effectiveMatrikil}",
              style = MaterialTheme.typography.labelLarge,
              color = BrandGold,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            )
            val roleLine =
              listOf(member.department, member.role.replaceFirstChar { it.uppercase() })
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            if (roleLine.isNotEmpty()) {
              Text(
                text = roleLine,
                style = MaterialTheme.typography.bodySmall,
                color = GlassWhite,
              )
            }
          }
        }

        val canonicalBarcode = member.canonicalBarcodeDisplay
        if (canonicalBarcode.isNotBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PureWhite)
                .border(1.dp, BlueNeon.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            CanonicalBarcodeStrip(
              value = canonicalBarcode,
              modifier = Modifier.fillMaxWidth().height(34.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = canonicalBarcode,
              style = MaterialTheme.typography.labelSmall,
              color = DeepNavy,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = PureWhite.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(12.dp))

        // Metadata Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          MetadataPairRow(
            leftLabel = labels.joinedDateLabel,
            leftValue = member.effectiveJoinedDate,
            rightLabel = labels.pastorLabel,
            rightValue = church.pastor.ifBlank { "Direksyon Legliz" },
          )
          if (!member.dateOfBirth.isNullOrBlank() || !member.baptismDate.isNullOrBlank()) {
            MetadataPairRow(
              leftLabel = labels.dobLabel,
              leftValue = member.dateOfBirth?.ifBlank { "—" } ?: "—",
              rightLabel = labels.baptismDateLabel,
              rightValue = member.baptismDate?.ifBlank { "—" } ?: "—",
            )
          }
          if (verificationConfig.showLegalDocuments && !member.documentNumber.isNullOrBlank()) {
            val docType = member.documentType?.takeIf { it.isNotBlank() } ?: labels.legalDocLabel
            val masked =
              maskLegalDocumentNumber(
                member.documentNumber,
                verificationConfig.maskLegalDocumentNumber,
              )
            MetadataSingleRow(label = docType, value = masked)
          }
          if (!member.emergencyContactName.isNullOrBlank() || !member.emergencyContact.isNullOrBlank()) {
            val emergencyValue =
              listOfNotNull(
                  member.emergencyContactName?.takeIf { it.isNotBlank() },
                  member.emergencyContact?.takeIf { it.isNotBlank() },
                )
                .joinToString(" • ")
            MetadataSingleRow(label = labels.emergencyLabel, value = emergencyValue)
          }
        }
      }

      // 4. Church Contact Info (gated by showChurchContactInfo)
      if (verificationConfig.showChurchContactInfo) {
        val hasAnyContact =
          church.address.isNotBlank() ||
            church.phone.isNotBlank() ||
            church.email.isNotBlank() ||
            church.website.isNotBlank()
        if (hasAnyContact) {
          GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = labels.churchContactTitle,
                style = MaterialTheme.typography.titleSmall,
                color = BlueNeon,
                fontWeight = FontWeight.Bold,
              )
              if (church.address.isNotBlank()) {
                ContactInfoLine(Icons.Default.LocationOn, church.address)
              }
              if (church.phone.isNotBlank()) {
                ContactInfoLine(Icons.Default.Phone, church.phone)
              }
              if (church.email.isNotBlank()) {
                ContactInfoLine(Icons.Default.Email, church.email)
              }
              if (church.website.isNotBlank()) {
                ContactInfoLine(Icons.Default.Language, church.website)
              }
            }
          }
        }
      }

      // 5. Official Declaration, Custom Authority Notice, Signature, Seal & QR Preview
      VerificationDeclarationCard(
        church = church,
        verificationConfig = verificationConfig,
        labels = labels,
        seal = seal,
        verifiedAtTimestamp = verifiedAtTimestamp,
        qrBitmap = qrBitmap,
      )

      // 6. Action Buttons (Copy Link, Share, Print/Save PDF)
      if (verificationConfig.allowPrintAndShare) {
        VerificationActionButtons(
          context = context,
          church = church,
          member = member,
          verifyUrl = verifyUrl,
          qrBitmap = qrBitmap,
          isLicensedChurch = isLicensedChurch,
          verificationConfig = verificationConfig,
          activeLang = activeLang,
          labels = labels,
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}
