package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.mixin.MinecraftClientAccessor;
import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.util.ImageExporter;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.glisco.isometricrenders.util.Translator.gui;
import static com.glisco.isometricrenders.util.RuntimeConfig.*;

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
        scaleSlider = new SliderWidgetImpl(50, 40, sliderWidth, gui("scale"), 0.25, 0.025, atlasScale / 10, aDouble -> {
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
        heightSlider = new SliderWidgetImpl(50, 70, sliderWidth, gui("render_height"), 0.5, 0.05, 1 - ((atlasHeight + 335) / 900d), aDouble -> {
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
        shiftSlider = new SliderWidgetImpl(50, 100, sliderWidth, gui("render_shift"), 0.5, 0.05, 1 - ((atlasShift + 335) / 900d), aDouble -> {
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
        client.textRenderer.draw(matrices, gui("atlas_options"), 12, 20, 0xAAAAAA);
        client.textRenderer.draw(matrices, gui("columns"), 52, 136, 0xFFFFFF);
    }

    @Override
    protected void drawContent(MatrixStack matrices) {
        float scale = atlasScale * 90 * height / 515f;

        MatrixStack modelStack = RenderSystem.getModelViewStack();
        modelStack.push();
        modelStack.translate(230 - atlasShift, atlasHeight, 1500);
        modelStack.scale(1, -1, -1);
        RenderSystem.applyModelViewMatrix();

        matrices.scale(scale, scale, -1);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderCallback.render(matrices, immediate, playAnimations ? client.getTickDelta() : 0);

        immediate.draw();
        modelStack.pop();

        RenderSystem.applyModelViewMatrix();
    }

    @Override
    protected IsometricRenderHelper.RenderCallback getExternalExportCallback() {
        return (matrices, vertexConsumerProvider, tickDelta) -> {

            matrices.push();

            matrices.translate((atlasShift - 225) / 250d, ((atlasHeight) / 250d), 0);
            matrices.scale(atlasScale * 85 * -0.004f, atlasScale * 85 * -0.004f, .15f);

            renderCallback.render(matrices, vertexConsumerProvider, tickDelta);

            matrices.pop();
        };
    }

    @Override
    protected CompletableFuture<File> addImageToExportQueue(NativeImage image) {
        return ImageExporter.addJob(image, currentFilename);
    }
}
