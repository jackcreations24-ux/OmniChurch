package com.zoutiw.omnichurch.core.utilities

object OmniLocalization {

  fun normalize(code: String?): String {
    val lang = code?.trim()?.lowercase() ?: "ht"
    return if (lang in listOf("ht", "fr", "en", "es")) lang else "ht"
  }

  fun navHome(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Accueil"
      "en" -> "Home"
      "es" -> "Inicio"
      else -> "Akèy"
    }

  fun navServices(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Cultes"
      "en" -> "Services"
      "es" -> "Servicios"
      else -> "Sèvis"
    }

  fun navNotifications(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Alertes"
      "en" -> "Alerts"
      "es" -> "Alertas"
      else -> "Anons"
    }

  fun navProfile(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Profil"
      "en" -> "Profile"
      "es" -> "Perfil"
      else -> "Pwofil"
    }

  fun navQrCenterDescription(lang: String, isScannerMode: Boolean): String =
    when (normalize(lang)) {
      "fr" -> if (isScannerMode) "Scanner QR" else "Mon Code QR"
      "en" -> if (isScannerMode) "QR Scanner" else "My QR Code"
      "es" -> if (isScannerMode) "Escáner QR" else "Mi Código QR"
      else -> if (isScannerMode) "Eskanè QR" else "Kòd QR Mwen"
    }

  fun memberListAction(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Liste des Membres"
      "en" -> "Member List"
      "es" -> "Lista de Miembros"
      else -> "Lis Manm"
    }

  fun memberCardAction(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Carte Membre"
      "en" -> "Member ID"
      "es" -> "Carnet"
      else -> "Kat Manm"
    }

  fun defaultMemberName(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Membre"
      "en" -> "Member"
      "es" -> "Miembro"
      else -> "Manm"
    }

  fun defaultDepartmentName(lang: String): String =
    when (normalize(lang)) {
      "fr" -> "Adoration"
      "en" -> "Worship"
      "es" -> "Adoración"
      else -> "Adorasyon"
    }

