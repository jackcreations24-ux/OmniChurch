package com.zoutiw.omnichurch.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

  protected lateinit var firestore: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var databaseId: String

  @Before
  open fun setUpFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resId =
      context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
    databaseId =
      if (resId != 0) context.getString(resId)
      else "ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1"

    val app =
      if (FirebaseApp.getApps(context).isEmpty()) {
        val newApp =
          FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
              .setApplicationId("com.zoutiw.omnichurch")
              .setProjectId(PROJECT_ID)
              .setApiKey("fake-api-key-for-emulator")
              .build(),
          )
        val newFirestore = FirebaseFirestore.getInstance(newApp, databaseId)
        newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
        newFirestore.firestoreSettings =
          FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
            .build()

        val newAuth = FirebaseAuth.getInstance(newApp)
        newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
        newApp
      } else {
        FirebaseApp.getInstance()
      }

    firestore = FirebaseFirestore.getInstance(app, databaseId)
    auth = FirebaseAuth.getInstance(app)
  }

  @After
  open fun tearDownFirebase() {
    auth.signOut()
  }

  protected suspend fun signInTestUser(email: String): String =
    withContext(Dispatchers.IO) {
      withTimeout(AUTH_TIMEOUT_MS) {
        var lastError: Throwable? = null
        try {
          auth.signOut()
        } catch (_: Exception) {}
        for (attempt in 1..3) {
          try {
            val result =
              try {
                auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
              } catch (e: Exception) {
                auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
              }
            val uid = result.user?.uid
            if (!uid.isNullOrBlank()) {
              return@withTimeout uid
            }
          } catch (e: Throwable) {
            lastError = e
            kotlinx.coroutines.delay(100)
          }
        }
        throw org.junit.AssumptionViolatedException("Firebase Auth emulator not running on port $AUTH_PORT: ${lastError?.message}")
      }
    }

  protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
    var current: Throwable? = this
    while (current != null) {
      if (current is FirebaseFirestoreException) return current
      current = current.cause
    }
    return null
  }

  private companion object {
    const val EMULATOR_HOST = "127.0.0.1"
    const val FIRESTORE_PORT = 8085
    const val AUTH_PORT = 9099
    const val PROJECT_ID = "demo-no-project"
    const val DEFAULT_PASSWORD = "password123"
    const val AUTH_TIMEOUT_MS = 5000L
  }
}
