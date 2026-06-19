package com.projet.cnss.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long          id;
    private String        titre;
    private String        message;
    private String        type;
    private boolean       lue;
    private Long          dossierId;
    private String        typeAvantage;
    private LocalDateTime dateCreation;
    private String        userEmail;
}