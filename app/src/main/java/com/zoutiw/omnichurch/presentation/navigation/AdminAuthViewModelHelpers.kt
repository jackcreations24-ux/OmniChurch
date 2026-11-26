package com.zoutiw.omnichurch.presentation.navigation

internal object AdminAuthLocalizedMessages {
  fun selectChurchFirst(lang: String): String =
    when (lang) {
      "fr" -> "Veuillez d'abord sélectionner votre église."
      "en" -> "Please select your church first."
      "es" -> "Por favor, seleccione su iglesia primero."
      else -> "Tanpri chwazi legliz ou a anvan."
    }

  fun passwordMismatch(lang: String): String =
    when (lang) {
      "fr" -> "Les deux mots de passe ne correspondent pas."
      "en" -> "The passwords do not match."
      "es" -> "Las contraseñas no coinciden."
      else -> "De modpas yo pa menm. Tanpri verifye yo."
    }

  fun passwordResetSent(lang: String, email: String): String =
    when (lang) {
      "fr" -> "Un e-mail de réinitialisation a été envoyé à ${email.trim()}."
      "en" -> "A password reset email has been sent to ${email.trim()}."
      "es" -> "Se envió un correo de restablecimiento a ${email.trim()}."
      else -> "Nou voye yon imèl pou reyinisyalize modpas la nan ${email.trim()}."
    }
}
