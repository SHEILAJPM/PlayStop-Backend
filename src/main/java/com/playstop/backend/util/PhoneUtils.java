package com.playstop.backend.util;

public class PhoneUtils {

    private PhoneUtils() {}

    public static boolean isValid(String phone) {
        return phone != null && phone.matches("^\\+?[0-9]{7,15}$");
    }

    public static String normalize(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[\\s\\-\\.\\(\\)]", "");
    }

    public static String format(String phone) {
        if (phone == null || phone.length() < 9) return phone;
        String clean = normalize(phone);
        if (clean.startsWith("+51")) {
            return clean.substring(0, 3) + " " + clean.substring(3, 6) + " " + clean.substring(6);
        }
        return clean;
    }
}
