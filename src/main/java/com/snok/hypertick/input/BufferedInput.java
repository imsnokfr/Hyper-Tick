package com.snok.hypertick.input;

/**
 * Represents a single user input captured by the client.
 * This is intentionally simple so it is easy to understand and extend later.
 */
public final class BufferedInput {
    /** epoch milliseconds when input happened */
    public final long timestampMs;

    /** hotbar slot index (0-8) or -1 if not applicable */
    public final int slotIndex;

    /** type of input (e.g., ATTACK, USE, SWAP) */
    public final InputType type;

    public BufferedInput(long timestampMs, int slotIndex, InputType type) {
        this.timestampMs = timestampMs;
        this.slotIndex = slotIndex;
        this.type = type;
    }
}


