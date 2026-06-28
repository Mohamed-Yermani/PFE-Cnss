package com.projet.cnss.services;

import com.projet.cnss.config.JwtService;
import com.projet.cnss.dto.*;
import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.entity.User;
import com.projet.cnss.exception.EmailAlreadyExistsException;
import com.projet.cnss.mail.EmailService;
import com.projet.cnss.repository.RoleRepository;
import com.projet.cnss.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private User buildUser(Long id, String email, String encodedPassword, ERole eRole, boolean enabled) {
        Role role = new Role(eRole);
        User user = new User();
        user.setId(id);
        user.setNom("Nom");
        user.setPrenom("Prenom");
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setEnabled(enabled);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    // =========================================================
    // createAdminAccount
    // =========================================================

    @Test
    void createAdminAccount_whenAdminDoesNotExist_createsNewAdmin() {
        Role adminRole = new Role(ERole.ROLE_ADMIN);

        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmail("admin@carrent.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin")).thenReturn("encodedAdminPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.createAdminAccount();

        verify(userRepository).save(argThat(u ->
                "admin@carrent.com".equals(u.getEmail())
                        && "Admin".equals(u.getNom())
                        && "System".equals(u.getPrenom())
                        && u.isEnabled()
                        && "encodedAdminPwd".equals(u.getPassword())
                        && u.getRoles().contains(adminRole)
        ));
    }

    @Test
    void createAdminAccount_whenAdminAlreadyExists_updatesExistingAdmin() {
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        User existingAdmin = buildUser(1L, "admin@carrent.com", "oldEncodedPwd", ERole.ROLE_ADMIN, true);

        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.findByEmail("admin@carrent.com")).thenReturn(Optional.of(existingAdmin));
        when(passwordEncoder.encode("admin")).thenReturn("newEncodedAdminPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.createAdminAccount();

        verify(userRepository).save(argThat(u ->
                u.getId().equals(1L) && "newEncodedAdminPwd".equals(u.getPassword())
        ));
    }

    @Test
    void createAdminAccount_whenRoleDoesNotExist_createsRoleThenAdmin() {
        Role newAdminRole = new Role(ERole.ROLE_ADMIN);

        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(newAdminRole);
        when(userRepository.findByEmail("admin@carrent.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin")).thenReturn("encodedAdminPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.createAdminAccount();

        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    // =========================================================
    // register
    // =========================================================

    @Test
    void register_success_withAssureRole_accountStaysDisabled() {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@test.com")
                .password("Plain@Pwd1")
                .cin("CIN1")
                .numeroAssure("NA1")
                .telephone("99999999")
                .role(ERole.ROLE_ASSURE)
                .build();

        Role role = new Role(ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(ERole.ROLE_ASSURE)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Plain@Pwd1")).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.getMessage().contains("Inscription réussie"));
        verify(emailService).sendVerificationEmail(any(User.class));
        verify(userRepository).save(argThat(u ->
                !u.isEnabled()
                        && u.getVerificationToken() != null
                        && "encodedPwd".equals(u.getPassword())
        ));
    }

    @Test
    void register_success_withNonAssureRole() {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@test.com")
                .password("Plain@Pwd1")
                .role(ERole.ROLE_AGENT_BUREAU)
                .build();

        Role role = new Role(ERole.ROLE_AGENT_BUREAU);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(ERole.ROLE_AGENT_BUREAU)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Plain@Pwd1")).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = authService.register(request);

        assertNotNull(response);
        verify(emailService).sendVerificationEmail(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsEmailAlreadyExistsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("jean@test.com")
                .role(ERole.ROLE_ASSURE)
                .build();

        User existing = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, true);
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(existing));

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("jean@test.com"));
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any());
    }

    @Test
    void register_roleDoesNotExist_createsRoleThenSavesUser() {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@test.com")
                .password("Plain@Pwd1")
                .role(ERole.ROLE_ASSURE)
                .build();

        Role newRole = new Role(ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(ERole.ROLE_ASSURE)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(newRole);
        when(passwordEncoder.encode("Plain@Pwd1")).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = authService.register(request);

        assertNotNull(response);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void register_emailSendingFails_throwsRuntimeException() {
        RegisterRequest request = RegisterRequest.builder()
                .nom("Dupont")
                .prenom("Jean")
                .email("jean@test.com")
                .password("Plain@Pwd1")
                .role(ERole.ROLE_ASSURE)
                .build();

        Role role = new Role(ERole.ROLE_ASSURE);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(ERole.ROLE_ASSURE)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("Plain@Pwd1")).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("SMTP down")).when(emailService).sendVerificationEmail(any(User.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(request));

        assertTrue(ex.getMessage().contains("Échec de l'envoi de l'email de vérification"));
    }

    // =========================================================
    // verifyUser
    // =========================================================

    @Test
    void verifyUser_withAssureRole_activatesAccountAndClearsToken() {
        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, false);
        user.setVerificationToken("token123");

        when(userRepository.findByVerificationToken("token123")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.verifyUser("token123");

        assertTrue(user.isEnabled());
        assertNull(user.getVerificationToken());
        verify(userRepository).save(user);
    }

    @Test
    void verifyUser_withNonAssureRole_doesNotActivateButClearsToken() {
        User user = buildUser(1L, "agent@test.com", "encodedPwd", ERole.ROLE_AGENT_BUREAU, false);
        user.setVerificationToken("token456");

        when(userRepository.findByVerificationToken("token456")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.verifyUser("token456");

        assertFalse(user.isEnabled());
        assertNull(user.getVerificationToken());
        verify(userRepository).save(user);
    }

    @Test
    void verifyUser_invalidToken_throwsRuntimeException() {
        when(userRepository.findByVerificationToken("badtoken")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.verifyUser("badtoken"));

        assertTrue(ex.getMessage().contains("Token de vérification invalide"));
    }

    // =========================================================
    // login
    // =========================================================

    @Test
    void login_success_returnsAuthResponseWithRole() {
        LoginRequest request = LoginRequest.builder()
                .email("jean@test.com")
                .password("plainPwd")
                .build();

        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, true);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPwd", "encodedPwd")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("jean@test.com", response.getEmail());
        assertEquals("ROLE_ASSURE", response.getRole());
        assertEquals(1L, response.getId());
    }

    @Test
    void login_userHasNoRoles_returnsNoRole() {
        LoginRequest request = LoginRequest.builder()
                .email("jean@test.com")
                .password("plainPwd")
                .build();

        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, true);
        user.setRoles(new HashSet<>());

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPwd", "encodedPwd")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("NO_ROLE", response.getRole());
    }

    @Test
    void login_userNotFound_throwsRuntimeException() {
        LoginRequest request = LoginRequest.builder()
                .email("absent@test.com")
                .password("pwd")
                .build();

        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Utilisateur non trouvé"));
    }

    @Test
    void login_wrongPassword_throwsRuntimeException() {
        LoginRequest request = LoginRequest.builder()
                .email("jean@test.com")
                .password("wrongPwd")
                .build();

        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, true);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPwd", "encodedPwd")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Mot de passe incorrect"));
    }

    @Test
    void login_accountNotEnabled_throwsRuntimeException() {
        LoginRequest request = LoginRequest.builder()
                .email("jean@test.com")
                .password("plainPwd")
                .build();

        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, false);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPwd", "encodedPwd")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Compte non activé"));
    }

    // =========================================================
    // requestPasswordReset
    // =========================================================

    @Test
    void requestPasswordReset_success_setsTokenAndExpiryAndSendsEmail() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("jean@test.com")
                .build();

        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, true);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.requestPasswordReset(request);

        assertNotNull(user.getResetToken());
        assertNotNull(user.getResetTokenExpiry());
        assertTrue(user.getResetTokenExpiry().isAfter(Instant.now()));
        verify(emailService).sendPasswordResetEmail(eq(user), anyString());
    }

    @Test
    void requestPasswordReset_emailNotFound_throwsRuntimeException() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("absent@test.com")
                .build();

        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.requestPasswordReset(request));

        assertTrue(ex.getMessage().contains("Aucun compte associé à cet email"));
    }

    @Test
    void requestPasswordReset_emailSendingFails_throwsRuntimeException() {
        PasswordResetRequest request = PasswordResetRequest.builder()
                .email("jean@test.com")
                .build();

        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, true);

        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendPasswordResetEmail(any(User.class), anyString());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.requestPasswordReset(request));

        assertTrue(ex.getMessage().contains("Échec de l'envoi de l'email de réinitialisation"));
    }

    // =========================================================
    // resetPassword
    // =========================================================

    @Test
    void resetPassword_success_updatesPasswordAndClearsToken() {
        NewPasswordRequest request = NewPasswordRequest.builder()
                .token("validToken")
                .newPassword("newPlainPwd")
                .build();

        User user = buildUser(1L, "jean@test.com", "oldEncodedPwd", ERole.ROLE_ASSURE, true);
        user.setResetToken("validToken");
        user.setResetTokenExpiry(Instant.now().plusSeconds(3600));

        when(userRepository.findByResetToken("validToken")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPlainPwd")).thenReturn("newEncodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resetPassword(request);

        assertEquals("newEncodedPwd", user.getPassword());
        assertNull(user.getResetToken());
        assertNull(user.getResetTokenExpiry());
    }

    @Test
    void resetPassword_invalidToken_throwsRuntimeException() {
        NewPasswordRequest request = NewPasswordRequest.builder()
                .token("invalidToken")
                .newPassword("newPwd")
                .build();

        when(userRepository.findByResetToken("invalidToken")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPassword(request));

        assertTrue(ex.getMessage().contains("Token invalide ou expiré"));
    }

    @Test
    void resetPassword_nullExpiry_throwsRuntimeException() {
        NewPasswordRequest request = NewPasswordRequest.builder()
                .token("validToken")
                .newPassword("newPwd")
                .build();

        User user = buildUser(1L, "jean@test.com", "oldEncodedPwd", ERole.ROLE_ASSURE, true);
        user.setResetToken("validToken");
        user.setResetTokenExpiry(null);

        when(userRepository.findByResetToken("validToken")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPassword(request));

        assertTrue(ex.getMessage().contains("Token expiré"));
    }

    @Test
    void resetPassword_expiredToken_throwsRuntimeException() {
        NewPasswordRequest request = NewPasswordRequest.builder()
                .token("validToken")
                .newPassword("newPwd")
                .build();

        User user = buildUser(1L, "jean@test.com", "oldEncodedPwd", ERole.ROLE_ASSURE, true);
        user.setResetToken("validToken");
        user.setResetTokenExpiry(Instant.now().minusSeconds(60));

        when(userRepository.findByResetToken("validToken")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPassword(request));

        assertTrue(ex.getMessage().contains("Token expiré"));
    }

    // =========================================================
    // getUserByEmail
    // =========================================================

    @Test
    void getUserByEmail_success_returnsUser() {
        User user = buildUser(1L, "jean@test.com", "encodedPwd", ERole.ROLE_ASSURE, true);
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));

        User result = authService.getUserByEmail("jean@test.com");

        assertEquals(user, result);
    }

    @Test
    void getUserByEmail_notFound_throwsRuntimeException() {
        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.getUserByEmail("absent@test.com"));

        assertTrue(ex.getMessage().contains("Utilisateur non trouvé"));
    }
}