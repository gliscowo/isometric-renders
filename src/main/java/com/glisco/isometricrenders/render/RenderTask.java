package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.screen.RenderScreen;
import com.glisco.isometricrenders.screen.ScreenScheduler;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

public enum RenderTask {
    ATLAS((source, renderables) -> {
        ScreenScheduler.schedule(new RenderScreen(
                new ItemAtlasRenderable(source, new ArrayList<>(renderables))
        ));
    }),
    BATCH_ITEM((source, renderables) -> {
        ScreenScheduler.schedule(new RenderScreen(
                BatchRenderable.of(
                        source + "/items",
                        renderables.stream()
                                .map(ItemRenderable::new)
                                .toList()
                )
        ));
    }),
    BATCH_TOOLTIP((source, renderables) -> {
        ScreenScheduler.schedule(new RenderScreen(
                BatchRenderable.of(
                        source + "/tooltips",
                        renderables.stream()
                                .map(TooltipRenderable::new)
                                .toList()
                )
        ));
    }),
    BATCH_BLOCK((source, renderables) -> {
        ScreenScheduler.schedule(new RenderScreen(
                BatchRenderable.of(
                        source + "/blocks",
                        renderables.stream()
                                .filter(stack -> stack.getItem() instanceof BlockItem)
                                .map(stack -> ((BlockItem) stack.getItem()).getBlock())
                                .map(BlockStateRenderable::of)
                                .toList()
                )
        ));
    });

    public final BiConsumer<String, Collection<ItemStack>> action;

    RenderTask(BiConsumer<String, Collection<ItemStack>> action) {
        this.action = action;
    }
}
