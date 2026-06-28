package com.projet.cnss.services;

import com.projet.cnss.dto.NotificationDTO;
import com.projet.cnss.dto.NotificationPayload;
import com.projet.cnss.entity.*;
import com.projet.cnss.repository.NotificationRepository;
import com.projet.cnss.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notifRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketService wsService;

    @InjectMocks
    private NotificationService notificationService;

    private User buildUser(Long id, String email, ERole eRole) {
        Role role = new Role(eRole);
        User user = new User();
        user.setId(id);
        user.setNom("Nom" + id);
        user.setPrenom("Prenom" + id);
        user.setEmail(email);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    private Dossier buildDossier(Long id, User user, String typeAvantage, String motifRefus) {
        Dossier d = new Dossier();
        d.setId(id);
        d.setUser(user);
        d.setTypeAvantage(typeAvantage);
        d.setMotifRefus(motifRefus);
        d.setStatut("EN_ATTENTE");
        return d;
    }

    private Notification buildNotification(Long id, User user, boolean lue) {
        return Notification.builder()
                .id(id)
                .titre("Titre")
                .message("Message")
                .type("INFO")
                .lue(lue)
                .dossierId(1L)
                .typeAvantage("Retraite")
                .dateCreation(LocalDateTime.now())
                .user(user)
                .build();
    }

    // ==========================================================
    // creerEtEnvoyer
    // ==========================================================

    @Test
    void creerEtEnvoyer_success_savesAndSendsWebSocketPayload() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(10L);
            n.setDateCreation(LocalDateTime.now());
            return n;
        });
        when(notifRepository.countByUserAndLueFalse(user)).thenReturn(3L);

        Notification result = notificationService.creerEtEnvoyer(
                user, "Titre test", "Message test", "INFO", 5L, "Retraite");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Titre test", result.getTitre());

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("jean@test.com"), captor.capture());

        NotificationPayload payload = captor.getValue();
        assertEquals(10L, payload.getId());
        assertEquals("Titre test", payload.getTitre());
        assertEquals("jean@test.com", payload.getDestinataire());
        assertEquals(5L, payload.getDossierId());
        assertEquals("Retraite", payload.getTypeAvantage());
        assertEquals(3L, payload.getNonLues());
    }

    // ==========================================================
    // notifierNouveauDossier
    // ==========================================================

    @Test
    void notifierNouveauDossier_notifiesAgentsBureauAndCnss() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        User agentBureau = buildUser(2L, "bureau@test.com", ERole.ROLE_AGENT_BUREAU);
        User agentCnss = buildUser(3L, "cnss@test.com", ERole.ROLE_AGENT_CNSS);
        User agentDirection = buildUser(4L, "direction@test.com", ERole.ROLE_AGENT_DIRECTION);

        when(userRepository.findAll()).thenReturn(List.of(agentBureau, agentCnss, agentDirection));
        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(any(User.class))).thenReturn(0L);

        notificationService.notifierNouveauDossier(dossier);

        // Seuls bureau et cnss doivent être notifiés, pas direction
        verify(wsService).envoyerA(eq("bureau@test.com"), any(NotificationPayload.class));
        verify(wsService).envoyerA(eq("cnss@test.com"), any(NotificationPayload.class));
        verify(wsService, never()).envoyerA(eq("direction@test.com"), any(NotificationPayload.class));
    }

    @Test
    void notifierNouveauDossier_typeAvantageNull_messageContainsEmptyString() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, null, null);

        User agentBureau = buildUser(2L, "bureau@test.com", ERole.ROLE_AGENT_BUREAU);

        when(userRepository.findAll()).thenReturn(List.of(agentBureau));
        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(any(User.class))).thenReturn(0L);

        notificationService.notifierNouveauDossier(dossier);

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("bureau@test.com"), captor.capture());
        assertTrue(captor.getValue().getMessage().contains("nouveau dossier #1"));
    }

    // ==========================================================
    // notifierStatutDossier
    // ==========================================================

    @Test
    void notifierStatutDossier_validationLocale() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierStatutDossier(dossier, "VALIDATION_LOCALE");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertTrue(captor.getValue().getTitre().contains("en cours d'examen"));
    }

    @Test
    void notifierStatutDossier_valide() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierStatutDossier(dossier, "VALIDE");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertTrue(captor.getValue().getTitre().contains("approuvée"));
        assertTrue(captor.getValue().getMessage().contains("Félicitations"));
    }

    @Test
    void notifierStatutDossier_refuseAvecMotif() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", "Document manquant");

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierStatutDossier(dossier, "REFUSE");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertTrue(captor.getValue().getTitre().contains("refusée"));
        assertTrue(captor.getValue().getMessage().contains("Document manquant"));
    }

    @Test
    void notifierStatutDossier_refuseSansMotif_usesNonPrecise() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierStatutDossier(dossier, "REFUSE");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertTrue(captor.getValue().getMessage().contains("Non précisé"));
    }

    @Test
    void notifierStatutDossier_statutInconnu_usesDefaultBranch() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierStatutDossier(dossier, "EN_COURS_INCONNU");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertTrue(captor.getValue().getTitre().contains("Mise à jour"));
        assertTrue(captor.getValue().getMessage().contains("EN_COURS_INCONNU"));
    }

    @Test
    void notifierStatutDossier_typeAvantageNull_usesDefaultLabel() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, null, null);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierStatutDossier(dossier, "VALIDE");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertTrue(captor.getValue().getTitre().contains("votre avantage"));
    }

    // ==========================================================
    // notifierValidationGlobale
    // ==========================================================

    @Test
    void notifierValidationGlobale_valide_notifiesCnssAndDirection() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        User agentCnss = buildUser(2L, "cnss@test.com", ERole.ROLE_AGENT_CNSS);
        User agentDirection = buildUser(3L, "direction@test.com", ERole.ROLE_AGENT_DIRECTION);
        User agentBureau = buildUser(4L, "bureau@test.com", ERole.ROLE_AGENT_BUREAU);

        when(userRepository.findAll()).thenReturn(List.of(agentCnss, agentDirection, agentBureau));
        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(any(User.class))).thenReturn(0L);

        notificationService.notifierValidationGlobale(dossier, "VALIDE");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("cnss@test.com"), captor.capture());
        assertEquals("✅ Dossier validé", captor.getValue().getTitre());
        verify(wsService).envoyerA(eq("direction@test.com"), any(NotificationPayload.class));
        verify(wsService, never()).envoyerA(eq("bureau@test.com"), any(NotificationPayload.class));
    }

    @Test
    void notifierValidationGlobale_refuse_notifiesWithDangerType() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, null, "Motif");

        User agentCnss = buildUser(2L, "cnss@test.com", ERole.ROLE_AGENT_CNSS);

        when(userRepository.findAll()).thenReturn(List.of(agentCnss));
        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(any(User.class))).thenReturn(0L);

        notificationService.notifierValidationGlobale(dossier, "REFUSE");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("cnss@test.com"), captor.capture());
        assertEquals("❌ Dossier refusé", captor.getValue().getTitre());
        assertEquals("DANGER", captor.getValue().getType());
    }

    // ==========================================================
    // notifierNouvellepiece
    // ==========================================================

    @Test
    void notifierNouvellepiece_notifiesAgentsBureauAndCnss() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        User agentBureau = buildUser(2L, "bureau@test.com", ERole.ROLE_AGENT_BUREAU);

        when(userRepository.findAll()).thenReturn(List.of(agentBureau));
        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(any(User.class))).thenReturn(0L);

        notificationService.notifierNouvellepiece(dossier, TypePiece.CIN);

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("bureau@test.com"), captor.capture());
        assertTrue(captor.getValue().getMessage().contains("Carte d'Identité Nationale"));
    }

    // ==========================================================
    // notifierValidationPiece
    // ==========================================================

    @Test
    void notifierValidationPiece_valide() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierValidationPiece(dossier, TypePiece.CIN, true, null);

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertEquals("✅ Pièce validée", captor.getValue().getTitre());
        assertEquals("SUCCESS", captor.getValue().getType());
    }

    @Test
    void notifierValidationPiece_refuse() {
        User assure = buildUser(1L, "assure@test.com", ERole.ROLE_ASSURE);
        Dossier dossier = buildDossier(1L, assure, "Retraite", null);

        when(notifRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(notifRepository.countByUserAndLueFalse(assure)).thenReturn(0L);

        notificationService.notifierValidationPiece(dossier, TypePiece.CIN, false, "Document illisible");

        ArgumentCaptor<NotificationPayload> captor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(wsService).envoyerA(eq("assure@test.com"), captor.capture());
        assertEquals("❌ Pièce refusée", captor.getValue().getTitre());
        assertEquals("DANGER", captor.getValue().getType());
        assertTrue(captor.getValue().getMessage().contains("Document illisible"));
    }

    // ==========================================================
    // getHistorique
    // ==========================================================

    @Test
    void getHistorique_returnsMappedDTOs() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        Notification notif = buildNotification(1L, user, false);

        when(notifRepository.findByUserOrderByDateCreationDesc(user)).thenReturn(List.of(notif));

        List<NotificationDTO> result = notificationService.getHistorique(user);

        assertEquals(1, result.size());
        assertEquals("Titre", result.get(0).getTitre());
        assertEquals("jean@test.com", result.get(0).getUserEmail());
    }

    // ==========================================================
    // getHistoriquePagine
    // ==========================================================

    @Test
    void getHistoriquePagine_returnsMappedPage() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        Notification notif = buildNotification(1L, user, false);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notif), pageable, 1);

        when(notifRepository.findByUserOrderByDateCreationDesc(eq(user), any(Pageable.class)))
                .thenReturn(page);

        Page<NotificationDTO> result = notificationService.getHistoriquePagine(user, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Titre", result.getContent().get(0).getTitre());
    }

    // ==========================================================
    // getNonLues
    // ==========================================================

    @Test
    void getNonLues_returnsMappedDTOs() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        Notification notif = buildNotification(1L, user, false);

        when(notifRepository.findByUserAndLueFalseOrderByDateCreationDesc(user))
                .thenReturn(List.of(notif));

        List<NotificationDTO> result = notificationService.getNonLues(user);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isLue());
    }

    // ==========================================================
    // countNonLues
    // ==========================================================

    @Test
    void countNonLues_returnsCount() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        when(notifRepository.countByUserAndLueFalse(user)).thenReturn(5L);

        long result = notificationService.countNonLues(user);

        assertEquals(5L, result);
    }

    // ==========================================================
    // getParDossier
    // ==========================================================

    @Test
    void getParDossier_returnsMappedDTOs() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        Notification notif = buildNotification(1L, user, false);

        when(notifRepository.findByUserAndDossierIdOrderByDateCreationDesc(user, 1L))
                .thenReturn(List.of(notif));

        List<NotificationDTO> result = notificationService.getParDossier(user, 1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDossierId());
    }

    // ==========================================================
    // marquerLue
    // ==========================================================

    @Test
    void marquerLue_success_returnsRemainingCountAndSendsCompteur() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(notifRepository.marquerLue(1L, user)).thenReturn(1);
        when(notifRepository.countByUserAndLueFalse(user)).thenReturn(2L);

        long result = notificationService.marquerLue(1L, user);

        assertEquals(2L, result);
        verify(wsService).envoyerCompteur("jean@test.com", 2L);
    }

    @Test
    void marquerLue_notFoundOrNotAuthorized_throwsException() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(notifRepository.marquerLue(99L, user)).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.marquerLue(99L, user));

        assertTrue(ex.getMessage().contains("non trouvée ou non autorisée"));
        verify(wsService, never()).envoyerCompteur(anyString(), anyLong());
    }

    // ==========================================================
    // marquerToutesLues
    // ==========================================================

    @Test
    void marquerToutesLues_success_returnsZeroAndSendsCompteur() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        long result = notificationService.marquerToutesLues(user);

        assertEquals(0L, result);
        verify(notifRepository).marquerToutesLues(user);
        verify(wsService).envoyerCompteur("jean@test.com", 0);
    }

    // ==========================================================
    // supprimerNotification
    // ==========================================================

    @Test
    void supprimerNotification_success() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        Notification notif = buildNotification(10L, user, false);

        when(notifRepository.findById(10L)).thenReturn(Optional.of(notif));

        notificationService.supprimerNotification(10L, user);

        verify(notifRepository).delete(notif);
    }

    @Test
    void supprimerNotification_notFound_throwsException() {
        when(notifRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.supprimerNotification(99L, buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE)));

        assertTrue(ex.getMessage().contains("Notification non trouvée"));
    }

    @Test
    void supprimerNotification_notAuthorized_throwsException() {
        User owner = buildUser(1L, "owner@test.com", ERole.ROLE_ASSURE);
        User otherUser = buildUser(2L, "other@test.com", ERole.ROLE_ASSURE);
        Notification notif = buildNotification(10L, owner, false);

        when(notifRepository.findById(10L)).thenReturn(Optional.of(notif));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.supprimerNotification(10L, otherUser));

        assertTrue(ex.getMessage().contains("Non autorisé"));
        verify(notifRepository, never()).delete(any());
    }

    // ==========================================================
    // supprimerToutes
    // ==========================================================

    @Test
    void supprimerToutes_success() {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        notificationService.supprimerToutes(user);

        verify(notifRepository).deleteByUser(user);
        verify(wsService).envoyerCompteur("jean@test.com", 0);
    }
}