package com.projet.cnss.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.cnss.dto.UpdateProfileRequest;
import com.projet.cnss.dto.UserDto;
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

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    private Authentication mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    private UserDto buildUserDto(Long id, String email) {
        return UserDto.builder()
                .id(id)
                .email(email)
                .nom("Dupont")
                .prenom("Jean")
                .enabled(true)
                .roles(Set.of("ROLE_ASSURE"))
                .build();
    }

    // ==========================================================
    // GET /api/users/me
    // ==========================================================

    @Test
    void getCurrentUser_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        UserDto dto = buildUserDto(1L, "jean@test.com");

        when(userService.getCurrentUserByEmail("jean@test.com")).thenReturn(dto);

        mockMvc.perform(get("/api/users/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jean@test.com"))
                .andExpect(jsonPath("$.nom").value("Dupont"));
    }

    // ==========================================================
    // PUT /api/users/me
    // ==========================================================

    @Test
    void updateProfile_success() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNom("NouveauNom");
        request.setPrenom("NouveauPrenom");
        request.setTelephone("99999999");

        UserDto updated = buildUserDto(1L, "jean@test.com");
        updated.setNom("NouveauNom");

        when(userService.updateProfile(eq("jean@test.com"), any(UpdateProfileRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/users/me")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("NouveauNom"));
    }

    @Test
    void updateProfile_invalidRequest_missingNom_returnsBadRequest() throws Exception {
        Authentication auth = mockAuth("jean@test.com");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNom(""); // @NotBlank violé
        request.setPrenom("Prenom");

        mockMvc.perform(put("/api/users/me")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================
    // GET /api/users/{id}
    // ==========================================================

    @Test
    void getUserById_success() throws Exception {
        UserDto dto = buildUserDto(1L, "jean@test.com");
        when(userService.getUserById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ==========================================================
    // GET /api/users
    // ==========================================================

    @Test
    void getAllUsers_success() throws Exception {
        List<UserDto> users = List.of(buildUserDto(1L, "u1@test.com"), buildUserDto(2L, "u2@test.com"));
        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==========================================================
    // GET /api/users/email/{email}
    // ==========================================================

    @Test
    void getUserByEmail_success() throws Exception {
        UserDto dto = buildUserDto(1L, "jean@test.com");
        when(userService.getCurrentUserByEmail("jean@test.com")).thenReturn(dto);

        mockMvc.perform(get("/api/users/email/jean@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jean@test.com"));
    }
}