package com.zoutiw.omnichurch.presentation.screens.donate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig.Channel
import com.zoutiw.omnichurch.core.donations.LocalizedDonationCopy
import com.zoutiw.omnichurch.core.donations.resolveLocalizedDonationCopy
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

// Quiet, borderless flat tonal hierarchy harmonized with OmniChurch DeepNavy (#030B1C)
private val FlatCanvas = DeepNavy
private val FlatSurface = Color(0xFF0A1730)
private val FlatSurfaceElevated = Color(0xFF102245)
private val FlatDivider = Color(0x14DCEBFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateScreen(
  donationConfig: DeveloperDonationConfig,
  currentLanguageCode: String,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBackClick() }

  val context = LocalContext.current
  val copy = remember(currentLanguageCode) { donateScreenCopyFor(currentLanguageCode) }
  val localizedDonationCopy =
    remember(donationConfig, currentLanguageCode) {
      resolveLocalizedDonationCopy(donationConfig, currentLanguageCode)
    }
  val enabledChannels = donationConfig.enabledChannels

  var selectedChannel by remember { mutableStateOf(enabledChannels.firstOrNull()) }
  LaunchedEffect(enabledChannels) {
    if (selectedChannel == null || selectedChannel !in enabledChannels) {
      selectedChannel = enabledChannels.firstOrNull()
    }
  }

  var copiedKey by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(copiedKey) {
    if (copiedKey != null) {
      delay(2000L)
      copiedKey = null
    }
  }

  val copyToClipboard: (key: String, label: String, value: String) -> Unit = { key, label, value ->
    val clean = value.trim()
    if (clean.isNotEmpty()) {
      val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      cm?.setPrimaryClip(ClipData.newPlainText(label, clean))
      copiedKey = key
    }
  }

  val openExternalLink: (String) -> Unit = { rawUrl ->
    val safeUrl = DeveloperDonationConfig.sanitizeExternalHttpUrl(rawUrl)
    if (safeUrl != null) {
      val opened =
        runCatching {
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(intent)
        }.isSuccess
      if (!opened) {
        Toast.makeText(context, copy.invalidLinkMessage, Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, copy.invalidLinkMessage, Toast.LENGTH_SHORT).show()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = FlatCanvas,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = FlatCanvas,
            titleContentColor = PureWhite,
            navigationIconContentColor = PureWhite,
          ),
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp).testTag("donate_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = copy.backContentDescription,
              tint = PureWhite,
            )
          }
        },
        title = {
          Text(
            text = copy.topBarTitle,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        actions = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(end = 14.dp),
          ) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = copy.verifiedOfficialBadgeCd,
              tint = SuccessGreen,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = copy.verifiedLabel,
              style = MaterialTheme.typography.labelSmall,
              color = SuccessGreen,
              fontWeight = FontWeight.SemiBold,
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 36.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        // 1. Editorial Hero — sits directly on the flat DeepNavy canvas, zero box or border
        DonateHeroSection(
          kicker = copy.heroKicker,
          title = localizedDonationCopy.screenTitle,
          subtitle = localizedDonationCopy.screenSubtitle,
        )

        // 2. Interactive Payment Channels
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = copy.channelsSectionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = BrandGold,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
          )

          if (enabledChannels.isEmpty()) {
            DonateEmptyChannelsBlock(copy = copy)
          } else {
            val activeChannel = selectedChannel ?: enabledChannels.first()

            DonateChannelTabs(
              channels = enabledChannels,
              selectedChannel = activeChannel,
              copy = copy,
              onSelectChannel = { selectedChannel = it },
            )

            AnimatedContent(
              targetState = activeChannel,
              transitionSpec = { fadeIn() togetherWith fadeOut() },
              label = "donate_channel_details",
            ) { channel ->
              DonateChannelDetailsSurface(
                channel = channel,
                config = donationConfig,
                copy = copy,
                copiedKey = copiedKey,
                onCopy = copyToClipboard,
                onOpenExternalLink = openExternalLink,
              )
            }
          }
        }

        // 3. Developer Profile & Vision
        DonateDeveloperSection(
          config = donationConfig,
          localizedCopy = localizedDonationCopy,
          copy = copy,
          copiedKey = copiedKey,
          onCopy = copyToClipboard,
        )

        // 4. Gratitude / Scripture Footer — calm editorial quote on flat tonal surface
        DonateGratitudeFooter(
          sectionLabel = copy.gratitudeSectionLabel,
          customNote = localizedDonationCopy.customNote,
          fallbackQuote = copy.scriptureQuote,
          fallbackRef = copy.scriptureRef,
        )
      }
    }
  }
}

@Composable
private fun DonateHeroSection(
  kicker: String,
  title: String,
  subtitle: String,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(BrandGold.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Favorite,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(20.dp),
        )
      }
      Text(
        text = kicker,
        style = MaterialTheme.typography.labelMedium,
        color = BrandGold,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
      )
    }

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 32.sp),
      color = PureWhite,
      fontWeight = FontWeight.ExtraBold,
      modifier = Modifier.testTag("donate_hero_title"),
    )

    if (subtitle.isNotBlank()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = MutedGlassText,
        modifier = Modifier.testTag("donate_hero_subtitle"),
      )
    }
  }
}

