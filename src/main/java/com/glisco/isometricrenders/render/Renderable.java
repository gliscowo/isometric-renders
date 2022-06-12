package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

public interface Renderable<P extends PropertyBundle> {

    Renderable<PropertyBundle> EMPTY = new EmptyRenderable();

    default void prepare() {}

    void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta);

    void draw(Matrix4f modelViewMatrix);

    default void cleanUp() {}

    default void dispose() {}

    P properties();

    ExportPathSpec exportPath();
}
