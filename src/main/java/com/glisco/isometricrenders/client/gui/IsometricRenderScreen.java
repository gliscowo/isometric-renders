package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExporter;
import com.glisco.isometricrenders.client.RuntimeConfig;
import com.glisco.isometricrenders.mixin.MinecraftClientAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;

import static com.glisco.isometricrenders.client.RuntimeConfig.*;

public class IsometricRenderScreen extends RenderScreen {

    private SliderWidgetImpl scaleSlider;
    private SliderWidgetImpl rotSlider;
    private SliderWidgetImpl angleSlider;
    private SliderWidgetImpl heightSlider;

    @Override
    protected void buildGuiElements() {
        final int sliderWidth = viewportBeginX - 55;

        TextFieldWidget scaleField = new TextFieldWidget(client.textRenderer, 10, 40, 35, 20, Text.of(String.valueOf(renderScale)));
        scaleField.setTextPredicate(s -> s.matches("[0-9]{0,3}+"));
        scaleField.setText(String.valueOf(renderScale));
        scaleField.setChangedListener(s -> {
            int tempScale = s.length() > 0 ? Integer.parseInt(s) : renderScale;
            if (tempScale == renderScale || tempScale < 1) return;
            scaleSlider.setValue((tempScale - 1d) / 449d);
        });
        scaleSlider = new SliderWidgetImpl(50, 40, sliderWidth, Text.of("Scale"), 0.399, 0.025, (renderScale - 1) / 449d, aDouble -> {
            renderScale = (int) Math.round(1d + aDouble * 449d);
            scaleField.setText(String.valueOf(renderScale));
        });

        TextFieldWidget rotationField = new TextFieldWidget(client.textRenderer, 10, 70, 35, 20, Text.of(String.valueOf(rotation)));
        rotationField.setTextPredicate(s -> s.matches("[0-9]{0,3}+"));
        rotationField.setText(String.valueOf(rotation));
        rotationField.setChangedListener(s -> {
            int tempRot = s.length() > 0 ? Integer.parseInt(s) : rotation;
            if (tempRot == rotation) return;
            rotSlider.setValue(tempRot / 360d);
        });
        rotSlider = new SliderWidgetImpl(50, 70, sliderWidth, Text.of("Rotation"), 0.625, 0.125, rotation / 360d, aDouble -> {
            rotation = (int) Math.round(aDouble * 360);
            rotationField.setText(String.valueOf(rotation));
        });
        rotSlider.allowRollover();

        TextFieldWidget angleField = new TextFieldWidget(client.textRenderer, 10, 100, 35, 20, Text.of(String.valueOf(angle)));
        angleField.setTextPredicate(s -> s.matches("-?[0-9]{0,2}+"));
        angleField.setText(String.valueOf(angle));
        angleField.setChangedListener(s -> {
            int tempAngle = 90 + (s.length() > 0 && !s.equals("-") ? Integer.parseInt(s) : angle);
            if (tempAngle == angle) return;
            angleSlider.setValue(tempAngle / 180d);
        });
        angleSlider = new SliderWidgetImpl(50, 100, sliderWidth, Text.of("Angle"), 2 / 3d, 5 / 30d, (90 + angle) / 180d, aDouble -> {
            angle = -90 + (int) Math.round(aDouble * 180);
            angleField.setText(String.valueOf(angle));
        });

        TextFieldWidget heightField = new TextFieldWidget(client.textRenderer, 10, 130, 35, 20, Text.of(String.valueOf(renderHeight)));
        heightField.setTextPredicate(s -> s.matches("-?[0-9]{0,4}"));
        heightField.setText(String.valueOf(renderHeight));
        heightField.setChangedListener(s -> {
            int tempHeight = s.length() > 0 && !s.equals("-") ? Integer.parseInt(s) : renderHeight;
            if (tempHeight == renderHeight) return;
            heightSlider.setValue(((tempHeight + 300) / 600d));
        });
        heightSlider = new SliderWidgetImpl(50, 130, sliderWidth, Text.of("Render Height"), 0.5, 0.05, (renderHeight + 300) / 600d, aDouble -> {
            renderHeight = (int) Math.round(aDouble * 600) - 300;
            heightField.setText(String.valueOf(renderHeight));
        });

        ButtonWidget dimetricButton = new ButtonWidget(10, 180, 60, 20, Text.of("Dimetric"), button -> {
            rotationField.setText("225");
            angleField.setText("30");
            heightField.setText("0");
        });

        ButtonWidget isometricButton = new ButtonWidget(75, 180, 60, 20, Text.of("Isometric"), button -> {
            rotationField.setText("225");
            angleField.setText("36");
            heightField.setText("0");
        });

        ButtonWidget lightingButton = new ButtonWidget(10, 225, 90, 20, lightingProfile.getFriendlyName(), button -> {
            if (lightingProfile instanceof DefaultLightingProfiles.UserLightingProfile) {
                lightingProfile = DefaultLightingProfiles.FLAT;
            } else if (lightingProfile == DefaultLightingProfiles.FLAT) {
                lightingProfile = DefaultLightingProfiles.DEFAULT_DEPTH_LIGHTING;
            } else {
                lightingProfile = DefaultLightingProfiles.FLAT;
            }
            button.setMessage(lightingProfile.getFriendlyName());
        });

        addDrawableChild(scaleSlider);
        addDrawableChild(scaleField);

        addDrawableChild(rotationField);
        addDrawableChild(rotSlider);

        addDrawableChild(angleField);
        addDrawableChild(angleSlider);

        addDrawableChild(heightField);
        addDrawableChild(heightSlider);

        addDrawableChild(dimetricButton);
        addDrawableChild(isometricButton);

        addDrawableChild(lightingButton);
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        client.textRenderer.draw(matrices, "Transform Options", 12, 20, 0xAAAAAA);
        client.textRenderer.draw(matrices, "Presets", 12, 165, 0xAAAAAA);
        client.textRenderer.draw(matrices, "Lighting Profile", 12, 210, 0xAAAAAA);
    }

