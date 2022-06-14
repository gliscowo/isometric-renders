package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.screen.ScreenScheduler;
import com.glisco.isometricrenders.util.ClientRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void openScheduled(Screen screen, CallbackInfo ci) {
        if (screen != null || !ScreenScheduler.hasScheduled()) return;

        ScreenScheduler.open();
        ci.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter;beginRenderTick(J)I"))
    private void onRenderStart(boolean tick, CallbackInfo ci) {
        ClientRenderCallback.EVENT.invoker().onRenderStart((MinecraftClient) (Object) this);
    }

}
