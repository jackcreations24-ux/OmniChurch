package com.zoutiw.omnichurch.core.network

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.zoutiw.omnichurch.R
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Singleton provider for the official OmniChurch Pro Named Firestore Database:
 * `ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1` in project `gen-lang-client-0674712996`.
 *
 * Configures persistent offline indexing and fast real-time synchronization so the Android companion
 * application instantly mirrors all churches registered on the Web platform.
 */
object FirestoreProvider {
  const val DATABASE_ID = "ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1"

  val db: FirebaseFirestore by lazy {
    val app = FirebaseApp.getInstance()
    val firestore = FirebaseFirestore.getInstance(app, DATABASE_ID)
    try {
      val settings =
        FirebaseFirestoreSettings.Builder()
          .setPersistenceEnabled(true)
          .build()
      firestore.firestoreSettings = settings
    } catch (_: Exception) {}
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
    try {
      val settings =
        FirebaseFirestoreSettings.Builder()
          .setPersistenceEnabled(true)
          .build()
      firestore.firestoreSettings = settings
    } catch (_: Exception) {}
    return firestore
  }

  fun ensureFirebaseInitialized(context: Context) {
    get(context)
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

