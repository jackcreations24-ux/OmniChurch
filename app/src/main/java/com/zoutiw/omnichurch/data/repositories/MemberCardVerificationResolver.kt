package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniQrParser
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import java.util.Locale
import kotlinx.coroutines.CancellationException

internal suspend fun resolveMemberCardVerificationImpl(
  db: FirebaseFirestore,
  rawTokenOrCode: String,
  fallbackChurchId: String? = null,
): Result<PublicVerificationResolution> {
  val cleanedInput = rawTokenOrCode.trim()
  if (cleanedInput.isEmpty()) {
    return Result.failure(
      IllegalArgumentException("Tanpri eskane yon kòd QR, yon kat NFC, oswa antre yon Matrikil.")
    )
  }

  val parsedQr = OmniQrParser.parseQrToken(cleanedInput)
  var candidateChurchId = parsedQr.churchId?.trim()?.takeIf { it.isNotEmpty() }
  var candidateDocId: String? = null
  val targetMemberIdentifier = parsedQr.memberId.trim().ifEmpty { cleanedInput }

  if (cleanedInput.startsWith("OMNICHURCH_", ignoreCase = true)) {
    val parts = cleanedInput.split("_")
    if (parts.size >= 3) {
      if (candidateChurchId.isNullOrBlank()) {
        candidateChurchId = parts[1].trim().takeIf { it.isNotEmpty() }
      }
      candidateDocId = parts[2].trim().takeIf { it.isNotEmpty() }
    }
  }

  val fallbackClean = fallbackChurchId?.trim()?.takeIf { it.isNotEmpty() }
  val rawUpper = cleanedInput.uppercase(Locale.US)
  val targetUpper = targetMemberIdentifier.uppercase(Locale.US)
  val rawWithoutNfc = rawUpper.removePrefix("NFC-UID:").trim()
  val targetWithoutNfc = targetUpper.removePrefix("NFC-UID:").trim()

  fun Member.matchesVerificationQuery(): Boolean {
    val memberNfcUid = nfcCardUid.trim().uppercase(Locale.US).removePrefix("NFC-UID:").trim()
    return effectiveMatrikil.uppercase(Locale.US) == targetUpper ||
      id.trim().uppercase(Locale.US) == targetUpper ||
      memberCode.trim().uppercase(Locale.US) == targetUpper ||
      barcodeToken.trim().uppercase(Locale.US) == targetUpper ||
      qrCode.trim().uppercase(Locale.US) == targetUpper ||
      barcodeToken.trim().uppercase(Locale.US) == rawUpper ||
      qrCode.trim().uppercase(Locale.US) == rawUpper ||
      effectiveMatrikil.uppercase(Locale.US) == rawUpper ||
      id.trim().uppercase(Locale.US) == rawUpper ||
      (candidateDocId != null && id.trim().equals(candidateDocId, ignoreCase = true)) ||
      (memberNfcUid.isNotEmpty() && (memberNfcUid == rawWithoutNfc || memberNfcUid == targetWithoutNfc))
  }

  suspend fun loadChurchByIdOrCode(identifier: String): Church? {
    val clean = identifier.trim()
    if (clean.isEmpty()) return null
    val direct =
      runCatching {
        db.collection("churches").document(clean).get().awaitSafe().toChurch()
      }.getOrNull()
    if (direct != null) return direct
    val allChurches =
      runCatching {
        db.collection("churches").get().awaitSafe().documents.mapNotNull { it.toChurch() }
      }.getOrDefault(emptyList())
    return allChurches.firstOrNull { church ->
      church.id.equals(clean, ignoreCase = true) ||
        (church.code.isNotBlank() && church.code.equals(clean, ignoreCase = true))
    }
  }

  return try {
    var resolvedChurch: Church? =
      candidateChurchId?.let { loadChurchByIdOrCode(it) }
        ?: run {
          val matrikilPrefix =
            targetMemberIdentifier.substringBefore("-", "").trim().takeIf {
              targetMemberIdentifier.contains("-") && it.length in 2..16
            }
          matrikilPrefix?.let { loadChurchByIdOrCode(it) }
        }
        ?: fallbackClean?.let { loadChurchByIdOrCode(it) }

    var matchedMember: Member? = null
    if (resolvedChurch != null) {
      val searchKeys =
        listOfNotNull(
          resolvedChurch.id.trim().takeIf { it.isNotEmpty() },
          resolvedChurch.code.trim().takeIf {
            it.isNotEmpty() && !it.equals(resolvedChurch.id.trim(), ignoreCase = true)
          },
        )
      for (key in searchKeys) {
        val snap =
          runCatching {
            db.collection("members").whereEqualTo("churchId", key).get().awaitSafe()
          }.getOrNull()
        val found = snap?.documents?.mapNotNull { it.toMember() }?.firstOrNull { it.matchesVerificationQuery() }
        if (found != null) {
          matchedMember = found
          break
        }
      }
    }

    if (matchedMember == null) {
      val directDocCandidates =
        listOfNotNull(
          candidateDocId,
          targetMemberIdentifier.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]{8,64}$")) },
        ).distinct()
      for (docId in directDocCandidates) {
        val doc =
          runCatching {
            db.collection("members").document(docId).get().awaitSafe().toMember()
          }.getOrNull()
        if (doc != null && doc.matchesVerificationQuery()) {
          matchedMember = doc
          break
        }
      }
    }

    if (matchedMember == null && fallbackClean != null && fallbackClean != resolvedChurch?.id) {
      val fallbackChurch = loadChurchByIdOrCode(fallbackClean)
      if (fallbackChurch != null) {
        val snap =
          runCatching {
            db.collection("members").whereEqualTo("churchId", fallbackChurch.id).get().awaitSafe()
          }.getOrNull()
        val found = snap?.documents?.mapNotNull { it.toMember() }?.firstOrNull { it.matchesVerificationQuery() }
        if (found != null) {
          matchedMember = found
          resolvedChurch = fallbackChurch
        }
      }
    }

    if (matchedMember == null) {
      return Result.failure(
        IllegalStateException(
          "Kat sa a pa jwenn nan rejis ofisyèl la. Verifye kòd QR la, kat NFC a, oswa Matrikil la."
        )
      )
    }

    val finalChurch =
      resolvedChurch?.takeIf {
        it.id.equals(matchedMember.churchId, ignoreCase = true) ||
          it.code.equals(matchedMember.churchId, ignoreCase = true)
      }
        ?: loadChurchByIdOrCode(matchedMember.churchId)
        ?: resolvedChurch
        ?: Church(
          id = matchedMember.churchId,
          name = matchedMember.churchName.ifBlank { "OmniChurch" },
        )

    Result.success(PublicVerificationResolution(church = finalChurch, member = matchedMember))
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    handleFirestoreError(e, OperationType.GET, "members")
    Result.failure(
      IllegalStateException(
        e.localizedMessage ?: "Erè pandan verifikasyon kat la nan sèvè a."
      )
    )
  }
}
