package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import java.util.Locale

internal object AuthErrorMessages {
  fun mergeAuthMethods(existing: String?, current: String): String {
    val cleanExisting = existing?.trim()?.lowercase().orEmpty()
    return when {
      cleanExisting.isEmpty() || cleanExisting == "legacy_pin" -> current
      cleanExisting == current -> current
      cleanExisting == "both" -> "both"
      (cleanExisting == "email_password" && current == "google") ||
        (cleanExisting == "google" && current == "email_password") -> "both"
      else -> current
    }
  }

  fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() &&
      email.length <= 200 &&
      Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)
  }

  fun mapFirebaseAuthException(error: Throwable, lang: String): String {
    val code =
      when (error) {
        is FirebaseAuthException -> error.errorCode
        is FirebaseException -> error.message.orEmpty()
        else -> ""
      }
    val rawMsg = (error.message ?: "").lowercase(Locale.US)
    return when {
      error is FirebaseTooManyRequestsException || code == "ERROR_TOO_MANY_REQUESTS" ->
        when (lang) {
          "fr" -> "Trop de tentatives. Veuillez patienter quelques minutes avant de réessayer."
          "en" -> "Too many attempts. Please wait a few minutes before trying again."
          "es" -> "Demasiados intentos. Espere unos minutos antes de volver a intentarlo."
          else -> "Twòp esè ki echwe. Tanpri tann kèk minit anvan ou eseye ankò."
        }
      error is FirebaseAuthWeakPasswordException || code == "ERROR_WEAK_PASSWORD" ->
        localizedWeakPassword(lang)
      error is FirebaseAuthUserCollisionException || code == "ERROR_EMAIL_ALREADY_IN_USE" ->
        localizedEmailInUseWrongPassword(lang)
      error is FirebaseAuthInvalidUserException || code == "ERROR_USER_NOT_FOUND" || code == "ERROR_USER_DISABLED" ->
        when (lang) {
          "fr" -> "Aucun compte administrateur trouvé avec cet e-mail."
          "en" -> "No admin account found for this email address."
          "es" -> "No se encontró ninguna cuenta de administrador para este correo."
          else -> "Pa gen okenn kont administratè ki anrejistre ak imèl sa a."
        }
      error is FirebaseAuthInvalidCredentialsException ||
        code == "ERROR_WRONG_PASSWORD" ||
        code == "ERROR_INVALID_CREDENTIAL" ||
        code == "ERROR_INVALID_EMAIL" ->
        when (lang) {
          "fr" -> "E-mail ou mot de passe incorrect. Vérifiez vos identifiants."
          "en" -> "Incorrect email or password. Please check your credentials."
          "es" -> "Correo o contraseña incorrectos. Verifique sus credenciales."
          else -> "Imèl la oswa modpas la pa kòrèk. Tanpri verifye enfòmasyon ou yo."
        }
      code == "ERROR_OPERATION_NOT_ALLOWED" || rawMsg.contains("operation-not-allowed") || rawMsg.contains("operation_not_allowed") ->
        when (lang) {
          "fr" -> "La connexion par e-mail/mot de passe n'est pas encore activée dans Firebase Console."
          "en" -> "Email/Password sign-in is not yet enabled in Firebase Console."
          "es" -> "El acceso con correo y contraseña aún no está habilitado en Firebase Console."
          else -> "Koneksyon Imèl + Modpas poko aktive nan Firebase Console pwojè sa a."
        }
      else -> error.localizedMessage ?: localizedGenericAuthError(lang)
    }
  }

  fun localizedInvalidEmail(lang: String): String =
    when (lang) {
      "fr" -> "Veuillez saisir une adresse e-mail valide."
      "en" -> "Please enter a valid email address."
      "es" -> "Por favor, ingrese una dirección de correo válida."
      else -> "Tanpri antre yon adrès imèl ki valid."
    }

  fun localizedWeakPassword(lang: String): String =
    when (lang) {
      "fr" -> "Le mot de passe doit contenir au moins 6 caractères."
      "en" -> "Password must be at least 6 characters long."
      "es" -> "La contraseña debe tener al menos 6 caracteres."
      else -> "Modpas la dwe gen omwen 6 karaktè."
    }

  fun localizedEmptyPassword(lang: String): String =
    when (lang) {
      "fr" -> "Veuillez saisir votre mot de passe."
      "en" -> "Please enter your password."
      "es" -> "Por favor, ingrese su contraseña."
      else -> "Tanpri antre modpas ou a."
    }

  fun localizedEmailInUseWrongPassword(lang: String): String =
    when (lang) {
      "fr" -> "Cet e-mail possède déjà un compte OmniChurch avec un mot de passe différent."
      "en" -> "This email already owns an account with a different password."
      "es" -> "Este correo ya tiene una cuenta con una contraseña diferente."
      else -> "Imèl sa a deja gen yon kont ak yon lòt modpas. Antre modpas ki kòrèk la."
    }

  fun localizedInvalidChurchName(lang: String): String =
    when (lang) {
      "fr" -> "Veuillez saisir le nom officiel de votre église."
      "en" -> "Please enter your church's official name."
      "es" -> "Por favor, ingrese el nombre oficial de su iglesia."
      else -> "Tanpri antre non ofisyèl legliz la."
    }

  fun localizedNoSignedInUser(lang: String): String =
    when (lang) {
      "fr" -> "Session expirée. Veuillez vous reconnecter avec votre e-mail ou Google."
      "en" -> "Please sign in with your Email & Password or Google account first."
      "es" -> "Inicie sesión primero con su correo y contraseña o cuenta de Google."
      else -> "Tanpri konekte ak Imèl + Modpas ou oswa Kont Google ou anvan."
    }

  fun localizedQuickPinUnavailable(lang: String): String =
    when (lang) {
      "fr" -> "Le déverrouillage rapide par PIN n'est disponible qu'après une connexion complète du propriétaire."
      "en" -> "Quick PIN resume requires an active signed-in church owner session."
      "es" -> "El PIN rápido requiere una sesión activa del propietario de la iglesia."
      else -> "Deblokaj ak PIN rapid disponib sèlman lè pwopriyetè legliz la deja konekte sou aparèy la."
    }

  fun localizedWrongPin(lang: String): String =
    when (lang) {
      "fr" -> "Code PIN administrateur incorrect."
      "en" -> "Incorrect Admin PIN."
      "es" -> "PIN de administrador incorrecto."
      else -> "Kòd PIN Administratè a pa kòrèk."
    }

  fun localizedPinFormatError(lang: String): String =
    when (lang) {
      "fr" -> "Le code PIN rapide doit contenir entre 4 et 6 chiffres."
      "en" -> "Quick PIN must be 4 to 6 numeric digits."
      "es" -> "El PIN rápido debe tener de 4 a 6 dígitos numéricos."
      else -> "Kòd PIN rapid la dwe gen ant 4 ak 6 chif sèlman."
    }

  fun localizedQuickPinSetupRequired(lang: String): String =
    when (lang) {
      "fr" -> "Définissez un code PIN de 4 à 6 chiffres pour activer le déverrouillage rapide."
      "en" -> "Please set a 4 to 6 digit numeric PIN to enable Quick PIN Unlock."
      "es" -> "Configure un PIN numérico de 4 a 6 dígitos para activar el desbloqueo rápido."
      else -> "Tanpri kreye yon PIN 4 a 6 chif pou aktive Deblokaj Rapid ak PIN."
    }

  fun localizedMismatchedOwnerError(
    languageCode: String,
    churchName: String,
    expectedEmail: String?,
    currentEmail: String?,
  ): String {
    val ownerHint = expectedEmail?.takeIf { it.isNotBlank() } ?: churchName
    val usedHint = currentEmail?.takeIf { it.isNotBlank() } ?: "account"
    return when (languageCode) {
      "fr" ->
        "Ce compte ($usedHint) n'est pas le propriétaire autorisé pour $churchName. Connectez-vous avec : $ownerHint"
      "en" ->
        "Account ($usedHint) is not the authorized owner for $churchName. Please sign in with: $ownerHint"
      "es" ->
        "La cuenta ($usedHint) no es el propietario autorizado de $churchName. Inicie sesión con: $ownerHint"
      else ->
        "Kont sa a ($usedHint) pa pwopriyetè otorize pou legliz $churchName. Tanpri konekte ak: $ownerHint"
    }
  }

  fun localizedGenericAuthError(lang: String): String =
    when (lang) {
      "fr" -> "Échec d'authentification. Veuillez réessayer."
      "en" -> "Authentication failed. Please try again."
      "es" -> "Error de autenticación. Inténtelo de nuevo."
      else -> "Otantifikasyon an echwe. Tanpri eseye ankò."
    }
}
