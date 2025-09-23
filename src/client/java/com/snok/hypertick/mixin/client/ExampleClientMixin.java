package com.snok.hypertick.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.snok.hypertick.input.BufferedInput;
import com.snok.hypertick.input.InputType;
import com.snok.hypertick.runtime.HyperTickRuntime;

@Mixin(MinecraftClient.class)
public class ExampleClientMixin {
	@Inject(at = @At("HEAD"), method = "run")
	private void init(CallbackInfo info) {
		// Keep placeholder; do not capture here to avoid overhead.
	}
}