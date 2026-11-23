package com.zoutiw.omnichurch.presentation.screens.member

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Refresh
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumEmptyState
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MidnightNavy
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
private fun launchWhatsAppMessageForMember(
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

/**
 * Borderless, 100% opaque swipeable member row with a pro left-swipe tray revealing
 * single centered quick actions (WhatsApp Message, Call, Copy ID) with no colored backgrounds
 * and zero duplicate buttons.
 */
@Composable
private fun SwipeableMemberRow(
  member: Member,
  isSwipedOpen: Boolean,
  onSwipeStateChange: (Boolean) -> Unit,
  onDragWillStart: () -> Unit,
  onMessage: () -> Unit,
  onCall: () -> Unit,
  onCopyCode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  val trayWidthDp = 180.dp
  val trayWidthPx = remember(density) { with(density) { trayWidthDp.toPx() } }
  val offsetX = remember { Animatable(0f) }

  val settleSpec = remember {
    spring<Float>(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    )
  }

  LaunchedEffect(isSwipedOpen, trayWidthPx) {
    val target = if (isSwipedOpen) -trayWidthPx else 0f
    if (kotlin.math.abs(offsetX.value - target) > 1f) {
      offsetX.animateTo(target, settleSpec)
    }
  }

  val trayRevealFraction = (-offsetX.value / trayWidthPx).coerceIn(0f, 1f)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(DeepNavy)
        .testTag("admin_member_card_${member.id}"),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .clipToBounds()
          .background(DeepNavy),
    ) {
      // Background Quick-Action Tray revealed cleanly on swipe left (opaque, centered, no colored button BGs)
      if (offsetX.value < -2f) {
        Box(
          modifier =
            Modifier
              .matchParentSize()
              .background(DeepNavy),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Row(
            modifier =
              Modifier
                .width(trayWidthDp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp)
                .alpha(trayRevealFraction),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SwipeTrayActionButton(
              icon = Icons.Default.Message,
              label = "Mesaj",
              iconTint = SuccessGreen,
              contentDescription = "Voye mesaj WhatsApp",
              enabled = trayRevealFraction > 0.25f,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
                onMessage()
              },
              modifier = Modifier.testTag("swipe_action_msg_${member.id}"),
            )

            SwipeTrayActionButton(
              icon = Icons.Default.Call,
              label = "Rele",
              iconTint = BlueNeon,
              contentDescription = "Rele",
              enabled = trayRevealFraction > 0.25f,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
                onCall()
              },
              modifier = Modifier.testTag("swipe_action_call_${member.id}"),
            )

            SwipeTrayActionButton(
              icon = Icons.Default.ContentCopy,
              label = "Kopye",
              iconTint = BrandGold,
              contentDescription = "Kopye ID",
              enabled = trayRevealFraction > 0.25f,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
                onCopyCode()
              },
              modifier = Modifier.testTag("swipe_action_copy_${member.id}"),
            )
          }
        }
      }

      // Foreground 100% Opaque Borderless Row that slides horizontally on drag
      Row(
        modifier =
          Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .fillMaxWidth()
            .background(PrimaryNavy)
            .draggable(
              orientation = Orientation.Horizontal,
              state =
                rememberDraggableState { delta ->
                  scope.launch {
                    val newOffset = (offsetX.value + delta).coerceIn(-trayWidthPx, 0f)
                    offsetX.snapTo(newOffset)
                  }
                },
              onDragStarted = {
                onDragWillStart()
              },
              onDragStopped = { velocity ->
                scope.launch {
                  val shouldOpen =
                    when {
                      velocity < -450f -> true
                      velocity > 450f -> false
                      else -> offsetX.value < -trayWidthPx * 0.42f
                    }
                  if (shouldOpen) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipeStateChange(true)
                    offsetX.animateTo(-trayWidthPx, settleSpec)
                  } else {
                    onSwipeStateChange(false)
                    offsetX.animateTo(0f, settleSpec)
                  }
                }
              },
            )
            .clickable {
              val currentlyOpen = offsetX.value < -trayWidthPx * 0.2f
              if (currentlyOpen) {
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
              } else {
                onSwipeStateChange(true)
                scope.launch { offsetX.animateTo(-trayWidthPx, settleSpec) }
              }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MemberAvatar3D(
          fullName = member.effectiveFullName,
          size = 46.dp,
          photoUrl = member.effectivePhotoUrl,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
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
              text = member.phone.trim(),
              style = MaterialTheme.typography.labelSmall,
              color = MutedGlassText,
            )
          }
        }
      }
    }

    HorizontalDivider(
      color = MidnightNavy,
      thickness = 0.7.dp,
      modifier = Modifier.padding(start = 74.dp, end = 16.dp),
    )
  }
}

@Composable
private fun SwipeTrayActionButton(
  icon: ImageVector,
  label: String,
  iconTint: Color,
  contentDescription: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .size(width = 52.dp, height = 56.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(enabled = enabled, onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = iconTint,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = PureWhite,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      softWrap = false,
      textAlign = TextAlign.Center,
    )
  }
}
