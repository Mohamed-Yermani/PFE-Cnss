package com.projet.cnss.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DossierDto {
    private Long id;
    private String cin;
    private String fileName;
    private String alfrescoNodeId;
    private String statut;
    private String motifRefus;
    private String agentEmail;
    private Long userId;
    private LocalDateTime dateUpload;
    private LocalDateTime dateTraitement;
}