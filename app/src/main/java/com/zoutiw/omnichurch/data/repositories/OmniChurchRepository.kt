package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

/**
 * Unified Facade Repository for OmniChurch Pro.
 * Delegates all operations to focused, modular domain repositories (`AuthRepository`,
 * `ChurchRepository`, `MemberRepository`, `ServiceRepository`, `AnnouncementRepository`,
 * `AttendanceRepository`, `NotificationRepository`).
 */
class OmniChurchRepository(
  val db: FirebaseFirestore,
  val churchRepository: ChurchRepository = ChurchRepository(db),
  val authRepository: AuthRepository = AuthRepository(db, churchRepository),
  val memberRepository: MemberRepository = MemberRepository(db),
  val serviceRepository: ServiceRepository = ServiceRepository(db),
  val announcementRepository: AnnouncementRepository = AnnouncementRepository(db),
  val attendanceRepository: AttendanceRepository = AttendanceRepository(db),
  val notificationRepository: NotificationRepository = NotificationRepository(db),
) {
  constructor(context: Context) : this(FirestoreProvider.get(context), context)

  constructor(db: FirebaseFirestore, context: Context) : this(
    db = db,
    churchRepository = ChurchRepository(db),
    authRepository =
      AuthRepository(
        db = db,
        churchRepository = ChurchRepository(db),
        sessionLockManager = com.zoutiw.omnichurch.core.utilities.SessionLockManager(context.applicationContext),
        preferencesManager = com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager(context.applicationContext),
      ),
    memberRepository = MemberRepository(db),
    serviceRepository = ServiceRepository(db),
    announcementRepository = AnnouncementRepository(db),
    attendanceRepository = AttendanceRepository(db),
    notificationRepository = NotificationRepository(db),
  )

  // Church Delegation
  fun getChurchesFlow(): Flow<List<Church>> = churchRepository.getChurchesFlow()
  fun observeChurches(): Flow<List<Church>> = churchRepository.observeChurches()
  fun observeChurchesResult(): Flow<Result<List<Church>>> = churchRepository.observeChurchesResult()
  suspend fun fetchAllChurches(): List<Church> = churchRepository.fetchAllChurches()
  fun observeChurch(churchId: String): Flow<Church?> = churchRepository.observeChurch(churchId)
  suspend fun setupChurchAndMemberProfile(
    createNewChurch: Boolean,
    churchNameInput: String,
    existingChurchIdInput: String,
    churchAddressInput: String,
    fullNameInput: String,
    departmentInput: String,
    ministryRoleInput: String,
    photoUrl: String?,
    churchCodeInput: String = "",
    adminPasscodeInput: String = "",
    memberPinInput: String = "1234",
    memberPhoneInput: String = "",
  ): Result<Member> =
    churchRepository.setupChurchAndMemberProfile(
      createNewChurch = createNewChurch,
      churchNameInput = churchNameInput,
      existingChurchIdInput = existingChurchIdInput,
      churchAddressInput = churchAddressInput,
      fullNameInput = fullNameInput,
      departmentInput = departmentInput,
      ministryRoleInput = ministryRoleInput,
      photoUrl = photoUrl,
      churchCodeInput = churchCodeInput,
      adminPasscodeInput = adminPasscodeInput,
      memberPinInput = memberPinInput,
      memberPhoneInput = memberPhoneInput,
    )

  // Auth Delegation
  suspend fun authenticateMemberWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<AuthenticatedMemberSession> =
    authRepository.authenticateMemberWithPin(selectedChurchId, idOrPhone, enteredPin)

  suspend fun authenticateMemberSmartWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<AuthRepository.MemberAutoLoginResolution> =
    authRepository.authenticateMemberSmartWithPin(selectedChurchId, idOrPhone, enteredPin)

  suspend fun findMemberByLinkedGoogleEmail(
    selectedChurchId: String?,
    googleEmailInput: String,
  ): Result<AuthenticatedMemberSession?> =
    authRepository.findMemberByLinkedGoogleEmail(selectedChurchId, googleEmailInput)

  suspend fun linkGoogleAccountToMember(
    member: Member,
    googleEmail: String,
    googleUid: String,
    googlePhotoUrl: String?,
  ): Result<Member> =
    authRepository.linkGoogleAccountToMember(member, googleEmail, googleUid, googlePhotoUrl)

  suspend fun authenticateAdminForChurch(
    selectedChurchId: String,
    enteredPasscode: String?,
    googleEmail: String?,
    googleUid: String?,
  ): Result<Church> =
    authRepository.authenticateAdminForChurch(selectedChurchId, enteredPasscode, googleEmail, googleUid)

  fun currentAuthUid(): String = authRepository.currentAuthUid()

  suspend fun signInAdminWithEmailAndPassword(
    email: String,
    password: String,
    selectedChurchId: String? = null,
  ): Result<Church> =
    authRepository.signInAdminWithEmailAndPassword(email, password, selectedChurchId)

  suspend fun signInAdminSmartWithEmailAndPassword(
    email: String,
    password: String,
  ): Result<AuthRepository.AdminAutoLoginResolution> =
    authRepository.signInAdminSmartWithEmailAndPassword(email, password)

  suspend fun signInAdminSmartWithCurrentGoogleUser(): Result<AuthRepository.AdminAutoLoginResolution> =
    authRepository.signInAdminSmartWithCurrentGoogleUser()

  suspend fun finalizeAdminChurchSelection(church: Church): Result<Church> =
    authRepository.finalizeAdminChurchSelection(church)

  suspend fun signOutFirebaseAuthOnly() {
    authRepository.signOutAdmin()
  }

  suspend fun sendAdminPasswordResetEmail(email: String): Result<Unit> =
    authRepository.sendPasswordResetEmail(email)

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    authRepository.observeUserProfile(userId)

  suspend fun ensureUserProfileInitialized(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String?,
  ): Result<UserProfile> =
    authRepository.ensureUserProfileInitialized(uid, email, displayName, photoUrl)

  suspend fun syncUserChurchContext(
    churchId: String,
    churchName: String,
    memberId: String? = null,
  ) = authRepository.syncUserChurchContext(churchId, churchName, memberId)

  suspend fun resolveAutoRebindChurchIfUniquelyOwned(failedChurchId: String?): Church? =
    authRepository.resolveAutoRebindChurchIfUniquelyOwned(failedChurchId)

  suspend fun refreshCurrentUserToken(): Boolean =
    authRepository.refreshCurrentUserToken()

  // Member Delegation
  suspend fun fetchMembersForChurch(churchId: String, churchCode: String? = null): List<Member> =
    memberRepository.fetchMembersForChurch(churchId, churchCode)

  fun observeChurchMembers(churchId: String, churchCode: String? = null): Flow<List<Member>> =
    memberRepository.observeChurchMembers(churchId, churchCode)

  fun observeChurchMembersResult(
    churchId: String,
    churchCode: String? = null,
  ): Flow<Result<List<Member>>> =
    memberRepository.observeChurchMembersResult(churchId, churchCode)

  fun observeCurrentMember(userId: String): Flow<Member?> =
    memberRepository.observeCurrentMember(userId)

  fun observeSessionMember(
    churchId: String,
    memberDocId: String,
    memberMatrikil: String,
  ): Flow<Member?> =
    memberRepository.observeSessionMember(churchId, memberDocId, memberMatrikil)

  suspend fun registerMemberWithMatrikilAndPin(
    church: Church,
    fullNameInput: String,
    customMatrikilInput: String?,
    pinInput: String,
    phoneInput: String?,
    departmentInput: String,
    ministryRoleInput: String,
  ): Result<Member> =
    memberRepository.registerMemberWithMatrikilAndPin(
      church = church,
      fullNameInput = fullNameInput,
      customMatrikilInput = customMatrikilInput,
      pinInput = pinInput,
      phoneInput = phoneInput,
      departmentInput = departmentInput,
      ministryRoleInput = ministryRoleInput,
    )

  suspend fun updateMemberProfile(
    member: Member,
    fullName: String,
    department: String,
    ministryRole: String,
  ): Result<Unit> =
    memberRepository.updateMemberProfile(member, fullName, department, ministryRole)

  suspend fun updateMemberProfilePhoto(
    member: Member,
    photoDataUrl: String,
  ): Result<Unit> =
    memberRepository.updateMemberProfilePhoto(member, photoDataUrl)

  suspend fun updateMemberByAdmin(
    memberDocId: String,
    fullName: String,
    department: String,
    ministryRole: String,
    phone: String? = null,
    titlePrefix: String? = null,
  ): Result<Unit> =
    memberRepository.updateMemberByAdmin(
      memberDocId = memberDocId,
      fullName = fullName,
      department = department,
      ministryRole = ministryRole,
      phone = phone,
      titlePrefix = titlePrefix,
    )

  suspend fun bindNfcCardUidToMember(
    memberDocId: String,
    nfcCardUid: String,
  ): Result<Unit> =
    memberRepository.bindNfcCardUidToMember(
      memberDocId = memberDocId,
      nfcCardUid = nfcCardUid,
    )

  suspend fun resolveMemberCardVerification(
    rawTokenOrCode: String,
    fallbackChurchId: String? = null,
  ): Result<PublicVerificationResolution> =
    memberRepository.resolveMemberCardVerification(
      rawTokenOrCode = rawTokenOrCode,
      fallbackChurchId = fallbackChurchId,
    )

  // Service Delegation
  fun observeServices(churchId: String): Flow<List<ChurchService>> =
    serviceRepository.observeServices(churchId)

  fun observeActiveAttendanceServices(churchId: String): Flow<List<ChurchService>> =
    serviceRepository.observeActiveAttendanceServices(churchId)

  suspend fun createService(
    churchId: String,
    churchName: String,
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    authorName: String,
    actorId: String? = null,
    category: String = "dimanche_matin",
    date: String = "",
    startTime: String = "",
    endTime: String = "",
    theme: String? = null,
    passage: String? = null,
    leader: String? = null,
    preacher: String? = null,
    reader: String? = null,
    musicians: String? = null,
    ushers: String? = null,
    sendReminder24h: Boolean = false,
    imageUrl: String? = null,
  ): Result<String> =
    serviceRepository.createService(
      churchId = churchId,
      churchName = churchName,
      title = title,
      description = description,
      dateFormatted = dateFormatted,
      lateToleranceMinutes = lateToleranceMinutes,
      isOpenForAttendance = isOpenForAttendance,
      isLive = isLive,
      authorName = authorName,
      actorId = actorId,
      category = category,
      date = date,
      startTime = startTime,
      endTime = endTime,
      theme = theme,
      passage = passage,
      leader = leader,
      preacher = preacher,
      reader = reader,
      musicians = musicians,
      ushers = ushers,
      sendReminder24h = sendReminder24h,
      imageUrl = imageUrl,
    )

  suspend fun toggleServiceAttendanceStatus(
    service: ChurchService,
    isOpen: Boolean,
  ): Result<Unit> =
    serviceRepository.toggleServiceAttendanceStatus(service, isOpen)

  suspend fun updateService(
    serviceId: String,
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    category: String,
    date: String,
    startTime: String,
    endTime: String,
    theme: String?,
    passage: String?,
    leader: String?,
    preacher: String?,
    reader: String?,
    musicians: String?,
    ushers: String?,
    sendReminder24h: Boolean? = null,
    imageUrl: String? = null,
  ): Result<Unit> =
    serviceRepository.updateService(
      serviceId = serviceId,
      title = title,
      description = description,
      dateFormatted = dateFormatted,
      lateToleranceMinutes = lateToleranceMinutes,
      isOpenForAttendance = isOpenForAttendance,
      isLive = isLive,
      category = category,
      date = date,
      startTime = startTime,
      endTime = endTime,
      theme = theme,
      passage = passage,
      leader = leader,
      preacher = preacher,
      reader = reader,
      musicians = musicians,
      ushers = ushers,
      sendReminder24h = sendReminder24h,
      imageUrl = imageUrl,
    )

  suspend fun deleteService(serviceId: String): Result<Unit> =
    serviceRepository.deleteService(serviceId)

  // Announcement Delegation
  fun observeAnnouncements(churchId: String): Flow<List<Announcement>> =
    announcementRepository.observeAnnouncements(churchId)

  fun observeAnnouncementsResult(churchId: String): Flow<Result<List<Announcement>>> =
    announcementRepository.observeAnnouncementsResult(churchId)

  suspend fun createAnnouncement(
    churchId: String,
    title: String,
    content: String,
    department: String,
    isPinned: Boolean,
    authorName: String,
    actorId: String? = null,
    category: String = "anons",
    date: String? = null,
    targetAudience: String = "Tout moun",
    showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info",
    isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null,
    posterUrl: String? = null,
    serviceId: String? = null,
  ): Result<String> =
    announcementRepository.createAnnouncement(
      churchId = churchId,
      title = title,
      content = content,
      department = department,
      isPinned = isPinned,
      authorName = authorName,
      actorId = actorId,
      category = category,
      date = date,
      targetAudience = targetAudience,
      showToastOnLogin = showToastOnLogin,
      toastAlertLevel = toastAlertLevel,
      isScheduledNotification = isScheduledNotification,
      scheduledNotificationDate = scheduledNotificationDate,
      posterUrl = posterUrl,
      serviceId = serviceId,
    )

  suspend fun updateAnnouncement(
    announcementId: String,
    title: String,
    content: String,
    department: String,
    category: String = "anons",
    date: String? = null,
    targetAudience: String = "Tout moun",
    isPinned: Boolean = false,
    showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info",
    isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null,
    posterUrl: String? = null,
    serviceId: String? = null,
  ): Result<Unit> =
    announcementRepository.updateAnnouncement(
      announcementId = announcementId,
      title = title,
      content = content,
      department = department,
      category = category,
      date = date,
      targetAudience = targetAudience,
      isPinned = isPinned,
      showToastOnLogin = showToastOnLogin,
      toastAlertLevel = toastAlertLevel,
      isScheduledNotification = isScheduledNotification,
      scheduledNotificationDate = scheduledNotificationDate,
      posterUrl = posterUrl,
      serviceId = serviceId,
    )

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> =
    announcementRepository.deleteAnnouncement(announcementId)

  fun observeReactions(churchId: String): Flow<List<AnnouncementReaction>> =
    announcementRepository.observeReactions(churchId)

  suspend fun reactToTarget(
    targetId: String,
    isServiceTarget: Boolean,
    churchId: String,
    userName: String,
    reactionType: String = "pray",
    actorId: String? = null,
    isCommentTarget: Boolean = false,
  ): Result<Unit> =
    announcementRepository.reactToTarget(
      targetId = targetId,
      isServiceTarget = isServiceTarget,
      churchId = churchId,
      userName = userName,
      reactionType = reactionType,
      actorId = actorId,
      isCommentTarget = isCommentTarget,
    )

  fun observeChurchComments(churchId: String): Flow<List<AnnouncementComment>> =
    announcementRepository.observeChurchComments(churchId)

  fun observeComments(churchId: String, targetId: String): Flow<List<AnnouncementComment>> =
    announcementRepository.observeComments(churchId, targetId)

  suspend fun addComment(
    targetId: String,
    isServiceTarget: Boolean,
    churchId: String,
    userName: String,
    userPhotoUrl: String?,
    content: String,
    actorId: String? = null,
  ): Result<String> =
    announcementRepository.addComment(
      targetId = targetId,
      isServiceTarget = isServiceTarget,
      churchId = churchId,
      userName = userName,
      userPhotoUrl = userPhotoUrl,
      content = content,
      actorId = actorId,
    )

  // Attendance Delegation
  suspend fun validateAndRecordAttendance(
    rawTokenOrCode: String,
    selectedService: ChurchService,
    scannerUserProfile: UserProfile,
    checkInMethod: String,
  ): ScannerValidationOutcome =
    attendanceRepository.validateAndRecordAttendance(
      rawTokenOrCode = rawTokenOrCode,
      selectedService = selectedService,
      scannerUserProfile = scannerUserProfile,
      checkInMethod = checkInMethod,
    )

  fun observeServiceAttendances(
    churchId: String,
    serviceId: String,
  ): Flow<List<AttendanceRecord>> =
    attendanceRepository.observeServiceAttendances(churchId, serviceId)

  // Notification Delegation
  fun observeNotifications(userId: String, churchId: String? = null): Flow<List<ChurchNotification>> =
    notificationRepository.observeNotifications(userId, churchId)

  suspend fun markNotificationRead(notificationId: String): Result<Unit> =
    notificationRepository.markNotificationRead(notificationId)
}
