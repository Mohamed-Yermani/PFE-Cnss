package com.projet.cnss.services;

import com.projet.cnss.dto.AiVerificationResult;
import com.projet.cnss.dto.DossierUploadResponse;
import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;
import com.projet.cnss.exception.AiVerificationException;
import com.projet.cnss.repository.DossierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierServiceTest {

    @Mock private DossierRepository dossierRepository;
    @Mock private MinioService minioService;
    @Mock private AiVerificationService aiVerificationService;
    @Mock private PdfExtractorService pdfExtractorService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private DossierService dossierService;

    private User user;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setNom("Dupont");
        user.setPrenom("Jean");
        user.setEmail("jean@test.com");

        file = new MockMultipartFile("file", "formulaire.pdf",
                "application/pdf", "contenu test".getBytes());
    }

    private Dossier buildDossier(Long id, String typeAvantage, Integer aiScore) {
        Dossier d = new Dossier();
        d.setId(id);
        d.setFileName("formulaire.pdf");
        d.setAlfrescoNodeId("object-name");
        d.setStatut("EN_ATTENTE");
        d.setUser(user);
        d.setTypeAvantage(typeAvantage);
        d.setAiScore(aiScore);
        d.setDateUpload(LocalDateTime.now());
        return d;
    }

    private AiVerificationResult buildValideResult(int score) {
        AiVerificationResult.SectionDetail ok = AiVerificationResult.SectionDetail.builder()
                .statut("OK").commentaire("Validé").build();
        return AiVerificationResult.builder()
                .valide(true)
                .score(score)
                .resume("Formulaire validé")
                .champsManquants(List.of())
                .champsInvalides(List.of())
                .detailIdentite(ok)
                .detailEmployeur(ok)
                .detailTypeDossier(ok)
                .detailPeriode(ok)
                .detailSignature(ok)
                .detailCoherence(ok)
                .build();
    }

    // ==========================================================
    // uploadDossier
    // ==========================================================

    @Test
    void uploadDossier_aiValideTrue_withPositiveScore_buildsResponseWithoutCallingAiService() throws Exception {
        when(pdfExtractorService.extraireTexte(file)).thenReturn("CONTENU SANS TYPE COCHE");
        when(minioService.uploadDocument("jean@test.com", file)).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> {
            Dossier d = invocation.getArgument(0);
            d.setId(10L);
            return d;
        });
        when(aiVerificationService.getBadge(95)).thenReturn("EXCELLENT");

        DossierUploadResponse response = dossierService.uploadDossier(
                file, "jean@test.com", user, true, 95);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("formulaire.pdf", response.getFileName());
        assertEquals("EN_ATTENTE", response.getStatut());
        assertTrue(response.getAiVerification().isValide());
        assertEquals(95, response.getAiVerification().getScore());
        verify(aiVerificationService, never()).verifierContenuPdf(anyString());
        verify(notificationService).notifierNouveauDossier(any(Dossier.class));
    }

    @Test
    void uploadDossier_aiValideTrue_withZeroOrNegativeScore_defaultsTo100() throws Exception {
        when(pdfExtractorService.extraireTexte(file)).thenReturn("texte");
        when(minioService.uploadDocument("jean@test.com", file)).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(100)).thenReturn("EXCELLENT");

        DossierUploadResponse response = dossierService.uploadDossier(
                file, "jean@test.com", user, true, 0);

        assertEquals(100, response.getAiVerification().getScore());
    }

    @Test
    void uploadDossier_aiValideFalse_backendVerificationValide_savesDossier() throws Exception {
        AiVerificationResult result = buildValideResult(80);

        when(pdfExtractorService.extraireTexte(file)).thenReturn("texte");
        when(aiVerificationService.verifierContenuPdf("texte")).thenReturn(result);
        when(minioService.uploadDocument("jean@test.com", file)).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(80)).thenReturn("BON");

        DossierUploadResponse response = dossierService.uploadDossier(
                file, "jean@test.com", user, false, 0);

        assertNotNull(response);
        assertEquals(80, response.getAiVerification().getScore());
        verify(aiVerificationService).verifierContenuPdf("texte");
        verify(notificationService).notifierNouveauDossier(any(Dossier.class));
    }

    @Test
    void uploadDossier_aiValideFalse_backendVerificationInvalide_throwsAiVerificationException() throws Exception {
        AiVerificationResult result = AiVerificationResult.builder()
                .valide(false)
                .score(20)
                .resume("Formulaire incomplet")
                .champsManquants(List.of("nom"))
                .champsInvalides(List.of())
                .build();

        when(pdfExtractorService.extraireTexte(file)).thenReturn("texte invalide");
        when(aiVerificationService.verifierContenuPdf("texte invalide")).thenReturn(result);

        AiVerificationException ex = assertThrows(AiVerificationException.class,
                () -> dossierService.uploadDossier(file, "jean@test.com", user, false, 0));

        assertTrue(ex.getMessage().contains("incomplet ou invalide"));
        assertEquals(result, ex.getVerification());
        verify(dossierRepository, never()).save(any());
        verify(minioService, never()).uploadDocument(anyString(), any());
    }

    @Test
    void uploadDossier_extraitTypeAvantage_maladieProfessionnelle() throws Exception {
        String contenu = "DEMANDE\n[X] MALADIE PROFESSIONNELLE\nFIN";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Maladie Professionnelle".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_accidentDeTravail() throws Exception {
        String contenu = "[ X ] ACCIDENT DE TRAVAIL";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Accident de Travail".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_indemnites() throws Exception {
        String contenu = "☒ INDEMNITES JOURNALIERES";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Indemnités Journalières".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_invalidite() throws Exception {
        String contenu = "☑ INVALIDITE PERMANENTE";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Invalidité".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_decesAvecAccent() throws Exception {
        String contenu = "[X] DÉCÈS DE L'ASSURE";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Décès".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_decesSansAccent() throws Exception {
        String contenu = "[X] DECES DE L'ASSURE";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Décès".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_regimeGeneralAvecAccent() throws Exception {
        String contenu = "[X] RÉGIME GÉNÉRAL";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Régime Général".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_regimeGeneralSansAccent() throws Exception {
        String contenu = "[X] REGIME GENERAL";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Régime Général".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_retraite() throws Exception {
        String contenu = "[X] RETRAITE";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Retraite".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_vieillesse() throws Exception {
        String contenu = "[X] VIEILLESSE";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Retraite".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_extraitTypeAvantage_ligneCocheeSansTypeReconnu_continueLoop() throws Exception {
        String contenu = "[X] AUTRE CHOSE NON RECONNUE\n[X] RETRAITE";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Retraite".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_contenuPdfBlank_defaultTypeAvantage() throws Exception {
        when(pdfExtractorService.extraireTexte(file)).thenReturn("   ");
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Avantage CNSS".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_aucuneCaseCochee_defaultTypeAvantage() throws Exception {
        when(pdfExtractorService.extraireTexte(file)).thenReturn("RETRAITE sans case cochee");
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Avantage CNSS".equals(d.getTypeAvantage())));
    }

    @Test
    void uploadDossier_carriageReturnsHandled() throws Exception {
        String contenu = "DEBUT\r\n[X] RETRAITE\r\nFIN";
        when(pdfExtractorService.extraireTexte(file)).thenReturn(contenu);
        when(minioService.uploadDocument(anyString(), any())).thenReturn("object-name");
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiVerificationService.getBadge(anyInt())).thenReturn("EXCELLENT");

        dossierService.uploadDossier(file, "jean@test.com", user, true, 100);

        verify(dossierRepository).save(argThat(d -> "Retraite".equals(d.getTypeAvantage())));
    }

    // ==========================================================
    // downloadDossier
    // ==========================================================

    @Test
    void downloadDossier_success() throws Exception {
        Dossier dossier = buildDossier(1L, "Retraite", 90);
        byte[] content = "pdf-bytes".getBytes();

        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(minioService.downloadDocument("object-name")).thenReturn(content);

        byte[] result = dossierService.downloadDossier(1L);

        assertArrayEquals(content, result);
    }

    @Test
    void downloadDossier_notFound_throwsException() {
        when(dossierRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dossierService.downloadDossier(99L));

        assertTrue(ex.getMessage().contains("Dossier non trouve"));
    }

    // ==========================================================
    // deleteDossier
    // ==========================================================

    @Test
    void deleteDossier_success() throws Exception {
        Dossier dossier = buildDossier(1L, "Retraite", 90);
        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));

        dossierService.deleteDossier(1L, "jean@test.com");

        verify(minioService).deleteDocument("object-name");
        verify(dossierRepository).delete(dossier);
    }

    @Test
    void deleteDossier_notFound_throwsException() {
        when(dossierRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dossierService.deleteDossier(99L, "jean@test.com"));

        assertTrue(ex.getMessage().contains("Dossier non trouve"));
        verify(dossierRepository, never()).delete(any());
    }

    // ==========================================================
    // getAllDossiers / getDossiersByUser / getDossiersByStatut
    // ==========================================================

    @Test
    void getAllDossiers_returnsAll() {
        List<Dossier> dossiers = List.of(buildDossier(1L, "Retraite", 90));
        when(dossierRepository.findAll()).thenReturn(dossiers);

        List<Dossier> result = dossierService.getAllDossiers();

        assertEquals(1, result.size());
    }

    @Test
    void getDossiersByUser_returnsFiltered() {
        List<Dossier> dossiers = List.of(buildDossier(1L, "Retraite", 90));
        when(dossierRepository.findByUser(user)).thenReturn(dossiers);

        List<Dossier> result = dossierService.getDossiersByUser(user);

        assertEquals(1, result.size());
    }

    @Test
    void getDossiersByStatut_returnsFiltered() {
        List<Dossier> dossiers = List.of(buildDossier(1L, "Retraite", 90));
        when(dossierRepository.findByStatut("EN_ATTENTE")).thenReturn(dossiers);

        List<Dossier> result = dossierService.getDossiersByStatut("EN_ATTENTE");

        assertEquals(1, result.size());
    }

    // ==========================================================
    // getStatistics
    // ==========================================================

    @Test
    void getStatistics_withScoredDossiers_computesAverage() {
        Dossier d1 = buildDossier(1L, "Retraite", 80);
        Dossier d2 = buildDossier(2L, "Invalidité", 100);
        Dossier d3 = buildDossier(3L, "Décès", null); // ignoré car aiScore null

        when(dossierRepository.count()).thenReturn(3L);
        when(dossierRepository.countByStatut("EN_ATTENTE")).thenReturn(1L);
        when(dossierRepository.countByStatut("VALIDATION_LOCALE")).thenReturn(0L);
        when(dossierRepository.countByStatut("VALIDE")).thenReturn(1L);
        when(dossierRepository.countByStatut("REFUSE")).thenReturn(1L);
        when(dossierRepository.findAll()).thenReturn(List.of(d1, d2, d3));

        Map<String, Object> stats = dossierService.getStatistics();

        assertEquals(3L, stats.get("total"));
        assertEquals(1L, stats.get("enAttente"));
        assertEquals(0L, stats.get("validationLocale"));
        assertEquals(1L, stats.get("valides"));
        assertEquals(1L, stats.get("refuses"));
        assertEquals(90L, stats.get("avgAiScore")); // (80+100)/2 = 90
    }

    @Test
    void getStatistics_noScoredDossiers_avgIsZero() {
        Dossier d1 = buildDossier(1L, "Retraite", null);

        when(dossierRepository.count()).thenReturn(1L);
        when(dossierRepository.countByStatut(anyString())).thenReturn(0L);
        when(dossierRepository.findAll()).thenReturn(List.of(d1));

        Map<String, Object> stats = dossierService.getStatistics();

        assertEquals(0L, stats.get("avgAiScore"));
    }

    // ==========================================================
    // changerStatut
    // ==========================================================

    @Test
    void changerStatut_withMotif_setsMotifAndStatut() {
        Dossier dossier = buildDossier(1L, "Retraite", 90);
        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dossier result = dossierService.changerStatut(1L, "REFUSE", "Dossier incomplet", "agent@test.com");

        assertEquals("REFUSE", result.getStatut());
        assertEquals("Dossier incomplet", result.getMotifRefus());
        assertEquals("agent@test.com", result.getAgentEmail());
        assertNotNull(result.getDateTraitement());
        verify(notificationService).notifierStatutDossier(result, "REFUSE");
        verify(notificationService).notifierValidationGlobale(result, "REFUSE");
    }

    @Test
    void changerStatut_statutValide_notifieValidationGlobale() {
        Dossier dossier = buildDossier(1L, "Retraite", 90);
        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dossier result = dossierService.changerStatut(1L, "VALIDE", null, "agent@test.com");

        assertEquals("VALIDE", result.getStatut());
        assertNull(result.getMotifRefus());
        verify(notificationService).notifierValidationGlobale(result, "VALIDE");
    }

    @Test
    void changerStatut_statutValidationLocale_notifieValidationGlobale() {
        Dossier dossier = buildDossier(1L, "Retraite", 90);
        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dossier result = dossierService.changerStatut(1L, "VALIDATION_LOCALE", null, "agent@test.com");

        verify(notificationService).notifierValidationGlobale(result, "VALIDATION_LOCALE");
    }

    @Test
    void changerStatut_statutAutre_neNotifiePasValidationGlobale() {
        Dossier dossier = buildDossier(1L, "Retraite", 90);
        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dossierService.changerStatut(1L, "EN_ATTENTE", null, "agent@test.com");

        verify(notificationService, never()).notifierValidationGlobale(any(), anyString());
        verify(notificationService).notifierStatutDossier(any(Dossier.class), eq("EN_ATTENTE"));
    }

    @Test
    void changerStatut_motifNull_neModifiePasMotifRefus() {
        Dossier dossier = buildDossier(1L, "Retraite", 90);
        dossier.setMotifRefus("ancien motif");
        when(dossierRepository.findById(1L)).thenReturn(Optional.of(dossier));
        when(dossierRepository.save(any(Dossier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Dossier result = dossierService.changerStatut(1L, "VALIDE", null, "agent@test.com");

        assertEquals("ancien motif", result.getMotifRefus());
    }

    @Test
    void changerStatut_notFound_throwsException() {
        when(dossierRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dossierService.changerStatut(99L, "VALIDE", null, "agent@test.com"));

        assertTrue(ex.getMessage().contains("Dossier non trouvé"));
    }
}