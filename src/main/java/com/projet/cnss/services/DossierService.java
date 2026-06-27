package com.projet.cnss.services;

import com.projet.cnss.dto.AiVerificationResult;
import com.projet.cnss.dto.DossierUploadResponse;
import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;

import com.projet.cnss.exception.AiVerificationException;
import com.projet.cnss.repository.DossierRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DossierService {

    @Autowired private DossierRepository      dossierRepository;
    @Autowired private MinioService           minioService;
    @Autowired private AiVerificationService  aiVerificationService;
    @Autowired private PdfExtractorService    pdfExtractorService;

    public DossierUploadResponse uploadDossier(MultipartFile file,
                                               String userEmail,
                                               User user,
                                               boolean aiValide,
                                               int aiScore) throws Exception {

        log.info("uploadDossier appelé - aiValide={}, aiScore={}", aiValide, aiScore);

        AiVerificationResult verification;
        String contenuPdf = "";

        if (aiValide) {
            log.info("Validation IA frontend acceptée - score: {}", aiScore);

            // Extraire quand même le texte pour récupérer le typeAvantage
            contenuPdf = pdfExtractorService.extraireTexte(file);

            verification = AiVerificationResult.builder()
                    .valide(true)
                    .score(aiScore > 0 ? aiScore : 100)
                    .resume("Formulaire validé par l'agent IA")
                    .champsManquants(List.of())
                    .champsInvalides(List.of())
                    .detailIdentite(ok())
                    .detailEmployeur(ok())
                    .detailTypeDossier(ok())
                    .detailPeriode(ok())
                    .detailSignature(ok())
                    .detailCoherence(ok())
                    .build();

        } else {
            log.info("Verification IA backend...");
            contenuPdf = pdfExtractorService.extraireTexte(file);
            verification = aiVerificationService.verifierContenuPdf(contenuPdf);

            if (!verification.isValide()) {
                throw new AiVerificationException(
                        "Le formulaire PDF est incomplet ou invalide", verification);
            }
        }

        // Upload MinIO
        String objectName = minioService.uploadDocument(userEmail, file);

        // Sauvegarde BDD
        Dossier dossier = new Dossier();
        dossier.setFileName(file.getOriginalFilename());
        dossier.setAlfrescoNodeId(objectName);
        dossier.setStatut("EN_ATTENTE");
        dossier.setUser(user);

        // ✅ Extraire et sauvegarder le typeAvantage depuis le PDF
        dossier.setTypeAvantage(extraireTypeAvantage(contenuPdf));

        // ✅ Persister le score IA pour qu'il compte dans les statistiques
        dossier.setAiScore(verification.getScore());

        Dossier saved = dossierRepository.save(dossier);

        // ✅ Notifier les agents
        notificationService.notifierNouveauDossier(saved);

        return buildResponse(saved, verification);
    }

    // ── Helper section OK ─────────────────────────────────────────
    private AiVerificationResult.SectionDetail ok() {
        return AiVerificationResult.SectionDetail.builder()
                .statut("OK")
                .commentaire("Validé")
                .build();
    }

    // ── Extraire le type d'avantage depuis le texte PDF ───────────
    private String extraireTypeAvantage(String contenuPdf) {

        if (contenuPdf == null || contenuPdf.isBlank()) {
            return "Avantage CNSS";
        }

        String pdf = contenuPdf
                .replace("\r", "")
                .toUpperCase();

        String[] lignes = pdf.split("\n");

        for (String ligne : lignes) {

            boolean coche =
                    ligne.contains("[X]") ||
                            ligne.contains("[ X ]") ||
                            ligne.contains("☒") ||
                            ligne.contains("☑");

            if (!coche) {
                continue;
            }

            System.out.println("LIGNE COCHEE = " + ligne);

            if (ligne.contains("MALADIE PROFESSIONNELLE")) {
                return "Maladie Professionnelle";
            }

            if (ligne.contains("ACCIDENT DE TRAVAIL")) {
                return "Accident de Travail";
            }

            if (ligne.contains("INDEMNIT")) {
                return "Indemnités Journalières";
            }

            if (ligne.contains("INVALID")) {
                return "Invalidité";
            }

            if (ligne.contains("DÉCÈS") || ligne.contains("DECES")) {
                return "Décès";
            }

            if (ligne.contains("RÉGIME GÉNÉRAL") || ligne.contains("REGIME GENERAL")) {
                return "Régime Général";
            }

            if (ligne.contains("RETRAITE") || ligne.contains("VIEILLESSE")) {
                return "Retraite";
            }
        }

        return "Avantage CNSS";
    }

    // ── Télécharger depuis MinIO ──────────────────────────────
    public byte[] downloadDossier(Long id) throws Exception {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouve"));
        return minioService.downloadDocument(dossier.getAlfrescoNodeId());
    }

    // ── Supprimer ─────────────────────────────────────────────
    public void deleteDossier(Long id, String userEmail) throws Exception {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouve"));
        minioService.deleteDocument(dossier.getAlfrescoNodeId());
        dossierRepository.delete(dossier);
    }

    // ── Requêtes ──────────────────────────────────────────────
    public List<Dossier> getAllDossiers() {
        return dossierRepository.findAll();
    }

    public List<Dossier> getDossiersByUser(User user) {
        return dossierRepository.findByUser(user);
    }

    public List<Dossier> getDossiersByStatut(String statut) {
        return dossierRepository.findByStatut(statut);
    }

    // ── Statistiques ──────────────────────────────────────────
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total",            dossierRepository.count());
        stats.put("enAttente",        dossierRepository.countByStatut("EN_ATTENTE"));
        stats.put("validationLocale", dossierRepository.countByStatut("VALIDATION_LOCALE"));
        stats.put("valides",          dossierRepository.countByStatut("VALIDE"));
        stats.put("refuses",          dossierRepository.countByStatut("REFUSE"));

        // ✅ Score IA moyen — uniquement sur les dossiers qui ont un score (les anciens sont ignorés, ai_score = null)
        double avgScore = dossierRepository.findAll().stream()
                .filter(d -> d.getAiScore() != null)
                .mapToInt(Dossier::getAiScore)
                .average()
                .orElse(0.0);

        stats.put("avgAiScore", Math.round(avgScore));

        return stats;
    }

    // ── Build response ────────────────────────────────────────
    private DossierUploadResponse buildResponse(Dossier dossier,
                                                AiVerificationResult v) {
        Map<String, DossierUploadResponse.SectionInfo> detailsMap = new LinkedHashMap<>();

        if (v.getDetailIdentite() != null)
            detailsMap.put("identiteAssure",
                    toSectionInfo(v.getDetailIdentite()));
        if (v.getDetailEmployeur() != null)
            detailsMap.put("informationsEmployeur",
                    toSectionInfo(v.getDetailEmployeur()));
        if (v.getDetailTypeDossier() != null)
            detailsMap.put("typeDossier",
                    toSectionInfo(v.getDetailTypeDossier()));
        if (v.getDetailPeriode() != null)
            detailsMap.put("periode",
                    toSectionInfo(v.getDetailPeriode()));
        if (v.getDetailSignature() != null)
            detailsMap.put("signature",
                    toSectionInfo(v.getDetailSignature()));
        if (v.getDetailCoherence() != null)
            detailsMap.put("coherenceGlobale",
                    toSectionInfo(v.getDetailCoherence()));

        DossierUploadResponse.AiVerificationDetails aiDetails =
                DossierUploadResponse.AiVerificationDetails.builder()
                        .valide(v.isValide())
                        .score(v.getScore())
                        .scoreBadge(aiVerificationService.getBadge(v.getScore()))
                        .resume(v.getResume())
                        .champsManquants(v.getChampsManquants())
                        .champsInvalides(v.getChampsInvalides())
                        .details(detailsMap)
                        .build();

        return DossierUploadResponse.builder()
                .id(dossier.getId())
                .fileName(dossier.getFileName())
                .statut(dossier.getStatut())
                .dateUpload(dossier.getDateUpload())
                .aiVerification(aiDetails)
                .build();
    }

    private DossierUploadResponse.SectionInfo toSectionInfo(
            AiVerificationResult.SectionDetail d) {
        return DossierUploadResponse.SectionInfo.builder()
                .statut(d.getStatut())
                .commentaire(d.getCommentaire())
                .build();
    }

    // Injecter NotificationService
    @Autowired
    private NotificationService notificationService;

    // Modifier changerStatut()
    public Dossier changerStatut(Long id, String statut,
                                 String motif, String agentEmail) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouvé"));

        dossier.setStatut(statut);
        dossier.setAgentEmail(agentEmail);
        dossier.setDateTraitement(LocalDateTime.now());
        if (motif != null) dossier.setMotifRefus(motif);

        Dossier saved = dossierRepository.save(dossier);

        // ✅ Notification automatique à l'assuré
        notificationService.notifierStatutDossier(saved, statut);

        // ✅ Notification agent CNSS + direction selon le statut
        switch (statut) {
            case "VALIDATION_LOCALE" ->
                    notificationService.notifierValidationGlobale(saved, statut);

            case "VALIDE", "REFUSE" ->
                    notificationService.notifierValidationGlobale(saved, statut);
        }

        return saved;
    }

}