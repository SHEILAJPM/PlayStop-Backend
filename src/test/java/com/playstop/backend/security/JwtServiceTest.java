package com.playstop.backend.security;

import com.playstop.backend.entity.User;
import com.playstop.backend.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-secret-solo-para-jwtservicetest-1234567890";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Jugador")
                .email("jugador@test.com")
                .role(Role.USER)
                .enabled(true)
                .tokenVersion(0)
                .build();
    }

    @Test
    void isTokenValid_matchingVersionAndEnabled_true() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_tokenVersionMismatch_false() {
        String token = jwtService.generateToken(user);

        // Simula un logout / cambio de contraseña: sube la version en el usuario
        // "actual" (el que carga el filtro en cada request), el token viejo
        // sigue teniendo la version anterior grabada.
        user.setTokenVersion(1);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_disabledUser_false() {
        String token = jwtService.generateToken(user);

        user.setEnabled(false);

        assertFalse(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_legacyTokenWithoutVersionClaim_treatedAsVersionZero() {
        // Token construido a mano, sin el claim "tv", simulando uno emitido
        // antes de este cambio. Debe seguir siendo valido para un usuario en
        // tokenVersion 0 (default de todo usuario existente).
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String legacyToken = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertTrue(jwtService.isTokenValid(legacyToken, user));
    }
}
