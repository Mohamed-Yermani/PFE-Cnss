package com.projet.cnss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiVerificationResult {

    private boolean valide;
    private int score;
    private String resume;
    private List<String> champsManquants;
    private List<String> champsInvalides;

    // Détails par section
    private SectionDetail detailIdentite;
    private SectionDetail detailEmployeur;
    private SectionDetail detailTypeDossier;
    private SectionDetail detailPeriode;
    private SectionDetail detailSignature;
    private SectionDetail detailCoherence;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionDetail {
        private String statut;       // OK / INCOMPLET / INVALIDE / ATTENTION
        private String commentaire;
    }
}
