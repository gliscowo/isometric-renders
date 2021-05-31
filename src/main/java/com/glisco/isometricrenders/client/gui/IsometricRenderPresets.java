package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.AreaBlockModelRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

import static com.glisco.isometricrenders.client.gui.IsometricRenderHelper.getParticleCamera;

public class IsometricRenderPresets {

    public static void setupBlockStateRender(IsometricRenderScreen screen, @NotNull BlockState state) {
        final MinecraftClient client = MinecraftClient.getInstance();
        final Identifier blockId = Registry.BLOCK.getId(state.getBlock());

        screen.setup((matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();
            matrices.translate(-0.5, 0, -0.5);

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

    public static void setupAreaRender(IsometricRenderScreen screen, @NotNull BlockState[][][] states, BlockPos origin) {

        AreaBlockModelRenderer.prepare();

        final MinecraftClient client = MinecraftClient.getInstance();
        CachedWorldView world = new CachedWorldView(client.world);

        screen.setup((matrices, vertexConsumerProvider, tickDelta) -> {
            final AreaBlockModelRenderer areaBlockRenderer = AreaBlockModelRenderer.get();

            matrices.push();

            matrices.translate(-states[0][0].length / 2f, 0, -states[0].length / 2f);

            int y = 0;
            int x;
            int z;

            BlockModelRenderer.enableBrightnessCache();

            for (BlockState[][] twoDim : states) {
                matrices.push();
                z = 0;
                for (BlockState[] oneDim : twoDim) {
                    matrices.push();
                    x = 0;
                    for (BlockState state : oneDim) {

                        if (state != null) {
                            final BlockPos renderPos = origin.add(x, y, z);

                            matrices.push();

                            areaBlockRenderer.clearCullingOverrides();
                            areaBlockRenderer.setCullDirection(Direction.EAST, x == states[0][0].length - 1);
                            areaBlockRenderer.setCullDirection(Direction.WEST, x == 0);
                            areaBlockRenderer.setCullDirection(Direction.SOUTH, z == states[0].length - 1);
                            areaBlockRenderer.setCullDirection(Direction.NORTH, z == 0);
                            areaBlockRenderer.setCullDirection(Direction.UP, y == states.length - 1);
                            areaBlockRenderer.setCullDirection(Direction.DOWN, y == 0);

                            areaBlockRenderer.render(world, client.getBlockRenderManager().getModel(state), state, renderPos, matrices, vertexConsumerProvider.getBuffer(RenderLayers.getBlockLayer(state)), true, client.world.random, state.getRenderingSeed(renderPos), OverlayTexture.DEFAULT_UV);
                            matrices.pop();
                        }
                        x++;
                        matrices.translate(1, 0, 0);
                    }
                    matrices.pop();
                    z++;
                    matrices.translate(0, 0, 1);
                }
                matrices.pop();
                y++;
                matrices.translate(0, 1, 0);

            }
            matrices.pop();

            BlockModelRenderer.disableBrightnessCache();

        }, "areas/" + "area_render");


    }

    public static void setupBlockEntityRender(IsometricRenderScreen screen, @NotNull BlockEntity entity) {

        final MinecraftClient client = MinecraftClient.getInstance();
        final Identifier blockId = Registry.BLOCK.getId(entity.getCachedState().getBlock());

        screen.setup((matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.push();
            matrices.translate(-0.5, 0, -0.5);

            client.getBlockRenderManager().renderBlockAsEntity(entity.getCachedState(), matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);

            if (BlockEntityRenderDispatcher.INSTANCE.get(entity) != null) {
                BlockEntityRenderDispatcher.INSTANCE.get(entity).render(entity, tickDelta, matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);
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

            if (entity instanceof Tickable) {
                ((Tickable) entity).tick();
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
            matrices.scale(4, 4, 4);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.GROUND, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumerProvider);
            matrices.pop();
        }, itemId.getNamespace() + "/items/" + itemId.getPath());
    }

    public static void setupEntityRender(IsometricRenderScreen screen, @NotNull Entity entity) {

        final MinecraftClient client = MinecraftClient.getInstance();
        final Identifier entityId = Registry.ENTITY_TYPE.getId(entity.getType());

        screen.setup((matrixStack, vertexConsumerProvider, delta) -> {
            matrixStack.push();
            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180));
            entity.setPos(client.player.getX(), client.player.getY(), client.player.getZ());
            client.getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, delta, matrixStack, vertexConsumerProvider, 15728880);

            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-180));
            matrixStack.translate(0, 2, 0);
            client.particleManager.renderParticles(matrixStack, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), delta);
            matrixStack.pop();
        }, entityId.getNamespace() + "/entities/" + entityId.getPath());
        screen.setTickCallback(() -> {
            client.world.tickEntity(entity);
        });
        screen.setClosedCallback(() -> {
            entity.updatePosition(0, 0, 0);
        });
    }

}
