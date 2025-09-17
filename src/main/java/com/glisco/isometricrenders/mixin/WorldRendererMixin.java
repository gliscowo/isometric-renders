package com.glisco.isometricrenders.mixin;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.util.AreaSelectionHelper;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void dontRenderInScreen(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        if (!IsometricRenders.skipWorldRender) return;

        IsometricRenders.skipWorldRender = false;
        ci.cancel();
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderTargetBlockOutline(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/util/math/MatrixStack;Z)V"))
    public void drawAreaSelection(GpuBufferSlice gpuBufferSlice, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Handle<Framebuffer> handle, Handle<Framebuffer> handle2, boolean bl, Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4, CallbackInfo ci, @Local(ordinal = 0) MatrixStack matrices) {
        AreaSelectionHelper.renderSelectionBox(matrices, camera);
    }
}
