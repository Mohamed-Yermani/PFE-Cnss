package com.projet.cnss.services;

import com.projet.cnss.dto.NotificationDTO;
import com.projet.cnss.dto.NotificationPayload;
import com.projet.cnss.entity.*;
import com.projet.cnss.repository.NotificationRepository;
import com.projet.cnss.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notifRepository;
    private final UserRepository         userRepository;
    private final WebSocketService       wsService;

    // ════════════════════════════════════════════════════════
    // MÉTHODE CENTRALE — Créer + Persister + Envoyer WS
    // ════════════════════════════════════════════════════════

    @Transactional
    public Notification creerEtEnvoyer(User user, String titre,
                                       String message, String type,
                                       Long dossierId, String typeAvantage) {
        // 1. Persister en BDD
        Notification notif = Notification.builder()
                .user(user)
                .titre(titre)
                .message(message)
                .type(type)
                .dossierId(dossierId)
                .typeAvantage(typeAvantage)
                .build();

        Notification saved = notifRepository.save(notif);

        // 2. Compter les non lues
        long nonLues = notifRepository.countByUserAndLueFalse(user);

        // 3. Envoyer via WebSocket
        NotificationPayload payload = NotificationPayload.builder()
                .id(saved.getId())
                .titre(saved.getTitre())
                .message(saved.getMessage())
                .type(saved.getType())
                .destinataire(user.getEmail())
                .dossierId(saved.getDossierId())
                .typeAvantage(saved.getTypeAvantage())
                .nonLues(nonLues)
                .dateEnvoi(saved.getDateCreation())
                .build();

        wsService.envoyerA(user.getEmail(), payload);

        log.info("Notification créée et envoyée à {} : {}", user.getEmail(), titre);
        return saved;
    }

    // ── Récupérer emails par rôles ───────────────────────────
    private List<User> getUsersParRoles(ERole... roles) {
        List<ERole> roleList = Arrays.asList(roles);
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> roleList.contains(r.getName())))
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════
    // NOTIFICATIONS DOSSIER
    // ════════════════════════════════════════════════════════
    @Transactional
    public void notifierNouveauDossier(Dossier dossier) {
        String msg = String.format(
                "L'assuré %s %s a soumis un nouveau dossier #%d (%s)",
                dossier.getUser().getNom(),
                dossier.getUser().getPrenom(),
                dossier.getId(),
                dossier.getTypeAvantage() != null ? dossier.getTypeAvantage() : "");

        getUsersParRoles(ERole.ROLE_AGENT_BUREAU, ERole.ROLE_AGENT_CNSS)
                .forEach(agent -> creerEtEnvoyer(agent,
                        "📁 Nouveau dossier déposé", msg, "INFO",
                        dossier.getId(), dossier.getTypeAvantage()));
    }
    @Transactional
    public void notifierStatutDossier(Dossier dossier, String statut) {
        String typeAvantage = dossier.getTypeAvantage() != null
                ? dossier.getTypeAvantage() : "votre avantage";
        String nomComplet = dossier.getUser().getNom()
                + " " + dossier.getUser().getPrenom();

        String titre, message, type;

        switch (statut) {
            case "VALIDATION_LOCALE" -> {
                titre   = "📋 Dossier en cours d'examen";
                message = String.format(
                        "Cher(e) %s, votre demande de %s (Dossier #%d) " +
                                "est transmise à l'agent de direction.",
                        nomComplet, typeAvantage, dossier.getId());
                type = "INFO";
            }
            case "VALIDE" -> {
                titre   = "✅ Demande de " + typeAvantage + " approuvée !";
                message = String.format(
                        "Félicitations %s ! Votre demande de %s (Dossier #%d) " +
                                "a été approuvée. Rapprochez-vous de votre agence CNSS.",
                        nomComplet, typeAvantage, dossier.getId());
                type = "SUCCESS";
            }
            case "REFUSE" -> {
                titre   = "❌ Demande de " + typeAvantage + " refusée";
                message = String.format(
                        "Cher(e) %s, votre demande de %s (Dossier #%d) " +
                                "a été refusée. Motif : %s",
                        nomComplet, typeAvantage, dossier.getId(),
                        dossier.getMotifRefus() != null
                                ? dossier.getMotifRefus() : "Non précisé");
                type = "DANGER";
            }
            default -> {
                titre   = "📁 Mise à jour - " + typeAvantage;
                message = String.format(
                        "Le statut de votre dossier #%d a changé : %s",
                        dossier.getId(), statut);
                type = "INFO";
            }
        }

        creerEtEnvoyer(dossier.getUser(),
                titre, message, type,
                dossier.getId(), typeAvantage);
    }
