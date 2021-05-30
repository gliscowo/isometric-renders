package com.glisco.isometricrenders.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.BitSet;
import java.util.List;

@Mixin(BlockModelRenderer.class)
public interface BlockModelRendererInvoker {

    @Invoker("renderQuadsSmooth")
    void isometric_renderQuadsSmooth(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrix, VertexConsumer vertexConsumer, List<BakedQuad> quads, float[] box, BitSet flags, BlockModelRenderer.AmbientOcclusionCalculator ambientOcclusionCalculator, int overlay);

    @Invoker("renderQuadsFlat")
    void isometric_renderQuadsFlat(BlockRenderView world, BlockState state, BlockPos pos, int light, int overlay, boolean useWorldLight, MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, BitSet flags);

}
