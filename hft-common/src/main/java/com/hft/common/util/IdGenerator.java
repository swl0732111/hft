package com.hft.common.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Zero-allocation ID generator using atomic counter.
 * Generates sequential IDs without string allocation.
 */
public final class IdGenerator {
    private static final AtomicLong counter = new AtomicLong(0);
    private static final ThreadLocal<StringBuilder> stringBuilder = ThreadLocal
            .withInitial(() -> new StringBuilder(32));

    private IdGenerator() {
    } // Utility class

    /**
     * Generate next ID as long.
     * Zero allocations.
     */
    public static long nextLong() {
        return counter.incrementAndGet();
    }

    /**
     * Generate next ID as String.
     * Reuses thread-local StringBuilder to minimize allocations.
     */
    public static String nextString() {
        StringBuilder sb = stringBuilder.get();
        sb.setLength(0);
        sb.append("ORD-").append(counter.incrementAndGet());
        return sb.toString();
    }

    /**
     * Reset counter (for testing only).
     */
    public static void reset() {
        counter.set(0);
    }
}
