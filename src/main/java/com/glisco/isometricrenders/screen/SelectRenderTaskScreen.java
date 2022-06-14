package com.glisco.isometricrenders.screen;

import com.glisco.isometricrenders.render.RenderTask;
import com.glisco.isometricrenders.util.Translate;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Collection;

public class SelectRenderTaskScreen extends Screen {

    private final Collection<ItemStack> items;

    public SelectRenderTaskScreen(Collection<ItemStack> items) {
        super(Text.empty());
        this.items = items;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2 - 40;
        int centerY = this.height / 2 - 35;

        this.addDrawableChild(new ButtonWidget(centerX, centerY, 80, 20, Translate.gui("select_item_batch"), button -> {
            RenderTask.BATCH_ITEM.action.accept("inventory", this.items);
            this.close();
        }));

        this.addDrawableChild(new ButtonWidget(centerX, centerY + 25, 80, 20, Translate.gui("select_block_batch"), button -> {
            RenderTask.BATCH_BLOCK.action.accept("inventory", this.items);
            this.close();
        }));

        this.addDrawableChild(new ButtonWidget(centerX, centerY + 50, 80, 20, Translate.gui("select_atlas"), button -> {
            RenderTask.ATLAS.action.accept("inventory", this.items);
            this.close();
        }));
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
