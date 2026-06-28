package com.projet.cnss.services.user;

import com.projet.cnss.dto.UpdateProfileRequest;
import com.projet.cnss.dto.UserDto;
import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User buildUser(Long id, String email, ERole eRole, String cin, String numeroAssure) {
        Role role = new Role(eRole);
        User user = new User();
        user.setId(id);
        user.setNom("Nom");
        user.setPrenom("Prenom");
        user.setEmail(email);
        user.setCin(cin);
        user.setNumeroAssure(numeroAssure);
        user.setTelephone("12345678");
        user.setPassword("encodedPwd");
        user.setEnabled(true);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    // ---------------------- getCurrentUserByEmail ----------------------

    @Test
    void getCurrentUserByEmail_success() {
        User user = buildUser(1L, "test@test.com", ERole.ROLE_ASSURE, "CIN1", "NA1");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDto result = userService.getCurrentUserByEmail("test@test.com");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
        assertEquals("CIN1", result.getCin());
        assertEquals("NA1", result.getNumeroAssure());
        assertTrue(result.getRoles().contains("ROLE_ASSURE"));
    }

    @Test
    void getCurrentUserByEmail_withNullRoles() {
        User user = buildUser(1L, "test@test.com", ERole.ROLE_ASSURE, "CIN1", "NA1");
        user.setRoles(null);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDto result = userService.getCurrentUserByEmail("test@test.com");

        assertNull(result.getRoles());
    }

    @Test
    void getCurrentUserByEmail_notFound_throwsException() {
        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getCurrentUserByEmail("absent@test.com"));

        assertTrue(ex.getMessage().contains("Utilisateur non trouvé"));
    }

    // ---------------------- updateProfile ----------------------

    @Test
    void updateProfile_success() {
        User user = buildUser(1L, "test@test.com", ERole.ROLE_ASSURE, "CIN1", "NA1");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNom("NouveauNom");
        request.setPrenom("NouveauPrenom");
        request.setTelephone("99999999");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto result = userService.updateProfile("test@test.com", request);

        assertEquals("NouveauNom", result.getNom());
        assertEquals("NouveauPrenom", result.getPrenom());
        assertEquals("99999999", result.getTelephone());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_notFound_throwsException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateProfile("absent@test.com", request));

        assertTrue(ex.getMessage().contains("Utilisateur non trouvé"));
        verify(userRepository, never()).save(any());
    }

    // ---------------------- getUserById ----------------------

    @Test
    void getUserById_success() {
        User user = buildUser(1L, "test@test.com", ERole.ROLE_ASSURE, "CIN1", "NA1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
        assertEquals("test@test.com", result.getEmail());
        // mapToDto (simple) ne renseigne pas cin/numeroAssure
        assertNull(result.getCin());
        assertNull(result.getNumeroAssure());
    }

    @Test
    void getUserById_withNullRoles() {
        User user = buildUser(1L, "test@test.com", ERole.ROLE_ASSURE, "CIN1", "NA1");
        user.setRoles(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = userService.getUserById(1L);

        assertNull(result.getRoles());
    }

    @Test
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getUserById(99L));

        assertTrue(ex.getMessage().contains("introuvable"));
    }

    // ---------------------- getAllUsers ----------------------

    @Test
    void getAllUsers_success() {
        User user1 = buildUser(1L, "u1@test.com", ERole.ROLE_ASSURE, "CIN1", "NA1");
        User user2 = buildUser(2L, "u2@test.com", ERole.ROLE_AGENT_BUREAU, "CIN2", "NA2");
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserDto> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("u1@test.com", result.get(0).getEmail());
        assertEquals("u2@test.com", result.get(1).getEmail());
    }

    @Test
    void getAllUsers_emptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserDto> result = userService.getAllUsers();

        assertTrue(result.isEmpty());
    }
}