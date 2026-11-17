package com.zoutiw.omnichurch.presentation.screens.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AdminMemberListScreen(
  userProfile: UserProfile?,
  isAdminSession: Boolean,
  churchMembers: List<Member>,
  onBackClick: () -> Unit,
  onAdminUpdateMember: (
    memberDocId: String,
    fullName: String,
    department: String,
    ministryRole: String,
    phone: String?,
    titlePrefix: String?,
  ) -> Unit,
  onRegisterNewMember: (
    fullName: String,
    customMatrikil: String?,
    pin: String,
    phone: String?,
    department: String,
    ministryRole: String,
  ) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val isAdmin = isAdminSession || userProfile?.isAdminOrSuperAdmin == true

  var searchQuery by remember { mutableStateOf("") }
  var selectedDepartmentFilter by remember { mutableStateOf("Tout") }
  var memberBeingEdited by remember { mutableStateOf<Member?>(null) }
  var showRegisterMemberDialog by remember { mutableStateOf(false) }

  val departmentsList = remember(churchMembers) {
    val depts = churchMembers.mapNotNull { it.department.trim().takeIf { d -> d.isNotBlank() } }.distinct()
    listOf("Tout") + depts
  }

  val filteredMembers = remember(churchMembers, searchQuery, selectedDepartmentFilter) {
    churchMembers.filter { member ->
      val matchesDept =
        selectedDepartmentFilter == "Tout" ||
          member.department.equals(selectedDepartmentFilter, ignoreCase = true)

      val query = searchQuery.trim().lowercase()
      val matchesQuery =
        query.isBlank() ||
          member.effectiveFullName.lowercase().contains(query) ||
          member.effectiveMatrikil.lowercase().contains(query) ||
          member.department.lowercase().contains(query) ||
          member.role.lowercase().contains(query) ||
          (member.phone?.lowercase()?.contains(query) == true) ||
          (member.titlePrefix?.lowercase()?.contains(query) == true)

      matchesDept && matchesQuery
    }
  }

  Column(
    modifier = modifier.fillMaxSize().statusBarsPadding(),
  ) {
    GlassTopBar(
      title = "Lis Manm Legliz la",
      rightBadgeText = "${churchMembers.size} Manm",
      onBackClick = onBackClick,
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            GlassInputField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              label = "Rechèch pa non, matrikil, wòl...",
              leadingIcon = Icons.Default.Search,
              modifier = Modifier.weight(1f),
              testTag = "admin_member_search_input",
            )

            if (isAdmin) {
              Box(
                modifier =
                  Modifier.size(52.dp)
                    .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = BlueNeon)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                      Brush.verticalGradient(listOf(Color(0xFF22ACFF), ElectricBlue, com.zoutiw.omnichurch.ui.theme.RoyalBlue))
                    )
                    .border(1.2.dp, PureWhite.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .clickable { showRegisterMemberDialog = true }
                    .testTag("admin_add_member_top_btn"),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.PersonAdd,
                  contentDescription = "Ajoute Manm",
                  tint = PureWhite,
                  modifier = Modifier.size(24.dp),
                )
              }
            }
          }

          if (departmentsList.size > 2) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              items(departmentsList) { dept ->
                val isSelected = selectedDepartmentFilter == dept
                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(50))
                      .background(if (isSelected) ElectricBlue else Color(0x550B1D3D))
                      .border(
                        1.dp,
                        if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.25f),
                        RoundedCornerShape(50),
                      )
                      .clickable { selectedDepartmentFilter = dept }
                      .padding(horizontal = 14.dp, vertical = 6.dp)
                      .testTag("filter_dept_$dept"),
                ) {
                  Text(
                    text = dept,
                    style = MaterialTheme.typography.labelMedium,
                    color = PureWhite,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Glise yon manm sou agòch pou jwenn aksyon rapid yo (WhatsApp, Rele, Modifye, Kopye)",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MutedGlassText,
          )
        }
      }

      if (filteredMembers.isEmpty()) {
        item {
          PremiumEmptyState(
            title = if (searchQuery.isNotBlank()) "Okenn Manm Pa Koresponn" else "Okenn Manm Anrejistre",
            subtitle =
              if (searchQuery.isNotBlank()) "Pa gen okenn manm ki jwenn ak rechèch \"$searchQuery\"."
              else "Kòmanse pa anrejistre manm legliz ou a avèk ID Matrikil ak PIN yo.",
            icon = Icons.Default.People,
            actionLabel = if (isAdmin) "Anrejistre yon Nouvo Manm" else null,
            onActionClick = if (isAdmin) ({ showRegisterMemberDialog = true }) else null,
          )
        }
      } else {
        items(filteredMembers, key = { it.id.ifBlank { it.effectiveMatrikil } }) { memberItem ->
          SwipeableMemberRow(
            member = memberItem,
            isAdmin = isAdmin,
            onEdit = { memberBeingEdited = memberItem },
            onWhatsAppMessage = {
              val phone = memberItem.phone?.trim().orEmpty()
              if (phone.isNotBlank()) {
                val digits = phone.replace(Regex("[^0-9]"), "")
                val formattedPhone = when {
                  digits.startsWith("509") -> digits
                  digits.length == 8 -> "509$digits"
                  else -> digits
                }
                val greeting = "Bonjou ${memberItem.effectiveFullName},"
                val whatsappUrl = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(greeting)}"
                val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))

                runCatching {
                  context.startActivity(whatsappIntent)
                }.onFailure {
                  val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$formattedPhone"))
                  smsIntent.putExtra("sms_body", greeting)
                  runCatching { context.startActivity(smsIntent) }
                    .onFailure {
                      Toast.makeText(context, "Telefòn: $phone", Toast.LENGTH_SHORT).show()
                    }
                }
              } else {
                Toast.makeText(context, "Manm sa a pa gen nimewo telefòn.", Toast.LENGTH_SHORT).show()
              }
            },
            onCall = {
              val phone = memberItem.phone?.trim().orEmpty()
              if (phone.isNotBlank()) {
                val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                runCatching { context.startActivity(dialIntent) }
                  .onFailure {
                    Toast.makeText(context, "Telefòn: $phone", Toast.LENGTH_SHORT).show()
                  }
              } else {
                Toast.makeText(context, "Manm sa a pa gen nimewo telefòn.", Toast.LENGTH_SHORT).show()
              }
            },
            onCopyCode = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("ID Matrikil", memberItem.effectiveMatrikil))
              Toast.makeText(context, "ID ${memberItem.effectiveMatrikil} kopye!", Toast.LENGTH_SHORT).show()
            },
          )
        }
      }
    }
  }

  if (memberBeingEdited != null) {
    EditMemberBottomSheet(
      member = memberBeingEdited!!,
      onDismissRequest = { memberBeingEdited = null },
      onSave = { fullName, department, role, phone, titlePrefix ->
        onAdminUpdateMember(
          memberBeingEdited!!.id,
          fullName,
          department,
          role,
          phone,
          titlePrefix,
        )
      },
    )
  }

  if (showRegisterMemberDialog) {
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
        testTag = "admin_new_member_name",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberMatrikil,
        onValueChange = { newMemberMatrikil = it },
        label = "ID Matrikil (Kite vid pou jenere otomatikman)",
        placeholder = "Egz: CH001-JD4827",
        testTag = "admin_new_member_matrikil",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberPin,
        onValueChange = { newMemberPin = it },
        label = "Kòd Sekirite (PIN Manm)",
        placeholder = "Egz: 4827",
        testTag = "admin_new_member_pin",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberPhone,
        onValueChange = { newMemberPhone = it },
        label = "Nimewo Telefòn (Opsyonèl)",
        placeholder = "Egz: 50937001234",
        testTag = "admin_new_member_phone",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberDept,
        onValueChange = { newMemberDept = it },
        label = "Depatman",
        testTag = "admin_new_member_dept",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = newMemberRole,
        onValueChange = { newMemberRole = it },
        label = "Wòl nan Legliz la",
        testTag = "admin_new_member_role",
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
          testTag = "admin_new_member_submit_btn",
        )
      }
    }
  }
}

