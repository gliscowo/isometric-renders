package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExportThread;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Iterator;

public class BatchIsometricBlockRenderScreen extends IsometricRenderScreen {

    private final Iterator<BlockState> toRender;
    private int frameDelay = 5;

    public BatchIsometricBlockRenderScreen(Iterator<BlockState> renderBlocks) {
        this.toRender = renderBlocks;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {

        if (frameDelay > 0) {
            if (ImageExportThread.acceptsJobs()) frameDelay--;
        } else {
            captureScheduled = true;
            frameDelay = 5;

            if (!toRender.hasNext()) {
                client.openScreen(null);
                ImageExportThread.enableExporting();
                return;
            }

            IsometricRenderHelper.setupBlockStateRender(this, toRender.next());
        }


        super.render(matrices, mouseX, mouseY, delta);
    }
}
