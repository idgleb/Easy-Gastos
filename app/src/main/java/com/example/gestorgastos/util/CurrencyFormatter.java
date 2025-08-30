package com.example.gestorgastos.util;

import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    
    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
    
    public static String format(double amount) {
        return currencyFormatter.format(amount);
    }
    
    public static String formatWithoutSymbol(double amount) {
        return String.format("$%.2f", amount);
    }
    
    public static String formatCompact(double amount) {
        if (amount >= 1000000) {
            return String.format("$%.1fM", amount / 1000000);
        } else if (amount >= 1000) {
            return String.format("$%.1fK", amount / 1000);
        } else {
            return formatWithoutSymbol(amount);
        }
    }
}



