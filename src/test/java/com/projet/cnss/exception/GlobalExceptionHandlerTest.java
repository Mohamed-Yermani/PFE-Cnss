package com.projet.cnss.exception;

import com.projet.cnss.dto.AiVerificationResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ==========================================================
    // handleAiVerification
    // ==========================================================

    @Test
    void handleAiVerification_withAllSectionDetails_buildsCompleteResponse() {
        AiVerificationResult.SectionDetail ok = AiVerificationResult.SectionDetail.builder()
                .statut("OK").commentaire("Validé").build();

        AiVerificationResult verification = AiVerificationResult.builder()
                .valide(false)
                .score(85)
                .resume("Bon dossier")
                .champsManquants(List.of("telephone"))
                .champsInvalides(List.of("adresse"))
                .detailIdentite(ok)
                .detailEmployeur(ok)
                .detailTypeDossier(ok)
                .detailPeriode(ok)
                .detailSignature(ok)
                .detailCoherence(ok)
                .build();

        AiVerificationException ex = new AiVerificationException("Formulaire invalide", verification);

        ResponseEntity<Map<String, Object>> response = handler.handleAiVerification(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("FORMULAIRE_INVALIDE", body.get("erreur"));
        assertEquals("Formulaire invalide", body.get("message"));

        @SuppressWarnings("unchecked")
        Map<String, Object> aiVerification = (Map<String, Object>) body.get("aiVerification");
        assertEquals(false, aiVerification.get("valide"));
        assertEquals(85, aiVerification.get("score"));
        assertEquals("BON", aiVerification.get("scoreBadge"));
        assertEquals("Bon dossier", aiVerification.get("resume"));

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) aiVerification.get("details");
        assertEquals(6, details.size());
        assertTrue(details.containsKey("identiteAssure"));
        assertTrue(details.containsKey("informationsEmployeur"));
        assertTrue(details.containsKey("typeDossier"));
        assertTrue(details.containsKey("periode"));
        assertTrue(details.containsKey("signature"));
        assertTrue(details.containsKey("coherenceGlobale"));
    }

    @Test
    void handleAiVerification_withNoSectionDetails_buildsEmptyDetailsMap() {
        AiVerificationResult verification = AiVerificationResult.builder()
                .valide(false)
                .score(30)
                .resume("Incomplet")
                .champsManquants(null)
                .champsInvalides(null)
                .build();

        AiVerificationException ex = new AiVerificationException("Incomplet", verification);

        ResponseEntity<Map<String, Object>> response = handler.handleAiVerification(ex);

        Map<String, Object> body = response.getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> aiVerification = (Map<String, Object>) body.get("aiVerification");

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) aiVerification.get("details");
        assertTrue(details.isEmpty());

        // champsManquants/champsInvalides null doivent devenir des listes vides, pas null
        assertEquals(List.of(), aiVerification.get("champsManquants"));
        assertEquals(List.of(), aiVerification.get("champsInvalides"));
    }

    @Test
    void handleAiVerification_withPartialSectionDetails_includesOnlyNonNullSections() {
        AiVerificationResult.SectionDetail ok = AiVerificationResult.SectionDetail.builder()
                .statut("OK").commentaire("Validé").build();

        AiVerificationResult verification = AiVerificationResult.builder()
                .valide(false)
                .score(60)
                .resume("Partiel")
                .champsManquants(List.of())
                .champsInvalides(List.of())
                .detailIdentite(ok)
                .detailSignature(ok)
                // les autres détails restent null
                .build();

        AiVerificationException ex = new AiVerificationException("Partiel", verification);

        ResponseEntity<Map<String, Object>> response = handler.handleAiVerification(ex);

        @SuppressWarnings("unchecked")
        Map<String, Object> aiVerification = (Map<String, Object>) response.getBody().get("aiVerification");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) aiVerification.get("details");

        assertEquals(2, details.size());
        assertTrue(details.containsKey("identiteAssure"));
        assertTrue(details.containsKey("signature"));
        assertFalse(details.containsKey("informationsEmployeur"));
        assertFalse(details.containsKey("typeDossier"));
        assertFalse(details.containsKey("periode"));
        assertFalse(details.containsKey("coherenceGlobale"));
    }

    @Test
    void handleAiVerification_scoreBadges_allBranchesCovered() {
        AiVerificationResult excellent = AiVerificationResult.builder()
                .score(95).resume("Excellent dossier")
                .champsManquants(List.of()).champsInvalides(List.of()).build();
        AiVerificationResult bon = AiVerificationResult.builder()
                .score(75).resume("Bon dossier")
                .champsManquants(List.of()).champsInvalides(List.of()).build();
        AiVerificationResult moyen = AiVerificationResult.builder()
                .score(55).resume("Dossier moyen")
                .champsManquants(List.of()).champsInvalides(List.of()).build();
        AiVerificationResult faible = AiVerificationResult.builder()
                .score(20).resume("Dossier faible")
                .champsManquants(List.of()).champsInvalides(List.of()).build();

        assertEquals("EXCELLENT", extractBadge(handler.handleAiVerification(
                new AiVerificationException("x", excellent))));
        assertEquals("BON", extractBadge(handler.handleAiVerification(
                new AiVerificationException("x", bon))));
        assertEquals("MOYEN", extractBadge(handler.handleAiVerification(
                new AiVerificationException("x", moyen))));
        assertEquals("FAIBLE", extractBadge(handler.handleAiVerification(
                new AiVerificationException("x", faible))));
    }
    @SuppressWarnings("unchecked")
    private String extractBadge(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> aiVerification = (Map<String, Object>) response.getBody().get("aiVerification");
        return (String) aiVerification.get("scoreBadge");
    }

    // ==========================================================
    // handleMaxSize
    // ==========================================================

    @Test
    void handleMaxSize_returnsPayloadTooLarge() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10_000_000L);

        ResponseEntity<Map<String, Object>> response = handler.handleMaxSize(ex);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(413, body.get("status"));
        assertEquals("FICHIER_TROP_GRAND", body.get("erreur"));
        assertNotNull(body.get("message"));
        assertNotNull(body.get("timestamp"));
    }

    // ==========================================================
    // handleAccessDenied
    // ==========================================================

    @Test
    void handleAccessDenied_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Accès interdit");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(403, body.get("status"));
        assertEquals("ACCES_REFUSE", body.get("erreur"));
        assertEquals("Vous n'avez pas les droits pour effectuer cette action", body.get("message"));
    }

    // ==========================================================
    // handleRuntime
    // ==========================================================

    @Test
    void handleRuntime_withMessage_returnsBadRequestWithMessage() {
        RuntimeException ex = new RuntimeException("Utilisateur non trouvé");

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(400, body.get("status"));
        assertEquals("ERREUR", body.get("erreur"));
        assertEquals("Utilisateur non trouvé", body.get("message"));
    }

    @Test
    void handleRuntime_withNullMessage_usesClassSimpleName() {
        RuntimeException ex = new RuntimeException();

        ResponseEntity<Map<String, Object>> response = handler.handleRuntime(ex);

        Map<String, Object> body = response.getBody();
        assertEquals("RuntimeException", body.get("message"));
    }

    // ==========================================================
    // handleGeneral
    // ==========================================================

    @Test
    void handleGeneral_returnsInternalServerError() {
        Exception ex = new Exception("Erreur inattendue quelconque");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertEquals(500, body.get("status"));
        assertEquals("ERREUR_SERVEUR", body.get("erreur"));
        assertEquals("Une erreur interne est survenue. Veuillez reessayer.", body.get("message"));
    }
}