package com.projet.cnss.services;

import com.projet.cnss.dto.NotificationPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    private NotificationPayload payload;

    @BeforeEach
    void setUp() {
        payload = NotificationPayload.builder()
                .id(1L)
                .titre("Titre test")
                .message("Message test")
                .type("INFO")
                .destinataire("jean@test.com")
                .dossierId(5L)
                .typeAvantage("Retraite")
                .nonLues(3L)
                .dateEnvoi(LocalDateTime.now())
                .build();
    }

    // ==========================================================
    // envoyerA
    // ==========================================================

    @Test
    void envoyerA_success_sendsToUserQueue() {
        webSocketService.envoyerA("jean@test.com", payload);

        verify(messagingTemplate).convertAndSendToUser(
                "jean@test.com", "/queue/notifications", payload);
    }

    @Test
    void envoyerA_exceptionThrown_isCaughtSilently() {
        doThrow(new RuntimeException("Erreur WS"))
                .when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any(NotificationPayload.class));

        assertDoesNotThrow(() -> webSocketService.envoyerA("jean@test.com", payload));
    }

    // ==========================================================
    // envoyerCompteur
    // ==========================================================

    @Test
    void envoyerCompteur_success_sendsCompteurMap() {
        webSocketService.envoyerCompteur("jean@test.com", 7L);

        verify(messagingTemplate).convertAndSendToUser(
                "jean@test.com", "/queue/compteur", Map.of("nonLues", 7L));
    }

    @Test
    void envoyerCompteur_exceptionThrown_isCaughtSilently() {
        doThrow(new RuntimeException("Erreur WS"))
                .when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> webSocketService.envoyerCompteur("jean@test.com", 0L));
    }

    // ==========================================================
    // diffuser
    // ==========================================================

    @Test
    void diffuser_success_sendsToTopic() {
        webSocketService.diffuser("notifications-globales", payload);

        verify(messagingTemplate).convertAndSend("/topic/notifications-globales", payload);
    }

    @Test
    void diffuser_exceptionThrown_isCaughtSilently() {
        doThrow(new RuntimeException("Erreur WS"))
                .when(messagingTemplate)
                .convertAndSend(anyString(), any(NotificationPayload.class));

        assertDoesNotThrow(() -> webSocketService.diffuser("topic-test", payload));
    }
}