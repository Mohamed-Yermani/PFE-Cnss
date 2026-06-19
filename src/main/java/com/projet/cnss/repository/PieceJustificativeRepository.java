package com.projet.cnss.repository;

import com.projet.cnss.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// PieceJustificativeRepository.java
@Repository
public interface PieceJustificativeRepository
        extends JpaRepository<PieceJustificative, Long> {
    List<PieceJustificative> findByDossier(Dossier dossier);
    List<PieceJustificative> findByDossierAndTypePiece(Dossier dossier, TypePiece type);
    long countByDossierAndStatut(Dossier dossier, String statut);
}