  fun homeSpaceSubtitle(lang: String, isAdmin: Boolean): String =
    when (normalize(lang)) {
      "fr" -> if (isAdmin) "Espace Admin" else "Membre Officiel"
      "en" -> if (isAdmin) "Admin Space" else "Official Member"
      "es" -> if (isAdmin) "Espacio Admin" else "Miembro Oficial"
      else -> if (isAdmin) "Espas Admin" else "Manm Ofisyèl"
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

object ProfileStrings {
  fun forLanguage(lang: String): ProfileStringValues {
    val norm = OmniLocalization.normalize(lang)
    return when (norm) {
      "fr" -> ProfileStringValues(
        screenTitle = "Profil & Paramètres",
        changePhoto = "Changer la photo de profil",
        changePhotoSub = "Sélectionnez une nouvelle photo d'identité pour votre profil et carte membre.",
        takePhoto = "Prendre une photo avec l'appareil",
        takePhotoSub = "Ouvrir l'appareil photo du téléphone",
        pickGallery = "Choisir dans la galerie",
        pickGallerySub = "Sélectionner une photo depuis votre appareil",
        cameraError = "Erreur de l'appareil photo : %s",
        cameraDenied = "Autorisation de l'appareil photo refusée. Vous pouvez toujours choisir une photo dans la Galerie.",
        photoError = "Erreur photo : %s",
        updatingPhoto = "Mise à jour de la photo de profil...",
        shareApp = "Partager l'application OmniChurch",
        securityTitle = "Sécurité & Compte Lié (Optionnel)",
        linkGoogle = "Lier un compte Google (Optionnel)",
        linkGoogleSub = "Connectez-vous avec Google sur n'importe quel téléphone",
        linkedGoogle = "Compte Google lié : %s",
        unlink = "Délier",
        biometricTitle = "Déverrouillage biométrique (Empreinte / Face ID)",
        biometricSub = "Déverrouiller la session ID %s sans retaper le code PIN",
        biometricPromptTitle = "Activer Empreinte / Face ID",
        biometricPromptSub = "Confirmez votre identité pour lier au matricule ID %s",
        languageLabel = "Langue de l'application",
        personalInfoTitle = "Informations Personnelles du Membre",
        adminOnlyNotice = "Vous pouvez cliquer sur votre photo de profil ci-dessus pour la modifier à tout moment. Pour vos autres informations personnelles (Nom, Matricule, Département, Rôle), seule l'administration de l'église est autorisée à les modifier.",
        fullNameLabel = "Nom Complet",
        matriculeLabel = "ID Matricule Officiel",
        churchLabel = "Église",
        departmentLabel = "Département",
        roleLabel = "Rôle / Ministère",
        phoneLabel = "Téléphone de Contact",
        copiedToast = "ID Membre : %s copié !",
        sessionPinSub = "Session ID Matricule + PIN",
        signOut = "Se déconnecter",
      )
      "en" -> ProfileStringValues(
        screenTitle = "Profile & Settings",
        changePhoto = "Change Profile Photo",
        changePhotoSub = "Take or choose a new portrait photo for your profile and digital member card.",
        takePhoto = "Take a Photo with Camera",
        takePhotoSub = "Open device camera directly",
        pickGallery = "Choose from Gallery",
        pickGallerySub = "Select an existing photo from device storage",
        cameraError = "Camera error: %s",
        cameraDenied = "Camera permission was denied. You can still choose a photo from the Gallery.",
        photoError = "Photo error: %s",
        updatingPhoto = "Updating profile photo...",
        shareApp = "Share OmniChurch App",
        securityTitle = "Security & Linked Account (Optional)",
        linkGoogle = "Link Google Account (Optional)",
        linkGoogleSub = "Sign in with Google on any device",
        linkedGoogle = "Linked Google account: %s",
        unlink = "Unlink",
        biometricTitle = "Biometric Quick Unlock (Fingerprint / Face ID)",
        biometricSub = "Unlock session ID %s without retyping PIN",
        biometricPromptTitle = "Enable Fingerprint / Face ID",
        biometricPromptSub = "Confirm your identity to link with Member ID %s",
        languageLabel = "App Language",
        personalInfoTitle = "Member Personal Information",
        adminOnlyNotice = "You can tap your profile photo above to change it anytime. For all other personal information (Full Name, Matricule, Department, Role), only church administrators are authorized to update them.",
        fullNameLabel = "Full Name",
        matriculeLabel = "Official Member ID",
        churchLabel = "Church",
        departmentLabel = "Department",
        roleLabel = "Role / Ministry",
        phoneLabel = "Contact Phone",
        copiedToast = "Member ID: %s copied!",
        sessionPinSub = "Member ID + PIN Session",
        signOut = "Sign Out",
      )
      "es" -> ProfileStringValues(
        screenTitle = "Perfil y Configuración",
        changePhoto = "Cambiar Foto de Perfil",
        changePhotoSub = "Toma o elige una nueva foto para tu perfil y tarjeta digital de miembro.",
        takePhoto = "Tomar Foto con la Cámara",
        takePhotoSub = "Abrir la cámara del dispositivo directamente",
        pickGallery = "Elegir de la Galería",
        pickGallerySub = "Seleccionar una foto existente en el dispositivo",
        cameraError = "Error de cámara: %s",
        cameraDenied = "Permiso de cámara denegado. Aún puedes elegir una foto de la Galería.",
        photoError = "Error de foto: %s",
        updatingPhoto = "Actualizando foto de perfil...",
        shareApp = "Compartir la Aplicación OmniChurch",
        securityTitle = "Seguridad y Cuenta Vinculada (Opcional)",
        linkGoogle = "Vincular Cuenta de Google (Opcional)",
        linkGoogleSub = "Inicia sesión con Google en cualquier dispositivo",
        linkedGoogle = "Cuenta de Google vinculada: %s",
        unlink = "Desvincular",
        biometricTitle = "Desbloqueo Biométrico (Huella / Face ID)",
        biometricSub = "Desbloquear sesión ID %s sin volver a escribir el PIN",
        biometricPromptTitle = "Activar Huella / Face ID",
        biometricPromptSub = "Confirma tu identidad para vincular con el ID %s",
        languageLabel = "Idioma de la Aplicación",
        personalInfoTitle = "Información Personal del Miembro",
        adminOnlyNotice = "Puedes tocar tu foto de perfil arriba para cambiarla en cualquier momento. Para tus demás datos personales (Nombre, Matrícula, Departamento, Rol), solo la administración de la iglesia está autorizada a modificarlos.",
        fullNameLabel = "Nombre Completo",
        matriculeLabel = "ID Matrícula Oficial",
        churchLabel = "Iglesia",
        departmentLabel = "Departamento",
        roleLabel = "Rol / Ministerio",
        phoneLabel = "Teléfono de Contacto",
        copiedToast = "¡ID de Miembro: %s copiado!",
        sessionPinSub = "Sesión ID Matrícula + PIN",
        signOut = "Cerrar Sesión",
      )
      else -> ProfileStringValues(
        screenTitle = "Profil & Paramèt",
        changePhoto = "Chanje Foto Pwofil Manm",
        changePhotoSub = "Chwazi kijan ou vle pran oswa chwazi nouvo foto paspò w la pou mete sou pwofil ak kat idantite w.",
        takePhoto = "Pran yon Foto ak Kamera",
        takePhotoSub = "Louvri kamera telefòn lan dirèkteman",
        pickGallery = "Chwazi nan Galri",
        pickGallerySub = "Chwazi yon foto ki deja sou aparèy la",
        cameraError = "Erè kamera : %s",
        cameraDenied = "Otorizasyon kamera refize. Ou ka toujou chwazi yon foto nan Galri a.",
        photoError = "Erè foto : %s",
        updatingPhoto = "Mete ajou foto pwofil la...",
        shareApp = "Pataje Aplikasyon OmniChurch la",
        securityTitle = "Sekirite & Kont Lye (Opsyonèl)",
        linkGoogle = "Lye yon kont Google (Opsyonèl)",
        linkGoogleSub = "Konekte ak Google sou nenpòt telefòn",
        linkedGoogle = "Kont Google lye : %s",
        unlink = "Delye",
        biometricTitle = "Deblokaj Biyometrik (Anprent / Face ID)",
        biometricSub = "Debloke sesyon ID %s san retape PIN",
        biometricPromptTitle = "Aktive Anprent / Face ID",
        biometricPromptSub = "Konfime idantite w pou lye ak ID %s",
        languageLabel = "Lang Aplikasyon an",
        personalInfoTitle = "Enfòmasyon Pèsonèl Manm nan",
        adminOnlyNotice = "Ou gen dwa klike sou foto pwofil ou anlè a pou chanje l nenpòt kilè. Pou nenpòt lòt done pèsonèl ou (Non, Matrikil, Depatman, Wòl), se administrasyon legliz la sèlman ki gen otorizasyon pou modifye yo.",
        fullNameLabel = "Non Konplè",
        matriculeLabel = "ID Matrikil Ofisyèl",
        churchLabel = "Legliz",
        departmentLabel = "Depatman",
        roleLabel = "Wòl / Ministè",
        phoneLabel = "Telefòn Kontak",
        copiedToast = "ID Manm : %s kopye!",
        sessionPinSub = "Sesyon ID Matrikil + PIN",
        signOut = "Dekonekte",
      )
    }
  }
}

data class ProfileStringValues(
  val screenTitle: String,
  val changePhoto: String,
  val changePhotoSub: String,
  val takePhoto: String,
  val takePhotoSub: String,
  val pickGallery: String,
  val pickGallerySub: String,
  val cameraError: String,
  val cameraDenied: String,
  val photoError: String,
  val updatingPhoto: String,
  val shareApp: String,
  val securityTitle: String,
  val linkGoogle: String,
  val linkGoogleSub: String,
  val linkedGoogle: String,
  val unlink: String,
  val biometricTitle: String,
  val biometricSub: String,
  val biometricPromptTitle: String,
  val biometricPromptSub: String,
  val languageLabel: String,
  val personalInfoTitle: String,
  val adminOnlyNotice: String,
  val fullNameLabel: String,
  val matriculeLabel: String,
  val churchLabel: String,
  val departmentLabel: String,
  val roleLabel: String,
  val phoneLabel: String,
  val copiedToast: String,
  val sessionPinSub: String,
  val signOut: String,
)
