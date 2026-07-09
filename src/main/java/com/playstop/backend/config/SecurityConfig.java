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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
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

    // Origenes extra solo para desarrollo (localhost, capacitor://localhost).
    // Vacio en produccion: ver application.properties vs application-local.properties.
    @Value("${app.cors.dev-origins:}")
    private String corsDevOrigins;

    // Mismos flags que la cookie del JWT (ver JwtCookieService): frontend y
    // backend viven en dominios distintos, asi que esta cookie tambien
    // necesita SameSite=None + Secure para que el navegador la reenvie en
    // requests cross-site.
    @Value("${app.jwt.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.jwt.cookie.same-site}")
    private String cookieSameSite;

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
            // Token de doble envio: la cookie XSRF-TOKEN (legible por JS, a
            // diferencia de la del JWT) debe volver como header X-XSRF-TOKEN
            // en cada request que cambia estado. Se excluyen:
            // - el webhook de Stripe (server-to-server, sin cookie)
            // - /ws/** (SockJS usa polling XHR en algunos transportes, y la
            //   sesion ya se valida por cookie en el handshake, ver
            //   WsAuthHandshakeHandler)
            // - /api/auth/** (login/registro/recuperar-contrasena): un
            //   usuario nuevo puede aterrizar directo en /login sin haber
            //   hecho ningun GET antes, por lo que su navegador aun no
            //   tendria la cookie XSRF-TOKEN. No hay sesion existente que
            //   proteger en estos endpoints, asi que el riesgo real de CSRF
            //   no aplica aqui; si aplica en todo lo que ocurre despues de
            //   iniciar sesion, que si queda protegido.
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                // Por defecto, Spring Security 6+ enmascara el token con XOR
                // (proteccion BREACH) y espera que el cliente reenvie ese
                // valor enmascarado, no el crudo de la cookie. Con el patron
                // "leer cookie -> mandar como header" tipico de una SPA, el
                // cliente reenvia el valor crudo, asi que hace falta el
                // handler simple (sin XOR) para que la comparacion funcione.
                .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(
                    org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/payments/webhook"),
                    org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern("/ws/**"),
                    org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern("/api/auth/**")
                )
            )
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

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite(cookieSameSite));
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // https://localhost es el origen que usa la app Android empaquetada con
        // Capacitor (WebView con androidScheme=https por defecto), no un navegador.
        List<String> origins = new ArrayList<>(List.of(
            "https://playstop-frontend.onrender.com",
            "https://localhost"
        ));
        if (!corsDevOrigins.isBlank()) {
            origins.addAll(Arrays.stream(corsDevOrigins.split(",")).map(String::trim).toList());
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}