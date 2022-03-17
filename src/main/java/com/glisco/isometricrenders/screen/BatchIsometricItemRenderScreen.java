package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.render.IsometricRenderHelper;
import com.glisco.isometricrenders.render.IsometricRenderPresets;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

import java.util.Iterator;

public class BatchIsometricItemRenderScreen extends BatchIsometricRenderScreen<ItemStack> {

    private ItemStack next = ItemStack.EMPTY;

    public BatchIsometricItemRenderScreen(Iterator<ItemStack> renderObjects, String name) {
        super(renderObjects, name);
        this.name = IsometricRenderHelper.getLastFile(IsometricRenderHelper.getNextFolder(FabricLoader.getInstance().getGameDir().resolve("renders/batches/items/").toFile(), name).toString());
    }

    @Override
    protected void setupRender() {
        while (next.isEmpty()) {
            if (!renderObjects.hasNext()) {
                this.close();
                return;
            }
            next = renderObjects.next();
        }

        captureScheduled = true;
        IsometricRenderPresets.setupItemStackRender(this, next);
        next = ItemStack.EMPTY;
    }

    @Override
    public void setup(IsometricRenderHelper.RenderCallback renderCallback, String filename) {
        super.setup(renderCallback, "batches/items/" + name + "/" + IsometricRenderHelper.getLastFile(filename));
    }
}
