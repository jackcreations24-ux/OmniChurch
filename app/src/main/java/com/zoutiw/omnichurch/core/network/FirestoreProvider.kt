package com.zoutiw.omnichurch.core.network

import android.content.Context
import com.zoutiw.omnichurch.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

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
