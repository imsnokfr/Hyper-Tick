package com.snok.hypertick.mixin.client;

import com.snok.hypertick.HyperTick;
import com.snok.hypertick.runtime.HyperTickRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ChatCommandMixin {
    @Inject(method = "sendChatMessageInternal", at = @At("HEAD"), cancellable = true)
    private void hypertick$handleCommands(String message, CallbackInfo ci) {
        if (message == null) return;
        if (!message.startsWith("/hyper") && !message.startsWith("/ht")) return;

        MinecraftClient mc = (MinecraftClient) (Object) this;
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        String[] parts = message.trim().split("\\s+");
        if (parts.length == 1) {
            player.sendMessage(net.minecraft.text.Text.literal("HyperTick: /hyper debug on|off | mode FIRST|LAST | addprio <slot> | clrprio"));
            ci.cancel();
            return;
        }
        switch (parts[1].toLowerCase()) {
            case "debug" -> {
                if (parts.length >= 3) {
                    boolean enable = parts[2].equalsIgnoreCase("on");
                    HyperTickRuntime.debugChatEnabled = enable;
                    player.sendMessage(net.minecraft.text.Text.literal("HyperTick debug chat: " + (enable ? "ON" : "OFF")));
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("Usage: /hyper debug on|off"));
                }
                ci.cancel();
            }
            case "mode" -> {
                if (parts.length >= 3) {
                    HyperTickRuntime.CONFIG.mode = parts[2].toUpperCase();
                    player.sendMessage(net.minecraft.text.Text.literal("HyperTick mode set to: " + HyperTickRuntime.CONFIG.mode));
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("Usage: /hyper mode FIRST|LAST"));
                }
                ci.cancel();
            }
            case "addprio" -> {
                if (parts.length >= 3) {
                    try {
                        int slot = Math.max(0, Math.min(8, Integer.parseInt(parts[2])));
                        int[] old = HyperTickRuntime.CONFIG.priority_slots;
                        int[] next = new int[old.length + 1];
                        System.arraycopy(old, 0, next, 0, old.length);
                        next[old.length] = slot;
                        HyperTickRuntime.CONFIG.priority_slots = next;
                        player.sendMessage(net.minecraft.text.Text.literal("HyperTick priority added: slot " + slot));
                    } catch (NumberFormatException e) {
                        player.sendMessage(net.minecraft.text.Text.literal("Usage: /hyper addprio <0-8>"));
                    }
                } else {
                    player.sendMessage(net.minecraft.text.Text.literal("Usage: /hyper addprio <0-8>"));
                }
                ci.cancel();
            }
            case "clrprio" -> {
                HyperTickRuntime.CONFIG.priority_slots = new int[0];
                player.sendMessage(net.minecraft.text.Text.literal("HyperTick priority cleared"));
                ci.cancel();
            }
            default -> {
                player.sendMessage(net.minecraft.text.Text.literal("HyperTick: unknown subcommand"));
                ci.cancel();
            }
        }
        HyperTick.LOGGER.info("HyperTick command: {}", message);
    }
}


