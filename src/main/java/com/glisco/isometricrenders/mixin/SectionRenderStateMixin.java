package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BlockRenderLayerGroup;
import net.minecraft.client.render.SectionRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Override framebuffer used during AreaRenderable mesh render (World Mesher implementation detail)
@Mixin(SectionRenderState.class)
public class SectionRenderStateMixin {
	@WrapOperation(method = "renderSection", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BlockRenderLayerGroup;getFramebuffer()Lnet/minecraft/client/gl/Framebuffer;"))
	private Framebuffer overrideFramebuffer(BlockRenderLayerGroup instance, Operation<Framebuffer> original) {
		if (IsometricRenders.mainTargetOverride != null) return IsometricRenders.mainTargetOverride;
		return instance.getFramebuffer();
	}
}
