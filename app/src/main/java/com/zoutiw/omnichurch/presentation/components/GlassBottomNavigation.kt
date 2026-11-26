package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.core.utilities.OmniLocalization
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue

/**
 * 19. GlassBottomNavigation — Ultra-premium floating 3D glass bottom navigation bar with active
 *     blue pill indicator and central glowing neon QR orb button (reproducing the reference image).
 */
@Composable
fun rememberFeedChromeNestedScrollConnection(
  listState: LazyListState,
  onChromeVisibilityChange: (Boolean) -> Unit,
): NestedScrollConnection {
  val isAtTop by remember(listState) {
    derivedStateOf {
      listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
    }
  }
  LaunchedEffect(isAtTop) {
    if (isAtTop) {
      onChromeVisibilityChange(true)
    }
  }
  return remember(listState, onChromeVisibilityChange) {
    object : NestedScrollConnection {
      override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
      ): Offset {
        if (!listState.canScrollForward && !listState.canScrollBackward) {
          return Offset.Zero
        }
        val dy = available.y
        val currentlyAtTop =
          listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        if (currentlyAtTop && dy >= 0f) {
          onChromeVisibilityChange(true)
        } else if (dy < -10f && !currentlyAtTop) {
          // Scrolling down into feed: quickly hide header & bottom menu
          onChromeVisibilityChange(false)
        } else if (dy > 6f) {
          // Scrolling up: rapidly re-display header & bottom menu
          onChromeVisibilityChange(true)
        }
        return Offset.Zero
      }

      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
      ): Offset {
        val currentlyAtTop =
          listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        if (currentlyAtTop) {
          onChromeVisibilityChange(true)
        }
        return Offset.Zero
      }
    }
  }
}

enum class OmniNavTab {
  HOME,
  SERVICES,
  MEMBER_CARD_OR_SCANNER,
  NOTIFICATIONS,
  PROFILE,
}

