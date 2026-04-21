package com.projet.cnss.controller;

import com.projet.cnss.dto.AiVerificationResult;
import com.projet.cnss.dto.DossierUploadResponse;
import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import com.projet.cnss.services.AiVerificationService;
import com.projet.cnss.services.DossierService;
import com.projet.cnss.services.PdfExtractorService;
import com.projet.cnss.services.PdfFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



@RestController
@RequestMapping("/api/dossiers")
@CrossOrigin(origins = "*")
public class DossierController {

    @Autowired
    private DossierService dossierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PdfFormService pdfFormService;
    @Autowired
    private AiVerificationService aiVerificationService;
    @Autowired
    private PdfExtractorService pdfExtractorService;

    // ── Télécharger le formulaire vide ────────────────────────
    @GetMapping("/formulaire")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<byte[]> telechargerFormulaire(
            Authentication authentication) throws Exception {

        // Récupérer l'utilisateur connecté
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        // Générer le PDF pré-rempli avec ses données
        byte[] pdfBytes = pdfFormService.genererFormulairePreRempli(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=formulaire_"
                                + user.getCin() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ── Upload dossier (avec vérification IA) ────────────────
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<DossierUploadResponse> uploadDossier(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        // Vérifier que c'est bien un PDF
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new RuntimeException("Seuls les fichiers PDF sont acceptes");
        }

        // Vérifier la taille (max 10 MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("Le fichier ne doit pas depasser 10 MB");
        }

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));

        DossierUploadResponse response = dossierService
                .uploadDossier(file, userEmail, user);

        return ResponseEntity.ok(response);
    }

    // ── Pré-vérification IA sans upload ──────────────────────
    @PostMapping("/pre-verifier")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> preVerifier(
            @RequestParam("file") MultipartFile file) throws Exception {

        String contenuPdf = pdfExtractorService.extraireTexte(file);
        AiVerificationResult result = aiVerificationService.verifierContenuPdf(contenuPdf);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valide",          result.isValide());
        response.put("score",           result.getScore());
        response.put("scoreBadge",      aiVerificationService.getBadge(result.getScore())); // ← ajouté
        response.put("resume",          result.getResume());
        response.put("champsManquants", result.getChampsManquants());
        response.put("champsInvalides", result.getChampsInvalides());
        response.put("message",         result.isValide()
                ? "Le formulaire peut etre soumis"
                : "Veuillez corriger les erreurs avant de soumettre");

        return ResponseEntity.ok(response);
    }
    // ── Mes dossiers (assuré) ─────────────────────────────────
    @GetMapping("/my-dossiers")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<List<Dossier>> myDossiers(
            Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));
        return ResponseEntity.ok(dossierService.getDossiersByUser(user));
    }

    // ── Tous les dossiers (Admin / Agent CNSS) ────────────────
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('AGENT_CNSS', 'ADMIN' )")
    public ResponseEntity<List<Dossier>> getAllDossiers() {
        return ResponseEntity.ok(dossierService.getAllDossiers());
    }

    // ── Dossiers EN_ATTENTE (Agent Bureau) ────────────────────
    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('AGENT_BUREAU', 'ADMIN', 'AGENT_CNSS')")
    public ResponseEntity<List<Dossier>> getDossiersEnAttente() {
        return ResponseEntity.ok(
                dossierService.getDossiersByStatut("EN_ATTENTE"));
    }

    // ── Dossiers VALIDATION_LOCALE (Agent Direction) ──────────
    @GetMapping("/valides-local")
    @PreAuthorize("hasAnyRole('AGENT_DIRECTION', 'ADMIN')")
    public ResponseEntity<List<Dossier>> getDossiersValidesLocal() {
        return ResponseEntity.ok(
                dossierService.getDossiersByStatut("VALIDATION_LOCALE"));
    }

    // ── Validation locale (Agent Bureau) ─────────────────────
    @PutMapping("/{id}/valider-local")
    @PreAuthorize("hasAnyRole('AGENT_BUREAU', 'ADMIN')")
    public ResponseEntity<Dossier> validerLocal(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                dossierService.changerStatut(id, "VALIDATION_LOCALE",
                        null, authentication.getName()));
    }

    // ── Validation globale (Agent Direction) ─────────────────
    @PutMapping("/{id}/valider-global")
    @PreAuthorize("hasAnyRole('AGENT_DIRECTION', 'ADMIN', 'AGENT_CNSS')")
    public ResponseEntity<Dossier> validerGlobal(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(
                dossierService.changerStatut(id, "VALIDE",
                        null, authentication.getName()));
    }

    // ── Refuser un dossier ────────────────────────────────────
    @PutMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('AGENT_BUREAU', 'AGENT_DIRECTION', 'ADMIN', 'AGENT_CNSS')")
    public ResponseEntity<Dossier> refuser(
            @PathVariable Long id,
            @RequestParam String motif,
            Authentication authentication) {
        return ResponseEntity.ok(
                dossierService.changerStatut(id, "REFUSE",
                        motif, authentication.getName()));
    }

    // ── Télécharger un document depuis MinIO ─────────────────
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ASSURE', 'AGENT_BUREAU', 'AGENT_DIRECTION', 'ADMIN', 'AGENT_CNSS')")
    public ResponseEntity<byte[]> downloadDossier(
            @PathVariable Long id) throws Exception {
        byte[] content = dossierService.downloadDossier(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=dossier_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    // ── Supprimer un dossier ──────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDossier(
            @PathVariable Long id,
            Authentication authentication) throws Exception {
        dossierService.deleteDossier(id, authentication.getName());
        return ResponseEntity.ok(Map.of("message",
                "Dossier supprime avec succes"));
    }

    // ── Statistiques ──────────────────────────────────────────
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('AGENT_BUREAU', 'AGENT_DIRECTION', 'ADMIN', 'AGENT_CNSS')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(dossierService.getStatistics());
    }

}