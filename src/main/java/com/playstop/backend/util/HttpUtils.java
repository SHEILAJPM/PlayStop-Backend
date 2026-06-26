package com.playstop.backend.util;

import jakarta.servlet.http.HttpServletRequest;

public class HttpUtils {

    private HttpUtils() {}

    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null ? realIp : request.getRemoteAddr();
    }

    public static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.AUTH_HEADER);
        if (header != null && header.startsWith(Constants.BEARER_PREFIX)) {
            return header.substring(Constants.BEARER_PREFIX.length());
        }
        return null;
    }
}
