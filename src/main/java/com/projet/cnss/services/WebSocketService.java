package com.projet.cnss.services;

import com.projet.cnss.dto.NotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // Envoyer une notification à un user spécifique
    public void envoyerA(String userEmail, NotificationPayload payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/notifications",
                    payload
            );
            log.info("WS notif envoyée à {} : {}", userEmail, payload.getTitre());
        } catch (Exception e) {
            log.error("Erreur WS envoi à {} : {}", userEmail, e.getMessage());
        }
    }

    // Envoyer mise à jour du compteur seulement
    public void envoyerCompteur(String userEmail, long nonLues) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    "/queue/compteur",
                    Map.of("nonLues", nonLues)
            );
            log.info("WS compteur envoyé à {} : {} non lues", userEmail, nonLues);
        } catch (Exception e) {
            log.error("Erreur WS compteur à {} : {}", userEmail, e.getMessage());
        }
    }

    // Diffuser à tous
    public void diffuser(String topic, NotificationPayload payload) {
        try {
            messagingTemplate.convertAndSend("/topic/" + topic, payload);
        } catch (Exception e) {
            log.error("Erreur WS diffusion : {}", e.getMessage());
        }
    }
}