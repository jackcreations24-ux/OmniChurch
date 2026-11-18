package com.zoutiw.omnichurch.presentation.screens.verification

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.core.utilities.VerificationPdfPrinter
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.buildCanonicalQrPayload
import com.zoutiw.omnichurch.data.models.computeDeterministicVerificationSeal
import com.zoutiw.omnichurch.data.models.maskLegalDocumentNumber
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.presentation.components.CanonicalBarcodeStrip
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassPanel
import com.zoutiw.omnichurch.presentation.components.OmniChurch3DFlameIcon
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.ErrorRedDark
import com.zoutiw.omnichurch.ui.theme.GlassCardBorderSubtle
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MidnightNavy
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import com.zoutiw.omnichurch.ui.theme.SuccessGreenDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class VerificationStrings(
  val topBarTitle: String,
  val loadingLabel: String,
  val errorTitle: String,
  val retryButton: String,
  val closeButton: String,
  val disabledTitle: String,
  val disabledBody: String,
  val officialCertificateSubtitle: String,
  val licensedChurchBadge: String,
  val activeBannerTitle: String,
  val activeBannerSubtitle: String,
  val inactiveBannerTitle: String,
  val inactiveBannerSubtitle: String,
  val statusAdvisoryTitle: String,
  val departmentLabel: String,
  val roleLabel: String,
  val memberIdLabel: String,
  val joinedDateLabel: String,
  val dobLabel: String,
  val baptismDateLabel: String,
  val legalDocLabel: String,
  val emergencyLabel: String,
  val pastorLabel: String,
  val churchContactTitle: String,
  val addressLabel: String,
  val phoneLabel: String,
  val emailLabel: String,
  val websiteLabel: String,
  val declarationTitle: String,
  val declarationBody: String,
  val authorizedDirectionLabel: String,
  val securitySealLabel: String,
  val verifiedAtLabel: String,
  val copyLinkLabel: String,
  val shareLinkLabel: String,
  val printPdfLabel: String,
  val copiedToast: String,
)

