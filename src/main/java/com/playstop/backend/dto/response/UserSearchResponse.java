package com.playstop.backend.dto.response;

import java.util.UUID;

public record UserSearchResponse(
    UUID id,
    String name,
    String email,
    String profileImageUrl
) {}
