package com.projet.cnss.controller;

import com.projet.cnss.dto.DossierDto;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import com.projet.cnss.services.DossierService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
public class DossierController {

    private final DossierService dossierService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<DossierDto> upload(
            @RequestParam String cin,
            @RequestParam MultipartFile file,
            Authentication authentication
    ) throws Exception {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        DossierDto dossier = dossierService.uploadDossier(cin, file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(dossier);
    }

    @GetMapping("/my-dossiers")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<List<DossierDto>> myDossiers(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(dossierService.getUserDossiers(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ASSURE', 'AGENT_CNSS', 'ADMIN')")
    public ResponseEntity<DossierDto> getDossierById(@PathVariable Long id) {
        return ResponseEntity.ok(dossierService.getDossierById(id));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAnyRole('AGENT_CNSS', 'ADMIN')")
    public ResponseEntity<List<DossierDto>> dossiersEnAttente() {
        return ResponseEntity.ok(dossierService.getDossiersEnAttente());
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('AGENT_CNSS', 'ADMIN')")
    public ResponseEntity<DossierDto> validerDossier(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(dossierService.validerDossier(id, authentication.getName()));
    }

    @PutMapping("/{id}/refuser")
    @PreAuthorize("hasAnyRole('AGENT_CNSS', 'ADMIN')")
    public ResponseEntity<DossierDto> refuserDossier(
            @PathVariable Long id,
            @RequestParam String motif,
            Authentication authentication
    ) {
        return ResponseEntity.ok(dossierService.refuserDossier(id, motif, authentication.getName()));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ASSURE', 'AGENT_CNSS', 'ADMIN')")
    public ResponseEntity<ByteArrayResource> downloadDossier(
            @PathVariable Long id,
            Authentication authentication
    ) throws Exception {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        byte[] fileContent = dossierService.downloadDossier(id, user);
        DossierDto dossier = dossierService.getDossierById(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dossier.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(fileContent.length)
                .body(new ByteArrayResource(fileContent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ASSURE', 'ADMIN')")
    public ResponseEntity<String> deleteDossier(
            @PathVariable Long id,
            Authentication authentication
    ) throws Exception {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        dossierService.deleteDossier(id, user);
        return ResponseEntity.ok("Dossier supprimé avec succès");
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('AGENT_CNSS', 'ADMIN')")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(dossierService.getStatistics());
    }
}