package com.projet.cnss.entity;

import java.util.List;

public enum TypePiece {
    CIN("Carte d'Identité Nationale", true, List.of("PDF", "JPG", "PNG")),
    EXTRAIT_NAISSANCE("Extrait de Naissance", true, List.of("PDF", "JPG", "PNG")),
    ATTESTATION_TRAVAIL("Attestation de Travail", true, List.of("PDF")),
    ATTESTATION_SALAIRE("Attestation de Salaire", true, List.of("PDF")),
    FORMULAIRE_SIGNE("Formulaire Signé CNSS", true, List.of("PDF")),
    CERTIFICAT_MEDICAL("Certificat Médical", false, List.of("PDF")),
    AUTRE("Autre Document", false, List.of("PDF", "JPG", "PNG"));

    private final String libelle;
    private final boolean obligatoire;
    private final List<String> formatsAcceptes;

    TypePiece(String libelle, boolean obligatoire, List<String> formatsAcceptes) {
        this.libelle = libelle;
        this.obligatoire = obligatoire;
        this.formatsAcceptes = formatsAcceptes;
    }

    public String getLibelle() { return libelle; }
    public boolean isObligatoire() { return obligatoire; }
    public List<String> getFormatsAcceptes() { return formatsAcceptes; }
}