private fun resolveVerificationStrings(lang: String): VerificationStrings =
  when (lang.trim().lowercase(Locale.US)) {
    "fr" ->
      VerificationStrings(
        topBarTitle = "Vérification Officielle",
        loadingLabel = "Vérification du registre officiel en cours...",
        errorTitle = "Certificat introuvable",
        retryButton = "Réessayer",
        closeButton = "Fermer",
        disabledTitle = "Vérification publique désactivée",
        disabledBody =
          "La vérification publique des cartes de membre est actuellement désactivée pour cette église par l'administration.",
        officialCertificateSubtitle = "CERTIFICAT OFFICIEL D'APPARTENANCE",
        licensedChurchBadge = "Église Officielle Licenciée ✓",
        activeBannerTitle = "MEMBRE ACTIF & VÉRIFIÉ",
        activeBannerSubtitle = "Identité confirmée dans le registre officiel de l'église.",
        inactiveBannerTitle = "STATUT INACTIF / SUSPENDU",
        inactiveBannerSubtitle =
          "Ce dossier n'est pas actuellement actif. Veuillez contacter la direction de l'église.",
        statusAdvisoryTitle = "Avis de statut",
        departmentLabel = "Département",
        roleLabel = "Rôle / Ministère",
        memberIdLabel = "Matricule",
        joinedDateLabel = "Date d'adhésion",
        dobLabel = "Date de naissance",
        baptismDateLabel = "Date de baptême",
        legalDocLabel = "Document légal",
        emergencyLabel = "Contact d'urgence",
        pastorLabel = "Pasteur",
        churchContactTitle = "Coordonnées de l'Église",
        addressLabel = "Adresse",
        phoneLabel = "Téléphone",
        emailLabel = "Courriel",
        websiteLabel = "Site Web",
        declarationTitle = "Déclaration Officielle d'Autorité",
        declarationBody =
          "Ce certificat atteste numériquement l'authenticité du membre ci-dessus auprès des autorités civiles, consulaires et institutionnelles.",
        authorizedDirectionLabel = "Direction Autorisée de l'Église",
        securitySealLabel = "Sceau Cryptographique",
        verifiedAtLabel = "Vérifié le",
        copyLinkLabel = "Copier le lien",
        shareLinkLabel = "Partager",
        printPdfLabel = "PDF / Imprimer",
        copiedToast = "Lien de vérification officiel copié !",
      )
    "en" ->
      VerificationStrings(
        topBarTitle = "Official Verification",
        loadingLabel = "Verifying official registry record...",
        errorTitle = "Verification Record Not Found",
        retryButton = "Retry",
        closeButton = "Close",
        disabledTitle = "Public Verification Disabled",
        disabledBody =
          "Public member ID verification is currently disabled for this church by the administrator.",
        officialCertificateSubtitle = "OFFICIAL MEMBERSHIP CERTIFICATE",
        licensedChurchBadge = "Official Licensed Church ✓",
        activeBannerTitle = "ACTIVE & VERIFIED MEMBER",
        activeBannerSubtitle = "Membership status confirmed in the official church registry.",
        inactiveBannerTitle = "INACTIVE / SUSPENDED STATUS",
        inactiveBannerSubtitle =
          "This member record is currently inactive or suspended. Please contact church leadership.",
        statusAdvisoryTitle = "Status Notice",
        departmentLabel = "Department",
        roleLabel = "Role / Ministry",
        memberIdLabel = "Member ID",
        joinedDateLabel = "Joined Date",
        dobLabel = "Date of Birth",
        baptismDateLabel = "Baptism Date",
        legalDocLabel = "Legal Document",
        emergencyLabel = "Emergency Contact",
        pastorLabel = "Pastor",
        churchContactTitle = "Church Contact Information",
        addressLabel = "Address",
        phoneLabel = "Phone",
        emailLabel = "Email",
        websiteLabel = "Website",
        declarationTitle = "Official Authority Declaration",
        declarationBody =
          "This digital certificate attests to the authenticity and standing of the member above for civic, institutional, and official verification.",
        authorizedDirectionLabel = "Authorized Church Leadership",
        securitySealLabel = "Security Seal",
        verifiedAtLabel = "Verified At",
        copyLinkLabel = "Copy Link",
        shareLinkLabel = "Share",
        printPdfLabel = "Save PDF",
        copiedToast = "Official verification link copied!",
      )
    "es" ->
      VerificationStrings(
        topBarTitle = "Verificación Oficial",
        loadingLabel = "Verificando registro oficial...",
        errorTitle = "Registro no encontrado",
        retryButton = "Reintentar",
        closeButton = "Cerrar",
        disabledTitle = "Verificación pública deshabilitada",
        disabledBody =
          "La verificación pública de credenciales está actualmente deshabilitada para esta iglesia.",
        officialCertificateSubtitle = "CERTIFICADO OFICIAL DE MEMBRESÍA",
        licensedChurchBadge = "Iglesia Oficial Licenciada ✓",
        activeBannerTitle = "MIEMBRO ACTIVO Y VERIFICADO",
        activeBannerSubtitle = "Identidad confirmada en el registro oficial de la iglesia.",
        inactiveBannerTitle = "ESTADO INACTIVO / SUSPENDIDO",
        inactiveBannerSubtitle =
          "Este registro no está activo actualmente. Contacte con la administración de la iglesia.",
        statusAdvisoryTitle = "Aviso de estado",
        departmentLabel = "Departamento",
        roleLabel = "Rol / Ministerio",
        memberIdLabel = "Matrícula",
        joinedDateLabel = "Fecha de ingreso",
        dobLabel = "Fecha de nacimiento",
        baptismDateLabel = "Fecha de bautismo",
        legalDocLabel = "Documento legal",
        emergencyLabel = "Contacto de emergencia",
        pastorLabel = "Pastor",
        churchContactTitle = "Contacto de la Iglesia",
        addressLabel = "Dirección",
        phoneLabel = "Teléfono",
        emailLabel = "Correo",
        websiteLabel = "Sitio web",
        declarationTitle = "Declaración Oficial de Autoridad",
        declarationBody =
          "Este certificado digital acredita la autenticidad del miembro indicado ante autoridades civiles e institucionales.",
        authorizedDirectionLabel = "Dirección Autorizada de la Iglesia",
        securitySealLabel = "Sello de Seguridad",
        verifiedAtLabel = "Verificado el",
        copyLinkLabel = "Copiar enlace",
        shareLinkLabel = "Compartir",
        printPdfLabel = "PDF / Imprimir",
        copiedToast = "¡Enlace de verificación oficial copiado!",
      )
    else ->
      VerificationStrings(
        topBarTitle = "Verifikasyon Ofisyèl",
        loadingLabel = "N ap verifye dosye ofisyèl la...",
        errorTitle = "Dosye Verifikasyon Pa Jwenn",
        retryButton = "Re-eseye",
        closeButton = "Fèmen",
        disabledTitle = "Verifikasyon Piblik Dezaktive",
        disabledBody =
          "Verifikasyon piblik kat manm lan dezaktive kounye a pou legliz sa a nan paramèt administrasyon an.",
        officialCertificateSubtitle = "SÈTIFIKA OFISYÈL VERIFIKASYON MANM",
        licensedChurchBadge = "Legliz Ofisyèl ki gen Lisans ✓",
        activeBannerTitle = "MANM AKTIF & VERIFYE",
        activeBannerSubtitle = "Estati manm lan konfime nan rejis ofisyèl legliz la.",
        inactiveBannerTitle = "STATI INAKTIF / SISPAN",
        inactiveBannerSubtitle =
          "Dosye manm sa a pa aktif kounye a. Tanpri kontakte direksyon legliz la.",
        statusAdvisoryTitle = "Nòt sou Estati",
        departmentLabel = "Depatman",
        roleLabel = "Wòl / Ministè",
        memberIdLabel = "Matrikil",
        joinedDateLabel = "Dat Antre",
        dobLabel = "Dat Nesans",
        baptismDateLabel = "Dat Batèm",
        legalDocLabel = "Dokiman Legal",
        emergencyLabel = "Kontak Ijans",
        pastorLabel = "Pastè",
        churchContactTitle = "Kowòdone Legliz la",
        addressLabel = "Adrès",
        phoneLabel = "Telefòn",
        emailLabel = "Imèl",
        websiteLabel = "Sit Wèb",
        declarationTitle = "Deklarasyon Ofisyèl Otorite",
        declarationBody =
          "Sètifika nimerik sa a ateste otantisite ak apatenans manm ki site anwo a devan otorite sivil, konsilè ak enstitisyonèl yo.",
        authorizedDirectionLabel = "Direksyon Otorize Legliz la",
        securitySealLabel = "So Sekirite Kriptografik",
        verifiedAtLabel = "Verifye nan dat",
        copyLinkLabel = "Kopye Lyen",
        shareLinkLabel = "Pataje",
        printPdfLabel = "Sove PDF",
        copiedToast = "Lyen verifikasyon ofisyèl la kopye!",
      )
  }

