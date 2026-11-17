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

  @Test
  fun `nfc attendance helper decodes well-known text, uri records, and formats hardware uid`() {
    val uriPayload =
      byteArrayOf(0x04) +
        "verify.omnichurch.download/?verify=1&church=CH001&member=CH001-JD4827"
          .toByteArray(Charsets.UTF_8)
    val decodedUri =
      com.zoutiw.omnichurch.core.utilities.NfcAttendanceHelper.decodeNdefRecord(
        tnf = android.nfc.NdefRecord.TNF_WELL_KNOWN,
        type = byteArrayOf(0x55),
        payload = uriPayload,
      )
    assertEquals(
      "https://verify.omnichurch.download/?verify=1&church=CH001&member=CH001-JD4827",
      decodedUri,
    )
    val parsedFromNfc = OmniQrParser.parseQrToken(decodedUri!!)
    assertEquals("CH001", parsedFromNfc.churchId)
    assertEquals("CH001-JD4827", parsedFromNfc.memberId)

    val textPayload = byteArrayOf(0x02, 'e'.code.toByte(), 'n'.code.toByte()) +
      "CHURCH:CH001|MEMBER:CH001-JD4827".toByteArray(Charsets.UTF_8)
    val decodedText =
      com.zoutiw.omnichurch.core.utilities.NfcAttendanceHelper.decodeNdefRecord(
        tnf = android.nfc.NdefRecord.TNF_WELL_KNOWN,
        type = byteArrayOf(0x54),
        payload = textPayload,
      )
    assertEquals("CHURCH:CH001|MEMBER:CH001-JD4827", decodedText)

    val uidToken =
      com.zoutiw.omnichurch.core.utilities.NfcAttendanceHelper.formatUidToken(
        byteArrayOf(0x04, 0xA1.toByte(), 0x2B, 0xFF.toByte())
      )
    assertEquals("NFC-UID:04A12BFF", uidToken)
  }

  @Test
  fun `email password church resolution matches by email, adminGoogleEmail, ownerId, profile, and rejects unrelated users`() {
    val church1 =
      com.zoutiw.omnichurch.data.models.Church(
        id = "ch_001",
        code = "CH001",
        name = "Legliz Betel",
        email = "betel@omnichurch.org",
        ownerId = "uid_owner_1",
      )
    val church2 =
      com.zoutiw.omnichurch.data.models.Church(
        id = "ch_002",
        code = "CH002",
        name = "Legliz Siyon",
        adminGoogleEmail = "siyon@omnichurch.org",
        adminGoogleUid = "uid_admin_2",
      )
    val churches = listOf(church1, church2)

    val byChurchEmail =
      OmniAuthEvaluator.resolveChurchForEmailPasswordLogin(
        churches = churches,
        email = " BETEL@OMNICHURCH.ORG ",
        uid = "other_uid",
        selectedChurchId = null,
      )
    assertTrue(byChurchEmail.isSuccess)
    assertEquals("ch_001", byChurchEmail.getOrThrow().id)

    val byAdminGoogleEmailAndWrongSelection =
      OmniAuthEvaluator.resolveChurchForEmailPasswordLogin(
        churches = churches,
        email = "siyon@omnichurch.org",
        uid = "other_uid",
        selectedChurchId = "ch_001",
      )
    assertTrue(byAdminGoogleEmailAndWrongSelection.isSuccess)
    assertEquals("ch_002", byAdminGoogleEmailAndWrongSelection.getOrThrow().id)

    val byOwnerUid =
      OmniAuthEvaluator.resolveChurchForEmailPasswordLogin(
        churches = churches,
        email = "alias@omnichurch.org",
        uid = "uid_owner_1",
      )
    assertTrue(byOwnerUid.isSuccess)
    assertEquals("ch_001", byOwnerUid.getOrThrow().id)

    val byUserProfileAdmin =
      OmniAuthEvaluator.resolveChurchForEmailPasswordLogin(
        churches = churches,
        email = "staff@omnichurch.org",
        uid = "uid_staff",
        profileChurchId = "CH002",
        profileRole = "admin",
      )
    assertTrue(byUserProfileAdmin.isSuccess)
    assertEquals("ch_002", byUserProfileAdmin.getOrThrow().id)

    val unauthorizedUser =
      OmniAuthEvaluator.resolveChurchForEmailPasswordLogin(
        churches = churches,
        email = "stranger@example.com",
        uid = "uid_stranger",
        selectedChurchId = "ch_001",
        profileChurchId = "ch_001",
        profileRole = "member",
      )
    assertTrue(unauthorizedUser.isFailure)

    val superAdminSelected =
      OmniAuthEvaluator.resolveChurchForEmailPasswordLogin(
        churches = churches,
        email = OmniAuthEvaluator.SUPER_ADMIN_EMAIL,
        uid = "uid_super",
        selectedChurchId = "ch_002",
      )
    assertTrue(superAdminSelected.isSuccess)
    assertEquals("ch_002", superAdminSelected.getOrThrow().id)
  }
}
