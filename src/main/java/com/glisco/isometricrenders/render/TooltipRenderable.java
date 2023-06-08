package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.IsometricRenders;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.util.ExportPathSpec;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.RotationAxis;

public class TooltipRenderable extends DefaultRenderable<TooltipRenderable.TooltipPropertyBundle> {

    private final ItemStack stack;

    public TooltipRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        IsometricRenders.centerNextTooltip = true;

        var client = MinecraftClient.getInstance();
        new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers())
                .drawItemTooltip(client.textRenderer, this.stack, 0, 0);
    }

    @Override
    public TooltipPropertyBundle properties() {
        return TooltipPropertyBundle.INSTANCE;
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.of("tooltip", Registries.ITEM.getId(stack.getItem()).getPath());
    }

    public static class TooltipPropertyBundle extends DefaultPropertyBundle {
        public static final TooltipPropertyBundle INSTANCE = new TooltipPropertyBundle();

        @Override
        public void buildGuiControls(Renderable<?> renderable, FlowLayout container) {
            IsometricUI.sectionHeader(container, "transform_options", false);
            IsometricUI.intControl(container, this.scale, "scale", 10);
        }

        @Override
        public void applyToViewMatrix(MatrixStack modelViewStack) {
            final float scale = this.scale.get() / 10000f;
            modelViewStack.scale(scale, scale, -scale);

            modelViewStack.translate(this.xOffset.get() / 260d, this.yOffset.get() / -260d, 0);
            modelViewStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
            modelViewStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
        }
    }
}
