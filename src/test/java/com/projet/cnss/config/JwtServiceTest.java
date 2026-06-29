package com.projet.cnss.config;

import com.projet.cnss.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Clé secrète encodée en base64, suffisamment longue pour HS256 (256 bits min recommandé)
        String secret = Base64.getEncoder().encodeToString(
                "ceci-est-une-cle-secrete-de-test-suffisamment-longue-256bits".getBytes());
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
    }

    private User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    @Test
    void generateToken_withoutExtraClaims_returnsValidToken() {
        User user = buildUser("jean@test.com");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("jean@test.com", jwtService.extractEmail(token));
    }

    @Test
    void generateToken_withExtraClaims_includesEmailAsSubject() {
        User user = buildUser("jean@test.com");
        java.util.Map<String, Object> extraClaims = java.util.Map.of("role", "ROLE_ASSURE");

        String token = jwtService.generateToken(extraClaims, user);

        assertEquals("jean@test.com", jwtService.extractEmail(token));
    }

    @Test
    void isTokenValid_matchingEmailAndNotExpired_returnsTrue() {
        User user = buildUser("jean@test.com");
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_differentUserEmail_returnsFalse() {
        User user = buildUser("jean@test.com");
        User otherUser = buildUser("autre@test.com");
        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_expiredToken_throwsExpiredJwtException() throws Exception {
        User user = buildUser("jean@test.com");

        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 100000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 50000))
                .signWith(getTestSignKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(expiredToken, user));
    }

    @Test
    void extractEmail_validToken_returnsCorrectEmail() {
        User user = buildUser("test@example.com");
        String token = jwtService.generateToken(user);

        assertEquals("test@example.com", jwtService.extractEmail(token));
    }

    private java.security.Key getTestSignKey() {
        String secret = (String) ReflectionTestUtils.getField(jwtService, "secretKey");
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secret);
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }
}