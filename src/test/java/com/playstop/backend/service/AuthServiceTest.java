package com.playstop.backend.service;

import com.playstop.backend.dto.request.LoginRequest;
import com.playstop.backend.dto.request.RegisterRequest;
import com.playstop.backend.dto.response.AuthResponse;
import com.playstop.backend.entity.User;
import com.playstop.backend.enums.Role;
import com.playstop.backend.repository.BranchEmployeeRepository;
import com.playstop.backend.repository.BranchInvitationRepository;
import com.playstop.backend.repository.UserRepository;
import com.playstop.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;
    @Mock
    private BranchInvitationRepository branchInvitationRepository;
    @Mock
    private BranchEmployeeRepository branchEmployeeRepository;

    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager, emailService, branchInvitationRepository, branchEmployeeRepository);

        registerRequest = new RegisterRequest();
        registerRequest.setName("Jugador Nuevo");
        registerRequest.setEmail("nuevo@test.com");
        registerRequest.setPassword("clave123");
        registerRequest.setPhone("999999999");
    }

    @Test
    void registerPlayer_emailAlreadyRegistered_throws409() {
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registerPlayer(registerRequest));

        assertEquals(409, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerPlayer_success_savesUserWithUserRoleAndReturnsToken() {
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.registerPlayer(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.USER, userCaptor.getValue().getRole());
        assertEquals("hashed", userCaptor.getValue().getPassword());
        verify(emailService).sendWelcomeEmail("nuevo@test.com", "Jugador Nuevo");
        assertEquals("jwt-token", response.getToken());
        assertEquals(Role.USER, response.getRole());
    }

    @Test
    void registerOwner_success_savesUserWithOwnerRole() {
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        authService.registerOwner(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.OWNER, userCaptor.getValue().getRole());
    }

    @Test
    void login_invalidCredentials_propagatesAuthenticationException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nuevo@test.com");
        loginRequest.setPassword("mal");

        doThrow(new BadCredentialsException("Credenciales incorrectas"))
                .when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_userVanishedAfterAuthentication_throwsRuntimeException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nuevo@test.com");
        loginRequest.setPassword("clave123");

        when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nuevo@test.com");
        loginRequest.setPassword("clave123");

        User user = User.builder().name("Jugador Nuevo").email("nuevo@test.com").role(Role.USER).build();
        when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals("nuevo@test.com", response.getEmail());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logout_incrementsTokenVersion() {
        User user = User.builder().email("nuevo@test.com").tokenVersion(3).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("nuevo@test.com", null));
        when(userRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(user));

        authService.logout();

        assertEquals(4, user.getTokenVersion());
        verify(userRepository).save(user);
    }
}
