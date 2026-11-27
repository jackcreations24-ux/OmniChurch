package com.zoutiw.omnichurch.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Immutable
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.utilities.LocaleManager
import java.net.URI
import java.util.Locale

const val OMNICHURCH_OFFICIAL_SHARE_URL = "https://omnichurch.download"

@Immutable
data class LocalizedShareAppCopy(
  val header: String = "",
  val churchHeaderTemplate: String = "",
  val message: String = "",
  val contentFooterLabel: String = "",
  val chooserTitle: String = "",
)

/**
 * Real-time remote App Share configuration synchronized from
 * `app_config/developer_engine` -> nested field `shareApp` (or `appShare`) in the shared OmniChurch
 * named Firestore database (`ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1`).
 *
 * Allows the Web Developer Panel to control:
 * 1. The official share URL (`shareUrl` and optional Android-specific `androidUrl`, e.g. Play Store or direct link).
 * 2. The accompanying share message, headline, and content footer across all 4 supported languages (`ht`, `fr`, `en`, `es`).
 * 3. Optional placeholders `{church}`, `{url}`, and `{link}` inside custom messages.
 */
@Immutable
data class RemoteShareAppConfig(
  val enabled: Boolean = true,
  val shareUrl: String = OMNICHURCH_OFFICIAL_SHARE_URL,
  val androidUrl: String = "",
  val localizedCopy: Map<String, LocalizedShareAppCopy> = DEFAULT_LOCALIZED_COPY,
) {
  val effectiveShareUrl: String
    get() =
      sanitizeShareUrl(androidUrl)
        ?: sanitizeShareUrl(shareUrl)
        ?: OMNICHURCH_OFFICIAL_SHARE_URL

  fun resolveCopy(languageCode: String): LocalizedShareAppCopy {
    val norm =
      when (languageCode.trim().lowercase(Locale.ROOT)) {
        "fr" -> "fr"
        "en" -> "en"
        "es" -> "es"
        else -> "ht"
      }
    val defaultForLang = DEFAULT_LOCALIZED_COPY[norm] ?: DEFAULT_HT_COPY
    val candidate = localizedCopy[norm] ?: return defaultForLang
    return LocalizedShareAppCopy(
      header = candidate.header.trim().ifEmpty { defaultForLang.header },
      churchHeaderTemplate =
        candidate.churchHeaderTemplate.trim().ifEmpty { defaultForLang.churchHeaderTemplate },
      message = candidate.message.trim().ifEmpty { defaultForLang.message },
      contentFooterLabel =
        candidate.contentFooterLabel.trim().ifEmpty { defaultForLang.contentFooterLabel },
      chooserTitle = candidate.chooserTitle.trim().ifEmpty { defaultForLang.chooserTitle },
    )
  }

  fun buildGeneralAppShareText(
    languageCode: String = "ht",
    churchName: String? = null,
  ): Pair<String, String> {
    val copy = resolveCopy(languageCode)
    val url = effectiveShareUrl
    val cleanChurch = churchName?.trim()?.takeIf { it.isNotEmpty() }

    val rawHeadline =
      if (cleanChurch != null) {
        if (copy.churchHeaderTemplate.contains("{church}", ignoreCase = true)) {
          copy.churchHeaderTemplate.replace(Regex("\\{church\\}", RegexOption.IGNORE_CASE), cleanChurch)
        } else {
          copy.header.replace(Regex("\\{church\\}", RegexOption.IGNORE_CASE), cleanChurch)
        }
      } else {
        copy.header.replace(Regex("\\{church\\}", RegexOption.IGNORE_CASE), "OmniChurch Pro")
      }.trim()

    val formattedBody =
      copy.message
        .replace(Regex("\\{church\\}", RegexOption.IGNORE_CASE), cleanChurch ?: "OmniChurch Pro")
        .replace(Regex("\\{url\\}|\\{link\\}", RegexOption.IGNORE_CASE), url)
        .trim()

    val bodyContainsUrl =
      copy.message.contains("{url}", ignoreCase = true) ||
        copy.message.contains("{link}", ignoreCase = true) ||
        formattedBody.contains(url, ignoreCase = true)

    val fullText = buildString {
      if (rawHeadline.isNotEmpty()) {
        append(rawHeadline)
      }
      if (formattedBody.isNotEmpty()) {
        if (isNotEmpty()) append("\n\n")
        append(formattedBody)
      }
      if (!bodyContainsUrl && url.isNotEmpty()) {
        if (isNotEmpty()) append("\n\n")
        if (copy.contentFooterLabel.isNotBlank()) {
          append(copy.contentFooterLabel.trim())
          append("\n")
        }
        append(url)
      }
    }
    return rawHeadline.ifEmpty { copy.chooserTitle } to fullText
  }

  fun buildContentShareText(
    title: String,
    body: String,
    languageCode: String = "ht",
  ): String {
    val copy = resolveCopy(languageCode)
    val url = effectiveShareUrl
    return buildString {
      append(title.trim())
      if (body.isNotBlank()) {
        append("\n\n")
        append(body.trim())
      }
      if (enabled && url.isNotEmpty()) {
        append("\n\n")
        if (copy.contentFooterLabel.isNotBlank()) {
          append(copy.contentFooterLabel.trim())
          append("\n")
        }
        append(url)
      }
    }
  }

  companion object {
    val DEFAULT_HT_COPY =
      LocalizedShareAppCopy(
        header = "🔥 Dekouvri OmniChurch Pro — Legliz la nan pòch ou!",
        churchHeaderTemplate = "🔥 Rejwenn {church} sou OmniChurch Pro!",
        message =
          "Swiv tout sèvis adorasyon an dirèk, anons ofisyèl, kat idantite manm dijital QR, ak prezans legliz ou sou OmniChurch Pro.",
        contentFooterLabel = "📲 OmniChurch Pro (Legliz la nan pòch ou):",
        chooserTitle = "Pataje sou OmniChurch",
      )

    val DEFAULT_FR_COPY =
      LocalizedShareAppCopy(
        header = "🔥 Découvrez OmniChurch Pro — Votre église dans votre poche !",
        churchHeaderTemplate = "🔥 Rejoignez {church} sur OmniChurch Pro !",
        message =
          "Suivez les cultes en direct, les annonces officielles, votre carte de membre numérique QR et les présences de votre église sur OmniChurch Pro.",
        contentFooterLabel = "📲 OmniChurch Pro (Votre église connectée) :",
        chooserTitle = "Partager via OmniChurch",
      )

    val DEFAULT_EN_COPY =
      LocalizedShareAppCopy(
        header = "🔥 Discover OmniChurch Pro — Your church in your pocket!",
        churchHeaderTemplate = "🔥 Join {church} on OmniChurch Pro!",
        message =
          "Follow live worship services, official announcements, your digital QR member ID card, and church attendance on OmniChurch Pro.",
        contentFooterLabel = "📲 OmniChurch Pro (Your connected church):",
        chooserTitle = "Share via OmniChurch",
      )

    val DEFAULT_ES_COPY =
      LocalizedShareAppCopy(
        header = "🔥 ¡Descubre OmniChurch Pro — Tu iglesia en tu bolsillo!",
        churchHeaderTemplate = "🔥 ¡Únete a {church} en OmniChurch Pro!",
        message =
          "Sigue los cultos en vivo, anuncios oficiales, tu credencial digital QR de miembro y la asistencia de tu iglesia en OmniChurch Pro.",
        contentFooterLabel = "📲 OmniChurch Pro (Tu iglesia conectada):",
        chooserTitle = "Compartir con OmniChurch",
      )

    val DEFAULT_LOCALIZED_COPY: Map<String, LocalizedShareAppCopy> =
      mapOf(
        "ht" to DEFAULT_HT_COPY,
        "fr" to DEFAULT_FR_COPY,
        "en" to DEFAULT_EN_COPY,
        "es" to DEFAULT_ES_COPY,
      )

    fun sanitizeShareUrl(rawUrl: String?): String? {
      val trimmed = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
      val withScheme =
        when {
          trimmed.startsWith("https://", ignoreCase = true) -> trimmed
          trimmed.startsWith("http://", ignoreCase = true) ->
            "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
          !trimmed.contains("://") && trimmed.contains(".") && !trimmed.contains(" ") ->
            "https://$trimmed"
          else -> return null
        }
      return try {
        val uri = URI(withScheme)
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "https") return null
        if (
          host == "localhost" ||
            host == "127.0.0.1" ||
            host == "0.0.0.0" ||
            host.endsWith(".local") ||
            !host.contains(".")
        ) {
          return null
        }
        withScheme
      } catch (_: Exception) {
        null
      }
    }

    fun fromMap(rawMap: Map<*, *>?): RemoteShareAppConfig {
      if (rawMap == null) return RemoteShareAppConfig()

      fun bool(key: String, default: Boolean): Boolean =
        when (val v = rawMap[key]) {
          is Boolean -> v
          is String -> {
            when (v.trim().lowercase(Locale.ROOT)) {
              "true", "1", "yes" -> true
              "false", "0", "no" -> false
              else -> default
            }
          }
          is Number -> v.toInt() != 0
          else -> default
        }

      fun str(vararg keys: String): String {
        for (k in keys) {
          val v = (rawMap[k] as? String)?.trim()
          if (!v.isNullOrEmpty()) return v
        }
        return ""
      }

      val resolvedShareUrl =
        sanitizeShareUrl(
          str("shareUrl", "url", "appUrl", "downloadUrl", "link", "webUrl")
        ) ?: OMNICHURCH_OFFICIAL_SHARE_URL

      val resolvedAndroidUrl =
        sanitizeShareUrl(
          str("androidUrl", "playStoreUrl", "apkUrl", "mobileUrl")
        ).orEmpty()

      val rawLocalized = rawMap["localizedCopy"] as? Map<*, *>

      fun parseLangCopy(
        langKey: String,
        suffix: String,
        defaultCopy: LocalizedShareAppCopy,
        allowTopLevelFallback: Boolean,
      ): LocalizedShareAppCopy {
        val nested = rawLocalized?.get(langKey) as? Map<*, *>
        fun nestedStr(vararg keys: String): String {
          if (nested != null) {
            for (k in keys) {
              val v = (nested[k] as? String)?.trim()
              if (!v.isNullOrEmpty()) return v
            }
          }
          return ""
        }

        val header =
          nestedStr("header", "title", "headline")
            .ifEmpty { str("header$suffix", "title$suffix") }
            .ifEmpty { if (allowTopLevelFallback) str("header", "title", "headline") else "" }
            .ifEmpty { defaultCopy.header }

        val churchHeaderTemplate =
          nestedStr("churchHeaderTemplate", "churchHeader", "churchTitle")
            .ifEmpty { str("churchHeaderTemplate$suffix", "churchHeader$suffix") }
            .ifEmpty {
              if (allowTopLevelFallback) str("churchHeaderTemplate", "churchHeader") else ""
            }
            .ifEmpty { defaultCopy.churchHeaderTemplate }

        val message =
          nestedStr("message", "body", "description", "text")
            .ifEmpty { str("message$suffix", "body$suffix", "text$suffix") }
            .ifEmpty {
              if (allowTopLevelFallback) str("message", "body", "description", "text") else ""
            }
            .ifEmpty { defaultCopy.message }

        val contentFooterLabel =
          nestedStr("contentFooterLabel", "footerLabel", "footer", "tagline")
            .ifEmpty { str("contentFooterLabel$suffix", "footerLabel$suffix", "footer$suffix") }
            .ifEmpty {
              if (allowTopLevelFallback) {
                str("contentFooterLabel", "footerLabel", "footer", "tagline")
              } else {
                ""
              }
            }
            .ifEmpty { defaultCopy.contentFooterLabel }

        val chooserTitle =
          nestedStr("chooserTitle", "dialogTitle", "shareTitle")
            .ifEmpty { str("chooserTitle$suffix", "shareTitle$suffix") }
            .ifEmpty {
              if (allowTopLevelFallback) str("chooserTitle", "dialogTitle", "shareTitle") else ""
            }
            .ifEmpty { defaultCopy.chooserTitle }

        return LocalizedShareAppCopy(
          header = header,
          churchHeaderTemplate = churchHeaderTemplate,
          message = message,
          contentFooterLabel = contentFooterLabel,
          chooserTitle = chooserTitle,
        )
      }

      val localizedMap =
        mapOf(
          "ht" to parseLangCopy("ht", "Ht", DEFAULT_HT_COPY, allowTopLevelFallback = true),
          "fr" to parseLangCopy("fr", "Fr", DEFAULT_FR_COPY, allowTopLevelFallback = false),
          "en" to parseLangCopy("en", "En", DEFAULT_EN_COPY, allowTopLevelFallback = false),
          "es" to parseLangCopy("es", "Es", DEFAULT_ES_COPY, allowTopLevelFallback = false),
        )

      return RemoteShareAppConfig(
        enabled = bool("enabled", true),
        shareUrl = resolvedShareUrl,
        androidUrl = resolvedAndroidUrl,
        localizedCopy = localizedMap,
      )
    }
  }
}

