package com.snok.hypertick.input;

import com.snok.hypertick.config.ConfigManager;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chooses which input to execute for a tick.
 * Defaults to first-input-wins, with optional priority slots for SWAP.
 */
public final class Resolver {
    private Resolver() {}

    public static Optional<BufferedInput> choose(List<BufferedInput> inputs, ConfigManager.Config cfg) {
        if (inputs == null || inputs.isEmpty()) return Optional.empty();

        // Normalize duplicates: keep earliest per (type, slot)
        inputs = normalize(inputs);

        // If there are any SWAP inputs and priority is set, prefer the highest priority slot
        if (cfg != null && cfg.priority_slots != null && cfg.priority_slots.length > 0) {
            Optional<BufferedInput> prioritized = inputs.stream()
                    .filter(b -> b.type == InputType.SWAP && b.slotIndex >= 0)
                    .min(Comparator.comparingInt(b -> priorityIndex(cfg.priority_slots, b.slotIndex)))
                    .filter(b -> priorityIndex(cfg.priority_slots, b.slotIndex) < Integer.MAX_VALUE);
            if (prioritized.isPresent()) return prioritized;
        }

        // Default: earliest timestamp wins
        return inputs.stream().min(Comparator.comparingLong(b -> b.timestampMs));
    }

    private static int priorityIndex(int[] order, int slot) {
        for (int i = 0; i < order.length; i++) {
            if (order[i] == slot) return i;
        }
        return Integer.MAX_VALUE;
    }

    private static List<BufferedInput> normalize(List<BufferedInput> inputs) {
        Map<String, BufferedInput> earliest = new HashMap<>();
        for (BufferedInput bi : inputs) {
            String key = bi.type.name() + ":" + bi.slotIndex;
            BufferedInput old = earliest.get(key);
            if (old == null || bi.timestampMs < old.timestampMs) {
                earliest.put(key, bi);
            }
        }
        return earliest.values().stream().toList();
    }
}


