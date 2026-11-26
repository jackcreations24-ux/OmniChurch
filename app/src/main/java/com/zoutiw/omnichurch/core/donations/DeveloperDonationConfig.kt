package com.zoutiw.omnichurch.core.donations

import java.util.Locale

data class LocalizedDonationCopy(
  val screenTitle: String = "",
  val screenSubtitle: String = "",
  val developerRole: String = "",
  val developerBio: String = "",
  val customNote: String = "",
)

/**
 * Real-time remote donation configuration synchronized from
 * `app_config/developer_engine` -> nested field `donations` in the shared OmniChurch
 * named Firestore database (`ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1`).
 */
data class DeveloperDonationConfig(
  val enabled: Boolean = true,
  val enableShakeAnimation: Boolean = true,
  val shakeIntervalSeconds: Int = DEFAULT_SHAKE_INTERVAL_SECONDS,
  val developerName: String = DEFAULT_DEVELOPER_NAME,
  val developerEmail: String = "",
  val developerPhone: String = "",
  // Legacy top-level HT fallback fields:
  val screenTitle: String = DEFAULT_HT_COPY.screenTitle,
  val screenSubtitle: String = DEFAULT_HT_COPY.screenSubtitle,
  val developerRole: String = DEFAULT_HT_COPY.developerRole,
  val developerBio: String = DEFAULT_HT_COPY.developerBio,
  val customNote: String = DEFAULT_HT_COPY.customNote,
  // 4-Language map synced directly from Web Dev Panel (app_config/developer_engine -> donations.localizedCopy.{ht,fr,en,es}):
  val localizedCopy: Map<String, LocalizedDonationCopy> = DEFAULT_LOCALIZED_COPY,
  val paypalEnabled: Boolean = false,
  val paypalEmail: String = "",
  val paypalMeUrl: String = "",
  val binanceEnabled: Boolean = false,
  val binancePayId: String = "",
  val binanceUsdtAddress: String = "",
  val binanceNetwork: String = "",
  val wiseEnabled: Boolean = false,
  val wiseEmail: String = "",
  val wiseTag: String = "",
  val wiseIban: String = "",
  val moncashEnabled: Boolean = false,
  val moncashNumber: String = "",
  val moncashName: String = "",
  val zelleEnabled: Boolean = false,
  val zelleInfo: String = "",
) {
  enum class Channel {
    PAYPAL,
    BINANCE,
    WISE,
    MONCASH,
    ZELLE,
  }

  val enabledChannels: List<Channel>
    get() =
      buildList {
        if (paypalEnabled) add(Channel.PAYPAL)
        if (binanceEnabled) add(Channel.BINANCE)
        if (wiseEnabled) add(Channel.WISE)
        if (moncashEnabled) add(Channel.MONCASH)
        if (zelleEnabled) add(Channel.ZELLE)
      }

  companion object {
    const val DEFAULT_SHAKE_INTERVAL_SECONDS = 6
    const val MIN_SHAKE_INTERVAL_SECONDS = 3
    const val MAX_SHAKE_INTERVAL_SECONDS = 30
    const val DEFAULT_DEVELOPER_NAME = "Jackson Charles"

    val DEFAULT_HT_COPY =
      LocalizedDonationCopy(
        screenTitle = "Soutni Misyon OmniChurch Pro",
        screenSubtitle =
          "Chak donasyon ede nou kenbe sèvè yo gratis, rapid epi devlope nouvo zouti pou legliz yo.",
        developerRole = "Fondatè & Enjenyè Full-Stack",
        developerBio =
          "Enjenyè lojisyèl k ap bati sistèm dijital modèn pou legliz yo ak kominote kretyèn yo atravè lemond.",
        customNote = "Mèsi anpil pou jenerozite ou ak priyè ou. Se Bondye ki pou beni ou!",
      )

    val DEFAULT_FR_COPY =
      LocalizedDonationCopy(
        screenTitle = "Soutenir la Mission OmniChurch Pro",
        screenSubtitle =
          "Chaque don nous aide à maintenir les serveurs rapides, fiables et à développer de nouveaux outils pour les églises.",
        developerRole = "Fondateur & Ingénieur Full-Stack",
        developerBio =
          "Ingénieur logiciel construisant des systèmes numériques modernes pour les églises et communautés chrétiennes à travers le monde.",
        customNote =
          "Merci infiniment pour votre générosité et vos prières. Que Dieu vous bénisse abondamment !",
      )

    val DEFAULT_EN_COPY =
      LocalizedDonationCopy(
        screenTitle = "Support the OmniChurch Pro Mission",
        screenSubtitle =
          "Every donation helps keep our servers fast, reliable, and fuels new tools for churches worldwide.",
        developerRole = "Founder & Full-Stack Engineer",
        developerBio =
          "Software engineer building modern digital ministry platforms for churches and Christian communities worldwide.",
        customNote =
          "Thank you sincerely for your generosity and prayers. May God bless you abundantly!",
      )

    val DEFAULT_ES_COPY =
      LocalizedDonationCopy(
        screenTitle = "Apoya la Misión de OmniChurch Pro",
        screenSubtitle =
          "Cada donación nos ayuda a mantener los servidores rápidos, seguros y crear nuevas herramientas para las iglesias.",
        developerRole = "Fundador e Ingeniero Full-Stack",
        developerBio =
          "Ingeniero de software construyendo plataformas digitales modernas para iglesias y comunidades cristianas en todo el mundo.",
        customNote =
          "Muchas gracias por tu generosidad y tus oraciones. ¡Que Dios te bendiga abundantemente!",
      )

    val DEFAULT_LOCALIZED_COPY: Map<String, LocalizedDonationCopy> =
      mapOf(
        "ht" to DEFAULT_HT_COPY,
        "fr" to DEFAULT_FR_COPY,
        "en" to DEFAULT_EN_COPY,
        "es" to DEFAULT_ES_COPY,
      )

    fun fromMap(rawMap: Map<*, *>?): DeveloperDonationConfig {
      if (rawMap == null) return DeveloperDonationConfig()

      fun bool(key: String, default: Boolean): Boolean =
        when (val v = rawMap[key]) {
          is Boolean -> v
          is String -> {
            val trimmed = v.trim()
            when {
              trimmed.equals("true", ignoreCase = true) -> true
              trimmed.equals("false", ignoreCase = true) -> false
              else -> default
            }
          }
          is Number -> v.toInt() != 0
          else -> default
        }

      fun strNonBlank(key: String, default: String): String =
        (rawMap[key] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: default

      fun strOptional(key: String): String =
        (rawMap[key] as? String)?.trim().orEmpty()

      fun intVal(key: String, default: Int): Int =
        when (val v = rawMap[key]) {
          is Number -> v.toInt()
          is String -> v.trim().toIntOrNull() ?: default
          else -> default
        }

      val clampedInterval =
        intVal("shakeIntervalSeconds", DEFAULT_SHAKE_INTERVAL_SECONDS)
          .coerceIn(MIN_SHAKE_INTERVAL_SECONDS, MAX_SHAKE_INTERVAL_SECONDS)

      val topScreenTitle = strNonBlank("screenTitle", DEFAULT_HT_COPY.screenTitle)
      val topScreenSubtitle = strNonBlank("screenSubtitle", DEFAULT_HT_COPY.screenSubtitle)
      val topDeveloperRole = strNonBlank("developerRole", DEFAULT_HT_COPY.developerRole)
      val topDeveloperBio = strNonBlank("developerBio", DEFAULT_HT_COPY.developerBio)
      val topCustomNote = strNonBlank("customNote", DEFAULT_HT_COPY.customNote)

      val rawLocalized = rawMap["localizedCopy"] as? Map<*, *>
      val parsedLocalizedCopy =
        buildMap<String, LocalizedDonationCopy> {
          for (lang in listOf("ht", "fr", "en", "es")) {
            val langMap = rawLocalized?.get(lang) as? Map<*, *>
            fun langStr(fieldKey: String): String =
              (langMap?.get(fieldKey) as? String)?.trim().orEmpty()

            put(
              lang,
              LocalizedDonationCopy(
                screenTitle = langStr("screenTitle"),
                screenSubtitle = langStr("screenSubtitle"),
                developerRole = langStr("developerRole"),
                developerBio = langStr("developerBio"),
                customNote = langStr("customNote"),
              ),
            )
          }
        }

      return DeveloperDonationConfig(
        enabled = bool("enabled", true),
        enableShakeAnimation = bool("enableShakeAnimation", true),
        shakeIntervalSeconds = clampedInterval,
        developerName = strNonBlank("developerName", DEFAULT_DEVELOPER_NAME),
        developerEmail = strOptional("developerEmail"),
        developerPhone = strOptional("developerPhone"),
        screenTitle = topScreenTitle,
        screenSubtitle = topScreenSubtitle,
        developerRole = topDeveloperRole,
        developerBio = topDeveloperBio,
        customNote = topCustomNote,
        localizedCopy = parsedLocalizedCopy,
        paypalEnabled = bool("paypalEnabled", false),
        paypalEmail = strOptional("paypalEmail"),
        paypalMeUrl = strOptional("paypalMeUrl"),
        binanceEnabled = bool("binanceEnabled", false),
        binancePayId = strOptional("binancePayId"),
        binanceUsdtAddress = strOptional("binanceUsdtAddress"),
        binanceNetwork = strOptional("binanceNetwork"),
        wiseEnabled = bool("wiseEnabled", false),
        wiseEmail = strOptional("wiseEmail"),
        wiseTag = strOptional("wiseTag"),
        wiseIban = strOptional("wiseIban"),
        moncashEnabled = bool("moncashEnabled", false),
        moncashNumber = strOptional("moncashNumber"),
        moncashName = strOptional("moncashName"),
        zelleEnabled = bool("zelleEnabled", false),
        zelleInfo = strOptional("zelleInfo"),
      )
    }

    /**
     * Validates that an external URL uses a safe `https://` or `http://` scheme
     * (or normalizes bare `paypal.me/...` links to `https://`) before launching an external browser Intent.
     */
    fun sanitizeExternalHttpUrl(rawUrl: String?): String? {
      val trimmed = rawUrl?.trim().orEmpty()
      if (trimmed.isEmpty()) return null
      val lower = trimmed.lowercase(Locale.US)
      return when {
        lower.startsWith("https://") || lower.startsWith("http://") -> trimmed
        lower.startsWith("paypal.me/") || lower.startsWith("www.paypal.me/") -> "https://$trimmed"
        else -> null
      }
    }
  }
}

/**
 * Strict 4-language resolution rule (Zero Language Leaks):
 * - Normalizes [appLang] to one of `"ht"`, `"fr"`, `"en"`, `"es"` (default `"ht"`).
 * - For each field (`screenTitle`, `screenSubtitle`, `developerRole`, `developerBio`, `customNote`):
 *   1) Uses `config.localizedCopy[lang]?.field` if non-blank.
 *   2) Only when `lang == "ht"`, allows falling back to legacy top-level `config.field` before the built-in Haitian Creole default.
 *   3) When `lang` is `"fr"`, `"en"`, or `"es"`, NEVER falls back to Haitian Creole top-level fields —
 *      falls back strictly to that language's built-in default copy.
 */
fun resolveLocalizedDonationCopy(
  config: DeveloperDonationConfig,
  appLang: String,
): LocalizedDonationCopy {
  val lang =
    when (appLang.trim().lowercase(Locale.US)) {
      "fr" -> "fr"
      "en" -> "en"
      "es" -> "es"
      else -> "ht"
    }
  val remote = config.localizedCopy[lang]
  val builtIn =
    DeveloperDonationConfig.DEFAULT_LOCALIZED_COPY[lang]
      ?: DeveloperDonationConfig.DEFAULT_HT_COPY

  fun resolveField(
    remoteValue: String?,
    legacyHtTopLevel: String,
    builtInDefault: String,
  ): String {
    val cleanRemote = remoteValue?.trim().orEmpty()
    if (cleanRemote.isNotEmpty()) return cleanRemote
    if (lang == "ht") {
      val cleanLegacy = legacyHtTopLevel.trim()
      if (cleanLegacy.isNotEmpty()) return cleanLegacy
    }
    return builtInDefault
  }

  return LocalizedDonationCopy(
    screenTitle =
      resolveField(
        remoteValue = remote?.screenTitle,
        legacyHtTopLevel = config.screenTitle,
        builtInDefault = builtIn.screenTitle,
      ),
    screenSubtitle =
      resolveField(
        remoteValue = remote?.screenSubtitle,
        legacyHtTopLevel = config.screenSubtitle,
        builtInDefault = builtIn.screenSubtitle,
      ),
    developerRole =
      resolveField(
        remoteValue = remote?.developerRole,
        legacyHtTopLevel = config.developerRole,
        builtInDefault = builtIn.developerRole,
      ),
    developerBio =
      resolveField(
        remoteValue = remote?.developerBio,
        legacyHtTopLevel = config.developerBio,
        builtInDefault = builtIn.developerBio,
      ),
    customNote =
      resolveField(
        remoteValue = remote?.customNote,
        legacyHtTopLevel = config.customNote,
        builtInDefault = builtIn.customNote,
      ),
  )
}
