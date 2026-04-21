package com.projet.cnss.dto;



import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DossierUploadResponse {

    private Long id;
    private String fileName;
    private String statut;
    private LocalDateTime dateUpload;
    private AiVerificationDetails aiVerification;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiVerificationDetails {
        private boolean valide;
        private int score;
        private String scoreBadge;
        private String resume;
        private java.util.List<String> champsManquants;
        private java.util.List<String> champsInvalides;
        private java.util.Map<String, SectionInfo> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionInfo {
        private String statut;
        private String commentaire;
    }
}