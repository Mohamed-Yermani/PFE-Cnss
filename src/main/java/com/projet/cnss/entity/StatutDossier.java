package com.projet.cnss.entity;

public enum StatutDossier {
    EN_ATTENTE("En attente de traitement"),
    VALIDE("Validé"),
    REFUSE("Refusé");

    private final String description;

    StatutDossier(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}