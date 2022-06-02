package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.mixin.access.MinecraftClientAccessor;
import com.glisco.isometricrenders.render.DefaultLightingProfiles;
import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.setting.Settings;
import com.glisco.isometricrenders.util.ImageExporter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

import java.io.File;
import java.util.concurrent.CompletableFuture;

import static com.glisco.isometricrenders.setting.Settings.*;
import static com.glisco.isometricrenders.util.Translate.gui;

public class IsometricRenderScreen extends RenderScreen {

    @Override
    protected void buildGuiElements() {
        this.addSetting(renderScale, "scale", 10);
        this.addSetting(rotation, "rotation", 45);
        this.addSetting(angle, "angle", 30);
        this.addSetting(renderHeight, "render_height", 30);

        addDrawableChild(new ButtonWidget(10, 180, 60, 20, gui("dimetric"), button -> {
            rotation.set(225);
            angle.set(30);
            renderHeight.set(0);
        }));

        addDrawableChild(new ButtonWidget(75, 180, 60, 20, gui("isometric"), button -> {
            rotation.set(225);
            angle.set(36);
            renderHeight.set(0);
        }));

        addDrawableChild(new ButtonWidget(10, 225, 90, 20, lightingProfile.getFriendlyName(), button -> {
            if (lightingProfile instanceof DefaultLightingProfiles.UserLightingProfile) {
                lightingProfile = DefaultLightingProfiles.FLAT;
            } else if (lightingProfile == DefaultLightingProfiles.FLAT) {
                lightingProfile = DefaultLightingProfiles.DEFAULT_DEPTH_LIGHTING;
            } else {
                lightingProfile = DefaultLightingProfiles.FLAT;
            }
            button.setMessage(lightingProfile.getFriendlyName());
        }));
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        client.textRenderer.draw(matrices, gui("transform_options"), 12, 20, 0xAAAAAA);
        client.textRenderer.draw(matrices, gui("presets"), 12, 165, 0xAAAAAA);
        client.textRenderer.draw(matrices, gui("lighting_profile"), 12, 210, 0xAAAAAA);
    }

    @Override
    protected void drawContent(MatrixStack matrices) {
        float scale = renderScale.get() * height / 515f;

        Matrix4f modelMatrix = RenderSystem.getModelViewMatrix().copy();

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();

        modelStack.scale(1, -1, -1);
        modelStack.translate(0, (float) Math.round(renderHeight.get() * (height / 515f)), 0);

        RenderSystem.applyModelViewMatrix();

        matrices.push();
        matrices.scale(scale, scale, -1);

        Quaternion flip = Vec3f.POSITIVE_Z.getDegreesQuaternion(0);
        flip.hamiltonProduct(Vec3f.POSITIVE_X.getDegreesQuaternion(angle.get()));

        Quaternion rotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(Settings.rotation.get());

        matrices.multiply(flip);
        matrices.multiply(rotation);

        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderCallback.render(matrices, immediate, playAnimations.get() ? ((MinecraftClientAccessor) client).getRenderTickCounter().tickDelta : 0);

        matrices.pop();
        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);

        modelStack.loadIdentity();
        modelStack.multiplyPositionMatrix(modelMatrix);

        RenderSystem.applyModelViewMatrix();

        modelStack.pop();
    }

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        return (matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.push();

            MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderShadows(false);

            Quaternion flip = Vec3f.POSITIVE_Z.getDegreesQuaternion(180);
            flip.hamiltonProduct(Vec3f.POSITIVE_X.getDegreesQuaternion(angle.get()));

            matrices.translate(0, (renderHeight.get() / -300d), 0);
            matrices.scale(renderScale.get() * 0.004f, renderScale.get() * 0.004f, 1f);

            Quaternion rotate = Vec3f.POSITIVE_Y.getDegreesQuaternion(rotation.get());

            matrices.multiply(flip);
            matrices.multiply(rotate);

            renderCallback.render(matrices, vertexConsumerProvider, tickDelta);

            MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderShadows(true);

            matrices.pop();

        };
    }

    @Override
    protected CompletableFuture<File> addImageToExportQueue(NativeImage image) {
        return ImageExporter.addJob(image, currentFilename);
    }
}
