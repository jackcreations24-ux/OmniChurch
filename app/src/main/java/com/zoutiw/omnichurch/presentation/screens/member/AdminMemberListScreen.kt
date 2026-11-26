package com.zoutiw.omnichurch.presentation.screens.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.MidnightNavy
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite

/**
 * Secure Member List Screen for Church Administrators.
 * Displays all registered church members with interactive swipeable quick actions
 * ('Edit Info', 'Message Member', 'Call Member', 'Copy ID') and admin creation.
 */
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
  churchMembersUiState: UiState<List<Member>> = UiState.Success(churchMembers),
  onRetryLoadMembers: () -> Unit = {},
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current
  val isAdmin = isAdminSession || userProfile?.isAdminOrSuperAdmin == true

  var searchQuery by remember { mutableStateOf("") }
  var selectedDepartmentFilter by remember { mutableStateOf("Tout") }

  // Security gate: non-admins cannot access this screen
  if (!isAdmin) {
    Box(
      modifier = modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient).padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      PremiumGlassCard(
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(24.dp),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = BrandOrange,
            modifier = Modifier.size(52.dp),
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Aksè Rezève pou Administratè",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Ou pa gen otorizasyon pou jere lis manm legliz sa a. Tanpri konekte kòm yon administratè otorize.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGlassText,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(20.dp))
          GlassButton(
            text = "Retounen nan Akèy",
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
    return
  }

  // Filtered members list
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

  var openedMemberKey by remember { mutableStateOf<String?>(null) }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(DeepNavy)
        .statusBarsPadding(),
  ) {
    // 1. Top Bar
    GlassTopBar(
      title = "Lis Manm Legliz la",
      rightBadgeText = "${churchMembers.size} Manm",
      onBackClick = onBackClick,
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize().background(DeepNavy),
      contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      // 2. Action Bar: Search input & Filters
      item {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(DeepNavy)
              .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
          GlassInputField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Rechèch pa non, matrikil, wòl...",
            leadingIcon = Icons.Default.Search,
            modifier = Modifier.fillMaxWidth(),
            testTag = "admin_member_search_input",
          )

          // 3. Department filter chips (opaque & borderless)
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
                    Modifier
                      .clip(RoundedCornerShape(50))
                      .background(if (isSelected) ElectricBlue else MidnightNavy)
                      .clickable { selectedDepartmentFilter = dept }
                      .padding(horizontal = 14.dp, vertical = 7.dp)
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

          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Glise manm a goch",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MutedGlassText,
            fontWeight = FontWeight.Medium,
          )
          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 4. Member List, Loading, Error, or Empty State
      when {
        churchMembersUiState is UiState.Loading && churchMembers.isEmpty() -> {
          item {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              GlassLoadingIndicator()
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "Ap chaje lis manm legliz la...",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGlassText,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
        churchMembersUiState is UiState.Error && churchMembers.isEmpty() -> {
          item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
              PremiumGlassCard(
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(20.dp),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = BrandOrange,
                    modifier = Modifier.size(44.dp),
                  )
                  Spacer(modifier = Modifier.height(12.dp))
                  Text(
                    text = "Nou pa t kapab chaje lis manm yo",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = churchMembersUiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGlassText,
                    textAlign = TextAlign.Center,
                  )
                  Spacer(modifier = Modifier.height(16.dp))
                  GlassButton(
                    text = "Re-eseye",
                    icon = Icons.Default.Refresh,
                    onClick = onRetryLoadMembers,
                    modifier = Modifier.fillMaxWidth().testTag("retry_load_members_button"),
                  )
                }
              }
            }
          }
        }
        filteredMembers.isEmpty() -> {
          item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
              PremiumEmptyState(
                title = if (searchQuery.isNotBlank()) "Okenn Manm Pa Koresponn" else "Okenn Manm Anrejistre",
                subtitle =
                  if (searchQuery.isNotBlank()) "Pa gen okenn manm ki jwenn ak rechèch \"$searchQuery\"."
                  else "Anrejistreman nouvo manm fèt sèlman sou Platfòm Web OmniChurch Pro a. Yon fwa yo anrejistre sou Web la, y ap parèt isit la otomatikman.",
                icon = Icons.Default.People,
              )
            }
          }
        }
        else -> {
          items(filteredMembers, key = { it.id.ifBlank { it.effectiveMatrikil } }) { memberItem ->
            val itemKey = memberItem.id.ifBlank { memberItem.effectiveMatrikil }
            SwipeableMemberRow(
              member = memberItem,
              isSwipedOpen = openedMemberKey == itemKey,
              onSwipeStateChange = { shouldOpen ->
                openedMemberKey = if (shouldOpen) itemKey else if (openedMemberKey == itemKey) null else openedMemberKey
              },
              onDragWillStart = {
                if (openedMemberKey != null && openedMemberKey != itemKey) {
                  openedMemberKey = null
                }
              },
              onMessage = {
                launchWhatsAppMessageForMember(context, memberItem)
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
  }
}

/**
 * Launches a WhatsApp message to the member using WhatsApp's official universal link (`https://wa.me/`)
 * and standard Android `ACTION_VIEW` Intent in strict compliance with both Google Play and WhatsApp policies.
 */
internal fun launchWhatsAppMessageForMember(
  context: Context,
  member: Member,
) {
  val rawPhone = member.phone?.trim().orEmpty()
  if (rawPhone.isBlank()) {
    Toast.makeText(context, "Manm sa a pa gen nimewo telefòn.", Toast.LENGTH_SHORT).show()
    return
  }

  val strippedDigits = rawPhone.replace(Regex("[^0-9]"), "").removePrefix("00")
  if (strippedDigits.length < 7) {
    Toast.makeText(context, "Nimewo telefòn sa a pa valid pou WhatsApp: $rawPhone", Toast.LENGTH_SHORT).show()
    return
  }

  // WhatsApp wa.me requires full international digits without '+' or '00'
  val waPhone =
    when {
      strippedDigits.length == 8 -> "509$strippedDigits"
      strippedDigits.length == 10 && !strippedDigits.startsWith("509") -> "1$strippedDigits"
      else -> strippedDigits
    }

  val greeting = "Bonjou ${member.effectiveFullName},"
  val waUri = Uri.parse("https://wa.me/$waPhone?text=${Uri.encode(greeting)}")
  val waIntent = Intent(Intent.ACTION_VIEW, waUri)

  runCatching { context.startActivity(waIntent) }
    .onFailure {
      Toast.makeText(context, "WhatsApp: +$waPhone", Toast.LENGTH_SHORT).show()
    }
}
