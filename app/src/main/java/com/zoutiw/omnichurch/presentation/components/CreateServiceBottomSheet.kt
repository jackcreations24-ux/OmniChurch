package com.zoutiw.omnichurch.presentation.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.formatCreoleDateWithDay
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Modern Minimalist Bottom Sheet pou kreyasyon ak jesyon Sèvis (Church Services).
 * Design anti-slop: 100% flat linear flow, san kat anndan kat, san bòdi lou,
 * avèk handle bar, background transparan lis, ak sticky action footer.
 */
private enum class ServiceRolePickerTarget(
  val modalTitle: String,
  val allowAppendMultiple: Boolean = false,
) {
  LEADER(modalTitle = "Chwazi Dirijan Sèvis"),
  PREACHER(modalTitle = "Chwazi Predikatè"),
  READER(modalTitle = "Chwazi Lektè Vèsè"),
  MUSICIANS(modalTitle = "Chwazi Mizisyen / Koral", allowAppendMultiple = true),
  USHERS(modalTitle = "Chwazi Akèy / Pwotokòl", allowAppendMultiple = true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceBottomSheet(
  onDismissRequest: () -> Unit,
  churchMembers: List<Member> = emptyList(),
  initialService: ChurchService? = null,
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

  val categories = listOf(
    "dimanche_matin" to "Dimanch Maten",
    "dimanche_soir" to "Dimanch Swa",
    "etude_biblique" to "Etid Biblik",
    "priere" to "Reyinyon Lapriyè",
    "jeunesse" to "Reyinyon Jenès",
    "special" to "Sèvis Espesyal",
  )

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
    mutableStateOf(initialService?.effectiveTitle ?: "Sèvis Adorasyon & Louwanj")
  }
  var date by remember(editKey) {
    mutableStateOf(initialService?.effectiveDateOnly?.ifBlank { todayDate } ?: todayDate)
  }
  var startTime by remember(editKey) {
    mutableStateOf(initialService?.startTime?.ifBlank { "09:00" } ?: "09:00")
  }
  var endTime by remember(editKey) {
    mutableStateOf(initialService?.endTime?.ifBlank { "11:30" } ?: "11:30")
  }
  var theme by remember(editKey) { mutableStateOf(initialService?.theme.orEmpty()) }
  var passage by remember(editKey) { mutableStateOf(initialService?.passage.orEmpty()) }
  var leader by remember(editKey) { mutableStateOf(initialService?.leader.orEmpty()) }
  var preacher by remember(editKey) { mutableStateOf(initialService?.preacher.orEmpty()) }
  var reader by remember(editKey) { mutableStateOf(initialService?.reader.orEmpty()) }
  var musicians by remember(editKey) { mutableStateOf(initialService?.musicians.orEmpty()) }
  var ushers by remember(editKey) { mutableStateOf(initialService?.ushers.orEmpty()) }
  var isOpenForAttendance by remember(editKey) {
    mutableStateOf(initialService?.isOpenForAttendance ?: true)
  }
  var sendReminder24h by remember(editKey) {
    mutableStateOf(initialService?.sendReminder24h ?: false)
  }
  var lateToleranceMinutesStr by remember(editKey) {
    mutableStateOf((initialService?.lateToleranceMinutes ?: 15L).toString())
  }
  var imageUrl by remember(editKey) { mutableStateOf(initialService?.effectiveImageUrl.orEmpty()) }
  var isCompressingImage by remember { mutableStateOf(false) }

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
                  err.localizedMessage ?: "Pa ka trete imaj sa a. Tanpri eseye yon lòt foto.",
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
      // Header Section
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isEditing) "Modifye Sèvis la" else "Kreye yon Nouvo Sèvis",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              if (isEditing) "Mizajou imedyat pou tout manm & akèy yo"
              else "Senkwonizasyon total ak tablodbò OmniChurch",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8), // Slate-400
          )
        }
        IconButton(
          onClick = closeSheet,
          modifier = Modifier
            .size(48.dp)
            .testTag("service_bottom_sheet_close_btn"),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Fèmen",
            tint = Color(0xFFCBD5E1),
          )
        }
      }

      // Scrollable Linear Flow (Zero nested cards)
      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. Kategori Sèvis
        SectionTitle(title = "Kategori Sèvis")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          categories.take(3).forEach { (catKey, catLabel) ->
            CategoryChip(
              label = catLabel,
              isSelected = selectedCategory == catKey,
              onClick = { selectedCategory = catKey },
              modifier = Modifier.weight(1f),
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          categories.drop(3).forEach { (catKey, catLabel) ->
            CategoryChip(
              label = catLabel,
              isSelected = selectedCategory == catKey,
              onClick = { selectedCategory = catKey },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // 2. Tit & Tèm
        SectionTitle(title = "Enfòmasyon Jeneral")
        MinimalTextField(
          value = title,
          onValueChange = { title = it },
          label = "Tit Sèvis la *",
          placeholder = "Egz: Sèvis Adorasyon & Louwanj",
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
            label = "Tèm Sèvis (Opsyonèl)",
            placeholder = "Egz: Lafwa ki deplase mòn",
            modifier = Modifier.weight(1f),
            testTag = "service_input_theme",
          )
          MinimalTextField(
            value = passage,
            onValueChange = { passage = it },
            label = "Vèsè / Pasaj (Opsyonèl)",
            placeholder = "Egz: Ebre 11:1-6",
            leadingIcon = Icons.Default.MenuBook,
            modifier = Modifier.weight(1f),
            testTag = "service_input_passage",
          )
        }

        // 3. Dat & Orè Sèvis la (Youn sou lòt, fòma natirèl an kreyòl ak non jou yo)
        SectionTitle(title = "Dat & Orè Sèvis la")
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // 3a. Dat Sèvis la
          val openDatePicker = {
            val cal = Calendar.getInstance()
            val parts = date.split("-")
            if (parts.size == 3) {
              val y = parts[0].toIntOrNull()
              val m = parts[1].toIntOrNull()
              val d = parts[2].toIntOrNull()
              if (y != null && m != null && d != null && m in 1..12) {
                cal.set(y, m - 1, d)
              }
            }
            DatePickerDialog(
              context,
              { _, year, month, dayOfMonth ->
                date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
              },
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH),
              cal.get(Calendar.DAY_OF_MONTH),
            ).show()
          }

          MinimalTextField(
            value = date,
            onValueChange = { date = it },
            label = "Dat Sèvis la (YYYY-MM-DD) *",
            placeholder = "YYYY-MM-DD (Egz: 2026-11-22)",
            leadingIcon = Icons.Default.CalendarMonth,
            trailingActionIcon = Icons.Default.CalendarMonth,
            trailingActionDescription = "Chwazi nan Kalandriye",
            onTrailingActionClick = openDatePicker,
            keyboardType = KeyboardType.Number,
            modifier = Modifier.fillMaxWidth(),
            testTag = "service_input_date",
          )

          // Preview dat an kreyòl ak non jou a
          val naturalCreoleDate = remember(date) { formatCreoleDateWithDay(date) }
          if (naturalCreoleDate.isNotBlank() && naturalCreoleDate != date) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                  .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = naturalCreoleDate,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE0F2FE),
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          // 3b. Lè Kòmanse
          val openStartTimePicker = {
            val cal = Calendar.getInstance()
            val parts = startTime.split(":", "h")
            val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
            val m = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0
            TimePickerDialog(
              context,
              { _, hourOfDay, minute ->
                startTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
              },
              h,
              m,
              true,
            ).show()
          }

          MinimalTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = "Lè Kòmanse (HH:mm)",
            placeholder = "Egz: 09:00",
            leadingIcon = Icons.Default.AccessTime,
            trailingActionIcon = Icons.Default.Schedule,
            trailingActionDescription = "Chwazi Lè Kòmanse",
            onTrailingActionClick = openStartTimePicker,
            modifier = Modifier.fillMaxWidth(),
            testTag = "service_input_start_time",
          )

          // 3c. Lè Fini
          val openEndTimePicker = {
            val cal = Calendar.getInstance()
            val parts = endTime.split(":", "h")
            val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 11
            val m = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 30
            TimePickerDialog(
              context,
              { _, hourOfDay, minute ->
                endTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
              },
              h,
              m,
              true,
            ).show()
          }

          MinimalTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = "Lè Fini (HH:mm)",
            placeholder = "Egz: 11:30",
            leadingIcon = Icons.Default.AccessTime,
            trailingActionIcon = Icons.Default.Schedule,
            trailingActionDescription = "Chwazi Lè Fini",
            onTrailingActionClick = openEndTimePicker,
            modifier = Modifier.fillMaxWidth(),
            testTag = "service_input_end_time",
          )
        }

        // 4. Lidè & Predikatè (Seleksyon Manm rapid oswa Envite)
        SectionTitle(title = "Ofisyan & Responsab")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MemberRoleSelectorField(
            value = leader,
            label = "Dirijan Sèvis",
            placeholder = "Chwazi / Envite",
            leadingIcon = Icons.Default.Person,
            onClick = { activeRolePicker = ServiceRolePickerTarget.LEADER },
            modifier = Modifier.weight(1f),
            testTag = "service_input_leader",
          )
          MemberRoleSelectorField(
            value = preacher,
            label = "Predikatè",
            placeholder = "Chwazi / Envite",
            leadingIcon = Icons.Default.Person,
            onClick = { activeRolePicker = ServiceRolePickerTarget.PREACHER },
            modifier = Modifier.weight(1f),
            testTag = "service_input_preacher",
          )
        }

        // Toggle plis responsab (Mizisyen, Akèy, Lektè)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvancedRoles = !showAdvancedRoles }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (showAdvancedRoles) "Kache wòl siplemantè yo ▲" else "Ajoute lòt wòl (Mizisyen, Akèy, Lektè) ▼",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF38BDF8), // Sky-400
            fontWeight = FontWeight.SemiBold,
          )
        }

        AnimatedVisibility(visible = showAdvancedRoles) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MemberRoleSelectorField(
              value = reader,
              label = "Lektè Vèsè",
              placeholder = "Chwazi yon manm oswa Envite",
              leadingIcon = Icons.Default.Person,
              onClick = { activeRolePicker = ServiceRolePickerTarget.READER },
              testTag = "service_input_reader",
            )
            MinimalTextField(
              value = musicians,
              onValueChange = { musicians = it },
              label = "Mizisyen / Koral",
              placeholder = "Egz: Koral Dawout oswa chwazi manm",
              leadingIcon = Icons.Default.MusicNote,
              trailingActionIcon = Icons.Default.GroupAdd,
              trailingActionDescription = "Chwazi manm pou mizisyen",
              onTrailingActionClick = { activeRolePicker = ServiceRolePickerTarget.MUSICIANS },
              testTag = "service_input_musicians",
            )
            MinimalTextField(
              value = ushers,
              onValueChange = { ushers = it },
              label = "Akèy / Pwotokòl",
              placeholder = "Egz: Ekip Pwotokòl #1 oswa chwazi manm",
              trailingActionIcon = Icons.Default.GroupAdd,
              trailingActionDescription = "Chwazi manm pou akèy",
              onTrailingActionClick = { activeRolePicker = ServiceRolePickerTarget.USHERS },
              testTag = "service_input_ushers",
            )
          }
        }

        // 5. Konfigirasyon Prezans & Notifikasyon
        SectionTitle(title = "Konfigirasyon Prezans & Notifikasyon")

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Ouvri pou Eskanè Prezans",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text =
                if (isAttendanceLocked)
                  "Sèvis sa a deja fini ak fèmen; eskanè a pa ka re-ouvri ankò."
                else "Pèmèt manm eskane QR code yo pou anrejistre prezans",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          Switch(
            checked = isOpenForAttendance,
            enabled = !isAttendanceLocked,
            onCheckedChange = { isOpenForAttendance = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = PureWhite,
              checkedTrackColor = SuccessGreen,
              uncheckedThumbColor = Color(0xFF64748B),
              uncheckedTrackColor = Color(0xFF1E293B),
            ),
            modifier = Modifier.testTag("service_toggle_open_attendance"),
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.5f))
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Voye Rapèl 24h Avan",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Notifikasyon otomatik pou manm legliz la anvan sèvis la",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          Switch(
            checked = sendReminder24h,
            onCheckedChange = { sendReminder24h = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = PureWhite,
              checkedTrackColor = Color(0xFFF59E0B),
              uncheckedThumbColor = Color(0xFF64748B),
              uncheckedTrackColor = Color(0xFF1E293B),
            ),
            modifier = Modifier.testTag("service_toggle_reminder"),
          )
        }

        MinimalTextField(
          value = lateToleranceMinutesStr,
          onValueChange = { lateToleranceMinutesStr = it },
          label = "Tolerans Reta (Minit)",
          placeholder = "15",
          keyboardType = KeyboardType.Number,
          testTag = "service_input_tolerance",
        )

        // 6. Imaj / Afich Sèvis (Lyen URL Opsyonèl & Apèsi — Enpòtasyon galri a nan ba anba a)
        SectionTitle(title = "Imaj / Afich Sèvis (Potre oswa Peyizaj)")
        MinimalTextField(
          value = if (imageUrl.startsWith("data:image")) "" else imageUrl,
          onValueChange = { imageUrl = it },
          label =
            if (imageUrl.startsWith("data:image"))
              "Foto Galri Enpòte ✓ (Oswa kole yon lyen URL)"
            else "Lyen URL Afich / Imaj (Opsyonèl)",
          placeholder = "https://...",
          leadingIcon = Icons.Default.Image,
          trailingActionIcon = if (imageUrl.isNotBlank()) Icons.Default.DeleteOutline else null,
          trailingActionDescription = "Retire Imaj la",
          onTrailingActionClick = { imageUrl = "" },
          testTag = "service_input_image_url",
        )

        if (imageUrl.isNotBlank()) {
          AnnouncementMediaShowcase(
            imageSource = imageUrl,
            title = title.ifBlank { "Afich Sèvis" },
            announcementId = "preview_srv",
            onImageClick = {},
            modifier = Modifier.testTag("service_image_preview"),
          )
        }
      }

      // Sticky Action Footer (Enpòte Imaj nan plas bouton Anile + Valider & Senkwonize)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0F172A))
          .border(width = 1.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(0.dp))
          .padding(horizontal = 20.dp, vertical = 14.dp)
          .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          enabled = !isCompressingImage,
          border = BorderStroke(
            width = 1.dp,
            color = if (imageUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.7f) else Color(0xFF38BDF8).copy(alpha = 0.55f),
          ),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (imageUrl.isNotBlank()) SuccessGreen.copy(alpha = 0.14f) else Color(0xFF1E293B),
            contentColor = PureWhite,
          ),
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp),
          modifier = Modifier
            .height(48.dp)
            .testTag("service_pick_gallery_image_btn"),
        ) {
          if (isCompressingImage) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = Color(0xFF38BDF8),
            )
          } else {
            Icon(
              imageVector = if (imageUrl.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.AddPhotoAlternate,
              contentDescription = "Enpòte Imaj",
              tint = if (imageUrl.isNotBlank()) SuccessGreen else Color(0xFF38BDF8),
              modifier = Modifier.size(18.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = when {
              isCompressingImage -> "Ap trete..."
              imageUrl.isNotBlank() -> "Imaj ✓"
              else -> "Enpòte Imaj"
            },
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
          )
        }

        Button(
          onClick = {
            val tol = lateToleranceMinutesStr.toLongOrNull() ?: 15L
            onSubmit(
              title,
              selectedCategory,
              date,
              startTime,
              endTime,
              theme.takeIf { it.isNotBlank() },
              passage.takeIf { it.isNotBlank() },
              leader.takeIf { it.isNotBlank() },
              preacher.takeIf { it.isNotBlank() },
              reader.takeIf { it.isNotBlank() },
              musicians.takeIf { it.isNotBlank() },
              ushers.takeIf { it.isNotBlank() },
              isOpenForAttendance,
              sendReminder24h,
              tol,
              imageUrl.trim().takeIf { it.isNotBlank() },
            )
            closeSheet()
          },
          enabled = title.isNotBlank() && date.isNotBlank() && !isCompressingImage,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF59E0B), // Amber-500
            contentColor = Color(0xFF0F172A), // Slate-950
            disabledContainerColor = Color(0xFF334155),
            disabledContentColor = Color(0xFF64748B),
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("service_bottom_sheet_submit_btn"),
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isEditing) "Anrejistre Modifikasyon" else "Valider & Senkwonize",
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }
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
    val orderedMembers = remember(selectableMembers, target) {
      if (target == ServiceRolePickerTarget.USHERS) {
        selectableMembers.sortedWith(
          compareByDescending<Member> { it.isUsherRole }
            .thenBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
        )
      } else {
        selectableMembers
      }
    }
    val toggleMultiValue: (String, String) -> String = { existing, chosen ->
      if (chosen.isBlank()) {
        ""
      } else {
        val currentTokens = existing.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val matchIndex = currentTokens.indexOfFirst { it.equals(chosen, ignoreCase = true) }
        if (matchIndex >= 0) {
          currentTokens.filterIndexed { index, _ -> index != matchIndex }.joinToString(", ")
        } else {
          (currentTokens + chosen).joinToString(", ")
        }
      }
    }
    ServiceMemberPickerDialog(
      title = target.modalTitle,
      currentValue = currentVal,
      members = orderedMembers,
      prioritizeUshers = target == ServiceRolePickerTarget.USHERS,
      helperSubtitle =
        if (target == ServiceRolePickerTarget.USHERS) {
          "Manm Akèy ou chwazi la a ap gen dwa eskane prezans tanporèman pou sèvis sa a."
        } else null,
      onSelectValue = { chosenName ->
        when (target) {
          ServiceRolePickerTarget.LEADER -> leader = chosenName
          ServiceRolePickerTarget.PREACHER -> preacher = chosenName
          ServiceRolePickerTarget.READER -> reader = chosenName
          ServiceRolePickerTarget.MUSICIANS -> musicians = toggleMultiValue(musicians, chosenName)
          ServiceRolePickerTarget.USHERS -> ushers = toggleMultiValue(ushers, chosenName)
        }
        activeRolePicker = null
      },
      onDismiss = { activeRolePicker = null },
    )
  }
}

