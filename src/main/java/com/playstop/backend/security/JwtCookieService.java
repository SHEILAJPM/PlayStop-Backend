package com.playstop.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Construye y adjunta la cookie httpOnly que transporta el JWT. El token
 * nunca viaja en el cuerpo JSON de las respuestas (ver AuthResponse), solo
 * en esta cookie, para que un XSS en el frontend no pueda leerlo con JS.
 */
@Service
public class JwtCookieService {

    @Value("${app.jwt.cookie.name}")
    private String cookieName;

    @Value("${app.jwt.cookie.secure}")
    private boolean secure;

    @Value("${app.jwt.cookie.same-site}")
    private String sameSite;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public void setAuthCookie(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token, Duration.ofMillis(jwtExpirationMs)).toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    public String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
