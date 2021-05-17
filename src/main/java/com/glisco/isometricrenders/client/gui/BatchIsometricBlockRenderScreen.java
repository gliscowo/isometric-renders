package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.export.ExportMetadata;
import net.minecraft.block.BlockState;

import java.util.Iterator;

public class BatchIsometricBlockRenderScreen extends BatchIsometricRenderScreen<BlockState> {

    public BatchIsometricBlockRenderScreen(Iterator<BlockState> renderObjects) {
        super(renderObjects);
    }

    @Override
    protected void setupRender() {
        if (!renderObjects.hasNext()) {
            onClose();
            return;
        }

        captureScheduled = true;
        ((ExportMetadata.BatchExportMetadata)exportMetadata).next();
        IsometricRenderPresets.setupBlockStateRender(this, renderObjects.next());
    }
}
