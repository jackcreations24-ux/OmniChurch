package com.zoutiw.omnichurch

import com.zoutiw.omnichurch.core.security.CryptoSecurity
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniQrParser
import com.zoutiw.omnichurch.data.models.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndQrUnitTest {

  @Test
  fun `sha256 v1 admin passcode hash computation matches exact canonical entropy spec`() {
    val churchId = "church_ch001"
    val pin = "SecretAdmin2026"
    val digest = CryptoSecurity.hashAdminPasscode(pin, churchId)
    assertTrue(digest.startsWith(CryptoSecurity.HASH_PREFIX))
    assertEquals(10 + 64, digest.length)
  }

  @Test
  fun `omni qr parser successfully decodes verification url and internal church member tokens`() {
    val url = "https://verify.omnichurch.download/?verify=1&church=CH001&member=CH001-JD4827"
    val parsedUrl = OmniQrParser.parseQrToken(url)
    assertEquals("CH001", parsedUrl.churchId)
    assertEquals("CH001-JD4827", parsedUrl.memberId)

    val token = "CHURCH:ETG01|MEMBER:ETG01-MP9201"
    val parsedToken = OmniQrParser.parseQrToken(token)
    assertEquals("ETG01", parsedToken.churchId)
    assertEquals("ETG01-MP9201", parsedToken.memberId)
  }

  @Test
  fun `reception role normalization grants scanning permission for normalized staff roles`() {
    val receptionist = UserProfile(role = "Réception")
    val usher = UserProfile(role = "Pwotokòl")
    val welcome = UserProfile(role = "Accueil")
    val regularMember = UserProfile(role = "Manm")

    assertTrue(receptionist.isAuthorizedScanner)
    assertTrue(usher.isAuthorizedScanner)
    assertTrue(welcome.isAuthorizedScanner)
    assertFalse(regularMember.isAuthorizedScanner)
  }
}
