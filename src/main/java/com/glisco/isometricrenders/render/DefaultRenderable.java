package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.CameraInvoker;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.mojang.blaze3d.buffers.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public abstract class DefaultRenderable<P extends DefaultPropertyBundle> implements Renderable<P> {

	private static final int LIGHTING_UBO_SIZE = new Std140SizeCalculator().putVec3().putVec3().get();
	private GpuBuffer lightingBuffer;

    @Override
    public void setupLighting(Matrix4f modelViewMatrix) {
        // Apply inverse transform to lighting to keep it consistent
        final var lightDirection = getLightDirection();
        final var lightTransform = new Matrix4f(modelViewMatrix);
        lightTransform.invert();
        lightDirection.mul(lightTransform);

        final var transformedLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);

		// Lazily create the lighting UBO buffer when it's actually needed.
		if (this.lightingBuffer == null)
			this.lightingBuffer = RenderSystem.getDevice().createBuffer(() -> "IsometricRenders DefaultRenderable Lighting UBO", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, LIGHTING_UBO_SIZE);

	    try (MemoryStack memoryStack = MemoryStack.stackPush()) {
		    ByteBuffer byteBuffer = Std140Builder.onStack(memoryStack, LIGHTING_UBO_SIZE).putVec3(transformedLightDirection).putVec3(transformedLightDirection).get();
		    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.lightingBuffer.slice(), byteBuffer);
	    }

        RenderSystem.setShaderLights(this.lightingBuffer.slice());
    }

	@Override
	public void dispose() {
		if (this.lightingBuffer != null) {
			this.lightingBuffer.close();
			this.lightingBuffer = null;
		}
	}

	@Override
    public void draw(Matrix4f modelViewMatrix) {
        // Draw all buffers
        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
    }

    protected void renderParticles(Matrix4f transform, float tickDelta) {
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(transform);

        var client = MinecraftClient.getInstance();
        this.withParticleCamera(camera -> {
            client.particleManager.renderParticles(
                camera,
                tickDelta,
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()
            );
        });

        modelView.popMatrix();
    }

    protected void withParticleCamera(Consumer<Camera> action) {
        Camera camera = MinecraftClient.getInstance().getEntityRenderDispatcher().camera;
        float previousYaw = camera.getYaw(), previousPitch = camera.getPitch();

        ((CameraInvoker) camera).isometric$setRotation(this.properties().rotation.get() + 180 + this.properties().rotationOffset(), this.properties().slant.get());
        action.accept(camera);

        ((CameraInvoker) camera).isometric$setRotation(previousYaw, previousPitch);
    }

    protected Vector4f getLightDirection() {
        return new Vector4f(this.properties().lightAngle.get() / 90f, .35f, 1, 0);
    }
}
