package com.zoutiw.omnichurch.presentation.screens.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.screens.member.EditMemberBottomSheet
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.CreateServiceBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.components.shareOmniChurchContent
import com.zoutiw.omnichurch.presentation.components.shareOmniChurchGeneralApp
import com.zoutiw.omnichurch.presentation.screens.auth.GoogleAccountPayload
import com.zoutiw.omnichurch.presentation.screens.auth.launchOptionalGoogleSignIn
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun ServicesScreen(
  userProfile: UserProfile?,
  servicesState: UiState<List<ChurchService>>,
  onCreateService: (
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
  ) -> Unit,
  onToggleAttendanceWindow: (ChurchService, Boolean) -> Unit,
  observeAttendances: (serviceId: String) -> Flow<List<AttendanceRecord>>,
  onOpenScannerForService: (String) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
  onDeleteService: (String) -> Unit = {},
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val isAdmin = userProfile?.isAdminOrSuperAdmin == true
  val isScanner = userProfile?.isAuthorizedScanner == true

  var showCreateDialog by remember { mutableStateOf(false) }
  var selectedAttendanceService by remember { mutableStateOf<ChurchService?>(null) }
  var servicePendingDeletion by remember { mutableStateOf<ChurchService?>(null) }

  Column(
    modifier = modifier.fillMaxSize().statusBarsPadding(),
  ) {
    GlassTopBar(
      title = stringResource(R.string.services_screen_title),
      onBackClick = onBackClick,
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (isAdmin) {
        item {
          GlassButton(
            text = stringResource(R.string.services_create_title),
            icon = Icons.Default.Add,
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            testTag = "services_create_new_btn",
          )
        }
      }

      when (servicesState) {
        is UiState.Loading -> item { GlassLoadingIndicator() }
        is UiState.Error ->
          item {
            PremiumEmptyState(
              title = stringResource(R.string.services_empty_title),
              subtitle = servicesState.message,
              icon = Icons.Default.CalendarMonth,
            )
          }
        is UiState.Success -> {
          val list = servicesState.data
          if (list.isEmpty()) {
            item {
              PremiumEmptyState(
                title = stringResource(R.string.services_empty_title),
                subtitle = stringResource(R.string.services_empty_subtitle),
                icon = Icons.Default.CalendarMonth,
                actionLabel = if (isAdmin) stringResource(R.string.services_create_title) else null,
                onActionClick = if (isAdmin) ({ showCreateDialog = true }) else null,
              )
            }
          } else {
            items(list, key = { it.id }) { service ->
              PremiumGlassCard(
                cornerRadius = 20.dp,
                isHighlighted = service.isOpenForAttendance,
                accentColor = if (service.isOpenForAttendance) SuccessGreen else ElectricBlue,
                contentPadding = PaddingValues(16.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = service.title,
                      style = MaterialTheme.typography.titleLarge,
                      color = PureWhite,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = service.dateFormatted,
                      style = MaterialTheme.typography.bodySmall,
                      color = GlassWhite.copy(alpha = 0.85f),
                    )
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    IconButton(
                      onClick = {
                        shareOmniChurchContent(
                          context = context,
                          title = "⛪ ${service.effectiveTitle}",
                          body = "📅 Dat & Lè: ${service.dateFormatted}",
                        )
                      },
                      modifier = Modifier.size(36.dp).testTag("share_service_btn_${service.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Pataje Sèvis la",
                        tint = GlassWhite.copy(alpha = 0.9f),
                        modifier = Modifier.size(19.dp),
                      )
                    }

                    if (isAdmin) {
                      Switch(
                        checked = service.isOpenForAttendance,
                        onCheckedChange = { onToggleAttendanceWindow(service, it) },
                        colors =
                          SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = SuccessGreen,
                          ),
                        modifier = Modifier.testTag("toggle_service_open_${service.id}"),
                      )
                      IconButton(
                        onClick = { servicePendingDeletion = service },
                        modifier =
                          Modifier.size(36.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.22f))
                            .testTag("delete_service_btn_${service.id}"),
                      ) {
                        Icon(
                          imageVector = Icons.Default.DeleteOutline,
                          contentDescription = "Efase Sèvis la",
                          tint = PureWhite,
                          modifier = Modifier.size(19.dp),
                        )
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier =
                      Modifier.size(8.dp)
                        .clip(CircleShape)
                        .background(
                          if (service.isOpenForAttendance) SuccessGreen else MutedGlassText
                        )
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text =
                      if (service.isOpenForAttendance)
                        stringResource(R.string.scanner_open_for_attendance)
                      else stringResource(R.string.scanner_closed_for_attendance),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (service.isOpenForAttendance) SuccessGreen else MutedGlassText,
                  )
                }

                if (isScanner) {
                  Spacer(modifier = Modifier.height(12.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (service.isOpenForAttendance) {
                      GlassButton(
                        text = stringResource(R.string.action_scan_qr),
                        icon = Icons.Default.QrCodeScanner,
                        onClick = { onOpenScannerForService(service.id) },
                        modifier = Modifier.weight(1f),
                        testTag = "scan_service_btn_${service.id}",
                      )
                    }
                    GlassButton(
                      text = stringResource(R.string.attendance_registered_title),
                      icon = Icons.Default.People,
                      isPrimaryNeon = false,
                      onClick = { selectedAttendanceService = service },
                      modifier = Modifier.weight(1f),
                      testTag = "view_attendances_btn_${service.id}",
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (servicePendingDeletion != null) {
    val srvToDelete = servicePendingDeletion!!
    GlassDialog(
      onDismissRequest = { servicePendingDeletion = null },
      title = "Efase Sèvis la?",
    ) {
      Text(
        text = "Èske ou sèten ou vle efase sèvis \"${srvToDelete.effectiveTitle}\" nan baz done legliz la?",
        style = MaterialTheme.typography.bodyMedium,
        color = GlassWhite,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text = stringResource(R.string.common_cancel),
          isPrimaryNeon = false,
          onClick = { servicePendingDeletion = null },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text = "Wi, Efase",
          icon = Icons.Default.DeleteOutline,
          onClick = {
            onDeleteService(srvToDelete.id)
            servicePendingDeletion = null
          },
          modifier = Modifier.weight(1f),
          testTag = "confirm_delete_service_dialog_btn_${srvToDelete.id}",
        )
      }
    }
  }

  if (selectedAttendanceService != null) {
    val srv = selectedAttendanceService!!
    val records by
      observeAttendances(srv.id).collectAsStateWithLifecycle(initialValue = emptyList())

    GlassModalBottomSheet(
      onDismissRequest = { selectedAttendanceService = null },
      title = "${srv.title} (${records.size})",
    ) {
      if (records.isEmpty()) {
        Text(
          text = stringResource(R.string.scanner_no_active_service),
          style = MaterialTheme.typography.bodyMedium,
          color = MutedGlassText,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(records, key = { it.id }) { rec ->
            val isOnTime = rec.status == "on_time"
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x660B1D3D))
                  .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                MemberAvatar3D(fullName = rec.memberName, size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = rec.memberName,
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = rec.memberCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGlassText,
                  )
                }
              }

              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(50))
                    .background(
                      (if (isOnTime) SuccessGreen else BrandOrange).copy(alpha = 0.25f)
                    )
                    .border(
                      1.dp,
                      if (isOnTime) SuccessGreen else BrandOrange,
                      RoundedCornerShape(50),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text(
                  text =
                    if (isOnTime) stringResource(R.string.attendance_status_on_time)
                    else stringResource(R.string.attendance_status_late),
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateServiceBottomSheet(
      onDismissRequest = { showCreateDialog = false },
      onSubmit = { sTitle, sCategory, sDate, sStartTime, sEndTime, sTheme, sPassage, sLeader, sPreacher, sReader, sMusicians, sUshers, sIsOpen, sSendReminder, sTol ->
        val sDateFormatted = "$sDate • $sStartTime"
        onCreateService(sTitle, sTheme, sDateFormatted, sTol, sIsOpen, true)
      },
    )
  }
}

@Composable
fun NotificationsScreen(
  notificationsState: UiState<List<ChurchNotification>>,
  onMarkRead: (String) -> Unit,
  onMarkAllRead: (List<ChurchNotification>) -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)

  Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
    GlassTopBar(
      title = stringResource(R.string.notifications_screen_title),
      onBackClick = onBackClick,
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      when (notificationsState) {
        is UiState.Loading -> item { GlassLoadingIndicator() }
        is UiState.Error ->
          item {
            PremiumEmptyState(
              title = stringResource(R.string.notifications_empty_title),
              subtitle = notificationsState.message,
              icon = Icons.Default.NotificationsNone,
            )
          }
        is UiState.Success -> {
          val list = notificationsState.data
          if (list.isEmpty()) {
            item {
              PremiumEmptyState(
                title = stringResource(R.string.notifications_empty_title),
                subtitle = stringResource(R.string.notifications_empty_subtitle),
                icon = Icons.Default.NotificationsNone,
              )
            }
          } else {
            val hasUnread = list.any { !it.isRead }
            if (hasUnread) {
              item {
                GlassButton(
                  text = stringResource(R.string.notifications_mark_all_read),
                  icon = Icons.Default.DoneAll,
                  isPrimaryNeon = false,
                  onClick = { onMarkAllRead(list) },
                  modifier = Modifier.fillMaxWidth(),
                  testTag = "notifications_mark_all_read_btn",
                )
              }
            }

            items(list, key = { it.id }) { notif ->
              PremiumGlassCard(
                cornerRadius = 18.dp,
                isHighlighted = !notif.isRead,
                accentColor = BlueNeon,
                onClick = {
                  if (!notif.isRead) {
                    onMarkRead(notif.id)
                  }
                },
                contentPadding = PaddingValues(14.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = notif.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (!notif.isRead) BrandGold else PureWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                  )
                  if (!notif.isRead) {
                    Box(
                      modifier =
                        Modifier.size(9.dp).clip(CircleShape).background(BlueNeon)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = notif.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = GlassWhite,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ProfileAndSettingsScreen(
  userProfile: UserProfile?,
  member: Member?,
  currentLanguageCode: String,
  onLanguageChange: (String) -> Unit,
  onSaveMemberChanges: (fullName: String, department: String, role: String) -> Unit,
  onSignOut: () -> Unit,
  onBackClick: () -> Unit,
  biometricEnabled: Boolean = false,
  onToggleBiometric: (Boolean) -> Unit = {},
  onLinkGoogleAccount: (GoogleAccountPayload) -> Unit = {},
  onRegisterNewMember:
    ((
      fullName: String,
      customMatrikil: String?,
      pin: String,
      phone: String?,
      department: String,
      ministryRole: String,
    ) -> Unit)? =
    null,
  churchMembers: List<Member> = emptyList(),
  onAdminUpdateMember:
    ((
      memberDocId: String,
      fullName: String,
      department: String,
      ministryRole: String,
      phone: String?,
      titlePrefix: String?,
    ) -> Unit)? =
    null,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val activityContext = remember(context) { context.findActivity() ?: context }
  val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
  val scope = rememberCoroutineScope()

  var fullName by
    remember(member?.fullName, userProfile?.displayName) {
      mutableStateOf(member?.fullName ?: userProfile?.displayName ?: "")
    }
  var department by remember(member?.department) { mutableStateOf(member?.department ?: "Adorasyon") }
  var role by remember(member?.role) { mutableStateOf(member?.role ?: "Manm") }
  var isLinkingGoogle by remember { mutableStateOf(false) }
  var showRegisterMemberDialog by remember { mutableStateOf(false) }
  var showManageChurchMembersSheet by remember { mutableStateOf(false) }
  var memberBeingEditedByAdmin by remember { mutableStateOf<Member?>(null) }

  val isAdmin = userProfile?.isAdminOrSuperAdmin == true
  val churchNameDisplay =
    member?.churchName?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchName?.takeIf { it.isNotBlank() }
      ?: "OmniChurch Pro"

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(bottom = 110.dp),
  ) {
    GlassTopBar(
      title = stringResource(R.string.profile_screen_title),
      onBackClick = onBackClick,
    )

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      PremiumGlassCard(
        cornerRadius = 22.dp,
        isHighlighted = true,
        contentPadding = PaddingValues(18.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          MemberAvatar3D(
            fullName = fullName,
            size = 64.dp,
            photoUrl = member?.effectivePhotoUrl ?: userProfile?.photoUrl,
            showVerifiedBadge = true,
          )
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = fullName.ifBlank { "Manm OmniChurch" },
              style = MaterialTheme.typography.headlineMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            val secondaryInfo =
              member?.googleEmail?.takeIf { it.isNotBlank() }
                ?: userProfile?.email?.takeIf { it.isNotBlank() }
                ?: member?.phone?.takeIf { it.isNotBlank() }
                ?: "Sesyon ID Matrikil + PIN"
            Text(
              text = secondaryInfo,
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite.copy(alpha = 0.85f),
            )
            if (member != null) {
              val matrikil = member.effectiveMatrikil
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier.clickable {
                    val clipboard =
                      context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                      ClipData.newPlainText("OmniChurch Matrikil", matrikil)
                    )
                    Toast.makeText(context, "ID Manm: $matrikil", Toast.LENGTH_SHORT)
                      .show()
                  },
              ) {
                Text(
                  text = "$churchNameDisplay • ID: $matrikil",
                  style = MaterialTheme.typography.labelMedium,
                  color = BrandGold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = null,
                  tint = BrandGold,
                  modifier = Modifier.size(14.dp),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        GlassButton(
          text = "Pataje Aplikasyon OmniChurch la",
          icon = Icons.Default.Share,
          isPrimaryNeon = false,
          onClick = { shareOmniChurchGeneralApp(context, churchNameDisplay) },
          modifier = Modifier.fillMaxWidth(),
          testTag = "profile_share_app_btn",
        )
      }

      if (member != null) {
        val matrikil = member.effectiveMatrikil
        PremiumGlassCard(
          cornerRadius = 22.dp,
          contentPadding = PaddingValues(16.dp),
        ) {
          Text(
            text = "Sekirite & Kont Lye (Opsyonèl)",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(10.dp))

          if (member.googleEmail.isNullOrBlank()) {
            GlassButton(
              text = "Lye yon kont Google (Opsyonèl)",
              subtitle = "Konekte ak Google sou nenpòt telefòn",
              icon = Icons.Default.Link,
              isPrimaryNeon = false,
              isLoading = isLinkingGoogle,
              onClick = {
                isLinkingGoogle = true
                launchOptionalGoogleSignIn(
                  context = activityContext,
                  credentialManager = credentialManager,
                  scope = scope,
                  onSuccess = { payload ->
                    isLinkingGoogle = false
                    onLinkGoogleAccount(payload)
                  },
                  onError = { err ->
                    isLinkingGoogle = false
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                  },
                )
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "profile_link_google_optional_btn",
            )
          } else {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x3300D878))
                  .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                  .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Kont Google lye: ${member.googleEmail}",
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = BrandGold,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Deblokaj Biyometrik (Anprent / Face ID)",
                  style = MaterialTheme.typography.titleSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Debloke sesyon ID $matrikil san retape PIN",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedGlassText,
                )
              }
            }
            Switch(
              checked = biometricEnabled,
              onCheckedChange = { wantEnabled ->
                if (wantEnabled && BiometricQuickUnlockHelper.isBiometricAvailable(context)) {
                  BiometricQuickUnlockHelper.promptBiometricUnlock(
                    context = activityContext,
                    title = "Aktive Anprent / Face ID",
                    subtitle = "Konfime idantite w pou lye ak ID $matrikil",
                    onSuccess = { onToggleBiometric(true) },
                    onError = { err ->
                      Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    },
                  )
                } else {
                  onToggleBiometric(wantEnabled)
                }
              },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = PureWhite,
                  checkedTrackColor = SuccessGreen,
                ),
              modifier = Modifier.testTag("profile_biometric_toggle_switch"),
            )
          }
        }
      }

      if (isAdmin && (onRegisterNewMember != null || onAdminUpdateMember != null)) {
        PremiumGlassCard(
          cornerRadius = 22.dp,
          accentColor = BrandGold,
          contentPadding = PaddingValues(16.dp),
        ) {
          Text(
            text = "Administrasyon Legliz — Jesyon Manm yo",
            style = MaterialTheme.typography.titleMedium,
            color = BrandGold,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Kòm Administratè legliz la, se ou ki responsab pou anrejistre nouvo manm epi mete ajou enfòmasyon pèsonèl manm legliz la.",
            style = MaterialTheme.typography.bodySmall,
            color = GlassWhite.copy(alpha = 0.85f),
          )
          Spacer(modifier = Modifier.height(10.dp))

          if (onRegisterNewMember != null) {
            GlassButton(
              text = "Anrejistre yon Manm (Matrikil + PIN)",
              icon = Icons.Default.PersonAdd,
              onClick = { showRegisterMemberDialog = true },
              modifier = Modifier.fillMaxWidth(),
              testTag = "admin_register_member_btn",
            )
          }

          if (onAdminUpdateMember != null) {
            Spacer(modifier = Modifier.height(8.dp))
            GlassButton(
              text = "Modifye Enfòmasyon Manm Legliz la (${churchMembers.size})",
              icon = Icons.Default.People,
              isPrimaryNeon = false,
              onClick = { showManageChurchMembersSheet = true },
              modifier = Modifier.fillMaxWidth(),
              testTag = "admin_manage_members_btn",
            )
          }
        }
      }

      PremiumGlassCard(
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = BlueNeon,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.profile_language_label),
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
        Spacer(modifier = Modifier.height(12.dp))

        val languages =
          listOf(
            "ht" to stringResource(R.string.profile_lang_ht),
            "fr" to stringResource(R.string.profile_lang_fr),
            "en" to stringResource(R.string.profile_lang_en),
            "es" to stringResource(R.string.profile_lang_es),
          )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          languages.forEach { (code, name) ->
            val isSelected = currentLanguageCode == code
            Box(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(
                    if (isSelected) ElectricBlue else Color(0x660B1D3D)
                  )
                  .border(
                    1.dp,
                    if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.25f),
                    RoundedCornerShape(14.dp),
                  )
                  .clickable { onLanguageChange(code) }
                  .padding(vertical = 10.dp, horizontal = 4.dp)
                  .testTag("lang_option_$code"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = name.substringBefore(" "),
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
              )
            }
          }
        }
      }

      if (member != null) {
        PremiumGlassCard(
          cornerRadius = 22.dp,
          contentPadding = PaddingValues(16.dp),
          modifier = Modifier.testTag("profile_member_personal_info_card"),
        ) {
          if (!isAdmin) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Enfòmasyon Pèsonèl Manm nan",
                style = MaterialTheme.typography.titleMedium,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                  Modifier.clip(RoundedCornerShape(50))
                    .background(Color(0x440D2249))
                    .border(1.dp, BrandGold.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("profile_readonly_badge"),
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Lekti Sèlman",
                  tint = BrandGold,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Lekti Sèlman",
                  style = MaterialTheme.typography.labelSmall,
                  color = BrandGold,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0x44091936))
                  .border(1.dp, ElectricBlue.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                  .padding(10.dp),
            ) {
              Text(
                text = "🔒 Enfòmasyon pèsonèl ou yo an mòd lekti sèlman. Se Administratè legliz ou a ki responsab pou modifye ak mete ajou enfòmasyon manm yo.",
                style = MaterialTheme.typography.bodySmall,
                color = GlassWhite.copy(alpha = 0.90f),
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ReadOnlyMemberFieldRow(
              label = stringResource(R.string.setup_member_fullname_label),
              value = member.effectiveFullName,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReadOnlyMemberFieldRow(
              label = "ID Matrikil Ofisyèl",
              value = member.effectiveMatrikil,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReadOnlyMemberFieldRow(
              label = "Legliz",
              value = churchNameDisplay,
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReadOnlyMemberFieldRow(
              label = stringResource(R.string.setup_member_department_label),
              value = member.department.ifBlank { "Adorasyon" },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReadOnlyMemberFieldRow(
              label = stringResource(R.string.setup_member_role_label),
              value = member.role.ifBlank { "Manm" },
            )
            if (!member.phone.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              ReadOnlyMemberFieldRow(
                label = "Telefòn Kontak",
                value = member.phone,
              )
            }
          } else {
            Text(
              text = "${stringResource(R.string.profile_edit_section)} (Admin)",
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassInputField(
              value = fullName,
              onValueChange = { fullName = it },
              label = stringResource(R.string.setup_member_fullname_label),
              testTag = "profile_edit_name_input",
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassInputField(
              value = department,
              onValueChange = { department = it },
              label = stringResource(R.string.setup_member_department_label),
              testTag = "profile_edit_dept_input",
            )
            Spacer(modifier = Modifier.height(10.dp))
            GlassInputField(
              value = role,
              onValueChange = { role = it },
              label = stringResource(R.string.setup_member_role_label),
              testTag = "profile_edit_role_input",
            )
            Spacer(modifier = Modifier.height(14.dp))
            GlassButton(
              text = stringResource(R.string.profile_save_changes),
              icon = Icons.Default.CheckCircle,
              enabled = fullName.isNotBlank(),
              onClick = {
                onSaveMemberChanges(fullName, department, role)
                Toast.makeText(
                    context,
                    context.getString(R.string.profile_saved_toast),
                    Toast.LENGTH_SHORT,
                  )
                  .show()
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "profile_save_btn",
            )
          }
        }
      }

      GlassButton(
        text = stringResource(R.string.auth_sign_out),
        icon = Icons.AutoMirrored.Filled.Logout,
        isPrimaryNeon = false,
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth(),
        testTag = "profile_sign_out_btn",
      )
    }
  }

  if (showManageChurchMembersSheet && isAdmin && onAdminUpdateMember != null) {
    GlassModalBottomSheet(
      onDismissRequest = { showManageChurchMembersSheet = false },
      title = "Manm Legliz la (${churchMembers.size})",
    ) {
      Text(
        text = "Chwazi yon manm pou modifye enfòmasyon pèsonèl li (Non, Tit, Depatman, Wòl, Telefòn):",
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(10.dp))

      if (churchMembers.isEmpty()) {
        Text(
          text = "Okenn manm poko chaje pou legliz sa a.",
          style = MaterialTheme.typography.bodyMedium,
          color = GlassWhite,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(churchMembers, key = { it.id.ifBlank { it.effectiveMatrikil } }) { itemMember ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0x660B1D3D))
                  .border(1.dp, GlassWhite.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                  .clickable {
                    memberBeingEditedByAdmin = itemMember
                  }
                  .padding(12.dp)
                  .testTag("admin_member_row_${itemMember.id}"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                MemberAvatar3D(
                  fullName = itemMember.effectiveFullName,
                  size = 40.dp,
                  photoUrl = itemMember.effectivePhotoUrl,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = itemMember.effectiveFullName,
                    style = MaterialTheme.typography.titleSmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "ID: ${itemMember.effectiveMatrikil} • ${itemMember.department.ifBlank { "Adorasyon" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandGold,
                  )
                }
              }

              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Modifye Manm",
                tint = BlueNeon,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }

  if (memberBeingEditedByAdmin != null && isAdmin && onAdminUpdateMember != null) {
    EditMemberBottomSheet(
      member = memberBeingEditedByAdmin!!,
      onDismissRequest = { memberBeingEditedByAdmin = null },
      onSave = { fullName, department, role, phone, titlePrefix ->
        onAdminUpdateMember(
          memberBeingEditedByAdmin!!.id,
          fullName,
          department,
          role,
          phone,
          titlePrefix,
        )
      },
    )
  }

  if (showRegisterMemberDialog && onRegisterNewMember != null) {
    var newMemberName by remember { mutableStateOf("") }
    var newMemberMatrikil by remember { mutableStateOf("") }
    var newMemberPin by remember { mutableStateOf("4827") }
    var newMemberPhone by remember { mutableStateOf("") }
    var newMemberDept by remember { mutableStateOf("Adorasyon") }
    var newMemberRole by remember { mutableStateOf("Manm Aktif") }

    GlassDialog(
      onDismissRequest = { showRegisterMemberDialog = false },
      title = "Anrejistre Manm (Matrikil + PIN)",
    ) {
      GlassInputField(
        value = newMemberName,
        onValueChange = { newMemberName = it },
        label = "Non Konplè Manm nan",
        placeholder = "Egz: Jean-David Pierre",
        testTag = "admin_reg_member_name",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberMatrikil,
        onValueChange = { newMemberMatrikil = it },
        label = "ID Matrikil (Kite vid pou jenere otomatikman)",
        placeholder = "Egz: CH001-JD4827",
        testTag = "admin_reg_member_matrikil",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberPin,
        onValueChange = { newMemberPin = it },
        label = "Kòd Sekirite (PIN Manm)",
        placeholder = "Egz: 4827",
        testTag = "admin_reg_member_pin",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberPhone,
        onValueChange = { newMemberPhone = it },
        label = "Nimewo Telefòn (Opsyonèl)",
        placeholder = "Egz: 50937001234",
        testTag = "admin_reg_member_phone",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberDept,
        onValueChange = { newMemberDept = it },
        label = "Depatman",
        testTag = "admin_reg_member_dept",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberRole,
        onValueChange = { newMemberRole = it },
        label = "Wòl nan Legliz la",
        testTag = "admin_reg_member_role",
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text = stringResource(R.string.common_cancel),
          isPrimaryNeon = false,
          onClick = { showRegisterMemberDialog = false },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text = "Anrejistre",
          enabled = newMemberName.isNotBlank() && newMemberPin.isNotBlank(),
          onClick = {
            onRegisterNewMember(
              newMemberName,
              newMemberMatrikil.takeIf { it.isNotBlank() },
              newMemberPin,
              newMemberPhone.takeIf { it.isNotBlank() },
              newMemberDept,
              newMemberRole,
            )
            showRegisterMemberDialog = false
          },
          modifier = Modifier.weight(1f),
          testTag = "admin_reg_member_submit_btn",
        )
      }
    }
  }
}

@Composable
private fun ReadOnlyMemberFieldRow(
  label: String,
  value: String,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0x550A1836))
        .border(1.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MutedGlassText,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyLarge,
      color = PureWhite,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
