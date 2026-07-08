package com.playstop.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ejercita la cadena real de Spring Security (JwtAuthenticationFilter +
 * @PreAuthorize por rol) contra la app completa, en vez de mocks — algo que
 * ningún test de servicio cubre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerLoginAndAccessProtectedEndpoints() throws Exception {
        String email = "seguridad-" + UUID.randomUUID() + "@test.com";

        Map<String, String> registerBody = Map.of(
                "name", "Usuario Seguridad",
                "email", email,
                "password", "clave123",
                "phone", "999999999"
        );

        // El JWT nunca viaja en el JSON (AuthResponse.token es @JsonIgnore):
        // solo como cookie httpOnly, ver JwtCookieService.
        Cookie authCookie = mockMvc.perform(post("/api/auth/register/player")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("playstop_token");

        assertThat(authCookie).isNotNull();
        assertThat(authCookie.getValue()).isNotBlank();
        String token = authCookie.getValue();

        // Registrar de nuevo con el mismo email -> 409
        mockMvc.perform(post("/api/auth/register/player")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isConflict());

        // Login con contraseña incorrecta -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "incorrecta"))))
                .andExpect(status().isUnauthorized());

        // Login correcto -> 200 + token
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "clave123"))))
                .andExpect(status().isOk());

        // Endpoint protegido sin token -> rechazado (esta app devuelve 403 para
        // no autenticados en vez de 401; el frontend ya trata ambos igual)
        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isForbidden());

        // Endpoint protegido con la cookie de sesión (flujo real del frontend
        // web, rol USER) -> 200
        mockMvc.perform(get("/api/reservations/my").cookie(authCookie))
                .andExpect(status().isOk());

        // También debe aceptar el mismo JWT por header Authorization (clientes
        // que no manejan cookies, ver JwtAuthenticationFilter)
        mockMvc.perform(get("/api/reservations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Endpoint que exige rol OWNER, con token de un usuario USER -> 403
        mockMvc.perform(get("/api/reservations/court/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
