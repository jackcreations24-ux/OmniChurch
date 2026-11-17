package com.zoutiw.omnichurch.core.utilities

import android.content.Context
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.buildCanonicalQrPayload
import com.zoutiw.omnichurch.data.models.computeDeterministicVerificationSeal
import com.zoutiw.omnichurch.data.models.maskLegalDocumentNumber
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VerificationPdfPrinter {
  private var activePrintWebView: WebView? = null

  fun printMemberCertificate(
    context: Context,
    church: Church,
    member: Member,
    qrBitmap: Bitmap?,
    isLicensedChurch: Boolean,
    verificationConfig: RemoteMemberVerificationConfig,
    languageCode: String = "ht",
  ) {
    val activity = context.findActivity() ?: context
    val printManager =
      activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        ?: run {
          Toast.makeText(context, "Sèvis enpresyon pa disponib sou aparèy sa a.", Toast.LENGTH_SHORT)
            .show()
          return
        }

    val html =
      buildCertificateHtml(
        church = church,
        member = member,
        qrBitmap = qrBitmap,
        isLicensedChurch = isLicensedChurch,
        config = verificationConfig,
        languageCode = languageCode,
      )

    val webView = WebView(activity)
    activePrintWebView = webView
    webView.settings.javaScriptEnabled = false
    webView.webViewClient =
      object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
          if (view == null) return
          val jobName = "OmniChurch_Certificate_${member.effectiveMatrikil.ifBlank { member.id }}"
          val adapter = view.createPrintDocumentAdapter(jobName)
          printManager.print(
            jobName,
            adapter,
            PrintAttributes.Builder()
              .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
              .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
              .build(),
          )
          activePrintWebView = null
        }
      }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
  }

  private fun bitmapToDataUri(bitmap: Bitmap?): String {
    if (bitmap == null) return ""
    return try {
      val stream = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      val bytes = stream.toByteArray()
      "data:image/png;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
    } catch (_: Exception) {
      ""
    }
  }

  private fun escapeHtml(raw: String?): String {
    if (raw.isNullOrEmpty()) return ""
    return raw
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }

  private fun buildCertificateHtml(
    church: Church,
    member: Member,
    qrBitmap: Bitmap?,
    isLicensedChurch: Boolean,
    config: RemoteMemberVerificationConfig,
    languageCode: String,
  ): String {
    val isActive =
      member.status.trim().lowercase(Locale.US) !in setOf("suspended", "inactive")
    val seal =
      computeDeterministicVerificationSeal(
        churchId = church.id.ifBlank { member.churchId },
        memberDocId = member.id,
        memberId = member.effectiveMatrikil,
        isActive = isActive,
      )
    val qrUri = bitmapToDataUri(qrBitmap)
    val verifyUrl =
      buildCanonicalQrPayload(
        churchId = church.id.ifBlank { member.churchId },
        memberId = member.effectiveMatrikil,
        verificationBaseUrl = config.effectiveVerificationBaseUrl,
      )
    val generatedAt =
      SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    val title =
      when (languageCode) {
        "fr" -> "CERTIFICAT OFFICIEL DE VÉRIFICATION DE MEMBRE"
        "en" -> "OFFICIAL MEMBER VERIFICATION CERTIFICATE"
        "es" -> "CERTIFICADO OFICIAL DE VERIFICACIÓN DE MIEMBRO"
        else -> "SÈTIFIKA OFISYÈL VERIFIKASYON MANM"
      }
    val statusBadge =
      when {
        isActive && languageCode == "fr" -> "MEMBRE ACTIF & VÉRIFIÉ"
        isActive && languageCode == "en" -> "ACTIVE & VERIFIED MEMBER"
        isActive && languageCode == "es" -> "MIEMBRO ACTIVO Y VERIFICADO"
        isActive -> "MANM AKTIF & VERIFYE"
        languageCode == "fr" -> "STATUT INACTIF / SUSPENDU"
        languageCode == "en" -> "INACTIVE / SUSPENDED STATUS"
        languageCode == "es" -> "ESTADO INACTIVO / SUSPENDIDO"
        else -> "STATI INAKTIF / SISPAN"
      }

    val maskedDoc =
      if (config.showLegalDocuments && !member.documentNumber.isNullOrBlank()) {
        val docLabel = member.documentType?.takeIf { it.isNotBlank() } ?: "CIN/ID"
        "$docLabel: ${maskLegalDocumentNumber(member.documentNumber, config.maskLegalDocumentNumber)}"
      } else {
        ""
      }

    val rncHtml =
      if (config.showChurchRncBadge && church.rnc.isNotBlank()) {
        "<span class='badge gold'>RNC: ${escapeHtml(church.rnc)}${if (church.rncVerified) " ✓" else ""}</span>"
      } else ""

    val licenseHtml =
      if (isLicensedChurch) {
        "<span class='badge green'>Official Licensed Church ✓</span>"
      } else ""

    val photoHtml =
      if (config.showMemberPhoto && !member.effectivePhotoUrl.isNullOrBlank()) {
        "<img src='${escapeHtml(member.effectivePhotoUrl)}' class='avatar' />"
      } else ""

    val signatureHtml =
      if (!church.signatureUrl.isNullOrBlank()) {
        "<img src='${escapeHtml(church.signatureUrl)}' class='sig' />"
      } else {
        "<div class='sig-line'></div>"
      }

    val noticeHtml =
      if (config.customAuthorityNotice.isNotBlank()) {
        "<p class='notice'>${escapeHtml(config.customAuthorityNotice)}</p>"
      } else ""

    val contactHtml =
      if (config.showChurchContactInfo) {
        """
        <div class='section'>
          <div><b>Church Address:</b> ${escapeHtml(church.address.ifBlank { church.city })}</div>
          <div><b>Phone:</b> ${escapeHtml(church.phone)} &nbsp;|&nbsp; <b>Email:</b> ${escapeHtml(church.email)}</div>
          ${if (church.website.isNotBlank()) "<div><b>Website:</b> ${escapeHtml(church.website)}</div>" else ""}
        </div>
        """.trimIndent()
      } else ""

    return """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset='utf-8'/>
        <style>
          body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color: #0b1b3a; margin: 28px; background: #fff; }
          .frame { border: 2px solid #0b2454; border-radius: 14px; padding: 24px; }
          .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #e2e8f0; padding-bottom: 14px; }
          .church { font-size: 20px; font-weight: 800; color: #07152f; margin: 0; }
          .subtitle { font-size: 12px; font-weight: 700; color: #1d4ed8; letter-spacing: 0.8px; margin-top: 4px; }
          .badges { margin-top: 10px; }
          .badge { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; margin-right: 6px; }
          .badge.green { background: #dcfce7; color: #166534; border: 1px solid #86efac; }
          .badge.red { background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; }
          .badge.gold { background: #fef3c7; color: #92400e; border: 1px solid #fcd34d; }
          .profile { display: flex; align-items: center; gap: 18px; margin-top: 18px; padding: 14px; background: #f8fafc; border-radius: 10px; border: 1px solid #e2e8f0; }
          .avatar { width: 82px; height: 82px; border-radius: 50%; object-fit: cover; border: 2px solid #1d4ed8; }
          .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 18px; margin-top: 14px; font-size: 12.5px; }
          .section { margin-top: 14px; padding-top: 10px; border-top: 1px solid #e2e8f0; font-size: 12px; }
          .notice { background: #eff6ff; border-left: 3px solid #2563eb; padding: 8px 12px; font-size: 11.5px; margin-top: 12px; }
          .footer { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 22px; padding-top: 14px; border-top: 1px solid #cbd5e1; }
          .qr { width: 110px; height: 110px; }
          .sig { height: 42px; object-fit: contain; }
          .sig-line { width: 160px; border-bottom: 1px solid #334155; height: 28px; }
          .mono { font-family: monospace; font-size: 11px; color: #1e293b; }
        </style>
      </head>
      <body>
        <div class='frame'>
          <div class='header'>
            <div>
              <h1 class='church'>${escapeHtml(church.name.ifBlank { "OmniChurch" })}</h1>
              <div class='subtitle'>$title</div>
              <div class='badges'>
                <span class='badge ${if (isActive) "green" else "red"}'>$statusBadge</span>
                $licenseHtml
                $rncHtml
              </div>
            </div>
            ${if (qrUri.isNotEmpty()) "<img src='$qrUri' class='qr' />" else ""}
          </div>

          <div class='profile'>
            $photoHtml
            <div>
              <div style='font-size:18px;font-weight:800;'>${escapeHtml((member.titlePrefix?.takeIf { it.isNotBlank() }?.let { "$it " } ?: "") + member.effectiveFullName)}</div>
              <div class='mono' style='margin-top:4px;'>ID: ${escapeHtml(member.effectiveMatrikil)} &nbsp;|&nbsp; ${escapeHtml(member.canonicalBarcodeDisplay)}</div>
              <div style='font-size:12px;color:#475569;margin-top:4px;'>${escapeHtml(member.department)} • ${escapeHtml(member.role)}</div>
            </div>
          </div>

          <div class='grid'>
            <div><b>Joined Date:</b> ${escapeHtml(member.effectiveJoinedDate)}</div>
            <div><b>Pastor:</b> ${escapeHtml(church.pastor.ifBlank { "Direksyon Legliz" })}</div>
            ${if (!member.dateOfBirth.isNullOrBlank()) "<div><b>Date of Birth:</b> ${escapeHtml(member.dateOfBirth)}</div>" else ""}
            ${if (!member.baptismDate.isNullOrBlank()) "<div><b>Baptism Date:</b> ${escapeHtml(member.baptismDate)}</div>" else ""}
            ${if (maskedDoc.isNotEmpty()) "<div><b>Legal Document:</b> ${escapeHtml(maskedDoc)}</div>" else ""}
            ${if (!member.emergencyContactName.isNullOrBlank() || !member.emergencyContact.isNullOrBlank()) "<div><b>Emergency:</b> ${escapeHtml(listOfNotNull(member.emergencyContactName, member.emergencyContact).joinToString(" • "))}</div>" else ""}
          </div>

          $contactHtml
          $noticeHtml

          <div class='footer'>
            <div>
              $signatureHtml
              <div style='font-size:11px;color:#475569;margin-top:4px;'>Authorized Church Direction • ${escapeHtml(church.pastor.ifBlank { church.name })}</div>
              <div class='mono' style='margin-top:6px;'>Seal: $seal</div>
              <div class='mono'>Verified URL: ${escapeHtml(verifyUrl)}</div>
              <div class='mono'>Issued: $generatedAt</div>
            </div>
          </div>
        </div>
      </body>
      </html>
    """.trimIndent()
  }
}
