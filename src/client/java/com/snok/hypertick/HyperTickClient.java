package com.snok.hypertick;

import com.snok.hypertick.input.BufferedInput;
import com.snok.hypertick.input.InputType;
import com.snok.hypertick.input.Resolver;
import com.snok.hypertick.runtime.HyperTickRuntime;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Wires a basic client tick hook that flushes inputs each 50ms tick.
 */
public class HyperTickClient implements ClientModInitializer {
    private static boolean prevAttackPressed = false;
    private static final boolean[] prevHotbarPressed = new boolean[9];

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

                // Capture hotbar swaps (keys 1..9 -> slots 0..8)
                if (mc.options.hotbarKeys != null && mc.options.hotbarKeys.length >= 9) {
                    for (int i = 0; i < 9; i++) {
                        boolean pressed = mc.options.hotbarKeys[i].isPressed();
                        if (pressed && !prevHotbarPressed[i]) {
                            HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, i, InputType.SWAP));
                        }
                        prevHotbarPressed[i] = pressed;
                    }
                }
            }
            var inputs = HyperTickRuntime.INPUT_BUFFER.collectSince(since);
            Resolver.choose(inputs, HyperTickRuntime.CONFIG).ifPresent(chosen -> {
                HyperTick.LOGGER.info("HyperTick chose input type={} slot={} ts={}",
                        chosen.type, chosen.slotIndex, chosen.timestampMs);
                // Apply SWAP immediately by selecting hotbar slot
                if (chosen.type == InputType.SWAP && mc != null && mc.player != null) {
                    int slot = Math.max(0, Math.min(8, chosen.slotIndex));
                    mc.player.getInventory().selectedSlot = slot;
                }
            });
            HyperTickRuntime.lastTickEpochMs = now;
        });
    }
}