@Composable
fun GlassBottomNavigation(
  selectedTab: OmniNavTab,
  unreadNotificationsCount: Int,
  onTabSelected: (OmniNavTab) -> Unit,
  onCenterQrOrbClick: () -> Unit,
  modifier: Modifier = Modifier,
  userAvatarUrl: String? = null,
  userDisplayName: String = "",
  currentLanguageCode: String = "ht",
  isScannerMode: Boolean = false,
) {
  val homeLabel = remember(currentLanguageCode) { OmniLocalization.navHome(currentLanguageCode) }
  val servicesLabel = remember(currentLanguageCode) { OmniLocalization.navServices(currentLanguageCode) }
  val alertsLabel = remember(currentLanguageCode) { OmniLocalization.navNotifications(currentLanguageCode) }
  val profileLabel = remember(currentLanguageCode) { OmniLocalization.navProfile(currentLanguageCode) }
  val qrCenterDescription =
    remember(currentLanguageCode, isScannerMode) {
      OmniLocalization.navQrCenterDescription(currentLanguageCode, isScannerMode)
    }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    // Glass pill bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(68.dp)
          .shadow(
            elevation = 20.dp,
            shape = RoundedCornerShape(28.dp),
            ambientColor = ElectricBlue,
            spotColor = DeepNavy,
          )
          .clip(RoundedCornerShape(28.dp))
          .background(
            Brush.verticalGradient(
              listOf(
                Color(0xE6152F61),
                Color(0xF2081630),
                Color(0xFA040C1C),
              )
            )
          )
          .border(
            1.3.dp,
            Brush.linearGradient(
              listOf(
                GlassWhite.copy(alpha = 0.50f),
                ElectricBlue.copy(alpha = 0.35f),
                BrandGold.copy(alpha = 0.25f),
              )
            ),
            RoundedCornerShape(28.dp),
          )
          .padding(horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        BottomNavTabItem(
          label = homeLabel,
          icon = Icons.Default.Home,
          isSelected = selectedTab == OmniNavTab.HOME,
          onClick = { onTabSelected(OmniNavTab.HOME) },
          testTag = "nav_tab_home",
          modifier = Modifier.weight(1f),
        )
        BottomNavTabItem(
          label = servicesLabel,
          icon = Icons.Default.CalendarMonth,
          isSelected = selectedTab == OmniNavTab.SERVICES,
          onClick = { onTabSelected(OmniNavTab.SERVICES) },
          testTag = "nav_tab_services",
          modifier = Modifier.weight(1f),
        )
      }

      // Spacer for center floating QR Orb
      Spacer(modifier = Modifier.width(68.dp))

      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        BottomNavTabItem(
          label = alertsLabel,
          icon = Icons.Default.Notifications,
          isSelected = selectedTab == OmniNavTab.NOTIFICATIONS,
          badgeCount = unreadNotificationsCount,
          onClick = { onTabSelected(OmniNavTab.NOTIFICATIONS) },
          testTag = "nav_tab_notifications",
          modifier = Modifier.weight(1f),
        )
        BottomNavTabItem(
          label = profileLabel,
          icon = Icons.Default.Person,
          isSelected = selectedTab == OmniNavTab.PROFILE,
          onClick = { onTabSelected(OmniNavTab.PROFILE) },
          testTag = "nav_tab_profile",
          useUserAvatar = true,
          userAvatarUrl = userAvatarUrl,
          userDisplayName = userDisplayName,
          modifier = Modifier.weight(1f),
        )
      }
    }

    // Center Floating 3D Neon QR Orb
    Box(
      modifier =
        Modifier.offset(y = (-10).dp)
          .size(66.dp)
          .shadow(
            elevation = 22.dp,
            shape = CircleShape,
            ambientColor = BlueNeon,
            spotColor = ElectricBlue,
          )
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(BlueNeon, ElectricBlue, RoyalBlue)
            )
          )
          .border(
            width = 2.6.dp,
            brush = Brush.verticalGradient(listOf(PureWhite, BlueNeon, ElectricBlue)),
            shape = CircleShape,
          )
          .clickable(onClick = onCenterQrOrbClick)
          .testTag("nav_center_qr_orb"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.QrCode2,
        contentDescription = qrCenterDescription,
        tint = PureWhite,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

@Composable
private fun BottomNavTabItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  useUserAvatar: Boolean = false,
  userAvatarUrl: String? = null,
  userDisplayName: String = "",
) {
  val bgMod =
    if (isSelected) {
      Modifier.shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = BlueNeon)
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF22ACFF), ElectricBlue, RoyalBlue)
          )
        )
        .border(1.1.dp, PureWhite.copy(alpha = 0.78f), RoundedCornerShape(20.dp))
    } else {
      Modifier.clip(RoundedCornerShape(20.dp))
    }

  Box(
    modifier =
      modifier
        .defaultMinSize(minHeight = 48.dp)
        .then(bgMod)
        .clickable(onClick = onClick)
        .padding(horizontal = 6.dp, vertical = 6.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box {
        if (useUserAvatar) {
          BottomNavUserAvatar(
            avatarUrl = userAvatarUrl,
            displayName = userDisplayName,
            contentDescription = label,
            isSelected = isSelected,
          )
        } else {
          Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.78f),
            modifier = Modifier.size(22.dp),
          )
        }
        if (badgeCount > 0) {
          GlassNotificationBadge(
            count = badgeCount,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
        color = if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.80f),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
      )
    }
  }
}

@Composable
private fun BottomNavUserAvatar(
  avatarUrl: String?,
  displayName: String,
  contentDescription: String,
  isSelected: Boolean,
) {
  val cleanUrl = remember(avatarUrl) { avatarUrl?.trim()?.takeIf { it.isNotEmpty() } }
  val decodedBitmap =
    remember(cleanUrl) {
      cleanUrl?.let { ImageCompressor.decodeBase64DataUrlToBitmap(it) }
    }
  var remoteLoadFailed by remember(cleanUrl) { mutableStateOf(false) }
  val initials =
    remember(displayName) {
      displayName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    }

  Box(
    modifier =
      Modifier.size(24.dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(
              BlueNeon,
              ElectricBlue,
              RoyalBlue,
            )
          )
        )
        .border(
          width = if (isSelected) 1.6.dp else 1.1.dp,
          color = if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.72f),
          shape = CircleShape,
        )
        .testTag("nav_tab_profile_avatar"),
    contentAlignment = Alignment.Center,
  ) {
    when {
      decodedBitmap != null -> {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      cleanUrl != null && !remoteLoadFailed -> {
        AsyncImage(
          model = cleanUrl,
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          onError = { remoteLoadFailed = true },
          modifier = Modifier.fillMaxSize(),
        )
      }
      initials.isNotBlank() -> {
        Text(
          text = initials,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
          color = PureWhite,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
        )
      }
      else -> {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = contentDescription,
          tint = PureWhite,
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}
