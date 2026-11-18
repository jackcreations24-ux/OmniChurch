package com.zoutiw.omnichurch.data.models

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val displayName: String = "",
  val photoUrl: String? = null,
  val churchId: String? = null,
  val churchName: String? = null,
  val role: String = "member",
  val memberId: String? = null,
  val language: String? = "ht",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isAdminOrSuperAdmin: Boolean
    get() = role == "admin" || role == "super_admin"

  val isAuthorizedScanner: Boolean
    get() {
      val normalized = role.trim().lowercase().let {
        java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
          .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
      }
      return normalized in listOf("admin", "usher", "super_admin", "staff", "recepcion", "resepsyon", "reception", "akey", "accueil", "pwotokol", "protokol", "protocol", "protocolo", "hospitality", "bienvenida")
    }
}

@IgnoreExtraProperties
data class Church(
  @DocumentId val id: String = "",
  val code: String = "",
  val name: String = "",
  val pastor: String = "",
  val address: String = "",
  val city: String = "Ayiti",
  val country: String = "Ayiti",
  val phone: String = "",
  val email: String = "",
  val motto: String = "",
  val adminPasscode: String = "",
  val adminGoogleEmail: String = "",
  val themeColor: String = "indigo",
  val logoIcon: String = "Church",
  val currency: String = "HTG",
  val status: String = "active",
  val memberCount: Int = 0,
  val createdAt: String = "",
  val ownerId: String = "",
  val logoUrl: String? = null,
  val bannerUrl: String? = null,
  val tagline: String = "",
  val updatedAt: String = "",
  val adminGoogleUid: String? = null,
  val adminGoogleName: String? = null,
  val adminGooglePhoto: String? = null,
  val adminAuthMethod: String? = null,
  @JvmField @field:PropertyName("quickPinEnabled") val quickPinEnabled: Boolean = false,
  val autoLockMinutes: Int = 5,
  val adminPasscodeUpdatedAt: String? = null,
  val rnc: String = "",
  val rncVerified: Boolean = false,
  val website: String = "",
  val signatureUrl: String? = null,
) {
  val effectiveCode: String
    get() = code.trim().ifEmpty { id.trim() }

  val isLegacyPinOnly: Boolean
    get() = ownerId.isBlank() && adminGoogleEmail.isBlank() && adminPasscode.isNotBlank()

  val hasCloudOwner: Boolean
    get() = ownerId.isNotBlank() || adminGoogleEmail.isNotBlank() || adminGoogleUid?.isNotBlank() == true

  val effectiveAutoLockMinutes: Int
    get() = if (autoLockMinutes <= 0) 5 else autoLockMinutes

  val effectiveQuickPinEnabled: Boolean
    get() = quickPinEnabled && adminPasscode.isNotBlank()
}

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

  val isUsherRole: Boolean
    get() = role.trim().lowercase() in listOf("usher", "akèy", "accueil", "protocol", "staff", "admin", "super_admin")

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

