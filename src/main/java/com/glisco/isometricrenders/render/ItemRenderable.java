package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.compatibility.PolymerSupport;
import com.glisco.isometricrenders.mixin.access.ItemRenderStateAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4fStack;

public class ItemRenderable extends DefaultRenderable<DefaultPropertyBundle> {

    private static final ItemRenderState RENDER_STATE = new ItemRenderState();
    private static final DefaultPropertyBundle PROPERTIES = new DefaultPropertyBundle() {
        @Override
        public void applyToViewMatrix(Matrix4fStack modelViewStack) {
            final float scale = (this.scale.get() / 100f) * (RENDER_STATE.hasDepth() ? 2f : 1.75f);
            modelViewStack.scale(scale, scale, scale);

            modelViewStack.translate(this.xOffset.get() / 26000f, this.yOffset.get() / -26000f, 0);

            modelViewStack.rotate(RotationAxis.POSITIVE_X.rotationDegrees(this.slant.get()));
            var bruhMatrices = new MatrixStack();
            RENDER_STATE.getTransformation().apply(false, bruhMatrices);
            modelViewStack.mul(bruhMatrices.peek().getPositionMatrix());
            modelViewStack.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(this.rotation.get()));

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
        MinecraftClient.getInstance().getItemModelManager().update(
            RENDER_STATE,
            this.stack,
            ModelTransformationMode.GUI,
            false,
            MinecraftClient.getInstance().world,
            null,
            0
        );
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        ((ItemRenderStateAccessor) RENDER_STATE).isometric$setTransformationMode(ModelTransformationMode.NONE);
        RENDER_STATE.render(matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
    }

    @Override
    public void cleanUp() {
        RENDER_STATE.clear();
    }

    @Override
    public DefaultPropertyBundle properties() {
        return PROPERTIES;
    }

    @Override
    public ExportPathSpec exportPath() {
        var id = PolymerSupport.getPolymerId(this.stack);
        return ExportPathSpec.ofIdentifiedAndNamed(
                id != null ? id : Registries.ITEM.getId(this.stack.getItem()),
                this.stack.getName(),
            "item"
        );
    }

//    private static class TransformlessBakedModel extends ForwardingBakedModel {
//        public TransformlessBakedModel(BakedModel inner) {
//            this.wrapped = inner;
//        }
//
//        @Override
//        public ModelTransformation getTransformation() {
//            return ModelTransformation.NONE;
//        }
//    }
}
