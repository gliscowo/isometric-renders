package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.client.gui.IsometricRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    public void openScheduled(Screen screen, CallbackInfo ci) {
        if (screen == null && IsometricRenderHelper.isScreenScheduled()) {
            IsometricRenderHelper.openScheduledScreen();
            ci.cancel();
        }
    }

}
