package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.client.gui.IsometricRenderScreen;
import com.glisco.isometricrenders.client.gui.RenderCallbackScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatScreen;sendMessage(Ljava/lang/String;)V", shift = At.Shift.AFTER), cancellable = true)
    public void dontClose(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (MinecraftClient.getInstance().currentScreen instanceof RenderCallbackScreen) {
            cir.setReturnValue(true);
        }
    }

}