@Composable
fun MemberPublicVerificationScreen(
  verificationState: UiState<PublicVerificationResolution?>,
  verificationConfig: RemoteMemberVerificationConfig,
  isChurchLicensed: (String?, String?) -> Boolean,
  initialLanguageCode: String = "ht",
  onRetry: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onClose)
  val context = LocalContext.current
  var activeLang by remember(initialLanguageCode) {
    mutableStateOf(
      initialLanguageCode.trim().lowercase(Locale.US).takeIf { it in setOf("ht", "fr", "en", "es") }
        ?: "ht"
    )
  }
  val labels = remember(activeLang) { resolveVerificationStrings(activeLang) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .statusBarsPadding()
        .navigationBarsPadding(),
  ) {
    VerificationTopBar(
      title = labels.topBarTitle,
      activeLang = activeLang,
      onSelectLang = { activeLang = it },
      onClose = onClose,
    )

    when (verificationState) {
      is UiState.Loading -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            GlassLoadingIndicator()
            Text(
              text = labels.loadingLabel,
              style = MaterialTheme.typography.bodyMedium,
              color = GlassWhite,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
      is UiState.Error -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(20.dp),
          contentAlignment = Alignment.Center,
        ) {
          GlassPanel(
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
            borderColors = listOf(ErrorRed.copy(alpha = 0.7f), ErrorRed.copy(alpha = 0.2f)),
            backgroundBrush = OmniBrushes.ErrorGlassBrush,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = labels.errorTitle,
                tint = ErrorRed,
                modifier = Modifier.size(48.dp),
              )
              Text(
                text = labels.errorTitle,
                style = MaterialTheme.typography.titleLarge,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
              )
              Text(
                text = verificationState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = GlassWhite,
                textAlign = TextAlign.Center,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = onClose,
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("btn_verification_error_close"),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Text(labels.closeButton, color = PureWhite, fontWeight = FontWeight.SemiBold)
                }
                Button(
                  onClick = onRetry,
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("btn_verification_error_retry"),
                  colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                  shape = RoundedCornerShape(14.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(labels.retryButton, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
      is UiState.Success -> {
        val resolved = verificationState.data
        if (resolved == null) {
          Box(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center,
          ) {
            Button(
              onClick = onClose,
              modifier = Modifier.minimumInteractiveComponentSize(),
              colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            ) {
              Text(labels.closeButton)
            }
          }
        } else {
          val church = resolved.church
          val member = resolved.member
          val isEnabledForThisChurch =
            verificationConfig.isEnabledForChurch(church.id, church.effectiveCode)

          if (!isEnabledForThisChurch) {
            Box(
              modifier = Modifier.fillMaxSize().padding(20.dp),
              contentAlignment = Alignment.Center,
            ) {
              GlassPanel(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                borderColors = listOf(BrandGold.copy(alpha = 0.7f), BrandOrange.copy(alpha = 0.3f)),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = labels.disabledTitle,
                    tint = BrandGold,
                    modifier = Modifier.size(48.dp),
                  )
                  Text(
                    text = labels.disabledTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                  )
                  Text(
                    text = labels.disabledBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GlassWhite,
                    textAlign = TextAlign.Center,
                  )
                  Button(
                    onClick = onClose,
                    modifier =
                      Modifier.fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("btn_verification_disabled_close"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(14.dp),
                  ) {
                    Text(labels.closeButton, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          } else {
            OfficialCertificateContent(
              context = context,
              church = church,
              member = member,
              verificationConfig = verificationConfig,
              isLicensedChurch = isChurchLicensed(church.id, church.effectiveCode),
              activeLang = activeLang,
              labels = labels,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VerificationTopBar(
  title: String,
  activeLang: String,
  onSelectLang: (String) -> Unit,
  onClose: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      IconButton(
        onClick = onClose,
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(MidnightNavy.copy(alpha = 0.7f))
            .testTag("btn_verification_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = PureWhite,
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "verify.omnichurch.download",
          style = MaterialTheme.typography.labelSmall,
          color = BlueNeon,
          maxLines = 1,
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      listOf("ht", "fr", "en", "es").forEach { code ->
        val selected = activeLang == code
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(
                if (selected) ElectricBlue else MidnightNavy.copy(alpha = 0.7f)
              )
              .border(
                width = 1.dp,
                color = if (selected) BlueNeon else GlassCardBorderSubtle,
                shape = RoundedCornerShape(50),
              )
              .clickable { onSelectLang(code) }
              .padding(horizontal = 9.dp, vertical = 6.dp)
              .testTag("btn_verification_lang_$code"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = code.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) PureWhite else MutedGlassText,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun OfficialCertificateContent(
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
            ChurchLogoAvatar(logoUrl = church.logoUrl, churchName = church.name)
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
                listOf(church.city, church.country)
                  .map { it.trim() }
                  .filter { it.isNotEmpty() }
                  .joinToString(", ")
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

            if (qrBitmap != null) {
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
                  bitmap = qrBitmap!!.asImageBitmap(),
                  contentDescription = "QR Verification URL",
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
          }
        }
      }

      // 6. Action Buttons (Copy Link, Share, Print/Save PDF)
      if (verificationConfig.allowPrintAndShare) {
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

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}

@Composable
private fun ChurchLogoAvatar(logoUrl: String?, churchName: String) {
  val clean = logoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val bitmap = remember(clean) { ImageCompressor.decodeBase64DataUrlToBitmap(clean) }
  var remoteFailed by remember(clean) { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.size(56.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(listOf(RoyalBlue, PrimaryNavy, DeepNavy)))
        .border(1.5.dp, BlueNeon.copy(alpha = 0.7f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    when {
      bitmap != null ->
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = churchName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      clean != null && !remoteFailed ->
        AsyncImage(
          model = clean,
          contentDescription = churchName,
          contentScale = ContentScale.Crop,
          onError = { remoteFailed = true },
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      else -> OmniChurch3DFlameIcon(size = 30.dp)
    }
  }
}

@Composable
private fun VerificationMemberPhoto(member: Member) {
  val photoUrl = member.effectivePhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val bitmap = remember(photoUrl) { ImageCompressor.decodeBase64DataUrlToBitmap(photoUrl) }
  var remoteFailed by remember(photoUrl) { mutableStateOf(false) }

  Box(
    modifier =
      Modifier.size(74.dp)
        .clip(CircleShape)
        .background(Brush.linearGradient(listOf(RoyalBlue, DeepNavy)))
        .border(2.dp, BlueNeon.copy(alpha = 0.8f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    when {
      bitmap != null ->
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = member.effectiveFullName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      photoUrl != null && !remoteFailed ->
        AsyncImage(
          model = photoUrl,
          contentDescription = member.effectiveFullName,
          contentScale = ContentScale.Crop,
          onError = { remoteFailed = true },
          modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
      else -> {
        val initials =
          member.effectiveFullName
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase(Locale.US) }
            .ifEmpty { "OC" }
        Text(
          text = initials,
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }
  }
}

@Composable
private fun AuthorityPillBadge(
  text: String,
  accentColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
  Row(
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(accentColor.copy(alpha = 0.16f))
        .border(1.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(50))
        .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = text,
      tint = accentColor,
      modifier = Modifier.size(14.dp),
    )
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun MetadataPairRow(
  leftLabel: String,
  leftValue: String,
  rightLabel: String,
  rightValue: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(leftLabel, style = MaterialTheme.typography.labelSmall, color = MutedGlassText)
      Text(
        text = leftValue,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(rightLabel, style = MaterialTheme.typography.labelSmall, color = MutedGlassText)
      Text(
        text = rightValue,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun MetadataSingleRow(label: String, value: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGlassText)
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      color = PureWhite,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun ContactInfoLine(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  value: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MutedGlassText,
      modifier = Modifier.size(16.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = GlassWhite,
    )
  }
}
