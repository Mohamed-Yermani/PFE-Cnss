package com.projet.cnss.controller;

import com.projet.cnss.dto.NotificationDTO;
import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import com.projet.cnss.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notifService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    private Authentication mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    private User buildUser(Long id, String email, ERole eRole) {
        Role role = new Role(eRole);
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    private NotificationDTO buildNotifDto(Long id) {
        return NotificationDTO.builder()
                .id(id)
                .titre("Titre")
                .message("Message")
                .type("INFO")
                .lue(false)
                .dossierId(1L)
                .typeAvantage("Retraite")
                .dateCreation(LocalDateTime.now())
                .userEmail("jean@test.com")
                .build();
    }

    // ==========================================================
    // GET /api/notifications
    // ==========================================================

    @Test
    void getHistorique_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.getHistorique(user)).thenReturn(List.of(buildNotifDto(1L)));

        mockMvc.perform(get("/api/notifications").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }



    // ==========================================================
    // GET /api/notifications/page
    // ==========================================================

    @Test
    void getHistoriquePage_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        Page<NotificationDTO> page = new PageImpl<>(List.of(buildNotifDto(1L)), PageRequest.of(0, 10), 1);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.getHistoriquePagine(user, 0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/notifications/page")
                        .param("page", "0")
                        .param("size", "10")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getHistoriquePage_defaultParams_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);
        Page<NotificationDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.getHistoriquePagine(user, 0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/notifications/page").principal(auth))
                .andExpect(status().isOk());
    }

    // ==========================================================
    // GET /api/notifications/non-lues
    // ==========================================================

    @Test
    void getNonLues_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.getNonLues(user)).thenReturn(List.of(buildNotifDto(1L)));

        mockMvc.perform(get("/api/notifications/non-lues").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // GET /api/notifications/count
    // ==========================================================

    @Test
    void count_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.countNonLues(user)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/count").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonLues").value(5));
    }

    // ==========================================================
    // GET /api/notifications/dossier/{dossierId}
    // ==========================================================

    @Test
    void getParDossier_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.getParDossier(user, 1L)).thenReturn(List.of(buildNotifDto(1L)));

        mockMvc.perform(get("/api/notifications/dossier/1").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // PUT /api/notifications/{id}/lire
    // ==========================================================

    @Test
    void marquerLue_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.marquerLue(1L, user)).thenReturn(2L);

        mockMvc.perform(put("/api/notifications/1/lire").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonLues").value(2));
    }

    // ==========================================================
    // PUT /api/notifications/lire-toutes
    // ==========================================================

    @Test
    void marquerToutesLues_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.marquerToutesLues(user)).thenReturn(0L);

        mockMvc.perform(put("/api/notifications/lire-toutes").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonLues").value(0));
    }

    // ==========================================================
    // DELETE /api/notifications/{id}
    // ==========================================================

    @Test
    void supprimer_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        doNothing().when(notifService).supprimerNotification(1L, user);

        mockMvc.perform(delete("/api/notifications/1").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification supprimée"));
    }

    // ==========================================================
    // DELETE /api/notifications/toutes
    // ==========================================================

    @Test
    void supprimerToutes_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        doNothing().when(notifService).supprimerToutes(user);

        mockMvc.perform(delete("/api/notifications/toutes").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Toutes les notifications supprimées"));
    }

    // ==========================================================
    // GET /api/notifications/status
    // ==========================================================

    @Test
    void status_success() throws Exception {
        mockMvc.perform(get("/api/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WebSocket + MySQL opérationnels"))
                .andExpect(jsonPath("$.endpoint").value("ws://localhost:8089/ws"));
    }

    // ==========================================================
    // POST /api/notifications/test/{email}
    // ==========================================================

    @Test
    void sendTestNotification_success() throws Exception {
        User user = buildUser(1L, "jean@test.com", ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(notifService.creerEtEnvoyer(
                eq(user), anyString(), eq("Message de test"), eq("INFO"), isNull(), isNull()))
                .thenReturn(mock(com.projet.cnss.entity.Notification.class));
        mockMvc.perform(post("/api/notifications/test/jean@test.com")
                        .param("message", "Message de test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification de test envoyée avec succès à jean@test.com"));
    }


}