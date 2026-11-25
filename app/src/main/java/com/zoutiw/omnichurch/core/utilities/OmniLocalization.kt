package com.zoutiw.omnichurch.core.utilities

object OmniLocalization {

  fun normalize(code: String?): String {
    val lang = code?.trim()?.lowercase() ?: "ht"
    return if (lang in listOf("ht", "fr", "en", "es")) lang else "ht"
  }

  fun homeTabFeed(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Fil d'Actualité"
      "en" -> "News Feed"
      "es" -> "Noticias"
      else -> "Fil Aktivite"
    }

  fun homeTabServices(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Services"
      "en" -> "Services"
      "es" -> "Servicios"
      else -> "Sèvis"
    }

  fun homeTabCard(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Carte Membre"
      "en" -> "Member Card"
      "es" -> "Tarjeta"
      else -> "Kat Manm"
    }

  fun homeTabProfile(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Profil"
      "en" -> "Profile"
      "es" -> "Perfil"
      else -> "Pwofil"
    }

  fun publishAnnouncementTitle(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Publier une Annonce"
      "en" -> "Publish Announcement"
      "es" -> "Publicar Anuncio"
      else -> "Pibliye Yon Anons"
    }

  fun createServiceTitle(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Créer un Nouveau Service"
      "en" -> "Create New Service"
      "es" -> "Crear Nuevo Servicio"
      else -> "Kreye Yon Nouvo Sèvis"
    }

  fun scannerTabAttendance(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Présence"
      "en" -> "Attendance"
      "es" -> "Asistencia"
      else -> "Prezans"
    }

  fun scannerTabLate(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Retard"
      "en" -> "Late"
      "es" -> "Retraso"
      else -> "Reta"
    }

  fun scannerTabAbsent(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Absence"
      "en" -> "Absence"
      "es" -> "Ausencia"
      else -> "Absans"
    }

  fun scannerTabVisitor(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Visiteur"
      "en" -> "Visitor"
      "es" -> "Visitante"
      else -> "Vizitè"
    }

  fun buttonSave(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Enregistrer"
      "en" -> "Save"
      "es" -> "Guardar"
      else -> "Anrejistre"
    }

  fun buttonCancel(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Annuler"
      "en" -> "Cancel"
      "es" -> "Cancelar"
      else -> "Anile"
    }

  fun buttonPublish(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Publier"
      "en" -> "Publish"
      "es" -> "Publicar"
      else -> "Pibliye"
    }

  fun buttonClose(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Fermer"
      "en" -> "Close"
      "es" -> "Cerrar"
      else -> "Fèmen"
    }

  fun labelDepartment(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Département"
      "en" -> "Department"
      "es" -> "Departamento"
      else -> "Depatman"
    }

  fun labelRole(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Rôle / Ministère"
      "en" -> "Role / Ministry"
      "es" -> "Rol / Ministerio"
      else -> "Wòl / Ministè"
    }

  fun labelPhone(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Téléphone"
      "en" -> "Phone"
      "es" -> "Teléfono"
      else -> "Telefòn"
    }

  fun labelEmail(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Email"
      "en" -> "Email"
      "es" -> "Correo"
      else -> "Imèl"
    }
}
