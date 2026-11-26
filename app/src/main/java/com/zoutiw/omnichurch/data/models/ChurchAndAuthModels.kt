package com.zoutiw.omnichurch.data.models

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Immutable
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

@Immutable
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
  val autoLockMinutes: Int = 0,
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
    get() = autoLockMinutes.coerceIn(0, 120)

  val effectiveQuickPinEnabled: Boolean
    get() = quickPinEnabled && adminPasscode.isNotBlank()
}

@Immutable
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

@Immutable
data class AuthenticatedMemberSession(
  val church: Church,
  val member: Member,
)

class OmniAuthException(override val message: String) : Exception(message)

@Immutable
data class PublicVerificationResolution(
  val church: Church,
  val member: Member,
)

fun filterNonNullMap(vararg pairs: Pair<String, Any?>): Map<String, Any> {
  val result = mutableMapOf<String, Any>()
  for ((k, v) in pairs) {
    if (v != null) {
      result[k] = v
    }
  }
  return result
}
