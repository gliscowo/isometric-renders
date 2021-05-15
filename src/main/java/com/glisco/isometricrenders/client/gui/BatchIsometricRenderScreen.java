package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExporter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Iterator;

public abstract class BatchIsometricRenderScreen<T> extends IsometricRenderScreen {

    protected final Iterator<T> renderObjects;
    protected final int delay;
    protected int delayTicks;
    protected boolean invalid = false;

    public BatchIsometricRenderScreen(Iterator<T> renderObjects, boolean allowInsaneResolutions) {
        this.renderObjects = renderObjects;
        this.drawBackground = true;
        if (ImageExporter.Threaded.busy()) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("§cThe export system is not available, try again in a few seconds. If this doesn't fix itself, restart your client"), false);
            invalid = true;
        }

        if (IsometricRenderScreen.hiResRender && IsometricRenderScreen.exportResolution > 2048 && !allowInsaneResolutions) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("§cResolutions over 2048x2048 are not supported for batch-rendering. If you want to risk it, append §binsane §cto your command"), false);
            invalid = true;
        }

        ImageExporter.Threaded.init();
        delay = (int) Math.pow(IsometricRenderScreen.exportResolution / 1024f, 2);
    }

    @Override
    protected void init() {
        super.init();
        for (AbstractButtonWidget widget : buttons) {
            widget.active = false;
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        ImageExporter.Threaded.finish();
    }

    @Override
    protected void addImageToQueue(NativeImage image) {
        ImageExporter.Threaded.submit(image);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (invalid) {
            onClose();
            return;
        }

        if (delayTicks > 0) {
            delayTicks--;
        }
        if (ImageExporter.Threaded.acceptsNew() && delayTicks == 0) {
            setupRender();
            delayTicks = delay;
        }
        super.render(matrices, mouseX, mouseY, delta);
    }

    protected abstract void setupRender();

}
