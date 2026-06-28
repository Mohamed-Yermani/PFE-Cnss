package com.projet.cnss.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.cnss.dto.AiVerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiVerificationServiceTest {

    @Mock private WebClient.Builder builder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;
    @Mock private Mono<String> monoString;

    private AiVerificationService aiVerificationService;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(webClient);
        aiVerificationService = new AiVerificationService(builder);

        ReflectionTestUtils.setField(aiVerificationService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(aiVerificationService, "apiUrl", "https://api.groq.test/v1/chat/completions");
    }

    /**
     * Construit la réponse "Groq" simulée :
     * {"choices":[{"message":{"content": "<innerJsonContent>"}}]}
     */
    private String buildGroqResponse(String innerContent) throws Exception {
        Map<String, Object> message = Map.of("content", innerContent);
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> root = Map.of("choices", List.of(choice));
        return mapper.writeValueAsString(root);
    }

    @SuppressWarnings("unchecked")
    private void wireChainUpToRetrieve() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(String.class))).thenReturn(monoString);
        when(monoString.timeout(any(Duration.class))).thenReturn(monoString);
    }

    private void stubWebClientToReturn(String response) {
        wireChainUpToRetrieve();
        when(monoString.block()).thenReturn(response);
    }

    private void stubWebClientToThrow(RuntimeException exception) {
        wireChainUpToRetrieve();
        when(monoString.block()).thenThrow(exception);
    }

    // ==========================================================
    // verifierContenuPdf
    // ==========================================================

    @Test
    void verifierContenuPdf_success_fullValidJson_parsesAllFields() throws Exception {
        String innerContent = """
                {
                  "valide": true,
                  "score": 85,
                  "resume": "Bon dossier",
                  "champsManquants": ["telephone"],
                  "champsInvalides": ["adresse"],
                  "details": {
                    "identiteAssure": {"statut":"OK","commentaire":"ok1"},
                    "informationsEmployeur": {"statut":"OK","commentaire":"ok2"},
                    "typeDossier": {"statut":"OK","commentaire":"ok3"},
                    "periode": {"statut":"OK","commentaire":"ok4"},
                    "signature": {"statut":"OK","commentaire":"ok5"},
                    "coherenceGlobale": {"statut":"OK","commentaire":"ok6"}
                  }
                }
                """;
        stubWebClientToReturn(buildGroqResponse(innerContent));

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf test");

        assertTrue(result.isValide());
        assertEquals(85, result.getScore());
        assertEquals("Bon dossier", result.getResume());
        assertEquals(List.of("telephone"), result.getChampsManquants());
        assertEquals(List.of("adresse"), result.getChampsInvalides());
        assertEquals("OK", result.getDetailIdentite().getStatut());
        assertEquals("ok1", result.getDetailIdentite().getCommentaire());
        assertEquals("ok6", result.getDetailCoherence().getCommentaire());
    }

    @Test
    void verifierContenuPdf_contentWrappedInMarkdownFences_stripsAndParses() throws Exception {
        String innerContent = "```json\n"
                + "{\"valide\": false, \"score\": 30, \"resume\": \"Incomplet\", "
                + "\"champsManquants\": [], \"champsInvalides\": [], \"details\": {}}"
                + "\n```";
        stubWebClientToReturn(buildGroqResponse(innerContent));

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf");

        assertFalse(result.isValide());
        assertEquals(30, result.getScore());
        assertEquals("Incomplet", result.getResume());
    }

    @Test
    void verifierContenuPdf_contentWithLeadingTrailingTextAroundJson_extractsJsonOnly() throws Exception {
        String innerContent = "Voici le resultat :\n"
                + "{\"valide\": true, \"score\": 60, \"resume\": \"Moyen\", "
                + "\"champsManquants\": [], \"champsInvalides\": [], \"details\": {}}"
                + "\nFin de l'analyse.";
        stubWebClientToReturn(buildGroqResponse(innerContent));

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf");

        assertTrue(result.isValide());
        assertEquals(60, result.getScore());
        assertEquals("Moyen", result.getResume());
    }

    @Test
    void verifierContenuPdf_jsonMissingFields_usesDefaultValues() throws Exception {
        String innerContent = "{}";
        stubWebClientToReturn(buildGroqResponse(innerContent));

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf");

        assertTrue(result.isValide());
        assertEquals(80, result.getScore());
        assertEquals("Verification effectuee", result.getResume());
        assertTrue(result.getChampsManquants().isEmpty());
        assertTrue(result.getChampsInvalides().isEmpty());
        assertEquals("OK", result.getDetailIdentite().getStatut());
        assertEquals("", result.getDetailIdentite().getCommentaire());
    }

    @Test
    void verifierContenuPdf_unparsableContent_fallsBackInParseResponseCatch() throws Exception {
        String innerContent = "ceci n'est pas du json valide { [ } ]";
        stubWebClientToReturn(buildGroqResponse(innerContent));

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf");

        assertTrue(result.isValide());
        assertEquals(75, result.getScore());
        assertEquals("Analyse effectuee avec avertissement", result.getResume());
        assertTrue(result.getChampsManquants().isEmpty());
        assertTrue(result.getChampsInvalides().isEmpty());
    }

    @Test
    void verifierContenuPdf_webClientThrowsException_fallsBackInOuterCatch() {
        stubWebClientToThrow(new RuntimeException("Erreur Groq 4xx"));

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf");

        assertTrue(result.isValide());
        assertEquals(100, result.getScore());
        assertEquals("Verification indisponible - dossier accepte automatiquement", result.getResume());
        assertTrue(result.getChampsManquants().isEmpty());
        assertTrue(result.getChampsInvalides().isEmpty());
    }

    @Test
    void verifierContenuPdf_malformedOuterResponse_fallsBackInOuterCatch() {
        // Réponse brute non-JSON => readTree échoue dans le bloc try principal
        stubWebClientToReturn("ceci n'est pas du JSON du tout");

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf");

        assertTrue(result.isValide());
        assertEquals(100, result.getScore());
        assertEquals("Verification indisponible - dossier accepte automatiquement", result.getResume());
    }

    @Test
    void verifierContenuPdf_apiKeyBlank_stillProcessesNormally() throws Exception {
        ReflectionTestUtils.setField(aiVerificationService, "apiKey", "");

        String innerContent = "{\"valide\": true, \"score\": 90, \"resume\": \"OK\", "
                + "\"champsManquants\": [], \"champsInvalides\": [], \"details\": {}}";
        stubWebClientToReturn(buildGroqResponse(innerContent));

        AiVerificationResult result = aiVerificationService.verifierContenuPdf("contenu pdf");

        assertTrue(result.isValide());
        assertEquals(90, result.getScore());
    }

    // ==========================================================
    // getBadge
    // ==========================================================

    @Test
    void getBadge_scoreAtLeast90_returnsExcellent() {
        assertEquals("EXCELLENT", aiVerificationService.getBadge(90));
        assertEquals("EXCELLENT", aiVerificationService.getBadge(100));
    }

    @Test
    void getBadge_scoreBetween70And89_returnsBon() {
        assertEquals("BON", aiVerificationService.getBadge(70));
        assertEquals("BON", aiVerificationService.getBadge(89));
    }

    @Test
    void getBadge_scoreBetween50And69_returnsMoyen() {
        assertEquals("MOYEN", aiVerificationService.getBadge(50));
        assertEquals("MOYEN", aiVerificationService.getBadge(69));
    }

    @Test
    void getBadge_scoreBelow50_returnsFaible() {
        assertEquals("FAIBLE", aiVerificationService.getBadge(49));
        assertEquals("FAIBLE", aiVerificationService.getBadge(0));
    }
}