@Composable
internal fun SectionTitle(title: String) {
  Text(
    text = title.uppercase(Locale.getDefault()),
    style = MaterialTheme.typography.labelSmall.copy(
      fontSize = 11.sp,
      letterSpacing = 1.sp,
      fontWeight = FontWeight.Bold,
    ),
    color = Color(0xFF94A3B8), // Slate-400
  )
}

@Composable
internal fun CategoryChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(
        if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.22f)
        else Color(0xFF1E293B).copy(alpha = 0.6f)
      )
      .border(
        width = 1.dp,
        color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155).copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
      )
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 6.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) Color(0xFFFBBF24) else Color(0xFFCBD5E1),
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
    )
  }
}

@Composable
internal fun MemberRoleSelectorField(
  value: String,
  label: String,
  placeholder: String,
  leadingIcon: ImageVector? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier.fillMaxWidth(),
  testTag: String = "",
) {
  val isGuest = value.contains("Envite", ignoreCase = true)
  Box(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      readOnly = true,
      label = { Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      placeholder = {
        Text(
          text = placeholder,
          color = Color(0xFF64748B),
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      },
      leadingIcon = leadingIcon?.let {
        {
          Icon(
            imageVector = if (isGuest) Icons.Default.Badge else it,
            contentDescription = null,
            tint = if (isGuest) Color(0xFFF59E0B) else Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp),
          )
        }
      },
      trailingIcon = {
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = label,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(20.dp),
        )
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.72f),
        unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.55f),
        focusedBorderColor = Color(0xFFF59E0B).copy(alpha = 0.7f),
        unfocusedBorderColor = if (value.isNotBlank()) Color(0xFF38BDF8).copy(alpha = 0.55f) else Color(0xFF334155).copy(alpha = 0.6f),
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedLabelColor = Color(0xFFFBBF24),
        unfocusedLabelColor = Color(0xFF94A3B8),
      ),
      modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .padding(top = 6.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
    )
  }
}

