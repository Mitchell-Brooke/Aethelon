package com.aethelon.mixin;

import com.aethelon.gui.AethelonMainScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void aethelon$onKeyPress(long windowPointer, int key, KeyEvent event, CallbackInfo ci) {
        if (event.key() != GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.level == null) {
            return;
        }
        mc.setScreen(new AethelonMainScreen());
        ci.cancel();
    }
}