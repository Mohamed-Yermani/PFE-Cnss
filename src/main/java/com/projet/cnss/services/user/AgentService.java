package com.projet.cnss.services.user;

import com.projet.cnss.dto.ChangePasswordRequest;
import com.projet.cnss.dto.CreateAgentRequest;
import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.entity.User;
import com.projet.cnss.exception.EmailAlreadyExistsException;
import com.projet.cnss.repository.UserRepository;
import com.projet.cnss.services.AuditLogService;
import com.projet.cnss.dto.UpdateAgentRequest;
import com.projet.cnss.dto.UserDto;
import com.projet.cnss.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * Création d'un agent par l'administrateur
     * agentType : "BUREAU" ou "DIRECTION"
     */
    @Transactional
    public UserDto createAgent(CreateAgentRequest request, String adminEmail, String agentType) {

        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + request.getEmail());
        }

        // ✅ Mapping agentType → ERole (couvre tous les cas)
        ERole eRole = switch (agentType.toUpperCase().trim()) {
            case "CNSS"      -> ERole.ROLE_AGENT_CNSS;
            case "BUREAU"    -> ERole.ROLE_AGENT_BUREAU;
            case "DIRECTION" -> ERole.ROLE_AGENT_DIRECTION;
            case "ADMIN"     -> ERole.ROLE_ADMIN;
            default          -> ERole.ROLE_AGENT_CNSS; // fallback sécurisé
        };

        // Récupérer le rôle depuis la base
        Role role = roleRepository.findByName(eRole)
                .orElseThrow(() -> new RuntimeException(
                        "Rôle introuvable en base : " + eRole.name() +
                                " — vérifie que la table 'roles' est bien initialisée"
                ));

        // Créer l'utilisateur
        User agent = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .cin(request.getCin())
                .numeroAssure(request.getNumeroAssure())
                .telephone(request.getTelephone())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(new java.util.HashSet<>(Set.of(role))) // ✅ mutable
                .enabled(true)
                .build();

        User saved = userRepository.save(agent);
        return mapToDto(saved);
    }

    /**
     * Vérifier si un user est agent (bureau ou direction)
     */
    private boolean isAgent(User user) {
        return user.getRoles().stream()
                .anyMatch(role ->
                        role.getName() == ERole.ROLE_AGENT_BUREAU ||
                                role.getName() == ERole.ROLE_AGENT_DIRECTION ||
                                role.getName() == ERole.ROLE_AGENT_CNSS
                );
    }

    /**
     * Récupérer un agent par son ID
     */
    public UserDto getAgentById(Long agentId) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent avec l'ID " + agentId + " introuvable"));

        if (!isAgent(agent)) {
            throw new RuntimeException("L'utilisateur avec l'ID " + agentId + " n'est pas un agent");
        }

        return mapToDto(agent);
    }

    /**
     * Récupérer tous les agents actifs
     */
    public List<UserDto> getActiveAgents() {
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(this::isAgent)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les agents bureau
     */
    public List<UserDto> getAgentsBureau() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == ERole.ROLE_AGENT_BUREAU))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les agents direction
     */
    public List<UserDto> getAgentsDirection() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == ERole.ROLE_AGENT_DIRECTION))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les agents (actifs et inactifs)
     */
    public List<UserDto> getAllAgents() {
        return userRepository.findAll().stream()
                .filter(this::isAgent)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Mettre à jour les informations d'un agent
     */
    @Transactional
    public UserDto updateAgent(Long id, UpdateAgentRequest request, String adminEmail) {

        User agent = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent introuvable : " + id));

        agent.setNom(request.getNom());
        agent.setPrenom(request.getPrenom());
        agent.setEmail(request.getEmail());
        agent.setCin(request.getCin());
        agent.setNumeroAssure(request.getNumeroAssure());
        agent.setTelephone(request.getTelephone());

        // ✅ Mettre à jour le password SEULEMENT s'il est fourni et non vide
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            agent.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // ✅ Mettre à jour le rôle si fourni
        // Dans updateAgent() :
        if (request.getRole() != null && !request.getRole().isBlank()) {
            ERole eRole = switch (request.getRole().toUpperCase().trim()) {
                case "ROLE_AGENT_CNSS"      -> ERole.ROLE_AGENT_CNSS;
                case "ROLE_AGENT_BUREAU"    -> ERole.ROLE_AGENT_BUREAU;
                case "ROLE_AGENT_DIRECTION" -> ERole.ROLE_AGENT_DIRECTION;
                case "ROLE_ADMIN"           -> ERole.ROLE_ADMIN;
                default                     -> ERole.ROLE_AGENT_CNSS;
            };

            Role role = roleRepository.findByName(eRole)
                    .orElseThrow(() -> new RuntimeException("Rôle introuvable : " + eRole));

            // ✅ Mieux : vider et repeupler la collection existante plutôt que la remplacer
            agent.getRoles().clear();
            agent.getRoles().add(role);
        }

        User saved = userRepository.save(agent);
        return mapToDto(saved);
    }

    /**
     * Désactiver un agent
     */
    @Transactional
    public UserDto deactivateAgent(Long agentId, String adminEmail) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent avec l'ID " + agentId + " introuvable"));

        if (!isAgent(agent)) {
            throw new RuntimeException("L'utilisateur avec l'ID " + agentId + " n'est pas un agent");
        }

        if (!agent.isEnabled()) {
            throw new RuntimeException("L'agent est déjà désactivé");
        }

        agent.setEnabled(false);
        userRepository.save(agent);

        auditLogService.log(
                "DEACTIVATE_AGENT",
                "Admin " + adminEmail + " a désactivé l'agent " + agent.getEmail()
        );

        return mapToDto(agent);
    }

    /**
     * Réactiver un agent
     */
    @Transactional
    public UserDto reactivateAgent(Long agentId, String adminEmail) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent avec l'ID " + agentId + " introuvable"));

        if (!isAgent(agent)) {
            throw new RuntimeException("L'utilisateur avec l'ID " + agentId + " n'est pas un agent");
        }

        if (agent.isEnabled()) {
            throw new RuntimeException("L'agent est déjà actif");
        }

        agent.setEnabled(true);
        userRepository.save(agent);

        auditLogService.log(
                "REACTIVATE_AGENT",
                "Admin " + adminEmail + " a réactivé l'agent " + agent.getEmail()
        );

        return mapToDto(agent);
    }

    /**
     * Changer le mot de passe d'un agent
     */
    @Transactional
    public void changeAgentPassword(Long agentId, ChangePasswordRequest request, String adminEmail) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent avec l'ID " + agentId + " introuvable"));

        if (!isAgent(agent)) {
            throw new RuntimeException("L'utilisateur avec l'ID " + agentId + " n'est pas un agent");
        }

        agent.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(agent);

        auditLogService.log(
                "CHANGE_AGENT_PASSWORD",
                "Admin " + adminEmail + " a changé le mot de passe de l'agent " + agent.getEmail()
        );
    }

    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .numeroAssure(user.getNumeroAssure())
                .cin(user.getCin())
                .telephone(user.getTelephone())
                .enabled(user.isEnabled())
                .roles(user.getRoles() == null ? null :
                        user.getRoles().stream()
                                .map(role -> role.getName().name())
                                .collect(Collectors.toSet()))
                .build();
    }
}