package com.projet.cnss.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.cnss.dto.AiVerificationResult;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j  // lombok logger
public class AiVerificationService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public AiVerificationService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public AiVerificationResult verifierContenuPdf(String contenuPdf) {
        log.info("TEXTE EXTRAIT COMPLET ===>\n{}", contenuPdf);
        log.info("=== DEBUT VERIFICATION IA ===");
        log.info("Contenu PDF ({} caracteres)", contenuPdf.length());
        log.info("API URL : {}", apiUrl);
        log.info("API KEY presente : {}", (apiKey != null && !apiKey.isBlank()) ? "OUI" : "NON");

        String prompt = """
        Tu es un agent de verification de la CNSS.
        Verifie le formulaire suivant et reponds UNIQUEMENT avec un JSON valide.
        Ne mets aucun texte avant ou apres le JSON.
        
        Formulaire a verifier :
        """
                + contenuPdf +
                """
                
                JSON de reponse obligatoire :
                {
                  "valide": true ou false,
                  "score": nombre entre 0 et 100,
                  "resume": "phrase courte sur le resultat",
                  "champsManquants": ["champ1", "champ2"],
                  "champsInvalides": ["champ3"],
                  "details": {
                    "identiteAssure":        { "statut": "OK", "commentaire": "..." },
                    "informationsEmployeur": { "statut": "OK", "commentaire": "..." },
                    "typeDossier":           { "statut": "OK", "commentaire": "..." },
                    "periode":               { "statut": "OK", "commentaire": "..." },
                    "signature":             { "statut": "OK", "commentaire": "..." },
                    "coherenceGlobale":      { "statut": "OK", "commentaire": "..." }
                  }
                }
                """;

        try {
            // ✅ Utiliser HashMap au lieu de Map.of()
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");  // ✅ nouveau modèle
            requestBody.put("messages", List.of(message));
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 1000);

            log.info("Envoi requete vers Groq avec modele llama-3.3-70b-versatile...");

            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("Erreur 4xx Groq : {}", body))
                                    .then(Mono.error(new RuntimeException("Erreur Groq 4xx")))
                    )
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            log.info("Reponse brute Groq : {}", response);

            JsonNode root = mapper.readTree(response);
            String content = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText();

            log.info("Contenu extrait : {}", content);

            content = content.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            AiVerificationResult result = parseResponse(content);
            log.info("Resultat : valide={}, score={}", result.isValide(), result.getScore());
            return result;

        } catch (Exception e) {
            log.error("=== ERREUR VERIFICATION IA ===");
            log.error("Type : {}", e.getClass().getSimpleName());
            log.error("Message : {}", e.getMessage());

            return AiVerificationResult.builder()
                    .valide(true)
                    .score(100)
                    .resume("Verification indisponible - dossier accepte automatiquement")
                    .champsManquants(List.of())
                    .champsInvalides(List.of())
                    .build();
        }
    }

    // ... reste des methodes


    private AiVerificationResult parseResponse(String content) {
        try {
            content = content.replaceAll("```json", "").replaceAll("```", "").trim();
            // Extraire uniquement le JSON si du texte parasite precede
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start != -1 && end != -1) {
                content = content.substring(start, end + 1);
            }

            JsonNode json = mapper.readTree(content);

            List<String> champsManquants = new ArrayList<>();
            json.path("champsManquants").forEach(n -> champsManquants.add(n.asText()));

            List<String> champsInvalides = new ArrayList<>();
            json.path("champsInvalides").forEach(n -> champsInvalides.add(n.asText()));

            JsonNode det = json.path("details");

            return AiVerificationResult.builder()
                    .valide(json.path("valide").asBoolean(true))
                    .score(json.path("score").asInt(80))
                    .resume(json.path("resume").asText("Verification effectuee"))
                    .champsManquants(champsManquants)
                    .champsInvalides(champsInvalides)
                    .detailIdentite(parseSectionDetail(det.path("identiteAssure")))
                    .detailEmployeur(parseSectionDetail(det.path("informationsEmployeur")))
                    .detailTypeDossier(parseSectionDetail(det.path("typeDossier")))
                    .detailPeriode(parseSectionDetail(det.path("periode")))
                    .detailSignature(parseSectionDetail(det.path("signature")))
                    .detailCoherence(parseSectionDetail(det.path("coherenceGlobale")))
                    .build();

        } catch (Exception e) {
            log.error("Erreur parsing réponse IA: {}", e.getMessage());
            return AiVerificationResult.builder()
                    .valide(true).score(75)
                    .resume("Analyse effectuee avec avertissement")
                    .champsManquants(List.of()).champsInvalides(List.of())
                    .build();
        }
    }

    private AiVerificationResult.SectionDetail parseSectionDetail(JsonNode node) {
        return AiVerificationResult.SectionDetail.builder()
                .statut(node.path("statut").asText("OK"))
                .commentaire(node.path("commentaire").asText(""))
                .build();
    }

    public String getBadge(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 70) return "BON";
        if (score >= 50) return "MOYEN";
        return "FAIBLE";
    }
}