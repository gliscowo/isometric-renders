package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.screen.AreaIsometricRenderScreen;
import com.glisco.isometricrenders.screen.IsometricRenderScreen;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.glisco.isometricrenders.render.IsometricRenderHelper.getParticleCamera;

public class IsometricRenderPresets {

    public static void setupBlockStateRender(IsometricRenderScreen screen, @NotNull BlockState state) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final Identifier blockId = Registry.BLOCK.getId(state.getBlock());

        screen.setup((matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();
            matrices.translate(-0.5, -0.5, -0.5);

            client.getBlockRenderManager().renderBlockAsEntity(state, matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);

            double xOffset = client.player.getX() % 1d;
            double zOffset = client.player.getZ() % 1d;

            if (xOffset < 0) xOffset += 1;
            if (zOffset < 0) zOffset += 1;

            matrices.translate(xOffset, 1.65 + client.player.getY() % 1d, zOffset);

            client.particleManager.renderParticles(matrices, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), tickDelta);

            matrices.pop();
        }, blockId.getNamespace() + "/blocks/" + blockId.getPath());

        screen.setTickCallback(() -> {
            if (client.world.random.nextDouble() < 0.150) {
                state.getBlock().randomDisplayTick(state, client.world, client.player.getBlockPos(), client.world.random);
            }
        });
    }

    public static void setupAreaRender(IsometricRenderScreen screen, BlockPos origin, BlockPos end, boolean enableTranslucency) {
        screen.setup(new AreaIsometricRenderScreen.AreaRenderCallback(origin, end, enableTranslucency), "areas/" + "area_render");
    }

    public static void setupBlockEntityRender(IsometricRenderScreen screen, @NotNull BlockEntity entity) {

        final MinecraftClient client = MinecraftClient.getInstance();
        final Identifier blockId = Registry.BLOCK.getId(entity.getCachedState().getBlock());

        screen.setup((matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.push();
            matrices.translate(-0.5, -0.5, -0.5);

            client.getBlockRenderManager().renderBlockAsEntity(entity.getCachedState(), matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);

            if (client.getBlockEntityRenderDispatcher().get(entity) != null) {
                client.getBlockEntityRenderDispatcher().get(entity).render(entity, tickDelta, matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);
            }

            double xOffset = client.player.getX() % 1d;
            double zOffset = client.player.getZ() % 1d;

            if (xOffset < 0) xOffset += 1;
            if (zOffset < 0) zOffset += 1;

            matrices.translate(xOffset, 1.65 + client.player.getY() % 1d, zOffset);

            client.particleManager.renderParticles(matrices, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), tickDelta);

            matrices.pop();
        }, blockId.getNamespace() + "/blocks/" + blockId.getPath());

        screen.setTickCallback(() -> {

            if (entity.getCachedState().getBlockEntityTicker(client.world, entity.getType()) != null) {
                entity.getCachedState().getBlockEntityTicker(client.world, (BlockEntityType<BlockEntity>) entity.getType()).tick(client.world, client.player.getBlockPos(), entity.getCachedState(), entity);
            }

            if (client.world.random.nextDouble() < 0.150) {
                entity.getCachedState().getBlock().randomDisplayTick(entity.getCachedState(), client.world, client.player.getBlockPos(), client.world.random);
            }
        });

    }

    public static void setupItemStackRender(IsometricRenderScreen screen, @NotNull ItemStack stack) {

        final Identifier itemId = Registry.ITEM.getId(stack.getItem());

        screen.setup((matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();
            matrices.translate(0, stack.getItem() instanceof BlockItem ? -0.75 : -0.5, 0);
            matrices.scale(4, 4, 4);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.GROUND, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumerProvider, 0);
            matrices.pop();
        }, itemId.getNamespace() + "/items/" + itemId.getPath());
    }

    public static void setupEntityRender(IsometricRenderScreen screen, @NotNull Entity rootEntity) {

        final MinecraftClient client = MinecraftClient.getInstance();
        final Identifier entityId = Registry.ENTITY_TYPE.getId(rootEntity.getType());

        screen.setup((matrixStack, vertexConsumerProvider, delta) -> {
            matrixStack.push();

            matrixStack.translate(0, 0.1 + rootEntity.getHeight() * -0.5d, 0);
            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));

            final MutableFloat y = new MutableFloat();

            applyToEntityAndPassengers(rootEntity, entity -> {
                entity.setPos(client.player.getX(), client.player.getY(), client.player.getZ());
                y.add(entity.hasVehicle() ? entity.getVehicle().getMountedHeightOffset() + entity.getHeightOffset() : 0);

                client.getEntityRenderDispatcher().render(entity, 0, y.floatValue(), 0, 0, delta, matrixStack, vertexConsumerProvider, 15728880);
            });

            matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-180));
            matrixStack.translate(0, 1.65, 0);
            client.particleManager.renderParticles(matrixStack, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), delta);
            matrixStack.pop();
        }, entityId.getNamespace() + "/entities/" + entityId.getPath());
        screen.setTickCallback(() -> {
            applyToEntityAndPassengers(rootEntity, entity -> client.world.tickEntity(entity));
        });
        screen.setClosedCallback(() -> {
            applyToEntityAndPassengers(rootEntity, entity -> entity.updatePosition(0, 0, 0));
        });
    }

    private static void applyToEntityAndPassengers(Entity entity, Consumer<Entity> action) {
        action.accept(entity);
        if (entity.getPassengerList().isEmpty()) return;
        for (Entity e : entity.getPassengerList()) applyToEntityAndPassengers(e, action);
    }

}
