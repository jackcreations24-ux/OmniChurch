package com.zoutiw.omnichurch.presentation.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Modern Minimalist Bottom Sheet pou kreyasyon ak piblikasyon Anons (Announcements).
 * Design anti-slop: 100% linear flow san bòdi lou, seleksyon kategori/odyans rapid,
 * nivo alèt toast, ak opsyon avanse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAnnouncementBottomSheet(
  onDismissRequest: () -> Unit,
  availableServices: List<ChurchService> = emptyList(),
  initialAnnouncement: Announcement? = null,
  currentLanguageCode: String = "ht",
  isDeaconMode: Boolean = false,
  lockedDeaconDepartment: String? = null,
  lockedDeaconAuthorLabel: String? = null,
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
  val copy = remember(currentLanguageCode) {
    CreateAnnouncementStrings.forLanguage(currentLanguageCode)
  }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val categories = copy.categories
  val audiences = copy.audiences
  val alertLevels = copy.alertLevels

  val todayDate = remember {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
  }

  val isEditing = initialAnnouncement != null
  val editKey = initialAnnouncement?.id ?: "new_announcement"
  val initialCat = remember(editKey) {
    val raw = initialAnnouncement?.category.orEmpty()
    if (categories.any { it.first == raw }) raw else "anons"
  }
  val initialAud = remember(editKey) {
    val raw = initialAnnouncement?.targetAudience.orEmpty()
    if (audiences.any { it.first == raw }) raw else "Tout moun"
  }
  val initialAlert = remember(editKey) {
    val raw = initialAnnouncement?.toastAlertLevel.orEmpty()
    if (alertLevels.any { it.first == raw }) raw else "info"
  }

  var selectedCategory by remember(editKey) { mutableStateOf(initialCat) }
  var selectedAudience by remember(editKey) { mutableStateOf(initialAud) }
  var selectedAlertLevel by remember(editKey) { mutableStateOf(initialAlert) }

  var title by remember(editKey) { mutableStateOf(initialAnnouncement?.title.orEmpty()) }
  var content by remember(editKey) { mutableStateOf(initialAnnouncement?.content.orEmpty()) }
  val effectiveDeaconDept =
    lockedDeaconDepartment?.trim()?.takeIf { it.isNotEmpty() }
      ?: com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.DEFAULT_DEACON_DEPARTMENT
  var department by remember(editKey, isDeaconMode, effectiveDeaconDept) {
    mutableStateOf(
      if (isDeaconMode) {
        effectiveDeaconDept
      } else {
        initialAnnouncement?.department?.ifBlank { copy.defaultDepartment }
          ?: copy.defaultDepartment
      }
    )
  }
  var date by remember(editKey) {
    mutableStateOf(initialAnnouncement?.date?.takeIf { it.isNotBlank() } ?: todayDate)
  }
  var isPinned by remember(editKey) { mutableStateOf(initialAnnouncement?.isPinned ?: true) }
  var showToastOnLogin by remember(editKey) {
    mutableStateOf(initialAnnouncement?.showToastOnLogin ?: false)
  }
  var isScheduledNotification by remember(editKey) {
    mutableStateOf(initialAnnouncement?.isScheduledNotification ?: false)
  }
  var scheduledNotificationDate by remember(editKey) {
    mutableStateOf(
      initialAnnouncement?.scheduledNotificationDate?.takeIf { it.isNotBlank() } ?: todayDate
    )
  }
  var posterUrl by remember(editKey) {
    mutableStateOf(initialAnnouncement?.effectiveImageUrl.orEmpty())
  }
  var isCompressingPoster by remember { mutableStateOf(false) }
  var selectedServiceId by remember(editKey) {
    mutableStateOf<String?>(initialAnnouncement?.serviceId)
  }

  var showAdvancedOptions by remember(editKey) {
    val initImg = initialAnnouncement?.effectiveImageUrl
    mutableStateOf(
      initialAnnouncement != null &&
        (!initialAnnouncement.serviceId.isNullOrBlank() ||
          (!initImg.isNullOrBlank() && !initImg.startsWith("data:", ignoreCase = true)))
    )
  }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        isCompressingPoster = true
        scope.launch {
          val compressedRes =
            ImageCompressor.compressUriToBase64(
              context = context,
              uri = uri,
              maxDimension = 900,
              quality = 80,
            )
          isCompressingPoster = false
          compressedRes
            .onSuccess { dataUrl -> posterUrl = dataUrl }
            .onFailure { err ->
              Toast.makeText(
                  context,
                  err.localizedMessage ?: copy.imageLoadError,
                  Toast.LENGTH_SHORT,
                )
                .show()
            }
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = Color(0xFF0F172A), // Slate-900
    contentColor = PureWhite,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .size(width = 48.dp, height = 5.dp)
          .clip(CircleShape)
          .background(Color(0xFF334155)) // Slate-700
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
      // Header Section
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isEditing) copy.headerTitleEdit else copy.headerTitleCreate,
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              if (isEditing) copy.headerSubtitleEdit
              else copy.headerSubtitleCreate,
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
            contentDescription = copy.closeCd,
            tint = Color(0xFF94A3B8),
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
        // 1. Kategori Anons
        SectionTitle(title = copy.categorySectionTitle)
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

        // 2. Enfòmasyon Prensipal
        SectionTitle(title = copy.contentSectionTitle)
        MinimalTextField(
          value = title,
          onValueChange = { title = it },
          label = copy.titleLabel,
          placeholder = copy.titlePlaceholder,
          leadingIcon = Icons.Default.Campaign,
          testTag = "announcement_input_title",
        )

        if (isDeaconMode) {
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.72f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = copy.departmentLabel,
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF94A3B8),
            )
            Text(
              text =
                listOfNotNull(
                    lockedDeaconAuthorLabel?.trim()?.takeIf { it.isNotEmpty() },
                    effectiveDeaconDept,
                  )
                  .joinToString(" • "),
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFF38BDF8),
              fontWeight = FontWeight.SemiBold,
            )
          }
        } else {
          MinimalTextField(
            value = department,
            onValueChange = { department = it },
            label = copy.departmentLabel,
            placeholder = copy.departmentPlaceholder,
            testTag = "announcement_input_dept",
          )
        }

        MinimalTextField(
          value = content,
          onValueChange = { content = it },
          label = copy.contentLabel,
          placeholder = copy.contentPlaceholder,
          singleLine = false,
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
          testTag = "announcement_input_content",
        )

        // 3. Odyans Sib (Target Audience)
        SectionTitle(title = copy.audienceSectionTitle)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          audiences.forEach { (audKey, audLabel) ->
            CategoryChip(
              label = audLabel,
              isSelected = selectedAudience == audKey,
              onClick = { selectedAudience = audKey },
              modifier = Modifier.weight(1f),
            )
          }
        }

        // 4. Optsyon Ijan & Pop-up
        AnnouncementOptionsAndAlertSection(
          isPinned = isPinned,
          onPinnedChange = { isPinned = it },
          showToastOnLogin = showToastOnLogin,
          onShowToastChange = { showToastOnLogin = it },
          selectedAlertLevel = selectedAlertLevel,
          onAlertLevelChange = { selectedAlertLevel = it },
          alertLevels = alertLevels,
          copy = copy,
        )

        // 5. Imaj / Afich Anons & Lyen avanse
        AnnouncementMediaAndLinksSection(
          title = title,
          posterUrl = posterUrl,
          onPosterUrlChange = { posterUrl = it },
          isCompressingPoster = isCompressingPoster,
          onPickImageClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          showAdvancedOptions = showAdvancedOptions,
          onToggleAdvancedOptions = { showAdvancedOptions = !showAdvancedOptions },
          availableServices = availableServices,
          selectedServiceId = selectedServiceId,
          onSelectServiceId = { selectedServiceId = it },
          copy = copy,
        )
      }

      // Sticky Action Footer (Clean & minimal)
      AnnouncementSubmitFooter(
        isEditing = isEditing,
        submitEnabled = title.isNotBlank() && content.isNotBlank(),
        copy = copy,
        onDismissRequest = onDismissRequest,
        onSubmitClick = {
          onSubmit(
            title,
            content,
            if (isDeaconMode) effectiveDeaconDept else department,
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
      )
    }
  }
}
