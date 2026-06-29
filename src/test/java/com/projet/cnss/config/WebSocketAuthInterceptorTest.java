package com.projet.cnss.config;

import com.projet.cnss.entity.ERole;
import com.projet.cnss.entity.Role;
import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    private User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        Set<Role> roles = new HashSet<>();
        roles.add(new Role(ERole.ROLE_ASSURE));
        user.setRoles(roles);
        return user;
    }

    private Message<?> buildConnectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        // Pattern recommandé par Spring : créer le message directement à partir
        // des MessageHeaders de l'accessor, pour que getAccessor(...) retrouve
        // bien cet accessor côté production.
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildNonConnectMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // ==========================================================
    // preSend - commande CONNECT
    // ==========================================================

    @Test
    void preSend_connectWithValidToken_setsUserOnAccessor() {
        User user = buildUser("jean@test.com");
        Message<?> message = buildConnectMessage("Bearer valid-token");

        when(jwtService.extractEmail("valid-token")).thenReturn("jean@test.com");
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, null);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
    }

    @Test
    void preSend_connectWithoutAuthorizationHeader_doesNotSetUser() {
        Message<?> message = buildConnectMessage(null);

        Message<?> result = interceptor.preSend(message, null);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
        verifyNoInteractions(jwtService);
    }

    @Test
    void preSend_connectWithNonBearerHeader_doesNotSetUser() {
        Message<?> message = buildConnectMessage("Basic abc123");

        Message<?> result = interceptor.preSend(message, null);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
        verifyNoInteractions(jwtService);
    }

    @Test
    void preSend_connectWithInvalidToken_doesNotSetUser() {
        User user = buildUser("jean@test.com");
        Message<?> message = buildConnectMessage("Bearer invalid-token");

        when(jwtService.extractEmail("invalid-token")).thenReturn("jean@test.com");
        when(userRepository.findByEmail("jean@test.com")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("invalid-token", user)).thenReturn(false);

        Message<?> result = interceptor.preSend(message, null);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    void preSend_connectWithUserNotFound_doesNotSetUser() {
        Message<?> message = buildConnectMessage("Bearer valid-token");

        when(jwtService.extractEmail("valid-token")).thenReturn("absent@test.com");
        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        Message<?> result = interceptor.preSend(message, null);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    void preSend_connectWithExceptionDuringTokenProcessing_isCaughtSilently() {
        Message<?> message = buildConnectMessage("Bearer broken-token");

        when(jwtService.extractEmail("broken-token")).thenThrow(new RuntimeException("Token cassé"));

        Message<?> result = assertDoesNotThrow(() -> interceptor.preSend(message, null));

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    // ==========================================================
    // preSend - commande différente de CONNECT
    // ==========================================================

    @Test
    void preSend_nonConnectCommand_doesNotProcessAuthentication() {
        Message<?> message = buildNonConnectMessage();

        Message<?> result = interceptor.preSend(message, null);

        assertEquals(message, result);
        verifyNoInteractions(jwtService, userRepository);
    }
}