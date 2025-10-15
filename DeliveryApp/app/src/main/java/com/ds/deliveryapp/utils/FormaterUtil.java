package com.ds.deliveryapp.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class FormaterUtil {
    public static String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "0₫";
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        currencyFormat.setMaximumFractionDigits(0);
        return currencyFormat.format(amount).trim();
    }

    public static String formatDistanceM(double meters) {
        if (meters < 1000) return Math.round(meters) + " m";
        return String.format("%.1f km", meters / 1000);
    }

    public static String formatDurationS(long seconds) {
        long minutes = seconds / 60;
        return "~" + minutes + " phút";
    }

    public static String formatWeight(double weight) {
        return String.format("%.2f kg", weight);
    }
}
