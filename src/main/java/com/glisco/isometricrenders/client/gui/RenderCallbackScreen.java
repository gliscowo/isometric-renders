package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExporter;
import com.glisco.isometricrenders.client.RuntimeConfig;
import com.glisco.isometricrenders.mixin.ParticleManagerAccessor;
import com.glisco.isometricrenders.mixin.SliderWidgetInvoker;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

import static com.glisco.isometricrenders.client.RuntimeConfig.*;

public abstract class RenderCallbackScreen extends Screen {

    private ButtonWidget exportButton;

    protected IsometricRenderHelper.RenderCallback renderCallback = (matrices, vertexConsumerProvider, tickDelta) -> {};
    protected Runnable tickCallback = () -> {};
    protected Runnable closedCallback = () -> {};

    public boolean playAnimations = false;
    protected boolean studioMode = false;
    protected boolean captureScheduled = false;
    protected boolean drawBackground = false;
    protected boolean tickParticles = true;

    protected int viewportBeginX;
    protected int viewportEndX;

    public RenderCallbackScreen() {
        super(Text.of(""));
    }

    @Override
    protected void init() {
        super.init();
        viewportBeginX = (int) ((this.width - this.height) * 0.5);
        viewportEndX = (int) (this.width - (this.width - this.height) * 0.5) + 1;

        ((ParticleManagerAccessor)client.particleManager).getParticles().clear();
        IsometricRenderHelper.allowParticles = false;

        buttons.clear();
        buildGuiElements();

        TextFieldWidget colorField = new TextFieldWidget(client.textRenderer, viewportEndX + 10, 38, 50, 20, Text.of("#00ff00"));
        colorField.setTextPredicate(s -> s.matches("^#([A-Fa-f0-9]{0,6})$"));
        colorField.setText("#" + String.format("%02X", backgroundColor >> 16) + String.format("%02X", backgroundColor >> 8 & 0xFF) + String.format("%02X", backgroundColor & 0xFF));
        colorField.setChangedListener(s -> {
            if (s.substring(1).length() < 6) return;
            backgroundColor = Integer.parseInt(s.substring(1), 16);
        });

        CheckboxWidget playAnimationsCheckbox = new CallbackCheckboxWidget(viewportEndX + 10, 68, Text.of("Animations"), playAnimations, aBoolean -> {
            playAnimations = aBoolean;
        });
        CheckboxWidget playParticlesCheckbox = new CallbackCheckboxWidget(viewportEndX + 10, 93, Text.of("Particles (requires animations)"), tickParticles, aBoolean -> {
            tickParticles = aBoolean;
        });

        CheckboxWidget doHiResCheckbox = new CallbackCheckboxWidget(viewportEndX + 10, 183, Text.of("Use Variable-Resolution Renderer"), useExternalRenderer, aBoolean -> {
            useExternalRenderer = aBoolean;
        });
        CheckboxWidget allowMultipleRendersCheckbox = new CallbackCheckboxWidget(viewportEndX + 10, 208, Text.of("Allow Multiple Export Jobs"), allowMultipleNonThreadedJobs, aBoolean -> {
            allowMultipleNonThreadedJobs = aBoolean;
        });
        ButtonWidget clearQueueButton = new ButtonWidget(viewportEndX + 90, 233, 75, 20, Text.of("Clear Queue"), button -> {
            ImageExporter.clearQueue();
        });

        exportButton = new ButtonWidget(viewportEndX + 10, 233, 65, 20, Text.of("Export"), button -> {
            if ((ImageExporter.getJobCount() < 1 || allowMultipleNonThreadedJobs)) {
                captureScheduled = true;
            }
        });
        TextFieldWidget resolutionField = new TextFieldWidget(client.textRenderer, viewportEndX + 10, 153, 50, 20, Text.of("2048"));
        resolutionField.setEditableColor(0x00FF00);
        resolutionField.setText(String.valueOf(exportResolution));
        resolutionField.setTextPredicate(s -> s.matches("[0-9]{0,5}"));
        resolutionField.setChangedListener(s -> {
            if (s.length() < 1) return;
            int resolution = Integer.parseInt(s);
            if ((resolution != 0) && ((resolution & (resolution - 1)) != 0) || resolution < 16 || (resolution > 16384 && !allowInsaneResolutions)) {
                resolutionField.setEditableColor(0xFF0000);
                exportButton.active = false;
            } else {
                resolutionField.setEditableColor(0x00FF00);
                exportResolution = resolution;
                exportButton.active = true;
            }
        });

        addButton(colorField);

        addButton(playAnimationsCheckbox);
        addButton(doHiResCheckbox);
        addButton(allowMultipleRendersCheckbox);
        addButton(playParticlesCheckbox);

        addButton(resolutionField);
        addButton(exportButton);
        addButton(clearQueueButton);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (((captureScheduled && !RuntimeConfig.useExternalRenderer) || studioMode) && !drawBackground) {
            fill(matrices, 0, 0, this.width, this.height, RuntimeConfig.backgroundColor | 255 << 24);
        } else {

            if (drawBackground) {
                fill(matrices, 0, 0, this.width, this.height, RuntimeConfig.backgroundColor | 255 << 24);
            } else {
                renderBackground(matrices);
                drawFramingHint(matrices);
            }

            drawGuiBackground(matrices);
            super.render(matrices, mouseX, mouseY, delta);

            drawGuiText(matrices);

            client.textRenderer.draw(matrices, "Render Options", viewportEndX + 12, 20, 0xAAAAAA);
            client.textRenderer.draw(matrices, "Background Color", viewportEndX + 66, 43, 0xFFFFFF);
            fill(matrices, viewportEndX + 160, 43, viewportEndX + 168, 51, RuntimeConfig.backgroundColor | 255 << 24);

            client.textRenderer.draw(matrices, "Export Options", viewportEndX + 12, 135, 0xAAAAAA);
            client.textRenderer.draw(matrices, "Renderer Resolution", viewportEndX + 66, 160, 0xFFFFFF);

            client.textRenderer.draw(matrices, "F10: Studio Mode  F12: Export", viewportEndX + 12, height - 20, 0xAAAAAA);

            client.textRenderer.draw(matrices, "Warning: Unless you have at least 6GB ", 10, height - 60, 0xAAAAAA);
            client.textRenderer.draw(matrices, "of memory available for Minecraft, ", 10, height - 50, 0xAAAAAA);
            client.textRenderer.draw(matrices, "resolutions over 8192x8192", 10, height - 40, 0xAAAAAA);
            client.textRenderer.draw(matrices, "are not recommended if you want", 10, height - 30, 0xAAAAAA);
            client.textRenderer.draw(matrices, "your computer to survive", 10, height - 20, 0xAAAAAA);

            client.textRenderer.draw(matrices, ImageExporter.getProgressBarText(), viewportEndX + 12, 350, 0xFFFFFF);

            if (ImageExporter.getJobCount() > 0) {
                drawExportProgressBar(matrices, viewportEndX + 12, 362, width - viewportEndX - 30, 150, 5);
            }
        }

        IsometricRenderHelper.setupLighting();

        int i = (this.width - 248) / 2 + 10;
        int j = (this.height - 166) / 2 + 8;
        GlStateManager.pushMatrix();
        GlStateManager.translatef(0, 0, 10F);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translatef(i, j, 10F);

        drawContent(matrices);

        GlStateManager.popMatrix();

        if (captureScheduled) {
            capture();
            captureScheduled = false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        closedCallback.run();
        IsometricRenderHelper.allowParticles = true;
    }

    @Override
    public void tick() {
        if (tickParticles) IsometricRenderHelper.allowParticles = true;
        if (playAnimations) tickCallback.run();
        IsometricRenderHelper.allowParticles = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F12) {
            if ((ImageExporter.getJobCount() < 1 || allowMultipleNonThreadedJobs) && exportButton.active) {
                captureScheduled = true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_F10) {
            studioMode = !studioMode;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void capture() {
        if (useExternalRenderer) {
            addImageToExportQueue(IsometricRenderHelper.renderIntoImage(exportResolution, getExternalExportCallback()));
        } else {
            addImageToExportQueue(IsometricRenderHelper.takeKeyedSnapshot(MinecraftClient.getInstance().getFramebuffer(), backgroundColor, true));
        }
    }

    protected void drawFramingHint(MatrixStack matrices) {
        fill(matrices, viewportBeginX + 5, 0, viewportEndX - 5, 5, 0x90000000);
        fill(matrices, viewportBeginX + 5, height - 5, viewportEndX - 5, height, 0x90000000);
        fill(matrices, viewportBeginX, 0, viewportBeginX + 5, height, 0x90000000);
        fill(matrices, viewportEndX - 5, 0, viewportEndX, height, 0x90000000);
    }

    protected void drawGuiBackground(MatrixStack matrices) {
        fill(matrices, 0, 0, viewportBeginX, height, 0x90000000);
        fill(matrices, viewportEndX, 0, width, height, 0x90000000);
    }

    public static void drawExportProgressBar(MatrixStack matrices, int x, int y, int drawWidth, int barWidth, double speed) {
        final int color = ImageExporter.currentlyExporting() ? 0xFF00FF00 : 0xFFFF8800;
        int end = x + drawWidth + barWidth;

        int offset = (int) (System.currentTimeMillis() / speed % (drawWidth + barWidth));

        int endWithOffset = x + offset;
        if (endWithOffset > end) endWithOffset = end;

        fill(matrices, Math.max(x + offset - barWidth, x), y, Math.min(endWithOffset, x + drawWidth), y + 2, color);
    }

    public void setRenderCallback(IsometricRenderHelper.RenderCallback renderCallback) {
        this.renderCallback = renderCallback;
    }

    public void setTickCallback(Runnable tickCallback) {
        this.tickCallback = tickCallback;
    }

    public void setClosedCallback(Runnable closedCallback) {
        this.closedCallback = closedCallback;
    }

    protected abstract void buildGuiElements();

    protected abstract void drawGuiText(MatrixStack matrices);

    protected abstract void drawContent(MatrixStack matrices);

    protected abstract IsometricRenderHelper.RenderCallback getExternalExportCallback();

    protected abstract void addImageToExportQueue(NativeImage image);


    protected static class SliderWidgetImpl extends SliderWidget {

        private final Consumer<Double> changeListener;
        private final double defaultValue;
        private final double scrollIncrement;
        private boolean allowRollover = false;

        public SliderWidgetImpl(int x, int y, int width, Text text, double defaultValue, double scrollIncrement, double initialValue, Consumer<Double> changeListener) {
            super(x, y, width, 20, text, initialValue);
            this.changeListener = changeListener;
            this.defaultValue = defaultValue;
            this.scrollIncrement = scrollIncrement;
        }

        @Override
        protected void updateMessage() {

        }

        public void allowRollover() {
            allowRollover = true;
        }

        @Override
        protected void applyValue() {
            changeListener.accept(value);
        }

        public void setValue(double value) {
            ((SliderWidgetInvoker) this).invokeSetValue(value);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 2 && this.clicked(mouseX, mouseY)) {
                setValue(defaultValue);
                return true;
            } else {
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {

            if (!isHovered()) return false;

            double newValue = value + amount * scrollIncrement;

            if (!allowRollover && (newValue < 0 || newValue > 1)) return false;

            if (newValue < 0) newValue += 1;
            if (newValue > 1) newValue -= 1;

            setValue(newValue);

            return true;
        }
    }

    protected static class CallbackCheckboxWidget extends CheckboxWidget {

        private final Consumer<Boolean> changeCallback;

        public CallbackCheckboxWidget(int x, int y, Text message, boolean checked, Consumer<Boolean> changeCallback) {
            super(x, y, 20, 20, message, checked);
            this.changeCallback = changeCallback;
        }

        @Override
        public void onPress() {
            super.onPress();
            changeCallback.accept(isChecked());
        }
    }
}