@IgnoreExtraProperties
data class ChurchService(
  @DocumentId val id: String = "",
  val churchId: String = "",
  val churchName: String = "",
  val category: String = "dimanche_matin",
  val title: String = "",
  val name: String = "",
  val description: String? = null,
  val imageUrl: String? = null,
  val dateFormatted: String = "",
  val date: String = "",
  val startTime: String = "",
  val endTime: String = "",
  val theme: String? = null,
  val passage: String? = null,
  val leader: String? = null,
  val preacher: String? = null,
  val reader: String? = null,
  val musicians: String? = null,
  val ushers: String? = null,
  val sendReminder24h: Boolean = false,
  val type: String = "dimanche_matin",
  val status: String = "active",
  val startTimeEpochMillis: Long = 0L,
  val lateToleranceMinutes: Long = 15L,
  @JvmField @field:PropertyName("isOpenForAttendance") val isOpenForAttendance: Boolean = false,
  @JvmField @field:PropertyName("isLive") val isLive: Boolean = false,
  val authorId: String = "",
  val authorName: String = "",
  val reactionsCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val effectiveTitle: String
    get() = title.ifBlank { name }

  val effectiveDateOnly: String
    get() = date.ifBlank { dateFormatted }

  val effectiveImageUrl: String?
    get() = imageUrl?.trim()?.takeIf { it.isNotEmpty() }

  fun isPermanentlyLocked(now: Long = System.currentTimeMillis()): Boolean = status.equals("locked", ignoreCase = true) || status.equals("archived", ignoreCase = true)

  fun isLiveBadgeActive(now: Long = System.currentTimeMillis()): Boolean = isLive || status.equals("live", ignoreCase = true)

  fun isTimerTicking(now: Long = System.currentTimeMillis()): Boolean = isLive || isOpenForAttendance

  fun isBeforeStart(now: Long = System.currentTimeMillis()): Boolean = startTimeEpochMillis > 0L && now < startTimeEpochMillis

  fun isServiceEnded(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (status.equals("ended", ignoreCase = true) || status.equals("closed", ignoreCase = true)) {
      return true
    }
    return startTimeEpochMillis > 0L && nowMillis > (startTimeEpochMillis + 4 * 3_600_000L)
  }

  fun isFinishedForAbsence(now: Long = System.currentTimeMillis()): Boolean {
    return isPermanentlyLocked(now) || now > (startTimeEpochMillis + 3_600_000L * 3)
  }

  val elapsedTimerLabel: String
    get() = if (isLiveBadgeActive()) "LIVE KONTINYE" else if (isTimerTicking()) "LÈ POU ANREJISTRE" else dateFormatted.ifBlank { "Sèvis" }
  fun elapsedTimerLabel(now: Long = System.currentTimeMillis()): String = elapsedTimerLabel

  val effectiveScheduleLabel: String
    get() = "$dateFormatted • $startTime".trim(' ', '•')
  fun effectiveScheduleLabel(now: Long = System.currentTimeMillis()): String = effectiveScheduleLabel

  val categoryLabelHt: String
    get() = when (category.trim().lowercase()) {
      "dimanche_matin", "sunday_morning", "dimanch maten" -> "Dimanch Maten"
      "jeudi_soir", "thursday_night", "jedi swè" -> "Jedi Swè"
      "jeunesse", "youth", "jennès" -> "Jèn yo"
      "priere", "prayer", "lapriyè" -> "Reinyon Lapriyè"
      "jeune", "fasting", "jen" -> "Jèn ak Lapriyè"
      else -> category.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
    }
}

@IgnoreExtraProperties
data class Announcement(
  @DocumentId val id: String = "",
  val churchId: String = "",
  val title: String = "",
  val content: String = "",
  val category: String = "anons",
  val department: String = "",
  val date: String? = null,
  val targetAudience: String = "Tout moun",
  @JvmField @field:PropertyName("isPinned") val isPinned: Boolean = false,
  @JvmField @field:PropertyName("showToastOnLogin") val showToastOnLogin: Boolean = false,
  val toastAlertLevel: String = "info",
  @JvmField @field:PropertyName("isScheduledNotification") val isScheduledNotification: Boolean = false,
  val scheduledNotificationDate: String? = null,
  val posterUrl: String? = null,
  val imageUrl: String? = null,
  val serviceId: String? = null,
  val authorId: String = "",
  val authorName: String = "",
  val reactionsCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val effectiveImageUrl: String?
    get() = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: posterUrl?.trim()?.takeIf { it.isNotEmpty() }
}

