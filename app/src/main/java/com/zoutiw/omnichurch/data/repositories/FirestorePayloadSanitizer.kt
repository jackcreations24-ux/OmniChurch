package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.tasks.await

/**
 * Centralized Firestore Rules Compliance & Payload Sanitizer for OmniChurch PRO.
 *
 * Strictly enforces:
 * - Valid document ID regex `^[a-zA-Z0-9_\-]+$` (1..128; churchId 2..128)
 * - Exact `hasOnly` key whitelisting for churches, members, services, announcements, attendances
 * - Valid enum clamping and terminal service lock invariants
 * - Active Firebase Auth session guard for all Admin writes (even after Quick PIN / Biometrics)
 * - ID token refresh helper after login / email verification
 */
object FirestorePayloadSanitizer {

  private val VALID_DOC_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")

  private val RESTRICTED_CHURCH_VERIFICATION_KEYS =
    setOf("adminVerified", "adminVerifiedAt", "adminVerifiedBy")

  val CHURCH_ALLOWED_KEYS: Set<String> =
    setOf(
      "id",
      "code",
      "name",
      "ownerId",
      "pastor",
      "address",
      "city",
      "country",
      "phone",
      "email",
      "motto",
      "themeColor",
      "logoIcon",
      "logoUrl",
      "bannerUrl",
      "tagline",
      "createdAt",
      "updatedAt",
      "rnc",
      "rncNumber",
      "rncDocumentPhoto",
      "rncDocumentUploadedAt",
      "rncVerificationStatus",
      "rncVerifiedAt",
      "rncLegalName",
      "rncDocumentType",
      "rncNotes",
      "adminGoogleEmail",
      "adminGoogleUid",
      "adminGoogleName",
      "adminGooglePhoto",
      "adminAuthMethod",
      "adminPasscode",
      "adminPasscodeUpdatedAt",
      "quickPinEnabled",
      "autoLockMinutes",
      "customCardTitle",
      "signatureUrl",
      "website",
      "windowsExeDownloadUrl",
      "desktopAppVersion",
      "donationInfo",
      "churchRules",
      "status",
      "archivedAt",
      "isLegacy",
    )

  val CHURCH_UPDATE_ALLOWED_KEYS: Set<String> =
    CHURCH_ALLOWED_KEYS - setOf("createdAt", "ownerId")

  val VALID_ADMIN_AUTH_METHODS: Set<String> =
    setOf("email_password", "google", "both", "legacy_pin")

  val VALID_CHURCH_STATUSES: Set<String> = setOf("active", "archived")

  val MEMBER_ALLOWED_KEYS: Set<String> =
    setOf(
      "id",
      "userId",
      "churchId",
      "churchName",
      "churchLogoUrl",
      "memberId",
      "memberCode",
      "pin",
      "securityPin",
      "titlePrefix",
      "fullName",
      "gender",
      "civilStatus",
      "maritalStatus",
      "profession",
      "dateOfBirth",
      "birthDate",
      "baptismDate",
      "joinedDate",
      "joinedAt",
      "joinDateFormatted",
      "phone",
      "emergencyContact",
      "emergencyContactName",
      "email",
      "googleEmail",
      "googleUid",
      "googlePhoto",
      "avatarUrl",
      "photoUrl",
      "avatar",
      "photo",
      "address",
      "city",
      "zone",
      "department",
      "ministry",
      "role",
      "memberType",
      "cellGroup",
      "status",
      "legalDocumentType",
      "legalDocumentNumber",
      "barcodeToken",
      "nfcCardUid",
      "qrCode",
      "notes",
      "createdBy",
      "updatedBy",
      "fcmToken",
      "language",
      "attendanceStreak",
      "totalAttendances",
      "isActive",
      "isDemo",
      "createdAt",
      "updatedAt",
    )

  val MEMBER_UPDATE_ALLOWED_KEYS: Set<String> =
    MEMBER_ALLOWED_KEYS - setOf("churchId", "createdAt")

  val VALID_MEMBER_STATUSES: Set<String> = setOf("active", "inactive", "pending", "suspended")

