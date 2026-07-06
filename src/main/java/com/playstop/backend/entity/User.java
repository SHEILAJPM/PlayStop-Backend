package com.playstop.backend.entity;

import com.playstop.backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;
    private String profileImageUrl;

    @Column(unique = true, length = 12)
    private String referralCode;

    // Para OAuth2 Google — si el usuario se registró vía Google (nullable para users existentes)
    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Moderación de chat ──────────────────────────────────────────────────
    @Column(name = "chat_warning_issued", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean chatWarningIssued;

    @Column(name = "chat_violation_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int chatViolationCount;

    @Column(name = "chat_suspended_until")
    private LocalDateTime chatSuspendedUntil;

    @Column(name = "chat_suspension_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int chatSuspensionCount;

    @Column(name = "chat_permanently_banned", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean chatPermanentlyBanned;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.referralCode == null) {
            this.referralCode = generateReferralCode();
        }
    }

    public static String generateReferralCode() {
        return java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }
}