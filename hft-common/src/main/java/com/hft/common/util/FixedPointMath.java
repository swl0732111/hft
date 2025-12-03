package com.hft.common.util;

/**
 * Fixed-point arithmetic utilities for zero-allocation trading.
 * Uses long with 8 decimal places (100,000,000 scale factor).
 * Example: 1.5 BTC = 150000000L
 */
public final class FixedPointMath {
    private FixedPointMath() {
    } // Utility class

    public static final long SCALE = 100_000_000L; // 8 decimal places
    public static final int DECIMAL_PLACES = 8;

    /**
     * Convert double to scaled long.
     */
    public static long fromDouble(double value) {
        return (long) (value * SCALE);
    }

    /**
     * Convert scaled long to double.
     */
    public static double toDouble(long scaled) {
        return (double) scaled / SCALE;
    }

    /**
     * Parse string to scaled long.
     * Example: "1.5" -> 150000000L
     */
    public static long parse(String value) {
        return fromDouble(Double.parseDouble(value));
    }

    /**
     * Format scaled long to string.
     */
    public static String format(long scaled) {
        return String.format("%.8f", toDouble(scaled));
    }

    /**
     * Add two scaled values.
     */
    public static long add(long a, long b) {
        return a + b;
    }

    /**
     * Subtract two scaled values.
     */
    public static long subtract(long a, long b) {
        return a - b;
    }

    /**
     * Multiply two scaled values.
     * Result is re-scaled to maintain precision.
     */
    public static long multiply(long a, long b) {
        return (a * b) / SCALE;
    }

    /**
     * Divide two scaled values.
     * Result is re-scaled to maintain precision.
     */
    public static long divide(long a, long b) {
        return (a * SCALE) / b;
    }

    /**
     * Compare two scaled values.
     * Returns: -1 if a < b, 0 if a == b, 1 if a > b
     */
    public static int compare(long a, long b) {
        return Long.compare(a, b);
    }

    /**
     * Check if value is zero.
     */
    public static boolean isZero(long value) {
        return value == 0;
    }

    /**
     * Check if value is positive.
     */
    public static boolean isPositive(long value) {
        return value > 0;
    }

    /**
     * Get minimum of two values.
     */
    public static long min(long a, long b) {
        return a < b ? a : b;
    }

    /**
     * Get maximum of two values.
     */
    public static long max(long a, long b) {
        return a > b ? a : b;
    }
}
