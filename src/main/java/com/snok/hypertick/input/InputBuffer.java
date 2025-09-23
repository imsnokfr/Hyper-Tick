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
    private static final int MAX_BUFFER_SIZE = 256; // small cap to avoid growth

    private final Deque<BufferedInput> queue = new ArrayDeque<>();

    public synchronized void add(BufferedInput input) {
        if (queue.size() >= MAX_BUFFER_SIZE) {
            queue.removeFirst();
        }
        queue.addLast(input);
    }

    /**
     * Collect all inputs since the last tick boundary.
     * Caller should pass the earlier boundary (exclusive) in epoch ms.
     */
    public synchronized List<BufferedInput> collectSince(long sinceEpochMs) {
        List<BufferedInput> out = new ArrayList<>();
        for (BufferedInput bi : queue) {
            if (bi.timestampMs > sinceEpochMs) {
                out.add(bi);
            }
        }
        // prune older entries to keep memory small
        long cutoff = System.currentTimeMillis() - 1500L;
        while (!queue.isEmpty() && queue.peekFirst().timestampMs < cutoff) {
            queue.removeFirst();
        }
        return out;
    }
}


