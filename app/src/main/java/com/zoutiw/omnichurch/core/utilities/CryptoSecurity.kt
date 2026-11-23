package com.zoutiw.omnichurch.core.utilities

import java.security.MessageDigest

data class PasscodeVerificationResult(
  val matches: Boolean,
  val needsUpgrade: Boolean = false,
)

/**
 * Cross-platform cryptographic hashing helper matching the Web/PC OmniChurch Pro client
 * byte-for-byte so PINs created on Web unlock on Android and vice versa.
 */
object CryptoSecurity {
  const val HASH_PREFIX = "sha256:v1:"
  private const val ENTROPY_SUFFIX = "security_entropy_2026"

  fun isHashedPasscode(stored: String?): Boolean =
    stored?.trim()?.startsWith(HASH_PREFIX, ignoreCase = false) == true

  /**
   * Computes `"sha256:v1:" + lowercase 64-char hex(SHA-256(UTF-8("omnichurch:${churchId.trim().lowercase()}:${passcode.trim()}:security_entropy_2026")))`.
   */
  fun hashAdminPasscode(passcode: String, churchId: String): String {
    val normalizedInput =
      "omnichurch:${churchId.trim().lowercase()}:${passcode.trim()}:$ENTROPY_SUFFIX"
    val digestBytes =
      MessageDigest.getInstance("SHA-256").digest(normalizedInput.toByteArray(Charsets.UTF_8))
    val hex = digestBytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    return "$HASH_PREFIX$hex"
  }

  /**
   * Constant-time passcode verification supporting:
   * 1. `"sha256:v1:"` salted hashes (`needsUpgrade = false`)
   * 2. Legacy plaintext passcodes (`needsUpgrade = true` so callers immediately re-hash in Firestore)
   */
  fun verifyAdminPasscode(
    entered: String?,
    stored: String?,
    churchId: String,
    fallbackSalt: String? = null,
  ): PasscodeVerificationResult {
    val cleanEntered = (entered ?: "").trim()
    val cleanStored = (stored ?: "").trim()
    if (cleanEntered.isEmpty() || cleanStored.isEmpty()) {
      return PasscodeVerificationResult(matches = false, needsUpgrade = false)
    }

    if (cleanStored.startsWith(HASH_PREFIX)) {
      val normalizedStoredBytes = cleanStored.lowercase().toByteArray(Charsets.UTF_8)
      val primaryBytes = hashAdminPasscode(cleanEntered, churchId).toByteArray(Charsets.UTF_8)
      if (MessageDigest.isEqual(primaryBytes, normalizedStoredBytes)) {
        return PasscodeVerificationResult(matches = true, needsUpgrade = false)
      }
      val cleanFallbackSalt = fallbackSalt?.trim().orEmpty()
      if (cleanFallbackSalt.isNotEmpty() && !cleanFallbackSalt.equals(churchId.trim(), ignoreCase = true)) {
        val fallbackBytes =
          hashAdminPasscode(cleanEntered, cleanFallbackSalt).toByteArray(Charsets.UTF_8)
        if (MessageDigest.isEqual(fallbackBytes, normalizedStoredBytes)) {
          return PasscodeVerificationResult(matches = true, needsUpgrade = false)
        }
      }
      return PasscodeVerificationResult(matches = false, needsUpgrade = false)
    }

    val enteredBytes = cleanEntered.toByteArray(Charsets.UTF_8)
    val storedBytes = cleanStored.toByteArray(Charsets.UTF_8)
    val plaintextMatch = MessageDigest.isEqual(enteredBytes, storedBytes)
    return PasscodeVerificationResult(
      matches = plaintextMatch,
      needsUpgrade = plaintextMatch,
    )
  }

  fun isoTimestampNow(): String {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
    fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
    return fmt.format(java.util.Date())
  }

