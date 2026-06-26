package com.playstop.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PriceUtils {

    private PriceUtils() {}

    public static double calcularTotal(double precioPorHora, int duracionMinutos) {
        double fraccion = duracionMinutos / 60.0;
        return round(precioPorHora * fraccion);
    }

    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static boolean isPriceValid(double price) {
        return price > 0 && price <= 10000;
    }
}
