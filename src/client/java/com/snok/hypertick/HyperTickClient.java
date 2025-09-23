package com.snok.hypertick;

import com.snok.hypertick.input.BufferedInput;
import com.snok.hypertick.input.InputType;
import com.snok.hypertick.runtime.HyperTickRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Wires a basic client tick hook that flushes inputs each 50ms tick.
 */
public class HyperTickClient implements ClientModInitializer {
    private static boolean prevAttackPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = System.currentTimeMillis();
            long since = HyperTickRuntime.lastTickEpochMs;
            // capture rising edge for attack as a simple first signal
            MinecraftClient mc = client;
            if (mc != null && mc.options != null && mc.player != null) {
                boolean attackPressed = mc.options.attackKey.isPressed();
                if (attackPressed && !prevAttackPressed) {
                    HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.ATTACK));
                }
                prevAttackPressed = attackPressed;
            }
            var inputs = HyperTickRuntime.INPUT_BUFFER.collectSince(since);
            if (!inputs.isEmpty()) {
                // Choose first-input-wins by timestamp
                BufferedInput chosen = inputs.stream()
                        .min((a, b) -> Long.compare(a.timestampMs, b.timestampMs))
                        .orElse(null);
                if (chosen != null) {
                    // For now we only log the chosen input; actual execution will follow.
                    HyperTick.LOGGER.debug("HyperTick chose input type={} slot={} ts={}",
                            chosen.type, chosen.slotIndex, chosen.timestampMs);
                }
            }
            HyperTickRuntime.lastTickEpochMs = now;
        });
    }
}