  const val SUPER_DEV_SALT_PREFIX = "omnichurch_super_dev_v1::"
  const val SUPER_DEV_SHA256_HASH =
    "395c68352dfbf743bb5e59d29ef288b03ba3f1494a3f574b681909f74f824fc6"
  private val SUPER_DEV_SHA256_HASHES =
    listOf(
      SUPER_DEV_SHA256_HASH,
      "3f62926f459090704ef60590c997dbdeebc1d8f946a874467fd812635986221b",
    )

  /**
   * Computes lowercase 64-char hex(SHA-256(UTF-8("omnichurch_super_dev_v1::" + email.trim().lowercase()))).
   */
  fun hashSuperDeveloperEmail(email: String): String {
    val normalizedInput = "$SUPER_DEV_SALT_PREFIX${email.trim().lowercase(java.util.Locale.US)}"
    val digestBytes =
      MessageDigest.getInstance("SHA-256").digest(normalizedInput.toByteArray(Charsets.UTF_8))
    return digestBytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
  }

  /**
   * Verifies whether the given email matches the Super Developer salted SHA-256 hash without
   * hardcoding the developer email in plain text.
   */
  fun isSuperDeveloperAccount(email: String?): Boolean {
    val cleanEmail = email?.trim()?.lowercase(java.util.Locale.US).orEmpty()
    if (cleanEmail.isEmpty()) return false
    val computedBytes = hashSuperDeveloperEmail(cleanEmail).toByteArray(Charsets.UTF_8)
    var matched = false
    for (targetHash in SUPER_DEV_SHA256_HASHES) {
      val targetBytes = targetHash.toByteArray(Charsets.UTF_8)
      if (MessageDigest.isEqual(computedBytes, targetBytes)) {
        matched = true
      }
    }
    return matched
  }

  private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
  private const val LOCAL_SECRET_KEY_ALIAS = "omnichurch_remembered_admin_key_v1"
  private const val LOCAL_SECRET_PREFIX = "aesgcm:v1:"

  private fun getOrCreateLocalSecretKey(): javax.crypto.SecretKey? {
    return runCatching {
      val keyStore = java.security.KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
      val existing = keyStore.getKey(LOCAL_SECRET_KEY_ALIAS, null) as? javax.crypto.SecretKey
      if (existing != null) return@runCatching existing

      val keyGenerator =
        javax.crypto.KeyGenerator.getInstance(
          android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
          KEYSTORE_PROVIDER,
        )
      val spec =
        android.security.keystore.KeyGenParameterSpec.Builder(
          LOCAL_SECRET_KEY_ALIAS,
          android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
            android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
        )
          .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
          .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
          .setKeySize(256)
          .build()
      keyGenerator.init(spec)
      keyGenerator.generateKey()
    }.getOrNull()
  }

  /**
   * Encrypts a device-local credential (such as Remember Me password) with an Android Keystore
   * hardware-backed AES-256-GCM key so plaintext passwords are never persisted to DataStore.
   */
  fun encryptLocalSecret(plaintext: String): String {
    if (plaintext.isEmpty()) return ""
    val key = getOrCreateLocalSecretKey() ?: return ""
    return runCatching {
      val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
      val iv = cipher.iv
      val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
      val ivB64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
      val dataB64 = android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
      "$LOCAL_SECRET_PREFIX$ivB64:$dataB64"
    }.getOrDefault("")
  }

  /**
   * Decrypts a local AES-256-GCM credential previously produced by [encryptLocalSecret].
   */
  fun decryptLocalSecret(encoded: String?): String {
    val raw = encoded?.trim().orEmpty()
    if (!raw.startsWith(LOCAL_SECRET_PREFIX)) return ""
    val body = raw.removePrefix(LOCAL_SECRET_PREFIX)
    val parts = body.split(":", limit = 2)
    if (parts.size != 2) return ""
    val key = getOrCreateLocalSecretKey() ?: return ""
    return runCatching {
      val iv = android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)
      val ciphertext = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
      val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
      val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
      cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, spec)
      String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrDefault("")
  }
}
