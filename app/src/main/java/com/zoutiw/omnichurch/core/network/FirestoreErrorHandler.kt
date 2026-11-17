package com.zoutiw.omnichurch.core.network

import android.content.Context
import android.util.Log
import com.zoutiw.omnichurch.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

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
    } catch (e: Exception) {
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
