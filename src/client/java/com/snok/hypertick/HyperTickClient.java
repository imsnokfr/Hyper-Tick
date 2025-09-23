package com.snok.hypertick;

import com.snok.hypertick.input.BufferedInput;
import com.snok.hypertick.input.InputType;
import com.snok.hypertick.input.Resolver;
import com.snok.hypertick.runtime.HyperTickRuntime;
import com.snok.hypertick.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import com.snok.hypertick.ui.SettingsScreen;

/**
 * Wires a basic client tick hook that flushes inputs each 50ms tick.
 */
public class HyperTickClient implements ClientModInitializer {
    private static boolean prevAttackPressed = false;
    private static boolean prevUsePressed = false;
    private static final boolean[] prevHotbarPressed = new boolean[9];
    private static boolean injectAttackRelease = false;
    private static boolean injectUseRelease = false;
    private static boolean prevSwapHandsPressed = false;
    private static boolean prevPickItemPressed = false;
    private static boolean injectInteractRelease = false;
    private static KeyBinding reloadConfigKey;
    private static KeyBinding openSettingsKey;

    @Override
    public void onInitializeClient() {
        // Register keys: reload config (R), open settings (O)
        reloadConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hypertick.reload_config",
                GLFW.GLFW_KEY_R,
                "key.categories.misc"
        ));
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hypertick.open_settings",
                GLFW.GLFW_KEY_O,
                "key.categories.misc"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long now = System.currentTimeMillis();
            long since = HyperTickRuntime.lastTickEpochMs;
            // Hot reload config if file changed
            try {
                var f = ConfigManager.getConfigFile();
                if (f.exists() && f.lastModified() > since) {
                    HyperTickRuntime.CONFIG = ConfigManager.loadOrCreateDefault();
                    HyperTick.LOGGER.info("HyperTick config reloaded: mode={} priority={} entries", HyperTickRuntime.CONFIG.mode, HyperTickRuntime.CONFIG.priority_slots.length);
                }
            } catch (Throwable ignored) {}
            // Manual reload via keybind
            if (reloadConfigKey != null && reloadConfigKey.wasPressed()) {
                try {
                    HyperTickRuntime.CONFIG = ConfigManager.loadOrCreateDefault();
                    HyperTick.LOGGER.info("HyperTick config reloaded (manual): mode={} priority={} entries", HyperTickRuntime.CONFIG.mode, HyperTickRuntime.CONFIG.priority_slots.length);
                } catch (Throwable e) {
                    HyperTick.LOGGER.info("HyperTick config reload failed: {}", e.getMessage());
                }
            }
            if (openSettingsKey != null && openSettingsKey.wasPressed()) {
                client.setScreen(new SettingsScreen());
            }
            // capture rising edge for attack as a simple first signal
            MinecraftClient mc = client;
            if (mc != null && mc.options != null && mc.player != null && mc.currentScreen == null && mc.player.isAlive()) {
                boolean attackPressed = mc.options.attackKey.isPressed();
                if (attackPressed && !prevAttackPressed) {
                    long minDelta = Math.max(1, 1000L / Math.max(1, HyperTickRuntime.CONFIG.buffer_rate));
                    if (now - HyperTickRuntime.lastAttackBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.ATTACK));
                        HyperTickRuntime.lastAttackBufferedMs = now;
                    }
                }
                prevAttackPressed = attackPressed;

                boolean usePressed = mc.options.useKey.isPressed();
                if (usePressed && !prevUsePressed) {
                    long minDelta = Math.max(1, 1000L / Math.max(1, HyperTickRuntime.CONFIG.buffer_rate));
                    if (now - HyperTickRuntime.lastUseBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.USE));
                        HyperTickRuntime.lastUseBufferedMs = now;
                    }
                }
                prevUsePressed = usePressed;

                // Capture interact-like keys: swap-hands (F) and pick-block (middle click) as INTERACT
                boolean swapHandsPressed = mc.options.swapHandsKey.isPressed();
                if (swapHandsPressed && !prevSwapHandsPressed) {
                    long minDelta = Math.max(1, 1000L / Math.max(1, HyperTickRuntime.CONFIG.buffer_rate));
                    if (now - HyperTickRuntime.lastInteractBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.INTERACT));
                        HyperTickRuntime.lastInteractBufferedMs = now;
                    }
                }
                prevSwapHandsPressed = swapHandsPressed;

                boolean pickItemPressed = mc.options.pickItemKey.isPressed();
                if (pickItemPressed && !prevPickItemPressed) {
                    long minDelta = Math.max(1, 1000L / Math.max(1, HyperTickRuntime.CONFIG.buffer_rate));
                    if (now - HyperTickRuntime.lastInteractBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.INTERACT));
                        HyperTickRuntime.lastInteractBufferedMs = now;
                    }
                }
                prevPickItemPressed = pickItemPressed;

                // Capture hotbar swaps (keys 1..9 -> slots 0..8)
                if (mc.options.hotbarKeys != null && mc.options.hotbarKeys.length >= 9) {
                    for (int i = 0; i < 9; i++) {
                        boolean pressed = mc.options.hotbarKeys[i].isPressed();
                        if (pressed && !prevHotbarPressed[i]) {
                            long minDelta = Math.max(1, 1000L / Math.max(1, HyperTickRuntime.CONFIG.buffer_rate));
                            if (now - HyperTickRuntime.lastSwapBufferedMs[i] >= minDelta) {
                                HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, i, InputType.SWAP));
                                HyperTickRuntime.lastSwapBufferedMs[i] = now;
                            }
                        }
                        prevHotbarPressed[i] = pressed;
                    }
                }
            }
            var inputs = HyperTickRuntime.INPUT_BUFFER.collectSince(since);
            var chosenList = Resolver.choosePair(inputs, HyperTickRuntime.CONFIG);
            // Execute up to two inputs: SWAP first (if present), then action
            for (int idx = 0; idx < Math.min(2, chosenList.size()); idx++) {
                var chosen = chosenList.get(idx);
                HyperTick.LOGGER.info("HyperTick chose input type={} slot={} ts={}",
                        chosen.type, chosen.slotIndex, chosen.timestampMs);
                if (HyperTickRuntime.debugChatEnabled && client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.literal(
                            "[HT] " + chosen.type + (chosen.slotIndex >= 0 ? (" slot=" + chosen.slotIndex) : "")
                    ));
                }
                // Apply SWAP immediately by selecting hotbar slot
                if (chosen.type == InputType.SWAP && mc != null && mc.player != null && mc.currentScreen == null && mc.player.isAlive()) {
                    int slot = Math.max(0, Math.min(8, chosen.slotIndex));
                    mc.player.getInventory().selectedSlot = slot;
                }
                // Execute ATTACK/USE at tick boundary to align with 20 TPS
                if (mc != null && mc.currentScreen == null && mc.player != null && mc.player.isAlive()) {
                    switch (chosen.type) {
                        case ATTACK -> {
                            if (mc.options != null) {
                                mc.options.attackKey.setPressed(true);
                                injectAttackRelease = true;
                            }
                        }
                        case USE -> {
                            if (mc.options != null) {
                                mc.options.useKey.setPressed(true);
                                injectUseRelease = true;
                            }
                        }
                        case INTERACT -> {
                            if (mc.options != null) {
                                // Prefer swap-hands as a benign interact; if held tool uses pick-block, it still goes through
                                mc.options.swapHandsKey.setPressed(true);
                                injectInteractRelease = true;
                            }
                        }
                        default -> {}
                    }
                }
            }
            // Release any injected key presses so they only last this tick
            if (mc != null && mc.options != null) {
                if (injectAttackRelease) {
                    mc.options.attackKey.setPressed(false);
                    injectAttackRelease = false;
                }
                if (injectUseRelease) {
                    mc.options.useKey.setPressed(false);
                    injectUseRelease = false;
                }
                if (injectInteractRelease) {
                    mc.options.swapHandsKey.setPressed(false);
                    injectInteractRelease = false;
                }
            }
            HyperTickRuntime.lastTickEpochMs = now;
        });
    }
}