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

    @GetMapping("/formulaire")
    @PreAuthorize("hasAnyRole('ROLE_ASSURE', 'ROLE_ADMIN')")
    public ResponseEntity<byte[]> telechargerFormulaire(
            Authentication authentication) throws Exception {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));
        byte[] pdfBytes = pdfFormService.genererFormulairePreRempli(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=formulaire_" + user.getCin() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ROLE_ASSURE', 'ROLE_ADMIN')")
    public ResponseEntity<DossierUploadResponse> uploadDossier(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "aiScore", defaultValue = "0") int aiScore,
            @RequestParam(value = "aiValide", defaultValue = "false") boolean aiValide,
            Authentication authentication) throws Exception {

        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        DossierUploadResponse response = dossierService
                .uploadDossier(file, userEmail, user, aiValide, aiScore);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/pre-verifier")
    @PreAuthorize("hasAnyRole('ROLE_ASSURE', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> preVerifier(
            @RequestParam("file") MultipartFile file) throws Exception {
        String contenuPdf = pdfExtractorService.extraireTexte(file);
        AiVerificationResult result = aiVerificationService.verifierContenuPdf(contenuPdf);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valide", result.isValide());
        response.put("score", result.getScore());
        response.put("scoreBadge", aiVerificationService.getBadge(result.getScore()));
        response.put("resume", result.getResume());
        response.put("champsManquants", result.getChampsManquants());
        response.put("champsInvalides", result.getChampsInvalides());
        response.put("message", result.isValide() ? "Le formulaire peut etre soumis" : "Veuillez corriger les erreurs avant de soumettre");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-dossiers")
    @PreAuthorize("hasAnyRole('ROLE_ASSURE', 'ROLE_ADMIN')")
    public ResponseEntity<List<Dossier>> myDossiers(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouve"));
        return ResponseEntity.ok(dossierService.getDossiersByUser(user));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_CNSS', 'ROLE_ADMIN' )")
    public ResponseEntity<List<Dossier>> getAllDossiers() {
        return ResponseEntity.ok(dossierService.getAllDossiers());
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_BUREAU', 'ROLE_ADMIN', 'ROLE_AGENT_CNSS')")
    public ResponseEntity<List<Dossier>> getDossiersEnAttente() {
        return ResponseEntity.ok(dossierService.getDossiersByStatut("EN_ATTENTE"));
    }

    @GetMapping("/valides-local")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_DIRECTION', 'ROLE_ADMIN')")
    public ResponseEntity<List<Dossier>> getDossiersValidesLocal() {
        return ResponseEntity.ok(dossierService.getDossiersByStatut("VALIDATION_LOCALE"));
    }

    @PutMapping("/{id}/valider-local")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_BUREAU', 'ROLE_ADMIN')")
    public ResponseEntity<Dossier> validerLocal(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(dossierService.changerStatut(id, "VALIDATION_LOCALE", null, authentication.getName()));
    }

    @PutMapping("/{id}/valider-global")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_DIRECTION', 'ROLE_ADMIN', 'ROLE_AGENT_CNSS')")
    public ResponseEntity<Dossier> validerGlobal(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(dossierService.changerStatut(id, "VALIDE", null, authentication.getName()));
    }

    @PutMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_BUREAU', 'ROLE_AGENT_DIRECTION', 'ROLE_ADMIN', 'ROLE_AGENT_CNSS')")
    public ResponseEntity<Dossier> refuser(@PathVariable Long id, @RequestParam String motif, Authentication authentication) {
        return ResponseEntity.ok(dossierService.changerStatut(id, "REFUSE", motif, authentication.getName()));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ROLE_ASSURE', 'ROLE_AGENT_BUREAU', 'ROLE_AGENT_DIRECTION', 'ROLE_ADMIN', 'ROLE_AGENT_CNSS')")
    public ResponseEntity<byte[]> downloadDossier(@PathVariable Long id) throws Exception {
        byte[] content = dossierService.downloadDossier(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dossier_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ASSURE', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDossier(@PathVariable Long id, Authentication authentication) throws Exception {
        dossierService.deleteDossier(id, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Dossier supprime avec succes"));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ROLE_AGENT_BUREAU', 'ROLE_AGENT_DIRECTION', 'ROLE_ADMIN', 'ROLE_AGENT_CNSS')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(dossierService.getStatistics());
    }
}
