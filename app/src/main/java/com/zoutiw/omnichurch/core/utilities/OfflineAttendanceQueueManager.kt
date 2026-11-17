package com.zoutiw.omnichurch.core.utilities

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class PendingOfflineAttendance(
  val attendanceDocId: String,
  val churchId: String,
  val serviceId: String,
  val serviceTitle: String,
  val serviceCategory: String,
  val serviceDate: String,
  val memberDocId: String,
  val memberId: String,
  val memberName: String,
  val memberPhone: String?,
  val memberDepartment: String?,
  val memberPhotoUrl: String?,
  val userId: String,
  val recordedBy: String,
  val attendanceStatus: String,
  val lateMinutes: Int,
  val lateToleranceMinutes: Int,
  val checkInMethod: String,
  val barcodeToken: String,
  val timestampMillis: Long,
  val timestampIso: String,
  val isVisitor: Boolean,
  val homeChurchId: String?,
  val homeChurchName: String?,
  val hostChurchName: String?,
) {
  fun toJson(): JSONObject =
    JSONObject().apply {
      put("attendanceDocId", attendanceDocId)
      put("churchId", churchId)
      put("serviceId", serviceId)
      put("serviceTitle", serviceTitle)
      put("serviceCategory", serviceCategory)
      put("serviceDate", serviceDate)
      put("memberDocId", memberDocId)
      put("memberId", memberId)
      put("memberName", memberName)
      put("memberPhone", memberPhone ?: "")
      put("memberDepartment", memberDepartment ?: "")
      put("memberPhotoUrl", memberPhotoUrl ?: "")
      put("userId", userId)
      put("recordedBy", recordedBy)
      put("attendanceStatus", attendanceStatus)
      put("lateMinutes", lateMinutes)
      put("lateToleranceMinutes", lateToleranceMinutes)
      put("checkInMethod", checkInMethod)
      put("barcodeToken", barcodeToken)
      put("timestampMillis", timestampMillis)
      put("timestampIso", timestampIso)
      put("isVisitor", isVisitor)
      put("homeChurchId", homeChurchId ?: "")
      put("homeChurchName", homeChurchName ?: "")
      put("hostChurchName", hostChurchName ?: "")
    }

  companion object {
    fun fromJson(obj: JSONObject): PendingOfflineAttendance? {
      val docId = obj.optString("attendanceDocId").trim()
      val churchId = obj.optString("churchId").trim()
      val serviceId = obj.optString("serviceId").trim()
      val memberDocId = obj.optString("memberDocId").trim()
      if (docId.isEmpty() || churchId.isEmpty() || serviceId.isEmpty() || memberDocId.isEmpty()) {
        return null
      }
      return PendingOfflineAttendance(
        attendanceDocId = docId,
        churchId = churchId,
        serviceId = serviceId,
        serviceTitle = obj.optString("serviceTitle"),
        serviceCategory = obj.optString("serviceCategory").ifBlank { "dimanche_matin" },
        serviceDate = obj.optString("serviceDate"),
        memberDocId = memberDocId,
        memberId = obj.optString("memberId"),
        memberName = obj.optString("memberName"),
        memberPhone = obj.optString("memberPhone").takeIf { it.isNotBlank() },
        memberDepartment = obj.optString("memberDepartment").takeIf { it.isNotBlank() },
        memberPhotoUrl = obj.optString("memberPhotoUrl").takeIf { it.isNotBlank() },
        userId = obj.optString("userId").ifBlank { memberDocId },
        recordedBy = obj.optString("recordedBy"),
        attendanceStatus = obj.optString("attendanceStatus").ifBlank { "on_time" },
        lateMinutes = obj.optInt("lateMinutes", 0),
        lateToleranceMinutes = obj.optInt("lateToleranceMinutes", 15),
        checkInMethod = obj.optString("checkInMethod").ifBlank { "qr_scan" },
        barcodeToken = obj.optString("barcodeToken"),
        timestampMillis = obj.optLong("timestampMillis", System.currentTimeMillis()),
        timestampIso = obj.optString("timestampIso"),
        isVisitor = obj.optBoolean("isVisitor", false),
        homeChurchId = obj.optString("homeChurchId").takeIf { it.isNotBlank() },
        homeChurchName = obj.optString("homeChurchName").takeIf { it.isNotBlank() },
        hostChurchName = obj.optString("hostChurchName").takeIf { it.isNotBlank() },
      )
    }
  }
}

