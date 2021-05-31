package com.glisco.isometricrenders.client;

import com.glisco.isometricrenders.mixin.BlockModelRendererInvoker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import java.util.BitSet;
import java.util.List;
import java.util.Random;

public class AreaBlockModelRenderer extends BlockModelRenderer {

    private byte cullingOverrides = 0;

    private static AreaBlockModelRenderer INSTANCE;

    public static AreaBlockModelRenderer get() {
        return INSTANCE;
    }

    public static void prepare() {
        if (INSTANCE != null) return;
        INSTANCE = new AreaBlockModelRenderer(MinecraftClient.getInstance().getBlockColors());
    }

    private AreaBlockModelRenderer(BlockColors colorMap) {
        super(colorMap);
    }

    public void setCullDirection(Direction direction, boolean alwaysDraw) {
        if (!alwaysDraw) return;
        cullingOverrides |= (1 << direction.getId());
    }

    public void clearCullingOverrides() {
        cullingOverrides = 0;
    }

    @Override
    public boolean renderSmooth(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack buffer, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean anyFacesRendered = false;
        float[] fs = new float[12];
        BitSet flags = new BitSet(3);
        BlockModelRenderer.AmbientOcclusionCalculator ambientOcclusionCalculator = new BlockModelRenderer.AmbientOcclusionCalculator();
        final BlockModelRendererInvoker invoker = (BlockModelRendererInvoker) this;

        for (Direction direction : Direction.values()) {
            random.setSeed(seed);
            List<BakedQuad> faceQuads = model.getQuads(state, direction, random);

            if (!faceQuads.isEmpty() && (!cull || shouldAlwaysDraw(direction) || Block.shouldDrawSide(state, world, pos, direction))) {
                invoker.isometric_renderQuadsSmooth(world, state, !shouldAlwaysDraw(direction) ? pos : pos.add(0, 500, 0), buffer, vertexConsumer, faceQuads, fs, flags, ambientOcclusionCalculator, overlay);
                anyFacesRendered = true;
            }
        }

        random.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(state, null, random);
        if (!quads.isEmpty()) {
            invoker.isometric_renderQuadsSmooth(world, state, pos, buffer, vertexConsumer, quads, fs, flags, ambientOcclusionCalculator, overlay);
            anyFacesRendered = true;
        }

        return anyFacesRendered;
    }

    @Override
    public boolean renderFlat(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack buffer, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean anyFacesRendered = false;
        BitSet flags = new BitSet(3);
        final BlockModelRendererInvoker invoker = (BlockModelRendererInvoker) this;

        for (Direction direction : Direction.values()) {
            random.setSeed(seed);
            List<BakedQuad> faceQuads = model.getQuads(state, direction, random);

            if (!faceQuads.isEmpty() && (!cull || shouldAlwaysDraw(direction) || Block.shouldDrawSide(state, world, pos, direction))) {
                int light = WorldRenderer.getLightmapCoordinates(world, state, pos.offset(direction));
                invoker.isometric_renderQuadsFlat(world, state, !shouldAlwaysDraw(direction) ? pos : pos.add(0, 500, 0), light, overlay, false, buffer, vertexConsumer, faceQuads, flags);
                anyFacesRendered = true;
            }
        }

        random.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(state, null, random);
        if (!quads.isEmpty()) {
            invoker.isometric_renderQuadsFlat(world, state, pos, -1, overlay, true, buffer, vertexConsumer, quads, flags);
            anyFacesRendered = true;
        }

        return anyFacesRendered;
    }

    private boolean shouldAlwaysDraw(Direction direction) {
        return (cullingOverrides & (1 << direction.getId())) != 0;
    }

}
