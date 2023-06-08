package com.glisco.isometricrenders.widget;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class DynamicLabelComponent extends BaseComponent {

    private final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

    private final Supplier<Text> content;
    private int color = 0xFFFFFF;
    private boolean shadow = true;

    public DynamicLabelComponent(Supplier<Text> content) {
        this.content = content;
    }

    public DynamicLabelComponent color(int color) {
        this.color = color;
        return this;
    }

    public DynamicLabelComponent shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 100;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return this.textRenderer.fontHeight;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
        context.drawText(this.textRenderer, this.content.get(), this.x, this.y, this.color, this.shadow);
    }
}
