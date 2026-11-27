package com.zoutiw.omnichurch

import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.donations.LocalizedDonationCopy
import com.zoutiw.omnichurch.core.donations.resolveLocalizedDonationCopy
import com.zoutiw.omnichurch.presentation.screens.donate.donateScreenCopyFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DonateMultilingualAndSchemaTest {

  @Test
  fun `fromMap parses app_config developer_engine donations schema and clamps shakeIntervalSeconds to 3_30`() {
    val rawMap =
      mapOf(
        "enabled" to true,
        "enableShakeAnimation" to true,
        "shakeIntervalSeconds" to 99,
        "developerName" to "Jackson Charles",
        "developerEmail" to "dev@omnichurch.org",
        "developerPhone" to "+509 3700-0000",
        "paypalEnabled" to true,
        "paypalEmail" to "pay@omnichurch.org",
        "paypalMeUrl" to "paypal.me/omnichurch",
        "binanceEnabled" to true,
        "binancePayId" to "88990011",
        "binanceUsdtAddress" to "TXYZ1234567890",
        "binanceNetwork" to "TRC20",
        "moncashEnabled" to true,
        "moncashNumber" to "+509 3811-2233",
        "moncashName" to "Jackson Charles",
        "localizedCopy" to
          mapOf(
            "ht" to mapOf("screenTitle" to "Tit Kreyol Remote"),
            "fr" to mapOf("screenTitle" to "Titre Français Remote"),
            "en" to mapOf("screenTitle" to "English Remote Title"),
            "es" to mapOf("screenTitle" to "Título Español Remote"),
          ),
      )

    val config = DeveloperDonationConfig.fromMap(rawMap)
    assertTrue(config.enabled)
    assertEquals(30, config.shakeIntervalSeconds)
    assertEquals(
      listOf(
        DeveloperDonationConfig.Channel.PAYPAL,
        DeveloperDonationConfig.Channel.BINANCE,
        DeveloperDonationConfig.Channel.MONCASH,
      ),
      config.enabledChannels,
    )

    val lowShakeConfig = DeveloperDonationConfig.fromMap(mapOf("shakeIntervalSeconds" to 1))
    assertEquals(3, lowShakeConfig.shakeIntervalSeconds)
  }

  @Test
  fun `resolveLocalizedDonationCopy enforces strict zero language leak across ht, fr, en, es`() {
    val legacyOnlyHtConfig =
      DeveloperDonationConfig(
        screenTitle = "Tit Legacy Kreyol Selman",
        screenSubtitle = "Soutit Legacy Kreyol",
        developerRole = "Wòl Legacy Kreyol",
        developerBio = "Bio Legacy Kreyol",
        customNote = "Nòt Legacy Kreyol",
        localizedCopy =
          mapOf(
            "ht" to LocalizedDonationCopy(),
            "fr" to LocalizedDonationCopy(),
            "en" to LocalizedDonationCopy(),
            "es" to LocalizedDonationCopy(),
          ),
      )

    // HT can fall back to legacy HT top-level fields when localizedCopy["ht"] is blank
    val htResolved = resolveLocalizedDonationCopy(legacyOnlyHtConfig, "ht")
    assertEquals("Tit Legacy Kreyol Selman", htResolved.screenTitle)
    assertEquals("Nòt Legacy Kreyol", htResolved.customNote)

    // FR, EN, ES must NEVER leak legacy HT top-level fields; they fall back to their native built-in defaults
    val frResolved = resolveLocalizedDonationCopy(legacyOnlyHtConfig, "fr")
    assertEquals(DeveloperDonationConfig.DEFAULT_FR_COPY.screenTitle, frResolved.screenTitle)
    assertFalse(frResolved.screenTitle.contains("Kreyol"))

    val enResolved = resolveLocalizedDonationCopy(legacyOnlyHtConfig, "en")
    assertEquals(DeveloperDonationConfig.DEFAULT_EN_COPY.screenTitle, enResolved.screenTitle)
    assertFalse(enResolved.screenTitle.contains("Kreyol"))

    val esResolved = resolveLocalizedDonationCopy(legacyOnlyHtConfig, "es")
    assertEquals(DeveloperDonationConfig.DEFAULT_ES_COPY.screenTitle, esResolved.screenTitle)
    assertFalse(esResolved.screenTitle.contains("Kreyol"))
  }

  @Test
  fun `sanitizeExternalHttpUrl normalizes paypal_me and rejects unsafe schemes`() {
    assertEquals(
      "https://paypal.me/jacksoncharles",
      DeveloperDonationConfig.sanitizeExternalHttpUrl("paypal.me/jacksoncharles"),
    )
    assertEquals(
      "https://www.paypal.me/jacksoncharles",
      DeveloperDonationConfig.sanitizeExternalHttpUrl("https://www.paypal.me/jacksoncharles"),
    )
    assertNull(DeveloperDonationConfig.sanitizeExternalHttpUrl("javascript:alert(1)"))
    assertNull(DeveloperDonationConfig.sanitizeExternalHttpUrl(""))
  }

  @Test
  fun `donateScreenCopyFor provides distinct localized UI copy for all 4 app languages`() {
    val ht = donateScreenCopyFor("ht")
    val fr = donateScreenCopyFor("fr")
    val en = donateScreenCopyFor("en")
    val es = donateScreenCopyFor("es")

    assertEquals("Kopye", ht.copyButtonCd)
    assertEquals("Copier", fr.copyButtonCd)
    assertEquals("Copy", en.copyButtonCd)
    assertEquals("Copiar", es.copyButtonCd)

    assertTrue(ht.moncashUssdHelper.contains("*202#"))
    assertTrue(fr.moncashUssdHelper.contains("*202#"))
    assertTrue(en.moncashUssdHelper.contains("*202#"))
    assertTrue(es.moncashUssdHelper.contains("*202#"))
  }

  @Test
  fun `RemoteShareAppConfig parses developer_engine shareApp schema, sanitizes URLs, and formats 4-language share messages`() {
    val rawShareMap =
      mapOf(
        "enabled" to true,
        "shareUrl" to "omnichurch.download",
        "androidUrl" to "https://play.google.com/store/apps/details?id=com.zoutiw.omnichurch",
        "localizedCopy" to
          mapOf(
            "ht" to
              mapOf(
                "header" to "Vin dekouvri OmniChurch Pro!",
                "churchHeaderTemplate" to "Rejwenn {church} sou OmniChurch Pro!",
                "message" to "Telechaje aplikasyon legliz {church} la isit la: {url}",
                "contentFooterLabel" to "📲 Aplikasyon Ofisyèl Legliz la:",
              ),
            "fr" to
              mapOf(
                "header" to "Découvrez OmniChurch Pro !",
                "message" to "Suivez votre église en direct.",
              ),
          ),
      )

    val parsed = com.zoutiw.omnichurch.presentation.components.RemoteShareAppConfig.fromMap(rawShareMap)
    assertTrue(parsed.enabled)
    assertEquals(
      "https://play.google.com/store/apps/details?id=com.zoutiw.omnichurch",
      parsed.effectiveShareUrl,
    )

    val (htSubject, htBody) = parsed.buildGeneralAppShareText(languageCode = "ht", churchName = "Legliz Salem")
    assertEquals("Rejwenn Legliz Salem sou OmniChurch Pro!", htSubject)
    assertTrue(
      htBody.contains(
        "Telechaje aplikasyon legliz Legliz Salem la isit la: https://play.google.com/store/apps/details?id=com.zoutiw.omnichurch"
      )
    )

    val (_, frBody) = parsed.buildGeneralAppShareText(languageCode = "fr", churchName = null)
    assertTrue(frBody.contains("Découvrez OmniChurch Pro !"))
    assertTrue(frBody.contains("Suivez votre église en direct."))
    assertTrue(frBody.contains("https://play.google.com/store/apps/details?id=com.zoutiw.omnichurch"))

    // Unsafe or localhost URLs fall back safely to official share URL
    assertNull(
      com.zoutiw.omnichurch.presentation.components.RemoteShareAppConfig.sanitizeShareUrl("http://localhost:3000")
    )
    assertNull(
      com.zoutiw.omnichurch.presentation.components.RemoteShareAppConfig.sanitizeShareUrl("javascript:alert(1)")
    )
  }

  @Test
  fun `ChurchLocationDetector maps 4-language country names, detects timezone city and country, and prevents country duplication`() {
    val detector = com.zoutiw.omnichurch.core.utilities.ChurchLocationDetector

    // 1. 4-language country names
    assertEquals("Ayiti", detector.getCountryNameFromCode("HT", "ht"))
    assertEquals("Haïti", detector.getCountryNameFromCode("HT", "fr"))
    assertEquals("Haiti", detector.getCountryNameFromCode("HT", "en"))
    assertEquals("Haití", detector.getCountryNameFromCode("HT", "es"))

    assertEquals("Repiblik Dominiken", detector.getCountryNameFromCode("DO", "ht"))
    assertEquals("République Dominicaine", detector.getCountryNameFromCode("DO", "fr"))
    assertEquals("Dominican Republic", detector.getCountryNameFromCode("DO", "en"))
    assertEquals("República Dominicana", detector.getCountryNameFromCode("DO", "es"))

    assertEquals("Etazini (USA)", detector.getCountryNameFromCode("US", "ht"))
    assertEquals("États-Unis (USA)", detector.getCountryNameFromCode("US", "fr"))
    assertEquals("United States (USA)", detector.getCountryNameFromCode("US", "en"))
    assertEquals("Estados Unidos (EE.UU.)", detector.getCountryNameFromCode("US", "es"))

    // 2. Offline timezone detection
    val haitiSync =
      detector.getDetectedChurchCountrySync(
        language = "ht",
        timezoneIdOverride = "America/Port-au-Prince",
      )
    assertEquals("HT", haitiSync.countryCode)
    assertEquals("Ayiti", haitiSync.countryName)
    assertEquals("Pòtoprens", haitiSync.cityHint)
    assertEquals("Pòtoprens, Ayiti", haitiSync.formattedDefaultCity)

    val drSync =
      detector.getDetectedChurchCountrySync(
        language = "es",
        timezoneIdOverride = "America/Santo_Domingo",
      )
    assertEquals("DO", drSync.countryCode)
    assertEquals("República Dominicana", drSync.countryName)
    assertEquals("Santo Domingo", drSync.cityHint)
    assertEquals("Santo Domingo, República Dominicana", drSync.formattedDefaultCity)

    // 3. Smart non-duplicating formatter
    assertEquals(
      "Delmas 33, Ayiti",
      detector.formatChurchLocationWithDetectedCountry(
        rawCity = "Delmas 33",
        language = "ht",
        explicitCountry = "HT",
      ),
    )
    assertEquals(
      "Pòtoprens, Ayiti",
      detector.formatChurchLocationWithDetectedCountry(
        rawCity = "Pòtoprens, Ayiti",
        language = "ht",
        explicitCountry = "HT",
      ),
    )
    assertEquals(
      "Port-au-Prince, Haïti",
      detector.formatChurchLocationWithDetectedCountry(
        rawCity = "Port-au-Prince, Haïti",
        language = "ht",
        explicitCountry = "HT",
      ),
    )
    assertEquals(
      "Santiago, Repiblik Dominiken",
      detector.formatChurchLocationWithDetectedCountry(
        rawCity = "Santiago, Repiblik Dominiken",
        language = "es",
        explicitCountry = "DO",
      ),
    )
  }
}
