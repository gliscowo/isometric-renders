package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExporter;
import com.glisco.isometricrenders.mixin.MinecraftClientAccessor;
import com.glisco.isometricrenders.mixin.ParticleManagerAccessor;
import com.glisco.isometricrenders.mixin.SliderWidgetInvoker;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.Quaternion;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class IsometricRenderScreen extends Screen {

    private SliderWidgetImpl scaleSlider;
    private SliderWidgetImpl rotSlider;
    private SliderWidgetImpl angleSlider;
    private SliderWidgetImpl heightSlider;

    protected static int rotation = 315;
    protected static int angle = 30;

    protected static int renderScale = 150;
    protected static int backgroundColor = 0x00ff00;
    protected static int renderHeight = 130;
    protected static boolean hiResRender = false;
    protected static boolean allowMultiple = false;
    protected static int exportResolution = 2048;
    protected static boolean studioMode = false;

    public boolean tickRender = false;

    protected boolean tickParticles = true;
    protected boolean captureScheduled = false;
    protected boolean drawBackground = false;

    protected IsometricRenderHelper.RenderCallback renderCallback = (matrices, vertexConsumerProvider, tickDelta) -> {};
    protected Runnable tickCallback = () -> {};
    protected Runnable closedCallback = () -> {};

    public IsometricRenderScreen() {
        super(Text.of(""));
    }

    @Override
    protected void init() {
        super.init();

        ((ParticleManagerAccessor) MinecraftClient.getInstance().particleManager).getParticles().clear();
        IsometricRenderHelper.allowParticles = false;

        TextFieldWidget colorField = new TextFieldWidget(client.textRenderer, 10, 190, 50, 20, Text.of("#00ff00"));
        colorField.setTextPredicate(s -> s.matches("^#([A-Fa-f0-9]{0,6})$"));
        colorField.setText("#" + String.format("%02X", backgroundColor >> 16) + String.format("%02X", backgroundColor >> 8 & 0xFF) + String.format("%02X", backgroundColor & 0xFF));
        colorField.setChangedListener(s -> {
            if (s.substring(1).length() < 6) return;
            backgroundColor = Integer.parseInt(s.substring(1), 16);
        });

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

        CheckboxWidget playAnimationsCheckbox = new CallbackCheckboxWidget(10, 220, Text.of("Animations"), tickRender, aBoolean -> {
            tickRender = aBoolean;
        });
        CheckboxWidget playParticlesCheckbox = new CallbackCheckboxWidget(10, 245, Text.of("Particles (requires animations)"), tickParticles, aBoolean -> {
            tickParticles = aBoolean;
        });

        ButtonWidget exportButton = new ButtonWidget(10, 385, 65, 20, Text.of("Export"), button -> {
            captureScheduled = true;
        });
        TextFieldWidget resolutionField = new TextFieldWidget(client.textRenderer, 10, 305, 50, 20, Text.of("2048"));
        resolutionField.setEditableColor(0x00FF00);
        resolutionField.setText(String.valueOf(exportResolution));
        resolutionField.setTextPredicate(s -> s.matches("[0-9]{0,5}"));
        resolutionField.setChangedListener(s -> {
            if (s.length() < 1) return;
            int resolution = Integer.parseInt(s);
            if ((resolution != 0) && ((resolution & (resolution - 1)) != 0) || resolution < 16 || resolution > 16384) {
                resolutionField.setEditableColor(0xFF0000);
                exportButton.active = false;
            } else {
                resolutionField.setEditableColor(0x00FF00);
                exportResolution = resolution;
                exportButton.active = true;
            }
        });
        CheckboxWidget doHiResCheckbox = new CallbackCheckboxWidget(10, 335, Text.of("Use Variable-Resolution Renderer"), hiResRender, aBoolean -> {
            hiResRender = aBoolean;
        });
        CheckboxWidget allowMultipleRendersCheckbox = new CallbackCheckboxWidget(10, 360, Text.of("Allow Multiple Export Jobs"), allowMultiple, aBoolean -> {
            allowMultiple = aBoolean;
        });
        ButtonWidget clearQueueButton = new ButtonWidget(90, 385, 75, 20, Text.of("Clear Queue"), button -> {
            ImageExporter.clearQueue();
        });

        buttons.clear();
        addButton(colorField);

        addButton(scaleSlider);
        addButton(scaleField);

        addButton(rotationField);
        addButton(rotSlider);

        addButton(angleField);
        addButton(angleSlider);

        addButton(heightField);
        addButton(heightSlider);

        addButton(playAnimationsCheckbox);
        addButton(doHiResCheckbox);
        addButton(allowMultipleRendersCheckbox);
        addButton(playParticlesCheckbox);

        addButton(resolutionField);
        addButton(exportButton);
        addButton(clearQueueButton);
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

    @Override
    public void tick() {
        if (tickParticles) IsometricRenderHelper.allowParticles = true;
        if (tickRender) tickCallback.run();
        IsometricRenderHelper.allowParticles = false;
    }

    @Override
    public void onClose() {
        super.onClose();
        closedCallback.run();
        IsometricRenderHelper.allowParticles = true;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {

        if (((captureScheduled && !hiResRender) || studioMode) && !drawBackground) {
            fill(matrices, 0, 0, this.width, this.height, backgroundColor | 255 << 24);
        } else {

            if (drawBackground) {
                fill(matrices, 0, 0, this.width, this.height, backgroundColor | 255 << 24);
            } else {
                renderBackground(matrices);
                drawFramingHint(matrices);
            }

            drawGuiBackground(matrices);
            super.render(matrices, mouseX, mouseY, delta);

            client.textRenderer.draw(matrices, "Transform Options", 12, 20, 0xAAAAAA);

            client.textRenderer.draw(matrices, "Render Options", 12, 172, 0xAAAAAA);
            client.textRenderer.draw(matrices, "Background Color", 66, 195, 0xFFFFFF);
            fill(matrices, 160, 195, 168, 203, backgroundColor | 255 << 24);

            client.textRenderer.draw(matrices, "Export Options", 12, 287, 0xAAAAAA);
            client.textRenderer.draw(matrices, "Renderer Resolution", 66, 310, 0xFFFFFF);

            client.textRenderer.draw(matrices, "F10: Studio Mode  F12: Export", 12, height - 20, 0xAAAAAA);

            int endX = (int) (this.width - (this.width - this.height) * 0.5);
            client.textRenderer.draw(matrices, "Warning: Unless you have at least 6GB ", endX + 10, height - 60, 0xAAAAAA);
            client.textRenderer.draw(matrices, "of memory available for Minecraft, ", endX + 10, height - 50, 0xAAAAAA);
            client.textRenderer.draw(matrices, "resolutions over 8192x8192", endX + 10, height - 40, 0xAAAAAA);
            client.textRenderer.draw(matrices, "are not recommended if you want", endX + 10, height - 30, 0xAAAAAA);
            client.textRenderer.draw(matrices, "your computer to survive", endX + 10, height - 20, 0xAAAAAA);

            int currentJobs = ImageExporter.getJobCount();

            client.textRenderer.draw(matrices, ImageExporter.getProgressBarText(), 12, 423, 0xFFFFFF);

            if (currentJobs > 0) {
                drawExportProgressBar(matrices, 12, 435, (int) ((this.width - this.height) * 0.5) - 18, 150, 5);
            }
        }

        RenderSystem.setupGuiFlatDiffuseLighting(Util.make(new Vector3f(0.2F, 1.0F, -0.7F), Vector3f::normalize), Util.make(new Vector3f(-0.2F, 1.0F, 0.7F), Vector3f::normalize));

        int i = (this.width - 248) / 2 + 10;
        int j = (this.height - 166) / 2 + 8;
        GlStateManager.pushMatrix();
        GlStateManager.translatef(0, 0, 10F);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translatef(i, j, 10F);

        drawIsometrically(115, Math.round(renderHeight * 1f + (height - 515f) / 10f), renderScale * height / 515f, rotation, angle, tickRender ? ((MinecraftClientAccessor) client).getRenderTickCounter().tickDelta : 0, renderCallback);

        GlStateManager.popMatrix();

        if (captureScheduled) {
            capture();
            captureScheduled = false;
        }
    }

    public static void drawIsometrically(int x, int y, float scale, float rotate, float angle, float delta, IsometricRenderHelper.RenderCallback renderCallback) {

        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) x, (float) y, 1500 + scale);
        RenderSystem.scalef(1, 1, -1);

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate(0, 0, 1000);

        matrixStack.scale(scale, scale, 1);

        Quaternion flip = Vector3f.POSITIVE_Z.getDegreesQuaternion(180);
        flip.hamiltonProduct(Vector3f.POSITIVE_X.getDegreesQuaternion(-angle));

        Quaternion rotation = Vector3f.POSITIVE_Y.getDegreesQuaternion(rotate);

        matrixStack.multiply(flip);
        matrixStack.multiply(rotation);

        EntityRenderDispatcher entityRenderDispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadows(false);

        VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        renderCallback.render(matrixStack, immediate, delta);

        immediate.draw();
        entityRenderDispatcher.setRenderShadows(true);
        RenderSystem.popMatrix();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F12) {
            if (ImageExporter.acceptsJobs() && (ImageExporter.getJobCount() < 1 || allowMultiple)) {
                captureScheduled = true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_F10) {
            studioMode = !studioMode;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void drawFramingHint(MatrixStack matrices) {
        int beginX = (int) ((this.width - this.height) * 0.5);
        int endX = (int) (this.width - (this.width - this.height) * 0.5);

        fillGradient(matrices, beginX + 5, 0, endX - 5, 5, 0x90000000, 0x90000000);
        fillGradient(matrices, beginX + 5, height - 5, endX - 5, height, 0x90000000, 0x90000000);
        fillGradient(matrices, beginX, 0, beginX + 5, height, 0x90000000, 0x90000000);
        fillGradient(matrices, endX - 5, 0, endX, height, 0x90000000, 0x90000000);
    }

    protected void drawGuiBackground(MatrixStack matrices) {
        int beginX = (int) ((this.width - this.height) * 0.5);
        int endX = (int) (this.width - (this.width - this.height) * 0.5);

        fillGradient(matrices, 0, 0, beginX, height, 0x90000000, 0x90000000);
        fillGradient(matrices, endX, 0, width, height, 0x90000000, 0x90000000);
    }

    public static void drawExportProgressBar(MatrixStack matrices, int x, int y, int drawWidth, int barWidth, double speed) {


        final int color = ImageExporter.currentlyExporting() ? 0xFF00FF00 : 0xFFFF8800;
        int end = x + drawWidth + barWidth;

        int offset = (int) (System.currentTimeMillis() / speed % (drawWidth + barWidth));

        int endWithOffset = x + offset;
        if (endWithOffset > end) endWithOffset = end;

        fill(matrices, Math.max(x + offset - barWidth, x), y, Math.min(endWithOffset, x + drawWidth), y + 2, color);

    }

    protected void capture() {

        if (hiResRender) {
            NativeImage image = IsometricRenderHelper.renderIntoImage(exportResolution, (matrices, vertexConsumerProvider, tickDelta) -> {

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

            });

            addImageToQueue(image);
        } else {
            addImageToQueue(IsometricRenderHelper.takeKeyedSnapshot(MinecraftClient.getInstance().getFramebuffer(), backgroundColor, true));
        }
    }

    protected void addImageToQueue(NativeImage image) {
        ImageExporter.addJob(image);
    }

    private static class SliderWidgetImpl extends SliderWidget {

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

    private static class CallbackCheckboxWidget extends CheckboxWidget {

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
