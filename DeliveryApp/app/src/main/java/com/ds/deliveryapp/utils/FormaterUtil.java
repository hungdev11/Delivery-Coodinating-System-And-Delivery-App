package com.ds.deliveryapp.utils;

import android.util.Log;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormaterUtil {
    private final static DateTimeFormatter apiFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final static DateTimeFormatter uiFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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

    public static String formatDateTime(String time) {
        LocalDateTime timeFormat = null;
        try {
            timeFormat = LocalDateTime.parse(time, apiFormatter);
            return timeFormat.format(uiFormatter);
        } catch (Exception e) {
            Log.e("TaskDetailActivity", "Error parsing completedAt date: " + timeFormat, e);
        }
        return "N/A";
    }
}
