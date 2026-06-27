package com.projet.cnss.controller;

import com.projet.cnss.dto.ChangePasswordRequest;
import com.projet.cnss.dto.CreateAgentRequest;
import com.projet.cnss.dto.UpdateAgentRequest;
import com.projet.cnss.dto.UserDto;
import com.projet.cnss.services.user.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /**
     * Créer un agent BUREAU ou DIRECTION (réservé à l'admin)
     * agentType : "BUREAU" ou "DIRECTION"
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> createAgent(
            @RequestBody @Valid CreateAgentRequest request,
            @RequestParam(required = false) String agentType,
            Authentication authentication) {

        String adminEmail = authentication.getName();

        // Déduire agentType depuis le role si non fourni en query param
        if (agentType == null || agentType.isBlank()) {
            String role = request.getRole(); // ← on lit le role depuis le body
            agentType = switch (role != null ? role : "") {
                case "ROLE_AGENT_CNSS"      -> "CNSS";
                case "ROLE_AGENT_BUREAU"    -> "BUREAU";
                case "ROLE_AGENT_DIRECTION" -> "DIRECTION";
                case "ROLE_ADMIN"           -> "ADMIN";
                default                     -> "CNSS";
            };
        }

        UserDto createdAgent = agentService.createAgent(request, adminEmail, agentType);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAgent);
    }

    /**
     * Récupérer un agent par son ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> getAgentById(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    /**
     * Récupérer tous les agents actifs
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getActiveAgents() {
        return ResponseEntity.ok(agentService.getActiveAgents());
    }

    /**
     * Récupérer tous les agents (actifs et inactifs)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllAgents() {
        return ResponseEntity.ok(agentService.getAllAgents());
    }

    /**
     * Récupérer tous les agents bureau
     */
    @GetMapping("/bureau")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAgentsBureau() {
        return ResponseEntity.ok(agentService.getAgentsBureau());
    }

    /**
     * Récupérer tous les agents direction
     */
    @GetMapping("/direction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAgentsDirection() {
        return ResponseEntity.ok(agentService.getAgentsDirection());
    }

    /**
     * Mettre à jour un agent
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateAgent(
            @PathVariable Long id,
            @RequestBody @Valid UpdateAgentRequest request,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        return ResponseEntity.ok(agentService.updateAgent(id, request, adminEmail));
    }

    /**
     * Désactiver un agent
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> deactivateAgent(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(agentService.deactivateAgent(id, authentication.getName()));
    }

    /**
     * Réactiver un agent
     */
    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> reactivateAgent(
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(agentService.reactivateAgent(id, authentication.getName()));
    }

    /**
     * Changer le mot de passe d'un agent
     */
    @PatchMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> changeAgentPassword(
            @PathVariable Long id,
            @RequestBody @Valid ChangePasswordRequest request,
            Authentication authentication) {

        agentService.changeAgentPassword(id, request, authentication.getName());
        return ResponseEntity.ok("Mot de passe modifié avec succès");
    }
}