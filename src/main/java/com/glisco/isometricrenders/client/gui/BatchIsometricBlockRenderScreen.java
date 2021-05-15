package com.glisco.isometricrenders.client.gui;

import net.minecraft.block.BlockState;

import java.util.Iterator;

public class BatchIsometricBlockRenderScreen extends BatchIsometricRenderScreen<BlockState> {

    public BatchIsometricBlockRenderScreen(Iterator<BlockState> renderObjects, boolean allowInsaneResolutions) {
        super(renderObjects, allowInsaneResolutions);
    }

    @Override
    protected void setupRender() {
        if (!renderObjects.hasNext()) {
            onClose();
            return;
        }

        captureScheduled = true;
        IsometricRenderHelper.setupBlockStateRender(this, renderObjects.next());
    }
}
