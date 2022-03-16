package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.screen.BatchIsometricBlockRenderScreen;
import com.glisco.isometricrenders.screen.BatchIsometricItemRenderScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Collectors;

@Mixin(HandledScreen.class)
public class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow
    @Final
    protected T handler;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"))
    public void renderInventory(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode != GLFW.GLFW_KEY_F12) return;

        if (Screen.hasControlDown()) {
            client.setScreen(new BatchIsometricBlockRenderScreen(IsometricRenderHelper.extractBlocks(handler.slots.stream().map(Slot::getStack).collect(Collectors.toList())), "inventory"));
            cir.cancel();
        } else if (Screen.hasAltDown()) {
            client.setScreen(new BatchIsometricItemRenderScreen(handler.slots.stream().map(Slot::getStack).iterator(), "inventory"));
            cir.cancel();
        } else if (Screen.hasShiftDown()) {
            IsometricRenderHelper.renderItemAtlas("inventory", handler.slots.stream().map(Slot::getStack).filter(itemStack -> !itemStack.isEmpty()).collect(Collectors.toList()), true);
            cir.cancel();
        }
    }

}
