package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.export.ExportMetadata;
import net.minecraft.item.ItemStack;

import java.util.Iterator;

public class BatchIsometricItemRenderScreen extends BatchIsometricRenderScreen<ItemStack> {

    private ItemStack next = ItemStack.EMPTY;

    public BatchIsometricItemRenderScreen(Iterator<ItemStack> renderObjects) {
        super(renderObjects);
    }

    @Override
    protected void setupRender() {
        while (next.isEmpty()) {
            if (!renderObjects.hasNext()) {
                onClose();
                return;
            }
            ((ExportMetadata.BatchExportMetadata)exportMetadata).next();
            next = renderObjects.next();
        }

        captureScheduled = true;
        IsometricRenderPresets.setupItemStackRender(this, next);
        next = ItemStack.EMPTY;
    }

}
