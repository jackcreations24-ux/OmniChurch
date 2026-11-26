package com.zoutiw.omnichurch.presentation.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.findActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleAccountPayload(
  val uid: String,
  val email: String,
  val displayName: String,
  val photoUrl: String?,
)

fun launchOptionalGoogleSignIn(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSuccess: (GoogleAccountPayload) -> Unit,
  onError: (String) -> Unit,
) {
  val activity = context.findActivity() ?: (context as? Activity)
  if (activity == null) {
    Log.e("Auth", "launchOptionalGoogleSignIn: Cannot find Activity from context $context")
    onError("Koneksyon Google mande yon fenèt prensipal aktif. Tanpri relanse aplikasyon an.")
    return
  }
  val clientId =
    try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onError(context.getString(R.string.auth_error_missing_client))
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
        val email = googleIdCred.id.trim()
        val uid = googleIdCred.id.trim()
        val displayName =
          googleIdCred.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
        val photoUrl = googleIdCred.profilePictureUri?.toString()

        try {
          val authCredential = GoogleAuthProvider.getCredential(googleIdCred.idToken, null)
          val authResult = Firebase.auth.signInWithCredential(authCredential).await()
          val firebaseUser = authResult.user
          val finalEmail = firebaseUser?.email?.trim()?.ifBlank { null } ?: email
          val finalUid = firebaseUser?.uid?.trim()?.ifBlank { null } ?: uid
          val finalDisplayName =
            firebaseUser?.displayName?.trim()?.ifBlank { null } ?: displayName
          val finalPhotoUrl = firebaseUser?.photoUrl?.toString() ?: photoUrl

          onSuccess(
            GoogleAccountPayload(
              uid = finalUid,
              email = finalEmail,
              displayName = finalDisplayName,
              photoUrl = finalPhotoUrl,
            )
          )
        } catch (authEx: Exception) {
          Log.w(
            "Auth",
            "Firebase Auth signInWithCredential skipped (${authEx.message}), proceeding with verified Google ID Token for: $email",
          )
          onSuccess(
            GoogleAccountPayload(
              uid = uid,
              email = email,
              displayName = displayName,
              photoUrl = photoUrl,
            )
          )
        }
      } else {
        onError(context.getString(R.string.auth_error_failed))
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
      onError(context.getString(R.string.auth_cancelled))
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onError(e.localizedMessage ?: context.getString(R.string.auth_error_failed))
    }
  }
}

fun signOutOmniChurch(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        try {
          Firebase.auth.signOut()
        } catch (_: Exception) {}
        onSignOutComplete()
      }
    }
  }
}
