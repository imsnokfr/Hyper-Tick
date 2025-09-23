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

        String mode = cfg != null && cfg.mode != null ? cfg.mode : "FIRST";

        // FIRST mode supports priority on SWAP
        if ("FIRST".equalsIgnoreCase(mode)) {
            if (cfg != null && cfg.priority_slots != null && cfg.priority_slots.length > 0) {
                Optional<BufferedInput> prioritized = inputs.stream()
                        .filter(b -> b.type == InputType.SWAP && b.slotIndex >= 0)
                        .min(Comparator.comparingInt(b -> priorityIndex(cfg.priority_slots, b.slotIndex)))
                        .filter(b -> priorityIndex(cfg.priority_slots, b.slotIndex) < Integer.MAX_VALUE);
                if (prioritized.isPresent()) return prioritized;
            }
            // earliest wins
            return inputs.stream().min(Comparator.comparingLong(b -> b.timestampMs));
        }

        // LAST mode: latest timestamp wins (no priority)
        return inputs.stream().max(Comparator.comparingLong(b -> b.timestampMs));
    }

    /**
     * Optionally choose up to two inputs for a tick, prioritizing SWAP first
     * when followed by ATTACK/USE in the same window to improve combo feel.
     */
    public static List<BufferedInput> choosePair(List<BufferedInput> inputs, ConfigManager.Config cfg) {
        if (inputs == null || inputs.isEmpty()) return List.of();
        inputs = normalize(inputs);

        // Try SWAP + action (ATTACK/USE/INTERACT) pair
        BufferedInput swap = inputs.stream()
                .filter(b -> b.type == InputType.SWAP && b.slotIndex >= 0)
                .min(Comparator.comparingLong(b -> b.timestampMs))
                .orElse(null);
        BufferedInput action = inputs.stream()
                .filter(b -> b.type == InputType.ATTACK || b.type == InputType.USE || b.type == InputType.INTERACT)
                .min(Comparator.comparingLong(b -> b.timestampMs))
                .orElse(null);

        if (swap != null && action != null) {
            // If priority slots exist, pick best swap first
            if (cfg != null && cfg.priority_slots != null && cfg.priority_slots.length > 0) {
                BufferedInput prioritizedSwap = inputs.stream()
                        .filter(b -> b.type == InputType.SWAP && b.slotIndex >= 0)
                        .min(Comparator.comparingInt(b -> priorityIndex(cfg.priority_slots, b.slotIndex)))
                        .filter(b -> priorityIndex(cfg.priority_slots, b.slotIndex) < Integer.MAX_VALUE)
                        .orElse(swap);
                swap = prioritizedSwap;
            }
            return List.of(swap, action);
        }

        // Fallback to single selection
        return choose(inputs, cfg).map(List::of).orElse(List.of());
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


