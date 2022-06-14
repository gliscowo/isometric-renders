package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.CameraInvoker;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;

public abstract class DefaultRenderable<P extends DefaultPropertyBundle> implements Renderable<P> {

    @Override
    public void draw(Matrix4f modelViewMatrix) {
        // Apply inverse transform to lighting to keep it consistent
        final var lightDirection = getLightDirection();
        final var lightTransform = modelViewMatrix.copy();
        lightTransform.invert();
        lightDirection.transform(lightTransform);

        final var transformedLightDirection = new Vec3f(lightDirection.getX(), lightDirection.getY(), lightDirection.getZ());
        RenderSystem.setShaderLights(transformedLightDirection, transformedLightDirection);

        // Draw all buffers
        MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().draw();
    }

    protected Camera getParticleCamera() {
        Camera camera = MinecraftClient.getInstance().getEntityRenderDispatcher().camera;
        ((CameraInvoker) camera).isometric$setRotation(this.properties().rotation.get() + 180 + this.properties().rotationOffset(), this.properties().slant.get());
        return camera;
    }

    protected Vector4f getLightDirection() {
        return new Vector4f(this.properties().lightAngle.get() / 90f, .35f, 1, 0);
    }
}
