package com.projet.cnss.services.user;

import com.projet.cnss.dto.ChangePasswordRequest;
import com.projet.cnss.dto.CreateAgentRequest;
import com.projet.cnss.dto.UpdateAgentRequest;
import com.projet.cnss.dto.UserDto;
import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.RoleRepository;
import com.projet.cnss.repository.UserRepository;
import com.projet.cnss.services.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AgentService agentService;

    private Role roleCnss;
    private Role roleBureau;
    private Role roleDirection;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        roleCnss = new Role(ERole.ROLE_AGENT_CNSS);
        roleBureau = new Role(ERole.ROLE_AGENT_BUREAU);
        roleDirection = new Role(ERole.ROLE_AGENT_DIRECTION);
        roleAdmin = new Role(ERole.ROLE_ADMIN);
    }

    private User buildAgent(Long id, ERole eRole, boolean enabled) {
        Role role = new Role(eRole);
        User user = new User();
        user.setId(id);
        user.setNom("Nom");
        user.setPrenom("Prenom");
        user.setEmail("agent" + id + "@test.com");
        user.setCin("CIN" + id);
        user.setNumeroAssure("NA" + id);
        user.setTelephone("12345678");
        user.setPassword("encodedPwd");
        user.setEnabled(enabled);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    private CreateAgentRequest buildCreateRequest() {
        CreateAgentRequest request = new CreateAgentRequest();
        request.setNom("Dupont");
        request.setPrenom("Jean");
        request.setEmail("jean.dupont@test.com");
        request.setCin("CIN123");
        request.setNumeroAssure("NA123");
        request.setTelephone("99999999");
        request.setPassword("plainPwd");
        return request;
    }

    // ---------------------- createAgent ----------------------

    @Test
    void createAgent_success_bureau() {
        CreateAgentRequest request = buildCreateRequest();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_AGENT_BUREAU)).thenReturn(Optional.of(roleBureau));
        when(passwordEncoder.encode("plainPwd")).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserDto result = agentService.createAgent(request, "admin@test.com", "BUREAU");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Dupont", result.getNom());
        assertEquals("jean.dupont@test.com", result.getEmail());
        assertTrue(result.isEnabled());
        assertTrue(result.getRoles().contains("ROLE_AGENT_BUREAU"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAgent_success_direction() {
        CreateAgentRequest request = buildCreateRequest();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_AGENT_DIRECTION)).thenReturn(Optional.of(roleDirection));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.createAgent(request, "admin@test.com", "direction");

        assertTrue(result.getRoles().contains("ROLE_AGENT_DIRECTION"));
    }

    @Test
    void createAgent_success_cnss() {
        CreateAgentRequest request = buildCreateRequest();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_AGENT_CNSS)).thenReturn(Optional.of(roleCnss));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.createAgent(request, "admin@test.com", "CNSS");

        assertTrue(result.getRoles().contains("ROLE_AGENT_CNSS"));
    }

    @Test
    void createAgent_success_admin() {
        CreateAgentRequest request = buildCreateRequest();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_ADMIN)).thenReturn(Optional.of(roleAdmin));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.createAgent(request, "admin@test.com", "ADMIN");

        assertTrue(result.getRoles().contains("ROLE_ADMIN"));
    }

    @Test
    void createAgent_success_unknownType_fallbackToCnss() {
        CreateAgentRequest request = buildCreateRequest();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_AGENT_CNSS)).thenReturn(Optional.of(roleCnss));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.createAgent(request, "admin@test.com", "UNKNOWN_TYPE");

        assertTrue(result.getRoles().contains("ROLE_AGENT_CNSS"));
    }

    @Test
    void createAgent_emailAlreadyExists_throwsException() {
        CreateAgentRequest request = buildCreateRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.createAgent(request, "admin@test.com", "BUREAU"));

        assertTrue(ex.getMessage().contains("Email déjà utilisé"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createAgent_roleNotFoundInDb_throwsException() {
        CreateAgentRequest request = buildCreateRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_AGENT_BUREAU)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.createAgent(request, "admin@test.com", "BUREAU"));

        assertTrue(ex.getMessage().contains("Rôle introuvable"));
        verify(userRepository, never()).save(any());
    }

    // ---------------------- getAgentById ----------------------

    @Test
    void getAgentById_success() {
        User agent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));

        UserDto result = agentService.getAgentById(1L);

        assertEquals(1L, result.getId());
        assertTrue(result.getRoles().contains("ROLE_AGENT_BUREAU"));
    }

    @Test
    void getAgentById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.getAgentById(99L));

        assertTrue(ex.getMessage().contains("introuvable"));
    }

    @Test
    void getAgentById_userIsNotAgent_throwsException() {
        User notAgent = buildAgent(2L, ERole.ROLE_ASSURE, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(notAgent));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.getAgentById(2L));

        assertTrue(ex.getMessage().contains("n'est pas un agent"));
    }

    // ---------------------- getActiveAgents ----------------------

    @Test
    void getActiveAgents_returnsOnlyEnabledAgents() {
        User activeAgent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        User inactiveAgent = buildAgent(2L, ERole.ROLE_AGENT_DIRECTION, false);
        User notAgentEnabled = buildAgent(3L, ERole.ROLE_ASSURE, true);

        when(userRepository.findAll()).thenReturn(List.of(activeAgent, inactiveAgent, notAgentEnabled));

        List<UserDto> result = agentService.getActiveAgents();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getActiveAgents_emptyList() {
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        List<UserDto> result = agentService.getActiveAgents();

        assertTrue(result.isEmpty());
    }

    // ---------------------- getAgentsBureau ----------------------

    @Test
    void getAgentsBureau_returnsOnlyBureauAgents() {
        User bureauAgent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        User directionAgent = buildAgent(2L, ERole.ROLE_AGENT_DIRECTION, true);

        when(userRepository.findAll()).thenReturn(List.of(bureauAgent, directionAgent));

        List<UserDto> result = agentService.getAgentsBureau();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getRoles().contains("ROLE_AGENT_BUREAU"));
    }

    // ---------------------- getAgentsDirection ----------------------

    @Test
    void getAgentsDirection_returnsOnlyDirectionAgents() {
        User bureauAgent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        User directionAgent = buildAgent(2L, ERole.ROLE_AGENT_DIRECTION, true);

        when(userRepository.findAll()).thenReturn(List.of(bureauAgent, directionAgent));

        List<UserDto> result = agentService.getAgentsDirection();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getRoles().contains("ROLE_AGENT_DIRECTION"));
    }

    // ---------------------- getAllAgents ----------------------

    @Test
    void getAllAgents_returnsAllAgentsRegardlessOfStatus() {
        User activeAgent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        User inactiveAgent = buildAgent(2L, ERole.ROLE_AGENT_DIRECTION, false);
        User notAgent = buildAgent(3L, ERole.ROLE_ASSURE, true);

        when(userRepository.findAll()).thenReturn(List.of(activeAgent, inactiveAgent, notAgent));

        List<UserDto> result = agentService.getAllAgents();

        assertEquals(2, result.size());
    }

    // ---------------------- updateAgent ----------------------

    @Test
    void updateAgent_success_withoutPasswordAndRoleChange() {
        User existing = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setNom("NouveauNom");
        request.setPrenom("NouveauPrenom");
        request.setEmail("nouveau@test.com");
        request.setCin("CIN999");
        request.setNumeroAssure("NA999");
        request.setTelephone("00000000");
        request.setPassword(null);
        request.setRole(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.updateAgent(1L, request, "admin@test.com");

        assertEquals("NouveauNom", result.getNom());
        assertEquals("nouveau@test.com", result.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateAgent_success_withBlankPassword_doesNotChangePassword() {
        User existing = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setNom("Nom");
        request.setPrenom("Prenom");
        request.setEmail("agent1@test.com");
        request.setCin("CIN1");
        request.setNumeroAssure("NA1");
        request.setTelephone("12345678");
        request.setPassword("   ");
        request.setRole(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        agentService.updateAgent(1L, request, "admin@test.com");

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateAgent_success_withNewPassword() {
        User existing = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setNom("Nom");
        request.setPrenom("Prenom");
        request.setEmail("agent1@test.com");
        request.setCin("CIN1");
        request.setNumeroAssure("NA1");
        request.setTelephone("12345678");
        request.setPassword("newPlainPwd");
        request.setRole(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPlainPwd")).thenReturn("newEncodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        agentService.updateAgent(1L, request, "admin@test.com");

        verify(passwordEncoder).encode("newPlainPwd");
    }

    @Test
    void updateAgent_success_withRoleChange() {
        User existing = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setNom("Nom");
        request.setPrenom("Prenom");
        request.setEmail("agent1@test.com");
        request.setCin("CIN1");
        request.setNumeroAssure("NA1");
        request.setTelephone("12345678");
        request.setPassword(null);
        request.setRole("ROLE_AGENT_DIRECTION");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByName(ERole.ROLE_AGENT_DIRECTION)).thenReturn(Optional.of(roleDirection));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.updateAgent(1L, request, "admin@test.com");

        assertTrue(result.getRoles().contains("ROLE_AGENT_DIRECTION"));
        assertFalse(result.getRoles().contains("ROLE_AGENT_BUREAU"));
    }

    @Test
    void updateAgent_success_withUnknownRoleString_fallbackToCnss() {
        User existing = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setNom("Nom");
        request.setPrenom("Prenom");
        request.setEmail("agent1@test.com");
        request.setCin("CIN1");
        request.setNumeroAssure("NA1");
        request.setTelephone("12345678");
        request.setPassword(null);
        request.setRole("ROLE_UNKNOWN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByName(ERole.ROLE_AGENT_CNSS)).thenReturn(Optional.of(roleCnss));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.updateAgent(1L, request, "admin@test.com");

        assertTrue(result.getRoles().contains("ROLE_AGENT_CNSS"));
    }

    @Test
    void updateAgent_roleNotFoundInDb_throwsException() {
        User existing = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setNom("Nom");
        request.setPrenom("Prenom");
        request.setEmail("agent1@test.com");
        request.setCin("CIN1");
        request.setNumeroAssure("NA1");
        request.setTelephone("12345678");
        request.setRole("ROLE_AGENT_DIRECTION");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByName(ERole.ROLE_AGENT_DIRECTION)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> agentService.updateAgent(1L, request, "admin@test.com"));
    }

    @Test
    void updateAgent_notFound_throwsException() {
        UpdateAgentRequest request = new UpdateAgentRequest();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.updateAgent(99L, request, "admin@test.com"));

        assertTrue(ex.getMessage().contains("Agent introuvable"));
    }

    // ---------------------- deactivateAgent ----------------------

    @Test
    void deactivateAgent_success() {
        User agent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.deactivateAgent(1L, "admin@test.com");

        assertFalse(result.isEnabled());
        verify(auditLogService).log(eq("DEACTIVATE_AGENT"), anyString());
    }

    @Test
    void deactivateAgent_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> agentService.deactivateAgent(99L, "admin@test.com"));
    }

    @Test
    void deactivateAgent_userIsNotAgent_throwsException() {
        User notAgent = buildAgent(2L, ERole.ROLE_ASSURE, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(notAgent));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.deactivateAgent(2L, "admin@test.com"));

        assertTrue(ex.getMessage().contains("n'est pas un agent"));
    }

    @Test
    void deactivateAgent_alreadyDeactivated_throwsException() {
        User agent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.deactivateAgent(1L, "admin@test.com"));

        assertTrue(ex.getMessage().contains("déjà désactivé"));
    }

    // ---------------------- reactivateAgent ----------------------

    @Test
    void reactivateAgent_success() {
        User agent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = agentService.reactivateAgent(1L, "admin@test.com");

        assertTrue(result.isEnabled());
        verify(auditLogService).log(eq("REACTIVATE_AGENT"), anyString());
    }

    @Test
    void reactivateAgent_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> agentService.reactivateAgent(99L, "admin@test.com"));
    }

    @Test
    void reactivateAgent_userIsNotAgent_throwsException() {
        User notAgent = buildAgent(2L, ERole.ROLE_ASSURE, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(notAgent));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.reactivateAgent(2L, "admin@test.com"));

        assertTrue(ex.getMessage().contains("n'est pas un agent"));
    }

    @Test
    void reactivateAgent_alreadyActive_throwsException() {
        User agent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.reactivateAgent(1L, "admin@test.com"));

        assertTrue(ex.getMessage().contains("déjà actif"));
    }

    // ---------------------- changeAgentPassword ----------------------

    @Test
    void changeAgentPassword_success() {
        User agent = buildAgent(1L, ERole.ROLE_AGENT_BUREAU, true);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newPlainPwd");

        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(passwordEncoder.encode("newPlainPwd")).thenReturn("newEncodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        agentService.changeAgentPassword(1L, request, "admin@test.com");

        assertEquals("newEncodedPwd", agent.getPassword());
        verify(auditLogService).log(eq("CHANGE_AGENT_PASSWORD"), anyString());
    }

    @Test
    void changeAgentPassword_notFound_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newPwd");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> agentService.changeAgentPassword(99L, request, "admin@test.com"));
    }

    @Test
    void changeAgentPassword_userIsNotAgent_throwsException() {
        User notAgent = buildAgent(2L, ERole.ROLE_ASSURE, true);
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("newPwd");

        when(userRepository.findById(2L)).thenReturn(Optional.of(notAgent));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> agentService.changeAgentPassword(2L, request, "admin@test.com"));

        assertTrue(ex.getMessage().contains("n'est pas un agent"));
    }
}