  val SERVICE_ALLOWED_KEYS: Set<String> =
    setOf(
      "id",
      "serviceId",
      "churchId",
      "churchName",
      "category",
      "type",
      "name",
      "title",
      "serviceTitle",
      "serviceName",
      "description",
      "imageUrl",
      "posterUrl",
      "bannerUrl",
      "date",
      "serviceDate",
      "dateFormatted",
      "time",
      "startTime",
      "endTime",
      "preacherOrLeader",
      "leader",
      "preacher",
      "reader",
      "musicians",
      "ushers",
      "department",
      "authorId",
      "authorName",
      "createdBy",
      "updatedBy",
      "theme",
      "passage",
      "notes",
      "status",
      "location",
      "address",
      "qrCode",
      "recurrence",
      "usherMemberIds",
      "usherUserIds",
      "isOpenForAttendance",
      "wasOpenedForAttendance",
      "isPermanentlyEnded",
      "isLive",
      "isActive",
      "recurring",
      "endedAt",
      "sendReminder24h",
      "reminderSentAt",
      "startTimeEpochMillis",
      "endTimeEpochMillis",
      "lateToleranceMinutes",
      "lateThresholdMinutes",
      "capacity",
      "durationMinutes",
      "attendanceCount",
      "reactionsCount",
      "commentsCount",
      "createdAt",
      "updatedAt",
      "isDemo",
    )

  val SERVICE_UPDATE_ALLOWED_KEYS: Set<String> =
    SERVICE_ALLOWED_KEYS - setOf("id", "serviceId", "churchId", "authorId", "authorName", "createdAt")

  val ENDED_SERVICE_UPDATE_ALLOWED_KEYS: Set<String> =
    setOf("notes", "reminderSentAt", "sendReminder24h", "updatedAt")

  val ANNOUNCEMENT_ALLOWED_KEYS: Set<String> =
    setOf(
      "id",
      "churchId",
      "title",
      "content",
      "department",
      "category",
      "date",
      "time",
      "targetAudience",
      "audience",
      "priority",
      "expiresAt",
      "authorPhotoUrl",
      "isPinned",
      "pinned",
      "showToastOnLogin",
      "toastAlertLevel",
      "isScheduledNotification",
      "isPublished",
      "scheduledNotificationDate",
      "scheduledNotificationEndDate",
      "scheduledTargetAudience",
      "posterUrl",
      "imageUrl",
      "imageCaption",
      "serviceId",
      "authorId",
      "authorName",
      "author",
      "authorType",
      "leader",
      "preacher",
      "reader",
      "musicians",
      "ushers",
      "title_ht",
      "title_fr",
      "title_en",
      "title_es",
      "content_ht",
      "content_fr",
      "content_en",
      "content_es",
      "reactionsCount",
      "commentsCount",
      "createdBy",
      "updatedBy",
      "isDemo",
      "createdAt",
      "updatedAt",
    )

  val ANNOUNCEMENT_UPDATE_ALLOWED_KEYS: Set<String> =
    ANNOUNCEMENT_ALLOWED_KEYS - setOf("id", "churchId", "authorId", "authorName", "createdAt")

  val REGULAR_ATTENDANCE_ALLOWED_KEYS: Set<String> =
    setOf(
      "churchId",
      "serviceId",
      "serviceName",
      "memberDocId",
      "memberId",
      "memberCode",
      "memberName",
      "memberPhotoUrl",
      "userId",
      "recordedByUid",
      "status",
      "checkInMethod",
      "barcodeToken",
      "timestamp",
      "createdAt",
      "attendanceRole",
      "isVisitor",
    )

  val VALID_CHECKIN_METHODS: Set<String> =
    setOf("qr_scan", "qr_scanner", "nfc_scan", "barcode", "manual", "kiosk")

  val VALID_ATTENDANCE_STATUSES: Set<String> = setOf("on_time", "late", "excused")

  val VALID_ATTENDANCE_ROLES: Set<String> = setOf("member", "visitor")

  fun isValidDocumentId(id: String?): Boolean {
    val clean = id?.trim().orEmpty()
    return clean.length in 1..128 && VALID_DOC_ID_REGEX.matches(clean)
  }

  fun isValidChurchId(churchId: String?): Boolean {
    val clean = churchId?.trim().orEmpty()
    return clean.length in 2..128 && VALID_DOC_ID_REGEX.matches(clean)
  }

  fun isoUtcNow(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
  }

