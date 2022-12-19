package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public interface Renderable<P extends PropertyBundle> {

    Renderable<PropertyBundle> EMPTY = new EmptyRenderable();

    default void prepare() {}

    void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta);

    void draw(Matrix4f modelViewMatrix);

    default void cleanUp() {}

    default void dispose() {}

    default ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.never();
    }

    P properties();

    ExportPathSpec exportPath();
}