@IgnoreExtraProperties
data class AnnouncementReaction(
  @DocumentId val id: String = "",
  val targetId: String = "",
  val churchId: String = "",
  val userId: String = "",
  val userName: String = "",
  val reactionType: String = "pray",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class OmniReactionType(val key: String, val labelHt: String) {
  LIKE("like", "J'aime"),
  LOVE("love", "Lanmou"),
  PRAY("pray", "Amèn"),
  HAHA("haha", "Haha"),
  WOW("wow", "Waw"),
  SAD("sad", "Tris"),
  CLAP("clap", "Bravo");

  companion object {
    fun fromKey(raw: String?): OmniReactionType {
      return when (raw?.trim()?.lowercase()) {
        "like" -> LIKE
        "love" -> LOVE
        "pray", "amen" -> PRAY
        "haha" -> HAHA
        "wow" -> WOW
        "sad" -> SAD
        "clap" -> CLAP
        else -> PRAY
      }
    }
  }
}

data class TargetReactionSummary(
  val totalCount: Int = 0,
  val topReactions: List<OmniReactionType> = emptyList(),
  val currentUserReaction: OmniReactionType? = null,
)

@IgnoreExtraProperties
data class AnnouncementComment(
  @DocumentId val id: String = "",
  val targetId: String = "",
  val churchId: String = "",
  val userId: String = "",
  val userName: String = "",
  val userPhotoUrl: String? = null,
  val content: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AttendanceRecord(
  @DocumentId val id: String = "",
  val churchId: String = "",
  val serviceId: String = "",
  val serviceName: String = "",
  val memberId: String = "",
  val memberDocId: String = "",
  val memberCode: String = "",
  val memberName: String = "",
  val memberPhotoUrl: String? = null,
  val userId: String = "",
  val recordedByUid: String = "",
  val status: String = "on_time",
  val checkInMethod: String = "qr_scanner",
  val barcodeToken: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  val effectiveStatus: String
    get() = status.trim().ifEmpty { "on_time" }

  val isCrossChurchVisitor: Boolean
    get() = false

  val effectiveMemberDbId: String
    get() = memberDocId.trim().ifEmpty { memberId.trim().ifEmpty { id } }

  val effectiveMemberCode: String
    get() = memberCode.trim().ifEmpty { memberId }

  val homeChurchName: String?
    get() = null

  val homeChurchId: String?
    get() = null
}

@IgnoreExtraProperties
data class ChurchNotification(
  @DocumentId val id: String = "",
  val userId: String = "",
  val churchId: String = "",
  val title: String = "",
  val message: String = "",
  val type: String = "system",
  @JvmField @field:PropertyName("isRead") val isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class OmniSessionState(
  val isInitialized: Boolean = false,
  val activeChurchId: String = "",
  val activeRole: String = "member",
  val activeMemberId: String = "",
  val activeMemberDocId: String = "",
  val biometricEnabled: Boolean = false,
  val biometricChurchId: String = "",
  val biometricMemberId: String = "",
  val biometricMemberDocId: String = "",
  val biometricMemberName: String = "",
) {
  val isAuthenticated: Boolean
    get() =
      activeChurchId.trim().isNotEmpty() &&
        (activeRole == "admin" ||
          activeMemberDocId.trim().isNotEmpty() ||
          activeMemberId.trim().isNotEmpty())

  val isAdminSession: Boolean
    get() = isAuthenticated && activeRole == "admin"

  val isMemberSession: Boolean
    get() = isAuthenticated && activeRole != "admin"
}

data class AuthenticatedMemberSession(
  val church: Church,
  val member: Member,
)

class OmniAuthException(override val message: String) : Exception(message)

object OmniAuthEvaluator {
  const val SUPER_ADMIN_EMAIL = "charlesjj3186@gmail.com"
  const val ERROR_SUPER_DEV_ISOLATION = "Kont sa a se yon kont devlopè sipè. Li pa ka kreye oswa modifye nan yon kontni regilye."
  const val ERROR_EMPTY_ID = "Tanpri antre ID Manm (Matrikil) ou oswa nimewo telefòn ou."
  const val ERROR_EMPTY_PIN =
    "Tanpri antre Kòd Sekirite (PIN) administrasyon legliz la ba ou a."
  const val ERROR_WRONG_PIN =
    "Kòd PIN sekirite sa a pa kòrèk pou manm sa a. Tanpri verifye kòd sekrè administratè a te ba ou a."
  const val ERROR_UNAUTHORIZED_ADMIN_GOOGLE =
    "Kont Google sa a pa nan lis administratè ki otorize pou legliz sa a."
  const val ERROR_NO_CHURCH_SELECTED =
    "Tanpri chwazi legliz ou a oswa antre yon ID Matrikil ki gen kòd legliz la (egzanp: CH001-JD4827)."
  const val ERROR_WRONG_ADMIN_PASSCODE =
    "Kòd Sekirite Administratè a pa kòrèk pou legliz sa a."
  const val ERROR_EMPTY_EMAIL = "Tanpri antre adrès imèl legliz la oswa imèl administratè a."
  const val ERROR_INVALID_EMAIL = "Adrès imèl sa a pa valid. Tanpri verifye fòma imèl la."
  const val ERROR_EMPTY_PASSWORD = "Tanpri antre modpas kont legliz la."

  fun memberNotFoundMessage(idOrPhone: String, churchName: String): String =
    "Pa gen okenn manm ki anrejistre avèk ID/Telefòn \"${idOrPhone.trim()}\" nan legliz $churchName."

  fun resolveChurchForMemberLogin(
    churches: List<Church>,
    selectedChurchId: String?,
    idOrPhoneInput: String?,
  ): Church? {
    val cleanSelected = (selectedChurchId ?: "").trim()
    val cleanInput = (idOrPhoneInput ?: "").trim()

    if (cleanSelected.isNotEmpty()) {
      val selectedUpper = cleanSelected.uppercase()
      val bySelected =
        churches.firstOrNull { church ->
          church.id.trim().uppercase() == selectedUpper ||
            (church.code ?: "").trim().uppercase() == selectedUpper
        }
      if (bySelected != null) {
        if (cleanInput.contains("-")) {
          val prefix = cleanInput.substringBefore("-").trim().uppercase()
          if (prefix.isNotEmpty()) {
            val byPrefix =
              churches.firstOrNull { church ->
                church.id.trim().uppercase() == prefix ||
                  (church.code ?: "").trim().uppercase() == prefix
              }
            if (byPrefix != null) return byPrefix
          }
        }
        return bySelected
      }
    }

    if (cleanInput.contains("-")) {
      val prefix = cleanInput.substringBefore("-").trim().uppercase()
      if (prefix.isNotEmpty()) {
        val byPrefix =
          churches.firstOrNull { church ->
            church.id.trim().uppercase() == prefix ||
              (church.code ?: "").trim().uppercase() == prefix
          }
        if (byPrefix != null) return byPrefix
      }
    }

    return null
  }

  fun doesMemberBelongToChurch(member: Member, church: Church): Boolean {
    val memChurchUpper = member.churchId.trim().uppercase()
    if (memChurchUpper.isEmpty()) return false
    val churchIdUpper = church.id.trim().uppercase()
    val churchCodeUpper = (church.code ?: "").trim().uppercase()
    return memChurchUpper == churchIdUpper ||
      (churchCodeUpper.isNotEmpty() && memChurchUpper == churchCodeUpper)
  }

  fun authenticateMemberFromList(
    church: Church?,
    churchMembers: List<Member>,
    idOrPhone: String?,
    enteredPin: String?,
  ): Result<AuthenticatedMemberSession> {
    val cleanInput = (idOrPhone ?: "").trim()
    val cleanPin = (enteredPin ?: "").trim()

    if (cleanInput.isEmpty()) {
      return Result.failure(OmniAuthException(ERROR_EMPTY_ID))
    }
    if (cleanPin.isEmpty()) {
      return Result.failure(OmniAuthException(ERROR_EMPTY_PIN))
    }
    if (church == null) {
      return Result.failure(OmniAuthException(ERROR_NO_CHURCH_SELECTED))
    }

    val cleanUpper = cleanInput.uppercase()
    val inputDigits = cleanInput.replace(Regex("[^0-9]"), "")

    val matchedMember =
      churchMembers
        .filter { doesMemberBelongToChurch(it, church) }
        .firstOrNull { member ->
          val memMatrikilUpper = (member.memberId ?: "").trim().uppercase()
          val memCodeUpper = member.memberCode.trim().uppercase()
          val memDocIdUpper = member.id.trim().uppercase()
          val memPhoneDigits = (member.phone ?: "").replace(Regex("[^0-9]"), "")
          val memEmailUpper = (member.email ?: "").trim().uppercase()

          val isMatrikilMatch =
            (memMatrikilUpper.isNotEmpty() && memMatrikilUpper == cleanUpper) ||
              (memCodeUpper.isNotEmpty() && memCodeUpper == cleanUpper)
          val isInternalIdMatch = memDocIdUpper.isNotEmpty() && memDocIdUpper == cleanUpper
          val isPhoneMatch =
            inputDigits.length >= 7 && memPhoneDigits.length >= 7 && memPhoneDigits == inputDigits
          val isEmailMatch = memEmailUpper.isNotEmpty() && memEmailUpper == cleanUpper

          isMatrikilMatch || isInternalIdMatch || isPhoneMatch || isEmailMatch
        }

    if (matchedMember == null) {
      return Result.failure(
        OmniAuthException(memberNotFoundMessage(cleanInput, church.name.ifBlank { church.id }))
      )
    }

    if (!verifyMemberPin(matchedMember, cleanPin)) {
      return Result.failure(OmniAuthException(ERROR_WRONG_PIN))
    }

    return Result.success(AuthenticatedMemberSession(church = church, member = matchedMember))
  }

  fun verifyMemberPin(member: Member, enteredPin: String?): Boolean {
    val cleanEntered = (enteredPin ?: "").trim()
    if (cleanEntered.isEmpty()) return false

    val storedPin =
      member.pin?.trim()?.takeIf { it.isNotEmpty() }
        ?: member.securityPin?.trim()?.takeIf { it.isNotEmpty() }

    return if (storedPin != null) {
      cleanEntered == storedPin
    } else {
      val sourceId =
        (member.memberId ?: "").trim().ifEmpty { member.memberCode.trim() }
      val fallbackPin = sourceId.takeLast(4).ifEmpty { "1234" }
      cleanEntered == fallbackPin
    }
  }

  fun computeSaltedAdminPasscodeDigest(salt: String, enteredPasscode: String): String {
    val cleanSalt = salt.trim().lowercase()
    val cleanPasscode = enteredPasscode.trim()
    val payload = "omnichurch:$cleanSalt:$cleanPasscode:security_entropy_2026"
    val digestBytes =
      MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    val hex = digestBytes.joinToString("") { "%02x".format(it) }
    return "sha256:v1:$hex"
  }

  fun verifyAdminPasscode(church: Church, enteredPasscode: String?): Boolean {
    val storedPasscode = (church.adminPasscode ?: "").trim()
    val cleanEntered = (enteredPasscode ?: "").trim()
    if (storedPasscode.isEmpty() || cleanEntered.isEmpty()) return false

    return if (storedPasscode.startsWith("sha256:v1:")) {
      val digestById = computeSaltedAdminPasscodeDigest(church.id, cleanEntered)
      if (storedPasscode.equals(digestById, ignoreCase = true)) {
        true
      } else {
        val codeSalt = (church.code ?: "").trim()
        if (codeSalt.isNotEmpty()) {
          val digestByCode = computeSaltedAdminPasscodeDigest(codeSalt, cleanEntered)
          storedPasscode.equals(digestByCode, ignoreCase = true)
        } else {
          false
        }
      }
    } else {
      storedPasscode == cleanEntered
    }
  }

  fun isGoogleEmailAuthorizedForAdmin(
    church: Church,
    googleEmail: String?,
    googleUid: String? = null,
  ): Boolean {
    val cleanEmail = (googleEmail ?: "").trim().lowercase()
    if (cleanEmail.isEmpty()) return false
    if (cleanEmail == SUPER_ADMIN_EMAIL) return true

    val configuredAdminEmail = (church.adminGoogleEmail ?: "").trim().lowercase()
    if (configuredAdminEmail.isNotEmpty()) {
      return cleanEmail == configuredAdminEmail
    }

    val ownerUid = church.ownerId.trim()
    return !googleUid.isNullOrBlank() && ownerUid.isNotEmpty() && ownerUid == googleUid.trim()
  }

  fun isChurchEmailAdminAuthorized(
    church: Church,
    email: String?,
    uid: String? = null,
  ): Boolean {
    val cleanEmail = (email ?: "").trim().lowercase()
    val cleanUid = (uid ?: "").trim()
    if (cleanEmail.isEmpty() && cleanUid.isEmpty()) return false
    if (cleanEmail == SUPER_ADMIN_EMAIL) return true

    val churchEmail = church.email.trim().lowercase()
    val adminGoogleEmail = church.adminGoogleEmail.trim().lowercase()
    val ownerUid = church.ownerId.trim()
    val adminGoogleUid = (church.adminGoogleUid ?: "").trim()

    return (cleanEmail.isNotEmpty() &&
      ((churchEmail.isNotEmpty() && churchEmail == cleanEmail) ||
        (adminGoogleEmail.isNotEmpty() && adminGoogleEmail == cleanEmail))) ||
      (cleanUid.isNotEmpty() &&
        ((ownerUid.isNotEmpty() && ownerUid == cleanUid) ||
          (adminGoogleUid.isNotEmpty() && adminGoogleUid == cleanUid)))
  }

  fun isChurchAdminAuthorized(church: Church, email: String?, uid: String?): Boolean =
    isChurchEmailAdminAuthorized(church, email, uid)

  fun hasMatchingChurchCloudOwner(church: Church, user: FirebaseUser?): Boolean {
    val uid = user?.uid?.trim() ?: return false
    val email = user.email?.trim()?.lowercase() ?: ""
    val ownerUid = church.ownerId.trim()
    val adminGoogleUid = (church.adminGoogleUid ?: "").trim()
    val adminGoogleEmail = church.adminGoogleEmail.trim().lowercase()
    return (ownerUid.isNotEmpty() && ownerUid == uid) ||
      (adminGoogleUid.isNotEmpty() && adminGoogleUid == uid) ||
      (adminGoogleEmail.isNotEmpty() && adminGoogleEmail == email) ||
      CryptoSecurity.isSuperDeveloperAccount(email)
  }

  fun resolveChurchForEmailPasswordLogin(
    churches: List<Church>,
    email: String?,
    uid: String?,
    selectedChurchId: String? = null,
    profileChurchId: String? = null,
    profileRole: String? = null,
  ): Result<Church> {
    val cleanEmail = (email ?: "").trim().lowercase()
    val cleanUid = (uid ?: "").trim()
    val cleanSelected = (selectedChurchId ?: "").trim()
    val cleanProfileChurchId = (profileChurchId ?: "").trim()
    val isProfileAdmin =
      profileRole?.trim()?.lowercase() in listOf("admin", "super_admin")

    fun matchesUserProfile(church: Church): Boolean {
      if (!isProfileAdmin || cleanProfileChurchId.isEmpty()) return false
      return church.id.trim().equals(cleanProfileChurchId, ignoreCase = true) ||
        church.code.trim().equals(cleanProfileChurchId, ignoreCase = true)
    }

    fun isAuthorizedForChurch(church: Church): Boolean {
      return isChurchEmailAdminAuthorized(church, cleanEmail, cleanUid) ||
        matchesUserProfile(church)
    }

    if (cleanSelected.isNotEmpty()) {
      val selectedChurch = resolveChurchForMemberLogin(churches, cleanSelected, null)
      if (selectedChurch != null && isAuthorizedForChurch(selectedChurch)) {
        return Result.success(selectedChurch)
      }
    }

    val directMatch =
      churches.firstOrNull { church ->
        (cleanEmail.isNotEmpty() &&
          (church.email.trim().equals(cleanEmail, ignoreCase = true) ||
            church.adminGoogleEmail.trim().equals(cleanEmail, ignoreCase = true))) ||
          (cleanUid.isNotEmpty() &&
            (church.ownerId.trim() == cleanUid ||
              church.adminGoogleUid?.trim() == cleanUid))
      }
    if (directMatch != null) {
      return Result.success(directMatch)
    }

    val profileMatch = churches.firstOrNull { matchesUserProfile(it) }
    if (profileMatch != null) {
      return Result.success(profileMatch)
    }

    if (cleanEmail == SUPER_ADMIN_EMAIL && churches.isNotEmpty()) {
      return Result.success(churches.first())
    }

    return Result.failure(
      OmniAuthException(
        "Okenn legliz pa jwenn oswa pa otorize pou imèl \"$cleanEmail\". Verifye imèl admin legliz la."
      )
    )
  }
}

sealed interface ScannerValidationOutcome {
  data object Idle : ScannerValidationOutcome

  data object Processing : ScannerValidationOutcome

  data class RegisteredSuccess(
    val record: AttendanceRecord,
    val member: Member,
    val service: ChurchService,
    val formattedTime: String,
  ) : ScannerValidationOutcome

  data class ValidationError(val errorType: AttendanceErrorType, val details: String? = null) :
    ScannerValidationOutcome
}

enum class AttendanceErrorType {
  INVALID_QR,
  WRONG_CHURCH,
  DUPLICATE_ATTENDANCE,
  SERVICE_CLOSED,
  UNAUTHORIZED,
  NETWORK_ERROR,
}

fun filterNonNullMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
  val result = mutableMapOf<String, Any>()
  for ((k, v) in pairs) {
    if (v != null) {
      result[k] = v
    }
  }
  return result
}

data class PublicVerificationResolution(
  val church: Church,
  val member: Member,
)

fun ChurchService.isServiceEnded(nowMillis: Long = System.currentTimeMillis()): Boolean {
  if (status.equals("ended", ignoreCase = true) || status.equals("closed", ignoreCase = true)) {
    return true
  }
  return startTimeEpochMillis > 0L && nowMillis > (startTimeEpochMillis + 4 * 3_600_000L)
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

fun formatCreoleDateWithDay(isoDate: String): String =
  isoDate.trim().ifBlank { "Dat Sèvis" }

enum class MemberAttendanceStatus {
  PRESENT, LATE, ABSENT, EXCUSED;

  val labelHt: String
    get() = when (this) {
      PRESENT -> "Prezan"
      LATE -> "An Retè"
      ABSENT -> "Absan"
      EXCUSED -> "Ekskize"
    }
}

data class MemberAttendanceHistoryItem(
  val id: String = "",
  val dateLabel: String = "",
  val timeLabel: String = "",
  val serviceTitle: String = "",
  val status: MemberAttendanceStatus = MemberAttendanceStatus.PRESENT,
  val churchId: String = "",
)

data class CrossChurchVisitItem(
  val id: String = "",
  val hostChurchName: String = "",
  val dateLabel: String = "",
  val timeLabel: String = "",
  val serviceTitle: String = "",
  val churchId: String = "",
)

val AttendanceRecord.effectiveStatus: String
  get() = status.trim().ifEmpty { "on_time" }

val AttendanceRecord.isCrossChurchVisitor: Boolean
  get() = false

val AttendanceRecord.effectiveMemberDbId: String
  get() = memberDocId.trim().ifEmpty { memberId.trim().ifEmpty { id } }

val AttendanceRecord.effectiveMemberCode: String
  get() = memberCode.trim().ifEmpty { memberId }

val AttendanceRecord.homeChurchName: String?
  get() = null

val AttendanceRecord.homeChurchId: String?
  get() = null

fun ChurchService.isFinishedForAbsence(): Boolean {
  return isPermanentlyLocked() || System.currentTimeMillis() > (startTimeEpochMillis + 3_600_000L * 3)
}

