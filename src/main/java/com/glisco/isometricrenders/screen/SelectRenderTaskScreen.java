package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.render.RenderTask;
import com.glisco.isometricrenders.util.Translate;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class SelectRenderTaskScreen extends BaseOwoScreen<FlowLayout> {

    private final Collection<ItemStack> items;

    public SelectRenderTaskScreen(Collection<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::horizontalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        var mainPanel = Containers.verticalFlow(Sizing.content(), Sizing.content());
        mainPanel.surface(Surface.DARK_PANEL).padding(Insets.of(5)).horizontalAlignment(HorizontalAlignment.CENTER);

        mainPanel.child(Components.label(Translate.gui("select_batch_operation")).shadow(true).margins(Insets.of(5).withBottom(10)));

        var contentPanel = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        contentPanel.verticalAlignment(VerticalAlignment.CENTER);

        contentPanel.child(Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(Components.button(Translate.gui("select_item_batch"), button -> {
                    RenderTask.BATCH_ITEM.action.accept("inventory", this.items);
                    this.close();
                }).horizontalSizing(Sizing.fixed(80)).margins(Insets.bottom(5)))
                .child(Components.button(Translate.gui("select_block_batch"), button -> {
                    RenderTask.BATCH_BLOCK.action.accept("inventory", this.items);
                    this.close();
                }).horizontalSizing(Sizing.fixed(80)).margins(Insets.bottom(5)))
                .child(Components.button(Translate.gui("select_atlas"), button -> {
                    RenderTask.ATLAS.action.accept("inventory", this.items);
                    this.close();
                }).horizontalSizing(Sizing.fixed(80)))
                .padding(Insets.of(5))
        );

        var itemPreviewPanel = Containers.verticalFlow(Sizing.content(), Sizing.content());

        itemPreviewPanel.child(Components.label(Translate.gui("render_task_size", this.items.size())).margins(Insets.of(7)))
                .horizontalAlignment(HorizontalAlignment.CENTER).padding(Insets.of(3))
                .surface(Surface.flat(0x77000000).and(Surface.outline(0x77000000)))
                .margins(Insets.left(10));

        final var itemContainer = Containers.verticalFlow(Sizing.content(), Sizing.content());

        final var itemList = this.items.stream().toList();
        final var rows = MathHelper.ceilDiv(itemList.size(), 9);
        for (int row = 0; row < rows; row++) {
            var rowContainer = Containers.horizontalFlow(Sizing.content(), Sizing.content());

            for (int column = 0; column < 9; column++) {
                final var index = row * 9 + column;
                if (index >= itemList.size()) break;

                rowContainer.child(Components.item(itemList.get(index)));
            }

            itemContainer.child(rowContainer);
        }

        itemPreviewPanel.child(Containers.verticalScroll(Sizing.content(), Sizing.fixed(Math.min(250, rows * 16)), itemContainer));

        contentPanel.child(itemPreviewPanel);
        mainPanel.child(contentPanel);
        rootComponent.child(mainPanel);
    }
}
