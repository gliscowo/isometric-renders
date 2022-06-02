package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.mixin.access.ParticleManagerAccessor;
import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.setting.IntSetting;
import com.glisco.isometricrenders.setting.Setting;
import com.glisco.isometricrenders.setting.Settings;
import com.glisco.isometricrenders.util.ImageExporter;
import com.glisco.isometricrenders.util.Translate;
import com.glisco.isometricrenders.widget.SettingCheckbox;
import com.glisco.isometricrenders.widget.SettingSliderWidget;
import com.glisco.isometricrenders.widget.SettingTextField;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.glisco.isometricrenders.setting.Settings.*;
import static com.glisco.isometricrenders.util.Translate.gui;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public abstract class RenderScreen extends Screen {

    private Map<IntSetting, SettingParameters> settings = new LinkedHashMap<>();

    private ButtonWidget exportButton;

    protected IsometricRenderHelper.RenderCallback renderCallback = (matrices, vertexConsumerProvider, tickDelta) -> {};
    protected Runnable tickCallback = () -> {};
    protected Runnable closedCallback = () -> {};
    protected Consumer<File> exportCallback = (file) -> {};

    public Setting<Boolean> playAnimations = Setting.of(false);
    protected Setting<Boolean> tickParticles = Setting.of(true);

    protected boolean studioMode = false;
    protected boolean drawBackground = false;
    protected boolean captureScheduled = false;

    protected int viewportBeginX;
    protected int viewportEndX;
    protected String currentFilename = "";

    public RenderScreen() {
        super(Text.of(""));
    }

    @Override
    protected void init() {
        viewportBeginX = (int) ((this.width - this.height) * 0.5);
        viewportEndX = (int) (this.width - (this.width - this.height) * 0.5) + 1;
        final int sliderWidth = viewportBeginX - 55;

        ((ParticleManagerAccessor) client.particleManager).getParticles().clear();
        com.glisco.isometricrenders.render.IsometricRenderHelper.allowParticles = false;

        buildGuiElements();

        int y = 40;
        for (var setting : this.settings.keySet()) {
            final var params = this.settings.get(setting);

            this.addDrawableChild(new SettingTextField(10, y, setting));
            this.addDrawableChild(new SettingSliderWidget(50, y, sliderWidth, Translate.gui(params.translationKey()), params.step(), setting));
            y += 30;
        }

        TextFieldWidget colorField = new TextFieldWidget(client.textRenderer, viewportEndX + 10, 38, 50, 20, Text.of("#0000ff"));
        colorField.setTextPredicate(s -> s.matches("^#([A-Fa-f0-9]{0,6})$"));
        colorField.setText("#" + String.format("%02X", backgroundColor >> 16) + String.format("%02X", backgroundColor >> 8 & 0xFF) + String.format("%02X", backgroundColor & 0xFF));
        colorField.setChangedListener(s -> {
            if (s.substring(1).length() < 6) return;
            backgroundColor = Integer.parseInt(s.substring(1), 16);
        });

        var playAnimationsCheckbox = new SettingCheckbox(viewportEndX + 10, 68, gui("animations"), playAnimations);
        var playParticlesCheckbox = new SettingCheckbox(viewportEndX + 10, 93, gui("particles"), tickParticles);

        var doHiResCheckbox = new SettingCheckbox(viewportEndX + 10, 183, gui("use_external_renderer"), useExternalRenderer);
        var allowMultipleRendersCheckbox = new SettingCheckbox(viewportEndX + 10, 208, gui("allow_multiple_export_jobs"), allowMultipleNonThreadedJobs);
        var dumpIntoRootCheckbox = new SettingCheckbox(viewportEndX + 10, 233, gui("dump_into_root"), dumpIntoRoot);

        var clearQueueButton = new ButtonWidget(viewportEndX + 80, 260, 75, 20, gui("clear_queue"), button -> {
            ImageExporter.clearQueue();
        });
        exportButton = new ButtonWidget(viewportEndX + 10, 260, 65, 20, gui("export"), button -> {
            if ((ImageExporter.getJobCount() < 1 || allowMultipleNonThreadedJobs.get())) {
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
            if (((resolution != 0) && ((resolution & (resolution - 1)) != 0) || resolution < 16 || resolution > 16384) && !allowInsaneResolutions.get()) {
                resolutionField.setEditableColor(0xFF0000);
                exportButton.active = false;
            } else {
                resolutionField.setEditableColor(0x00FF00);
                exportResolution = resolution;
                exportButton.active = true;
            }
        });

        ButtonWidget rendersFolderButton = new ButtonWidget(viewportEndX + 10, 285, 90, 20, gui("open_folder"), button -> {
            Util.getOperatingSystem().open(FabricLoader.getInstance().getGameDir().resolve("renders").toFile());
        });

        addDrawableChild(colorField);

        addDrawableChild(playAnimationsCheckbox);
        addDrawableChild(doHiResCheckbox);
        addDrawableChild(allowMultipleRendersCheckbox);
        addDrawableChild(playParticlesCheckbox);
        addDrawableChild(dumpIntoRootCheckbox);

        addDrawableChild(resolutionField);
        addDrawableChild(exportButton);
        addDrawableChild(clearQueueButton);
        addDrawableChild(rendersFolderButton);
    }

    protected void addSetting(IntSetting setting, String translationKey, int step) {
        this.settings.put(setting, new SettingParameters(translationKey, step));
    }

    protected void removeSetting(IntSetting setting) {
        this.settings.remove(setting);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        final boolean drawOnlyBackground = ((captureScheduled && !useExternalRenderer.get()) || studioMode) && !this.drawBackground;

        matrices.push();
        matrices.translate(0, 0, -760);

        if (this.drawBackground || drawOnlyBackground) {
            fill(matrices, 0, 0, this.width, this.height, Settings.backgroundColor | 255 << 24);
        } else {
            renderBackground(matrices);
        }
        matrices.pop();

        lightingProfile.setup();

        int i = (width) / 2;
        int j = (height) / 2;

        matrices.push();
        matrices.loadIdentity();
        matrices.translate(0, 0, 750);
        matrices.translate(i, -j, 0);
        drawContent(matrices);
        matrices.pop();

        if (!drawOnlyBackground) {
            if (!this.drawBackground) drawFramingHint(matrices);

            drawGuiBackground(matrices);

            RenderSystem.clear(GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            super.render(matrices, mouseX, mouseY, delta);

            drawGuiText(matrices);

            client.textRenderer.draw(matrices, gui("render_options"), viewportEndX + 12, 20, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("background_color"), viewportEndX + 66, 43, 0xFFFFFF);
            fill(matrices, viewportEndX + 160, 43, viewportEndX + 168, 51, Settings.backgroundColor | 255 << 24);

            client.textRenderer.draw(matrices, gui("export_options"), viewportEndX + 12, 135, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("renderer_resolution"), viewportEndX + 66, 160, 0xFFFFFF);

            client.textRenderer.draw(matrices, gui("hotkeys"), viewportEndX + 12, height - 20, 0xAAAAAA);

            client.textRenderer.draw(matrices, gui("memory_warning1"), 10, height - 60, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning2"), 10, height - 50, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning3"), 10, height - 40, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning4"), 10, height - 30, 0xAAAAAA);
            client.textRenderer.draw(matrices, gui("memory_warning5"), 10, height - 20, 0xAAAAAA);

            client.textRenderer.draw(matrices, ImageExporter.getProgressBarText(), viewportEndX + 12, 350, 0xFFFFFF);

            if (ImageExporter.getJobCount() > 0) {
                drawExportProgressBar(matrices, viewportEndX + 12, 362, width - viewportEndX - 30, 150, 5);
            }
        }

        if (captureScheduled) {
            capture().whenComplete((file, throwable) -> exportCallback.accept(file));
            captureScheduled = false;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        super.close();
        closedCallback.run();
        IsometricRenderHelper.allowParticles = true;
    }

    @Override
    public void tick() {
        if (tickParticles.get()) IsometricRenderHelper.allowParticles = true;
        if (playAnimations.get()) tickCallback.run();
        IsometricRenderHelper.allowParticles = false;
    }

    public void scheduleCapture() {
        this.captureScheduled = true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_F12) {
            if ((ImageExporter.getJobCount() < 1 || allowMultipleNonThreadedJobs.get()) && exportButton.active) {
                captureScheduled = true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_F10) {
            studioMode = !studioMode;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected CompletableFuture<File> capture() {
        if (useExternalRenderer.get()) {
            return addImageToExportQueue(IsometricRenderHelper.renderIntoImage(exportResolution, getExternalExportCallback(), lightingProfile));
        } else {
            return addImageToExportQueue(IsometricRenderHelper.takeSnapshot(MinecraftClient.getInstance().getFramebuffer(), backgroundColor, true, true));
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

    protected boolean isInViewport(double mouseX) {
        return mouseX > viewportBeginX && mouseX < viewportEndX;
    }

    public static void drawExportProgressBar(MatrixStack matrices, int x, int y, int drawWidth, int barWidth, double speed) {
        final int color = ImageExporter.currentlyExporting() ? 0xFF00FF00 : 0xFFFF8800;
        int end = x + drawWidth + barWidth;

        int offset = (int) (System.currentTimeMillis() / speed % (drawWidth + barWidth));

        int endWithOffset = x + offset;
        if (endWithOffset > end) endWithOffset = end;

        fill(matrices, Math.max(x + offset - barWidth, x), y, Math.min(endWithOffset, x + drawWidth), y + 2, color);
    }

    public void setup(IsometricRenderHelper.RenderCallback renderCallback, String filename) {
        this.renderCallback = renderCallback;
        this.currentFilename = filename;
    }

    public void setTickCallback(Runnable tickCallback) {
        this.tickCallback = tickCallback;
    }

    public void setClosedCallback(Runnable closedCallback) {
        this.closedCallback = closedCallback;
    }

    public void setExportCallback(Consumer<File> exportCallback) {
        this.exportCallback = exportCallback;
    }

    protected abstract void buildGuiElements();

    protected abstract void drawGuiText(MatrixStack matrices);

    protected abstract void drawContent(MatrixStack matrices);

    protected abstract IsometricRenderHelper.RenderCallback getExternalExportCallback();

    protected abstract CompletableFuture<File> addImageToExportQueue(NativeImage image);

    private record SettingParameters(String translationKey, int step){}
}