private fun resolveCurrentShareConfigAndLanguage(
  context: Context,
  explicitLanguageCode: String?,
): Pair<RemoteShareAppConfig, String> {
  val config =
    runCatching { AdMobRemoteController.getInstance(context).shareAppConfigState.value }
      .getOrDefault(RemoteShareAppConfig())
  val lang =
    explicitLanguageCode?.trim()?.takeIf { it.isNotEmpty() }
      ?: Locale.getDefault().language.trim().ifEmpty { "ht" }
  return config to lang
}

fun shareOmniChurchContent(
  context: Context,
  title: String,
  body: String,
  languageCode: String? = null,
) {
  val (shareConfig, resolvedLang) = resolveCurrentShareConfigAndLanguage(context, languageCode)
  val copy = shareConfig.resolveCopy(resolvedLang)
  val fullMessage =
    shareConfig.buildContentShareText(
      title = title,
      body = body,
      languageCode = resolvedLang,
    )
  runCatching {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, fullMessage)
      }
    val chooser =
      Intent.createChooser(sendIntent, copy.chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }.onFailure {
    copyOmniChurchTextToClipboard(context, title, fullMessage)
  }
}

fun shareOmniChurchGeneralApp(
  context: Context,
  churchName: String? = null,
  languageCode: String? = null,
) {
  val (shareConfig, resolvedLang) = resolveCurrentShareConfigAndLanguage(context, languageCode)
  val copy = shareConfig.resolveCopy(resolvedLang)
  val (subject, fullMessage) =
    shareConfig.buildGeneralAppShareText(
      languageCode = resolvedLang,
      churchName = churchName,
    )
  runCatching {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, fullMessage)
      }
    val chooser =
      Intent.createChooser(sendIntent, copy.chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }.onFailure {
    copyOmniChurchTextToClipboard(context, subject, fullMessage)
  }
}

fun copyOmniChurchTextToClipboard(
  context: Context,
  label: String,
  text: String,
) {
  runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Tèks la kopye avèk siksè!", Toast.LENGTH_SHORT).show()
  }
}
