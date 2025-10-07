package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.util.FramebufferUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RenderableDispatcher {

	private static final RawProjectionMatrix projMatrix = new RawProjectionMatrix("RenderableDispatcher");

    /**
     * Renders the given renderable into the current framebuffer,
     * with the projection matrix adjusted to compensate for the buffer's
     * aspect ratio
     *
     * @param renderable  The renderable to draw
     * @param aspectRatio The aspect ratio of the current framebuffer
     * @param tickDelta   The tick delta to use
     */
    public static void drawIntoActiveFramebuffer(Renderable<?> renderable, float aspectRatio, float tickDelta, Consumer<Matrix4fStack> transformer) {

        renderable.prepare();

        // Prepare model view matrix
        final var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();

        transformer.accept(modelViewStack);

        renderable.properties().applyToViewMatrix(modelViewStack);

        Matrix4f projectionMatrix = new Matrix4f().setOrtho(-aspectRatio, aspectRatio, -1, 1, -1000, 3000);
	    IsometricRenders.beginRenderableDraw(projMatrix, projectionMatrix);

        renderable.setupLighting(modelViewStack);

        // TODO replacement?
//        RenderSystem.runAsFancy(() -> {
            // Emit untransformed vertices
            renderable.emitVertices(
                    new MatrixStack(),
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                    tickDelta
            );

            // --> Draw
            renderable.draw(modelViewStack);
//        });

        IsometricRenders.endRenderableDraw();

        modelViewStack.popMatrix();

        renderable.cleanUp();
    }

    /**
     * Directly draws the given renderable into a {@link NativeImage} at the given resolution.
     * This method is essentially just a shorthand for {@code copyFramebufferIntoImage(drawIntoTexture(renderable, size))}
     *
     * @param renderable The renderable to draw
     * @param size       The resolution to render at
     * @return The created image
     */
    public static CompletableFuture<NativeImage> drawIntoImage(Renderable<?> renderable, float tickDelta, int size) {
		final var texture = drawIntoTexture(renderable, tickDelta, size);
        return copyTextureIntoImage(texture).whenComplete((i, t) -> texture.close());
    }

    /**
     * Draws the given renderable into a new framebuffer. The FBO and depth attachment
     * are deleted afterwards to save video memory, only the color attachment remains
     *
     * @param renderable The renderable to render
     * @param size       The resolution to render aat
     * @return The color attachment
     */
    @SuppressWarnings("ConstantConditions")
    public static GpuTexture drawIntoTexture(Renderable<?> renderable, float tickDelta, int size) {
        final var framebuffer = new SimpleFramebuffer("Isometric Renders RenderableDispatcher.drawIntoTexture Framebuffer", size, size, true);

	    IsometricRenders.mainTargetOverride = framebuffer;
		RenderSystem.outputColorTextureOverride = framebuffer.getColorAttachmentView();
		RenderSystem.outputDepthTextureOverride = framebuffer.getDepthAttachmentView();

        drawIntoActiveFramebuffer(renderable, 1, tickDelta, matrixStack -> {});

	    RenderSystem.outputColorTextureOverride = null;
	    RenderSystem.outputDepthTextureOverride = null;
	    IsometricRenders.mainTargetOverride = null;
	    var texture = FramebufferUtils.cloneColorAttachment(framebuffer);

	    // Release depth attachment and FBO to save on VRAM - we only need
	    // the color attachment texture to later turn into an image
		framebuffer.delete();

        return texture;
    }

    /**
     * Copies the given color attachment from video
     * memory in to system memory, wrapped in a {@link NativeImage}
     *
     * @param gpuTexture The texture to copy
     * @return The created image
     */

	public static CompletableFuture<NativeImage> copyTextureIntoImage(@NotNull GpuTexture gpuTexture) {
		var future = new CompletableFuture<NativeImage>();

		ScreenshotRecorder.takeScreenshot(new Framebuffer(null, false) {
			{
				this.textureWidth = gpuTexture.getWidth(0);
				this.textureHeight = gpuTexture.getHeight(0);
				this.colorAttachment = gpuTexture;
			}
		}, future::complete);

		return future;
	}
}
