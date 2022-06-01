package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.util.Translate;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.worldmesher.WorldMesh;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.glfw.GLFW;

import static com.glisco.isometricrenders.util.RuntimeConfig.*;

public class AreaIsometricRenderScreen extends IsometricRenderScreen {

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        if (this.renderCallback instanceof AreaRenderCallback areaCallback) areaCallback.doSquareFramebufferOnce();
        return super.getExternalExportCallback();
    }

    @Override
    protected void buildGuiElements() {
        super.buildGuiElements();
        this.remove(heightSlider);
        this.remove(heightField);
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        super.drawGuiText(matrices);

        final AreaRenderCallback renderCallback = (AreaRenderCallback) this.renderCallback;
        renderCallback.setOffset(this.xOffset, this.yOffset);

        var meshStatus = Translate.gui("mesh_status");
        if (renderCallback.canRender()) {
            meshStatus.append(Translate.gui("mesh_ready").formatted(Formatting.GREEN));
        } else {
            var percentage = Math.round(renderCallback.getMeshProgress() * 100);
            meshStatus.append(Translate.gui("mesh_building", percentage).formatted(Formatting.RED));
        }

        MinecraftClient.getInstance().textRenderer.draw(matrices, meshStatus, 12, 260, 0xAAAAAA);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isInViewport(mouseX)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                this.xOffset += deltaX * (450d / renderScale);
                this.yOffset += deltaY * (450d / renderScale);
                return true;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                rotation = (int) ((rotation + deltaX * 2) % 360);
                if (rotation < 0) rotation += 360;

                rotSlider.setValue(rotation / 360d);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                angle = (int) MathHelper.clamp(angle + deltaY * 2, -90, 90);

                angleSlider.setValue(.5 + angle / 180d);
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isInViewport(mouseX)) {
            renderScale = (int) MathHelper.clamp(renderScale + amount * Math.max(1, renderScale * 0.075), 1, 450);
            scaleSlider.setValue((renderScale - 1d) / 449d);

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isInViewport(mouseX) && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && Screen.hasControlDown()) {
            this.xOffset = 0;
            this.yOffset = 0;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static class AreaRenderCallback implements IsometricRenderHelper.RenderCallback {

        private final Window window = MinecraftClient.getInstance().getWindow();
        private final WorldMesh mesh;

        private final int xSize;
        private final int ySize;
        private final int zSize;

        private boolean scaleFramebuffer = true;

        private double xOffset = 0;
        private double yOffset = 0;

        public AreaRenderCallback(BlockPos origin, BlockPos end) {
            final WorldMesh.Builder builder = new WorldMesh.Builder(MinecraftClient.getInstance().world, origin, end);
            mesh = builder.build();
            xSize = 1 + Math.max(origin.getX(), end.getX()) - Math.min(origin.getX(), end.getX());
            ySize = 1 + Math.max(origin.getY(), end.getY()) - Math.min(origin.getY(), end.getY());
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

        private void setOffset(double xOffset, double yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, float tickDelta) {
            if (!mesh.canRender()) {
                mesh.scheduleRebuild();
            } else {
                if (mesh.canRender()) {
                    final var client = MinecraftClient.getInstance();

                    float windowScale = scaleFramebuffer ? window.getFramebufferHeight() / (float) window.getFramebufferWidth() : 1;
                    if (!scaleFramebuffer) scaleFramebuffer = true;

                    RenderSystem.backupProjectionMatrix();
                    Matrix4f projectionMatrix = Matrix4f.projectionMatrix(-1 / windowScale, 1 / windowScale, 1, -1, -1000, 3000);
                    RenderSystem.setProjectionMatrix(projectionMatrix);

                    final MatrixStack stack = new MatrixStack();

                    float scaledRenderScale = renderScale * (window.getScaledHeight() / 515f) * 0.001f;
                    stack.scale(scaledRenderScale, -scaledRenderScale, -scaledRenderScale);

                    stack.translate(xOffset / 53.5, yOffset / 53.5, 0);

                    stack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(angle));
                    stack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-rotation));
                    stack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180));

                    stack.translate(-xSize / 2f, renderHeight * -0.1 - ySize / 2f, -zSize / 2f);
                    mesh.render(stack);

                    stack.push();
                    final var blockEntities = mesh.getRenderInfo().getBlockEntities();
                    blockEntities.forEach((blockPos, entity) -> {
                        stack.loadIdentity();
                        stack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                        client.getBlockEntityRenderDispatcher().render(entity, 0, stack, vertexConsumerProvider);
                    });

                    final var modelViewStack = RenderSystem.getModelViewStack();

                    stack.pop();

                    modelViewStack.push();
                    modelViewStack.peek().getPositionMatrix().load(stack.peek().getPositionMatrix());
                    RenderSystem.applyModelViewMatrix();
                    client.getBufferBuilders().getEntityVertexConsumers().draw();
                    modelViewStack.pop();

                    final var entities = mesh.getRenderInfo().getEntities();
                    entities.forEach((vec3d, entry) -> {
                        stack.push();
                        stack.loadIdentity();
                        client.getEntityRenderDispatcher().render(entry.entity(), vec3d.x, vec3d.y, vec3d.z, 0, 0, stack, vertexConsumerProvider, entry.light());
                        stack.pop();

                        modelViewStack.push();
                        modelViewStack.peek().getPositionMatrix().load(stack.peek().getPositionMatrix());
                        RenderSystem.applyModelViewMatrix();
                        client.getBufferBuilders().getEntityVertexConsumers().draw();
                        modelViewStack.pop();
                    });

                    RenderSystem.restoreProjectionMatrix();
                }
            }
        }
    }
}
