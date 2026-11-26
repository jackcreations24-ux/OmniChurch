package com.zoutiw.omnichurch.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

const val OMNICHURCH_OFFICIAL_SHARE_URL =
  "https://ais-pre-hzeqr4nb7corovn7vkwgnr-412075223902.us-east5.run.app"

fun shareOmniChurchContent(
  context: Context,
  title: String,
  body: String,
) {
  val fullMessage = buildString {
    append(title.trim())
    if (body.isNotBlank()) {
      append("\n\n")
      append(body.trim())
    }
    append("\n\n📲 OmniChurch Pro (Legliz la nan poch ou):\n")
    append(OMNICHURCH_OFFICIAL_SHARE_URL)
  }
  runCatching {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, fullMessage)
      }
    val chooser =
      Intent.createChooser(sendIntent, "Pataje sou OmniChurch").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }.onFailure {
    copyOmniChurchTextToClipboard(context, title, fullMessage)
  }
}

fun shareOmniChurchGeneralApp(
  context: Context,
  churchName: String? = null,
) {
  val header =
    if (!churchName.isNullOrBlank()) {
      "🔥 Rejwenn $churchName sou OmniChurch Mobile!"
    } else {
      "🔥 Dekouvri OmniChurch Mobile — Legliz la nan poch ou!"
    }
  val body =
    "Swiv tout sèvis adorasyon an dirèk, anons ofisyèl, kat idantite manm dijital QR, ak prezans legliz ou sou OmniChurch Pro."
  shareOmniChurchContent(context, header, body)
}

fun copyOmniChurchTextToClipboard(
  context: Context,
  label: String,
  text: String,
) {
  runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Tèks la kopye avèk siksè!", Toast.LENGTH_SHORT).show()
  }
}
