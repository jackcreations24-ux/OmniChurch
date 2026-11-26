package com.zoutiw.omnichurch.data.repositories

internal object FirestorePayloadAllowedKeys {
  val RESTRICTED_CHURCH_VERIFICATION_KEYS =
    setOf("adminVerified", "adminVerifiedAt", "adminVerifiedBy")

  val CHURCH_ALLOWED_KEYS: Set<String> =
    setOf(
      "id", "code", "name", "ownerId", "pastor", "address", "city", "country",
      "phone", "email", "motto", "themeColor", "logoIcon", "logoUrl", "bannerUrl",
      "tagline", "createdAt", "updatedAt", "rnc", "rncNumber", "rncDocumentPhoto",
      "rncDocumentUploadedAt", "rncVerificationStatus", "rncVerifiedAt", "rncLegalName",
      "rncDocumentType", "rncNotes", "adminGoogleEmail", "adminGoogleUid",
      "adminGoogleName", "adminGooglePhoto", "adminAuthMethod", "adminPasscode",
      "adminPasscodeUpdatedAt", "quickPinEnabled", "autoLockMinutes", "customCardTitle",
      "signatureUrl", "website", "windowsExeDownloadUrl", "desktopAppVersion",
      "donationInfo", "churchRules", "status", "archivedAt", "isLegacy",
    )

  val CHURCH_UPDATE_ALLOWED_KEYS: Set<String> =
    CHURCH_ALLOWED_KEYS - setOf("createdAt", "ownerId")

  val VALID_ADMIN_AUTH_METHODS: Set<String> =
    setOf("email_password", "google", "both", "legacy_pin")

  val VALID_CHURCH_STATUSES: Set<String> = setOf("active", "archived")

  val MEMBER_ALLOWED_KEYS: Set<String> =
    setOf(
      "id", "userId", "churchId", "churchName", "churchLogoUrl", "memberId",
      "memberCode", "pin", "securityPin", "titlePrefix", "fullName", "gender",
      "civilStatus", "maritalStatus", "profession", "dateOfBirth", "birthDate",
      "baptismDate", "joinedDate", "joinedAt", "joinDateFormatted", "phone",
      "emergencyContact", "emergencyContactName", "email", "googleEmail", "googleUid",
      "googlePhoto", "avatarUrl", "photoUrl", "avatar", "photo", "address", "city",
      "zone", "department", "ministry", "role", "memberType", "cellGroup", "status",
      "legalDocumentType", "legalDocumentNumber", "barcodeToken", "nfcCardUid",
      "qrCode", "notes", "createdBy", "updatedBy", "fcmToken", "language",
      "attendanceStreak", "totalAttendances", "isActive", "isDemo", "createdAt",
      "updatedAt",
    )

  val MEMBER_UPDATE_ALLOWED_KEYS: Set<String> =
    MEMBER_ALLOWED_KEYS - setOf("churchId", "createdAt")

  val VALID_MEMBER_STATUSES: Set<String> = setOf("active", "inactive", "pending", "suspended")

  val SERVICE_ALLOWED_KEYS: Set<String> =
    setOf(
      "id", "serviceId", "churchId", "churchName", "category", "type", "name",
      "title", "serviceTitle", "serviceName", "description", "imageUrl", "posterUrl",
      "bannerUrl", "date", "serviceDate", "dateFormatted", "time", "startTime",
      "endTime", "preacherOrLeader", "leader", "preacher", "reader", "musicians",
      "ushers", "department", "authorId", "authorName", "createdBy", "updatedBy",
      "theme", "passage", "notes", "status", "location", "address", "qrCode",
      "recurrence", "usherMemberIds", "usherUserIds", "isOpenForAttendance",
      "wasOpenedForAttendance", "isPermanentlyEnded", "isLive", "isActive",
      "recurring", "endedAt", "sendReminder24h", "reminderSentAt",
      "startTimeEpochMillis", "endTimeEpochMillis", "lateToleranceMinutes",
      "lateThresholdMinutes", "capacity", "durationMinutes", "attendanceCount",
      "reactionsCount", "commentsCount", "createdAt", "updatedAt", "isDemo",
    )

  val SERVICE_UPDATE_ALLOWED_KEYS: Set<String> =
    SERVICE_ALLOWED_KEYS - setOf("id", "serviceId", "churchId", "authorId", "authorName", "createdAt")

  val ENDED_SERVICE_UPDATE_ALLOWED_KEYS: Set<String> =
    setOf("notes", "reminderSentAt", "sendReminder24h", "updatedAt")

  val ANNOUNCEMENT_ALLOWED_KEYS: Set<String> =
    setOf(
      "id", "churchId", "title", "content", "department", "category", "date", "time",
      "targetAudience", "audience", "priority", "expiresAt", "authorPhotoUrl",
      "isPinned", "pinned", "showToastOnLogin", "toastAlertLevel",
      "isScheduledNotification", "isPublished", "scheduledNotificationDate",
      "scheduledNotificationEndDate", "scheduledTargetAudience", "posterUrl",
      "imageUrl", "imageCaption", "serviceId", "authorId", "authorName", "author",
      "authorType", "leader", "preacher", "reader", "musicians", "ushers",
      "title_ht", "title_fr", "title_en", "title_es",
      "content_ht", "content_fr", "content_en", "content_es",
      "reactionsCount", "commentsCount", "createdBy", "updatedBy", "isDemo",
      "createdAt", "updatedAt",
    )

  val ANNOUNCEMENT_UPDATE_ALLOWED_KEYS: Set<String> =
    ANNOUNCEMENT_ALLOWED_KEYS - setOf("id", "churchId", "authorId", "authorName", "createdAt")

  val REGULAR_ATTENDANCE_ALLOWED_KEYS: Set<String> =
    setOf(
      "churchId", "serviceId", "serviceName", "memberDocId", "memberId",
      "memberCode", "memberName", "memberPhotoUrl", "userId", "recordedByUid",
      "status", "checkInMethod", "barcodeToken", "timestamp", "createdAt",
      "attendanceRole", "isVisitor",
    )

  val VALID_CHECKIN_METHODS: Set<String> =
    setOf("qr_scan", "qr_scanner", "nfc_scan", "barcode", "manual", "kiosk")

  val VALID_ATTENDANCE_STATUSES: Set<String> = setOf("on_time", "late", "excused")

  val VALID_ATTENDANCE_ROLES: Set<String> = setOf("member", "visitor")
}
