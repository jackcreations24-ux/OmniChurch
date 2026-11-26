package com.zoutiw.omnichurch.data.models

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest

@Immutable
@IgnoreExtraProperties
data class Member(
  @DocumentId val id: String = "",
  val memberId: String? = null,
  val userId: String = "",
  val churchId: String = "",
  val churchName: String = "",
  val churchLogoUrl: String? = null,
  val firstName: String = "",
  val lastName: String = "",
  val fullName: String = "",
  val memberCode: String = "",
  val phone: String? = null,
  val email: String? = null,
  val address: String = "",
  val gender: String = "other",
  val pin: String? = null,
  val securityPin: String? = null,
  val googleEmail: String? = null,
  val googleUid: String? = null,
  val googlePhoto: String? = null,
  val photoUrl: String? = null,
  val qrCode: String = "",
  val department: String = "",
  val role: String = "member",
  val memberType: String = "member",
  val status: String = "active",
  val barcodeToken: String = "",
  val nfcCardUid: String = "",
  val joinedAt: String = "",
  val joinDateFormatted: String? = null,
  val attendanceStreak: Int = 0,
  val totalAttendances: Int = 0,
  val titlePrefix: String? = null,
  val avatarUrl: String? = null,
  val emergencyContact: String? = null,
  val joinedDate: String? = null,
  val notes: String = "",
  val dateOfBirth: String? = null,
  val baptismDate: String? = null,
  val documentNumber: String? = null,
  val documentType: String? = null,
  val emergencyContactName: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val effectiveFullName: String
    get() = fullName.trim().ifEmpty { "$firstName $lastName".trim() }

  val effectiveMatrikil: String
    get() =
      (memberId ?: "").trim().ifEmpty {
        memberCode.trim().ifEmpty { id.trim() }
      }

  val effectiveMemberDbId: String
    get() = id.trim().ifEmpty { memberId ?: "" }

  val effectiveMemberCode: String
    get() = memberCode.trim().ifEmpty { memberId ?: id }

  val effectiveStatus: String
    get() = status.trim().ifEmpty { "active" }

  val isVisitorType: Boolean
    get() = memberType.trim().equals("visitor", ignoreCase = true)

  val isCrossChurchVisitor: Boolean
    get() = false

  val homeChurchName: String?
    get() = churchName.takeIf { it.isNotBlank() }

  val homeChurchId: String?
    get() = churchId.takeIf { it.isNotBlank() }

  val isFinishedForAbsence: Boolean
    get() = false

  val effectivePhotoUrl: String?
    get() =
      avatarUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: photoUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: googlePhoto?.trim()?.takeIf { it.isNotEmpty() }

  val effectiveBarcodeToken: String
    get() = barcodeToken.trim().ifEmpty { qrCode.trim().ifEmpty { effectiveMatrikil } }

  val canonicalBarcodeDisplay: String
    get() = effectiveBarcodeToken

  val isDeaconRole: Boolean
    get() = OmniRoleEvaluator.isActiveDeacon(this)

  val isReceptionRole: Boolean
    get() = OmniRoleEvaluator.isActiveReception(this)

  val isUsherRole: Boolean
    get() = OmniRoleEvaluator.isActiveReception(this) ||
      OmniRoleEvaluator.tokenizeRole(role).any { it in setOf("staff", "admin", "superadmin") }

  /**
   * Returns a lightweight copy containing ONLY public display name & avatar fields
   * with all sensitive PII, PINs, contact info, and legal identifiers stripped.
   */
  fun toPublicDisplayEntry(): Member =
    Member(
      id = id,
      churchId = churchId,
      churchName = churchName,
      firstName = firstName,
      lastName = lastName,
      fullName = effectiveFullName,
      titlePrefix = titlePrefix,
      avatarUrl = avatarUrl,
      photoUrl = photoUrl,
      googlePhoto = googlePhoto,
      department = department,
      role = role,
      memberType = memberType,
      status = status,
    )

  val effectiveJoinedDate: String
    get() =
      joinedDate?.trim()?.ifEmpty { null }
        ?: joinedAt.trim().ifEmpty { null }
        ?: joinDateFormatted?.trim()?.ifEmpty { null }
        ?: "Manm Ofisyèl"

  val verificationUrl: String
    get() =
      if (churchId.isNotBlank() && effectiveMatrikil.isNotBlank()) {
        "https://verify.omnichurch.download/?verify=1&church=$churchId&member=$effectiveMatrikil"
      } else {
        effectiveBarcodeToken
      }

  val internalTokenPayload: String
    get() =
      if (churchId.isNotBlank() && effectiveMatrikil.isNotBlank()) {
        "CHURCH:$churchId|MEMBER:$effectiveMatrikil"
      } else {
        effectiveBarcodeToken
      }

  val standardizedQrPayload: String
    get() = verificationUrl
}

