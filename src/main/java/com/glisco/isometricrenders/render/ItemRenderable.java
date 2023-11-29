package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.RotationAxis;

public class ItemRenderable extends DefaultRenderable<DefaultPropertyBundle> {

    private static BakedModel currentModel = null;
    private static final DefaultPropertyBundle PROPERTIES = new DefaultPropertyBundle() {
        @Override
        public void applyToViewMatrix(MatrixStack modelViewStack) {
            final float scale = (this.scale.get() / 100f) * (currentModel != null && currentModel.hasDepth() ? 2f : 1.75f);
            modelViewStack.scale(scale, scale, scale);

            modelViewStack.translate(this.xOffset.get() / 26000d, this.yOffset.get() / -26000d, 0);

            modelViewStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.slant.get()));
            if (currentModel != null) currentModel.getTransformation().getTransformation(ModelTransformationMode.GUI).apply(false, modelViewStack);
            modelViewStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotation.get()));

            this.updateAndApplyRotationOffset(modelViewStack);
        }
    };

    static {
        PROPERTIES.slant.setDefaultValue(0).setToDefault();
        PROPERTIES.rotation.setDefaultValue(0).setToDefault();
    }

    private final ItemStack stack;

    public ItemRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void prepare() {
        var itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        if (this.stack.isOf(Items.TRIDENT)) {
            currentModel = itemRenderer.getModels().getModelManager().getModel(ModelIdentifier.ofVanilla("trident", "inventory"));
        } else if (this.stack.isOf(Items.SPYGLASS)) {
            currentModel = itemRenderer.getModels().getModelManager().getModel(ModelIdentifier.ofVanilla("spyglass", "inventory"));
        } else {
            currentModel = itemRenderer.getModel(this.stack, MinecraftClient.getInstance().world, null, 0);
        }
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        final var itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        final var model = itemRenderer.getModel(this.stack, null, null, 0);

        itemRenderer.renderItem(
                this.stack,
                ModelTransformationMode.GUI,
                false,
                matrices,
                vertexConsumers,
                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV,
                new TransformlessBakedModel(model)
        );
    }

    @Override
    public void cleanUp() {
        currentModel = null;
    }

    @Override
    public DefaultPropertyBundle properties() {
        return PROPERTIES;
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.ofIdentified(
                Registries.ITEM.getId(this.stack.getItem()),
                "item"
        );
    }

    private static class TransformlessBakedModel extends ForwardingBakedModel {
        public TransformlessBakedModel(BakedModel inner) {
            this.wrapped = inner;
        }

        @Override
        public ModelTransformation getTransformation() {
            return ModelTransformation.NONE;
        }
    }
}
