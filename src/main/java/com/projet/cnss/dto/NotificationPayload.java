package com.projet.cnss.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {

    private Long          id;           // ID BDD
    private String        titre;
    private String        message;
    private String        type;
    private String        destinataire;
    private Long          dossierId;
    private String        typeAvantage;
    private long          nonLues;      // compteur mis à jour

    @Builder.Default
    private LocalDateTime dateEnvoi = LocalDateTime.now();
}