package com.projet.cnss.repository;

import com.projet.cnss.entity.Dossier;
import com.projet.cnss.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DossierRepository extends JpaRepository<Dossier, Long> {
    List<Dossier> findByUser(User user);
    List<Dossier> findByStatut(String statut);
    long countByStatut(String statut);
}