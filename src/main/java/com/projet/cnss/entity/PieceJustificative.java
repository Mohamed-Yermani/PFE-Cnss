package com.projet.cnss.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pieces_justificatives")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieceJustificative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TypePiece typePiece;

    private String fileName;
    private String minioPath;       // chemin dans MinIO
    private String statut;          // EN_ATTENTE / VALIDE / REFUSE
    private String motifRefus;
    private LocalDateTime dateUpload;

    @PrePersist
    protected void onCreate() { dateUpload = LocalDateTime.now(); }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    @JsonIgnore
    private Dossier dossier;
}