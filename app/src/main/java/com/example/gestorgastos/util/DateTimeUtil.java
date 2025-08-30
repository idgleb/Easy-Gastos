package com.example.gestorgastos.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateTimeUtil {
    
    public static String formatDateTime(long epochMillis, String zoneId) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of(zoneId));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", new Locale("es", "AR"));
        return dateTime.format(formatter);
    }
    
    public static String formatDate(long epochMillis, String zoneId) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of(zoneId));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", new Locale("es", "AR"));
        return dateTime.format(formatter);
    }
    
    public static String getMonthYearString(int year, int month) {
        LocalDateTime dateTime = LocalDateTime.of(year, month, 1, 0, 0);
        String monthName = dateTime.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "AR"));
        return monthName + " " + year;
    }
    
    public static long getMonthStartEpoch(int year, int month, String zoneId) {
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        return startOfMonth.atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli();
    }
    
    public static long getMonthEndEpoch(int year, int month, String zoneId) {
        LocalDateTime endOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0)
                .plusMonths(1)
                .minusNanos(1);
        return endOfMonth.atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli();
    }
    
    public static long getCurrentEpochMillis() {
        return Instant.now().toEpochMilli();
    }
    
    public static String getCurrentZoneId() {
        return ZoneId.systemDefault().getId();
    }
}



