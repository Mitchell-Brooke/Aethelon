package com.aethelon.mixin;

import com.aethelon.AethelonClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void aethelon$onClientTick(CallbackInfo ci) {
        if (AethelonClient.INSTANCE != null) {
            AethelonClient.INSTANCE.onClientTick();
        }
    }
}