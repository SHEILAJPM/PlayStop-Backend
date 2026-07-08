package com.playstop.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playstop.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID id;

    // Nunca se serializa al cliente: viaja solo como cookie httpOnly (ver
    // JwtCookieService/AuthController), para que un XSS no pueda leerlo.
    // Se mantiene aquí, no en JSON, para que el controller pueda tomarlo y
    // ponerlo en la cookie sin cambiar la forma en que AuthService construye
    // esta respuesta.
    @JsonIgnore
    private String token;

    private String name;
    private String email;
    private Role role;
    private boolean bannedFromReservations;
}