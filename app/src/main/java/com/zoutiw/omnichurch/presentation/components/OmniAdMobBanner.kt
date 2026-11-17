package com.zoutiw.omnichurch.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.ads.EffectiveMobileAdMobState
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Evaluates whether a banner ad should be rendered for the given [placement] based on
 * the [AdMobRemoteController] state (`shouldShowAds`, `enabled`, `mobileEnabled`, `!isChurchAdFree`)
 * and the specific `mobileShowBanner*` flags from the remote `app_config/developer_engine` configuration.
 */
fun isPlacementBannerEnabled(
  placement: OmniAdPlacement,
  adMobState: EffectiveMobileAdMobState,
): Boolean {
  val config = adMobState.config
  val masterAllowed =
    adMobState.shouldShowAds &&
      config.enabled &&
      config.mobileEnabled &&
      !adMobState.isChurchAdFree

  if (!masterAllowed) return false

  val placementAllowed =
    when (placement) {
      OmniAdPlacement.HOME -> config.mobileShowBannerHome
      OmniAdPlacement.ANNOUNCEMENTS -> config.mobileShowBannerAnnouncements
      OmniAdPlacement.SERVICES -> config.mobileShowBannerServices
    }

  return placementAllowed && adMobState.effectiveBannerId.isNotBlank()
}

/**
 * Reusable Compose AdMob Banner (`OmniAdMobBanner`) synchronized in real time with
 * the OmniChurch Pro Web Developer Engine (`app_config/developer_engine`).
 *
 * Accepts a [placement] enum (`HOME`, `ANNOUNCEMENTS`, `SERVICES`) and optionally an
 * [adMobState] or observes [AdMobRemoteController] directly when not passed.
 *
 * Conditional rendering rules:
 * - Checks master visibility (`shouldShowAds`: `config.enabled && config.mobileEnabled && !isChurchAdFree`)
 * - Checks placement-specific remote flag:
 *   - `OmniAdPlacement.HOME` -> `config.mobileShowBannerHome`
 *   - `OmniAdPlacement.ANNOUNCEMENTS` -> `config.mobileShowBannerAnnouncements`
 *   - `OmniAdPlacement.SERVICES` -> `config.mobileShowBannerServices`
 * - Collapses to `0.dp` height with zero layout overhead whenever disabled.
 */
@Composable
fun OmniAdMobBanner(
  placement: OmniAdPlacement,
  modifier: Modifier = Modifier,
  adMobState: EffectiveMobileAdMobState? = null,
) {
  val context = LocalContext.current
  val controllerState =
    if (adMobState != null) {
      adMobState
    } else {
      val controller = remember(context) { AdMobRemoteController.getInstance(context) }
      val liveState by controller.state.collectAsStateWithLifecycle()
      liveState
    }

  val shouldRenderPlacement =
    remember(placement, controllerState) {
      isPlacementBannerEnabled(placement = placement, adMobState = controllerState)
    }

  if (!shouldRenderPlacement) {
    return
  }

  val configuration = LocalConfiguration.current
  val isInspectionMode = LocalInspectionMode.current
  val effectiveBannerId = controllerState.effectiveBannerId
  val adWidthDp = (configuration.screenWidthDp - 28).coerceAtLeast(320)

  key(placement, effectiveBannerId, adWidthDp) {
    var hasAdFailed by remember(effectiveBannerId) { mutableStateOf(false) }

    val adView =
      remember(context, effectiveBannerId, adWidthDp) {
        if (isInspectionMode) {
          null
        } else {
          runCatching {
              AdMobRemoteController.getInstance(context).ensureMobileAdsInitialized(context)
              AdView(context).apply {
                setAdSize(
                  AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    adWidthDp,
                  )
                )
                adUnitId = effectiveBannerId
                adListener =
                  object : AdListener() {
                    override fun onAdLoaded() {
                      hasAdFailed = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                      hasAdFailed = true
                    }
                  }
                loadAd(AdRequest.Builder().build())
              }
            }
            .getOrNull()
        }
      }

    DisposableEffect(adView) {
      onDispose {
        runCatching { adView?.destroy() }
      }
    }

    AnimatedVisibility(
      visible = !hasAdFailed,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically(),
    ) {
      Column(
        modifier =
          modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD907142B))
            .border(0.8.dp, GlassWhite.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("omni_admob_banner_${placement.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "ESPAS PIBLISITE",
            style = MaterialTheme.typography.labelSmall,
            color = MutedGlassText,
            fontWeight = FontWeight.SemiBold,
          )
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(
                  if (controllerState.isTestMode) {
                    BrandGold.copy(alpha = 0.16f)
                  } else {
                    BlueNeon.copy(alpha = 0.16f)
                  }
                )
                .padding(horizontal = 8.dp, vertical = 2.dp),
          ) {
            Text(
              text = if (controllerState.isTestMode) "AdMob • Test Mode" else "AdMob",
              style = MaterialTheme.typography.labelSmall,
              color = if (controllerState.isTestMode) BrandGold else BlueNeon,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (adView != null) {
          AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adView },
          )
        }
      }
    }
  }
}
