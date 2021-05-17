package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.export.ExportMetadata;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tickable;
import org.jetbrains.annotations.NotNull;

import static com.glisco.isometricrenders.client.gui.IsometricRenderHelper.getParticleCamera;

public class IsometricRenderPresets {

    public static void setupBlockStateRender(IsometricRenderScreen screen, @NotNull BlockState state) {
        final MinecraftClient client = MinecraftClient.getInstance();

        screen.setExportMetadata(new ExportMetadata.Block(state));
        screen.setRenderCallback((matrices, vertexConsumerProvider, tickDelta) -> {
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
        });

        screen.setTickCallback(() -> {
            if (client.world.random.nextDouble() < 0.150) {
                state.getBlock().randomDisplayTick(state, client.world, client.player.getBlockPos(), client.world.random);
            }
        });
    }

    public static void setupAreaRender(IsometricRenderScreen screen, @NotNull BlockState[][][] states) {
        final MinecraftClient client = MinecraftClient.getInstance();

        screen.setExportMetadata(new ExportMetadata.Area("area_render", states));
        screen.setRenderCallback((matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();

            matrices.translate(-states[0][0].length / 2f, 0, -states[0].length / 2f);

            for (BlockState[][] twoDim : states) {
                matrices.push();
                for (BlockState[] oneDim : twoDim) {
                    matrices.push();
                    for (BlockState state : oneDim) {
                        client.getBlockRenderManager().renderBlockAsEntity(state, matrices, vertexConsumerProvider, 15728880, OverlayTexture.DEFAULT_UV);
                        matrices.translate(1, 0, 0);
                    }
                    matrices.pop();
                    matrices.translate(0, 0, 1);
                }
                matrices.pop();
                matrices.translate(0, 1, 0);
            }

            matrices.pop();
        });
    }

    public static void setupBlockEntityRender(IsometricRenderScreen screen, @NotNull BlockEntity entity) {

        final MinecraftClient client = MinecraftClient.getInstance();

        screen.setExportMetadata(new ExportMetadata.Block(entity.getCachedState()));
        screen.setRenderCallback((matrices, vertexConsumerProvider, tickDelta) -> {

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
        });

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

        screen.setExportMetadata(new ExportMetadata.Item(stack));
        screen.setRenderCallback((matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();
            matrices.scale(4, 4, 4);
            MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformation.Mode.GROUND, 15728880, OverlayTexture.DEFAULT_UV, matrices, vertexConsumerProvider);
            matrices.pop();
        });
    }

    public static void setupEntityRender(IsometricRenderScreen screen, @NotNull Entity entity) {

        final MinecraftClient client = MinecraftClient.getInstance();

        screen.setExportMetadata(new ExportMetadata.EntityData(entity));
        screen.setRenderCallback((matrixStack, vertexConsumerProvider, delta) -> {
            matrixStack.push();
            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180));
            entity.setPos(client.player.getX(), client.player.getY(), client.player.getZ());
            client.getEntityRenderDispatcher().render(entity, 0, 0, 0, 0, delta, matrixStack, vertexConsumerProvider, 15728880);

            matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-180));
            matrixStack.translate(0, 2, 0);
            client.particleManager.renderParticles(matrixStack, (VertexConsumerProvider.Immediate) vertexConsumerProvider, client.gameRenderer.getLightmapTextureManager(), getParticleCamera(), delta);
            matrixStack.pop();
        });
        screen.setTickCallback(() -> {
            client.world.tickEntity(entity);
        });
        screen.setClosedCallback(() -> {
            entity.updatePosition(0, 0, 0);
        });
    }

}
