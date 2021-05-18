package com.glisco.isometricrenders.client.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;

import java.util.Iterator;

public class BatchIsometricBlockRenderScreen extends BatchIsometricRenderScreen<BlockState> {

    public BatchIsometricBlockRenderScreen(Iterator<BlockState> renderObjects, String name) {
        super(renderObjects, name);
        this.name = IsometricRenderHelper.getLastFile(IsometricRenderHelper.getNextFolder(FabricLoader.getInstance().getGameDir().resolve("renders/batches/blocks/").toFile(), name).toString());
    }

    @Override
    protected void setupRender() {
        if (!renderObjects.hasNext()) {
            onClose();
            return;
        }

        captureScheduled = true;
        IsometricRenderPresets.setupBlockStateRender(this, renderObjects.next());
    }

    @Override
    public void setup(IsometricRenderHelper.RenderCallback renderCallback, String filename) {
        super.setup(renderCallback, "batches/blocks/" + name + "/" + IsometricRenderHelper.getLastFile(filename));
    }
}
