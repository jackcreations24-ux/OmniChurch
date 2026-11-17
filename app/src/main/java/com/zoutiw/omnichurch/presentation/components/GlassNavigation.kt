package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue

enum class OmniNavTab {
  HOME,
  SERVICES,
  MEMBER_CARD_OR_SCANNER,
  NOTIFICATIONS,
  PROFILE,
}

@Composable
fun GlassTopBar(
  title: String,
  onBackClick: (() -> Unit)? = null,
  rightBadgeText: String? = null,
  onMenuClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      if (onBackClick != null) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.size(48.dp).testTag("top_bar_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.common_close),
            tint = PureWhite,
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    if (rightBadgeText != null) {
      Row(
        modifier =
          Modifier.shadow(8.dp, RoundedCornerShape(50), ambientColor = BrandOrange)
            .clip(RoundedCornerShape(50))
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF6B4209), Color(0xFF9E630D), Color(0xFF523206))
              )
            )
            .border(
              1.3.dp,
              Brush.horizontalGradient(listOf(PureWhite.copy(alpha = 0.7f), BrandGold, BrandOrange)),
              RoundedCornerShape(50),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "👑 ",
          fontSize = 13.sp,
        )
        Text(
          text = rightBadgeText,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    } else if (onMenuClick != null) {
      IconButton(
        onClick = onMenuClick,
        modifier = Modifier.size(48.dp).testTag("top_bar_menu_button"),
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = stringResource(R.string.profile_screen_title),
          tint = PureWhite,
        )
      }
    }
  }
}

@Composable
fun GlassBottomNavigation(
  selectedTab: OmniNavTab,
  unreadNotificationsCount: Int,
  onTabSelected: (OmniNavTab) -> Unit,
  onCenterQrOrbClick: () -> Unit,
  isAdmin: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
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
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      BottomNavTabItem(
        label = stringResource(R.string.nav_home),
        icon = Icons.Default.Home,
        isSelected = selectedTab == OmniNavTab.HOME,
        onClick = { onTabSelected(OmniNavTab.HOME) },
        testTag = "nav_tab_home",
      )
      BottomNavTabItem(
        label = stringResource(R.string.nav_services),
        icon = Icons.Default.CalendarMonth,
        isSelected = selectedTab == OmniNavTab.SERVICES,
        onClick = { onTabSelected(OmniNavTab.SERVICES) },
        testTag = "nav_tab_services",
      )

      Spacer(modifier = Modifier.width(64.dp))

      BottomNavTabItem(
        label = stringResource(R.string.nav_notifications),
        icon = Icons.Default.Notifications,
        isSelected = selectedTab == OmniNavTab.NOTIFICATIONS,
        badgeCount = unreadNotificationsCount,
        onClick = { onTabSelected(OmniNavTab.NOTIFICATIONS) },
        testTag = "nav_tab_notifications",
      )
      BottomNavTabItem(
        label = stringResource(R.string.nav_profile),
        icon = Icons.Default.Person,
        isSelected = selectedTab == OmniNavTab.PROFILE,
        onClick = { onTabSelected(OmniNavTab.PROFILE) },
        testTag = "nav_tab_profile",
      )
    }

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
        contentDescription = if (isAdmin) "Lis Manm & Eskane QR" else stringResource(R.string.nav_qr_center),
        tint = PureWhite,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

@Composable
fun BottomNavTabItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit,
  testTag: String,
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
      bgMod
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.78f),
          modifier = Modifier.size(22.dp),
        )
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
      )
    }
  }
}