    @Override
    protected void drawContent(MatrixStack matrices) {
        float scale = renderScale * height / 515f;

        Matrix4f modelMatrix = RenderSystem.getModelViewMatrix().copy();

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();

        modelStack.scale(1, -1, -1);
        modelStack.translate(0, (float) Math.round(renderHeight * (height / 515f)), 0);

        RenderSystem.applyModelViewMatrix();

        matrices.push();
        matrices.scale(scale, scale, -1);

        Quaternion flip = Vec3f.POSITIVE_Z.getDegreesQuaternion(0);
        flip.hamiltonProduct(Vec3f.POSITIVE_X.getDegreesQuaternion(angle));

        Quaternion rotation = Vec3f.POSITIVE_Y.getDegreesQuaternion(RuntimeConfig.rotation);

        matrices.multiply(flip);
        matrices.multiply(rotation);

        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderCallback.render(matrices, immediate, playAnimations ? ((MinecraftClientAccessor) client).getRenderTickCounter().tickDelta : 0);

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
            flip.hamiltonProduct(Vec3f.POSITIVE_X.getDegreesQuaternion(angle));

            matrices.translate(0, (renderHeight / -300d), 0);
            matrices.scale(renderScale * 0.004f, renderScale * 0.004f, 1f);

            Quaternion rotate = Vec3f.POSITIVE_Y.getDegreesQuaternion(rotation);

            matrices.multiply(flip);
            matrices.multiply(rotate);

            renderCallback.render(matrices, vertexConsumerProvider, tickDelta);

            MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderShadows(true);

            matrices.pop();

        };
    }

    @Override
    protected void addImageToExportQueue(NativeImage image) {
        ImageExporter.addJob(image, currentFilename);
    }
}