/**
 * Centralized least-privilege RBAC role evaluator matching OmniChurch Web & Firestore Rules.
 * - Active Deacons are identified strictly by word-token matching on `member.role`
 *   (NEVER `department`) where `status == 'active'` and `memberType != 'visitor'`.
 * - Pure Reception (`Resepsyon`) remains scan-only unless also explicitly holding a Deacon role.
 */
object OmniRoleEvaluator {
  val DEACON_ROLE_TOKENS: Set<String> =
    setOf(
      "dyak",
      "dyakones",
      "diacre",
      "diaconesse",
      "diacono",
      "diaconisa",
      "deacon",
      "deaconess",
    )

  val RECEPTION_ROLE_TOKENS: Set<String> =
    setOf(
      "resepsyon",
      "recepcion",
      "reception",
      "usher",
      "akey",
      "accueil",
      "pwotokol",
      "protokol",
      "protocol",
      "protocolo",
      "hospitality",
      "bienvenida",
    )

  private val COMBINING_DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
  private val NON_LETTER_BOUNDARY = Regex("[^\\p{L}]+")

  fun normalizeRoleText(raw: String?): String {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return ""
    val decomposed = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFD)
    return COMBINING_DIACRITICS.replace(decomposed, "").lowercase(java.util.Locale.US)
  }

  fun tokenizeRole(raw: String?): Set<String> {
    val normalized = normalizeRoleText(raw)
    if (normalized.isEmpty()) return emptySet()
    return normalized
      .split(NON_LETTER_BOUNDARY)
      .filter { it.isNotBlank() }
      .toSet()
  }

  private fun isEligibleActiveNonVisitor(member: Member?): Boolean {
    if (member == null) return false
    if (!member.effectiveStatus.equals("active", ignoreCase = true)) return false
    if (member.isVisitorType) return false
    return true
  }

  fun isActiveDeacon(member: Member?): Boolean {
    if (!isEligibleActiveNonVisitor(member)) return false
    val tokens = tokenizeRole(member?.role)
    return tokens.any { it in DEACON_ROLE_TOKENS }
  }

  fun isActiveReception(member: Member?): Boolean {
    if (!isEligibleActiveNonVisitor(member)) return false
    val tokens = tokenizeRole(member?.role)
    return tokens.any { it in RECEPTION_ROLE_TOKENS }
  }

  fun isPureDeacon(member: Member?, isAdmin: Boolean): Boolean =
    !isAdmin && isActiveDeacon(member) && !isActiveReception(member)

  fun canScanAttendance(member: Member?, isAdmin: Boolean): Boolean =
    isAdmin || isActiveReception(member)

  fun canManageServices(member: Member?, isAdmin: Boolean): Boolean =
    isAdmin || isActiveDeacon(member)

  fun canPublishAnnouncements(member: Member?, isAdmin: Boolean): Boolean =
    isAdmin || isActiveDeacon(member)

  fun canDeleteService(
    service: ChurchService,
    isAdmin: Boolean,
    isDeacon: Boolean,
    currentAuthUid: String?,
  ): Boolean {
    if (isAdmin) return true
    if (!isDeacon || currentAuthUid.isNullOrBlank()) return false
    if (service.isPermanentlyLocked()) return false
    val uid = currentAuthUid.trim()
    return service.authorId.trim() == uid || service.createdBy.trim() == uid
  }

  fun canEditOrDeleteAnnouncement(
    announcement: Announcement,
    isAdmin: Boolean,
    isDeacon: Boolean,
    currentAuthUid: String?,
  ): Boolean {
    if (isAdmin) return true
    if (!isDeacon || currentAuthUid.isNullOrBlank()) return false
    return announcement.authorId.trim() == currentAuthUid.trim()
  }

  fun defaultDeaconAuthorName(member: Member): String {
    val cleanName = member.effectiveFullName.trim().ifBlank { "Dyak" }
    val normalized = normalizeRoleText(cleanName)
    return if (normalized.startsWith("dyak ") || normalized.startsWith("deacon ") || normalized.startsWith("diacre ")) {
      cleanName
    } else {
      "Dyak $cleanName"
    }
  }

  const val DEFAULT_DEACON_DEPARTMENT: String = "Dyakonat / Ministè"
}

