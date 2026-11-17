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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementBottomSheet(
  onDismissRequest: () -> Unit,
  availableServices: List<ChurchService> = emptyList(),
  onSubmit: (
    title: String,
    content: String,
    department: String,
    category: String,
    date: String,
    targetAudience: String,
    isPinned: Boolean,
    showToastOnLogin: Boolean,
    toastAlertLevel: String,
    isScheduledNotification: Boolean,
    scheduledNotificationDate: String?,
    posterUrl: String?,
    serviceId: String?,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val categories = listOf(
    "anons" to "Anons Jeneral",
    "ore" to "Orè & Chanjman",
    "sevis" to "Sèvis Espesyal",
    "mesaj" to "Mesaj Pastoral",
  )

  val audiences = listOf("Tout moun", "Jeunesse", "Dames", "Hommes", "Enfants")
  val alertLevels = listOf(
    "info" to ("Enfo" to Color(0xFF38BDF8)),
    "urgent" to ("Ijan" to Color(0xFFEF4444)),
    "warning" to ("Avètisman" to Color(0xFFF59E0B)),
    "celebration" to ("Fèt" to Color(0xFF10B981)),
  )

  val todayDate = remember {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
  }

  var selectedCategory by remember { mutableStateOf("anons") }
  var selectedAudience by remember { mutableStateOf("Tout moun") }
  var selectedAlertLevel by remember { mutableStateOf("info") }

  var title by remember { mutableStateOf("") }
  var content by remember { mutableStateOf("") }
  var department by remember { mutableStateOf("Depatman Administrasyon") }
  var date by remember { mutableStateOf(todayDate) }
  var isPinned by remember { mutableStateOf(true) }
  var showToastOnLogin by remember { mutableStateOf(false) }
  var isScheduledNotification by remember { mutableStateOf(false) }
  var scheduledNotificationDate by remember { mutableStateOf(todayDate) }
  var posterUrl by remember { mutableStateOf("") }
  var selectedServiceId by remember { mutableStateOf<String?>(null) }

  var showAdvancedOptions by remember { mutableStateOf(false) }

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
            text = "Pibliye yon Nouvo Anons",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "Parèt sou fil aktyalite a e sou tablodbò Web la imedyatman",
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
        SectionTitle(title = "Kategori Anons")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          categories.forEach { (catKey, catLabel) ->
            CategoryChip(
              label = catLabel,
              isSelected = selectedCategory == catKey,
              onClick = { selectedCategory = catKey },
              modifier = Modifier.weight(1f),
            )
          }
        }

        SectionTitle(title = "Kontni Anons la")
        MinimalTextField(
          value = title,
          onValueChange = { title = it },
          label = "Tit Anons la *",
          placeholder = "Egz: Konferans Anyèl Fanmi an",
          leadingIcon = Icons.Default.Campaign,
          testTag = "announcement_input_title",
        )

        MinimalTextField(
          value = department,
          onValueChange = { department = it },
          label = "Depatman oswa Komite",
          placeholder = "Egz: Depatman Jenès, Dames, elatriye",
          testTag = "announcement_input_dept",
        )

        MinimalTextField(
          value = content,
          onValueChange = { content = it },
          label = "Mesaj / Kontni Anons la *",
          placeholder = "Detaye mesaj anons la pou tout manm yo ka li l...",
          singleLine = false,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
          testTag = "announcement_input_content",
        )

        SectionTitle(title = "Odyans Sib")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          audiences.forEach { aud ->
            CategoryChip(
              label = aud,
              isSelected = selectedAudience == aud,
              onClick = { selectedAudience = aud },
              modifier = Modifier.weight(1f),
            )
          }
        }

        SectionTitle(title = "Opsyon Piblikasyon & Alèt")

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
              text = "Epingle an tèt (Pin to Top)",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Anons sa a ap toujou parèt an premye nan fil la",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          Switch(
            checked = isPinned,
            onCheckedChange = { isPinned = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = PureWhite,
              checkedTrackColor = Color(0xFFF59E0B),
              uncheckedThumbColor = Color(0xFF64748B),
              uncheckedTrackColor = Color(0xFF1E293B),
            ),
            modifier = Modifier.testTag("announcement_toggle_pinned"),
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
              text = "Afiche Pop-up lè manm lan ouvri app a",
              style = MaterialTheme.typography.bodyMedium,
              color = PureWhite,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Deklanche yon notifikasyon alèt (Toast/Modal) lè yo konekte",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          Switch(
            checked = showToastOnLogin,
            onCheckedChange = { showToastOnLogin = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = PureWhite,
              checkedTrackColor = Color(0xFFEF4444),
              uncheckedThumbColor = Color(0xFF64748B),
              uncheckedTrackColor = Color(0xFF1E293B),
            ),
            modifier = Modifier.testTag("announcement_toggle_toast"),
          )
        }

        AnimatedVisibility(visible = showToastOnLogin) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "NIVO ALÈT POP-UP",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
              fontWeight = FontWeight.Bold,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              alertLevels.forEach { (lvlKey, lvlPair) ->
                val (lvlLabel, lvlColor) = lvlPair
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                      if (selectedAlertLevel == lvlKey) lvlColor.copy(alpha = 0.25f)
                      else Color(0xFF1E293B).copy(alpha = 0.6f)
                    )
                    .border(
                      width = 1.dp,
                      color = if (selectedAlertLevel == lvlKey) lvlColor else Color(0xFF334155),
                      shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { selectedAlertLevel = lvlKey }
                    .padding(vertical = 8.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = lvlLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedAlertLevel == lvlKey) lvlColor else Color(0xFFCBD5E1),
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvancedOptions = !showAdvancedOptions }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (showAdvancedOptions) "Kache lyen & afich ▲" else "Ajoute afich oswa lye ak yon sèvis ▼",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF38BDF8),
            fontWeight = FontWeight.SemiBold,
          )
        }

        AnimatedVisibility(visible = showAdvancedOptions) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MinimalTextField(
              value = posterUrl,
              onValueChange = { posterUrl = it },
              label = "Lyen Imaj / Afich (URL)",
              placeholder = "https://...",
              testTag = "announcement_input_poster_url",
            )

            if (availableServices.isNotEmpty()) {
              SectionTitle(title = "Lye ak yon sèvis planifye")
              availableServices.take(4).forEach { srv ->
                val isSelected = selectedServiceId == srv.id
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF1E293B))
                    .border(
                      1.dp,
                      if (isSelected) Color(0xFFF59E0B) else Color(0xFF334155),
                      RoundedCornerShape(10.dp),
                    )
                    .clickable {
                      selectedServiceId = if (isSelected) null else srv.id
                    }
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = srv.effectiveTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) PureWhite else Color(0xFFCBD5E1),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  )
                }
              }
            }
          }
        }
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
            onSubmit(
              title,
              content,
              department,
              selectedCategory,
              date,
              selectedAudience,
              isPinned,
              showToastOnLogin,
              selectedAlertLevel,
              isScheduledNotification,
              scheduledNotificationDate.takeIf { isScheduledNotification },
              posterUrl.takeIf { it.isNotBlank() },
              selectedServiceId,
            )
            onDismissRequest()
          },
          enabled = title.isNotBlank() && content.isNotBlank(),
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
            .testTag("announcement_bottom_sheet_submit_btn"),
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Pibliye & Senkwonize", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
