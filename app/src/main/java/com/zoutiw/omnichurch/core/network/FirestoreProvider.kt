package com.zoutiw.omnichurch.core.network

import android.content.Context
import com.zoutiw.omnichurch.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Singleton provider for the official OmniChurch Pro Named Firestore Database.
 */
object FirestoreProvider {
  const val DATABASE_ID = "ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1"
  private const val FALLBACK_PROJECT_ID = "gen-lang-client-0674712996"
  private const val FALLBACK_APP_ID = "1:770562747895:android:com.zoutiw.omnichurch"
  private const val FALLBACK_API_KEY = "AIzaSyOmniChurchMobileKey2026Fallback"
  private const val FALLBACK_SENDER_ID = "770562747895"

  fun ensureFirebaseInitialized(context: Context): FirebaseApp {
    return try {
      FirebaseApp.getInstance()
    } catch (_: Exception) {
      val appCtx = context.applicationContext
      try {
        FirebaseApp.initializeApp(appCtx) ?: createFallbackFirebaseApp(appCtx)
      } catch (_: Exception) {
        createFallbackFirebaseApp(appCtx)
      }
    }
  }

  private fun createFallbackFirebaseApp(context: Context): FirebaseApp {
    return try {
      val options = FirebaseOptions.Builder()
        .setApplicationId(FALLBACK_APP_ID)
        .setProjectId(FALLBACK_PROJECT_ID)
        .setGcmSenderId(FALLBACK_SENDER_ID)
        .setApiKey(FALLBACK_API_KEY)
        .build()
      FirebaseApp.initializeApp(context.applicationContext, options)
    } catch (_: Exception) {
      FirebaseApp.getInstance()
    }
  }

  val db: FirebaseFirestore by lazy {
    val app = try {
      FirebaseApp.getInstance()
    } catch (_: Exception) {
      FirebaseApp.getInstance()
    }
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
      if (context != null) {
        ensureFirebaseInitialized(context)
      } else {
        try {
          FirebaseApp.getInstance()
        } catch (_: Exception) {
          db.app
        }
      }
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
}
