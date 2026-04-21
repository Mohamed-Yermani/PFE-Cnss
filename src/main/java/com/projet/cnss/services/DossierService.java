package com.projet.cnss.services;

import com.projet.cnss.dto.AiVerificationResult;
import com.projet.cnss.dto.DossierUploadResponse;
import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;

import com.projet.cnss.exception.AiVerificationException;
import com.projet.cnss.repository.DossierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DossierService {

    @Autowired private DossierRepository      dossierRepository;
    @Autowired private MinioService           minioService;
    @Autowired private AiVerificationService  aiVerificationService;
    @Autowired private PdfExtractorService    pdfExtractorService;

    // ── Upload avec vérification IA ───────────────────────────
    public DossierUploadResponse uploadDossier(MultipartFile file,
                                               String userEmail,
                                               User user) throws Exception {
        // Étape 1 : Extraction texte PDF
        String contenuPdf = pdfExtractorService.extraireTexte(file);

        // Étape 2 : Vérification IA
        AiVerificationResult verification = aiVerificationService
                .verifierContenuPdf(contenuPdf);

        // Étape 3 : Refus si invalide
        if (!verification.isValide()) {
            throw new AiVerificationException(
                    "Le formulaire PDF est incomplet ou invalide", verification);
        }

        // Étape 4 : Upload MinIO
        String objectName = minioService.uploadDocument(userEmail, file);

        // Étape 5 : Sauvegarde BDD
        Dossier dossier = new Dossier();
        dossier.setFileName(file.getOriginalFilename());
        dossier.setAlfrescoNodeId(objectName);
        dossier.setStatut("EN_ATTENTE");
        dossier.setUser(user);
        Dossier saved = dossierRepository.save(dossier);

        return buildResponse(saved, verification);
    }

    // ── Changer statut ────────────────────────────────────────
    public Dossier changerStatut(Long id, String statut,
                                 String motif, String agentEmail) {
        Dossier dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dossier non trouve"));

        dossier.setStatut(statut);
        dossier.setAgentEmail(agentEmail);
        dossier.setDateTraitement(LocalDateTime.now());
        if (motif != null) dossier.setMotifRefus(motif);

        return dossierRepository.save(dossier);
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
}