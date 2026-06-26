package com.playstop.backend.util;

import java.time.LocalDate;
import java.time.LocalTime;

public class ValidationUtils {

    private ValidationUtils() {}

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    public static boolean isValidDuration(int minutes) {
        return minutes == 30 || minutes == 60 || minutes == 90 || minutes == 120;
    }

    public static boolean isDateInFuture(LocalDate date) {
        return date != null && !date.isBefore(LocalDate.now());
    }

    public static boolean isValidStartTime(LocalTime time) {
        LocalTime opening = LocalTime.of(Constants.OPENING_HOUR, 0);
        LocalTime closing = LocalTime.of(Constants.CLOSING_HOUR, 0);
        return time != null && !time.isBefore(opening) && !time.isAfter(closing);
    }

    public static boolean isStrongPassword(String password) {
        return password != null
            && password.length() >= 8
            && password.chars().anyMatch(Character::isUpperCase)
            && password.chars().anyMatch(Character::isDigit);
    }

    public static boolean isValidPrice(double price) {
        return price > 0 && price <= 10000;
    }
}
