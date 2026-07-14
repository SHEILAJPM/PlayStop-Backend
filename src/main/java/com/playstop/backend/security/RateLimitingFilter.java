package com.playstop.backend.security;

import com.playstop.backend.util.HttpUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de solicitudes por IP en login/registro para frenar fuerza bruta de
 * contraseñas y registro masivo automatizado. En memoria: si el backend
 * llega a correr en más de una instancia a la vez, el límite deja de ser
 * global entre instancias (habría que migrar a bucket4j-redis).
 * La IP se toma de HttpUtils.getClientIp() (X-Forwarded-For / X-Real-IP) y
 * no de request.getRemoteAddr(), porque detrás del proxy inverso de Render
 * este último es siempre la IP del proxy, no la del cliente real.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register/player",
            "/api/auth/register/owner",
            "/api/auth/forgot-password",
            "/api/auth/reset-password"
    );

    // Bucket separado del de auth: /api/upload lo golpea un usuario ya
    // autenticado (otro modelo de amenaza — abuso de costo, no fuerza
    // bruta), no tiene sentido que comparta presupuesto con login/registro.
    private static final Set<String> UPLOAD_PATHS = Set.of("/api/upload");

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> uploadBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (LIMITED_PATHS.contains(uri)) {
            enforce(buckets, this::newAuthBucket, request, response, filterChain);
        } else if (UPLOAD_PATHS.contains(uri)) {
            enforce(uploadBuckets, this::newUploadBucket, request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void enforce(
            ConcurrentHashMap<String, Bucket> bucketsByIp,
            java.util.function.Supplier<Bucket> bucketFactory,
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Bucket bucket = bucketsByIp.computeIfAbsent(HttpUtils.getClientIp(request), ip -> bucketFactory.get());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Demasiados intentos. Intenta de nuevo en un minuto.\"}"
            );
        }
    }

    private Bucket newAuthBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build())
                .build();
    }

    private Bucket newUploadBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofMinutes(1)).build())
                .build();
    }
}
