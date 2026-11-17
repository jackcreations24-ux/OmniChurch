package com.zoutiw.omnichurch.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import android.net.Uri
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceBottomSheet(
  onDismissRequest: () -> Unit,
  serviceToEdit: ChurchService? = null,
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
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

  var selectedCategory by remember { mutableStateOf(serviceToEdit?.category ?: "dimanche_matin") }
  var title by remember { mutableStateOf(serviceToEdit?.title ?: "Sèvis Adorasyon & Louwanj") }
  var date by remember { mutableStateOf(serviceToEdit?.date?.takeIf { it.isNotBlank() } ?: todayDate) }
  var startTime by remember { mutableStateOf(serviceToEdit?.startTime?.takeIf { it.isNotBlank() } ?: "09:00") }
  var endTime by remember { mutableStateOf(serviceToEdit?.endTime?.takeIf { it.isNotBlank() } ?: "11:30") }
  var theme by remember { mutableStateOf(serviceToEdit?.theme.orEmpty()) }
  var passage by remember { mutableStateOf(serviceToEdit?.passage.orEmpty()) }
  var leader by remember { mutableStateOf(serviceToEdit?.leader.orEmpty()) }
  var preacher by remember { mutableStateOf(serviceToEdit?.preacher.orEmpty()) }
  var reader by remember { mutableStateOf(serviceToEdit?.reader.orEmpty()) }
  var musicians by remember { mutableStateOf(serviceToEdit?.musicians.orEmpty()) }
  var ushers by remember { mutableStateOf(serviceToEdit?.ushers.orEmpty()) }
  var isOpenForAttendance by remember { mutableStateOf(serviceToEdit?.isOpenForAttendance ?: true) }
  var sendReminder24h by remember { mutableStateOf(serviceToEdit?.sendReminder24h ?: false) }
  var lateToleranceMinutesStr by remember { mutableStateOf(serviceToEdit?.lateToleranceMinutes?.toString() ?: "15") }

  var showAdvancedRoles by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = Color(0xFF0F172A),
    contentColor = PureWhite,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .size(width = 48.dp, height = 5.dp)
          .clip(CircleShape)
          .background(Color(0xFF334155))
      )
    },
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = modifier.imePadding(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = if (serviceToEdit != null) "Modifye Sèvis la" else "Kreye yon Nouvo Sèvis",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Senkwonizasyon total ak tablodbò OmniChurch",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
          )
        }
        IconButton(
          onClick = onDismissRequest,
          modifier = Modifier.size(36.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Fèmen",
            tint = Color(0xFF94A3B8),
          )
        }
      }

      Column(
        modifier = Modifier
          .weight(1f, fill = false)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
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

        SectionTitle(title = "Dat & Orè")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MinimalTextField(
            value = date,
            onValueChange = { date = it },
            label = "Dat (YYYY-MM-DD) *",
            placeholder = "2026-10-26",
            leadingIcon = Icons.Default.CalendarMonth,
            modifier = Modifier.weight(1.3f),
            keyboardType = KeyboardType.Number,
            testTag = "service_input_date",
          )
          MinimalTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = "Kòmanse (HH:mm)",
            placeholder = "09:00",
            leadingIcon = Icons.Default.AccessTime,
            modifier = Modifier.weight(1f),
            testTag = "service_input_start_time",
          )
          MinimalTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = "Fini (HH:mm)",
            placeholder = "11:30",
            modifier = Modifier.weight(1f),
            testTag = "service_input_end_time",
          )
        }

        SectionTitle(title = "Ofisyan & Responsab")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MinimalTextField(
            value = leader,
            onValueChange = { leader = it },
            label = "Dirijan Sèvis",
            placeholder = "Egz: Frè Pierre",
            leadingIcon = Icons.Default.Person,
            modifier = Modifier.weight(1f),
            testTag = "service_input_leader",
          )
          MinimalTextField(
            value = preacher,
            onValueChange = { preacher = it },
            label = "Predikatè",
            placeholder = "Egz: Pastè Jean",
            leadingIcon = Icons.Default.Person,
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
            text = if (showAdvancedRoles) "Kache wòl siplemantè yo ▲" else "Ajoute lòt wòl (Mizisyen, Akèy, Lektè) ▼",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.SemiBold,
          )
        }

        AnimatedVisibility(visible = showAdvancedRoles) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MinimalTextField(
              value = reader,
              onValueChange = { reader = it },
              label = "Lektè Vèsè",
              placeholder = "Egz: Sè Marie",
              testTag = "service_input_reader",
            )
            MinimalTextField(
              value = musicians,
              onValueChange = { musicians = it },
              label = "Mizisyen / Koral",
              placeholder = "Egz: Koral Dawout, Gwoup Adorasyon",
              leadingIcon = Icons.Default.MusicNote,
              testTag = "service_input_musicians",
            )
            MinimalTextField(
              value = ushers,
              onValueChange = { ushers = it },
              label = "Akèy / Pwotokòl",
              placeholder = "Egz: Ekip Pwotokòl #1",
              testTag = "service_input_ushers",
            )
          }
        }

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
              text = "Pèmèt manm eskane QR code yo pou anrejistre prezans",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          Switch(
            checked = isOpenForAttendance,
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
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0F172A))
          .border(width = 1.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(0.dp))
          .padding(horizontal = 20.dp, vertical = 14.dp)
          .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onDismissRequest,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1E293B),
            contentColor = Color(0xFFCBD5E1),
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(48.dp),
        ) {
          Text("Annuler", fontWeight = FontWeight.SemiBold)
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
            )
            onDismissRequest()
          },
          enabled = title.isNotBlank() && date.isNotBlank(),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF59E0B),
            contentColor = Color(0xFF0F172A),
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
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (serviceToEdit != null) "Mete ajou Sèvis la" else "Kreye Sèvis la",
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