class OfflineAttendanceQueueManager(context: Context) {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val lock = Any()

  private val _pendingItemsState = MutableStateFlow(loadQueueFromDisk())
  val pendingItemsState: StateFlow<List<PendingOfflineAttendance>> =
    _pendingItemsState.asStateFlow()

  private val _pendingCountState = MutableStateFlow(_pendingItemsState.value.size)
  val pendingCountState: StateFlow<Int> = _pendingCountState.asStateFlow()

  fun containsAttendance(attendanceDocId: String): Boolean {
    val cleanId = attendanceDocId.trim()
    if (cleanId.isEmpty()) return false
    return _pendingItemsState.value.any { it.attendanceDocId == cleanId }
  }

  fun enqueue(item: PendingOfflineAttendance): Boolean {
    synchronized(lock) {
      val current = loadQueueFromDisk().toMutableList()
      if (current.any { it.attendanceDocId == item.attendanceDocId }) {
        return false
      }
      current.add(item)
      saveQueueToDisk(current)
      _pendingItemsState.value = current.toList()
      _pendingCountState.value = current.size
      return true
    }
  }

  suspend fun flushPending(
    db: FirebaseFirestore,
    currentUidProvider: () -> String?,
  ): Int {
    val snapshot =
      synchronized(lock) { _pendingItemsState.value.toList() }
    if (snapshot.isEmpty()) return 0

    var flushedCount = 0
    val remaining = snapshot.toMutableList()

    for (item in snapshot) {
      try {
        val docRef = db.collection("attendances").document(item.attendanceDocId)
        val existing = runCatching { docRef.get().await() }.getOrNull()
        if (existing != null && existing.exists()) {
          remaining.removeAll { it.attendanceDocId == item.attendanceDocId }
          flushedCount++
          continue
        }

        val effectiveActorUid =
          currentUidProvider()?.trim()?.takeIf { it.isNotEmpty() }
            ?: item.recordedBy.trim().ifBlank { "offline_usher" }

        val payload = linkedMapOf<String, Any>()
        if (item.isVisitor && !item.homeChurchId.isNullOrBlank()) {
          payload["id"] = item.attendanceDocId
          payload["churchId"] = item.churchId
          payload["serviceId"] = item.serviceId
          payload["serviceCategory"] = item.serviceCategory.ifBlank { "dimanche_matin" }
          payload["serviceTitle"] = item.serviceTitle.ifBlank { "Sèvis Adorasyon" }
          payload["serviceDate"] = item.serviceDate
          payload["memberId"] = item.memberId.ifBlank { item.memberDocId }
          payload["memberDbId"] = item.memberDocId
          payload["memberName"] = item.memberName.ifBlank { "Vizitè" }
          item.memberPhone?.let { payload["memberPhone"] = it }
          item.memberDepartment?.let { payload["memberDepartment"] = it }
          if (item.barcodeToken.isNotBlank()) {
            payload["barcodeToken"] = item.barcodeToken
          }
          payload["timestamp"] = item.timestampIso
          payload["checkInMethod"] =
            when (item.checkInMethod.trim().lowercase()) {
              "manual" -> "manual"
              "barcode" -> "barcode"
              else -> "qr_scan"
            }
          payload["recordedBy"] = effectiveActorUid
          payload["attendanceStatus"] = item.attendanceStatus
          payload["lateMinutes"] = item.lateMinutes
          payload["lateToleranceMinutes"] = item.lateToleranceMinutes
          payload["attendanceRole"] = "visitor"
          payload["isVisitor"] = true
          payload["homeChurchId"] = item.homeChurchId
          payload["homeChurchName"] = item.homeChurchName?.ifBlank { item.homeChurchId } ?: item.homeChurchId
          payload["hostChurchName"] = item.hostChurchName?.ifBlank { "OmniChurch" } ?: "OmniChurch"
        } else {
          payload["churchId"] = item.churchId
          payload["serviceId"] = item.serviceId
          payload["serviceName"] = item.serviceTitle.ifBlank { "Sèvis Adorasyon" }
          payload["memberDocId"] = item.memberDocId
          payload["memberId"] = item.memberId
          payload["memberCode"] = item.memberId
          payload["memberName"] = item.memberName.ifBlank { "Manm" }
          item.memberPhotoUrl?.let { payload["memberPhotoUrl"] = it }
          payload["userId"] = item.userId.ifBlank { item.memberDocId }
          payload["recordedByUid"] = effectiveActorUid
          payload["status"] = if (item.attendanceStatus == "late") "late" else "on_time"
          payload["checkInMethod"] =
            if (item.checkInMethod.trim().lowercase() == "manual") "manual" else "qr_scan"
          payload["barcodeToken"] = item.barcodeToken
          payload["attendanceRole"] = "member"
          payload["isVisitor"] = false
          payload["timestamp"] = item.timestampMillis
          payload["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(payload).await()
        remaining.removeAll { it.attendanceDocId == item.attendanceDocId }
        flushedCount++
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Offline attendance flush deferred for ${item.attendanceDocId}: ${e.message}")
      }
    }

    synchronized(lock) {
      val currentOnDisk = loadQueueFromDisk()
      val flushedIds = snapshot.map { it.attendanceDocId }.toSet() - remaining.map { it.attendanceDocId }.toSet()
      val updated = currentOnDisk.filterNot { it.attendanceDocId in flushedIds }
      saveQueueToDisk(updated)
      _pendingItemsState.value = updated
      _pendingCountState.value = updated.size
    }

    return flushedCount
  }

  private fun loadQueueFromDisk(): List<PendingOfflineAttendance> {
    val raw = prefs.getString(KEY_QUEUE_JSON, null) ?: return emptyList()
    return try {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          PendingOfflineAttendance.fromJson(obj)?.let { add(it) }
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun saveQueueToDisk(items: List<PendingOfflineAttendance>) {
    val arr = JSONArray()
    items.forEach { arr.put(it.toJson()) }
    prefs.edit().putString(KEY_QUEUE_JSON, arr.toString()).apply()
  }

  companion object {
    private const val TAG = "OfflineAttendanceQueue"
    private const val PREFS_NAME = "omnichurch_offline_attendance_queue"
    private const val KEY_QUEUE_JSON = "pending_queue_v1"

    fun isOnline(context: Context): Boolean {
      return try {
        val cm =
          context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      } catch (_: Exception) {
        true
      }
    }

    fun observeNetworkAvailability(context: Context): Flow<Boolean> =
      callbackFlow {
          val appCtx = context.applicationContext
          val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
          if (cm == null) {
            trySend(true)
            awaitClose {}
            return@callbackFlow
          }
          trySend(isOnline(appCtx))
          val callback =
            object : ConnectivityManager.NetworkCallback() {
              override fun onAvailable(network: Network) {
                trySend(true)
              }

              override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
              ) {
                val hasInternet =
                  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(hasInternet)
              }

              override fun onLost(network: Network) {
                trySend(isOnline(appCtx))
              }
            }
          val request =
            NetworkRequest.Builder()
              .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
              .build()
          runCatching { cm.registerNetworkCallback(request, callback) }
          awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
        }
        .distinctUntilChanged()
  }
}
