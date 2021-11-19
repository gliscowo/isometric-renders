package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExporter;
import com.glisco.isometricrenders.mixin.MinecraftClientAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.glisco.isometricrenders.client.RuntimeConfig.*;

public class ItemAtlasRenderScreen extends RenderScreen {

    private SliderWidgetImpl scaleSlider;
    private SliderWidgetImpl shiftSlider;
    private SliderWidgetImpl heightSlider;

    @Override
    protected void buildGuiElements() {
        final int sliderWidth = viewportBeginX - 55;
        TextFieldWidget scaleField = new TextFieldWidget(client.textRenderer, 10, 40, 35, 20, Text.of(String.valueOf(atlasScale)));
        scaleField.setTextPredicate(s -> s.matches("^[-+]?([0-9]+(\\.[0-9]+)?|\\.[0-9]+)$") || s.isEmpty());
        scaleField.setText(String.format(Locale.ENGLISH, "%.1f", atlasScale));
        scaleField.setChangedListener(s -> {
            float tempScale = s.length() > 0 ? Float.parseFloat(s) : atlasScale;
            if (tempScale == atlasScale) return;
            scaleSlider.setValue(tempScale / 10);
        });
        scaleSlider = new SliderWidgetImpl(50, 40, sliderWidth, Text.of("Scale"), 0.25, 0.025, atlasScale / 10, aDouble -> {
            atlasScale = (float) (aDouble * 10);
            scaleField.setText(String.format(Locale.ENGLISH, "%.1f", atlasScale));
        });

        TextFieldWidget heightField = new TextFieldWidget(client.textRenderer, 10, 70, 35, 20, Text.of(String.valueOf(atlasHeight)));
        heightField.setTextPredicate(s -> s.matches("-?[0-9]{0,4}"));
        heightField.setText(String.valueOf(115 - atlasHeight));
        heightField.setChangedListener(s -> {
            int tempHeight = s.length() > 0 && !s.equals("-") ? 115 - Integer.parseInt(s) : atlasHeight;
            if (tempHeight == atlasHeight) return;
            heightSlider.setValue(1 - ((tempHeight + 335) / 900d));
        });
        heightSlider = new SliderWidgetImpl(50, 70, sliderWidth, Text.of("Render Height"), 0.5, 0.05, 1 - ((atlasHeight + 335) / 900d), aDouble -> {
            atlasHeight = 565 - (int) Math.round(aDouble * 900);
            heightField.setText(String.valueOf(115 - atlasHeight));
        });

        TextFieldWidget shiftField = new TextFieldWidget(client.textRenderer, 10, 100, 35, 20, Text.of(String.valueOf(atlasShift)));
        shiftField.setTextPredicate(s -> s.matches("-?[0-9]{0,4}"));
        shiftField.setText(String.valueOf(115 - atlasShift));
        shiftField.setChangedListener(s -> {
            int tempShift = s.length() > 0 && !s.equals("-") ? 115 - Integer.parseInt(s) : atlasShift;
            if (tempShift == atlasShift) return;
            shiftSlider.setValue(1 - ((tempShift + 335) / 900d));
        });
        shiftSlider = new SliderWidgetImpl(50, 100, sliderWidth, Text.of("Render Shift"), 0.5, 0.05, 1 - ((atlasShift + 335) / 900d), aDouble -> {
            atlasShift = 565 - (int) Math.round(aDouble * 900);
            shiftField.setText(String.valueOf(115 - atlasShift));
        });

        TextFieldWidget columnsField = new TextFieldWidget(client.textRenderer, 10, 130, 35, 20, Text.of(String.valueOf(atlasColumns)));
        columnsField.setTextPredicate(s -> s.matches("^([1-9][0-9]{0,2})?$"));
        columnsField.setText(String.valueOf(atlasColumns));
        columnsField.setChangedListener(s -> {
            atlasColumns = s.length() > 0 ? Integer.parseInt(s) : atlasColumns;
        });

        addDrawableChild(scaleField);
        addDrawableChild(scaleSlider);

        addDrawableChild(heightField);
        addDrawableChild(heightSlider);

        addDrawableChild(shiftField);
        addDrawableChild(shiftSlider);

        addDrawableChild(columnsField);
    }

    @Override
    protected void drawGuiText(MatrixStack matrices) {
        client.textRenderer.draw(matrices, "Atlas Options", 12, 20, 0xAAAAAA);
        client.textRenderer.draw(matrices, "Columns", 52, 136, 0xFFFFFF);
    }

    @Override
    protected void drawContent(MatrixStack matrices) {
        float scale = atlasScale * 90 * height / 515f;

        Matrix4f modelMatrix = RenderSystem.getModelViewMatrix().copy();

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();
        modelStack.translate((float) Math.round(230 - atlasShift * 1f + (height - 515f) / 30f), (float) Math.round(atlasHeight * 1f + (height - 515f) / 10f), 1500);
        modelStack.scale(1, -1, -1);
        RenderSystem.applyModelViewMatrix();

        matrices.scale(scale, scale, -1);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderCallback.render(matrices, immediate, playAnimations ? ((MinecraftClientAccessor) client).getRenderTickCounter().tickDelta : 0);

        immediate.draw();

        modelStack.loadIdentity();
        modelStack.multiplyPositionMatrix(modelMatrix);

        RenderSystem.applyModelViewMatrix();

        modelStack.pop();
    }

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        return (matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.push();

            matrices.translate(-(115 - atlasShift) / 270d, 0.20 + ((atlasHeight - 115) / 270d), 0);
            matrices.scale(atlasScale * 85 * 0.004f, atlasScale * 85 * 0.004f, 1f);

            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));

            renderCallback.render(matrices, vertexConsumerProvider, tickDelta);

            matrices.pop();

        };
    }

    @Override
    protected CompletableFuture<File> addImageToExportQueue(NativeImage image) {
        return ImageExporter.addJob(image, currentFilename);
    }
}
