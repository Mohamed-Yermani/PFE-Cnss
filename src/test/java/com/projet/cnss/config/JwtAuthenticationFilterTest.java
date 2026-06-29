package com.projet.cnss.config;

import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_authorizationHeaderNotBearer_continuesChainWithoutAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic abc123");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_validToken_setsAuthenticationInContext() throws Exception {
        request.addHeader("Authorization", "Bearer valid-jwt-token");

        User user = new User();
        user.setEmail("jean@test.com");
        user.setRoles(java.util.Set.of(new com.projet.cnss.entity.Role(com.projet.cnss.entity.ERole.ROLE_ASSURE)));

        when(jwtService.extractEmail("valid-jwt-token")).thenReturn("jean@test.com");
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid-jwt-token", user)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_doesNotSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer invalid-jwt-token");

        User user = new User();
        user.setEmail("jean@test.com");

        when(jwtService.extractEmail("invalid-jwt-token")).thenReturn("jean@test.com");
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("invalid-jwt-token", user)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_userNotFound_doesNotSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer valid-jwt-token");

        when(jwtService.extractEmail("valid-jwt-token")).thenReturn("absent@test.com");
        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_extractEmailThrowsException_isCaughtAndChainContinues() throws Exception {
        request.addHeader("Authorization", "Bearer malformed-token");

        when(jwtService.extractEmail("malformed-token")).thenThrow(new RuntimeException("Token malformé"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_authenticationAlreadySet_doesNotOverwrite() throws Exception {
        request.addHeader("Authorization", "Bearer valid-jwt-token");

        // Authentification déjà présente dans le contexte
        User existingUser = new User();
        existingUser.setEmail("existing@test.com");
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        existingUser, null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(jwtService.extractEmail("valid-jwt-token")).thenReturn("jean@test.com");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // L'authentification existante ne doit pas être remplacée
        assertEquals(existingUser, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(userRepository, never()).findByEmail(anyString());
        verify(filterChain).doFilter(request, response);
    }
}