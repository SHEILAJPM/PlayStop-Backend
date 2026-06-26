package com.playstop.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    String userName,
    String userAvatar,
    int rating,
    String comment,
    List<String> photoUrls,
    LocalDateTime createdAt
) {}
