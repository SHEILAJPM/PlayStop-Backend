package com.playstop.backend.util;

public class Constants {

    private Constants() {}

    public static final String ROLE_USER        = "USER";
    public static final String ROLE_OWNER       = "OWNER";
    public static final String ROLE_ADMIN       = "ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER ADMIN";

    public static final int TOKEN_EXPIRY_HOURS          = 24;
    public static final int RESET_TOKEN_EXPIRY_MINUTES  = 30;
    public static final int MAX_RESERVATIONS_PER_DAY    = 3;
    public static final int MIN_RESERVATION_HOURS_AHEAD = 1;

    public static final String DATE_FORMAT     = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIME_FORMAT     = "HH:mm";

    public static final int    OPENING_HOUR    = 7;
    public static final int    CLOSING_HOUR    = 22;
    public static final String BEARER_PREFIX   = "Bearer ";
    public static final String AUTH_HEADER     = "Authorization";
    public static final int    DEFAULT_PAGE    = 0;
    public static final int    DEFAULT_SIZE    = 10;
    public static final int    MAX_PAGE_SIZE   = 50;
}
