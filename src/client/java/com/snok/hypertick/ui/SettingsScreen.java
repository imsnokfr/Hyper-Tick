package com.snok.hypertick.ui;

import com.snok.hypertick.HyperTick;
import com.snok.hypertick.config.ConfigManager;
import com.snok.hypertick.runtime.HyperTickRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Simple in-game settings screen for Hyper Tick. */
public class SettingsScreen extends Screen {
    private TextFieldWidget bufferRateField;

    public SettingsScreen() {
        super(Text.literal("Hyper Tick Settings"));
    }

    @Override
    protected void init() {
        int y = this.height / 4;

        // Mode toggle
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Mode: " + HyperTickRuntime.CONFIG.mode), b -> {
            HyperTickRuntime.CONFIG.mode = "FIRST".equalsIgnoreCase(HyperTickRuntime.CONFIG.mode) ? "LAST" : "FIRST";
            b.setMessage(Text.literal("Mode: " + HyperTickRuntime.CONFIG.mode));
            saveAndToast();
        }).dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;

        // Buffer rate text input
        bufferRateField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, y, 120, 20, Text.literal("buffer"));
        bufferRateField.setText(String.valueOf(HyperTickRuntime.CONFIG.buffer_rate));
        this.addDrawableChild(bufferRateField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply Hz"), b -> {
            try {
                int hz = Math.max(1, Integer.parseInt(bufferRateField.getText().trim()));
                HyperTickRuntime.CONFIG.buffer_rate = hz;
                saveAndToast();
            } catch (NumberFormatException ignored) {
                toast("Invalid Hz value");
            }
        }).dimensions(this.width / 2 + 26, y, 74, 20).build());
        y += 24;

        // Debug toggle
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Debug Chat: " + (HyperTickRuntime.debugChatEnabled ? "ON" : "OFF")), b -> {
            HyperTickRuntime.debugChatEnabled = !HyperTickRuntime.debugChatEnabled;
            b.setMessage(Text.literal("Debug Chat: " + (HyperTickRuntime.debugChatEnabled ? "ON" : "OFF")));
        }).dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;

        // Priority add current slot
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Add Current Slot to Priority"), b -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                int slot = mc.player.getInventory().selectedSlot;
                int[] old = HyperTickRuntime.CONFIG.priority_slots;
                int[] next = new int[old.length + 1];
                System.arraycopy(old, 0, next, 0, old.length);
                next[old.length] = slot;
                HyperTickRuntime.CONFIG.priority_slots = next;
                saveAndToast();
            }
        }).dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;

        // Clear priority
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Clear Priority"), b -> {
            HyperTickRuntime.CONFIG.priority_slots = new int[0];
            saveAndToast();
        }).dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 28;

        // Close
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> this.close()).dimensions(this.width / 2 - 100, y, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 4 - 20, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private void saveAndToast() {
        try { ConfigManager.save(HyperTickRuntime.CONFIG); } catch (Exception ignored) {}
        toast("Saved: mode=" + HyperTickRuntime.CONFIG.mode + ", hz=" + HyperTickRuntime.CONFIG.buffer_rate);
    }

    private void toast(String msg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendMessage(net.minecraft.text.Text.literal("[HT] " + msg));
        } else {
            HyperTick.LOGGER.info("[HT] {}", msg);
        }
    }
}


