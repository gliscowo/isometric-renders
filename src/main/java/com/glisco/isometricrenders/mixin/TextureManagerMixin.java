package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.client.gui.RenderScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public class TextureManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void stopTick(CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof RenderScreen && !((RenderScreen) MinecraftClient.getInstance().currentScreen).playAnimations) {
            ci.cancel();
        }
    }

}
