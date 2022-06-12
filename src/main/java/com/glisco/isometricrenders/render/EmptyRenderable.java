package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.widget.WidgetColumnBuilder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

public class EmptyRenderable implements Renderable<PropertyBundle> {

    private static final PropertyBundle EMPTY_BUNDLE = new PropertyBundle() {
        @Override
        public void buildGuiControls(Renderable<?> renderable, WidgetColumnBuilder builder) {}

        @Override
        public void applyToViewMatrix(MatrixStack modelViewStack) {}
    };

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {}

    @Override
    public void draw(Matrix4f modelViewMatrix) {}

    @Override
    public PropertyBundle properties() {
        return EMPTY_BUNDLE;
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("", "empty");
    }
}
