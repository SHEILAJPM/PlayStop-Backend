package com.playstop.backend.util;

public class EmailUtils {

    private EmailUtils() {}

    public static boolean isValid(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    public static String normalize(String email) {
        return email != null ? email.trim().toLowerCase() : null;
    }

    public static String obfuscate(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }

    public static String extractDomain(String email) {
        int at = email.indexOf('@');
        return at >= 0 ? email.substring(at + 1) : "";
    }
}
