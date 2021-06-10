package com.glisco.isometricrenders.client.gui;

import com.glisco.worldmesher.WorldMesh;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import static com.glisco.isometricrenders.client.RuntimeConfig.*;

public class AreaIsometricRenderScreen extends IsometricRenderScreen {

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        if (this.renderCallback instanceof AreaRenderCallback) ((AreaRenderCallback) this.renderCallback).doSquareFramebufferOnce();
        return super.getExternalExportCallback();
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        super.drawGuiText(matrices);

        final var renderCallback = (AreaRenderCallback) this.renderCallback;
        StringBuilder buildString = new StringBuilder("§cBuilding - ");
        buildString.append(Math.round(renderCallback.getMeshProgress() * 100));
        buildString.append("%");

        MinecraftClient.getInstance().textRenderer.draw(matrices, "Mesh Status: " + (renderCallback.canRender() ? "§aReady" : buildString), 12, 200, 0xAAAAAA);
    }

    public static class AreaRenderCallback implements IsometricRenderHelper.RenderCallback {

        private final Window window = MinecraftClient.getInstance().getWindow();
        private final WorldMesh mesh;

        private final int xSize;
        private final int zSize;

        private boolean scaleFramebuffer = true;

        public AreaRenderCallback(BlockPos origin, BlockPos end) {
            mesh = new WorldMesh.Builder(MinecraftClient.getInstance().world, origin, end).build();
            xSize = 1 + Math.max(origin.getX(), end.getX()) - Math.min(origin.getX(), end.getX());
            zSize = 1 + Math.max(origin.getZ(), end.getZ()) - Math.min(origin.getZ(), end.getZ());
        }

        public boolean canRender() {
            return mesh.canRender();
        }

        public float getMeshProgress() {
            return mesh.getBuildProgress();
        }

        public void doSquareFramebufferOnce() {
            this.scaleFramebuffer = false;
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, float tickDelta) {
            if (!mesh.canRender()) {
                mesh.scheduleRebuild();
            } else {
                if (mesh.canRender()) {
                    float windowScale = scaleFramebuffer ? window.getFramebufferHeight() / (float) window.getFramebufferWidth() : 1;
                    if (!scaleFramebuffer) scaleFramebuffer = true;

                    RenderSystem.backupProjectionMatrix();
                    Matrix4f projectionMatrix = Matrix4f.projectionMatrix(-1 / windowScale, 1 / windowScale, 1, -1, -1000, 3000);
                    RenderSystem.setProjectionMatrix(projectionMatrix);

                    final MatrixStack stack = new MatrixStack();

                    stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(angle));
                    stack.multiply(Vec3f.NEGATIVE_Y.getDegreesQuaternion(rotation));
                    stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180));

                    float scaledRenderScale = renderScale * (window.getScaledHeight() / 515f) * 0.001f;
                    stack.scale(scaledRenderScale, -scaledRenderScale, -scaledRenderScale);

                    stack.translate(-xSize / 2f, (renderHeight - 130) * -0.1, -zSize / 2f);

                    mesh.render(stack.peek().getModel());

                    RenderSystem.restoreProjectionMatrix();
                }
            }
        }
    }
}
