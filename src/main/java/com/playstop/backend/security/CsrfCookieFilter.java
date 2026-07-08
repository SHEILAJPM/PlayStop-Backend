package com.playstop.backend.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6 resuelve el CsrfToken de forma perezosa: si nada lo lee
 * durante el request, la cookie XSRF-TOKEN nunca se escribe. Este filtro
 * fuerza esa lectura en cada request para que el frontend siempre reciba la
 * cookie (legible por JS, a diferencia de la del JWT) y pueda reenviarla
 * como header X-XSRF-TOKEN en las solicitudes que cambian estado.
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
