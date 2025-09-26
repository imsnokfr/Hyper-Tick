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
    
    // Movement input tracking
    private static boolean prevForwardPressed = false;
    private static boolean prevBackwardPressed = false;
    private static boolean prevLeftPressed = false;
    private static boolean prevRightPressed = false;
    private static boolean prevJumpPressed = false;
    private static boolean prevSneakPressed = false;
    private static boolean prevSprintPressed = false;
    
    // Camera input tracking
    private static float lastMouseX = 0.0f;
    private static float lastMouseY = 0.0f;
    private static boolean cameraInputActive = false;

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

                // Capture movement inputs - only buffer on rising edge for performance
                // Use actual key bindings so it works with any key configuration
                if (mc.options != null) {
                    long minDelta = Math.max(1, 1000L / Math.max(1, HyperTickRuntime.CONFIG.buffer_rate));
                    
                    // Forward movement
                    boolean forwardPressed = mc.options.forwardKey.isPressed();
                    if (forwardPressed && !prevForwardPressed && now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.MOVE_FORWARD));
                        HyperTickRuntime.lastBufferedMs = now;
                    }
                    prevForwardPressed = forwardPressed;

                    // Backward movement
                    boolean backwardPressed = mc.options.backKey.isPressed();
                    if (backwardPressed && !prevBackwardPressed && now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.MOVE_BACKWARD));
                        HyperTickRuntime.lastBufferedMs = now;
                    }
                    prevBackwardPressed = backwardPressed;

                    // Left movement
                    boolean leftPressed = mc.options.leftKey.isPressed();
                    if (leftPressed && !prevLeftPressed && now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.MOVE_LEFT));
                        HyperTickRuntime.lastBufferedMs = now;
                    }
                    prevLeftPressed = leftPressed;

                    // Right movement
                    boolean rightPressed = mc.options.rightKey.isPressed();
                    if (rightPressed && !prevRightPressed && now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.MOVE_RIGHT));
                        HyperTickRuntime.lastBufferedMs = now;
                    }
                    prevRightPressed = rightPressed;

                    // Jump (works with any key binding)
                    boolean jumpPressed = mc.options.jumpKey.isPressed();
                    if (jumpPressed && !prevJumpPressed && now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.JUMP));
                        HyperTickRuntime.lastBufferedMs = now;
                    }
                    prevJumpPressed = jumpPressed;

                    // Sneak (works with any key binding)
                    boolean sneakPressed = mc.options.sneakKey.isPressed();
                    if (sneakPressed && !prevSneakPressed && now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.SNEAK));
                        HyperTickRuntime.lastBufferedMs = now;
                    }
                    prevSneakPressed = sneakPressed;

                    // Sprint (works with any key binding - including your R key!)
                    boolean sprintPressed = mc.options.sprintKey.isPressed();
                    if (sprintPressed && !prevSprintPressed && now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                        HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.SPRINT));
                        HyperTickRuntime.lastBufferedMs = now;
                    }
                    prevSprintPressed = sprintPressed;
                }

                // Capture camera inputs (mouse movement) - optimized for performance
                if (mc.mouse != null && cameraInputActive) {
                    float mouseX = (float) mc.mouse.getX();
                    float mouseY = (float) mc.mouse.getY();
                    
                    float deltaX = mouseX - lastMouseX;
                    float deltaY = mouseY - lastMouseY;
                    
                    // Only buffer significant mouse movement to reduce noise
                    if (Math.abs(deltaX) > 0.5f || Math.abs(deltaY) > 0.5f) {
                        long minDelta = Math.max(1, 1000L / Math.max(1, HyperTickRuntime.CONFIG.buffer_rate));
                        if (now - HyperTickRuntime.lastBufferedMs >= minDelta) {
                            if (Math.abs(deltaX) > 0.5f) {
                                HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.CAMERA_YAW, deltaX));
                            }
                            if (Math.abs(deltaY) > 0.5f) {
                                HyperTickRuntime.INPUT_BUFFER.add(new BufferedInput(now, -1, InputType.CAMERA_PITCH, deltaY));
                            }
                            HyperTickRuntime.lastBufferedMs = now;
                        }
                    }
                    
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                } else if (mc.mouse != null && !cameraInputActive) {
                    // Initialize camera tracking on first frame
                    lastMouseX = (float) mc.mouse.getX();
                    lastMouseY = (float) mc.mouse.getY();
                    cameraInputActive = true;
                }
            }
            var inputs = HyperTickRuntime.INPUT_BUFFER.collectSince(since);
            var chosenList = Resolver.choosePair(inputs, HyperTickRuntime.CONFIG);
            // Execute up to two inputs: SWAP first (if present), then action
            for (int idx = 0; idx < Math.min(2, chosenList.size()); idx++) {
                var chosen = chosenList.get(idx);
                HyperTick.LOGGER.info("HyperTick chose input type={} slot={} ts={}",
                        chosen.type, chosen.slotIndex, chosen.timestampMs);
                // Keep logs; chat debug is optional via UI/commands
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
                        case MOVE_FORWARD -> {
                            if (mc.options != null) {
                                mc.options.forwardKey.setPressed(true);
                            }
                        }
                        case MOVE_BACKWARD -> {
                            if (mc.options != null) {
                                mc.options.backKey.setPressed(true);
                            }
                        }
                        case MOVE_LEFT -> {
                            if (mc.options != null) {
                                mc.options.leftKey.setPressed(true);
                            }
                        }
                        case MOVE_RIGHT -> {
                            if (mc.options != null) {
                                mc.options.rightKey.setPressed(true);
                            }
                        }
                        case JUMP -> {
                            if (mc.options != null) {
                                mc.options.jumpKey.setPressed(true);
                            }
                        }
                        case SNEAK -> {
                            if (mc.options != null) {
                                mc.options.sneakKey.setPressed(true);
                            }
                        }
                        case SPRINT -> {
                            if (mc.options != null) {
                                mc.options.sprintKey.setPressed(true);
                            }
                        }
                        case CAMERA_PITCH, CAMERA_YAW -> {
                            // Camera inputs are handled differently - they're applied immediately
                            // The floatValue contains the delta movement
                            if (mc.player != null) {
                                float delta = chosen.floatValue;
                                if (chosen.type == InputType.CAMERA_PITCH) {
                                    mc.player.changeLookDirection(0, delta * 0.15f);
                                } else if (chosen.type == InputType.CAMERA_YAW) {
                                    mc.player.changeLookDirection(delta * 0.15f, 0);
                                }
                            }
                        }
                        default -> {}
                    }
                }
            }
            // Release any injected key presses so they only last this tick
            if (mc != null && mc.options != null) {
                if (injectAttackRelease) {
                    // Keep ATTACK held while actively breaking; if finished but player still holds
                    // physical left-click, hand control back without forcing release
                    boolean breaking = false;
                    if (mc.interactionManager != null) {
                        try {
                            breaking = mc.interactionManager.isBreakingBlock();
                        } catch (Throwable ignored) {}
                    }
                    boolean physicalHeldAttack = mc.options.attackKey.isPressed();
                    if (mc.player != null && mc.player.isAlive() && breaking) {
                        mc.options.attackKey.setPressed(true);
                        // keep injectAttackRelease true to re-evaluate next tick
                    } else {
                        if (physicalHeldAttack) {
                            // Stop injecting; physical input maintains continuous mining
                            injectAttackRelease = false;
                        } else {
                            mc.options.attackKey.setPressed(false);
                            injectAttackRelease = false;
                        }
                    }
                }
                if (injectUseRelease) {
                    // Keep USE held while consuming/charging; if finished but player still holds physical right-click,
                    // do NOT force release (lets vanilla continue continuous eating/using/crossbow charging)
                    boolean using = mc.player != null && mc.player.isAlive() && mc.player.isUsingItem();
                    boolean physicalHeld = mc.options.useKey.isPressed();
                    
                    // Check if player is charging a crossbow or similar item
                    boolean charging = false;
                    if (mc.player != null && mc.player.getMainHandStack() != null) {
                        var item = mc.player.getMainHandStack().getItem();
                        // Crossbow charging detection
                        charging = using && (item.toString().contains("crossbow") || 
                                           mc.player.getItemUseTime() > 0);
                    }
                    
                    if (using || charging) {
                        mc.options.useKey.setPressed(true);
                        // keep injectUseRelease true to re-evaluate next tick
                    } else {
                        if (physicalHeld) {
                            // Hand control back to the player's held input
                            injectUseRelease = false;
                        } else {
                            mc.options.useKey.setPressed(false);
                            injectUseRelease = false;
                        }
                    }
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