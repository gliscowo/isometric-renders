package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(Screen.class)
public class ScreenMixin {

    @Unique private int isometric$tooltipWidth = 0;
    @Unique private int isometric$tooltipHeight = 0;

    @Inject(method = "renderTooltipFromComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void captureTooltipDimensions(MatrixStack matrices, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci, int width, int height) {
        this.isometric$tooltipWidth = width;
        this.isometric$tooltipHeight = height;
    }

    @ModifyVariable(method = "renderTooltipFromComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"), ordinal = 4)
    private int centerXIfNeeded(int orig) {
        if (!IsometricRenders.centerNextTooltip) return orig;
        return orig - 12 - isometric$tooltipWidth / 2;
    }

    @ModifyVariable(method = "renderTooltipFromComponents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V"), ordinal = 5)
    private int centerYIfNeeded(int orig) {
        if (!IsometricRenders.centerNextTooltip) return orig;
        return orig + 8 - isometric$tooltipHeight / 2;
    }

    @Inject(method = "renderTooltipFromComponents", at = @At("TAIL"))
    private void resetCenterState(MatrixStack matrices, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci) {
        IsometricRenders.centerNextTooltip = false;
    }

}
