package com.zoutiw.omnichurch.presentation.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Modern Minimalist Bottom Sheet pou kreyasyon ak jesyon Sèvis (Church Services).
 * Design anti-slop: 100% flat linear flow, san kat anndan kat, san bòdi lou,
 * avèk handle bar, background transparan lis, ak sticky action footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceBottomSheet(
  onDismissRequest: () -> Unit,
  churchMembers: List<Member> = emptyList(),
  initialService: ChurchService? = null,
  currentLanguageCode: String = "ht",
  onSubmit: (
    title: String,
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
    isOpenForAttendance: Boolean,
    sendReminder24h: Boolean,
    lateToleranceMinutes: Long,
    imageUrl: String?,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  val copy = remember(currentLanguageCode) { CreateServiceStrings.forLanguage(currentLanguageCode) }
  var allowProgrammaticClose by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { targetValue ->
      allowProgrammaticClose || targetValue != SheetValue.Hidden
    },
  )
  val closeSheet: () -> Unit = {
    allowProgrammaticClose = true
    onDismissRequest()
  }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val categories = copy.categories

  val todayDate = remember {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
  }

  val isEditing = initialService != null
  val isAttendanceLocked = remember(initialService) { initialService?.isPermanentlyLocked() == true }
  val editKey = initialService?.id ?: "new_service"
  val initialCategory = remember(editKey) {
    val rawCat = initialService?.category?.ifBlank { initialService.type }.orEmpty()
    if (categories.any { it.first == rawCat }) rawCat else "dimanche_matin"
  }

  var selectedCategory by remember(editKey) { mutableStateOf(initialCategory) }
  var title by remember(editKey) {
    mutableStateOf(initialService?.effectiveTitle ?: copy.defaultServiceTitle)
  }
  var date by remember(editKey) {
    mutableStateOf(initialService?.effectiveDateOnly?.ifBlank { todayDate } ?: todayDate)
  }
  val initialStartParsed = remember(editKey) {
    parseServiceTimeTo12Hour(
      raw = initialService?.startTime.orEmpty(),
      fallbackTime12 = "09:00",
      fallbackIsPm = false,
    )
  }
  val initialEndParsed = remember(editKey) {
    parseServiceTimeTo12Hour(
      raw = initialService?.endTime.orEmpty(),
      fallbackTime12 = "11:30",
      fallbackIsPm = false,
    )
  }
  var startTime by remember(editKey) { mutableStateOf(initialStartParsed.first) }
  var startIsPm by remember(editKey) { mutableStateOf(initialStartParsed.second) }
  var endTime by remember(editKey) { mutableStateOf(initialEndParsed.first) }
  var endIsPm by remember(editKey) { mutableStateOf(initialEndParsed.second) }
  var theme by remember(editKey) { mutableStateOf(initialService?.theme.orEmpty()) }
  var passage by remember(editKey) { mutableStateOf(initialService?.passage.orEmpty()) }
  var leader by remember(editKey) { mutableStateOf(initialService?.leader.orEmpty()) }
  var preacher by remember(editKey) { mutableStateOf(initialService?.preacher.orEmpty()) }
  var reader by remember(editKey) { mutableStateOf(initialService?.reader.orEmpty()) }
  var musicians by remember(editKey) { mutableStateOf(initialService?.musicians.orEmpty()) }
  var ushers by remember(editKey) { mutableStateOf(initialService?.ushers.orEmpty()) }
  var isOpenForAttendance by remember(editKey) {
    mutableStateOf(
      if (initialService != null) {
        initialService.isOpenForAttendance && !initialService.isBeforeStart()
      } else {
        false
      }
    )
  }
  var sendReminder24h by remember(editKey) {
    mutableStateOf(initialService?.sendReminder24h ?: false)
  }
  var lateToleranceMinutesStr by remember(editKey) {
    mutableStateOf((initialService?.lateToleranceMinutes ?: 15L).toString())
  }
  var imageUrl by remember(editKey) { mutableStateOf(initialService?.effectiveImageUrl.orEmpty()) }
  var isCompressingImage by remember { mutableStateOf(false) }

  val canonicalStartForCheck = remember(startTime, startIsPm) {
    convert12HourToCanonical24Hour(startTime, startIsPm)
  }
  val isBeforeScheduledStart = remember(date, canonicalStartForCheck) {
    ChurchService(date = date, startTime = canonicalStartForCheck).isBeforeStart()
  }
  val startPreviewForGuard = remember(startTime, startIsPm) {
    formatDisplay12HourLabel(startTime, startIsPm)
  }

  var showAdvancedRoles by remember(editKey) {
    mutableStateOf(
      initialService != null &&
        (!initialService.reader.isNullOrBlank() ||
          !initialService.musicians.isNullOrBlank() ||
          !initialService.ushers.isNullOrBlank())
    )
  }
  var activeRolePicker by remember { mutableStateOf<ServiceRolePickerTarget?>(null) }

  val selectableMembers = remember(churchMembers) {
    churchMembers
      .filter { !it.id.startsWith("ADMIN-", ignoreCase = true) && it.effectiveFullName.isNotBlank() }
      .distinctBy { it.id.ifBlank { it.effectiveMatrikil.ifBlank { it.effectiveFullName } } }
      .sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
  }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
      if (uri != null) {
        scope.launch {
          isCompressingImage = true
          val compressedRes =
            ImageCompressor.compressUriToBase64(
              context = context,
              uri = uri,
              maxDimension = 900,
              quality = 80,
            )
          isCompressingImage = false
          compressedRes
            .onSuccess { dataUrl -> imageUrl = dataUrl }
            .onFailure { err ->
              Toast.makeText(
                  context,
                  err.localizedMessage ?: copy.imageProcessError,
                  Toast.LENGTH_SHORT,
                )
                .show()
            }
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = {
      // Bloke fèmti aksidantèl sou swipdown oswa klik deyò; sèlman bouton 'X' oswa soumèt ki fèmen l
    },
    sheetState = sheetState,
    properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    containerColor = Color(0xFF0F172A), // Slate-900
    contentColor = PureWhite,
    dragHandle = null,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = modifier.imePadding(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
    ) {
      CreateServiceSheetHeader(
        isEditing = isEditing,
        copy = copy,
        onCloseClick = closeSheet,
      )

      // Scrollable Linear Flow (Zero nested cards)
      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        CreateServiceCategoryGrid(
          categories = categories,
          selectedCategory = selectedCategory,
          copy = copy,
          onSelectCategory = { selectedCategory = it },
        )

        // 2. Tit & Tèm
        SectionTitle(title = copy.generalInfoSection)
        MinimalTextField(
          value = title,
          onValueChange = { title = it },
          label = copy.titleLabel,
          placeholder = copy.titlePlaceholder,
          leadingIcon = Icons.Default.Church,
          testTag = "service_input_title",
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MinimalTextField(
            value = theme,
            onValueChange = { theme = it },
            label = copy.themeLabel,
            placeholder = copy.themePlaceholder,
            modifier = Modifier.weight(1f),
            testTag = "service_input_theme",
          )
          MinimalTextField(
            value = passage,
            onValueChange = { passage = it },
            label = copy.passageLabel,
            placeholder = copy.passagePlaceholder,
            leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
            modifier = Modifier.weight(1f),
            testTag = "service_input_passage",
          )
        }

        // 3. Dat & Orè Sèvis la
        CreateServiceScheduleSection(
          date = date,
          onDateChange = { date = it },
          startTime = startTime,
          startIsPm = startIsPm,
          onStartTimeChange = { t, pm ->
            startTime = t
            startIsPm = pm
          },
          endTime = endTime,
          endIsPm = endIsPm,
          onEndTimeChange = { t, pm ->
            endTime = t
            endIsPm = pm
          },
          copy = copy,
          languageCode = currentLanguageCode,
        )

        // 4. Lidè & Predikatè (Seleksyon Manm rapid oswa Envite)
        SectionTitle(title = copy.officialsSection)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MemberRoleSelectorField(
            value = leader,
            label = copy.leaderLabel,
            placeholder = copy.chooseOrGuestPlaceholder,
            leadingIcon = Icons.Default.Person,
            onClick = { activeRolePicker = ServiceRolePickerTarget.LEADER },
            modifier = Modifier.weight(1f),
            testTag = "service_input_leader",
          )
          MemberRoleSelectorField(
            value = preacher,
            label = copy.preacherLabel,
            placeholder = copy.chooseOrGuestPlaceholder,
            leadingIcon = Icons.Default.Person,
            onClick = { activeRolePicker = ServiceRolePickerTarget.PREACHER },
            modifier = Modifier.weight(1f),
            testTag = "service_input_preacher",
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvancedRoles = !showAdvancedRoles }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (showAdvancedRoles) copy.hideAdvancedRoles else copy.showAdvancedRoles,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF38BDF8), // Sky-400
            fontWeight = FontWeight.SemiBold,
          )
        }

        AnimatedVisibility(visible = showAdvancedRoles) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MemberRoleSelectorField(
              value = reader,
              label = copy.readerLabel,
              placeholder = copy.readerPlaceholder,
              leadingIcon = Icons.Default.Person,
              onClick = { activeRolePicker = ServiceRolePickerTarget.READER },
              testTag = "service_input_reader",
            )
            MinimalTextField(
              value = musicians,
              onValueChange = { musicians = it },
              label = copy.musiciansLabel,
              placeholder = copy.musiciansPlaceholder,
              leadingIcon = Icons.Default.MusicNote,
              trailingActionIcon = Icons.Default.GroupAdd,
              trailingActionDescription = copy.musiciansActionCd,
              onTrailingActionClick = { activeRolePicker = ServiceRolePickerTarget.MUSICIANS },
              testTag = "service_input_musicians",
            )
            MinimalTextField(
              value = ushers,
              onValueChange = { ushers = it },
              label = copy.ushersLabel,
              placeholder = copy.ushersPlaceholder,
              trailingActionIcon = Icons.Default.GroupAdd,
              trailingActionDescription = copy.ushersActionCd,
              onTrailingActionClick = { activeRolePicker = ServiceRolePickerTarget.USHERS },
              testTag = "service_input_ushers",
            )
          }
        }

        // 5. Konfigirasyon Prezans, Notifikasyon & Imaj
        CreateServiceAttendanceAndMediaSection(
          isAttendanceLocked = isAttendanceLocked,
          isBeforeScheduledStart = isBeforeScheduledStart,
          startTimePreview = startPreviewForGuard,
          languageCode = currentLanguageCode,
          isOpenForAttendance = isOpenForAttendance && !isBeforeScheduledStart,
          onOpenForAttendanceChange = { wantOpen ->
            isOpenForAttendance = if (isBeforeScheduledStart) false else wantOpen
          },
          sendReminder24h = sendReminder24h,
          onSendReminder24hChange = { sendReminder24h = it },
          lateToleranceMinutesStr = lateToleranceMinutesStr,
          onLateToleranceChange = { lateToleranceMinutesStr = it },
          title = title,
          imageUrl = imageUrl,
          onImageUrlChange = { imageUrl = it },
          copy = copy,
        )
      }

      // Sticky Action Footer
      CreateServiceStickyFooter(
        isEditing = isEditing,
        isCompressingImage = isCompressingImage,
        imageUrl = imageUrl,
        canSubmit = title.isNotBlank() && date.isNotBlank() && !isCompressingImage,
        copy = copy,
        onPickGalleryImage = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        onSubmitClick = {
          val tol = lateToleranceMinutesStr.toLongOrNull() ?: 15L
          val canonicalStartTime = convert12HourToCanonical24Hour(startTime, startIsPm)
          val canonicalEndTime = convert12HourToCanonical24Hour(endTime, endIsPm)
          val safeIsOpen = isOpenForAttendance && !isBeforeScheduledStart && !isAttendanceLocked
          onSubmit(
            title,
            selectedCategory,
            date,
            canonicalStartTime,
            canonicalEndTime,
            theme.takeIf { it.isNotBlank() },
            passage.takeIf { it.isNotBlank() },
            leader.takeIf { it.isNotBlank() },
            preacher.takeIf { it.isNotBlank() },
            reader.takeIf { it.isNotBlank() },
            musicians.takeIf { it.isNotBlank() },
            ushers.takeIf { it.isNotBlank() },
            safeIsOpen,
            sendReminder24h,
            tol,
            imageUrl.trim().takeIf { it.isNotBlank() },
          )
          closeSheet()
        },
      )
    }
  }

  activeRolePicker?.let { target ->
    val currentVal = when (target) {
      ServiceRolePickerTarget.LEADER -> leader
      ServiceRolePickerTarget.PREACHER -> preacher
      ServiceRolePickerTarget.READER -> reader
      ServiceRolePickerTarget.MUSICIANS -> musicians
      ServiceRolePickerTarget.USHERS -> ushers
    }
    CreateServiceRolePickerOverlay(
      target = target,
      selectableMembers = selectableMembers,
      currentVal = currentVal,
      copy = copy,
      onApplyRoleSelection = { chosenTarget, chosenName ->
        when (chosenTarget) {
          ServiceRolePickerTarget.LEADER -> leader = chosenName
          ServiceRolePickerTarget.PREACHER -> preacher = chosenName
          ServiceRolePickerTarget.READER -> reader = chosenName
          ServiceRolePickerTarget.MUSICIANS -> musicians = toggleMultiRoleValue(musicians, chosenName)
          ServiceRolePickerTarget.USHERS -> ushers = toggleMultiRoleValue(ushers, chosenName)
        }
      },
      onDismiss = { activeRolePicker = null },
    )
  }
}