  /**
   * Verifies that an active Firebase Auth user is present before performing Admin Firestore writes.
   * Prevents PERMISSION_DENIED when local UI was unlocked via Quick PIN or Biometrics after the
   * Firebase Auth session expired.
   */
  fun requireActiveAdminAuth(
    auth: FirebaseAuth,
    operationHint: String = "ekri done administratè yo",
  ): Result<FirebaseUser> {
    val user = auth.currentUser
    return if (user != null && user.uid.isNotBlank()) {
      Result.success(user)
    } else {
      Result.failure(
        IllegalStateException(
          "Sesyon Firebase Admin ou a ekspire. Tanpri rekonekte ak imèl/modpas oswa Google anvan ou $operationHint."
        )
      )
    }
  }

  /**
   * Refreshes the Firebase Auth ID token so updated `email_verified` claims propagate immediately
   * to Firestore security rules before subsequent writes.
   */
  suspend fun refreshAuthToken(user: FirebaseUser?): Boolean {
    if (user == null) return false
    return runCatching {
      user.reload().await()
      user.getIdToken(true).await()
      true
    }.getOrDefault(false)
  }

  fun sanitizeChurchCreate(
    raw: Map<String, Any?>,
    docId: String,
    currentUser: FirebaseUser?,
  ): Map<String, Any> {
    val allowRestricted = CryptoSecurity.isSuperDeveloperAccount(currentUser?.email)
    val out = linkedMapOf<String, Any>()
    for ((k, v) in raw) {
      if (v == null) continue
      if (k in RESTRICTED_CHURCH_VERIFICATION_KEYS && !allowRestricted) continue
      if (k !in CHURCH_ALLOWED_KEYS) continue
      out[k] = v
    }
    out["id"] = docId
    if (currentUser != null) {
      out["ownerId"] = currentUser.uid
      out["adminGoogleUid"] = currentUser.uid
      val lowerEmail = currentUser.email?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotEmpty() }
      if (lowerEmail != null) {
        out["adminGoogleEmail"] = lowerEmail
      }
    }
    (out["adminAuthMethod"] as? String)?.let { method ->
      out["adminAuthMethod"] =
        if (method in VALID_ADMIN_AUTH_METHODS) method else "email_password"
    }
    (out["status"] as? String)?.let { status ->
      out["status"] = if (status in VALID_CHURCH_STATUSES) status else "active"
    }
    (out["autoLockMinutes"] as? Number)?.let { mins ->
      out["autoLockMinutes"] = mins.toInt().coerceIn(0, 120)
    }
    return out
  }

  fun sanitizeChurchUpdate(
    raw: Map<String, Any?>,
    currentUser: FirebaseUser?,
  ): Map<String, Any?> {
    val allowRestricted = CryptoSecurity.isSuperDeveloperAccount(currentUser?.email)
    val out = linkedMapOf<String, Any?>()
    for ((k, v) in raw) {
      if (k in RESTRICTED_CHURCH_VERIFICATION_KEYS && !allowRestricted) continue
      if (k !in CHURCH_UPDATE_ALLOWED_KEYS) continue
      if (v == null) continue
      when (k) {
        "adminAuthMethod" -> {
          val m = (v as? String)?.trim()?.lowercase(Locale.US).orEmpty()
          if (m in VALID_ADMIN_AUTH_METHODS) out[k] = m
        }
        "status" -> {
          val s = (v as? String)?.trim()?.lowercase(Locale.US).orEmpty()
          if (s in VALID_CHURCH_STATUSES) out[k] = s
        }
        "adminGoogleEmail" -> {
          val em = (v as? String)?.trim()?.lowercase(Locale.US)
          if (!em.isNullOrEmpty()) out[k] = em
        }
        "autoLockMinutes" -> {
          val mins = (v as? Number)?.toInt()?.coerceIn(0, 120) ?: 5
          out[k] = mins
        }
        else -> out[k] = v
      }
    }
    return out
  }

  fun sanitizeMemberCreate(raw: Map<String, Any?>, docId: String): Map<String, Any> {
    val out = linkedMapOf<String, Any>()
    for ((k, v) in raw) {
      if (v == null) continue
      if (k !in MEMBER_ALLOWED_KEYS) continue
      out[k] = v
    }
    // If id is included, it MUST match document ID
    if (out.containsKey("id")) {
      out["id"] = docId
    }
    (out["fullName"] as? String)?.let { name ->
      out["fullName"] = name.trim().take(200)
    }
    (out["status"] as? String)?.let { st ->
      out["status"] = if (st in VALID_MEMBER_STATUSES) st else "active"
    }
    return out
  }

  fun sanitizeMemberUpdate(raw: Map<String, Any?>): Map<String, Any?> {
    val out = linkedMapOf<String, Any?>()
    for ((k, v) in raw) {
      if (v == null) continue
      if (k !in MEMBER_UPDATE_ALLOWED_KEYS) continue
      when (k) {
        "fullName" -> out[k] = (v as? String)?.trim()?.take(200) ?: continue
        "status" -> {
          val st = (v as? String)?.trim()?.lowercase(Locale.US).orEmpty()
          if (st in VALID_MEMBER_STATUSES) out[k] = st
        }
        else -> out[k] = v
      }
    }
    return out
  }

  fun sanitizeServiceCreate(raw: Map<String, Any?>): Map<String, Any> {
    val out = linkedMapOf<String, Any>()
    for ((k, v) in raw) {
      if (v == null) continue
      if (k !in SERVICE_ALLOWED_KEYS) continue
      out[k] = v
    }
    (out["title"] as? String)?.let { t -> out["title"] = t.trim().take(200) }
    (out["lateToleranceMinutes"] as? Number)?.let { tol ->
      out["lateToleranceMinutes"] = tol.toLong().coerceIn(0L, 360L)
    }
    val ended = out["isPermanentlyEnded"] == true
    if (ended) {
      out["isOpenForAttendance"] = false
    }
    return out
  }

  fun sanitizeServiceUpdate(
    raw: Map<String, Any?>,
    isPermanentlyEnded: Boolean,
  ): Map<String, Any?> {
    val allowedKeys =
      if (isPermanentlyEnded) ENDED_SERVICE_UPDATE_ALLOWED_KEYS else SERVICE_UPDATE_ALLOWED_KEYS
    val out = linkedMapOf<String, Any?>()
    for ((k, v) in raw) {
      if (k !in allowedKeys) continue
      if (v == null && k != "description" && k != "theme" && k != "passage" && k != "leader" &&
          k != "preacher" && k != "reader" && k != "musicians" && k != "ushers" && k != "imageUrl") {
        continue
      }
      out[k] = v
    }
    (out["title"] as? String)?.let { t -> out["title"] = t.trim().take(200) }
    (out["lateToleranceMinutes"] as? Number)?.let { tol ->
      out["lateToleranceMinutes"] = tol.toLong().coerceIn(0L, 360L)
    }
    if (isPermanentlyEnded || out["isPermanentlyEnded"] == true) {
      out.remove("isOpenForAttendance")
    }
    return out
  }

  fun sanitizeAnnouncementCreate(raw: Map<String, Any?>): Map<String, Any> {
    val out = linkedMapOf<String, Any>()
    for ((k, v) in raw) {
      if (v == null) continue
      if (k !in ANNOUNCEMENT_ALLOWED_KEYS) continue
      out[k] = v
    }
    (out["title"] as? String)?.let { t -> out["title"] = t.trim().take(300) }
    (out["content"] as? String)?.let { c -> out["content"] = c.trim().take(5000) }
    return out
  }

  fun sanitizeAnnouncementUpdate(raw: Map<String, Any?>): Map<String, Any?> {
    val out = linkedMapOf<String, Any?>()
    for ((k, v) in raw) {
      if (k !in ANNOUNCEMENT_UPDATE_ALLOWED_KEYS) continue
      out[k] = v
    }
    (out["title"] as? String)?.let { t -> out["title"] = t.trim().take(300) }
    (out["content"] as? String)?.let { c -> out["content"] = c.trim().take(5000) }
    return out
  }

  fun sanitizeRegularAttendanceCreate(raw: Map<String, Any?>): Map<String, Any> {
    val out = linkedMapOf<String, Any>()
    for ((k, v) in raw) {
      if (v == null) continue
      if (k !in REGULAR_ATTENDANCE_ALLOWED_KEYS) continue
      out[k] = v
    }
    val method = (out["checkInMethod"] as? String)?.trim()?.lowercase(Locale.US).orEmpty()
    out["checkInMethod"] = if (method in VALID_CHECKIN_METHODS) method else "qr_scan"
    val status = (out["status"] as? String)?.trim()?.lowercase(Locale.US).orEmpty()
    out["status"] = if (status in VALID_ATTENDANCE_STATUSES) status else "on_time"
    (out["attendanceRole"] as? String)?.let { role ->
      out["attendanceRole"] = if (role in VALID_ATTENDANCE_ROLES) role else "member"
    }
    return out
  }
}