@Composable
private fun ServiceMemberPickerDialog(
  title: String,
  currentValue: String,
  members: List<Member>,
  onSelectValue: (String) -> Unit,
  onDismiss: () -> Unit,
  prioritizeUshers: Boolean = false,
  helperSubtitle: String? = null,
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredMembers = remember(members, searchQuery) {
    val q = searchQuery.trim().lowercase(Locale.getDefault())
    if (q.isEmpty()) {
      members
    } else {
      members.filter { m ->
        m.effectiveFullName.lowercase(Locale.getDefault()).contains(q) ||
          m.effectiveMatrikil.lowercase(Locale.getDefault()).contains(q) ||
          m.department.lowercase(Locale.getDefault()).contains(q) ||
          m.role.lowercase(Locale.getDefault()).contains(q)
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = Color(0xFF0F172A),
      border = BorderStroke(1.dp, Color(0xFF334155)),
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .heightIn(min = 300.dp, max = 560.dp)
        .imePadding(),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = helperSubtitle ?: "${members.size} manm disponib • oswa mete Envite",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          if (currentValue.isNotBlank()) {
            TextButton(onClick = { onSelectValue("") }) {
              Text(
                text = "Vid",
                color = Color(0xFFF87171),
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fèmen",
              tint = Color(0xFFCBD5E1),
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Guest ("Envite") Action Strip
        val trimmedQuery = searchQuery.trim()
        val guestLabelToAssign =
          if (trimmedQuery.isNotEmpty()) "$trimmedQuery (Envite)" else "Envite"

        Surface(
          onClick = { onSelectValue(guestLabelToAssign) },
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFFF59E0B).copy(alpha = 0.14f),
          border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("picker_select_guest_btn"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.PersonAddAlt1,
                contentDescription = null,
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(20.dp),
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text =
                  if (trimmedQuery.isNotEmpty())
                    "Mete \"$trimmedQuery\" kòm Envite"
                  else "Envite (Moun ki pa manm legliz la)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  if (trimmedQuery.isNotEmpty())
                    "Klike pou anrejistre envite ekstèn sa a"
                  else "Klike pou mete Envite oswa tape non li anba a",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
              )
            }
            if (currentValue.equals("Envite", ignoreCase = true)) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Search Input
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = "Chèche manm oswa tape non envite...",
              color = Color(0xFF64748B),
              style = MaterialTheme.typography.bodySmall,
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(18.dp),
            )
          },
          trailingIcon = if (searchQuery.isNotBlank()) {
            {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Efase rechèch",
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          } else null,
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.7f),
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedBorderColor = Color(0xFF334155),
            focusedTextColor = PureWhite,
            unfocusedTextColor = PureWhite,
            cursorColor = Color(0xFF38BDF8),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("picker_member_search_input"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredMembers.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text =
                  if (members.isEmpty()) "Pa gen lis manm ki chaje pou legliz la."
                  else "Okenn manm pa koresponn ak \"$trimmedQuery\".",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Ou ka itilize bouton Envite anwo a dirèkteman.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B),
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            items(
              items = filteredMembers,
              key = { it.id.ifBlank { it.effectiveMatrikil.ifBlank { it.effectiveFullName } } },
            ) { mem ->
              val isSelected =
                currentValue.equals(mem.effectiveFullName, ignoreCase = true) ||
                  currentValue.split(",").any { it.trim().equals(mem.effectiveFullName, ignoreCase = true) }
              val initials = remember(mem.effectiveFullName) {
                mem.effectiveFullName
                  .split(" ")
                  .filter { it.isNotBlank() }
                  .take(2)
                  .joinToString("") { it.first().uppercaseChar().toString() }
                  .ifBlank { "M" }
              }

              Surface(
                onClick = { onSelectValue(mem.effectiveFullName) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.16f) else Color(0xFF1E293B).copy(alpha = 0.55f),
                border = BorderStroke(
                  width = 1.dp,
                  color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.65f) else Color(0xFF334155).copy(alpha = 0.45f),
                ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  val avatarUrl = mem.effectivePhotoUrl
                  if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                      model = avatarUrl,
                      contentDescription = mem.effectiveFullName,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                          Brush.linearGradient(
                            colors = listOf(Color(0xFF0284C7), Color(0xFF2563EB))
                          )
                        ),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = initials,
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }

                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = mem.effectiveFullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                      )
                      if (prioritizeUshers && mem.isUsherRole) {
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF0EA5E9).copy(alpha = 0.22f))
                            .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                          Text(
                            text = "Akèy",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                          )
                        }
                      }
                    }
                    val metaParts = buildList {
                      if (mem.department.isNotBlank()) add(mem.department)
                      if (mem.effectiveMatrikil.isNotBlank()) add(mem.effectiveMatrikil)
                      if (mem.role.isNotBlank() && !mem.role.equals("member", ignoreCase = true)) {
                        add(mem.role.replaceFirstChar { it.uppercase() })
                      }
                    }
                    if (metaParts.isNotEmpty()) {
                      Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }

                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Chwazi",
                      tint = Color(0xFF38BDF8),
                      modifier = Modifier.size(20.dp),
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
}

