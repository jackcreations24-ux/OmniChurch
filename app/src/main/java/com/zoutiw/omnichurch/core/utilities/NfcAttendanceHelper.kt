package com.zoutiw.omnichurch.core.utilities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NfcIntentBus {
  private val _latestIntent = MutableStateFlow<Intent?>(null)
  val latestIntent: StateFlow<Intent?> = _latestIntent.asStateFlow()

  fun dispatch(intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    if (
      action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
        action == NfcAdapter.ACTION_TECH_DISCOVERED ||
        action == NfcAdapter.ACTION_TAG_DISCOVERED
    ) {
      _latestIntent.value = intent
    }
  }

  fun consume() {
    _latestIntent.value = null
  }
}

class NfcAttendanceHelper(
  private val activity: Activity,
  private val onTagScanned: (String) -> Unit,
) {
  private val nfcAdapter: NfcAdapter? by lazy { NfcAdapter.getDefaultAdapter(activity) }
  private var pendingIntent: PendingIntent? = null
  private var intentFiltersArray: Array<IntentFilter>? = null

  init {
    if (nfcAdapter != null) {
      val intent =
        Intent(activity, activity.javaClass).apply {
          addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
      val flags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
          PendingIntent.FLAG_UPDATE_CURRENT
        }
      pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

      val ndefMimeFilter =
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
          try {
            addDataType("*/*")
          } catch (e: Exception) {
            Log.e(TAG, "Error adding MIME data type", e)
          }
        }
      val ndefUriFilter =
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
          addDataScheme("https")
          addDataScheme("http")
        }
      intentFiltersArray =
        arrayOf(
          ndefMimeFilter,
          ndefUriFilter,
          IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
          IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        )
    }
  }

  fun isNfcSupported(): Boolean = nfcAdapter != null

  fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

  fun onResume() {
    try {
      nfcAdapter?.let { adapter ->
        pendingIntent?.let { pi ->
          adapter.enableForegroundDispatch(activity, pi, intentFiltersArray, null)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error enabling foreground dispatch", e)
    }
  }

  fun onPause() {
    try {
      nfcAdapter?.disableForegroundDispatch(activity)
    } catch (e: Exception) {
      Log.e(TAG, "Error disabling foreground dispatch", e)
    }
  }

  fun handleIntent(intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    if (
      NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
        NfcAdapter.ACTION_TAG_DISCOVERED == action ||
        NfcAdapter.ACTION_TECH_DISCOVERED == action
    ) {
      val fromIntentNdef = extractNdefFromIntent(intent)
      if (!fromIntentNdef.isNullOrBlank()) {
        onTagScanned(fromIntentNdef)
        return
      }

      val tag: Tag? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

      tag?.let {
        val scannedData = extractDataFromTag(it)
        if (scannedData.isNotBlank()) {
          onTagScanned(scannedData)
        }
      }
    }
  }

  private fun extractNdefFromIntent(intent: Intent): String? {
    val rawMsgs =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
      }
    if (rawMsgs.isNullOrEmpty()) return null
    for (msg in rawMsgs) {
      val ndefMsg = msg as? NdefMessage ?: continue
      val parsed = extractFromNdefMessage(ndefMsg)
      if (!parsed.isNullOrBlank()) return parsed
    }
    return null
  }

  private fun extractDataFromTag(tag: Tag): String {
    val ndef = Ndef.get(tag)
    if (ndef != null) {
      try {
        ndef.connect()
        val ndefMessage = ndef.ndefMessage ?: ndef.cachedNdefMessage
        val parsed = ndefMessage?.let { extractFromNdefMessage(it) }
        if (!parsed.isNullOrBlank()) {
          return parsed
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error reading NDEF from tag", e)
      } finally {
        try {
          ndef.close()
        } catch (_: Exception) {}
      }
    }

    return formatUidToken(tag.id)
  }

  private fun extractFromNdefMessage(ndefMessage: NdefMessage): String? {
    for (record in ndefMessage.records) {
      val decoded = decodeNdefRecord(record.tnf, record.type, record.payload)
      if (!decoded.isNullOrBlank()) return decoded
    }
    return null
  }

  companion object {
    private const val TAG = "NfcAttendanceHelper"

    private val URI_PREFIX_MAP =
      arrayOf(
        "",
        "http://www.",
        "https://www.",
        "http://",
        "https://",
        "tel:",
        "mailto:",
        "ftp://anonymous:anonymous@",
        "ftp://ftp.",
        "ftps://",
        "sftp://",
        "smb://",
        "nfs://",
        "ftp://",
        "dav://",
        "news:",
        "telnet://",
        "imap:",
        "rtsp://",
        "urn:",
        "pop:",
        "sip:",
        "sips:",
        "tftp:",
        "btspp://",
        "btl2cap://",
        "btgoep://",
        "tcpobex://",
        "irdaobex://",
        "file://",
        "urn:epc:id:",
        "urn:epc:tag:",
        "urn:epc:pat:",
        "urn:epc:raw:",
        "urn:epc:",
        "urn:nfc:",
      )

    private val RTD_TEXT_BYTES = byteArrayOf(0x54)
    private val RTD_URI_BYTES = byteArrayOf(0x55)

    fun formatUidToken(id: ByteArray?): String {
      if (id == null || id.isEmpty()) return ""
      val hexString = id.joinToString("") { "%02X".format(it) }
      return "NFC-UID:$hexString"
    }

    fun decodeNdefRecord(tnf: Short, type: ByteArray?, payload: ByteArray?): String? {
      if (payload == null || payload.isEmpty()) return null
      return when (tnf) {
        NdefRecord.TNF_WELL_KNOWN -> {
          val isText =
            type != null &&
              (type.contentEquals(RTD_TEXT_BYTES) ||
                @Suppress("SENSELESS_COMPARISON")
                (NdefRecord.RTD_TEXT != null && type.contentEquals(NdefRecord.RTD_TEXT)))
          val isUri =
            type != null &&
              (type.contentEquals(RTD_URI_BYTES) ||
                @Suppress("SENSELESS_COMPARISON")
                (NdefRecord.RTD_URI != null && type.contentEquals(NdefRecord.RTD_URI)))
          when {
            isText -> {
              val status = payload[0].toInt()
              val textEncoding = if ((status and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
              val languageCodeLength = status and 0x3F
              val start = languageCodeLength + 1
              if (start >= payload.size) return null
              String(payload, start, payload.size - start, textEncoding).trim().takeIf {
                it.isNotEmpty()
              }
            }
            isUri -> {
              val prefixCode = payload[0].toInt() and 0xFF
              val prefix = URI_PREFIX_MAP.getOrElse(prefixCode) { "" }
              val rest =
                if (payload.size > 1) {
                  String(payload, 1, payload.size - 1, Charsets.UTF_8)
                } else {
                  ""
                }
              (prefix + rest).trim().takeIf { it.isNotEmpty() }
            }
            else -> null
          }
        }
        NdefRecord.TNF_ABSOLUTE_URI,
        NdefRecord.TNF_MIME_MEDIA,
        NdefRecord.TNF_EXTERNAL_TYPE -> {
          String(payload, Charsets.UTF_8).trim().takeIf { it.isNotEmpty() }
        }
        else -> null
      }
    }
  }
}
