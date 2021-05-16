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
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.text.Text;
import net.minecraft.util.math.Quaternion;

import static com.glisco.isometricrenders.client.RuntimeConfig.*;

public class IsometricRenderScreen extends RenderCallbackScreen {

    private SliderWidgetImpl scaleSlider;
    private SliderWidgetImpl rotSlider;
    private SliderWidgetImpl angleSlider;
    private SliderWidgetImpl heightSlider;

    @Override
    protected void buildGuiElements() {
        TextFieldWidget scaleField = new TextFieldWidget(client.textRenderer, 10, 40, 35, 20, Text.of(String.valueOf(renderScale)));
        scaleField.setTextPredicate(s -> s.matches("[0-9]{0,3}+"));
        scaleField.setText(String.valueOf(renderScale));
        scaleField.setChangedListener(s -> {
            int tempScale = s.length() > 0 ? Integer.parseInt(s) : renderScale;
            if (tempScale == renderScale || tempScale < 25) return;
            scaleSlider.setValue((tempScale - 25d) / 400d);
        });
        scaleSlider = new SliderWidgetImpl(50, 40, 170, Text.of("Scale"), 0.3125, 0.025, (renderScale - 25) / 400d, aDouble -> {
            renderScale = (int) Math.round(25d + aDouble * 400d);
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
        rotSlider = new SliderWidgetImpl(50, 70, 170, Text.of("Rotation"), 0.875, 0.125, rotation / 360d, aDouble -> {
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
        angleSlider = new SliderWidgetImpl(50, 100, 170, Text.of("Angle"), 1, 0.25, (30 + angle) / 60d, aDouble -> {
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
        heightSlider = new SliderWidgetImpl(50, 130, 170, Text.of("Render Height"), 0.5, 0.05, 1 - ((renderHeight + 170) / 600d), aDouble -> {
            renderHeight = 430 - (int) Math.round(aDouble * 600);
            heightField.setText(String.valueOf(130 - renderHeight));
        });

        buttons.clear();

        addButton(scaleSlider);
        addButton(scaleField);

        addButton(rotationField);
        addButton(rotSlider);

        addButton(angleField);
        addButton(angleSlider);

        addButton(heightField);
        addButton(heightSlider);
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        client.textRenderer.draw(matrices, "Transform Options", 12, 20, 0xAAAAAA);
    }

    @Override
    protected void drawContent(MatrixStack matrices) {
        float scale = renderScale * height / 515f;

        RenderSystem.pushMatrix();
        RenderSystem.translatef(115, (float) Math.round(renderHeight * 1f + (height - 515f) / 10f), 1500);
        RenderSystem.scalef(1, 1, -1);

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(0, 0, 1000);

        matrixStack.scale(scale, scale, 1);

        Quaternion flip = Vector3f.POSITIVE_Z.getDegreesQuaternion(180);
        flip.hamiltonProduct(Vector3f.POSITIVE_X.getDegreesQuaternion(-angle));

        Quaternion rotation = Vector3f.POSITIVE_Y.getDegreesQuaternion(RuntimeConfig.rotation);

        matrixStack.multiply(flip);
        matrixStack.multiply(rotation);

        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderCallback.render(matrixStack, immediate, playAnimations ? ((MinecraftClientAccessor) client).getRenderTickCounter().tickDelta : 0);

        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        RenderSystem.popMatrix();
    }

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        return (matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.push();

            MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderShadows(false);

            Quaternion flip = Vector3f.POSITIVE_Z.getDegreesQuaternion(180);
            flip.hamiltonProduct(Vector3f.POSITIVE_X.getDegreesQuaternion(-angle));

            matrices.translate(0, 0.25 + ((renderHeight - 130) / 270d), 0);
            matrices.scale(renderScale * 0.004f, renderScale * 0.004f, -1f);

            Quaternion rotate = Vector3f.POSITIVE_Y.getDegreesQuaternion(rotation);

            matrices.multiply(flip);
            matrices.multiply(rotate);

            renderCallback.render(matrices, vertexConsumerProvider, tickDelta);

            MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderShadows(true);

            matrices.pop();

        };
    }

    @Override
    protected void addImageToExportQueue(NativeImage image) {
        //TODO migrate to export dispatcher
        ImageExporter.addJob(image);
    }
}
