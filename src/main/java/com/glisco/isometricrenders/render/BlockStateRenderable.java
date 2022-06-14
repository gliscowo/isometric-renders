package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.mixin.access.BlockEntityAccessor;
import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ParticleRestriction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BlockStateRenderable extends DefaultRenderable<DefaultPropertyBundle> implements TickingRenderable<DefaultPropertyBundle> {

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final BlockState state;
    private final @Nullable BlockEntity entity;

    public BlockStateRenderable(BlockState state, @Nullable BlockEntity entity) {
        this.state = state;
        this.entity = entity;
    }

    public static BlockStateRenderable of(Block block) {
        return of(block.getDefaultState(), null);
    }

    public static BlockStateRenderable of(BlockState state, @Nullable NbtCompound nbt) {
        final var client = MinecraftClient.getInstance();

        BlockEntity blockEntity = null;

        if (state.getBlock() instanceof BlockEntityProvider provider) {
            blockEntity = provider.createBlockEntity(client.player.getBlockPos(), state);
            prepareBlockEntity(state, blockEntity, nbt);
        }

        return new BlockStateRenderable(state, blockEntity);
    }

    public static BlockStateRenderable copyOf(World world, BlockPos pos) {
        final var state = world.getBlockState(pos);
        final var data = world.getBlockEntity(pos) != null
                ? world.getBlockEntity(pos).createNbt()
                : null;

        return of(state, data);
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        matrices.push();
        matrices.translate(-0.5, -0.5, -0.5);

        if (this.state.getRenderType() != BlockRenderType.ENTITYBLOCK_ANIMATED) {
            client.getBlockRenderManager().renderBlockAsEntity(this.state, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        }

        if (this.entity != null && client.getBlockEntityRenderDispatcher().get(this.entity) != null) {
            client.getBlockEntityRenderDispatcher().get(this.entity).render(entity, tickDelta, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        }

        double xOffset = client.player.getX() % 1d;
        double zOffset = client.player.getZ() % 1d;

        if (xOffset < 0) xOffset += 1;
        if (zOffset < 0) zOffset += 1;

        matrices.translate(xOffset, 1.65 + client.player.getY() % 1d, zOffset);

        client.particleManager.renderParticles(matrices,
                (VertexConsumerProvider.Immediate) vertexConsumers,
                client.gameRenderer.getLightmapTextureManager(),
                getParticleCamera(),
                tickDelta
        );

        matrices.pop();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void tick() {
        if (this.entity != null && this.state.getBlockEntityTicker(client.world, this.entity.getType()) != null) {
            final var ticker = this.state.getBlockEntityTicker(client.world, (BlockEntityType<BlockEntity>) this.entity.getType());
            if (ticker == null) return;

            ticker.tick(client.world, client.player.getBlockPos(), this.state, this.entity);
        }

        if (client.world.random.nextDouble() < 0.150) {
            this.state.getBlock().randomDisplayTick(this.state, client.world, client.player.getBlockPos(), client.world.random);
        }
    }

    @Override
    public DefaultPropertyBundle properties() {
        return DefaultPropertyBundle.get();
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
        return ParticleRestriction.duringTick();
    }

    @Override
    public ExportPathSpec exportPath() {
        return ExportPathSpec.ofIdentified(
                Registry.BLOCK.getId(this.state.getBlock()),
                "block"
        );
    }

    private static void prepareBlockEntity(BlockState state, BlockEntity blockEntity, @Nullable NbtCompound nbt) {
        if (blockEntity == null) return;

        ((BlockEntityAccessor) blockEntity).isometric$setCachedState(state);
        blockEntity.setWorld(MinecraftClient.getInstance().world);

        if (nbt == null) return;

        final var nbtCopy = nbt.copy();

        nbtCopy.putInt("x", 0);
        nbtCopy.putInt("y", 0);
        nbtCopy.putInt("z", 0);

        blockEntity.readNbt(nbtCopy);
    }
}
