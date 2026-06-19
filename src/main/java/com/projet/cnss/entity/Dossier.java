package com.projet.cnss.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cin;
    private String fileName;
    private String alfrescoNodeId;
    private String statut;

    private String motifRefus;
    private String agentEmail;
    // Dans Dossier.java
    private String typeAvantage;  // Ex: "Retraite", "Invalidite", "Accident de travail"

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "date_upload", nullable = false)
    private LocalDateTime dateUpload;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @PrePersist
    protected void onCreate() {
        if (dateUpload == null) {
            dateUpload = LocalDateTime.now();
        }
    }
}