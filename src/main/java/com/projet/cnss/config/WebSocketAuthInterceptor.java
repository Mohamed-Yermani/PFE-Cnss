package com.projet.cnss.config;

import com.projet.cnss.entity.User;
import com.projet.cnss.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService     jwtService;    // ✅ votre classe exacte
    private final UserRepository userRepository; // ✅ pour charger le User

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null
                && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    // ✅ extractEmail() — votre méthode exacte
                    String email = jwtService.extractEmail(token);

                    if (email != null) {
                        // ✅ charger le User depuis la BDD
                        User user = userRepository.findByEmail(email)
                                .orElse(null);

                        if (user != null
                                // ✅ isTokenValid(token, user) — votre méthode exacte
                                && jwtService.isTokenValid(token, user)) {

                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            user,
                                            null,
                                            user.getAuthorities() // ✅ User doit implémenter UserDetails
                                    );

                            accessor.setUser(auth);
                            log.info("WebSocket authentifié : {}", email);
                        } else {
                            log.warn("WebSocket token invalide pour : {}", email);
                        }
                    }
                } catch (Exception e) {
                    log.warn("WebSocket erreur token : {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket connexion sans token Authorization");
            }
        }
        return message;
    }
}