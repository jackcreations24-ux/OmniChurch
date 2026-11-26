package com.zoutiw.omnichurch.presentation.screens.auth

internal data class AuthPortalCopy(
  val rememberMeLabel: String,
  val forgotPasswordLabel: String,
  val autoChurchBadgeLabel: String,
  val autoMemberBadgeLabel: String,
  val memberSubtitleText: String,
  val ownedChurchesSheetTitle: String,
  val ownedChurchesSheetSubtitle: String,
  val memberMatchedChurchesSubtitle: String,
  val memberTitleText: String,
  val adminTitleText: String,
  val memberIdInputLabel: String,
  val memberPinInputLabel: String,
  val googleButtonCaption: String,
  val adminSubtitleText: String,
  val adminGoogleSubtitleText: String,
  val adminEmailLabel: String,
  val adminPasswordLabel: String,
  val connectButtonText: String,
  val adminTabEmailLabel: String,
  val adminTabGoogleLabel: String,
  val memberTabLabel: String,
  val adminTabLabel: String,
  val memberIdPlaceholder: String,
  val memberPinPlaceholder: String,
  val adminQuickPinInfoText: String,
  val hideLegacyCodeLabel: String,
  val showLegacyCodeLabel: String,
) {
  companion object {
    fun forLanguage(languageCode: String): AuthPortalCopy =
      when (languageCode.lowercase()) {
        "fr" ->
          AuthPortalCopy(
            rememberMeLabel = "Se souvenir de moi",
            forgotPasswordLabel = "Mot de passe oublié ?",
            autoChurchBadgeLabel = "Chargement automatique de votre église • Données isolées",
            autoMemberBadgeLabel = "Reconnaissance automatique de votre église par ID • Données isolées",
            memberSubtitleText =
              "Entrez votre ID Matricule ou téléphone et votre code PIN. Votre église sera reconnue automatiquement.",
            ownedChurchesSheetTitle = "Choisissez votre église",
            ownedChurchesSheetSubtitle =
              "Votre compte gère plusieurs églises. Sélectionnez celle que vous souhaitez ouvrir :",
            memberMatchedChurchesSubtitle =
              "Votre profil membre est vérifié dans ces églises. Choisissez celle que vous souhaitez ouvrir :",
            memberTitleText = "Connexion Membre (ID Matricule + PIN)",
            adminTitleText = "Connexion Administrateur",
            memberIdInputLabel = "ID Matricule ou Téléphone",
            memberPinInputLabel = "Code de Sécurité (PIN)",
            googleButtonCaption = "Google (Si compte lié)",
            adminSubtitleText =
              "Connectez-vous avec l'e-mail et le mot de passe de l'église pour gérer les membres, les services et les présences.",
            adminGoogleSubtitleText =
              "Connectez-vous avec votre compte Google Administrateur pour ouvrir automatiquement votre église.",
            adminEmailLabel = "Email Église / Administrateur",
            adminPasswordLabel = "Mot de passe du compte",
            connectButtonText = "Connexion",
            adminTabEmailLabel = "E-mail + Mot de passe",
            adminTabGoogleLabel = "Google Admin",
            memberTabLabel = "Espace Membre",
            adminTabLabel = "Espace Admin",
            memberIdPlaceholder = "Ex: CH001-JD4827 ou 509...",
            memberPinPlaceholder = "Entrez le PIN fourni par l'administrateur",
            adminQuickPinInfoText =
              "Le code PIN Admin sert à reprendre rapidement votre activité après la mise en pause automatique (Paramètres → Reprise Rapide).",
            hideLegacyCodeLabel = "Masquer l'accès ancien code",
            showLegacyCodeLabel = "Ancien compte avec code d'église ?",
          )
        "en" ->
          AuthPortalCopy(
            rememberMeLabel = "Remember me",
            forgotPasswordLabel = "Forgot password?",
            autoChurchBadgeLabel = "Auto-loads your church • Isolated tenant data",
            autoMemberBadgeLabel = "Auto-detects your church from your ID • Isolated tenant data",
            memberSubtitleText =
              "Enter your Member ID or phone number and PIN. Your church will be detected automatically.",
            ownedChurchesSheetTitle = "Select Your Church",
            ownedChurchesSheetSubtitle =
              "Your account manages multiple churches. Select the one you want to open:",
            memberMatchedChurchesSubtitle =
              "Your member profile is verified in these churches. Select the one you want to open:",
            memberTitleText = "Member Sign-In (Member ID + PIN)",
            adminTitleText = "Church Admin Sign-In",
            memberIdInputLabel = "Member ID or Phone",
            memberPinInputLabel = "Security Code (PIN)",
            googleButtonCaption = "Google (If account linked)",
            adminSubtitleText =
              "Sign in with church email and password to manage members, services, and attendance.",
            adminGoogleSubtitleText =
              "Sign in with your authorized Admin Google account to automatically open your church.",
            adminEmailLabel = "Church / Admin Email",
            adminPasswordLabel = "Church Account Password",
            connectButtonText = "Sign In",
            adminTabEmailLabel = "Email + Password",
            adminTabGoogleLabel = "Admin Google",
            memberTabLabel = "Member Area",
            adminTabLabel = "Admin Area",
            memberIdPlaceholder = "Ex: CH001-JD4827 or 509...",
            memberPinPlaceholder = "Enter the PIN given by administrator",
            adminQuickPinInfoText =
              "Admin Quick PIN is used to rapidly resume activity after an auto-lock pause (configured in Settings → Quick Resume).",
            hideLegacyCodeLabel = "Hide legacy church passcode",
            showLegacyCodeLabel = "Legacy account with church passcode?",
          )
        "es" ->
          AuthPortalCopy(
            rememberMeLabel = "Recordarme",
            forgotPasswordLabel = "¿Olvidaste tu contraseña?",
            autoChurchBadgeLabel = "Carga automática de tu iglesia • Datos aislados",
            autoMemberBadgeLabel = "Detecta tu iglesia automáticamente por tu ID • Datos aislados",
            memberSubtitleText =
              "Ingresa tu ID de miembro o teléfono y tu PIN. Tu iglesia se detectará automáticamente.",
            ownedChurchesSheetTitle = "Selecciona tu iglesia",
            ownedChurchesSheetSubtitle =
              "Tu cuenta administra varias iglesias. Selecciona la que deseas abrir:",
            memberMatchedChurchesSubtitle =
              "Tu perfil de miembro está verificado en estas iglesias. Selecciona la que deseas abrir:",
            memberTitleText = "Inicio de Sesión de Miembro (ID + PIN)",
            adminTitleText = "Inicio de Sesión de Administrador",
            memberIdInputLabel = "ID de Miembro o Teléfono",
            memberPinInputLabel = "Código de Seguridad (PIN)",
            googleButtonCaption = "Google (Si cuenta vinculada)",
            adminSubtitleText =
              "Inicia sesión con el correo y contraseña de la iglesia para administrar miembros, servicios y asistencia.",
            adminGoogleSubtitleText =
              "Inicia sesión con tu cuenta Google de administrador para abrir automáticamente tu iglesia.",
            adminEmailLabel = "Correo de Iglesia / Administrador",
            adminPasswordLabel = "Contraseña de la cuenta",
            connectButtonText = "Conectar",
            adminTabEmailLabel = "Correo + Contraseña",
            adminTabGoogleLabel = "Google Admin",
            memberTabLabel = "Área Miembro",
            adminTabLabel = "Área Admin",
            memberIdPlaceholder = "Ej: CH001-JD4827 o 509...",
            memberPinPlaceholder = "Ingresa el PIN dado por el administrador",
            adminQuickPinInfoText =
              "El PIN rápido de Admin sirve para reanudar tu actividad tras la pausa automática (Ajustes → Reanudación Rápida).",
            hideLegacyCodeLabel = "Ocultar acceso de código antiguo",
            showLegacyCodeLabel = "¿Cuenta antigua con código de iglesia?",
          )
        else ->
          AuthPortalCopy(
            rememberMeLabel = "Sonje mwen",
            forgotPasswordLabel = "Bliye modpas?",
            autoChurchBadgeLabel = "Chajman otomatik legliz pa ou • Done 100% izole",
            autoMemberBadgeLabel = "Rekonesans otomatik legliz pa ou ak ID ou • Done 100% izole",
            memberSubtitleText =
              "Antre ID Matrikil oswa telefòn ou ak Kòd PIN ou. Sistèm lan ap rekonèt legliz pa ou otomatikman.",
            ownedChurchesSheetTitle = "Chwazi Legliz Pa Ou",
            ownedChurchesSheetSubtitle =
              "Kont ou a jere plizyè legliz. Chwazi legliz ou vle ouvri kounye a :",
            memberMatchedChurchesSubtitle =
              "Pwofil manm ou an verifye nan legliz sa yo. Chwazi legliz ou vle ouvri kounye a :",
            memberTitleText = "Koneksyon Manm (ID Matrikil + PIN)",
            adminTitleText = "Koneksyon Administratè Legliz",
            memberIdInputLabel = "ID Manm (Matrikil) oswa Telefòn",
            memberPinInputLabel = "Kòd Sekirite (PIN)",
            googleButtonCaption = "Google (Si kont ou lye)",
            adminSubtitleText =
              "Konekte ak imèl ak modpas kont legliz la pou jere manm yo, sèvis yo ak prezans yo.",
            adminGoogleSubtitleText =
              "Konekte ak Kont Google Administratè ou pou ouvri legliz pa ou otomatikman.",
            adminEmailLabel = "Imèl Legliz / Administratè",
            adminPasswordLabel = "Modpas Kont Legliz la",
            connectButtonText = "Konekte",
            adminTabEmailLabel = "Imèl + Modpas",
            adminTabGoogleLabel = "Google Admin",
            memberTabLabel = "Espas Manm",
            adminTabLabel = "Espas Admin",
            memberIdPlaceholder = "Egzanp: CH001-JD4827 oswa 509...",
            memberPinPlaceholder = "Antre PIN administratè a ba ou a",
            adminQuickPinInfoText =
              "PIN Admin lan sèvi pou reprann aktivite w rapid apre tan de poz la (aktive li nan Paramèt → Reprann Aktivite Rapid).",
            hideLegacyCodeLabel = "Kache aksè ansyen kòd legliz la",
            showLegacyCodeLabel = "Ansyen kont ki gen kòd legliz?",
          )
      }
  }
}