@Immutable
data class ParsedQrResult(val churchId: String?, val memberId: String)

object OmniQrParser {
  fun parseQrToken(rawText: String): ParsedQrResult {
    val text = rawText.trim()
    if (text.isEmpty()) return ParsedQrResult(null, "")

    if (text.contains("verify", ignoreCase = true) || text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
      try {
        val uri = android.net.Uri.parse(text)
        val c = uri?.getQueryParameter("church") ?: uri?.getQueryParameter("c")
        val m = uri?.getQueryParameter("member") ?: uri?.getQueryParameter("m") ?: uri?.getQueryParameter("id")
        if (!m.isNullOrBlank()) {
          return ParsedQrResult(churchId = c?.trim(), memberId = m.trim())
        }
      } catch (_: Exception) {}

      val queryPart = text.substringAfter("?", "").substringBefore("#")
      if (queryPart.isNotEmpty()) {
        val params =
          queryPart
            .split("&")
            .mapNotNull { pair ->
              val idx = pair.indexOf('=')
              if (idx > 0) {
                val key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8").lowercase()
                val value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8").trim()
                key to value
              } else null
            }
            .toMap()
        val c = params["church"] ?: params["c"]
        val m = params["member"] ?: params["m"] ?: params["id"]
        if (!m.isNullOrBlank()) {
          return ParsedQrResult(churchId = c?.trim(), memberId = m.trim())
        }
      }
    }

    if (text.contains("CHURCH:", ignoreCase = true) && text.contains("MEMBER:", ignoreCase = true)) {
      val churchRegex = Regex("CHURCH:([^|]+)", RegexOption.IGNORE_CASE)
      val memberRegex = Regex("MEMBER:([^|]+)", RegexOption.IGNORE_CASE)
      val c = churchRegex.find(text)?.groupValues?.get(1)?.trim()
      val m = memberRegex.find(text)?.groupValues?.get(1)?.trim()
      if (!m.isNullOrBlank()) {
        return ParsedQrResult(churchId = c, memberId = m)
      }
    }

    return ParsedQrResult(churchId = null, memberId = text)
  }
}

fun buildCanonicalQrPayload(
  churchId: String,
  memberId: String,
  verificationBaseUrl: String = "https://verify.omnichurch.download/",
): String {
  val base = verificationBaseUrl.trimEnd('/')
  return "$base/?verify=1&church=${churchId.trim()}&member=${memberId.trim()}"
}

fun maskLegalDocumentNumber(raw: String?, shouldMask: Boolean): String {
  val clean = raw?.trim().orEmpty()
  if (clean.isEmpty()) return "—"
  if (!shouldMask || clean.length <= 4) return clean
  return "•".repeat(clean.length - 4) + clean.takeLast(4)
}

fun computeDeterministicVerificationSeal(
  churchId: String,
  memberDocId: String,
  memberId: String,
  isActive: Boolean,
): String {
  val payload = "${churchId.trim()}|${memberDocId.trim()}|${memberId.trim()}|$isActive"
  val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
  return "OC-" + digest.take(6).joinToString("") { "%02X".format(it) }
}
