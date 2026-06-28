package com.projet.cnss.exception;



import com.projet.cnss.dto.AiVerificationResult;
import com.projet.cnss.dto.DossierUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Refus IA ──────────────────────────────────────────────
    @ExceptionHandler(AiVerificationException.class)
    public ResponseEntity<Map<String, Object>> handleAiVerification(
            AiVerificationException ex) {

        AiVerificationResult v = ex.getVerification();

        // Construire détails sections
        Map<String, Object> details = new LinkedHashMap<>();
        if (v.getDetailIdentite() != null)
            details.put("identiteAssure", sectionMap(v.getDetailIdentite()));
        if (v.getDetailEmployeur() != null)
            details.put("informationsEmployeur", sectionMap(v.getDetailEmployeur()));
        if (v.getDetailTypeDossier() != null)
            details.put("typeDossier", sectionMap(v.getDetailTypeDossier()));
        if (v.getDetailPeriode() != null)
            details.put("periode", sectionMap(v.getDetailPeriode()));
        if (v.getDetailSignature() != null)
            details.put("signature", sectionMap(v.getDetailSignature()));
        if (v.getDetailCoherence() != null)
            details.put("coherenceGlobale", sectionMap(v.getDetailCoherence()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", 400);
        response.put("erreur", "FORMULAIRE_INVALIDE");
        response.put("message", ex.getMessage());
        response.put("aiVerification", Map.of(
                "valide",          false,
                "score",           v.getScore(),
                "scoreBadge",      getBadge(v.getScore()),
                "resume",          v.getResume(),
                "champsManquants", v.getChampsManquants() != null ? v.getChampsManquants() : List.of(),
                "champsInvalides", v.getChampsInvalides() != null ? v.getChampsInvalides() : List.of(),
                "details",         details
        ));

        return ResponseEntity.badRequest().body(response);
    }

    // ── Fichier trop grand ────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSize(
            MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(errorMap(413, "FICHIER_TROP_GRAND",
                        "Le fichier depasse la taille maximale autorisee (10 MB)"));
    }

    // ── Accès refusé ──────────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorMap(403, "ACCES_REFUSE",
                        "Vous n'avez pas les droits pour effectuer cette action"));
    }

    // ── RuntimeException générique ────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Erreur runtime: ", ex); // 👈 log la stack trace complète côté serveur
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorMap(400, "ERREUR", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
    }

    // ── Erreur serveur ────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMap(500, "ERREUR_SERVEUR",
                        "Une erreur interne est survenue. Veuillez reessayer."));
    }

    // ── Helpers ───────────────────────────────────────────────
    private Map<String, Object> errorMap(int status, String erreur, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", LocalDateTime.now().toString());
        map.put("status", status);
        map.put("erreur", erreur);
        map.put("message", message);
        return map;
    }

    private Map<String, String> sectionMap(AiVerificationResult.SectionDetail detail) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("statut", detail.getStatut());
        map.put("commentaire", detail.getCommentaire());
        return map;
    }

    private String getBadge(int score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 70) return "BON";
        if (score >= 50) return "MOYEN";
        return "FAIBLE";
    }


}
