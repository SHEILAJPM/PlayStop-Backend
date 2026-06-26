package com.playstop.backend.dto.response;

import com.playstop.backend.enums.Role;

public record UserResponse(
    Long id,
    String nombre,
    String email,
    Role role,
    boolean activo
) {}
