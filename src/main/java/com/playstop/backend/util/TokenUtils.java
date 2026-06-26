package com.playstop.backend.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class TokenUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenUtils() {}

    public static String generateResetToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isTokenExpired(java.time.LocalDateTime expiration) {
        return expiration != null && expiration.isBefore(java.time.LocalDateTime.now());
    }
}
