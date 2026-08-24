package com.javabank.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility for formatting numeric amounts to standard locale-specific currency string.
 * Demonstrates static utility patterns and final classes.
 */
public final class CurrencyFormatter {

    // Suppress default constructor to block instantiation
    private CurrencyFormatter() {
        throw new UnsupportedOperationException("Utility class should not be instantiated.");
    }

    /**
     * Formats balance double values into US currency string (e.g. $1,250.50).
     */
    public static String formatUSD(double amount) {
        NumberFormat usdFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        return usdFormatter.format(amount);
    }
}