@Transactional
    public void notifierValidationGlobale(Dossier dossier, String statut) {
        String typeAvantage = dossier.getTypeAvantage() != null
                ? dossier.getTypeAvantage() : "Avantage";

        String msg = String.format(
                "Le dossier #%d de %s %s (%s) est : %s",
                dossier.getId(),
                dossier.getUser().getNom(),
                dossier.getUser().getPrenom(),
                typeAvantage, statut);

        boolean valide = statut.equals("VALIDE");

        getUsersParRoles(ERole.ROLE_AGENT_CNSS, ERole.ROLE_AGENT_DIRECTION)
                .forEach(agent -> creerEtEnvoyer(agent,
                        valide ? "✅ Dossier validé" : "❌ Dossier refusé",
                        msg,
                        valide ? "SUCCESS" : "DANGER",
                        dossier.getId(), typeAvantage));
    }

    // ════════════════════════════════════════════════════════
    // NOTIFICATIONS PIÈCES
    // ════════════════════════════════════════════════════════
@Transactional
    public void notifierNouvellepiece(Dossier dossier, TypePiece typePiece) {
        String msg = String.format(
                "L'assuré %s %s a déposé : %s (Dossier #%d)",
                dossier.getUser().getNom(),
                dossier.getUser().getPrenom(),
                typePiece.getLibelle(),
                dossier.getId());

        getUsersParRoles(ERole.ROLE_AGENT_BUREAU, ERole.ROLE_AGENT_CNSS)
                .forEach(agent -> creerEtEnvoyer(agent,
                        "📎 Nouvelle pièce déposée", msg, "INFO",
                        dossier.getId(), null));
    }
    @Transactional
    public void notifierValidationPiece(Dossier dossier,
                                        TypePiece typePiece,
                                        boolean valide,
                                        String motif) {
        String titre   = valide ? "✅ Pièce validée" : "❌ Pièce refusée";
        String message = valide
                ? String.format("Votre %s (Dossier #%d) a été validée.",
                typePiece.getLibelle(), dossier.getId())
                : String.format("Votre %s (Dossier #%d) a été refusée. Motif : %s",
                typePiece.getLibelle(), dossier.getId(), motif);

        creerEtEnvoyer(dossier.getUser(),
                titre, message,
                valide ? "SUCCESS" : "DANGER",
                dossier.getId(), null);
    }

    // ════════════════════════════════════════════════════════
    // LECTURE — Historique + Compteur
    // ════════════════════════════════════════════════════════

    // Toutes les notifications (historique)
    public List<NotificationDTO> getHistorique(User user) {
        return notifRepository
                .findByUserOrderByDateCreationDesc(user)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Historique paginé
    public Page<NotificationDTO> getHistoriquePagine(User user,
                                                     int page, int size) {
        return notifRepository
                .findByUserOrderByDateCreationDesc(
                        user, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    // Non lues seulement
    public List<NotificationDTO> getNonLues(User user) {
        return notifRepository
                .findByUserAndLueFalseOrderByDateCreationDesc(user)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Compteur non lues
    public long countNonLues(User user) {
        return notifRepository.countByUserAndLueFalse(user);
    }

    // Notifications par dossier
    public List<NotificationDTO> getParDossier(User user, Long dossierId) {
        return notifRepository
                .findByUserAndDossierIdOrderByDateCreationDesc(user, dossierId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════
    // ACTIONS — Marquer lue / Supprimer
    // ════════════════════════════════════════════════════════

    @Transactional
    public long marquerLue(Long notifId, User user) {
        int updated = notifRepository.marquerLue(notifId, user);
        if (updated == 0) {
            throw new RuntimeException("Notification non trouvée ou non autorisée");
        }
        long restantes = notifRepository.countByUserAndLueFalse(user);

        // Envoyer mise à jour compteur via WebSocket
        wsService.envoyerCompteur(user.getEmail(), restantes);
        return restantes;
    }

    @Transactional
    public long marquerToutesLues(User user) {
        notifRepository.marquerToutesLues(user);

        // Envoyer compteur = 0 via WebSocket
        wsService.envoyerCompteur(user.getEmail(), 0);
        return 0;
    }

    @Transactional
    public void supprimerNotification(Long notifId, User user) {
        Notification notif = notifRepository.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        if (!notif.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Non autorisé");
        }
        notifRepository.delete(notif);
    }

    @Transactional
    public void supprimerToutes(User user) {
        notifRepository.deleteByUser(user);
        wsService.envoyerCompteur(user.getEmail(), 0);
    }

    // ── Mapper entité → DTO ───────────────────────────────────
    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .titre(n.getTitre())
                .message(n.getMessage())
                .type(n.getType())
                .lue(n.isLue())
                .dossierId(n.getDossierId())
                .typeAvantage(n.getTypeAvantage())
                .dateCreation(n.getDateCreation())
                .userEmail(n.getUser().getEmail())
                .build();
    }
}