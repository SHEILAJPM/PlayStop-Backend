package com.playstop.backend.dto.response;

import com.playstop.backend.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String name,
    String email,
    String phone,
    String profileImageUrl,
    Role role,
    LocalDateTime createdAt
) {}
