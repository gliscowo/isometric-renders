package com.glisco.isometricrenders.client.gui;

import net.minecraft.item.ItemStack;

import java.util.Iterator;

public class BatchIsometricItemRenderScreen extends BatchIsometricRenderScreen<ItemStack> {

    private ItemStack next = ItemStack.EMPTY;

    public BatchIsometricItemRenderScreen(Iterator<ItemStack> renderObjects, boolean allowInsaneResolutions) {
        super(renderObjects, allowInsaneResolutions);
    }

    @Override
    protected void setupRender() {
        while (next.isEmpty()) {
            if (!renderObjects.hasNext()) {
                onClose();
                return;
            }
            next = renderObjects.next();
        }

        captureScheduled = true;
        IsometricRenderPresets.setupItemStackRender(this, next);
        next = ItemStack.EMPTY;
    }

}
