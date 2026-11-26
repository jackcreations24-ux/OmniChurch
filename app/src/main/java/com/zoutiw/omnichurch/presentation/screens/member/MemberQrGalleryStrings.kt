package com.zoutiw.omnichurch.presentation.screens.member

import com.zoutiw.omnichurch.core.utilities.OmniLocalization

internal data class MemberQrGalleryCopy(
  val title: String,
  val qrContentDescription: String,
  val memberIdFormat: String,
  val offlineCachedLabel: String,
  val copyLinkLabel: String,
  val copyLinkToast: String,
  val shareQrLabel: String,
  val verifyCardLabel: String,
  val attendanceHistoryTitle: String,
  val legendPresent: String,
  val legendLate: String,
  val legendAbsent: String,
  val emptyHistoryMsg: String,
  val crossChurchVisitsTitle: String,
  val emptyVisitsMsg: String,
  val statusPresent: String,
  val statusLate: String,
  val statusAbsent: String,
  val statusExcused: String,
  val visitorBadge: String,
)

internal object MemberQrGalleryStrings {
  fun forLanguage(languageCode: String): MemberQrGalleryCopy =
    when (OmniLocalization.normalize(languageCode)) {
      "fr" ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Code QR Officiel Membre",
          memberIdFormat = "ID Membre : %s",
          offlineCachedLabel = "Jeton QR Vérifié • Prêt à Scanner",
          copyLinkLabel = "Copier Lien",
          copyLinkToast = "Lien de vérification officiel copié !",
          shareQrLabel = "Partager QR",
          verifyCardLabel = "Vérifier Carte",
          attendanceHistoryTitle = "Historique",
          legendPresent = "Présent",
          legendLate = "Retard",
          legendAbsent = "Absent",
          emptyHistoryMsg = "Aucun historique de présence pour les dates de culte.",
          crossChurchVisitsTitle = "Mes Visites dans d'Autres Églises",
          emptyVisitsMsg = "Aucune visite enregistrée dans d'autres églises.",
          statusPresent = "Présent",
          statusLate = "En Retard",
          statusAbsent = "Absent",
          statusExcused = "Excusé",
          visitorBadge = "Visiteur ✓",
        )
      "en" ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Official Member QR Code",
          memberIdFormat = "Member ID: %s",
          offlineCachedLabel = "Verified QR Token • Ready to Scan",
          copyLinkLabel = "Copy Link",
          copyLinkToast = "Official verification link copied!",
          shareQrLabel = "Share QR",
          verifyCardLabel = "Verify Card",
          attendanceHistoryTitle = "Attendance History",
          legendPresent = "Present",
          legendLate = "Late",
          legendAbsent = "Absent",
          emptyHistoryMsg = "No attendance history recorded for service dates yet.",
          crossChurchVisitsTitle = "My Visits to Other Churches",
          emptyVisitsMsg = "No visits recorded at other churches yet.",
          statusPresent = "Present",
          statusLate = "Late",
          statusAbsent = "Absent",
          statusExcused = "Excused",
          visitorBadge = "Visitor ✓",
        )
      "es" ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Código QR Oficial de Miembro",
          memberIdFormat = "ID Miembro: %s",
          offlineCachedLabel = "Token QR Verificado • Listo para Escanear",
          copyLinkLabel = "Copiar Enlace",
          copyLinkToast = "¡Enlace de verificación oficial copiado!",
          shareQrLabel = "Compartir QR",
          verifyCardLabel = "Verificar Tarjeta",
          attendanceHistoryTitle = "Historial",
          legendPresent = "Presente",
          legendLate = "Tarde",
          legendAbsent = "Ausente",
          emptyHistoryMsg = "Aún no hay historial de asistencia para las fechas de servicio.",
          crossChurchVisitsTitle = "Mis Visitas a Otras Iglesias",
          emptyVisitsMsg = "Aún no hay visitas registradas en otras iglesias.",
          statusPresent = "Presente",
          statusLate = "Tarde",
          statusAbsent = "Ausente",
          statusExcused = "Excusado",
          visitorBadge = "Visitante ✓",
        )
      else ->
        MemberQrGalleryCopy(
          title = "QR",
          qrContentDescription = "Kòd QR Ofisyèl Manm",
          memberIdFormat = "ID Manm: %s",
          offlineCachedLabel = "Token QR Verifye • Pare pou Eskane",
          copyLinkLabel = "Kopye Lyen",
          copyLinkToast = "Lyen verifikasyon ofisyèl la kopye!",
          shareQrLabel = "Pataje QR",
          verifyCardLabel = "Verifye Kat",
          attendanceHistoryTitle = "Istwa Prezans",
          legendPresent = "Prezan",
          legendLate = "An Reta",
          legendAbsent = "Absan",
          emptyHistoryMsg = "Poko gen okenn istwa prezans pou dat sèvis yo.",
          crossChurchVisitsTitle = "Vizit Mwen nan Lòt Legliz",
          emptyVisitsMsg = "Poko gen okenn vizit anrejistre nan lòt legliz.",
          statusPresent = "Prezan",
          statusLate = "An Reta",
          statusAbsent = "Absan",
          statusExcused = "Ekskize",
          visitorBadge = "Vizitè ✓",
        )
    }
}
