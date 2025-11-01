package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.util.FramebufferUtils;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.system.MemoryUtil;

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
	    RenderSystem.getDevice().createCommandEncoder()
			    .clearColorAndDepthTextures(framebuffer.getColorAttachment(), 0, framebuffer.getDepthAttachment(), 1.0);

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
		final var future = new CompletableFuture<NativeImage>();

		final int width  = gpuTexture.getWidth (0);
		final int height = gpuTexture.getHeight(0);

		// Optimized version of vanilla's ScreenshotRecorder.takeScreenshot
		// that simply copies an RGBA8 GpuTexture's contents to an RGBA NativeImage, with vertical flipping.

		// Color attachments [in vanilla] are always RGBA8, therefore != RGBA8 implies non-color attachment
		if (gpuTexture.getFormat() != TextureFormat.RGBA8)
			throw new IllegalStateException("Tried to copy non-compatible texture into image");

		GpuBuffer gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "Isometric Renders RenderableDispatcher.copyTextureIntoImage buffer", GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, 4 * width * height);
		CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
		RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gpuTexture, gpuBuffer, 0, () -> {
			try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(gpuBuffer, true, false)) {
				NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

				// Skip redundant safety checks, do the memory copies directly.
				final long stride = 4L * width;
				final long srcBuf = MemoryUtil.memAddress(mappedView.data());
				final long dstBuf = nativeImage.imageId();

				long src = srcBuf;
				long dst = dstBuf + stride * (height - 1);

				for (int y = 0; y < height; y++) {
					MemoryUtil.memCopy(src, dst, stride);
					src += stride;
					dst -= stride;
				}

				future.complete(nativeImage);
			}

			gpuBuffer.close();
		}, 0);

		return future;
	}
}
