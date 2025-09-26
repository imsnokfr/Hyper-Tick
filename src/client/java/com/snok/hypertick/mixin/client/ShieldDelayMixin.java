package com.snok.hypertick.mixin.client;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes shield delay to allow instant shield raising/lowering.
 */
@Mixin(ClientPlayerEntity.class)
public class ShieldDelayMixin {
    
    @Inject(method = "getShieldBlockingDelay", at = @At("HEAD"), cancellable = true)
    private void removeShieldDelay(CallbackInfoReturnable<Float> cir) {
        // Return 0 to remove shield delay completely
        cir.setReturnValue(0.0f);
    }
}
