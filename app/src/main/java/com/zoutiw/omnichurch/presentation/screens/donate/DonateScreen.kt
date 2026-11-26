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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.donations.resolveLocalizedDonationCopy
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

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
  val enabledChannels = remember(donationConfig) { donationConfig.enabledChannels }

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

  val copyToClipboard =
    remember(context) {
      { key: String, label: String, value: String ->
        val clean = value.trim()
        if (clean.isNotEmpty()) {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          cm?.setPrimaryClip(ClipData.newPlainText(label, clean))
          copiedKey = key
        }
      }
    }

  val openExternalLink =
    remember(context, copy.invalidLinkMessage) {
      { rawUrl: String ->
        val safeUrl = DeveloperDonationConfig.sanitizeExternalHttpUrl(rawUrl)
        if (safeUrl != null) {
          val opened =
            runCatching {
              val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
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
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = DonateFlatCanvas,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = DonateFlatCanvas,
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
