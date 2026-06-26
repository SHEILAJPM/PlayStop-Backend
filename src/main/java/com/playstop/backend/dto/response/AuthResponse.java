package com.playstop.backend.dto.response;

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
    private String token;
    private String name;
    private String email;
    private Role role;
    private boolean bannedFromReservations;
}