@Composable
internal fun MinimalTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String = "",
  leadingIcon: ImageVector? = null,
  trailingActionIcon: ImageVector? = null,
  trailingActionDescription: String? = null,
  onTrailingActionClick: (() -> Unit)? = null,
  singleLine: Boolean = true,
  keyboardType: KeyboardType = KeyboardType.Text,
  modifier: Modifier = Modifier.fillMaxWidth(),
  testTag: String = "",
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
    placeholder = {
      if (placeholder.isNotBlank()) {
        Text(placeholder, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
      }
    },
    leadingIcon = leadingIcon?.let {
      {
        Icon(
          imageVector = it,
          contentDescription = null,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(18.dp),
        )
      }
    },
    trailingIcon = if (trailingActionIcon != null && onTrailingActionClick != null) {
      {
        IconButton(onClick = onTrailingActionClick) {
          Icon(
            imageVector = trailingActionIcon,
            contentDescription = trailingActionDescription,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    } else null,
    singleLine = singleLine,
    keyboardOptions = KeyboardOptions(
      capitalization = KeyboardCapitalization.Sentences,
      keyboardType = keyboardType,
    ),
    shape = RoundedCornerShape(12.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = Color(0xFF1E293B),
      unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.55f),
      focusedBorderColor = Color(0xFFF59E0B).copy(alpha = 0.7f),
      unfocusedBorderColor = Color(0xFF334155).copy(alpha = 0.6f),
      focusedTextColor = PureWhite,
      unfocusedTextColor = PureWhite,
      focusedLabelColor = Color(0xFFFBBF24),
      unfocusedLabelColor = Color(0xFF94A3B8),
      cursorColor = Color(0xFFF59E0B),
    ),
    modifier = modifier.testTag(testTag),
  )
}