@Composable
fun EditMemberBottomSheet(
  member: Member,
  onDismissRequest: () -> Unit,
  onSave: (
    fullName: String,
    department: String,
    ministryRole: String,
    phone: String?,
    titlePrefix: String?,
  ) -> Unit,
) {
  var titlePrefix by remember(member.id) { mutableStateOf(member.titlePrefix.orEmpty()) }
  var fullName by remember(member.id) { mutableStateOf(member.fullName) }
  var department by remember(member.id) { mutableStateOf(member.department.ifBlank { "Adorasyon" }) }
  var role by remember(member.id) { mutableStateOf(member.role.ifBlank { "Manm" }) }
  var phone by remember(member.id) { mutableStateOf(member.phone.orEmpty()) }

  GlassModalBottomSheet(
    onDismissRequest = onDismissRequest,
    title = "Modifye Manm (${member.effectiveMatrikil})",
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(bottom = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x440B1D3D))
            .border(1.dp, GlassWhite.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MemberAvatar3D(
          fullName = member.effectiveFullName,
          size = 44.dp,
          photoUrl = member.effectivePhotoUrl,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = member.effectiveFullName,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "ID: ${member.effectiveMatrikil} • ${member.churchName.ifBlank { "OmniChurch Pro" }}",
            style = MaterialTheme.typography.bodySmall,
            color = BrandGold,
          )
        }
      }

      GlassInputField(
        value = titlePrefix,
        onValueChange = { titlePrefix = it },
        label = "Tit (Egz: Pastè, Diak, Sè, Frè)",
        placeholder = "Egz: Pastè / Diak",
        testTag = "admin_sheet_member_title",
      )

      GlassInputField(
        value = fullName,
        onValueChange = { fullName = it },
        label = "Non Konplè Manm nan *",
        placeholder = "Egz: Jean-David Pierre",
        testTag = "admin_sheet_member_fullname",
      )

      GlassInputField(
        value = department,
        onValueChange = { department = it },
        label = "Depatman / Ministè",
        placeholder = "Egz: Adorasyon / Koral",
        testTag = "admin_sheet_member_dept",
      )

      GlassInputField(
        value = role,
        onValueChange = { role = it },
        label = "Wòl nan Legliz la",
        placeholder = "Egz: Manm Aktif / Mizisyen",
        testTag = "admin_sheet_member_role",
      )

      GlassInputField(
        value = phone,
        onValueChange = { phone = it },
        label = "Nimewo Telefòn Kontak",
        placeholder = "Egz: 50937001234",
        keyboardType = KeyboardType.Phone,
        testTag = "admin_sheet_member_phone",
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassButton(
          text = stringResource(R.string.common_cancel),
          isPrimaryNeon = false,
          onClick = onDismissRequest,
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text = stringResource(R.string.profile_save_changes),
          icon = Icons.Default.CheckCircle,
          enabled = fullName.isNotBlank(),
          onClick = {
            onSave(
              fullName,
              department,
              role,
              phone.takeIf { it.isNotBlank() },
              titlePrefix.takeIf { it.isNotBlank() },
            )
            onDismissRequest()
          },
          modifier = Modifier.weight(1f),
          testTag = "admin_sheet_save_member_btn",
        )
      }
    }
  }
}

