package com.projet.cnss.controller;

import com.projet.cnss.dto.NotificationDTO;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import com.projet.cnss.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;
    private final UserRepository      userRepository;

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // ── Historique complet ────────────────────────────────────
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getHistorique(
            Authentication auth) {
        return ResponseEntity.ok(
                notifService.getHistorique(getUser(auth)));
    }

    // ── Historique paginé ─────────────────────────────────────
    @GetMapping("/page")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<NotificationDTO>> getHistoriquePage(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return ResponseEntity.ok(
                notifService.getHistoriquePagine(getUser(auth), page, size));
    }

    // ── Non lues ──────────────────────────────────────────────
    @GetMapping("/non-lues")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getNonLues(
            Authentication auth) {
        return ResponseEntity.ok(
                notifService.getNonLues(getUser(auth)));
    }

    // ── Compteur non lues ─────────────────────────────────────
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> count(Authentication auth) {
        return ResponseEntity.ok(Map.of(
                "nonLues", notifService.countNonLues(getUser(auth))
        ));
    }

    // ── Notifications d'un dossier ────────────────────────────
    @GetMapping("/dossier/{dossierId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getParDossier(
            @PathVariable Long dossierId,
            Authentication auth) {
        return ResponseEntity.ok(
                notifService.getParDossier(getUser(auth), dossierId));
    }

    // ── Marquer une notification comme lue ───────────────────
    @PutMapping("/{id}/lire")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> marquerLue(
            @PathVariable Long id,
            Authentication auth) {
        long restantes = notifService.marquerLue(id, getUser(auth));
        return ResponseEntity.ok(Map.of("nonLues", restantes));
    }

    // ── Marquer toutes comme lues ─────────────────────────────
    @PutMapping("/lire-toutes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> marquerToutesLues(
            Authentication auth) {
        long restantes = notifService.marquerToutesLues(getUser(auth));
        return ResponseEntity.ok(Map.of("nonLues", restantes));
    }

    // ── Supprimer une notification ────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> supprimer(
            @PathVariable Long id,
            Authentication auth) {
        notifService.supprimerNotification(id, getUser(auth));
        return ResponseEntity.ok(Map.of(
                "message", "Notification supprimée"));
    }

    // ── Supprimer toutes ──────────────────────────────────────
    @DeleteMapping("/toutes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> supprimerToutes(
            Authentication auth) {
        notifService.supprimerToutes(getUser(auth));
        return ResponseEntity.ok(Map.of(
                "message", "Toutes les notifications supprimées"));
    }

    // ── Statut WebSocket ──────────────────────────────────────
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "status",    "WebSocket + MySQL opérationnels",
                "endpoint",  "ws://localhost:8089/ws",
                "topics",    List.of(
                        "/user/queue/notifications",
                        "/user/queue/compteur"
                )
        ));
    }
}