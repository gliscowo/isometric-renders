package com.glisco.isometricrenders.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

public class RenderableDispatcher {

    public static void drawIntoActiveFramebuffer(Renderable<?> renderable, float aspectRatio, float tickDelta) {
        final var client = MinecraftClient.getInstance();
        final var window = client.getWindow();

        RenderSystem.backupProjectionMatrix();
        Matrix4f projectionMatrix = Matrix4f.projectionMatrix(-aspectRatio, aspectRatio, 1, -1, -1000, 3000);
        RenderSystem.setProjectionMatrix(projectionMatrix);

        renderable.prepare();

        // Prepare model view matrix
        final var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.push();
        modelViewStack.loadIdentity();

        renderable.properties().applyToViewMatrix(modelViewStack);
        RenderSystem.applyModelViewMatrix();

        // Emit untransformed vertices
        renderable.emitVertices(
                new MatrixStack(),
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                tickDelta
        );

        // --> Draw
        renderable.draw(modelViewStack.peek().getPositionMatrix());

        modelViewStack.pop();
        RenderSystem.applyModelViewMatrix();

        renderable.cleanUp();
        RenderSystem.restoreProjectionMatrix();
    }

    public static NativeImage drawIntoImage(Renderable<?> renderable, int size) {
        Framebuffer framebuffer = new SimpleFramebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);

        RenderSystem.enableBlend();
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.setClearColor(0, 0, 0, 0);
        framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.beginWrite(true);
        drawIntoActiveFramebuffer(renderable, 1, 0);
        framebuffer.endWrite();

        final NativeImage img = new NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);
        RenderSystem.bindTexture(framebuffer.getColorAttachment());
        img.loadFromTextureImage(0, false);
        img.mirrorVertically();

        framebuffer.delete();

        return img;
    }

}