@Composable
private fun DonateChannelTabs(
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
        .background(FlatSurface)
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
private fun DonateChannelDetailsSurface(
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
    color = FlatSurface,
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
              .background(FlatSurfaceElevated),
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
            HorizontalDivider(color = FlatDivider, thickness = 0.8.dp)
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
                .background(FlatSurfaceElevated)
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
private fun DonateCopyableDetailRow(
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
            if (isCopied) SuccessGreen.copy(alpha = 0.16f) else FlatSurfaceElevated
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
private fun DonateEmptyChannelsBlock(copy: DonateScreenCopy) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("donate_empty_channels"),
    shape = RoundedCornerShape(20.dp),
    color = FlatSurface,
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
            .background(FlatSurfaceElevated),
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

@Composable
private fun DonateDeveloperSection(
  config: DeveloperDonationConfig,
  localizedCopy: LocalizedDonationCopy,
  copy: DonateScreenCopy,
  copiedKey: String?,
  onCopy: (key: String, label: String, value: String) -> Unit,
) {
  val initials =
    remember(config.developerName) {
      config.developerName
        .trim()
        .split(Regex("\\s+"))
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)
        .ifEmpty { "OC" }
    }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = copy.developerSectionLabel,
      style = MaterialTheme.typography.labelLarge,
      color = BrandGold,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.4.sp,
    )

    Surface(
      modifier = Modifier.fillMaxWidth().testTag("donate_developer_section"),
      shape = RoundedCornerShape(20.dp),
      color = FlatSurface,
      tonalElevation = 0.dp,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(BrandGold.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = initials,
              style = MaterialTheme.typography.titleMedium,
              color = BrandGold,
              fontWeight = FontWeight.ExtraBold,
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = config.developerName,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            if (localizedCopy.developerRole.isNotBlank()) {
              Text(
                text = localizedCopy.developerRole,
                style = MaterialTheme.typography.bodySmall,
                color = BrandGold,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }

        if (localizedCopy.developerBio.isNotBlank()) {
          Text(
            text = localizedCopy.developerBio,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = GlassWhite.copy(alpha = 0.90f),
          )
        }

        val hasEmail = config.developerEmail.isNotBlank()
        val hasPhone = config.developerPhone.isNotBlank()
        if (hasEmail || hasPhone) {
          HorizontalDivider(color = FlatDivider, thickness = 0.8.dp)
          Text(
            text = copy.directContactLabel,
            style = MaterialTheme.typography.labelMedium,
            color = BrandGold,
            fontWeight = FontWeight.SemiBold,
          )
        }

        if (hasEmail) {
          DonateCopyableDetailRow(
            fieldKey = "developer_email",
            label = copy.developerEmailLabel,
            value = config.developerEmail,
            isCopied = copiedKey == "developer_email",
            copyLabel = copy.copyButtonCd,
            copiedLabel = copy.copiedFeedback,
            onCopy = { onCopy("developer_email", copy.developerEmailLabel, config.developerEmail) },
            leadingIcon = Icons.Default.Email,
          )
        }

        if (hasEmail && hasPhone) {
          HorizontalDivider(color = FlatDivider, thickness = 0.8.dp)
        }

        if (hasPhone) {
          DonateCopyableDetailRow(
            fieldKey = "developer_phone",
            label = copy.developerPhoneLabel,
            value = config.developerPhone,
            isCopied = copiedKey == "developer_phone",
            copyLabel = copy.copyButtonCd,
            copiedLabel = copy.copiedFeedback,
            onCopy = { onCopy("developer_phone", copy.developerPhoneLabel, config.developerPhone) },
            leadingIcon = Icons.Default.Phone,
          )
        }
      }
    }
  }
}

@Composable
private fun DonateGratitudeFooter(
  sectionLabel: String,
  customNote: String,
  fallbackQuote: String,
  fallbackRef: String,
) {
  val hasCustomNote = customNote.isNotBlank()

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("donate_gratitude_footer"),
    shape = RoundedCornerShape(20.dp),
    color = FlatSurface,
    tonalElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = sectionLabel,
        style = MaterialTheme.typography.labelMedium,
        color = BrandGold,
        fontWeight = FontWeight.SemiBold,
      )

      if (hasCustomNote) {
        Text(
          text = customNote,
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
          color = PureWhite,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        Box(
          modifier =
            Modifier
              .width(3.dp)
              .height(48.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(BrandGold.copy(alpha = 0.65f)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "“$fallbackQuote”",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
            fontStyle = FontStyle.Italic,
            color = GlassWhite.copy(alpha = 0.82f),
          )
          Text(
            text = "— $fallbackRef",
            style = MaterialTheme.typography.labelSmall,
            color = BrandGold,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

private fun channelDisplayName(channel: Channel): String =
  when (channel) {
    Channel.PAYPAL -> "PayPal"
    Channel.BINANCE -> "Binance"
    Channel.WISE -> "Wise"
    Channel.MONCASH -> "MonCash"
    Channel.ZELLE -> "Zelle"
  }

private fun channelSublabel(channel: Channel, copy: DonateScreenCopy): String =
  when (channel) {
    Channel.PAYPAL -> copy.paypalSublabel
    Channel.BINANCE -> copy.binanceSublabel
    Channel.WISE -> copy.wiseSublabel
    Channel.MONCASH -> copy.moncashSublabel
    Channel.ZELLE -> copy.zelleSublabel
  }

private fun channelFullTitle(channel: Channel): String =
  when (channel) {
    Channel.PAYPAL -> "PayPal"
    Channel.BINANCE -> "Binance Pay / USDT"
    Channel.WISE -> "Wise Transfer"
    Channel.MONCASH -> "MonCash / Natcash"
    Channel.ZELLE -> "Zelle / Bank Transfer"
  }

private fun channelIcon(channel: Channel): ImageVector =
  when (channel) {
    Channel.PAYPAL -> Icons.Default.Payments
    Channel.BINANCE -> Icons.Default.CurrencyBitcoin
    Channel.WISE -> Icons.Default.AccountBalance
    Channel.MONCASH -> Icons.Default.Smartphone
    Channel.ZELLE -> Icons.Default.AccountBalance
  }
