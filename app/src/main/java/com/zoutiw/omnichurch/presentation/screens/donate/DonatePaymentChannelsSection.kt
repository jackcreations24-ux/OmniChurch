package com.zoutiw.omnichurch.presentation.screens.donate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig.Channel
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun DonateChannelTabs(
  channels: List<Channel>,
  selectedChannel: Channel,
  copy: DonateScreenCopy,
  onSelectChannel: (Channel) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(DonateFlatSurface)
        .horizontalScroll(rememberScrollState())
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    channels.forEach { channel ->
      val isSelected = channel == selectedChannel
      val label = channelDisplayName(channel)
      val sublabel = channelSublabel(channel, copy)
      val icon = channelIcon(channel)
      val bgColor = if (isSelected) BrandGold else Color.Transparent
      val contentColor = if (isSelected) DeepNavy else GlassWhite.copy(alpha = 0.88f)
      val subColor = if (isSelected) DeepNavy.copy(alpha = 0.78f) else MutedGlassText

      Row(
        modifier =
          Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(
              role = Role.Tab,
              onClick = { onSelectChannel(channel) },
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("donate_channel_tab_${channel.name.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(17.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
          )
          Text(
            text = sublabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = subColor,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
internal fun DonateChannelDetailsSurface(
  channel: Channel,
  config: DeveloperDonationConfig,
  copy: DonateScreenCopy,
  copiedKey: String?,
  onCopy: (key: String, label: String, value: String) -> Unit,
  onOpenExternalLink: (String) -> Unit,
) {
  val rows =
    buildList {
      when (channel) {
        Channel.PAYPAL -> {
          if (config.paypalEmail.isNotBlank()) {
            add(Triple("paypal_email", copy.paypalEmailLabel, config.paypalEmail))
          }
          if (config.paypalMeUrl.isNotBlank()) {
            add(Triple("paypal_me", copy.paypalMeLabel, config.paypalMeUrl))
          }
        }
        Channel.BINANCE -> {
          if (config.binancePayId.isNotBlank()) {
            add(Triple("binance_pay_id", copy.binancePayIdLabel, config.binancePayId))
          }
          if (config.binanceUsdtAddress.isNotBlank()) {
            add(Triple("binance_usdt", copy.binanceUsdtLabel, config.binanceUsdtAddress))
          }
          if (config.binanceNetwork.isNotBlank()) {
            add(Triple("binance_network", copy.binanceNetworkLabel, config.binanceNetwork))
          }
        }
        Channel.WISE -> {
          if (config.wiseEmail.isNotBlank()) {
            add(Triple("wise_email", copy.wiseEmailLabel, config.wiseEmail))
          }
          if (config.wiseTag.isNotBlank()) {
            add(Triple("wise_tag", copy.wiseTagLabel, config.wiseTag))
          }
          if (config.wiseIban.isNotBlank()) {
            add(Triple("wise_iban", copy.wiseIbanLabel, config.wiseIban))
          }
        }
        Channel.MONCASH -> {
          if (config.moncashNumber.isNotBlank()) {
            add(Triple("moncash_number", copy.moncashNumberLabel, config.moncashNumber))
          }
          if (config.moncashName.isNotBlank()) {
            add(Triple("moncash_name", copy.moncashNameLabel, config.moncashName))
          }
        }
        Channel.ZELLE -> {
          if (config.zelleInfo.isNotBlank()) {
            add(Triple("zelle_info", copy.zelleInfoLabel, config.zelleInfo))
          }
        }
      }
    }

  val validPaypalUrl =
    if (channel == Channel.PAYPAL) {
      DeveloperDonationConfig.sanitizeExternalHttpUrl(config.paypalMeUrl)
    } else {
      null
    }

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("donate_channel_details_surface"),
    shape = RoundedCornerShape(20.dp),
    color = DonateFlatSurface,
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(DonateFlatSurfaceElevated),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = channelIcon(channel),
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(19.dp),
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = channelFullTitle(channel),
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = channelSublabel(channel, copy),
            style = MaterialTheme.typography.labelSmall,
            color = BrandGold,
          )
        }
      }

      if (rows.isEmpty() && validPaypalUrl == null) {
        Text(
          text = copy.channelMissingDetails,
          style = MaterialTheme.typography.bodyMedium,
          color = MutedGlassText,
          modifier = Modifier.padding(vertical = 4.dp),
        )
      } else {
        rows.forEachIndexed { index, (key, label, value) ->
          if (index > 0) {
            HorizontalDivider(color = DonateFlatDivider, thickness = 0.8.dp)
          }
          DonateCopyableDetailRow(
            fieldKey = key,
            label = label,
            value = value,
            isCopied = copiedKey == key,
            copyLabel = copy.copyButtonCd,
            copiedLabel = copy.copiedFeedback,
            onCopy = { onCopy(key, label, value) },
          )
        }

        if (channel == Channel.MONCASH) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DonateFlatSurfaceElevated)
                .padding(horizontal = 14.dp, vertical = 10.dp),
          ) {
            Text(
              text = copy.moncashUssdHelper,
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
              color = GlassWhite.copy(alpha = 0.88f),
            )
          }
        }

        if (validPaypalUrl != null) {
          Spacer(modifier = Modifier.height(4.dp))
          Button(
            onClick = { onOpenExternalLink(validPaypalUrl) },
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = PureWhite,
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("donate_open_paypal_me_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.OpenInNew,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = copy.openPaypalMeLabel,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun DonateCopyableDetailRow(
  fieldKey: String,
  label: String,
  value: String,
  isCopied: Boolean,
  copyLabel: String,
  copiedLabel: String,
  onCopy: () -> Unit,
  leadingIcon: ImageVector? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (leadingIcon != null) {
      Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        tint = MutedGlassText,
        modifier = Modifier.size(18.dp),
      )
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MutedGlassText,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
    }

    Row(
      modifier =
        Modifier
          .heightIn(min = 48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(
            if (isCopied) SuccessGreen.copy(alpha = 0.16f) else DonateFlatSurfaceElevated
          )
          .clickable(
            role = Role.Button,
            onClick = onCopy,
          )
          .padding(horizontal = 12.dp, vertical = 10.dp)
          .testTag("donate_copy_$fieldKey"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
        contentDescription = if (isCopied) copiedLabel else "$copyLabel $label",
        tint = if (isCopied) SuccessGreen else BrandGold,
        modifier = Modifier.size(16.dp),
      )
      Text(
        text = if (isCopied) copiedLabel else copyLabel,
        style = MaterialTheme.typography.labelMedium,
        color = if (isCopied) SuccessGreen else PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
internal fun DonateEmptyChannelsBlock(copy: DonateScreenCopy) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("donate_empty_channels"),
    shape = RoundedCornerShape(20.dp),
    color = DonateFlatSurface,
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(DonateFlatSurfaceElevated),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.VolunteerActivism,
          contentDescription = null,
          tint = MutedGlassText,
          modifier = Modifier.size(22.dp),
        )
      }
      Text(
        text = copy.emptyChannelsTitle,
        style = MaterialTheme.typography.titleSmall,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = copy.emptyChannelsSubtitle,
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
        color = MutedGlassText,
      )
    }
  }
}

internal fun channelDisplayName(channel: Channel): String =
  when (channel) {
    Channel.PAYPAL -> "PayPal"
    Channel.BINANCE -> "Binance"
    Channel.WISE -> "Wise"
    Channel.MONCASH -> "MonCash"
    Channel.ZELLE -> "Zelle"
  }

internal fun channelSublabel(channel: Channel, copy: DonateScreenCopy): String =
  when (channel) {
    Channel.PAYPAL -> copy.paypalSublabel
    Channel.BINANCE -> copy.binanceSublabel
    Channel.WISE -> copy.wiseSublabel
    Channel.MONCASH -> copy.moncashSublabel
    Channel.ZELLE -> copy.zelleSublabel
  }

internal fun channelFullTitle(channel: Channel): String =
  when (channel) {
    Channel.PAYPAL -> "PayPal"
    Channel.BINANCE -> "Binance Pay / USDT"
    Channel.WISE -> "Wise Transfer"
    Channel.MONCASH -> "MonCash / Natcash"
    Channel.ZELLE -> "Zelle / Bank Transfer"
  }

internal fun channelIcon(channel: Channel): ImageVector =
  when (channel) {
    Channel.PAYPAL -> Icons.Default.Payments
    Channel.BINANCE -> Icons.Default.CurrencyBitcoin
    Channel.WISE -> Icons.Default.AccountBalance
    Channel.MONCASH -> Icons.Default.Smartphone
    Channel.ZELLE -> Icons.Default.AccountBalance
  }
