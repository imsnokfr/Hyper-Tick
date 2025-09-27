package com.snok.hypertick.input;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Very small, thread-safe buffer for inputs with millisecond timestamps.
 * Keeps the last ~1-2 seconds of inputs to cover timing jitter.
 */
public final class InputBuffer {
    private static final int MAX_BUFFER_SIZE = 256; // Increased for 8kHz performance
    private static final long CLEANUP_INTERVAL_MS = 500L; // Clean up every 500ms for 8kHz

    private final Deque<BufferedInput> queue = new ArrayDeque<>();
    private long lastCleanupMs = System.currentTimeMillis();

    public synchronized void add(BufferedInput input) {
        // Periodic cleanup for better performance
        long now = System.currentTimeMillis();
        if (now - lastCleanupMs > CLEANUP_INTERVAL_MS) {
            cleanup();
            lastCleanupMs = now;
        }
        
        if (queue.size() >= MAX_BUFFER_SIZE) {
            queue.removeFirst();
        }
        queue.addLast(input);
    }
    
    private void cleanup() {
        long cutoff = System.currentTimeMillis() - 500L; // Keep only last 500ms for 8kHz
        while (!queue.isEmpty() && queue.peekFirst().timestampMs < cutoff) {
            queue.removeFirst();
        }
    }

    /**
     * Collect all inputs since the last tick boundary.
     * Caller should pass the earlier boundary (exclusive) in epoch ms.
     */
    public synchronized List<BufferedInput> collectSince(long sinceEpochMs) {
        List<BufferedInput> out = new ArrayList<>(32); // Pre-allocate for 8kHz performance
        for (BufferedInput bi : queue) {
            if (bi.timestampMs > sinceEpochMs) {
                out.add(bi);
            }
        }
        return out;
    }
}


