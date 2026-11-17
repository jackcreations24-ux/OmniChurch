package com.zoutiw.omnichurch.core.security

import java.security.MessageDigest

object CryptoSecurity {
  const val HASH_PREFIX = "sha256:v1:"

  fun hashAdminPasscode(passcode: String, churchId: String): String {
    val cleanSalt = churchId.trim().lowercase()
    val cleanPasscode = passcode.trim()
    val input = "omnichurch:$cleanSalt:$cleanPasscode:security_entropy_2026"
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    val hex = bytes.joinToString("") { "%02x".format(it) }
    return "$HASH_PREFIX$hex"
  }

  data class VerifyResult(val matches: Boolean, val needsUpgrade: Boolean)

  fun verifyAdminPasscode(enteredPasscode: String?, storedPasscode: String?, churchId: String): VerifyResult {
    val cleanEntered = (enteredPasscode ?: "").trim()
    val cleanStored = (storedPasscode ?: "").trim()
    if (cleanEntered.isEmpty() || cleanStored.isEmpty()) {
      return VerifyResult(matches = false, needsUpgrade = false)
    }

    if (cleanStored.startsWith(HASH_PREFIX)) {
      val computed = hashAdminPasscode(cleanEntered, churchId)
      val matches = MessageDigest.isEqual(computed.toByteArray(Charsets.UTF_8), cleanStored.toByteArray(Charsets.UTF_8))
      return VerifyResult(matches = matches, needsUpgrade = false)
    } else {
      val matches = cleanStored == cleanEntered
      return VerifyResult(matches = matches, needsUpgrade = matches)
    }
  }
}
