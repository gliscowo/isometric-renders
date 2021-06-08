package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExporter;
import com.glisco.isometricrenders.client.RuntimeConfig;
import com.glisco.isometricrenders.mixin.MinecraftClientAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
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
    private SliderWidgetImpl opacitySlider;

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
            int tempAngle = 30 + (s.length() > 0 && !s.equals("-") ? Integer.parseInt(s) : angle);
            if (tempAngle == angle) return;
            angleSlider.setValue(tempAngle / 60d);
        });
        angleSlider = new SliderWidgetImpl(50, 100, sliderWidth, Text.of("Angle"), 1, 0.25, (30 + angle) / 60d, aDouble -> {
            angle = -30 + (int) Math.round(aDouble * 60);
            angleField.setText(String.valueOf(angle));
        });

        TextFieldWidget heightField = new TextFieldWidget(client.textRenderer, 10, 130, 35, 20, Text.of(String.valueOf(renderHeight)));
        heightField.setTextPredicate(s -> s.matches("-?[0-9]{0,4}"));
        heightField.setText(String.valueOf(130 - renderHeight));
        heightField.setChangedListener(s -> {
            int tempHeight = s.length() > 0 && !s.equals("-") ? 130 - Integer.parseInt(s) : renderHeight;
            if (tempHeight == renderHeight) return;
            heightSlider.setValue(1 - ((tempHeight + 170) / 600d));
        });
        heightSlider = new SliderWidgetImpl(50, 130, sliderWidth, Text.of("Render Height"), 0.5, 0.05, 1 - ((renderHeight + 170) / 600d), aDouble -> {
            renderHeight = 430 - (int) Math.round(aDouble * 600);
            heightField.setText(String.valueOf(130 - renderHeight));
        });

        TextFieldWidget opacityField = new TextFieldWidget(client.textRenderer, 10, 160, 35, 20, Text.of(String.valueOf(exportOpacity)));
        opacityField.setTextPredicate(s -> s.matches("[0-9]{0,3}"));
        opacityField.setText(String.valueOf(exportOpacity));
        opacityField.setChangedListener(s -> {
            int tempOpacity = s.length() > 0 && !s.equals("-") ? Integer.parseInt(s) : exportOpacity;
            if (tempOpacity == exportOpacity) return;
            opacitySlider.setValue(exportOpacity / 100f);
        });
        opacitySlider = new SliderWidgetImpl(50, 160, sliderWidth, Text.of("Export Opacity"), 1, 0.05, exportOpacity / 100f, aDouble -> {
            exportOpacity = (int) Math.round(aDouble * 100);
            opacityField.setText(String.valueOf(exportOpacity));
        });

        addDrawableChild(scaleSlider);
        addDrawableChild(scaleField);

        addDrawableChild(rotationField);
        addDrawableChild(rotSlider);

        addDrawableChild(angleField);
        addDrawableChild(angleSlider);

        addDrawableChild(heightField);
        addDrawableChild(heightSlider);

        addDrawableChild(opacityField);
        addDrawableChild(opacitySlider);
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        client.textRenderer.draw(matrices, "Transform Options", 12, 20, 0xAAAAAA);
    }

    @Override
    protected void drawContent(MatrixStack matrices) {
        float scale = renderScale * height / 515f;

        Matrix4f modelMatrix = RenderSystem.getModelViewMatrix().copy();

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();

        modelStack.translate(115, (float) Math.round(renderHeight * 1f + (height - 515f) / 10f), 1500);
        modelStack.scale(1, -1, -1);

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
        modelStack.method_34425(modelMatrix);

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

            matrices.translate(0, 0.25 + ((renderHeight - 130) / 270d), 0);
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