@Composable
private fun SwipeableMemberRow(
  member: Member,
  isAdmin: Boolean,
  onEdit: () -> Unit,
  onWhatsAppMessage: () -> Unit,
  onCall: () -> Unit,
  onCopyCode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val offsetX = remember { Animatable(0f) }
  val targetSwipeDp = if (isAdmin) (-190).dp else (-140).dp
  val maxSwipePx = remember(density, targetSwipeDp) { with(density) { targetSwipeDp.toPx() } }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .background(Color(0xFF07142B))
        .testTag("admin_member_card_${member.id}"),
  ) {
    Row(
      modifier =
        Modifier.fillMaxSize()
          .background(Color(0xFF07142B))
          .padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          scope.launch { offsetX.animateTo(0f) }
          onWhatsAppMessage()
        },
        modifier = Modifier.size(40.dp).testTag("swipe_action_whatsapp_${member.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.Message,
          contentDescription = "WhatsApp",
          tint = Color(0xFF25D366),
          modifier = Modifier.size(22.dp),
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      IconButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          scope.launch { offsetX.animateTo(0f) }
          onCall()
        },
        modifier = Modifier.size(40.dp).testTag("swipe_action_call_${member.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.Call,
          contentDescription = "Rele",
          tint = BrandOrange,
          modifier = Modifier.size(22.dp),
        )
      }

      if (isAdmin) {
        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch { offsetX.animateTo(0f) }
            onEdit()
          },
          modifier = Modifier.size(40.dp).testTag("swipe_action_edit_${member.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Modifye",
            tint = BlueNeon,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Spacer(modifier = Modifier.width(4.dp))

      IconButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          scope.launch { offsetX.animateTo(0f) }
          onCopyCode()
        },
        modifier = Modifier.size(40.dp).testTag("swipe_action_copy_${member.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.ContentCopy,
          contentDescription = "Kopye ID",
          tint = BrandGold,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Box(
      modifier =
        Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
          .fillMaxWidth()
          .background(Color(0xFF0A1836))
          .draggable(
            orientation = Orientation.Horizontal,
            state =
              rememberDraggableState { delta ->
                scope.launch {
                  val newOffset = (offsetX.value + delta).coerceIn(maxSwipePx, 0f)
                  offsetX.snapTo(newOffset)
                }
              },
            onDragStopped = {
              scope.launch {
                if (offsetX.value < maxSwipePx / 2) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  offsetX.animateTo(maxSwipePx, tween(160))
                } else {
                  offsetX.animateTo(0f, tween(160))
                }
              }
            },
          ),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            if (offsetX.value != 0f) {
              scope.launch { offsetX.animateTo(0f) }
            } else if (isAdmin) {
              onEdit()
            } else {
              onWhatsAppMessage()
            }
          }
          .padding(horizontal = 16.dp, vertical = 14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            MemberAvatar3D(
              fullName = member.effectiveFullName,
              size = 46.dp,
              photoUrl = member.effectivePhotoUrl,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (!member.titlePrefix.isNullOrBlank()) {
                  Text(
                    text = "${member.titlePrefix} ",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = BrandGold,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Text(
                  text = member.effectiveFullName,
                  style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Spacer(modifier = Modifier.height(2.dp))

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "ID: ${member.effectiveMatrikil}",
                  style = MaterialTheme.typography.bodySmall,
                  color = BrandGold,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = " • ${member.department.ifBlank { "Adorasyon" }}",
                  style = MaterialTheme.typography.bodySmall,
                  color = GlassWhite.copy(alpha = 0.85f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              if (!member.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "📞 ${member.phone}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MutedGlassText,
                )
              }
            }
          }

          Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Glise pou Aksyon",
            tint = GlassWhite.copy(alpha = 0.50f),
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}
