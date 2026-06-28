package com.projet.cnss.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.cnss.dto.ChangePasswordRequest;
import com.projet.cnss.dto.CreateAgentRequest;
import com.projet.cnss.dto.UpdateAgentRequest;
import com.projet.cnss.dto.UserDto;
import com.projet.cnss.services.user.AgentService;
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

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock
    private AgentService agentService;

    @InjectMocks
    private AgentController agentController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(agentController).build();

    }

    private Authentication mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    private UserDto buildAgentDto(Long id, String role) {
        return UserDto.builder()
                .id(id)
                .nom("Dupont")
                .prenom("Jean")
                .email("agent@test.com")
                .enabled(true)
                .roles(Set.of(role))
                .build();
    }

    // ==========================================================
    // POST /api/admin/agents/create
    // ==========================================================

    @Test
    void createAgent_withAgentTypeParam_success() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        CreateAgentRequest request = CreateAgentRequest.builder()
                .nom("Dupont").prenom("Jean").email("agent@test.com")
                .cin("CIN1").numeroAssure("NA1").telephone("99999999")
                .password("Plain@Pwd1")
                .build();

        UserDto created = buildAgentDto(1L, "ROLE_AGENT_BUREAU");

        when(agentService.createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("BUREAU")))
                .thenReturn(created);

        mockMvc.perform(post("/api/admin/agents/create")
                        .param("agentType", "BUREAU")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createAgent_withoutAgentTypeParam_deducedFromRoleInBody() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        CreateAgentRequest request = CreateAgentRequest.builder()
                .nom("Dupont").prenom("Jean").email("agent@test.com")
                .cin("CIN1").numeroAssure("NA1").telephone("99999999")
                .password("Plain@Pwd1")
                .role("ROLE_AGENT_DIRECTION")
                .build();

        UserDto created = buildAgentDto(1L, "ROLE_AGENT_DIRECTION");

        when(agentService.createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("DIRECTION")))
                .thenReturn(created);

        mockMvc.perform(post("/api/admin/agents/create")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(agentService).createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("DIRECTION"));
    }

    @Test
    void createAgent_withoutAgentTypeAndRole_defaultsToCnss() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        CreateAgentRequest request = CreateAgentRequest.builder()
                .nom("Dupont").prenom("Jean").email("agent@test.com")
                .cin("CIN1").numeroAssure("NA1").telephone("99999999")
                .password("Plain@Pwd1")
                .build();

        UserDto created = buildAgentDto(1L, "ROLE_AGENT_CNSS");

        when(agentService.createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("CNSS")))
                .thenReturn(created);

        mockMvc.perform(post("/api/admin/agents/create")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(agentService).createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("CNSS"));
    }

    @Test
    void createAgent_blankAgentTypeParam_deducedFromRoleAdmin() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        CreateAgentRequest request = CreateAgentRequest.builder()
                .nom("Dupont").prenom("Jean").email("agent@test.com")
                .cin("CIN1").numeroAssure("NA1").telephone("99999999")
                .password("Plain@Pwd1")
                .role("ROLE_ADMIN")
                .build();

        UserDto created = buildAgentDto(1L, "ROLE_ADMIN");

        when(agentService.createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("ADMIN")))
                .thenReturn(created);

        mockMvc.perform(post("/api/admin/agents/create")
                        .param("agentType", "")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(agentService).createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("ADMIN"));
    }

    @Test
    void createAgent_unknownRoleInBody_defaultsToCnss() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        CreateAgentRequest request = CreateAgentRequest.builder()
                .nom("Dupont").prenom("Jean").email("agent@test.com")
                .cin("CIN1").numeroAssure("NA1").telephone("99999999")
                .password("Plain@Pwd1")
                .role("ROLE_INCONNU")
                .build();

        UserDto created = buildAgentDto(1L, "ROLE_AGENT_CNSS");

        when(agentService.createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("CNSS")))
                .thenReturn(created);

        mockMvc.perform(post("/api/admin/agents/create")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(agentService).createAgent(any(CreateAgentRequest.class), eq("admin@test.com"), eq("CNSS"));
    }
    // ==========================================================
    // GET /api/admin/agents/{id}
    // ==========================================================

    @Test
    void getAgentById_success() throws Exception {
        UserDto dto = buildAgentDto(1L, "ROLE_AGENT_BUREAU");
        when(agentService.getAgentById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/agents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ==========================================================
    // GET /api/admin/agents/active
    // ==========================================================

    @Test
    void getActiveAgents_success() throws Exception {
        when(agentService.getActiveAgents()).thenReturn(List.of(buildAgentDto(1L, "ROLE_AGENT_BUREAU")));

        mockMvc.perform(get("/api/admin/agents/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // GET /api/admin/agents
    // ==========================================================

    @Test
    void getAllAgents_success() throws Exception {
        when(agentService.getAllAgents()).thenReturn(List.of(
                buildAgentDto(1L, "ROLE_AGENT_BUREAU"), buildAgentDto(2L, "ROLE_AGENT_DIRECTION")));

        mockMvc.perform(get("/api/admin/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==========================================================
    // GET /api/admin/agents/bureau
    // ==========================================================

    @Test
    void getAgentsBureau_success() throws Exception {
        when(agentService.getAgentsBureau()).thenReturn(List.of(buildAgentDto(1L, "ROLE_AGENT_BUREAU")));

        mockMvc.perform(get("/api/admin/agents/bureau"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // GET /api/admin/agents/direction
    // ==========================================================

    @Test
    void getAgentsDirection_success() throws Exception {
        when(agentService.getAgentsDirection()).thenReturn(List.of(buildAgentDto(1L, "ROLE_AGENT_DIRECTION")));

        mockMvc.perform(get("/api/admin/agents/direction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================================
    // PUT /api/admin/agents/{id}
    // ==========================================================

    @Test
    void updateAgent_success() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        UpdateAgentRequest request = new UpdateAgentRequest();
        request.setNom("NouveauNom");
        request.setPrenom("NouveauPrenom");
        request.setEmail("agent@test.com");
        request.setCin("CIN1");
        request.setNumeroAssure("NA1");
        request.setTelephone("99999999");

        UserDto updated = buildAgentDto(1L, "ROLE_AGENT_BUREAU");

        when(agentService.updateAgent(eq(1L), any(UpdateAgentRequest.class), eq("admin@test.com")))
                .thenReturn(updated);

        mockMvc.perform(put("/api/admin/agents/1")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ==========================================================
    // PATCH /api/admin/agents/{id}/deactivate
    // ==========================================================

    @Test
    void deactivateAgent_success() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        UserDto deactivated = buildAgentDto(1L, "ROLE_AGENT_BUREAU");
        deactivated.setEnabled(false);

        when(agentService.deactivateAgent(1L, "admin@test.com")).thenReturn(deactivated);

        mockMvc.perform(patch("/api/admin/agents/1/deactivate").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    // ==========================================================
    // PATCH /api/admin/agents/{id}/reactivate
    // ==========================================================

    @Test
    void reactivateAgent_success() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        UserDto reactivated = buildAgentDto(1L, "ROLE_AGENT_BUREAU");
        reactivated.setEnabled(true);

        when(agentService.reactivateAgent(1L, "admin@test.com")).thenReturn(reactivated);

        mockMvc.perform(patch("/api/admin/agents/1/reactivate").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    // ==========================================================
    // PATCH /api/admin/agents/{id}/change-password
    // ==========================================================

    @Test
    void changeAgentPassword_success() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("NewPlain@Pwd1")
                .build();

        doNothing().when(agentService).changeAgentPassword(eq(1L), any(ChangePasswordRequest.class), eq("admin@test.com"));

        mockMvc.perform(patch("/api/admin/agents/1/change-password")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe modifié avec succès"));
    }

    @Test
    void changeAgentPassword_invalidPassword_returnsBadRequest() throws Exception {
        Authentication auth = mockAuth("admin@test.com");
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .newPassword("short") // ne respecte pas le pattern @Pattern
                .build();

        mockMvc.perform(patch("/api/admin/agents/1/change-password")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}