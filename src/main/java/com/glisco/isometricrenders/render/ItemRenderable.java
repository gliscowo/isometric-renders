package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;

public class ItemRenderable extends DefaultRenderable<DefaultPropertyBundle> {

    private static final DefaultPropertyBundle PROPERTIES = new DefaultPropertyBundle();

    static {
        PROPERTIES.slant.setDefaultValue(0).setToDefault();
        PROPERTIES.rotation.setDefaultValue(180).setToDefault();
    }

    private final ItemStack stack;

    public ItemRenderable(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        final var itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        final var hasDepth = itemRenderer.getModel(this.stack, null, null, 0).hasDepth();
        final float scale = hasDepth ? 2f : 1.75f;

        matrices.push();
        matrices.scale(scale, scale, scale);

        // This funny matrix manipulation here is done in order to
        // avoid funny axis rotation on models with depth
        if (hasDepth) {
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-this.properties().rotation.get()));
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(30));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(this.properties().rotation.get() + 135));
        }

        itemRenderer.renderItem(this.stack, ModelTransformation.Mode.FIXED, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, 0);
        matrices.pop();
    }

    @Override
    public DefaultPropertyBundle properties() {
        return PROPERTIES;
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.ofIdentified(
                Registry.ITEM.getId(this.stack.getItem()),
                "item"
        );
    }
}
