package com.playstop.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Autentica el handshake SockJS/STOMP con el mismo JWT que el resto de la API:
 * primero la cookie httpOnly (SockJS ya la envía sola en sus XHR de handshake,
 * ver AbstractXHRObject en sockjs-client) y, si no hay cookie, el header
 * Authorization, para clientes que no manejan cookies. El Principal que
 * devuelve este método queda asociado a toda la sesión STOMP, así que los
 * @MessageMapping (ChatController) lo reciben ya autenticado sin filtros
 * adicionales.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsAuthHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtService jwtService;
    private final JwtCookieService jwtCookieService;
    private final UserDetailsService userDetailsService;

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) return null;

        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (jwtService.isTokenValid(token, userDetails)) {
                return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            }
        } catch (Exception e) {
            log.warn("WebSocket: JWT inválido en el handshake: {}", e.getMessage());
        }
        return null;
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String fromCookie = jwtCookieService.extractToken(servletRequest.getServletRequest());
            if (fromCookie != null) return fromCookie;
        }
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
