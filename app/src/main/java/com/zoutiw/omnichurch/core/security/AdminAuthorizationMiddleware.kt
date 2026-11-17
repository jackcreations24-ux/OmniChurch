package com.zoutiw.omnichurch.core.security

import com.google.firebase.firestore.FirebaseFirestore
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException

class AdminAuthorizationMiddleware(
  @Suppress("unused") private val db: FirebaseFirestore = FirestoreProvider.db
) {

  /**
   * Validates the admin passcode against the current church document in Firestore
   * without modifying or fetching full collections unnecessarily.
   */
  fun validateAdminPasscode(
    church: Church,
    enteredPasscode: String
  ): Result<Boolean> {
    val cleanPasscode = enteredPasscode.trim()
    if (cleanPasscode.isEmpty()) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_PIN))
    }

    val storedPasscode = church.adminPasscode.trim()
    if (storedPasscode.isEmpty()) {
      return Result.failure(OmniAuthException("Legliz sa a poko konfigire ak yon kòd admin."))
    }

    val isVerified = OmniAuthEvaluator.verifyAdminPasscode(church, cleanPasscode)
    if (!isVerified) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_WRONG_ADMIN_PASSCODE))
    }

    return Result.success(true)
  }

  fun authorizeAdminAction(
    @Suppress("unused") churchId: String,
    enteredPasscode: String?,
    googleEmail: String?,
    googleUid: String?,
    church: Church
  ): Result<Boolean> {
    val cleanEmail = (googleEmail ?: "").trim().lowercase()
    val cleanPasscode = (enteredPasscode ?: "").trim()

    val hasGoogleEmail = cleanEmail.isNotEmpty()
    val configuredAdminEmail = (church.adminGoogleEmail ?: "").trim().lowercase()

    if (hasGoogleEmail) {
      val isAuthorizedGoogle = OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(
        church = church,
        googleEmail = cleanEmail,
        googleUid = googleUid
      )
      if (!isAuthorizedGoogle && configuredAdminEmail.isNotEmpty()) {
        return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_UNAUTHORIZED_ADMIN_GOOGLE))
      }
    }

    val passcodeVerified = if (cleanPasscode.isNotEmpty()) {
      validateAdminPasscode(church, cleanPasscode).getOrDefault(false)
    } else {
      false
    }

    val googleVerified = hasGoogleEmail && OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(
      church = church,
      googleEmail = cleanEmail,
      googleUid = googleUid
    )

    if (cleanPasscode.isNotEmpty() && !passcodeVerified) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_WRONG_ADMIN_PASSCODE))
    }

    if (!googleVerified && !passcodeVerified) {
      return if (hasGoogleEmail) {
        Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_UNAUTHORIZED_ADMIN_GOOGLE))
      } else {
        Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_WRONG_ADMIN_PASSCODE))
      }
    }

    return Result.success(true)
  }
}

