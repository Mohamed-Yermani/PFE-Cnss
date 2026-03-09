package com.projet.cnss.controller;

import com.projet.cnss.dto.*;
import com.projet.cnss.entity.AuditLog;
import com.projet.cnss.services.AuditLogService;
import com.projet.cnss.services.AuthService;
import com.projet.cnss.services.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    // 🔹 Inscription (CLIENT ou AGENCE)
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody @Valid RegisterRequest request) {
        RegistrationResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    // 🔹 Vérification email (CLIENT)
    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        authService.verifyUser(token);
        return ResponseEntity.ok("Compte vérifié avec succès !");
    }

    // 🔹 Connexion
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }


    // 🔹 Mot de passe oublié
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok("Email de réinitialisation envoyé");
    }

    // 🔹 Réinitialiser le mot de passe
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody NewPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Mot de passe réinitialisé avec succès");
    }

    // 🔹 Récupération de l’utilisateur courant
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserDto userDto = userService.getCurrentUserByEmail(email);
        return ResponseEntity.ok(userDto);
    }


    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }


}
