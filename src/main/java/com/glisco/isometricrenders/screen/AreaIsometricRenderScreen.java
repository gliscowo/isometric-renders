package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.worldmesher.WorldMesh;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import static com.glisco.isometricrenders.util.Translator.gui;
import static com.glisco.isometricrenders.util.Translator.tr;
import static com.glisco.isometricrenders.util.RuntimeConfig.*;

public class AreaIsometricRenderScreen extends IsometricRenderScreen {

    private RenderScreen.SliderWidgetImpl opacitySlider;
    private final boolean translucencyEnabled;

    public AreaIsometricRenderScreen(boolean enableTranslucency) {
        this.translucencyEnabled = enableTranslucency;
    }

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        if (this.renderCallback instanceof AreaRenderCallback areaCallback) areaCallback.doSquareFramebufferOnce();
        return super.getExternalExportCallback();
    }

    @Override
    protected void buildGuiElements() {
        super.buildGuiElements();

        if (!translucencyEnabled) return;

        final int sliderWidth = viewportBeginX - 55;
        TextFieldWidget opacityField = new TextFieldWidget(client.textRenderer, 10, 275, 35, 20, Text.of(String.valueOf(areaRenderOpacity)));
        opacityField.setTextPredicate(s -> s.matches("[0-9]{0,3}"));
        opacityField.setText(String.valueOf(areaRenderOpacity));
        opacityField.setChangedListener(s -> {
            int tempOpacity = s.length() > 0 && !s.equals("-") ? Integer.parseInt(s) : areaRenderOpacity;
            if (tempOpacity == areaRenderOpacity) return;
            opacitySlider.setValue(areaRenderOpacity / 100f);
        });
        opacitySlider = new RenderScreen.SliderWidgetImpl(50, 275, sliderWidth, tr("message.isometric-renders.opacity"), 1, 0.05, areaRenderOpacity / 100f, aDouble -> {
            areaRenderOpacity = (int) Math.round(aDouble * 100);
            opacityField.setText(String.valueOf(areaRenderOpacity));
        });

        addDrawableChild(opacityField);
        addDrawableChild(opacitySlider);
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        super.drawGuiText(matrices);

        final AreaRenderCallback renderCallback = (AreaRenderCallback) this.renderCallback;
        var meshStatus = gui("mesh_status");
        if (renderCallback.canRender()) {
            meshStatus.append(gui("mesh_ready"));
        } else {
            var percentage = Math.round(renderCallback.getMeshProgress() * 100);
            meshStatus.append(gui("mesh_building", percentage));
        }

        MinecraftClient.getInstance().textRenderer.draw(matrices, meshStatus, 12, 260, 0xAAAAAA);
    }

    public static class AreaRenderCallback implements IsometricRenderHelper.RenderCallback {

        private final Window window = MinecraftClient.getInstance().getWindow();
        private final WorldMesh mesh;

        private final int xSize;
        private final int ySize;
        private final int zSize;

        private boolean scaleFramebuffer = true;

        public AreaRenderCallback(BlockPos origin, BlockPos end, boolean translucencyEnabled) {
            final WorldMesh.Builder builder = new WorldMesh.Builder(MinecraftClient.getInstance().world, origin, end);
//            builder.enableBlockEntities();
            if (translucencyEnabled) {
                builder.renderActions(() -> {
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1, 1, 1, areaRenderOpacity / 100f);
                }, () -> {
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    RenderSystem.disableBlend();
                });
                builder.disableCulling();
            }
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

                    stack.translate(-xSize / 2f, renderHeight * -0.1 - ySize / 2f, -zSize / 2f);

                    mesh.render(stack);

                    RenderSystem.restoreProjectionMatrix();
                }
            }
        }
    }
}
