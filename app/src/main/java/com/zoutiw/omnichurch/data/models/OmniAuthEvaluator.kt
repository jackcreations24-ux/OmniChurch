package com.zoutiw.omnichurch.data.models

import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import java.security.MessageDigest

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

  fun memberNotFoundGloballyMessage(idOrPhone: String): String =
    "Pa gen okenn manm ki anrejistre avèk ID/Telefòn \"${idOrPhone.trim()}\". Verifye ID Matrikil oswa nimewo telefòn ou."

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

    val matchedMember =
      churchMembers
        .filter { doesMemberBelongToChurch(it, church) }
        .firstOrNull { member -> doesMemberMatchIdentifier(member, cleanInput, allowSuffixMatch = false) }
        ?: churchMembers
          .filter { doesMemberBelongToChurch(it, church) }
          .firstOrNull { member -> doesMemberMatchIdentifier(member, cleanInput, allowSuffixMatch = true) }

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

  fun doesMemberMatchIdentifier(
    member: Member,
    idOrPhone: String,
    allowSuffixMatch: Boolean = false,
  ): Boolean {
    val cleanInput = idOrPhone.trim()
    if (cleanInput.isEmpty()) return false

    val cleanUpper = cleanInput.uppercase()
    val inputDigits = cleanInput.replace(Regex("[^0-9]"), "")

    val memMatrikilUpper = (member.memberId ?: "").trim().uppercase()
    val memCodeUpper = member.memberCode.trim().uppercase()
    val memDocIdUpper = member.id.trim().uppercase()
    val memPhoneDigits = (member.phone ?: "").replace(Regex("[^0-9]"), "")
    val memEmailUpper = (member.email ?: "").trim().uppercase()
    val memGoogleEmailUpper = (member.googleEmail ?: "").trim().uppercase()

    val isMatrikilMatch =
      (memMatrikilUpper.isNotEmpty() && memMatrikilUpper == cleanUpper) ||
        (memCodeUpper.isNotEmpty() && memCodeUpper == cleanUpper)
    val isInternalIdMatch = memDocIdUpper.isNotEmpty() && memDocIdUpper == cleanUpper
    val isPhoneMatch =
      inputDigits.length >= 7 && memPhoneDigits.length >= 7 && memPhoneDigits == inputDigits
    val isEmailMatch =
      (memEmailUpper.isNotEmpty() && memEmailUpper == cleanUpper) ||
        (memGoogleEmailUpper.isNotEmpty() && memGoogleEmailUpper == cleanUpper)
    val isSuffixMatch =
      allowSuffixMatch &&
        cleanUpper.length >= 4 &&
        !cleanUpper.contains("-") &&
        ((memMatrikilUpper.isNotEmpty() && memMatrikilUpper.endsWith("-$cleanUpper")) ||
          (memCodeUpper.isNotEmpty() && memCodeUpper.endsWith("-$cleanUpper")))

    return isMatrikilMatch || isInternalIdMatch || isPhoneMatch || isEmailMatch || isSuffixMatch
  }

  fun resolveChurchForMatchedMember(
    member: Member,
    churches: List<Church>,
  ): Church {
    return churches.firstOrNull { doesMemberBelongToChurch(member, it) }
      ?: Church(
        id = member.churchId.trim(),
        code = member.churchId.trim(),
        name = member.churchName.trim().ifBlank { "OmniChurch Pro" },
        logoUrl = member.churchLogoUrl,
      )
  }

  fun authenticateMemberAutoResolving(
    churches: List<Church>,
    allMembers: List<Member>,
    idOrPhone: String?,
    enteredPin: String?,
    selectedChurchId: String? = null,
  ): Result<List<AuthenticatedMemberSession>> {
    val cleanInput = (idOrPhone ?: "").trim()
    val cleanPin = (enteredPin ?: "").trim()
    val cleanSelectedChurchId = (selectedChurchId ?: "").trim()

    if (cleanInput.isEmpty()) {
      return Result.failure(OmniAuthException(ERROR_EMPTY_ID))
    }
    if (cleanPin.isEmpty()) {
      return Result.failure(OmniAuthException(ERROR_EMPTY_PIN))
    }

    if (cleanSelectedChurchId.isNotEmpty()) {
      val explicitChurch = resolveChurchForMemberLogin(churches, cleanSelectedChurchId, cleanInput)
      return authenticateMemberFromList(
        church = explicitChurch,
        churchMembers = allMembers,
        idOrPhone = cleanInput,
        enteredPin = cleanPin,
      ).map { listOf(it) }
    }

    val prefixedChurch = resolveChurchForMemberLogin(churches, null, cleanInput)
    val scopedCandidates =
      if (prefixedChurch != null) {
        val churchScoped =
          allMembers.filter {
            doesMemberBelongToChurch(it, prefixedChurch) &&
              doesMemberMatchIdentifier(it, cleanInput, allowSuffixMatch = false)
          }
        churchScoped.ifEmpty {
          allMembers.filter { doesMemberMatchIdentifier(it, cleanInput, allowSuffixMatch = false) }
        }
      } else {
        allMembers.filter { doesMemberMatchIdentifier(it, cleanInput, allowSuffixMatch = false) }
      }

    val identifierMatches =
      scopedCandidates.ifEmpty {
        allMembers.filter { doesMemberMatchIdentifier(it, cleanInput, allowSuffixMatch = true) }
      }

    if (identifierMatches.isEmpty()) {
      return Result.failure(OmniAuthException(memberNotFoundGloballyMessage(cleanInput)))
    }

    val pinVerified = identifierMatches.filter { verifyMemberPin(it, cleanPin) }
    if (pinVerified.isEmpty()) {
      return Result.failure(OmniAuthException(ERROR_WRONG_PIN))
    }

    val activeVerified =
      pinVerified.filter { member ->
        val st = member.effectiveStatus.lowercase()
        st != "suspended" && st != "inactive"
      }
    if (activeVerified.isEmpty()) {
      return Result.failure(
        OmniAuthException("Kont manm sa a sispann oswa pa aktif. Kontakte administratè legliz ou a.")
      )
    }

    val resolvedSessions =
      activeVerified
        .map { member ->
          AuthenticatedMemberSession(
            church = resolveChurchForMatchedMember(member, churches),
            member = member,
          )
        }
        .distinctBy { it.church.id.trim().uppercase().ifEmpty { it.member.id } }

    return Result.success(resolvedSessions)
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
