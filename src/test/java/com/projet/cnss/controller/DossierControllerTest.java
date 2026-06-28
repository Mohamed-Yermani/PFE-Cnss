package com.projet.cnss.controller;

import com.projet.cnss.dto.AiVerificationResult;
import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import com.projet.cnss.services.AiVerificationService;
import com.projet.cnss.services.DossierService;
import com.projet.cnss.services.PdfExtractorService;
import com.projet.cnss.services.PdfFormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DossierControllerTest {

    @Mock private DossierService dossierService;
    @Mock private UserRepository userRepository;
    @Mock private PdfFormService pdfFormService;
    @Mock private AiVerificationService aiVerificationService;
    @Mock private PdfExtractorService pdfExtractorService;

    @InjectMocks
    private DossierController dossierController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dossierController).build();
    }

    private Authentication mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    private User buildUser(Long id, String email, String cin) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setCin(cin);
        return user;
    }

    private Dossier buildDossier(Long id) {
        Dossier d = new Dossier();
        d.setId(id);
        d.setStatut("EN_ATTENTE");
        d.setFileName("formulaire.pdf");
        d.setDateUpload(LocalDateTime.now());
        return d;
    }

    // ==========================================================
    // GET /api/dossiers/formulaire
    // ==========================================================

    @Test
    void telechargerFormulaire_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", "CIN123");
        byte[] pdfBytes = "pdf-content".getBytes();

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(pdfFormService.genererFormulairePreRempli(user)).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/dossiers/formulaire").principal(auth))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfBytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename=formulaire_CIN123.pdf"));
    }

    // ==========================================================
    // POST /api/dossiers/upload
    // ==========================================================

    @Test
    void uploadDossier_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", "CIN123");

        MockMultipartFile file = new MockMultipartFile("file", "formulaire.pdf",
                "application/pdf", "contenu".getBytes());

        com.projet.cnss.dto.DossierUploadResponse response =
                com.projet.cnss.dto.DossierUploadResponse.builder()
                        .id(10L)
                        .fileName("formulaire.pdf")
                        .statut("EN_ATTENTE")
                        .build();

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(dossierService.uploadDossier(any(), eq("jean@test.com"), eq(user), eq(true), eq(95)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/dossiers/upload")
                        .file(file)
                        .param("aiScore", "95")
                        .param("aiValide", "true")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    void uploadDossier_defaultParams_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", "CIN123");

        MockMultipartFile file = new MockMultipartFile("file", "formulaire.pdf",
                "application/pdf", "contenu".getBytes());

        com.projet.cnss.dto.DossierUploadResponse response =
                com.projet.cnss.dto.DossierUploadResponse.builder().id(11L).build();

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(dossierService.uploadDossier(any(), eq("jean@test.com"), eq(user), eq(false), eq(0)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/dossiers/upload").file(file).principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    // ==========================================================
    // POST /api/dossiers/pre-verifier
    // ==========================================================

    @Test
    void preVerifier_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "formulaire.pdf",
                "application/pdf", "contenu".getBytes());

        AiVerificationResult result = AiVerificationResult.builder()
                .valide(true)
                .score(90)
                .resume("OK")
                .champsManquants(List.of())
                .champsInvalides(List.of())
                .build();

        when(pdfExtractorService.extraireTexte(any())).thenReturn("texte extrait");
        when(aiVerificationService.verifierContenuPdf("texte extrait")).thenReturn(result);
        when(aiVerificationService.getBadge(90)).thenReturn("EXCELLENT");

        mockMvc.perform(multipart("/api/dossiers/pre-verifier").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valide").value(true))
                .andExpect(jsonPath("$.score").value(90))
                .andExpect(jsonPath("$.scoreBadge").value("EXCELLENT"))
                .andExpect(jsonPath("$.message").value("Le formulaire peut etre soumis"));
    }

    @Test
    void preVerifier_invalide_returnsCorrectionMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "formulaire.pdf",
                "application/pdf", "contenu".getBytes());

        AiVerificationResult result = AiVerificationResult.builder()
                .valide(false)
                .score(30)
                .resume("Incomplet")
                .champsManquants(List.of("nom"))
                .champsInvalides(List.of())
                .build();

        when(pdfExtractorService.extraireTexte(any())).thenReturn("texte");
        when(aiVerificationService.verifierContenuPdf("texte")).thenReturn(result);
        when(aiVerificationService.getBadge(30)).thenReturn("FAIBLE");

        mockMvc.perform(multipart("/api/dossiers/pre-verifier").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valide").value(false))
                .andExpect(jsonPath("$.message").value("Veuillez corriger les erreurs avant de soumettre"));
    }

    // ==========================================================
    // GET /api/dossiers/my-dossiers
    // ==========================================================

    @Test
    void myDossiers_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", "CIN123");

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(dossierService.getDossiersByUser(user)).thenReturn(List.of(buildDossier(1L)));

        mockMvc.perform(get("/api/dossiers/my-dossiers").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // GET /api/dossiers/all
    // ==========================================================

    @Test
    void getAllDossiers_success() throws Exception {
        when(dossierService.getAllDossiers()).thenReturn(List.of(buildDossier(1L), buildDossier(2L)));

        mockMvc.perform(get("/api/dossiers/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==========================================================
    // GET /api/dossiers/en-attente
    // ==========================================================

    @Test
    void getDossiersEnAttente_success() throws Exception {
        when(dossierService.getDossiersByStatut("EN_ATTENTE")).thenReturn(List.of(buildDossier(1L)));

        mockMvc.perform(get("/api/dossiers/en-attente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // GET /api/dossiers/valides-local
    // ==========================================================

    @Test
    void getDossiersValidesLocal_success() throws Exception {
        when(dossierService.getDossiersByStatut("VALIDATION_LOCALE")).thenReturn(List.of(buildDossier(1L)));

        mockMvc.perform(get("/api/dossiers/valides-local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // PUT /api/dossiers/{id}/valider-local
    // ==========================================================

    @Test
    void validerLocal_success() throws Exception {
        Authentication auth = mockAuth("agent@test.com");
        Dossier dossier = buildDossier(1L);
        dossier.setStatut("VALIDATION_LOCALE");

        when(dossierService.changerStatut(1L, "VALIDATION_LOCALE", null, "agent@test.com"))
                .thenReturn(dossier);

        mockMvc.perform(put("/api/dossiers/1/valider-local").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDATION_LOCALE"));
    }

    // ==========================================================
    // PUT /api/dossiers/{id}/valider-global
    // ==========================================================

    @Test
    void validerGlobal_success() throws Exception {
        Authentication auth = mockAuth("agent@test.com");
        Dossier dossier = buildDossier(1L);
        dossier.setStatut("VALIDE");

        when(dossierService.changerStatut(1L, "VALIDE", null, "agent@test.com"))
                .thenReturn(dossier);

        mockMvc.perform(put("/api/dossiers/1/valider-global").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"));
    }

    // ==========================================================
    // PUT /api/dossiers/{id}/refuser
    // ==========================================================

    @Test
    void refuser_success() throws Exception {
        Authentication auth = mockAuth("agent@test.com");
        Dossier dossier = buildDossier(1L);
        dossier.setStatut("REFUSE");
        dossier.setMotifRefus("Document manquant");

        when(dossierService.changerStatut(1L, "REFUSE", "Document manquant", "agent@test.com"))
                .thenReturn(dossier);

        mockMvc.perform(put("/api/dossiers/1/refuser")
                        .param("motif", "Document manquant")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REFUSE"));
    }

    // ==========================================================
    // GET /api/dossiers/{id}/download
    // ==========================================================

    @Test
    void downloadDossier_success() throws Exception {
        byte[] content = "pdf-content".getBytes();
        when(dossierService.downloadDossier(1L)).thenReturn(content);

        mockMvc.perform(get("/api/dossiers/1/download"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content))
                .andExpect(header().string("Content-Disposition", "attachment; filename=dossier_1.pdf"));
    }

    // ==========================================================
    // DELETE /api/dossiers/{id}
    // ==========================================================

    @Test
    void deleteDossier_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        doNothing().when(dossierService).deleteDossier(1L, "jean@test.com");

        mockMvc.perform(delete("/api/dossiers/1").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Dossier supprime avec succes"));
    }

    // ==========================================================
    // GET /api/dossiers/statistics
    // ==========================================================

    @Test
    void getStatistics_success() throws Exception {
        Map<String, Object> stats = Map.of(
                "total", 10L,
                "enAttente", 3L,
                "valides", 5L,
                "refuses", 2L
        );

        when(dossierService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/dossiers/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }
}