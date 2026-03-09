package com.projet.cnss.services;

import com.projet.cnss.config.JwtService;
import com.projet.cnss.dto.*;
import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.entity.User;
import com.projet.cnss.exception.EmailAlreadyExistsException;
;
import com.projet.cnss.mail.EmailService;
import com.projet.cnss.repository.RoleRepository;
import com.projet.cnss.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Duration PASSWORD_RESET_EXPIRATION = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    /**
     * Création automatique du compte ADMIN si inexistant
     */
    @PostConstruct
    public void createAdminAccount() {

        String adminEmail = "admin@carrent.com";

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));

        User admin = userRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            admin = new User();
            admin.setEmail(adminEmail);
        }

        admin.setNom("Admin");
        admin.setPrenom("System");
        admin.setRoles(Set.of(adminRole));
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setEnabled(true);

        userRepository.save(admin);

        System.out.println("✅ Admin créé / mis à jour");
    }

    /**
     * Inscription d’un utilisateur (CLIENT ou AGENCE)
     */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException("L'email " + request.getEmail() + " est déjà utilisé");
                });

        Role role = roleRepository.findByName(request.getRole())
                .orElseGet(() -> roleRepository.save(new Role(request.getRole())));

        User user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .cin(request.getCin())
                .numeroAssure(request.getNumeroAssure())
                .roles(Set.of(role))
                .enabled(false) // 🚨 pas activé par défaut
                .verificationToken(UUID.randomUUID().toString())
                .build();

        // 🚨 Seuls les CLIENTS sont activés après vérification email
        // 🚨 Les AGENCES doivent être validées par un ADMIN
        if (request.getRole() == ERole.ROLE_ASSURE) {
            user.setEnabled(false); // email obligatoire
        }

        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user);
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'envoi de l'email de vérification", e);
        }

        return RegistrationResponse.builder()
                .message("Inscription réussie. Vérifiez votre email.")
                .build();
    }

    /**
     * Vérification par token (CLIENT uniquement)
     */
    @Transactional
    public void verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token de vérification invalide"));

        if (user.getRoles().stream().anyMatch(r -> r.getName() == ERole.ROLE_ASSURE)) {
            user.setEnabled(true); // client activé par email
        }

        user.setVerificationToken(null);
        userRepository.save(user);
    }





    /**
     * Connexion utilisateur
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Compte non activé. Veuillez vérifier votre email ou attendre validation admin.");
        }

        String firstRole = user.getRoles().stream()
                .findFirst()
                .map(r -> r.getName().name())
                .orElse("NO_ROLE");

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .email(user.getEmail())
                .role(firstRole)
                .id(user.getId())
                .build();
    }

    /**
     * Demande de reset password
     */
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Aucun compte associé à cet email"));

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(Instant.now().plus(PASSWORD_RESET_EXPIRATION));
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(user, resetToken);
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'envoi de l'email de réinitialisation", e);
        }
    }

    /**
     * Reset password
     */
    @Transactional
    public void resetPassword(NewPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new RuntimeException("Token expiré");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }






}
