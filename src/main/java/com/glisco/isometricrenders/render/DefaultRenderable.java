package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.CameraInvoker;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.function.Consumer;

public abstract class DefaultRenderable<P extends DefaultPropertyBundle> implements Renderable<P> {

    @Override
    public void draw(Matrix4f modelViewMatrix) {
        // Apply inverse transform to lighting to keep it consistent
        final var lightDirection = getLightDirection();
        final var lightTransform = new Matrix4f(modelViewMatrix);
        lightTransform.invert();
        lightDirection.mul(lightTransform);

        final var transformedLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);
        RenderSystem.setShaderLights(transformedLightDirection, transformedLightDirection);

        // Draw all buffers
        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
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
