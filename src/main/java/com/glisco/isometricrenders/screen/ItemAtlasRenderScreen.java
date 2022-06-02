package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.util.ImageExporter;
import com.glisco.isometricrenders.widget.SettingTextField;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static com.glisco.isometricrenders.setting.Settings.*;
import static com.glisco.isometricrenders.util.Translate.gui;

public class ItemAtlasRenderScreen extends RenderScreen {

    @Override
    protected void buildGuiElements() {
        this.addSetting(atlasScale, "scale", 1);
        this.addSetting(atlasHeight, "render_height", 45);
        this.addSetting(atlasShift, "render_shift", 45);

        addDrawableChild(new SettingTextField(10, 130, atlasColumns));
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        client.textRenderer.draw(matrices, gui("atlas_options"), 12, 20, 0xAAAAAA);
        client.textRenderer.draw(matrices, gui("columns"), 52, 136, 0xFFFFFF);
    }

    @Override
    protected void drawContent(MatrixStack matrices) {
        float scale = atlasScale.get() * 4 * height / 515f;

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();
        modelStack.translate(atlasShift.get(), -atlasHeight.get(), 1500);
        modelStack.scale(1, -1, -1);
        RenderSystem.applyModelViewMatrix();

        matrices.scale(scale, scale, -1);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderCallback.render(matrices, immediate, playAnimations.get() ? client.getTickDelta() : 0);

        immediate.draw();
        modelStack.pop();

        RenderSystem.applyModelViewMatrix();
    }

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        return (matrices, vertexConsumerProvider, tickDelta) -> {
            matrices.push();

            matrices.translate((-atlasShift.get() + 25) / 250d, ((atlasHeight.get()) / -250d), 0);
            matrices.scale(atlasScale.get() * 4 * -0.004f, atlasScale.get() * 4 * -0.004f, .15f);

            renderCallback.render(matrices, vertexConsumerProvider, tickDelta);

            matrices.pop();
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isInViewport(mouseX)) {
            atlasScale.modify((int) (amount * Math.max(1, atlasScale.get() * 0.075)));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isInViewport(mouseX) && button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            atlasShift.modify((int) (deltaX));
            atlasHeight.modify((int) (-deltaY));
//            this.xOffset += deltaX * (450d / renderScale.get());
//            this.yOffset += deltaY * (450d / renderScale.get());
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected CompletableFuture<File> addImageToExportQueue(NativeImage image) {
        return ImageExporter.addJob(image, currentFilename);
    }
}
