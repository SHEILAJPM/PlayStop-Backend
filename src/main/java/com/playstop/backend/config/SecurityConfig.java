package com.playstop.backend.config;

import com.playstop.backend.security.JwtAuthenticationFilter;
import com.playstop.backend.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final AuthenticationProvider authenticationProvider;

    @Value("${swagger.username:}")
    private String swaggerUsername;

    @Value("${swagger.password:}")
    private String swaggerPassword;

    /**
     * Cadena aparte, con mayor prioridad, solo para la documentacion de la
     * API. Usa HTTP Basic con una credencial propia (no una cuenta real de
     * la app) porque el JWT por header no se puede adjuntar automaticamente
     * al cargar la pagina en el navegador. Si SWAGGER_USERNAME/PASSWORD no
     * estan configuradas, la ruta queda bloqueada (fail-closed) en vez de
     * publica.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain docsFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        http.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (swaggerUsername.isBlank() || swaggerPassword.isBlank()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
            return http.build();
        }

        UserDetails docsUser = org.springframework.security.core.userdetails.User
                .withUsername(swaggerUsername)
                .password(passwordEncoder.encode(swaggerPassword))
                .roles("DOCS")
                .build();
        DaoAuthenticationProvider docsProvider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(docsUser));
        docsProvider.setPasswordEncoder(passwordEncoder);

        http.authenticationProvider(docsProvider)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                // Necesario para que el reenvio interno de Spring Boot a /error
                // (ej. tras un 401 de HTTP Basic en docsFilterChain) no vuelva a
                // pasar por la autorizacion y termine reescribiendo el status
                .requestMatchers("/error").permitAll()
                // Mas especifico que /api/auth/** de abajo: logout necesita un usuario autenticado
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/courts", "/api/courts/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/court/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/match").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                // WebSocket SockJS handshake + info
                .requestMatchers("/ws/**").permitAll()
                // Stripe llama a este endpoint server-to-server, sin JWT
                .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "https://localhost",
            "capacitor://localhost",
            "https://playstop-frontend.onrender.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}