package com.glisco.isometricrenders.client.gui;

import com.glisco.isometricrenders.client.ImageExportThread;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import java.util.Iterator;

public class BatchIsometricItemRenderScreen extends IsometricRenderScreen {

    private final Iterator<ItemStack> toRender;
    private ItemStack next = ItemStack.EMPTY;
    private int frameDelay = 5;

    public BatchIsometricItemRenderScreen(Iterator<ItemStack> stacks) {
        this.toRender = stacks;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {

        if (frameDelay > 0) {
            frameDelay--;
        } else {
            while (next.isEmpty()) {
                if (!toRender.hasNext()) {
                    client.openScreen(null);
                    ImageExportThread.enableExporting();
                    return;
                }
                next = toRender.next();
            }

            IsometricRenderHelper.setupItemStackRender(this, next);

            captureScheduled = true;
            frameDelay = 5;
        }


        super.render(matrices, mouseX, mouseY, delta);

    }
}
