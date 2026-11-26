package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite

/** 7. GlassInputField — Styled translucent text field with neon focus rim. */
@Composable
fun GlassInputField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  leadingIcon: ImageVector? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardType: KeyboardType? = null,
  singleLine: Boolean = true,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  testTag: String = "glass_input_field",
) {
  val effectiveKeyboardOptions =
    if (keyboardType != null) keyboardOptions.copy(keyboardType = keyboardType)
    else keyboardOptions

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, color = GlassWhite.copy(alpha = 0.85f)) },
    placeholder =
      if (placeholder.isNotBlank()) {
        { Text(placeholder, color = MutedGlassText.copy(alpha = 0.6f)) }
      } else null,
    leadingIcon =
      if (leadingIcon != null) {
        {
          Icon(
            imageVector = leadingIcon,
            contentDescription = label,
            tint = BlueNeon,
            modifier = Modifier.size(20.dp),
          )
        }
      } else null,
    trailingIcon = trailingIcon,
    visualTransformation = visualTransformation,
    singleLine = singleLine,
    keyboardOptions = effectiveKeyboardOptions,
    shape = RoundedCornerShape(16.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedContainerColor = Color(0x880B1D3D),
        unfocusedContainerColor = Color(0x6607142B),
        focusedBorderColor = BlueNeon,
        unfocusedBorderColor = GlassWhite.copy(alpha = 0.30f),
        cursorColor = BlueNeon,
      ),
    modifier = modifier.fillMaxWidth().testTag(testTag),
  )
}

/** 8. GlassDialog — Modal 3D glass dialog. */
@Composable
fun GlassDialog(
  onDismissRequest: () -> Unit,
  title: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Dialog(onDismissRequest = onDismissRequest) {
    PremiumGlassCard(
      cornerRadius = 24.dp,
      isHighlighted = true,
      accentColor = ElectricBlue,
      contentPadding = PaddingValues(20.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(14.dp))
      content()
    }
  }
}

/** 9. GlassModalBottomSheet — Translucent dark navy bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
  onDismissRequest: () -> Unit,
  title: String = "",
  content: @Composable ColumnScope.() -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = PrimaryNavy,
    contentColor = PureWhite,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp)
    ) {
      if (title.isNotBlank()) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
      }
      content()
    }
  }
}

/**
 * 10. PremiumProfileCard — Member header bar showing real member photo (or 3D avatar),
 *     personalized greeting ("Bonjou, Jackson 👋"), church role & compact animated Donate pill button.
 */
@Composable
fun PremiumProfileCard(
  memberName: String,
  subtitle: String,
  onProfileClick: () -> Unit,
  onDonateClick: () -> Unit,
  donationConfig: DeveloperDonationConfig = DeveloperDonationConfig(),
  currentLanguageCode: String = "ht",
  photoUrl: String? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        Modifier.weight(1f)
          .clip(RoundedCornerShape(50))
          .clickable(onClick = onProfileClick)
          .padding(vertical = 4.dp)
          .testTag("header_profile_row"),
    ) {
      MemberAvatar3D(
        fullName = memberName,
        size = 50.dp,
        photoUrl = photoUrl,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(R.string.home_greeting, memberName.substringBefore(" ")),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(text = " 👋", fontSize = 17.sp)
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.85f),
        )
      }
    }

    if (donationConfig.enabled) {
      Spacer(modifier = Modifier.width(8.dp))
      HeaderDonateButton(
        donationConfig = donationConfig,
        currentLanguageCode = currentLanguageCode,
        onClick = onDonateClick,
      )
    }
  }
}

/**
 * 11. PremiumSectionHeader — Reusable section header with bold white title on the left
 *     and golden-orange "Tout >" action on the right.
 */
@Composable
fun PremiumSectionHeader(
  title: String,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  trailingExtraContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (trailingExtraContent != null) {
        trailingExtraContent()
      }
      if (actionText != null && onActionClick != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable(onClick = onActionClick)
              .padding(horizontal = 6.dp, vertical = 4.dp)
              .testTag("feed_see_all_button"),
        ) {
          Text(
            text = actionText,
            style = MaterialTheme.typography.titleSmall,
            color = BrandGold,
            fontWeight = FontWeight.Bold,
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
