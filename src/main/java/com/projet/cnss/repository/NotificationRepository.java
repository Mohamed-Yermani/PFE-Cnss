package com.projet.cnss.repository;

import com.projet.cnss.entity.Notification;
import com.projet.cnss.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // Toutes les notifications d'un user (tri date desc)
    List<Notification> findByUserOrderByDateCreationDesc(User user);

    // Avec pagination
    Page<Notification> findByUserOrderByDateCreationDesc(
            User user, Pageable pageable);

    // Non lues seulement
    List<Notification> findByUserAndLueFalseOrderByDateCreationDesc(User user);

    // Compter les non lues
    long countByUserAndLueFalse(User user);

    // Marquer une notif comme lue
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lue = true WHERE n.id = :id AND n.user = :user")
    int marquerLue(@Param("id") Long id, @Param("user") User user);

    // Marquer toutes comme lues
    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lue = true WHERE n.user = :user AND n.lue = false")
    int marquerToutesLues(@Param("user") User user);

    // Supprimer toutes les notifs d'un user
    @Modifying
    @Transactional
    void deleteByUser(User user);

    // Notifications par type
    List<Notification> findByUserAndTypeOrderByDateCreationDesc(
            User user, String type);

    // Notifications liées à un dossier
    List<Notification> findByUserAndDossierIdOrderByDateCreationDesc(
            User user, Long dossierId);
}