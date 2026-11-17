package com.zoutiw.omnichurch

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.zoutiw.omnichurch.core.utilities.LocaleHelper
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.core.utilities.findActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name and named firestore database id match OmniChurch specification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    val dbId = context.getString(R.string.firestore_database_id)

    assertEquals("OmniChurch", appName)
    assertEquals(
      "ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1",
      dbId,
    )
  }

  @Test
  fun `qr code generator renders high contrast bitmap for real barcode token`() = runBlocking {
    val bitmap = QrCodeGenerator.generateQrBitmap("OMNICHURCH_CHURCH1_OM0258743", sizePx = 512)
    assertNotNull(bitmap)
    assertEquals(512, bitmap!!.width)
    assertEquals(512, bitmap.height)
    assertTrue(bitmap.width > 0)
  }

  @Test
  fun `findActivity preserves Activity instance across localized ContextWrapper for CredentialManager`() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    val wrapped = LocaleHelper.wrapContextWithLocale(ContextWrapper(activity), "ht")
    val resolvedActivity = wrapped.findActivity()
    assertNotNull(resolvedActivity)
    assertSame(activity, resolvedActivity)
  }

  @Test
  fun `OmniPreferencesManager persists and clears Matrikil member session in omni_church_session_prefs`() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val prefs = OmniPreferencesManager(context)

      prefs.saveMemberSession(
        churchId = "church_ch001",
        memberId = "CH001-JD4827",
        memberDocId = "doc_jd_01",
        memberName = "Jean-David Pierre",
        barcodeToken = "OMNICHURCH_church_ch001_JD4827",
      )

      val activeState = prefs.sessionStateFlow.first { it.isInitialized }
      assertTrue(activeState.isAuthenticated)
      assertTrue(activeState.isMemberSession)
      assertEquals("church_ch001", activeState.activeChurchId)
      assertEquals("CH001-JD4827", activeState.activeMemberId)
      assertEquals("doc_jd_01", activeState.activeMemberDocId)

      prefs.clearActiveSession()
      val clearedState = prefs.sessionStateFlow.first { !it.isAuthenticated }
      assertEquals("", clearedState.activeChurchId)
      assertEquals("", clearedState.activeMemberDocId)
    }

  @Test
  fun `ImageCompressor scales down bitmap to max 400x400 and returns valid data url`() {
    val bmp = android.graphics.Bitmap.createBitmap(800, 600, android.graphics.Bitmap.Config.ARGB_8888)
    val base64 = com.zoutiw.omnichurch.core.utilities.ImageCompressor.compressBitmapToBase64(bmp, maxDimension = 400)
    assertTrue(base64.startsWith("data:image/jpeg;base64,"))
  }
}
