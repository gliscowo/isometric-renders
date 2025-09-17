package com.glisco.isometricrenders.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.gl.Framebuffer;

public class FramebufferUtils {
	public static GpuTexture cloneColorAttachment(Framebuffer framebuffer) {
		var original = framebuffer.getColorAttachment();
		assert original != null;
		var gpuDevice = RenderSystem.getDevice();
		var copy = gpuDevice.createTexture(() -> "[IsometricRenders] Copy of: " + original.getLabel(),
				GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
				TextureFormat.RGBA8, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);

		gpuDevice.createCommandEncoder()
				.copyTextureToTexture(original, copy, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);

		return copy;
	}
}
