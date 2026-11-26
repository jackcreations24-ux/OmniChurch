package com.zoutiw.omnichurch.core.network

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.zoutiw.omnichurch.R
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Singleton provider for the official OmniChurch Pro Named Firestore Database:
 * `ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1` in project `gen-lang-client-0674712996`.
 *
 * Configures unlimited local SQLite persistent offline caching (`PersistentCacheSettings` with
 * `CACHE_SIZE_UNLIMITED`) and real-time synchronization so the Android application works reliably
 * in low/no-connectivity church environments while staying 100% synchronized with Cloud Firestore.
 */
object FirestoreProvider {
  const val DATABASE_ID = "ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1"

  private fun configureUnlimitedOfflineCache(firestore: FirebaseFirestore) {
    try {
      val cacheSettings =
        PersistentCacheSettings.newBuilder()
          .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
          .build()
      val settings =
        FirebaseFirestoreSettings.Builder()
          .setLocalCacheSettings(cacheSettings)
          .build()
      firestore.firestoreSettings = settings
    } catch (_: Exception) {
      // Firestore instance already started; keep active persistent cache settings
    }
  }

  val db: FirebaseFirestore by lazy {
    val app = FirebaseApp.getInstance()
    val firestore = FirebaseFirestore.getInstance(app, DATABASE_ID)
    configureUnlimitedOfflineCache(firestore)
    firestore
  }

  fun get(context: Context? = null): FirebaseFirestore {
    val app: FirebaseApp =
      try {
        FirebaseApp.getInstance()
      } catch (e: Exception) {
        if (context != null) {
          FirebaseApp.initializeApp(context.applicationContext) ?: FirebaseApp.getInstance()
        } else {
          throw e
        }
      } ?: FirebaseApp.getInstance()
    val dbId =
      try {
        context?.applicationContext?.getString(R.string.firestore_database_id)
          ?.takeIf { it.isNotBlank() } ?: DATABASE_ID
      } catch (_: Exception) {
        DATABASE_ID
      }
    val firestore = FirebaseFirestore.getInstance(app, dbId)
    configureUnlimitedOfflineCache(firestore)
    return firestore
  }

  fun ensureFirebaseInitialized(context: Context) {
    get(context)
    OmniAppCheckReadiness.ensureSafeAppCheckIfConfigured(context)
  }
}

/**
 * Defensive App Check readiness manager:
 * Ensures that if `firebase_app_check_recaptcha_site_key` is not provisioned in `firebase_applet_config.xml`,
 * the app never installs a broken/dummy App Check provider that would block Cloud Firestore operations.
 * When a real production reCAPTCHA v3 site key is provisioned and the App Check library is present,
 * it activates safely via reflection.
 */
object OmniAppCheckReadiness {
  @Volatile private var checked = false

  fun isValidRecaptchaSiteKey(rawKey: String?): Boolean {
    val clean = rawKey?.trim().orEmpty()
    if (clean.length < 16) return false
    val lower = clean.lowercase()
    return !lower.contains("placeholder") &&
      !lower.contains("your_") &&
      !lower.contains("unconfigured") &&
      !lower.startsWith("ai-studio-")
  }

  fun resolveConfiguredSiteKey(context: Context): String? =
    try {
      val appCtx = context.applicationContext
      val resId =
        appCtx.resources.getIdentifier(
          "firebase_app_check_recaptcha_site_key",
          "string",
          appCtx.packageName,
        )
      if (resId != 0) {
        appCtx.getString(resId).takeIf { isValidRecaptchaSiteKey(it) }
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }

  fun ensureSafeAppCheckIfConfigured(context: Context): Boolean {
    val siteKey = resolveConfiguredSiteKey(context) ?: return false
    if (checked) return true
    checked = true
    return try {
      val appCheckClass = Class.forName("com.google.firebase.appcheck.FirebaseAppCheck")
      val factoryClass =
        Class.forName("com.google.firebase.appcheck.AppCheckProviderFactory")
      val recaptchaClass =
        Class.forName("com.google.firebase.appcheck.recaptchaenterprise.ReCaptchaEnterpriseAppCheckProviderFactory")
      val appCheckInstance = appCheckClass.getMethod("getInstance").invoke(null)
      val factoryInstance =
        recaptchaClass.getMethod("getInstance", String::class.java).invoke(null, siteKey)
      appCheckClass
        .getMethod("installAppCheckProviderFactory", factoryClass)
        .invoke(appCheckInstance, factoryInstance)
      true
    } catch (_: Throwable) {
      false
    }
  }
}

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

object NamedFirestoreProvider {
  const val OMNICHURCH_NAMED_DB_ID = FirestoreProvider.DATABASE_ID

  fun getInstance(context: Context): FirebaseFirestore = FirestoreProvider.get(context)

  fun getInstance(databaseId: String): FirebaseFirestore {
    require(databaseId.isNotBlank()) { "Named Firestore databaseId must not be blank" }
    return FirebaseFirestore.getInstance(FirebaseApp.getInstance(), databaseId)
  }
}

fun handleFirestoreError(
  exception: Throwable,
  operationType: OperationType,
  path: String?,
): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError =
    causes.firstOrNull { it is FirebaseFirestoreException }
      ?: causes.firstOrNull { it !is CancellationException }
      ?: exception

  val currentUser =
    try {
      FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
      null
    }

  val providerInfoList =
    currentUser?.providerData?.map { provider ->
      JSONObject().apply {
        put("providerId", provider.providerId)
        put("email", provider.email)
      }
    } ?: emptyList()

  val authInfoJson =
    JSONObject().apply {
      put("userId", currentUser?.uid)
      put("email", currentUser?.email)
      put("emailVerified", currentUser?.isEmailVerified)
      put("tenantId", currentUser?.tenantId)
      put("providerInfo", JSONArray(providerInfoList))
    }

  val errorInfoJson =
    JSONObject().apply {
      put("error", rootError.message ?: rootError.toString())
      put("operationType", operationType.value)
      put("path", path)
      put("authInfo", authInfoJson)
    }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

