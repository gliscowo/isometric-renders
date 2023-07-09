package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.mixin.access.FramebufferAccessor;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.function.Consumer;

public class RenderableDispatcher {

    /**
     * Renders the given renderable into the current framebuffer,
     * with the projection matrix adjusted to compensate for the buffer's
     * aspect ratio
     *
     * @param renderable  The renderable to draw
     * @param aspectRatio The aspect ratio of the current framebuffer
     * @param tickDelta   The tick delta to use
     */
    public static void drawIntoActiveFramebuffer(Renderable<?> renderable, float aspectRatio, float tickDelta, Consumer<MatrixStack> transformer) {

        renderable.prepare();

        // Prepare model view matrix
        final var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.push();
        modelViewStack.loadIdentity();

        transformer.accept(modelViewStack);

        renderable.properties().applyToViewMatrix(modelViewStack);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.backupProjectionMatrix();
        Matrix4f projectionMatrix = new Matrix4f().setOrtho(-aspectRatio, aspectRatio, -1, 1, -1000, 3000);

        // Unproject to get the camera position for vertex sorting
        var camPos = new Vector4f(0, 0, 0, 1);
        camPos.mul(new Matrix4f(projectionMatrix).invert()).mul(new Matrix4f(modelViewStack.peek().getPositionMatrix()).invert());
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.byDistance(-camPos.x, -camPos.y, -camPos.z));

        IsometricRenders.beginRenderableDraw();

        RenderSystem.runAsFancy(() -> {
            // Emit untransformed vertices
            renderable.emitVertices(
                    new MatrixStack(),
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                    tickDelta
            );

            // --> Draw
            renderable.draw(modelViewStack.peek().getPositionMatrix());
        });

        IsometricRenders.endRenderableDraw();

        modelViewStack.pop();
        RenderSystem.applyModelViewMatrix();

        renderable.cleanUp();
        RenderSystem.restoreProjectionMatrix();
    }

    /**
     * Directly draws the given renderable into a {@link NativeImage} at the given resolution.
     * This method is essentially just a shorthand for {@code copyFramebufferIntoImage(drawIntoTexture(renderable, size))}
     *
     * @param renderable The renderable to draw
     * @param size       The resolution to render at
     * @return The created image
     */
    public static NativeImage drawIntoImage(Renderable<?> renderable, float tickDelta, int size) {
        return copyFramebufferIntoImage(drawIntoTexture(renderable, tickDelta, size));
    }

    /**
     * Draws the given renderable into a new framebuffer. The FBO and depth attachment
     * are deleted afterwards to save video memory, only the color attachment remains
     *
     * @param renderable The renderable to render
     * @param size       The resolution to render aat
     * @return The framebuffer object holding the pointer to the color attachment
     */
    @SuppressWarnings("ConstantConditions")
    public static Framebuffer drawIntoTexture(Renderable<?> renderable, float tickDelta, int size) {
        final var framebuffer = new SimpleFramebuffer(size, size, true, MinecraftClient.IS_SYSTEM_MAC);

        RenderSystem.enableBlend();
        RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.setClearColor(0, 0, 0, 0);
        framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);

        framebuffer.beginWrite(true);
        drawIntoActiveFramebuffer(renderable, 1, tickDelta, matrixStack -> {});
        framebuffer.endWrite();

        // Release depth attachment and FBO to save on VRAM - we only need
        // the color attachment texture to later turn into an image
        final var accessor = (FramebufferAccessor) framebuffer;
        TextureUtil.releaseTextureId(framebuffer.getDepthAttachment());
        accessor.isometric$setDepthAttachment(-1);

        GlStateManager._glDeleteFramebuffers(accessor.isometric$getFbo());
        accessor.isometric$setFbo(-1);

        return framebuffer;
    }

    /**
     * Copies the given framebuffer's color attachment from video
     * memory in to system memory, wrapped in a {@link NativeImage}
     *
     * @param framebuffer The framebuffer to copy
     * @return The created image
     */
    public static NativeImage copyFramebufferIntoImage(Framebuffer framebuffer) {
        final NativeImage img = new NativeImage(framebuffer.textureWidth, framebuffer.textureHeight, false);

        // This call internally binds the buffer's color attachment texture
        framebuffer.beginRead();

        // This method gets the pixels from the currently bound texture
        img.loadFromTextureImage(0, false);
        img.mirrorVertically();

        framebuffer.delete();

        return img;
    }
}
