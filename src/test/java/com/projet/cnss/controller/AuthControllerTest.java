package com.projet.cnss.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.cnss.dto.*;
import com.projet.cnss.entity.AuditLog;
import com.projet.cnss.services.AuditLogService;
import com.projet.cnss.services.AuthService;
import com.projet.cnss.services.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    private Authentication mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    // ==========================================================
    // POST /api/auth/register
    // ==========================================================

    @Test
    void register_success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@test.com")
                .password("Plain@Pwd1")
                .cin("CIN1")
                .numeroAssure("NA1")
                .telephone("99999999")
                .role(com.projet.cnss.entity.ERole.ROLE_ASSURE)
                .build();

        RegistrationResponse response = RegistrationResponse.builder()
                .message("Inscription réussie. Vérifiez votre email.")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Inscription réussie. Vérifiez votre email."));
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("email-invalide")
                .password("Plain@Pwd1")
                .cin("CIN1")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================
    // GET /api/auth/verify
    // ==========================================================

    @Test
    void verifyAccount_success() throws Exception {
        doNothing().when(authService).verifyUser("token123");

        mockMvc.perform(get("/api/auth/verify").param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Compte vérifié avec succès !"));
    }

    // ==========================================================
    // POST /api/auth/login
    // ==========================================================

    @Test
    void login_success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("jean@test.com")
                .password("plainPwd")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("jwt-token")
                .email("jean@test.com")
                .role("ROLE_ASSURE")
                .id(1L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ROLE_ASSURE"));
    }

    @Test
    void login_invalidEmail_returnsBadRequest() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("pas-un-email")
                .password("pwd")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================
    // POST /api/auth/forgot-password
    // ==========================================================

    @Test
    void forgotPassword_success() throws Exception {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("jean@test.com")
                .build();

        doNothing().when(authService).requestPasswordReset(any(PasswordResetRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Email de réinitialisation envoyé"));
    }

    // ==========================================================
    // POST /api/auth/reset-password
    // ==========================================================

    @Test
    void resetPassword_success() throws Exception {
        NewPasswordRequest request = NewPasswordRequest.builder()
                .token("validToken")
                .newPassword("newPlainPwd")
                .build();

        doNothing().when(authService).resetPassword(any(NewPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe réinitialisé avec succès"));
    }

    // ==========================================================
    // GET /api/auth/me
    // ==========================================================

    @Test
    void getCurrentUser_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        UserDto dto = UserDto.builder()
                .id(1L)
                .email("jean@test.com")
                .nom("Dupont")
                .roles(Set.of("ROLE_ASSURE"))
                .build();

        when(userService.getCurrentUserByEmail("jean@test.com")).thenReturn(dto);

        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jean@test.com"));
    }

    // ==========================================================
    // GET /api/auth/audit-logs
    // ==========================================================

    @Test
    void getAuditLogs_success() throws Exception {
        AuditLog log = new AuditLog(1L, "CREATE_USER", "détail", LocalDateTime.now());
        when(auditLogService.getAllLogs()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/auth/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].action").value("CREATE_USER"));
    }
}